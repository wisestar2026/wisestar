package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.ReviewConsts;
import cn.wisestar.server.impl.fsrs.ReviewGrade;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 薄弱状态机单元测试（对需求 R5/R6 的口径校验）。
 *
 * @author wisestar
 * @date 2026/11/16
 */
class WeakStateMachineTest {

	private static WeakStateMachine.WeakSnapshot snapshot(boolean weak, String reason, String lastAngle,
			int answers, int consecutiveWrong, boolean everConquered) {
		return new WeakStateMachine.WeakSnapshot(weak, reason, lastAngle, answers, consecutiveWrong, 0,
				everConquered, answers, 0);
	}

	private static WeakStateMachine.WeakSnapshot snapshot(boolean weak, String reason, String lastAngle,
			int answers, int consecutiveWrong, boolean everConquered, int corrects, int lastGrade) {
		return new WeakStateMachine.WeakSnapshot(weak, reason, lastAngle, answers, consecutiveWrong, 0,
				everConquered, corrects, lastGrade);
	}

	private static WeakStateMachine.WeakEvidence evidence(boolean correct, int grade, double beforeR,
			double afterStability, String demand, String angle, boolean outputRequired) {
		boolean output = ReviewConsts.DEMAND_CUED.equals(demand) || ReviewConsts.DEMAND_APPLICATION.equals(demand)
				|| ReviewConsts.DEMAND_FULL.equals(demand);
		return new WeakStateMachine.WeakEvidence(correct, grade, beforeR, afterStability, demand, output, angle,
				outputRequired);
	}

	private static WeakStateMachine.WeakEvidence evidence(boolean correct, int grade, double beforeR,
			double afterStability, String demand, boolean output, String angle, boolean outputRequired) {
		return new WeakStateMachine.WeakEvidence(correct, grade, beforeR, afterStability, demand, output, angle,
				outputRequired);
	}

