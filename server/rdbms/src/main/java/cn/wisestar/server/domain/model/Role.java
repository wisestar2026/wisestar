package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author javahuang
 * @date 2021/8/24
 */
@Data
@TableName(value = "t_role", autoResultMap = true)
@EqualsAndHashCode(callSuper = false)
public class Role extends BaseModel {

	private String name;

	private String code;

	private String remark;

	/** 用户权限列表，以逗号分割 */
	private String authority;

	@TableField(fill = FieldFill.INSERT)
	private Date createAt;

	/**
	 * 0失活 1 激活
	 */
	private Integer status;

	/**
	 * 是否内置角色：1 内置（系统预置，不可删除、编码不可修改）；0 普通
	 */
	private Integer builtin;

	/**
	 * 数据范围：ALL 全校可见（不施加校区过滤）；CAMPUS 仅本人绑定校区可见。
	 * 见 {@link cn.wisestar.server.domain.dto.CampusScope#DATA_SCOPE_ALL} /
	 * {@link cn.wisestar.server.domain.dto.CampusScope#DATA_SCOPE_CAMPUS}。
	 * 为空按 ALL 处理（兼容历史数据）。
	 */
	private String dataScope;

}
