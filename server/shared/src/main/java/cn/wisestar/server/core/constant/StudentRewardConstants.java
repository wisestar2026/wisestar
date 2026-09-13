package cn.wisestar.server.core.constant;

/**
 * 学员积分·学币奖励体系常量。
 *
 * <p>【设计原则】奖励金额只由「学习行为类型」决定，与章节/小节/知识点配置无关，
 * 因此教学内容调整不影响积分与学习币体系（体系稳定）。</p>
 *
 * <p>【学习币】分学科、单学期上限 {@link #SUBJECT_COIN_LIMIT}，只用于商品兑换；
 * 【学海积分】全学科、终身、无上限，只用于荣誉评价（头衔/证书）。</p>
 *
 * @author wisestar
 * @date 2026/9/10
 */
public final class StudentRewardConstants {

	private StudentRewardConstants() {
	}

	// ---------------- 学习行为类型 ----------------

	/** 预习 */
	public static final String ACTION_PREVIEW = "preview";

	/** 专项练习 */
	public static final String ACTION_PRACTICE = "practice";

	/** 试炼检测 */
	public static final String ACTION_TRIAL = "trial";

	/** 试炼正确率≥90% 追加奖励 */
	public static final String ACTION_TRIAL_BONUS = "trial_bonus";

	/** 错题订正 */
	public static final String ACTION_WRONG_CORRECT = "wrong_correct";

	/** 薄弱知识点复测达标 */
	public static final String ACTION_WEAK_CONQUER = "weak_conquer";

	/** 每日：完成 2 个知识点练习 */
	public static final String ACTION_DAILY_KP = "daily_kp";

	/** 每日：订正错题 ≥3 道 */
	public static final String ACTION_DAILY_WRONG = "daily_wrong";

	/** 每日：有效学习 30 分钟 */
	public static final String ACTION_DAILY_TIME = "daily_time";

	/** 章节阶段达成 */
	public static final String ACTION_CHAPTER_STAGE = "chapter_stage";

	/** 在线宝箱：在线满 30 分钟 */
	public static final String ACTION_ONLINE_CHEST_30 = "online_chest_30";

	/** 在线宝箱：在线满 60 分钟 */
	public static final String ACTION_ONLINE_CHEST_60 = "online_chest_60";

	/** 在线宝箱：在线满 120 分钟 */
	public static final String ACTION_ONLINE_CHEST_120 = "online_chest_120";

	/** 消灭知识点（掌握度达精通） */
	public static final String ACTION_KP_MASTER = "kp_master";

	/** 每日签到 */
	public static final String ACTION_DAILY_CHECKIN = "daily_checkin";

	/** 任务完成 */
	public static final String ACTION_TASK_DONE = "task_done";

	// ---------------- 奖励数值 ----------------

	/** 在线宝箱档位（分钟），与 {@link #reward} 学币一一对应 */
	public static final int[] ONLINE_CHEST_TIERS = { 30, 60, 120 };

	/** 单科单学期学习币上限 */
	public static final int SUBJECT_COIN_LIMIT = 10000;

	/** 消灭知识点（掌握度精通）阈值 */
	public static final int MASTER_THRESHOLD = 85;

	/** 试炼额外奖励的正确率门槛（% ） */
	public static final int TRIAL_BONUS_RATE = 90;

	/** 每个知识点每天有效学习 30 分钟的毫秒数 */
	public static final long DAILY_TIME_MILLIS = 30L * 60 * 1000;

