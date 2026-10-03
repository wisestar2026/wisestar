package cn.wisestar.server.domain.dto.archive;

import lombok.Data;

/**
 * 学员档案保存请求（管理端填写目标规划表/承诺书/学期报告）。
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Data
public class StudentArchiveSaveRequest {

	/** 档案ID（为空表示新建） */
	private String id;

	/** 学员ID（必填） */
	private String studentId;

	/** 学年，如 2026-2027（为空按当前日期推导） */
	private String schoolYear;

	/** 学期键，如 2026-1（为空按当前日期推导） */
	private String semester;

	/** 学期名称，如 第一学期（为空按当前日期推导） */
	private String termLabel;

	/** 学科ID（可空表示全科） */
	private String subjectId;

	/** 负责老师姓名 */
	private String teacherName;

	/** 状态 draft/active/closed */
	private String status;

	/** 本学期目标规划表正文 */
	private String goalPlan;

	/** 学员承诺书正文 */
	private String promise;

	/** 学期报告正文 */
	private String reportContent;

	/** 学期报告状态 none/draft/final */
	private String reportStatus;

	/** 备注 */
	private String remark;

}
