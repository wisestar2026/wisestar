package cn.wisestar.server.api;

import cn.wisestar.server.core.annotation.EnableDataPerm;
import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 问卷项目接口（ProjectApi）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层，Spring MVC REST Controller）。</p>
 * <p><b>类职责</b>：提供问卷项目的管理接口：项目列表/详情/设置、项目增删改、
 * 参与者管理（列表/增删/下载/导入）、回收站（删除列表/彻底删除/恢复）、
 * 以及问卷编辑器用到的公共选择接口（用户/部门/角色/岗位/字典/模板/题库/标签）。</p>
 * <p><b>请求路径前缀</b>：类级路径为 {@code ${api.prefix}/project}（api.prefix 通常为 /api），
 * 各方法在类级路径上追加子路径（如 /api/project/list、/api/project/partner/list 等）。</p>
 * <p><b>被谁调用</b>：前端管理后台：项目列表页、问卷编辑器（设计器）、参与者管理页、
 * 回收站页面。除 select* 系列（编辑器内选择器）外，大部分接口要求登录并校验权限点。</p>
 * <p><b>依赖的服务</b>：</p>
 * <ul>
 *   <li>{@link ProjectService}——项目 CRUD、回收站、设置；</li>
 *   <li>{@link ProjectPartnerService}——参与者管理；</li>
 *   <li>{@link UserService}——编辑器用户选择；</li>
 *   <li>{@link PositionService} / {@link DeptService} / {@link RoleService}——编辑器岗位/部门/角色选择；</li>
 *   <li>{@link DictService} / {@link TemplateService} / {@link RepoService} / {@link TagService}——编辑器字典/模板/题库/标签选择。</li>
 * </ul>
 *
 * <p><b>数据权限说明</b>：getProject/setting/update/delete 及 partner 系列接口使用了
 * 自定义注解 {@link EnableDataPerm}，用于校验当前用户对指定项目的数据权限
 * （项目所有者/参与者），key 表达式指定从参数中取项目 id。</p>
 *
 * <p><b>数据流概览</b>：前端 HTTP 请求 → 本类方法（权限 + 数据权限注解拦截）→
 * 对应 shared Service 接口 → rdbms 实现 → MyBatis Mapper → 数据库 → 视图 DTO 返回。</p>
 *
 * @author javahuang
 * @date 2021/8/6
 */
@RestController
@RequestMapping("${api.prefix}/project")
@RequiredArgsConstructor
public class ProjectApi {

	/**
	 * 项目服务（项目 CRUD、回收站、设置入口，构造器注入）。
	 */
	private final ProjectService projectService;

	/**
	 * 项目参与者服务（参与者列表/增删/下载/导入入口）。
	 */
	private final ProjectPartnerService projectPartnerService;

	/**
	 * 用户服务（编辑器用户选择入口）。
	 */
	private final UserService userService;

	/**
	 * 岗位服务（编辑器岗位选择入口）。
	 */
	private final PositionService positionService;

	/**
	 * 部门服务（编辑器部门选择入口）。
	 */
	private final DeptService deptService;

	/**
	 * 角色服务（编辑器角色选择入口）。
	 */
	private final RoleService roleService;

	/**
	 * 字典服务（编辑器字典选择入口）。
	 */
	private final DictService dictService;

	/**
	 * 模板服务（编辑器题库模板选择入口）。
	 */
	private final TemplateService templateService;

	/**
	 * 题库服务（编辑器题库选择入口）。
	 */
	private final RepoService repoService;

	/**
	 * 标签服务（编辑器标签选择入口）。
	 */
	private final TagService tagService;

