package cn.wisestar.research.auth;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import cn.wisestar.research.common.BizException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RemoteAuthClient {

	private final RestTemplate restTemplate;

	private final String baseUrl;

	private final String token;

	public RemoteAuthClient(RestTemplateBuilder builder, @Value("${research.remote.base-url}") String baseUrl,
			@Value("${research.remote.token}") String token,
			@Value("${research.remote.connect-timeout-ms:1000}") long connectTimeoutMs,
			@Value("${research.remote.read-timeout-ms:3000}") long readTimeoutMs) {
		this.restTemplate = builder.setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
				.setReadTimeout(Duration.ofMillis(readTimeoutMs)).build();
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		this.token = token;
	}

	public RemoteUser verify(String username, String password) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Research-Token", token);
		Map<String, String> body = Map.of("username", username, "password", password);
		try {
			ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/open/research/v1/auth/verify",
					new HttpEntity<>(body, headers), Map.class);
			return parse(response.getBody());
		}
		catch (HttpStatusCodeException e) {
			if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				throw new BizException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
			}
			log.warn("远端账号校验失败: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new BizException(HttpStatus.BAD_GATEWAY, "远端账号服务返回异常，请稍后重试");
		}
		catch (ResourceAccessException e) {
			log.warn("远端账号服务不可达: {}", e.getMessage());
			throw new BizException(HttpStatus.SERVICE_UNAVAILABLE, "远端账号服务不可用，请使用管理员账号登录");
		}
	}

	@SuppressWarnings("unchecked")
	private RemoteUser parse(Map body) {
		if (body == null) {
			throw new BizException(HttpStatus.BAD_GATEWAY, "远端账号服务返回为空");
		}
		Object code = body.get("code");
		if (code instanceof Number && ((Number) code).intValue() != 200) {
			String message = String.valueOf(body.getOrDefault("message", "远端账号校验失败"));
			throw new BizException(HttpStatus.UNAUTHORIZED, message);
		}
		Object dataObject = body.get("data");
		if (!(dataObject instanceof Map)) {
			throw new BizException(HttpStatus.BAD_GATEWAY, "远端账号服务返回格式异常");
		}
		Map<String, Object> data = (Map<String, Object>) dataObject;
		List<String> roleCodes = data.get("roleCodes") instanceof List
				? ((List<Object>) data.get("roleCodes")).stream().map(String::valueOf).toList() : List.of();
		return new RemoteUser(String.valueOf(data.get("userId")), String.valueOf(data.get("username")),
				String.valueOf(data.getOrDefault("name", data.get("username"))),
				String.valueOf(data.getOrDefault("userType", "")), roleCodes);
	}

	public record RemoteUser(String userId, String username, String name, String userType, List<String> roleCodes) {
	}

}
