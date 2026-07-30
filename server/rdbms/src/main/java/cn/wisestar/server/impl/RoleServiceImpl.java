package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.RoleView;
import cn.wisestar.server.domain.dto.SelectRoleRequest;
import cn.wisestar.server.domain.mapper.RoleViewMapper;
import cn.wisestar.server.domain.model.Role;
import cn.wisestar.server.mapper.RoleMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author javahuang
 * @date 2021/10/12
 */
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class RoleServiceImpl extends BaseService<RoleMapper, Role> implements RoleService {

	private final RoleViewMapper roleViewMapper;

	@Override
	public List<RoleView> selectRoles(SelectRoleRequest request) {
		return list().stream().map(role -> roleViewMapper.toView(role)).collect(Collectors.toList());
	}

}
