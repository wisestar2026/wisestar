package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统管理接口（SystemApi）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层，Spring MVC REST Controller）。</p>
 * <p><b>类职责</b>：提供系统级管理功能：系统信息查询/更新、AI 设置、角色管理、
 * 权限管理、系统用户管理、岗位管理、部门管理、字典/字典条目管理。是后台管理端
 * "系统管理"菜单对应的后端接口集合。</p>
 * <p><b>请求路径前缀</b>：类级路径为 {@code ${api.prefix}/system}（api.prefix 通常为 /api），
 * 各方法再追加子路径（如 /api/system/user/list、/api/system/role/list 等）。</p>
 * <p><b>被谁调用</b>：前端管理后台的系统管理模块页面：系统信息设置页、AI 设置页、
 * 角色管理页、权限管理页、系统用户管理页、岗位管理页、部门管理页、字典管理页。</p>
 * <p><b>依赖的服务</b>：</p>
 * <ul>
 *   <li>{@link SystemService}——系统信息、AI 设置、角色、权限相关业务；</li>
 *   <li>{@link UserService}——系统用户管理业务；</li>
 *   <li>{@link PositionService}——岗位管理业务；</li>
 *   <li>{@link DeptService}——部门管理业务；</li>
 *   <li>{@link DictService}——字典与字典条目业务；</li>
 *   <li>{@link MessageSource}——i18n 国际化消息源，用于删除类接口的校验提示文案。</li>
 * </ul>
 *
 * <p><b>数据流概览</b>：前端 HTTP 请求 → 本类各方法（权限注解校验 + 参数校验）→
 * 对应的 shared Service 接口方法 → rdbms 模块实现 → MyBatis Mapper → 数据库 → 结果返回。</p>
 *
 * @author javahuang
 * @date 2021/10/12
 */
@RestController
@RequestMapping("${api.prefix}/system")
@RequiredArgsConstructor
public class SystemApi {

	/**
	 * 系统信息服务（系统信息、AI 设置、角色、权限管理入口）。
	 */
	private final SystemService systemService;

	/**
	 * 用户服务（系统用户管理入口）。
	 */
	private final UserService userService;

	/**
	 * 岗位服务（岗位管理入口）。
	 */
	private final PositionService positionService;

	/**
	 * 部门服务（部门管理入口）。
	 */
	private final DeptService deptService;

	/**
	 * 字典服务（字典与字典条目管理入口）。
	 */
	private final DictService dictService;

	/**
	 * i18n 消息源，配合 LocaleContextHolder 读取国际化校验提示（如删除前至少保留一条）。
	 */
	private final MessageSource messageSource;

	/**
	 * 获取当前系统信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/system（如 /api/system）。</p>
	 *
	 * <p><b>功能</b>：返回系统公开配置信息（系统名称、描述、图标、默认语言、版本、
	 * 注册开关、验证码开关、版权、备案号、RSA 公钥、AI 是否启用等）。
	 * 登录页/注册页/全局展示均依赖此接口（前端用它取 publicKey 做密码 RSA 加密）。</p>
	 *
	 * <p><b>请求参数</b>：无。</p>
	 *
	 * <p><b>返回值结构</b>：{@link SystemInfo}（见 shared 模块 DTO，含嵌套的
	 * RegisterInfo、SystemSetting、AiSetting 子结构）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SystemService#getSystemInfo()}。</p>
	 *
	 * @return 当前系统信息
	 */
	@GetMapping
	public SystemInfo getSystemInfo() {
		return systemService.getSystemInfo();
	}

	/**
	 * 获取系统 AI 设置。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/system/aiSetting（如 /api/system/aiSetting）。</p>
	 *
	 * <p><b>功能</b>：返回 AI 功能设置（是否启用、可用模型列表、提示词、Token）。
	 * 安全处理：返回前将 token（AI 平台 API Key）置空，避免敏感凭证泄露给前端。
	 * ai 模块的 SiliconflowChatServiceImpl 会通过 SystemService#getSystemAiSetting
	 * 读取同样的配置（包含 token）用于调用大模型 API。</p>
	 *
	 * <p><b>请求参数</b>：无。</p>
	 *
	 * <p><b>返回值结构</b>：{@code SystemInfo.AiSetting}（enabled、models、prompt；token 恒为 null）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasRole('admin')")——仅管理员可查。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SystemService#getSystemAiSetting()}。</p>
	 *
	 * @return AI 设置（Token 已被脱敏为 null）
	 */
	@GetMapping("/aiSetting")
	@PreAuthorize("hasRole('admin')")
	public SystemInfo.AiSetting getSystemAiSetting() {
		SystemInfo.AiSetting aiSetting = systemService.getSystemAiSetting();
		if (aiSetting != null) {
			aiSetting.setToken(null);
		}
		return aiSetting;
	}

