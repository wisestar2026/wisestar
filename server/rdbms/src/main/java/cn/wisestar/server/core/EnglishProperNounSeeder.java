package cn.wisestar.server.core;

import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishWordMapper;
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
 * 人教版初中英语专有名词补充初始化器。
 *
 * <p>正文词表只收录普通词汇，教材附录中的人名、地名、国家名等专有名词没有进库，
 * 导致阅读与听力文本里的专有名词无法查询。这里读取 {@code english/proper-nouns.json}，
 * 按「版本+年级+册别+单元+拼写」补入缺失的专有名词，已存在的词条保持原样。</p>
 *
 * @author wisestar
 * @date 2026/11/23
 */
@Slf4j
@Component
@Order(7)
@RequiredArgsConstructor
public class EnglishProperNounSeeder implements ApplicationRunner {

	private final EnglishWordMapper wordMapper;

	private final ObjectMapper objectMapper;

	@Override
	public void run(ApplicationArguments args) {
		try {
			JsonNode root = loadItems();
			if (root == null || !root.isArray()) {
				return;
			}
			int inserted = 0;
			for (JsonNode node : root) {
				String version = node.path("version").asText();
				String grade = node.path("grade").asText();
				String term = node.path("term").asText();
				String unit = node.path("unit").asText();
				String spell = node.path("spell").asText();
				Long count = wordMapper.selectCount(Wrappers.<EnglishWord>lambdaQuery()
						.eq(EnglishWord::getVersion, version)
						.eq(EnglishWord::getGrade, grade)
						.eq(EnglishWord::getTerm, term)
						.eq(EnglishWord::getUnit, unit)
						.eq(EnglishWord::getSpell, spell));
				if (count != null && count > 0) {
					continue;
				}
				EnglishWord entity = new EnglishWord();
				entity.setId(node.path("id").asText());
				entity.setSpell(spell);
				entity.setPhonetic(node.path("phonetic").asText(null));
				entity.setMeaning(node.path("meaning").asText(null));
				entity.setVersion(version);
				entity.setGrade(grade);
				entity.setTerm(term);
				entity.setUnit(unit);
				wordMapper.insert(entity);
				inserted++;
			}
			if (inserted > 0) {
				log.info("人教版初中英语专有名词补充完成：新增 {} 条", inserted);
			}
		}
		catch (Exception e) {
			log.warn("人教版初中英语专有名词补充失败：{}", e.getMessage());
		}
	}

	private JsonNode loadItems() throws Exception {
		try (InputStream in = new ClassPathResource("english/proper-nouns.json").getInputStream()) {
			return objectMapper.readTree(in);
		}
	}

}
