package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.EnglishPassRule;

/**
 * 英语知识点三态与薄弱/攻克的纯规则计算。
 *
 * <p>抽成纯函数便于单测：输入一组计数，输出掌握状态、是否薄弱、是否攻克，
 * 不读写数据库、不依赖 Spring。</p>
 *
 * <p>口径（与需求文档 R6 一致）：</p>
 * <ul>
 *   <li>判定薄弱看聚合行为，不看单次做错：连续答错 ≥2 次，或累计正确率 &lt; 60%，
 *       或输出类题型出现错误；选择题错 1 次只记录不加薄。</li>
 *   <li>攻克要求连续 2 道不同角度的派生题答对；若该知识点有输出类错题，
 *       还要求复习中至少 1 道输出题答对，防止靠选择题蒙过。</li>
 *   <li>做对 1 题后紧接着做错，连续正确计数清零重算。</li>
 * </ul>
 *
 * @author wisestar
 * @date 2026/11/05
 */
public final class EnglishStateRule {

	private EnglishStateRule() {
	}

	/** 掌握状态：熟练 */
	public static final String FAMILIAR = "familiar";

	/** 掌握状态：一般 */
	public static final String ORDINARY = "ordinary";

	/** 掌握状态：陌生 */
	public static final String UNFAMILIAR = "unfamiliar";

	/** 薄弱原因：连续答错 */
	public static final String REASON_CONSECUTIVE_WRONG = "consecutive_wrong";

	/** 薄弱原因：累计正确率不足 */
	public static final String REASON_ACCURACY = "accuracy";

	/** 薄弱原因：输出类题型出错 */
	public static final String REASON_OUTPUT_ERROR = "output_error";

	/**
	 * 由累计答题数据推导三态。
	 *
	 * @param total 累计答题数（记忆 + 练习 + 运用）
	 * @param correct 累计答对数
	 * @param weak 是否已在薄弱池
	 * @return familiar / ordinary / unfamiliar
	 */
	public static String stateOf(int total, int correct, boolean weak) {
		if (total <= 0) {
			return UNFAMILIAR;
		}
		int rate = percent(correct, total);
		if (weak) {
			// 在薄弱池时最高只能到「一般」，避免状态与复习池口径打架
			return rate >= EnglishMastery.STATE_ORDINARY_RATE ? ORDINARY : UNFAMILIAR;
		}
		if (rate >= EnglishMastery.STATE_FAMILIAR_RATE) {
			return FAMILIAR;
		}
		if (rate >= EnglishMastery.STATE_ORDINARY_RATE) {
			return ORDINARY;
		}
		return UNFAMILIAR;
	}

	/**
	 * 判定是否薄弱。
	 *
	 * @param consecutiveWrong 连续答错次数
	 * @param total 累计答题数
	 * @param correct 累计答对数
	 * @param outputWrong 输出类题型是否出现过错误
	 * @return 薄弱原因；不薄弱返回 null
	 */
	public static String weakReason(int consecutiveWrong, int total, int correct, boolean outputWrong) {
		if (consecutiveWrong >= EnglishPassRule.WEAK_CONSECUTIVE_WRONG) {
			return REASON_CONSECUTIVE_WRONG;
		}
		// 样本不足时只看连续答错：选择题错 1 次不直接判薄弱（R6.3）
		if (total >= EnglishPassRule.WEAK_MIN_SAMPLE
				&& percent(correct, total) < EnglishPassRule.WEAK_ACCURACY_RATE) {
			return REASON_ACCURACY;
		}
		if (outputWrong) {
			return REASON_OUTPUT_ERROR;
		}
		return null;
	}

	/**
	 * 判定薄弱点是否被攻克。
	 *
	 * @param consecutiveCorrect 连续答对的不同角度派生题数
	 * @param outputCorrectCount 输出类题型累计答对次数
	 * @param requireOutput 是否需要输出类题型达标（该知识点有输出类错题时为 true）
	 * @return true 表示攻克
	 */
	public static boolean canBreakthrough(int consecutiveCorrect, int outputCorrectCount, boolean requireOutput) {
		if (consecutiveCorrect < EnglishPassRule.BREAKTHROUGH_CONSECUTIVE_CORRECT) {
			return false;
		}
		return !requireOutput || outputCorrectCount >= 1;
	}

	/**
	 * 是否需要输出类题型达标才能攻克。
	 *
	 * @param weakReason 当前薄弱原因
	 * @return true 表示需要输出题答对
	 */
	public static boolean requireOutputForBreakthrough(String weakReason) {
		return REASON_OUTPUT_ERROR.equals(weakReason);
	}

	/**
	 * 正确率（百分比，四舍五入）。
	 *
	 * @param correct 答对数
	 * @param total 总数
	 * @return 0~100，总数为 0 时返回 0
	 */
	public static int percent(int correct, int total) {
		if (total <= 0) {
			return 0;
		}
		return (int) Math.round(correct * 100.0 / total);
	}
}
