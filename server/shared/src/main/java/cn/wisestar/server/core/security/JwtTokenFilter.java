package cn.wisestar.server.core.security;

import cn.wisestar.server.core.config.WebSecurityConfig;
import cn.wisestar.server.core.constant.AppConsts;
import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * JWT 认证过滤器（JwtTokenFilter）。
 *
 * <p><b>所属模块</b>：shared 模块核心安全框架（cn.wisestar.server.core.security）。</p>
 * <p><b>类职责</b>：作为 Spring Security 过滤器链中的一个前置过滤器（注册在
 * {@code UsernamePasswordAuthenticationFilter} 之前），负责在每个 HTTP 请求进入
 * Controller 之前完成 JWT 令牌的解析与用户身份认证：</p>
 * <ul>
 *   <li>从请求 Cookie（{@link AppConsts#TOKEN_NAME}）或 URL 查询参数中取出 JWT 令牌；</li>
 *   <li>校验令牌的签名、有效期等合法性（委托 {@link JwtTokenUtil#validate}）；</li>
 *   <li>令牌合法时根据令牌内携带的 userId 加载用户信息（委托 {@link UserService#loadUserById}），
 *       组装 {@link UsernamePasswordAuthenticationToken} 并写入
 *       {@link SecurityContextHolder}，完成登录态的建立；</li>
 *   <li>若请求仅以 URL 参数携带令牌（如 H5 下载链接等场景），认证通过后还会将令牌
 *       回写为 HttpOnly Cookie，方便后续请求自动携带。</li>
 * </ul>
 * <p><b>为什么放行而不直接 401</b>：本过滤器对"无令牌 / 令牌非法"的请求不主动拦截，
 * 直接放行到后续流程——因为整个应用的授权控制是通过 Controller 方法上的
 * {@code @PreAuthorize} 注解（配合 {@code @EnableGlobalMethodSecurity}）完成的，
 * 未登录用户访问受保护接口时会被方法级安全抛出认证异常，再交给
 * {@link RestAuthenticationEntryPoint} 统一输出 JSON 错误。这样单 jar 部署时
 * 任意前端路由都可以直达静态页面。</p>
 *
 * <p><b>异常处理</b>：{@code loadUserById} 抛出 {@link AuthenticationException}
 * （如账号已禁用/不存在）时，由于该异常发生在 Spring Security 过滤器链内部，
 * 普通 {@code @RestControllerAdvice} 的 {@code GlobalExceptionHandler} 无法捕获，
 * 因此这里显式调用 {@link RestAuthenticationEntryPoint#commence} 输出
 * application/json 格式的未认证响应。</p>
 *
 * <p><b>调用方</b>：由 Spring Security 过滤器链自动调用（见
 * {@link WebSecurityConfig#configure(HttpSecurity)} 中的
 * {@code http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)}）。</p>
 *
 * @author javahuang
 * @date 2021/8/23
 */
@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

	/**
	 * JWT 工具类：负责令牌的生成、校验与用户信息解析（构造器注入）。
	 */
	private final JwtTokenUtil jwtTokenUtil;

	/**
	 * 用户服务：根据令牌中的 userId 加载用户详细信息（含权限列表），
	 * 用于构建 Spring Security 的认证对象。
	 */
	private final UserService userService;

	/**
	 * REST 认证失败入口点：在过滤器链内部认证失败时输出 JSON 格式的 401 响应。
	 */
	private final RestAuthenticationEntryPoint resolveException;

	/**
	 * 请求认证核心逻辑（每请求只执行一次，由 {@code OncePerRequestFilter} 保证）。
	 *
	 * <p><b>执行流程</b>：</p>
	 * <ol>
	 *   <li><b>取令牌</b>：优先从 Cookie 中读取；若配置了
	 *       {@code sk.security.url-token-authentication.enabled=true}（默认开启），
	 *       则同时允许从 URL 查询参数中读取同名参数作为令牌。</li>
	 *   <li><b>无令牌放行</b>：Cookie 与参数都取不到令牌时，不建立认证，直接放行到过滤器链后续环节
	 *       （未登录访问受保护接口将由方法级安全拦截）。</li>
	 *   <li><b>校验令牌</b>：调用 {@link JwtTokenUtil#validate} 校验签名与有效期，
	 *       非法令牌同样直接放行（不在此处拦截）。</li>
	 *   <li><b>建立认证</b>：按令牌内 userId 加载用户（{@link UserService#loadUserById}），
	 *       构造 {@link UsernamePasswordAuthenticationToken}（携带该用户的权限集合），
	 *       并绑定请求详情（IP、Session 等，见 {@link WebAuthenticationDetailsSource}），
	 *       最后写入 {@link SecurityContextHolder} 供后续 @PreAuthorize 校验使用。</li>
	 *   <li><b>令牌回写 Cookie</b>：若本次令牌来自 URL 参数（而非 Cookie），认证成功后将其
	 *       回写为 HttpOnly Cookie，实现"一次携带、后续自动带上"。</li>
	 *   <li><b>放行</b>：继续执行过滤器链，进入 Controller。</li>
	 * </ol>
	 *
	 * @param request  HTTP 请求（从中读取 Cookie / URL 参数中的令牌）
	 * @param response HTTP 响应（可能回写认证 Cookie）
	 * @param chain    过滤器链
	 * @throws ServletException 过滤器链执行异常
	 * @throws IOException      IO 异常
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		// Get authorization cookie and validate
		Cookie tokenFromCookie = WebUtils.getCookie(request, AppConsts.TOKEN_NAME);
		WebSecurityConfig securityConfig = ContextHelper.getBean(WebSecurityConfig.class);
		String tokenFromParameter = securityConfig.getUrlTokenAuthentication().isEnabled()
				? request.getParameter(AppConsts.TOKEN_NAME) : null;
		if (tokenFromCookie == null && isBlank(tokenFromParameter)) {
			chain.doFilter(request, response);
			return;
		}

		// Get jwt token and validate
		final String token = isNotBlank(tokenFromParameter) ? tokenFromParameter : tokenFromCookie.getValue().trim();
		if (!jwtTokenUtil.validate(token)) {
			chain.doFilter(request, response);
			return;
		}

		try {
			// Get user identity and set it on the spring security context
			UserDetails userDetails = userService.loadUserById(jwtTokenUtil.getUser(token).getUserId());

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
					null, ofNullable(userDetails).map(UserDetails::getAuthorities).orElse(new ArrayList<>()));
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

			SecurityContextHolder.getContext().setAuthentication(authentication);

			// Execute login
			if (tokenFromCookie == null && tokenFromParameter != null) {
				Cookie cookie = new Cookie(AppConsts.TOKEN_NAME, tokenFromParameter);
				cookie.setPath("/");
				cookie.setHttpOnly(true);
				response.addCookie(cookie);
			}

			chain.doFilter(request, response);
		}
		catch (AuthenticationException e) {
			// spring security filter 里面的异常，GlobalExceptionHandler 不能捕获
			resolveException.commence(request, response, e);
		}

	}

}
