package cn.wisestar.server.domain.dto.review;

import lombok.Data;

import java.util.Date;

/**
 * 记忆调度作答结果。
 *
 * @author wisestar
 * @date 2026/11/16
 */
@Data
public class ReviewAnswerResult {

	/** 本次评分 1~4 */
	private int grade;

	/** 调度状态 */
	private String state;

	/** 复习前可提取度 0~1 */
	private double retrievabilityBefore;

	/** 复习后稳定度（天） */
	private double stability;

	/** 复习后难度 1~10 */
	private double difficulty;

	/** 本次安排间隔（天） */
	private double intervalDays;

	/** 下次到期时间 */
	private Date dueAt;

	/** 掌握度 0~100（由可提取度与历史正确率融合） */
	private int mastery;

	/** 是否在薄弱池 */
	private boolean weak;

	/** 薄弱原因 */
	private String weakReason;

	/** 本次是否达成攻克 */
	private boolean conquered;

	/** 累计复习次数 */
	private int reviews;

	/** 累计失误次数 */
	private int lapses;

	/** 是否未计入调度（重做/补答只作训练） */
	private boolean ignored;

}
