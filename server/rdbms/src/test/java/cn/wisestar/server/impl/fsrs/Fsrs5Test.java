package cn.wisestar.server.impl.fsrs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FSRS-5 纯函数单元测试。
 *
 * @author wisestar
 * @date 2026/11/16
 */
class Fsrs5Test {

	@Test
	void retrievabilityIsOneAtReviewTime() {
		assertThat(Fsrs5.retrievability(0, 10)).isEqualTo(1.0);
	}

	@Test
	void retrievabilityReachesTargetWhenElapsedEqualsStability() {
		assertThat(Fsrs5.retrievability(10, 10)).isCloseTo(0.9, org.assertj.core.data.Offset.offset(0.0001));
	}

	@Test
	void retrievabilityDecreasesWithElapsedTime() {
		assertThat(Fsrs5.retrievability(5, 10)).isGreaterThan(Fsrs5.retrievability(20, 10));
	}

	@Test
	void nextIntervalGrowsWithStability() {
		assertThat(Fsrs5.nextInterval(10, 0.9)).isGreaterThan(Fsrs5.nextInterval(5, 0.9));
	}

	@Test
	void nextIntervalMatchesStabilityAtTargetRetention() {
		assertThat(Fsrs5.nextInterval(10, 0.9)).isCloseTo(10.0, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void initialStabilityIncreasesWithGrade() {
		assertThat(Fsrs5.initStability(ReviewGrade.EASY)).isGreaterThan(Fsrs5.initStability(ReviewGrade.GOOD));
		assertThat(Fsrs5.initStability(ReviewGrade.GOOD)).isGreaterThan(Fsrs5.initStability(ReviewGrade.AGAIN));
	}

	@Test
	void initialDifficultyIncreasesAsGradeWorsens() {
		assertThat(Fsrs5.initDifficulty(ReviewGrade.AGAIN)).isGreaterThan(Fsrs5.initDifficulty(ReviewGrade.EASY));
	}

	@Test
	void difficultyStaysInRange() {
		double difficulty = 5;
		for (int i = 0; i < 50; i++) {
			difficulty = Fsrs5.nextDifficulty(difficulty, ReviewGrade.AGAIN);
		}
		assertThat(difficulty).isBetween(Fsrs5.MIN_DIFFICULTY, Fsrs5.MAX_DIFFICULTY);
		for (int i = 0; i < 100; i++) {
			difficulty = Fsrs5.nextDifficulty(difficulty, ReviewGrade.EASY);
		}
		assertThat(difficulty).isBetween(Fsrs5.MIN_DIFFICULTY, Fsrs5.MAX_DIFFICULTY);
	}

	@Test
	void rememberingIncreasesStabilityAndForgettingShrinksIt() {
		double stability = 10;
		double recalled = Fsrs5.nextStability(5, stability, 0.7, ReviewGrade.GOOD, 1.0);
		assertThat(recalled).isGreaterThan(stability);
		double forgotten = Fsrs5.nextStabilityAfterLapse(5, stability, 0.7, 1.0);
		assertThat(forgotten).isLessThan(stability);
	}

	@Test
	void shortTermSuccessStillIncreasesStability() {
		assertThat(Fsrs5.nextShortTermStability(2, ReviewGrade.GOOD, 1.0)).isGreaterThan(2);
	}

}
