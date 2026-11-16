package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.ReviewConsts;
import cn.wisestar.server.impl.fsrs.ReviewGrade;

/**
 * 薄弱状态机（纯函数），对英语三模块与全学科知识点使用同一套判定。
 *
 * <p>进入薄弱（满足任一）：复习前可提取度 &lt;0.6 且已有作答、连续答错 ≥2 次、
 * 输出题答错、攻克后再次答错、连续两次「答对但不稳」（提示/超时/刚过线的评分 2）、
 * 累计作答 ≥6 次且正确率 &lt;60%。</p>
 *
 * <p>攻克（全部满足）：在到期点（复习前可提取度 &lt;0.85）独立作答评分 ≥3、
 * 上一轮同样评分 ≥3（连续两次间隔复现）、换角度（无角度概念的知识点自动满足）、
 * 曾错输出题时本次需为输出型作答、复习后稳定度 ≥7 天。</p>
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

	/** 累计正确率低于该值视为薄弱 */
	public static final double LOW_ACCURACY = 0.6;

	/** 判定累计正确率前的最少作答次数 */
	public static final int MIN_ANSWERS_FOR_ACCURACY = 6;

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
		int corrects = before.corrects() + (evidence.correct() ? 1 : 0);
		boolean output = evidence.outputDemand();
		// 知识点级没有考察角度，缺省视为「换了角度」，靠连续通过把关
		boolean angleChanged = evidence.angle() == null || before.lastAngle() == null
				|| !evidence.angle().equals(before.lastAngle());

		if (!evidence.correct()) {
			if (weak) {
				if (output) {
					reason = ReviewConsts.REASON_OUTPUT_ERROR;
				}
				else if (before.consecutiveWrong() + 1 >= 2) {
					reason = ReviewConsts.REASON_CONSECUTIVE_WRONG;
				}
			}
			else if (before.everConquered()) {
				// 攻克后再答错即复发：不论是否到期，能答错说明并未真正稳固
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
			boolean outputSatisfied = !evidence.outputWeakRequired() || output;
			boolean due = evidence.beforeRetrievability() < DUE_RETRIEVABILITY;
			boolean stableEnough = evidence.afterStability() >= CONQUER_STABILITY_DAYS;
			boolean repeatedPass = before.lastGrade() >= CONQUER_GRADE;
			if (due && evidence.grade() >= CONQUER_GRADE && repeatedPass && angleChanged
					&& outputSatisfied && stableEnough) {
				weak = false;
				reason = null;
				conquered = true;
			}
		}
		else if (isFragile(before, evidence)) {
			// 两次「答对但不独立」：靠提示、超时或刚过线，表面全对实际没掌握
			weak = true;
			reason = ReviewConsts.REASON_FRAGILE;
		}
		else if (answers >= MIN_ANSWERS_FOR_ACCURACY && corrects < answers * LOW_ACCURACY) {
			// 对错交替也能靠稳定性蒙混过关，累计正确率兜底
			weak = true;
			reason = ReviewConsts.REASON_LOW_ACCURACY;
		}

		return new WeakTransition(weak, reason, conquered, reopened, lapseDelta);
	}

	/**
	 * 是否连续两次「答对但不独立」。
	 */
	private static boolean isFragile(WeakSnapshot before, WeakEvidence evidence) {
		return evidence.grade() == ReviewGrade.HARD && before.lastGrade() == ReviewGrade.HARD
				&& before.consecutiveWrong() == 0;
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

		private final int corrects;

		private final int lastGrade;

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
		 * @param corrects 累计答对次数
		 * @param lastGrade 最近一次评分
		 */
		public WeakSnapshot(boolean weakFlag, String weakReason, String lastAngle, int answers,
				int consecutiveWrong, int lapses, boolean everConquered, int corrects, int lastGrade) {
			this.weakFlag = weakFlag;
			this.weakReason = weakReason;
			this.lastAngle = lastAngle;
			this.answers = answers;
			this.consecutiveWrong = consecutiveWrong;
			this.lapses = lapses;
			this.everConquered = everConquered;
			this.corrects = corrects;
			this.lastGrade = lastGrade;
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

		public int corrects() {
			return corrects;
		}

		public int lastGrade() {
			return lastGrade;
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

		private final boolean outputDemand;

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
		 * @param outputDemand 是否为输出型作答（知识点级恒为 false）
		 * @param angle 本次角度，知识点级可为空
		 * @param outputWeakRequired 是否要求输出题达标
		 */
		public WeakEvidence(boolean correct, int grade, double beforeRetrievability, double afterStability,
				String demand, boolean outputDemand, String angle, boolean outputWeakRequired) {
			this.correct = correct;
			this.grade = grade;
			this.beforeRetrievability = beforeRetrievability;
			this.afterStability = afterStability;
			this.demand = demand;
			this.outputDemand = outputDemand;
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

		public boolean outputDemand() {
			return outputDemand;
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
