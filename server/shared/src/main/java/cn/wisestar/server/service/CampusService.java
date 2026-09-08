package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.CampusRequest;
import cn.wisestar.server.domain.dto.CampusView;

import java.util.List;

/**
 * 校区服务接口（行政管理-校区管理）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包。实现类位于 rdbms 模块（CampusServiceImpl）。</p>
 * <p><b>类职责</b>：校区档案 CRUD 与统计；校区列表/下拉均遵守当前账号的
 * 校区数据权限范围（校长/教务/学管师仅见自己绑定校区）。</p>
 *
 * @author wisestar
 * @date 2026/9/8
 */
public interface CampusService {

	/**
	 * 校区管理列表（当前数据范围内；行内含引用学员数 studentCount 与绑定员工数 roleCount）。
	 */
	List<CampusView> listCampuses();

	/**
	 * 校区下拉数据源（当前数据范围，不带统计列）。
	 *
	 * @param includeDisabled 是否包含停用校区；ALL 模式下默认仅返回启用校区，
	 *                        三角色（SCOPED）始终返回自己绑定校区（含停用，便于历史展示）。
	 */
	List<CampusView> campusOptions(boolean includeDisabled);

	/**
	 * 新增校区（名称必填且全局唯一，默认启用）。
	 */
	void createCampus(CampusRequest request);

	/**
	 * 更新校区（名称唯一校验；改名时同事务同步 t_student.campus 引用学员）。
	 */
	void updateCampus(CampusRequest request);

	/**
	 * 删除校区（仅允许零引用：无引用学员且无绑定员工；否则抛业务异常提示先停用）。
	 */
	void deleteCampus(String id);

	/**
	 * 校验学员可被分配该校区：新赋值校区必须存在且启用。
	 *
	 * @param name        待校验校区名称（空/空白直接放行，表示清空校区）
	 * @param currentName 该学员当前校区名称；当两者相同（历史停用校区回填）时放行
	 */
	void checkAssignable(String name, String currentName);

}
