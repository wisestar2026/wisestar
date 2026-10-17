package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.dto.detect.DetectReportView;
import cn.wisestar.server.domain.dto.english.EnglishSentenceExercise;
import cn.wisestar.server.domain.dto.english.EnglishUnitPaperRequest;
import cn.wisestar.server.domain.dto.english.EnglishUnitPaperSubmitRequest;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.EnglishContentTag;
import cn.wisestar.server.domain.model.EnglishScenarioPack;
import cn.wisestar.server.domain.model.EnglishSentencePack;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.UserWeakKnowledge;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.EnglishContentTagMapper;
import cn.wisestar.server.mapper.EnglishScenarioPackMapper;
import cn.wisestar.server.mapper.EnglishSentencePackMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.UserWeakKnowledgeMapper;
import cn.wisestar.server.service.EnglishUnitPaperService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 英语单元练习组卷实现（内容包阶梯卷）。
 *
 * <p><b>组卷</b>：按标签表的知识点分组，组内按 D1→D3 递进取题；缺失难度用最近难度补位，
 * 题量不足继续从下一知识点取题，并优先覆盖学员当前薄弱的模块。</p>
 *
 * <p><b>交卷</b>：按内容键回读句子包/情景包复核答案，逐题生成解析，
 * 并按「单元·模块」把作答结果纳入学情评价。</p>
 *
 * @author wisestar
 * @date 2026/10/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnglishUnitPaperServiceImpl implements EnglishUnitPaperService {

	private static final int DEFAULT_COUNT = 20;

	private static final int MAX_COUNT = 30;

	private static final List<String> LEVELS = List.of("D1", "D2", "D3");

	private static final List<String> MODULES = List.of("句子", "单词", "句式");

	private static final Pattern BLANK = Pattern.compile("_{3,}");

	private static final String[] CIRCLED = { "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧" };

	private final EnglishContentTagMapper tagMapper;

	private final EnglishSentencePackMapper sentencePackMapper;

	private final EnglishScenarioPackMapper scenarioPackMapper;

	private final ChapterMapper chapterMapper;

	private final UserWeakKnowledgeMapper userWeakKnowledgeMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	private final EnglishPracticeEvaluator practiceEvaluator;

	private final ObjectMapper objectMapper;

	@Override
	public List<StudentQuestionView> generate(EnglishUnitPaperRequest request) {
		if (request == null || !StringUtils.hasText(request.getUnit())) {
			return Collections.emptyList();
		}
		List<Q> all = loadQuestions(request.getVersion(), request.getGrade(), request.getTerm(), request.getUnit());
		if (all.isEmpty()) {
			return Collections.emptyList();
		}
		int limit = request.getQuestionCount() == null ? DEFAULT_COUNT
				: Math.max(1, Math.min(request.getQuestionCount(), MAX_COUNT));
		Map<String, Integer> groupPriority = groupPriority(request, all);
		List<String> moduleOrder = moduleOrder(groupPriority);
		Map<String, Deque<Group>> queues = groupQueues(all, groupPriority);
		List<Q> paper = new ArrayList<>(limit);
		while (paper.size() < limit) {
			boolean added = false;
			for (String module : moduleOrder) {
				Deque<Group> queue = queues.get(module);
				if (queue == null || queue.isEmpty()) {
					continue;
				}
				paper.addAll(ladder(queue.poll(), limit - paper.size()));
				added = true;
				if (paper.size() >= limit) {
					break;
				}
			}
			if (!added) {
				break;
			}
		}
		return paper.stream().map(this::toView).collect(Collectors.toList());
	}

	@Override
	public DetectReportView submit(EnglishUnitPaperSubmitRequest request) {
		DetectReportView report = new DetectReportView();
		if (request == null || !StringUtils.hasText(request.getUnit())
				|| request.getItems() == null || request.getItems().isEmpty()) {
			return report;
		}
		Map<String, Q> questions = loadQuestions(request.getVersion(), request.getGrade(), request.getTerm(),
				request.getUnit()).stream().collect(Collectors.toMap(q -> q.tag.getContentKey(), q -> q, (a, b) -> a));
		Map<String, int[]> kpAgg = new LinkedHashMap<>();
		Map<String, int[]> moduleAgg = new LinkedHashMap<>();
		Map<String, List<Integer>> moduleScores = new LinkedHashMap<>();
		for (EnglishUnitPaperSubmitRequest.Item item : request.getItems()) {
			Q q = questions.get(item.getContentKey());
			if (q == null) {
				continue;
			}
			EnglishBehaviorJudge.Mastery mastery = behavior(q, item);
			int score = mastery.getScore();
			DetectReportView.Detail detail = new DetectReportView.Detail();
			detail.setQuestionId(q.tag.getContentKey());
			detail.setChapter(request.getUnit());
			detail.setKnowledgePoint(q.tag.getKnowledgePoint());
			detail.setQuestionType(q.type.name());
			detail.setMastery(mastery.getLabel());
			detail.setScore(score);
			detail.setCorrect(mastery == EnglishBehaviorJudge.Mastery.MASTERED ? 1 : 0);
			detail.setQuestion(toView(q));
			detail.setCorrectAnswers(q.correctAnswers());
			detail.setStudentAnswer(formatStudentAnswer(q, item.getAnswer()));
			detail.setAnalysis(q.analysis);
			report.getDetails().add(detail);

			int[] kp = kpAgg.computeIfAbsent(q.tag.getKnowledgePoint(), k -> new int[3]);
			kp[0] += 1;
			kp[2] += score;
			int[] module = moduleAgg.computeIfAbsent(q.tag.getModule(), k -> new int[3]);
			module[0] += 1;
			module[2] += score;
			if (mastery == EnglishBehaviorJudge.Mastery.MASTERED) {
				kp[1] += 1;
				module[1] += 1;
			}
			moduleScores.computeIfAbsent(q.tag.getModule(), k -> new ArrayList<>()).add(score);
		}
		int total = report.getDetails().size();
		int correctCount = (int) report.getDetails().stream()
				.filter(d -> d.getCorrect() != null && d.getCorrect() == 1).count();
		report.setTotal(total);
		report.setCorrect(correctCount);
		report.setAccuracy(total == 0 ? 0 : Math.round(correctCount * 100f / total));
		report.setMastered((int) report.getDetails().stream()
				.filter(d -> "掌握".equals(d.getMastery())).count());
		report.setUnstable((int) report.getDetails().stream()
				.filter(d -> "不稳定".equals(d.getMastery())).count());
		report.setFlawed((int) report.getDetails().stream()
				.filter(d -> "漏洞".equals(d.getMastery())).count());
		report.setMasteryScore(total == 0 ? 0
				: (int) Math.round(report.getDetails().stream()
						.mapToInt(d -> d.getScore() == null ? 0 : d.getScore()).sum() / (double) total));

		DetectReportView.ChapterStat chapterStat = new DetectReportView.ChapterStat();
		chapterStat.setName(request.getUnit());
		chapterStat.setTotal(total);
		chapterStat.setCorrect(correctCount);
		chapterStat.setAccuracy(total == 0 ? 0 : Math.round(correctCount * 100f / total));
		report.getChapterStats().add(chapterStat);

		List<DetectReportView.WeakPoint> weakPoints = new ArrayList<>();
		kpAgg.forEach((name, stat) -> {
			int wrong = stat[0] - stat[1];
			// 与薄弱阈值对齐：加权得分（掌握 100 / 不稳定 60 / 漏洞 0）低于阈值才列为薄弱
			if (wrong <= 0 || stat[2] >= EvaluationServiceImpl.WEAK_THRESHOLD * stat[0]) {
				return;
			}
			DetectReportView.WeakPoint wp = new DetectReportView.WeakPoint();
			wp.setName(name);
			wp.setChapter(request.getUnit());
			wp.setTotal(stat[0]);
			wp.setCorrect(stat[1]);
			wp.setWrong(wrong);
			wp.setAccuracy(stat[0] == 0 ? 0 : Math.round(stat[1] * 100f / stat[0]));
			weakPoints.add(wp);
		});
		weakPoints.sort((a, b) -> Integer.compare(a.getAccuracy(), b.getAccuracy()));
		report.setWeakPoints(weakPoints);

		String userId = SecurityContextUtils.getUserId();
		moduleAgg.forEach((module, stat) -> practiceEvaluator.recordScores(userId, request.getVersion(),
				request.getGrade(), request.getTerm(), request.getUnit(), module,
				moduleScores.getOrDefault(module, Collections.emptyList())));
		return report;
	}

	/**
	 * 按内容键回读句子包/情景包，组装成可组卷的题目。
	 */
	private List<Q> loadQuestions(String version, String grade, String term, String unit) {
		Map<String, Q> sentence = loadSentenceQuestions(version, grade, term, unit);
		Map<String, Q> word = loadWordQuestions(version, grade, term, unit);
		Map<String, Q> pattern = loadPatternQuestions(version, grade, term, unit);
		Map<String, Q> all = new HashMap<>(sentence);
		all.putAll(word);
		all.putAll(pattern);
		List<EnglishContentTag> tags = tagMapper.selectList(Wrappers.<EnglishContentTag>lambdaQuery()
				.eq(StringUtils.hasText(version), EnglishContentTag::getVersion, version)
				.eq(StringUtils.hasText(grade), EnglishContentTag::getGrade, grade)
				.eq(StringUtils.hasText(term), EnglishContentTag::getTerm, term)
				.eq(EnglishContentTag::getUnit, unit)
				.eq(EnglishContentTag::getIsExamQuestion, 1)
				.orderByAsc(EnglishContentTag::getContentKey));
		List<Q> result = new ArrayList<>(tags.size());
		for (EnglishContentTag tag : tags) {
			Q q = all.get(tag.getContentKey());
			if (q != null) {
				// 打标文件个别情景标题带「情景标题N：」前缀或「情景标题(X)」包裹，
				// 统一收敛为纯标题，避免同一情景被拆成多个知识点。
				tag.setKnowledgePoint(normalizeKnowledgePoint(tag.getKnowledgePoint()));
				q.tag = tag;
				q.module = tag.getModule();
				result.add(q);
			}
		}
		return result;
	}

	private Map<String, Q> loadSentenceQuestions(String version, String grade, String term, String unit) {
		EnglishSentencePack pack = sentencePackMapper.selectOne(Wrappers.<EnglishSentencePack>lambdaQuery()
				.eq(StringUtils.hasText(version), EnglishSentencePack::getVersion, version)
				.eq(StringUtils.hasText(grade), EnglishSentencePack::getGrade, grade)
				.eq(StringUtils.hasText(term), EnglishSentencePack::getTerm, term)
				.eq(EnglishSentencePack::getUnit, unit)
				.last("limit 1"));
		Map<String, Q> result = new HashMap<>();
		if (pack == null || !StringUtils.hasText(pack.getContent())) {
			return result;
		}
		try {
			List<EnglishSentenceExercise> items = objectMapper.readValue(pack.getContent(),
					new TypeReference<List<EnglishSentenceExercise>>() {
					});
			for (EnglishSentenceExercise item : items) {
				if (item == null || !StringUtils.hasText(item.getKind()) || !StringUtils.hasText(item.getEn())) {
					continue;
				}
				String key = sentenceKey(version, grade, term, unit, item);
				Q q = new Q();
				q.kind = item.getKind();
				q.answer = item.getEn();
				q.analysis = StringUtils.hasText(item.getExplanation()) ? item.getExplanation() : item.getZh();
				if ("choice".equals(item.getKind())) {
					List<String> options = item.getOptions() == null || item.getOptions().isEmpty()
							? List.of(item.getEn()) : item.getOptions();
					q.type = SurveySchema.QuestionType.Radio;
					q.title = item.getZh() + "（选择正确的英文表达）";
					q.options = options;
					q.answer = StringUtils.hasText(item.getAnswer()) ? item.getAnswer() : item.getEn();
				}
				else if ("build".equals(item.getKind())) {
					q.type = SurveySchema.QuestionType.FillBlank;
					q.title = item.getZh() + "（用所给单词连成句子：" + wordBank(item) + "）";
				}
				else {
					q.type = SurveySchema.QuestionType.FillBlank;
					q.title = item.getZh() + "（请写出英文句子）";
				}
				result.put(key, q);
			}
		}
		catch (Exception e) {
			log.warn("english unit paper: parse sentence pack failed, unit={}", unit, e);
		}
		return result;
	}

	private Map<String, Q> loadWordQuestions(String version, String grade, String term, String unit) {
		EnglishScenarioPack pack = scenarioPackMapper.selectOne(Wrappers.<EnglishScenarioPack>lambdaQuery()
				.eq(StringUtils.hasText(version), EnglishScenarioPack::getVersion, version)
				.eq(StringUtils.hasText(grade), EnglishScenarioPack::getGrade, grade)
				.eq(StringUtils.hasText(term), EnglishScenarioPack::getTerm, term)
				.eq(EnglishScenarioPack::getUnit, unit)
				.last("limit 1"));
		Map<String, Q> result = new HashMap<>();
		if (pack == null || !StringUtils.hasText(pack.getWordContent())) {
			return result;
		}
		try {
			JsonNode root = objectMapper.readTree(pack.getWordContent());
			for (JsonNode group : root.path("groups")) {
				String groupNo = group.path("groupNo").asText();
				for (JsonNode scene : group.path("scenarios")) {
					String title = scene.path("title").asText();
					int turn = 0;
					for (JsonNode line : scene.path("lines")) {
						turn++;
						List<String> answers = new ArrayList<>();
						for (JsonNode answer : line.path("answers")) {
							answers.add(answer.asText());
						}
						if (answers.isEmpty()) {
							continue;
						}
						String en = line.path("en").asText();
						String blanked = blankify(en, answers.size());
						if (!StringUtils.hasText(blanked)) {
							continue;
						}
						Q q = new Q();
						q.type = SurveySchema.QuestionType.MultipleBlank;
						q.title = line.path("speaker").asText("A") + ": " + blanked + "（" + line.path("zh").asText() + "）";
						q.answers = answers;
						q.analysis = line.path("zh").asText();
						String key = "word:" + version + ":" + grade + ":" + term + ":" + unit + ":"
								+ groupNo + ":" + title + ":" + turn;
						result.put(key, q);
					}
				}
			}
		}
		catch (Exception e) {
			log.warn("english unit paper: parse word scenarios failed, unit={}", unit, e);
		}
		return result;
	}

	private Map<String, Q> loadPatternQuestions(String version, String grade, String term, String unit) {
		EnglishScenarioPack pack = scenarioPackMapper.selectOne(Wrappers.<EnglishScenarioPack>lambdaQuery()
				.eq(StringUtils.hasText(version), EnglishScenarioPack::getVersion, version)
				.eq(StringUtils.hasText(grade), EnglishScenarioPack::getGrade, grade)
				.eq(StringUtils.hasText(term), EnglishScenarioPack::getTerm, term)
				.eq(EnglishScenarioPack::getUnit, unit)
				.last("limit 1"));
		Map<String, Q> result = new HashMap<>();
		if (pack == null || !StringUtils.hasText(pack.getPatternContent())) {
			return result;
		}
		try {
			JsonNode root = objectMapper.readTree(pack.getPatternContent());
			for (JsonNode group : root.path("groups")) {
				String groupNo = group.path("groupNo").asText();
				for (JsonNode scene : group.path("scenarios")) {
					String title = scene.path("title").asText();
					int turn = 0;
					for (JsonNode node : scene.path("turns")) {
						turn++;
						int answerIndex = node.path("answerIndex").asInt(-1);
						List<String> options = new ArrayList<>();
						for (JsonNode option : node.path("options")) {
							options.add(option.asText());
						}
						if (answerIndex < 0 || answerIndex >= options.size()) {
							continue;
						}
						Q q = new Q();
						q.type = SurveySchema.QuestionType.Radio;
						String cue = node.path("cue").asText();
						q.title = (StringUtils.hasText(cue) ? cue : title) + "（选择符合情景的句子）";
						q.options = options;
						q.answer = options.get(answerIndex);
						q.analysis = node.path("explanation").asText();
						String key = "pattern:" + version + ":" + grade + ":" + term + ":" + unit + ":"
								+ groupNo + ":" + title + ":" + turn;
						result.put(key, q);
					}
				}
			}
		}
		catch (Exception e) {
			log.warn("english unit paper: parse pattern scenarios failed, unit={}", unit, e);
		}
		return result;
	}

	/**
	 * 知识点分组：优先精细知识点命中，其次薄弱模块，最后普通分组。
	 */
	private Map<String, Integer> groupPriority(EnglishUnitPaperRequest request, List<Q> questions) {
		Set<String> weakNames = weakKnowledgeNames(request);
		Map<String, Integer> result = new HashMap<>();
		for (Q q : questions) {
			String kp = q.tag.getKnowledgePoint();
			int priority = 2;
			if (weakNames.contains(kp)) {
				priority = 0;
			}
			else if (weakNames.contains(request.getUnit() + "·" + q.tag.getModule())) {
				priority = 1;
			}
			result.merge(groupKey(q), priority, Math::min);
		}
		return result;
	}

	private Set<String> weakKnowledgeNames(EnglishUnitPaperRequest request) {
		String userId = SecurityContextUtils.getUserId();
		if (!StringUtils.hasText(userId)) {
			return Collections.emptySet();
		}
		String subjectId = null;
		if (StringUtils.hasText(request.getChapterId())) {
			Chapter chapter = chapterMapper.selectById(request.getChapterId());
			subjectId = chapter == null ? null : chapter.getSubjectId();
		}
		if (!StringUtils.hasText(subjectId)) {
			return Collections.emptySet();
		}
		List<UserWeakKnowledge> weak = userWeakKnowledgeMapper.selectList(Wrappers.<UserWeakKnowledge>lambdaQuery()
				.eq(UserWeakKnowledge::getUserId, userId)
				.eq(UserWeakKnowledge::getSubjectId, subjectId)
				.eq(UserWeakKnowledge::getStatus, "active"));
		if (weak.isEmpty()) {
			return Collections.emptySet();
		}
		Set<String> ids = weak.stream().map(UserWeakKnowledge::getKnowledgePointId)
				.filter(StringUtils::hasText).collect(Collectors.toSet());
		if (ids.isEmpty()) {
			return Collections.emptySet();
		}
		return knowledgePointMapper.selectBatchIds(ids).stream().map(KnowledgePoint::getName)
				.filter(StringUtils::hasText).collect(Collectors.toSet());
	}

	private List<String> moduleOrder(Map<String, Integer> priorities) {
		Set<String> weakModules = new LinkedHashSet<>();
		MODULES.forEach(module -> {
			if (priorities.entrySet().stream().anyMatch(e -> e.getKey().startsWith(module + "|") && e.getValue() < 2)) {
				weakModules.add(module);
			}
		});
		List<String> order = new ArrayList<>(weakModules);
		MODULES.stream().filter(module -> !weakModules.contains(module)).forEach(order::add);
		return order;
	}

	private Map<String, Deque<Group>> groupQueues(List<Q> questions, Map<String, Integer> priorities) {
		Map<String, List<Q>> grouped = new LinkedHashMap<>();
		for (Q q : questions) {
			grouped.computeIfAbsent(groupKey(q), k -> new ArrayList<>()).add(q);
		}
		Map<String, List<Group>> byModule = new LinkedHashMap<>();
		grouped.forEach((key, items) -> {
			Group group = new Group();
			group.key = key;
			group.items = items;
			group.priority = priorities.getOrDefault(key, 2);
			byModule.computeIfAbsent(items.get(0).module, k -> new ArrayList<>()).add(group);
		});
		Map<String, Deque<Group>> result = new LinkedHashMap<>();
		byModule.forEach((module, groups) -> {
			groups.sort((a, b) -> {
				if (!a.priority.equals(b.priority)) {
					return Integer.compare(a.priority, b.priority);
				}
				if (a.items.size() != b.items.size()) {
					return Integer.compare(b.items.size(), a.items.size());
				}
				return a.key.compareTo(b.key);
			});
			result.put(module, new ArrayDeque<>(groups));
		});
		return result;
	}

	/**
	 * 组内阶梯：D1→D2→D3 逐级取题，缺失难度取最近且未用过的题目。
	 */
	private List<Q> ladder(Group group, int slots) {
		List<Q> used = new ArrayList<>(3);
		for (String level : LEVELS) {
			Q best = null;
			int bestDistance = Integer.MAX_VALUE;
			int bestRank = Integer.MAX_VALUE;
			for (Q q : group.items) {
				if (used.contains(q)) {
					continue;
				}
				int rank = difficultyRank(q.tag.getDifficultySeed());
				int distance = Math.abs(rank - difficultyRank(level));
				if (distance < bestDistance || (distance == bestDistance && rank < bestRank)) {
					best = q;
					bestDistance = distance;
					bestRank = rank;
				}
			}
			if (best != null) {
				used.add(best);
			}
			if (used.size() >= slots) {
				break;
			}
		}
		return used;
	}

	private int difficultyRank(String difficulty) {
		int index = difficulty == null ? -1 : LEVELS.indexOf(difficulty.trim().toUpperCase(Locale.ROOT));
		return index < 0 ? LEVELS.size() : index;
	}

	private StudentQuestionView toView(Q q) {
		StudentQuestionView view = new StudentQuestionView();
		view.setId(q.tag.getContentKey());
		view.setName(q.title);
		view.setQuestionType(q.type);
		view.setTag(new String[] { q.tag.getKnowledgePoint(), q.tag.getDifficultySeed() });
		SurveySchema schema = new SurveySchema();
		schema.setId(q.tag.getContentKey());
		schema.setTitle(q.title);
		schema.setType(q.type);
		if (q.options != null) {
			List<SurveySchema> children = new ArrayList<>(q.options.size());
			for (int i = 0; i < q.options.size(); i++) {
				SurveySchema child = new SurveySchema();
				child.setId(q.tag.getContentKey() + "#" + i);
				child.setTitle(q.options.get(i));
				child.setType(SurveySchema.QuestionType.Option);
				children.add(child);
			}
			schema.setChildren(children);
		}
		view.setSchema(schema);
		return view;
	}

	/**
	 * 按题型执行行为三态判定（掌握 / 不稳定 / 漏洞）。
	 */
	private EnglishBehaviorJudge.Mastery behavior(Q q, EnglishUnitPaperSubmitRequest.Item item) {
		Map<String, Object> answer = item.getAnswer();
		if (answer == null) {
			return EnglishBehaviorJudge.Mastery.FLAW;
		}
		if (SurveySchema.QuestionType.Radio.equals(q.type)) {
			String selected = optionTitle(q, answer);
			boolean correct = selected != null
					&& normalizeSentence(selected).equals(normalizeSentence(q.answer));
			return EnglishBehaviorJudge.judgeChoice(correct, item.getChanges());
		}
		String text = answer.get("text") == null ? "" : String.valueOf(answer.get("text"));
		if (SurveySchema.QuestionType.MultipleBlank.equals(q.type)) {
			return EnglishBehaviorJudge.judgeBlanks(q.answers, text, item.getHintUsed());
		}
		if ("build".equals(q.kind)) {
			return EnglishBehaviorJudge.judgeBuild(q.answer, text);
		}
		if ("dictation".equals(q.kind)) {
			return EnglishBehaviorJudge.judgeDictation(q.answer, text);
		}
		return EnglishBehaviorJudge.judgeTurn(q.correctAnswers(), text, item.getChanges());
	}

	private String optionTitle(Q q, Map<String, Object> answer) {
		Object type = answer.get("type");
		if (!"option".equals(type)) {
			return null;
		}
		String optionId = String.valueOf(answer.get("optionId"));
		if (q.options == null) {
			return null;
		}
		String prefix = q.tag.getContentKey() + "#";
		if (optionId.startsWith(prefix)) {
			try {
				int index = Integer.parseInt(optionId.substring(prefix.length()));
				return index >= 0 && index < q.options.size() ? q.options.get(index) : null;
			}
			catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private String formatStudentAnswer(Q q, Map<String, Object> answer) {
		if (answer == null) {
			return null;
		}
		if (SurveySchema.QuestionType.Radio.equals(q.type)) {
			return optionTitle(q, answer);
		}
		Object text = answer.get("text");
		if (text == null) {
			return null;
		}
		return String.valueOf(text).replace("|", " / ");
	}

	private String sentenceKey(String version, String grade, String term, String unit, EnglishSentenceExercise item) {
		return "sentence:" + version + ":" + grade + ":" + term + ":" + unit + ":"
				+ item.getSentenceId() + ":" + item.getKind();
	}

	private String wordBank(EnglishSentenceExercise item) {
		List<String> tokens = new ArrayList<>(List.of(item.getEn().trim().split("\\s+")));
		Collections.shuffle(tokens, new Random(item.getEn().hashCode()));
		return String.join(" / ", tokens);
	}

	/**
	 * 把英文台词中的空位改写为 __①__ 形式，使练习端按题干识别空位数。
	 */
	private String blankify(String en, int blankCount) {
		if (!StringUtils.hasText(en) || blankCount <= 0) {
			return null;
		}
		Matcher matcher = BLANK.matcher(en);
		StringBuilder sb = new StringBuilder();
		int index = 0;
		while (matcher.find() && index < blankCount) {
			matcher.appendReplacement(sb, "__" + CIRCLED[Math.min(index, CIRCLED.length - 1)] + "__");
			index++;
		}
		matcher.appendTail(sb);
		return index == blankCount ? sb.toString() : null;
	}

	private String normalizeSentence(String value) {
		if (value == null) {
			return "";
		}
		String text = value.toLowerCase(Locale.ROOT)
				.replace('’', '\'')
				.replace('‘', '\'')
				.trim()
				.replaceAll("\\s+", " ")
				.replaceAll("[.!?。！？]+$", "");
		return text;
	}

	private String normalizeWord(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
				.replaceAll("^[\"'“”‘’]+|[\"'“”‘’]+$", "");
	}

	private String normalizeKnowledgePoint(String knowledgePoint) {
		if (!StringUtils.hasText(knowledgePoint)) {
			return knowledgePoint;
		}
		String text = knowledgePoint.replaceAll("情景标题\\d*[：:]\\s*", "");
		return text.replaceAll("情景标题[（(]([^）)]*)[）)]", "$1");
	}

	private String groupKey(Q q) {
		return q.tag.getModule() + "|" + q.tag.getKnowledgePoint();
	}

	/**
	 * 组卷候选题：题目正文 + 标准答案 + 标签。
	 */
	private static class Q {

		private EnglishContentTag tag;

		private String module;

		private String title;

		private SurveySchema.QuestionType type;

		/** 句子题型：choice / build / dictation（单词与句式为 null） */
		private String kind;

		private List<String> options;

		private List<String> answers;

		private String answer;

		private String analysis;

		private List<String> correctAnswers() {
			if (SurveySchema.QuestionType.MultipleBlank.equals(type)) {
				return answers == null ? Collections.emptyList() : new ArrayList<>(answers);
			}
			return StringUtils.hasText(answer) ? List.of(answer) : Collections.emptyList();
		}

	}

	/**
	 * 同一模块 + 知识点的题目组。
	 */
	private static class Group {

		private String key;

		private List<Q> items;

		private Integer priority = 2;

	}

}