	/**
	 * 获取项目列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/project/list（如 /api/project/list）。</p>
	 *
	 * <p><b>功能</b>：分页查询当前用户有权限的项目列表（支持按名称/状态/创建时间等筛选），
	 * 供项目列表页展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectQuery}（GET 查询参数）——分页参数 + 筛选条件。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;ProjectView&gt;（分页包装的项目列表）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('project:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectService#listProject(ProjectQuery)}。</p>
	 *
	 * @param query 分页查询参数
	 * @return 项目列表分页结果
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('project:list')")
	public PaginationResponse<ProjectView> listProject(ProjectQuery query) {
		return projectService.listProject(query);
	}

	/**
	 * 获取项目信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/project?id=xxx（如 /api/project?id=xxx）。</p>
	 *
	 * <p><b>功能</b>：按项目 id 获取项目详情（基本信息 + 问卷 schema + 配置），
	 * 供问卷编辑器打开项目时加载。</p>
	 *
	 * <p><b>请求参数</b>：id（GET 查询参数，项目 id）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link ProjectView}（项目视图：基本信息 + 问卷 schema）。</p>
	 *
	 * <p><b>权限/数据权限</b>：@PreAuthorize("hasAuthority('project:detail')")
	 * + @EnableDataPerm(key = "#id")——校验对 #id 项目的操作权。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectService#getProject(String)}。</p>
	 *
	 * @param id 项目 id
	 * @return 项目详情视图
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('project:detail')")
	@EnableDataPerm(key = "#id")
	public ProjectView getProject(String id) {
		return projectService.getProject(id);
	}

	/**
	 * 获取项目设置。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/project/setting（如 /api/project/setting?id=xxx）。</p>
	 *
	 * <p><b>功能</b>：获取项目设置信息（问卷风格、答题限制、白名单、随机规则等
	 * ProjectSetting），供项目设置页加载。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectQuery}（GET 查询参数，含项目 id）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link ProjectSetting}（项目设置视图）。</p>
	 *
	 * <p><b>权限/数据权限</b>：@PreAuthorize("hasAuthority('project:detail')")
	 * + @EnableDataPerm(key = "#id")（key 指向 query 中的 id）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectService#getSetting(ProjectQuery)}。</p>
	 *
	 * @param query 项目查询参数（含项目 id）
	 * @return 项目设置视图
	 */
	@GetMapping("/setting")
	@PreAuthorize("hasAuthority('project:detail')")
	@EnableDataPerm(key = "#id")
	public ProjectSetting getSetting(ProjectQuery query) {
		return projectService.getSetting(query);
	}

	/**
	 * 添加项目。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/create（如 /api/project/create）。</p>
	 *
	 * <p><b>功能</b>：新建问卷项目（基本信息 + 初始问卷 schema），返回新项目视图。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectRequest}（@RequestBody JSON）——项目名称、类型、
	 * 描述、schema 等。</p>
	 *
	 * <p><b>返回值结构</b>：{@link ProjectView}（新建的项目视图，含新项目 id）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('project:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectService#addProject(ProjectRequest)}。</p>
	 *
	 * @param project 项目创建请求
	 * @return 新建的项目视图
	 */
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('project:create')")
	public ProjectView addProject(@RequestBody ProjectRequest project) {
		return projectService.addProject(project);
	}

	/**
	 * 更新项目。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/update（如 /api/project/update）。</p>
	 *
	 * <p><b>功能</b>：更新项目基本信息与问卷 schema（编辑器保存时调用）。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectRequest}（@RequestBody JSON，含项目 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限/数据权限</b>：@PreAuthorize("hasAuthority('project:update')")
	 * + @EnableDataPerm(key = "#project.id")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectService#updateProject(ProjectRequest)}。</p>
	 *
	 * @param project 项目更新请求
	 */
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('project:update')")
	@EnableDataPerm(key = "#project.id")
	public void updateProject(@RequestBody ProjectRequest project) {
		projectService.updateProject(project);
	}

