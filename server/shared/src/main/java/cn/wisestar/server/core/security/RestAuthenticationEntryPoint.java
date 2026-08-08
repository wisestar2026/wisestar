package cn.wisestar.server.core.security;

import cn.wisestar.server.core.common.ApiResponse;
import cn.wisestar.server.core.constant.ResponseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * REST 认证失败入口点（RestAuthenticationEntryPoint）。
 *
 * <p><b>所属模块</b>：shared 模块核心安全框架（cn.wisestar.server.core.security）。</p>
 * <p><b>类职责</b>：实现 Spring Security 的 {@link AuthenticationEntryPoint}，
 * 在"未认证/认证失败"的请求访问受保护资源时，向客户端输出
 * <em>application/json</em> 格式的统一错误响应，而不是 Spring Security
 * 默认的重定向到登录页或返回 HTML 错误页。</p>
 *
 * <p><b>触发场景</b>：</p>
 * <ul>
 *   <li>匿名用户访问受保护的 {@code /api/**} 接口（被 WebSecurityConfig 中
 *       {@code .antMatchers("/api/**").authenticated()} 拦截）；</li>
 *   <li>{@link JwtTokenFilter} 在过滤器链内加载用户失败抛出
 *       {@link AuthenticationException} 时被显式调用（见该过滤器 catch 分支）。</li>
 * </ul>
 *
 * <p><b>响应格式</b>：HTTP 状态码保持 200（业务约定），响应体为
 * {@link ApiResponse} 的 JSON 结构：{@code {code: ResponseCode.UNAUTHORIZED.code,
 * message: 异常信息}}。前端根据 code 判断未登录并跳转登录页。</p>
 *
 * <p><b>调用方</b>：Spring Security 异常处理机制自动调用（见
 * {@link WebSecurityConfig#configure(HttpSecurity)} 中
 * {@code http.exceptionHandling().authenticationEntryPoint(...)}）；以及
 * {@link JwtTokenFilter} 手动调用。Bean 名称 "restAuthenticationEntryPoint"
 * 供按名称注入使用。</p>
 *
 * @author javahuang
 * @date 2021/8/24
 */
@Component("restAuthenticationEntryPoint")
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	/**
	 * Jackson 序列化器：把 ApiResponse 对象序列化为 JSON 字符串写入响应流。
	 */
	private final ObjectMapper objectMapper;

	/**
	 * 输出未认证 JSON 响应（认证失败的统一出口）。
	 *
	 * @param request                HTTP 请求（未使用）
	 * @param response               HTTP 响应，本方法将其 Content-Type 设为 application/json
	 * @param authenticationException 认证失败异常（message 会被透传给前端）
	 * @throws IOException 响应流写出失败
	 */
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authenticationException) throws IOException, ServletException {

		response.setContentType("application/json");
		ApiResponse<String> apiResponse = new ApiResponse(ResponseCode.UNAUTHORIZED.code,
				authenticationException.getMessage());

		response.setStatus(HttpServletResponse.SC_OK);
		response.getOutputStream().println(objectMapper.writeValueAsString(apiResponse));
	}

}