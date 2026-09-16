package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.TagCategoryEnum;
import cn.wisestar.server.core.constant.ErrorCode;
import cn.wisestar.server.core.constant.ProjectModeEnum;
import cn.wisestar.server.core.exception.ErrorCodeException;
import cn.wisestar.server.core.uitls.AnswerScoreEvaluator;
import cn.wisestar.server.core.uitls.RepoTemplateExcelParseHelper;
import cn.wisestar.server.core.uitls.RepoTemplateI18n;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.core.uitls.ExcelExporter;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.domain.dto.RepoBindLocationView.RepoNodeBinding;
import cn.wisestar.server.domain.mapper.RepoViewMapper;
import cn.wisestar.server.domain.mapper.UserBookViewMapper;
import cn.wisestar.server.domain.model.*;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.ChapterRepoMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.KnowledgePointQuestionMapper;
import cn.wisestar.server.mapper.RepoMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.SectionRepoMapper;
import cn.wisestar.server.mapper.SubjectMapper;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Arrays;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;

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
 * 4. 题库导出增强：exportRepoQuestions（标准单表 29 列导出：学科/题型/章节/小节/知识点/题目/
 *    选项A-H/难易程度/正确答案1-12/解析/标签），配套辅助方法 standardRowOf / answerCellsOf /
 *    queryQuestionsForExport / buildGuideSheet；导入模板与导出共用列结构
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
     * 学科表：Excel 标准模板导入时按「学科」列名称匹配已有学科。
     */
    private final SubjectMapper subjectMapper;

    /**
     * 章节表：Excel 标准模板导入时按「学科 + 章节」匹配已有章节。
     */
    private final ChapterMapper chapterMapper;

    /**
     * 小节表：Excel 标准模板导入时用于定位章节下的知识点集合。
     */
    private final SectionMapper sectionMapper;

    /**
     * 知识点表：Excel 标准模板导入时校验「知识点」列命中章节下已有知识点。
     */
    private final KnowledgePointMapper knowledgePointMapper;

    /**
     * 章节-练习绑定表（习题列表页绑定位置反查/节点题聚合用）。
     */
    private final ChapterRepoMapper chapterRepoMapper;

    /**
     * 小节-练习绑定表（习题列表页绑定位置反查/节点题聚合用）。
     */
    private final SectionRepoMapper sectionRepoMapper;

    /**
     * 知识点-题目绑定表（习题列表页知识点节点题聚合用）。
     */
    private final KnowledgePointQuestionMapper knowledgePointQuestionMapper;

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
                Wrappers.<Repo>lambdaQuery()
                        .eq(StringUtils.hasText(query.getId()), Repo::getId, query.getId())
                        .like(isNotBlank(query.getName()), Repo::getName, query.getName())
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
     * <p>注意：跳过条件必须写成 <code>repo_id IS NULL OR repo_id &lt;&gt; 目标</code>——
     * 若只写 <code>repo_id &lt;&gt; 目标</code>，SQL 三值逻辑会把 repo_id 为 NULL
     * （尚未绑定任何题库）的题目一并排除，导致题目管理中的新题目永远绑定不进来。</p>
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
                .and(w -> w.isNull(Template::getRepoId).or().ne(Template::getRepoId, request.getRepoId()))
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
     * <p>嗅探首 sheet 表头：命中标准单表模板（含「学科」「题型」表头）时走标准导入
     * （逐行校验学科/章节/知识点归属与答案合法性，行级错误整体中止）；否则向后兼容
     * 旧的多题型分 sheet 模板（RepoTemplateExcelParseHelper）。</p>
     */
    @SneakyThrows
    private List<TemplateRequest> parseExcelToTemplate(MultipartFile file) {
        if (isStandardTemplate(file)) {
            return parseStandardQuestions(file);
        }
        return new RepoTemplateExcelParseHelper(file).parse();
    }

    /**
     * 嗅探上传文件是否为标准单表模板（读取首个 sheet 首行表头是否含「学科」「题型」）。
     */
    private boolean isStandardTemplate(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream(); ReadableWorkbook wb = new ReadableWorkbook(is)) {
            java.util.Optional<org.dhatim.fastexcel.reader.Sheet> first = wb.getSheets().findFirst();
            if (!first.isPresent()) {
                return false;
            }
            try (Stream<Row> rows = first.get().openStream()) {
                Row header = rows.findFirst().orElse(null);
                if (header == null) {
                    return false;
                }
                boolean subject = false;
                boolean type = false;
                for (int c = 0; c < 15; c++) {
                    String t = cellText(header, c);
                    if (t.contains("学科")) {
                        subject = true;
                    }
                    if (t.contains("题型")) {
                        type = true;
                    }
                }
                return subject && type;
            }
        }
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
     * 导出题库题目为 Excel（标准单表模板，29 列单 sheet）。
     *
     * <p>列结构与「题目管理 → 导入模板」完全一致：学科/题型/章节/知识点/题目/选项A~H/
     * 难易程度/正确答案1~12/解析/标签；题型仅含判断/单选/单项填空/多选/多项填空
     * （Textarea 简答题随题型收窄不再导出）。</p>
     *
     * <p>当筛选结果为空（含未传 repoId 下载模板场景）时：第一个 sheet 只输出表头，
     * 并附加「填写说明」sheet 展示列规则与格式示例，可直接作为导入模板使用。</p>
     *
     * 【数据流向】
     * RepoApi.exportRepoQuestions → exportRepoQuestions → queryQuestionsForExport（t_template）
     * → standardRowOf 逐题装配 29 列 → fastexcel 写流 → 浏览器下载 xlsx。
     *
     * @param request 含题库 id（可空：空则导出全部题目）及题目维度筛选条件
     *        （name/questionType/subject/chapter/section/knowledgePoint/difficulty）
     */
    @Override
    @SneakyThrows
    public void exportRepoQuestions(RepoRequest request) {
        Repo repo = getById(request.getId());
        String fileName = (repo != null && repo.getName() != null ? repo.getName() : "题库") + ".xlsx";

        ContextHelper.getCurrentHttpResponse()
                .setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ContextHelper.getCurrentHttpResponse().setHeader("Content-Disposition", "attachment; filename="
                + java.net.URLEncoder.encode(fileName, "UTF-8"));

        List<Template> questions = queryQuestionsForExport(request);

        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            org.dhatim.fastexcel.Workbook workbook = new org.dhatim.fastexcel.Workbook(baos,
                    RepoTemplateI18n.workbookName(), "1.0");

            org.dhatim.fastexcel.Worksheet sheet = workbook.newWorksheet("题目列表");
            writeHeaderRow(sheet);
            int rowIndex = 1;
            for (Template template : questions) {
                List<String> row = standardRowOf(template);
                for (int c = 0; c < row.size(); c++) {
                    sheet.value(rowIndex, c, orEmpty(row.get(c)));
                }
                rowIndex++;
            }
            if (questions.isEmpty()) {
                // 空结果（模板下载/空导出）：附加填写说明 sheet，便于按新模板格式录入
                buildGuideSheet(workbook);
            }
            workbook.finish();
            ContextHelper.getCurrentHttpResponse().getOutputStream().write(baos.toByteArray());
        }
    }

    /**
     * 下载题目导入模板（标准单表 29 列空模板 + 「填写说明」sheet）。
     *
     * <p>仅输出表头与说明页，不含任何题目数据，供「题目管理 → 导入 → 下载模板」使用；
     * 与 exportRepoQuestions（无 repoId 导出全量题目）语义区分。</p>
     *
     * @implNote 调用链：RepoApi.downloadImportTemplate → downloadImportTemplate →
     * writeHeaderRow + buildGuideSheet 写流。
     */
    @Override
    @SneakyThrows
    public void downloadImportTemplate() {
        String fileName = "题目导入模板.xlsx";
        ContextHelper.getCurrentHttpResponse()
                .setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ContextHelper.getCurrentHttpResponse().setHeader("Content-Disposition", "attachment; filename="
                + java.net.URLEncoder.encode(fileName, "UTF-8"));
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            org.dhatim.fastexcel.Workbook workbook = new org.dhatim.fastexcel.Workbook(baos,
                    RepoTemplateI18n.workbookName(), "1.0");
            org.dhatim.fastexcel.Worksheet sheet = workbook.newWorksheet("题目列表");
            writeHeaderRow(sheet);
            buildGuideSheet(workbook);
            workbook.finish();
            ContextHelper.getCurrentHttpResponse().getOutputStream().write(baos.toByteArray());
        }
    }

    // ============================================================
    // 标准单表模板（导入/导出共用的列结构、行装配、答案拆分）
    // ============================================================

    /**
     * 标准单表模板列头（29 列，导入导出共用，顺序与用户约定模板一致）。
     */
    private static final List<String> STANDARD_HEADERS = Collections.unmodifiableList(Arrays.asList(
            "学科", "题型", "章节", "小节", "知识点", "题目", "选项A", "选项B", "选项C", "选项D", "选项E", "选项F", "选项G", "选项H",
            "难易程度", "正确答案1", "正确答案2", "正确答案3", "正确答案4", "正确答案5", "正确答案6", "正确答案7", "正确答案8",
            "正确答案9", "正确答案10", "正确答案11", "正确答案12", "解析", "标签"));

    /**
     * 各列导出列宽（与 STANDARD_HEADERS 一一对应）。
     */
    private static final int[] STANDARD_WIDTHS = { 14, 12, 16, 14, 22, 50, 12, 12, 12, 12, 12, 12, 12, 12, 10, 12, 12,
            12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 42, 24 };

    /**
     * 「填写说明」sheet 引导文案（下载模板时附在第二个 sheet，不参与导入解析）。
     */
    private static final List<String> GUIDE_LINES = Collections.unmodifiableList(Arrays.asList(
            "填写说明（本页仅作格式指引，导入时只解析第一个 sheet「题目列表」）：",
            "1. 学科：填写系统内已存在的学科名称（例如：数学），不会自动新建学科；",
            "2. 题型：仅支持 判断 / 单选 / 单项填空 / 多选 / 多项填空 五种；",
            "3. 章节：填写系统内该学科下已存在的章节名称，需与学科配套；",
            "4. 小节：填写该章节下已存在的小节名称（可留空），需与学科/章节配套；",
            "5. 知识点：整格按一个知识点名匹配（名称含顿号也可，如「单价、数量、总价的应用」）；",
            "   整格未匹配到时才按顿号拆分多个知识点校验；填写了小节时须属于该小节，",
            "   小节留空时须属于该章节下的已有知识点；",
            "6. 题目：题干文本，必填；",
            "7. 选项A~H：单选/多选填写选项内容，其他题型留空；",
            "8. 难易程度：简单 / 中等 / 困难，可留空；",
            "9. 正确答案1~12：判断填“正确”或“错误”；单选填选项字母 A~H；",
            "   多选依次填 1~12 个字母；单项填空填答案文本；多项填空按空位顺序依次填 1~12 个答案（一道题最多 12 个空）；",
            "10. 解析：题目解析，可留空；",
            "11. 标签：可填多个，用逗号或顿号分隔，可留空；",
            "12. 任一行的学科/章节/小节/知识点/选项/答案校验不通过会中止整次导入并提示行号；",
            "13. 富内容写法（题干/选项/解析列均可使用，学员端会自动渲染）：",
            "    图片：![图片说明](图片URL)，URL 须为公网可访问的 http(s) 地址；",
            "    行内公式：$x^2$、$\\frac{a}{b}$；独立公式：$$...$$；化学式：$\\ce{H2O}$；",
            "    加粗：**重点内容**；单元格内换行：按 Alt+Enter 输入换行。",
            "",
            "下方为格式示例（位于本说明页，不会参与导入）："));

    /**
     * 查询导出的题目集合（筛选与题目管理页一致，题型限定五种业务题型）。
     *
     * @param request 导出筛选条件
     * @return 命中题目列表（按题型 + 创建时间升序）
     * @implNote 知识点按 JSON 数组文本做内存二次过滤（与旧导出一致）。
     */
    private List<Template> queryQuestionsForExport(RepoRequest request) {
        List<Template> questions = templateService.list(Wrappers.<Template>lambdaQuery()
                .eq(request.getId() != null, Template::getRepoId, request.getId())
                .like(request.getName() != null && !request.getName().isEmpty(), Template::getName, request.getName())
                .eq(request.getQuestionType() != null && !request.getQuestionType().isEmpty(),
                        Template::getQuestionType, request.getQuestionType())
                .eq(request.getSubject() != null && !request.getSubject().isEmpty(), Template::getSubject,
                        request.getSubject())
                .eq(request.getChapter() != null && !request.getChapter().isEmpty(), Template::getChapter,
                        request.getChapter())
                .eq(request.getSection() != null && !request.getSection().isEmpty(), Template::getSection,
                        request.getSection())
                .eq(request.getDifficulty() != null && !request.getDifficulty().isEmpty(), Template::getDifficulty,
                        request.getDifficulty())
                .in(Template::getQuestionType, Arrays.asList(
                        SurveySchema.QuestionType.Radio,
                        SurveySchema.QuestionType.Checkbox,
                        SurveySchema.QuestionType.Judge,
                        SurveySchema.QuestionType.FillBlank,
                        SurveySchema.QuestionType.MultipleBlank))
                .orderByAsc(Template::getQuestionType, Template::getCreateAt));
        if (request.getKnowledgePoint() != null && !request.getKnowledgePoint().isEmpty()) {
            String kp = request.getKnowledgePoint();
            questions = questions.stream()
                    .filter(t -> t.getKnowledgePoint() != null
                            && Arrays.asList(t.getKnowledgePoint()).stream()
                                    .anyMatch(x -> x != null && x.contains(kp)))
                    .collect(Collectors.toList());
        }
        if (request.getTag() != null && request.getTag().length > 0) {
            List<String> wantedTags = Arrays.asList(request.getTag());
            questions = questions.stream()
                    .filter(t -> t.getTag() != null
                            && Arrays.stream(t.getTag()).anyMatch(wantedTags::contains))
                    .collect(Collectors.toList());
        }
        return questions;
    }

    /**
     * 输出标准模板表头行并设置列宽。
     *
     * @param sheet 目标 sheet（第 0 行）
     */
    private void writeHeaderRow(org.dhatim.fastexcel.Worksheet sheet) {
        for (int c = 0; c < STANDARD_HEADERS.size(); c++) {
            sheet.value(0, c, STANDARD_HEADERS.get(c));
        }
        for (int c = 0; c < STANDARD_WIDTHS.length; c++) {
            try {
                sheet.width(c, STANDARD_WIDTHS[c]);
            } catch (RuntimeException ignore) {
                // 列宽设置失败不影响模板可用
            }
        }
    }

    /**
     * 在 workbook 上追加「填写说明」sheet（仅模板下载/空导出时附加）。
     *
     * @param workbook 目标工作簿
     */
    private void buildGuideSheet(org.dhatim.fastexcel.Workbook workbook) {
        org.dhatim.fastexcel.Worksheet guide = workbook.newWorksheet("填写说明");
        int r = 0;
        for (String line : GUIDE_LINES) {
            guide.value(r++, 0, line);
        }
        r++;
        for (int c = 0; c < STANDARD_HEADERS.size(); c++) {
            guide.value(r, c, STANDARD_HEADERS.get(c));
        }
        r++;
        for (List<String> demo : demoRows()) {
            for (int c = 0; c < demo.size(); c++) {
                guide.value(r, c, orEmpty(demo.get(c)));
            }
            r++;
        }
    }

    /**
     * 示例数据行（仅供格式展示，位于填写说明 sheet，不参与导入解析）。
     */
    private static List<List<String>> demoRows() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(Arrays.asList("数学", "判断", "有理数", "数轴", "相反数", "示例：0 的相反数是 0。",
                "", "", "", "", "", "", "", "",
                "简单", "正确", "", "", "", "", "", "", "", "", "", "", "",
                "示例解析：0 的相反数是其本身。", "示例标签"));
        rows.add(Arrays.asList("数学", "多项填空", "整式加减", "去括号", "合并同类项", "示例：2x+3x 与 5y-2y 的结果分别是？",
                "", "", "", "", "", "", "", "",
                "简单", "5x", "3y", "", "", "", "", "", "", "", "", "",
                "示例解析：合并同类项系数相加减。", "示例标签"));
        rows.add(Arrays.asList("数学", "单选", "有理数", "数轴", "绝对值",
                "示例：如图，点 A 表示的数是 $x$，且 $|x|=3$，则 $x$ 的值是？![数轴示意图](https://example.com/number-axis.png)",
                "3", "-3", "$\\pm 3$", "以上都不对", "", "", "", "",
                "中等", "C", "", "", "", "", "", "", "", "", "", "", "",
                "示例解析：由 $|x|=3$ 得 $x=3$ 或 $x=-3$，故 $x=\\pm 3$。", "示例标签"));
        return rows;
    }

    /**
     * 题型中文标签（标准模板「题型」列，五种业务题型）。
     *
     * @param type 题型枚举
     * @return 判断 / 单选 / 单项填空 / 多选 / 多项填空
     */
    private static String standardTypeLabel(SurveySchema.QuestionType type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case Judge:
                return "判断";
            case Radio:
                return "单选";
            case FillBlank:
                return "单项填空";
            case Checkbox:
                return "多选";
            case MultipleBlank:
                return "多项填空";
            case Textarea:
                return "简答题";
            default:
                return type.name();
        }
    }

    /**
     * 难度中文化（标准模板「难易程度」列）。
     *
     * @param difficulty easy/medium/hard 或已有中文字样
     * @return 简单 / 中等 / 困难；无法识别返回原文
     */
    private static String difficultyLabelOf(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return "";
        }
        switch (difficulty.trim().toLowerCase()) {
            case "easy":
                return "简单";
            case "medium":
                return "中等";
            case "hard":
                return "困难";
            default:
                return difficulty.trim();
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String firstText(String... values) {
        if (values != null) {
            for (String v : values) {
                if (StringUtils.hasText(v)) {
                    return v;
                }
            }
        }
        return null;
    }

    /**
     * 单表导出的一行（29 列，顺序与 STANDARD_HEADERS 一致）。
     *
     * @param template 题目实体
     * @return 导出行数据
     * @implNote 学科/章节/小节/知识点/难度兼容顶层新格式与 attribute 旧快照两种存储。
     */
    private List<String> standardRowOf(Template template) {
        SurveySchema schema = template.getTemplate();
        SurveySchema.Attribute attr = schema != null ? schema.getAttribute() : null;
        String subject = firstText(template.getSubject(), attr != null ? attr.getSubject() : null);
        String chapter = firstText(template.getChapter(), attr != null ? attr.getChapter() : null);
        String section = firstText(template.getSection(), attr != null ? attr.getSection() : null);
        String difficulty = firstText(template.getDifficulty(), attr != null ? attr.getDifficulty() : null);
        String[] topKps = template.getKnowledgePoint();
        List<String> kpList = topKps != null && topKps.length > 0 ? Arrays.asList(topKps)
                : (attr != null && attr.getKnowledgePoint() != null && !attr.getKnowledgePoint().isEmpty()
                        ? new ArrayList<>(attr.getKnowledgePoint()) : null);
        List<SurveySchema> children = schema != null && schema.getChildren() != null ? schema.getChildren()
                : Collections.emptyList();

        List<String> row = new ArrayList<>(STANDARD_HEADERS.size());
        row.add(orEmpty(subject));
        row.add(standardTypeLabel(template.getQuestionType()));
        row.add(orEmpty(chapter));
        row.add(orEmpty(section));
        row.add(kpList == null ? "" : String.join("、", kpList));
        row.add(orEmpty(schema != null ? schema.getTitle() : null));
        for (int i = 0; i < 8; i++) {
            row.add(i < children.size() && children.get(i) != null ? orEmpty(children.get(i).getTitle()) : "");
        }
        row.add(difficultyLabelOf(difficulty));
        List<String> answers = answerCellsOf(template);
        for (int i = 0; i < MAX_ANSWER_CELLS; i++) {
            row.add(i < answers.size() ? answers.get(i) : "");
        }
        row.add(attr != null ? orEmpty(attr.getExamAnalysis()) : "");
        row.add(template.getTag() != null && template.getTag().length > 0
                ? String.join("、", template.getTag()) : "");
        return row;
    }

    /**
     * 提取题目正确答案，拆分到模板「正确答案1~12」列（最多 12 个单元）。
     *
     * <p>各题型取法：</p>
     * <ul>
     *   <li>判断：整题级或选项级答案统一归一为“正确/错误”，单个单元；</li>
     *   <li>单选/多选：优先整题级答案（兼容 “A,B”/“B\nC”/选项文本/选项 id），逐项映射为
     *       选项字母；无整题答案时按选项级答案标记顺序转字母；</li>
     *   <li>单项填空/多项填空：整题级答案按 | 拆空位；无整题答案时取子空答案文本。</li>
     * </ul>
     *
     * @param template 题目实体
     * @return 答案单元列表（不超过 12 个）
     */
    private List<String> answerCellsOf(Template template) {
        List<String> cells = new ArrayList<>();
        SurveySchema schema = template.getTemplate();
        if (schema == null) {
            return cells;
        }
        SurveySchema.QuestionType type = template.getQuestionType() != null ? template.getQuestionType()
                : schema.getType();
        SurveySchema.Attribute attr = schema.getAttribute();
        String top = attr != null ? attr.getExamCorrectAnswer() : null;
        List<SurveySchema> children = schema.getChildren() != null ? schema.getChildren() : Collections.emptyList();

        if (type == SurveySchema.QuestionType.Judge) {
            String value = StringUtils.hasText(top) ? top : checkedChildText(children, "");
            cells.add(normalizeJudgeAnswer(value));
            return cells;
        }
        if (type == SurveySchema.QuestionType.Radio || type == SurveySchema.QuestionType.Checkbox) {
            if (StringUtils.hasText(top)) {
                String trimmed = top.trim();
                Integer direct = indexOfOption(children, trimmed);
                if (direct != null) {
                    cells.add(letterOf(direct));
                    return cells;
                }
                for (String token : splitAnswers(trimmed)) {
                    Integer idx = indexOfOption(children, token);
                    cells.add(idx != null ? letterOf(idx) : token);
                }
            } else {
                for (int i = 0; i < children.size() && cells.size() < MAX_ANSWER_CELLS; i++) {
                    SurveySchema option = children.get(i);
                    if (option != null && option.getAttribute() != null
                            && StringUtils.hasText(option.getAttribute().getExamCorrectAnswer())) {
                        cells.add(letterOf(i));
                    }
                }
            }
            return cells.size() > MAX_ANSWER_CELLS ? cells.subList(0, MAX_ANSWER_CELLS) : cells;
        }
        // FillBlank / MultipleBlank：整题级答案按 | 拆空位
        if (StringUtils.hasText(top)) {
            for (String piece : top.split("\\|")) {
                cells.add(piece.trim());
            }
        } else {
            for (SurveySchema child : children) {
                if (child != null && child.getAttribute() != null
                        && StringUtils.hasText(child.getAttribute().getExamCorrectAnswer())) {
                    cells.add(child.getAttribute().getExamCorrectAnswer().trim());
                }
            }
        }
        return cells.size() > MAX_ANSWER_CELLS ? cells.subList(0, MAX_ANSWER_CELLS) : cells;
    }

    /**
     * 取首个带答案标记的子项文本（判断题选项级答案场景）。
     */
    private String checkedChildText(List<SurveySchema> children, String fallback) {
        for (SurveySchema child : children) {
            if (child != null && child.getAttribute() != null
                    && StringUtils.hasText(child.getAttribute().getExamCorrectAnswer())) {
                if (StringUtils.hasText(child.getTitle())) {
                    return child.getTitle();
                }
                return child.getAttribute().getExamCorrectAnswer();
            }
        }
        return fallback;
    }

    /**
     * 判断题答案归一化（模板「正确答案」列输出 正确/错误）。
     */
    private static String normalizeJudgeAnswer(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String v = value.trim().toLowerCase();
        if ("正确".equals(v) || "对".equals(v) || "true".equals(v) || "1".equals(v) || "t".equals(v)
                || "√".equals(v) || "y".equals(v)) {
            return "正确";
        }
        if ("错误".equals(v) || "错".equals(v) || "false".equals(v) || "0".equals(v) || "f".equals(v)
                || "×".equals(v) || "n".equals(v)) {
            return "错误";
        }
        return value.trim();
    }

    private static String letterOf(int index) {
        return String.valueOf((char) ('A' + index));
    }

    /**
     * 在选项中定位答案 token 对应下标（支持：选项字母 A~H、选项 id、选项标题精确匹配）。
     *
     * @return 命中下标；未命中返回 null
     */
    private Integer indexOfOption(List<SurveySchema> children, String token) {
        if (children == null || token == null) {
            return null;
        }
        String t = token.trim();
        if (t.length() == 1 && Character.isLetter(t.charAt(0))) {
            int letterIndex = Character.toUpperCase(t.charAt(0)) - 'A';
            if (letterIndex >= 0 && letterIndex < children.size()) {
                return letterIndex;
            }
        }
        for (int i = 0; i < children.size(); i++) {
            SurveySchema option = children.get(i);
            if (option == null) {
                continue;
            }
            if (option.getId() != null && t.equalsIgnoreCase(option.getId())) {
                return i;
            }
            if (option.getTitle() != null && t.equals(option.getTitle().trim())) {
                return i;
            }
        }
        return null;
    }

    /**
     * 拆分答案文本为多个 token（兼容 
 与中英文逗号/顿号/分号/竖线分隔）。
     */
    private static List<String> splitAnswers(String text) {
        List<String> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        for (String t : text.split("[\n,，、|;；]")) {
            String v = t.trim();
            if (!v.isEmpty()) {
                result.add(v);
            }
        }
        return result;
    }


    // ============================================================
    // 标准单表模板导入解析（与导出共用列结构，逐行校验后整体入库）
    // ============================================================

    /** 标准模板「学科」列序号（0 起，与 STANDARD_HEADERS 对齐）。 */
    private static final int COL_SUBJECT = 0;
    /** 标准模板「题型」列序号。 */
    private static final int COL_TYPE = 1;
    /** 标准模板「章节」列序号。 */
    private static final int COL_CHAPTER = 2;
    /** 标准模板「小节」列序号（知识结构：学科→章节→小节→知识点）。 */
    private static final int COL_SECTION = 3;
    /** 标准模板「知识点」列序号。 */
    private static final int COL_KNOWLEDGE_POINT = 4;
    /** 标准模板「题目」列序号。 */
    private static final int COL_TITLE = 5;
    /** 标准模板选项列起始序号（选项A）。 */
    private static final int COL_OPTION_START = 6;
    /** 标准模板选项列结束序号（选项H）。 */
    private static final int COL_OPTION_END = 13;
    /** 标准模板「难易程度」列序号。 */
    private static final int COL_DIFFICULTY = 14;
    /** 标准模板答案列起始序号（正确答案1）。 */
    private static final int COL_ANSWER_START = 15;
    /** 标准模板答案单元上限（正确答案1~12）：一道多项填空最多 12 个空位。 */
    private static final int MAX_ANSWER_CELLS = 12;
    /** 标准模板答案列结束序号（正确答案12）。 */
    private static final int COL_ANSWER_END = COL_ANSWER_START + MAX_ANSWER_CELLS - 1;
    /** 标准模板「解析」列序号。 */
    private static final int COL_ANALYSIS = COL_ANSWER_END + 1;
    /** 标准模板「标签」列序号。 */
    private static final int COL_TAGS = COL_ANALYSIS + 1;

    /** 单次导入最多展示的行级错误条数。 */
    private static final int MAX_SHOWN_ERRORS = 20;

    /**
     * 解析标准单表模板（首 sheet，29 列）。
     *
     * <p>逐行校验：学科/章节/小节按名称匹配系统已有体系（不自动新建）；知识点列整格优先
     * 按一个知识点名匹配（名称可含顿号），整格未命中才按顿号等拆分多个知识点逐 token 校验，
     * 填了小节时须属于该小节、小节留空时须属于该章节下已有知识点；选项与答案按题型
     * 规则校验。任一行有错即整体中止，抛错携带行级明细。全部通过才返回题目列表供
     * batchAddRepoTemplate 落库。</p>
     *
     * @param file 上传文件（已确认首 sheet 表头为标准模板）
     * @return 模板请求列表
     */
    @SneakyThrows
    private List<TemplateRequest> parseStandardQuestions(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        List<TemplateRequest> result = new ArrayList<>();
        // 知识点归属缓存：key 为 "c:"+章节id 或 "s:"+小节id（填了小节按小节校验，否则按章节全集校验）
        Map<String, Set<String>> kpCache = new HashMap<>();
        try (InputStream is = file.getInputStream(); ReadableWorkbook wb = new ReadableWorkbook(is)) {
            java.util.Optional<org.dhatim.fastexcel.reader.Sheet> first = wb.getSheets().findFirst();
            if (first.isPresent()) {
                try (Stream<Row> rows = first.get().openStream()) {
                    Iterator<Row> it = rows.iterator();
                    boolean headerPassed = false;
                    while (it.hasNext()) {
                        Row r = it.next();
                        if (!headerPassed) {
                            headerPassed = true;
                            continue;
                        }
                        parseStandardRow(r, errors, result, kpCache);
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            List<String> shown = errors.size() > MAX_SHOWN_ERRORS
                    ? new ArrayList<>(errors.subList(0, MAX_SHOWN_ERRORS)) : errors;
            String summary = String.join("；", shown)
                    + (errors.size() > MAX_SHOWN_ERRORS
                            ? "；……等共" + errors.size() + "处问题（已中止导入）" : "（已中止导入）");
            throw new ErrorCodeException(ErrorCode.FileParseError, "导入校验失败，请修正模板后重新上传。" + summary);
        }
        return result;
    }

    /**
     * 解析标准模板的一行数据（行级校验，错误累计进 errors）。
     */
    private void parseStandardRow(Row r, List<String> errors, List<TemplateRequest> result,
            Map<String, Set<String>> kpCache) {
        int rowNum = r.getRowNum();
        String subjectText = cellText(r, COL_SUBJECT);
        String title = cellText(r, COL_TITLE);
        // 学科与题目均为空 → 视为空行跳过
        if (!StringUtils.hasText(subjectText) && !StringUtils.hasText(title)) {
            return;
        }
        String typeText = cellText(r, COL_TYPE);
        String chapterText = cellText(r, COL_CHAPTER);
        String sectionText = cellText(r, COL_SECTION);
        String kpText = cellText(r, COL_KNOWLEDGE_POINT);
        String difficultyText = cellText(r, COL_DIFFICULTY);
        String analysis = cellText(r, COL_ANALYSIS);
        String tagsText = cellText(r, COL_TAGS);

        List<String> rowErrors = new ArrayList<>();

        // 学科（名称或编码匹配系统已有学科，不自动新建）
        Subject subject = null;
        if (!StringUtils.hasText(subjectText)) {
            rowErrors.add("学科不能为空");
        } else {
            subject = findSubject(subjectText);
            if (subject == null) {
                rowErrors.add("学科不存在（不自动新建学科）：" + subjectText);
            }
        }
        String subjectName = subject != null ? subject.getName() : subjectText;

        // 题型（收窄为五种业务题型）
        SurveySchema.QuestionType type = null;
        if (!StringUtils.hasText(typeText)) {
            rowErrors.add("题型不能为空");
        } else {
            type = typeFromLabel(typeText);
            if (type == null) {
                rowErrors.add("题型不支持：" + typeText + "（仅支持 判断/单选/单项填空/多选/多项填空）");
            }
        }

        // 题干
        if (!StringUtils.hasText(title)) {
            rowErrors.add("题目内容不能为空");
        }

        // 章节（须属于该学科）
        Chapter chapter = null;
        if (StringUtils.hasText(chapterText)) {
            if (subject == null) {
                rowErrors.add("章节「" + chapterText + "」缺少可用的学科上下文");
            } else {
                chapter = findChapter(subject.getId(), chapterText);
                if (chapter == null) {
                    rowErrors.add("学科「" + subjectName + "」下不存在章节：" + chapterText);
                }
            }
        } else if (StringUtils.hasText(sectionText) || StringUtils.hasText(kpText)) {
            rowErrors.add("填写了小节或知识点但章节为空");
        }
        // 小节（须属于该章节，可留空）
        Section section = null;
        if (StringUtils.hasText(sectionText)) {
            if (chapter == null) {
                rowErrors.add("小节「" + sectionText + "」缺少可用的章节上下文");
            } else {
                section = findSection(chapter.getId(), sectionText);
                if (section == null) {
                    rowErrors.add("学科「" + subjectName + "」章节「" + chapterText + "」下不存在小节：" + sectionText);
                }
            }
        }
        // 知识点列归属校验：整格优先精确匹配（知识点名可含顿号，如「单价、数量、总价的应用」，
        // 此时顿号是名称的一部分，不能拆分）；整格未命中时才按顿号/逗号等拆分为多个
        // 知识点名逐 token 校验（兼容历史「一行多知识点」文件）。拆分不全命中时报未命中的名字。
        String kpWhole = kpText == null ? null : kpText.trim();
        List<String> kpNames = new ArrayList<>();
        if (chapter != null && StringUtils.hasText(kpWhole)) {
            String cacheSectionId = section != null ? section.getId() : null;
            String cacheChapterId = chapter.getId();
            Set<String> names = cacheSectionId != null
                    ? kpCache.computeIfAbsent("s:" + cacheSectionId,
                            id -> kpNamesOfSection(cacheSectionId))
                    : kpCache.computeIfAbsent("c:" + cacheChapterId,
                            id -> kpNamesOfChapter(cacheChapterId));
            if (names.contains(kpWhole)) {
                // 整格命中：整个格子作为一个知识点名（含顿号也合法）
                kpNames.add(kpWhole);
            } else {
                List<String> tokens = splitAnswers(kpWhole);
                boolean allHit = true;
                for (String kp : tokens) {
                    if (!names.contains(kp)) {
                        allHit = false;
                        if (section != null) {
                            rowErrors.add("小节「" + sectionText + "」下不存在知识点：" + kp);
                        } else {
                            rowErrors.add("章节「" + chapterText + "」下不存在知识点：" + kp);
                        }
                    }
                }
                if (allHit) {
                    kpNames.addAll(tokens);
                }
            }
        }

        // 难易程度
        String difficulty = null;
        if (StringUtils.hasText(difficultyText)) {
            difficulty = difficultyOf(difficultyText);
            if (difficulty == null) {
                rowErrors.add("难易程度仅支持：简单/中等/困难");
            }
        }

        // 选项列（仅 单选/多选 读取，需从选项A 起连续填写、无重复、至少 2 个）
        List<String> options = new ArrayList<>();
        if (type == SurveySchema.QuestionType.Radio || type == SurveySchema.QuestionType.Checkbox) {
            boolean gap = false;
            Set<String> seen = new HashSet<>();
            for (int c = COL_OPTION_START; c <= COL_OPTION_END; c++) {
                String t = cellText(r, c);
                if (!StringUtils.hasText(t)) {
                    gap = true;
                    continue;
                }
                if (gap) {
                    rowErrors.add("选项列存在空缺，请从选项A开始连续填写");
                    gap = false;
                }
                if (!seen.add(t)) {
                    rowErrors.add("选项内容重复：" + t);
                }
                options.add(t);
            }
            if (options.size() < 2) {
                rowErrors.add("选择题至少需要填写 2 个选项");
            }
        }

        // 正确答案列（正确答案1~12）
        List<String> answerTokens = new ArrayList<>();
        for (int c = COL_ANSWER_START; c <= COL_ANSWER_END; c++) {
            String t = cellText(r, c);
            if (StringUtils.hasText(t)) {
                answerTokens.add(t);
            }
        }
        List<SurveySchema> builtChildren = null;
        List<String> marks = null;
        String topAnswer = null;
        if (type == SurveySchema.QuestionType.Radio) {
            if (answerTokens.isEmpty()) {
                rowErrors.add("单选题未填写正确答案");
            } else if (answerTokens.size() > 1) {
                rowErrors.add("单选题正确答案只需填写 1 个（列正确答案1）");
            } else {
                int idx = optionIndexOf(options, answerTokens.get(0));
                if (idx < 0) {
                    rowErrors.add("单选题正确答案需为选项字母或选项内容：" + answerTokens.get(0));
                } else {
                    marks = Collections.singletonList(String.valueOf((char) ('A' + idx)));
                }
            }
        } else if (type == SurveySchema.QuestionType.Checkbox) {
            if (answerTokens.isEmpty()) {
                rowErrors.add("多选题未填写正确答案");
            } else {
                Set<String> letterSet = new HashSet<>();
                for (String token : answerTokens) {
                    int idx = optionIndexOf(options, token);
                    if (idx < 0) {
                        rowErrors.add("多选题正确答案需为选项字母或选项内容：" + token);
                        continue;
                    }
                    String letter = String.valueOf((char) ('A' + idx));
                    if (!letterSet.add(letter)) {
                        rowErrors.add("多选题正确答案重复：" + token);
                    }
                }
                if (rowErrors.isEmpty()) {
                    marks = new ArrayList<>(letterSet);
                }
            }
        } else if (type == SurveySchema.QuestionType.Judge) {
            if (answerTokens.isEmpty()) {
                rowErrors.add("判断题未填写正确答案");
            } else if (answerTokens.size() > 1) {
                rowErrors.add("判断题正确答案只需填写 1 个（正确答案1 填 正确 或 错误）");
            } else {
                String judge = normalizeJudgeAnswer(answerTokens.get(0));
                if (!"正确".equals(judge) && !"错误".equals(judge)) {
                    rowErrors.add("判断题正确答案需为 正确/错误（或其同义写法），当前值：" + answerTokens.get(0));
                } else {
                    topAnswer = judge;
                }
            }
        } else if (type == SurveySchema.QuestionType.FillBlank) {
            if (answerTokens.isEmpty()) {
                rowErrors.add("填空题未填写正确答案");
            } else if (answerTokens.size() > 1) {
                rowErrors.add("单项填空仅 1 个空，正确答案填在「正确答案1」即可；多空请用「多项填空」题型");
            } else {
                topAnswer = answerTokens.get(0);
            }
        } else if (type == SurveySchema.QuestionType.MultipleBlank) {
            if (answerTokens.isEmpty()) {
                rowErrors.add("多项填空未填写正确答案");
            } else if (answerTokens.size() > MAX_ANSWER_CELLS) {
                rowErrors.add("多项填空答案不能超过 " + MAX_ANSWER_CELLS + " 个空");
            } else {
                topAnswer = String.join("|", answerTokens);
            }
        }

        if (!rowErrors.isEmpty()) {
            for (String e : rowErrors) {
                if (errors.size() < MAX_SHOWN_ERRORS) {
                    errors.add("第" + rowNum + "行：" + e);
                }
            }
            return;
        }

        // —— 行级校验通过，装配题目
        List<String> tags = splitAnswers(tagsText);
        List<SurveySchema> children = new ArrayList<>();
        if (type == SurveySchema.QuestionType.Radio || type == SurveySchema.QuestionType.Checkbox) {
            for (int i = 0; i < options.size(); i++) {
                children.add(SurveySchema.builder().id(newQuestionId()).title(options.get(i))
                        .attribute(SurveySchema.Attribute.builder().build()).build());
            }
            if (marks != null) {
                for (String letter : marks) {
                    int idx = letter.charAt(0) - 'A';
                    children.get(idx).setAttribute(SurveySchema.Attribute.builder()
                            .examCorrectAnswer(children.get(idx).getId()).build());
                }
            }
        } else if (type == SurveySchema.QuestionType.Judge) {
            children.add(SurveySchema.builder().id(newQuestionId()).title("正确")
                    .attribute(SurveySchema.Attribute.builder().build()).build());
            children.add(SurveySchema.builder().id(newQuestionId()).title("错误")
                    .attribute(SurveySchema.Attribute.builder().build()).build());
        } else if (type == SurveySchema.QuestionType.FillBlank) {
            children.add(SurveySchema.builder().id(newQuestionId())
                    .attribute(SurveySchema.Attribute.builder().build()).build());
        } else if (type == SurveySchema.QuestionType.MultipleBlank) {
            int blankCount = topAnswer.split("\\|", -1).length;
            for (int i = 0; i < blankCount; i++) {
                children.add(SurveySchema.builder().id(newQuestionId())
                        .attribute(SurveySchema.Attribute.builder().build()).build());
            }
        }

        SurveySchema.Attribute.AttributeBuilder attrBuilder = SurveySchema.Attribute.builder()
                .subject(subjectName)
                .difficulty(difficulty);
        if (StringUtils.hasText(analysis)) {
            attrBuilder.examAnalysis(analysis);
        }
        if (!kpNames.isEmpty()) {
            attrBuilder.knowledgePoint(kpNames);
        }
        if (StringUtils.hasText(chapterText) && chapter != null) {
            attrBuilder.chapter(chapter.getName());
        }
        if (section != null) {
            attrBuilder.section(section.getName());
        }
        if (type == SurveySchema.QuestionType.Judge || type == SurveySchema.QuestionType.FillBlank
                || type == SurveySchema.QuestionType.MultipleBlank) {
            attrBuilder.examCorrectAnswer(topAnswer);
        }
        if (type == SurveySchema.QuestionType.Radio || type == SurveySchema.QuestionType.Judge) {
            attrBuilder.examAnswerMode(SurveySchema.ExamScoreMode.onlyOne);
        } else {
            attrBuilder.examAnswerMode(SurveySchema.ExamScoreMode.selectAll);
        }
        if (type == SurveySchema.QuestionType.FillBlank || type == SurveySchema.QuestionType.MultipleBlank) {
            attrBuilder.examMatchRule(SurveySchema.ExamMatchRule.completeSame);
        }
        SurveySchema schema = SurveySchema.builder()
                .id(newQuestionId())
                .title(title)
                .type(type)
                .attribute(attrBuilder.build())
                .children(children)
                .tags(tags.isEmpty() ? null : tags)
                .build();

        TemplateRequest request = TemplateRequest.builder()
                .name(title)
                .questionType(type)
                .mode(ProjectModeEnum.exam)
                .subject(subjectName)
                .chapter(chapter != null ? chapter.getName() : null)
                .section(section != null ? section.getName() : null)
                .knowledgePoint(kpNames.isEmpty() ? null : kpNames.toArray(new String[0]))
                .difficulty(difficulty)
                .tag(tags.isEmpty() ? null : tags.toArray(new String[0]))
                .template(schema)
                .build();
        result.add(request);
    }

    /**
     * 安全读取单元格文本（越界/空返回空串）。
     */
    private static String cellText(Row row, int col) {
        if (row == null) {
            return "";
        }
        try {
            return row.getCellAsString(col).orElse("").trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String newQuestionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 按名称或编码查找学科。
     */
    private Subject findSubject(String nameOrCode) {
        String v = nameOrCode.trim();
        List<Subject> byName = subjectMapper.selectList(Wrappers.<Subject>lambdaQuery().eq(Subject::getName, v));
        if (!byName.isEmpty()) {
            return byName.get(0);
        }
        List<Subject> byCode = subjectMapper.selectList(Wrappers.<Subject>lambdaQuery().eq(Subject::getCode, v));
        return byCode.isEmpty() ? null : byCode.get(0);
    }

    /**
     * 按学科 + 名称查找章节。
     */
    private Chapter findChapter(String subjectId, String name) {
        List<Chapter> list = chapterMapper.selectList(Wrappers.<Chapter>lambdaQuery()
                .eq(Chapter::getSubjectId, subjectId).eq(Chapter::getName, name.trim()));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 按章节 + 名称查找小节。
     */
    private Section findSection(String chapterId, String name) {
        List<Section> list = sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
                .eq(Section::getChapterId, chapterId).eq(Section::getName, name.trim()));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 章节下全部知识点名称集合（经 章节→小节→知识点 两级查询；空则空集合）。
     */
    private Set<String> kpNamesOfChapter(String chapterId) {
        Set<String> names = new HashSet<>();
        List<Section> sections = sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
                .eq(Section::getChapterId, chapterId));
        if (sections.isEmpty()) {
            return names;
        }
        List<String> sectionIds = sections.stream().map(Section::getId).collect(Collectors.toList());
        List<KnowledgePoint> kps = knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
                .in(KnowledgePoint::getSectionId, sectionIds));
        for (KnowledgePoint kp : kps) {
            if (kp.getName() != null && !kp.getName().trim().isEmpty()) {
                names.add(kp.getName().trim());
            }
        }
        return names;
    }

    /**
     * 小节下全部知识点名称集合（空则空集合）。
     */
    private Set<String> kpNamesOfSection(String sectionId) {
        Set<String> names = new HashSet<>();
        List<KnowledgePoint> kps = knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
                .eq(KnowledgePoint::getSectionId, sectionId));
        for (KnowledgePoint kp : kps) {
            if (kp.getName() != null && !kp.getName().trim().isEmpty()) {
                names.add(kp.getName().trim());
            }
        }
        return names;
    }

    /**
     * 题型中文/英文别名 → 枚举（五种业务题型）。
     */
    private static SurveySchema.QuestionType typeFromLabel(String label) {
        String v = label.trim().toLowerCase();
        switch (v) {
            case "判断":
            case "判断题":
            case "judge":
            case "truefalse":
            case "tf":
                return SurveySchema.QuestionType.Judge;
            case "单选":
            case "单选题":
            case "单选择":
            case "radio":
            case "single":
                return SurveySchema.QuestionType.Radio;
            case "单项填空":
            case "单选填空":
            case "填空":
            case "填空题":
            case "fillblank":
                return SurveySchema.QuestionType.FillBlank;
            case "多选":
            case "多选题":
            case "多选择":
            case "checkbox":
                return SurveySchema.QuestionType.Checkbox;
            case "多项填空":
            case "多填空":
            case "多选填空":
            case "多空":
            case "multipleblank":
                return SurveySchema.QuestionType.MultipleBlank;
            default:
                return null;
        }
    }

    /**
     * 难易程度中文/英文 → easy/medium/hard；无法识别返回 null。
     */
    private static String difficultyOf(String text) {
        String v = text.trim().toLowerCase();
        if ("简单".equals(v) || "易".equals(v) || "easy".equals(v)) {
            return "easy";
        }
        if ("中等".equals(v) || "中".equals(v) || "medium".equals(v) || "normal".equals(v)) {
            return "medium";
        }
        if ("困难".equals(v) || "难".equals(v) || "hard".equals(v)) {
            return "hard";
        }
        return null;
    }

    /**
     * 在选项文本列表中定位答案（支持：字母 A~H 下标、选项内容精确匹配）。
     *
     * @param options 按 A~H 顺序的选项文本
     * @param token   答案（字母或选项内容）
     * @return 选项下标；未命中 -1
     */
    private static int optionIndexOf(List<String> options, String token) {
        if (options == null || token == null) {
            return -1;
        }
        String t = token.trim();
        if (t.length() == 1 && Character.isLetter(t.charAt(0))) {
            int letterIndex = Character.toUpperCase(t.charAt(0)) - 'A';
            if (letterIndex >= 0 && letterIndex < options.size()) {
                return letterIndex;
            }
        }
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i) != null && t.equals(options.get(i).trim())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 练习绑定位置反查：返回该练习绑定的章节/小节及其知识上下文。
     *
     * @implNote 同时查询 t_chapter_repo 与 t_section_repo；章节上下文（学科/年级/学期/版本）
     * 供前端回显与跳转习题列表页定位。
     */
    @Override
    public RepoBindLocationView listRepoLocations(String repoId) {
        RepoBindLocationView view = new RepoBindLocationView();
        if (!StringUtils.hasText(repoId)) {
            return view;
        }
        // 章节绑定
        List<ChapterRepo> chapterBindings = chapterRepoMapper.selectList(Wrappers.<ChapterRepo>lambdaQuery()
                .eq(ChapterRepo::getRepoId, repoId).orderByAsc(ChapterRepo::getCreateAt));
        if (!chapterBindings.isEmpty()) {
            List<String> chapterIds = chapterBindings.stream().map(ChapterRepo::getChapterId)
                    .collect(Collectors.toList());
            Map<String, Chapter> chapterMap = chapterMapper.selectBatchIds(chapterIds).stream()
                    .collect(Collectors.toMap(Chapter::getId, c -> c));
            chapterBindings.stream().map(b -> chapterMap.get(b.getChapterId()))
                    .filter(Objects::nonNull).forEach(chapter -> {
                        RepoNodeBinding item = new RepoNodeBinding();
                        item.setNodeType("CHAP");
                        item.setNodeId(chapter.getId());
                        item.setNodeName(chapter.getName());
                        item.setSubjectId(chapter.getSubjectId());
                        item.setGrade(chapter.getGrade());
                        item.setTerm(chapter.getTerm());
                        item.setVersion(chapter.getVersion());
                        view.getBindings().add(item);
                    });
        }
        // 小节绑定（附所属章节上下文）
        List<SectionRepo> sectionBindings = sectionRepoMapper.selectList(Wrappers.<SectionRepo>lambdaQuery()
                .eq(SectionRepo::getRepoId, repoId).orderByAsc(SectionRepo::getCreateAt));
        if (!sectionBindings.isEmpty()) {
            List<String> sectionIds = sectionBindings.stream().map(SectionRepo::getSectionId)
                    .collect(Collectors.toList());
            Map<String, Section> sectionMap = sectionMapper.selectBatchIds(sectionIds).stream()
                    .collect(Collectors.toMap(Section::getId, s -> s));
            Set<String> parentChapterIds = sectionMap.values().stream().map(Section::getChapterId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            Map<String, Chapter> parentChapterMap = parentChapterIds.isEmpty() ? Collections.emptyMap()
                    : chapterMapper.selectBatchIds(parentChapterIds).stream()
                    .collect(Collectors.toMap(Chapter::getId, c -> c));
            sectionBindings.stream().map(b -> sectionMap.get(b.getSectionId()))
                    .filter(Objects::nonNull).forEach(section -> {
                        Chapter chapter = parentChapterMap.get(section.getChapterId());
                        RepoNodeBinding item = new RepoNodeBinding();
                        item.setNodeType("SECTION");
                        item.setNodeId(section.getId());
                        item.setNodeName(section.getName());
                        item.setParentNodeId(chapter == null ? null : chapter.getId());
                        item.setParentNodeName(chapter == null ? null : chapter.getName());
                        item.setSubjectId(chapter == null ? null : chapter.getSubjectId());
                        item.setGrade(chapter == null ? section.getGrade() : chapter.getGrade());
                        item.setTerm(section.getTerm());
                        view.getBindings().add(item);
                    });
        }
        return view;
    }

    /**
     * 节点刷题内容预览（习题列表页）：与学员端 study/questions 同语义聚合题目。
     *
     * @implNote nodeType 支持 chapter / section / knowledgePoint / repo；
     * 练习题目按 t_template.repo_id 归属取题（覆盖种子与组题两套关联）；
     * 知识点直绑题经 t_knowledge_point_question 取题；两类取题并集去重，
     * 创建时间倒序取前 100。
     */
    @Override
    public List<NodeQuestionView> listNodeQuestions(String nodeType, String nodeId, Boolean withAnswer) {
        if (!StringUtils.hasText(nodeType) || !StringUtils.hasText(nodeId)) {
            return Collections.emptyList();
        }
        List<String> templateIds = new ArrayList<>(collectNodeTemplateIds(nodeType, nodeId));
        if (templateIds.isEmpty()) {
            return Collections.emptyList();
        }
        boolean expose = Boolean.TRUE.equals(withAnswer);
        return templateService.list(Wrappers.<Template>lambdaQuery()
                        .in(Template::getId, templateIds)
                        .orderByDesc(Template::getCreateAt))
                .stream()
                .sorted(Comparator.comparing(Template::getCreateAt,
                        Comparator.nullsFirst(Comparator.reverseOrder())))
                .limit(100)
                .map(template -> toNodeQuestionView(template, expose))
                .collect(Collectors.toList());
    }

    /**
     * 按节点聚合题目 id 集合（LinkedHashSet 保序去重）。
     */
    private Set<String> collectNodeTemplateIds(String nodeType, String nodeId) {
        Set<String> ids = new LinkedHashSet<>();
        List<String> repoIds = new ArrayList<>();
        List<String> sectionIds = new ArrayList<>();
        if ("repo".equals(nodeType)) {
            repoIds.add(nodeId);
        }
        else if ("chapter".equals(nodeType)) {
            chapterRepoMapper.selectList(Wrappers.<ChapterRepo>lambdaQuery()
                            .eq(ChapterRepo::getChapterId, nodeId).select(ChapterRepo::getRepoId))
                    .forEach(x -> repoIds.add(x.getRepoId()));
            sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
                            .eq(Section::getChapterId, nodeId).select(Section::getId))
                    .forEach(s -> sectionIds.add(s.getId()));
        }
        else if ("section".equals(nodeType)) {
            sectionIds.add(nodeId);
        }
        else if ("knowledgePoint".equals(nodeType)) {
            collectKnowledgePointQuestions(Collections.singletonList(nodeId), ids);
            return ids;
        }
        if (!sectionIds.isEmpty()) {
            sectionRepoMapper.selectList(Wrappers.<SectionRepo>lambdaQuery()
                            .in(SectionRepo::getSectionId, sectionIds).select(SectionRepo::getRepoId))
                    .forEach(x -> repoIds.add(x.getRepoId()));
        }
        if (!repoIds.isEmpty()) {
            templateService.list(Wrappers.<Template>lambdaQuery()
                            .in(Template::getRepoId, repoIds).select(Template::getId))
                    .forEach(t -> ids.add(t.getId()));
        }
        // 章节/小节节点同时聚合其下知识点直绑题目
        List<String> kpIds = sectionIds.isEmpty() ? Collections.emptyList()
                : knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
                        .in(KnowledgePoint::getSectionId, sectionIds).select(KnowledgePoint::getId))
                .stream().map(KnowledgePoint::getId).collect(Collectors.toList());
        collectKnowledgePointQuestions(kpIds, ids);
        return ids;
    }

    /**
     * 收集知识点直绑题目 id（t_knowledge_point_question）。
     */
    private void collectKnowledgePointQuestions(List<String> knowledgePointIds, Set<String> ids) {
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return;
        }
        knowledgePointQuestionMapper.selectList(Wrappers.<KnowledgePointQuestion>lambdaQuery()
                        .in(KnowledgePointQuestion::getKnowledgePointId, knowledgePointIds)
                        .select(KnowledgePointQuestion::getQuestionId))
                .forEach(x -> ids.add(x.getQuestionId()));
    }

    /**
     * 模板转节点预览视图；withAnswer=false 时递归剥离标准答案字段。
     */
    private NodeQuestionView toNodeQuestionView(Template template, boolean expose) {
        NodeQuestionView view = new NodeQuestionView();
        view.setId(template.getId());
        view.setName(template.getName());
        view.setQuestionType(template.getQuestionType());
        view.setTag(template.getTag());
        view.setDifficulty(template.getDifficulty());
        SurveySchema schema = template.getTemplate();
        if (!expose && schema != null) {
            stripAnswer(schema);
        }
        view.setSchema(schema);
        return view;
    }

    /**
     * 递归清除 schema 及其子题的答案字段（预览默认不泄题）。
     */
    private void stripAnswer(SurveySchema schema) {
        if (schema.getAttribute() != null) {
            schema.getAttribute().setExamCorrectAnswer(null);
        }
        if (schema.getChildren() != null) {
            schema.getChildren().forEach(this::stripAnswer);
        }
    }

}
