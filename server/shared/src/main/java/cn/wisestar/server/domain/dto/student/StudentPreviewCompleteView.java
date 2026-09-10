package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学员预习完成视图（奖励结算结果）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentPreviewCompleteView {

	/** 是否成功 */
	private boolean ok;

	/** 本次是否首次完成并结算奖励（false=此前已完成，未重复发放） */
	private boolean firstTime;

	/** 本次发放学习币 */
	private int coins;

	/** 本次发放学海积分 */
	private int points;

}