	/**
	 * 删除项目（放入回收站）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/delete（如 /api/project/delete）。</p>
	 *
	 * <p><b>功能</b>：将项目标记删除（逻辑删除，进入回收站，可恢复）。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectRequest}（@RequestBody JSON，含项目 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限/数据权限</b>：@PreAuthorize("hasAuthority('project:delete')")
	 * + @EnableDataPerm(key = "#project.id")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectService#deleteProject(ProjectRequest)}。</p>
	 *
	 * @param project 项目删除请求
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('project:delete')")
	@EnableDataPerm(key = "#project.id")
	public void deleteProject(@RequestBody ProjectRequest project) {
		projectService.deleteProject(project);
	}

	/**
	 * 获取项目参与者列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/project/partner/list
	 * （如 /api/project/partner/list?projectId=xxx）。</p>
	 *
	 * <p><b>功能</b>：分页查询指定项目的参与者（协作成员）列表，供参与者管理页展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectPartnerQuery}（GET 查询参数，含 projectId 与分页条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;ProjectPartnerView&gt;。</p>
	 *
	 * <p><b>数据权限</b>：@EnableDataPerm(key = "#query.projectId")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectPartnerService#listProjectPartner(ProjectPartnerQuery)}。</p>
	 *
	 * @param query 参与者查询参数
	 * @return 参与者分页列表
	 */
	@GetMapping("/partner/list")
	@EnableDataPerm(key = "#query.projectId")
	public PaginationResponse<ProjectPartnerView> listProjectPartner(ProjectPartnerQuery query) {
		return projectPartnerService.listProjectPartner(query);
	}

	/**
	 * 添加项目参与者。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/partner/create
	 * （如 /api/project/partner/create）。</p>
	 *
	 * <p><b>功能</b>：向项目添加协作成员（指定用户 + 角色权限）。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectPartnerRequest}（@RequestBody JSON，含 projectId、userId、角色等）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>数据权限</b>：@EnableDataPerm(key = "#request.projectId")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectPartnerService#addProjectPartner(ProjectPartnerRequest)}。</p>
	 *
	 * @param request 参与者添加请求
	 */
	@PostMapping("/partner/create")
	@EnableDataPerm(key = "#request.projectId")
	public void addProjectPartner(@RequestBody ProjectPartnerRequest request) {
		projectPartnerService.addProjectPartner(request);
	}

	/**
	 * 删除项目参与者。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/partner/delete
	 * （如 /api/project/partner/delete）。</p>
	 *
	 * <p><b>功能</b>：移除项目中的某个协作成员。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectPartnerRequest}（@RequestBody JSON，含 projectId 与参与者 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>数据权限</b>：@EnableDataPerm(key = "#request.projectId")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectPartnerService#deleteProjectPartner(ProjectPartnerRequest)}。</p>
	 *
	 * @param request 参与者删除请求
	 */
	@PostMapping("/partner/delete")
	@EnableDataPerm(key = "#request.projectId")
	public void deleteProjectPartner(@RequestBody ProjectPartnerRequest request) {
		projectPartnerService.deleteProjectPartner(request);
	}

	/**
	 * 下载项目参与者列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/project/partner/download
	 * （如 /api/project/partner/download?projectId=xxx）。</p>
	 *
	 * <p><b>功能</b>：将指定项目的参与者列表导出为 Excel 文件（响应流由服务层写出）。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectPartnerQuery}（GET 查询参数，含 projectId）。</p>
	 *
	 * <p><b>返回值结构</b>：无方法返回值；服务层直接把 Excel 写入 HTTP 响应流。</p>
	 *
	 * <p><b>数据权限</b>：@EnableDataPerm(key = "#query.projectId")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectPartnerService#downloadPartner(ProjectPartnerQuery)}。</p>
	 *
	 * @param query 参与者查询参数（定位项目）
	 */
	@GetMapping("/partner/download")
	@EnableDataPerm(key = "#query.projectId")
	public void downloadPartner(ProjectPartnerQuery query) {
		projectPartnerService.downloadPartner(query);
	}

	/**
	 * 导入项目参与者列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/partner/import
	 * （如 /api/project/partner/import）。</p>
	 *
	 * <p><b>功能</b>：按模板 Excel 批量导入项目参与者（与 download 下载的模板配套）。</p>
	 *
	 * <p><b>请求参数</b>：{@link WhiteListRequest}（multipart 表单绑定，含 projectId 与 Excel 文件）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>数据权限</b>：@EnableDataPerm(key = "#request.projectId")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectPartnerService#importPartner(WhiteListRequest)}。</p>
	 *
	 * @param request 导入请求（含文件与项目 id）
	 */
	@PostMapping("/partner/import")
	@EnableDataPerm(key = "#request.projectId")
	public void importPartner(WhiteListRequest request) {
		projectPartnerService.importPartner(request);
	}

