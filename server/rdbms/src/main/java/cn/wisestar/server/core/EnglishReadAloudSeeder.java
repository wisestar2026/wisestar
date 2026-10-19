package cn.wisestar.server.core;

import cn.wisestar.server.domain.model.EnglishReadAloud;
import cn.wisestar.server.mapper.EnglishReadAloudMapper;
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
import java.util.HashMap;
import java.util.Map;

/**
 * 英语课文跟读内容初始化器。
 *
 * <p>启动时把 {@code english/read-aloud.json} 按「版本+年级+册别+单元+标题」增量写入
 * {@code t_english_read_aloud}；已有篇章按内容比对更新，避免重复插入。</p>
 *
 * @author wisestar
 * @date 2026/10/19
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class EnglishReadAloudSeeder implements ApplicationRunner {

	private final EnglishReadAloudMapper readAloudMapper;

	private final ObjectMapper objectMapper;

	@Override
	public void run(ApplicationArguments args) {
		try {
			JsonNode root;
			try (InputStream in = new ClassPathResource("english/read-aloud.json").getInputStream()) {
				root = objectMapper.readTree(in);
			}
			if (root == null || !root.isArray() || root.size() == 0) {
				return;
			}
			Map<String, EnglishReadAloud> existing = new HashMap<>();
			for (EnglishReadAloud passage : readAloudMapper.selectList(null)) {
				existing.put(key(passage.getVersion(), passage.getGrade(), passage.getTerm(),
						passage.getUnit(), passage.getTitle()), passage);
			}
			int inserted = 0;
			int updated = 0;
			Map<String, Integer> unitSort = new HashMap<>();
			for (JsonNode node : root) {
				String version = node.path("version").asText();
				String grade = node.path("grade").asText();
				String term = node.path("term").asText();
				String unit = node.path("unit").asText();
				String title = node.path("title").asText();
				String unitKey = version + "|" + grade + "|" + term + "|" + unit;
				int sort = unitSort.merge(unitKey, 1, Integer::sum);
				String content = objectMapper.writeValueAsString(node.path("lines"));
				int lineCount = node.path("lines").size();
				EnglishReadAloud passage = existing.get(key(version, grade, term, unit, title));
				if (passage == null) {
					passage = new EnglishReadAloud();
					passage.setVersion(version);
					passage.setGrade(grade);
					passage.setTerm(term);
					passage.setUnit(unit);
					passage.setTitle(title);
					passage.setSource(node.path("source").asText(""));
					passage.setSort(sort);
					passage.setContent(content);
					passage.setLineCount(lineCount);
					readAloudMapper.insert(passage);
					inserted++;
				}
				else if (!content.equals(passage.getContent()) || !Integer.valueOf(lineCount).equals(passage.getLineCount())) {
					passage.setSource(node.path("source").asText(""));
					passage.setSort(sort);
					passage.setContent(content);
					passage.setLineCount(lineCount);
					readAloudMapper.updateById(passage);
					updated++;
				}
			}
			if (inserted > 0 || updated > 0) {
				log.info("英语课文跟读内容初始化完成：新增 {} 篇，更新 {} 篇", inserted, updated);
			}
		}
		catch (Exception e) {
			log.warn("英语课文跟读内容初始化失败：{}", e.getMessage());
		}
	}

	private String key(String version, String grade, String term, String unit, String title) {
		return version + "|" + grade + "|" + term + "|" + unit + "|" + title;
	}

}
