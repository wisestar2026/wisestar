package cn.wisestar.server.core;

import cn.wisestar.server.domain.model.EnglishContentTag;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 英语题目打标标签初始化器。
 *
 * 【类职责】
 * 应用启动时把 `english/content-tags.json` 中的打标结果批量写入 `t_english_content_tag`。
 * 按 content_key 增量补齐：已存在的标签跳过，只插入新增题目，避免重复导入；打标结果来自
 * 《英语题库打标结果》，只包含句子包/情景包中真实存在的考题（语境行不入库）。
 *
 * 【依赖什么】
 * - JdbcTemplate：批量插入标签；
 * - ObjectMapper：解析 classpath 下的打标 JSON。
 *
 * 【核心数据流】
 * Spring 启动完成 SQL 初始化 → 本 Runner 读取 JSON 与已有 content_key → 只把缺失标签 batchUpdate 写入。
 *
 * @author wisestar
 * @date 2026/10/17
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnglishContentTagSeeder implements ApplicationRunner {

	private static final String INSERT_SQL = "INSERT INTO t_english_content_tag "
			+ "(id, module, content_key, version, grade, term, unit, group_no, scene_title, turn_no, "
			+ "question_type, knowledge_point, language_skill, cognitive_level, distractor_strength, "
			+ "difficulty_seed, is_exam_question) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private final JdbcTemplate jdbcTemplate;

	private final ObjectMapper objectMapper;

	@Override
	public void run(ApplicationArguments args) {
		try {
			List<EnglishContentTag> tags;
			try (InputStream in = new ClassPathResource("english/content-tags.json").getInputStream()) {
				tags = objectMapper.readValue(in, new TypeReference<List<EnglishContentTag>>() {
				});
			}
			if (tags.isEmpty()) {
				return;
			}
			Set<String> existing = new HashSet<>(
					jdbcTemplate.queryForList("SELECT content_key FROM t_english_content_tag", String.class));
			List<EnglishContentTag> missing = tags.stream()
					.filter(tag -> !existing.contains(tag.getContentKey())).toList();
			if (missing.isEmpty()) {
				log.info("英语打标标签已是最新（{} 条），跳过初始化", existing.size());
				return;
			}
			jdbcTemplate.batchUpdate(INSERT_SQL, missing, 500, (ps, tag) -> {
				ps.setString(1, tag.getId());
				ps.setString(2, tag.getModule());
				ps.setString(3, tag.getContentKey());
				ps.setString(4, tag.getVersion());
				ps.setString(5, tag.getGrade());
				ps.setString(6, tag.getTerm());
				ps.setString(7, tag.getUnit());
				ps.setString(8, tag.getGroupNo());
				ps.setString(9, tag.getSceneTitle());
				ps.setObject(10, tag.getTurnNo());
				ps.setString(11, tag.getQuestionType());
				ps.setString(12, tag.getKnowledgePoint());
				ps.setString(13, tag.getLanguageSkill());
				ps.setString(14, tag.getCognitiveLevel());
				ps.setString(15, tag.getDistractorStrength());
				ps.setString(16, tag.getDifficultySeed());
				ps.setInt(17, tag.getIsExamQuestion() == null ? 1 : tag.getIsExamQuestion());
			});
			log.info("英语打标标签补齐完成：新增 {} 条，已有 {} 条", missing.size(), existing.size());
		}
		catch (Exception e) {
			log.warn("英语打标标签初始化失败：{}", e.getMessage());
		}
	}

}