	/**
	 * 奖励值：返回 `[学习币, 学海积分]`。
	 *
	 * @param actionType 学习行为类型
	 * @param stage      章节阶段（仅 chapter_stage 用，1..6，6 及以上按 6 计）
	 */
	public static int[] reward(String actionType, Integer stage) {
		switch (actionType == null ? "" : actionType) {
			case ACTION_PREVIEW:
				return new int[] { 5, 3 };
			case ACTION_PRACTICE:
				return new int[] { 20, 6 };
			case ACTION_TRIAL:
				return new int[] { 30, 10 };
			case ACTION_TRIAL_BONUS:
				return new int[] { 15, 8 };
			case ACTION_WRONG_CORRECT:
				return new int[] { 5, 4 };
			case ACTION_WEAK_CONQUER:
				return new int[] { 25, 12 };
			case ACTION_KP_MASTER:
				return new int[] { 60, 0 };
			case ACTION_DAILY_CHECKIN:
				return new int[] { 10, 0 };
			case ACTION_TASK_DONE:
				return new int[] { 15, 0 };
			case ACTION_DAILY_KP:
				return new int[] { 8, 5 };
			case ACTION_DAILY_WRONG:
				return new int[] { 6, 3 };
			case ACTION_DAILY_TIME:
				return new int[] { 7, 4 };
			case ACTION_CHAPTER_STAGE:
				int s = stage == null ? 1 : Math.max(1, Math.min(6, stage));
				int[] coins = { 300, 350, 400, 450, 500, 600 };
				int[] points = { 40, 50, 60, 75, 90, 100 };
				return new int[] { coins[s - 1], points[s - 1] };
			case ACTION_ONLINE_CHEST_30:
				return new int[] { 5, 0 };
			case ACTION_ONLINE_CHEST_60:
				return new int[] { 10, 0 };
			case ACTION_ONLINE_CHEST_120:
				return new int[] { 20, 0 };
			default:
				return null;
		}
	}

	/**
	 * 在线宝箱档位对应的行为类型。
	 *
	 * @param tierMinutes 档位分钟数（30/60/120）
	 * @return 行为类型；非法档位返回 null
	 */
	public static String onlineChestAction(int tierMinutes) {
		switch (tierMinutes) {
			case 30:
				return ACTION_ONLINE_CHEST_30;
			case 60:
				return ACTION_ONLINE_CHEST_60;
			case 120:
				return ACTION_ONLINE_CHEST_120;
			default:
				return null;
		}
	}

	/**
	 * 由累计学海积分计算头衔等级（1..5，只升不降由调用方保证）。
	 */
	public static int titleLevel(int points) {
		if (points >= 9000) {
			return 5;
		}
		if (points >= 5500) {
			return 4;
		}
		if (points >= 1600) {
			return 3;
		}
		if (points >= 300) {
			return 2;
		}
		return 1;
	}

	/**
	 * 头衔名称。
	 */
	public static String titleName(int level) {
		switch (level) {
			case 5:
				return "领航者";
			case 4:
				return "善思者";
			case 3:
				return "深耕者";
			case 2:
				return "勤学者";
			default:
				return "初探者";
		}
	}

	/**
	 * 下一头衔所需积分；已是最高头衔返回 -1。
	 */
	public static int nextTitlePoints(int level) {
		switch (level) {
			case 1:
				return 300;
			case 2:
				return 1600;
			case 3:
				return 5500;
			case 4:
				return 9000;
			default:
				return -1;
		}
	}

	/**
	 * 当前学期键：上学期 9/1-次年 1/31 记 {学年}-1；下学期 2/1-8/31 记 {年}-2。
	 */
	public static String currentSemester() {
		java.time.LocalDate now = java.time.LocalDate.now();
		int month = now.getMonthValue();
		int year = now.getYear();
		if (month >= 9) {
			return year + "-1";
		}
		if (month <= 1) {
			return (year - 1) + "-1";
		}
		return year + "-2";
	}

	/**
	 * 行为名称（积分板块/明细展示用）。
	 */
	public static String actionLabel(String actionType) {
		switch (actionType == null ? "" : actionType) {
			case ACTION_PREVIEW:
				return "知识点预习";
			case ACTION_PRACTICE:
				return "专项练习";
			case ACTION_TRIAL:
				return "试炼检测";
			case ACTION_TRIAL_BONUS:
				return "试炼优秀奖励";
			case ACTION_WRONG_CORRECT:
				return "错题订正";
			case ACTION_WEAK_CONQUER:
				return "薄弱点攻克";
			case ACTION_DAILY_KP:
				return "每日知识点练习";
			case ACTION_DAILY_WRONG:
				return "每日错题订正";
			case ACTION_DAILY_TIME:
				return "每日有效学习";
			case ACTION_CHAPTER_STAGE:
				return "章节阶段达成";
			case ACTION_ONLINE_CHEST_30:
				return "在线宝箱（30 分钟）";
			case ACTION_ONLINE_CHEST_60:
				return "在线宝箱（60 分钟）";
			case ACTION_ONLINE_CHEST_120:
				return "在线宝箱（120 分钟）";
			case ACTION_KP_MASTER:
				return "消灭知识点";
			case ACTION_DAILY_CHECKIN:
				return "每日签到";
			case ACTION_TASK_DONE:
				return "任务完成";
			default:
				return actionType;
		}
	}

}