	/**
	 * 更新系统信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/update（如 /api/system/update）。</p>
	 *
	 * <p><b>功能</b>：更新系统配置信息（系统名称、描述、图标、语言、注册开关、
	 * 验证码开关、版权、备案号、AI 设置含 token/模型/prompt 等）。</p>
	 *
	 * <p><b>请求参数</b>：{@link SystemInfoRequest}（@RequestBody JSON）——系统信息更新请求体，
	 * 包含 SystemInfo 各字段及嵌套设置对象。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasRole('admin')")——仅管理员可操作。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SystemService#updateSystemInfo(SystemInfoRequest)}。</p>
	 *
	 * @param request 系统信息更新请求体
	 */
	@PostMapping("/update")
	@PreAuthorize("hasRole('admin')")
	public void updateSystemInfo(@RequestBody SystemInfoRequest request) {
		systemService.updateSystemInfo(request);
	}

	/**
	 * 获取系统角色列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：任意方法 + ${api.prefix}/system/role/list
	 * （@RequestMapping 不限定方法，如 /api/system/role/list）。</p>
	 *
	 * <p><b>功能</b>：分页查询系统角色列表（角色名、编码、描述、成员数等），
	 * 供角色管理页表格展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link RoleQuery}（分页及筛选条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;RoleView&gt;（分页包装的角色列表）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:role:list')")——需要该权限点。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SystemService#getRoles(RoleQuery)}。</p>
	 *
	 * @param query 角色查询请求（分页参数）
	 * @return 角色分页列表
	 */
	@RequestMapping("/role/list")
	@PreAuthorize("hasAuthority('system:role:list')")
	public PaginationResponse<RoleView> roles(RoleQuery query) {
		return systemService.getRoles(query);
	}

	/**
	 * 添加系统角色。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/role/create
	 * （如 /api/system/role/create）。</p>
	 *
	 * <p><b>功能</b>：新建一个角色，并绑定角色对应的权限集合。</p>
	 *
	 * <p><b>请求参数</b>：{@link RoleRequest}（@RequestBody JSON）——角色名称、编码、
	 * 描述、勾选的权限 id 列表等。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:role:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SystemService#createRole(RoleRequest)}。</p>
	 *
	 * @param request 角色信息
	 */
	@PostMapping("/role/create")
	@PreAuthorize("hasAuthority('system:role:create')")
	public void createRole(@RequestBody RoleRequest request) {
		systemService.createRole(request);
	}

	/**
	 * 更新系统角色。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/role/update
	 * （如 /api/system/role/update）。</p>
	 *
	 * <p><b>功能</b>：更新角色基本信息及其绑定的权限集合。</p>
	 *
	 * <p><b>请求参数</b>：{@link RoleRequest}（@RequestBody JSON，含角色 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:role:update')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SystemService#updateRole(RoleRequest)}。</p>
	 *
	 * @param request 角色信息
	 */
	@PostMapping("/role/update")
	@PreAuthorize("hasAuthority('system:role:update')")
	public void updateRole(@RequestBody RoleRequest request) {
		systemService.updateRole(request);
	}

