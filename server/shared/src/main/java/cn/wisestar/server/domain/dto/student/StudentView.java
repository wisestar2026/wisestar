package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.Date;

/**
 * 学员视图（学员管理模块，返回前端展示用）。
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Data
public class StudentView {

	private String id;

	/**
	 * 学号（8 位数字，系统自动生成，即登录账号）。
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
	 * 联系号码。
	 */
	private String phone;

	/**
	 * 学校。
	 */
	private String school;

	/**
	 * 校区（本迭代仅占位）。
	 */
	private String campus;

	/**
	 * 创建时间。
	 */
	private Date createAt;

}
