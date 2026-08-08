package cn.wisestar.server.core.config;

import cn.wisestar.server.core.security.JwtTokenFilter;
import cn.wisestar.server.core.security.RestAuthenticationEntryPoint;
import cn.wisestar.server.service.UserService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Spring Security 核心配置（WebSecurityConfig）。
 *
 * <p><b>所属模块</b>：shared 模块核心框架配置包（cn.wisestar.server.core.config）。</p>
 * <p><b>类职责</b>：基于 Spring Security 的 WebSecurityConfigurerAdapter，
 * 定义整个后端的安全策略：</p>
 * <ul>
 *   <li><b>启用全局方法级安全</b>（@EnableGlobalMethodSecurity）：
 *       Controller 上的 @Secured / @PreAuthorize 注解生效，授权控制粒度到方法；</li>
 *   <li><b>无状态会话</b>（STATELESS）：不创建 HttpSession，认证状态完全依赖 JWT；</li>
 *   <li><b>关闭 CSRF</b>、开启 CORS（自定义 CorsFilter，允许任意来源携带凭据）；</li>
 *   <li><b>URL 级访问规则</b>：公开接口（/api/public/**、/api/system、/captcha/get、
 *       /captcha/check、GET /api/file/**、/）permitAll，其余 /api/** 要求认证，
 *       但实际授权主要靠方法级注解，URL 规则只做兜底；</li>
 *   <li><b>注册 JWT 过滤器</b>：把 {@link JwtTokenFilter} 插入到
 *       UsernamePasswordAuthenticationFilter 之前，实现无状态 JWT 认证；</li>
 *   <li><b>认证失败处理</b>：使用 {@link RestAuthenticationEntryPoint} 输出 JSON；</li>
 *   <li><b>支持 URL 参数携带令牌</b>（urlTokenAuthentication，默认开启），
 *       用于文件下载等无法携带 Cookie 的场景。</li>
 * </ul>
 *
 * <p><b>外部化配置</b>：本类同时是 {@code @ConfigurationProperties("sk.security")}，
 * 可通过配置文件前缀 {@code sk.security} 绑定属性（如
 * {@code sk.security.url-token-authentication.enabled} 控制 URL 参数令牌认证开关）。</p>
 *
 * <p><b>认证机制</b>：{@link #configure(AuthenticationManagerBuilder)} 指定使用
 * {@link UserService#loadUserByUsername} 加载用户（配合 {@link PasswordEncoder}
 * 做 BCrypt 密码比对）。</p>
 *
 * @author javahuang
 * @date 2021/8/6
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@ConfigurationProperties("sk.security")
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	/**
	 * URL 参数令牌认证配置（默认开启）。
	 *
	 * <p>开启后，{@link JwtTokenFilter} 除了从 Cookie 读取令牌外，还允许从
	 * URL 查询参数（参数名见 {@link cn.wisestar.server.core.constant.AppConsts#TOKEN_NAME}）
	 * 读取令牌，用于下载链接、邮件链接等无法自动携带 Cookie 的场景。</p>
	 */
	private final UrlTokenAuthentication urlTokenAuthentication = new UrlTokenAuthentication();

	/**
	 * JWT 认证过滤器：负责解析请求携带的令牌并建立 SecurityContext（构造器注入）。
	 */
	private final JwtTokenFilter jwtTokenFilter;

	/**
	 * 用户服务：提供 loadUserByUsername / loadUserById 加载用户信息（构造器注入）。
	 */
	private final UserService userService;

	/**
	 * 认证失败入口点：未认证访问受保护资源时输出 JSON 错误（构造器注入）。
	 */
	private final RestAuthenticationEntryPoint authenticationEntryPoint;

	public WebSecurityConfig(JwtTokenFilter jwtTokenFilter, UserService userService,
			RestAuthenticationEntryPoint authenticationEntryPoint) {
		this.jwtTokenFilter = jwtTokenFilter;
		this.userService = userService;
		this.authenticationEntryPoint = authenticationEntryPoint;
		// 允许在 @Async 方法里面获取 SecurityContext
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
	}

	/**
	 * 配置认证管理器：指定用户加载方式与密码编码器。
	 *
	 * <p>通过 {@code userDetailsService} 注册 UserService.loadUserByUsername，
	 * 配合全局 {@link PasswordEncoder}（BCrypt）完成用户名 + 密码的认证。
	 * 预留了 {@code auth.authenticationProvider()} 扩展点，可追加更多认证方式。</p>
	 *
	 * @param auth 认证管理器构建器
	 * @throws Exception 配置异常
	 */
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(username -> userService.loadUserByUsername(username));
		// 添加更多类型的认证方式 auth.authenticationProvider();
	}

	/**
	 * 配置 HTTP 级安全规则（过滤器链）。
	 *
	 * <p><b>规则明细</b>：</p>
	 * <ul>
	 *   <li>禁用 X-Frame-Options（允许 iframe 嵌入，如问卷预览页面被嵌入）；</li>
	 *   <li>开启 CORS、关闭 CSRF；</li>
	 *   <li>无状态会话（STATELESS）；</li>
	 *   <li>认证失败走 {@link RestAuthenticationEntryPoint}；</li>
	 *   <li>URL 匹配规则（见类注释），所有请求默认放行 + 注解授权，保证单 jar 部署时
	 *       任意路由都能命中前端静态页面；</li>
	 *   <li>在 UsernamePasswordAuthenticationFilter 之前插入 {@link JwtTokenFilter}。</li>
	 * </ul>
	 *
	 * @param http HttpSecurity 构建器
	 * @throws Exception 配置异常
	 */
	@Override
	public void configure(HttpSecurity http) throws Exception {
		// 设置允许 iframe 引用
		http.headers().frameOptions().disable();
		http = http.cors().and().csrf().disable();
		http = http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and();

		http = http.exceptionHandling().authenticationEntryPoint(authenticationEntryPoint).and();
		// 所有请求都放行，目的是单 jar 部署，输入任意路由也能跳转到对应的页面，权限拦截通过注解配置

		http.authorizeRequests().antMatchers("/api/public/**").permitAll().antMatchers("/api/system").permitAll()
				.antMatchers("/captcha/get", "/captcha/check").permitAll().antMatchers(HttpMethod.GET, "/api/file/**")
				.permitAll().antMatchers("/api/**").authenticated().antMatchers("/").permitAll();
		http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
	}

	/**
	 * 全局 CORS 过滤器 Bean。
	 *
	 * <p>允许任意来源（Origin）、任意请求头、任意方法，并支持携带凭据（Cookie），
	 * 满足前后端分离部署与本地开发调试场景。</p>
	 *
	 * @return CorsFilter 实例
	 */
	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.addAllowedOriginPattern("*");
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}

	// Expose authentication manager bean
	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	// Remove the default ROLE_ prefix
	// @Bean
	// public GrantedAuthorityDefaults grantedAuthorityDefaults() {
	// return new GrantedAuthorityDefaults("");
	// }

	/**
	 * 获取 URL 参数令牌认证配置（供 JwtTokenFilter 读取开关状态）。
	 *
	 * @return 内嵌配置对象 UrlTokenAuthentication
	 */
	public UrlTokenAuthentication getUrlTokenAuthentication() {
		return urlTokenAuthentication;
	}

	/**
	 * URL 参数令牌认证的内嵌配置类。
	 *
	 * <p>属性由 Spring Boot 根据 {@code sk.security.url-token-authentication.*}
	 * 前缀绑定（@ConfigurationProperties 机制支持嵌套对象自动绑定）。</p>
	 */
	public static class UrlTokenAuthentication {

		/**
		 * 是否开启 token 认证
		 */
		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}
