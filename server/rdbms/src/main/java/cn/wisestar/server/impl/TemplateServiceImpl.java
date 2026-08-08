package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.domain.mapper.TemplateViewMapper;
import cn.wisestar.server.domain.model.Repo;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.domain.model.UserBook;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.TemplateService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.util.StringUtils.hasText;

/**
 * 题目模板（Template）业务实现：题目 CRUD、四维筛选（学科/章节/知识点/难度）、
 * 分类/标签查询与错题本信息回填。
 *
 * 【类职责】
 * 1. 题目 CRUD：listTemplate 分页四维筛选、addTemplate/batchAddTemplate/updateTemplate/
 *    batchUpdateTemplate/deleteTemplate、getTemplate 详情（含错题本信息）、
 *    selectTemplate 按题库分组选择题目、listTemplateCategories 分类列表、getTags 标签集合
 * 2. 与题库（Repo）联动：题目挂在题库下（repoId），题库导出/批量导入委托本服务操作题目
 *
 * 【被谁调用】
 * - Controller：TemplateController（模板广场/题目管理）
 * - 业务层：RepoServiceImpl（批量添加/删除/导出题目、随机抽题 pickQuestionFromRepo）、
 *   SurveyServiceImpl（题库练习时加载题目列表）、RandomSurveyProcessor（间接）、
 *   UserBookServiceImpl/错题本相关（间接）
 *
 * 【依赖什么】
 * - TemplateMapper（BaseMapper CRUD）、TemplateViewMapper（MapStruct 转换）、
 *   RepoServiceImpl（selectTemplate 取题库列表，注意循环依赖用 ContextHelper 取 Bean）、
 *   UserBookServiceImpl（getTemplate 回填错题本 note/correctTimes/wrongTimes）
 *
 * 【核心数据流】
 * 管理端/模板广场 → TemplateController → TemplateServiceImpl → TemplateMapper（t_template）
 * → 列表/详情/标签/分类返回；出卷时题目经 RepoServiceImpl.pickQuestionFromRepo 转 SurveySchema。
 *
 * @author javahuang
 * @date 2021/9/23
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TemplateServiceImpl extends BaseService<TemplateMapper, Template> implements TemplateService {

    /**
     * MapStruct 转换器：Template ↔ TemplateView ↔ TemplateRequest。
     */
    private final TemplateViewMapper templateViewMapper;

    /**
     * 错题本服务：getTemplate 时回填当前用户对该题的错题信息（@Lazy 延迟注入，
     * 避免与 RepoServiceImpl 等形成构造器循环依赖）。
     */
    @Resource
    @Lazy
    private UserBookServiceImpl userBookService;

    /**
     * 分页查询题目模板列表（四维筛选：学科 subject / 章节 chapter / 知识点 knowledgePoint /
     * 难度 difficulty）。
     *
     * 【筛选条件说明】
     * - name 模糊、questionType（为空时排除 Survey 普通问卷题，默认只查考试题型）、
     *   category 分类、repoId 题库、mode 模式；
     * - 四维筛选：subject/chapter/difficulty 精确等值；knowledgePoint 用 like
     *   （因存储为 JSON 数组字符串，只能做包含匹配）；
     * - tag 标签：t_tag 表 exists 子查询（IN 匹配）；
     * - shared 权限：shared=0 只查本人；shared=null 且未指定 repoId 时也只查本人；
     *   排序按 priority 升序（值越小越靠前）。
     *
     * @param query 查询条件
     * @return 分页的 TemplateView 列表
     * @implNote 调用链：TemplateController.listTemplate → listTemplate → TemplateMapper 分页
     * → TemplateViewMapper.toView 转换。四维筛选同时被 AI 自习室"按知识点出题"场景使用。
     */
    @Override
    public PaginationResponse<TemplateView> listTemplate(TemplateQuery query) {
        Page<Template> templatePage = pageByQuery(query, Wrappers.<Template>lambdaQuery()
                .like(isNotEmpty(query.getName()), Template::getName, query.getName())
                .eq(query.getQuestionType() != null, Template::getQuestionType, query.getQuestionType())
                // 默认查询额是普通题型
                .ne(query.getQuestionType() == null, Template::getQuestionType, SurveySchema.QuestionType.Survey)
                .in(!query.getCategories().isEmpty(), Template::getCategory, query.getCategories())
                .eq(query.getRepoId() != null, Template::getRepoId, query.getRepoId())
                .eq(query.getMode() != null, Template::getMode, query.getMode())
                .eq(query.getSubject() != null, Template::getSubject, query.getSubject())
                .eq(query.getChapter() != null, Template::getChapter, query.getChapter())
                .eq(query.getDifficulty() != null, Template::getDifficulty, query.getDifficulty())
                .like(query.getKnowledgePoint() != null, Template::getKnowledgePoint, query.getKnowledgePoint())
                .exists(!query.getTag().isEmpty(),
                        String.format("select 1 from t_tag t where t.entity_id = t_template.id and t.name in (%s)",
                                query.getTag().stream().map(x -> "'" + x + "'").collect(Collectors.joining(","))))
                .eq(query.getShared() != null, Template::getShared, query.getShared())
                .eq(query.getShared() != null && query.getShared() == 0, Template::getCreateBy,
                        SecurityContextUtils.getUserId())
                .eq(query.getShared() == null && query.getRepoId() == null, Template::getCreateBy, SecurityContextUtils.getUserId())
                .orderByAsc(Template::getPriority));
        return new PaginationResponse<>(templatePage.getTotal(),
                templatePage.getRecords().stream().map(templateViewMapper::toView).collect(Collectors.toList()));
    }

    /**
     * 新增单个题目模板。
     *
     * @param request 模板请求（含完整题目 JSON template）
     * @return 新模板 ID
     * @implNote 被 TemplateController.addTemplate 调用；id 由 BaseModel 雪花算法生成。
     */
    @Override
    public String addTemplate(TemplateRequest request) {
        Template template = templateViewMapper.fromRequest(request);
        save(template);
        return template.getId();
    }

    /**
     * 批量新增题目模板。
     *
     * @param templateRequests 模板请求列表
     * @implNote 被 RepoServiceImpl.batchAddRepoTemplate（Excel 导入新增分支）调用。
     */
    @Override
    public void batchAddTemplate(List<TemplateRequest> templateRequests) {
        saveBatch(templateViewMapper.fromRequest(templateRequests));
    }

    /**
     * 批量更新题目模板。
     *
     * @param templateRequests 模板请求列表
     * @implNote 被 RepoServiceImpl.batchAddRepoTemplate（Excel 导入更新分支）调用。
     */
    @Override
    public void batchUpdateTemplate(List<TemplateRequest> templateRequests) {
        updateBatchById(templateViewMapper.fromRequest(templateRequests));
    }

    /**
     * 更新单个题目模板。
     *
     * @param request 模板请求（须含 id）
     * @implNote 被 TemplateController.updateTemplate 调用。
     */
    @Override
    public void updateTemplate(TemplateRequest request) {
        updateById(templateViewMapper.fromRequest(request));
    }

    /**
     * 删除题目模板（批量逻辑删除）。
     *
     * @param request 含 ids 列表
     * @implNote 被 TemplateController.deleteTemplate 调用。
     */
    @Override
    public void deleteTemplate(TemplateRequest request) {
        removeBatchByIds(request.getIds());
    }

    /**
     * 按题库分组选择题目（出卷界面按题库浏览题目）。
     *
     * @param request 含 mode（问卷/考试模式），题库范围：共享 OR 本人创建
     * @return Map<题库名称, 该题库下题目列表>（保持题库顺序）
     * @implNote 通过 ContextHelper.getBean 取 RepoServiceImpl（本类与 RepoServiceImpl
     * 互相引用，避免构造器循环依赖）。
     */
    @Override
    public Map<String, List<TemplateView>> selectTemplate(SelectTemplateRequest request) {
        RepoServiceImpl repoService = ContextHelper.getBean(RepoServiceImpl.class);
        List<Repo> repos = repoService.list(Wrappers.<Repo>lambdaQuery().eq(Repo::getMode, request.getMode().name())
                .and(x -> x.eq(Repo::getShared, 1).or(y -> y.eq(Repo::getCreateBy, SecurityContextUtils.getUserId()))));
        Map<String, List<TemplateView>> result = new LinkedHashMap<>();
        repos.forEach(repo -> {
            List<TemplateView> templateViews = templateViewMapper
                    .toView(list(Wrappers.<Template>lambdaQuery().eq(Template::getRepoId, repo.getId())));
            result.put(repo.getName(), templateViews);
        });
        return result;
    }

    /**
     * 查询模板分类集合（模板广场分类导航）。
     *
     * @param query 含名称模糊、shared、questionType 条件
     * @return 去重后的分类名集合
     * @implNote 用 QueryWrapper 原生 SQL select DISTINCT category 查询。
     */
    public Set<String> listTemplateCategories(CategoryQuery query) {
        QueryWrapper<Template> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT category");
        queryWrapper.like(hasText(query.getName()), "category", query.getName());
        queryWrapper.eq("shared", query.getShared());
        queryWrapper.eq("question_type", query.getQuestionType());
        return list(queryWrapper).stream().filter(x -> x != null).map(x -> x.getCategory()).collect(Collectors.toSet());
    }

    /**
     * 查询模板标签集合（问卷题模板广场标签筛选）。
     *
     * @param query 含 shared 与共享归属条件
     * @return 去重后的标签集合
     * @implNote 仅查询 questionType=Survey（问卷题）的模板，聚合其 tag 数组。
     */
    @Override
    public Set<String> getTags(TagQuery query) {
        Set<String> tags = new HashSet<>();
        list(Wrappers.<Template>lambdaQuery().select(Template::getTag)
                .eq(Template::getQuestionType, SurveySchema.QuestionType.Survey)
                .eq(query.getShared() == 0, Template::getCreateBy, SecurityContextUtils.getUserId())
                .eq(Template::getShared, query.getShared())).forEach(x -> {
            if (x.getTag() != null) {
                tags.addAll(Arrays.asList(x.getTag()));
            }
        });
        return tags;
    }

    /**
     * 查询单个题目模板详情（含当前用户的错题本信息回填）。
     *
     * @param query 含 id
     * @return 模板视图；若当前用户在该题上有错题本记录，则回填 note/correctTimes/wrongTimes
     * @implNote 调用链：TemplateController.getTemplate → getTemplate →
     * getById + userBookService 按 templateId+createBy 查错题本。
     */
    @Override
    public TemplateView getTemplate(TemplateQuery query) {
        Template template = this.getById(query.getId());
        TemplateView templateView = templateViewMapper.toView(template);
        SurveySchema schema = template.getTemplate();
        schema.setId(query.getId());

        UserBook userBook = userBookService.getOne(Wrappers.<UserBook>lambdaQuery().eq(UserBook::getTemplateId, query.getId())
                .eq(UserBook::getCreateBy, SecurityContextUtils.getUserId()));
        if (userBook != null) {
            templateView.setNote(userBook.getNote());
            templateView.setCorrectTimes(userBook.getCorrectTimes());
            templateView.setWrongTimes(userBook.getWrongTimes());
        }
        return templateView;
    }
}
