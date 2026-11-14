package cn.wisestar.server.core;

import cn.wisestar.server.domain.model.EnglishScenarioPack;
import cn.wisestar.server.mapper.EnglishScenarioPackMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 英语内容包数据修正初始化器。
 *
 * <p>启动时按 {@code english/pack-fixes.json} 补齐情景行的中文与挖空答案，并按业务键
 * （版本+年级+册别+单元）收敛重复内容包：保留内容量最大的一条，其余逻辑删除。</p>
 *
 * @author wisestar
 * @date 2026/10/19
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class EnglishPackDataFixSeeder implements ApplicationRunner {

	private final EnglishScenarioPackMapper packMapper;

	private final ObjectMapper objectMapper;

	@Override
	public void run(ApplicationArguments args) {
		try {
			List<EnglishScenarioPack> packs = packMapper.selectList(null);
			int fixed = applyFixes(packs);
			int removed = dedupe(packs);
			int sanitized = sanitizeWordContent(packs);
			int trimmed = sanitizePatternContent(packs);
			if (fixed > 0 || removed > 0 || sanitized > 0 || trimmed > 0) {
				log.info("英语内容包数据修正完成：补齐 {} 行，收敛重复包 {} 个，规整情景 {} 个，收敛句式选项 {} 个",
						fixed, removed, sanitized, trimmed);
			}
		}
		catch (Exception e) {
			log.warn("英语内容包数据修正失败：{}", e.getMessage());
		}
	}

	/** 去掉「情景标题：」「情景标题:」前缀与首尾空白。 */
	private String stripTitlePrefix(String title) {
		String value = title == null ? "" : title.trim();
		for (String prefix : new String[] { "情景标题：", "情景标题:", "情景：" }) {
			if (value.startsWith(prefix)) {
				value = value.substring(prefix.length()).trim();
			}
		}
		return value;
	}

	private boolean containsIgnoreCase(ArrayNode words, String value) {
		for (JsonNode word : words) {
			if (word.asText("").trim().equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 规整单词情景正文：去掉「情景标题：」这类拼接残留，并把挖空答案补进本段备选单词库。
	 *
	 * <p>学员端的单词情景会把 {@code scenario.words} 当作题干下方的备选词库展示，
	 * 历史内容里存在答案词形（如 {@code shared}、{@code september}）不在词库中的情况，
	 * 学员照着词库找不到答案。这里按空位答案补词，已存在时按大小写不敏感跳过。</p>
	 */
	private int sanitizeWordContent(List<EnglishScenarioPack> packs) throws Exception {
		int sanitized = 0;
		for (EnglishScenarioPack pack : packs) {
			if (!StringUtils.hasText(pack.getWordContent())) {
				continue;
			}
			ObjectNode root;
			try {
				root = (ObjectNode) objectMapper.readTree(pack.getWordContent());
			}
			catch (Exception e) {
				continue;
			}
			boolean changed = false;
			for (JsonNode group : root.withArray("groups")) {
				for (JsonNode scenario : group.path("scenarios")) {
					ObjectNode node = (ObjectNode) scenario;
					String title = node.path("title").asText("");
					String cleaned = stripTitlePrefix(title);
					if (!cleaned.equals(title)) {
						node.put("title", cleaned);
						changed = true;
					}
					ArrayNode words = node.withArray("words");
					for (JsonNode line : node.path("lines")) {
						for (JsonNode answer : line.path("answers")) {
							String value = answer.asText("").trim();
							if (value.isEmpty() || containsIgnoreCase(words, value)) {
								continue;
							}
							words.add(value);
							changed = true;
						}
					}
				}
			}
			if (changed) {
				pack.setWordContent(objectMapper.writeValueAsString(root));
				packMapper.updateById(pack);
				sanitized++;
			}
		}
		return sanitized;
	}

	/**
	 * 规整句式情景正文：把作答轮候选收敛到恰好 4 个（需求口径），并保证正确项不丢。
	 *
	 * <p>历史 AI 内容里存在 5 个候选的作答轮，选项一多更容易用排除法蒙对，也和
	 * 「恰好 4 个选项」的口径不一致。这里保留正确项与前 3 个干扰项，正确项放到首位，
	 * 学员端读取时仍会按稳定旋转打乱顺序。</p>
	 */
	private int sanitizePatternContent(List<EnglishScenarioPack> packs) throws Exception {
		int sanitized = 0;
		for (EnglishScenarioPack pack : packs) {
			if (!StringUtils.hasText(pack.getPatternContent())) {
				continue;
			}
			ObjectNode root;
			try {
				root = (ObjectNode) objectMapper.readTree(pack.getPatternContent());
			}
			catch (Exception e) {
				continue;
			}
			boolean changed = false;
			for (JsonNode group : root.withArray("groups")) {
				for (JsonNode scenario : group.path("scenarios")) {
					for (JsonNode turn : scenario.path("turns")) {
						JsonNode options = turn.path("options");
						int answerIndex = turn.path("answerIndex").asInt(-1);
						if (!options.isArray() || options.size() <= 4 || answerIndex < 0
								|| answerIndex >= options.size()) {
							continue;
						}
						String answer = options.get(answerIndex).asText("").trim();
						if (answer.isEmpty()) {
							continue;
						}
						List<String> trimmed = new ArrayList<>();
						trimmed.add(answer);
						for (JsonNode option : options) {
							if (trimmed.size() >= 4) {
								break;
							}
							String value = option.asText("").trim();
							if (!value.isEmpty() && !trimmed.contains(value)) {
								trimmed.add(value);
							}
						}
						if (trimmed.size() != 4) {
							continue;
						}
						((ObjectNode) turn).put("options", objectMapper.valueToTree(trimmed));
						((ObjectNode) turn).put("answerIndex", 0);
						changed = true;
					}
				}
			}
			if (changed) {
				pack.setPatternContent(objectMapper.writeValueAsString(root));
				packMapper.updateById(pack);
				sanitized++;
			}
		}
		return sanitized;
	}

	private int applyFixes(List<EnglishScenarioPack> packs) throws Exception {
		JsonNode fixes;
		try (InputStream in = new ClassPathResource("english/pack-fixes.json").getInputStream()) {
			fixes = objectMapper.readTree(in);
		}
		if (fixes == null || !fixes.isArray() || fixes.size() == 0) {
			return 0;
		}
		int fixed = 0;
		for (EnglishScenarioPack pack : packs) {
			boolean changed = false;
			for (JsonNode fix : fixes) {
				if (!same(pack.getGrade(), fix.path("grade").asText())
						|| !same(pack.getTerm(), fix.path("term").asText())
						|| !same(pack.getUnit(), fix.path("unit").asText())) {
					continue;
				}
				changed |= applyFixToContent(pack, fix);
			}
			if (changed) {
				packMapper.updateById(pack);
				fixed++;
			}
		}
		return fixed;
	}

	/**
	 * 把一条修正同时作用到单词情景与句式情景：命中 {@code en} 的台词整体改写成 {@code newEn}，
	 * 并同步中文与空位答案；句式情景的候选选项里若存在同样的呼语残留也一并替换。
	 */
	private boolean applyFixToContent(EnglishScenarioPack pack, JsonNode fix) throws Exception {
		String en = fix.path("en").asText("");
		if (en.isEmpty()) {
			return false;
		}
		String newEn = fix.path("newEn").asText(en);
		boolean changed = false;
		for (String content : new String[] { pack.getWordContent(), pack.getPatternContent() }) {
			if (!StringUtils.hasText(content)) {
				continue;
			}
			JsonNode parsed;
			try {
				parsed = objectMapper.readTree(content);
			}
			catch (Exception e) {
				continue;
			}
			if (!(parsed instanceof ObjectNode)) {
				continue;
			}
			ObjectNode root = (ObjectNode) parsed;
			if (rewriteNode(root, en, newEn, fix)) {
				String updated = objectMapper.writeValueAsString(root);
				if (same(content, pack.getWordContent())) {
					pack.setWordContent(updated);
				}
				else {
					pack.setPatternContent(updated);
				}
				changed = true;
			}
		}
		return changed;
	}

	/** 遍历分组内的情景，替换命中的台词文本、中文与答案。 */
	private boolean rewriteNode(ObjectNode root, String en, String newEn, JsonNode fix) {
		boolean changed = false;
		for (JsonNode group : root.path("groups")) {
			changed |= rewriteScenarios(group.path("scenarios"), en, newEn, fix);
		}
		return changed;
	}

	private boolean rewriteScenarios(JsonNode scenarios, String en, String newEn, JsonNode fix) {
		boolean changed = false;
		for (JsonNode scenario : scenarios) {
			if (!(scenario instanceof ObjectNode)) {
				continue;
			}
			ObjectNode scenarioNode = (ObjectNode) scenario;
			for (JsonNode line : scenarioNode.path("lines")) {
				if (!(line instanceof ObjectNode)) {
					continue;
				}
				ObjectNode lineNode = (ObjectNode) line;
				if (!same(lineNode.path("en").asText(), en)) {
					continue;
				}
				if (!same(lineNode.path("en").asText(), newEn)) {
					lineNode.put("en", newEn);
					changed = true;
				}
				if (fix.hasNonNull("zh") && !same(lineNode.path("zh").asText(), fix.path("zh").asText())) {
					lineNode.put("zh", fix.path("zh").asText());
					changed = true;
				}
				if (fix.has("answers")) {
					ArrayNode answers = objectMapper.createArrayNode();
					for (JsonNode answer : fix.path("answers")) {
						answers.add(answer.asText());
					}
					if (!answers.equals(lineNode.path("answers"))) {
						lineNode.set("answers", answers);
						changed = true;
					}
				}
			}
			for (JsonNode turn : scenarioNode.path("turns")) {
				JsonNode options = turn.path("options");
				if (!options.isArray()) {
					continue;
				}
				for (int i = 0; i < options.size(); i++) {
					if (same(options.get(i).asText(""), en) && !same(newEn, en)) {
						((ArrayNode) options).set(i, objectMapper.getNodeFactory().textNode(newEn));
						changed = true;
					}
				}
			}
		}
		return changed;
	}

	private int dedupe(List<EnglishScenarioPack> packs) {
		Map<String, List<EnglishScenarioPack>> groups = new LinkedHashMap<>();
		for (EnglishScenarioPack pack : packs) {
			String key = pack.getVersion() + "|" + pack.getGrade() + "|" + pack.getTerm() + "|" + pack.getUnit();
			groups.computeIfAbsent(key, k -> new ArrayList<>()).add(pack);
		}
		int removed = 0;
		for (List<EnglishScenarioPack> group : groups.values()) {
			if (group.size() < 2) {
				continue;
			}
			group.sort(Comparator
					.comparingInt((EnglishScenarioPack pack) -> weight(pack))
					.thenComparing(EnglishScenarioPack::getId)
					.reversed());
			for (int i = 1; i < group.size(); i++) {
				packMapper.deleteById(group.get(i).getId());
				removed++;
			}
		}
		return removed;
	}

	private int weight(EnglishScenarioPack pack) {
		return (pack.getWordCount() == null ? 0 : pack.getWordCount())
				+ (pack.getPatternCount() == null ? 0 : pack.getPatternCount());
	}

	private boolean same(String left, String right) {
		return left != null && right != null && left.trim().equals(right.trim());
	}

}
