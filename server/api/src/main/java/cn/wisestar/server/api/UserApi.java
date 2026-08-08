package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.AppConsts;
import cn.wisestar.server.core.constant.ErrorCode;
import cn.wisestar.server.core.exception.ErrorCodeException;
import cn.wisestar.server.core.security.JwtTokenUtil;
import cn.wisestar.server.core.uitls.RSAUtils;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.service.UserService;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * 用户相关接口（UserApi）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层，Spring MVC REST Controller）。</p>
 * <p><b>类职责</b>：提供用户侧核心接口：登录、登出、注册、当前用户信息、
 * 用户信息更新、注册角色查询、用户导入、我的任务/历史任务查询等。</p>
 * <p><b>请求路径前缀</b>：类级路径为 {@code ${api.prefix}}（api.prefix 通常为 /api），
 * 即该类的所有接口直接挂在 /api 下，无额外类级子路径。</p>
 * <p><b>被谁调用</b>：前端登录页（/api/public/login、/api/public/register）、
 * 管理后台用户中心（/api/currentUser、/api/userOverview、/api/user 更新）、
 * 首页任务看板（/api/listUserTask、/api/listHistoryTask）、
 * 用户管理页（/api/importUser）等。</p>
 * <p><b>依赖的服务/组件</b>：</p>
 * <ul>
 *   <li>{@link UserService}（shared 模块接口）——用户注册、查询、更新、任务查询等业务逻辑；</li>
 *   <li>{@link AuthenticationManager}——Spring Security 认证管理器，校验用户名密码；</li>
 *   <li>{@link JwtTokenUtil}——JWT 令牌生成工具；</li>
 *   <li>{@link RSAUtils}——RSA 解密工具（登录密码前端 RSA 加密传输，后端解密）；</li>
 *   <li>{@link SecurityContextUtils}——从 SecurityContext 读取当前登录用户 id；</li>
 *   <li>{@link CaptchaService}（anji-plus 滑动验证码）——注册等场景的验证码校验（部分流程使用）。</li>
 * </ul>
 *
 * <p><b>认证方式说明</b>：登录成功后服务端生成 JWT，通过两条通道返回给前端：
 * 1）写入 HttpOnly Cookie（TOKEN_NAME）；2）放在 Authorization 响应头。后续请求携带二者之一即可。</p>
 *
 * @author javahuang
 * @date 2021/10/12
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}")
public class UserApi {

	/**
	 * 用户服务（业务层入口，Lombok @RequiredArgsConstructor 构造器注入）。
	 */
	private final UserService userService;

	/**
	 * Spring Security 认证管理器（构造器注入），负责将用户名密码令牌交给
	 * AuthenticationProvider（DaoAuthenticationProvider + UserDetailsService）完成校验。
	 */
	private final AuthenticationManager authenticationManager;

	/**
	 * JWT 令牌工具（构造器注入），用于登录成功后签发访问令牌。
	 */
	private final JwtTokenUtil jwtTokenUtil;

	/**
	 * 用户登录。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/login（如 /api/public/login）。</p>
	 *
	 * <p><b>功能</b>：校验用户名密码，成功后生成 JWT 令牌并返回。密码流程：前端使用系统
	 * 公钥（/api/system 返回的 publicKey）RSA 加密密码明文，后端用 {@link RSAUtils#decrypt}
	 * 解密得到明文后交给 AuthenticationManager 认证。</p>
	 *
	 * <p><b>请求参数</b>：{@link AuthRequest}（@RequestBody JSON，@Valid 校验）——
	 * username（用户名）、password（RSA 密文密码），可能含验证码相关字段。</p>
	 *
	 * <p><b>返回值结构</b>：HTTP 200，无 body；响应头包含：</p>
	 * <ul>
	 *   <li>Set-Cookie：TOKEN_NAME=JWT（HttpOnly、path=/）——浏览器自动携带；</li>
	 *   <li>Authorization：JWT 字符串——非浏览器客户端（App/Postman）取此头使用。</li>
	 * </ul>
	 *
	 * <p><b>异常</b>：用户名或密码错误（RSA 解密失败、认证失败）统一捕获后抛出
	 * {@link ErrorCodeException}(ErrorCode.UsernameOrPasswordError)，由全局异常处理器
	 * 转成 401 及错误信息响应。</p>
	 *
	 * <p><b>调用的下层组件</b>：RSAUtils.decrypt → authenticationManager.authenticate
	 * → JwtTokenUtil.generateAccessToken(UserTokenView)。</p>
	 *
	 * <p><b>数据流</b>：前端 POST JSON → 本方法 → RSA 解密密码 → AuthenticationManager 认证
	 * （UserDetailsService 查询用户、BCrypt 比对密码）→ 成功取得 UserInfo
	 * → 生成 JWT（载荷含 userId）→ 写入 Cookie + Authorization 头返回。</p>
	 *
	 * @param request 登录请求（用户名 + RSA 加密后的密码）
	 * @return 携带 JWT 令牌（Cookie 与 Authorization 头）的空响应体
	 */
	@PostMapping("/public/login")
	public ResponseEntity login(@RequestBody @Valid AuthRequest request) {
		Authentication authentication;
		try {
			String decryptPwd = RSAUtils.decrypt(request.getPassword());
			authentication = new UsernamePasswordAuthenticationToken(request.getUsername(), decryptPwd);
			Authentication authenticate = authenticationManager.authenticate(authentication);
			UserInfo user = (UserInfo) authenticate.getPrincipal();
			HttpCookie cookie = ResponseCookie
					.from(AppConsts.TOKEN_NAME, jwtTokenUtil.generateAccessToken(new UserTokenView(user.getUserId())))
					.path("/").httpOnly(true).build();
			return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
					.header(HttpHeaders.AUTHORIZATION,
							jwtTokenUtil.generateAccessToken(new UserTokenView(user.getUserId())))
					.build();
		} catch (Exception e) {
			throw new ErrorCodeException(ErrorCode.UsernameOrPasswordError);
		}
	}

