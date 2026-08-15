package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学员主数据实体（对应数据库表 t_student，学员管理模块）。
 *
 * <p><b>账号关联</b>：新增学员时服务端在同一事务内创建 t_student 与
 * t_account（user_type=Student、auth_account=学号），学号即学员登录账号。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Data
@TableName("t_student")
@EqualsAndHashCode(callSuper = false)
public class Student extends BaseModel {

	/**
	 * 学号（8 位数字，系统自动生成，全局唯一，登录账号）。
	 */
	private String studentNo;

	/**
	 * 姓名。
	 */
	private String name;

	/**
	 * 年龄。
	 */
	private Integer age;

	/**
	 * 联系号码（与姓名组合查重，防重复录入）。
	 */
	private String phone;

	/**
	 * 学校。
	 */
	private String school;

	/**
	 * 校区（本迭代仅占位，业务逻辑后续迭代）。
	 */
	private String campus;

	/**
	 * 扩展预留字段（JSON，前端不展示，供后续数据分析）。
	 */
	private String extra;

	/**
	 * 状态（1 正常 0 停用）。
	 */
	private Integer status;

}