	/**
	 * 删除系统角色。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/role/delete
	 * （如 /api/system/role/delete）。</p>
	 *
	 * <p><b>功能</b>：删除角色。前置保护逻辑（Controller 层完成）：</p>
	 * <ol>
	 *   <li>请求体无 id 直接静默返回；</li>
	 *   <li>若当前系统中角色总数 &lt;= 1，则抛出 ValidationException（i18n 消息
	 *       system.role.delete.retainOne），提示必须至少保留一个角色。</li>
	 * </ol>
	 *
	 * <p><b>请求参数</b>：{@link RoleRequest}（@RequestBody JSON，含角色 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>异常</b>：唯一角色时抛 {@link ValidationException}，消息源读取国际化文案。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:role:delete')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SystemService#getRoles(RoleQuery)}
	 * → {@link SystemService#deleteRole(RoleRequest)}。</p>
	 *
	 * @param request 角色信息
	 */
	@PostMapping("/role/delete")
	@PreAuthorize("hasAuthority('system:role:delete')")
	public void deleteRole(@RequestBody RoleRequest request) {
		if (request.getId() == null) {
			return;
		}
		Long totalRoleObj = systemService.getRoles(new RoleQuery()).getTotal();
		long totalRoles = totalRoleObj == null ? 0 : totalRoleObj;
		// 统一口径：删除后至少保留 1 个角色（与 deleteUser 的 totalUsers - userIds.size() < 1 语义一致）
		if (totalRoles - 1 < 1) {
			throw new ValidationException(messageSource.getMessage("system.role.delete.retainOne", null,
					LocaleContextHolder.getLocale()));
		}
		systemService.deleteRole(request);
	}

	/**
	 * 获取系统权限列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：任意方法 + ${api.prefix}/system/permission/list
	 * （如 /api/system/permission/list）。</p>
	 *
	 * <p><b>功能</b>：返回系统中全部权限点（菜单/按钮权限树），供角色编辑页勾选权限使用。</p>
	 *
	 * <p><b>请求参数</b>：无。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<PermissionView>}（权限视图列表，含层级父子关系）。</p>
	 *
	 * <p><b>权限</b>：无 @PreAuthorize 注解（由全局安全规则控制）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SystemService#getPermissions()}。</p>
	 *
	 * @return 权限列表
	 */
	@RequestMapping("/permission/list")
	public List<PermissionView> permissions() {
		return systemService.getPermissions();
	}

	/**
	 * 比对数据库和代码里面配置的权限。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/system/permission/diff
	 * （如 /api/system/permission/diff）。</p>
	 *
	 * <p><b>功能</b>：开发/运维辅助接口——扫描代码中 @PreAuthorize 等注解声明的权限点，
	 * 与数据库权限表比对，输出差异（缺失/多余权限），用于排查权限配置不一致问题。</p>
	 *
	 * <p><b>请求参数</b>：无。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（差异结果由服务层内部日志输出）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasRole('admin')")——仅管理员。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SystemService#extractCodeDiffDbPermissions()}。</p>
	 */
	@GetMapping("/permission/diff")
	@PreAuthorize("hasRole('admin')")
	public void extractCodeDiffDbPermissions() {
		systemService.extractCodeDiffDbPermissions();
	}

	/**
	 * 系统用户列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：任意方法 + ${api.prefix}/system/user/list
	 * （如 /api/system/user/list）。</p>
	 *
	 * <p><b>功能</b>：分页查询系统全部用户（管理员视角，含用户名、角色、部门、岗位、
	 * 状态等），供系统用户管理页表格展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link UserQuery}（分页及筛选条件：关键字、角色、部门等）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;UserView&gt;（分页包装的用户列表）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:user:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#getUsers(UserQuery)}。</p>
	 *
	 * @param query 查询用户信息（分页参数）
	 * @return 用户分页列表
	 */
	@RequestMapping("/user/list")
	@PreAuthorize("hasAuthority('system:user:list')")
	public PaginationResponse<UserView> roles(UserQuery query) {
		return userService.getUsers(query);
	}

	/**
	 * 创建系统用户。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/user/create
	 * （如 /api/system/user/create）。</p>
	 *
	 * <p><b>功能</b>：管理员新建系统用户（设置用户名、初始密码、角色、部门、岗位等）。</p>
	 *
	 * <p><b>请求参数</b>：{@link UserRequest}（@RequestBody JSON，@Valid 校验）——用户信息 + 角色分配。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:user:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#createUser(UserRequest)}。</p>
	 *
	 * @param request 用户信息
	 */
	@PostMapping("/user/create")
	@PreAuthorize("hasAuthority('system:user:create')")
	public void createUser(@RequestBody @Valid UserRequest request) {
		userService.createUser(request);
	}

