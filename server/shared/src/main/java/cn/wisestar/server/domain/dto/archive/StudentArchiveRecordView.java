package cn.wisestar.server.domain.dto.archive;

import lombok.Data;

/**
 * 学员上课记录视图。
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Data
public class StudentArchiveRecordView {

	private String id;

	private String archiveId;

	private String studentId;

	/** 上课日期 yyyy-MM-dd */
	private String recordDate;

	private String subjectId;

	private String title;

	/** 当日学习情况 */
	private String studySummary;

	/** 解决的问题 */
	private String solvedProblems;

	/** 强化的知识点 */
	private String strengthenedKps;

	/** 暴露的弱点 */
	private String weaknesses;

	/** 课后作业/任务 */
	private String homework;

	/** 教师寄语 */
	private String teacherComment;

	private Integer durationMinutes;

	private Integer points;

	private Integer coins;

	private String source;

	private String status;

	private Integer sort;

}
