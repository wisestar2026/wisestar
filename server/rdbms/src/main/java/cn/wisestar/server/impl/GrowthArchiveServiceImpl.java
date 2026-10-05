package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.growth.GrowthCompareView;
import cn.wisestar.server.domain.dto.growth.GrowthEventContext;
import cn.wisestar.server.domain.dto.growth.GrowthEventView;
import cn.wisestar.server.domain.dto.growth.GrowthReportRequest;
import cn.wisestar.server.domain.dto.growth.GrowthReportView;
import cn.wisestar.server.domain.model.DetectRecord;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.LearningGrowth;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.StudentArchive;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.domain.model.UserKnowledgeProgress;
import cn.wisestar.server.domain.model.UserWeakKnowledge;
import cn.wisestar.server.mapper.DetectRecordMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.LearningGrowthMapper;
import cn.wisestar.server.mapper.StudentArchiveMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.mapper.UserKnowledgeProgressMapper;
import cn.wisestar.server.mapper.UserWeakKnowledgeMapper;
import cn.wisestar.server.service.GrowthArchiveService;
import cn.wisestar.server.service.StudySummaryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成长档案服务实现（学习轨迹 + 成长对比 + 成长报告）。
 *
 * <p>轨迹写入独立于业务主事务、按「学员 + 来源类型 + 业务对象ID」幂等；语法类按
 * 「grammarId + 日期」累加。成长对比以档案中冻结的基线快照为参照，逐知识点计算
 * 「基线 → 当前」变化。成长报告汇总基线、对比、轨迹与目标规划，AI 可用时润色、
 * 否则规则降级，写入学员档案。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthArchiveServiceImpl implements GrowthArchiveService {

	/** 报告系统提示词 */
	private static final String REPORT_SYSTEM_PROMPT =
			"你是一位负责的学习规划老师，擅长用客观、鼓励且可执行的语言撰写学员成长报告。";

	/** 攻克判定掌握度阈值 */
	private static final int CONQUER_MASTERY = 70;

	/** 报告状态：无 */
	private static final String REPORT_NONE = "none";

	/** 报告状态：草稿 */
	private static final String REPORT_DRAFT = "draft";

	private final LearningGrowthMapper learningGrowthMapper;
	private final StudentArchiveMapper archiveMapper;
	private final DetectRecordMapper detectRecordMapper;
	private final StudentMapper studentMapper;
	private final SubjectMapper subjectMapper;
	private final KnowledgePointMapper knowledgePointMapper;
	private final UserWeakKnowledgeMapper weakMapper;
	private final UserKnowledgeProgressMapper progressMapper;
	private final StudySummaryService studySummaryService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	// ------------------------------------------------------------------ 轨迹写入

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void record(GrowthEventContext context) {
		if (context == null || !StringUtils.hasText(context.getStudentId())
				|| !StringUtils.hasText(context.getSourceType())
				|| !StringUtils.hasText(context.getSourceId())) {
			return;
		}
		String studentId = context.getStudentId();
		String sourceType = context.getSourceType();
		String sourceId = context.getSourceId();

		LearningGrowth existing = learningGrowthMapper.selectOne(Wrappers.<LearningGrowth>lambdaQuery()
				.eq(LearningGrowth::getStudentId, studentId)
				.eq(LearningGrowth::getSourceType, sourceType)
				.eq(LearningGrowth::getSourceId, sourceId)
				.last("limit 1"));
		if (existing != null) {
			// 语法类按 grammarId + 日期累加
			if ("grammar".equals(sourceType)) {
				int question = nz(existing.getQuestionCount()) + nz(context.getQuestionCount());
				int correct = nz(existing.getCorrectCount()) + nz(context.getCorrectCount());
				existing.setQuestionCount(question);
				existing.setCorrectCount(correct);
				existing.setAccuracy(question == 0 ? 0 : Math.round(correct * 100f / question));
				existing.setPoints(nz(existing.getPoints()) + nz(context.getPoints()));
				existing.setCoins(nz(existing.getCoins()) + nz(context.getCoins()));
				existing.setOccurredAt(context.getOccurredAt() == null ? new Date() : context.getOccurredAt());
				learningGrowthMapper.updateById(existing);
			}
			return;
		}

		LearningGrowth growth = new LearningGrowth();
		growth.setStudentId(studentId);
		growth.setSubjectId(context.getSubjectId());
		growth.setSubjectName(resolveSubjectName(context.getSubjectId(), context.getSubjectName()));
		growth.setEventType(StringUtils.hasText(context.getEventType())
				? context.getEventType() : deriveEventType(sourceType));
		growth.setSourceType(sourceType);
		growth.setSourceId(sourceId);
		growth.setEventDate(StringUtils.hasText(context.getEventDate()) ? context.getEventDate() : today());
		growth.setOccurredAt(context.getOccurredAt() == null ? new Date() : context.getOccurredAt());
		growth.setChapterId(context.getChapterId());
		growth.setChapter(context.getChapter());
		growth.setKnowledgePoints(toJson(context.getKnowledgePoints()));
		int question = nz(context.getQuestionCount());
		int correct = nz(context.getCorrectCount());
		growth.setQuestionCount(question);
		growth.setCorrectCount(correct);
		growth.setAccuracy(context.getAccuracy() != null ? context.getAccuracy()
				: (question == 0 ? 0 : Math.round(correct * 100f / question)));
		growth.setDurationMs(context.getDurationMs() == null ? 0L : context.getDurationMs());
		growth.setPoints(nz(context.getPoints()));
		growth.setCoins(nz(context.getCoins()));
		growth.setTitle(context.getTitle());
		growth.setRemark(context.getRemark());
		learningGrowthMapper.insert(growth);
	}

	// ------------------------------------------------------------------ 轨迹查询

	@Override
	public List<GrowthEventView> timeline(String studentId, String subjectId, String from, String to) {
		String uid = StringUtils.hasText(studentId) ? studentId : SecurityContextUtils.getUserId();
		if (!StringUtils.hasText(uid)) {
			return new ArrayList<>();
		}
		List<LearningGrowth> list = learningGrowthMapper.selectList(Wrappers.<LearningGrowth>lambdaQuery()
				.eq(LearningGrowth::getStudentId, uid)
				.eq(StringUtils.hasText(subjectId), LearningGrowth::getSubjectId, subjectId)
				.ge(StringUtils.hasText(from), LearningGrowth::getEventDate, from)
				.le(StringUtils.hasText(to), LearningGrowth::getEventDate, to)
				.orderByDesc(LearningGrowth::getOccurredAt));
		return list.stream().map(this::toEventView).collect(Collectors.toList());
	}

	private GrowthEventView toEventView(LearningGrowth growth) {
		GrowthEventView view = new GrowthEventView();
		view.setId(growth.getId());
		view.setEventType(growth.getEventType());
		view.setSourceType(growth.getSourceType());
		view.setSubjectId(growth.getSubjectId());
		view.setSubjectName(growth.getSubjectName());
		view.setEventDate(growth.getEventDate());
		view.setOccurredAt(growth.getOccurredAt());
		view.setChapter(growth.getChapter());
		view.setKnowledgePoints(fromJsonStringList(growth.getKnowledgePoints()));
		view.setQuestionCount(growth.getQuestionCount());
		view.setCorrectCount(growth.getCorrectCount());
		view.setAccuracy(growth.getAccuracy());
		view.setDurationMs(growth.getDurationMs());
		view.setPoints(growth.getPoints());
		view.setCoins(growth.getCoins());
		view.setTitle(growth.getTitle());
		view.setRemark(growth.getRemark());
		return view;
	}

	// ------------------------------------------------------------------ 成长对比

	@Override
	public GrowthCompareView compare(String studentId, String subjectId, String semester) {
		String uid = StringUtils.hasText(studentId) ? studentId : SecurityContextUtils.getUserId();
		if (!StringUtils.hasText(uid)) {
			throw new ValidationException("未登录");
		}
		String semesterKey = StringUtils.hasText(semester) ? semester : currentSemester();
		StudentArchive archive = findArchive(uid, semesterKey, subjectId);

		GrowthCompareView view = new GrowthCompareView();
		view.setStudentId(uid);
		view.setSubjectId(subjectId);
		view.setSubjectName(resolveSubjectName(subjectId, null));
		view.setSemester(semesterKey);

		BaselineSnapshot baseline = parseBaseline(archive == null ? null : archive.getProfileSnapshot());
		view.setHasBaseline(archive != null && (StringUtils.hasText(archive.getBaselineDetectId())
				|| baseline.hasData()));
		view.setBaselineDetectId(archive == null ? null : archive.getBaselineDetectId());
		view.setBaselineAt(archive == null ? null : archive.getBaselineAt());
		view.setBaselineAccuracy(baseline.accuracy);

		Map<String, Integer> currentMastery = currentMastery(uid, subjectId);
		Map<String, Integer> baselineMastery = new LinkedHashMap<>();
		List<GrowthCompareView.DeltaItem> deltas = new ArrayList<>();
		int resolved = 0;
		for (BaselineItem item : baseline.items) {
			String kpId = item.kpId;
			if (!StringUtils.hasText(kpId)) {
				kpId = resolveKpIdByName(item.name);
			}
			if (StringUtils.hasText(kpId)) {
				baselineMastery.put(kpId, item.accuracy);
			}
			int current = StringUtils.hasText(kpId) ? currentMastery.getOrDefault(kpId, 0) : 0;
			GrowthCompareView.DeltaItem delta = new GrowthCompareView.DeltaItem();
			delta.setKpId(item.kpId);
			delta.setName(item.name);
			delta.setBaselineAccuracy(item.accuracy);
			delta.setCurrentMastery(current);
			delta.setDelta(current - (item.accuracy == null ? 0 : item.accuracy));
			boolean isResolved = current >= CONQUER_MASTERY;
			delta.setResolved(isResolved);
			if (isResolved) {
				resolved++;
			}
			deltas.add(delta);
		}
		view.setDeltas(deltas);
		view.setBaselineWeakCount(baseline.items.size());
		view.setResolvedCount(resolved);
		view.setRemainingCount(baseline.items.size() - resolved);

		// 新增薄弱：当前 active 薄弱点中不在基线集合内的知识点
		int newlyWeak = 0;
		List<UserWeakKnowledge> weaks = weakMapper.selectList(Wrappers.<UserWeakKnowledge>lambdaQuery()
				.eq(UserWeakKnowledge::getUserId, uid)
				.eq(StringUtils.hasText(subjectId), UserWeakKnowledge::getSubjectId, subjectId)
				.eq(UserWeakKnowledge::getStatus, "active"));
		for (UserWeakKnowledge w : weaks) {
			if (!baselineMastery.containsKey(w.getKnowledgePointId())) {
				newlyWeak++;
			}
		}
		view.setNewlyWeakCount(newlyWeak);

		// 轨迹聚合：题量/正确数/正确率/事件数
		List<LearningGrowth> events = learningGrowthMapper.selectList(Wrappers.<LearningGrowth>lambdaQuery()
				.eq(LearningGrowth::getStudentId, uid)
				.eq(StringUtils.hasText(subjectId), LearningGrowth::getSubjectId, subjectId));
		int q = 0;
		int c = 0;
		for (LearningGrowth g : events) {
			q += nz(g.getQuestionCount());
			c += nz(g.getCorrectCount());
		}
		view.setEventCount(events.size());
		view.setTotalQuestionCount(q);
		view.setTotalCorrectCount(c);
		view.setCurrentAccuracy(q == 0 ? null : Math.round(c * 100f / q));
		return view;
	}

	// ------------------------------------------------------------------ 成长报告

	@Override
	public GrowthReportView generate(GrowthReportRequest request) {
		String uid = request != null && StringUtils.hasText(request.getStudentId())
				? request.getStudentId() : SecurityContextUtils.getUserId();
		if (!StringUtils.hasText(uid)) {
			throw new ValidationException("未登录");
		}
		String subjectId = request == null ? null : request.getSubjectId();
		String semesterKey = request != null && StringUtils.hasText(request.getSemester())
				? request.getSemester() : currentSemester();

		StudentArchive archive = findArchive(uid, semesterKey, subjectId);
		if (archive == null) {
			archive = createArchive(uid, semesterKey, subjectId);
		}
		GrowthCompareView compare = compare(uid, subjectId, semesterKey);
		List<GrowthEventView> timeline = timeline(uid, subjectId, null, null);

		String content = studySummaryService.aiText(REPORT_SYSTEM_PROMPT,
				buildReportPrompt(archive, compare, timeline));
		String model = "ai";
		if (!StringUtils.hasText(content)) {
			content = buildRuleReport(archive, compare, timeline);
			model = "rule";
		}
		Date now = new Date();
		archive.setReportContent(content);
		archive.setReportStatus(REPORT_DRAFT);
		archive.setReportModel(model);
		archive.setReportGeneratedAt(now);
		archiveMapper.updateById(archive);
		return toReportView(uid, subjectId, semesterKey, content, REPORT_DRAFT, model, now);
	}

	@Override
	public GrowthReportView report(String studentId, String subjectId, String semester) {
		String uid = StringUtils.hasText(studentId) ? studentId : SecurityContextUtils.getUserId();
		if (!StringUtils.hasText(uid)) {
			throw new ValidationException("未登录");
		}
		String semesterKey = StringUtils.hasText(semester) ? semester : currentSemester();
		StudentArchive archive = findArchive(uid, semesterKey, subjectId);
		if (archive == null) {
			return toReportView(uid, subjectId, semesterKey, null, REPORT_NONE, null, null);
		}
		return toReportView(uid, subjectId, semesterKey, archive.getReportContent(),
				StringUtils.hasText(archive.getReportStatus()) ? archive.getReportStatus() : REPORT_NONE,
				archive.getReportModel(), archive.getReportGeneratedAt());
	}

	private GrowthReportView toReportView(String studentId, String subjectId, String semester, String content,
			String status, String model, Date generatedAt) {
		GrowthReportView view = new GrowthReportView();
		view.setStudentId(studentId);
		Student student = studentMapper.selectById(studentId);
		if (student != null) {
			view.setStudentNo(student.getStudentNo());
			view.setStudentName(student.getName());
		}
		view.setSubjectId(subjectId);
		view.setSubjectName(resolveSubjectName(subjectId, null));
		view.setSemester(semester);
		Semester sem = deriveSemester(semester);
		view.setSchoolYear(sem.schoolYear);
		view.setTermLabel(sem.termLabel);
		view.setContent(content);
		view.setStatus(status);
		view.setModel(model);
		view.setGeneratedAt(generatedAt);
		return view;
	}

	private String buildReportPrompt(StudentArchive archive, GrowthCompareView compare,
			List<GrowthEventView> timeline) {
		StringBuilder sb = new StringBuilder();
		sb.append("学员：").append(archive.getStudentName()).append("\n");
		sb.append("学期：").append(archive.getTermLabel()).append("（").append(archive.getSchoolYear()).append("学年）\n");
		if (StringUtils.hasText(archive.getGoalPlan())) {
			sb.append("本学期目标：").append(archive.getGoalPlan()).append("\n");
		}
		if (Boolean.TRUE.equals(compare.getHasBaseline())) {
			sb.append("学前检测正确率：").append(compare.getBaselineAccuracy()).append("%\n");
			sb.append("基线薄弱知识点 ").append(compare.getBaselineWeakCount()).append(" 个，其中已攻克 ")
					.append(compare.getResolvedCount()).append(" 个\n");
		}
		sb.append("本学期累计学习事件 ").append(compare.getEventCount()).append(" 次，练习正确率 ")
				.append(compare.getCurrentAccuracy()).append("%\n");
		List<String> resolvedNames = compare.getDeltas().stream()
				.filter(d -> Boolean.TRUE.equals(d.getResolved())).map(GrowthCompareView.DeltaItem::getName)
				.filter(n -> n != null && !n.isEmpty()).limit(8).collect(Collectors.toList());
		if (!resolvedNames.isEmpty()) {
			sb.append("已攻克：").append(String.join("、", resolvedNames)).append("\n");
		}
		List<String> remainingNames = compare.getDeltas().stream()
				.filter(d -> !Boolean.TRUE.equals(d.getResolved())).map(GrowthCompareView.DeltaItem::getName)
				.filter(n -> n != null && !n.isEmpty()).limit(8).collect(Collectors.toList());
		if (!remainingNames.isEmpty()) {
			sb.append("仍需巩固：").append(String.join("、", remainingNames)).append("\n");
		}
		sb.append("\n请用 400 字以内的中文撰写学员成长报告，包含：基线学情、本学期进步、"
				+ "仍需巩固、下阶段建议。直接输出正文，不要标题与 Markdown。");
		return sb.toString();
	}

	private String buildRuleReport(StudentArchive archive, GrowthCompareView compare,
			List<GrowthEventView> timeline) {
		StringBuilder sb = new StringBuilder();
		sb.append("【").append(archive.getTermLabel()).append(" 成长报告】");
		if (Boolean.TRUE.equals(compare.getHasBaseline())) {
			sb.append("学期初检测正确率为 ").append(compare.getBaselineAccuracy()).append("%，共有薄弱知识点 ")
					.append(compare.getBaselineWeakCount()).append(" 个。");
		}
		sb.append("本学期共记录学习事件 ").append(compare.getEventCount()).append(" 次。");
		if (compare.getTotalQuestionCount() != null && compare.getTotalQuestionCount() > 0) {
			sb.append("累计练习 ").append(compare.getTotalQuestionCount()).append(" 题，正确 ")
					.append(compare.getTotalCorrectCount()).append(" 题，正确率 ")
					.append(compare.getCurrentAccuracy()).append("%。");
		}
		if (compare.getBaselineWeakCount() != null && compare.getBaselineWeakCount() > 0) {
			sb.append("基线薄弱点已攻克 ").append(compare.getResolvedCount()).append(" 个，仍需巩固 ")
					.append(compare.getRemainingCount()).append(" 个。");
			List<String> remaining = compare.getDeltas().stream()
					.filter(d -> !Boolean.TRUE.equals(d.getResolved()))
					.map(GrowthCompareView.DeltaItem::getName)
					.filter(n -> n != null && !n.isEmpty()).limit(6).collect(Collectors.toList());
			if (!remaining.isEmpty()) {
				sb.append("建议下阶段重点突破：").append(String.join("、", remaining)).append("。");
			}
		} else {
			sb.append("建议保持练习节奏并拓展综合应用。");
		}
		return sb.toString();
	}

	// ------------------------------------------------------------------ 重建

	@Override
	public void rebuild(String studentId, String subjectId, String semester) {
		if (!StringUtils.hasText(studentId)) {
			return;
		}
		List<DetectRecord> records = detectRecordMapper.selectList(Wrappers.<DetectRecord>lambdaQuery()
				.eq(DetectRecord::getStudentId, studentId)
				.eq(StringUtils.hasText(subjectId), DetectRecord::getSubjectId, subjectId)
				.eq(StringUtils.hasText(semester), DetectRecord::getSemester, semester));
		for (DetectRecord r : records) {
			GrowthEventContext ctx = new GrowthEventContext();
			ctx.setStudentId(studentId);
			ctx.setSubjectId(r.getSubjectId());
			ctx.setSubjectName(r.getSubjectName());
			ctx.setEventType("DETECT");
			ctx.setSourceType("detect");
			ctx.setSourceId(r.getId());
			ctx.setEventDate(r.getCreateAt() == null ? today()
					: LocalDate.ofInstant(r.getCreateAt().toInstant(), java.time.ZoneId.systemDefault())
							.format(DateTimeFormatter.ISO_LOCAL_DATE));
			ctx.setOccurredAt(r.getCreateAt());
			ctx.setQuestionCount(r.getTotal());
			ctx.setCorrectCount(r.getCorrectCount());
			ctx.setAccuracy(r.getAccuracy());
			ctx.setDurationMs(r.getDurationMs());
			ctx.setTitle(Boolean.TRUE.equals(r.getIsBaseline()) ? "学前检测" : "单元检测");
			record(ctx);
		}
	}

	// ------------------------------------------------------------------ 内部方法

	/** 按「学员 + 学期 + 学科」定位档案（学科为空定位全科条目）。 */
	private StudentArchive findArchive(String studentId, String semester, String subjectId) {
		LambdaQueryWrapper<StudentArchive> wrapper = Wrappers.<StudentArchive>lambdaQuery()
				.eq(StudentArchive::getStudentId, studentId)
				.eq(StudentArchive::getSemester, semester);
		if (StringUtils.hasText(subjectId)) {
			wrapper.eq(StudentArchive::getSubjectId, subjectId);
		} else {
			wrapper.isNull(StudentArchive::getSubjectId);
		}
		return archiveMapper.selectOne(wrapper.last("limit 1"));
	}

	/** 建档（带学科）；已存在则直接返回。 */
	private StudentArchive createArchive(String studentId, String semester, String subjectId) {
		Student student = studentMapper.selectById(studentId);
		if (student == null) {
			throw new ValidationException("学员不存在");
		}
		StudentArchive archive = new StudentArchive();
		archive.setStudentId(studentId);
		archive.setStudentNo(student.getStudentNo());
		archive.setStudentName(student.getName());
		Semester sem = deriveSemester(semester);
		archive.setSchoolYear(sem.schoolYear);
		archive.setSemester(semester);
		archive.setTermLabel(sem.termLabel);
		archive.setSubjectId(subjectId);
		archive.setStatus("draft");
		archive.setReportStatus(REPORT_NONE);
		archiveMapper.insert(archive);
		return archive;
	}

	/** 当前学科下各知识点掌握度（userId + knowledgePointId）。 */
	private Map<String, Integer> currentMastery(String studentId, String subjectId) {
		List<UserKnowledgeProgress> list = progressMapper.selectList(
				Wrappers.<UserKnowledgeProgress>lambdaQuery()
						.eq(UserKnowledgeProgress::getUserId, studentId)
						.eq(StringUtils.hasText(subjectId), UserKnowledgeProgress::getSubjectId, subjectId));
		Map<String, Integer> map = new LinkedHashMap<>();
		for (UserKnowledgeProgress p : list) {
			map.put(p.getKnowledgePointId(), nz(p.getMastery()));
		}
		return map;
	}

	/** 解析基线快照（兼容旧数组格式与新对象格式）。 */
	private BaselineSnapshot parseBaseline(String snapshot) {
		BaselineSnapshot result = new BaselineSnapshot();
		if (!StringUtils.hasText(snapshot)) {
			return result;
		}
		try {
			JsonNode root = objectMapper.readTree(snapshot);
			JsonNode weakNodes;
			if (root.isArray()) {
				weakNodes = root;
			} else {
				result.accuracy = root.hasNonNull("accuracy") ? root.get("accuracy").asInt() : null;
				if (StringUtils.hasText(root.path("detectId").asText(""))) {
					result.detectId = root.get("detectId").asText();
				}
				weakNodes = root.path("weakPoints");
			}
			if (weakNodes != null && weakNodes.isArray()) {
				for (JsonNode node : weakNodes) {
					BaselineItem item = new BaselineItem();
					item.kpId = node.path("kpId").asText(null);
					item.name = node.path("name").asText(null);
					item.accuracy = node.hasNonNull("accuracy") ? node.get("accuracy").asInt()
							: (node.hasNonNull("mastery") ? node.get("mastery").asInt() : 0);
					if (StringUtils.hasText(item.name) || StringUtils.hasText(item.kpId)) {
						result.items.add(item);
					}
				}
			}
		} catch (Exception e) {
			log.warn("解析基线快照失败：{}", e.getMessage());
		}
		return result;
	}

	/** 基线薄弱点无 kpId 时按名称回查知识点ID（检测报告薄弱点仅含名称）。 */
	private String resolveKpIdByName(String name) {
		if (!StringUtils.hasText(name)) {
			return null;
		}
		KnowledgePoint kp = knowledgePointMapper.selectOne(Wrappers.<KnowledgePoint>lambdaQuery()
				.eq(KnowledgePoint::getName, name).last("limit 1"));
		return kp == null ? null : kp.getId();
	}

	private String resolveSubjectName(String subjectId, String fallback) {
		if (StringUtils.hasText(fallback)) {
			return fallback;
		}
		if (!StringUtils.hasText(subjectId)) {
			return null;
		}
		Subject subject = subjectMapper.selectById(subjectId);
		return subject == null ? null : subject.getName();
	}

	private String deriveEventType(String sourceType) {
		switch (sourceType) {
			case "practice":
				return "PRACTICE";
			case "detect":
				return "DETECT";
			case "grammar":
				return "GRAMMAR";
			case "archive_record":
				return "CLASS";
			default:
				return sourceType.toUpperCase();
		}
	}

	private String toJson(List<String> names) {
		if (names == null || names.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(names);
		} catch (Exception e) {
			log.warn("序列化轨迹知识点失败：{}", e.getMessage());
			return null;
		}
	}

	private List<String> fromJsonStringList(String json) {
		if (!StringUtils.hasText(json)) {
			return new ArrayList<>();
		}
		try {
			List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {
			});
			return list == null ? new ArrayList<>() : list;
		} catch (Exception e) {
			log.warn("解析轨迹知识点失败：{}", e.getMessage());
			return new ArrayList<>();
		}
	}

	private int nz(Integer value) {
		return value == null ? 0 : value;
	}

	private String today() {
		return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	/** 学期推导：传入学期键时反推学年与名称，为空时按当前日期。 */
	private Semester deriveSemester(String semesterKey) {
		Semester sem = new Semester();
		if (StringUtils.hasText(semesterKey) && semesterKey.contains("-")) {
			int dash = semesterKey.lastIndexOf('-');
			String startYear = semesterKey.substring(0, dash);
			String half = semesterKey.substring(dash + 1);
			sem.schoolYear = ("1".equals(half) ? startYear : String.valueOf(Integer.parseInt(startYear) - 1))
					+ "-" + ("1".equals(half) ? String.valueOf(Integer.parseInt(startYear) + 1) : startYear);
			sem.termLabel = "1".equals(half) ? "第一学期" : "第二学期";
			return sem;
		}
		LocalDate now = LocalDate.now();
		int year = now.getYear();
		int month = now.getMonthValue();
		if (month >= 9) {
			sem.schoolYear = year + "-" + (year + 1);
			sem.termLabel = "第一学期";
		} else if (month == 1) {
			sem.schoolYear = (year - 1) + "-" + year;
			sem.termLabel = "第一学期";
		} else {
			sem.schoolYear = (year - 1) + "-" + year;
			sem.termLabel = "第二学期";
		}
		return sem;
	}

	private String currentSemester() {
		LocalDate now = LocalDate.now();
		int year = now.getYear();
		int month = now.getMonthValue();
		if (month >= 9) {
			return year + "-1";
		} else if (month == 1) {
			return (year - 1) + "-1";
		} else {
			return (year - 1) + "-2";
		}
	}

	/** 基线快照解析结果。 */
	private static class BaselineSnapshot {
		private Integer accuracy;
		private String detectId;
		private final List<BaselineItem> items = new ArrayList<>();

		private boolean hasData() {
			return !items.isEmpty() || accuracy != null;
		}
	}

	/** 基线快照中的单个薄弱知识点。 */
	private static class BaselineItem {
		private String kpId;
		private String name;
		private Integer accuracy;
	}

	/** 学年/学期推导结果。 */
	private static class Semester {
		private String schoolYear;
		private String termLabel;
	}

}
