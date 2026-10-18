package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishPatternScenarioView;
import cn.wisestar.server.domain.dto.english.EnglishPatternTurn;
import cn.wisestar.server.domain.dto.english.EnglishPatternSentence;
import cn.wisestar.server.domain.dto.english.EnglishScenarioPackQuery;
import cn.wisestar.server.domain.dto.english.EnglishScenarioPackView;
import cn.wisestar.server.domain.dto.english.EnglishScenarioView;
import cn.wisestar.server.domain.dto.english.EnglishScenarioWord;
import cn.wisestar.server.domain.model.EnglishScenarioPack;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishScenarioPackMapper;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.service.EnglishPatternScenarioService;
import cn.wisestar.server.service.EnglishScenarioPackService;
import cn.wisestar.server.service.EnglishScenarioService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 英语单元情景练习内容包服务实现（预生成 + 固化落库 + 学员端只读）。
 *
 * <p>管理端按 版本+年级+册别+单元 把单元单词/句式自动分组后逐组调用 AI 生成情景（预览，不落库），
 * 确认后以业务键覆盖保存；学员端仅读取固化内容并按组顺序合并展示，不再触发实时 AI。</p>
 *
 * @author wisestar
 * @date 2026/10/13
 */
@Slf4j
@Service
public class EnglishScenarioPackServiceImpl implements EnglishScenarioPackService {

	/** 单词情景分组大小 */
	private static final int WORD_GROUP_SIZE = 8;

	/** 句式情景分组大小 */
	private static final int PATTERN_GROUP_SIZE = 6;

	private final EnglishScenarioPackMapper packMapper;
	private final EnglishWordMapper wordMapper;
	private final EnglishSentenceMapper sentenceMapper;
	private final EnglishScenarioService wordScenarioService;
	private final EnglishPatternScenarioService patternScenarioService;
	private final ObjectMapper objectMapper;

	public EnglishScenarioPackServiceImpl(EnglishScenarioPackMapper packMapper, EnglishWordMapper wordMapper,
			EnglishSentenceMapper sentenceMapper, EnglishScenarioService wordScenarioService,
			EnglishPatternScenarioService patternScenarioService) {
		this.packMapper = packMapper;
		this.wordMapper = wordMapper;
		this.sentenceMapper = sentenceMapper;
		this.wordScenarioService = wordScenarioService;
		this.patternScenarioService = patternScenarioService;
		this.objectMapper = new ObjectMapper();
	}

	// ------------------------------------------------------------------ 生成

