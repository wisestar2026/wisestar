package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.List;

/**
 * 订单请求（学员管理模块）。
 *
 * <p>create 用：studentId 必传；subjectIds 多选学科ID、grades 多选年级，
 * 服务端按学科×年级笛卡尔积展开写入权限表。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Data
public class OrderRequest {

	/**
	 * 订单ID（取消/删除用；创建时为空）。
	 */
	private String id;

	/**
	 * 学员ID（t_student.id，必传）。
	 */
	private String studentId;

	/**
	 * 学科ID多选（t_subject.id，必传）。
	 */
	private List<String> subjectIds;

	/**
	 * 年级多选（必传）。
	 */
	private List<String> grades;

	/**
	 * 教材版本（如 人教版/苏教版/北师大版/外研版）。
	 */
	private String version;

	/**
	 * 账号时长数值（必传，大于 0）。
	 */
	private Integer duration;

	/**
	 * 时长单位（DAY/MONTH/YEAR，必传）。
	 */
	private String durationUnit;

}
