package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * @author javahuang
 * @date 2022/4/24
 */
@Data
public class UserOverview {

	/**
	 * 考试数量
	 */
	private Long examCount;

	/**
	 * 问卷数量
	 */
	private Long surveyCount;

	/**
	 * 学员总数（t_student 状态正常且未删除）。
	 */
	private Long studentCount;

	/**
	 * 科次总数（按学科统计的学员数之和）。
	 */
	private Long courseCount;

	/**
	 * 按学科统计的学员数（含学员数为 0 的学科，按学科排序）。
	 */
	private List<SubjectStudentCount> subjectStudentCounts;

}
