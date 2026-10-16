package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.AiPromptKeys;
import cn.wisestar.server.core.uitls.AiEndpointUtils;
import cn.wisestar.server.domain.dto.SystemInfo;
import cn.wisestar.server.domain.dto.english.EnglishSentenceExercise;
import cn.wisestar.server.domain.dto.english.EnglishSentencePackQuery;
import cn.wisestar.server.domain.dto.english.EnglishSentencePackView;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishSentencePack;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishSentencePackMapper;
import cn.wisestar.server.service.EnglishSentencePackService;
import cn.wisestar.server.service.SystemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 英语单元句子同步练习内容包服务实现（预生成 + 固化落库 + 学员端只读）。
 *
 * <p>管理端按 版本+年级+册别+单元 取出单元重点句子，按固定分组逐组调用系统 AI 生成
 * 多题型练习（连词成句 / 句子默写 / 句子选择），确认后以业务键覆盖保存；
 * 学员端仅读取固化内容，不再触发实时 AI。</p>
 *
 * @author wisestar
 * @date 2026/10/15
 */
@Slf4j
@Service
public class EnglishSentencePackServiceImpl implements EnglishSentencePackService {

	/** 每组送入 AI 的句子条数 */
	private static final int SENTENCE_GROUP_SIZE = 6;

	/** 未配置模型时的兜底模型 */
	private static final String DEFAULT_MODEL = "deepseek-chat";

	/** 生成结果 JSON 允许的题目数组 key 别名 */
	private static final String[] EXERCISE_ARRAY_KEYS = { "exercises", "questions", "items", "list" };

	private final EnglishSentencePackMapper packMapper;
	private final EnglishSentenceMapper sentenceMapper;
	private final SystemService systemService;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public EnglishSentencePackServiceImpl(EnglishSentencePackMapper packMapper, EnglishSentenceMapper sentenceMapper,
			SystemService systemService) {
		this.packMapper = packMapper;
		this.sentenceMapper = sentenceMapper;
		this.systemService = systemService;
		this.objectMapper = new ObjectMapper();
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(15_000);
		factory.setReadTimeout(180_000);
		this.restTemplate = new RestTemplate(factory);
	}

	// ------------------------------------------------------------------ 生成

