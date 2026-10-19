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
			if (fixed > 0 || removed > 0) {
				log.info("英语内容包数据修正完成：补齐 {} 行，收敛重复包 {} 个", fixed, removed);
			}
		}
		catch (Exception e) {
			log.warn("英语内容包数据修正失败：{}", e.getMessage());
		}
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
			ArrayNode groups = root.withArray("groups");
			for (JsonNode fix : fixes) {
				if (!same(pack.getGrade(), fix.path("grade").asText())
						|| !same(pack.getTerm(), fix.path("term").asText())
						|| !same(pack.getUnit(), fix.path("unit").asText())) {
					continue;
				}
				String en = fix.path("en").asText("");
				for (JsonNode group : groups) {
					JsonNode scenarios = group.path("scenarios");
					for (JsonNode scenario : scenarios) {
						JsonNode lines = scenario.path("lines");
						for (JsonNode line : lines) {
							if (!same(line.path("en").asText(), en)) {
								continue;
							}
							ObjectNode target = (ObjectNode) line;
							if (fix.hasNonNull("zh") && !same(target.path("zh").asText(), fix.path("zh").asText())) {
								target.put("zh", fix.path("zh").asText());
								changed = true;
							}
							if (fix.has("answers")) {
								ArrayNode answers = objectMapper.createArrayNode();
								for (JsonNode answer : fix.path("answers")) {
									answers.add(answer.asText());
								}
								target.set("answers", answers);
								changed = true;
							}
						}
					}
				}
			}
			if (changed) {
				pack.setWordContent(objectMapper.writeValueAsString(root));
				packMapper.updateById(pack);
				fixed++;
			}
		}
		return fixed;
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
