package cn.wisestar.server.impl;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 英语考察角度规则单元测试。
 *
 * @author wisestar
 * @date 2026/11/05
 */
class EnglishExamAngleTest {

	@Test
	void wordPoolHasSixAngles() {
		assertThat(EnglishExamAngle.anglesOf(EnglishExamAngle.MODULE_WORD)).hasSize(6);
	}

	@Test
	void unknownModuleReturnsEmptyPool() {
		assertThat(EnglishExamAngle.anglesOf("unknown")).isEmpty();
		assertThat(EnglishExamAngle.nextAngle("unknown", Collections.emptyList())).isNull();
	}

	@Test
	void nextAnglePrefersUnused() {
		List<String> used = Arrays.asList(EnglishExamAngle.WORD_MEANING, EnglishExamAngle.MEANING_WORD);
		String next = EnglishExamAngle.nextAngle(EnglishExamAngle.MODULE_WORD, used);

		assertThat(next).isEqualTo(EnglishExamAngle.IMAGE_WORD);
		assertThat(used).doesNotContain(next);
	}

	@Test
	void nextAngleFallsBackToFirstWhenAllUsed() {
		String next = EnglishExamAngle.nextAngle(EnglishExamAngle.MODULE_WORD,
				new java.util.ArrayList<>(EnglishExamAngle.anglesOf(EnglishExamAngle.MODULE_WORD)));

		assertThat(next).isEqualTo(EnglishExamAngle.WORD_MEANING);
	}

	@Test
	void nextAngleIgnoresUnknownAnglesInUsedList() {
		String next = EnglishExamAngle.nextAngle(EnglishExamAngle.MODULE_WORD, Arrays.asList("bogus"));

		assertThat(next).isEqualTo(EnglishExamAngle.WORD_MEANING);
	}

	@Test
	void outputAnglesAreMarkedCorrectly() {
		assertThat(EnglishExamAngle.isOutputAngle(EnglishExamAngle.WORD_DICTATION)).isTrue();
		assertThat(EnglishExamAngle.isOutputAngle(EnglishExamAngle.SENTENCE_FILL)).isTrue();
		assertThat(EnglishExamAngle.isOutputAngle(EnglishExamAngle.GRAMMAR_REWRITE)).isTrue();
		assertThat(EnglishExamAngle.isOutputAngle(EnglishExamAngle.WORD_MEANING)).isFalse();
		assertThat(EnglishExamAngle.isOutputAngle(EnglishExamAngle.SENTENCE_EN_ZH)).isFalse();
	}

	@Test
	void normalizeUsedDropsUnknownAndDuplicates() {
		List<String> normalized = EnglishExamAngle.normalizeUsed(EnglishExamAngle.MODULE_SENTENCE,
				Arrays.asList(EnglishExamAngle.SENTENCE_BUILD, "bogus", EnglishExamAngle.SENTENCE_BUILD, null));

		assertThat(normalized).containsExactly(EnglishExamAngle.SENTENCE_BUILD);
	}

	@Test
	void labelOfReturnsChineseLabel() {
		assertThat(EnglishExamAngle.labelOf(EnglishExamAngle.WORD_DICTATION)).isEqualTo("单词默写");
		assertThat(EnglishExamAngle.labelOf("bogus")).isEqualTo("bogus");
	}
}
