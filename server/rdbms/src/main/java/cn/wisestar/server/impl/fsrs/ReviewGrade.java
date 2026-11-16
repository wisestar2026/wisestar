package cn.wisestar.server.impl.fsrs;

/**
 * 评分折算：对错、用时、提示与重做 → FSRS 评分 1~4。
 *
 * <p>答错为 1；答对但用了提示或经过重做为 2；答对且用时低于该作答方式中位用时的
 * 55% 为 4；高于慢速阈值为 2；其余为 3；未提供用时按 3 处理。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
public final class ReviewGrade {

	/** 评分：Again */
	public static final int AGAIN = 1;

	/** 评分：Hard */
	public static final int HARD = 2;

	/** 评分：Good */
	public static final int GOOD = 3;

	/** 评分：Easy */
	public static final int EASY = 4;

	private ReviewGrade() {
	}

	/**
	 * 折算评分。
	 *
	 * @param correct 是否正确
	 * @param latencyMs 用时（毫秒），可空
	 * @param hintUsed 是否使用提示
	 * @param retried 是否经过重做
	 * @param demand 作答方式
	 * @return 1~4
	 */
	public static int gradeOf(boolean correct, Long latencyMs, boolean hintUsed, boolean retried, String demand) {
		if (!correct) {
			return AGAIN;
		}
		if (hintUsed || retried) {
			return HARD;
		}
		if (latencyMs == null || latencyMs <= 0) {
			return GOOD;
		}
		if (latencyMs < ReviewDemand.easyMillis(demand)) {
			return EASY;
		}
		if (latencyMs > ReviewDemand.hardMillis(demand)) {
			return HARD;
		}
		return GOOD;
	}

	/**
	 * 会话正确率折算评分（知识点级使用）。
	 *
	 * @param correctRate 正确率 0-100
	 * @param total 会话题量
	 * @return 1~4
	 */
	public static int gradeOfRate(int correctRate, int total) {
		if (total <= 0) {
			return GOOD;
		}
		if (correctRate >= 90) {
			return EASY;
		}
		if (correctRate >= 75) {
			return GOOD;
		}
		if (correctRate >= 60) {
			return HARD;
		}
		return AGAIN;
	}

}
