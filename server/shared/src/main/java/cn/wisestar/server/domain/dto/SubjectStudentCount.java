package cn.wisestar.server.domain.dto;

import lombok.Data;

/**
 * 按学科统计的学员数（管理端仪表盘「科次」卡片数据项）。
 *
 * @author wisestar
 * @date 2026/9/26
 */
@Data
public class SubjectStudentCount {

	/**
	 * 学科ID（t_subject.id）。
	 */
	private String subjectId;

	/**
	 * 学科名称（如 语文/数学/英语）。
	 */
	private String subjectName;

	/**
	 * 该学科下有效权限的学员数（同一学员去重）。
	 */
	private Long studentCount;

}