	/**
	 * 更新系统用户。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/user/update
	 * （如 /api/system/user/update）。</p>
	 *
	 * <p><b>功能</b>：管理员更新用户基本信息（昵称、邮箱、角色、部门、启用状态等）。</p>
	 *
	 * <p><b>请求参数</b>：{@link UserRequest}（@RequestBody JSON，@Valid 校验，含用户 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:user:update')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#updateUser(UserRequest)}。</p>
	 *
	 * @param request 用户信息
	 */
	@PostMapping("/user/update")
	@PreAuthorize("hasAuthority('system:user:update')")
	public void updateUser(@RequestBody @Valid UserRequest request) {
		userService.updateUser(request);
	}

	/**
	 * 更新用户岗位信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/user/updatePosition
	 * （如 /api/system/user/updatePosition）。</p>
	 *
	 * <p><b>功能</b>：单独更新指定用户的岗位绑定（与 updateUser 拆分，便于岗位调整场景）。</p>
	 *
	 * <p><b>请求参数</b>：{@link UserRequest}（@RequestBody JSON，@Valid 校验，含用户 id 与岗位 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:user:updatePosition')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#updateUserPosition(UserRequest)}。</p>
	 *
	 * @param request 用户岗位信息
	 */
	@PostMapping("/user/updatePosition")
	@PreAuthorize("hasAuthority('system:user:updatePosition')")
	public void updateUserPosition(@RequestBody @Valid UserRequest request) {
		userService.updateUserPosition(request);
	}

	/**
	 * 删除用户。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/user/delete
	 * （如 /api/system/user/delete）。</p>
	 *
	 * <p><b>功能</b>：按 id 批量删除用户（id 支持逗号分隔多个）。前置保护逻辑：</p>
	 * <ol>
	 *   <li>无 id 直接返回；</li>
	 *   <li>id 按逗号拆分、trim、去重、过滤空串；</li>
	 *   <li>不允许删除自己（id 列表含当前登录用户时抛 i18n 异常 system.user.delete.self）；</li>
	 *   <li>删除后系统剩余用户数必须 &gt;= 1（否则抛 system.user.delete.retainOne）。</li>
	 * </ol>
	 *
	 * <p><b>请求参数</b>：{@link UserRequest}（@RequestBody JSON，id 字段可含多个逗号分隔 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>异常</b>：删除自己/仅剩一个用户时抛 {@link ValidationException}（i18n 文案）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:user:delete')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#getUsers(UserQuery)}
	 * → 循环 {@link UserService#deleteUser(String)}。</p>
	 *
	 * @param request 用户信息（id 支持逗号分隔批量删除）
	 */
	@PostMapping("/user/delete")
	@PreAuthorize("hasAuthority('system:user:delete')")
	public void deleteUser(@RequestBody UserRequest request) {
		if (request.getId() == null) {
			return;
		}
		List<String> userIds = Arrays.stream(request.getId().split(",")).map(String::trim)
				.filter(id -> !id.isEmpty()).distinct().collect(Collectors.toList());
		if (userIds.isEmpty()) {
			return;
		}
		String currentUserId = SecurityContextUtils.getUserId();
		if (userIds.contains(currentUserId)) {
			throw new ValidationException(messageSource.getMessage("system.user.delete.self", null,
					LocaleContextHolder.getLocale()));
		}
		Long total = userService.getUsers(new UserQuery()).getTotal();
		long totalUsers = total == null ? 0 : total;
		if (totalUsers - userIds.size() < 1) {
			throw new ValidationException(messageSource.getMessage("system.user.delete.retainOne", null,
					LocaleContextHolder.getLocale()));
		}
		userIds.forEach(userService::deleteUser);
	}

	/**
	 * 检查登录名是否存在。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/system/checkUsernameExist
	 * （如 /api/system/checkUsernameExist?username=xxx）。</p>
	 *
	 * <p><b>功能</b>：校验用户名是否已被占用，供用户创建/编辑表单做实时重名校验。</p>
	 *
	 * <p><b>请求参数</b>：username（GET 查询参数，登录用户名）。</p>
	 *
	 * <p><b>返回值结构</b>：boolean——true 表示用户名已存在。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#checkUsernameExist(String)}。</p>
	 *
	 * @param username 登录用户名
	 * @return 用户名是否已存在（true=存在）
	 */
	@GetMapping("/checkUsernameExist")
	public boolean checkUsernameExist(String username) {
		return userService.checkUsernameExist(username);
	}

