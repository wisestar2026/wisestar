package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.AiPromptKeys;
import cn.wisestar.server.core.uitls.AiEndpointUtils;
import cn.wisestar.server.domain.dto.SystemInfo;
import cn.wisestar.server.domain.dto.english.EnglishReadingView;
import cn.wisestar.server.domain.model.EnglishScenarioPack;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishScenarioPackMapper;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.service.EnglishReadingService;
import cn.wisestar.server.service.SystemService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 英语单元阅读理解服务实现。
 *
 * <p>首次读取某单元时，把单元核心单词与重点句子交给 AI 生成短文和 5 道选择题，
 * 结果缓存进 {@code t_english_scenario_pack.reading_content}，后续直接读取，避免重复调用。</p>
 *
 * @author wisestar
 * @date 2026/10/18
 */
@Slf4j
@Service
public class EnglishReadingServiceImpl implements EnglishReadingService {

	private static final String DEFAULT_MODEL = "deepseek-chat";

	private final SystemService systemService;
	private final EnglishScenarioPackMapper packMapper;
	private final EnglishWordMapper wordMapper;
	private final EnglishSentenceMapper sentenceMapper;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public EnglishReadingServiceImpl(SystemService systemService, EnglishScenarioPackMapper packMapper,
			EnglishWordMapper wordMapper, EnglishSentenceMapper sentenceMapper) {
		this.systemService = systemService;
		this.packMapper = packMapper;
		this.wordMapper = wordMapper;
		this.sentenceMapper = sentenceMapper;
		this.objectMapper = new ObjectMapper();
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(15_000);
		factory.setReadTimeout(180_000);
		this.restTemplate = new RestTemplate(factory);
	}

	@Override
	public EnglishReadingView getReading(String version, String grade, String term, String unit) {
		requireText(version, "教材版本");
		requireText(grade, "年级");
		requireText(term, "册别");
		requireText(unit, "单元");
		EnglishScenarioPack pack = packMapper.selectOne(Wrappers.<EnglishScenarioPack>lambdaQuery()
				.eq(EnglishScenarioPack::getVersion, version)
				.eq(EnglishScenarioPack::getGrade, grade)
				.eq(EnglishScenarioPack::getTerm, term)
				.eq(EnglishScenarioPack::getUnit, unit)
				.last("limit 1"));
		if (pack != null && StringUtils.hasText(pack.getReadingContent())) {
			try {
				return objectMapper.readValue(pack.getReadingContent(), EnglishReadingView.class);
			}
			catch (Exception e) {
				log.warn("english reading cache parse failed, regenerate: {}", e.getMessage());
			}
		}
		EnglishReadingView view = generate(version, grade, term, unit);
		saveCache(pack, version, grade, term, unit, view);
		return view;
	}

