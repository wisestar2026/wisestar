package cn.wisestar.server.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 英语三态与薄弱/攻克规则单元测试（对需求 R6 的口径校验）。
 *
 * @author wisestar
 * @date 2026/11/05
 */
class EnglishStateRuleTest {

	@Test
	void noAnswerIsUnfamiliar() {
		assertThat(EnglishStateRule.stateOf(0, 0, false)).isEqualTo(EnglishStateRule.UNFAMILIAR);
	}

	@Test
	void highAccuracyIsFamiliar() {
		assertThat(EnglishStateRule.stateOf(10, 9, false)).isEqualTo(EnglishStateRule.FAMILIAR);
	}

	@Test
	void mediumAccuracyIsOrdinary() {
		assertThat(EnglishStateRule.stateOf(10, 7, false)).isEqualTo(EnglishStateRule.ORDINARY);
	}

	@Test
	void lowAccuracyIsUnfamiliar() {
		assertThat(EnglishStateRule.stateOf(10, 5, false)).isEqualTo(EnglishStateRule.UNFAMILIAR);
	}

	@Test
	void weakCapsFamiliarToOrdinary() {
		assertThat(EnglishStateRule.stateOf(10, 9, true)).isEqualTo(EnglishStateRule.ORDINARY);
	}

	@Test
	void weakWithLowAccuracyStaysUnfamiliar() {
		assertThat(EnglishStateRule.stateOf(10, 5, true)).isEqualTo(EnglishStateRule.UNFAMILIAR);
	}

	@Test
	void twoConsecutiveWrongMarksWeak() {
		assertThat(EnglishStateRule.weakReason(2, 2, 0, false))
				.isEqualTo(EnglishStateRule.REASON_CONSECUTIVE_WRONG);
	}

	@Test
	void singleWrongOnChoiceIsNotWeak() {
		assertThat(EnglishStateRule.weakReason(1, 1, 0, false)).isNull();
	}

	@Test
	void smallSampleDoesNotTriggerAccuracyWeak() {
		// 2 题错 1 题正确率 50%，但样本不足，只看连续答错
		assertThat(EnglishStateRule.weakReason(0, 2, 1, false)).isNull();
	}

	@Test
	void lowAccuracyMarksWeak() {
		assertThat(EnglishStateRule.weakReason(0, 10, 5, false)).isEqualTo(EnglishStateRule.REASON_ACCURACY);
	}

	@Test
	void outputErrorMarksWeak() {
		assertThat(EnglishStateRule.weakReason(0, 10, 9, true)).isEqualTo(EnglishStateRule.REASON_OUTPUT_ERROR);
	}

	@Test
	void breakthroughNeedsTwoAngles() {
		assertThat(EnglishStateRule.canBreakthrough(1, 1, false)).isFalse();
		assertThat(EnglishStateRule.canBreakthrough(2, 0, false)).isTrue();
	}

	@Test
	void outputErrorRequiresOutputCorrect() {
		assertThat(EnglishStateRule.canBreakthrough(2, 0, true)).isFalse();
		assertThat(EnglishStateRule.canBreakthrough(2, 1, true)).isTrue();
	}

	@Test
	void requireOutputOnlyForOutputError() {
		assertThat(EnglishStateRule.requireOutputForBreakthrough(EnglishStateRule.REASON_OUTPUT_ERROR)).isTrue();
		assertThat(EnglishStateRule.requireOutputForBreakthrough(EnglishStateRule.REASON_ACCURACY)).isFalse();
		assertThat(EnglishStateRule.requireOutputForBreakthrough(null)).isFalse();
	}

	@Test
	void percentRoundsAndGuardsZero() {
		assertThat(EnglishStateRule.percent(1, 3)).isEqualTo(33);
		assertThat(EnglishStateRule.percent(2, 3)).isEqualTo(67);
		assertThat(EnglishStateRule.percent(0, 0)).isZero();
	}
}
