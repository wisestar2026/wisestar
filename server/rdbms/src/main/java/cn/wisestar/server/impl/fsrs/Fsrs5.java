package cn.wisestar.server.impl.fsrs;

import static java.lang.Math.exp;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;

/**
 * FSRS-5 纯函数实现。
 *
 * <p>用难度 D（1~10）、稳定度 S（天）与可提取度 R（0~1）描述记忆。
 * R(t,S) = (1 + 19/81 · t/S)^(-0.5)，目标保持率 r 对应间隔
 * I(S,r) = S / (19/81) · (r^(-2) - 1)。参数取 FSRS-5 默认值。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
public final class Fsrs5 {

	/** 衰减指数 */
	public static final double DECAY = -0.5;

	/** 间隔系数 19/81 */
	public static final double FACTOR = 19.0 / 81.0;

	/** 难度下限 */
	public static final double MIN_DIFFICULTY = 1.0;

	/** 难度上限 */
	public static final double MAX_DIFFICULTY = 10.0;

	/** 稳定度下限（天） */
	public static final double MIN_STABILITY = 0.1;

	/** FSRS-5 默认参数 */
	private static final double[] W = { 0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046,
			1.54575, 0.1192, 1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315, 2.9898, 0.51655, 0.6621 };

	private Fsrs5() {
	}

	/**
	 * 初始稳定度 S0(G)。
	 *
	 * @param grade 评分 1~4
	 * @return 稳定度（天）
	 */
	public static double initStability(int grade) {
		int index = Math.max(1, Math.min(grade, 4)) - 1;
		return max(MIN_STABILITY, W[index]);
	}

	/**
	 * 初始难度 D0(G)。
	 *
	 * @param grade 评分 1~4
	 * @return 难度 1~10
	 */
	public static double initDifficulty(int grade) {
		int g = Math.max(1, Math.min(grade, 4));
		return clampDifficulty(W[4] - exp(W[5] * (g - 1)) + 1);
	}

	/**
	 * 可提取度。
	 *
	 * @param elapsedDays 距上次复习天数
	 * @param stability 稳定度（天）
	 * @return 0~1
	 */
	public static double retrievability(double elapsedDays, double stability) {
		double elapsed = max(0, elapsedDays);
		double s = max(MIN_STABILITY, stability);
		return pow(1 + FACTOR * elapsed / s, DECAY);
	}

	/**
	 * 目标保持率对应的复习间隔。
	 *
	 * @param stability 稳定度（天）
	 * @param retention 目标保持率 0~1
	 * @return 间隔（天）
	 */
	public static double nextInterval(double stability, double retention) {
		double r = Math.max(0.5, Math.min(retention, 0.99));
		double s = max(MIN_STABILITY, stability);
		return s / FACTOR * (pow(r, 1 / DECAY) - 1);
	}

	/**
	 * 难度更新。
	 *
	 * @param difficulty 当前难度
	 * @param grade 评分 1~4
	 * @return 新难度 1~10
	 */
	public static double nextDifficulty(double difficulty, int grade) {
		int g = Math.max(1, Math.min(grade, 4));
		double d0Easy = W[4] - exp(W[5] * 3) + 1;
		double next = W[7] * d0Easy + (1 - W[7]) * (clampDifficulty(difficulty) - W[6] * (g - 3));
		return clampDifficulty(next);
	}

	/**
	 * 成功后的稳定度。
	 *
	 * @param difficulty 难度
	 * @param stability 当前稳定度
	 * @param retrievability 复习前可提取度
	 * @param grade 评分 2~4
	 * @param demandGain 作答方式增益（产出型 &gt;1）
	 * @return 新稳定度（天）
	 */
	public static double nextStability(double difficulty, double stability, double retrievability, int grade,
			double demandGain) {
		double d = clampDifficulty(difficulty);
		double s = max(MIN_STABILITY, stability);
		double r = clampRetrievability(retrievability);
		double hardPenalty = grade == 2 ? W[15] : 1.0;
		double easyBonus = grade == 4 ? W[16] : 1.0;
		double growth = 1 + exp(W[8]) * (11 - d) * pow(s, -W[9]) * (exp(W[10] * (1 - r)) - 1)
				* hardPenalty * easyBonus;
		return max(MIN_STABILITY, s * max(1.0, growth) * demandGain);
	}

	/**
	 * 失败后的稳定度（不高于复习前）。
	 *
	 * @param difficulty 难度
	 * @param stability 当前稳定度
	 * @param retrievability 复习前可提取度
	 * @param demandPenalty 作答方式惩罚（产出型 &lt;1）
	 * @return 新稳定度（天）
	 */
	public static double nextStabilityAfterLapse(double difficulty, double stability, double retrievability,
			double demandPenalty) {
		double d = clampDifficulty(difficulty);
		double s = max(MIN_STABILITY, stability);
		double r = clampRetrievability(retrievability);
		double next = W[11] * pow(d, -W[12]) * (pow(s + 1, W[13]) - 1) * exp(W[14] * (1 - r));
		next = next * demandPenalty;
		return max(MIN_STABILITY, min(s, next));
	}

	/**
	 * 同日复习的稳定度。
	 *
	 * @param stability 当前稳定度
	 * @param grade 评分 1~4
	 * @param demandGain 作答方式增益
	 * @return 新稳定度（天）
	 */
	public static double nextShortTermStability(double stability, int grade, double demandGain) {
		int g = Math.max(1, Math.min(grade, 4));
		double s = max(MIN_STABILITY, stability);
		return max(MIN_STABILITY, s * exp(W[17] * (g - 3 + W[18])) * demandGain);
	}

	/**
	 * 难度钳制。
	 *
	 * @param difficulty 难度
	 * @return 1~10
	 */
	public static double clampDifficulty(double difficulty) {
		return Math.max(MIN_DIFFICULTY, Math.min(MAX_DIFFICULTY, difficulty));
	}

	/**
	 * 可提取度钳制。
	 *
	 * @param retrievability 可提取度
	 * @return 0.01~1
	 */
	public static double clampRetrievability(double retrievability) {
		return Math.max(0.01, Math.min(1.0, retrievability));
	}

}
