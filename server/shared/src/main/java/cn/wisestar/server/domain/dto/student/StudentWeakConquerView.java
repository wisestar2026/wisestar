package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 薄弱点攻克结果视图。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentWeakConquerView {

	private boolean ok;

	/** 是否消除薄弱标记 */
	private boolean weakCleared;

	/** 本次发放学习币 */
	private int coins;

	/** 本次发放学海积分 */
	private int points;

}
