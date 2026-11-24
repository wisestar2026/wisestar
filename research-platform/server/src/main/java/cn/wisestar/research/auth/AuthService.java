package cn.wisestar.research.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import cn.wisestar.research.auth.dto.LoginRequest;
import cn.wisestar.research.auth.dto.LoginResponse;
import cn.wisestar.research.common.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final LocalAdminMapper localAdminMapper;

	private final RemoteAuthClient remoteAuthClient;

	private final JwtUtil jwtUtil;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public LoginResponse login(LoginRequest request) {
		String username = request.username().trim();
		LocalAdmin admin = localAdminMapper
				.selectOne(Wrappers.<LocalAdmin>lambdaQuery().eq(LocalAdmin::getUsername, username));
		if (admin != null) {
			if (!Integer.valueOf(1).equals(admin.getStatus()) || !passwordEncoder.matches(request.password(),
					admin.getPassword())) {
				throw new BizException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
			}
			return build(new CurrentUser(admin.getId(), admin.getUsername(), admin.getName(),
					AuthRole.valueOf(admin.getRole()), "LOCAL"));
		}

		RemoteAuthClient.RemoteUser remoteUser = remoteAuthClient.verify(username, request.password());
		AuthRole role = resolveRole(remoteUser.roleCodes());
		return build(new CurrentUser(remoteUser.userId(), remoteUser.username(), remoteUser.name(), role, "REMOTE"));
	}

	private AuthRole resolveRole(List<String> roleCodes) {
		if (roleCodes == null || roleCodes.isEmpty()) {
			throw new BizException(HttpStatus.FORBIDDEN, "该账号未开通教研平台权限，请联系管理员");
		}
		if (roleCodes.contains("admin")) {
			return AuthRole.ADMIN;
		}
		if (roleCodes.contains("principal")) {
			return AuthRole.SUPERVISOR;
		}
		if (roleCodes.contains("teacher") || roleCodes.contains("consultant")) {
			return AuthRole.TEACHER;
		}
		throw new BizException(HttpStatus.FORBIDDEN, "该账号未开通教研平台权限，请联系管理员");
	}

	public LoginResponse build(CurrentUser user) {
		String token = jwtUtil.issue(user);
		return new LoginResponse(token, new LoginResponse.UserView(user.id(), user.username(), user.name(),
				user.role().name(), roleName(user.role()), user.source()));
	}

	public LoginResponse.UserView view(CurrentUser user) {
		return new LoginResponse.UserView(user.id(), user.username(), user.name(), user.role().name(),
				roleName(user.role()), user.source());
	}

	private String roleName(AuthRole role) {
		return switch (role) {
			case ADMIN -> "系统管理员";
			case SUPERVISOR -> "教学主管";
			case TEACHER -> "任课老师";
		};
	}

	public long cookieMaxAgeSeconds() {
		return jwtUtil.getTtlMillis() / 1000;
	}

	public void logLogin(CurrentUser user) {
		log.info("教研平台登录成功: {} ({}, {})", user.username(), user.role(), user.source());
		if (!StringUtils.hasText(user.id())) {
			throw new BizException(HttpStatus.UNAUTHORIZED, "账号信息异常，请联系管理员");
		}
	}

}
