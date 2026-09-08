package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.PermissionConsts;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.CampusScope;
import cn.wisestar.server.domain.dto.UserInfo;
import cn.wisestar.server.domain.model.Campus;
import cn.wisestar.server.domain.model.UserCampus;
import cn.wisestar.server.mapper.CampusMapper;
import cn.wisestar.server.mapper.UserCampusMapper;
import cn.wisestar.server.service.CampusScopeService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 校区数据权限解析实现。
 *
 * <p>依据当前登录用户的角色（authorities 中的 {@code ROLE_<code>}）与
 * t_user_campus 绑定计算可见范围，供学员/订单/督学服务统一过滤。</p>
 *
 * @author wisestar
 * @date 2026/9/8
 */
@Service
@RequiredArgsConstructor
public class CampusScopeServiceImpl implements CampusScopeService {

	/** 参与校区数据隔离的内置角色编码 */
	private static final Set<String> SCOPED_ROLE_CODES = new LinkedHashSet<>(
			Arrays.asList(PermissionConsts.ROLE_PRINCIPAL, PermissionConsts.ROLE_CONSULTANT,
					PermissionConsts.ROLE_ACADEMIC));

	private final UserCampusMapper userCampusMapper;

	private final CampusMapper campusMapper;

	@Override
	public CampusScope resolveScope() {
		UserInfo user = SecurityContextUtils.getUser();
		Set<String> roleCodes = roleCodes(user);
		if (roleCodes.contains(PermissionConsts.ROLE_ADMIN) || roleCodes.stream().noneMatch(SCOPED_ROLE_CODES::contains)) {
			// 管理员 / 教师 / 自定义角色 / 学员与匿名：不施加校区过滤
			return CampusScope.all();
		}
		String userId = user.getUserId();
		if (!StringUtils.hasText(userId)) {
			return CampusScope.all();
		}
		List<UserCampus> bindings = userCampusMapper
				.selectList(Wrappers.<UserCampus>lambdaQuery().eq(UserCampus::getUserId, userId));
		if (CollectionUtils.isEmpty(bindings)) {
			// 三类角色未绑定任何校区：看不到任何校区数据
			return CampusScope.empty();
		}
		Set<String> campusIds = bindings.stream().map(UserCampus::getCampusId).collect(Collectors.toSet());
		Set<String> names = campusMapper.selectBatchIds(campusIds).stream().map(Campus::getName)
				.filter(StringUtils::hasText).collect(Collectors.toSet());
		return CampusScope.scoped(names);
	}

	/**
	 * 从认证信息中提取角色编码集合（{@code ROLE_xxx} 前缀剥离）。
	 */
	private Set<String> roleCodes(UserInfo user) {
		if (user == null || user.getAuthorities() == null) {
			return new LinkedHashSet<>();
		}
		Set<String> result = new LinkedHashSet<>();
		for (GrantedAuthority authority : user.getAuthorities()) {
			String value = authority.getAuthority();
			if (value != null && value.startsWith("ROLE_")) {
				result.add(value.substring("ROLE_".length()));
			}
		}
		return result;
	}

}
