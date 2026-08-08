package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.service.RepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 题库接口（RepoApi）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层，Spring MVC REST Controller）。</p>
 * <p><b>类职责</b>：提供"题库"（Repo）管理接口：题库列表/详情、题库增删改、
 * 批量创建（从模板批量入题）、模板绑定/解绑、从题库挑题、模板导入题库、
 * 以及"我的笔记"（UserBook）的列表/增改删，和题库题目导出。</p>
 * <p><b>请求路径前缀</b>：类级路径为 {@code ${api.prefix}/repo}（api.prefix 通常为 /api），
 * 各方法再追加子路径（如 /api/repo/list、/api/repo/book/list 等）。</p>
 * <p><b>被谁调用</b>：前端管理后台：题库管理页（题目库维护）、问卷编辑器
 * （从题库挑题）、笔记管理页（我的笔记）。</p>
 * <p><b>依赖的服务</b>：注入 {@link RepoService}（shared 模块接口，rdbms 模块实现）——
 * 负责题库 CRUD、模板绑定、挑题、笔记、导出等业务。</p>
 *
 * <p><b>数据流概览</b>：前端 HTTP 请求 → 本类方法（权限注解）→ RepoService
 * → rdbms 实现 → 题库/模板关联/笔记 Mapper → 数据库 → 视图 DTO 返回。</p>
 *
 * @author javahuang
 * @date 2022/4/27
 */
@RequestMapping("${api.prefix}/repo")
@RequiredArgsConstructor
@RestController
public class RepoApi {

	/**
	 * 题库服务（业务层入口，构造器注入）。
	 */
	private final RepoService repoService;

	/**
	 * 获取题库列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/repo/list（如 /api/repo/list）。</p>
	 *
	 * <p><b>功能</b>：分页查询题库列表（按名称/标签/类型筛选），供题库管理页展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link RepoQuery}（GET 查询参数）——分页 + 筛选条件。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;RepoView&gt;（分页包装的题库列表）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#listRepo(RepoQuery)}。</p>
	 *
	 * @param query 题库查询参数
	 * @return 题库分页列表
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('repo:list')")
	public PaginationResponse<RepoView> listRepo(RepoQuery query) {
		return repoService.listRepo(query);
	}

	/**
	 * 获取题库详情。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/repo（如 /api/repo?id=xxx）。
	 * 由类级路径 + 方法级 @GetMapping（无路径值）组合映射。</p>
	 *
	 * <p><b>功能</b>：按 id 获取题库详情（含题库下题目列表）。</p>
	 *
	 * <p><b>请求参数</b>：id（GET 查询参数，题库 id）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link RepoView}（题库详情视图）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:detail')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#getRpo(String)}。</p>
	 *
	 * @param id 题库 id
	 * @return 题库详情视图
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('repo:detail')")
	public RepoView getRpo(String id) {
		return repoService.getRpo(id);
	}

	/**
	 * 创建题库。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/create（如 /api/repo/create）。</p>
	 *
	 * <p><b>功能</b>：新建题库（题库名称、类型、题目列表等）。</p>
	 *
	 * <p><b>请求参数</b>：{@link RepoRequest}（@RequestBody JSON，题库信息）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#addRepo(RepoRequest)}。</p>
	 *
	 * @param request 题库创建请求
	 */
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('repo:create')")
	public void addRepo(@RequestBody RepoRequest request) {
		repoService.addRepo(request);
	}

	/**
	 * 更新题库。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/update（如 /api/repo/update）。</p>
	 *
	 * <p><b>功能</b>：更新题库信息（含题目列表的增删改）。</p>
	 *
	 * <p><b>请求参数</b>：{@link RepoRequest}（@RequestBody JSON，含题库 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:update')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#updateRepo(RepoRequest)}。</p>
	 *
	 * @param request 题库更新请求
	 */
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('repo:update')")
	public void updateRepo(@RequestBody RepoRequest request) {
		repoService.updateRepo(request);
	}

