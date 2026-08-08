package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 问卷/问题模板服务接口（TemplateService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供问卷模板（Template）与题目模板的完整管理能力：
 * 模板分页列表、详情、创建（含批量）、更新（含批量）、删除、模板选择器、
 * 模板广场的分类与标签查询。模板可携带知识点四字段
 * （subject 学科 / chapter 章节 / knowledgePoint 知识点 / difficulty 难度），
 * 供 AI 自习室系统按知识点检索与统计。实现类位于 rdbms 模块
 * （TemplateServiceImpl）。</p>
 *
 * <p><b>调用方</b>：api 模块 TemplateApi（路径前缀 ${api.prefix}/template）。</p>
 *
 * @author javahuang
 * @date 2021/9/23
 */
public interface TemplateService {

	/**
	 * 分页查询模板列表（模板管理页 / 模板广场）。
	 *
	 * @param query 分页 + 筛选条件（分类、标签、名称、知识点四字段等，见 {@link TemplateQuery}）
	 * @return 模板分页列表
	 */
	PaginationResponse<TemplateView> listTemplate(TemplateQuery query);

	/**
	 * 新增模板。
	 *
	 * @param template 模板创建请求（见 {@link TemplateRequest}）
	 * @return 新模板 id
	 */
	String addTemplate(TemplateRequest template);

	/**
	 * 批量新增模板（批量导入题目模板）。
	 *
	 * @param templateRequests 模板请求列表
	 */
	void batchAddTemplate(List<TemplateRequest> templateRequests);

	/**
	 * 批量更新模板。
	 *
	 * @param templateRequests 模板请求列表（含 id）
	 */
	void batchUpdateTemplate(List<TemplateRequest> templateRequests);

	/**
	 * 更新单个模板。
	 *
	 * @param request 模板更新请求（含 id）
	 */
	void updateTemplate(TemplateRequest request);

	/**
	 * 删除模板（仅所有者可删，逻辑删除）。
	 *
	 * @param request 模板删除请求（含 id）
	 */
	void deleteTemplate(TemplateRequest request);

	/**
	 * 模板选择器数据源（按分类返回模板分组列表）。
	 *
	 * @param request 选择条件（见 {@link SelectTemplateRequest}）
	 * @return 分类 → 模板列表 的映射
	 */
	Map<String, List<TemplateView>> selectTemplate(SelectTemplateRequest request);

	/**
	 * 查询模板分类集合（模板广场筛选项，去重）。
	 *
	 * @param query 分类查询条件（见 {@link CategoryQuery}）
	 * @return 分类名集合
	 */
	Set<String> listTemplateCategories(CategoryQuery query);

	/**
	 * 查询模板标签集合（模板广场筛选项，去重）。
	 *
	 * @param query 标签查询条件（见 {@link TagQuery}）
	 * @return 标签集合
	 */
	Set<String> getTags(TagQuery query);

	/**
	 * 获取模板详情。
	 *
	 * @param query 查询条件（含模板 id，见 {@link TemplateQuery}）
	 * @return 模板详情视图（含题目 schema）
	 */
	TemplateView getTemplate(TemplateQuery query);
}
