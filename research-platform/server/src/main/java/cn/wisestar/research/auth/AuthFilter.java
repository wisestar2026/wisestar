package cn.wisestar.research.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import cn.wisestar.research.common.R;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

	private static final Set<String> PUBLIC_PATHS = Set.of("/auth/login", "/health");

	private final JwtUtil jwtUtil;

	private final ObjectMapper objectMapper;

	@Value("${research.auth.cookie-name:research-token}")
	private String cookieName;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return PUBLIC_PATHS.contains(request.getServletPath()) || "OPTIONS".equalsIgnoreCase(request.getMethod());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String token = resolveToken(request);
		if (!StringUtils.hasText(token)) {
			reject(response, "未登录或登录已过期");
			return;
		}
		try {
			UserContext.set(jwtUtil.parse(token));
		}
		catch (JwtException | IllegalArgumentException e) {
			reject(response, "登录已过期，请重新登录");
			return;
		}
		try {
			chain.doFilter(request, response);
		}
		finally {
			UserContext.clear();
		}
	}

	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	private void reject(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), R.error(401, message));
	}

}
