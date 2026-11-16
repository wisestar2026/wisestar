package cn.wisestar.server.domain.dto.review;

import lombok.Data;

import java.util.Date;

/**
 * 复习队列条目。
 *
 * @author wisestar
 * @date 2026/11/16
 */
@Data
public class ReviewQueueItemView {

	/** 模块 */
	private String module;

	/** 学科ID */
	private String subjectId;

	/** 知识点ID */
	private String knowledgeId;

	/** 知识点名称 */
	private String name;

	/** 建议考察角度 */
	private String angle;

	/** 建议作答方式 */
	private String demand;

	/** 复习前可提取度 0~1 */
	private double retrievability;

	/** 严重度 0~3 */
	private double severity;

	/** 掌握度 0~100 */
	private int mastery;

	/** 到期时间 */
	private Date dueAt;

	/** 逾期天数 */
	private double overdueDays;

	/** 是否薄弱 */
	private boolean weak;

	/** 薄弱原因 */
	private String weakReason;

	/** 最近一次评分 */
	private Integer lastGrade;

	/** 稳定度（天） */
	private double stability;

	/** 调度状态 */
	private String state;

	/** 来源：due/weak/new */
	private String source;

}