	/**
	 * 用户登出。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/logout（如 /api/public/logout）。</p>
	 *
	 * <p><b>功能</b>：清除浏览器端登录 Cookie——将 TOKEN_NAME 的值置空并把 maxAge 设为 0
	 * （立即过期），使浏览器后续不再携带该 Cookie。服务端 JWT 本身无状态，不做失效处理。</p>
	 *
	 * <p><b>请求参数</b>：无。</p>
	 *
	 * <p><b>返回值结构</b>：HTTP 200，仅返回 Set-Cookie（清空令牌）的响应头。</p>
	 *
	 * @return 清空认证 Cookie 的空响应体
	 */
	@PostMapping("/public/logout")
	public ResponseEntity logout() {
		HttpCookie cookie = ResponseCookie.from(AppConsts.TOKEN_NAME, "").path("/").httpOnly(true).maxAge(0).build();
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
	}

	/**
	 * 用户注册。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/register（如 /api/public/register）。</p>
	 *
	 * <p><b>功能</b>：创建新用户账号。系统是否开放注册由 SystemInfo.RegisterInfo.registerEnabled
	 * 控制，注册时按配置校验强密码规则、可选注册角色等（具体在 UserService.register 内实现）。</p>
	 *
	 * <p><b>请求参数</b>：{@link RegisterRequest}（@RequestBody JSON）——用户名、密码、
	 * 昵称、邮箱、注册角色等字段。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>异常</b>：用户名已存在、未开放注册等业务异常由 UserService 抛出
	 * ErrorCodeException，全局异常处理器统一处理。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#register(RegisterRequest)}。</p>
	 *
	 * @param request 注册请求体
	 */
	@PostMapping("/public/register")
	public void register(@RequestBody RegisterRequest request) {
		userService.register(request);
	}

	/**
	 * 获取当前登录用户信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/currentUser（如 /api/currentUser）。</p>
	 *
	 * <p><b>功能</b>：返回当前登录用户的完整信息（个人信息 + 角色 + 权限等），
	 * 前端登录后拉取用户信息与权限菜单即调用此接口。</p>
	 *
	 * <p><b>请求参数</b>：无（当前用户 id 从 SecurityContext 获取）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link UserInfo}（用户信息视图，含 userId、用户名、昵称、
	 * 角色列表、权限列表、所属部门/岗位等）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("isAuthenticated()")——需登录。</p>
	 *
	 * <p><b>调用的下层 Service</b>：SecurityContextUtils.getUserId()
	 * → {@link UserService#loadUserById(String)}。</p>
	 *
	 * @return 当前登录用户信息
	 */
	@GetMapping("/currentUser")
	@PreAuthorize("isAuthenticated()")
	public UserInfo currentUser() {
		return userService.loadUserById(SecurityContextUtils.getUserId());
	}

	/**
	 * 获取当前用户的总览数据。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/userOverview（如 /api/userOverview）。</p>
	 *
	 * <p><b>功能</b>：返回当前登录用户的工作台/首页总览数据（如我创建的问卷数、
	 * 我参与的问卷数、待办任务数等统计），供前端首页卡片展示。</p>
	 *
	 * <p><b>请求参数</b>：无。</p>
	 *
	 * <p><b>返回值结构</b>：{@link UserOverview}（用户总览统计视图）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("isAuthenticated()")——需登录。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#getUserOverviewData()}。</p>
	 *
	 * @return 当前用户的统计总览数据
	 */
	@GetMapping("/userOverview")
	@PreAuthorize("isAuthenticated()")
	public UserOverview userOverview() {
		return userService.getUserOverviewData();
	}

