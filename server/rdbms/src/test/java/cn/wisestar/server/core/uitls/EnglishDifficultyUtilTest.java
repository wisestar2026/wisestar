package cn.wisestar.server.core.uitls;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 英语难度评分器单元测试。
 *
 * <p>覆盖三个历史误判：短句因题型被直接判难、简单选择题判难、长短句不分年级；
 * 同时验证长难句仍能进入困难档。</p>
 *
 * @author wisestar
 * @date 2026/10/21
 */
class EnglishDifficultyUtilTest {

	@Test
	void shortCuedFillIsMediumInsteadOfHard() {
		EnglishDifficultyUtil.Result result = EnglishDifficultyUtil.evaluate("It's sunny and ___.", "hot",
				"三年级", EnglishDifficultyUtil.Demand.CUED, 1, Collections.emptyList(), null);

		assertThat(result.getLevel()).isEqualTo(2);
		assertThat(result.getLabel()).isEqualTo("medium");
	}

	@Test
	void shortRecognitionQuestionIsEasy() {
		EnglishDifficultyUtil.Result result = EnglishDifficultyUtil.evaluate("I'm Sam.", "B", "三年级",
				EnglishDifficultyUtil.Demand.RECOGNITION, 0,
				Arrays.asList("I'm nine.", "I'm Sam.", "I'm fine.", "I'm six."), "I'm Sam.");

		assertThat(result.getLevel()).isEqualTo(1);
		assertThat(result.getLabel()).isEqualTo("easy");
	}

	@Test
	void lowerGradeUsesShorterLengthThreshold() {
		String sentence = "I usually play football with my friends after school.";
		EnglishDifficultyUtil.Result gradeThree = EnglishDifficultyUtil.evaluate(sentence, "football", "三年级",
				EnglishDifficultyUtil.Demand.CUED, 1, Collections.emptyList(), null);
		EnglishDifficultyUtil.Result gradeFive = EnglishDifficultyUtil.evaluate(sentence, "football", "五年级",
				EnglishDifficultyUtil.Demand.CUED, 1, Collections.emptyList(), null);

		assertThat(gradeThree.getScore()).isGreaterThan(gradeFive.getScore());
	}

	@Test
	void longComplexPassageIsHard() {
		String title = "Although it was raining, Tom went to school by bus because his school starts at eight. "
				+ "He had to carry a heavy bag for his younger sister every morning before the first class started.";
		EnglishDifficultyUtil.Result result = EnglishDifficultyUtil.evaluate(title, "A", "五年级",
				EnglishDifficultyUtil.Demand.RECOGNITION, 0,
				Arrays.asList(title, "Tom walks to school.", "Tom stays at home.", "Tom goes to bed."), title);

		assertThat(result.getLevel()).isEqualTo(3);
		assertThat(result.getLabel()).isEqualTo("hard");
	}

	@Test
	void questionWithShortAnswerAndCanStaysMedium() {
		EnglishDifficultyUtil.Result result = EnglishDifficultyUtil.evaluate(
				"Can we travel around the world in two ___? Yes, we can!", "hours", "四年级",
				EnglishDifficultyUtil.Demand.CUED, 1, Collections.emptyList(), null);

		assertThat(result.getLevel()).isEqualTo(2);
	}

	@Test
	void goingToPlaceIsNotTreatedAsFuture() {
		EnglishDifficultyUtil.Result result = EnglishDifficultyUtil.evaluate(
				"Where are you going ___ the field trip? We're going to a forest.", "on", "四年级",
				EnglishDifficultyUtil.Demand.CUED, 1, Collections.emptyList(), null);

		assertThat(result.getLevel()).isEqualTo(2);
	}

	@Test
	void chineseAnnotationDoesNotInflateWordCount() {
		EnglishDifficultyUtil.Result result = EnglishDifficultyUtil.evaluate(
				"How ___ is it?（它有多高？2020年时高8848.86米。）", "high", "六年级",
				EnglishDifficultyUtil.Demand.CUED, 1, Collections.emptyList(), null);

		assertThat(result.getLevel()).isEqualTo(2);
	}

	@Test
	void phraseAnswerAndMultipleBlanksRaiseScore() {
		String title = "Please ___ the word and ___ it in your notebook.";
		EnglishDifficultyUtil.Result single = EnglishDifficultyUtil.evaluate(title, "copy", "四年级",
				EnglishDifficultyUtil.Demand.CUED, 1, Collections.emptyList(), null);
		EnglishDifficultyUtil.Result multiple = EnglishDifficultyUtil.evaluate(title, "write down", "四年级",
				EnglishDifficultyUtil.Demand.CUED, 2, Collections.emptyList(), null);

		assertThat(multiple.getScore()).isGreaterThan(single.getScore());
	}

	@Test
	void fullProductionStartsHigherThanRecognition() {
		String sentence = "I go to school by bus.";
		EnglishDifficultyUtil.Result recognition = EnglishDifficultyUtil.evaluate(sentence, "A", "三年级",
				EnglishDifficultyUtil.Demand.RECOGNITION, 0, Arrays.asList(sentence, "I walk to school."), sentence);
		EnglishDifficultyUtil.Result full = EnglishDifficultyUtil.evaluate(sentence, null, "三年级",
				EnglishDifficultyUtil.Demand.FULL, 0, Collections.emptyList(), null);

		assertThat(full.getScore()).isGreaterThan(recognition.getScore());
		assertThat(full.getLevel()).isGreaterThanOrEqualTo(2);
	}
}
