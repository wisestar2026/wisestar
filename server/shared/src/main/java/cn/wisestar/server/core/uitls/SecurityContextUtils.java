package cn.wisestar.server.core.uitls;

import cn.wisestar.server.core.constant.AppConsts;
import cn.wisestar.server.domain.dto.UserInfo;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author javahuang
 * @date 2021/8/31
 */
public class SecurityContextUtils {

	/**
	 * 获取当前登录用户信息，未认证或主体非用户信息时返回匿名用户。
	 *
	 * @return 当前用户信息，未登录时返回匿名用户
	 */
	public static UserInfo getUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return new UserInfo(AppConsts.ANONYMOUS_USER_ID);
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof UserInfo) {
			return (UserInfo) principal;
		}
		return new UserInfo(AppConsts.ANONYMOUS_USER_ID);
	}

	/**
	 * 获取当前登录用户的用户名。
	 *
	 * @return 用户名
	 */
	public static String getUsername() {
		return getUser().getUsername();
	}

	/**
	 * 获取当前登录用户的用户 ID。
	 *
	 * @return 用户 ID
	 */
	public static String getUserId() {
		return getUser().getUserId();
	}

	/**
	 * 用户是否已登录
	 * @return
	 */
	public static boolean isAuthenticated() {
		return getUserId() != null;
	}

	/**
	 * 当前是否是匿名访问
	 * @return
	 */
	public static boolean isAnonymous() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication instanceof AnonymousAuthenticationToken;
	}

	/**
	 * 判断当前登录用户是否具有管理员权限。
	 *
	 * @return 具有管理员角色返回 true，否则返回 false
	 */
	public static boolean isAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication.getAuthorities().stream().filter(s -> AppConsts.ROLE_ADMIN.equals(s.getAuthority()))
				.count() > 0;
	}

}