	/**
	 * 查询岗位列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/system/position/list
	 * （如 /api/system/position/list）。</p>
	 *
	 * <p><b>功能</b>：分页查询系统岗位列表（岗位名称、编码、状态等），供岗位管理页展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link PositionQuery}（分页及筛选条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;PositionView&gt;。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:position:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link PositionService#listPosition(PositionQuery)}。</p>
	 *
	 * @param query 岗位查询参数
	 * @return 岗位分页列表
	 */
	@GetMapping("/position/list")
	@PreAuthorize("hasAuthority('system:position:list')")
	public PaginationResponse<PositionView> listPosition(PositionQuery query) {
		return positionService.listPosition(query);
	}

	/**
	 * 添加岗位。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/position/create
	 * （如 /api/system/position/create）。</p>
	 *
	 * <p><b>功能</b>：新建岗位。</p>
	 *
	 * <p><b>请求参数</b>：{@link PositionRequest}（@RequestBody JSON，岗位名称/编码等）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:position:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link PositionService#addPosition(PositionRequest)}。</p>
	 *
	 * @param request 岗位信息
	 */
	@PostMapping("/position/create")
	@PreAuthorize("hasAuthority('system:position:create')")
	public void addPosition(@RequestBody PositionRequest request) {
		positionService.addPosition(request);
	}

	/**
	 * 更新岗位信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/position/update
	 * （如 /api/system/position/update）。</p>
	 *
	 * <p><b>功能</b>：更新岗位基本信息。</p>
	 *
	 * <p><b>请求参数</b>：{@link PositionRequest}（@RequestBody JSON，含岗位 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:position:update')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link PositionService#updatePosition(PositionRequest)}。</p>
	 *
	 * @param request 岗位信息
	 */
	@PostMapping("/position/update")
	@PreAuthorize("hasAuthority('system:position:update')")
	public void updatePosition(@RequestBody PositionRequest request) {
		positionService.updatePosition(request);
	}

	/**
	 * 删除岗位信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/position/delete
	 * （如 /api/system/position/delete）。</p>
	 *
	 * <p><b>功能</b>：按 id 删除岗位。</p>
	 *
	 * <p><b>请求参数</b>：{@link PositionRequest}（@RequestBody JSON，含岗位 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:position:delete')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link PositionService#deletePosition(String)}（注意传入的是 id）。</p>
	 *
	 * @param request 岗位信息
	 */
	@PostMapping("/position/delete")
	@PreAuthorize("hasAuthority('system:position:delete')")
	public void deletePosition(@RequestBody PositionRequest request) {
		positionService.deletePosition(request.getId());
	}

	/**
	 * 获取部门列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/system/dept/list
	 * （如 /api/system/dept/list）。</p>
	 *
	 * <p><b>功能</b>：获取部门树（全部部门，含父子层级），供部门管理页及用户编辑页部门选择器使用。</p>
	 *
	 * <p><b>请求参数</b>：无（null 表示查询全部）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<DeptView>}（部门视图树）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dept:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DeptService#listDept(null)}。</p>
	 *
	 * @return 部门列表
	 */
	@GetMapping("/dept/list")
	@PreAuthorize("hasAuthority('system:dept:list')")
	public List<DeptView> listDept() {
		return deptService.listDept(null);
	}

	/**
	 * 添加部门。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/dept/create
	 * （如 /api/system/dept/create）。</p>
	 *
	 * <p><b>功能</b>：新建部门节点（含父部门、排序号等）。</p>
	 *
	 * <p><b>请求参数</b>：{@link DeptRequest}（@RequestBody JSON，部门信息）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dept:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DeptService#addDept(DeptRequest)}。</p>
	 *
	 * @param request 部门信息
	 */
	@PostMapping("/dept/create")
	@PreAuthorize("hasAuthority('system:dept:create')")
	public void addOrg(@RequestBody DeptRequest request) {
		deptService.addDept(request);
	}

	/**
	 * 更新部门。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/dept/update
	 * （如 /api/system/dept/update）。</p>
	 *
	 * <p><b>功能</b>：更新部门名称、上级部门、排序等信息。</p>
	 *
	 * <p><b>请求参数</b>：{@link DeptRequest}（@RequestBody JSON，含部门 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dept:update')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DeptService#updateDept(DeptRequest)}。</p>
	 *
	 * @param request 部门信息
	 */
	@PostMapping("/dept/update")
	@PreAuthorize("hasAuthority('system:dept:update')")
	public void updateOrg(@RequestBody DeptRequest request) {
		deptService.updateDept(request);
	}

