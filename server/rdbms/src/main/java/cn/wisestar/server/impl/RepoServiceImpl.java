package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.TagCategoryEnum;
import cn.wisestar.server.core.uitls.AnswerScoreEvaluator;
import cn.wisestar.server.core.uitls.RepoTemplateExcelParseHelper;
import cn.wisestar.server.core.uitls.RepoTemplateI18n;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.core.uitls.ExcelExporter;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.domain.mapper.RepoViewMapper;
import cn.wisestar.server.domain.mapper.UserBookViewMapper;
import cn.wisestar.server.domain.model.*;
import cn.wisestar.server.mapper.RepoMapper;
import cn.wisestar.server.service.AnswerService;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.RepoService;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Arrays;

import static cn.wisestar.server.impl.UserBookServiceImpl.BOOK_TYPE_WRONG;
import static com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank;

/**
 * 题库（Repo）业务实现：题库 CRUD、题目批量管理、随机抽题、错题本与题库导出。
 *
 * 【类职责】
 * 1. 题库 CRUD：listRepo 分页查询、addRepo/updateRepo/deleteRepo、selectRepo 选择器
 * 2. 题库-题目批量管理：batchAddRepoTemplate（Excel 导入/批量保存，按"序号+题型"幂等更新）、
 *    batchUnBindTemplate 解绑题目
 * 3. 随机抽题：pickQuestionFromRepo（按题库/题型/标签条件随机选题，供考试随机抽题与练习使用）
 * 4. 题库导出增强：exportRepoQuestions（按题型分 sheet 导出 Excel，含知识点/正确答案/分值/
 *    解析/标签），配套辅助方法 questionTypeLabel / repoNameOf / knowledgePointText /
 *    extractCorrectAnswer
 * 5. 错题本：listUserBook/createUserBook/updateUserBook/deleteUserBook
 *
 * 【被谁调用】
 * - Controller：RepoController（题库管理/导出/错题本接口）
 * - 业务层：SurveyServiceImpl（题库练习加载）、RandomSurveyProcessor（随机抽题）、
 *   TemplateServiceImpl.selectTemplate（间接取题库列表）、AnswerServiceImpl（无）
 *
 * 【依赖什么】
 * - RepoMapper（BaseMapper + selectRepoTemplateTags/selectRepoQuestionTypes 自定义统计）、
 *   TemplateServiceImpl（题目 CRUD）、TagServiceImpl（标签）、UserBookServiceImpl（错题本）、
 *   AnswerServiceImpl（错题练习时保存临时答案）
 * - RepoViewMapper / UserBookViewMapper（MapStruct 转换）、RepoTemplateExcelParseHelper（导入解析）、
 *   ExcelExporter 对应物（导出用 fastexcel 直接写）、AnswerScoreEvaluator（错题判分）
 *
 * 【核心数据流】
 * 管理端维护题库 → RepoController → RepoServiceImpl → RepoMapper（t_repo）+ TemplateServiceImpl
 * （t_template）+ TagServiceImpl（t_tag）→ 列表回填各题统计；导出时按题型分组写入 xlsx 流。
 *
 * @author javahuang
 * @date 2022/4/27
 */
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class RepoServiceImpl extends BaseService<RepoMapper, Repo> implements RepoService {

    /**
     * 题目模板服务：题库下的题目 CRUD 与统计都委托给它（本类与 TemplateServiceImpl
     * 通过 Spring 上下文互相依赖，注意循环依赖处理）。
     */
    private final TemplateServiceImpl templateService;

    /**
     * MapStruct 转换器：Repo ↔ RepoView ↔ RepoRequest。
     */
    private final RepoViewMapper repoViewMapper;

    /**
     * 标签服务：批量添加/删除题库标签。
     */
    private final TagServiceImpl tagService;

    /**
     * 错题本服务：用户错题记录的增删改查。
     */
    private final UserBookServiceImpl userBookService;

    /**
     * MapStruct 转换器：UserBook ↔ UserBookView ↔ UserBookRequest。
     */
    private final UserBookViewMapper userBookViewMapper;

    /**
     * 答卷服务：错题练习时保存/更新临时答案与每题得分。
     */
    private final AnswerServiceImpl answerService;

    /**
     * 学员-题库分配服务：练习题库手动分配记录的增删查（t_user_repo）。
     */
    private final UserRepoServiceImpl userRepoService;

    /**
     * 用户服务：分配记录列表回填学员姓名时查询用户信息。
     */
    private final UserServiceImpl userService;

    /**
     * 分页查询题库列表。
     *
     * @param query 条件：name 模糊、category、isPractice、mode、subject、grade、difficulty；
     *              权限范围：自己创建 OR shared=true（共享题库）
     * @return 分页的 RepoView（每项回填题目总数 total、各标签题数 templateTags、
     *         各题型题数 repoQuestionTypes）
     * @implNote 调用链：RepoController.listRepo → listRepo → RepoMapper 分页 +
     * TemplateServiceImpl.count（题目总数）+ RepoMapper.selectRepoTemplateTags /
     * selectRepoQuestionTypes（自定义 SQL 统计）。
     */
    @Override
    public PaginationResponse<RepoView> listRepo(RepoQuery query) {
        Page<Repo> page = pageByQuery(query,
                Wrappers.<Repo>lambdaQuery().like(isNotBlank(query.getName()), Repo::getName, query.getName())
                        .eq(StringUtils.hasText(query.getCategory()), Repo::getCategory, query.getCategory())
                        .and(x -> x.eq(Repo::getCreateBy, SecurityContextUtils.getUserId())
                                .or(y -> y.eq(Repo::getShared, true)))
                        .eq(query.getIsPractice() != null, Repo::getIsPractice, query.getIsPractice())
                        .eq(query.getMode() != null, Repo::getMode, query.getMode())
                        .eq(StringUtils.hasText(query.getSubject()), Repo::getSubject, query.getSubject())
                        .eq(StringUtils.hasText(query.getGrade()), Repo::getGrade, query.getGrade())
                        .eq(StringUtils.hasText(query.getDifficulty()), Repo::getDifficulty, query.getDifficulty())
                        .orderByAsc(Repo::getCreateAt));
        PaginationResponse<RepoView> result = new PaginationResponse<>(page.getTotal(),
                repoViewMapper.toView(page.getRecords()));
        result.getList().forEach(repoView -> {
            repoView.setTotal(
                    templateService.count(Wrappers.<Template>lambdaQuery().eq(Template::getRepoId, repoView.getId())));
            // 获取每个标签对应的题的数量
            repoView.setTemplateTags(this.getBaseMapper().selectRepoTemplateTags(repoView.getId()));
            // 获取每个问题类型对应的题的数量
            repoView.setRepoQuestionTypes(this.getBaseMapper().selectRepoQuestionTypes(repoView.getId()));
        });
        return result;
    }

    /**
     * 查询单个题库详情。
     *
     * @param id 题库 ID
     * @return 题库视图；不存在返回 null
     * @implNote 被 RepoController.getRpo / SurveyServiceImpl.loadProject（题库练习加载题目）调用。
     */
    @Override
    public RepoView getRpo(String id) {
        return repoViewMapper.toView(getById(id));
    }

    /**
     * 新增题库：先生成雪花 ID，再保存题库与标签。
     *
     * @param request 题库请求（name、mode、category、tag 等）
     * @implNote 调用链：RepoController.addRepo → addRepo → save(t_repo) +
     * tagService.batchAddTag（t_tag 关联 entity_id=repoId）。
     */
    @Override
    public void addRepo(RepoRequest request) {
        request.setId(IdWorker.getIdStr());
        save(repoViewMapper.fromRequest(request));
        tagService.batchAddTag(request.getId(), TagCategoryEnum.repo, request.getTag());
    }

    /**
     * 更新题库（含标签全量重置：先删旧标签再批量新增）。
     *
     * @param request 题库请求（须含 id）
     */
    @Override
    public void updateRepo(RepoRequest request) {
        updateById(repoViewMapper.fromRequest(request));
        tagService.batchAddTag(request.getId(), TagCategoryEnum.repo, request.getTag());
    }

    /**
     * 删除题库：级联删除该题库下所有题目（t_template）与题库标签（t_tag）。
     *
     * @param request 含 id
     */
    @Override
    public void deleteRepo(RepoRequest request) {
        String id = request.getId();
        removeById(id);
        // 删除题库下面的所有题
        templateService.remove(Wrappers.<Template>lambdaUpdate().eq(Template::getRepoId, id));
        // 取消题库标签
        tagService.remove(Wrappers.<Tag>lambdaUpdate().eq(Tag::getEntityId, id));
    }

    /**
     * 批量添加/更新题库下的题目（Excel 导入或页面批量操作）。
     *
     * 【内部逻辑步骤】
     * 1. 加载该题库现有题目列表（按 repoId）；
     * 2. 遍历请求中的每道题：以"序号 serialNo + 题型 questionType"为匹配键在现有题目中查找，
     *    命中则复用其 id（视为更新，进 templatesUpdate），否则生成新 id（进 templatesAdd）；
     * 3. 冗余同步：把 template.tags 同步到模板顶层 tag 数组；收集所有标签为 Tag 实体待批量入库；
     * 4. questionType 统一取 template.type；
     * 5. 新增批量走 templateService.batchAddTemplate（保存 + 关联 repoId）；
     *    更新批量走 batchUpdateTemplate（更新前删除这些题目的旧标签）；
     * 6. 最后批量插入收集好的题目标签。
     *
     * 【为什么这么写】
     * - 以"序号+题型"而非 id 做匹配，是为了 Excel 反复导入同一题库时能幂等更新，
     *   避免同一道题重复落库；
     * - 标签双写（模板 tag 列 + t_tag 表）是为了标签筛选走 exists 子查询（t_tag 支持
     *   高效 IN 匹配），列表展示直接用 tag 列，各取所长。
     *
     * @param request 含 repoId 与模板列表（templates）
     * @implNote 被 RepoController.batchAddRepoTemplate / importFromTemplate 调用。
     */
    @Override
    public void batchAddRepoTemplate(RepoTemplateRequest request) {
        List<Tag> tagList = new ArrayList<>();
        List<TemplateRequest> templatesAdd = new ArrayList<>();
        List<TemplateRequest> templatesUpdate = new ArrayList<>();
        List<Template> templateListOfCurrentRepo = templateService.list(Wrappers.<Template>lambdaQuery()
                .eq(Template::getRepoId, request.getRepoId()));

        request.getTemplates().forEach(template -> {
            // 根据序号更新更新题库
            templateListOfCurrentRepo.stream().filter(t -> StringUtils.hasText(t.getSerialNo()) &&
                    t.getSerialNo().equals(template.getSerialNo())
                    && t.getQuestionType() == template.getQuestionType()).findFirst().ifPresent(t -> {
                        template.setId(t.getId());
                    });

            if (template.getId() == null) {
                template.setId(IdWorker.getIdStr());
                templatesAdd.add(template);
            } else {
                templatesUpdate.add(template);
            }
            // template 里面的 tags 冗余了
            if (template.getTemplate().getTags() != null) {
                template.setTag(template.getTemplate().getTags().toArray(new String[0]));
            }

            List<String> tags = template.getTemplate().getTags();
            if (tags != null && tags.size() > 0) {
                tags.forEach(tagStr -> {
                    Tag tag = new Tag();
                    tag.setName(tagStr);
                    tag.setEntityId(template.getId());
                    tag.setCategory(TagCategoryEnum.template.name());
                    tagList.add(tag);
                });
            }
            template.setQuestionType(template.getTemplate().getType());
        });

        if (!templatesAdd.isEmpty()) {
            // 添加模板的时候，需要添加题库与模板的关联关系
            templatesAdd.forEach(x -> x.setRepoId(request.getRepoId()));
            templateService.batchAddTemplate(templatesAdd);
        }
        if (!templatesUpdate.isEmpty()) {
            templatesUpdate.forEach(x -> x.setRepoId(request.getRepoId()));
            templateService.batchUpdateTemplate(templatesUpdate);
            // 更新模板时需要删除之前的标签
            tagService.remove(Wrappers.<Tag>lambdaUpdate().in(Tag::getEntityId,
                    templatesUpdate.stream().map(x -> x.getId()).collect(Collectors.toList())));
        }

        // 添加模板问题标签
        if (!tagList.isEmpty()) {
            tagService.saveBatch(tagList);
        }
    }

    /**
     * 批量解绑题库下的题目（题目从题库移除，保留在题目管理全局库中）。
     *
     * @param request 含 ids 题目 ID 列表
     * @implNote 被 RepoController.batchUnBindTemplate 调用，仅清空题目 repoId，
     * 不删除 t_template 记录（与接口注释"移除关联、不删除模板本身"语义一致）。
     */
    @Override
    public void batchUnBindTemplate(RepoTemplateRequest request) {
        if (request.getIds() != null) {
            templateService.lambdaUpdate()
                    .in(Template::getId, request.getIds())
                    .set(Template::getRepoId, null)
                    .update();
        }
    }

    /**
     * 批量绑定已有题目到题库（题目管理中的题目 → 指定题库）。
     *
     * <p>仅更新题目归属字段 repoId，不改动题目内容（名称/题型/答案/解析等）。
     * 已在目标题库的题目（repoId 已等于目标值）自动跳过，保证幂等。</p>
     *
     * @param request 含 repoId（目标题库 id）+ ids（题目 ID 列表）
     * @implNote 被 RepoController.bindTemplates 调用，供题库详情页"批量选择题目"使用。
     */
    @Override
    public void bindTemplates(RepoTemplateRequest request) {
        if (request.getRepoId() == null || CollectionUtils.isEmpty(request.getIds())) {
            return;
        }
        templateService.lambdaUpdate()
                .in(Template::getId, request.getIds())
                .ne(Template::getRepoId, request.getRepoId())
                .set(Template::getRepoId, request.getRepoId())
                .update();
    }

    /**
     * 从题库中随机挑选题目（考试随机抽题/练习出卷核心）。
     *
     * 【内部逻辑步骤】
     * 1. 遍历每个抽题条件（RandomSurveyCondition）：按 repoId + 题型（types 可选）+
     *    标签（tags 可选，t_tag exists 子查询）筛选题目；
     * 2. 若配置了 questionsNum：Collections.shuffle 打乱后截取前 N 题；
     * 3. 给选中题目附加分值：配置了 examScore 时写入题目 attribute.examScore（无 attribute
     *    则先创建），同一题目在多条件中重复命中时只保留第一个（去重）；
     * 4. 把 Template 转为 SurveySchema（id 用模板 id，保证答案回填能对上题目），
     *    按题型排序返回（相同题型排在一起，便于前端分组展示）。
     *
     * 【为什么这么写】
     * - 随机性用 shuffle 而非数据库 RAND()：题目总量可控，内存打乱更稳定可控；
     * - schema.id 复用模板 id：提交答案时 questionId 即模板 id，明细/计分能回源到原题。
     *
     * @param repos 抽题条件列表（每个题库一个条件）
     * @return 组装好的题目 schema 列表（按题型排序）
     * @implNote 被 RandomSurveyProcessor.processSingleRandomSurvey / processRandomQuestionSelection
     * 调用。
     */
    @Override
    public List<SurveySchema> pickQuestionFromRepo(List<ProjectSetting.RandomSurveyCondition> repos) {
        List<Template> templates = new ArrayList<>();
        repos.forEach(repo -> {
            List<Template> repoTemplates = templateService.list(Wrappers.<Template>lambdaQuery()
                    .eq(Template::getRepoId, repo.getRepoId())
                    .in(!CollectionUtils.isEmpty(repo.getTypes()), Template::getQuestionType, repo.getTypes())
                    .exists(!CollectionUtils.isEmpty(repo.getTags()),
                            String.format("select 1 from t_tag t where t.entity_id = t_template.id and t.name in (%s)",
                                    Optional.ofNullable(repo.getTags()).orElse(new ArrayList<>()).stream()
                                            .map(x -> "'" + x + "'").collect(Collectors.joining(",")))));
            if (repo.getQuestionsNum() != null) {
                // 随机从问题里面挑选指定数量的题
                Collections.shuffle(repoTemplates);
                if (repoTemplates.size() > repo.getQuestionsNum()) {
                    repoTemplates = repoTemplates.subList(0, repo.getQuestionsNum());
                }
            }

            // 给问题添加分值
            repoTemplates.forEach(template -> {
                if (templates.stream().filter(x -> x.getId().equals(template.getId())).findFirst().isPresent()) {
                    return;
                }
                if (repo.getExamScore() != null) {
                    if (template.getTemplate().getAttribute() == null) {
                        template.getTemplate().setAttribute(new SurveySchema.Attribute());
                    }
                    template.getTemplate().getAttribute().setExamScore(repo.getExamScore());
                }
                templates.add(template);
            });
        });
        // 相同类型的问题排放在一起
        return templates.stream().map(x -> {
            SurveySchema schema = x.getTemplate();
            schema.setId(x.getId());
            return schema;
        }).sorted(Comparator.comparing(SurveySchema::getType)).collect(Collectors.toList());
    }

    /**
     * 从 Excel 文件导入题目（先解析再批量入库）。
     *
     * @param request 含上传文件（file）与目标题库（repoId）
     * @implNote 调用链：RepoController.importFromTemplate → importFromTemplate →
     * parseExcelToTemplate（RepoTemplateExcelParseHelper 解析）→ batchAddRepoTemplate 落库。
     */
    @Override
    @SneakyThrows
    public void importFromTemplate(RepoTemplateRequest request) {
        request.setTemplates(parseExcelToTemplate(request.getFile()));
        batchAddRepoTemplate(request);
    }

    /**
     * 解析题库导入 Excel 为模板请求列表。
     *
     * @param file 上传的 xlsx 文件
     * @return 模板请求列表
     * @implNote 委托 RepoTemplateExcelParseHelper 解析（支持多题型 sheet）。
     */
    @SneakyThrows
    private List<TemplateRequest> parseExcelToTemplate(MultipartFile file) {
        return new RepoTemplateExcelParseHelper(file).parse();
    }

    /**
     * 查询当前登录学员「我的题库」。
     *
     * <p><b>分配来源</b>（并集去重）：</p>
     * <ol>
     *   <li>老师手动分配：t_user_repo 中 user_id=当前用户 的题库</li>
     *   <li>系统按标签自动分配：题库 tag 与学员标签（t_tag, category=user）有交集</li>
     * </ol>
     *
     * @return 可练习题库列表（每项回填题目总数 total）
     * @implNote 调用链：RepoController.myRepos → myRepos → UserRepoMapper 手动分配 +
     * TagServiceImpl.list 学员标签 → 内存匹配 Repo.tag → RepoView 回填题目总数。
     */
    @Override
    public List<RepoView> myRepos() {
        String userId = SecurityContextUtils.getUserId();
        // 1. 老师手动分配的题库
        Set<String> repoIds = new HashSet<>();
        List<UserRepo> assigns = userRepoService.list(
                Wrappers.<UserRepo>lambdaQuery().eq(UserRepo::getUserId, userId));
        assigns.forEach(a -> repoIds.add(a.getRepoId()));

        // 2. 系统按标签自动分配：题库 tag ∩ 学员标签 ≠ ∅
        Set<String> userTags = getUserTags(userId);
        if (!userTags.isEmpty()) {
            List<Repo> autoRepos = list();
            autoRepos.stream()
                    .filter(r -> r.getTag() != null && r.getTag().length > 0)
                    .filter(r -> Arrays.stream(r.getTag()).anyMatch(userTags::contains))
                    .forEach(r -> repoIds.add(r.getId()));
        }
        if (repoIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<RepoView> result = repoViewMapper.toView(listByIds(repoIds));
        result.forEach(repoView -> repoView.setTotal(
                templateService.count(Wrappers.<Template>lambdaQuery().eq(Template::getRepoId, repoView.getId()))));
        return result;
    }

    /**
     * 老师手动分配题库给学员（批量，幂等）。
     *
     * @param userId  学员用户 ID
     * @param repoIds 题库 ID 列表
     * @implNote 已分配过的题库自动跳过，避免重复记录。
     */
    @Override
    public void assignRepo(String userId, List<String> repoIds) {
        if (!StringUtils.hasText(userId) || CollectionUtils.isEmpty(repoIds)) {
            return;
        }
        Set<String> existRepoIds = userRepoService.list(
                Wrappers.<UserRepo>lambdaQuery().eq(UserRepo::getUserId, userId))
                .stream().map(UserRepo::getRepoId).collect(Collectors.toSet());
        repoIds.stream().filter(repoId -> !existRepoIds.contains(repoId)).forEach(repoId -> {
            UserRepo assign = new UserRepo();
            assign.setId(IdWorker.getIdStr());
            assign.setUserId(userId);
            assign.setRepoId(repoId);
            assign.setAssignType("manual");
            userRepoService.save(assign);
        });
    }

    /**
     * 删除分配记录（批量，逻辑删除）。
     *
     * @param ids 分配记录 ID 列表
     */
    @Override
    public void deleteAssign(List<String> ids) {
        if (CollectionUtils.isNotEmpty(ids)) {
            userRepoService.removeByIds(ids);
        }
    }

    /**
     * 查询某学员的分配记录（管理端展示）。
     *
     * @param userId 学员用户 ID
     * @return 分配记录列表（含学员姓名/题库名称）
     */
    @Override
    public List<RepoAssignView> listAssign(String userId) {
        List<UserRepo> list = userRepoService.list(Wrappers.<UserRepo>lambdaQuery()
                .eq(StringUtils.hasText(userId), UserRepo::getUserId, userId)
                .orderByDesc(UserRepo::getCreateAt));
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> userIds = list.stream().map(UserRepo::getUserId).collect(Collectors.toSet());
        Set<String> repoIds = list.stream().map(UserRepo::getRepoId).collect(Collectors.toSet());
        Map<String, User> userMap = new HashMap<>();
        Map<String, Repo> repoMap = new HashMap<>();
        userService.listByIds(userIds).forEach(r -> userMap.put(r.getId(), r));
        listByIds(repoIds).forEach(r -> repoMap.put(r.getId(), r));
        return list.stream().map(assign -> {
            RepoAssignView view = new RepoAssignView();
            view.setId(assign.getId());
            view.setUserId(assign.getUserId());
            view.setRepoId(assign.getRepoId());
            view.setAssignType(assign.getAssignType());
            view.setCreateAt(assign.getCreateAt());
            User user = userMap.get(assign.getUserId());
            if (user != null) {
                view.setUserName(user.getName());
            }
            Repo repo = repoMap.get(assign.getRepoId());
            if (repo != null) {
                view.setRepoName(repo.getName());
            }
            return view;
        }).collect(Collectors.toList());
    }

    /**
     * 查询学员标签（t_tag, category=user）。
     *
     * @param userId 学员用户 ID
     * @return 学员标签集合
     */
    @Override
    public Set<String> getUserTags(String userId) {
        if (!StringUtils.hasText(userId)) {
            return new HashSet<>();
        }
        return tagService.list(Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getEntityId, userId)
                .eq(Tag::getCategory, TagCategoryEnum.user.name()))
                .stream().map(Tag::getName).collect(Collectors.toSet());
    }

    /**
     * 保存学员标签（覆盖式，category=user），用于按标签自动分配题库。
     *
     * @param userId 学员用户 ID
     * @param tags   标签数组（为空则清除全部）
     */
    @Override
    public void saveUserTags(String userId, String[] tags) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        tagService.batchAddTag(userId, TagCategoryEnum.user, tags);
    }

    /**
     * 分页查询当前用户的错题本。
     *
     * @param query 条件：name 模糊、type、创建时间区间；仅查本人（createBy=当前用户）
     * @return 分页的 UserBookView
     * @implNote 调用链：RepoController.listUserBook → listUserBook → UserBookMapper 分页。
     */
    @Override
    public PaginationResponse<UserBookView> listUserBook(UserBookQuery query) {
        Page<UserBook> page = userBookService.pageByQuery(query,
                Wrappers.<UserBook>lambdaQuery().like(query.getName() != null, UserBook::getName, query.getName())
                        .eq(query.getType() != null, UserBook::getType, query.getType())
                        .ge(query.getStartDate() != null, UserBook::getCreateAt, query.getStartDate())
                        .le(query.getEndDate() != null, UserBook::getCreateAt, query.getEndDate())
                        .eq(UserBook::getCreateBy, SecurityContextUtils.getUserId()));
        PaginationResponse<UserBookView> result = new PaginationResponse<>(page.getTotal(),
                userBookViewMapper.toView(page.getRecords()));
        return result;
    }

    /**
     * 创建错题本记录（加入错题本）。
     *
     * @param request 错题本请求（templateId 题目、repoId、type 等）
     * @implNote 调用链：RepoController.createUserBook。直接保存一条 UserBook 记录。
     */
    @Override
    public void createUserBook(UserBookRequest request) {
        UserBook userBook = userBookViewMapper.fromRequest(request);
        userBookService.save(userBook);
    }

    /**
     * 更新错题本（错题练习判分核心）：作答后记录答对/答错次数、连续答对自动移出错题本，
     * 并同步保存练习答卷的临时答案与每题得分。
     *
     * 【内部逻辑步骤】
     * 1. 若 userBook.id 为空：按 templateId + 当前用户查找已有记录，命中则复用 id（幂等）；
     * 2. 若请求带 answer（本次作答）：从模板加载题目，强制设置每题分值 5 分，
     *    用 AnswerScoreEvaluator 判分得到 qScore：
     *    - qScore > 0（答对）：correctTimes+1；若用户配置的"连续答对移出错题数"
     *      （userInfo.correctTimes）>= 当前正确次数，则 wrongTimes 置 0（移出错题本）；
     *    - qScore = 0（答错）：wrongTimes+1，correctTimes 清零（做错一次重置连续正确数）；
     * 3. 若带 answerId（练习答卷）：把本次答案合并进答卷 tempAnswer，并把每题得分写进
     *    examInfo.questionScore（answerService.updateById 保存）；
     * 4. 有 id 更新、无 id 新增错题本记录。
     *
     * 【为什么这么写】
     * - 连续答对 N 次自动移出错题：避免学生永远困在同一道错题上，答对一定次数视为已掌握；
     * - 一次做错就清零连续正确数：错题本机制的核心是"连续做对才算掌握"。
     *
     * @param request 错题本请求（templateId + answer 作答 + answerId 练习答卷可选）
     * @return 本次判分结果（qscore 供前端提示对错）
     * @implNote 被 RepoController.updateUserBook 调用。
     */
    @Override
    public UserBookView updateUserBook(UserBookRequest request) {
        UserBookView result = new UserBookView();
        UserBook userBook = userBookViewMapper.fromRequest(request);
        if (userBook.getId() == null) {
            UserBook exist = userBookService.getOne(Wrappers.<UserBook>lambdaQuery()
                    .eq(UserBook::getTemplateId, request.getTemplateId())
                    .eq(UserBook::getCreateBy, SecurityContextUtils.getUserId())
                    .last("limit 1"));
            if (exist != null) {
                userBook.setId(exist.getId());
            }
        }
        if (request.getAnswer() != null) {
            Template template = templateService.getById(request.getTemplateId());
            userBook.setRepoId(template.getRepoId());
            userBook.setName(template.getName());
            userBook.setType(BOOK_TYPE_WRONG);
            // 模板问题分值默认是没有分数的，需要手动设置一个分数用于正确和错误运算
            template.getTemplate().getAttribute().setExamScore(5.0);
            template.getTemplate().setId(template.getId());
            AnswerScoreEvaluator answerScoreEvaluator = new AnswerScoreEvaluator(template.getTemplate(),
                    request.getAnswer());
            Double qScore = answerScoreEvaluator.eval();
            UserInfo userInfo = SecurityContextUtils.getUser();
            // 回答正确
            // 连续做对几次，自动移出错题/0代表永不移出
            if (qScore > 0) {
                userBook.setCorrectTimes(Optional.ofNullable(userBook.getCorrectTimes()).orElse(0) + 1);
                // 正确几次之后会从错题本移除
                if (Optional.ofNullable(userInfo.getCorrectTimes()).orElse(0) >= userBook.getCorrectTimes()) {
                    userBook.setWrongTimes(0);
                }
            } else {
                // 回答失败，只要做错一次就给正确次数置 0
                userBook.setWrongTimes(Optional.ofNullable(userBook.getWrongTimes()).orElse(0) + 1);
                userBook.setCorrectTimes(0);
            }

            result.setQscore(qScore);
            // 保存临时答案
            if (request.getAnswerId() != null) {
                Answer answer = answerService.getOne(Wrappers.<Answer>lambdaQuery().select(Answer::getId,
                        Answer::getTempAnswer, Answer::getExamInfo)
                        .eq(Answer::getId, request.getAnswerId()));
                LinkedHashMap tempAnswer = Optional.ofNullable(answer.getTempAnswer()).orElse(new LinkedHashMap());
                tempAnswer.putAll(request.getAnswer());
                answer.setTempAnswer(tempAnswer);
                AnswerExamInfo examInfo = Optional.ofNullable(answer.getExamInfo()).orElse(new AnswerExamInfo());
                LinkedHashMap<String, Double> questionScore = Optional.ofNullable(examInfo.getQuestionScore())
                        .orElse(new LinkedHashMap<>());
                examInfo.setQuestionScore(questionScore);
                questionScore.put(request.getTemplateId(), qScore);
                answer.setExamInfo(examInfo);
                answerService.updateById(answer);
            }
        }
        if (userBook.getId() != null) {
            userBookService.updateById(userBook);
        } else {
            userBookService.save(userBook);
        }

        return result;

    }

    /**
     * 删除错题本记录（支持按 id / ids / templateId 三种方式，均限本人数据）。
     *
     * @param request 含 id 或 ids 或 templateId
     */
    @Override
    public void deleteUserBook(UserBookRequest request) {
        if (request.getId() != null) {
            userBookService.removeById(request.getId());
        }
        if (CollectionUtils.isNotEmpty(request.getIds())) {
            userBookService.removeByIds(request.getIds());
        }
        if (request.getTemplateId() != null) {
            userBookService
                    .remove(Wrappers.<UserBook>lambdaUpdate().eq(UserBook::getTemplateId, request.getTemplateId())
                            .eq(UserBook::getCreateBy, SecurityContextUtils.getUserId()));
        }
    }

    /**
     * 题库选择器：按模式查询本人创建或共享的题库（供出卷/抽题界面下拉选择）。
     *
     * @param request 含 mode（survey/exam）
     * @return 题库视图列表（含题目总数与标签/题型统计）
     * @implNote 与 listRepo 权限口径一致：createBy=当前用户 OR shared=1。
     */
    @Override
    public List<RepoView> selectRepo(SelectRepoRequest request) {
        List<RepoView> result = repoViewMapper
                .toView(list(Wrappers.<Repo>lambdaQuery().eq(Repo::getMode, request.getMode()).and(x -> x
                        .eq(Repo::getCreateBy, SecurityContextUtils.getUserId()).or(y -> y.eq(Repo::getShared, 1)))));
        result.forEach(repoView -> {
            repoView.setTotal(
                    templateService.count(Wrappers.<Template>lambdaQuery().eq(Template::getRepoId, repoView.getId())));
            // 获取每个标签对应的题的数量
            repoView.setTemplateTags(this.getBaseMapper().selectRepoTemplateTags(repoView.getId()));
            // 获取每个问题类型对应的题的数量
            repoView.setRepoQuestionTypes(this.getBaseMapper().selectRepoQuestionTypes(repoView.getId()));
        });
        return result;
    }

    /**
     * 导出题库题目为 Excel（按题型分 5 个 sheet：单选/多选/判断/填空/简答）。
     *
     * 【导出列结构】（各题型略有差异）
     * - 单选/多选：序号、题型、所属题库、题干、选项A~H、知识点（学科>章节>知识点+难度）、
     *   正确答案、分值、解析、标签
     * - 判断：序号、题型、所属题库、题干、选项A、选项B、知识点、正确答案、分值、解析、标签
     * - 填空：序号、题型、所属题库、题干、空1~空8、知识点、正确答案、分值、解析、标签
     * - 简答：序号、题型、所属题库、题干、答案、知识点、分值、解析、标签
     *
     * 【内部逻辑步骤】
     * 1. 按筛选条件查询题目（未指定 repoId 时导出全部；支持 name/questionType/
     *    subject/chapter/difficulty 的 SQL 过滤 + knowledgePoint 的内存过滤），
     *    限定 5 种常规题型、按题型+创建时间排序；
     * 2. 按题型分组（groupingBy），每种题型构建行数据（列值来自辅助方法：
     *    questionTypeLabel 题型中文、repoNameOf 所属题库名、knowledgePointText 知识点组合、
     *    extractCorrectAnswer 正确答案、attribute.examScore 分值、attribute.examAnalysis 解析、
     *    tag 数组 join）；
     * 3. 空 sheet 兜底：没有任何题型数据时创建空白 sheet，避免 fastexcel 空工作簿 finish 报错；
     * 4. 按题型创建 5 个 sheet，写表头 + 数据行，fitToWidth 适配列宽；
     * 5. workbook.finish() 后把字节流写入 HTTP 响应（Content-Disposition 附件下载）。
     *
     * 【数据流向】
     * RepoController.exportRepoQuestions → exportRepoQuestions → TemplateMapper 查询 t_template
     * → 内存组装行数据 → fastexcel 写流 → 浏览器下载 xlsx。
     *
     * @param request 含题库 id（可空：空则导出全部题目）及题目维度筛选条件
     *        （name/questionType/subject/chapter/knowledgePoint/difficulty）
     */
    @Override
    @SneakyThrows
    public void exportRepoQuestions(RepoRequest request) {
        // 获取题库信息
        Repo repo = getById(request.getId());
        String fileName = (repo != null && repo.getName() != null ? repo.getName() : "题库") + ".xlsx";

        // 设置响应头
        ContextHelper.getCurrentHttpResponse()
                .setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ContextHelper.getCurrentHttpResponse().setHeader("Content-Disposition", "attachment; filename=" +
                java.net.URLEncoder.encode(fileName, "UTF-8"));

        // 查询题库中的各种题型（未指定题库时导出全部题目）
        // 筛选条件与题目管理页一致（AND 关系）：repoId 题库、name 名称模糊、
        // questionType 题型、subject/chapter/knowledgePoint/difficulty 知识点四维
        List<Template> questions = templateService.list(Wrappers.<Template>lambdaQuery()
                .eq(request.getId() != null, Template::getRepoId, request.getId())
                .like(request.getName() != null && !request.getName().isEmpty(), Template::getName,
                        request.getName())
                .eq(request.getQuestionType() != null && !request.getQuestionType().isEmpty(),
                        Template::getQuestionType, request.getQuestionType())
                .eq(request.getSubject() != null && !request.getSubject().isEmpty(), Template::getSubject,
                        request.getSubject())
                .eq(request.getChapter() != null && !request.getChapter().isEmpty(), Template::getChapter,
                        request.getChapter())
                .eq(request.getDifficulty() != null && !request.getDifficulty().isEmpty(), Template::getDifficulty,
                        request.getDifficulty())
                .in(Template::getQuestionType, Arrays.asList(
                        SurveySchema.QuestionType.Radio,
                        SurveySchema.QuestionType.Checkbox,
                        SurveySchema.QuestionType.Judge,
                        SurveySchema.QuestionType.FillBlank,
                        SurveySchema.QuestionType.Textarea))
                .orderByAsc(Template::getQuestionType, Template::getCreateAt));
        // 知识点筛选：knowledge_point 存储为 JSON 数组字符串，无法用 SQL 等值匹配，
        // 这里做内存二次过滤（like 查询）——只要题目 JSON 数组文本包含该知识点即命中
        if (request.getKnowledgePoint() != null && !request.getKnowledgePoint().isEmpty()) {
            questions = questions.stream()
                    .filter(t -> t.getKnowledgePoint() != null
                            && Arrays.asList(t.getKnowledgePoint()).stream()
                                    .anyMatch(kp -> kp != null && kp.contains(request.getKnowledgePoint())))
                    .collect(Collectors.toList());
        }

        // 准备导出数据 - 按题型分组
        Map<SurveySchema.QuestionType, List<Template>> questionsByType = questions.stream()
                .collect(Collectors.groupingBy(Template::getQuestionType));

        // 先处理单选题
        List<List<Object>> radioRows = new ArrayList<>();
        List<Template> radioQuestions = questionsByType.getOrDefault(SurveySchema.QuestionType.Radio,
                new ArrayList<>());
        int radioIndex = 1;

        for (Template template : radioQuestions) {
            SurveySchema schema = template.getTemplate();
            List<Object> row = new ArrayList<>();

            // 序号
            row.add(radioIndex++);

            // 题型
            row.add(questionTypeLabel(template.getQuestionType()));

            // 所属题库
            row.add(repoNameOf(template.getRepoId()));

            // 题干
            row.add(schema.getTitle());

            // 选项A-H (单选题处理)
            List<SurveySchema> options = schema.getChildren();
            String[] optionTexts = new String[8];
            Double examScore = schema.getAttribute() != null ? schema.getAttribute().getExamScore() : null;

            if (options != null) {
                for (int i = 0; i < Math.min(options.size(), 8); i++) {
                    SurveySchema option = options.get(i);
                    optionTexts[i] = option.getTitle();
                }
            }

            // 添加选项A-H
            for (int i = 0; i < 8; i++) {
                row.add(optionTexts[i] != null ? optionTexts[i] : "");
            }

            // 知识点（学科>章节>知识点，含难度）
            row.add(knowledgePointText(template));

            // 正确答案
            row.add(extractCorrectAnswer(template, false));

            // 分值
            row.add(examScore != null ? examScore : "");

            // 解析
            String analysis = schema.getAttribute() != null ? schema.getAttribute().getExamAnalysis() : "";
            row.add(analysis != null ? analysis : "");

            // 标签
            String tags = "";
            if (template.getTag() != null && template.getTag().length > 0) {
                tags = String.join(",", template.getTag());
            }
            row.add(tags);

            radioRows.add(row);
        }

        // 处理多选题
        List<List<Object>> checkboxRows = new ArrayList<>();
        List<Template> checkboxQuestions = questionsByType.getOrDefault(SurveySchema.QuestionType.Checkbox,
                new ArrayList<>());
        int checkboxIndex = 1;

        for (Template template : checkboxQuestions) {
            SurveySchema schema = template.getTemplate();
            List<Object> row = new ArrayList<>();

            // 序号
            row.add(checkboxIndex++);

            // 题型
            row.add(questionTypeLabel(template.getQuestionType()));

            // 所属题库
            row.add(repoNameOf(template.getRepoId()));

            // 题干
            row.add(schema.getTitle());

            // 选项A-H (多选题处理)
            List<SurveySchema> options = schema.getChildren();
            String[] optionTexts = new String[8];
            Double examScore = schema.getAttribute() != null ? schema.getAttribute().getExamScore() : null;

            if (options != null) {
                for (int i = 0; i < Math.min(options.size(), 8); i++) {
                    SurveySchema option = options.get(i);
                    optionTexts[i] = option.getTitle();
                }
            }

            // 添加选项A-H
            for (int i = 0; i < 8; i++) {
                row.add(optionTexts[i] != null ? optionTexts[i] : "");
            }

            // 知识点（学科>章节>知识点，含难度）
            row.add(knowledgePointText(template));

            // 正确答案（多选题多个答案用逗号分隔）
            row.add(extractCorrectAnswer(template, true));

            // 分值
            row.add(examScore != null ? examScore : "");

            // 解析
            String analysis = schema.getAttribute() != null ? schema.getAttribute().getExamAnalysis() : "";
            row.add(analysis != null ? analysis : "");

            // 标签
            String tags = "";
            if (template.getTag() != null && template.getTag().length > 0) {
                tags = String.join(",", template.getTag());
            }
            row.add(tags);

            checkboxRows.add(row);
        }

        // 处理判断题
        List<List<Object>> judgeRows = new ArrayList<>();
        List<Template> judgeQuestions = questionsByType.getOrDefault(SurveySchema.QuestionType.Judge,
                new ArrayList<>());
        int judgeIndex = 1;

        for (Template template : judgeQuestions) {
            SurveySchema schema = template.getTemplate();
            List<Object> row = new ArrayList<>();

            // 序号
            row.add(judgeIndex++);

            // 题型
            row.add(questionTypeLabel(template.getQuestionType()));

            // 所属题库
            row.add(repoNameOf(template.getRepoId()));

            // 题干
            row.add(schema.getTitle());

            // 选项A、选项B（判断题一般是正确/错误）
            List<SurveySchema> options = schema.getChildren();
            String optionA = "";
            String optionB = "";
            Double examScore = schema.getAttribute() != null ? schema.getAttribute().getExamScore() : null;

            if (options != null && options.size() >= 2) {
                optionA = options.get(0).getTitle();
                optionB = options.get(1).getTitle();
            }

            row.add(optionA);
            row.add(optionB);

            // 知识点（学科>章节>知识点，含难度）
            row.add(knowledgePointText(template));

            // 正确答案
            row.add(extractCorrectAnswer(template, false));

            // 分值
            row.add(examScore != null ? examScore : "");

            // 解析
            String analysis = schema.getAttribute() != null ? schema.getAttribute().getExamAnalysis() : "";
            row.add(analysis != null ? analysis : "");

            // 标签
            String tags = "";
            if (template.getTag() != null && template.getTag().length > 0) {
                tags = String.join(" ", template.getTag());
            }
            row.add(tags);

            judgeRows.add(row);
        }

        // 处理填空题
        List<List<Object>> fillBlankRows = new ArrayList<>();
        List<Template> fillBlankQuestions = questionsByType.getOrDefault(SurveySchema.QuestionType.FillBlank,
                new ArrayList<>());
        int fillBlankIndex = 1;

        for (Template template : fillBlankQuestions) {
            SurveySchema schema = template.getTemplate();
            List<Object> row = new ArrayList<>();

            // 序号
            row.add(fillBlankIndex++);

            // 题型
            row.add(questionTypeLabel(template.getQuestionType()));

            // 所属题库
            row.add(repoNameOf(template.getRepoId()));

            // 题干
            row.add(schema.getTitle());

            // 空1-空8
            List<SurveySchema> blanks = schema.getChildren();
            String[] blankAnswers = new String[8];
            Double examScore = schema.getAttribute() != null ? schema.getAttribute().getExamScore() : null;

            if (blanks != null) {
                for (int i = 0; i < Math.min(blanks.size(), 8); i++) {
                    SurveySchema blank = blanks.get(i);
                    if (blank.getAttribute() != null && blank.getAttribute().getExamCorrectAnswer() != null) {
                        blankAnswers[i] = blank.getAttribute().getExamCorrectAnswer();
                    }
                }
            }

            // 添加空1-空8
            for (int i = 0; i < 8; i++) {
                row.add(blankAnswers[i] != null ? blankAnswers[i] : "");
            }

            // 知识点（学科>章节>知识点，含难度）
            row.add(knowledgePointText(template));

            // 正确答案（整题级答案文本）
            row.add(extractCorrectAnswer(template, false));

            // 分值
            row.add(examScore != null ? examScore : "");

            // 解析
            String analysis = schema.getAttribute() != null ? schema.getAttribute().getExamAnalysis() : "";
            row.add(analysis != null ? analysis : "");

            // 标签
            String tags = "";
            if (template.getTag() != null && template.getTag().length > 0) {
                tags = String.join(" ", template.getTag());
            }
            row.add(tags);

            fillBlankRows.add(row);
        }

        // 处理简答题（Textarea）
        List<List<Object>> textareaRows = new ArrayList<>();
        List<Template> textareaQuestions = questionsByType.getOrDefault(SurveySchema.QuestionType.Textarea,
                new ArrayList<>());
        int textareaIndex = 1;

        for (Template template : textareaQuestions) {
            SurveySchema schema = template.getTemplate();
            List<Object> row = new ArrayList<>();

            // 序号
            row.add(textareaIndex++);

            // 题型
            row.add(questionTypeLabel(template.getQuestionType()));

            // 所属题库
            row.add(repoNameOf(template.getRepoId()));

            // 题干
            row.add(schema.getTitle());

            // 答案（对于简答题，可能存储在第一个子元素中）
            String answer = "";
            if (schema.getChildren() != null && !schema.getChildren().isEmpty()) {
                SurveySchema firstChild = schema.getChildren().get(0);
                if (firstChild.getAttribute() != null && firstChild.getAttribute().getExamCorrectAnswer() != null) {
                    answer = firstChild.getAttribute().getExamCorrectAnswer();
                }
            }
            row.add(answer);

            // 知识点（学科>章节>知识点，含难度）
            row.add(knowledgePointText(template));

            // 分值
            Double examScore = schema.getAttribute() != null ? schema.getAttribute().getExamScore() : null;
            row.add(examScore != null ? examScore : "");

            // 解析
            String analysis = schema.getAttribute() != null ? schema.getAttribute().getExamAnalysis() : "";
            row.add(analysis != null ? analysis : "");

            // 标签
            String tags = "";
            if (template.getTag() != null && template.getTag().length > 0) {
                tags = String.join(" ", template.getTag());
            }
            row.add(tags);

            textareaRows.add(row);
        }

        // 准备不同题型的列标题（序号/题型/所属题库/题干/选项或空/知识点/正确答案/分值/解析/标签）
        List<String> radioCheckboxHeaders = new ArrayList<>();
        radioCheckboxHeaders.add(RepoTemplateI18n.HeaderLabel.SERIAL_NO.displayLabel());
        radioCheckboxHeaders.add("题型");
        radioCheckboxHeaders.add("所属题库");
        radioCheckboxHeaders.add(RepoTemplateI18n.HeaderLabel.TITLE.displayLabel());
        for (String suffix : Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H")) {
            radioCheckboxHeaders.add(RepoTemplateI18n.optionLabel(suffix));
        }
        radioCheckboxHeaders.add("知识点");
        radioCheckboxHeaders.add("正确答案");
        radioCheckboxHeaders.add(RepoTemplateI18n.HeaderLabel.SCORE.displayLabel());
        radioCheckboxHeaders.add(RepoTemplateI18n.HeaderLabel.ANALYSIS.displayLabel());
        radioCheckboxHeaders.add(RepoTemplateI18n.HeaderLabel.TAGS.displayLabel());

        List<String> judgeHeaders = new ArrayList<>();
        judgeHeaders.add(RepoTemplateI18n.HeaderLabel.SERIAL_NO.displayLabel());
        judgeHeaders.add("题型");
        judgeHeaders.add("所属题库");
        judgeHeaders.add(RepoTemplateI18n.HeaderLabel.TITLE.displayLabel());
        judgeHeaders.add(RepoTemplateI18n.optionLabel("A"));
        judgeHeaders.add(RepoTemplateI18n.optionLabel("B"));
        judgeHeaders.add("知识点");
        judgeHeaders.add("正确答案");
        judgeHeaders.add(RepoTemplateI18n.HeaderLabel.SCORE.displayLabel());
        judgeHeaders.add(RepoTemplateI18n.HeaderLabel.ANALYSIS.displayLabel());
        judgeHeaders.add(RepoTemplateI18n.HeaderLabel.TAGS.displayLabel());

        List<String> fillBlankHeaders = new ArrayList<>();
        fillBlankHeaders.add(RepoTemplateI18n.HeaderLabel.SERIAL_NO.displayLabel());
        fillBlankHeaders.add("题型");
        fillBlankHeaders.add("所属题库");
        fillBlankHeaders.add(RepoTemplateI18n.HeaderLabel.TITLE.displayLabel());
        for (String index : Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8")) {
            fillBlankHeaders.add(RepoTemplateI18n.blankLabel(index));
        }
        fillBlankHeaders.add("知识点");
        fillBlankHeaders.add("正确答案");
        fillBlankHeaders.add(RepoTemplateI18n.HeaderLabel.SINGLE_BLANK_SCORE.displayLabel());
        fillBlankHeaders.add(RepoTemplateI18n.HeaderLabel.ANALYSIS.displayLabel());
        fillBlankHeaders.add(RepoTemplateI18n.HeaderLabel.TAGS.displayLabel());

        List<String> textareaHeaders = Arrays.asList(
                RepoTemplateI18n.HeaderLabel.SERIAL_NO.displayLabel(),
                "题型",
                "所属题库",
                RepoTemplateI18n.HeaderLabel.TITLE.displayLabel(),
                RepoTemplateI18n.HeaderLabel.ANSWER.displayLabel(),
                "知识点",
                RepoTemplateI18n.HeaderLabel.SCORE.displayLabel(),
                RepoTemplateI18n.HeaderLabel.ANALYSIS.displayLabel(),
                RepoTemplateI18n.HeaderLabel.TAGS.displayLabel());

        // 创建Excel工作簿，包含多个sheet
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            org.dhatim.fastexcel.Workbook workbook = new org.dhatim.fastexcel.Workbook(baos, RepoTemplateI18n.workbookName(), "1.0");

            // 兜底：没有任何题型数据时创建空白 sheet，避免空工作簿导致 finish 抛异常
            if (radioRows.isEmpty() && checkboxRows.isEmpty() && judgeRows.isEmpty()
                    && fillBlankRows.isEmpty() && textareaRows.isEmpty()) {
                workbook.newWorksheet(RepoTemplateI18n.SheetType.SINGLE_CHOICE.displayName());
            }

            // 创建单选题sheet
            if (!radioRows.isEmpty()) {
                org.dhatim.fastexcel.Worksheet radioSheet = workbook.newWorksheet(RepoTemplateI18n.SheetType.SINGLE_CHOICE.displayName());
                radioSheet.fitToWidth((short) 10);
                radioSheet.setFitToPage(true);

                // 添加表头
                for (int i = 0; i < radioCheckboxHeaders.size(); i++) {
                    radioSheet.value(0, i, radioCheckboxHeaders.get(i));
                }

                // 添加数据行
                for (int r = 0; r < radioRows.size(); r++) {
                    List<Object> rowData = radioRows.get(r);
                    for (int c = 0; c < rowData.size(); c++) {
                        Object value = rowData.get(c);
                        if (value instanceof Integer) {
                            radioSheet.value(r + 1, c, (Number) value);
                        } else if (value instanceof String) {
                            radioSheet.value(r + 1, c, (String) value);
                        } else if (value != null) {
                            radioSheet.value(r + 1, c, value.toString());
                        }
                    }
                }
            }

            // 创建多选题sheet
            if (!checkboxRows.isEmpty()) {
                org.dhatim.fastexcel.Worksheet checkboxSheet = workbook.newWorksheet(RepoTemplateI18n.SheetType.MULTIPLE_CHOICE.displayName());
                checkboxSheet.fitToWidth((short) 10);
                checkboxSheet.setFitToPage(true);

                // 添加表头
                for (int i = 0; i < radioCheckboxHeaders.size(); i++) {
                    checkboxSheet.value(0, i, radioCheckboxHeaders.get(i));
                }

                // 添加数据行
                for (int r = 0; r < checkboxRows.size(); r++) {
                    List<Object> rowData = checkboxRows.get(r);
                    for (int c = 0; c < rowData.size(); c++) {
                        Object value = rowData.get(c);
                        if (value instanceof Integer) {
                            checkboxSheet.value(r + 1, c, (Number) value);
                        } else if (value instanceof String) {
                            checkboxSheet.value(r + 1, c, (String) value);
                        } else if (value != null) {
                            checkboxSheet.value(r + 1, c, value.toString());
                        }
                    }
                }
            }

            // 创建判断题sheet
            if (!judgeRows.isEmpty()) {
                org.dhatim.fastexcel.Worksheet judgeSheet = workbook.newWorksheet(RepoTemplateI18n.SheetType.TRUE_FALSE.displayName());
                judgeSheet.fitToWidth((short) 10);
                judgeSheet.setFitToPage(true);

                // 添加表头
                for (int i = 0; i < judgeHeaders.size(); i++) {
                    judgeSheet.value(0, i, judgeHeaders.get(i));
                }

                // 添加数据行
                for (int r = 0; r < judgeRows.size(); r++) {
                    List<Object> rowData = judgeRows.get(r);
                    for (int c = 0; c < rowData.size(); c++) {
                        Object value = rowData.get(c);
                        if (value instanceof Integer) {
                            judgeSheet.value(r + 1, c, (Number) value);
                        } else if (value instanceof String) {
                            judgeSheet.value(r + 1, c, (String) value);
                        } else if (value != null) {
                            judgeSheet.value(r + 1, c, value.toString());
                        }
                    }
                }
            }

            // 创建填空题sheet
            if (!fillBlankRows.isEmpty()) {
                org.dhatim.fastexcel.Worksheet fillBlankSheet = workbook.newWorksheet(RepoTemplateI18n.SheetType.FILL_BLANK.displayName());
                fillBlankSheet.fitToWidth((short) 10);
                fillBlankSheet.setFitToPage(true);

                // 添加表头
                for (int i = 0; i < fillBlankHeaders.size(); i++) {
                    fillBlankSheet.value(0, i, fillBlankHeaders.get(i));
                }

                // 添加数据行
                for (int r = 0; r < fillBlankRows.size(); r++) {
                    List<Object> rowData = fillBlankRows.get(r);
                    for (int c = 0; c < rowData.size(); c++) {
                        Object value = rowData.get(c);
                        if (value instanceof Integer) {
                            fillBlankSheet.value(r + 1, c, (Number) value);
                        } else if (value instanceof String) {
                            fillBlankSheet.value(r + 1, c, (String) value);
                        } else if (value != null) {
                            fillBlankSheet.value(r + 1, c, value.toString());
                        }
                    }
                }
            }

            // 创建简答题sheet
            if (!textareaRows.isEmpty()) {
                org.dhatim.fastexcel.Worksheet textareaSheet = workbook.newWorksheet(RepoTemplateI18n.SheetType.TEXTAREA.displayName());
                textareaSheet.fitToWidth((short) 10);
                textareaSheet.setFitToPage(true);

                // 添加表头
                for (int i = 0; i < textareaHeaders.size(); i++) {
                    textareaSheet.value(0, i, textareaHeaders.get(i));
                }

                // 添加数据行
                for (int r = 0; r < textareaRows.size(); r++) {
                    List<Object> rowData = textareaRows.get(r);
                    for (int c = 0; c < rowData.size(); c++) {
                        Object value = rowData.get(c);
                        if (value instanceof Integer) {
                            textareaSheet.value(r + 1, c, (Number) value);
                        } else if (value instanceof String) {
                            textareaSheet.value(r + 1, c, (String) value);
                        } else if (value != null) {
                            textareaSheet.value(r + 1, c, value.toString());
                        }
                    }
                }
            }

            workbook.finish();

            // 写入响应流
            ContextHelper.getCurrentHttpResponse().getOutputStream().write(baos.toByteArray());
        }
    }

    // ============================================================
    // 导出辅助方法
    // ============================================================

    /**
     * 题型中文标签（导出"题型"列使用）。
     *
     * @param type 题型枚举
     * @return 中文标签：Radio→单选题、Checkbox→多选题、Judge→判断题、
     *         FillBlank→填空题、Textarea→简答题；未知类型返回枚举名；null 返回空串
     * @implNote 被 exportRepoQuestions 调用。
     */
    private String questionTypeLabel(SurveySchema.QuestionType type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case Radio:
                return "单选题";
            case Checkbox:
                return "多选题";
            case Judge:
                return "判断题";
            case FillBlank:
                return "填空题";
            case Textarea:
                return "简答题";
            default:
                return type.name();
        }
    }

    /**
     * 所属题库名称（导出"所属题库"列使用）：按 repoId 查询题库名，查不到返回空串。
     *
     * @param repoId 题库 ID
     * @return 题库名称
     * @implNote 被 exportRepoQuestions 调用；未指定题库导出全部题目时该项可能是空。
     */
    private String repoNameOf(String repoId) {
        if (!StringUtils.hasText(repoId)) {
            return "";
        }
        Repo repo = getById(repoId);
        return repo != null && repo.getName() != null ? repo.getName() : "";
    }

    /**
     * 知识点组合文本（导出"知识点"列）：学科 > 章节 > 知识点（多值逗号连接），
     * 难度以全角括号附加标注，如 "数学 > 函数 > 单调性,奇偶性（中等）"。
     *
     * 【数据来源双格式兼容】
     * - 优先题目顶层字段（新数据格式）：template.subject/chapter/knowledgePoint/difficulty；
     * - 回退题目 JSON attribute 快照（旧数据格式）：attr.subject/chapter/knowledgePoint/difficulty，
     *   保证旧题库导出不丢维度信息。
     *
     * 【格式约定】
     * - 各级间用 " > " 连接；知识点多值用英文逗号连接；
     * - 难度映射：easy→简单、medium→中等、hard→困难，未知原样输出。
     *
     * @param template 题目模板实体
     * @return 组装好的知识点文本
     * @implNote 被 exportRepoQuestions 调用（五种题型 sheet 均使用）。
     */
    private String knowledgePointText(Template template) {
        SurveySchema schema = template.getTemplate();
        SurveySchema.Attribute attr = schema != null ? schema.getAttribute() : null;
        String subject = template.getSubject() != null ? template.getSubject()
                : (attr != null ? attr.getSubject() : null);
        String chapter = template.getChapter() != null ? template.getChapter()
                : (attr != null ? attr.getChapter() : null);
        String kp = "";
        if (template.getKnowledgePoint() != null && template.getKnowledgePoint().length > 0) {
            kp = String.join(",", template.getKnowledgePoint());
        } else if (attr != null && attr.getKnowledgePoint() != null && !attr.getKnowledgePoint().isEmpty()) {
            kp = String.join(",", attr.getKnowledgePoint());
        }
        String difficulty = template.getDifficulty() != null ? template.getDifficulty()
                : (attr != null ? attr.getDifficulty() : null);
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(subject)) {
            sb.append(subject);
        }
        if (StringUtils.hasText(chapter)) {
            if (sb.length() > 0) {
                sb.append(" > ");
            }
            sb.append(chapter);
        }
        if (StringUtils.hasText(kp)) {
            if (sb.length() > 0) {
                sb.append(" > ");
            }
            sb.append(kp);
        }
        if (StringUtils.hasText(difficulty)) {
            String label;
            switch (difficulty) {
                case "easy":
                    label = "简单";
                    break;
                case "medium":
                    label = "中等";
                    break;
                case "hard":
                    label = "困难";
                    break;
                default:
                    label = difficulty;
            }
            sb.append("（").append(label).append("）");
        }
        return sb.toString();
    }

    /**
     * 提取正确答案文本（导出"正确答案"列）。
     *
     * 【提取优先级】
     * 1. 整题级答案：题目 attribute.examCorrectAnswer（多选题 \n 分隔多个时，
     *    multi=true 会转成逗号分隔 "A,B"）；
     * 2. 选项级答案：遍历子选项，attribute.examCorrectAnswer 非空的选项
     *    按位置转成 A/B/C... 字母，多个用逗号连接。
     *
     * @param template 题目模板实体
     * @param multi    是否多选题（true 时整题级答案按 \n 拆开转逗号分隔）
     * @return 正确答案文本；无标准答案返回空串
     * @implNote 被 exportRepoQuestions 调用：单选/判断/填空/简答传 false，多选传 true。
     */
    private String extractCorrectAnswer(Template template, boolean multi) {
        SurveySchema schema = template.getTemplate();
        if (schema == null) {
            return "";
        }
        SurveySchema.Attribute attr = schema.getAttribute();
        if (attr != null && StringUtils.hasText(attr.getExamCorrectAnswer())) {
            String top = attr.getExamCorrectAnswer();
            if (multi && top.contains("\n")) {
                return Arrays.stream(top.split("\n")).map(String::trim)
                        .filter(s -> !s.isEmpty()).collect(Collectors.joining(","));
            }
            return top;
        }
        List<String> letters = new ArrayList<>();
        if (schema.getChildren() != null) {
            for (int i = 0; i < schema.getChildren().size(); i++) {
                SurveySchema option = schema.getChildren().get(i);
                if (option.getAttribute() != null
                        && StringUtils.hasText(option.getAttribute().getExamCorrectAnswer())) {
                    letters.add(String.valueOf((char) ('A' + i)));
                }
            }
        }
        return letters.isEmpty() ? "" : String.join(",", letters);
    }

}
