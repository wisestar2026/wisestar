package cn.wisestar.server.core.security;

import cn.wisestar.server.domain.dto.UserTokenView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.lang.Maps;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JWT 令牌工具类（JwtTokenUtil）。
 *
 * <p><b>所属模块</b>：shared 模块核心安全框架（cn.wisestar.server.core.security）。</p>
 * <p><b>类职责</b>：封装基于 jjwt 库的 JWT（JSON Web Token）生成、校验与解析逻辑，
 * 为无状态（STATELESS）的登录认证提供支撑：</p>
 * <ul>
 *   <li><b>生成令牌</b>：{@link #generateAccessToken} —— 登录成功后由
 *       {@link UserService}（rdbms 实现）调用，将当前用户信息
 *       （{@link UserTokenView}）作为自定义 claim 写入令牌；</li>
 *   <li><b>校验令牌</b>：{@link #validate} —— 由 {@link JwtTokenFilter} 在每次请求时调用，
 *       校验签名与有效期，捕获并记录各类非法令牌日志；</li>
 *   <li><b>解析令牌</b>：{@link #getUser} —— 从合法令牌中反序列化出用户信息
 *       {@link UserTokenView}，供过滤器定位 userId。</li>
 * </ul>
 *
 * <p><b>签名机制</b>：使用 HS512（HMAC-SHA512）算法对称签名，密钥由
 * 旧版每次启动随机生成密钥导致重启后令牌全部失效；现改为固定配置（wisestar.jwt.secret），应用每次重启后所有
 * 已签发的令牌都会失效</em>（用户需要重新登录）。</p>
 * <p><b>数据流</b>：登录成功 → 组装 UserTokenView → generateAccessToken 生成令牌返回前端
 * → 前端存入 Cookie/请求头 → 后续请求 → JwtTokenFilter → validate 校验 → getUser 解析用户。</p>
 *
 * @author javahuang
 * @date 2021/8/23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

	/**
	 * Jackson 序列化器：用于把 UserTokenView 自定义 claim 以 JSON 形式
	 * 写入/读出令牌的 payload（保证 userId 等字段正确序列化）。
	 */
	private final ObjectMapper objectMapper;

	/**
	 * JWT 签名密钥：固定配置（wisestar.jwt.secret，可经 application.yml 覆盖），
	 * 保证后端重启后已签发令牌仍有效（刷新页面/重启服务不登出）。
	 */
	@Value("${wisestar.jwt.secret:wisestar-jwt-secret-2026-fixed-key-for-preview-env-0123456789abcdef}")
	private String jwtSecret;

	/**
	 * 生成访问令牌（Access Token）。
	 *
	 * @param user 登录用户信息（含 userId、用户名、角色等），作为自定义 claim "user" 写入
	 * @return 签名后的 JWT 字符串（三部分：header.payload.signature）
	 */
	public String generateAccessToken(UserTokenView user) {
		return Jwts.builder().serializeToJsonWith(new JacksonSerializer(objectMapper)).claim("user", user)
				.setIssuedAt(new Date()).signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes())).compact();
	}

	/**
	 * 校验令牌的签名与结构是否合法（不含业务校验）。
	 *
	 * <p>捕获 jjwt 抛出的全部令牌异常类型并记录 error 日志，任意一种异常都会返回 false：</p>
	 * <ul>
	 *   <li>{@link SignatureException}：签名不匹配（令牌被篡改或使用错误密钥签发）；</li>
	 *   <li>{@link MalformedJwtException}：令牌结构畸形（非三段式、非法 Base64 等）；</li>
	 *   <li>{@link ExpiredJwtException}：令牌已过期；</li>
	 *   <li>{@link UnsupportedJwtException}：不支持的令牌类型；</li>
	 *   <li>{@link IllegalArgumentException}：令牌为空字符串。</li>
	 * </ul>
	 *
	 * @param token 待校验的 JWT 字符串
	 * @return true 表示令牌合法；false 表示非法（调用方直接放行，由方法级安全拦截）
	 */
	public boolean validate(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes())).build().parseClaimsJws(token);
			return true;
		}
		catch (SignatureException ex) {
			log.error("Invalid JWT signature - {}", ex.getMessage());
		}
		catch (MalformedJwtException ex) {
			log.error("Invalid JWT token - {}", ex.getMessage());
		}
		catch (ExpiredJwtException ex) {
			log.error("Expired JWT token - {}", ex.getMessage());
		}
		catch (UnsupportedJwtException ex) {
			log.error("Unsupported JWT token - {}", ex.getMessage());
		}
		catch (IllegalArgumentException ex) {
			log.error("JWT claims string is empty - {}", ex.getMessage());
		}
		return false;
	}

	/**
	 * 从合法令牌中解析出登录用户信息。
	 *
	 * <p>使用 Jackson 反序列化器把 payload 中的 "user" claim 映射回
	 * {@link UserTokenView} 对象。注意：本方法不校验签名，调用方应保证
	 * 传入的令牌已通过 {@link #validate} 校验（{@link JwtTokenFilter} 中已保证）。</p>
	 *
	 * @param token 已通过校验的 JWT 字符串
	 * @return 令牌中携带的用户信息（至少包含 userId，供 loadUserById 使用）
	 */
	public UserTokenView getUser(String token) {
		return Jwts.parserBuilder()
				.deserializeJsonWith(new JacksonDeserializer(Maps.of("user", UserTokenView.class).build()))
				.setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes())).build().parseClaimsJws(token).getBody()
				.get("user", UserTokenView.class);
	}


}
