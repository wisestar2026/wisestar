package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.AnswerJudgeUtil;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.dto.WrongQuestionQuery;
import cn.wisestar.server.domain.dto.WrongQuestionView;
import cn.wisestar.server.domain.dto.math.MathPaperMetaView;
import cn.wisestar.server.domain.dto.math.MathPaperRequest;
import cn.wisestar.server.domain.dto.math.MathPaperStudentView;
import cn.wisestar.server.domain.dto.math.MathPaperView;
import cn.wisestar.server.domain.dto.student.StudentQuery;
import cn.wisestar.server.domain.dto.student.StudentView;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.service.MathPaperService;
import cn.wisestar.server.service.PracticeService;
import cn.wisestar.server.service.StudentService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数学组卷实现（后台管理端）。
 *
 * <p><b>取题口径</b>：数学题目以「学科 + 章节名 + 小节名 + 知识点名」直接标注，
 * 因此按名称圈定范围，忽略题库绑定差异；同名题目按「题型 + 题干」去重，
 * 避免同一道题被多个题库重复收录而在一张卷里出现两次。</p>
 *
 * <p><b>两次查询</b>：先只取标注列做筛选（题量大时避免整表 schema 反序列化），
 * 选出题目后再按 ID 取完整 schema，兼顾性能与答案/解析输出。</p>
 *
 * @author wisestar
 * @date 2026/10/26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MathPaperServiceImpl implements MathPaperService {

	/** 数学学科 ID */
	private static final String MATH_SUBJECT_ID = "1002";

	private static final int DEFAULT_COUNT = 20;

	private static final int MAX_COUNT = 60;

	private static final int STUDENT_WRONG_LIMIT = 1000;

	private static final String CIRCLED = "①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳";

	private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private static final List<String> GRADE_ORDER = Arrays.asList("一年级", "二年级", "三年级", "四年级", "五年级", "六年级");

	private static final Map<String, String> DIFFICULTY_LABELS = new LinkedHashMap<>();

	private static final Map<String, String> TYPE_LABELS = new LinkedHashMap<>();

	private static final List<String> TYPE_ORDER = Arrays.asList("Radio", "Judge", "Select", "Checkbox", "FillBlank",
			"MultipleBlank", "Textarea", "Text", "Score");

	private static final Set<String> OPTION_TYPES = new HashSet<>(Arrays.asList("Radio", "Checkbox", "Select", "Judge"));

	static {
		DIFFICULTY_LABELS.put("easy", "简单");
		DIFFICULTY_LABELS.put("medium", "中等");
		DIFFICULTY_LABELS.put("hard", "困难");

		TYPE_LABELS.put("Radio", "单选题");
		TYPE_LABELS.put("Checkbox", "多选题");
		TYPE_LABELS.put("Select", "下拉选择");
		TYPE_LABELS.put("Judge", "判断题");
		TYPE_LABELS.put("FillBlank", "填空题");
		TYPE_LABELS.put("MultipleBlank", "多项填空");
		TYPE_LABELS.put("Textarea", "解答题");
		TYPE_LABELS.put("Text", "解答题");
		TYPE_LABELS.put("Score", "评分题");
	}

	private final TemplateMapper templateMapper;

	private final ChapterMapper chapterMapper;

	private final SectionMapper sectionMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	private final SubjectMapper subjectMapper;

	private final StudentMapper studentMapper;

	private final StudentService studentService;

	private final PracticeService practiceService;

	// ==================== 组卷配置元数据 ====================

	@Override
	public MathPaperMetaView meta() {
		String subjectName = mathSubjectName();
		List<Chapter> chapters = chapterMapper.selectList(Wrappers.<Chapter>lambdaQuery()
				.eq(Chapter::getSubjectId, MATH_SUBJECT_ID));
		chapters.sort(Comparator.comparingInt((Chapter c) -> gradeRank(c.getGrade()))
				.thenComparingInt(c -> termRank(c.getTerm()))
				.thenComparing(c -> c.getSort() == null ? Integer.MAX_VALUE : c.getSort()));

		List<Section> sections = chapters.isEmpty() ? Collections.emptyList()
				: sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
						.in(Section::getChapterId, chapters.stream().map(Chapter::getId).collect(Collectors.toList()))
						.orderByAsc(Section::getSort));
		Map<String, List<Section>> sectionsByChapter = sections.stream()
				.collect(Collectors.groupingBy(Section::getChapterId, LinkedHashMap::new, Collectors.toList()));

		Map<String, List<KnowledgePoint>> pointsBySection = new HashMap<>();
		if (!sections.isEmpty()) {
			List<KnowledgePoint> points = knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
					.in(KnowledgePoint::getSectionId,
							sections.stream().map(Section::getId).collect(Collectors.toList()))
					.orderByAsc(KnowledgePoint::getSort));
			pointsBySection = points.stream()
					.collect(Collectors.groupingBy(KnowledgePoint::getSectionId, LinkedHashMap::new,
							Collectors.toList()));
		}

		// 只取标注列统计题量，不拉取整题 schema
		List<Template> rows = templateMapper.selectList(Wrappers.<Template>lambdaQuery()
				.select(Template::getChapter, Template::getSection, Template::getDifficulty,
						Template::getQuestionType)
				.eq(Template::getSubject, subjectName));

		Map<String, Integer> chapterCounts = new HashMap<>();
		Map<String, Integer> sectionCounts = new HashMap<>();
		Map<String, Integer> difficultyCounts = new LinkedHashMap<>();
		Map<String, Integer> typeCounts = new LinkedHashMap<>();
		for (Template row : rows) {
			if (StringUtils.hasText(row.getChapter())) {
				chapterCounts.merge(row.getChapter(), 1, Integer::sum);
			}
			if (StringUtils.hasText(row.getChapter()) && StringUtils.hasText(row.getSection())) {
				sectionCounts.merge(sectionKey(row.getChapter(), row.getSection()), 1, Integer::sum);
			}
			if (StringUtils.hasText(row.getDifficulty())) {
				difficultyCounts.merge(row.getDifficulty(), 1, Integer::sum);
			}
			if (row.getQuestionType() != null) {
				typeCounts.merge(row.getQuestionType().name(), 1, Integer::sum);
			}
		}

		MathPaperMetaView view = new MathPaperMetaView();
		view.setTotalQuestions(rows.size());
		view.setDifficulties(DIFFICULTY_LABELS.entrySet().stream()
				.map(e -> new MathPaperMetaView.Option(e.getKey(), e.getValue(),
						difficultyCounts.getOrDefault(e.getKey(), 0)))
				.collect(Collectors.toList()));
		view.setQuestionTypes(TYPE_LABELS.entrySet().stream()
				.map(e -> new MathPaperMetaView.Option(e.getKey(), e.getValue(),
						typeCounts.getOrDefault(e.getKey(), 0)))
				.collect(Collectors.toList()));

		// 年级 → 册别 → 章节 → 小节 → 知识点
		Map<String, Map<String, List<MathPaperMetaView.ChapterNode>>> tree = new LinkedHashMap<>();
		for (Chapter chapter : chapters) {
			MathPaperMetaView.ChapterNode chapterNode = new MathPaperMetaView.ChapterNode();
			chapterNode.setId(chapter.getId());
			chapterNode.setName(chapter.getName());
			chapterNode.setQuestionCount(chapterCounts.getOrDefault(chapter.getName(), 0));
			List<MathPaperMetaView.SectionNode> sectionNodes = new ArrayList<>();
			for (Section section : sectionsByChapter.getOrDefault(chapter.getId(), Collections.emptyList())) {
				MathPaperMetaView.SectionNode sectionNode = new MathPaperMetaView.SectionNode();
				sectionNode.setId(section.getId());
				sectionNode.setName(section.getName());
				sectionNode.setType(section.getType());
				sectionNode.setQuestionCount(
						sectionCounts.getOrDefault(sectionKey(chapter.getName(), section.getName()), 0));
				sectionNode.setKnowledgePoints(pointsBySection
						.getOrDefault(section.getId(), Collections.emptyList()).stream().map(point -> {
							MathPaperMetaView.KnowledgePointOption option = new MathPaperMetaView.KnowledgePointOption();
							option.setId(point.getId());
							option.setName(point.getName());
							return option;
						}).collect(Collectors.toList()));
				sectionNodes.add(sectionNode);
			}
			chapterNode.setSections(sectionNodes);
			String grade = StringUtils.hasText(chapter.getGrade()) ? chapter.getGrade() : "未分年级";
			String term = StringUtils.hasText(chapter.getTerm()) ? chapter.getTerm() : "上";
			tree.computeIfAbsent(grade, k -> new LinkedHashMap<>()).computeIfAbsent(term, k -> new ArrayList<>())
					.add(chapterNode);
		}

		List<MathPaperMetaView.GradeNode> gradeNodes = new ArrayList<>();
		tree.forEach((grade, books) -> {
			MathPaperMetaView.GradeNode gradeNode = new MathPaperMetaView.GradeNode();
			gradeNode.setGrade(grade);
			List<MathPaperMetaView.BookNode> bookNodes = new ArrayList<>();
			books.forEach((term, chapterNodes) -> {
				MathPaperMetaView.BookNode bookNode = new MathPaperMetaView.BookNode();
				bookNode.setTerm(term);
				bookNode.setLabel(term + "册");
				bookNode.setChapters(chapterNodes);
				bookNodes.add(bookNode);
			});
			gradeNode.setBooks(bookNodes);
			gradeNodes.add(gradeNode);
		});
		view.setGrades(gradeNodes);
		return view;
	}

	// ==================== 选人 ====================

	@Override
	public List<MathPaperStudentView> searchStudents(String keyword) {
		StudentQuery query = new StudentQuery();
		query.setCurrent(1);
		query.setPageSize(20);
		if (StringUtils.hasText(keyword)) {
			String trimmed = keyword.trim();
			// 学号形如 a000001，按字母开头判定；其余按姓名模糊匹配
			if (trimmed.matches("^[a-zA-Z].*")) {
				query.setStudentNo(trimmed);
			}
			else {
				query.setName(trimmed);
			}
		}
		return studentService.pageStudents(query).getList().stream().map(student -> {
			MathPaperStudentView view = new MathPaperStudentView();
			view.setId(student.getId());
			view.setStudentNo(student.getStudentNo());
			view.setName(student.getName());
			view.setCampus(student.getCampus());
			return view;
		}).collect(Collectors.toList());
	}

	// ==================== 组卷 ====================

	@Override
	public MathPaperView generate(MathPaperRequest request) {
		MathPaperRequest req = request == null ? new MathPaperRequest() : request;
		boolean studentMode = "student".equalsIgnoreCase(req.getMode());
		int limit = req.getQuestionCount() == null ? DEFAULT_COUNT
				: Math.max(1, Math.min(req.getQuestionCount(), MAX_COUNT));

		MathPaperView view = new MathPaperView();
		view.setMode(studentMode ? "student" : "chapter");
		view.setWarnings(new ArrayList<>());

		String subjectName = mathSubjectName();
		List<String> allChapterNames = chapterMapper.selectList(Wrappers.<Chapter>lambdaQuery()
				.select(Chapter::getName)
				.eq(Chapter::getSubjectId, MATH_SUBJECT_ID)).stream().map(Chapter::getName)
				.filter(StringUtils::hasText).collect(Collectors.toList());

		Scope scope = resolveScope(req);

		Map<String, Integer> wrongCounts = new HashMap<>();
		List<Template> wrongPool = Collections.emptyList();
		Student student = null;
		if (studentMode) {
			student = resolveStudent(req);
			if (student == null) {
				view.getWarnings().add("未找到对应学员，已按通用规则组卷");
				studentMode = false;
				view.setMode("chapter");
			}
		}
		if (studentMode && student != null) {
			view.setStudentId(student.getId());
			view.setStudentNo(student.getStudentNo());
			view.setStudentName(student.getName());
			wrongCounts.putAll(loadWrongCounts(student.getId()));
			List<Template> wrongTemplates = loadTemplates(new ArrayList<>(wrongCounts.keySet())).stream()
					.filter(t -> isMathTemplate(t, subjectName, allChapterNames))
					.collect(Collectors.toList());
			wrongTemplates.sort(Comparator.comparingInt((Template t) -> wrongCounts.getOrDefault(t.getId(), 0)).reversed());
			wrongPool = wrongTemplates;
			if (scope.isEmpty() && !wrongTemplates.isEmpty()) {
				// 未指定范围时，按学员错题涉及章节圈定补题范围
				Set<String> chapterNames = wrongTemplates.stream().map(Template::getChapter)
						.filter(StringUtils::hasText).collect(Collectors.toCollection(LinkedHashSet::new));
				scope = Scope.ofChapters(chapterNames);
			}
			view.getWarnings().add(wrongTemplates.isEmpty() ? "该学员暂无数学错题，已按所选范围组卷"
					: "该学员数学错题 " + wrongTemplates.size() + " 道，已优先纳入试卷");
		}

		final Scope paperScope = scope;

		// 通用候选池：只取标注列
		List<Template> rows = templateMapper.selectList(Wrappers.<Template>lambdaQuery()
				.select(Template::getId, Template::getName, Template::getQuestionType, Template::getDifficulty,
						Template::getChapter, Template::getSection, Template::getKnowledgePoint)
				.eq(Template::getSubject, subjectName));

		Set<String> requestedPoints = req.getKnowledgePoints() == null ? Collections.emptySet()
				: req.getKnowledgePoints().stream().filter(StringUtils::hasText).map(String::trim)
						.collect(Collectors.toSet());
		Set<String> requestedTypes = req.getQuestionTypes() == null ? Collections.emptySet()
				: req.getQuestionTypes().stream().filter(StringUtils::hasText).collect(Collectors.toSet());

		List<Template> pool = dedupe(rows.stream().filter(t -> paperScope.contains(t)).filter(t -> matchesPoints(t, requestedPoints))
				.filter(t -> matchesTypes(t, requestedTypes)).collect(Collectors.toList()));

		List<Template> selected = new ArrayList<>();
		Set<String> usedKeys = new HashSet<>();
		Set<String> usedIds = new HashSet<>();
		boolean difficultyRelaxed = false;

		if (studentMode) {
			List<Template> wrongFiltered = wrongPool.stream().filter(t -> paperScope.contains(t))
					.filter(t -> matchesPoints(t, requestedPoints)).filter(t -> matchesTypes(t, requestedTypes))
					.filter(t -> !StringUtils.hasText(req.getDifficulty())
							|| req.getDifficulty().equals(t.getDifficulty()))
					.collect(Collectors.toList());
			for (Template template : wrongFiltered) {
				if (selected.size() >= limit) {
					break;
				}
				if (addCandidate(selected, usedKeys, usedIds, template)) {
					continue;
				}
			}
			if (selected.size() < limit) {
				// 错题不足：用同范围练习题补齐；指定难度不足时再放宽难度
				int before = selected.size();
				difficultyRelaxed = takeFromPool(pool, req.getDifficulty(), limit - before, selected, usedKeys,
						usedIds);
				int filled = selected.size() - before;
				if (filled > 0 && !wrongPool.isEmpty()) {
					view.getWarnings().add("错题不足，已补充 " + filled + " 道同范围练习题");
				}
			}
		}
		else {
			difficultyRelaxed = takeFromPool(pool, req.getDifficulty(), limit, selected, usedKeys, usedIds);
		}

		if (selected.size() < limit) {
			view.getWarnings().add("可用题目不足，实际组卷 " + selected.size() + " 题");
		}
		if (difficultyRelaxed) {
			view.getWarnings().add("所选难度题目不足，已用其他难度补足");
		}
		if (!StringUtils.hasText(req.getDifficulty()) && !requestedPoints.isEmpty()) {
			view.getWarnings().add("已按所选知识点筛题");
		}

		// 选完后按题型分组、组内由易到难
		Map<String, Integer> chapterOrder = new HashMap<>();
		if (!paperScope.chapterNames.isEmpty()) {
			int index = 0;
			for (String name : paperScope.chapterNames) {
				chapterOrder.put(name, index++);
			}
		}
		selected.sort(Comparator.comparingInt((Template t) -> typeRank(t.getQuestionType()))
				.thenComparingInt(t -> difficultyRank(t.getDifficulty()))
				.thenComparingInt(t -> chapterOrder.getOrDefault(t.getChapter(), Integer.MAX_VALUE)));

		List<Template> full = loadTemplates(selected.stream().map(Template::getId).collect(Collectors.toList()));
		Map<String, Template> fullById = full.stream()
				.collect(Collectors.toMap(Template::getId, t -> t, (a, b) -> a));

		List<MathPaperView.Question> questions = new ArrayList<>();
		double totalScore = 0;
		for (Template light : selected) {
			Template template = fullById.getOrDefault(light.getId(), light);
			MathPaperView.Question question = toQuestion(template);
			question.setWrongCount(wrongCounts.get(template.getId()));
			totalScore += question.getScore() == null ? 0 : question.getScore();
			questions.add(question);
		}

		view.setQuestions(questions);
		view.setQuestionCount(questions.size());
		view.setWrongQuestionCount((int) questions.stream()
				.filter(q -> q.getWrongCount() != null && q.getWrongCount() > 0).count());
		view.setTotalScore(Math.round(totalScore * 100) / 100.0);
		view.setGeneratedAt(LocalDateTime.now().format(GENERATED_AT));
		view.setTypeStats(countBy(questions, MathPaperView.Question::getQuestionTypeLabel));
		view.setDifficultyStats(countBy(questions, MathPaperView.Question::getDifficultyLabel));
		view.setChapterStats(countBy(questions, q -> StringUtils.hasText(q.getChapter()) ? q.getChapter() : "未分章节"));
		view.setKnowledgePointStats(countPoints(questions));
		view.setTitle(buildTitle(req, student, paperScope, !wrongPool.isEmpty()));
		view.getWarnings().add(0, paperScope.describe());
		return view;
	}

	// ==================== 组卷内部实现 ====================

	/**
	 * 从候选池取题：优先指定难度，不足时用其他难度补足。
	 *
	 * @return 是否放宽了难度限制
	 */
	private boolean takeFromPool(List<Template> pool, String difficulty, int need, List<Template> selected,
			Set<String> usedKeys, Set<String> usedIds) {
		if (need <= 0 || pool.isEmpty()) {
			return false;
		}
		List<Template> shuffled = new ArrayList<>(pool);
		Collections.shuffle(shuffled, new Random());
		boolean relaxed = false;
		int before = selected.size();
		if (StringUtils.hasText(difficulty)) {
			for (Template template : shuffled) {
				if (selected.size() - before >= need) {
					break;
				}
				if (difficulty.equals(template.getDifficulty())) {
					addCandidate(selected, usedKeys, usedIds, template);
				}
			}
		}
		for (Template template : shuffled) {
			if (selected.size() - before >= need) {
				break;
			}
			if (StringUtils.hasText(difficulty) && difficulty.equals(template.getDifficulty())) {
				continue;
			}
			if (addCandidate(selected, usedKeys, usedIds, template) && StringUtils.hasText(difficulty)) {
				relaxed = true;
			}
		}
		return relaxed;
	}

	private boolean addCandidate(List<Template> selected, Set<String> usedKeys, Set<String> usedIds,
			Template template) {
		if (!StringUtils.hasText(template.getId()) || usedIds.contains(template.getId())) {
			return false;
		}
		String key = dedupeKey(template);
		if (key != null && usedKeys.contains(key)) {
			return false;
		}
		if (key != null) {
			usedKeys.add(key);
		}
		usedIds.add(template.getId());
		selected.add(template);
		return true;
	}

	/** 按「题型 + 题干」保留首次出现的题目，消除多题库重复收录。 */
	private List<Template> dedupe(List<Template> templates) {
		Map<String, Template> map = new LinkedHashMap<>();
		for (Template template : templates) {
			map.putIfAbsent(dedupeKey(template), template);
		}
		return new ArrayList<>(map.values());
	}

	private String dedupeKey(Template template) {
		String name = template.getName() == null ? "" : template.getName().replaceAll("\\s+", "");
		if (!StringUtils.hasText(name)) {
			return null;
		}
		String type = template.getQuestionType() == null ? "" : template.getQuestionType().name();
		return type + "|" + name;
	}

	private boolean matchesPoints(Template template, Set<String> points) {
		if (points.isEmpty()) {
			return true;
		}
		String[] values = template.getKnowledgePoint();
		if (values == null || values.length == 0) {
			return false;
		}
		for (String value : values) {
			if (value != null && points.contains(value.trim())) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesTypes(Template template, Set<String> types) {
		if (types.isEmpty()) {
			return true;
		}
		return template.getQuestionType() != null && types.contains(template.getQuestionType().name());
	}

	private boolean isMathTemplate(Template template, String subjectName, List<String> chapterNames) {
		return subjectName.equals(template.getSubject())
				|| (StringUtils.hasText(template.getChapter()) && chapterNames.contains(template.getChapter()));
	}

	private Map<String, Integer> loadWrongCounts(String studentId) {
		WrongQuestionQuery query = new WrongQuestionQuery();
		query.setUserId(studentId);
		query.setCurrent(1);
		query.setPageSize(STUDENT_WRONG_LIMIT);
		Map<String, Integer> counts = new LinkedHashMap<>();
		try {
			for (WrongQuestionView item : practiceService.listWrongQuestions(query).getList()) {
				if (StringUtils.hasText(item.getQuestionId())) {
					counts.merge(item.getQuestionId(),
							item.getWrongCount() == null ? 1 : Math.max(1, item.getWrongCount().intValue()),
							Integer::sum);
				}
			}
		}
		catch (Exception e) {
			log.warn("load student wrong questions failed, studentId={}", studentId, e);
		}
		return counts;
	}

	private List<Template> loadTemplates(List<String> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return Collections.emptyList();
		}
		return templateMapper.selectBatchIds(ids);
	}

	private Student resolveStudent(MathPaperRequest req) {
		if (StringUtils.hasText(req.getStudentId())) {
			Student student = studentMapper.selectById(req.getStudentId());
			if (student != null) {
				return student;
			}
		}
		if (StringUtils.hasText(req.getStudentNo())) {
			StudentQuery query = new StudentQuery();
			query.setCurrent(1);
			query.setPageSize(1);
			query.setStudentNo(req.getStudentNo().trim());
			List<StudentView> list = studentService.pageStudents(query).getList();
			if (!list.isEmpty()) {
				return studentMapper.selectById(list.get(0).getId());
			}
		}
		return null;
	}

	private Scope resolveScope(MathPaperRequest req) {
		if (!CollectionUtils.isEmpty(req.getSectionIds())) {
			List<Section> sections = sectionMapper.selectBatchIds(req.getSectionIds());
			if (!sections.isEmpty()) {
				List<String> chapterIds = sections.stream().map(Section::getChapterId).filter(StringUtils::hasText)
						.distinct().collect(Collectors.toList());
				Map<String, String> chapterNames = chapterIds.isEmpty() ? Collections.emptyMap()
						: chapterMapper.selectBatchIds(chapterIds).stream().collect(Collectors
								.toMap(Chapter::getId, Chapter::getName, (a, b) -> a));
				Scope scope = new Scope();
				for (Section section : sections) {
					String chapterName = chapterNames.get(section.getChapterId());
					if (StringUtils.hasText(chapterName) && StringUtils.hasText(section.getName())) {
						scope.chapterNames.add(chapterName);
						scope.sectionKeys.add(sectionKey(chapterName, section.getName()));
					}
				}
				return scope;
			}
		}
		if (!CollectionUtils.isEmpty(req.getChapterIds())) {
			List<Chapter> chapters = chapterMapper.selectBatchIds(req.getChapterIds());
			Scope scope = new Scope();
			chapters.stream().map(Chapter::getName).filter(StringUtils::hasText)
					.forEach(scope.chapterNames::add);
			return scope;
		}
		return new Scope();
	}

	private MathPaperView.Question toQuestion(Template template) {
		MathPaperView.Question question = new MathPaperView.Question();
		question.setId(template.getId());
		question.setName(template.getName());
		SurveySchema schema = template.getTemplate();
		if (schema != null) {
			schema = schema.deepCopy();
		}
		question.setSchema(schema);
		String type = template.getQuestionType() == null ? null : template.getQuestionType().name();
		question.setQuestionType(type);
		question.setQuestionTypeLabel(TYPE_LABELS.getOrDefault(type, type == null ? "题目" : type));
		question.setDifficulty(template.getDifficulty());
		question.setDifficultyLabel(DIFFICULTY_LABELS.getOrDefault(template.getDifficulty(), "未标注"));
		question.setChapter(template.getChapter());
		question.setSection(template.getSection());
		question.setKnowledgePoints(template.getKnowledgePoint() == null ? Collections.emptyList()
				: Arrays.stream(template.getKnowledgePoint()).filter(StringUtils::hasText).map(String::trim)
						.collect(Collectors.toList()));
		question.setScore(scoreOf(schema));
		question.setAnswerText(answerText(type, schema));
		question.setAnalysis(schema == null || schema.getAttribute() == null ? null
				: schema.getAttribute().getExamAnalysis());
		return question;
	}

	private Double scoreOf(SurveySchema schema) {
		if (schema != null && schema.getAttribute() != null && schema.getAttribute().getExamScore() != null) {
			return schema.getAttribute().getExamScore();
		}
		return 1.0;
	}

	/** 标准答案展示文本：选择题转字母，填空题按空用「；」分隔。 */
	private String answerText(String type, SurveySchema schema) {
		List<String> answers = AnswerJudgeUtil.extractCorrectAnswers(schema);
		if (answers == null || answers.isEmpty()) {
			return null;
		}
		if ("MultipleBlank".equals(type) || "FillBlank".equals(type)) {
			String joined = answers.stream().filter(StringUtils::hasText).map(String::trim)
					.collect(Collectors.joining("；"));
			if (joined.contains("|")) {
				String[] cells = joined.split("\\|", -1);
				StringBuilder text = new StringBuilder();
				for (int i = 0; i < cells.length; i++) {
					if (i > 0) {
						text.append("；");
					}
					if ("MultipleBlank".equals(type) && i < CIRCLED.length()) {
						text.append(CIRCLED.charAt(i));
					}
					text.append(cells[i].trim());
				}
				return text.toString();
			}
			return joined;
		}
		if ("Radio".equals(type) || "Checkbox".equals(type) || "Select".equals(type)) {
			List<SurveySchema> children = schema.getChildren() == null ? Collections.emptyList()
					: schema.getChildren();
			return answers.stream().map(answer -> {
				String trimmed = answer == null ? "" : answer.trim();
				for (int i = 0; i < children.size(); i++) {
					String title = children.get(i).getTitle();
					if (title != null && title.trim().equals(trimmed)) {
						return String.valueOf((char) ('A' + i));
					}
				}
				return trimmed;
			}).collect(Collectors.joining("、"));
		}
		return answers.stream().filter(StringUtils::hasText).map(String::trim).collect(Collectors.joining("；"));
	}

	private List<MathPaperView.StatItem> countBy(List<MathPaperView.Question> questions,
			java.util.function.Function<MathPaperView.Question, String> classifier) {
		Map<String, Integer> counter = new LinkedHashMap<>();
		for (MathPaperView.Question question : questions) {
			String key = classifier.apply(question);
			counter.merge(StringUtils.hasText(key) ? key : "未标注", 1, Integer::sum);
		}
		return counter.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
				.map(e -> new MathPaperView.StatItem(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	private List<MathPaperView.StatItem> countPoints(List<MathPaperView.Question> questions) {
		Map<String, Integer> counter = new LinkedHashMap<>();
		for (MathPaperView.Question question : questions) {
			if (CollectionUtils.isEmpty(question.getKnowledgePoints())) {
				counter.merge("未标注知识点", 1, Integer::sum);
				continue;
			}
			question.getKnowledgePoints().forEach(point -> counter.merge(point, 1, Integer::sum));
		}
		return counter.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
				.map(e -> new MathPaperView.StatItem(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	private String buildTitle(MathPaperRequest req, Student student, Scope scope, boolean hasWrong) {
		if (StringUtils.hasText(req.getTitle())) {
			return req.getTitle().trim();
		}
		if (student != null) {
			return student.getName() + (hasWrong ? " 数学错题巩固卷" : " 数学练习卷");
		}
		List<String> names = new ArrayList<>(scope.chapterNames);
		if (names.isEmpty()) {
			return "数学练习卷";
		}
		if (names.size() == 1) {
			return names.get(0) + " 数学练习卷";
		}
		return "数学综合练习卷（" + names.get(0) + "、" + names.get(1) + "等）";
	}

	private String mathSubjectName() {
		Subject subject = subjectMapper.selectById(MATH_SUBJECT_ID);
		return subject != null && StringUtils.hasText(subject.getName()) ? subject.getName() : "数学";
	}

	private static String sectionKey(String chapter, String section) {
		return chapter + "|" + section;
	}

	private static int gradeRank(String grade) {
		int index = GRADE_ORDER.indexOf(grade);
		return index < 0 ? GRADE_ORDER.size() : index;
	}

	private static int termRank(String term) {
		return "上".equals(term) ? 0 : 1;
	}

	private static int typeRank(SurveySchema.QuestionType type) {
		if (type == null) {
			return TYPE_ORDER.size();
		}
		int index = TYPE_ORDER.indexOf(type.name());
		return index < 0 ? TYPE_ORDER.size() : index;
	}

	private static int difficultyRank(String difficulty) {
		if ("easy".equals(difficulty)) {
			return 0;
		}
		if ("medium".equals(difficulty)) {
			return 1;
		}
		if ("hard".equals(difficulty)) {
			return 2;
		}
		return 3;
	}

	/**
	 * 取题范围（章节名集合 / 章节+小节键集合）。
	 */
	private static class Scope {

		private final Set<String> chapterNames = new LinkedHashSet<>();

		private final Set<String> sectionKeys = new LinkedHashSet<>();

		static Scope ofChapters(Set<String> chapters) {
			Scope scope = new Scope();
			scope.chapterNames.addAll(chapters);
			return scope;
		}

		boolean isEmpty() {
			return chapterNames.isEmpty() && sectionKeys.isEmpty();
		}

		boolean contains(Template template) {
			if (sectionKeys.isEmpty() && chapterNames.isEmpty()) {
				return true;
			}
			if (!sectionKeys.isEmpty()) {
				return StringUtils.hasText(template.getChapter()) && StringUtils.hasText(template.getSection())
						&& sectionKeys.contains(sectionKey(template.getChapter(), template.getSection()));
			}
			return StringUtils.hasText(template.getChapter()) && chapterNames.contains(template.getChapter());
		}

		String describe() {
			if (isEmpty()) {
				return "题源范围：全部数学题库";
			}
			if (!sectionKeys.isEmpty()) {
				return "题源范围：" + sectionKeys.size() + " 个小节";
			}
			return "题源范围：" + String.join("、", chapterNames);
		}

	}

}
