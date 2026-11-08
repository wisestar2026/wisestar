package cn.wisestar.server.core.uitls;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 英语年级难度上限单元测试。
 *
 * <p>校验 {@code maxLevelOf} 与 {@code Result#exceedsGradeLimit} 的口径：
 * 1~2 年级句式只允许 easy，3~4 年级不开放 hard，5 年级以上按年级阈值上限 3。</p>
 *
 * @author wisestar
 * @date 2026/11/05
 */
class EnglishDifficultyUtilTest {

	@Test
	void gradeNumberParsesChineseAndArabic() {
		assertThat(EnglishDifficultyUtil.gradeNumber("三年级")).isEqualTo(3);
		assertThat(EnglishDifficultyUtil.gradeNumber("6年级")).isEqualTo(6);
		assertThat(EnglishDifficultyUtil.gradeNumber("七年级")).isEqualTo(7);
		assertThat(EnglishDifficultyUtil.gradeNumber("")).isZero();
		assertThat(EnglishDifficultyUtil.gradeNumber(null)).isZero();
	}

	@Test
	void lowerGradeSentenceOnlyAllowsEasy() {
		assertThat(EnglishDifficultyUtil.maxLevelOf("一年级", "sentence")).isEqualTo(1);
		assertThat(EnglishDifficultyUtil.maxLevelOf("二年级", "sentence")).isEqualTo(1);
		assertThat(EnglishDifficultyUtil.maxLevelOf("一年级", "word")).isEqualTo(2);
	}

	@Test
	void middleGradeAllowsMediumOnly() {
		assertThat(EnglishDifficultyUtil.maxLevelOf("三年级", "sentence")).isEqualTo(2);
		assertThat(EnglishDifficultyUtil.maxLevelOf("四年级", "grammar")).isEqualTo(2);
	}

	@Test
	void upperGradeAllowsHard() {
		assertThat(EnglishDifficultyUtil.maxLevelOf("五年级", "sentence")).isEqualTo(3);
		assertThat(EnglishDifficultyUtil.maxLevelOf("六年级", "word")).isEqualTo(3);
	}

	@Test
	void unknownGradeFallsBackToHardest() {
		assertThat(EnglishDifficultyUtil.maxLevelOf(null, "sentence")).isEqualTo(3);
		assertThat(EnglishDifficultyUtil.maxLevelOf("", "word")).isEqualTo(3);
	}

	@Test
	void exceedsGradeLimitComparesLevelWithCeiling() {
		// 长难句（含从句 + 被动 + 比较级 + 多句）在一年级肯定超限
		EnglishDifficultyUtil.Result hard = EnglishDifficultyUtil.evaluate(
				"The book which was written by the famous writer is more interesting than that one. "
						+ "Although it was bought last year, it is still be read by many students.",
				"", "一年级", EnglishDifficultyUtil.Demand.CUED, 1, null, null);

		assertThat(hard.exceedsGradeLimit("一年级", "sentence")).isTrue();
		assertThat(hard.exceedsGradeLimit("一年级", "word")).isTrue();
	}

	@Test
	void shortWordWithinGradeLimit() {
		EnglishDifficultyUtil.Result easy = EnglishDifficultyUtil.evaluate("cat", "cat", "一年级",
				EnglishDifficultyUtil.Demand.RECOGNITION, 0, null, "cat");

		assertThat(easy.exceedsGradeLimit("一年级", "word")).isFalse();
	}
}
