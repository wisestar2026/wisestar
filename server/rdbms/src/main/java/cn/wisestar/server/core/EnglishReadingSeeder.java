package cn.wisestar.server.core;

import cn.wisestar.server.domain.model.EnglishScenarioPack;
import cn.wisestar.server.mapper.EnglishScenarioPackMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 英语阅读理解内容初始化器。
 *
 * <p>启动时把 {@code english/reading-articles.json} 按单元写入情景内容包的
 * {@code reading_content}，格式为 {@code {"articles":[...]}}；同单元多篇文章按 articleNo 排序。
 * 已与资源一致时跳过，避免每次启动重复写库。</p>
 *
 * @author wisestar
 * @date 2026/10/19
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class EnglishReadingSeeder implements ApplicationRunner {

	private final EnglishScenarioPackMapper packMapper;

	private final ObjectMapper objectMapper;

	@Override
	public void run(ApplicationArguments args) {
		try {
			List<JsonNode> articles = loadArticles();
			if (articles.isEmpty()) {
				return;
			}
			Map<String, List<JsonNode>> groups = new LinkedHashMap<>();
			for (JsonNode article : articles) {
				String key = key(article.path("version").asText(), article.path("grade").asText(),
						article.path("term").asText(), article.path("unit").asText());
				groups.computeIfAbsent(key, k -> new ArrayList<>()).add(article);
			}
			int updated = 0;
			for (Map.Entry<String, List<JsonNode>> entry : groups.entrySet()) {
				List<JsonNode> group = entry.getValue();
				group.sort(Comparator.comparingInt(node -> node.path("articleNo").asInt()));
				JsonNode first = group.get(0);
				ObjectNode wrapper = objectMapper.createObjectNode();
				ArrayNode array = wrapper.putArray("articles");
				for (JsonNode article : group) {
					array.add(article);
				}
				String json = objectMapper.writeValueAsString(wrapper);
				EnglishScenarioPack pack = packMapper.selectOne(Wrappers.<EnglishScenarioPack>lambdaQuery()
						.eq(EnglishScenarioPack::getVersion, first.path("version").asText())
						.eq(EnglishScenarioPack::getGrade, first.path("grade").asText())
						.eq(EnglishScenarioPack::getTerm, first.path("term").asText())
						.eq(EnglishScenarioPack::getUnit, first.path("unit").asText())
						.last("limit 1"));
				if (pack == null) {
					pack = new EnglishScenarioPack();
					pack.setVersion(first.path("version").asText());
					pack.setGrade(first.path("grade").asText());
					pack.setTerm(first.path("term").asText());
					pack.setUnit(first.path("unit").asText());
					pack.setWordCount(0);
					pack.setPatternCount(0);
					pack.setReadingContent(json);
					packMapper.insert(pack);
					updated++;
				}
				else if (!json.equals(pack.getReadingContent())) {
					pack.setReadingContent(json);
					packMapper.updateById(pack);
					updated++;
				}
			}
			if (updated > 0) {
				log.info("英语阅读理解内容初始化完成：更新 {} 个单元", updated);
			}
		}
		catch (Exception e) {
			log.warn("英语阅读理解内容初始化失败：{}", e.getMessage());
		}
	}

	private List<JsonNode> loadArticles() throws Exception {
		List<JsonNode> result = new ArrayList<>();
		try (InputStream in = new ClassPathResource("english/reading-articles.json").getInputStream()) {
			JsonNode root = objectMapper.readTree(in);
			if (root != null && root.isArray()) {
				for (JsonNode node : root) {
					result.add(node);
				}
			}
		}
		return result;
	}

	private String key(String version, String grade, String term, String unit) {
		return version + "|" + grade + "|" + term + "|" + unit;
	}

}
