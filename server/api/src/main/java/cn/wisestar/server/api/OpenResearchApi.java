package cn.wisestar.server.api;

import cn.wisestar.server.core.constant.AppConsts;
import cn.wisestar.server.core.constant.ErrorCode;
import cn.wisestar.server.core.exception.ErrorCodeException;
import cn.wisestar.server.core.security.PasswordEncoder;
import cn.wisestar.server.domain.dto.UserInfo;
import cn.wisestar.server.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 教研平台只读开放接口（OpenResearchApi）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层）。</p>
 * <p><b>类职责</b>：向独立部署的教研平台提供只读能力，全部接口挂在
 * {@code /open/research/v1/**} 下，不经过 JWT，改用服务令牌 {@code X-Research-Token}
 * 校验。只读接口不新增表、不写业务数据。</p>
 * <p><b>部署约束</b>：本前缀只在服务器回环/私网内暴露，nginx 不得把
 * {@code /open/} 转发到公网；令牌通过环境变量 {@code RESEARCH_OPEN_TOKEN} 配置。</p>
 *
 * @author wisestar
 */
@Slf4j
@RestController
@RequestMapping("/open/research/v1")
@RequiredArgsConstructor
public class OpenResearchApi {

	private final UserService userService;

	private final PasswordEncoder passwordEncoder;

	@Value("${research.open.token:${RESEARCH_OPEN_TOKEN:dev-research-token}}")
	private String openToken;

	/**
	 * 账号校验：供教研平台登录使用，返回角色编码列表供其映射为教研角色。
	 */
	@PostMapping("/auth/verify")
	public VerifyResult verify(@RequestHeader(value = "X-Research-Token", required = false) String token,
			@RequestBody VerifyRequest request) {
		assertToken(token);
		if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
			throw new ErrorCodeException(ErrorCode.UsernameOrPasswordError);
		}
		UserInfo user;
		try {
			user = (UserInfo) userService.loadUserByUsername(request.getUsername().trim());
		}
		catch (Exception e) {
			throw new ErrorCodeException(ErrorCode.UsernameOrPasswordError);
		}
		if (user == null || user.getPassword() == null
				|| !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new ErrorCodeException(ErrorCode.UsernameOrPasswordError);
		}
		if (AppConsts.USER_TYPE.Student.toString().equals(user.getUserType())) {
			throw new ErrorCodeException(ErrorCode.PermVerifyFailed);
		}
		if (!Integer.valueOf(1).equals(user.getStatus())) {
			throw new ErrorCodeException(ErrorCode.UsernameOrPasswordError);
		}
		if (user.getAuthorities() == null || user.getAuthorities().isEmpty()) {
			UserInfo withRoles = userService.loadUserById(user.getUserId());
			if (withRoles != null && withRoles.getAuthorities() != null) {
				user.setAuthorities(new java.util.HashSet<>(withRoles.getAuthorities()));
				if (!StringUtils.hasText(user.getName())) {
					user.setName(withRoles.getName());
				}
			}
		}
		List<String> roleCodes = roleCodes(user);
		if (roleCodes.isEmpty()) {
			throw new ErrorCodeException(ErrorCode.PermVerifyFailed);
		}
		log.info("教研平台账号校验通过: {} roles={}", user.getUserId(), roleCodes);
		return new VerifyResult(user.getUserId(), user.getUsername(), user.getName(), user.getUserType(), roleCodes);
	}

	/**
	 * 健康探针，供教研平台启动自检远端连通性。
	 */
	@GetMapping("/ping")
	public Map<String, Object> ping(@RequestHeader(value = "X-Research-Token", required = false) String token) {
		assertToken(token);
		return Map.of("ok", true, "version", "v1");
	}

	private void assertToken(String token) {
		if (token == null || !token.equals(openToken)) {
			throw new ErrorCodeException(ErrorCode.PermVerifyFailed);
		}
	}

	private List<String> roleCodes(UserInfo user) {
		Set<String> codes = new LinkedHashSet<>();
		if (user.getAuthorities() != null) {
			user.getAuthorities().forEach(authority -> {
				String value = authority.getAuthority();
				if (value != null && value.startsWith("ROLE_")) {
					codes.add(value.substring(5));
				}
			});
		}
		return List.copyOf(codes);
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class VerifyRequest {

		private String username;

		private String password;

	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class VerifyResult {

		private String userId;

		private String username;

		private String name;

		private String userType;

		private List<String> roleCodes;

	}

}
