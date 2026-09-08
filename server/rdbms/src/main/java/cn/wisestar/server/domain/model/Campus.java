package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 校区档案（对应表 t_campus，行政管理-校区管理）。
 *
 * <p>校区以唯一名称标识；学员主数据 t_student.campus 存校区名称文本，
 * 员工账号通过 t_user_campus 绑定多校区（构成校长/教务/学管师的数据权限范围）。</p>
 *
 * @author wisestar
 * @date 2026/9/8
 */
@Data
@TableName("t_campus")
@EqualsAndHashCode(callSuper = false)
public class Campus extends BaseModel {

	/**
	 * 校区名称（全局唯一）。
	 */
	private String name;

	/**
	 * 状态（1 启用 0 停用）。
	 */
	private Integer status;

	/**
	 * 备注。
	 */
	private String remark;

}
