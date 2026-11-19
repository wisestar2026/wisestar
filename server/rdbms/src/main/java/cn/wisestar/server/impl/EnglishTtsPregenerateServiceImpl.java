package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.english.EnglishTtsPregenerateSummary;
import cn.wisestar.server.domain.model.EnglishGrammar;
import cn.wisestar.server.domain.model.EnglishReadAloud;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishGrammarMapper;
import cn.wisestar.server.mapper.EnglishReadAloudMapper;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.service.EnglishTtsPregenerateService;
import cn.wisestar.server.service.EnglishTtsService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 英语课程音频批量预生成实现。
 *
 * <p>数据来源与学员端播放入口一一对应：句子表按默认语速（109）、单词表按课文学习与练习页
 * 两种语速（109/125）、课文跟读行按默认语速、语法课本原文/例句按默认语速。缓存键复用
 * {@link EnglishTtsService#synthesize(String, String)} 的归一化与哈希逻辑，线上请求可直接命中。</p>
 *
 * @author wisestar
 * @date 2026/11/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnglishTtsPregenerateServiceImpl implements EnglishTtsPregenerateService {

	private final EnglishSentenceMapper sentenceMapper;

	private final EnglishWordMapper wordMapper;

	private final EnglishReadAloudMapper readAloudMapper;

	private final EnglishGrammarMapper grammarMapper;

	private final EnglishTtsService ttsService;

	private final ObjectMapper objectMapper;

	/** 句子/课文/语法使用的默认语速，与 english.tts.speed 保持一致 */
	@Value("${english.tts.speed:109}")
	private int defaultSpeed;

	/** 单词预生成语速：单词学习页用默认语速，练习页用 125 */
	@Value("${english.tts.pregenerate.word-speeds:109,125}")
	private String wordSpeeds;

	/** 本次最多生成多少条（0=不限制），用于小流量冒烟 */
	@Value("${english.tts.pregenerate.limit:0}")
	private int limit;

	/** 每生成多少条输出一次进度（0=不输出） */
	@Value("${english.tts.pregenerate.log-every:50}")
	private int logEvery;

	@Override
	public EnglishTtsPregenerateSummary pregenerate() {
		if (!ttsService.available()) {
			throw new IllegalStateException("服务端语音合成不可用，请先检查 english.tts.engine 与 Piper 安装");
		}
		long started = System.currentTimeMillis();
		EnglishTtsPregenerateSummary summary = new EnglishTtsPregenerateSummary();
		Set<String> seen = new HashSet<>();

		List<EnglishSentence> sentences = sentenceMapper.selectList(
				Wrappers.<EnglishSentence>lambdaQuery().select(EnglishSentence::getEn));
		generate(summary, seen, "句子", defaultSpeed, sentenceTexts(sentences));

		List<EnglishWord> words = wordMapper.selectList(
				Wrappers.<EnglishWord>lambdaQuery().select(EnglishWord::getSpell));
		List<String> wordTexts = wordTexts(words);
		for (int speed : parseSpeeds(wordSpeeds, 125)) {
			generate(summary, seen, "单词", speed, wordTexts);
		}

		List<EnglishReadAloud> passages = readAloudMapper.selectList(
				Wrappers.<EnglishReadAloud>lambdaQuery().select(EnglishReadAloud::getContent));
		generate(summary, seen, "课文跟读", defaultSpeed, readAloudTexts(passages));

		List<EnglishGrammar> grammars = grammarMapper.selectList(Wrappers.<EnglishGrammar>lambdaQuery()
				.select(EnglishGrammar::getTextbook, EnglishGrammar::getSamples, EnglishGrammar::getExamples));
		generate(summary, seen, "语法例句", defaultSpeed, grammarTexts(grammars));

		summary.setElapsedMillis(System.currentTimeMillis() - started);
		return summary;
	}

	/**
	 * 逐条补齐某个来源的音频缓存。
	 *
	 * @param summary 汇总统计
	 * @param seen    已处理的「语速 + 归一化文本」，跨来源去重
	 * @param name    来源名称
	 * @param speed   播放语速
	 * @param texts   候选英文文本
	 */
	private void generate(EnglishTtsPregenerateSummary summary, Set<String> seen, String name, int speed,
			Collection<String> texts) {
		EnglishTtsPregenerateSummary.Source source = summary.source(name + " @" + speed);
		for (String raw : texts) {
			if (limit > 0 && summary.getTotal() >= limit) {
				return;
			}
			String text = ttsService.normalizeText(raw);
			if (text == null) {
				continue;
			}
			String key = speed + "|" + text;
			if (!seen.add(key)) {
				continue;
			}
			source.setTotal(source.getTotal() + 1);
			summary.setTotal(summary.getTotal() + 1);

			if (ttsService.cached(text, String.valueOf(speed))) {
				source.setCached(source.getCached() + 1);
				summary.setCached(summary.getCached() + 1);
				continue;
			}

			byte[] audio = ttsService.synthesize(text, String.valueOf(speed));
			if (audio == null || audio.length == 0) {
				// 常驻进程偶发卡死时 synthesize 内部会回退一次性进程；这里再补一次，降低单句失败率
				audio = ttsService.synthesize(text, String.valueOf(speed));
			}
			if (audio == null || audio.length == 0) {
				source.setFailed(source.getFailed() + 1);
				summary.setFailed(summary.getFailed() + 1);
				log.warn("语音预生成失败：{} @ {}，文本 {}", name, speed, abbreviate(text));
			} else {
				source.setGenerated(source.getGenerated() + 1);
				summary.setGenerated(summary.getGenerated() + 1);
			}

			if (logEvery > 0 && summary.getTotal() % logEvery == 0) {
				log.info("语音预生成进度：已处理 {} 条，新增 {}，命中 {}，失败 {}，当前 {} @ {}", summary.getTotal(),
						summary.getGenerated(), summary.getCached(), summary.getFailed(), name, speed);
			}
		}
	}

	private List<String> sentenceTexts(List<EnglishSentence> sentences) {
		List<String> texts = new ArrayList<>();
		for (EnglishSentence sentence : sentences) {
			texts.add(sentence.getEn());
		}
		return texts;
	}

	private List<String> wordTexts(List<EnglishWord> words) {
		List<String> texts = new ArrayList<>();
		for (EnglishWord word : words) {
			texts.add(word.getSpell());
		}
		return texts;
	}

	private List<String> readAloudTexts(List<EnglishReadAloud> passages) {
		List<String> texts = new ArrayList<>();
		for (EnglishReadAloud passage : passages) {
			texts.addAll(jsonTexts(passage.getContent(), objectMapper));
		}
		return texts;
	}

	private List<String> grammarTexts(List<EnglishGrammar> grammars) {
		List<String> texts = new ArrayList<>();
		for (EnglishGrammar grammar : grammars) {
			texts.addAll(jsonTexts(grammar.getTextbook(), objectMapper));
			texts.addAll(jsonTexts(grammar.getSamples(), objectMapper));
			texts.addAll(jsonTexts(grammar.getExamples(), objectMapper));
		}
		return texts;
	}

	/**
	 * 解析 JSON 示例列，兼容 JSON 字符串、字符串套 JSON、数组及 `{en,text,example}` 对象。
	 *
	 * <p>包内可见便于单测；与前端 `EnglishGrammarLearnPage` 的 `exampleText` 取值顺序一致。</p>
	 *
	 * @param raw    原始字段值
	 * @param mapper JSON 解析器
	 * @return 英文文本列表
	 */
	static List<String> jsonTexts(String raw, ObjectMapper mapper) {
		List<String> texts = new ArrayList<>();
		if (raw == null || raw.trim().isEmpty()) {
			return texts;
		}
		JsonNode node = readJson(raw.trim(), mapper);
		if (node == null) {
			return texts;
		}
		for (int depth = 0; depth < 2 && node.isTextual(); depth++) {
			String value = node.asText().trim();
			if (value.isEmpty()) {
				return texts;
			}
			JsonNode nested = readJson(value, mapper);
			if (nested == null) {
				texts.add(value);
				return texts;
			}
			node = nested;
		}
		if (node.isArray()) {
			node.forEach(child -> addNodeText(child, texts));
		} else {
			addNodeText(node, texts);
		}
		return texts;
	}

	private static void addNodeText(JsonNode node, List<String> texts) {
		if (node == null || node.isNull()) {
			return;
		}
		if (node.isTextual()) {
			addText(node.asText(), texts);
			return;
		}
		if (node.isObject()) {
			for (String field : new String[] { "en", "text", "example" }) {
				JsonNode value = node.get(field);
				if (value != null && value.isTextual()) {
					addText(value.asText(), texts);
					return;
				}
			}
		}
	}

	private static void addText(String value, List<String> texts) {
		if (value != null && !value.trim().isEmpty()) {
			texts.add(value.trim());
		}
	}

	private static JsonNode readJson(String value, ObjectMapper mapper) {
		try {
			return mapper.readTree(value);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 解析逗号分隔的语速，与运行时 `parseSpeed` 保持同样的 80~220 边界。
	 *
	 * @param raw      配置值
	 * @param fallback 无有效值时使用的语速
	 * @return 去重后的语速列表
	 */
	static List<Integer> parseSpeeds(String raw, int fallback) {
		Set<Integer> speeds = new LinkedHashSet<>();
		if (raw != null) {
			for (String part : raw.split(",")) {
				String value = part.trim();
				if (value.isEmpty()) {
					continue;
				}
				try {
					int speed = Integer.parseInt(value);
					speeds.add(Math.max(80, Math.min(220, speed)));
				} catch (NumberFormatException e) {
					log.warn("忽略非法语速配置：{}", value);
				}
			}
		}
		if (speeds.isEmpty()) {
			speeds.add(Math.max(80, Math.min(220, fallback)));
		}
		return new ArrayList<>(speeds);
	}

	private String abbreviate(String text) {
		String value = text.toLowerCase(Locale.ROOT);
		return value.length() <= 60 ? value : value.substring(0, 60) + "...";
	}

}
