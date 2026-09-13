package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学员端在线时长宝箱视图（首页悬浮窗用）。
 *
 * <p>在线时长取自当日学习心跳会话累计时长，宝箱分 30/60/120 分钟三档，
 * 达档可领取学习币，按「学员 + 档位 + 日期」幂等。</p>
 *
 * @author wisestar
 * @date 2026/9/12
 */
@Data
public class StudentOnlineChestView {

	/** 当日在线分钟（向下取整） */
	private long onlineMinutes;

	/** 三档宝箱状态 */
	private List<Chest> chests = new ArrayList<>();

	/**
	 * 单个宝箱档位。
	 */
	@Data
	public static class Chest {

		/** 档位分钟数（30/60/120） */
		private int tierMinutes;

		/** 达档可领学习币 */
		private int coins;

		/** 状态：locked 未达标 / claimable 可领取 / claimed 已领取 */
		private String state;

		public Chest() {
		}

		public Chest(int tierMinutes, int coins, String state) {
			this.tierMinutes = tierMinutes;
			this.coins = coins;
			this.state = state;
		}
	}

}
