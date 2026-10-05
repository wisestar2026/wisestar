package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.core.uitls.AnswerJudgeUtil;
import cn.wisestar.server.core.uitls.KnowledgeValueNormalizer;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.dto.detect.DetectGenerateRequest;
import cn.wisestar.server.domain.dto.detect.DetectRecordView;
import cn.wisestar.server.domain.dto.detect.DetectReportView;
import cn.wisestar.server.domain.dto.detect.DetectSubmitRequest;
import cn.wisestar.server.domain.dto.detect.DetectUnitView;
import cn.wisestar.server.domain.dto.growth.GrowthEventContext;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.domain.dto.student.StudentWeakView;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.ChapterRepo;
import cn.wisestar.server.domain.model.DetectRecord;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.SectionRepo;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.ChapterRepoMapper;
import cn.wisestar.server.mapper.DetectRecordMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.SectionRepoMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.service.DetectionService;
import cn.wisestar.server.service.EvaluationService;
import cn.wisestar.server.service.GrowthArchiveService;
import cn.wisestar.server.service.StudentArchiveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学员端知识点检测服务实现。
 *
 * <p>检测直接基于题目表（t_template）的学科/年级/章节/难度/知识点标签取题与判分，
 * 不写练习记录、不发放奖励；判分复用 {@link AnswerJudgeUtil}，与练习/试炼语义一致。</p>
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionServiceImpl implements DetectionService {

	private static final int DEFAULT_COUNT = 10;

	private static final int MAX_COUNT = 50;

	private final SubjectMapper subjectMapper;

	private final ChapterMapper chapterMapper;

	private final TemplateMapper templateMapper;

	private final ChapterRepoMapper chapterRepoMapper;

	private final SectionMapper sectionMapper;

	private final SectionRepoMapper sectionRepoMapper;

	private final DetectRecordMapper detectRecordMapper;

	private final EvaluationService evaluationService;

	private final GrowthArchiveService growthArchiveService;

	private final StudentArchiveService studentArchiveService;

	private final ObjectMapper objectMapper;

	@Override
	public List<DetectUnitView> units(String subjectId, String grade, String term) {
		if (!StringUtils.hasText(subjectId)) {
			return Collections.emptyList();
		}
		Subject subject = subjectMapper.selectById(subjectId);
		if (subject == null) {
			return Collections.emptyList();
		}
		String termKey = KnowledgeValueNormalizer.term(term);
		List<Chapter> chapters = chapterMapper.selectList(Wrappers.<Chapter>lambdaQuery()
				.eq(Chapter::getSubjectId, subjectId)
				.eq(StringUtils.hasText(grade), Chapter::getGrade, grade)
				.orderByAsc(Chapter::getSort)
				.orderByAsc(Chapter::getName));
		List<DetectUnitView> result = new ArrayList<>();
		if (chapters.isEmpty()) {
			return result;
		}
		Map<String, List<String>> repoMap = repoIdsByChapter(
				chapters.stream().map(Chapter::getId).collect(Collectors.toList()));
		for (Chapter chapter : chapters) {
			if (termKey != null && !termKey.equals(KnowledgeValueNormalizer.term(chapter.getTerm()))) {
				continue;
			}
			// 题目按「章节 → 题库绑定」精确取数：t_template 无册别列，仅靠章节名会串册，
			// 故优先用该章节绑定的题库 ID 圈定范围；无绑定时回退到「学科 + 章节名（可选年级）」
			List<String> repoIds = repoMap.get(chapter.getId());
			LambdaQueryWrapper<Template> countWrapper = Wrappers.<Template>lambdaQuery()
					.eq(Template::getSubject, subject.getName())
					.eq(Template::getChapter, chapter.getName());
			if (repoIds != null && !repoIds.isEmpty()) {
				countWrapper.in(Template::getRepoId, repoIds);
			}
			else {
				countWrapper.eq(StringUtils.hasText(grade), Template::getGrade, grade);
			}
			Long count = templateMapper.selectCount(countWrapper);
			int questionCount = count == null ? 0 : count.intValue();
			if (questionCount <= 0) {
				continue;
			}
			DetectUnitView view = new DetectUnitView();
			view.setId(chapter.getId());
			view.setName(chapter.getName());
			view.setGrade(chapter.getGrade());
			view.setTerm(chapter.getTerm());
			view.setQuestionCount(questionCount);
			result.add(view);
		}
		return result;
	}

	@Override
	public List<StudentQuestionView> generate(DetectGenerateRequest request) {
		if (request == null || request.getChapterIds() == null || request.getChapterIds().isEmpty()) {
			return Collections.emptyList();
		}
		Subject subject = subjectMapper.selectById(request.getSubjectId());
		if (subject == null) {
			return Collections.emptyList();
		}
		List<Chapter> chapters = chapterMapper.selectBatchIds(request.getChapterIds());
		Set<String> chapterNames = chapters.stream().map(Chapter::getName)
				.filter(StringUtils::hasText).collect(Collectors.toSet());
		if (chapterNames.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<Template> wrapper = Wrappers.<Template>lambdaQuery()
				.eq(Template::getSubject, subject.getName())
				.in(Template::getChapter, chapterNames);
		// 用章节绑定的题库 ID 精确圈定题目，避免同名章节跨册串题；无绑定时回退年级过滤
		Map<String, List<String>> repoMap = repoIdsByChapter(
				chapters.stream().map(Chapter::getId).collect(Collectors.toList()));
		Set<String> repoIds = repoMap.values().stream().flatMap(List::stream)
				.filter(StringUtils::hasText).collect(Collectors.toSet());
		if (!repoIds.isEmpty()) {
			wrapper.in(Template::getRepoId, repoIds);
		}
		else {
			wrapper.eq(StringUtils.hasText(request.getGrade()), Template::getGrade, request.getGrade());
		}
		List<Template> all = templateMapper.selectList(wrapper);
		if (all.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> types = request.getTypes();
		if (types != null && !types.isEmpty()) {
			all = all.stream()
					.filter(t -> t.getQuestionType() != null && types.contains(t.getQuestionType().name()))
					.collect(Collectors.toList());
		}
		if (all.isEmpty()) {
			return Collections.emptyList();
		}
		int limit = request.getQuestionCount() == null ? DEFAULT_COUNT
				: Math.max(1, Math.min(request.getQuestionCount(), MAX_COUNT));
		String difficulty = request.getDifficulty();
		Random random = new Random();
		Collections.shuffle(all, random);
		List<Template> ordered;
		if (StringUtils.hasText(difficulty)) {
			// 优先命中难度，不足时用其他难度补齐题量
			ordered = new ArrayList<>(all.size());
			all.stream().filter(t -> difficulty.equals(t.getDifficulty())).forEach(ordered::add);
			all.stream().filter(t -> !difficulty.equals(t.getDifficulty())).forEach(ordered::add);
		}
		else {
			ordered = all;
		}
		return ordered.stream().limit(limit).map(this::toView).collect(Collectors.toList());
	}

	@Override
	public DetectReportView submit(DetectSubmitRequest request) {
		DetectReportView report = new DetectReportView();
		if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
			return report;
		}
		List<String> ids = request.getItems().stream().map(DetectSubmitRequest.Item::getQuestionId)
				.filter(StringUtils::hasText).distinct().collect(Collectors.toList());
		if (ids.isEmpty()) {
			return report;
		}
		Map<String, Template> templates = templateMapper.selectBatchIds(ids).stream()
				.collect(Collectors.toMap(Template::getId, t -> t, (a, b) -> a));

		Map<String, int[]> chapterAgg = new LinkedHashMap<>();
		Map<String, int[]> kpAgg = new LinkedHashMap<>();
		Map<String, String> kpChapter = new LinkedHashMap<>();

		for (DetectSubmitRequest.Item item : request.getItems()) {
			Template template = templates.get(item.getQuestionId());
			if (template == null) {
				continue;
			}
			SurveySchema schema = template.getTemplate();
			Integer correct = AnswerJudgeUtil.evaluate(schema, item.getAnswer());
			boolean isCorrect = correct != null && correct == 1;
			String chapter = StringUtils.hasText(template.getChapter()) ? template.getChapter() : "未分类";
			String kp = firstKnowledgePoint(template, chapter);

			int[] ch = chapterAgg.computeIfAbsent(chapter, k -> new int[2]);
			ch[0] += 1;
			if (isCorrect) {
				ch[1] += 1;
			}
			String kpKey = chapter + "|" + kp;
			int[] kpStat = kpAgg.computeIfAbsent(kpKey, k -> new int[2]);
			kpStat[0] += 1;
			if (isCorrect) {
				kpStat[1] += 1;
			}
			kpChapter.put(kpKey, chapter);

			DetectReportView.Detail detail = new DetectReportView.Detail();
			detail.setQuestionId(template.getId());
			detail.setChapter(chapter);
			detail.setKnowledgePoint(kp);
			detail.setQuestionType(template.getQuestionType() == null ? null : template.getQuestionType().name());
			detail.setCorrect(correct);
			detail.setCorrectAnswers(AnswerJudgeUtil.extractCorrectAnswers(schema));
			detail.setStudentAnswer(AnswerJudgeUtil.formatAnswer(schema, item.getAnswer()));
			if (schema != null && schema.getAttribute() != null) {
				detail.setAnalysis(schema.getAttribute().getExamAnalysis());
			}
			report.getDetails().add(detail);
		}

		int total = report.getDetails().size();
		int correctCount = (int) report.getDetails().stream()
				.filter(d -> d.getCorrect() != null && d.getCorrect() == 1).count();
		report.setTotal(total);
		report.setCorrect(correctCount);
		report.setAccuracy(total == 0 ? 0 : Math.round(correctCount * 100f / total));

		chapterAgg.forEach((name, stat) -> {
			DetectReportView.ChapterStat cs = new DetectReportView.ChapterStat();
			cs.setName(name);
			cs.setTotal(stat[0]);
			cs.setCorrect(stat[1]);
			cs.setAccuracy(stat[0] == 0 ? 0 : Math.round(stat[1] * 100f / stat[0]));
			report.getChapterStats().add(cs);
		});

		List<DetectReportView.WeakPoint> weakPoints = new ArrayList<>();
		kpAgg.forEach((key, stat) -> {
			int wrong = stat[0] - stat[1];
			if (wrong <= 0) {
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
		// WPB 定基：落库检测记录并判定成长基线；失败不阻断诊断报告返回
		try {
			persistRecord(request, report);
			String studentId = SecurityContextUtils.getUserId();
			// 成长档案：检测轨迹留痕
			recordDetectGrowth(request, report, studentId);
			// 首次基线：一次性播种薄弱知识点（口径 a，此后检测不重写）并定格成长基线
			if (report.isBaseline()) {
				seedBaselineWeak(request.getSubjectId(), report);
				bindBaselineArchive(request, report, studentId);
			}
		}
		catch (Exception e) {
			log.warn("detect submit: persist record failed, ignored", e);
		}
		return report;
	}

	/** 成长档案：写入一条 DETECT 轨迹事件（幂等，失败不阻断）。 */
	private void recordDetectGrowth(DetectSubmitRequest request, DetectReportView report, String studentId) {
		if (!StringUtils.hasText(studentId) || !StringUtils.hasText(report.getRecordId())) {
			return;
		}
		try {
			GrowthEventContext ctx = new GrowthEventContext();
			ctx.setStudentId(studentId);
			ctx.setSubjectId(request.getSubjectId());
			ctx.setEventType("DETECT");
			ctx.setSourceType("detect");
			ctx.setSourceId(report.getRecordId());
			ctx.setQuestionCount(report.getTotal());
			ctx.setCorrectCount(report.getCorrect());
			ctx.setAccuracy(report.getAccuracy());
			ctx.setDurationMs(request.getDurationMs());
			ctx.setChapter(request.getChapterNames() == null ? null
					: String.join("、", request.getChapterNames()));
			List<String> kpNames = report.getWeakPoints() == null ? new ArrayList<>()
					: report.getWeakPoints().stream().map(DetectReportView.WeakPoint::getName)
							.filter(StringUtils::hasText).distinct().collect(Collectors.toList());
			ctx.setKnowledgePoints(kpNames);
			ctx.setTitle(Boolean.TRUE.equals(report.isBaseline()) ? "学前检测" : "单元检测");
			growthArchiveService.record(ctx);
		}
		catch (Exception e) {
			log.warn("detect submit: record growth failed, ignored", e);
		}
	}

	/** 成长档案：把本次基线检测定格为「学员 + 学期 + 学科」档案的成长基线。 */
	private void bindBaselineArchive(DetectSubmitRequest request, DetectReportView report, String studentId) {
		if (!StringUtils.hasText(studentId) || !StringUtils.hasText(request.getSubjectId())
				|| !StringUtils.hasText(report.getRecordId())) {
			return;
		}
		try {
			String semester = StringUtils.hasText(request.getSemester()) ? request.getSemester()
					: StudentRewardConstants.currentSemester();
			List<StudentWeakView> weakList = new ArrayList<>();
			if (report.getWeakPoints() != null) {
				for (DetectReportView.WeakPoint wp : report.getWeakPoints()) {
					weakList.add(new StudentWeakView(null, wp.getName(), request.getSubjectId(), wp.getAccuracy()));
				}
			}
			studentArchiveService.bindBaseline(studentId, request.getSubjectId(), semester,
					report.getRecordId(), report.getAccuracy(), weakList);
		}
		catch (Exception e) {
			log.warn("detect submit: bind baseline archive failed, ignored", e);
		}
	}

	/**
	 * 基线薄弱点播种：取检测报告中答错的知识点（正确率 &lt; 薄弱阈值）写入薄弱系统。
	 */
	private void seedBaselineWeak(String subjectId, DetectReportView report) {
		if (!StringUtils.hasText(subjectId) || report.getWeakPoints() == null
				|| report.getWeakPoints().isEmpty()) {
			return;
		}
		Map<String, Integer> kpMastery = new LinkedHashMap<>();
		for (DetectReportView.WeakPoint wp : report.getWeakPoints()) {
			if (StringUtils.hasText(wp.getName())) {
				kpMastery.putIfAbsent(wp.getName(), wp.getAccuracy());
			}
		}
		if (kpMastery.isEmpty()) {
			return;
		}
		String ref = report.getRecordId() == null ? "detect" : "detect:" + report.getRecordId();
		evaluationService.seedWeakFromBaseline(SecurityContextUtils.getUserId(), subjectId, ref, kpMastery);
	}

	@Override
	public List<DetectRecordView> history(String studentId, String subjectId, String semester) {
		String uid = StringUtils.hasText(studentId) ? studentId : SecurityContextUtils.getUserId();
		if (!StringUtils.hasText(uid)) {
			return Collections.emptyList();
		}
		List<DetectRecord> records = detectRecordMapper.selectList(Wrappers.<DetectRecord>lambdaQuery()
				.eq(DetectRecord::getStudentId, uid)
				.eq(StringUtils.hasText(subjectId), DetectRecord::getSubjectId, subjectId)
				.eq(StringUtils.hasText(semester), DetectRecord::getSemester, semester)
				.orderByDesc(DetectRecord::getCreateAt));
		return records.stream().map(this::toRecordView).collect(Collectors.toList());
	}

	/**
	 * 落库一条检测记录并判定成长基线（第 12.7 节，幂等 + 并发兜底）。
	 *
	 * <p>基线判定：某学员某学科某学期无 PRE 记录时，本次标 PRE + {@code is_baseline=1}，
	 * 并写唯一键 {@code studentId:subjectId:semester}；否则为 STAGE。重复 {@code clientToken}
	 * 或并发冲突时回查既有记录返回，不再新增。</p>
	 */
	private void persistRecord(DetectSubmitRequest request, DetectReportView report) {
		String studentId = SecurityContextUtils.getUserId();
		if (!StringUtils.hasText(studentId)) {
			return;
		}
		// 客户端幂等：同 token 直接复用既有记录
		String clientToken = request.getClientToken();
		if (StringUtils.hasText(clientToken)) {
			DetectRecord existing = detectRecordMapper.selectOne(Wrappers.<DetectRecord>lambdaQuery()
					.eq(DetectRecord::getStudentId, studentId)
					.eq(DetectRecord::getClientToken, clientToken).last("limit 1"));
			if (existing != null) {
				applyMeta(report, existing);
				return;
			}
		}
		String subjectId = request.getSubjectId();
		String semester = StringUtils.hasText(request.getSemester()) ? request.getSemester()
				: StudentRewardConstants.currentSemester();
		String detectType = "STAGE";
		boolean baseline = false;
		String baselineKey = null;
		if (StringUtils.hasText(subjectId) && StringUtils.hasText(semester)) {
			Long preCount = detectRecordMapper.selectCount(Wrappers.<DetectRecord>lambdaQuery()
					.eq(DetectRecord::getStudentId, studentId)
					.eq(DetectRecord::getSubjectId, subjectId)
					.eq(DetectRecord::getSemester, semester)
					.eq(DetectRecord::getDetectType, "PRE"));
			if (preCount == null || preCount == 0) {
				detectType = "PRE";
				baseline = true;
				baselineKey = studentId + ":" + subjectId + ":" + semester;
			}
		}
		DetectRecord record = new DetectRecord();
		record.setStudentId(studentId);
		record.setSubjectId(subjectId);
		Subject subject = StringUtils.hasText(subjectId) ? subjectMapper.selectById(subjectId) : null;
		record.setSubjectName(subject == null ? null : subject.getName());
		record.setGrade(request.getGrade());
		record.setTerm(request.getTerm());
		record.setSemester(semester);
		record.setDetectType(detectType);
		record.setIsBaseline(baseline);
		record.setBaselineKey(baselineKey);
		record.setChapterIds(toJson(request.getChapterIds()));
		record.setChapterNames(toJson(request.getChapterNames()));
		record.setQuestionCount(request.getQuestionCount());
		record.setTotal(report.getTotal());
		record.setCorrectCount(report.getCorrect());
		record.setAccuracy(report.getAccuracy());
		record.setDurationMs(request.getDurationMs());
		record.setWeakPoints(toJson(report.getWeakPoints()));
		record.setDetails(toJson(report.getDetails()));
		record.setClientToken(clientToken);
		try {
			detectRecordMapper.insert(record);
		}
		catch (DuplicateKeyException dup) {
			// 并发冲突：回查既有记录（优先 clientToken，其次基线键）
			DetectRecord existing = null;
			if (StringUtils.hasText(clientToken)) {
				existing = detectRecordMapper.selectOne(Wrappers.<DetectRecord>lambdaQuery()
						.eq(DetectRecord::getStudentId, studentId)
						.eq(DetectRecord::getClientToken, clientToken).last("limit 1"));
			}
			if (existing == null && StringUtils.hasText(baselineKey)) {
				existing = detectRecordMapper.selectOne(Wrappers.<DetectRecord>lambdaQuery()
						.eq(DetectRecord::getStudentId, studentId)
						.eq(DetectRecord::getBaselineKey, baselineKey).last("limit 1"));
			}
			if (existing == null) {
				throw dup;
			}
			record = existing;
		}
		applyMeta(report, record);
	}

	private void applyMeta(DetectReportView report, DetectRecord record) {
		report.setRecordId(record.getId());
		report.setDetectType(record.getDetectType());
		report.setBaseline(Boolean.TRUE.equals(record.getIsBaseline()));
	}

	private DetectRecordView toRecordView(DetectRecord record) {
		DetectRecordView view = new DetectRecordView();
		view.setId(record.getId());
		view.setSubjectId(record.getSubjectId());
		view.setSubjectName(record.getSubjectName());
		view.setGrade(record.getGrade());
		view.setTerm(record.getTerm());
		view.setSemester(record.getSemester());
		view.setDetectType(record.getDetectType());
		view.setBaseline(Boolean.TRUE.equals(record.getIsBaseline()));
		view.setQuestionCount(record.getQuestionCount());
		view.setTotal(record.getTotal());
		view.setCorrect(record.getCorrectCount());
		view.setAccuracy(record.getAccuracy());
		view.setDurationMs(record.getDurationMs());
		view.setCreateTime(record.getCreateAt());
		List<String> names = fromJson(record.getChapterNames(), new TypeReference<List<String>>() {
		});
		if (names != null) {
			view.setChapterNames(names);
		}
		List<DetectReportView.WeakPoint> weak = fromJson(record.getWeakPoints(),
				new TypeReference<List<DetectReportView.WeakPoint>>() {
				});
		if (weak != null) {
			view.setWeakPoints(weak);
		}
		return view;
	}

	private String toJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (Exception e) {
			log.warn("detect record json serialize failed", e);
			return null;
		}
	}

	private <T> T fromJson(String json, TypeReference<T> type) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			return objectMapper.readValue(json, type);
		}
		catch (Exception e) {
			log.warn("detect record json parse failed", e);
			return null;
		}
	}

	/**
	 * 批量查询「章节 → 绑定的题库 ID 列表」映射（用于把题目精确圈定到册别/单元）。
	 *
	 * <p>章节与题库存在两级绑定：章节级（t_chapter_repo，英语等）与小节级
	 * （t_section_repo，数学等按小节绑定）。此处取并集，兼顾两类内容组织方式；
	 * 无任何绑定时返回空，调用方回退到「学科 + 章节名（可选年级）」精确匹配。</p>
	 */
	private Map<String, List<String>> repoIdsByChapter(Collection<String> chapterIds) {
		if (chapterIds == null || chapterIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, Set<String>> map = new HashMap<>();
		// 章节级绑定
		chapterRepoMapper.selectList(Wrappers.<ChapterRepo>lambdaQuery()
						.in(ChapterRepo::getChapterId, chapterIds))
				.forEach(b -> addRepo(map, b.getChapterId(), b.getRepoId()));
		// 小节级绑定
		List<Section> sections = sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
				.in(Section::getChapterId, chapterIds));
		if (!sections.isEmpty()) {
			Map<String, String> sectionChapter = sections.stream()
					.filter(s -> StringUtils.hasText(s.getId()) && StringUtils.hasText(s.getChapterId()))
					.collect(Collectors.toMap(Section::getId, Section::getChapterId, (a, b) -> a));
			if (!sectionChapter.isEmpty()) {
				Map<String, List<String>> reposBySection = sectionRepoMapper.selectList(
								Wrappers.<SectionRepo>lambdaQuery()
										.in(SectionRepo::getSectionId, sectionChapter.keySet()))
						.stream().filter(b -> StringUtils.hasText(b.getRepoId()))
						.collect(Collectors.groupingBy(SectionRepo::getSectionId,
								Collectors.mapping(SectionRepo::getRepoId, Collectors.toList())));
				reposBySection.forEach((sectionId, repos) -> {
					String chapterId = sectionChapter.get(sectionId);
					if (chapterId != null) {
						repos.forEach(repoId -> addRepo(map, chapterId, repoId));
					}
				});
			}
		}
		Map<String, List<String>> result = new HashMap<>();
		map.forEach((chapterId, repoIds) -> result.put(chapterId, new ArrayList<>(repoIds)));
		return result;
	}

	private void addRepo(Map<String, Set<String>> map, String chapterId, String repoId) {
		if (StringUtils.hasText(chapterId) && StringUtils.hasText(repoId)) {
			map.computeIfAbsent(chapterId, k -> new HashSet<>()).add(repoId);
		}
	}

	/**
	 * 题目首级知识点标签：优先顶层 knowledge_point 数组，其次题干快照，最后回退单元名。
	 */
	private String firstKnowledgePoint(Template template, String fallback) {
		String[] kps = template.getKnowledgePoint();
		if (kps != null && kps.length > 0 && StringUtils.hasText(kps[0])) {
			return kps[0];
		}
		SurveySchema schema = template.getTemplate();
		if (schema != null && schema.getAttribute() != null
				&& schema.getAttribute().getKnowledgePoint() != null
				&& !schema.getAttribute().getKnowledgePoint().isEmpty()) {
			String kp = schema.getAttribute().getKnowledgePoint().get(0);
			if (StringUtils.hasText(kp)) {
				return kp;
			}
		}
		return fallback;
	}

	/**
	 * 题目转学员端视图（剥离标准答案与解析，防作弊）。
	 */
	private StudentQuestionView toView(Template template) {
		StudentQuestionView view = new StudentQuestionView();
		view.setId(template.getId());
		view.setName(template.getName());
		view.setQuestionType(template.getQuestionType());
		view.setTag(template.getTag());
		SurveySchema schema = template.getTemplate();
		if (schema != null) {
			schema = schema.deepCopy();
			strip(schema);
		}
		view.setSchema(schema);
		return view;
	}

	/**
	 * 递归清除标准答案与解析。
	 */
	private void strip(SurveySchema schema) {
		if (schema.getAttribute() != null) {
			schema.getAttribute().setExamCorrectAnswer(null);
			schema.getAttribute().setExamAnalysis(null);
		}
		if (schema.getChildren() != null) {
			schema.getChildren().forEach(this::strip);
		}
	}

}
