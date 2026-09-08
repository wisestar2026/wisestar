package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.CampusScope;

/**
 * 校区数据权限解析服务。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包。实现类位于 rdbms 模块（CampusScopeServiceImpl）。</p>
 * <p><b>类职责</b>：解析当前登录用户可见校区范围（ALL / EMPTY / SCOPED），
 * 供学员管理、订单、督学等服务在列表查询与单对象访问处统一施加过滤，
 * 使校长/教务/学管师只能看到自己绑定校区的学员业务数据。</p>
 *
 * @author wisestar
 * @date 2026/9/8
 */
public interface CampusScopeService {

	/**
	 * 解析当前登录用户的校区数据权限范围。
	 *
	 * <p>规则：匿名/学员/教师及自定义普通角色 → ALL（不施加校区过滤）；
	 * 角色含 admin → ALL；角色含校长/教务/学管师且绑定校区 → SCOPED；
	 * 上述三类角色但未绑定任何校区 → EMPTY。</p>
	 */
	CampusScope resolveScope();

}
