package cn.wisestar.server.core;

import cn.wisestar.server.domain.model.EnglishScenarioPack;
import cn.wisestar.server.mapper.EnglishScenarioPackMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 英语单元情景内容包初始化器（单词情景 + 句式情景）。
 *
 * <p>启动时把 {@code english/scenario-packs.json} 按「版本+年级+册别+单元」增量写入
 * {@code t_english_scenario_pack} 的 {@code word_content} / {@code pattern_content}；
 * 只更新情景两列，保留由 {@link EnglishReadingSeeder} 维护的阅读内容。</p>
 *
 * @author wisestar
 * @date 2026/11/22
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class EnglishScenarioSeeder implements ApplicationRunner {

	private final EnglishScenarioPackMapper packMapper;

	private final ObjectMapper objectMapper;

	@Override
	public void run(ApplicationArguments args) {
		try {
			JsonNode root;
			try (InputStream in = new ClassPathResource("english/scenario-packs.json").getInputStream()) {
				root = objectMapper.readTree(in);
			}
			if (root == null || !root.isArray() || root.size() == 0) {
				return;
			}
			int inserted = 0;
			int updated = 0;
			for (JsonNode node : root) {
				String version = node.path("version").asText();
				String grade = node.path("grade").asText();
				String term = node.path("term").asText();
				String unit = node.path("unit").asText();
				String wordContent = node.path("wordContent").asText("");
				String patternContent = node.path("patternContent").asText("");
				int wordCount = node.path("wordCount").asInt(0);
				int patternCount = node.path("patternCount").asInt(0);
				if (version.isEmpty() || grade.isEmpty() || unit.isEmpty()) {
					continue;
				}
				EnglishScenarioPack pack = packMapper.selectOne(Wrappers.<EnglishScenarioPack>lambdaQuery()
						.eq(EnglishScenarioPack::getVersion, version)
						.eq(EnglishScenarioPack::getGrade, grade)
						.eq(EnglishScenarioPack::getTerm, term)
						.eq(EnglishScenarioPack::getUnit, unit)
						.last("limit 1"));
				if (pack == null) {
					pack = new EnglishScenarioPack();
					pack.setVersion(version);
					pack.setGrade(grade);
					pack.setTerm(term);
					pack.setUnit(unit);
					pack.setWordContent(wordContent);
					pack.setWordCount(wordCount);
					pack.setPatternContent(patternContent);
					pack.setPatternCount(patternCount);
					packMapper.insert(pack);
					inserted++;
				}
				else if (!wordContent.equals(pack.getWordContent()) || !patternContent.equals(pack.getPatternContent())) {
					pack.setWordContent(wordContent);
					pack.setWordCount(wordCount);
					pack.setPatternContent(patternContent);
					pack.setPatternCount(patternCount);
					packMapper.updateById(pack);
					updated++;
				}
			}
			if (inserted > 0 || updated > 0) {
				log.info("英语情景内容初始化完成：新增 {} 个单元，更新 {} 个单元", inserted, updated);
			}
		}
		catch (Exception e) {
			log.warn("英语情景内容初始化失败：{}", e.getMessage());
		}
	}

}
