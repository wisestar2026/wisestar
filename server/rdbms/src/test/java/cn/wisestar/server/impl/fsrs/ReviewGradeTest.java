package cn.wisestar.server.impl.fsrs;

import cn.wisestar.server.core.constant.ReviewConsts;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 作答评分折算单元测试。
 *
 * @author wisestar
 * @date 2026/11/16
 */
class ReviewGradeTest {

	private static final String RECOGNITION = ReviewConsts.DEMAND_RECOGNITION;

	@Test
	void wrongAnswerIsAgain() {
		assertThat(ReviewGrade.gradeOf(false, null, false, false, RECOGNITION)).isEqualTo(ReviewGrade.AGAIN);
	}

	@Test
	void correctWithoutLatencyIsGood() {
		assertThat(ReviewGrade.gradeOf(true, null, false, false, RECOGNITION)).isEqualTo(ReviewGrade.GOOD);
	}

	@Test
	void correctWithHintOrRetryIsHard() {
		assertThat(ReviewGrade.gradeOf(true, null, true, false, RECOGNITION)).isEqualTo(ReviewGrade.HARD);
		assertThat(ReviewGrade.gradeOf(true, null, false, true, RECOGNITION)).isEqualTo(ReviewGrade.HARD);
	}

	@Test
	void fastCorrectAnswerIsEasy() {
		long fast = ReviewDemand.easyMillis(RECOGNITION) - 1;
		assertThat(ReviewGrade.gradeOf(true, fast, false, false, RECOGNITION)).isEqualTo(ReviewGrade.EASY);
	}

	@Test
	void slowCorrectAnswerIsHard() {
		long slow = ReviewDemand.hardMillis(RECOGNITION) + 1;
		assertThat(ReviewGrade.gradeOf(true, slow, false, false, RECOGNITION)).isEqualTo(ReviewGrade.HARD);
	}

	@Test
	void accuracyConvertsToGrade() {
		assertThat(ReviewGrade.gradeOfRate(95, 20)).isEqualTo(ReviewGrade.EASY);
		assertThat(ReviewGrade.gradeOfRate(80, 20)).isEqualTo(ReviewGrade.GOOD);
		assertThat(ReviewGrade.gradeOfRate(65, 20)).isEqualTo(ReviewGrade.HARD);
		assertThat(ReviewGrade.gradeOfRate(40, 20)).isEqualTo(ReviewGrade.AGAIN);
	}

	@Test
	void emptySessionDefaultsToGood() {
		assertThat(ReviewGrade.gradeOfRate(100, 0)).isEqualTo(ReviewGrade.GOOD);
	}

}