	/**
	 * 删除部门。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/dept/delete
	 * （如 /api/system/dept/delete）。</p>
	 *
	 * <p><b>功能</b>：删除部门。前置保护逻辑：无 id 直接返回；若删除前部门总数 &lt;= 1
	 * 则抛 i18n 异常 system.dept.delete.retainOne（至少保留一个部门）。</p>
	 *
	 * <p><b>请求参数</b>：{@link DeptRequest}（@RequestBody JSON，含部门 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>异常</b>：仅剩一个部门时抛 {@link ValidationException}（i18n 文案）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dept:delete')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DeptService#listDept(null)}
	 * → {@link DeptService#deleteDept(String)}。</p>
	 *
	 * @param request 部门信息
	 */
	@PostMapping("/dept/delete")
	@PreAuthorize("hasAuthority('system:dept:delete')")
	public void deleteOrg(@RequestBody DeptRequest request) {
		if (request.getId() == null) {
			return;
		}
		List<DeptView> depts = deptService.listDept(null);
		if (depts == null || depts.size() <= 1) {
			throw new ValidationException(messageSource.getMessage("system.dept.delete.retainOne", null,
					LocaleContextHolder.getLocale()));
		}
		deptService.deleteDept(request.getId());
	}

	/**
	 * 部门排序。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/dept/sort
	 * （如 /api/system/dept/sort）。</p>
	 *
	 * <p><b>功能</b>：调整部门节点间的排序（同级排序调整，前端拖拽后提交新排序）。</p>
	 *
	 * <p><b>请求参数</b>：{@link DeptSortRequest}（@RequestBody JSON，含部门 id 与排序值）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dept:create')")——复用创建权限。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DeptService#sortDept(DeptSortRequest)}。</p>
	 *
	 * @param request 部门排序请求
	 */
	@PostMapping("/dept/sort")
	@PreAuthorize("hasAuthority('system:dept:create')")
	public void sortOrg(@RequestBody DeptSortRequest request) {
		deptService.sortDept(request);
	}

	/**
	 * 获取字典项列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/system/dict/list
	 * （如 /api/system/dict/list）。</p>
	 *
	 * <p><b>功能</b>：分页查询系统字典（字典名、编码、描述等），供字典管理页展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link CommDictQuery}（分页及筛选条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;CommDictView&gt;。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dict:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DictService#listDict(CommDictQuery)}。</p>
	 *
	 * @param query 字典分页参数
	 * @return 字典分页列表
	 */
	@GetMapping("/dict/list")
	@PreAuthorize("hasAuthority('system:dict:list')")
	public PaginationResponse<CommDictView> listDict(CommDictQuery query) {
		return dictService.listDict(query);
	}

	/**
	 * 创建字典项。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/dict/create
	 * （如 /api/system/dict/create）。</p>
	 *
	 * <p><b>功能</b>：新建一个字典（如"性别""状态"等字典定义）。</p>
	 *
	 * <p><b>请求参数</b>：{@link CommDictRequest}（@RequestBody JSON，字典编码/名称等）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dict:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DictService#addDict(CommDictRequest)}。</p>
	 *
	 * @param request 字典信息
	 */
	@PostMapping("/dict/create")
	@PreAuthorize("hasAuthority('system:dict:create')")
	public void addDict(@RequestBody CommDictRequest request) {
		dictService.addDict(request);
	}

	/**
	 * 更新字典项。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/dict/update
	 * （如 /api/system/dict/update）。</p>
	 *
	 * <p><b>功能</b>：更新字典基本信息。</p>
	 *
	 * <p><b>请求参数</b>：{@link CommDictRequest}（@RequestBody JSON，含字典 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dict:update')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DictService#updateDict(CommDictRequest)}。</p>
	 *
	 * @param request 字典信息
	 */
	@PostMapping("/dict/update")
	@PreAuthorize("hasAuthority('system:dict:update')")
	public void updateDict(@RequestBody CommDictRequest request) {
		dictService.updateDict(request);
	}