	@Override
	public EnglishSentencePackView generate(String version, String grade, String term, String unit) {
		requireNotBlank(version, "教材版本");
		requireNotBlank(grade, "年级");
		requireNotBlank(term, "册别");
		requireNotBlank(unit, "单元");

		List<EnglishSentence> sentences = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term)
				.eq(EnglishSentence::getUnit, unit)
				.orderByAsc(EnglishSentence::getSort));

		EnglishSentencePackView view = new EnglishSentencePackView();
		view.setVersion(version);
		view.setGrade(grade);
		view.setTerm(term);
		view.setUnit(unit);

		List<EnglishSentenceExercise> all = new ArrayList<>();
		for (List<EnglishSentence> chunk : partition(sentences, SENTENCE_GROUP_SIZE)) {
			all.addAll(generateGroup(grade, unit, chunk));
		}
		view.setContent(writeExercises(all));
		view.setCount(all.size());
		return view;
	}

	/**
	 * 调用 AI 生成一组句子的多题型练习。
	 */
	private List<EnglishSentenceExercise> generateGroup(String grade, String unit, List<EnglishSentence> chunk) {
		List<EnglishSentence> valid = chunk.stream()
				.filter(s -> s.getEn() != null && !s.getEn().trim().isEmpty())
				.collect(Collectors.toList());
		if (valid.isEmpty()) {
			return new ArrayList<>();
		}
		String raw = callChat(grade, unit, valid);
		JsonNode root = parseRawJson(raw);
		return normalizeExercises(root, valid);
	}

	/**
	 * 调用大模型生成句子练习。
	 */
	private String callChat(String grade, String unit, List<EnglishSentence> sentences) {
		SystemInfo.AiSetting aiSetting = systemService.getSystemAiSetting();
		if (aiSetting == null || aiSetting.getEnabled() == null || !aiSetting.getEnabled()) {
			throw new ValidationException("AI 服务未启用：请先在系统设置中开启 AI 并完成平台配置");
		}
		String token = aiSetting.getToken();
		if (token == null || token.trim().isEmpty()) {
			throw new ValidationException("AI Token 未配置：请先在系统设置中填写 AI 平台 Token");
		}

		String model = (aiSetting.getModels() != null && !aiSetting.getModels().isEmpty())
				? aiSetting.getModels().get(0)
				: DEFAULT_MODEL;

		String userPrompt = buildUserPrompt(grade, unit, sentences);

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(token.trim());

			List<Map<String, String>> messages = new ArrayList<>();
			messages.add(message("system", AiPromptKeys.resolve(aiSetting, AiPromptKeys.EN_SENTENCE, null)));
			messages.add(message("user", userPrompt));

			Map<String, Object> body = new HashMap<>();
			body.put("model", model);
			body.put("messages", messages);
			body.put("temperature", 0.7);
			body.put("max_tokens", 4096);
			body.put("stream", false);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
			JsonNode resp = objectMapper.readTree(restTemplate.postForObject(
					AiEndpointUtils.resolveChatUrl(aiSetting.getBaseUrl()), request, String.class));
			JsonNode choices = resp.path("choices");
			if (choices.isArray() && choices.size() > 0) {
				String content = choices.get(0).path("message").path("content").asText("");
				if (!content.trim().isEmpty()) {
					return content;
				}
			}
			log.error("AI 返回异常结构：{}", writeJson(resp));
			throw new ValidationException("AI 返回内容为空，请稍后重试");
		} catch (ValidationException e) {
			throw e;
		} catch (Exception e) {
			log.error("AI 句子练习生成请求失败：{}", e.getMessage(), e);
			throw new ValidationException("AI 生成失败：" + e.getMessage());
		}
	}

	/**
	 * 构建请求 AI 生成句子练习的用户提示词。
	 */
	private String buildUserPrompt(String grade, String unit, List<EnglishSentence> sentences) {
		StringBuilder sb = new StringBuilder();
		sb.append("请基于以下英语单元重点句子，编写多题型句子同步练习：\n");
		sb.append("年级：").append(grade).append("\n");
		sb.append("单元：").append(unit).append("\n");
		sb.append("本组句子（务必全部覆盖）：\n");
		for (EnglishSentence s : sentences) {
			String zh = s.getZh() == null ? "" : s.getZh().trim();
			sb.append("- ").append(s.getEn().trim()).append("（").append(zh).append("）\n");
		}
		sb.append("\n要求：\n");
		sb.append("1) 本组共 ").append(sentences.size()).append(" 句，请逐句出题：每句 1~2 道题、同一句最多 2 道（禁止对同一句反复出题），三种 kind 混用、尽量均衡；\n");
		sb.append("2) kind=build 连词成句、kind=dictation 句子默写、kind=choice 句子选择；\n");
		sb.append("3) choice 的 options 恰好 4 个，含 1 个正确项（等于 answer/en）；\n");
		sb.append("4) 所有 en/answer 与给定句子一致（大小写与标点保持原样），不得改写或拼接其它句子。");
		return sb.toString();
	}

	/**
	 * 将模型返回内容解析为 JSON 节点（容忍 ```json 代码块包裹与前后噪声）。
	 */
	private JsonNode parseRawJson(String raw) {
		String text = raw.trim();
		if (text.contains("```")) {
			int first = text.indexOf("```");
			int last = text.lastIndexOf("```");
			if (last > first) {
				text = text.substring(first + 3, last).trim();
			}
			int newline = text.indexOf('\n');
			if (newline > 0) {
				String firstLine = text.substring(0, newline).trim();
				if (firstLine.startsWith("json")) {
					text = text.substring(newline + 1).trim();
				}
			}
		}
		int start = text.indexOf('{');
		int end = text.lastIndexOf('}');
		if (start >= 0 && end > start) {
			text = text.substring(start, end + 1);
		}
		try {
			return objectMapper.readTree(text);
		} catch (Exception e) {
			log.error("AI 返回内容无法解析为 JSON：{}", text.length() > 500 ? text.substring(0, 500) : text);
			throw new ValidationException("AI 返回内容格式异常，请重新生成");
		}
	}

	/**
	 * 规整模型 JSON 为题目列表：归一题型、补全选择题 answer、过滤无效项、回填来源句子 ID。
	 */
	private List<EnglishSentenceExercise> normalizeExercises(JsonNode root, List<EnglishSentence> sentences) {
		List<EnglishSentenceExercise> result = new ArrayList<>();
		JsonNode rawExercises = firstNode(root, EXERCISE_ARRAY_KEYS);
		if (rawExercises == null || !rawExercises.isArray()) {
			return result;
		}
		for (JsonNode raw : rawExercises) {
			String en = text(raw, "en", "sentence", "answer");
			String zh = text(raw, "zh", "cn", "prompt", "stem", "title");
			if (isBlank(en)) {
				continue;
			}
			EnglishSentenceExercise ex = new EnglishSentenceExercise();
			ex.setEn(en);
			ex.setZh(zh);
			ex.setHint(text(raw, "hint"));
			ex.setExplanation(text(raw, "explanation", "analysis"));

			String kind = text(raw, "kind", "type");
			kind = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
			if (!"build".equals(kind) && !"dictation".equals(kind) && !"choice".equals(kind)) {
				kind = "build";
			}
			ex.setKind(kind);

			if ("choice".equals(kind)) {
				List<String> options = readStringArray(raw, "options", "choices");
				String answer = text(raw, "answer");
				if (isBlank(answer)) {
					answer = en;
				}
				if (options == null || options.size() < 2) {
					// 选项不足则退化为默写，避免无效选择题
					ex.setKind("dictation");
				} else {
					LinkedHashSet<String> merged = new LinkedHashSet<>(options);
					merged.add(answer);
					ex.setOptions(new ArrayList<>(merged));
					ex.setAnswer(answer);
					ex.setEn(answer);
				}
			}

			ex.setSentenceId(matchSentenceId(sentences, ex.getEn()));
			result.add(ex);
		}
		return capPerSource(result);
	}

	/**
	 * 限制同一来源句子最多保留 2 道题（已匹配来源按 sentenceId，未匹配按归一化英文），
	 * 避免对同一句反复出题。
	 */
	private List<EnglishSentenceExercise> capPerSource(List<EnglishSentenceExercise> list) {
		Map<String, Integer> used = new HashMap<>();
		List<EnglishSentenceExercise> filtered = new ArrayList<>();
		for (EnglishSentenceExercise ex : list) {
			String key = ex.getSentenceId() != null
					? "id:" + ex.getSentenceId()
					: "en:" + normalizeKey(ex.getEn() == null ? "" : ex.getEn());
			int count = used.getOrDefault(key, 0);
			if (count >= 2) {
				continue;
			}
			used.put(key, count + 1);
			filtered.add(ex);
		}
		return filtered;
	}

	/**
	 * 依据英文句子匹配来源句子 ID（忽略大小写与首尾空白与结尾标点）。
	 */
	private String matchSentenceId(List<EnglishSentence> sentences, String en) {
		if (isBlank(en)) {
			return null;
		}
		String target = normalizeKey(en);
		for (EnglishSentence s : sentences) {
			if (s.getEn() != null && normalizeKey(s.getEn()).equals(target)) {
				return s.getId();
			}
		}
		return null;
	}

	private static String normalizeKey(String s) {
		return s.trim().toLowerCase(Locale.ROOT).replaceAll("[.!?。！？\\s]+$", "");
	}

	// ------------------------------------------------------------------ 保存 / 查询

	@Override
	public EnglishSentencePackView savePack(EnglishSentencePackView pack) {
		if (pack == null) {
			throw new ValidationException("请先生成内容再保存");
		}
		requireNotBlank(pack.getVersion(), "教材版本");
		requireNotBlank(pack.getGrade(), "年级");
		requireNotBlank(pack.getTerm(), "册别");
		requireNotBlank(pack.getUnit(), "单元");
		if (isBlank(pack.getContent())) {
			throw new ValidationException("请先生成内容再保存");
		}

		EnglishSentencePack entity = packMapper.selectOne(Wrappers.<EnglishSentencePack>lambdaQuery()
				.eq(EnglishSentencePack::getVersion, pack.getVersion())
				.eq(EnglishSentencePack::getGrade, pack.getGrade())
				.eq(EnglishSentencePack::getTerm, pack.getTerm())
				.eq(EnglishSentencePack::getUnit, pack.getUnit())
				.last("limit 1"));
		boolean update = entity != null;
		if (entity == null) {
			entity = new EnglishSentencePack();
		}
		entity.setVersion(pack.getVersion());
		entity.setGrade(pack.getGrade());
		entity.setTerm(pack.getTerm());
		entity.setUnit(pack.getUnit());
		entity.setContent(pack.getContent());
		entity.setCount(countExercises(pack.getContent()));

		if (update) {
			packMapper.updateById(entity);
		} else {
			packMapper.insert(entity);
		}
		return toView(packMapper.selectById(entity.getId()));
	}

	@Override
	public PaginationResponse<EnglishSentencePackView> listPacks(EnglishSentencePackQuery query) {
		LambdaQueryWrapper<EnglishSentencePack> wrapper = Wrappers.<EnglishSentencePack>lambdaQuery()
				.eq(notBlank(query.getVersion()), EnglishSentencePack::getVersion, query.getVersion())
				.eq(notBlank(query.getGrade()), EnglishSentencePack::getGrade, query.getGrade())
				.eq(notBlank(query.getTerm()), EnglishSentencePack::getTerm, query.getTerm())
				.eq(notBlank(query.getUnit()), EnglishSentencePack::getUnit, query.getUnit())
				.orderByDesc(EnglishSentencePack::getCreateAt);

		Page<EnglishSentencePack> page = new Page<>(query.getCurrent(), query.getPageSize());
		Page<EnglishSentencePack> result = packMapper.selectPage(page, wrapper);

		List<EnglishSentencePackView> views = result.getRecords().stream().map(pack -> {
			EnglishSentencePackView view = toView(pack);
			// 列表不下发正文，控制传输体积
			view.setContent(null);
			return view;
		}).collect(Collectors.toList());

		return new PaginationResponse<>(result.getTotal(), views);
	}

	@Override
	public EnglishSentencePackView getPack(String id) {
		EnglishSentencePack pack = packMapper.selectById(id);
		if (pack == null) {
			throw new ValidationException("句子练习内容包不存在或已被删除");
		}
		return toView(pack);
	}

	@Override
	public void deletePack(String id) {
		packMapper.deleteById(id);
	}

	@Override
	public List<String> listUnits(String version, String grade, String term) {
		requireNotBlank(version, "教材版本");
		requireNotBlank(grade, "年级");
		requireNotBlank(term, "册别");

		Set<String> units = new LinkedHashSet<>();
		List<EnglishSentence> sentences = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.select(EnglishSentence::getUnit)
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term));
		for (EnglishSentence s : sentences) {
			if (notBlank(s.getUnit())) {
				units.add(s.getUnit().trim());
			}
		}
		return new ArrayList<>(units);
	}

	// ------------------------------------------------------------------ 学员端只读

	@Override
	public List<EnglishSentenceExercise> getExercises(String version, String grade, String term, String unit) {
		EnglishSentencePack pack = findLatest(version, grade, term, unit);
		if (pack == null || isBlank(pack.getContent())) {
			return new ArrayList<>();
		}
		try {
			JsonNode arr = objectMapper.readTree(pack.getContent());
			List<EnglishSentenceExercise> result = new ArrayList<>();
			if (arr.isArray()) {
				for (JsonNode node : arr) {
					result.add(objectMapper.treeToValue(node, EnglishSentenceExercise.class));
				}
			}
			return result;
		} catch (Exception e) {
			log.error("句子练习内容包解析失败：{}", e.getMessage());
			return new ArrayList<>();
		}
	}

	// ------------------------------------------------------------------ 辅助

	private EnglishSentencePack findLatest(String version, String grade, String term, String unit) {
		if (isBlank(version) || isBlank(grade) || isBlank(unit)) {
			return null;
		}
		return packMapper.selectOne(Wrappers.<EnglishSentencePack>lambdaQuery()
				.eq(EnglishSentencePack::getVersion, version)
				.eq(EnglishSentencePack::getGrade, grade)
				.eq(EnglishSentencePack::getUnit, unit)
				.eq(notBlank(term), EnglishSentencePack::getTerm, term)
				.orderByDesc(EnglishSentencePack::getCreateAt)
				.last("limit 1"));
	}

	private int countExercises(String content) {
		if (isBlank(content)) {
			return 0;
		}
		try {
			JsonNode node = objectMapper.readTree(content);
			return node.isArray() ? node.size() : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	private String writeExercises(List<EnglishSentenceExercise> list) {
		try {
			return objectMapper.writeValueAsString(list);
		} catch (Exception e) {
			return "[]";
		}
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private static <T> List<List<T>> partition(List<T> list, int size) {
		List<List<T>> result = new ArrayList<>();
		if (list == null || list.isEmpty()) {
			return result;
		}
		for (int i = 0; i < list.size(); i += size) {
			result.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + size))));
		}
		return result;
	}

	private static Map<String, String> message(String role, String content) {
		Map<String, String> map = new HashMap<>();
		map.put("role", role);
		map.put("content", content);
		return map;
	}

	private static JsonNode firstNode(JsonNode root, String... keys) {
		for (String key : keys) {
			JsonNode node = root.path(key);
			if (node.isArray()) {
				return node;
			}
		}
		return null;
	}

	private static String text(JsonNode node, String... keys) {
		for (String key : keys) {
			JsonNode value = node.path(key);
			if (value.isTextual() && !value.asText().trim().isEmpty()) {
				return value.asText().trim();
			}
		}
		return null;
	}

	private static List<String> readStringArray(JsonNode node, String... keys) {
		for (String key : keys) {
			JsonNode value = node.path(key);
			if (value.isArray()) {
				List<String> list = new ArrayList<>();
				for (JsonNode item : value) {
					if (item.isTextual() && !item.asText().trim().isEmpty()) {
						list.add(item.asText().trim());
					}
				}
				return list;
			}
		}
		return null;
	}

	private EnglishSentencePackView toView(EnglishSentencePack pack) {
		EnglishSentencePackView view = new EnglishSentencePackView();
		view.setId(pack.getId());
		view.setVersion(pack.getVersion());
		view.setGrade(pack.getGrade());
		view.setTerm(pack.getTerm());
		view.setUnit(pack.getUnit());
		view.setContent(pack.getContent());
		view.setCount(pack.getCount());
		view.setCreateAt(pack.getCreateAt());
		return view;
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static boolean notBlank(String s) {
		return !isBlank(s);
	}

	private static void requireNotBlank(String value, String name) {
		if (isBlank(value)) {
			throw new ValidationException(name + "不能为空");
		}
	}

}
