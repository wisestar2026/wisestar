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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 人教版初中英语句库扩充初始化器。
 *
 * <p>历史种子每单元只有 3~14 句重点句，且混有表格碎片与杜撰段落。这里读取
 * {@code english/unit-sentences.json}，按「版本+年级+册别+单元」以教材整理稿为准：
 * 清单内单元新增缺失句子、刷新中文释义与排序，并清理不在清单内的旧句；
 * 未出现在清单中的单元保持原样。</p>
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
			Map<String, JsonNode> curated = new HashMap<>();
			Set<String> curatedUnits = new HashSet<>();
			for (JsonNode item : items) {
				curated.put(key(item.path("version").asText(), item.path("grade").asText(),
						item.path("term").asText(), item.path("unit").asText(), item.path("en").asText()), item);
				curatedUnits.add(unitKey(item.path("version").asText(), item.path("grade").asText(),
						item.path("term").asText(), item.path("unit").asText()));
			}
			List<EnglishSentence> rows = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
					.eq(EnglishSentence::getVersion, "人教版"));
			// 历史残留行可能占用了清单里的 id（旧实现中途失败留下的半截数据），
			// 插入前统一收集已占用 id，碰到冲突就换一个后缀，保证幂等可重跑
			Set<String> usedIds = new HashSet<>();
			for (EnglishSentence row : sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
					.select(EnglishSentence::getId))) {
				usedIds.add(row.getId());
			}
			Map<String, EnglishSentence> existing = new HashMap<>();
			List<String> removed = new ArrayList<>();
			for (EnglishSentence row : rows) {
				String key = key(row);
				// 由教材整理的单元以 JSON 为准：清掉旧种子中不在清单内的表格碎片/杜撰句
				if (curatedUnits.contains(unitKey(row.getVersion(), row.getGrade(), row.getTerm(), row.getUnit()))
						&& !curated.containsKey(key)) {
					removed.add(row.getId());
					continue;
				}
				existing.put(key, row);
			}
			if (!removed.isEmpty()) {
				sentenceMapper.deleteBatchIds(removed);
				usedIds.removeAll(removed);
			}
			int inserted = 0;
			int updated = 0;
			for (JsonNode item : items) {
				String key = key(item.path("version").asText(), item.path("grade").asText(),
						item.path("term").asText(), item.path("unit").asText(),
						item.path("en").asText());
				EnglishSentence exist = existing.get(key);
				if (exist == null) {
					EnglishSentence entity = new EnglishSentence();
					entity.setEn(item.path("en").asText());
					entity.setZh(item.path("zh").asText(null));
					entity.setVersion(item.path("version").asText());
					entity.setGrade(item.path("grade").asText());
					entity.setTerm(item.path("term").asText());
					entity.setUnit(item.path("unit").asText());
					entity.setSort(item.path("sort").asInt(0));
					insertWithFreeId(sentenceMapper, entity, item.path("id").asText(), usedIds);
					existing.put(key, entity);
					inserted++;
				}
				else {
					String zh = item.path("zh").asText("");
					int sort = item.path("sort").asInt(0);
					boolean zhChanged = StringUtils.hasText(zh) && !zh.equals(exist.getZh());
					boolean sortChanged = exist.getSort() == null || exist.getSort() != sort;
					if (zhChanged || sortChanged) {
						EnglishSentence update = new EnglishSentence();
						update.setId(exist.getId());
						if (zhChanged) {
							update.setZh(zh);
						}
						if (sortChanged) {
							update.setSort(sort);
						}
						sentenceMapper.updateById(update);
						updated++;
					}
				}
			}
			if (inserted > 0 || updated > 0 || !removed.isEmpty()) {
				log.info("人教版初中英语句库同步完成：新增 {} 句，更新 {} 句，清理 {} 句", inserted, updated, removed.size());
			}
		}
		catch (Exception e) {
			log.warn("人教版初中英语句库同步失败：{}", e.getMessage());
		}
	}

	/**
	 * 插入句子，遇到主键冲突（旧实现中途失败残留的同 id 行，可能已被逻辑删除而查询不可见）
	 * 时换后缀重试，保证 seed 幂等可重跑。
	 */
	private void insertWithFreeId(EnglishSentenceMapper mapper, EnglishSentence entity, String baseId,
			Set<String> usedIds) {
		String id = baseId;
		for (int suffix = 0; ; suffix++) {
			if (!usedIds.contains(id)) {
				entity.setId(id);
				try {
					mapper.insert(entity);
					usedIds.add(id);
					return;
				}
				catch (DuplicateKeyException e) {
					log.debug("句子 id 已存在，换后缀重试：{}", id);
				}
			}
			id = baseId + '-' + (suffix + 2);
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

	private String unitKey(String version, String grade, String term, String unit) {
		return version + "|" + grade + "|" + term + "|" + unit;
	}

	private String key(String version, String grade, String term, String unit, String en) {
		return version + "|" + grade + "|" + term + "|" + unit + "|"
				+ (en == null ? "" : en.trim().toLowerCase());
	}

}
