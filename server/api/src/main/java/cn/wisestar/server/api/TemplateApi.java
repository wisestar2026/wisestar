package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 问卷/问题模板接口（TemplateApi）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层，Spring MVC REST Controller）。</p>
 * <p><b>类职责</b>：提供问卷模板的管理接口：模板分页列表、模板详情、创建/批量创建、
 * 更新、删除，以及"模板广场"的分类与标签查询。</p>
 * <p><b>请求路径前缀</b>：类级路径为 {@code ${api.prefix}/template}（api.prefix 通常为 /api），
 * 各方法再追加子路径（如 /api/template/list、/api/template/listCategory 等）。</p>
 * <p><b>被谁调用</b>：前端管理后台：问卷编辑器（从模板新建问卷）、模板管理页、
 * 模板广场页（分类/标签筛选、模板列表）。</p>
 * <p><b>依赖的服务</b>：注入 {@link TemplateService}（shared 模块接口，rdbms 模块实现）——
 * 负责模板 CRUD、分类/标签查询、模板选择器等业务。</p>
 *
 * <p><b>数据流概览</b>：前端 HTTP 请求 → 本类方法（权限注解）→ TemplateService
 * → rdbms 实现 → 模板 Mapper → 数据库 → 视图 DTO 返回。</p>
 *
 * @author javahuang
 * @date 2021/9/23
 */
@RestController
@RequestMapping("${api.prefix}/template")
@RequiredArgsConstructor
public class TemplateApi {

	/**
	 * 模板服务（业务层入口，构造器注入）。
	 */
	private final TemplateService templateService;

	/**
	 * 获取模板分页列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/template/list（如 /api/template/list）。</p>
	 *
	 * <p><b>功能</b>：分页查询问卷/问题模板列表（支持按分类、标签、名称筛选），
	 * 供模板管理页与模板广场展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link TemplateQuery}（GET 查询参数）——分页 + 筛选条件。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;TemplateView&gt;（分页包装的模板列表）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('template:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link TemplateService#listTemplate(TemplateQuery)}。</p>
	 *
	 * @param query 模板查询参数
	 * @return 模板分页列表
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('template:list')")
	public PaginationResponse<TemplateView> listQuestionTemplate(TemplateQuery query) {
		return templateService.listTemplate(query);
	}

	/**
	 * 获取模板详情。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/template/get（如 /api/template/get?id=xxx）。</p>
	 *
	 * <p><b>功能</b>：按 id 获取单个模板的完整内容（含题目 schema），供模板编辑或
	 * 从模板新建问卷时加载。无权限注解（是否要求登录由全局安全规则控制）。</p>
	 *
	 * <p><b>请求参数</b>：{@link TemplateQuery}（GET 查询参数，含模板 id）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link TemplateView}（模板详情视图，含题目 schema）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link TemplateService#getTemplate(TemplateQuery)}。</p>
	 *
	 * @param query 模板查询参数（含 id）
	 * @return 模板详情视图
	 */
	@GetMapping("/get")
	public TemplateView getTemplate(TemplateQuery query) {
		return templateService.getTemplate(query);
	}

	/**
	 * 创建模板。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/template/create（如 /api/template/create）。</p>
	 *
	 * <p><b>功能</b>：新建一个问卷/问题模板，返回新模板 id。</p>
	 *
	 * <p><b>请求参数</b>：{@link TemplateRequest}（@RequestBody JSON）——模板名称、分类、
	 * 标签、题目 schema 等。</p>
	 *
	 * <p><b>返回值结构</b>：String（新模板 id）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('template:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link TemplateService#addTemplate(TemplateRequest)}。</p>
	 *
	 * @param template 模板创建请求
	 * @return 新模板 id
	 */
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('template:create')")
	public String addTemplate(@RequestBody TemplateRequest template) {
		return templateService.addTemplate(template);
	}

	/**
	 * 批量创建题目模板。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/template/batchCreate
	 * （如 /api/template/batchCreate）。</p>
	 *
	 * <p><b>功能</b>：一次提交多个模板（批量导入题目模板），逐条创建。</p>
	 *
	 * <p><b>请求参数</b>：{@code List<TemplateRequest>}（@RequestBody JSON 数组）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('template:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link TemplateService#batchAddTemplate(List)}。</p>
	 *
	 * @param templateRequests 模板创建请求列表
	 */
	@PostMapping("/batchCreate")
	@PreAuthorize("hasAuthority('template:create')")
	public void batchAddTemplate(@RequestBody List<TemplateRequest> templateRequests) {
		templateService.batchAddTemplate(templateRequests);
	}

	/**
	 * 更新模板。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/template/update（如 /api/template/update）。</p>
	 *
	 * <p><b>功能</b>：更新模板信息与题目 schema。</p>
	 *
	 * <p><b>请求参数</b>：{@link TemplateRequest}（@RequestBody JSON，含模板 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('template:update')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link TemplateService#updateTemplate(TemplateRequest)}。</p>
	 *
	 * @param template 模板更新请求
	 */
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('template:update')")
	public void updateTemplate(@RequestBody TemplateRequest template) {
		templateService.updateTemplate(template);
	}

	/**
	 * 删除模板。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/template/delete（如 /api/template/delete）。</p>
	 *
	 * <p><b>功能</b>：按 id 删除模板（逻辑删除，由服务层实现）。</p>
	 *
	 * <p><b>请求参数</b>：{@link TemplateRequest}（@RequestBody JSON，含模板 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('template:delete')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link TemplateService#deleteTemplate(TemplateRequest)}。</p>
	 *
	 * @param request 模板删除请求
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('template:delete')")
	public void deleteTemplate(@RequestBody TemplateRequest request) {
		templateService.deleteTemplate(request);
	}

	/**
	 * 模板广场获取分类。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/template/listCategory
	 * （如 /api/template/listCategory）。</p>
	 *
	 * <p><b>功能</b>：返回模板广场页展示的分类集合（按条件过滤后的去重分类列表），
	 * 供分类筛选项使用。</p>
	 *
	 * <p><b>请求参数</b>：{@link CategoryQuery}（GET 查询参数，分类筛选条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code Set<String>}（去重分类名集合）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link TemplateService#listTemplateCategories(CategoryQuery)}。</p>
	 *
	 * @param query 分类查询参数
	 * @return 分类名集合
	 */
	@GetMapping("/listCategory")
	public Set<String> listTemplateCategories(CategoryQuery query) {
		return templateService.listTemplateCategories(query);
	}

	/**
	 * 模板广场获取标签。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/template/listTag（如 /api/template/listTag）。</p>
	 *
	 * <p><b>功能</b>：返回模板广场页展示的标签集合（去重），供标签筛选项使用。</p>
	 *
	 * <p><b>请求参数</b>：{@link TagQuery}（GET 查询参数，标签筛选条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code Set<String>}（去重标签集合）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link TemplateService#getTags(TagQuery)}。</p>
	 *
	 * @param query 标签查询参数
	 * @return 标签集合
	 */
	@GetMapping("/listTag")
	public Set<String> getTags(TagQuery query) {
		return templateService.getTags(query);
	}

}
