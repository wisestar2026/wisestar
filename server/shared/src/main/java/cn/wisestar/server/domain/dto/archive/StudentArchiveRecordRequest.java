package cn.wisestar.server.domain.dto.archive;

import lombok.Data;

/**
 * 学员上课记录保存请求。
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Data
public class StudentArchiveRecordRequest {

	/** 记录ID（为空表示新增） */
	private String id;

	/** 档案ID（可空，为空时按学员+学期自动定位/创建档案） */
	private String archiveId;

	/** 学员ID（必填） */
	private String studentId;

	/** 上课日期 yyyy-MM-dd（必填） */
	private String recordDate;

	/** 学科ID */
	private String subjectId;

	private String title;

	private String studySummary;

	private String solvedProblems;

	private String strengthenedKps;

	private String weaknesses;

	private String homework;

	private String teacherComment;

	private Integer durationMinutes;

	private Integer points;

	private Integer coins;

	/** draft/final */
	private String status;

}
