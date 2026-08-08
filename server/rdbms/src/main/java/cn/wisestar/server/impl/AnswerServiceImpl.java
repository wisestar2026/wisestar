package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.AttachmentNameVariableEnum;
import cn.wisestar.server.core.constant.ProjectModeEnum;
import cn.wisestar.server.core.constant.StorageTypeEnum;
import cn.wisestar.server.core.exception.InternalServerError;
import cn.wisestar.server.core.uitls.*;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.domain.mapper.AnswerViewMapper;
import cn.wisestar.server.domain.model.Answer;
import cn.wisestar.server.domain.model.AnswerDetail;
import cn.wisestar.server.domain.model.Project;
import cn.wisestar.server.domain.model.ProjectPartner;
import cn.wisestar.server.mapper.AnswerMapper;
import cn.wisestar.server.mapper.ProjectMapper;
import cn.wisestar.server.mapper.ProjectPartnerMapper;
import cn.wisestar.server.mapper.AnswerDetailMapper;
import cn.wisestar.server.service.*;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.springframework.beans.BeanUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 答卷（Answer）业务实现：答卷的保存/查询/导出/删除、考试计分与答题明细生成。
 *
 * 【类职责】
 * 处理"答卷"全生命周期业务：
 * 1. 答卷 CRUD：listAnswer 分页查询、getAnswer 详情、saveAnswer/updateAnswer 保存与更新、
 *    deleteAnswer 删除、回收站（listAnswerDeleted/restoreAnswer/batchDestroyAnswer）
 * 2. 考试计分：beforeSaveAnswer → computeExamScore 用 AnswerScoreEvaluator 计算总分与每题得分
 * 3. 答题明细生成：generateAnswerDetails（提交答卷时按题落库 t_answer_detail，
 *    供学生知识点分析使用，幂等先删后插）
 * 4. 导出：downloadSurvey 导出 xlsx、downloadAttachment 附件下载/打包 zip、
 *    附件重命名表达式 parseAttachmentNameByExp
 * 5. 其他：Excel 批量导入 upload、练习历史 historyExercise、关联问卷答案同步
 *    updateLinkSurveyAnswer
 *
 * 【被谁调用】
 * - Controller：AnswerController（管理端答卷管理）、SurveyController（公开答卷提交间接调用）
 * - 业务层：SurveyServiceImpl.saveAnswer / tempSaveAnswer / loadProject、RepoServiceImpl
 *   （错题本保存临时答案）、RandomSurveyProcessor（保存随机 schema）、UserServiceImpl
 *   （历史任务查询）、FileServiceImpl（附件下载时验证项目）
 *
 * 【依赖什么】
 * - AnswerMapper（BaseMapper CRUD + 回收站自定义 SQL）、AnswerDetailMapper（答题明细）、
 *   ProjectMapper / ProjectPartnerMapper（项目与参与人）
 * - FileService（附件）、UserService/DeptService/ProjectService（答案附加信息回填）
 * - AnswerViewMapper（MapStruct：Answer↔AnswerView↔AnswerRequest 转换）
 * - AnswerScoreEvaluator（考试计分）、SchemaHelper（schema 扁平化/解析）、ExcelExporter（导出）
 *
 * 【核心数据流】
 * 学生提交答卷 → SurveyServiceImpl.saveAnswer → saveAnswer/updateAnswer
 * → beforeSaveAnswer（计分 + 关联问卷同步）→ save/updateById 写 t_answer
 * → generateAnswerDetails 按题写 t_answer_detail（先删后插）
 * → AnswerViewMapper.toView 返回 AnswerView（含题目附加信息/排名等）。
 *
 * @author javahuang
 * @date 2021/8/3
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AnswerServiceImpl extends ServiceImpl<AnswerMapper, Answer> implements AnswerService {

    private final ProjectMapper projectMapper;

    private final FileService fileService;

    private final AnswerViewMapper answerViewMapper;

    private final UserService userService;

    private final DeptService deptService;

    private final ProjectService projectService;

    private final ProjectPartnerMapper projectPartnerMapper;

    private final AnswerDetailMapper answerDetailMapper;

    /**
     * 分页查询答卷列表。
     *
     * @param query 查询条件：projectId 项目、ids/id 主键、startTime/endTime 创建时间区间
     * @return 分页的 AnswerView 列表
     * @implNote 调用链：AnswerController.listAnswer → listAnswer → AnswerMapper 分页查询。
     * 过滤条件：answer 非空（暂存题答案为空则不算有效答卷）、projectId/ids/时间区间可选、
     * 按创建时间倒序。仅当指定了 projectId 时才补充答案附加信息（解析项目问卷 schema，
     * 把答案中的用户/部门/签名/上传文件 ID 转成可读对象，见 setAnswerExtraInfo）。
     */
	@Override
	public PaginationResponse<AnswerView> listAnswer(AnswerQuery query) {
		Page<Answer> page = new Page<>(query.getCurrent(), query.getPageSize());
		// 项目名称模糊搜索：先按名称模糊查出匹配的 projectId 集合，再对答卷做 IN 过滤
		// （避免 JOIN，保持分页查询简单；名称匹配不区分大小写）
		List<String> matchedProjectIds = null;
		if (query.getProjectName() != null && !query.getProjectName().trim().isEmpty()) {
			matchedProjectIds = projectMapper.selectList(
							Wrappers.<Project>lambdaQuery()
									.like(Project::getName, query.getProjectName().trim()))
					.stream().map(Project::getId).collect(Collectors.toList());
			// 无匹配项目时直接返回空分页，避免 IN () 报错
			if (matchedProjectIds.isEmpty()) {
				return new PaginationResponse<>(0L, new ArrayList<>());
			}
		}
		super.page(page, Wrappers.<Answer>lambdaQuery().isNotNull(Answer::getAnswer) // 暂存题答案会为空
				.eq(query.getProjectId() != null, Answer::getProjectId, query.getProjectId())
				.in(query.getIds() != null && query.getIds().size() > 0, Answer::getId, query.getIds())
				.in(matchedProjectIds != null, Answer::getProjectId, matchedProjectIds)
				.lt(query.getEndTime() != null, Answer::getCreateAt, query.getEndTime())
				.gt(query.getStartTime() != null, Answer::getCreateAt, query.getStartTime())
				.eq(query.getId() != null, Answer::getId, query.getId()).orderByDesc(Answer::getCreateAt));
		List<AnswerView> list = answerViewMapper.toView(page.getRecords());
		// 仅当指定了 projectId 时才补充答案附加信息（项目问卷选项文本等）
		if (query.getProjectId() != null) {
			Project project = projectMapper.selectById(query.getProjectId());
			if (project != null && project.getSurvey() != null) {
				FlatSurveySchemaByType schemaByType = parseSurveySchemaByType(project.getSurvey());
				list.forEach(view -> setAnswerExtraInfo(view, schemaByType));
			}
		}
		return new PaginationResponse<>(page.getTotal(), list);
	}

    /**
     * 解析项目问卷 schema，按题型分类扁平化：
     * User（用户选择题）/ Dept（部门选择题）/ Signature（签名）/ Upload（上传）四类。
     *
     * @param schema 项目问卷原始 schema
     * @return 按题型分组的扁平结构
     * @implNote 被 setAnswerExtraInfo / listAnswerDeleted 调用；
     * 通过 SchemaHelper.flatSurveySchema 把嵌套树拍平成题目列表，再按 QuestionType 过滤分组。
     */
    private FlatSurveySchemaByType parseSurveySchemaByType(SurveySchema schema) {
        FlatSurveySchemaByType schemaByType = new FlatSurveySchemaByType();
        List<SurveySchema> schemaDataTypes = SchemaHelper.flatSurveySchema(schema);
        schemaByType.setSchemaDataTypes(schemaDataTypes);
        schemaByType.setUserQuestions(parseSurveySchemaByType(schemaDataTypes, SurveySchema.QuestionType.User));
        schemaByType.setDeptQuestions(parseSurveySchemaByType(schemaDataTypes, SurveySchema.QuestionType.Dept));
        schemaByType.setFileQuestions(parseSurveySchemaByType(schemaDataTypes, SurveySchema.QuestionType.Signature));
        schemaByType.getFileQuestions()
                .addAll((parseSurveySchemaByType(schemaDataTypes, SurveySchema.QuestionType.Upload)));
        return schemaByType;
    }

    /**
     * 从扁平化题目列表中按题型过滤。
     *
     * @param flattedSurveySchema 扁平化后的全部题目
     * @param questionType        目标题型
     * @return 该题型的题目列表
     */
    private List<SurveySchema> parseSurveySchemaByType(List<SurveySchema> flattedSurveySchema,
                                                       SurveySchema.QuestionType questionType) {
        return flattedSurveySchema.stream().filter(x -> x.getType() == questionType).collect(Collectors.toList());
    }

    /**
     * 为单个 AnswerView 补充附加信息：用户/部门/文件类题目的可读对象 + 创建人姓名。
     *
     * @param view          目标答卷视图
     * @param schemaByType  按题型分组的项目 schema
     * @implNote 依次处理部门题、文件题（签名/上传）、用户题，最后回填创建人姓名（setUserName）。
     */
    private void setAnswerExtraInfo(AnswerView view, FlatSurveySchemaByType schemaByType) {
        setAnswerTypeInfo(schemaByType.getDeptQuestions(), view);
        setAnswerTypeInfo(schemaByType.getFileQuestions(), view);
        setAnswerTypeInfo(schemaByType.getUserQuestions(), view);
        setUserName(view);
    }

    /**
     * 按题型把答案中的 ID 引用转换成可读对象写入 AnswerView：
     * - User 题：答案 value → 用户信息（loadUserById().simpleMode()）写入 view.users
     * - Signature/Upload 题：答案 value（fileId 列表）→ 附件信息（fileService.listFiles）
     *   合并进 view.attachment
     * - Dept 题：答案 value → 部门信息写入 view.depts
     *
     * @param flatQuestionSchema 某一题型的题目列表
     * @param view               目标答卷视图
     * @implNote 答案结构说明：常规题 value 为 {optionId: 值}；签名/上传题前端存的是
     * {qId: {oId: fileId}}，故此处先取 map.values() 把 fileId 转为数组再查文件。
     */
    private void setAnswerTypeInfo(List<SurveySchema> flatQuestionSchema, AnswerView view) {
        if (flatQuestionSchema.size() == 0) {
            return;
        }
        LinkedHashMap<String, Object> answers = view.getAnswer();
        SurveySchema.QuestionType questionType = flatQuestionSchema.get(0).getType();
        flatQuestionSchema.forEach(question -> {
            String questionId = question.getId();
            Object option2value = answers.get(questionId);
            if (option2value != null && option2value instanceof Map) {
                // 签名题前端存的是 {qId: {oId: fileId}} 需要转换成数组
                ((Map<String, Object>) option2value).values().stream().map(x -> {
                    if (x instanceof List) {
                        return (List<String>) x;
                    }
                    return Collections.singletonList(x.toString());
                }).collect(Collectors.toList()).forEach(ids -> {
                    if (questionType == SurveySchema.QuestionType.User) {
                        view.setUsers(ids.stream().map(userId -> userService.loadUserById(userId).simpleMode())
                                .collect(Collectors.toList()));
                    } else if (questionType == SurveySchema.QuestionType.Signature
                            || questionType == SurveySchema.QuestionType.Upload) {
                        FileQuery query = new FileQuery();
                        query.setType(StorageTypeEnum.ANSWER_ATTACHMENT.getType());
                        query.setIds(ids);
                        // 图片上传和签名需要做一个合并
                        view.getAttachment().addAll(fileService.listFiles(query));
                    } else if (questionType == SurveySchema.QuestionType.Dept) {
                        view.setDepts(ids
                                .stream().map(id -> deptService.listDept(null).stream()
                                        .filter(x -> x.getId().equals(id)).findFirst().orElseGet(DeptView::new))
                                .collect(Collectors.toList()));
                    }
                });
            }
        });
    }

    /**
     * 回填答卷创建人姓名：优先查系统用户（UserService），查不到再查项目参与人
     * （ProjectPartner，白名单导入用户答卷时的 createBy 是 partner.id）。
     *
     * @param answerView 目标答卷视图
     */
    private void setUserName(AnswerView answerView) {
        if (answerView.getCreateBy() != null) {
            UserInfo userInfo = userService.loadUserById(answerView.getCreateBy());
            if (userInfo != null) {
                answerView.setCreateByName(userInfo.getName());
                return;
            }
            ProjectPartner projectPartner = projectPartnerMapper.selectById(answerView.getCreateBy());
            if (projectPartner != null) {
                answerView.setCreateByName(projectPartner.getUserName());
                return;
            }
        }
    }

    /**
     * 查询单份答卷详情。
     *
     * @param query 支持按 id 精确查，或按 projectId+latest 查该项目最近一次答卷
     *              （latest 时默认限定当前登录用户本人，可用 createBy 覆写）
     * @return 答卷视图（含附加信息与可选排名）；未找到返回 null
     * @implNote 调用链：AnswerController.getAnswer / SurveyServiceImpl.getAnswer 相关。
     * 若 isRankEnabled=true，则按该项目所有答卷 examScore 倒序求名次
     * （scores.indexOf(examScore)+1，同分同名次）。随机问题问卷（project.survey 为 null）
     * 不补充附加信息，直接用答卷内快照。
     */
    @Override
    public AnswerView getAnswer(AnswerQuery query) {
        AnswerView answerView = null;
        if (query.getId() != null) {
            answerView = answerViewMapper.toView(getById(query.getId()));
        } else if (query.getProjectId() != null && Boolean.TRUE.equals(query.getLatest())) {
            answerView = answerViewMapper.toView(list(Wrappers.<Answer>lambdaQuery()
                    .eq(Answer::getProjectId, query.getProjectId())
                    .eq(SecurityContextUtils.isAuthenticated(), Answer::getCreateBy, SecurityContextUtils.getUserId())
                    .eq(query.getCreateBy() != null, Answer::getCreateBy, query.getCreateBy())
                    .orderByDesc(Answer::getCreateAt)).stream().findFirst().orElse(null));
        }
        if (answerView == null) {
            return null;
        }
        // 获取考试排名信息
        if (query.isRankEnabled()) {
            List<Double> scores = list(Wrappers.<Answer>lambdaQuery().select(Answer::getExamScore, Answer::getId)
                    .eq(Answer::getProjectId, answerView.getProjectId())).stream()
                    .map(x -> Optional.ofNullable(x.getExamScore()).orElse(Double.valueOf(0)))
                    .collect(Collectors.toList());
            Collections.sort(scores, Collections.reverseOrder());
            answerView.setRank(scores.indexOf(answerView.getExamScore()) + 1);
        }
        String projectId = answerView.getProjectId();
        Project project = projectMapper.selectById(projectId);
        if (project == null || project.getSurvey() == null) {
            // 随机问题问卷没有 schema
            return answerView;
        }
        FlatSurveySchemaByType schemaByType = parseSurveySchemaByType(project.getSurvey());
        setAnswerExtraInfo(answerView, schemaByType);
        return answerView;
    }

    /**
     * 保存答卷（新增或更新分派）。
     *
     * @param request 答卷请求（metaInfo 客户端信息、tempSave 状态、id 为空表示新增）
     * @return 保存后的答卷视图
     * @implNote 调用链：SurveyServiceImpl.saveAnswer → saveAnswer。
     * - 解析客户端信息（parseClientInfo）；
     * - tempSave 非空时强制置 1（提交态）；
     * - 有 id 走 updateAnswer，否则新增：使用 UUID 作为主键（公开查询引用、防暴力破解），
     *   保存后生成答题明细（generateAnswerDetails）。
     */
    @Override
    public AnswerView saveAnswer(AnswerRequest request) {
        // 公开查询修改答案时不会传元数据
        if (request.getMetaInfo() != null) {
            request.getMetaInfo().setClientInfo(parseClientInfo(request.getMetaInfo().getClientInfo()));
        }
        if (request.getTempSave() != null) {
            request.setTempSave(1);
        }
        if (StringUtils.hasText(request.getId())) {
            return updateAnswer(request);
        } else {
            Answer answer = answerViewMapper.fromRequest(request);
            // 使用 uuid 作为外部公开查询引用，防暴力破解
            answer.setId(UUID.randomUUID().toString());
            answer.setCreateAt(new Date());
            save(beforeSaveAnswer(answer));
            generateAnswerDetails(answer);
            return answerViewMapper.toView(answer);
        }
    }

    /**
     * 统计答卷数量（用于提交限制校验，如最大答题数/时间窗/登录/Cookie/IP/白名单限制）。
     *
     * @param query 条件：projectId、startTime/endTime、createBy、metaInfo like ip/cookie、
     *              answer like valueQuery
     * @return 命中条件的答卷数
     * @implNote 被 SurveyServiceImpl.validateProject / validateLoginLimit / validateCookieLimit /
     * validateIpLimit / doValidate / validateAnswer 调用。
     */
    @Override
    public long count(AnswerQuery query) {
        return count(Wrappers.<Answer>lambdaQuery().eq(Answer::getProjectId, query.getProjectId())
                .ge(query.getStartTime() != null, Answer::getCreateAt, query.getStartTime())
                .lt(query.getEndTime() != null, Answer::getCreateAt, query.getEndTime())
                .eq(query.getCreateBy() != null, Answer::getCreateBy, query.getCreateBy())
                .like(query.getIp() != null, Answer::getMetaInfo, query.getIp())
                .like(query.getValueQuery() != null, Answer::getAnswer, query.getValueQuery())
                .like(query.getCookie() != null, Answer::getMetaInfo, query.getCookie()));
    }

    /**
     * 更新答卷：保存前计算考试分值/同步关联问卷答案，更新后重新生成答题明细。
     *
     * @param request 答卷请求（须含 id）
     * @return 更新后的答卷视图
     * @implNote 调用链：AnswerServiceImpl.saveAnswer（有 id 分支）/ SurveyServiceImpl.tempSaveAnswer /
     * SurveyServiceImpl.updateProjectPartnerByAnswer。重复提交时 generateAnswerDetails 先删后插，
     * 保证明细与最终答案一致。
     */
    @Override
    public AnswerView updateAnswer(AnswerRequest request) {
        Answer answer = beforeSaveAnswer(answerViewMapper.fromRequest(request));
        updateById(answer);
        generateAnswerDetails(answer);
        return answerViewMapper.toView(answer);
    }

    /**
     * 删除答卷（逻辑删除，is_deleted=1；回收站可见，可恢复）。
     *
     * @param request 含 ids 列表
     */
    @Override
    public void deleteAnswer(AnswerRequest request) {
        super.removeByIds(request.getIds());
    }

    /**
     * 导出答卷为 Excel（异步管道流，避免大数据量一次性载入内存）。
     *
     * @param query 项目 id、答卷 id 列表、时间区间、分页参数（pageSize=0 表示全量导出）
     * @return 下载数据（xlsx 流 + 文件名）
     * @implNote 调用链：AnswerController.downloadSurvey。
     * 用 PipedOutputStream/InputStream 在线程中执行 export（ExcelExporter 写流），
     * 同时传递 Locale 保证导出文本国际化一致。文件名 = 项目名 + .xlsx。
     */
    @Override
    public DownloadData downloadSurvey(DownloadQuery query) {
        Project project = projectMapper.selectById(query.getProjectId());

        AnswerQuery answerQuery = new AnswerQuery();
        answerQuery.setProjectId(query.getProjectId());
        answerQuery.setIds(query.getIds());
        answerQuery.setStartTime(query.getStartTime());
        answerQuery.setEndTime(query.getEndTime());
        if (query.getPageSize() != 0) {
            answerQuery.setCurrent(query.getCurrent());
            answerQuery.setPageSize(query.getPageSize());
        } else {
            answerQuery.setPageSize(Integer.MAX_VALUE);
        }
        List<AnswerView> answerViews = listAnswer(answerQuery).getList();

        DownloadData download = new DownloadData();
        download.setFileName(project.getName() + ".xlsx");
        try {
            PipedOutputStream outputStream = new PipedOutputStream();
            PipedInputStream inputStream = new PipedInputStream(outputStream);
            Locale locale = LocaleContextHolder.getLocale();
            new Thread(() -> {
                Locale previousLocale = LocaleContextHolder.getLocale();
                try {
                    LocaleContextHolder.setLocale(locale);
                    export(project, answerViews, outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    LocaleContextHolder.setLocale(previousLocale);
                }
            }).start();
            download.setResource(new InputStreamResource(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }

        download.setMediaType(MediaType.parseMediaType("application/vnd.ms-excel"));
        return download;
    }

    /**
     * 下载答卷附件：指定 answerId 时下载单份答卷附件，否则把选定答卷的附件打包为 zip。
     *
     * @param query 项目 id、答卷 id 列表、answerId（单份）、时间区间、分页参数
     * @return 附件流（单文件 / zip 包）与文件名
     * @implNote 调用链：AnswerController.downloadAttachment。
     * 下载附件使用 InputStreamResource 避免整包载入内存；zip 生成见 answerAttachToZip。
     */
    @Override
    public DownloadData downloadAttachment(DownloadQuery query) {
        Project project = projectMapper.selectById(query.getProjectId());
        DownloadData downloadData = new DownloadData();
        AnswerQuery answerQuery = new AnswerQuery();
        answerQuery.setIds(query.getIds());
        answerQuery.setProjectId(query.getProjectId());
        answerQuery.setStartTime(query.getStartTime());
        answerQuery.setEndTime(query.getEndTime());
        if (query.getCurrent() != 0 && query.getPageSize() != 0) {
            answerQuery.setCurrent(query.getCurrent());
            answerQuery.setPageSize(query.getPageSize());
        } else {
            answerQuery.setPageSize(Integer.MAX_VALUE);
        }
        // 下载某个问卷答案的附件
        if (query.getAnswerId() != null) {
            answerQuery.setId(query.getAnswerId());
            return generateSurveyAttachment(project, listAnswer(answerQuery).getList().get(0));
        } else {
            // 下载所有问卷答案的附件
            downloadData.setResource(
                    new InputStreamResource(answerAttachToZip(project, listAnswer(answerQuery).getList(), query)));
            downloadData.setFileName(project.getName() + ".zip");
            downloadData.setMediaType(MediaType.parseMediaType("application/zip"));
        }
        return downloadData;
    }

    /**
     * 查询回收站中已逻辑删除的答卷。
     *
     * @param query 项目 id
     * @return 已删除答卷视图列表（含附加信息）
     * @implNote 走 AnswerMapper.selectLogicDeleted 自定义 SQL（绕过 @TableLogic 过滤）。
     */
    @Override
    public List<AnswerView> listAnswerDeleted(AnswerQuery query) {
        List<AnswerView> list = answerViewMapper.toView(getBaseMapper().selectLogicDeleted(query.getProjectId()));
        Project project = projectMapper.selectById(query.getProjectId());
        FlatSurveySchemaByType schemaByType = parseSurveySchemaByType(project.getSurvey());
        list.forEach(view -> setAnswerExtraInfo(view, schemaByType));
        return list;
    }

    /**
     * 彻底销毁答卷（物理删除，不可恢复）。
     *
     * @param request 含 ids 列表
     */
    @Override
    public void batchDestroyAnswer(AnswerRequest request) {
        this.getBaseMapper().batchDestroy(request.getIds());
    }

    /**
     * 恢复回收站中的答卷（is_deleted 置 0）。
     *
     * @param request 含 ids 列表
     */
    @Override
    public void restoreAnswer(AnswerRequest request) {
        this.getBaseMapper().restoreAnswer(request.getIds());
    }

    /**
     * Excel 批量导入答卷（行头匹配已有项目 schema 或自动创建项目 schema）。
     *
     * @param request 上传文件；projectId 指定已有项目（按表头过滤出对应列 schema），
     *                autoSchema=true 时按第一行表头自动创建新项目
     * @return 导入结果（projectId + schema），供前端继续展示
     * @implNote 调用链：AnswerController.upload。
     * 用 fastexcel 流式读取：第 1 行解析 schema（filterSchemaByRow 或 parseRow2Schema），
     * 其余行 parseRow2Answer 转 Answer 后 saveBatch 批量保存。
     */
    @Override
    @SneakyThrows
    public AnswerUploadView upload(AnswerUploadRequest request) {
        AnswerUploadView view = new AnswerUploadView();
        try (InputStream is = request.getFile().getInputStream(); ReadableWorkbook wb = new ReadableWorkbook(is)) {
            Sheet sheet = wb.getFirstSheet();
            String name = request.getFile().getOriginalFilename().substring(0,
                    request.getFile().getOriginalFilename().lastIndexOf("."));
            List<Answer> answers = new ArrayList<>();
            try (Stream<Row> rows = sheet.openStream()) {
                // 第一行作为行头
                rows.forEach(r -> {
                    int rowNum = r.getRowNum();
                    if (rowNum == 1) {
                        if (request.getProjectId() != null) {
                            ProjectView projectView = projectService.getProject(request.getProjectId());
                            view.setProjectId(projectView.getId());
                            view.setSchema(filterSchemaByRow(r, projectView.getSurvey()));
                        } else if (Boolean.TRUE.equals(request.getAutoSchema())) {
                            ProjectView projectView = parseRow2Schema(r, name, request.getParentId());
                            view.setProjectId(projectView.getId());
                            view.setSchema(projectView.getSurvey());
                        }
                    } else {
                        // 处理答案
                        answers.add(parseRow2Answer(view, r));
                    }
                });
            }
            if (answers.size() > 0) {
                saveBatch(answers);
            }

        }
        return view;
    }

    /**
     * 练习历史分页查询（顺序/随机/错题练习记录）。
     *
     * @param query 项目、练习类型（examExerciseType 非空）、状态（tempSave）条件；
     *              仅查当前登录用户本人
     * @return 分页的 ExerciseView（含题目总数/已答数/完成百分比）
     * @implNote 调用链：AnswerController.historyExercise。
     * 完成百分比 = 已答题目数 / 问卷快照题目总数 * 100（取整）。
     */
    @Override
    public PaginationResponse<ExerciseView> historyExercise(HistoryExerciseQuery query) {
        Page<Answer> page = new Page<>(query.getCurrent(), query.getPageSize());
        super.page(page, Wrappers.<Answer>lambdaQuery()
                .eq(query.getProjectId() != null, Answer::getProjectId, query.getProjectId())
                .isNotNull(Answer::getExamExerciseType)
                .eq(query.getTempSave() != null, Answer::getTempSave, query.getTempSave())
                .eq(Answer::getCreateBy, SecurityContextUtils.getUserId())
                .orderByDesc(Answer::getCreateAt));

        List<ExerciseView> list = page.getRecords().stream().map(x -> {
            ExerciseView view = new ExerciseView();
            view.setId(x.getId());
            view.setProjectName(x.getProjectId());
            view.setTempSave(x.getTempSave());
            view.setCreateAt(x.getCreateAt());
            view.setExamExerciseType(x.getExamExerciseType());
            view.setAnswerId(x.getId());
            SurveySchema schema = x.getSurvey();
            view.setProjectName(schema.getTitle());
            view.setRepoId(x.getRepoId());
            view.setPercent(0L);

            if (x.getSurvey() == null || x.getTempAnswer() == null || x.getSurvey().getChildren().isEmpty()) {
                return view;
            }
            int totalQuestions = x.getSurvey().getChildren().size();
            int answeredQuestions = x.getTempSave() == 1 ? x.getAnswer().size() : x.getTempAnswer().size();

            double percent = ((double) answeredQuestions / totalQuestions) * 100;
            view.setPercent(Math.round(percent));
            return view;
        }).collect(Collectors.toList());

        return new PaginationResponse<>(page.getTotal(), list);
    }

    /**
     * 生成单份答卷的附件下载：仅一个附件时直接返回文件流，多个附件时打包 zip。
     *
     * @param project 项目信息（含 schema）
     * @param answer  答卷视图（附件列表来自附加信息回填）
     * @return 下载数据（文件流或 zip）
     * @implNote 被 downloadAttachment 调用（answerId 分支）。
     */
    private DownloadData generateSurveyAttachment(Project project, AnswerView answer) {
        DownloadData downloadData = new DownloadData();
        List<FileView> files = answer.getAttachment();
        // 如果只有一个附件，则直接返回附件的结果
        if (files.size() == 1) {
            FileView attachment = files.get(0);
            downloadData.setFileName(attachment.getOriginalName());
            downloadData.setResource(fileService.loadFile(new FileQuery(attachment.getId())).getBody());
        } else {
            // 多个附件，压缩包
            downloadData.setResource(new InputStreamResource(
                    answerAttachToZip(project, Collections.singletonList(answer), new DownloadQuery())));
            downloadData.setFileName(answer.getId() + ".zip");
            downloadData.setMediaType(MediaType.parseMediaType("application/zip"));
        }
        return downloadData;
    }

    /**
     * 将多份答卷的附件打包成 zip 压缩包（异步管道流）。
     *
     * @param answers 答卷视图列表
     * @param query   下载参数（type=answerAttachment 时额外在 zip 内附一份 xlsx 汇总表；
     *                nameExp 为附件重命名表达式）
     * @return zip 输入流
     * @implNote 被 downloadAttachment / generateSurveyAttachment 调用。
     * 附件文件名按 parseAttachmentNameByExp 重命名（支持 #{projectId}、#{serialNum}、
     * #{sourceName}、#{questionTitle} 等变量）。
     */
    private InputStream answerAttachToZip(Project project, List<AnswerView> answers, DownloadQuery query) {
        try {
            PipedOutputStream outputStream = new PipedOutputStream();
            PipedInputStream inputStream = new PipedInputStream(outputStream);
            Locale locale = LocaleContextHolder.getLocale();
            new Thread(() -> {
                Locale previousLocale = LocaleContextHolder.getLocale();
                try (ZipOutputStream zout = new ZipOutputStream(outputStream);) {
                    LocaleContextHolder.setLocale(locale);
                    int[] serialNum = {0, 0};
                    List<SurveySchema> uploadQuestions = SchemaHelper.flatSurveySchema(project.getSurvey()).stream()
                            .filter(qSchema -> SurveySchema.QuestionType.Upload.equals(qSchema.getType()))
                            .collect(Collectors.toList());
                    answers.forEach(answer -> {
                        serialNum[1] = 0;
                        answer.getAttachment().forEach(attachment -> {
                            serialNum[0] += 1;
                            serialNum[1] += 1;
                            ByteArrayResource resource = (ByteArrayResource) fileService
                                    .loadFile(new FileQuery(attachment.getId())).getBody();
                            String parsedFileName = parseAttachmentNameByExp(answer, query.getNameExp(), attachment,
                                    serialNum, uploadQuestions);
                            ZipEntry entry = new ZipEntry(parsedFileName);
                            try {
                                zout.putNextEntry(entry);
                                zout.write(resource.getByteArray());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        });
                    });
                    // 生成表格
                    if (DownloadQuery.DownloadType.answerAttachment.equals(query.getType())) {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        export(project, answers, byteArrayOutputStream);
                        ZipEntry entry = new ZipEntry(project.getName() + ".xlsx");
                        zout.putNextEntry(entry);
                        zout.write(byteArrayOutputStream.toByteArray());
                    }
                } catch (Exception e) {
                    throw new InternalServerError("生成压缩文件失败", e);
                } finally {
                    LocaleContextHolder.setLocale(previousLocale);
                }
            }).start();
            return inputStream;
        } catch (Exception e) {
            throw new InternalServerError("生成压缩文件失败", e);
        }
    }

    /**
     * 把答卷列表导出为 Excel（ExcelExporter 写流）。
     *
     * @param project      项目信息（含 schema 与模式）
     * @param answerViews  答卷视图列表
     * @param outputStream 输出流（管道流或字节流）
     * @implNote 被 downloadSurvey / answerAttachToZip 调用。
     * 列定义与行数据均由 SchemaHelper.parseColumns / parseRowData 依据 schema 生成。
     */
    private void export(Project project, List<AnswerView> answerViews, OutputStream outputStream) {
        List<SurveySchema> schemaDataTypes = SchemaHelper.flatSurveySchema(project.getSurvey());
        int[] indexArr = {0};
        new ExcelExporter.Builder().setSheetName(project.getName()).setOutputStream(outputStream)
                .setRows(answerViews.stream().map(answer -> {
                    indexArr[0] = indexArr[0] += 1;
                    return SchemaHelper.parseRowData(answer, schemaDataTypes, indexArr[0], project.getMode());
                }).collect(Collectors.toList()))
                .setColumns(SchemaHelper.parseColumns(schemaDataTypes, project.getMode())).build().exportToStream();
    }

    /**
     * 根据命名表达式解析附件文件名（支持变量替换与后缀拼接）。
     *
     * 支持的变量（AttachmentNameVariableEnum）：
     * - #{projectId} 项目ID、#{serialNum} 全局序号、#{serialNumInAnswer} 答卷内序号
     * - #{uploadDate}/#{uploadDateTime} 上传日期（yyyyMMdd/yyyyMMddHHmmss）
     * - #{sourceName} 原文件名（去后缀）、#{questionTitle} 所在题目标题
     * - #{questionId} 其他问题变量：有 optionId 时取对应值，否则取第一个值
     *
     * @param answerView      当前答案
     * @param nameExp         附件名称表达式（可空，空则返回原始文件名）
     * @param file            当前附件
     * @param serialNum       序号（[0]=全局累计，[1]=当前答卷内累计）
     * @param uploadQuestions 问卷上传题 schema 列表
     * @return 新的附件名称
     * @implNote 被 answerAttachToZip 调用。
     */
    private String parseAttachmentNameByExp(AnswerView answerView, String nameExp, FileView file, int[] serialNum,
                                            List<SurveySchema> uploadQuestions) {
        if (StringUtils.hasText(nameExp)) {
            String fileName = nameExp;
            LinkedHashMap<String, Object> answerMap = answerView.getAnswer();
            Pattern pattern = Pattern.compile("#\\{([a-zA-Z0-9]+)\\.?([a-zA-Z0-9]*)\\}");
            Matcher matcher = pattern.matcher(nameExp);
            String exp, questionId, optionId = null;
            while (matcher.find()) {
                int count = matcher.groupCount();
                exp = matcher.group(0);
                questionId = matcher.group(1);
                if (count > 1) {
                    optionId = matcher.group(2);
                }
                String expValue = "";
                if (AttachmentNameVariableEnum.projectId.name().equals(questionId)) {
                    expValue = answerView.getProjectId();
                } else if (AttachmentNameVariableEnum.serialNum.name().equals(questionId)) {
                    expValue = serialNum[0] + "";
                } else if (AttachmentNameVariableEnum.serialNumInAnswer.name().equals(questionId)) {
                    expValue = serialNum[1] + "";
                } else if (AttachmentNameVariableEnum.uploadDate.name().equals(questionId)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    expValue = sdf.format(answerView.getCreateAt());
                } else if (AttachmentNameVariableEnum.uploadDateTime.name().equals(questionId)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    expValue = sdf.format(answerView.getCreateAt());
                } else if (AttachmentNameVariableEnum.sourceName.name().equals(questionId)) {
                    expValue = getFileNameWithoutSuffix(file.getOriginalName());
                } else if (AttachmentNameVariableEnum.questionTitle.name().equals(questionId)) {
                    for (SurveySchema uploadQuestion : uploadQuestions) {
                        Object qValue = answerView.getAnswer().get(uploadQuestion.getId());
                        if (qValue != null) {
                            boolean fileInCurrentSchema = ((Map<String, List<String>>) qValue)
                                    .get(uploadQuestion.getChildren().get(0).getId()).stream()
                                    .filter(x -> x.equals(file.getId())).findFirst().isPresent();
                            if (fileInCurrentSchema) {
                                expValue = uploadQuestion.getTitle();
                                break;
                            }
                        }
                    }
                } else {
                    // 问题变量
                    Map<String, Object> questionValue = (Map<String, Object>) answerMap.get(questionId);

                    if (questionValue != null) {
                        // 单行文本表达式只有问题id #{xxxx}
                        if (StringUtils.hasText(optionId)) {
                            expValue = questionValue.get(optionId).toString();
                        } else {
                            // 多行文本会有选项id #{ancd.a3dx}
                            expValue = questionValue.values().toArray()[0].toString();
                        }
                    }
                }

                fileName = StringUtils.replace(fileName, exp, expValue);
            }
            String suffix = getFileExtension(file.getOriginalName());
            // 返回解析之后的文件名字
            if (suffix != null) {
                return fileName + "." + suffix;
            }
            return fileName;
        } else {
            // 返回原始文件名字
            return file.getOriginalName();
        }
    }

    /**
     * 获取文件后缀
     *
     * @param fileName 文件名称
     * @return 文件后缀
     */
    private String getFileExtension(String fileName) {
        String extension = null;
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }
        return extension;
    }

    private String getFileNameWithoutSuffix(String fileName) {
        String fileNameWithoutSuffix = null;
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            fileNameWithoutSuffix = fileName.substring(0, i);
        }
        return fileNameWithoutSuffix;
    }

    /**
     * 答案保存之前的统一预处理：计算考试分值 + 更新关联问卷答案。
     *
     * @param answer 待保存的答卷实体
     * @return 处理后的答卷实体
     * @implNote 被 saveAnswer（新增）/ updateAnswer（更新）调用：
     * 1. computeExamScore：考试模式（且非练习项目）用 AnswerScoreEvaluator 计算总分与每题得分；
     * 2. updateLinkSurveyAnswer：题目配置了"关联问卷更新"时同步修改/新增关联答卷。
     */
    private Answer beforeSaveAnswer(Answer answer) {
        ProjectView project = projectService.getProject(answer.getProjectId());
        computeExamScore(answer, project);
        updateLinkSurveyAnswer(answer, project);
        return answer;
    }

    // ============================================================
    // 答题明细生成（学生答题情况分析数据基础）
    // ============================================================

    /**
     * 提交答卷时按题生成答题明细（t_answer_detail），先删后插保证重复提交幂等。
     *
     * 【触发条件】仅对已完成答卷（tempSave=1）且包含问卷快照（survey）与答案（answer）的记录生成；
     * 随机卷/练习卷等未提交（tempSave=0）的中间记录不生成明细。
     *
     * 【内部逻辑步骤】
     * 1. 按 answerId 删除该答卷已存在的明细（幂等关键：重复提交时旧明细先清掉）；
     * 2. 将答卷内问卷快照 SchemaHelper.flatSurveySchema 扁平化为题目列表，逐题构建明细：
     *    - answerId/projectId/questionId/questionType：来源与归属信息；
     *    - subject/chapter/knowledgePoint：取自题目 attribute（知识点四维快照，
     *      多值知识点 join 逗号，与 AnalysisServiceImpl 的逗号拆分约定一致）；
     *    - userAnswer：学生答案格式化（formatAnswerValue）；
     *    - isCorrect：对错判定（evaluateQuestionCorrect）；
     *    - score：优先考试模式 examInfo.questionScore 的每题分值，其次题目自带 attribute.examScore；
     * 3. 逐条 insert 入库。
     *
     * 【为什么这么写】
     * - "先删后插"而非"按 answerId 更新"：因为题目集合可能变化（随机卷/允许修改答案），
     *   直接重建明细最简单且不会残留已删除题的旧明细；
     * - 明细生成失败（catch Exception）只记日志不阻断答卷保存主流程：
     *   明细是分析侧数据，不能因为分析功能异常影响学生正常交卷。
     *
     * 【数据流向】
     * 学生提交答卷 → saveAnswer/updateAnswer → 本方法 → t_answer_detail 表
     * → AnalysisServiceImpl.knowledgePointStats/studentProfile 聚合分析。
     *
     * @param answer 已保存（含 id）的答卷实体
     */
    private void generateAnswerDetails(Answer answer) {
        if (answer == null || answer.getId() == null || answer.getAnswer() == null || answer.getSurvey() == null
                || !Integer.valueOf(1).equals(answer.getTempSave())) {
            return;
        }
        try {
            answerDetailMapper.delete(Wrappers.<AnswerDetail>lambdaQuery()
                    .eq(AnswerDetail::getAnswerId, answer.getId()));
            List<AnswerDetail> details = new ArrayList<>();
            List<SurveySchema> questions = SchemaHelper.flatSurveySchema(answer.getSurvey());
            for (SurveySchema question : questions) {
                AnswerDetail detail = new AnswerDetail();
                detail.setAnswerId(answer.getId());
                detail.setProjectId(answer.getProjectId());
                detail.setQuestionId(question.getId());
                detail.setQuestionType(question.getType() != null ? question.getType().name() : null);
                if (question.getAttribute() != null) {
                    detail.setSubject(question.getAttribute().getSubject());
                    detail.setChapter(question.getAttribute().getChapter());
                    if (!CollectionUtils.isEmpty(question.getAttribute().getKnowledgePoint())) {
                        detail.setKnowledgePoint(String.join(",", question.getAttribute().getKnowledgePoint()));
                    }
                }
                Object studentAnswer = answer.getAnswer().get(question.getId());
                detail.setUserAnswer(formatAnswerValue(studentAnswer));
                detail.setIsCorrect(evaluateQuestionCorrect(question, studentAnswer));
                // 分值：优先考试模式的每题分值，其次题目自带分值
                Double qScore = null;
                if (answer.getExamInfo() != null && answer.getExamInfo().getQuestionScore() != null) {
                    qScore = answer.getExamInfo().getQuestionScore().get(question.getId());
                }
                if (qScore == null && question.getAttribute() != null) {
                    qScore = question.getAttribute().getExamScore();
                }
                detail.setScore(qScore);
                details.add(detail);
            }
            if (!details.isEmpty()) {
                details.forEach(answerDetailMapper::insert);
            }
        } catch (Exception e) {
            // 明细生成失败不影响答卷保存主流程
            log.error("生成答题明细失败, answerId={}", answer.getId(), e);
        }
    }

    /**
     * 学生答案格式化为字符串（写入 t_answer_detail.user_answer）。
     *
     * 【格式约定】
     * - Map（原生结构 optionId->value，如选择题/签名题）：取 values 逐项 toString 后按逗号连接；
     * - Collection（如多选数组）：逐项 toString 后按逗号连接；
     * - 其他类型：直接 toString。
     *
     * @param value 学生答案原始值（来自 answer Map 中某题的 value）
     * @return 格式化后的答案字符串；null 原样返回
     * @implNote 被 generateAnswerDetails / evaluateQuestionCorrect 调用。
     */
    private String formatAnswerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).values().stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
        return String.valueOf(value);
    }

    /**
     * 判定题目对错（写入 t_answer_detail.is_correct）。
     *
     * 【返回值约定】null=无标准答案不计分、1=正确、0=错误（含未作答/空答案）。
     *
     * 【判定规则】
     * - 先 extractCorrectAnswers 提取标准答案列表；列表为 null（无标准答案）返回 null；
     * - 学生答案空（null/空白）直接返回 0；
     * - 多选题（Checkbox）：学生答案与标准答案按"选项集合相等"判定（与顺序无关，
     *   因为 splitAnswerSet 会排序去重成 Set 后 equals）；
     * - 其他题型（单选/判断/填空）：学生答案与任一标准答案做 trim 后文本等值匹配。
     *
     * 【为什么这么写】
     * 多选不能用文本相等（学生勾选顺序与标准答案顺序不一定一致），故转为集合比较；
     * 填空/判断/单选是唯一确定值，文本匹配足够且与人工阅卷习惯一致。
     *
     * @param question      题目 schema（含类型与标准答案配置）
     * @param studentAnswer 学生答案原始值
     * @return 1 正确 / 0 错误 / null 无标准答案
     * @implNote 被 generateAnswerDetails 调用。
     */
    private Integer evaluateQuestionCorrect(SurveySchema question, Object studentAnswer) {
        List<String> correctAnswers = extractCorrectAnswers(question);
        if (correctAnswers == null) {
            return null;
        }
        String student = formatAnswerValue(studentAnswer);
        if (student == null || student.trim().isEmpty()) {
            return 0;
        }
        if (SurveySchema.QuestionType.Checkbox.equals(question.getType())) {
            Set<String> studentSet = splitAnswerSet(student);
            Set<String> correctSet = new HashSet<>();
            for (String correct : correctAnswers) {
                correctSet.addAll(splitAnswerSet(correct));
            }
            return studentSet.equals(correctSet) ? 1 : 0;
        }
        for (String correct : correctAnswers) {
            if (correct != null && correct.trim().equals(student.trim())) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * 提取题目正确答案列表；无标准答案时返回 null。
     *
     * 【提取优先级】
     * 1. 整题级答案：题目 attribute.examCorrectAnswer（多选题多个答案以 \n 分隔，如 "A\nB"）；
     * 2. 选项级答案：遍历子选项，收集 attribute.examCorrectAnswer 非空的选项标题
     *    （如填空各空的答案、以选项标题作为答案的题目）。
     *
     * @param question 题目 schema
     * @return 标准答案字符串列表；无任何标准答案时返回 null
     * @implNote 被 evaluateQuestionCorrect 调用。
     */
    private List<String> extractCorrectAnswers(SurveySchema question) {
        if (question.getAttribute() != null && StringUtils.hasText(question.getAttribute().getExamCorrectAnswer())) {
            return Arrays.asList(question.getAttribute().getExamCorrectAnswer().split("\n"));
        }
        List<String> correct = new ArrayList<>();
        if (question.getChildren() != null) {
            for (SurveySchema child : question.getChildren()) {
                if (child.getAttribute() != null
                        && StringUtils.hasText(child.getAttribute().getExamCorrectAnswer())) {
                    correct.add(child.getTitle());
                }
            }
        }
        return correct.isEmpty() ? null : correct;
    }

    /**
     * 将答案字符串按逗号/换行拆分为去重集合（用于多选题无序比较）。
     *
     * @param answer 答案字符串（如 "A,B" 或 "A\nB"）
     * @return 去重后的选项集合；null/空串返回空集
     * @implNote 被 evaluateQuestionCorrect 调用；逗号与换行都作为分隔符，
     * 兼容整题级（\n 分隔）与格式化后（逗号分隔）两种来源。
     */
    private Set<String> splitAnswerSet(String answer) {
        if (answer == null || answer.isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(answer.split("[,\n]")).map(String::trim)
                .filter(x -> !x.isEmpty()).collect(Collectors.toSet());
    }

    /**
     * 计算考试总分与每题得分（考试模式专用）。
     *
     * @param answer  待保存的答卷（可能已存在旧记录，用于随机卷取回问卷快照）
     * @param project 项目视图（mode 为 exam 才计分）
     * @implNote 被 beforeSaveAnswer 调用。
     * - 仅考试模式且非练习项目（EXERCISE_PROJECT_ID）才计分；
     * - 随机抽题卷：项目 schema 不含实际题目，需根据 answer.id 从旧记录取 survey 快照作为评分依据；
     * - AnswerScoreEvaluator.eval() 计算总分写入 answer.examScore；
     * - 每题得分写入 answer.examInfo.questionScore（同时是生成答题明细分值的数据源）。
     */
    private void computeExamScore(Answer answer, ProjectView project) {
        if (project != null && ProjectModeEnum.exam.equals(project.getMode()) && answer != null
                && answer.getAnswer() != null && !ExerciseProjectTemplate.EXERCISE_PROJECT_ID.equals(project.getId())) {
            SurveySchema srcSchema = project.getSurvey();
            // 随机抽题需要根据答案获取 schema
            if (answer.getId() != null) {
                Answer existAnswer = getById(answer.getId());
                if (existAnswer != null && existAnswer.getSurvey() != null) {
                    srcSchema = existAnswer.getSurvey();
                }
            }
            AnswerScoreEvaluator evaluator = new AnswerScoreEvaluator(srcSchema, answer.getAnswer());
            answer.setExamScore(evaluator.eval());
            AnswerExamInfo examInfo = new AnswerExamInfo();
            examInfo.setQuestionScore(evaluator.getQuestionScore());
            answer.setExamInfo(examInfo);
        }
    }

    /**
     * 更新原始问卷答案：处理题目配置了"关联问卷（linkSurvey）"且 enableUpdate=true 的场景。
     *
     * 【场景】某题选项选择后可触发对另一份关联问卷的自动回填：
     * - 找到关联问卷中匹配选项值（buildLinkLikeCondition 构造 JSON 片段 LIKE）的最近一份答卷：
     *   命中则把当前答卷中 linkFields 指定字段的值写回关联答卷（修改）；
     *   未命中则创建一份新的关联答卷（复制当前答卷属性，仅填关联问题/字段，新增）。
     *
     * @param answer  当前答卷（已含答案）
     * @param project 项目视图（含 schema）
     * @implNote 被 beforeSaveAnswer 调用；用于问卷联动场景（如 A 问卷选择某选项后
     * 自动在 B 问卷登记该选项对应的信息）。
     */
    private void updateLinkSurveyAnswer(Answer answer, ProjectView project) {
        SchemaHelper.flatSurveySchema(project.getSurvey()).stream()
                .filter(qSchema -> !CollectionUtils.isEmpty(qSchema.getChildren().get(0).getLinkSurveys()))
                .forEach(qSchemaHasLinkSurvey -> {
                    Map<String, Object> qValue = (Map<String, Object>) answer.getAnswer()
                            .get(qSchemaHasLinkSurvey.getId());
                    if (qValue == null) {
                        return;
                    }
                    SurveySchema optionSchemaHasLinkAttr = qSchemaHasLinkSurvey.getChildren().get(0);
                    optionSchemaHasLinkAttr.getLinkSurveys().forEach(linkSurvey -> {
                        if (Boolean.TRUE.equals(linkSurvey.getEnableUpdate())) {
                            Object optionValue = qValue.get(optionSchemaHasLinkAttr.getId());
                            Answer linkedAnswer = this.getOne(Wrappers.<Answer>lambdaQuery()
                                    .eq(Answer::getProjectId, linkSurvey.getLinkSurveyId())
                                    .like(Answer::getAnswer,
                                            SchemaHelper.buildLinkLikeCondition(linkSurvey, optionValue))
                                    .orderByDesc(Answer::getCreateAt).last("limit 1"));
                            if (linkedAnswer != null) {
                                // 修改
                                for (SurveySchema.LinkField linkField : linkSurvey.getLinkFields()) {
                                    SchemaHelper.setQuestionValue(linkedAnswer.getAnswer(),
                                            linkField.getLinkQuestionId(), linkField.getLinkOptionId(),
                                            SchemaHelper.getQuestionValue(answer.getAnswer(),
                                                    linkField.getFillQuestionId(), linkField.getFillOptionId()));
                                }
                                this.updateById(linkedAnswer);
                            } else {
                                // 添加
                                Answer addAnswer = new Answer();
                                BeanUtils.copyProperties(answer, addAnswer, "id", "examScore", "examInfo", "answer",
                                        "projectId");
                                addAnswer.setProjectId(linkSurvey.getLinkSurveyId());
                                LinkedHashMap addAnswerMap = new LinkedHashMap();
                                addAnswer.setAnswer(addAnswerMap);
                                SchemaHelper.setQuestionValue(addAnswerMap, linkSurvey.getLinkQuestionId(),
                                        linkSurvey.getLinkOptionId(), optionValue);
                                for (SurveySchema.LinkField linkField : linkSurvey.getLinkFields()) {
                                    SchemaHelper.setQuestionValue(addAnswerMap, linkField.getLinkQuestionId(),
                                            linkField.getLinkOptionId(),
                                            SchemaHelper.getQuestionValue(answer.getAnswer(),
                                                    linkField.getFillQuestionId(), linkField.getFillOptionId()));
                                }
                                this.save(addAnswer);
                            }
                        }
                    });
                });
    }

    /**
     * Excel 导入且 autoSchema=true 时：按表头行自动创建新项目（问卷模式）。
     *
     * @param row      表头行（每列文本作为一道填空题的题干）
     * @param name     项目名（取自文件名去后缀）
     * @param parentId 父目录 id
     * @return 新建的项目视图（含自动生成的 schema）
     * @implNote 被 upload 调用：createSurveyFromExcelRowHeader 生成 FillBlank 题目列表，
     * 组装 ProjectRequest 后走 projectService.addProject 落库。
     */
    private ProjectView parseRow2Schema(Row row, String name, String parentId) {
        // 处理行头自动生成 schema
        SurveySchema schema = createSurveyFromExcelRowHeader(row);
        schema.setTitle(name);
        ProjectRequest projectRequest = new ProjectRequest();
        projectRequest.setSurvey(schema);
        projectRequest.setName(name);
        projectRequest.setMode(ProjectModeEnum.survey);
        projectRequest.setParentId(parentId);
        projectRequest.setSetting(ProjectSetting.builder().mode(ProjectModeEnum.survey).status(1).build());
        return projectService.addProject(projectRequest);
    }

    /**
     * 把 Excel 表头行转换为问卷 schema（每列生成一道填空题，NanoId 生成题目/选项 id）。
     *
     * @param row 表头行
     * @return 新建的问卷 schema（title 为空，由调用方设置）
     * @implNote 被 parseRow2Schema 调用。
     */
    private SurveySchema createSurveyFromExcelRowHeader(Row row) {
        Set<String> ids = new HashSet<>();
        SurveySchema schema = SurveySchema.builder()
                .children(row.stream()
                        .map(cell -> SurveySchema.builder().id(NanoIdUtils.randomNanoId(4, ids))
                                .type(SurveySchema.QuestionType.FillBlank).title(cell.getText())
                                .children(Collections.singletonList(
                                        SurveySchema.builder().id(NanoIdUtils.randomNanoId(4, ids)).build()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
        return schema;
    }

    /**
     * Excel 导入且指定了已有项目时：按表头匹配项目 schema 中的题目，
     * 生成与列顺序一致的导入用 schema（表头不匹配的列给空 schema 占位）。
     *
     * @param row    表头行
     * @param schema 项目原始 schema
     * @return 过滤后的导入 schema
     * @implNote 被 upload 调用；匹配键是题目标题（cell.getText()）。
     */
    private SurveySchema filterSchemaByRow(Row row, SurveySchema schema) {
        List<SurveySchema> flatSurveySchema = SchemaHelper.flatSurveySchema(schema);
        return SurveySchema.builder().id(schema.getId()).children(row.stream().map(cell -> {
            String title = cell.getText();
            return flatSurveySchema.stream().filter(x -> x.getTitle().equals(title)).findFirst()
                    .orElse(SurveySchema.builder().build());
        }).collect(Collectors.toList())).build();
    }

    /**
     * 把 Excel 数据行转换为 Answer 实体（按导入 schema 的列顺序逐列取值）。
     *
     * @param view 导入上下文（projectId + schema）
     * @param r    数据行
     * @return 答案实体（answer Map 结构 {questionId: {optionId: 单元格文本}}）
     * @implNote 被 upload 调用；单行文本题型（首个子选项）直接取单元格文本作为值。
     */
    private Answer parseRow2Answer(AnswerUploadView view, Row r) {
        Answer answer = new Answer();
        answer.setProjectId(view.getProjectId());
        LinkedHashMap<String, Map<String, String>> answerMap = new LinkedHashMap();
        int i = 0;
        for (SurveySchema questionSchema : view.getSchema().getChildren()) {
            String cellValue = r.getCellText(i);
            String questionId = questionSchema.getId();
            if (questionSchema.getChildren() == null || questionSchema.getChildren().size() == 0) {
                continue;
            }
            String optionId = questionSchema.getChildren().get(0).getId();
            if (cellValue != null) {
                Map<String, String> optionValue = new LinkedHashMap<>();
                optionValue.put(optionId, cellValue);
                answerMap.put(questionId, optionValue);
            }
            i++;
        }
        answer.setAnswer(answerMap);
        return answer;
    }

}
