package cn.wisestar.server.impl.fsrs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SM-2 冷启动单元测试。
 *
 * @author wisestar
 * @date 2026/11/16
 */
class Sm2ColdStartTest {

	@Test
	void firstSuccessSchedulesOneDay() {
		Sm2ColdStart.Sm2Result result = Sm2ColdStart.next(0, 0, Sm2ColdStart.DEFAULT_EASE, ReviewGrade.GOOD);
		assertThat(result.intervalDays()).isEqualTo(Sm2ColdStart.FIRST_INTERVAL);
		assertThat(result.repetitions()).isEqualTo(1);
	}

	@Test
	void secondSuccessSchedulesSixDays() {
		Sm2ColdStart.Sm2Result result = Sm2ColdStart.next(1, 1, Sm2ColdStart.DEFAULT_EASE, ReviewGrade.GOOD);
		assertThat(result.intervalDays()).isEqualTo(Sm2ColdStart.SECOND_INTERVAL);
	}

	@Test
	void laterSuccessMultipliesByEaseFactor() {
		Sm2ColdStart.Sm2Result result = Sm2ColdStart.next(2, 6, Sm2ColdStart.DEFAULT_EASE, ReviewGrade.GOOD);
		assertThat(result.intervalDays()).isEqualTo(6 * Sm2ColdStart.DEFAULT_EASE);
	}

	@Test
	void easyAnswerRaisesEaseFactor() {
		Sm2ColdStart.Sm2Result result = Sm2ColdStart.next(2, 6, Sm2ColdStart.DEFAULT_EASE, ReviewGrade.EASY);
		assertThat(result.easeFactor()).isGreaterThan(Sm2ColdStart.DEFAULT_EASE);
	}

	@Test
	void failureResetsToOneDayAndLowersEaseFactor() {
		Sm2ColdStart.Sm2Result result = Sm2ColdStart.next(3, 15, Sm2ColdStart.DEFAULT_EASE, ReviewGrade.AGAIN);
		assertThat(result.intervalDays()).isEqualTo(Sm2ColdStart.FIRST_INTERVAL);
		assertThat(result.easeFactor()).isEqualTo(Sm2ColdStart.DEFAULT_EASE - 0.2);
		assertThat(result.repetitions()).isZero();
	}

	@Test
	void easeFactorNeverDropsBelowFloor() {
		Sm2ColdStart.Sm2Result result = Sm2ColdStart.next(3, 15, Sm2ColdStart.MIN_EASE, ReviewGrade.AGAIN);
		assertThat(result.easeFactor()).isEqualTo(Sm2ColdStart.MIN_EASE);
	}

}
