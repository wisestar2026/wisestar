package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.ReviewConsts;
import cn.wisestar.server.impl.fsrs.ReviewDemand;

/**
 * 薄弱状态机（纯函数）。
 *
 * <p>进入薄弱：证据 ≥2 次且可提取度 &lt;0.6、连续答错 ≥2 次、输出题答错，或攻克后再次答错。
 * 保持薄弱：可提取度回升不自动移出。攻克：到期复习（复习前可提取度 &lt;0.85）、评分 ≥3、
 * 角度不同、输出薄弱需输出题达标、复习后稳定度 ≥7 天。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
public final class WeakStateMachine {

	/** 攻克所需的最低稳定度（天） */
	public static final double CONQUER_STABILITY_DAYS = 7.0;

	/** 判定到期复习的最大复习前可提取度 */
	public static final double DUE_RETRIEVABILITY = 0.85;

	/** 进入薄弱的最大可提取度 */
	public static final double WEAK_RETRIEVABILITY = 0.6;

	/** 攻克所需最低评分 */
	public static final int CONQUER_GRADE = 3;

	private WeakStateMachine() {
	}

	/**
	 * 评估状态转移。
	 *
	 * @param before 作答前快照
	 * @param evidence 本次证据
	 * @return 转移结果
	 */
	public static WeakTransition evaluate(WeakSnapshot before, WeakEvidence evidence) {
		boolean weak = before.weakFlag();
		String reason = before.weakReason();
		boolean conquered = false;
		boolean reopened = false;
		int lapseDelta = 0;
		int answers = before.answers() + 1;
		boolean output = ReviewDemand.isOutput(evidence.demand());

		if (!evidence.correct()) {
			if (weak) {
				if (output) {
					reason = ReviewConsts.REASON_OUTPUT_ERROR;
				}
				else if (before.consecutiveWrong() + 1 >= 2) {
					reason = ReviewConsts.REASON_CONSECUTIVE_WRONG;
				}
			}
			else if (before.everConquered() && evidence.beforeRetrievability() < DUE_RETRIEVABILITY) {
				weak = true;
				reason = ReviewConsts.REASON_LAPSE;
				reopened = true;
				lapseDelta = 1;
			}
			else if (output) {
				weak = true;
				reason = ReviewConsts.REASON_OUTPUT_ERROR;
			}
			else if (before.consecutiveWrong() + 1 >= 2 && answers >= 2) {
				weak = true;
				reason = ReviewConsts.REASON_CONSECUTIVE_WRONG;
			}
			else if (evidence.beforeRetrievability() < WEAK_RETRIEVABILITY && answers >= 2) {
				weak = true;
				reason = ReviewConsts.REASON_LOW_RETRIEVABILITY;
			}
		}
		else if (weak) {
			boolean angleChanged = evidence.angle() != null && !evidence.angle().equals(before.lastAngle());
			boolean outputSatisfied = !evidence.outputWeakRequired() || output;
			boolean due = evidence.beforeRetrievability() < DUE_RETRIEVABILITY;
			boolean stableEnough = evidence.afterStability() >= CONQUER_STABILITY_DAYS;
			if (due && evidence.grade() >= CONQUER_GRADE && angleChanged && outputSatisfied && stableEnough) {
				weak = false;
				reason = null;
				conquered = true;
			}
		}

		return new WeakTransition(weak, reason, conquered, reopened, lapseDelta);
	}

	/**
	 * 作答前快照。
	 */
	public static final class WeakSnapshot {

		private final boolean weakFlag;

		private final String weakReason;

		private final String lastAngle;

		private final int answers;

		private final int consecutiveWrong;

		private final int lapses;

		private final boolean everConquered;

		/**
		 * 构造快照。
		 *
		 * @param weakFlag 是否在薄弱池
		 * @param weakReason 薄弱原因
		 * @param lastAngle 最近一次角度
		 * @param answers 累计作答次数
		 * @param consecutiveWrong 连续答错次数
		 * @param lapses 累计失误
		 * @param everConquered 是否曾经攻克
		 */
		public WeakSnapshot(boolean weakFlag, String weakReason, String lastAngle, int answers,
				int consecutiveWrong, int lapses, boolean everConquered) {
			this.weakFlag = weakFlag;
			this.weakReason = weakReason;
			this.lastAngle = lastAngle;
			this.answers = answers;
			this.consecutiveWrong = consecutiveWrong;
			this.lapses = lapses;
			this.everConquered = everConquered;
		}

		public boolean weakFlag() {
			return weakFlag;
		}

		public String weakReason() {
			return weakReason;
		}

		public String lastAngle() {
			return lastAngle;
		}

		public int answers() {
			return answers;
		}

		public int consecutiveWrong() {
			return consecutiveWrong;
		}

		public int lapses() {
			return lapses;
		}

		public boolean everConquered() {
			return everConquered;
		}
	}

	/**
	 * 本次证据。
	 */
	public static final class WeakEvidence {

		private final boolean correct;

		private final int grade;

		private final double beforeRetrievability;

		private final double afterStability;

		private final String demand;

		private final String angle;

		private final boolean outputWeakRequired;

		/**
		 * 构造证据。
		 *
		 * @param correct 是否正确
		 * @param grade 评分
		 * @param beforeRetrievability 复习前可提取度
		 * @param afterStability 复习后稳定度
		 * @param demand 作答方式
		 * @param angle 本次角度
		 * @param outputWeakRequired 是否要求输出题达标
		 */
		public WeakEvidence(boolean correct, int grade, double beforeRetrievability, double afterStability,
				String demand, String angle, boolean outputWeakRequired) {
			this.correct = correct;
			this.grade = grade;
			this.beforeRetrievability = beforeRetrievability;
			this.afterStability = afterStability;
			this.demand = demand;
			this.angle = angle;
			this.outputWeakRequired = outputWeakRequired;
		}

		public boolean correct() {
			return correct;
		}

		public int grade() {
			return grade;
		}

		public double beforeRetrievability() {
			return beforeRetrievability;
		}

		public double afterStability() {
			return afterStability;
		}

		public String demand() {
			return demand;
		}

		public String angle() {
			return angle;
		}

		public boolean outputWeakRequired() {
			return outputWeakRequired;
		}
	}

	/**
	 * 状态转移结果。
	 */
	public static final class WeakTransition {

		private final boolean weakFlag;

		private final String weakReason;

		private final boolean conquered;

		private final boolean reopened;

		private final int lapseDelta;

		/**
		 * 构造转移结果。
		 *
		 * @param weakFlag 是否在薄弱池
		 * @param weakReason 薄弱原因
		 * @param conquered 本次是否攻克
		 * @param reopened 是否复发
		 * @param lapseDelta 复发次数增量
		 */
		public WeakTransition(boolean weakFlag, String weakReason, boolean conquered, boolean reopened,
				int lapseDelta) {
			this.weakFlag = weakFlag;
			this.weakReason = weakReason;
			this.conquered = conquered;
			this.reopened = reopened;
			this.lapseDelta = lapseDelta;
		}

		public boolean weakFlag() {
			return weakFlag;
		}

		public String weakReason() {
			return weakReason;
		}

		public boolean conquered() {
			return conquered;
		}

		public boolean reopened() {
			return reopened;
		}

		public int lapseDelta() {
			return lapseDelta;
		}
	}

}
