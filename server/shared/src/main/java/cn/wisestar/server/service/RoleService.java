package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.RoleView;
import cn.wisestar.server.domain.dto.SelectRoleRequest;

import java.util.List;

/**
 * 角色服务接口（RoleService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：角色（Role）相关业务。当前提供"角色选择器"数据源：
 * 供新建/编辑用户、分配角色等场景下拉选择使用。
 * 完整角色 CRUD 在 {@link SystemService}（getRoles/createRole/updateRole/deleteRole）中。</p>
 *
 * <p><b>实现类</b>：rdbms 模块 RoleServiceImpl；<b>调用方</b>：api 模块用户/角色相关接口。</p>
 *
 * @author javahuang
 * @date 2022/2/3
 */
public interface RoleService {

	/**
	 * 查询角色列表（角色选择器数据源）。
	 *
	 * @param request 查询条件（如角色名称过滤、排除超级管理员等）
	 * @return 角色视图列表
	 */
	List<RoleView> selectRoles(SelectRoleRequest request);

}
