package cn.wisestar.research.auth;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	private final Key key;

	private final long ttlMillis;

	public JwtUtil(@Value("${research.auth.jwt-secret}") String secret,
			@Value("${research.auth.token-ttl-hours:168}") long ttlHours) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.ttlMillis = ttlHours * 3600_000L;
	}

	public String issue(CurrentUser user) {
		Date now = new Date();
		return Jwts.builder().setSubject(user.id()).claim("uname", user.username()).claim("name", user.name())
				.claim("role", user.role().name()).claim("src", user.source()).setIssuedAt(now)
				.setExpiration(new Date(now.getTime() + ttlMillis)).signWith(key, SignatureAlgorithm.HS256).compact();
	}

	public CurrentUser parse(String token) {
		Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
		return new CurrentUser(claims.getSubject(), claims.get("uname", String.class),
				claims.get("name", String.class), AuthRole.valueOf(claims.get("role", String.class)),
				claims.get("src", String.class));
	}

	public long getTtlMillis() {
		return ttlMillis;
	}

}
