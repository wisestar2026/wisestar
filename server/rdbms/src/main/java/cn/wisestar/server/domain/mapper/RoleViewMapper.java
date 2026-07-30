package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.RoleRequest;
import cn.wisestar.server.domain.dto.RoleView;
import cn.wisestar.server.domain.model.Role;
import org.mapstruct.*;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;

/**
 * @author javahuang
 * @date 2021/10/12
 */
@Mapper
public interface RoleViewMapper extends BaseModelMapper<RoleRequest, RoleView, Role> {

	@AfterMapping
	default void afterMapping(Role role, @MappingTarget RoleView target) {
		if(role.getAuthority() != null) {
			target.setAuthorities(Arrays.asList(role.getAuthority().split(",")));
		}

	}

	@AfterMapping
	default void afterMappingRole(RoleRequest request, @MappingTarget Role target) {
		if (CollectionUtils.isEmpty(request.getAuthorities())) {
			return;
		}
		target.setAuthority(String.join(",", request.getAuthorities()));
	}

}
