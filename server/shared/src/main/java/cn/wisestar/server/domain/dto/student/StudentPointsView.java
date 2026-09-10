package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 个人中心-积分板块视图（简单明了呈现积分评价）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentPointsView {

	/** 当前学海积分 */
	private int points;

	/** 当前头衔等级 */
	private int titleLevel = 1;

	/** 当前头衔名称 */
	private String titleName = "初探者";

	/** 下一头衔名称（已是最高头衔为 null） */
	private String nextTitleName;

	/** 下一头衔所需积分（已是最高头衔为 -1） */
	private int nextTitlePoints = -1;

	/** 距离下一头衔还差多少积分 */
	private int pointsToNextTitle;

	/** 积分获取规则 */
	private List<RuleItem> rules = new ArrayList<>();

	/** 最近积分明细（倒序，最多 20 条） */
	private List<RecordItem> recent = new ArrayList<>();

	@Data
	public static class RuleItem {

		/** 行为类型 */
		private String actionType;

		/** 行为名称 */
		private String label;

		/** 可获得学海积分 */
		private int points;

		/** 可获得学习币 */
		private int coins;

		public RuleItem() {
		}

		public RuleItem(String actionType, String label, int points, int coins) {
			this.actionType = actionType;
			this.label = label;
			this.points = points;
			this.coins = coins;
		}
	}

	@Data
	public static class RecordItem {

		/** 行为名称 */
		private String label;

		/** 本次学海积分 */
		private int points;

		/** 本次学习币 */
		private int coins;

		/** 发生时间 */
		private java.util.Date at;

		public RecordItem() {
		}

		public RecordItem(String label, int points, int coins, java.util.Date at) {
			this.label = label;
			this.points = points;
			this.coins = coins;
			this.at = at;
		}
	}

}
