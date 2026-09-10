package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学员端今日总览 + 积分获取引导视图（主页用）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentTodayView {

	/** 今日学习时长（分钟） */
	private long minutes;

	/** 今日完成知识点数 */
	private int kps;

	/** 今日获得学海积分 */
	private int points;

	/** 今日获得学习币 */
	private int coins;

	/** 积分获取引导项（当天可执行的主动学习行为） */
	private List<Guide> guides = new ArrayList<>();

	@Data
	public static class Guide {

		/** 行为类型 */
		private String actionType;

		/** 展示名称 */
		private String label;

		/** 可获得学海积分 */
		private int points;

		/** 可获得学习币 */
		private int coins;

		/** 是否已完成 */
		private boolean done;

		/** 进度描述，如 1/2 */
		private String progress;

		/** 跳转标识（前端据此路由，如 study/preview/trial/wrong/weak） */
		private String target;

		public Guide() {
		}

		public Guide(String actionType, String label, int points, int coins, boolean done, String progress,
				String target) {
			this.actionType = actionType;
			this.label = label;
			this.points = points;
			this.coins = coins;
			this.done = done;
			this.progress = progress;
			this.target = target;
		}
	}

}