	@Test
	void singleWrongDoesNotMakeWeak() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(false, null, "word-meaning", 0, 0, false),
				evidence(false, ReviewGrade.AGAIN, 0.9, 1, ReviewConsts.DEMAND_RECOGNITION, "word-meaning", false));
		assertThat(result.weakFlag()).isFalse();
	}

	@Test
	void secondConsecutiveWrongMakesWeak() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(false, null, "word-meaning", 1, 1, false),
				evidence(false, ReviewGrade.AGAIN, 0.9, 1, ReviewConsts.DEMAND_RECOGNITION, "word-meaning", false));
		assertThat(result.weakFlag()).isTrue();
		assertThat(result.weakReason()).isEqualTo(ReviewConsts.REASON_CONSECUTIVE_WRONG);
	}

	@Test
	void outputErrorMakesWeakImmediately() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(false, null, "word-dictation", 0, 0, false),
				evidence(false, ReviewGrade.AGAIN, 0.9, 1, ReviewConsts.DEMAND_FULL, "word-dictation", false));
		assertThat(result.weakFlag()).isTrue();
		assertThat(result.weakReason()).isEqualTo(ReviewConsts.REASON_OUTPUT_ERROR);
	}

	@Test
	void lowRetrievabilityOnSecondAnswerMakesWeak() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(false, null, "word-meaning", 1, 0, false),
				evidence(false, ReviewGrade.AGAIN, 0.5, 1, ReviewConsts.DEMAND_RECOGNITION, "word-meaning", false));
		assertThat(result.weakFlag()).isTrue();
		assertThat(result.weakReason()).isEqualTo(ReviewConsts.REASON_LOW_RETRIEVABILITY);
	}

	@Test
	void weakItemIsConqueredWhenAllConditionsMet() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(true, ReviewConsts.REASON_CONSECUTIVE_WRONG, "word-meaning", 5, 0, false, 4,
						ReviewGrade.GOOD),
				evidence(true, ReviewGrade.GOOD, 0.7, 9, ReviewConsts.DEMAND_RECOGNITION, "meaning-word", false));
		assertThat(result.conquered()).isTrue();
		assertThat(result.weakFlag()).isFalse();
	}

	@Test
	void singlePassDoesNotConquer() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(true, ReviewConsts.REASON_CONSECUTIVE_WRONG, "word-meaning", 5, 0, false, 3,
						ReviewGrade.AGAIN),
				evidence(true, ReviewGrade.GOOD, 0.7, 9, ReviewConsts.DEMAND_RECOGNITION, "meaning-word", false));
		assertThat(result.conquered()).isFalse();
		assertThat(result.weakFlag()).isTrue();
	}

	@Test
	void hintAnswerCannotConquer() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(true, ReviewConsts.REASON_CONSECUTIVE_WRONG, "word-meaning", 5, 0, false, 4,
						ReviewGrade.GOOD),
				evidence(true, ReviewGrade.HARD, 0.7, 9, ReviewConsts.DEMAND_RECOGNITION, "meaning-word", false));
		assertThat(result.conquered()).isFalse();
		assertThat(result.weakFlag()).isTrue();
	}

	@Test
	void sameAngleKeepsWeak() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(true, ReviewConsts.REASON_CONSECUTIVE_WRONG, "word-meaning", 5, 0, false, 4,
						ReviewGrade.GOOD),
				evidence(true, ReviewGrade.GOOD, 0.7, 9, ReviewConsts.DEMAND_RECOGNITION, "word-meaning", false));
		assertThat(result.conquered()).isFalse();
		assertThat(result.weakFlag()).isTrue();
	}

	@Test
	void insufficientStabilityKeepsWeak() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(true, ReviewConsts.REASON_CONSECUTIVE_WRONG, "word-meaning", 5, 0, false, 4,
						ReviewGrade.GOOD),
				evidence(true, ReviewGrade.GOOD, 0.7, 3, ReviewConsts.DEMAND_RECOGNITION, "meaning-word", false));
		assertThat(result.conquered()).isFalse();
		assertThat(result.weakFlag()).isTrue();
	}

	@Test
	void notDueReviewKeepsWeak() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(true, ReviewConsts.REASON_CONSECUTIVE_WRONG, "word-meaning", 5, 0, false, 4,
						ReviewGrade.GOOD),
				evidence(true, ReviewGrade.EASY, 0.95, 20, ReviewConsts.DEMAND_RECOGNITION, "meaning-word", false));
		assertThat(result.conquered()).isFalse();
		assertThat(result.weakFlag()).isTrue();
	}

	@Test
	void outputWeakNeedsOutputAnswerToConquer() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(true, ReviewConsts.REASON_OUTPUT_ERROR, "word-dictation", 5, 0, false, 4,
						ReviewGrade.GOOD),
				evidence(true, ReviewGrade.GOOD, 0.7, 9, ReviewConsts.DEMAND_RECOGNITION, "word-meaning", true));
		assertThat(result.conquered()).isFalse();
		assertThat(result.weakFlag()).isTrue();
	}

	@Test
	void knowledgePointWithoutAngleCanConquer() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(true, ReviewConsts.REASON_CONSECUTIVE_WRONG, null, 5, 0, false, 4, ReviewGrade.GOOD),
				evidence(true, ReviewGrade.GOOD, 0.7, 9, ReviewConsts.DEMAND_CUED, false, null, false));
		assertThat(result.conquered()).isTrue();
		assertThat(result.weakFlag()).isFalse();
	}

	@Test
	void knowledgePointWrongIsNotOutputError() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(false, null, null, 0, 0, false, 0, 0),
				evidence(false, ReviewGrade.AGAIN, 0.9, 1, ReviewConsts.DEMAND_CUED, false, null, false));
		assertThat(result.weakFlag()).isFalse();
	}

	@Test
	void twoFragilePassesMakeWeak() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(false, null, "word-meaning", 4, 0, false, 4, ReviewGrade.HARD),
				evidence(true, ReviewGrade.HARD, 0.9, 4, ReviewConsts.DEMAND_RECOGNITION, "meaning-word", false));
		assertThat(result.weakFlag()).isTrue();
		assertThat(result.weakReason()).isEqualTo(ReviewConsts.REASON_FRAGILE);
	}

	@Test
	void singleFragilePassStaysClean() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(false, null, "word-meaning", 4, 0, false, 4, ReviewGrade.HARD),
				evidence(true, ReviewGrade.GOOD, 0.9, 4, ReviewConsts.DEMAND_RECOGNITION, "meaning-word", false));
		assertThat(result.weakFlag()).isFalse();
	}

	@Test
	void lowAccuracyMakesWeak() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(false, null, "word-meaning", 5, 0, false, 2, ReviewGrade.GOOD),
				evidence(true, ReviewGrade.GOOD, 0.9, 6, ReviewConsts.DEMAND_RECOGNITION, "meaning-word", false));
		assertThat(result.weakFlag()).isTrue();
		assertThat(result.weakReason()).isEqualTo(ReviewConsts.REASON_LOW_ACCURACY);
	}

	@Test
	void highAccuracyStaysClean() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(false, null, "word-meaning", 5, 0, false, 5, ReviewGrade.GOOD),
				evidence(true, ReviewGrade.GOOD, 0.9, 6, ReviewConsts.DEMAND_RECOGNITION, "meaning-word", false));
		assertThat(result.weakFlag()).isFalse();
	}

	@Test
	void relapseAfterConquestReopensWeak() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(false, null, "word-meaning", 8, 0, true, 7, ReviewGrade.GOOD),
				evidence(false, ReviewGrade.AGAIN, 0.5, 1, ReviewConsts.DEMAND_RECOGNITION, "word-meaning", false));
		assertThat(result.weakFlag()).isTrue();
		assertThat(result.reopened()).isTrue();
		assertThat(result.weakReason()).isEqualTo(ReviewConsts.REASON_LAPSE);
		assertThat(result.lapseDelta()).isEqualTo(1);
	}

	@Test
	void wrongRightAfterConquestAlsoReopens() {
		WeakStateMachine.WeakTransition result = WeakStateMachine.evaluate(
				snapshot(false, null, "word-meaning", 8, 0, true, 7, ReviewGrade.GOOD),
				evidence(false, ReviewGrade.AGAIN, 0.95, 1, ReviewConsts.DEMAND_RECOGNITION, "word-meaning", false));
		assertThat(result.weakFlag()).isTrue();
		assertThat(result.reopened()).isTrue();
	}

}
