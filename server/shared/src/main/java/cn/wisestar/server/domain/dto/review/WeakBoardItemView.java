package cn.wisestar.server.domain.dto.review;

import lombok.Data;

import java.util.Date;

/**
 * 薄弱看板条目。
 *
 * @author wisestar
 * @date 2026/11/16
 */
@Data
public class WeakBoardItemView {

	/** 模块 */
	private String module;

	/** 学科ID */
	private String subjectId;

	/** 知识点ID */
	private String knowledgeId;

	/** 知识点名称 */
	private String name;

	/** 严重度 0~3 */
	private double severity;

	/** 可提取度 0~1 */
	private double retrievability;

	/** 掌握度 0~100 */
	private int mastery;

	/** 预计遗忘日期（可提取度降至 0.6 的日期） */
	private Date forgettingAt;

	/** 薄弱原因 */
	private String weakReason;

	/** 最近一次评分 */
	private Integer lastGrade;

	/** 累计失误 */
	private int lapses;

	/** 到期时间 */
	private Date dueAt;

	/** 建议考察角度 */
	private String angle;

	/** 建议作答方式 */
	private String demand;

}
