package cn.wisestar.server.domain.dto;

import lombok.Data;

/**
 * 校区视图（行政管理-校区管理列表/下拉）。
 *
 * <p>列表场景附带统计：引用学员数 studentCount 与绑定
 * 校长/教务/学管师三类角色员工数（roleCount，按 t_user_campus 关联角色统计）；
 * 用户管理绑定场景仅使用 id/name/status 字段。</p>
 *
 * @author wisestar
 * @date 2026/9/8
 */
@Data
public class CampusView {

	private String id;

	/**
	 * 校区名称（唯一，学员主数据 t_student.campus 存该名称）。
	 */
	private String name;

	/**
	 * 状态：1 启用 0 停用。
	 */
	private Integer status;

	/**
	 * 备注。
	 */
	private String remark;

	/**
	 * 引用学员数（t_student.campus = 本校区名称）。
	 */
	private Long studentCount;

	/**
	 * 绑定校长/教务/学管师角色员工数（多角色去重统计）。
	 */
	private Long roleCount;

}