	private EnglishReadingView generate(String version, String grade, String term, String unit) {
		List<EnglishWord> words = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(EnglishWord::getVersion, version)
				.eq(EnglishWord::getGrade, grade)
				.eq(EnglishWord::getTerm, term)
				.eq(EnglishWord::getUnit, unit)
				.orderByAsc(EnglishWord::getSpell)
				.last("LIMIT 40"));
		List<EnglishSentence> sentences = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term)
				.eq(EnglishSentence::getUnit, unit)
				.orderByAsc(EnglishSentence::getSort)
				.last("LIMIT 30"));
		if (words.isEmpty() && sentences.isEmpty()) {
			throw new ValidationException("本单元暂无阅读素材，请先完善单元单词与句子");
		}
		SystemInfo.AiSetting aiSetting = systemService.getSystemAiSetting();
		if (aiSetting == null || aiSetting.getEnabled() == null || !aiSetting.getEnabled()) {
			throw new ValidationException("AI 服务未启用：请先在系统设置中开启 AI");
		}
		String token = aiSetting.getToken();
		if (!StringUtils.hasText(token)) {
			throw new ValidationException("AI Token 未配置：请先在系统设置中填写 AI 平台 Token");
		}
		String model = (aiSetting.getModels() != null && !aiSetting.getModels().isEmpty())
				? aiSetting.getModels().get(0) : DEFAULT_MODEL;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(token.trim());
			List<Map<String, String>> messages = new ArrayList<>();
			messages.add(message("system", AiPromptKeys.resolve(aiSetting, AiPromptKeys.EN_READING, null)));
			messages.add(message("user", buildUserPrompt(grade, unit, words, sentences)));
			Map<String, Object> body = new HashMap<>();
			body.put("model", model);
			body.put("messages", messages);
			body.put("temperature", 0.7);
			body.put("max_tokens", 4096);
			body.put("stream", false);
			JsonNode resp = objectMapper.readTree(restTemplate.postForObject(
					AiEndpointUtils.resolveChatUrl(aiSetting.getBaseUrl()),
					new HttpEntity<>(body, headers), String.class));
			JsonNode choices = resp.path("choices");
			if (choices.isArray() && choices.size() > 0) {
				String content = choices.get(0).path("message").path("content").asText("");
				if (StringUtils.hasText(content)) {
					return normalize(content);
				}
			}
			throw new ValidationException("AI 返回内容为空，请稍后重试");
		}
		catch (ValidationException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("english reading generate failed: {}", e.getMessage(), e);
			throw new ValidationException("阅读理解生成失败：" + e.getMessage());
		}
	}

	private String buildUserPrompt(String grade, String unit, List<EnglishWord> words,
			List<EnglishSentence> sentences) {
		StringBuilder sb = new StringBuilder();
		sb.append("请为下面单元编写一篇英语短文和 5 道阅读理解选择题：\n");
		sb.append("年级：").append(grade).append("\n");
		sb.append("单元：").append(unit).append("\n");
		sb.append("核心单词：\n");
		for (EnglishWord word : words) {
			if (StringUtils.hasText(word.getSpell())) {
				sb.append("- ").append(word.getSpell());
				if (StringUtils.hasText(word.getMeaning())) {
					sb.append("（").append(word.getMeaning()).append("）");
				}
				sb.append("\n");
			}
		}
		sb.append("重点句子：\n");
		for (EnglishSentence sentence : sentences) {
			if (StringUtils.hasText(sentence.getEn())) {
				sb.append("- ").append(sentence.getEn());
				if (StringUtils.hasText(sentence.getZh())) {
					sb.append("（").append(sentence.getZh()).append("）");
				}
				sb.append("\n");
			}
		}
		sb.append("\n短文要自然复现这些单词和句式，题目全部基于短文，答案必须有唯一依据。");
		return sb.toString();
	}

	private EnglishReadingView normalize(String raw) throws Exception {
		String text = raw.trim();
		if (text.contains("```")) {
			int first = text.indexOf("```");
			int last = text.lastIndexOf("```");
			if (last > first) {
				text = text.substring(first + 3, last).trim();
			}
			int newline = text.indexOf('\n');
			if (newline > 0 && text.substring(0, newline).trim().toLowerCase().startsWith("json")) {
				text = text.substring(newline + 1).trim();
			}
		}
		int start = text.indexOf('{');
		int end = text.lastIndexOf('}');
		if (start >= 0 && end > start) {
			text = text.substring(start, end + 1);
		}
		JsonNode root = objectMapper.readTree(text);
		EnglishReadingView view = new EnglishReadingView();
		view.setTitle(text(root, "title"));
		view.setPassage(text(root, "passage", "text", "content"));
		view.setPassageZh(text(root, "passageZh", "translation", "zh"));
		if (!StringUtils.hasText(view.getPassage())) {
			throw new ValidationException("AI 未返回短文内容，请重试");
		}
		JsonNode questions = firstNode(root, "questions", "items", "exercises");
		if (questions != null && questions.isArray()) {
			for (JsonNode node : questions) {
				EnglishReadingView.Question question = new EnglishReadingView.Question();
				question.setStem(text(node, "stem", "question", "q", "title"));
				question.setExplanation(text(node, "explanation", "analysis"));
				JsonNode options = firstNode(node, "options", "choices");
				if (options != null && options.isArray()) {
					for (JsonNode option : options) {
						String value = option.isValueNode() ? option.asText("")
								: text(option, "text", "label", "value");
						if (StringUtils.hasText(value)) {
							question.getOptions().add(value.trim());
						}
					}
				}
				JsonNode answerIndex = node.get("answerIndex");
				if (answerIndex != null && answerIndex.isNumber()) {
					question.setAnswerIndex(answerIndex.asInt());
				}
				else {
					String answer = text(node, "answer", "correct");
					int index = indexOfAnswer(answer, question.getOptions());
					question.setAnswerIndex(index);
				}
				if (question.getOptions().size() >= 2 && question.getAnswerIndex() != null
						&& question.getAnswerIndex() >= 0
						&& question.getAnswerIndex() < question.getOptions().size()
						&& StringUtils.hasText(question.getStem())) {
					view.getQuestions().add(question);
				}
			}
		}
		if (view.getQuestions().size() < 3) {
			throw new ValidationException("AI 返回的阅读题不足，请重试");
		}
		return view;
	}

	private int indexOfAnswer(String answer, List<String> options) {
		if (!StringUtils.hasText(answer)) {
			return -1;
		}
		String value = answer.trim();
		for (int i = 0; i < options.size(); i++) {
			if (options.get(i).trim().equalsIgnoreCase(value)) {
				return i;
			}
		}
		if (value.length() == 1) {
			char letter = Character.toUpperCase(value.charAt(0));
			if (letter >= 'A' && letter <= 'D') {
				return letter - 'A';
			}
		}
		return -1;
	}

	private void saveCache(EnglishScenarioPack pack, String version, String grade, String term,
			String unit, EnglishReadingView view) {
		try {
			String json = objectMapper.writeValueAsString(view);
			if (pack == null) {
				pack = new EnglishScenarioPack();
				pack.setVersion(version);
				pack.setGrade(grade);
				pack.setTerm(term);
				pack.setUnit(unit);
				pack.setReadingContent(json);
				pack.setWordCount(0);
				pack.setPatternCount(0);
				packMapper.insert(pack);
			}
			else {
				pack.setReadingContent(json);
				packMapper.updateById(pack);
			}
		}
		catch (Exception e) {
			log.warn("english reading cache save failed: {}", e.getMessage());
		}
	}

	private Map<String, String> message(String role, String content) {
		Map<String, String> message = new HashMap<>();
		message.put("role", role);
		message.put("content", content);
		return message;
	}

	private JsonNode firstNode(JsonNode root, String... keys) {
		for (String key : keys) {
			JsonNode node = root.get(key);
			if (node != null && !node.isNull()) {
				return node;
			}
		}
		return null;
	}

	private String text(JsonNode node, String... keys) {
		JsonNode value = firstNode(node, keys);
		return value == null ? null : value.asText("");
	}

	private void requireText(String value, String name) {
		if (!StringUtils.hasText(value)) {
			throw new ValidationException(name + "不能为空");
		}
	}

}
