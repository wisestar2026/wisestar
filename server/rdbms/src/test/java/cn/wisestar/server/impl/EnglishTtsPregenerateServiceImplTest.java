package cn.wisestar.server.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 英语音频预生成文本解析单元测试。
 *
 * @author wisestar
 * @date 2026/11/19
 */
class EnglishTtsPregenerateServiceImplTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void parsesArrayOfStringsAndObjects() {
		List<String> texts = EnglishTtsPregenerateServiceImpl.jsonTexts(
				"[\"Hello.\", {\"en\":\"How are you?\",\"zh\":\"你好吗？\"}, {\"text\":\"Fine.\"}]", mapper);

		assertThat(texts).containsExactly("Hello.", "How are you?", "Fine.");
	}

	@Test
	void unwrapsStringEncodedJson() throws Exception {
		String raw = mapper.writeValueAsString("[{\"en\":\"Nice to meet you.\"}]");

		assertThat(EnglishTtsPregenerateServiceImpl.jsonTexts(raw, mapper))
				.containsExactly("Nice to meet you.");
	}

	@Test
	void keepsQuotedJsonString() throws Exception {
		assertThat(EnglishTtsPregenerateServiceImpl.jsonTexts(mapper.writeValueAsString("Good morning!"), mapper))
				.containsExactly("Good morning!");
	}

	@Test
	void ignoresMalformedOrEmptyValues() {
		assertThat(EnglishTtsPregenerateServiceImpl.jsonTexts("not-json", mapper)).isEmpty();
		assertThat(EnglishTtsPregenerateServiceImpl.jsonTexts("[]", mapper)).isEmpty();
		assertThat(EnglishTtsPregenerateServiceImpl.jsonTexts("  ", mapper)).isEmpty();
	}

	@Test
	void parsesSpeedsDeduplicatesAndFallsBack() {
		assertThat(EnglishTtsPregenerateServiceImpl.parseSpeeds("109, 125, 109, bad", 109))
				.containsExactly(109, 125);
		assertThat(EnglishTtsPregenerateServiceImpl.parseSpeeds("", 109)).containsExactly(109);
		assertThat(EnglishTtsPregenerateServiceImpl.parseSpeeds("999", 109)).containsExactly(220);
	}

}