	/**
	 * 获取回收站里的项目列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/project/trash（如 /api/project/trash）。</p>
	 *
	 * <p><b>功能</b>：查询当前用户已删除（回收站中）的项目列表，供回收站页面展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectQuery}（GET 查询参数，筛选条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<ProjectView>}（已删除项目列表）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('project:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectService#getDeleted(ProjectQuery)}。</p>
	 *
	 * @param query 项目查询参数
	 * @return 回收站项目列表
	 */
	@GetMapping("/trash")
	@PreAuthorize("hasAuthority('project:list')")
	public List<ProjectView> getDeleted(ProjectQuery query) {
		return projectService.getDeleted(query);
	}

	/**
	 * 从回收站彻底移除项目。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/destroy（如 /api/project/destroy）。</p>
	 *
	 * <p><b>功能</b>：将回收站中的项目（支持多个 id）物理删除（连同答案等关联数据），
	 * 不可恢复。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectRequest}（@RequestBody JSON，id 字段支持逗号分隔多个项目 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('project:create')")——复用创建权限。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectService#batchDestroyProject(ProjectRequest)}。</p>
	 *
	 * @param request 项目请求（含待销毁项目 id）
	 */
	@PostMapping("/destroy")
	@PreAuthorize("hasAuthority('project:create')")
	public void batchDestroyProject(@RequestBody ProjectRequest request) {
		projectService.batchDestroyProject(request);
	}

	/**
	 * 从回收站里面恢复项目。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/restore（如 /api/project/restore）。</p>
	 *
	 * <p><b>功能</b>：将回收站中的项目（支持多个 id）恢复为正常状态。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectRequest}（@RequestBody JSON，含项目 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('project:create')")——复用创建权限。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ProjectService#restoreProject(ProjectRequest)}。</p>
	 *
	 * @param request 项目请求（含待恢复项目 id）
	 */
	@PostMapping("/restore")
	@PreAuthorize("hasAuthority('project:create')")
	public void restoreProject(@RequestBody ProjectRequest request) {
		projectService.restoreProject(request);
	}

	/**
	 * 编辑器里面获取用户信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/selectUser（如 /api/project/selectUser）。</p>
	 *
	 * <p><b>功能</b>：问卷编辑器中的"用户选择器"数据源——按关键字/条件搜索用户列表
	 * （如权限设置、条件跳转等场景引用用户）。</p>
	 *
	 * <p><b>请求参数</b>：{@link SelectUserRequest}（@RequestBody JSON，搜索条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<UserInfo>}（匹配的用户列表）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#selectUsers(SelectUserRequest)}。</p>
	 *
	 * @param request 用户选择请求（搜索条件）
	 * @return 匹配的用户列表
	 */
	@PostMapping("/selectUser")
	public List<UserInfo> selectUser(@RequestBody SelectUserRequest request) {
		return userService.selectUsers(request);
	}

	/**
	 * 编辑器里面获取部门信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/selectDept（如 /api/project/selectDept）。</p>
	 *
	 * <p><b>功能</b>：问卷编辑器中的"部门选择器"数据源——按条件查询部门列表。</p>
	 *
	 * <p><b>请求参数</b>：{@link SelectDeptRequest}（@RequestBody JSON，搜索条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<DeptView>}（部门视图列表）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DeptService#listDept(SelectDeptRequest)}（复用部门查询，参数类型不同）。</p>
	 *
	 * @param request 部门选择请求
	 * @return 部门视图列表
	 */
	@PostMapping("/selectDept")
	public List<DeptView> selectDept(@RequestBody SelectDeptRequest request) {
		return deptService.listDept(request);
	}

