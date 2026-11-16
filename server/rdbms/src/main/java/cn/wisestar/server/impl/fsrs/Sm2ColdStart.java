package cn.wisestar.server.impl.fsrs;

/**
 * SM-2 冷启动回退。
 *
 * <p>调度项历史不足 2 次时使用：首次成功 1 天、第二次 6 天，之后按 ease factor 递增；
 * 失败回到 1 天并把 ease factor 降低 0.2（下限 1.3）。稳定度取等价间隔天数。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
public final class Sm2ColdStart {

	/** 初始 ease factor */
	public static final double DEFAULT_EASE = 2.5;

	/** ease factor 下限 */
	public static final double MIN_EASE = 1.3;

	/** 首次成功间隔（天） */
	public static final double FIRST_INTERVAL = 1.0;

	/** 第二次成功间隔（天） */
	public static final double SECOND_INTERVAL = 6.0;

	private Sm2ColdStart() {
	}

	/**
	 * 计算下一次间隔与 ease factor。
	 *
	 * @param repetitions 当前成功次数
	 * @param intervalDays 当前间隔（天）
	 * @param easeFactor 当前 ease factor
	 * @param grade 评分 1~4
	 * @return 冷启动结果
	 */
	public static Sm2Result next(int repetitions, double intervalDays, double easeFactor, int grade) {
		double ease = easeFactor <= 0 ? DEFAULT_EASE : easeFactor;
		if (grade <= 1) {
			return new Sm2Result(FIRST_INTERVAL, Math.max(MIN_EASE, ease - 0.2), 0);
		}
		double qualityBonus = grade >= 4 ? 0.1 : 0;
		ease = ease + qualityBonus;
		int reps = repetitions + 1;
		if (reps <= 1) {
			return new Sm2Result(FIRST_INTERVAL, ease, reps);
		}
		if (reps == 2) {
			return new Sm2Result(SECOND_INTERVAL, ease, reps);
		}
		double base = intervalDays <= 0 ? SECOND_INTERVAL : intervalDays;
		return new Sm2Result(base * ease, ease, reps);
	}

	/**
	 * SM-2 结果。
	 */
	public static final class Sm2Result {

		private final double intervalDays;

		private final double easeFactor;

		private final int repetitions;

		/**
		 * 构造结果。
		 *
		 * @param intervalDays 下次间隔（天）
		 * @param easeFactor 新 ease factor
		 * @param repetitions 成功次数
		 */
		public Sm2Result(double intervalDays, double easeFactor, int repetitions) {
			this.intervalDays = intervalDays;
			this.easeFactor = easeFactor;
			this.repetitions = repetitions;
		}

		public double intervalDays() {
			return intervalDays;
		}

		public double easeFactor() {
			return easeFactor;
		}

		public int repetitions() {
			return repetitions;
		}
	}

}
