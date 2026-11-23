package cn.wisestar.server.core;

import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishWordMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 人教版初中英语单词分节初始化器。
 *
 * <p>小学单词按主题小节维护，初中单词量大（单单元 30~80 词），学习页会把整单元塞进
 * 「未分节」一组。这里在单元内按学习顺序均衡切成每节约 30 词的小节
 * （30 词以内 1 节，61~90 词 3 节），只处理 {@code section} 为空的人教版单词，
 * 已被管理端手工维护的小节不会被覆盖。</p>
 *
 * @author wisestar
 * @date 2026/11/22
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class EnglishWordSectionSeeder implements ApplicationRunner {

	/** 每节目标词数：超过则继续拆节。 */
	private static final int TARGET_SECTION_SIZE = 30;

	private static final String VERSION = "人教版";

	/**
	 * 小节名用阿拉伯数字：中文数字按字符编码排序会得到「一、三、二」，
	 * 列表按 section 升序分组时顺序错乱。
	 */
	private static final String[] SECTION_NAMES = { "重点词汇（1）", "重点词汇（2）", "重点词汇（3）",
			"重点词汇（4）" };

	/** 历史中文数字小节名 → 新名称，启动时统一迁移。 */
	private static final Map<String, String> LEGACY_SECTION_NAMES = Map.of("重点词汇（一）", "重点词汇（1）",
			"重点词汇（二）", "重点词汇（2）", "重点词汇（三）", "重点词汇（3）", "重点词汇（四）", "重点词汇（4）");

	private final EnglishWordMapper wordMapper;

	@Override
	public void run(ApplicationArguments args) {
		try {
			List<EnglishWord> words = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
					.eq(EnglishWord::getVersion, VERSION)
					.orderByAsc(EnglishWord::getGrade)
					.orderByAsc(EnglishWord::getTerm)
					.orderByAsc(EnglishWord::getUnit)
					.orderByAsc(EnglishWord::getId));
			if (words.isEmpty()) {
				return;
			}
			int renamed = 0;
			for (EnglishWord word : words) {
				if (!StringUtils.hasText(word.getSection())) {
					continue;
				}
				String fixed = LEGACY_SECTION_NAMES.get(word.getSection());
				if (fixed != null) {
					EnglishWord update = new EnglishWord();
					update.setId(word.getId());
					update.setSection(fixed);
					wordMapper.updateById(update);
					word.setSection(fixed);
					renamed++;
				}
			}
			if (renamed > 0) {
				log.info("人教版初中英语单词小节名迁移完成：更新 {} 个单词", renamed);
			}
			Map<String, List<EnglishWord>> groups = new LinkedHashMap<>();
			for (EnglishWord word : words) {
				String key = word.getGrade() + "|" + word.getTerm() + "|" + word.getUnit();
				groups.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
			}
			int updated = 0;
			for (List<EnglishWord> group : groups.values()) {
				boolean maintained = group.stream().anyMatch(w -> StringUtils.hasText(w.getSection()));
				// 40 词以内一屏可学完，不再硬拆；超过 40 词按每节约 30 词均衡分节
				if (maintained || group.size() <= 40) {
					continue;
				}
				int sectionCount = (group.size() + TARGET_SECTION_SIZE - 1) / TARGET_SECTION_SIZE;
				int perSection = (group.size() + sectionCount - 1) / sectionCount;
				for (int i = 0; i < group.size(); i++) {
					int sectionIndex = Math.min(i / perSection, sectionCount - 1);
					EnglishWord word = group.get(i);
					EnglishWord update = new EnglishWord();
					update.setId(word.getId());
					update.setSection(SECTION_NAMES[Math.min(sectionIndex, SECTION_NAMES.length - 1)]);
					wordMapper.updateById(update);
					updated++;
				}
			}
			if (updated > 0) {
				log.info("人教版初中英语单词分节完成：更新 {} 个单词", updated);
			}
		}
		catch (Exception e) {
			log.warn("人教版初中英语单词分节失败：{}", e.getMessage());
		}
	}

}