	/**
	 * 编辑器里面获取角色信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/selectRole（如 /api/project/selectRole）。</p>
	 *
	 * <p><b>功能</b>：问卷编辑器中的"角色选择器"数据源——按条件查询角色列表。</p>
	 *
	 * <p><b>请求参数</b>：{@link SelectRoleRequest}（@RequestBody JSON，搜索条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<RoleView>}（角色视图列表）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RoleService#selectRoles(SelectRoleRequest)}。</p>
	 *
	 * @param request 角色选择请求
	 * @return 角色视图列表
	 */
	@PostMapping("/selectRole")
	public List<RoleView> selectRole(@RequestBody SelectRoleRequest request) {
		return roleService.selectRoles(request);
	}

	/**
	 * 编辑器里面获取岗位信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/selectPosition
	 * （如 /api/project/selectPosition）。</p>
	 *
	 * <p><b>功能</b>：问卷编辑器中的"岗位选择器"数据源——按条件查询岗位列表。</p>
	 *
	 * <p><b>请求参数</b>：{@link SelectPositionRequest}（@RequestBody JSON，搜索条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<PositionView>}（岗位视图列表）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link PositionService#selectPositions(SelectPositionRequest)}。</p>
	 *
	 * @param request 岗位选择请求
	 * @return 岗位视图列表
	 */
	@PostMapping("/selectPosition")
	public List<PositionView> selectPosition(@RequestBody SelectPositionRequest request) {
		return positionService.selectPositions(request);
	}

	/**
	 * 编辑器里面获取字典。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/selectDict（如 /api/project/selectDict）。</p>
	 *
	 * <p><b>功能</b>：问卷编辑器中的"字典选择器"数据源——返回全部字典列表（无需参数）。</p>
	 *
	 * <p><b>请求参数</b>：无。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<CommDictView>}（字典视图列表）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DictService#selectDict()}。</p>
	 *
	 * @return 字典视图列表
	 */
	@PostMapping("/selectDict")
	public List<CommDictView> selectDict() {
		return dictService.selectDict();
	}

	/**
	 * 编辑器里面获取题库模板。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/selectTemplate
	 * （如 /api/project/selectTemplate）。</p>
	 *
	 * <p><b>功能</b>：问卷编辑器中的"题库模板选择器"数据源——按分类/条件查询模板，
	 * 返回按分类分组的模板 Map。</p>
	 *
	 * <p><b>请求参数</b>：{@link SelectTemplateRequest}（@RequestBody JSON，模板筛选条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code Map<String, List<TemplateView>>}——key 为模板分类，
	 * value 为该分类下的模板列表。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link TemplateService#selectTemplate(SelectTemplateRequest)}。</p>
	 *
	 * @param request 模板选择请求
	 * @return 按分类分组的模板 Map
	 */
	@PostMapping("/selectTemplate")
	public Map<String, List<TemplateView>> selectTemplate(@RequestBody SelectTemplateRequest request) {
		return templateService.selectTemplate(request);
	}

	/**
	 * 编辑器里面获取题库。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/selectRepo（如 /api/project/selectRepo）。</p>
	 *
	 * <p><b>功能</b>：问卷编辑器中的"题库选择器"数据源——按条件查询题库列表。</p>
	 *
	 * <p><b>请求参数</b>：{@link SelectRepoRequest}（@RequestBody JSON，题库筛选条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<RepoView>}（题库视图列表）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#selectRepo(SelectRepoRequest)}。</p>
	 *
	 * @param request 题库选择请求
	 * @return 题库视图列表
	 */
	@PostMapping("/selectRepo")
	public List<RepoView> selectRepo(@RequestBody SelectRepoRequest request) {
		return repoService.selectRepo(request);
	}

	/**
	 * 编辑器里面获取标签。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/project/selectTag（如 /api/project/selectTag）。</p>
	 *
	 * <p><b>功能</b>：问卷编辑器中的"标签选择器"数据源——按条件查询标签集合。</p>
	 *
	 * <p><b>请求参数</b>：{@link SelectTagRequest}（@RequestBody JSON，标签筛选条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code Set<String>}（去重后的标签字符串集合）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link TagService#selectTag(SelectTagRequest)}。</p>
	 *
	 * @param request 标签选择请求
	 * @return 标签字符串集合
	 */
	@PostMapping("/selectTag")
	public Set<String> selectTag(@RequestBody SelectTagRequest request) {
		return tagService.selectTag(request);
	}

}
