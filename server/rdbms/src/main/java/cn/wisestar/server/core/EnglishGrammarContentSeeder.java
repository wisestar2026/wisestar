package cn.wisestar.server.core;

import cn.wisestar.server.domain.model.EnglishGrammar;
import cn.wisestar.server.mapper.EnglishGrammarMapper;
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

/**
 * 人教版初中英语语法点讲解初始化器。
 *
 * <p>历史种子只写了语法标题和主题，{@code content} 形如「标题。主题：……」，
 * {@code structure} 与标题相同。这里读取 {@code english/grammar-content.json}
 * 为人教版初中语法点补齐中文讲解与结构公式；同时把两条由教材目录拼接产生的
 * 合并标题拆成两个独立语法点（原行保留第一点，新增第二点）。</p>
 *
 * <p>只覆盖仍为占位内容的行，管理端已手工修改的讲解不会被重置。</p>
 *
 * @author wisestar
 * @date 2026/11/22
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class EnglishGrammarContentSeeder implements ApplicationRunner {

	private final EnglishGrammarMapper grammarMapper;

	private final ObjectMapper objectMapper;

	@Override
	public void run(ApplicationArguments args) {
		try {
			JsonNode root = loadContent();
			if (root == null) {
				return;
			}
			int updated = 0;
			for (JsonNode node : root.path("rows")) {
				updated += refresh(node);
			}
			for (JsonNode node : root.path("newRows")) {
				updated += createIfAbsent(node);
			}
			if (updated > 0) {
				log.info("人教版初中英语语法讲解初始化完成：更新 {} 条", updated);
			}
		}
		catch (Exception e) {
			log.warn("人教版初中英语语法讲解初始化失败：{}", e.getMessage());
		}
	}

	private JsonNode loadContent() throws Exception {
		try (InputStream in = new ClassPathResource("english/grammar-content.json").getInputStream()) {
			return objectMapper.readTree(in);
		}
	}

	/** 已存在的语法行：仅在仍为占位内容时刷新标题、讲解和结构。 */
	private int refresh(JsonNode node) {
		EnglishGrammar entity = grammarMapper.selectById(node.path("id").asText());
		if (entity == null) {
			return 0;
		}
		boolean placeholder = isPlaceholder(entity.getContent(), entity.getStructure(), entity.getTitle());
		String title = node.path("title").asText();
		boolean titleChanged = !title.equals(entity.getTitle());
		if (!placeholder && !titleChanged) {
			return 0;
		}
		EnglishGrammar update = new EnglishGrammar();
		update.setId(entity.getId());
		if (titleChanged) {
			update.setTitle(title);
		}
		if (placeholder) {
			update.setContent(node.path("content").asText());
			update.setStructure(node.path("structure").asText());
		}
		grammarMapper.updateById(update);
		return 1;
	}

	/** 拆分出来的第二语法点：不存在则按原行维度新增。 */
	private int createIfAbsent(JsonNode node) {
		String id = node.path("id").asText();
		EnglishGrammar exist = grammarMapper.selectById(id);
		if (exist != null) {
			return 0;
		}
		EnglishGrammar entity = new EnglishGrammar();
		entity.setId(id);
		entity.setTitle(node.path("title").asText());
		entity.setContent(node.path("content").asText());
		entity.setStructure(node.path("structure").asText());
		entity.setTextbook(node.path("textbook").asText(null));
		entity.setSamples(node.path("samples").asText(null));
		entity.setExamples(node.path("examples").asText(null));
		entity.setExercises(node.path("exercises").asText(null));
		entity.setVersion(node.path("version").asText());
		entity.setGrade(node.path("grade").asText());
		entity.setTerm(node.path("term").asText());
		entity.setUnit(node.path("unit").asText());
		entity.setSection(node.path("section").asText(null));
		entity.setSort(node.path("sort").asInt(0));
		grammarMapper.insert(entity);
		return 1;
	}

	private boolean isPlaceholder(String content, String structure, String title) {
		if (!StringUtils.hasText(content) || content.contains("主题：")) {
			return true;
		}
		return !StringUtils.hasText(structure) || structure.trim().equals(title);
	}

}