	@Override
	public EnglishScenarioPackView generate(String version, String grade, String term, String unit) {
		requireNotBlank(version, "教材版本");
		requireNotBlank(grade, "年级");
		requireNotBlank(term, "册别");
		requireNotBlank(unit, "单元");

		List<EnglishWord> words = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(EnglishWord::getVersion, version)
				.eq(EnglishWord::getGrade, grade)
				.eq(EnglishWord::getTerm, term)
				.eq(EnglishWord::getUnit, unit)
				.orderByAsc(EnglishWord::getSpell));
		List<EnglishSentence> sentences = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term)
				.eq(EnglishSentence::getUnit, unit)
				.orderByAsc(EnglishSentence::getSort));

		EnglishScenarioPackView view = new EnglishScenarioPackView();
		view.setVersion(version);
		view.setGrade(grade);
		view.setTerm(term);
		view.setUnit(unit);

		// 单词情景：分组后逐组生成，按组序号合并
		ArrayNode wordGroups = objectMapper.createArrayNode();
		int wordScenarioTotal = 0;
		int wordGroupNo = 1;
		for (List<EnglishWord> chunk : partition(words, WORD_GROUP_SIZE)) {
			List<EnglishScenarioWord> input = chunk.stream()
					.map(w -> new EnglishScenarioWord(w.getSpell(), w.getMeaning()))
					.collect(Collectors.toList());
			List<EnglishScenarioView> scenarios = wordScenarioService.generateFromWords(grade, unit, input);
			wordScenarioTotal += scenarios.size();

			ObjectNode group = wordGroups.addObject();
			group.put("groupNo", wordGroupNo++);
			ArrayNode groupWords = group.putArray("words");
			for (EnglishWord w : chunk) {
				if (w.getSpell() != null && !w.getSpell().trim().isEmpty()) {
					groupWords.add(w.getSpell().trim().toLowerCase());
				}
			}
			group.set("scenarios", objectMapper.valueToTree(scenarios));
		}
		view.setWordContent(writeGroups(wordGroups));
		view.setWordCount(wordScenarioTotal);

		// 句式情景：全单元共生成 10 个情景，按组均分后逐组生成
		PatternDraft patternDraft = buildPatternDraft(grade, unit, sentences);
		view.setPatternContent(patternDraft.content);
		view.setPatternCount(patternDraft.count);

		return view;
	}

	// ------------------------------------------------------------------ 保存 / 查询

	@Override
	public EnglishScenarioPackView regeneratePattern(String version, String grade, String term, String unit) {
		requireNotBlank(version, "教材版本");
		requireNotBlank(grade, "年级");
		requireNotBlank(term, "册别");
		requireNotBlank(unit, "单元");
		EnglishScenarioPack entity = packMapper.selectOne(Wrappers.<EnglishScenarioPack>lambdaQuery()
				.eq(EnglishScenarioPack::getVersion, version)
				.eq(EnglishScenarioPack::getGrade, grade)
				.eq(EnglishScenarioPack::getTerm, term)
				.eq(EnglishScenarioPack::getUnit, unit)
				.last("limit 1"));
		if (entity == null) {
			throw new ValidationException("该单元情景内容包尚未固化，请先生成并保存");
		}
		List<EnglishSentence> sentences = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term)
				.eq(EnglishSentence::getUnit, unit)
				.orderByAsc(EnglishSentence::getSort));
		if (sentences.isEmpty()) {
			throw new ValidationException("本单元暂无重点句式，无法生成句式情景");
		}
		PatternDraft patternDraft = buildPatternDraft(grade, unit, sentences);
		entity.setPatternContent(patternDraft.content);
		entity.setPatternCount(patternDraft.count);
		packMapper.updateById(entity);
		return toView(packMapper.selectById(entity.getId()));
	}

	@Override
	public EnglishScenarioPackView savePack(EnglishScenarioPackView pack) {
		if (pack == null) {
			throw new ValidationException("请先生成内容再保存");
		}
		requireNotBlank(pack.getVersion(), "教材版本");
		requireNotBlank(pack.getGrade(), "年级");
		requireNotBlank(pack.getTerm(), "册别");
		requireNotBlank(pack.getUnit(), "单元");
		if (isBlank(pack.getWordContent()) && isBlank(pack.getPatternContent())) {
			throw new ValidationException("请先生成内容再保存");
		}

		EnglishScenarioPack entity = packMapper.selectOne(Wrappers.<EnglishScenarioPack>lambdaQuery()
				.eq(EnglishScenarioPack::getVersion, pack.getVersion())
				.eq(EnglishScenarioPack::getGrade, pack.getGrade())
				.eq(EnglishScenarioPack::getTerm, pack.getTerm())
				.eq(EnglishScenarioPack::getUnit, pack.getUnit())
				.last("limit 1"));
		boolean update = entity != null;
		if (entity == null) {
			entity = new EnglishScenarioPack();
		}
		entity.setVersion(pack.getVersion());
		entity.setGrade(pack.getGrade());
		entity.setTerm(pack.getTerm());
		entity.setUnit(pack.getUnit());
		entity.setWordContent(pack.getWordContent());
		entity.setWordCount(countScenarios(pack.getWordContent()));
		entity.setPatternContent(pack.getPatternContent());
		entity.setPatternCount(countScenarios(pack.getPatternContent()));

		if (update) {
			packMapper.updateById(entity);
		} else {
			packMapper.insert(entity);
		}
		return toView(packMapper.selectById(entity.getId()));
	}

	@Override
	public PaginationResponse<EnglishScenarioPackView> listPacks(EnglishScenarioPackQuery query) {
		LambdaQueryWrapper<EnglishScenarioPack> wrapper = Wrappers.<EnglishScenarioPack>lambdaQuery()
				.eq(notBlank(query.getVersion()), EnglishScenarioPack::getVersion, query.getVersion())
				.eq(notBlank(query.getGrade()), EnglishScenarioPack::getGrade, query.getGrade())
				.eq(notBlank(query.getTerm()), EnglishScenarioPack::getTerm, query.getTerm())
				.eq(notBlank(query.getUnit()), EnglishScenarioPack::getUnit, query.getUnit())
				.orderByDesc(EnglishScenarioPack::getCreateAt);

		Page<EnglishScenarioPack> page = new Page<>(query.getCurrent(), query.getPageSize());
		Page<EnglishScenarioPack> result = packMapper.selectPage(page, wrapper);

		List<EnglishScenarioPackView> views = result.getRecords().stream().map(pack -> {
			EnglishScenarioPackView view = toView(pack);
			// 列表不下发正文，控制传输体积
			view.setWordContent(null);
			view.setPatternContent(null);
			return view;
		}).collect(Collectors.toList());

		return new PaginationResponse<>(result.getTotal(), views);
	}

	@Override
	public EnglishScenarioPackView getPack(String id) {
		EnglishScenarioPack pack = packMapper.selectById(id);
		if (pack == null) {
			throw new ValidationException("情景内容包不存在或已被删除");
		}
		return toView(pack);
	}

	@Override
	public void deletePack(String id) {
		packMapper.deleteById(id);
	}

	@Override
	public List<String> listUnits(String version, String grade, String term) {
		requireNotBlank(version, "教材版本");
		requireNotBlank(grade, "年级");
		requireNotBlank(term, "册别");

		Set<String> units = new LinkedHashSet<>();
		List<EnglishWord> words = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.select(EnglishWord::getUnit)
				.eq(EnglishWord::getVersion, version)
				.eq(EnglishWord::getGrade, grade)
				.eq(EnglishWord::getTerm, term));
		for (EnglishWord w : words) {
			if (notBlank(w.getUnit())) {
				units.add(w.getUnit().trim());
			}
		}
		List<EnglishSentence> sentences = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.select(EnglishSentence::getUnit)
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term));
		for (EnglishSentence s : sentences) {
			if (notBlank(s.getUnit())) {
				units.add(s.getUnit().trim());
			}
		}
		return new ArrayList<>(units);
	}

	// ------------------------------------------------------------------ 学员端只读

	@Override
	public List<EnglishScenarioView> getWordScenarios(String version, String grade, String term, String unit) {
		EnglishScenarioPack pack = findLatest(version, grade, term, unit);
		if (pack == null || isBlank(pack.getWordContent())) {
			return new ArrayList<>();
		}
		return readScenarios(pack.getWordContent(), EnglishScenarioView.class);
	}

	@Override
	public List<EnglishPatternScenarioView> getPatternScenarios(String version, String grade, String term, String unit) {
		EnglishScenarioPack pack = findLatest(version, grade, term, unit);
		if (pack == null || isBlank(pack.getPatternContent())) {
			return new ArrayList<>();
		}
		List<EnglishPatternScenarioView> scenes = readScenarios(pack.getPatternContent(),
				EnglishPatternScenarioView.class);
		return dedupeAndShuffle(scenes);
	}

	/**
	 * 学员端句式情景：去除重复对话，并打乱候选句式顺序。
	 *
	 * <p>AI 生成的内容存在同一段对话被写入多个情景（标题不同、台词完全相同）的情况，
	 * 直接展示会重复练习；候选项又总是把正确句式放在首位，等于提前给出答案。
	 * 这里按台词序列去重，并用「情景标题 + 轮次 + 候选内容」作为随机种子打乱选项，
	 * 保证同一内容每次展示顺序稳定、正确答案不再固定在最上面。</p>
	 */
	private List<EnglishPatternScenarioView> dedupeAndShuffle(List<EnglishPatternScenarioView> scenes) {
		List<EnglishPatternScenarioView> result = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (EnglishPatternScenarioView scene : scenes) {
			String signature = patternSignature(scene);
			if (!seen.add(signature)) {
				continue;
			}
			shuffleOptions(scene);
			result.add(scene);
		}
		return result;
	}

	/**
	 * 情景去重签名：说话人 + 台词/情景提示 + 正确项下标。
	 */
	private String patternSignature(EnglishPatternScenarioView scene) {
		StringBuilder sb = new StringBuilder();
		if (scene.getTurns() != null) {
			for (EnglishPatternTurn turn : scene.getTurns()) {
				sb.append(turn.getSpeaker()).append('\u0001')
						.append(turn.getEn() == null ? "" : turn.getEn().trim()).append('\u0001')
						.append(turn.getCue() == null ? "" : turn.getCue().trim()).append('\u0001')
						.append(turn.getAnswerIndex()).append('\u0002');
			}
		}
		return sb.toString();
	}

	/**
	 * 打乱单个情景内各作答轮的候选顺序，并同步正确项下标。
	 */
	private void shuffleOptions(EnglishPatternScenarioView scene) {
		if (scene.getTurns() == null) {
			return;
		}
		for (int i = 0; i < scene.getTurns().size(); i++) {
			EnglishPatternTurn turn = scene.getTurns().get(i);
			List<String> options = turn.getOptions();
			Integer answerIndex = turn.getAnswerIndex();
			if (options == null || options.size() < 2 || answerIndex == null
					|| answerIndex < 0 || answerIndex >= options.size()) {
				continue;
			}
			int size = options.size();
			// 至少左移 1 位：AI 原本总把正确项放在首位，移位后正确答案不会再出现在最上面
			int offset = 1 + Math.floorMod(Objects.hash(scene.getTitle(), i, String.join("|", options)),
					size - 1);
			List<String> rotated = new ArrayList<>(options);
			Collections.rotate(rotated, -offset);
			turn.setOptions(rotated);
			turn.setAnswerIndex((answerIndex - offset + size) % size);
		}
	}

	// ------------------------------------------------------------------ 辅助

	/**
	 * 按业务键查询最新一条情景内容包（学期为空时不限定学期）。
	 */
	private EnglishScenarioPack findLatest(String version, String grade, String term, String unit) {
		if (isBlank(version) || isBlank(grade) || isBlank(unit)) {
			return null;
		}
		return packMapper.selectOne(Wrappers.<EnglishScenarioPack>lambdaQuery()
				.eq(EnglishScenarioPack::getVersion, version)
				.eq(EnglishScenarioPack::getGrade, grade)
				.eq(EnglishScenarioPack::getUnit, unit)
				.eq(notBlank(term), EnglishScenarioPack::getTerm, term)
				.orderByDesc(EnglishScenarioPack::getCreateAt)
				.last("limit 1"));
	}

	/**
	 * 从分组 JSON 中按组顺序展平情景列表。
	 */
	private <T> List<T> readScenarios(String content, Class<T> clazz) {
		List<T> result = new ArrayList<>();
		try {
			JsonNode root = objectMapper.readTree(content);
			JsonNode groups = root.path("groups");
			if (!groups.isArray()) {
				return result;
			}
			for (JsonNode group : groups) {
				JsonNode scenarios = group.path("scenarios");
				if (!scenarios.isArray()) {
					continue;
				}
				for (JsonNode scn : scenarios) {
					result.add(objectMapper.treeToValue(scn, clazz));
				}
			}
		} catch (Exception e) {
			log.error("情景内容包解析失败：{}", e.getMessage());
		}
		return result;
	}

	/**
	 * 统计分组 JSON 中的情景总数。
	 */
	private int countScenarios(String content) {
		if (isBlank(content)) {
			return 0;
		}
		int count = 0;
		try {
			JsonNode root = objectMapper.readTree(content);
			JsonNode groups = root.path("groups");
			if (groups.isArray()) {
				for (JsonNode group : groups) {
					JsonNode scenarios = group.path("scenarios");
					if (scenarios.isArray()) {
						count += scenarios.size();
					}
				}
			}
		} catch (Exception e) {
			log.error("情景内容包计数失败：{}", e.getMessage());
		}
		return count;
	}

	private String writeGroups(ArrayNode groups) {
		ObjectNode root = objectMapper.createObjectNode();
		root.set("groups", groups);
		try {
			return objectMapper.writeValueAsString(root);
		} catch (Exception e) {
			return "{\"groups\":[]}";
		}
	}

	/**
	 * 构建句式情景分组 JSON：全单元固定 10 个情景，按分组数量均分，并把已用标题传给后续分组避免重复。
	 */
	private PatternDraft buildPatternDraft(String grade, String unit, List<EnglishSentence> sentences) {
		ArrayNode patternGroups = objectMapper.createArrayNode();
		int patternScenarioTotal = 0;
		int patternGroupNo = 1;
		List<List<EnglishSentence>> patternChunks = partition(sentences, PATTERN_GROUP_SIZE);
		List<String> usedScenarioTitles = new ArrayList<>();
		Set<String> usedScenarioSignatures = new java.util.HashSet<>();
		if (!patternChunks.isEmpty()) {
			int patternCountPerUnit = 10;
			int basePerGroup = patternCountPerUnit / patternChunks.size();
			int extraGroups = patternCountPerUnit % patternChunks.size();
			for (int chunkIndex = 0; chunkIndex < patternChunks.size(); chunkIndex++) {
				List<EnglishSentence> chunk = patternChunks.get(chunkIndex);
				int groupScenarioCount = basePerGroup + (chunkIndex < extraGroups ? 1 : 0);
				List<EnglishPatternSentence> input = chunk.stream()
						.map(s -> new EnglishPatternSentence(s.getEn(), s.getZh()))
						.collect(Collectors.toList());
				// 模型容易在后半段重复同一段对话，先多要一些，再按台词签名去重；不足则继续补生成
				List<EnglishPatternScenarioView> scenarios = new ArrayList<>();
				int round = 0;
				while (scenarios.size() < groupScenarioCount && round < 3) {
					int missing = groupScenarioCount - scenarios.size();
					int requestCount = missing + Math.max(2, missing / 2);
					List<EnglishPatternScenarioView> rawScenarios = patternScenarioService.generateFromSentences(
							grade, unit, input, requestCount, usedScenarioTitles);
					for (EnglishPatternScenarioView scenario : rawScenarios) {
						String signature = patternSignature(scenario);
						if (!usedScenarioSignatures.add(signature)) {
							continue;
						}
						scenarios.add(scenario);
						if (scenario.getTitle() != null && !scenario.getTitle().trim().isEmpty()) {
							usedScenarioTitles.add(scenario.getTitle().trim());
						}
						if (scenarios.size() >= groupScenarioCount) {
							break;
						}
					}
					round++;
				}
				patternScenarioTotal += scenarios.size();
				ObjectNode group = patternGroups.addObject();
				group.put("groupNo", patternGroupNo++);
				ArrayNode groupSentences = group.putArray("sentences");
				for (EnglishSentence s : chunk) {
					if (s.getEn() != null && !s.getEn().trim().isEmpty()) {
						groupSentences.add(s.getEn().trim());
					}
				}
				group.set("scenarios", objectMapper.valueToTree(scenarios));
			}
		}
		return new PatternDraft(writeGroups(patternGroups), patternScenarioTotal);
	}

	/**
	 * 句式情景草稿：分组 JSON 与情景总数。
	 */
	private static final class PatternDraft {

		private final String content;
		private final int count;

		private PatternDraft(String content, int count) {
			this.content = content;
			this.count = count;
		}

	}

	private static <T> List<List<T>> partition(List<T> list, int size) {
		List<List<T>> result = new ArrayList<>();
		if (list == null || list.isEmpty()) {
			return result;
		}
		for (int i = 0; i < list.size(); i += size) {
			result.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + size))));
		}
		return result;
	}

	private EnglishScenarioPackView toView(EnglishScenarioPack pack) {
		EnglishScenarioPackView view = new EnglishScenarioPackView();
		view.setId(pack.getId());
		view.setVersion(pack.getVersion());
		view.setGrade(pack.getGrade());
		view.setTerm(pack.getTerm());
		view.setUnit(pack.getUnit());
		view.setWordContent(pack.getWordContent());
		view.setWordCount(pack.getWordCount());
		view.setPatternContent(pack.getPatternContent());
		view.setPatternCount(pack.getPatternCount());
		view.setCreateAt(pack.getCreateAt());
		return view;
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static boolean notBlank(String s) {
		return !isBlank(s);
	}

	private static void requireNotBlank(String value, String name) {
		if (isBlank(value)) {
			throw new ValidationException(name + "不能为空");
		}
	}

}