	/**
	 * 删除题库。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/delete（如 /api/repo/delete）。</p>
	 *
	 * <p><b>功能</b>：按 id 删除题库（逻辑删除，由服务层实现）。</p>
	 *
	 * <p><b>请求参数</b>：{@link RepoRequest}（@RequestBody JSON，含题库 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:delete')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#deleteRepo(RepoRequest)}。</p>
	 *
	 * @param request 题库删除请求
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('repo:delete')")
	public void deleteRepo(@RequestBody RepoRequest request) {
		repoService.deleteRepo(request);
	}

	/**
	 * 批量创建题库（从模板批量入题）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/batchCreate
	 * （如 /api/repo/batchCreate）。</p>
	 *
	 * <p><b>功能</b>：将一批问卷/问题模板（Template）批量导入/绑定到题库，快速初始化题库。</p>
	 *
	 * <p><b>请求参数</b>：{@link RepoTemplateRequest}（@RequestBody JSON）——题库与模板关联信息。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#batchAddRepoTemplate(RepoTemplateRequest)}。</p>
	 *
	 * @param request 批量入题请求
	 */
	@PostMapping("/batchCreate")
	@PreAuthorize("hasAuthority('repo:create')")
	public void batchAddRepoTemplate(@RequestBody RepoTemplateRequest request) {
		repoService.batchAddRepoTemplate(request);
	}

	/**
	 * 批量绑定已有题目到题库。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/bind（如 /api/repo/bind）。</p>
	 *
	 * <p><b>功能</b>：将题目管理中的一批已有题目（ids）绑定到指定题库（repoId），
	 * 仅更新题目归属，不修改题目内容；已在目标题库的题目自动跳过（幂等）。</p>
	 *
	 * <p><b>请求参数</b>：{@link RepoTemplateRequest}（@RequestBody JSON，repoId + ids）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#bindTemplates(RepoTemplateRequest)}。</p>
	 *
	 * @param request 绑定请求（repoId 目标题库 + ids 题目列表）
	 */
	@PostMapping("/bind")
	@PreAuthorize("hasAuthority('repo:create')")
	public void bindTemplates(@RequestBody RepoTemplateRequest request) {
		repoService.bindTemplates(request);
	}

	/**
	 * 模板解绑题库。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/unbind（如 /api/repo/unbind）。</p>
	 *
	 * <p><b>功能</b>：将题库与一批模板解除绑定（题目从题库中移除关联，不删除模板本身）。</p>
	 *
	 * <p><b>请求参数</b>：{@link RepoTemplateRequest}（@RequestBody JSON，题库与模板关联信息）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：无 @PreAuthorize 注解（是否要求登录由全局安全规则控制）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#batchUnBindTemplate(RepoTemplateRequest)}。</p>
	 *
	 * @param request 解绑请求
	 */
	@PostMapping("/unbind")
	public void batchUnBindTemplate(@RequestBody RepoTemplateRequest request) {
		repoService.batchUnBindTemplate(request);
	}

	/**
	 * 从题库里面挑选试题。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/pick（如 /api/repo/pick）。</p>
	 *
	 * <p><b>功能</b>：问卷编辑器"随机抽题"场景——按随机条件（题库 + 抽题数量）从题库
	 * 挑选题目，返回可直接放入问卷 schema 的题目数据。</p>
	 *
	 * <p><b>请求参数</b>：{@code List<ProjectSetting.RandomSurveyCondition>}（@RequestBody JSON 数组）——
	 * 每个元素描述一次随机抽题条件（题库 id、抽题数等）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<SurveySchema>}（挑选出的题目 schema 列表）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#pickQuestionFromRepo(List)}。</p>
	 *
	 * @param repos 随机抽题条件列表
	 * @return 挑选出的题目 schema 列表
	 */
	@PostMapping("/pick")
	public List<SurveySchema> pickQuestionFromRepo(@RequestBody List<ProjectSetting.RandomSurveyCondition> repos) {
		return repoService.pickQuestionFromRepo(repos);
	}

	/**
	 * 从模板导入题库。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/import（如 /api/repo/import）。</p>
	 *
	 * <p><b>功能</b>：将指定模板导入到题库中（与 batchCreate 类似，导入方向为模板 → 题库）。</p>
	 *
	 * <p><b>请求参数</b>：{@link RepoTemplateRequest}（@RequestBody JSON，题库与模板关联信息）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：无 @PreAuthorize 注解。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#importFromTemplate(RepoTemplateRequest)}。</p>
	 *
	 * @param request 导入请求
	 */
	@PostMapping("/import")
	public void importFromTemplate(RepoTemplateRequest request) {
		repoService.importFromTemplate(request);
	}

