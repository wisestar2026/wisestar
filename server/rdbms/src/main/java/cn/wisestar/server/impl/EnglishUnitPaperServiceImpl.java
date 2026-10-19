package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.dto.detect.DetectReportView;
import cn.wisestar.server.domain.dto.english.EnglishDiagnosePaperView;
import cn.wisestar.server.domain.dto.english.EnglishDiagnoseRequest;
import cn.wisestar.server.domain.dto.english.EnglishDiagnoseSubmitRequest;
import cn.wisestar.server.domain.dto.english.EnglishSentenceExercise;
import cn.wisestar.server.domain.dto.english.EnglishUnitPaperRequest;
import cn.wisestar.server.domain.dto.english.EnglishUnitPaperSubmitRequest;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.DetectRecord;
import cn.wisestar.server.domain.model.EnglishContentTag;
import cn.wisestar.server.domain.model.EnglishScenarioPack;
import cn.wisestar.server.domain.model.EnglishSentencePack;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.domain.model.UserWeakKnowledge;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.DetectRecordMapper;
import cn.wisestar.server.mapper.EnglishContentTagMapper;
import cn.wisestar.server.mapper.EnglishScenarioPackMapper;
import cn.wisestar.server.mapper.EnglishSentencePackMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.SubjectMapper;
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
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

	/** 诊断卷每档题量（三档共 24 题，覆盖更多单元与知识点） */
	private static final int DIAGNOSE_PER_LEVEL = 8;

	private static final Map<String, String> LEVEL_LABELS = Map.of("D1", "基础", "D2", "进阶", "D3", "挑战");

	private static final DateTimeFormatter TREND_DATE = DateTimeFormatter.ofPattern("MM-dd");

	private static final List<String> LEVELS = List.of("D1", "D2", "D3");

	private static final List<String> MODULES = List.of("单词", "句式", "语法");

	private static final Pattern BLANK = Pattern.compile("_{3,}");

	private final EnglishContentTagMapper tagMapper;

	private final EnglishSentencePackMapper sentencePackMapper;

	private final EnglishScenarioPackMapper scenarioPackMapper;

	private final ChapterMapper chapterMapper;

	private final UserWeakKnowledgeMapper userWeakKnowledgeMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	private final EnglishPracticeEvaluator practiceEvaluator;

	private final ObjectMapper objectMapper;

	private final DetectRecordMapper detectRecordMapper;

	private final SubjectMapper subjectMapper;

	private final EnglishFusionQuestionFactory fusionQuestionFactory;

	@Override
	public List<StudentQuestionView> generate(EnglishUnitPaperRequest request) {
		if (request == null || !StringUtils.hasText(request.getUnit())) {
			return Collections.emptyList();
		}
		List<Q> all = loadQuestions(request.getVersion(), request.getGrade(), request.getTerm(), request.getUnit());
		if (StringUtils.hasText(request.getModule())) {
			all.removeIf(q -> !request.getModule().equals(q.module));
		}
		if (all.isEmpty()) {
			return Collections.emptyList();
		}
		int limit = request.getQuestionCount() == null ? DEFAULT_COUNT
				: Math.max(1, Math.min(request.getQuestionCount(), MAX_COUNT));
		Map<String, Integer> groupPriority = groupPriority(request, all);
		Map<String, Deque<Group>> queues = groupQueues(all, groupPriority);
		List<Q> paper = new ArrayList<>(limit);
		// 单元测试按「单词 → 句式 → 语法」固定路径组卷；模块内部仍按薄弱点与 D1~D3 阶梯取题。
		List<String> order = new ArrayList<>(MODULES);
		order.retainAll(queues.keySet());
		int base = order.isEmpty() ? 0 : limit / order.size();
		int remainder = order.isEmpty() ? 0 : limit % order.size();
		int carry = 0;
		for (int i = 0; i < order.size() && paper.size() < limit; i++) {
			Deque<Group> queue = queues.get(order.get(i));
			if (queue == null) {
				continue;
			}
			int slots = base + (i < remainder ? 1 : 0) + carry;
			int target = Math.min(limit, paper.size() + slots);
			while (paper.size() < target && !queue.isEmpty()) {
				paper.addAll(ladder(queue.poll(), target - paper.size()));
			}
			carry = Math.max(0, target - paper.size());
		}
		// 某模块题量不足时，用后置模块或同模块剩余题目补足。
		while (paper.size() < limit) {
			boolean added = false;
			for (String module : order) {
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
		if (request == null || !StringUtils.hasText(request.getUnit())
				|| request.getItems() == null || request.getItems().isEmpty()) {
			return new DetectReportView();
		}
		Map<String, Q> questions = loadQuestions(request.getVersion(), request.getGrade(), request.getTerm(),
				request.getUnit()).stream().collect(Collectors.toMap(q -> q.tag.getContentKey(), q -> q, (a, b) -> a));
		return buildReport(request.getVersion(), request.getGrade(), request.getTerm(), questions,
				request.getItems(), Collections.emptyMap(), request.getUnit(), true);
	}

	@Override
	public EnglishDiagnosePaperView diagnosePaper(EnglishDiagnoseRequest request) {
		EnglishDiagnosePaperView paper = new EnglishDiagnosePaperView();
		if (request == null) {
			return paper;
		}
		List<String> units = diagnoseUnits(request);
		if (units.isEmpty()) {
			return paper;
		}
		List<Q> all = loadQuestions(request.getVersion(), request.getGrade(), request.getTerm(), units);
		if (all.isEmpty()) {
			return paper;
		}
		paper.setTitle("英语学习诊断");
		String userId = SecurityContextUtils.getUserId();
		long seed = (userId == null ? 0 : userId.hashCode()) * 31L
				+ java.time.LocalDate.now().toString().hashCode();
		Set<String> used = new LinkedHashSet<>();
		for (String level : LEVELS) {
			List<Q> picked = pickLevel(all, level, DIAGNOSE_PER_LEVEL, used, seed);
			if (picked.isEmpty()) {
				continue;
			}
			EnglishDiagnosePaperView.Section section = new EnglishDiagnosePaperView.Section();
			section.setLevel(level);
			section.setLabel(LEVEL_LABELS.getOrDefault(level, level));
			section.setQuestions(picked.stream().map(this::toView).collect(Collectors.toList()));
			paper.getSections().add(section);
		}
		return paper;
	}

	@Override
	public DetectReportView diagnoseSubmit(EnglishDiagnoseSubmitRequest request) {
		DetectReportView report = new DetectReportView();
		if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
			return report;
		}
		List<String> keys = request.getItems().stream()
				.map(EnglishUnitPaperSubmitRequest.Item::getContentKey)
				.filter(StringUtils::hasText).distinct().collect(Collectors.toList());
		if (keys.isEmpty()) {
			return report;
		}
		List<EnglishContentTag> tags = tagMapper.selectList(Wrappers.<EnglishContentTag>lambdaQuery()
				.in(EnglishContentTag::getContentKey, keys));
		if (tags.isEmpty()) {
			return report;
		}
		Map<String, String> chapterByKey = tags.stream().collect(Collectors.toMap(
				EnglishContentTag::getContentKey, EnglishContentTag::getUnit, (a, b) -> a));
		List<String> units = chapterByKey.values().stream().filter(StringUtils::hasText)
				.distinct().collect(Collectors.toList());
		Map<String, Q> questions = loadQuestions(request.getVersion(), request.getGrade(), request.getTerm(),
				units).stream().collect(Collectors.toMap(q -> q.tag.getContentKey(), q -> q, (a, b) -> a));
		report = buildReport(request.getVersion(), request.getGrade(), request.getTerm(), questions,
				request.getItems(), chapterByKey, null, true);
		try {
			String userId = SecurityContextUtils.getUserId();
			persistDiagnose(request, report, units, userId);
			report.setTrend(diagnoseTrend(userId));
		}
		catch (Exception e) {
			log.warn("english diagnose: persist record failed, ignored", e);
		}
		return report;
	}

	/**
	 * 诊断卷可选单元：显式指定优先，否则取当前版本/年级/册别的全部单元。
	 */
	private List<String> diagnoseUnits(EnglishDiagnoseRequest request) {
		if (request.getUnits() != null && !request.getUnits().isEmpty()) {
			return request.getUnits().stream().filter(StringUtils::hasText).map(String::trim)
					.distinct().collect(Collectors.toList());
		}
		List<EnglishContentTag> tags = tagMapper.selectList(Wrappers.<EnglishContentTag>lambdaQuery()
				.select(EnglishContentTag::getUnit)
				.eq(StringUtils.hasText(request.getVersion()), EnglishContentTag::getVersion, request.getVersion())
				.eq(StringUtils.hasText(request.getGrade()), EnglishContentTag::getGrade, request.getGrade())
				.eq(StringUtils.hasText(request.getTerm()), EnglishContentTag::getTerm, request.getTerm())
				.eq(EnglishContentTag::getIsExamQuestion, 1));
		return tags.stream().map(EnglishContentTag::getUnit).filter(StringUtils::hasText).map(String::trim)
				.distinct().sorted().collect(Collectors.toList());
	}

	/**
	 * 按难度档取题：优先命中该档，题量不足用最近难度补位；同档内按单元轮转，
	 * 让诊断卷覆盖更多单元，避免集中在同一单元。
	 */
	private List<Q> pickLevel(List<Q> all, String level, int count, Set<String> used, long seed) {
		List<Q> pool = all.stream()
				.filter(q -> !used.contains(q.tag.getContentKey()))
				.filter(q -> level.equalsIgnoreCase(q.tag.getDifficultySeed()))
				.collect(Collectors.toList());
		Collections.shuffle(pool, new Random(seed + level.hashCode()));
		List<Q> ordered = new ArrayList<>(pool);
		List<Q> rest = all.stream()
				.filter(q -> !used.contains(q.tag.getContentKey()) && !ordered.contains(q))
				.sorted((a, b) -> Integer.compare(
						Math.abs(difficultyRank(a.tag.getDifficultySeed()) - difficultyRank(level)),
						Math.abs(difficultyRank(b.tag.getDifficultySeed()) - difficultyRank(level))))
				.collect(Collectors.toList());
		ordered.addAll(rest);
		List<Q> picked = new ArrayList<>(count);
		Set<String> pickedUnits = new LinkedHashSet<>();
		// 第一轮优先取未出现过的单元，第二轮放开单元限制补足题量
		for (int round = 0; round < 2 && picked.size() < count; round++) {
			for (Q q : ordered) {
				if (picked.size() >= count) {
					break;
				}
				if (picked.contains(q)) {
					continue;
				}
				String unit = q.tag.getUnit();
				if (round == 0 && !pickedUnits.add(unit)) {
					continue;
				}
				picked.add(q);
			}
		}
		picked.forEach(q -> used.add(q.tag.getContentKey()));
		return picked;
	}

	/**
	 * 统一报告聚合：逐题三态判分，输出章节/知识点/模块/能力维度统计，并按需入账学情。
	 */
	private DetectReportView buildReport(String version, String grade, String term, Map<String, Q> questions,
			List<EnglishUnitPaperSubmitRequest.Item> items, Map<String, String> chapterByKey,
			String defaultChapter, boolean recordEvaluation) {
		DetectReportView report = new DetectReportView();
		Map<String, int[]> chapterAgg = new LinkedHashMap<>();
		Map<String, int[]> kpAgg = new LinkedHashMap<>();
		Map<String, String> kpChapter = new LinkedHashMap<>();
		Map<String, int[]> moduleAgg = new LinkedHashMap<>();
		Map<String, List<Integer>> evalScores = new LinkedHashMap<>();
		Map<String, int[]> skillAgg = new LinkedHashMap<>();
		Map<String, int[]> cognitiveAgg = new LinkedHashMap<>();
		Map<String, int[]> typeAgg = new LinkedHashMap<>();
		Map<String, int[]> difficultyAgg = new LinkedHashMap<>();
		for (EnglishUnitPaperSubmitRequest.Item item : items) {
			Q q = questions.get(item.getContentKey());
			if (q == null) {
				continue;
			}
			String chapter = chapterByKey.getOrDefault(q.tag.getContentKey(), defaultChapter);
			if (!StringUtils.hasText(chapter)) {
				chapter = q.tag.getUnit();
			}
			EnglishBehaviorJudge.Mastery mastery = behavior(q, item);
			int score = mastery.getScore();
			DetectReportView.Detail detail = new DetectReportView.Detail();
			detail.setQuestionId(q.tag.getContentKey());
			detail.setChapter(chapter);
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

			int[] ch = chapterAgg.computeIfAbsent(chapter, k -> new int[3]);
			ch[0] += 1;
			ch[2] += score;
			if (mastery == EnglishBehaviorJudge.Mastery.MASTERED) {
				ch[1] += 1;
			}
			String kpKey = chapter + "|" + q.tag.getKnowledgePoint();
			int[] kp = kpAgg.computeIfAbsent(kpKey, k -> new int[3]);
			kp[0] += 1;
			kp[2] += score;
			if (mastery == EnglishBehaviorJudge.Mastery.MASTERED) {
				kp[1] += 1;
			}
			kpChapter.put(kpKey, chapter);
			int[] module = moduleAgg.computeIfAbsent(q.tag.getModule(), k -> new int[3]);
			module[0] += 1;
			module[2] += score;
			if (mastery == EnglishBehaviorJudge.Mastery.MASTERED) {
				module[1] += 1;
			}
			evalScores.computeIfAbsent(chapter + "|" + q.tag.getModule(), k -> new ArrayList<>()).add(score);
			accumulate(skillAgg, q.tag.getLanguageSkill(), mastery, score);
			accumulate(cognitiveAgg, q.tag.getCognitiveLevel(), mastery, score);
			accumulate(typeAgg, q.tag.getQuestionType(), mastery, score);
			accumulate(difficultyAgg, q.tag.getDifficultySeed(), mastery, score);
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

		chapterAgg.forEach((name, stat) -> {
			DetectReportView.ChapterStat chapterStat = new DetectReportView.ChapterStat();
			chapterStat.setName(name);
			chapterStat.setTotal(stat[0]);
			chapterStat.setCorrect(stat[1]);
			chapterStat.setAccuracy(stat[0] == 0 ? 0 : Math.round(stat[1] * 100f / stat[0]));
			report.getChapterStats().add(chapterStat);
		});

		List<DetectReportView.WeakPoint> weakPoints = new ArrayList<>();
		kpAgg.forEach((key, stat) -> {
			int wrong = stat[0] - stat[1];
			// 与薄弱阈值对齐：加权得分（掌握 100 / 不稳定 60 / 漏洞 0）低于阈值才列为薄弱
			if (wrong <= 0 || stat[2] >= EvaluationServiceImpl.WEAK_THRESHOLD * stat[0]) {
				return;
			}
			DetectReportView.WeakPoint wp = new DetectReportView.WeakPoint();
			wp.setName(key.substring(key.indexOf('|') + 1));
			wp.setChapter(kpChapter.get(key));
			wp.setTotal(stat[0]);
			wp.setCorrect(stat[1]);
			wp.setWrong(wrong);
			wp.setAccuracy(stat[0] == 0 ? 0 : Math.round(stat[1] * 100f / stat[0]));
			weakPoints.add(wp);
		});
		weakPoints.sort((a, b) -> Integer.compare(a.getAccuracy(), b.getAccuracy()));
		report.setWeakPoints(weakPoints);
		report.setModuleStats(dimensionStats(moduleAgg));
		report.setSkillStats(dimensionStats(skillAgg));
		report.setCognitiveStats(dimensionStats(cognitiveAgg));
		report.setTypeStats(dimensionStats(typeAgg));
		report.setDifficultyStats(dimensionStats(difficultyAgg));

		if (recordEvaluation) {
			String userId = SecurityContextUtils.getUserId();
			evalScores.forEach((key, scores) -> {
				int split = key.indexOf('|');
				if (split <= 0 || split >= key.length() - 1) {
					return;
				}
				practiceEvaluator.recordScores(userId, version, grade, term,
						key.substring(0, split), key.substring(split + 1), scores);
			});
		}
		return report;
	}

	/**
	 * 落学习诊断记录（detect_type=DIAG），学习趋势据此聚合。
	 */
	private void persistDiagnose(EnglishDiagnoseSubmitRequest request, DetectReportView report,
			List<String> units, String userId) {
		if (!StringUtils.hasText(userId)) {
			return;
		}
		String clientToken = request.getClientToken();
		if (StringUtils.hasText(clientToken)) {
			DetectRecord existing = detectRecordMapper.selectOne(Wrappers.<DetectRecord>lambdaQuery()
					.eq(DetectRecord::getStudentId, userId)
					.eq(DetectRecord::getClientToken, clientToken).last("limit 1"));
			if (existing != null) {
				report.setRecordId(existing.getId());
				return;
			}
		}
		Subject subject = subjectMapper.selectOne(Wrappers.<Subject>lambdaQuery()
				.eq(Subject::getCode, "ENGLISH").last("limit 1"));
		DetectRecord record = new DetectRecord();
		record.setStudentId(userId);
		record.setSubjectId(subject == null ? null : subject.getId());
		record.setSubjectName(subject == null ? "英语" : subject.getName());
		record.setGrade(request.getGrade());
		record.setTerm(request.getTerm());
		record.setSemester(StudentRewardConstants.currentSemester());
		record.setDetectType("DIAG");
		record.setIsBaseline(false);
		record.setChapterNames(toJson(units));
		record.setQuestionCount(request.getItems() == null ? report.getTotal() : request.getItems().size());
		record.setTotal(report.getTotal());
		record.setCorrectCount(report.getCorrect());
		record.setAccuracy(report.getAccuracy());
		record.setDurationMs(request.getDurationMs());
		record.setWeakPoints(toJson(report.getWeakPoints()));
		record.setDetails(toJson(report.getDetails()));
		record.setClientToken(clientToken);
		detectRecordMapper.insert(record);
		report.setRecordId(record.getId());
		report.setDetectType("DIAG");
	}

	/**
	 * 学习趋势：最近 8 次诊断的加权得分与正确率（按时间升序）。
	 */
	private List<DetectReportView.TrendPoint> diagnoseTrend(String userId) {
		if (!StringUtils.hasText(userId)) {
			return Collections.emptyList();
		}
		Subject subject = subjectMapper.selectOne(Wrappers.<Subject>lambdaQuery()
				.eq(Subject::getCode, "ENGLISH").last("limit 1"));
		if (subject == null) {
			return Collections.emptyList();
		}
		List<DetectRecord> records = detectRecordMapper.selectList(Wrappers.<DetectRecord>lambdaQuery()
				.eq(DetectRecord::getStudentId, userId)
				.eq(DetectRecord::getSubjectId, subject.getId())
				.eq(DetectRecord::getDetectType, "DIAG")
				.orderByDesc(DetectRecord::getCreateAt)
				.last("limit 8"));
		Collections.reverse(records);
		List<DetectReportView.TrendPoint> trend = new ArrayList<>(records.size());
		for (DetectRecord record : records) {
			DetectReportView.TrendPoint point = new DetectReportView.TrendPoint();
			point.setAccuracy(record.getAccuracy() == null ? 0 : record.getAccuracy());
			point.setTotal(record.getTotal() == null ? 0 : record.getTotal());
			point.setMasteryScore(masteryScore(record.getDetails()));
			Date createAt = record.getCreateAt();
			point.setDate(createAt == null ? ""
					: createAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(TREND_DATE));
			trend.add(point);
		}
		return trend;
	}

	/**
	 * 序列化为 JSON，失败时返回 null（落库失败不阻断报告返回）。
	 */
	private String toJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (Exception e) {
			log.warn("english diagnose: serialize json failed, ignored", e);
			return null;
		}
	}

	/**
	 * 从历史诊断明细中重算加权得分（掌握 100 / 不稳定 60 / 漏洞 0）。
	 */
	private int masteryScore(String detailsJson) {
		if (!StringUtils.hasText(detailsJson)) {
			return 0;
		}
		try {
			List<DetectReportView.Detail> details = objectMapper.readValue(detailsJson,
					new TypeReference<List<DetectReportView.Detail>>() {
					});
			if (details.isEmpty()) {
				return 0;
			}
			int sum = details.stream().mapToInt(d -> d.getScore() == null ? 0 : d.getScore()).sum();
			return Math.round(sum / (float) details.size());
		}
		catch (Exception e) {
			log.warn("english diagnose: parse trend details failed, ignored", e);
			return 0;
		}
	}

	/**
	 * 维度聚合：total / 全对数 / 三态得分。
	 */
	private void accumulate(Map<String, int[]> agg, String name, EnglishBehaviorJudge.Mastery mastery,
			int score) {
		if (!StringUtils.hasText(name)) {
			return;
		}
		int[] stat = agg.computeIfAbsent(name.trim(), k -> new int[3]);
		stat[0] += 1;
		stat[2] += score;
		if (mastery == EnglishBehaviorJudge.Mastery.MASTERED) {
			stat[1] += 1;
		}
	}

	/**
	 * 维度聚合结果按题量降序输出。
	 */
	private List<DetectReportView.DimensionStat> dimensionStats(Map<String, int[]> agg) {
		List<DetectReportView.DimensionStat> stats = new ArrayList<>(agg.size());
		agg.forEach((name, stat) -> {
			DetectReportView.DimensionStat item = new DetectReportView.DimensionStat();
			item.setName(name);
			item.setTotal(stat[0]);
			item.setCorrect(stat[1]);
			item.setAccuracy(stat[0] == 0 ? 0 : Math.round(stat[1] * 100f / stat[0]));
			item.setMasteryScore(stat[0] == 0 ? 0 : Math.round(stat[2] / (float) stat[0]));
			stats.add(item);
		});
		stats.sort((a, b) -> Integer.compare(b.getTotal(), a.getTotal()));
		return stats;
	}

	/**
	 * 跨单元加载题目：诊断卷按单元循环读取后合并，供三档难度抽题。
	 */
	private List<Q> loadQuestions(String version, String grade, String term, List<String> units) {
		List<Q> result = new ArrayList<>();
		for (String unit : units) {
			result.addAll(loadQuestions(version, grade, term, unit));
		}
		return result;
	}

	/**
	 * 按内容键回读句子包/情景包，组装成可组卷的题目。
	 */
	private List<Q> loadQuestions(String version, String grade, String term, String unit) {
		Map<String, Q> sentence = loadSentenceQuestions(version, grade, term, unit);
		Map<String, Q> all = new HashMap<>(sentence);
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
				// 句子包统一归入「句式」板块，与句式改写、情景选原句一起组成句型路径。
				if ("句子".equals(tag.getModule())) {
					tag.setModule("句式");
				}
				q.tag = tag;
				q.module = tag.getModule();
				result.add(q);
			}
		}
		// 在前三板块之外补充由单元句子/语法例句即时派生的融合题：
		// 单词选词填空、句式原句挖空/换人称/情景选原句、语法变形。
		for (EnglishFusionQuestionFactory.DerivedQuestion item : fusionQuestionFactory.build(
				version, grade, term, unit)) {
			if (!all.containsKey(item.key()) && result.stream()
					.noneMatch(q -> item.key().equals(q.tag.getContentKey()))) {
				result.add(derivedQuestion(item, version, grade, term, unit));
			}
		}
		return result;
	}

	private Q derivedQuestion(EnglishFusionQuestionFactory.DerivedQuestion item, String version, String grade,
			String term, String unit) {
		Q q = new Q();
		EnglishContentTag tag = new EnglishContentTag();
		tag.setContentKey(item.key());
		tag.setModule(item.module());
		tag.setVersion(version);
		tag.setGrade(grade);
		tag.setTerm(term);
		tag.setUnit(unit);
		tag.setQuestionType(item.questionType());
		tag.setKnowledgePoint(item.knowledgePoint());
		tag.setLanguageSkill(item.languageSkill());
		tag.setCognitiveLevel(item.cognitiveLevel());
		tag.setDistractorStrength("无");
		tag.setDifficultySeed(item.difficulty());
		tag.setIsExamQuestion(1);
		q.tag = tag;
		q.module = item.module();
		q.title = item.title();
		q.type = item.type();
		q.answer = item.answer();
		q.answers = item.answers();
		q.options = item.options();
		q.analysis = item.analysis();
		return q;
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
					q.title = "连词成句，用所给单词连成句子\n"
							+ item.getZh() + "（" + wordBank(item) + "）";
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
					String title = normalizeKnowledgePoint(scene.path("title").asText());
					// 情景对话题干带上完整上下文：情景标题 + 本轮之前的全部台词（中英对照），
					// 否则只给作答轮会出现「He's very ___」这类无法判断所指的孤立句子。
					StringBuilder context = new StringBuilder("情景：").append(title);
					int turn = 0;
					for (JsonNode line : scene.path("lines")) {
						turn++;
						String speaker = line.path("speaker").asText("A");
						String en = line.path("en").asText();
						String zh = line.path("zh").asText();
						List<String> answers = new ArrayList<>();
						for (JsonNode answer : line.path("answers")) {
							answers.add(answer.asText());
						}
						if (!answers.isEmpty()) {
							String blanked = blankify(en, answers.size());
							if (StringUtils.hasText(blanked)) {
								Q q = new Q();
								// 单空按单项填空展示，多空按多项填空展示；空位一律用「_____」，
								// 答案有几个单词就出现几个空，不再使用 __①__ 编号标记。
								q.type = answers.size() == 1 ? SurveySchema.QuestionType.FillBlank
										: SurveySchema.QuestionType.MultipleBlank;
								q.answers = answers;
								if (answers.size() == 1) {
									q.answer = answers.get(0);
								}
								// 前文空位统一弱化为「……」，只有本轮作答行保留「_____」并用【作答】标出，
								// 避免整段对话里出现多个下划线、学员找不到该填哪一处。
								q.title = context + "\n【作答】" + speaker + ": " + blanked + "（" + maskContext(zh) + "）";
								q.analysis = zh;
								String key = "word:" + version + ":" + grade + ":" + term + ":" + unit + ":"
										+ groupNo + ":" + title + ":" + turn;
								result.put(key, q);
							}
						}
						// 已作答轮也以完整台词进入后续轮的上下文，保证对话连贯；
						// 其中历史空位统一显示为「……」，把下划线留给本轮真正要作答的空。
						context.append("\n").append(speaker).append(": ").append(maskContext(en));
						if (StringUtils.hasText(zh)) {
							context.append("（").append(maskContext(zh)).append("）");
						}
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
						// 正确答案不再固定放在首位：按内容稳定打乱候选项
						String key = "pattern:" + version + ":" + grade + ":" + term + ":" + unit + ":"
								+ groupNo + ":" + title + ":" + turn;
						String correct = options.get(answerIndex);
						int size = options.size();
						int offset = 1 + Math.floorMod((key + "|" + String.join("|", options)).hashCode(),
								size - 1);
						Collections.rotate(options, -offset);
						q.options = options;
						q.answer = correct;
						q.analysis = node.path("explanation").asText();
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
		List<Q> used = new ArrayList<>(Math.min(slots, group.items.size()));
		// 先按 D1→D2→D3 各取一道；同一知识点题量超过 3 道时继续按同一顺序补足，
		// 避免一个知识点只出 3 题导致整卷题量不足。
		while (used.size() < slots) {
			boolean added = false;
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
					added = true;
				}
				if (used.size() >= slots) {
					break;
				}
			}
			if (!added) {
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
		if (SurveySchema.QuestionType.MultipleBlank.equals(q.type) && q.answers != null) {
			schema.getAttribute().setBlankCount(q.answers.size());
		}
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
		return String.join(" 、 ", tokens);
	}

	/**
	 * 上下文台词中的历史空位弱化为「……」，只把本轮作答空保留为下划线。
	 */
	private String maskContext(String text) {
		return text == null ? "" : text.replaceAll("_{2,}", "……");
	}

	/**
	 * 把英文台词中的空位改写为「_____」，每个答案单词对应一个空，练习端据此识别空位数。
	 */
	private String blankify(String en, int blankCount) {
		if (!StringUtils.hasText(en) || blankCount <= 0) {
			return null;
		}
		Matcher matcher = BLANK.matcher(en);
		StringBuilder sb = new StringBuilder();
		int index = 0;
		while (matcher.find() && index < blankCount) {
			matcher.appendReplacement(sb, "_____");
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
