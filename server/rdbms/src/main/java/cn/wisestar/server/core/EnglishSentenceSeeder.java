package cn.wisestar.server.core;

import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
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
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 人教版初中英语句库扩充初始化器。
 *
 * <p>历史种子每单元只有 3~14 句重点句，句式练习可练内容偏少。这里读取
 * {@code english/unit-sentences.json}，按「版本+年级+册别+单元+英文」幂等补齐
 * 教材原文与阅读理解中的完整句子；已存在的句子只回填缺失的中文释义，
 * 不覆盖管理端维护的内容。</p>
 *
 * @author wisestar
 * @date 2026/11/22
 */
@Slf4j
@Component
@Order(6)
@RequiredArgsConstructor
public class EnglishSentenceSeeder implements ApplicationRunner {

	private final EnglishSentenceMapper sentenceMapper;

	private final ObjectMapper objectMapper;

	@Override
	public void run(ApplicationArguments args) {
		try {
			List<JsonNode> items = loadItems();
			if (items.isEmpty()) {
				return;
			}
			Map<String, EnglishSentence> existing = sentenceMapper
					.selectList(Wrappers.<EnglishSentence>lambdaQuery()
							.eq(EnglishSentence::getVersion, "人教版"))
					.stream()
					.collect(Collectors.toMap(this::key, Function.identity(), (a, b) -> a,
							HashMap::new));
			int inserted = 0;
			int updated = 0;
			for (JsonNode item : items) {
				String key = key(item.path("version").asText(), item.path("grade").asText(),
						item.path("term").asText(), item.path("unit").asText(),
						item.path("en").asText());
				EnglishSentence exist = existing.get(key);
				if (exist == null) {
					EnglishSentence entity = new EnglishSentence();
					entity.setId(item.path("id").asText());
					entity.setEn(item.path("en").asText());
					entity.setZh(item.path("zh").asText(null));
					entity.setVersion(item.path("version").asText());
					entity.setGrade(item.path("grade").asText());
					entity.setTerm(item.path("term").asText());
					entity.setUnit(item.path("unit").asText());
					entity.setSort(item.path("sort").asInt(0));
					sentenceMapper.insert(entity);
					existing.put(key, entity);
					inserted++;
				}
				else if (!StringUtils.hasText(exist.getZh()) && StringUtils.hasText(item.path("zh").asText())) {
					EnglishSentence update = new EnglishSentence();
					update.setId(exist.getId());
					update.setZh(item.path("zh").asText());
					sentenceMapper.updateById(update);
					updated++;
				}
			}
			if (inserted > 0 || updated > 0) {
				log.info("人教版初中英语句库扩充完成：新增 {} 句，补中文 {} 句", inserted, updated);
			}
		}
		catch (Exception e) {
			log.warn("人教版初中英语句库扩充失败：{}", e.getMessage());
		}
	}

	private List<JsonNode> loadItems() throws Exception {
		try (InputStream in = new ClassPathResource("english/unit-sentences.json").getInputStream()) {
			JsonNode root = objectMapper.readTree(in);
			List<JsonNode> result = new java.util.ArrayList<>();
			if (root != null && root.isArray()) {
				root.forEach(result::add);
			}
			return result;
		}
	}

	private String key(EnglishSentence sentence) {
		return key(sentence.getVersion(), sentence.getGrade(), sentence.getTerm(), sentence.getUnit(),
				sentence.getEn());
	}

	private String key(String version, String grade, String term, String unit, String en) {
		return version + "|" + grade + "|" + term + "|" + unit + "|"
				+ (en == null ? "" : en.trim().toLowerCase());
	}

}