	/**
	 * 我的笔记列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/repo/book/list（如 /api/repo/book/list）。</p>
	 *
	 * <p><b>功能</b>：分页查询当前用户的"笔记"列表（对题目/知识点的个人笔记），
	 * 供笔记管理页展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link UserBookQuery}（GET 查询参数）——分页 + 筛选条件。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;UserBookView&gt;。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:book')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#listUserBook(UserBookQuery)}。</p>
	 *
	 * @param query 笔记查询参数
	 * @return 笔记分页列表
	 */
	@GetMapping("/book/list")
	@PreAuthorize("hasAuthority('repo:book')")
	public PaginationResponse<UserBookView> listUserBook(UserBookQuery query) {
		return repoService.listUserBook(query);
	}

	/**
	 * 创建笔记。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/book/create
	 * （如 /api/repo/book/create）。</p>
	 *
	 * <p><b>功能</b>：为当前用户创建一条笔记。</p>
	 *
	 * <p><b>请求参数</b>：{@link UserBookRequest}（@RequestBody JSON）——笔记内容及关联题目/知识点。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:book')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#createUserBook(UserBookRequest)}。</p>
	 *
	 * @param request 笔记创建请求
	 */
	@PostMapping("/book/create")
	@PreAuthorize("hasAuthority('repo:book')")
	public void createUserBook(@RequestBody UserBookRequest request) {
		repoService.createUserBook(request);
	}

	/**
	 * 更新笔记。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/book/update
	 * （如 /api/repo/book/update）。</p>
	 *
	 * <p><b>功能</b>：更新笔记内容，返回更新后的笔记视图。</p>
	 *
	 * <p><b>请求参数</b>：{@link UserBookRequest}（@RequestBody JSON，含笔记 id）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link UserBookView}（更新后的笔记视图）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:book')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#updateUserBook(UserBookRequest)}。</p>
	 *
	 * @param request 笔记更新请求
	 * @return 更新后的笔记视图
	 */
	@PostMapping("/book/update")
	@PreAuthorize("hasAuthority('repo:book')")
	public UserBookView updateUserBook(@RequestBody UserBookRequest request) {
		return repoService.updateUserBook(request);
	}

	/**
	 * 删除笔记。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/book/delete
	 * （如 /api/repo/book/delete）。</p>
	 *
	 * <p><b>功能</b>：按 id 删除当前用户的笔记。</p>
	 *
	 * <p><b>请求参数</b>：{@link UserBookRequest}（@RequestBody JSON，含笔记 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:book')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#deleteUserBook(UserBookRequest)}。</p>
	 *
	 * @param request 笔记删除请求
	 */
	@PostMapping("/book/delete")
	@PreAuthorize("hasAuthority('repo:book')")
	public void deleteUserBook(@RequestBody UserBookRequest request) {
		repoService.deleteUserBook(request);
	}

	/**
	 * 导出题库题目。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/repo/export（如 /api/repo/export）。</p>
	 *
	 * <p><b>功能</b>：将题库中的题目导出为 Excel 文件（响应流由服务层写出）。
	 * 注意：权限注解被注释掉（见代码中的 @PreAuthorize 注释行），当前任何登录用户均可导出。</p>
	 *
	 * <p><b>请求参数</b>：{@link RepoRequest}（GET 查询参数，含题库 id/筛选条件）。</p>
	 *
	 * <p><b>返回值结构</b>：无方法返回值；服务层直接把 Excel 写入 HTTP 响应流。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#exportRepoQuestions(RepoRequest)}。</p>
	 *
	 * @param request 导出查询参数
	 */
	@GetMapping("/export")
	// @PreAuthorize("hasAuthority('repo:export')")
	public void exportRepoQuestions(RepoRequest request) {
		repoService.exportRepoQuestions(request);
	}

