package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.Tuple2;
import cn.wisestar.server.core.constant.*;
import cn.wisestar.server.core.exception.ErrorCodeException;
import cn.wisestar.server.core.security.JwtTokenUtil;
import cn.wisestar.server.core.uitls.*;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.domain.mapper.ProjectViewMapper;
import cn.wisestar.server.domain.model.*;
import cn.wisestar.server.mapper.ProjectPartnerMapper;
import cn.wisestar.server.service.ProjectService;
import cn.wisestar.server.service.SurveyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 问卷/考试公开访问业务实现：问卷加载校验、公开答卷提交、公开查询、成绩查询、
 * 答题限制（登录/密码/白名单/Cookie/IP）、关联问卷联动等。
 *
 * 【类职责】
 * 面向"答卷人"（学生/外部用户）的公开入口逻辑：
 * 1. 问卷加载：loadProject（含登录表单验证、题库练习、随机问题处理）、validateProject 校验
 * 2. 答卷提交：saveAnswer（区分随机卷/公开查询修改/允许修改开关）、tempSaveAnswer 暂存、
 *    答题后更新白名单状态 updateProjectPartnerByAnswer
 * 3. 公开查询：loadQuery 查询表单、getQueryResult 查询结果（字段权限过滤/可编辑回写）、
 *    loadExamResult 成绩查询（排名/正确答案可见性由考试设置控制）、loadLinkResult 关联问卷回填
 * 4. 答题限制：登录限制/密码/白名单（内部/导入用户）/Cookie 限制/IP 限制/最大答题数/
 *    时间窗（CronHelper）校验
 * 5. 其他：loadDict 字典加载、答案唯一性/配额校验（validateAnswer）
 *
 * 【被谁调用】
 * - Controller：SurveyController（公开访问接口）、AnswerController（间接）
 * - 业务层：FileServiceImpl.upload（公开上传时校验项目状态）
 *
 * 【依赖什么】
 * - ProjectService/ProjectViewMapper（项目与视图）、AnswerServiceImpl（答卷读写）、
 *   ProjectPartnerMapper（参与人/白名单）、RepoServiceImpl/UserBookServiceImpl/
 *   TemplateServiceImpl（题库练习）、RandomSurveyProcessor（随机问题处理）、
 *   DictItemServiceImpl（字典）、JwtTokenUtil/AuthenticationManager（答卷登录）、
 *   ObjectMapper（JSON 序列化）、MessageSource（i18n）
 *
 * 【核心数据流】
 * 答卷人访问链接 → SurveyController → loadProject（校验+加载 schema）→ 提交答案
 * → saveAnswer（限制校验 → AnswerServiceImpl.saveAnswer 落库）→ 考试模式计算错题入错题本
 * → 白名单状态更新 → 返回答卷 ID；查询侧按配置的字段权限过滤后返回。
 *
 * @author javahuang
 * @date 2021/8/22
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SurveyServiceImpl implements SurveyService {

    private final ProjectService projectService;

    private final ProjectViewMapper projectViewMapper;

    private final AnswerServiceImpl answerService;

    private final ProjectPartnerMapper projectPartnerMapper;

    private final AuthenticationManager authenticationManager;

    private final JwtTokenUtil jwtTokenUtil;

    private final DictItemServiceImpl dictItemService;

    private final RepoServiceImpl repoService;

    private final UserBookServiceImpl userBookService;

    private final TemplateServiceImpl templateService;

    private final ObjectMapper objectMapper;

    private final RandomSurveyProcessor randomSurveyProcessor;

    private final MessageSource messageSource;

    /**
     * 加载公开问卷/考试页面数据（进入答卷页面的主入口）。
     *
     * 【分支逻辑】
     * 1. repoId 非空（题库练习）：从题库加载题目组装练习卷；已有未完成答卷则回填
     *    已答内容（examInfo 供前端判断对错），否则按练习类型（O 顺序/R 随机/W 错题）
     *    生成题目列表并预创建一份 tempSave=0 的答卷；
     * 2. answerId 非空（随机卷/修改答案）：直接按答卷 id 回显答案与问卷快照；
     * 3. 其他：先做登录表单校验（convertAndValidateLoginFormIfNeeded），再校验问卷状态
     *    （validateProject：停用/数量/时间/各类限制）；
     *    - 需要登录/密码/白名单时返回登录表单 schema（loginRequired=true）；
     *    - 否则处理随机问题（randomSurveyProcessor.processRandomSurvey）并回填最近答案
     *      （允许修改开关开启时 getLatestAnswer）。
     *
     * @param query 项目 id、repoId（练习）、answerId、examExerciseType 等
     * @return 公开项目视图（survey schema + 答案 + 登录标识）
     * @implNote 调用链：SurveyController.loadProject → loadProject → ProjectService.getProject
     * → AnswerServiceImpl/RandomSurveyProcessor 等。answerService 如需验证密码，
     * 则只有密码输入正确后才开始加载 schema。
     */
    @Override
    public PublicProjectView loadProject(ProjectQuery query) {
        ProjectView project = projectService.getProject(query.getId());
        PublicProjectView projectView = projectViewMapper.toPublicProjectView(project);
        if (project == null) {
            throw new ErrorCodeException(ErrorCode.ProjectNotFound);
        }
        SurveySchema loginFormSchema = null;
        // 题库练习，从题库加载题目
        if (query.getRepoId() != null) {
            RepoView repo = repoService.getRpo(query.getRepoId());
            Answer answer = answerService.getOne(Wrappers.<Answer>lambdaQuery()
                    .eq(StringUtils.isNotBlank(query.getAnswerId()), Answer::getId, query.getAnswerId())
                    .eq(Answer::getCreateBy, SecurityContextUtils.getUserId())
                    .eq(StringUtils.isBlank(query.getAnswerId()), Answer::getTempSave, 0)
                    .eq(StringUtils.isBlank(query.getAnswerId()), Answer::getRepoId, query.getRepoId())
                    .eq(StringUtils.isBlank(query.getAnswerId()), Answer::getExamExerciseType, query.getExamExerciseType())
                    .last("limit 1"));
            if (answer != null) {
                projectView.setAnswerId(answer.getId());
                projectView.setAnswer(Optional.ofNullable(answer.getAnswer()).orElse(answer.getTempAnswer()));
                //  用于前端判断题目是否已答，及是否正确或者失败
                projectView.setExamInfo(answer.getExamInfo());
                if (answer.getSurvey() != null) {
                    projectView.setSurvey(answer.getSurvey());
                }
            } else {
                // 创建一条答案数据
                // 题库顺序练习
                List<SurveySchema> questionList = new ArrayList<>();
                if (query.getExamExerciseType() == ExamExerciseTypeEnum.O || query.getExamExerciseType() == ExamExerciseTypeEnum.R) {
                    questionList = templateService.list(Wrappers.<Template>lambdaQuery()
                            .select(Template::getId)
                            .eq(Template::getRepoId, query.getRepoId())).stream().map(t -> {
                        SurveySchema question = new SurveySchema();
                        question.setId(t.getId());
                        return question;
                    }).collect(Collectors.toList());
                }
                if (query.getExamExerciseType() == ExamExerciseTypeEnum.R) {
                    Collections.shuffle(questionList);
                }
                if (query.getExamExerciseType() == ExamExerciseTypeEnum.W) {
                    questionList = userBookService.list(Wrappers.<UserBook>lambdaQuery()
                                    .select(UserBook::getTemplateId)
                                    .eq(UserBook::getRepoId, query.getRepoId())
                                    .eq(UserBook::getCreateBy, SecurityContextUtils.getUserId())
                                    .gt(UserBook::getWrongTimes, 0)).stream()
                            .map(t -> {
                                SurveySchema question = new SurveySchema();
                                question.setId(t.getTemplateId());
                                return question;
                            }).collect(Collectors.toList());
                }
                SurveySchema survey = SurveySchema.builder().id(query.getRepoId())
                        .title(repo.getName())
                        .attribute(SurveySchema.Attribute.builder()
                                .mode(SurveySchema.SchemaMode.exam)
                                .submitButton(i18n("survey.practice.finish"))
                                .build())
                        .children(questionList)
                        .build();
                Answer answer2save = new Answer();
                answer2save.setSurvey(survey);
                answer2save.setRepoId(query.getRepoId());
                answer2save.setTempSave(0);
                answer2save.setProjectId(ExerciseProjectTemplate.EXERCISE_PROJECT_ID);
                answer2save.setExamExerciseType(query.getExamExerciseType());
                answer2save.setCreateBy(SecurityContextUtils.getUserId());
                answerService.save(answer2save);
                projectView.setSurvey(survey);
                projectView.setAnswerId(answer2save.getId());
            }
            return projectView;
        } else if (query.getAnswerId() == null) {
            // 表单需要验证
            loginFormSchema = convertAndValidateLoginFormIfNeeded(project, null);
            // 校验问卷
            validateProject(project);
        } else {
            // 直接根据答案加载出 schema
            AnswerQuery answerQuery = new AnswerQuery();
            answerQuery.setId(query.getAnswerId());
            AnswerView answerView = answerService.getAnswer(answerQuery);
            if (answerView != null) {
                projectView.setAnswer(answerView.getAnswer());
                if (answerView.getSurvey() != null) {
                    // 随机问题
                    projectView.setSurvey(answerView.getSurvey());
                }
            }
            return projectView;
        }

        // 如果需要登录，将问卷 schema 替换成登录表单的 schema
        if (loginFormSchema != null) {
            projectView.setSurvey(loginFormSchema);
            projectView.setLoginRequired(true);
        } else {
            // 随机问题
            randomSurveyProcessor.processRandomSurvey(project, projectView);
           //  replaceSchemaIfRandomSchema(project, projectView);
            // 允许修改答案
            projectView.setAnswer(getLatestAnswer(projectView, null));
        }
        projectView.setIsAuthenticated(SecurityContextUtils.isAuthenticated());
        return projectView;
    }

    /**
     * 校验问卷并加载页面数据（登录表单验证 + 随机问题处理 + 问卷状态校验）。
     *
     * @param query 项目 id 与登录表单答案（含 whitelistName）
     * @return 公开项目视图（回填最近答案，白名单用户按 whitelistName 匹配答卷人）
     * @implNote 调用链：SurveyController.validateProject → validateProject。
     */
    @Override
    public PublicProjectView validateProject(ProjectQuery query) {
        String projectId = query.getId();
        ProjectView project = projectService.getProject(projectId);
        // 登录验证
        convertAndValidateLoginFormIfNeeded(project, query.getAnswer());
        PublicProjectView projectView = projectViewMapper.toPublicProjectView(project);
        // 随机问题
        randomSurveyProcessor.processRandomSurvey(project, projectView);
        // 校验问卷
        validateProject(project);
        projectView.setAnswer(getLatestAnswer(projectView, (String) SchemaHelper.getLoginFormAnswer(query.getAnswer(),
                SchemaHelper.LoginFormFieldEnum.whitelistName)));
        return projectView;
    }

    /**
     * 问卷答题统计（各题选项计数，供前端实时统计/配额校验）。
     *
     * @param query 项目 id
     * @return 公开统计视图（各题选项被选次数）
     * @implNote 调用链：SurveyController.statProject / 本类 validateAnswer（配额校验）。
     * 基于该项目全部答卷（pageSize=-1 全量）与项目 schema 统计。
     */
    @Override
    public PublicStatisticsView statProject(ProjectQuery query) {
        AnswerQuery answerQuery = new AnswerQuery();
        answerQuery.setProjectId(query.getId());
        answerQuery.setPageSize(-1);
        List<AnswerView> answers = answerService.listAnswer(answerQuery).getList();
        ProjectView project = projectService.getProject(query.getId());
        return new ProjectStatHelper(project.getSurvey(), answers).stat();
    }

    /**
     * 公开提交/更新答卷（答卷人入口）。
     *
     * 【答案归属确定逻辑】（按优先级）
     * 1. 随机卷 Cookie（COOKIE_RANDOM_PROJECT_PREFIX+projectId）非空 → 复用 Cookie 中答卷 id；
     * 2. 公开查询修改（queryId 非空）→ validateAndMergeAnswer 校验可编辑字段后合并旧答案；
     * 3. 显式传 id 且非练习项目 → 需项目开启"允许修改答案"开关（enableUpdate）否则拒绝；
     * 4. 其他 → validateAndGetLatestAnswer：校验通过且（已登录 + 允许修改）时复用最近一次答卷。
     *
     * 【保存后处理】
     * - AnswerServiceImpl.saveAnswer 落库 + 生成答题明细；
     * - 考试模式（非练习项目）：返回总分与每题得分，并把错题写入错题本
     *   （userBookService.saveWrongQuestion）；
     * - 白名单答卷：updateProjectPartnerByAnswer 更新参与人状态为已答题；
     * - 清理随机卷 Cookie。
     *
     * @param request 答卷请求（projectId、answer、id、queryId、whitelistName 等）
     * @return 公开答卷视图（answerId，考试模式附带 examScore/questionScore）
     * @implNote 调用链：SurveyController.saveAnswer → saveAnswer。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublicAnswerView saveAnswer(AnswerRequest request) {
        String projectId = request.getProjectId();
        PublicAnswerView result = new PublicAnswerView();
        ProjectView project = projectService.getProject(projectId);
        String answerId = request.getId();
        // 随机问卷更新答案
        String randomSurveyCookieName = AppConsts.COOKIE_RANDOM_PROJECT_PREFIX + project.getId();
        String answerIdFromCookie = ContextHelper.getCookie(randomSurveyCookieName);
        if (isNotBlank(answerIdFromCookie)) {
            answerId = answerIdFromCookie;
        } else if (isNotBlank(request.getQueryId())) {
            // 公开查询修改答案
            validateAndMergeAnswer(project, request);
        } else if (isNotBlank(request.getId()) && !ExerciseProjectTemplate.EXERCISE_PROJECT_ID.equals(request.getProjectId())) {
            // 传入答案ID 并且设置了允许修改答案，则可以修改问卷答案
            boolean enableUpdate = Boolean.TRUE.equals(project.getSetting().getSubmittedSetting().getEnableUpdate());
            if (!enableUpdate) {
                throw new ErrorCodeException(ErrorCode.AnswerChangeDisabled);
            }
        } else {
            // 问卷允许修改答案 开关修改答案
            AnswerView latestAnswer = validateAndGetLatestAnswer(project, request);
            if (latestAnswer != null) {
                answerId = latestAnswer.getId();
            }
        }

        // 保存答案
        request.setId(answerId);
        AnswerView answerView = answerService.saveAnswer(request);
        result.setAnswerId(answerView.getId());
        // 考试模式，计算分值传给前端
        if (ProjectModeEnum.exam.equals(project.getMode()) && !ExerciseProjectTemplate.EXERCISE_PROJECT_ID.equals(projectId)) {
            result.setExamScore(answerView.getExamScore());
            result.setQuestionScore(answerView.getExamInfo().getQuestionScore());
            // 计算错题
            AnswerExamInfo answerExamInfo = answerView.getExamInfo();
            LinkedHashMap<String, Double> questionScore = answerExamInfo.getQuestionScore();
            userBookService.saveWrongQuestion(questionScore);
        }
        // 白名单更新答题信息
        request.setId(answerView.getId());
        updateProjectPartnerByAnswer(request, project);
        // 完事儿删除 cookie 标识
        if (isNotBlank(answerIdFromCookie)) {
            Cookie cookie = new Cookie(randomSurveyCookieName, answerIdFromCookie);
            cookie.setMaxAge(0);
            ContextHelper.getCurrentHttpResponse().addCookie(cookie);
        }
        return result;
    }

    /**
     * 加载公开查询验证表单（校验链接有效性后返回查询条件表单 schema）。
     *
     * @param request 含项目 id、查询配置 id（resultId）与密码等
     * @return 查询验证视图（表单 schema，含密码题则追加密码输入框）
     * @implNote 调用链：SurveyController.loadQuery → loadQuery → getProjectAndQueryThenValidate
     * → buildQueryFormSchema。
     */
    @Override
    @SneakyThrows
    public PublicQueryVerifyView loadQuery(PublicQueryRequest request) {
        Tuple2<ProjectView, ProjectSetting.PublicQuery> projectAndQuery = getProjectAndQueryThenValidate(request);
        SurveySchema schema = buildQueryFormSchema(projectAndQuery.getFirst(), projectAndQuery.getSecond());
        PublicQueryVerifyView view = new PublicQueryVerifyView();
        view.setSchema(schema);
        return view;
    }

    /**
     * 公开查询答卷结果。
     *
     * @param request 项目 id、查询配置 id、查询表单答案（answer）与 URL 参数（query）
     * @return 查询结果视图（结果 schema + 字段权限 + 匹配答卷列表）
     * @implNote 调用链：SurveyController.getQueryResult → getQueryResult。
     * - findAnswerByQuery 按条件（表单 + URL 参数 + 密码字段剔除）查匹配答卷；
     * - 按 fieldPermission 过滤隐藏字段（filterAnswerByFieldPermission）；
     * - 考试模式字段权限允许时附带 examScore 到答案中。
     */
    @Override
    public PublicQueryView getQueryResult(PublicQueryRequest request) {
        Tuple2<ProjectView, ProjectSetting.PublicQuery> projectAndQuery = getProjectAndQueryThenValidate(request);
        SurveySchema schema = buildQueryResultSchema(projectAndQuery.getFirst(), projectAndQuery.getSecond());
        PublicQueryView view = new PublicQueryView();
        view.setSchema(schema);
        List<Answer> answers = findAnswerByQuery(request, projectAndQuery);
        // 根据配置的权限信息过滤答案
        LinkedHashMap<String, Integer> fieldPermission = projectAndQuery.getSecond().getFieldPermission();
        view.setFieldPermission(fieldPermission);
        view.setAnswers(answers.stream().map(answer -> {
            filterAnswerByFieldPermission(answer.getAnswer(), fieldPermission);
            PublicAnswerView answerView = new PublicAnswerView();
            answerView.setAnswerId(answer.getId());
            answerView.setAnswer(answer.getAnswer());
            if (FieldPermissionType.visible.equals(fieldPermission.get("examScore"))
                    || FieldPermissionType.editable.equals(fieldPermission.get("examScore"))) {
                // 公开查询允许查询分值
                answer.getAnswer().put("examScore", Collections.singletonMap("examScore", answer.getExamScore()));
            }
            answerView.setCreateAt(answer.getCreateAt());
            return answerView;
        }).collect(Collectors.toList()));
        return view;
    }

    /**
     * 加载公开字典项（答卷页下拉选项数据）。
     *
     * @param request 字典码 dictCode + 级联层级/父值/搜索词/条数限制
     * @return 公开字典视图列表（label/value）
     * @implNote 调用链：SurveyController.loadDict → loadDict → DictItemServiceImpl.list。
     */
    @Override
    public List<PublicDictView> loadDict(PublicDictRequest request) {
        return dictItemService
                .list(Wrappers.<CommDictItem>lambdaQuery().eq(CommDictItem::getDictCode, request.getDictCode())
                        .eq(request.getCascaderLevel() != null, CommDictItem::getItemLevel, request.getCascaderLevel())
                        .eq(request.getParentValue() != null, CommDictItem::getParentItemValue,
                                request.getParentValue())
                        .and(isNotBlank(request.getSearch()),
                                i -> i.like(CommDictItem::getItemName, request.getSearch()).or()
                                        .like(CommDictItem::getItemValue, request.getSearch()))
                        .last(String.format("limit %d", request.getLimit() != null ? request.getLimit() : 50)))
                .stream().map(x -> {
                    PublicDictView view = new PublicDictView();
                    view.setLabel(x.getItemName());
                    view.setValue(x.getItemValue());
                    return view;
                }).collect(Collectors.toList());
    }

    /**
     * 考试结束后的成绩查询页数据。
     *
     * @param request 项目 id + 答卷 id（answerId）
     * @return 成绩结果视图，可见性受 submittedSetting 控制：
     * - answerAnalysis=true 且考试已结束：返回正确答案与解析（answer + schema + examInfo）；
     * - transcriptVisible=true：返回总分；rankVisible=true：返回排名；
     * - 随机卷：用答卷内问卷快照替代项目 schema。
     * @implNote 调用链：SurveyController.loadExamResult → loadExamResult → AnswerServiceImpl.getAnswer。
     */
    @Override
    public PublicExamResult loadExamResult(PublicExamRequest request) {
        ProjectView project = projectService.getProject(request.getId());
        AnswerQuery answerQuery = new AnswerQuery();
        answerQuery.setId(request.getAnswerId());
        if (Boolean.TRUE.equals(project.getSetting().getSubmittedSetting().getRankVisible())) {
            answerQuery.setRankEnabled(true);
        }
        AnswerView answerView = answerService.getAnswer(answerQuery);
        ProjectSetting.SubmittedSetting submittedSetting = project.getSetting().getSubmittedSetting();
        ProjectSetting.ExamSetting examSetting = project.getSetting().getExamSetting();
        PublicExamResult result = new PublicExamResult();
        result.setName(project.getName());
        // 可以查看正确答案和解析;
        // 考试结束之后才可以查看正确答案和解析
        if (Boolean.TRUE.equals(submittedSetting.getAnswerAnalysis()) && examFinished(examSetting)) {
            result.setAnswer(answerView.getAnswer());
            result.setSchema(project.getSurvey());
            result.setExamInfo(answerView.getExamInfo());
        }
        // 显示成绩单
        if (Boolean.TRUE.equals(submittedSetting.getTranscriptVisible())) {
            result.setExamScore(answerView.getExamScore());
        }
        // 显示排名 TODO:显示排行榜
        if (Boolean.TRUE.equals(submittedSetting.getRankVisible())) {
            result.setRank(answerView.getRank());
        }
        // 随机问题
        if (answerView.getSurvey() != null) {
            result.setSchema(answerView.getSurvey());
        }
        result.setMetaInfo(answerView.getMetaInfo());
        return result;
    }

    /**
     * 暂存答案（目前仅支持登录用户 + 随机卷，按 Cookie 中的答卷 id 更新 tempAnswer）。
     *
     * @param request 含 projectId、tempSave=0、tempAnswer
     * @implNote 调用链：SurveyController.tempSaveAnswer → tempSaveAnswer → AnswerServiceImpl.updateAnswer。
     */
    @Override
    public void tempSaveAnswer(AnswerRequest request) {
        String projectId = request.getProjectId();
        if (!Integer.valueOf(0).equals(request.getTempSave()) || request.getTempAnswer() == null || projectId == null) {
            return;
        }
        String answerId = ContextHelper.getCookie(AppConsts.COOKIE_RANDOM_PROJECT_PREFIX + projectId);
        if (answerId == null) {
            return;
        }
        if (!SecurityContextUtils.isAuthenticated()) {
            // 目前仅支持登录用户后端暂存
            return;
        }
        AnswerRequest answerRequest = new AnswerRequest();
        answerRequest.setId(answerId);
        answerRequest.setTempSave(0);
        answerRequest.setTempAnswer(request.getTempAnswer());
        answerService.updateAnswer(answerRequest);
    }

    /**
     * 关联问卷联动数据加载：选择某题的某个选项后，返回关联问卷中匹配该选项值的
     * 最近答卷字段，用于自动回填。
     *
     * @param request 项目 id + 题目 id + 选项 id + 选项值（value）
     * @return 联动结果（fillAnswer：{填充题id: {填充选项id: 值}}）
     * @implNote 调用链：SurveyController.loadLinkResult → loadLinkResult。
     * 通过 buildLinkLikeCondition 构造 JSON 片段 LIKE 在关联问卷答案中匹配。
     */
    @Override
    public PublicLinkResult loadLinkResult(PublicLinkRequest request) {
        PublicLinkResult result = new PublicLinkResult();
        ProjectView projectView = projectService.getProject(request.getProjectId());
        if (projectView == null) {
            throw new ErrorCodeException(ErrorCode.ProjectNotFound);
        }
        SurveySchema currentQuestionSchema = SchemaHelper.flatSurveySchema(projectView.getSurvey()).stream()
                .filter(x -> x.getId().equals(request.getQuestionId())).findFirst().orElseGet(() -> {
                    SurveySchema surveySchema = new SurveySchema();
                    surveySchema.setChildren(new ArrayList<>());
                    return surveySchema;
                });

        List<SurveySchema.LinkSurvey> linkSurveys = currentQuestionSchema.getChildren().stream()
                .filter(x -> request.getOptionId().equals(x.getId())).findFirst().orElseGet(SurveySchema::new).getLinkSurveys();
        if (linkSurveys == null) {
            throw new ErrorCodeException(ErrorCode.LinkConditionNotFound);
        }
        LinkedHashMap<String, Map<String, Object>> fillAnswer = new LinkedHashMap<>();
        result.setAnswer(fillAnswer);
        for (SurveySchema.LinkSurvey linkSurvey : linkSurveys) {
            Answer answer = answerService
                    .getOne(Wrappers.<Answer>lambdaQuery().eq(Answer::getProjectId, linkSurvey.getLinkSurveyId())
                            .like(Answer::getAnswer, buildLinkLikeCondition(linkSurvey, request.getValue()))
                            .orderByDesc(Answer::getCreateAt).last("limit 1"));
            fillLinkFieldAndAnswer(answer != null ? answer.getAnswer() : null, linkSurvey.getLinkFields(), fillAnswer);
        }

        return result;
    }

    /**
     * 构造关联问卷答案的 LIKE 匹配片段（把选项值 JSON 序列化后去掉外层花括号）。
     *
     * @param linkSurvey 关联问卷配置
     * @param value      选项值
     * @return 形如 "optionId":"value" 的 JSON 片段
     * @implNote 被 loadLinkResult / AnswerServiceImpl.updateLinkSurveyAnswer 调用。
     */
    @SneakyThrows
    private String buildLinkLikeCondition(SurveySchema.LinkSurvey linkSurvey, Object value) {
        Map<String, Object> optionValue = new HashMap<>();
        optionValue.put(linkSurvey.getLinkOptionId(), value);
        // Map<String, Map<String, Object>> questionValue = new HashMap<>();
        // questionValue.put(linkSurvey.getLinkQuestionId(), optionValue);
        return StringUtils.substringBetween(objectMapper.writeValueAsString(optionValue), "{", "}");
    }

    /**
     * 填充关联问卷字段值到联动结果 Map（按 linkFields 配置把关联答卷中的字段值
     * 拷贝到填充题的对应选项位置）。
     *
     * @param answer     关联答卷的答案（可 null）
     * @param linkFields 联动字段配置列表
     * @param fillAnswer 结果 Map（填充题 id → {填充选项 id → 值}）
     */
    public void fillLinkFieldAndAnswer(LinkedHashMap answer, List<SurveySchema.LinkField> linkFields,
                                       LinkedHashMap<String, Map<String, Object>> fillAnswer) {
        for (SurveySchema.LinkField linkField : linkFields) {
            Map<String, Object> questionValueMap = fillAnswer.computeIfAbsent(linkField.getFillQuestionId(),
                    (k) -> new HashMap<>());
            if (answer == null) {
                continue;
            }
            Map<String, Object> linkQuestionValue = (Map<String, Object>) answer.get(linkField.getLinkQuestionId());
            if (linkQuestionValue == null) {
                continue;
            }
            questionValueMap.put(linkField.getFillOptionId(), linkQuestionValue.get(linkField.getLinkOptionId()));
        }
    }

    /**
     * 判断考试是否结束（未设置结束时间默认为已结束）。
     *
     * @param examSetting 考试设置
     * @return true=已结束（或未配置结束时间）
     * @implNote 被 loadExamResult 调用：答案与解析需考试结束后才可见。
     */
    private boolean examFinished(ProjectSetting.ExamSetting examSetting) {
        if (examSetting.getEndTime() == null || examSetting.getEndTime() < System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    /**
     * 按问卷设置校验项目状态与各类答题限制（提交前必查）。
     *
     * 【校验项】（按顺序，任一不满足即抛错）
     * 1. 项目存在性；2. status=0 已暂停（SurveySuspend）；
     * 3. 最大答卷数 maxAnswers（AnswerService.count 统计）；
     * 4. 问卷结束时间 endTime；5. 登录限制 loginLimit（需开启 loginRequired）；
     * 6. Cookie 限制 cookieLimit；7. IP 限制 ipLimit；8. 白名单限制 whitelistLimit；
     * 9. 考试时间窗（validateExamSetting）。
     *
     * @param project 项目视图
     * @implNote 被 loadProject / validateProject(ProjectQuery) / validateAndGetLatestAnswer /
     * FileServiceImpl.upload（公开上传校验）调用。
     */
    public void validateProject(ProjectView project) {
        if (project == null) {
            throw new ErrorCodeException(ErrorCode.ProjectNotFound);
        }

        ProjectSetting setting = project.getSetting();
        String projectId = project.getId();
        if (setting.getStatus() == 0) {
            throw new ErrorCodeException(ErrorCode.SurveySuspend);
        }

        Long maxAnswers = setting.getAnswerSetting().getMaxAnswers();
        // 校验最大答案条数限制
        if (maxAnswers != null) {
            AnswerQuery answerQuery = new AnswerQuery();
            answerQuery.setProjectId(project.getId());
            long totalAnswers = answerService.count(answerQuery);
            if (totalAnswers >= maxAnswers) {
                throw new ErrorCodeException(ErrorCode.ExceededMaxAnswers);
            }
        }
        // 校验问卷是否已到结束时间
        Long endTime = setting.getAnswerSetting().getEndTime();
        if (endTime != null) {
            if (new Date().getTime() > endTime) {
                throw new ErrorCodeException(ErrorCode.ExceededEndTime);
            }
        }
        // 如果需要登录，则使用账号进行限制
        if (setting.getAnswerSetting().getLoginLimit() != null
                && Boolean.TRUE.equals(setting.getAnswerSetting().getLoginRequired())) {
            validateLoginLimit(projectId, setting);
        }
        // cookie 限制
        if (setting.getAnswerSetting().getCookieLimit() != null) {
            validateCookieLimit(projectId, setting);
        }
        // ip 限制
        if (setting.getAnswerSetting().getIpLimit() != null) {
            validateIpLimit(projectId, setting);
        }
        // 白名单限制
        if (setting.getAnswerSetting().getWhitelistLimit() != null) {
            validateWhitelistLimit(projectId, setting);
        }
        validateExamSetting(project);
    }

    /**
     * 答案校验：唯一性校验 + 选项配额校验（提交答案前）。
     *
     * 【唯一性】题目配置 attribute.unique=true 的选项：检查已有答卷中该选项值是否已存在
     * （构造 JSON 片段 LIKE 查询），存在则抛 ValidationException（可配置提示文案 uniqueText）。
     * 【配额】题目配置 attribute.quota=N 的选项：统计该项目该选项当前被选次数，
     * 超过配额（optionSelectedCount+1 > quota）时拒绝提交。
     *
     * @param project 项目视图
     * @param request 答卷请求（含答案）
     * @implNote 被 validateAndGetLatestAnswer 调用。
     */
    private void validateAnswer(ProjectView project, AnswerRequest request) {
        List<SurveySchema> uniqueSchemaList = SchemaHelper.findSchemaListByAttribute(project.getSurvey(), "unique",
                true);
        SchemaHelper.TreeNode treeNode = SchemaHelper.SurveySchema2TreeNode(project.getSurvey());
        uniqueSchemaList.forEach(optionSchema -> {
            // 支持数值和字符串
            String questionId = treeNode.getTreeNodeMap().get(optionSchema.getId()).getParent().getData().getId();
            Object questionValue = request.getAnswer().get(questionId);
            if (questionValue == null) {
                return;
            }
            String uniqueQuery = String.format("\"%s\":", optionSchema.getId());
            if (SurveySchema.DataType.number == optionSchema.getAttribute().getDataType()) {
                uniqueQuery += ((Map) questionValue).get(optionSchema.getId());
            } else {
                uniqueQuery += "\"" + ((Map) questionValue).get(optionSchema.getId()) + "\"";
            }
            AnswerQuery query = new AnswerQuery();
            query.setProjectId(project.getId());
            query.setValueQuery(uniqueQuery);
            if (answerService.count(query) > 0) {
                String uniqueText = optionSchema.getAttribute().getUniqueText();
                throw new ValidationException(isNotBlank(uniqueText) ? uniqueText : i18n("survey.answer.duplicate"));
            }
        });

        // 选项配额校验
        List<SurveySchema> hasQuotaSchemaList = SchemaHelper.findSchemaHasAttribute(project.getSurvey(), "quota");
        if (hasQuotaSchemaList.size() > 0) {
            ProjectQuery query = new ProjectQuery();
            query.setId(request.getProjectId());
            PublicStatisticsView statisticsView = statProject(query);

            hasQuotaSchemaList.forEach(optionSchema -> {
                String questionId = treeNode.getTreeNodeMap().get(optionSchema.getId()).getParent().getData().getId();
                Object questionValue = request.getAnswer().get(questionId);
                if (questionValue == null) {
                    return;
                }
                boolean optionNotChecked = ((Map) questionValue).get(optionSchema.getId()) == null;
                if (optionNotChecked) {
                    return;
                }
                PublicStatisticsView.QuestionStatistics questionStatistics = statisticsView.getQuestionStatistics()
                        .get(questionId);
                int optionSelectedCount = questionStatistics.getOptionStatistics().stream()
                        .filter(x -> x.getOptionId().equals(optionSchema.getId())).findFirst()
                        .orElse(new PublicStatisticsView.OptionStatistics()).getCount();
                Integer quota = optionSchema.getAttribute().getQuota();
                if (quota != null && optionSelectedCount + 1 > quota) {
                    throw new ValidationException(i18n("survey.option.limitExceeded"));
                }
            });
        }

    }

    /**
     * 校验考试时间窗：开始时间未到抛 ExamUnStarted，结束时间已过抛 ExamFinished。
     *
     * @param project 项目视图（非考试模式直接跳过）
     * @implNote 被 validateProject 调用。
     */
    private void validateExamSetting(ProjectView project) {
        ProjectSetting.ExamSetting examSetting = project.getSetting().getExamSetting();
        if (examSetting == null || !ProjectModeEnum.exam.equals(project.getMode())) {
            return;
        }
        // 校验考试开始时间
        if (examSetting.getStartTime() != null && new Date(examSetting.getStartTime()).compareTo(new Date()) > 0) {
            throw new ErrorCodeException(ErrorCode.ExamUnStarted);
        }
        // 校验考试结束时间
        if (examSetting.getEndTime() != null && new Date(examSetting.getEndTime()).compareTo(new Date()) < 0) {
            throw new ErrorCodeException(ErrorCode.ExamFinished);
        }
    }

    /**
     * 校验问卷并判断是否要更新最近一次的答案（允许修改开关场景）。
     *
     * @param project 项目视图
     * @param request 答卷请求（含答案）
     * @return 最近一次答卷视图（可修改时）；否则 null
     * @implNote 被 saveAnswer 的默认分支调用。
     * - 校验通过且（已登录 + 允许修改答案）：取最近一次答卷用于更新；
     * - 校验抛 SurveySubmitted（已达提交上限）但（已登录 + 允许修改）：
     *   同样允许继续修改（视为"已提交但可改"），其余错误原样抛出。
     */
    private AnswerView validateAndGetLatestAnswer(ProjectView project, AnswerRequest request) {
        ProjectSetting setting = project.getSetting();
        boolean needGetLatest = false;
        try {
            validateProject(project);
            validateAnswer(project, request);
            // 未设时间限制&需要登录&可以修改，永远修改的是同一份
            if (SecurityContextUtils.isAuthenticated() && setting != null
                    && Boolean.TRUE.equals(setting.getSubmittedSetting().getEnableUpdate())) {
                needGetLatest = true;
            }
        } catch (ErrorCodeException e) {
            // 如果设置了时间限制，只能修改某个时间区间内的问卷
            // 登录&问卷已提交&允许修改，则可以继续修改
            if (ErrorCode.SurveySubmitted.equals(e.getErrorCode()) && SecurityContextUtils.isAuthenticated()
                    && setting != null && Boolean.TRUE.equals(setting.getSubmittedSetting().getEnableUpdate())) {
                needGetLatest = true;
            } else {
                throw e;
            }
        }
        // 获取最近一份的问卷执行答案更新操作
        if (needGetLatest) {
            AnswerQuery answerQuery = new AnswerQuery();
            answerQuery.setProjectId(project.getId());
            answerQuery.setLatest(true);
            AnswerView latestAnswer = answerService.getAnswer(answerQuery);
            if (latestAnswer != null) {
                return latestAnswer;
            }
        }
        return null;
    }

    /**
     * 获取最近一次的答案（项目开启"允许修改答案"时回填已答内容）。
     *
     * @param projectView  公开项目视图
     * @param whitelistName 白名单用户姓名（导入用户白名单场景：用它定位答卷人 partner）
     * @return 最近一次答卷的答案 Map；无则 null
     * @implNote 被 loadProject / validateProject(ProjectQuery) 调用。
     * 白名单（未登录）场景按 partner.id 定位答卷（createBy=partner.id），
     * 登录场景按当前用户定位。
     */
    private LinkedHashMap<String, Object> getLatestAnswer(PublicProjectView projectView, String whitelistName) {
        // 打开了答案允许修改开关
        ProjectSetting projectSetting = projectView.getSetting();
        if (projectSetting == null || projectSetting.getSubmittedSetting() == null
                || !Boolean.TRUE.equals(projectSetting.getSubmittedSetting().getEnableUpdate())) {
            return null;
        }
        AnswerQuery answerQuery = new AnswerQuery();
        answerQuery.setProjectId(projectView.getId());
        answerQuery.setLatest(true);
        if (whitelistName != null) {
            ProjectPartner partner = projectPartnerMapper.selectOne(
                    Wrappers.<ProjectPartner>lambdaQuery().eq(ProjectPartner::getProjectId, projectView.getId())
                            .eq(ProjectPartner::getUserName, whitelistName));
            if (partner != null && !SecurityContextUtils.isAuthenticated()) {
                answerQuery.setCreateBy(partner.getId());
            }
        }
        // 通过白名单或者答卷人来获取最近一次的答卷记录
        if (SecurityContextUtils.isAuthenticated() || answerQuery.getCreateBy() != null) {
            return Optional.ofNullable(answerService.getAnswer(answerQuery)).map(x -> x.getAnswer()).orElse(null);
        }
        return null;
    }

    /**
     * 公开查询修改答案：因涉及权限，需要把已存在答案与本次提交答案做 merge
     * （本次未提交的字段保留旧值，避免覆盖其他渠道写入的数据）。
     *
     * @param project 项目视图
     * @param answer  答卷请求（须含 queryId 与 id）
     * @implNote 被 saveAnswer 的"公开查询修改答案"分支调用。
     * 仅当查询配置存在且包含 editable 字段权限时才允许；异常统一抛 QueryResultUpdateError。
     */
    private void validateAndMergeAnswer(ProjectView project, AnswerRequest answer) {
        if (isBlank(answer.getQueryId()) || isBlank(answer.getId())) {
            throw new ErrorCodeException(ErrorCode.QueryResultUpdateError);
        }
        try {
            // 公开查询设置必须存在，并且包含可编辑字段
            ProjectSetting.PublicQuery query = project.getSetting().getSubmittedSetting().getPublicQuery().stream()
                    .filter(x -> x.getId().equals(answer.getQueryId())).findFirst().orElseGet(() -> {
                        ProjectSetting.PublicQuery publicQuery = new ProjectSetting.PublicQuery();
                        publicQuery.setFieldPermission(new LinkedHashMap<>());
                        return publicQuery;
                    });
            if (!query.getFieldPermission().values().contains(FieldPermissionType.editable)) {
                throw new ErrorCodeException(ErrorCode.QueryResultUpdateError);
            }
            AnswerQuery answerQuery = new AnswerQuery();
            answerQuery.setId(answer.getId());
            AnswerView latestAnswer = answerService.getAnswer(answerQuery);
            LinkedHashMap<String, Object> existAnswer = latestAnswer.getAnswer();
            existAnswer.forEach((key, value) -> {
                if (!answer.getAnswer().containsKey(key)) {
                    answer.getAnswer().put(key, value);
                }
            });
        } catch (Exception e) {
            throw new ErrorCodeException(ErrorCode.QueryResultUpdateError);
        }
    }

    /**
     * 登录次数限制校验：统计当前用户在该项目时间窗内（按 cron 计算）的答卷数，
     * 达到上限抛 SurveySubmitted。
     *
     * @param projectId 项目 id
     * @param setting   项目设置（含 answerSetting.loginLimit）
     */
    private void validateLoginLimit(String projectId, ProjectSetting setting) {
        String userId = SecurityContextUtils.getUserId();
        if (userId == null) {
            log.info("user is empty");
            return;
        }
        AnswerQuery query = new AnswerQuery();
        query.setProjectId(projectId);
        query.setCreateBy(userId);
        doValidate(setting, query, setting.getAnswerSetting().getLoginLimit());
    }

    /**
     * Cookie 次数限制校验：首次访问下发限制 Cookie（100 年有效期），
     * 之后每次提交按 Cookie 值统计答卷数。
     *
     * @param projectId 项目 id
     * @param setting   项目设置（含 answerSetting.cookieLimit）
     */
    private void validateCookieLimit(String projectId, ProjectSetting setting) {
        HttpServletRequest request = ContextHelper.getCurrentHttpRequest();

        Cookie limitCookie = WebUtils.getCookie(request, AppConsts.COOKIE_LIMIT_NAME);
        if (limitCookie == null) {
            // 添加 cookie
            HttpServletResponse response = ContextHelper.getCurrentHttpResponse();

            final Cookie cookie = new Cookie(AppConsts.COOKIE_LIMIT_NAME, UUID.randomUUID().toString());
            cookie.setPath("/");
            cookie.setMaxAge(100 * 360 * 24 * 60 * 60);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
            response.addCookie(cookie);
            return;
        }
        AnswerQuery query = new AnswerQuery();
        query.setProjectId(projectId);
        query.setCookie(limitCookie.getValue());
        doValidate(setting, query, setting.getAnswerSetting().getCookieLimit());
    }

    /**
     * IP 次数限制校验：按客户端 IP 统计答卷数。
     *
     * @param projectId 项目 id
     * @param setting   项目设置（含 answerSetting.ipLimit）
     */
    private void validateIpLimit(String projectId, ProjectSetting setting) {
        HttpServletRequest request = ContextHelper.getCurrentHttpRequest();
        String ip = IPUtils.getClientIpAddress(request);
        if (ip == null) {
            log.info("ip is empty");
            return;
        }
        AnswerQuery query = new AnswerQuery();
        query.setProjectId(projectId);
        query.setIp(ip);
        doValidate(setting, query, setting.getAnswerSetting().getIpLimit());
    }

    /**
     * 白名单次数限制校验：白名单校验通过后（request 属性 createBy 已由
     * convertAndValidateLoginFormIfNeeded 设置），按该答卷人统计答卷数。
     *
     * @param projectId 项目 id
     * @param setting   项目设置（含 answerSetting.whitelistLimit）
     */
    private void validateWhitelistLimit(String projectId, ProjectSetting setting) {
        AnswerQuery query = new AnswerQuery();
        query.setProjectId(projectId);
        // 如果白名单校验成功，导入用户获取partner表的id，系统用户是当前登录用户 id
        String createBy = (String) ContextHelper.getCurrentHttpRequest().getAttribute("createBy");
        if (createBy != null) {
            query.setCreateBy(createBy);
            doValidate(setting, query, setting.getAnswerSetting().getWhitelistLimit());
        }
    }

    /**
     * 通用限制校验执行体：按 cron 计算时间窗并统计该时间窗内答卷数，
     * 达到 limitNum 抛 SurveySubmitted；开启"允许修改答案"时直接放行（可覆盖旧答卷）。
     *
     * @param setting     项目设置（含 submittedSetting）
     * @param query       答卷统计条件（已按限制类型预置 projectId + 归属）
     * @param limitSetting 限制配置（cron 频率 + limitNum 上限）
     * @implNote 被 validateLoginLimit/validateCookieLimit/validateIpLimit/validateWhitelistLimit 调用。
     */
    private void doValidate(ProjectSetting setting, AnswerQuery query, ProjectSetting.UniqueLimitSetting limitSetting) {
        // 通过 cron 计算时间窗
        CronHelper helper = new CronHelper(limitSetting.getLimitFreq().getCron());
        Tuple2<LocalDateTime, LocalDateTime> currentWindow = helper.currentWindow();
        if (currentWindow != null) {
            query.setStartTime(Date.from(currentWindow.getFirst().atZone(ZoneId.systemDefault()).toInstant()));
            query.setEndTime(Date.from(currentWindow.getSecond().atZone(ZoneId.systemDefault()).toInstant()));
        }
        long total = answerService.count(query);
        // 允许修改答案的话就获取最近一次的答案，不抛出异常
        if (setting.getSubmittedSetting() != null && Boolean.TRUE.equals(setting.getSubmittedSetting().getEnableUpdate())) {
            return;
        }
        if (limitSetting.getLimitNum() != null && total >= limitSetting.getLimitNum()) {
            throw new ErrorCodeException(ErrorCode.SurveySubmitted);
        }
    }

    /**
     * 获取项目与查询配置并校验查询有效性。
     *
     * @param request 含项目 id（id）与查询配置 id（resultId）
     * @return 二元组（项目视图 + 查询配置）
     * @implNote 被 loadQuery / getQueryResult 调用；查询不存在抛 QueryNotExist，
     * 密码/有效期/开关校验见 validatePublicQuery。
     */
    private Tuple2<ProjectView, ProjectSetting.PublicQuery> getProjectAndQueryThenValidate(PublicQueryRequest request) {
        ProjectView project = projectService.getProject(request.getId());
        List<ProjectSetting.PublicQuery> queries = project.getSetting().getSubmittedSetting().getPublicQuery();
        if (queries == null || queries.size() == 0) {
            throw new ErrorCodeException(ErrorCode.QueryNotExist);
        }
        ProjectSetting.PublicQuery query = queries.stream().filter(x -> x.getId().equals(request.getResultId()))
                .findFirst().orElseThrow(() -> new ErrorCodeException(ErrorCode.QueryNotExist));
        validatePublicQuery(query, request.getAnswer());
        return new Tuple2<>(project, query);
    }

    /**
     * 校验公开查询配置：开关（enabled）、有效期（linkValidityPeriod 时间区间）、
     * 密码（password，匹配后从答案中移除密码字段）。
     *
     * @param query  查询配置
     * @param answer 查询表单答案（含密码字段时可空）
     */
    @SneakyThrows
    private void validatePublicQuery(ProjectSetting.PublicQuery query, LinkedHashMap answer) {
        if (Boolean.FALSE.equals(query.getEnabled())) {
            throw new ErrorCodeException(ErrorCode.QueryDisabled);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<String> linkValidityPeriod = query.getLinkValidityPeriod();
        if (linkValidityPeriod != null && linkValidityPeriod.size() == 2 && !DateUtils.isBetween(new Date(),
                sdf.parse(linkValidityPeriod.get(0)), sdf.parse(linkValidityPeriod.get(1)))) {
            throw new ErrorCodeException(ErrorCode.QueryDisabled);
        }
        // 校验密码
        if (isNotBlank(query.getPassword()) && answer != null) {
            if (!answer.containsKey(AppConsts.PUBLIC_QUERY_PASSWORD_FIELD_ID)) {
                throw new ErrorCodeException(ErrorCode.QueryPasswordError);
            }
            String password = (String) ((Map) answer.get(AppConsts.PUBLIC_QUERY_PASSWORD_FIELD_ID))
                    .get(AppConsts.PUBLIC_QUERY_PASSWORD_FIELD_ID);
            if (isBlank(password) || !query.getPassword().equals(password.trim())) {
                throw new ErrorCodeException(ErrorCode.QueryPasswordError);
            }
            answer.remove(AppConsts.PUBLIC_QUERY_PASSWORD_FIELD_ID);
        }
    }

    /**
     * 动态构建查询表单 schema（前端支持动态主题切换）。
     *
     * @param project 项目视图
     * @param query   查询配置（title/description/conditionQuestion 条件题）
     * @return 查询表单 schema（含密码题则追加一个隐藏的密码输入题）
     * @implNote 被 loadQuery 调用；条件题来自配置中的 #{questionId} 占位符匹配。
     */
    private SurveySchema buildQueryFormSchema(ProjectView project, ProjectSetting.PublicQuery query) {
        SurveySchema schema = SurveySchema.builder().id(query.getId()).title(query.getTitle())
                .description(query.getDescription())
                .children(findMatchChildrenInSchema(query.getConditionQuestion(), project))
                .attribute(SurveySchema.Attribute.builder().submitButton(i18n("survey.query.submit")).build()).build();
        // 目前只支持文本题 #{huaw}#{fhpd}
        if (isNotBlank(query.getPassword())) {
            // 添加一个password的schema，用于密码校验
            SurveySchema passwordSchema = SurveySchema.builder().id(AppConsts.PUBLIC_QUERY_PASSWORD_FIELD_ID)
                    .title(i18n("survey.password.title")).type(SurveySchema.QuestionType.FillBlank)
                    .attribute(SurveySchema.Attribute.builder().required(true).build()).build();
            passwordSchema.setChildren(Collections
                    .singletonList(SurveySchema.builder().id(AppConsts.PUBLIC_QUERY_PASSWORD_FIELD_ID).build()));
            schema.getChildren().add(passwordSchema);
        }
        return schema;
    }

    private List<SurveySchema> findMatchChildrenInSchema(String conditionQuestion, ProjectView project) {
        if (isBlank(conditionQuestion)) {
            return new ArrayList<>();
        }
        Pattern condPattern = Pattern.compile("#\\{(.*?)\\}");
        Matcher m = condPattern.matcher(conditionQuestion);
        List<String> conditionIds = new ArrayList<>();
        while (m.find()) {
            String qId = m.group(1);
            conditionIds.add(qId);
        }
        return SchemaHelper.flatSurveySchema(project.getSurvey()).stream()
                .filter(qSchema -> conditionIds.contains(qSchema.getId())).collect(Collectors.toList());
    }

    /**
     * 根据配置的字段权限信息来过滤要查询的字段
     *
     * @param project
     * @param query
     * @return
     */
    private SurveySchema buildQueryResultSchema(ProjectView project, ProjectSetting.PublicQuery query) {
        SurveySchema schema = project.getSurvey().deepCopy();
        SchemaHelper.updateSchemaByPermission(query.getFieldPermission(), schema);
        if (query.getFieldPermission().values().contains(FieldPermissionType.editable)) {
            schema.setAttribute(SurveySchema.Attribute.builder().submitButton(i18n("survey.edit.submit")).suffix(null).build());
        } else {
            schema.setAttribute(null);
        }
        if (ProjectModeEnum.exam.equals(project.getMode())) {
            schema.getChildren()
                    .add(SurveySchema.builder().id("examScore").title(i18n("survey.score.title")).type(SurveySchema.QuestionType.FillBlank)
                            .attribute(SurveySchema.Attribute.builder().readOnly(true).build())
                            .children(Collections.singletonList(SurveySchema.builder().id("examScore").build()))
                            .build());
        }
        // 公开查询表单需要取消结束、显示隐藏、跳转规则
        SchemaHelper.ignoreAttributes(schema, "finishRule", "visibleRule", "jumpRule");
        return schema;
    }

    /**
     * @param request         提交的请求
     * @param projectAndQuery 项目信息和当前查询信息
     * @return
     */
    private List<Answer> findAnswerByQuery(PublicQueryRequest request,
                                           Tuple2<ProjectView, ProjectSetting.PublicQuery> projectAndQuery) {
        ProjectView projectView = projectAndQuery.getFirst();
        List<SurveySchema> conditionSchemaList = findMatchChildrenInSchema(
                projectAndQuery.getSecond().getConditionQuestion(), projectAndQuery.getFirst());
        if (conditionSchemaList.size() == 0 && request.getQuery().size() == 0) {
            throw new ErrorCodeException(ErrorCode.QueryResultNotExist);
        }
        SchemaHelper.TreeNode treeNode = SchemaHelper.SurveySchema2TreeNode(projectView.getSurvey());

        // 通过 url 参数构建查询表单
        LinkedHashMap<String, Map> queryFormValues = buildFormValuesFromQueryParameter(treeNode, request.getQuery());
        // 将查询表单和url参数构建的查询表单合并
        queryFormValues.putAll(request.getAnswer());

        List<Answer> answer = ((AnswerServiceImpl) answerService)
                .list(Wrappers.<Answer>lambdaQuery().eq(Answer::getProjectId, projectView.getId()).and(i -> {
                    queryFormValues.forEach((qId, qValueObj) -> {
                        i.like(Answer::getAnswer,
                                buildLikeQueryConditionOfQuestion(treeNode.getTreeNodeMap().get(qId), qValueObj));
                    });
                }));
        if (answer.size() == 0) {
            throw new ErrorCodeException(ErrorCode.QueryResultNotExist);
        }
        // 根据配置过滤结果
        return answer;
    }

    /**
     * 通过查询参数里面构建 form values
     *
     * @param query
     * @return
     */
    private LinkedHashMap buildFormValuesFromQueryParameter(SchemaHelper.TreeNode surveySchemaTreeNode,
                                                            Map<String, String> query) {
        LinkedHashMap<String, Map> formValues = new LinkedHashMap<>();
        query.forEach((id, value) -> {
            // 默认为选项
            SchemaHelper.TreeNode findNode = surveySchemaTreeNode.getTreeNodeMap().get(id);
            String questionId = findNode.getParent().getData().getId();
            Map questionValueMap = formValues.computeIfAbsent(questionId, k -> new HashMap<>());
            questionValueMap.put(id, value);
        });
        return formValues;
    }

    /**
     * 通过问题答案手动构建like 查询
     *
     * @param qNode     当前问题的 schema node 节点
     * @param qValueObj 当前问题的答案
     * @return
     */
    private String buildLikeQueryConditionOfQuestion(SchemaHelper.TreeNode qNode, Map qValueObj) {
        SurveySchema optionSchema = qNode.getData().getChildren().get(0);
        String optionId = optionSchema.getId();
        Object optionValue = qValueObj.get(optionId);
        String value = optionValue.toString();
        // 选项非数值类型
        if (optionSchema.getAttribute() == null
                || !SurveySchema.DataType.number.equals(optionSchema.getAttribute().getDataType())) {
            value = "\"" + value + "\"";
        }
        return String.format("{\"%s\":%s}", optionId, value);
    }

    /**
     * 根据字段权限配置过滤结果集，过滤掉隐藏题的答案
     *
     * @param answer
     * @param fieldPermission
     */
    private void filterAnswerByFieldPermission(LinkedHashMap answer, LinkedHashMap<String, Integer> fieldPermission) {
        fieldPermission.entrySet().forEach(entry -> {
            String qId = entry.getKey();
            Integer permission = entry.getValue();
            if (FieldPermissionType.hidden == permission) {
                answer.remove(qId);
            }
        });
    }

    /**
     * 如果问卷需要登录、需要密码答卷、需要白名单答卷进入问卷之前需要弹出验证表单
     *
     * @param project 当前项目
     * @param answer  查询表单的答案
     * @return 返回查询表单的 schema，如果未空，则会直接进入到答卷页面
     */
    private SurveySchema convertAndValidateLoginFormIfNeeded(ProjectView project,
                                                             LinkedHashMap<String, Object> answer) {
        boolean loginRequired = false;
        // 需要更新答题者为已访问的状态
        boolean updatePartnerVisited = false;
        LambdaQueryWrapper<ProjectPartner> projectPartnerQuery = Wrappers.<ProjectPartner>lambdaQuery()
                .eq(ProjectPartner::getProjectId, project.getId());
        Authentication authentication = null;
        List<SurveySchema> queryConditions = new ArrayList<>();
        SurveySchema loginFormSchema = SurveySchema.builder().id(project.getId()).children(queryConditions)
                .title(project.getName()).build();
        // 错题练习模式需要登录
        if (project != null && project.getSetting() != null && project.getSetting().getExamSetting() != null) {
            ProjectSetting.ExamSetting examSetting = project.getSetting().getExamSetting();
            if (Boolean.TRUE.equals(examSetting.getRandomSurveyWrong()) && !SecurityContextUtils.isAuthenticated()) {
                loginRequired = true;
                SchemaHelper.appendChildIfNotExist(loginFormSchema,
                        SchemaHelper.buildFillBlankQuerySchema(SchemaHelper.LoginFormFieldEnum.username));
                SchemaHelper.appendChildIfNotExist(loginFormSchema,
                        SchemaHelper.buildFillBlankQuerySchema(SchemaHelper.LoginFormFieldEnum.password));

                if (answer != null) {
                    authentication = validateUsernameAndPassword(answer);
                }
            }
        }
        if (project != null && project.getSetting() != null && project.getSetting().getAnswerSetting() != null) {
            ProjectSetting.AnswerSetting answerSetting = project.getSetting().getAnswerSetting();
            // 需要登录答卷
            if (Boolean.TRUE.equals(answerSetting.getLoginRequired()) && !SecurityContextUtils.isAuthenticated()) {
                loginRequired = true;
                SchemaHelper.appendChildIfNotExist(loginFormSchema,
                        SchemaHelper.buildFillBlankQuerySchema(SchemaHelper.LoginFormFieldEnum.username));
                SchemaHelper.appendChildIfNotExist(loginFormSchema,
                        SchemaHelper.buildFillBlankQuerySchema(SchemaHelper.LoginFormFieldEnum.password));

                if (answer != null) {
                    authentication = validateUsernameAndPassword(answer);
                }
            }
            // 需要密码答卷
            if (answerSetting.getPassword() != null) {
                loginRequired = true;
                queryConditions
                        .add(SchemaHelper.buildFillBlankQuerySchema(SchemaHelper.LoginFormFieldEnum.extraPassword));

                if (answer != null) {
                    if (!answerSetting.getPassword().equals(
                            SchemaHelper.getLoginFormAnswer(answer, SchemaHelper.LoginFormFieldEnum.extraPassword))) {
                        throw new ErrorCodeException(ErrorCode.ValidationError);
                    }
                }
            }
            // 白名单为系统用户
            if (answerSetting.getWhitelistType() != null
                    && ProjectPartnerTypeEnum.RESPONDENT_SYS_USER.getType() == answerSetting.getWhitelistType()) {
                // 如果当前已登录且当前用户不在白名单内，则报错
                if (SecurityContextUtils.isAuthenticated()) {
                    boolean currentHasPerm = projectPartnerMapper.selectCount(
                            Wrappers.<ProjectPartner>lambdaQuery().eq(ProjectPartner::getProjectId, project.getId())
                                    .eq(ProjectPartner::getType, ProjectPartnerTypeEnum.RESPONDENT_SYS_USER.getType())
                                    .eq(ProjectPartner::getUserId, SecurityContextUtils.getUserId())) == 1;
                    if (!currentHasPerm) {
                        throw new ErrorCodeException(ErrorCode.PermVerifyFailed);
                    }
                    updatePartnerVisited = true;
                    projectPartnerQuery.eq(ProjectPartner::getUserId, SecurityContextUtils.getUserId())
                            .eq(ProjectPartner::getType, ProjectPartnerTypeEnum.RESPONDENT_SYS_USER.getType());
                } else {
                    // 需要执行登录操作
                    SchemaHelper.appendChildIfNotExist(loginFormSchema,
                            SchemaHelper.buildFillBlankQuerySchema(SchemaHelper.LoginFormFieldEnum.username));
                    SchemaHelper.appendChildIfNotExist(loginFormSchema,
                            SchemaHelper.buildFillBlankQuerySchema(SchemaHelper.LoginFormFieldEnum.password));
                    loginRequired = true;

                    if (answer != null) {
                        if (authentication == null) {
                            authentication = validateUsernameAndPassword(answer);
                        }
                        // 执行登录操作
                        UserInfo user = (UserInfo) authentication.getPrincipal();
                        // 判断登录用户是否在白名单里面
                        /**
                         * todo 这里应该加上用户类型，否则会出查询出来多条
                         * 多条原因：
                         * 新创建的问卷的时候，默认的插入一条名单数据，type = 1
                         * 修改答题白名单设置为系统用户时，又插入一条，type = 3
                         * 修改答题白名单设置为外部用户时，又插入一条，type = 4，当然这条数据没有用户id，在这里不会被查出来
                         * 综上，这里至少会查询出2条，所以需要加入用户类型限制，或者修改答题白名单时移除旧数据
                         * 这里验证的是"白名单为系统用户"，所以type = 3
                         */
                        boolean currentHasPerm = projectPartnerMapper.selectCount(
                                Wrappers.<ProjectPartner>lambdaQuery().eq(ProjectPartner::getProjectId, project.getId())
                                        .eq(ProjectPartner::getType, ProjectPartnerTypeEnum.RESPONDENT_SYS_USER.getType())
                                        .eq(ProjectPartner::getUserId, user.getUserId())) == 1;
                        if (!currentHasPerm) {
                            throw new ErrorCodeException(ErrorCode.PermVerifyFailed);
                        }

                        updatePartnerVisited = true;
                        projectPartnerQuery.eq(ProjectPartner::getUserId, user.getUserId()).eq(ProjectPartner::getType,
                                ProjectPartnerTypeEnum.RESPONDENT_SYS_USER.getType());
                    }
                }
            }
            // 白名单为导入用户
            if (answerSetting.getWhitelistType() != null
                    && ProjectPartnerTypeEnum.RESPONDENT_IMP_USER.getType() == answerSetting.getWhitelistType()) {
                SchemaHelper.appendChildIfNotExist(loginFormSchema,
                        SchemaHelper.buildFillBlankQuerySchema(SchemaHelper.LoginFormFieldEnum.whitelistName));
                loginRequired = true;

                if (answer != null) {
                    // 校验登录白名单名称是否在白名单列表里面
                    String whitelistName = (String) SchemaHelper.getLoginFormAnswer(answer,
                            SchemaHelper.LoginFormFieldEnum.whitelistName);
                    if (whitelistName == null) {
                        throw new ErrorCodeException(ErrorCode.PermVerifyFailed);
                    }
                    boolean currentHasPerm = projectPartnerMapper.selectCount(
                            Wrappers.<ProjectPartner>lambdaQuery().eq(ProjectPartner::getProjectId, project.getId())
                                    .eq(ProjectPartner::getUserName, whitelistName)) == 1;
                    if (!currentHasPerm) {
                        throw new ErrorCodeException(ErrorCode.PermVerifyFailed);
                    }

                    updatePartnerVisited = true;
                    projectPartnerQuery.eq(ProjectPartner::getUserName, whitelistName).eq(ProjectPartner::getType,
                            ProjectPartnerTypeEnum.RESPONDENT_IMP_USER.getType());
                }
            }

        }

        if (authentication != null) {
            // 执行登录操作，方便后续保存答案的时候获取认证信息
            UserInfo user = (UserInfo) authentication.getPrincipal();
            HttpCookie cookie = ResponseCookie
                    .from(AppConsts.TOKEN_NAME, jwtTokenUtil.generateAccessToken(new UserTokenView(user.getUserId())))
                    .path("/").httpOnly(true).build();
            ContextHelper.getCurrentHttpResponse().setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        // 更新问卷状态为已访问
        if (updatePartnerVisited) {
            ProjectPartner projectPartner = projectPartnerMapper.selectOne(projectPartnerQuery);
            if (projectPartner != null && projectPartner.getStatus() == AppConsts.ProjectPartnerStatus.UNVISITED) {
                projectPartner.setStatus(AppConsts.ProjectPartnerStatus.VISITED);
                projectPartnerMapper.updateById(projectPartner);
            }
            // 如果配置的外部用户，则 createBy 为 partner 的 id，如果是内部用户则是用户 id
            if (projectPartner.getUserName() != null) {
                ContextHelper.getCurrentHttpRequest().setAttribute("createBy", projectPartner.getId());
            } else if (projectPartner.getUserId() != null) {
                ContextHelper.getCurrentHttpRequest().setAttribute("createBy", SecurityContextUtils.getUserId());
            }
        }

        if (loginRequired) {
            return loginFormSchema;
        }

        return null;
    }

    /**
     * 答卷前登录校验用户名和密码
     *
     * @param answer
     * @return
     */
    private Authentication validateUsernameAndPassword(LinkedHashMap<String, Object> answer) {
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    SchemaHelper.getLoginFormAnswer(answer, SchemaHelper.LoginFormFieldEnum.username),
                    SchemaHelper.getLoginFormAnswer(answer, SchemaHelper.LoginFormFieldEnum.password)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return authentication;
        } catch (Exception e) {
            throw new ErrorCodeException(ErrorCode.ValidationError);
        }
    }

    /**
     * 如果存在白名单，答卷完更新白名单为已访问的状态，由于导入用户白名单没有系统用户id，需要将答案的创建人更新为答题参与表的id。
     *
     * @param request
     * @param project
     */
	private void updateProjectPartnerByAnswer(AnswerRequest request, ProjectView project) {
		if (project.getSetting() != null && project.getSetting().getAnswerSetting() != null) {
			Integer whitelistType = project.getSetting().getAnswerSetting().getWhitelistType();
            if (whitelistType == null) {
                return;
            }

            LambdaQueryWrapper<ProjectPartner> queryWrapper = Wrappers.<ProjectPartner>lambdaQuery()
                    .eq(ProjectPartner::getProjectId, project.getId());
            if (ProjectPartnerTypeEnum.RESPONDENT_SYS_USER.getType() == whitelistType) {
                queryWrapper.eq(ProjectPartner::getUserId, SecurityContextUtils.getUserId()).eq(ProjectPartner::getType,
                        ProjectPartnerTypeEnum.RESPONDENT_SYS_USER.getType());
                if (!SecurityContextUtils.isAuthenticated()) {
                    return;
                }
            } else if (ProjectPartnerTypeEnum.RESPONDENT_IMP_USER.getType() == whitelistType) {
                queryWrapper.eq(ProjectPartner::getUserName, request.getWhitelistName()).eq(ProjectPartner::getType,
                        ProjectPartnerTypeEnum.RESPONDENT_IMP_USER.getType());
            }

            ProjectPartner projectPartner = projectPartnerMapper.selectOne(queryWrapper);
            projectPartner.setStatus(AppConsts.ProjectPartnerStatus.ANSWERED);
            projectPartnerMapper.updateById(projectPartner);

            // 白名单导入用户答题需要更新答案表的 createBy 为 partner 的 id
            if (ProjectPartnerTypeEnum.RESPONDENT_IMP_USER.getType() == whitelistType) {
                AnswerRequest answerUpdateRequest = new AnswerRequest();
                answerUpdateRequest.setId(request.getId());
                answerUpdateRequest.setCreateBy(projectPartner.getId());
                answerUpdateRequest.setProjectId(request.getProjectId());
                answerService.updateAnswer(answerUpdateRequest);
            }

		}
	}

	private String i18n(String key, Object... args) {
		return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
	}

}
