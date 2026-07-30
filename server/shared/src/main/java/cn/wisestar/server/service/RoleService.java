package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.RoleView;
import cn.wisestar.server.domain.dto.SelectRoleRequest;

import java.util.List;

/**
 * @author javahuang
 * @date 2022/2/3
 */
public interface RoleService {

	List<RoleView> selectRoles(SelectRoleRequest request);

}
