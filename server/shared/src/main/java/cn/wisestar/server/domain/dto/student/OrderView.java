package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 订单视图（学员管理模块，返回前端展示用）。
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Data
public class OrderView {

	private String id;

	/**
	 * 学员ID。
	 */
	private String studentId;

	/**
	 * 学员学号。
	 */
	private String studentNo;

	/**
	 * 学员姓名。
	 */
	private String studentName;

	/**
	 * 学科（多选，含 id 与名称）。
	 */
	private List<SubjectItem> subjects;

	/**
	 * 年级（多选）。
	 */
	private List<String> grades;

	/**
	 * 教材版本。
	 */
	private String version;

	/**
	 * 账号时长数值。
	 */
	private Integer duration;

	/**
	 * 时长单位（DAY/MONTH/YEAR）。
	 */
	private String durationUnit;

	/**
	 * 有效期至。
	 */
	private Date expireAt;

	/**
	 * 状态（1 生效 0 作废）。
	 */
	private Integer status;

	/**
	 * 创建时间。
	 */
	private Date createAt;

	/**
	 * 订单内单个学科（id + 名称）。
	 */
	@Data
	public static class SubjectItem {

		private String id;

		private String name;

	}

}