	/**
	 * 删除字典项。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/dict/delete
	 * （如 /api/system/dict/delete）。</p>
	 *
	 * <p><b>功能</b>：按 id 删除字典（同时处理其下字典条目的级联，具体由服务层决定）。</p>
	 *
	 * <p><b>请求参数</b>：{@link CommDictRequest}（@RequestBody JSON，含字典 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dict:delete')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DictService#deleteDict(String)}（传入 id）。</p>
	 *
	 * @param request 字典信息
	 */
	@PostMapping("/dict/delete")
	@PreAuthorize("hasAuthority('system:dict:delete')")
	public void deleteDict(@RequestBody CommDictRequest request) {
		dictService.deleteDict(request.getId());
	}

	/**
	 * 获取字典条目列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/system/dictItem/list
	 * （如 /api/system/dictItem/list）。</p>
	 *
	 * <p><b>功能</b>：分页查询某字典下的具体字典条目（键值对：label/value），
	 * 供字典条目管理页展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link CommDictItemQuery}（分页及所属字典过滤条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;CommDictItemView&gt;。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dictItem:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DictService#listDictItem(CommDictItemQuery)}。</p>
	 *
	 * @param query 字典条目查询参数
	 * @return 字典条目分页列表
	 */
	@GetMapping("/dictItem/list")
	@PreAuthorize("hasAuthority('system:dictItem:list')")
	public PaginationResponse<CommDictItemView> listDictItem(CommDictItemQuery query) {
		return dictService.listDictItem(query);
	}

	/**
	 * 添加字典条目。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/dictItem/create
	 * （如 /api/system/dictItem/create）。</p>
	 *
	 * <p><b>功能</b>：为字典新增一个字典条目（label/value 键值对）。</p>
	 *
	 * <p><b>请求参数</b>：{@link CommDictItemRequest}（@RequestBody JSON，所属字典 id + 条目值）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dictItem:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DictService#saveOrUpdateDictItem(CommDictItemRequest)}
	 * （新增与更新复用同一方法，由 id 是否为空区分）。</p>
	 *
	 * @param request 字典条目信息
	 */
	@PostMapping("/dictItem/create")
	@PreAuthorize("hasAuthority('system:dictItem:create')")
	public void createDictItem(@RequestBody CommDictItemRequest request) {
		dictService.saveOrUpdateDictItem(request);
	}

	/**
	 * 修改字典条目。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/dictItem/update
	 * （如 /api/system/dictItem/update）。</p>
	 *
	 * <p><b>功能</b>：更新字典条目（与 create 共用 saveOrUpdateDictItem，按 id 存在性区分新增/修改）。</p>
	 *
	 * <p><b>请求参数</b>：{@link CommDictItemRequest}（@RequestBody JSON，含条目 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dictItem:update')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DictService#saveOrUpdateDictItem(CommDictItemRequest)}。</p>
	 *
	 * @param request 字典条目信息
	 */
	@PostMapping("/dictItem/update")
	@PreAuthorize("hasAuthority('system:dictItem:update')")
	public void updateItem(@RequestBody CommDictItemRequest request) {
		dictService.saveOrUpdateDictItem(request);
	}

	/**
	 * 导入字典条目。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/dictItem/import
	 * （如 /api/system/dictItem/import）。</p>
	 *
	 * <p><b>功能</b>：批量导入字典条目（Excel 或 JSON 列表，由服务层解析落库）。</p>
	 *
	 * <p><b>请求参数</b>：CommDictItemRequest（multipart 表单绑定，含导入文件或条目数据）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dictItem:import')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DictService#importDictItem(CommDictItemRequest)}。</p>
	 *
	 * @param request 导入请求（含文件/数据）
	 */
	@PostMapping("/dictItem/import")
	@PreAuthorize("hasAuthority('system:dictItem:import')")
	public void importDictItem(CommDictItemRequest request) {
		dictService.importDictItem(request);
	}

	/**
	 * 删除字典条目。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/system/dictItem/delete
	 * （如 /api/system/dictItem/delete）。</p>
	 *
	 * <p><b>功能</b>：按 id 删除字典条目。</p>
	 *
	 * <p><b>请求参数</b>：{@link CommDictItemRequest}（@RequestBody JSON，含条目 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:dictItem:delete')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DictService#deleteDictItem(String)}（传入 id）。</p>
	 *
	 * @param request 字典条目信息
	 */
	@PostMapping("/dictItem/delete")
	@PreAuthorize("hasAuthority('system:dictItem:delete')")
	public void deleteDictItem(@RequestBody CommDictItemRequest request) {
		dictService.deleteDictItem(request.getId());
	}

}
