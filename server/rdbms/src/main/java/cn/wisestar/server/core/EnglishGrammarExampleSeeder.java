package cn.wisestar.server.core;

import cn.wisestar.server.domain.model.EnglishGrammar;
import cn.wisestar.server.mapper.EnglishGrammarMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 人教版初中英语语法例句/练习补全器。
 *
 * <p>历史种子给部分语法点写入的 examples 是单词占位（如 Class (AM) / joyful），
 * 无法派生足量语法练习题。这里读取 {@code english/grammar-examples.json}：
 * <ul>
 *   <li>{@code mode=replace}：整组替换 examples / samples / textbook / exercises；</li>
 *   <li>{@code mode=append}：仅追加 examples 中尚未出现的例句，并补入缺失的选择题。</li>
 * </ul>
 * 只操作例句与练习字段，不覆盖管理端维护的讲解（content/structure）。</p>
 *
 * @author wisestar
 * @date 2026/11/24
 */
@Slf4j
@Component
@Order(8)
@RequiredArgsConstructor
public class EnglishGrammarExampleSeeder implements ApplicationRunner {

	private final EnglishGrammarMapper grammarMapper;

	private final ObjectMapper objectMapper;

	@Override
	public void run(ApplicationArguments args) {
		try {
			JsonNode root = loadContent();
			if (root == null || !root.path("rows").isArray()) {
				return;
			}
			int updated = 0;
			for (JsonNode node : root.path("rows")) {
				updated += apply(node);
			}
			if (updated > 0) {
				log.info("英语语法例句补全完成：更新 {} 条", updated);
			}
		}
		catch (Exception e) {
			log.warn("英语语法例句补全失败：{}", e.getMessage());
		}
	}

	private JsonNode loadContent() throws Exception {
		try (InputStream in = new ClassPathResource("english/grammar-examples.json").getInputStream()) {
			return objectMapper.readTree(in);
		}
	}

	private int apply(JsonNode node) throws Exception {
		String id = node.path("id").asText();
		EnglishGrammar entity = grammarMapper.selectById(id);
		if (entity == null) {
			return 0;
		}
		boolean replace = "replace".equalsIgnoreCase(node.path("mode").asText());
		if (replace || isPlaceholderExamples(entity.getExamples())) {
			return replaceAll(entity, node);
		}
		return append(entity, node);
	}

	/** 整组替换例句、学习页例句、课本原句与练习。 */
	private int replaceAll(EnglishGrammar entity, JsonNode node) throws Exception {
		boolean changed = false;
		EnglishGrammar update = new EnglishGrammar();
		update.setId(entity.getId());
		if (node.path("examples").isArray()) {
			update.setExamples(node.path("examples").toString());
			changed = true;
		}
		if (node.path("samples").isArray()) {
			update.setSamples(node.path("samples").toString());
			changed = true;
		}
		if (node.path("textbook").isArray()) {
			update.setTextbook(node.path("textbook").toString());
			changed = true;
		}
		if (node.path("exercises").isArray()) {
			update.setExercises(node.path("exercises").toString());
			changed = true;
		}
		if (!changed) {
			return 0;
		}
		grammarMapper.updateById(update);
		return 1;
	}

	/** 追加缺失例句与练习，已有的同日不再重复写入。 */
	private int append(EnglishGrammar entity, JsonNode node) throws Exception {
		boolean changed = false;
		EnglishGrammar update = new EnglishGrammar();
		update.setId(entity.getId());
		ArrayNode examples = parseArray(entity.getExamples());
		Set<String> exampleKeys = new LinkedHashSet<>();
		for (JsonNode item : examples) {
			exampleKeys.add(normalize(item.path("en").asText(item.asText())));
		}
		int addedExample = 0;
		for (JsonNode item : node.path("examples")) {
			String en = item.isTextual() ? item.asText() : item.path("en").asText();
			if (!StringUtils.hasText(en) || !exampleKeys.add(normalize(en))) {
				continue;
			}
			examples.add(item);
			addedExample++;
		}
		if (addedExample > 0) {
			update.setExamples(examples.toString());
			changed = true;
		}
		ArrayNode exercises = parseArray(entity.getExercises());
		Set<String> exerciseKeys = new LinkedHashSet<>();
		for (JsonNode item : exercises) {
			exerciseKeys.add(normalize(item.path("q").asText()));
		}
		int addedExercise = 0;
		for (JsonNode item : node.path("exercises")) {
			String q = item.path("q").asText();
			if (!StringUtils.hasText(q) || !exerciseKeys.add(normalize(q))) {
				continue;
			}
			exercises.add(item);
			addedExercise++;
		}
		if (addedExercise > 0) {
			update.setExercises(exercises.toString());
			changed = true;
		}
		if (!changed) {
			return 0;
		}
		grammarMapper.updateById(update);
		return 1;
	}

	/** 例句是否仍是占位：没有任何一条达到 4 个及以上单词。 */
	private boolean isPlaceholderExamples(String json) {
		if (!StringUtils.hasText(json)) {
			return true;
		}
		try {
			JsonNode node = objectMapper.readTree(json);
			if (!node.isArray() || node.isEmpty()) {
				return true;
			}
			for (JsonNode item : node) {
				String en = item.isTextual() ? item.asText() : item.path("en").asText();
				if (StringUtils.hasText(en) && en.trim().split("\\s+").length >= 4) {
					return false;
				}
			}
		}
		catch (Exception e) {
			return true;
		}
		return true;
	}

	private ArrayNode parseArray(String json) {
		if (StringUtils.hasText(json)) {
			try {
				JsonNode node = objectMapper.readTree(json);
				if (node.isArray()) {
					return (ArrayNode) node;
				}
			}
			catch (Exception ignored) {
				// 解析失败按空数组处理
			}
		}
		return objectMapper.createArrayNode();
	}

	private String normalize(String text) {
		if (!StringUtils.hasText(text)) {
			return "";
		}
		return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ").trim();
	}

}
