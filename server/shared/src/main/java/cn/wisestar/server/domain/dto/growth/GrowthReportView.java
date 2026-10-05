package cn.wisestar.server.domain.dto.growth;

import lombok.Data;

import java.util.Date;

/**
 * 成长报告视图（正文 + 状态 + 打印头信息）。
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Data
public class GrowthReportView {

	/** 学员ID */
	private String studentId;

	/** 学号快照 */
	private String studentNo;

	/** 姓名快照 */
	private String studentName;

	/** 学科ID */
	private String subjectId;

	/** 学科名称 */
	private String subjectName;

	/** 学期键 */
	private String semester;

	/** 学期名称，如 第一学期 */
	private String termLabel;

	/** 学年，如 2026-2027 */
	private String schoolYear;

	/** 报告正文 */
	private String content;

	/** 报告状态 none/draft/final */
	private String status;

	/** 生成模型（ai 模型名或 rule） */
	private String model;

	/** 生成时间 */
	private Date generatedAt;

}