	/**
	 * 更新当前登录用户的个人信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/user（如 /api/user）。</p>
	 *
	 * <p><b>功能</b>：修改当前登录用户的个人资料（昵称、邮箱、头像等）。
	 * 安全控制：本接口只允许修改"本人"的信息——Controller 强制把请求体中的 id
	 * 覆盖为当前登录用户 id，防止越权修改他人资料。</p>
	 *
	 * <p><b>请求参数</b>：{@link UserRequest}（@RequestBody JSON）——需更新的用户资料字段。</p>
	 *
	 * <p><b>返回值结构</b>：更新后的 {@link UserInfo}（重新加载用户信息返回）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('user:update')")——需要该权限点。</p>
	 *
	 * <p><b>调用的下层 Service</b>：SecurityContextUtils.getUserId()
	 * → {@link UserService#updateUser(UserRequest)} → {@link UserService#loadUserById(String)}。</p>
	 *
	 * @param request 用户资料更新请求体（id 字段会被强制改为当前登录用户）
	 * @return 更新后的用户信息
	 */
	@PostMapping("/user")
	@PreAuthorize("hasAuthority('user:update')")
	public UserInfo updateUser(@RequestBody UserRequest request) {
		// 只有本人才能通过调用这个接口修改个人信息
		request.setId(SecurityContextUtils.getUserId());
		userService.updateUser(request);
		return userService.loadUserById(SecurityContextUtils.getUserId());
	}

	/**
	 * 获取注册时可选的角色列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/public/listRegisterRole
	 * （如 /api/public/listRegisterRole）。</p>
	 *
	 * <p><b>功能</b>：返回开放注册时可供新用户选择的角色列表（公开接口，无需登录），
	 * 供注册页角色下拉框使用。</p>
	 *
	 * <p><b>请求参数</b>：无。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<RegisterRoleView>}（角色视图列表：角色 id、名称等）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#getRegisterRoles()}。</p>
	 *
	 * @return 注册可选角色列表
	 */
	@GetMapping("/public/listRegisterRole")
	public List<RegisterRoleView> getRegisterRoles() {
		return userService.getRegisterRoles();
	}

	/**
	 * 导入用户。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/importUser（如 /api/importUser）。</p>
	 *
	 * <p><b>功能</b>：按 Excel 模板批量导入用户（管理员在用户管理页下载模板、填写后上传导入），
	 * 请求为 multipart 表单（文件 + 参数绑定到 UserRequest）。</p>
	 *
	 * <p><b>请求参数</b>：UserRequest（multipart 表单绑定）——含上传的 Excel 文件流等。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('home')")——需登录且有 home 权限。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#importUser(UserRequest)}。</p>
	 *
	 * @param request 导入用户请求（含 Excel 文件）
	 */
	@PostMapping("/importUser")
	@PreAuthorize("hasAuthority('home')")
	public void importUser(UserRequest request) {
		userService.importUser(request);
	}

	/**
	 * 查询用户任务。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/listUserTask（如 /api/listUserTask）。</p>
	 *
	 * <p><b>功能</b>：分页查询当前登录用户收到的"待办/进行中"任务列表
	 * （例如问卷分发、审批等协同任务），供首页任务看板展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link MyTaskQuery}（GET 查询参数）——分页参数 pageNo/pageSize
	 * 及任务筛选条件。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;MyTaskView&gt;（分页包装：
	 * total + 当前页任务列表）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('home')")——需登录且有 home 权限。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#queryTask(MyTaskQuery)}。</p>
	 *
	 * @param query 任务查询参数（GET 绑定）
	 * @return 用户任务分页结果
	 */
	@GetMapping("/listUserTask")
	@PreAuthorize("hasAuthority('home')")
	public PaginationResponse<MyTaskView> myTask(MyTaskQuery query) {
		return userService.queryTask(query);
	}

	/**
	 * 查询历史任务。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/listHistoryTask
	 * （如 /api/listHistoryTask）。</p>
	 *
	 * <p><b>功能</b>：分页查询当前登录用户的"已完成/历史"任务列表（与 listUserTask 相对），
	 * 供首页任务看板的历史 Tab 展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link MyTaskQuery}（GET 查询参数，同 queryTask）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;MyTaskView&gt;（分页包装）。</p>
	 *
	 * <p><b>权限</b>：无 @PreAuthorize 注解——但 Spring Security 全局规则仍要求登录
	 * （具体由 SecurityConfig 的放行/拦截规则决定）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#queryHistoryTask(MyTaskQuery)}。</p>
	 *
	 * @param query 历史任务查询参数（GET 绑定）
	 * @return 历史任务分页结果
	 */
	@GetMapping("/listHistoryTask")
	public PaginationResponse<MyTaskView> myHistoryTask(MyTaskQuery query) {
		return userService.queryHistoryTask(query);
	}

	/**
	 * 分页查询用户列表（管理端：题库分配选学员等场景）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/user/list（如 /api/user/list）。</p>
	 *
	 * <p><b>功能</b>：分页查询系统用户（按姓名模糊筛选），
	 * 供管理端「题库分配」页选择学员使用。</p>
	 *
	 * <p><b>请求参数</b>：{@link UserQuery}（GET 查询参数）——分页 + name 模糊筛选。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;UserView&gt;。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('system:user:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link UserService#getUsers(UserQuery)}。</p>
	 *
	 * @param query 用户查询参数
	 * @return 用户分页列表
	 */
	@GetMapping("/user/list")
	@PreAuthorize("hasAuthority('system:user:list')")
	public PaginationResponse<UserView> listUser(UserQuery query) {
		return userService.getUsers(query);
	}

}