	/**
	 * 学员端「我的题库」。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/repo/my（如 /api/repo/my）。</p>
	 *
	 * <p><b>功能</b>：查询当前登录学员可练习的题库列表
	 * （老师手动分配 ∪ 系统按标签自动匹配），每项回填题目总数。
	 * 学员端选题页以此为数据源，只能选择分配到的题库，不能勾选单题。</p>
	 *
	 * <p><b>返回值结构</b>：List&lt;RepoView&gt;。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("isAuthenticated()")——登录即可（学员端入口）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#myRepos()}。</p>
	 *
	 * @return 我的题库列表
	 */
	@GetMapping("/my")
	@PreAuthorize("isAuthenticated()")
	public List<RepoView> myRepos() {
		return repoService.myRepos();
	}

	/**
	 * 老师手动分配题库给学员。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/assign（如 /api/repo/assign）。</p>
	 *
	 * <p><b>功能</b>：为指定学员批量分配题库（幂等，已分配的跳过）。
	 * 分配后学员端「我的题库」即可见。</p>
	 *
	 * <p><b>请求参数</b>：{@link RepoAssignRequest}（@RequestBody JSON）——userId + repoIds。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#assignRepo(String, java.util.List)}。</p>
	 *
	 * @param request 分配请求
	 */
	@PostMapping("/assign")
	@PreAuthorize("hasAuthority('repo:list')")
	public void assignRepo(@RequestBody RepoAssignRequest request) {
		repoService.assignRepo(request.getUserId(), request.getRepoIds());
	}

	/**
	 * 删除分配记录。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/assign/delete（如 /api/repo/assign/delete）。</p>
	 *
	 * <p><b>功能</b>：按分配记录 id 批量删除（逻辑删除），删除后学员端不再可见该题库。</p>
	 *
	 * <p><b>请求参数</b>：{@link RepoAssignRequest}（@RequestBody JSON）——ids。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#deleteAssign(java.util.List)}。</p>
	 *
	 * @param request 删除请求
	 */
	@PostMapping("/assign/delete")
	@PreAuthorize("hasAuthority('repo:list')")
	public void deleteAssign(@RequestBody RepoAssignRequest request) {
		repoService.deleteAssign(request.getIds());
	}

	/**
	 * 查询学员分配记录（管理端）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/repo/assign/list（如 /api/repo/assign/list）。</p>
	 *
	 * <p><b>功能</b>：查询指定学员（或全部）的题库分配记录，含学员姓名/题库名称。</p>
	 *
	 * <p><b>请求参数</b>：userId（GET 查询参数，可选，为空查全部）。</p>
	 *
	 * <p><b>返回值结构</b>：List&lt;RepoAssignView&gt;。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#listAssign(String)}。</p>
	 *
	 * @param userId 学员用户 ID（可选）
	 * @return 分配记录列表
	 */
	@GetMapping("/assign/list")
	@PreAuthorize("hasAuthority('repo:list')")
	public List<RepoAssignView> listAssign(String userId) {
		return repoService.listAssign(userId);
	}

	/**
	 * 查询学员标签。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/repo/user/tags（如 /api/repo/user/tags）。</p>
	 *
	 * <p><b>功能</b>：查询学员标签（category=user），用于「按标签自动分配题库」的规则设置。</p>
	 *
	 * <p><b>请求参数</b>：userId（GET 查询参数，必填）。</p>
	 *
	 * <p><b>返回值结构</b>：Set&lt;String&gt;。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#getUserTags(String)}。</p>
	 *
	 * @param userId 学员用户 ID
	 * @return 学员标签集合
	 */
	@GetMapping("/user/tags")
	@PreAuthorize("hasAuthority('repo:list')")
	public Set<String> getUserTags(String userId) {
		return repoService.getUserTags(userId);
	}

	/**
	 * 保存学员标签。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/repo/user/tags（如 /api/repo/user/tags）。</p>
	 *
	 * <p><b>功能</b>：覆盖式保存学员标签（category=user）。题库 tag 与学员标签有交集时，
	 * 系统自动将该题库分配到该学员的「我的题库」。</p>
	 *
	 * <p><b>请求参数</b>：{@link RepoAssignRequest}（@RequestBody JSON）——userId + tags。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('repo:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link RepoService#saveUserTags(String, String[])}。</p>
	 *
	 * @param request 标签保存请求
	 */
	@PostMapping("/user/tags")
	@PreAuthorize("hasAuthority('repo:list')")
	public void saveUserTags(@RequestBody RepoAssignRequest request) {
		repoService.saveUserTags(request.getUserId(), request.getTags());
	}

}
