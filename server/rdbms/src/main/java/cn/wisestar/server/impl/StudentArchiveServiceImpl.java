package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.archive.ArchiveRecordDraftView;
import cn.wisestar.server.domain.dto.archive.StudentArchiveOverviewView;
import cn.wisestar.server.domain.dto.archive.StudentArchiveRecordRequest;
import cn.wisestar.server.domain.dto.archive.StudentArchiveRecordView;
import cn.wisestar.server.domain.dto.archive.StudentArchiveSaveRequest;
import cn.wisestar.server.domain.dto.archive.StudentArchiveView;
import cn.wisestar.server.domain.dto.growth.GrowthEventContext;
import cn.wisestar.server.domain.dto.student.StudentWeakView;
import cn.wisestar.server.domain.dto.student.StudySummaryView;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.StudentArchive;
import cn.wisestar.server.domain.model.StudentArchiveRecord;
import cn.wisestar.server.domain.model.UserKnowledgeProgress;
import cn.wisestar.server.domain.model.UserWeakKnowledge;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.StudentArchiveMapper;
import cn.wisestar.server.mapper.StudentArchiveRecordMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.UserKnowledgeProgressMapper;
import cn.wisestar.server.mapper.UserWeakKnowledgeMapper;
import cn.wisestar.server.service.StudentArchiveService;
import cn.wisestar.server.service.StudySummaryService;
import cn.wisestar.server.service.GrowthArchiveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
 * 学员档案服务实现。
 *
 * <p>学期按当前日期自动推导（可手填覆盖）；新建档案时定格当前薄弱知识点为初始档案快照；
 * 上课记录支持按日期自动拉取当日学习数据生成草稿，老师编辑后定稿；
 * 学期报告在 AI 可用时润色、否则降级为规则模板。</p>
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentArchiveServiceImpl implements StudentArchiveService {

	/** 学期报告系统提示词 */
	private static final String REPORT_SYSTEM_PROMPT =
			"你是一位负责的学习规划老师，擅长用客观、鼓励且可执行的语言撰写学员学期学习报告。";

	/** 档案状态：草稿 */
	private static final String STATUS_DRAFT = "draft";

	/** 上课记录来源：手动 */
	private static final String SOURCE_MANUAL = "manual";

	/** 报告状态：无 */
	private static final String REPORT_NONE = "none";

	/** 报告状态：草稿 */
	private static final String REPORT_DRAFT = "draft";

	/** 报告状态：定稿 */
	private static final String REPORT_FINAL = "final";

	private final StudentArchiveMapper archiveMapper;
	private final StudentArchiveRecordMapper recordMapper;
	private final StudentMapper studentMapper;
	private final UserWeakKnowledgeMapper weakMapper;
	private final UserKnowledgeProgressMapper progressMapper;
	private final KnowledgePointMapper knowledgePointMapper;
	private final StudySummaryService studySummaryService;
	private final GrowthArchiveService growthArchiveService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	// ------------------------------------------------------------------ 查询

	@Override
	public StudentArchiveView getArchive(String studentId, String semester) {
		if (!StringUtils.hasText(studentId)) {
			throw new ValidationException("学员ID不能为空");
		}
		String semesterKey = StringUtils.hasText(semester) ? semester : deriveSemester().semester;
		// 优先定位全科条目（subject_id 为空），保持既有「学员 + 学期」档案语义；无则任取一条
		StudentArchive archive = archiveMapper.selectOne(new LambdaQueryWrapper<StudentArchive>()
				.eq(StudentArchive::getStudentId, studentId)
				.eq(StudentArchive::getSemester, semesterKey)
				.isNull(StudentArchive::getSubjectId)
				.last("limit 1"));
		if (archive == null) {
			archive = archiveMapper.selectOne(new LambdaQueryWrapper<StudentArchive>()
					.eq(StudentArchive::getStudentId, studentId)
					.eq(StudentArchive::getSemester, semesterKey)
					.last("limit 1"));
		}

		StudentArchiveView view = new StudentArchiveView();
		Student student = studentMapper.selectById(studentId);
		if (archive != null) {
			fillView(view, archive);
		} else {
			Semester sem = deriveSemester();
			view.setStudentId(studentId);
			view.setSemester(sem.semester);
			view.setSchoolYear(sem.schoolYear);
			view.setTermLabel(sem.termLabel);
			view.setStatus(STATUS_DRAFT);
			view.setReportStatus(REPORT_NONE);
		}
		if (student != null) {
			view.setStudentNo(student.getStudentNo());
			view.setStudentName(student.getName());
		}
		view.setInitialWeakPoints(parseSnapshot(archive == null ? null : archive.getProfileSnapshot()));
		view.setWeakPoints(buildWeakPoints(studentId));
		if (archive != null) {
			view.setRecords(listRecords(archive, true));
		}
		view.setToday(studySummaryService.preview(studentId, today()));
		return view;
	}

	@Override
	public StudentArchiveView myArchive() {
		String studentId = SecurityContextUtils.getUserId();
		if (!StringUtils.hasText(studentId)) {
			throw new ValidationException("未登录");
		}
		return getArchive(studentId, null);
	}

	@Override
	public StudentArchiveOverviewView overview(String studentId) {
		if (!StringUtils.hasText(studentId)) {
			throw new ValidationException("学员ID不能为空");
		}
		Semester sem = deriveSemester();
		StudentArchive archive = archiveMapper.selectOne(new LambdaQueryWrapper<StudentArchive>()
				.eq(StudentArchive::getStudentId, studentId)
				.eq(StudentArchive::getSemester, sem.semester)
				.last("limit 1"));

		StudentArchiveOverviewView view = new StudentArchiveOverviewView();
		view.setStudentId(studentId);
		Student student = studentMapper.selectById(studentId);
		view.setStudentName(student == null ? null : student.getName());
		view.setHasArchive(archive != null);
		view.setSemester(sem.semester);
		view.setTermLabel(archive == null ? sem.termLabel : archive.getTermLabel());
		Long weakCount = weakMapper.selectCount(new LambdaQueryWrapper<UserWeakKnowledge>()
				.eq(UserWeakKnowledge::getUserId, studentId)
				.eq(UserWeakKnowledge::getStatus, "active"));
		view.setWeakCount(weakCount == null ? 0 : weakCount.intValue());
		if (archive != null) {
			Long recordCount = recordMapper.selectCount(new LambdaQueryWrapper<StudentArchiveRecord>()
					.eq(StudentArchiveRecord::getArchiveId, archive.getId()));
			view.setRecordCount(recordCount == null ? 0 : recordCount.intValue());
			StudentArchiveRecord latest = recordMapper.selectOne(new LambdaQueryWrapper<StudentArchiveRecord>()
					.eq(StudentArchiveRecord::getArchiveId, archive.getId())
					.orderByDesc(StudentArchiveRecord::getRecordDate)
					.last("limit 1"));
			view.setLastRecordDate(latest == null ? null : latest.getRecordDate());
			view.setReportStatus(StringUtils.hasText(archive.getReportStatus())
					? archive.getReportStatus() : REPORT_NONE);
		} else {
			view.setReportStatus(REPORT_NONE);
		}
		return view;
	}

	// ------------------------------------------------------------------ 保存

	@Override
	@Transactional(rollbackFor = Exception.class)
	public StudentArchiveView save(StudentArchiveSaveRequest request) {
		if (request == null || !StringUtils.hasText(request.getStudentId())) {
			throw new ValidationException("学员ID不能为空");
		}
		Student student = studentMapper.selectById(request.getStudentId());
		if (student == null) {
			throw new ValidationException("学员不存在");
		}
		Semester sem = deriveSemester();
		String schoolYear = StringUtils.hasText(request.getSchoolYear())
				? request.getSchoolYear() : sem.schoolYear;
		String termLabel = StringUtils.hasText(request.getTermLabel())
				? request.getTermLabel() : sem.termLabel;
		String semesterKey = StringUtils.hasText(request.getSemester())
				? request.getSemester() : sem.semester;

		StudentArchive archive;
		if (StringUtils.hasText(request.getId())) {
			archive = archiveMapper.selectById(request.getId());
			if (archive == null) {
				throw new ValidationException("档案不存在");
			}
		} else {
			archive = archiveMapper.selectOne(new LambdaQueryWrapper<StudentArchive>()
					.eq(StudentArchive::getStudentId, request.getStudentId())
					.eq(StudentArchive::getSemester, semesterKey)
					.last("limit 1"));
		}
		boolean isNew = archive == null;
		if (isNew) {
			archive = new StudentArchive();
			archive.setStudentId(request.getStudentId());
			archive.setStudentNo(student.getStudentNo());
			archive.setStudentName(student.getName());
			archive.setSchoolYear(schoolYear);
			archive.setSemester(semesterKey);
			archive.setTermLabel(termLabel);
			archive.setSubjectId(request.getSubjectId());
			archive.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : STATUS_DRAFT);
			archive.setProfileSnapshot(buildSnapshotJson(request.getStudentId()));
			archive.setReportStatus(StringUtils.hasText(request.getReportStatus())
					? request.getReportStatus() : REPORT_NONE);
		} else {
			archive.setSchoolYear(schoolYear);
			archive.setSemester(semesterKey);
			archive.setTermLabel(termLabel);
			if (StringUtils.hasText(request.getSubjectId())) {
				archive.setSubjectId(request.getSubjectId());
			}
			if (StringUtils.hasText(request.getStatus())) {
				archive.setStatus(request.getStatus());
			}
		}
		if (request.getTeacherName() != null) {
			archive.setTeacherName(request.getTeacherName());
		}
		if (StringUtils.hasText(SecurityContextUtils.getUserId())
				&& !StringUtils.hasText(archive.getTeacherId())) {
			archive.setTeacherId(SecurityContextUtils.getUserId());
			if (!StringUtils.hasText(archive.getTeacherName())) {
				archive.setTeacherName(SecurityContextUtils.getUsername());
			}
		}
		if (request.getGoalPlan() != null) {
			archive.setGoalPlan(request.getGoalPlan());
		}
		if (request.getPromise() != null) {
			archive.setPromise(request.getPromise());
		}
		if (request.getReportContent() != null) {
			archive.setReportContent(request.getReportContent());
		}
		if (StringUtils.hasText(request.getReportStatus())) {
			archive.setReportStatus(request.getReportStatus());
		}
		if (request.getRemark() != null) {
			archive.setRemark(request.getRemark());
		}

		if (isNew) {
			archiveMapper.insert(archive);
		} else {
			archiveMapper.updateById(archive);
		}
		return getArchive(request.getStudentId(), archive.getSemester());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public StudentArchiveRecordView saveRecord(StudentArchiveRecordRequest request) {
		if (request == null || !StringUtils.hasText(request.getStudentId())) {
			throw new ValidationException("学员ID不能为空");
		}
		if (!StringUtils.hasText(request.getRecordDate())) {
			throw new ValidationException("上课日期不能为空");
		}
		StudentArchive archive = resolveArchive(request.getStudentId(), request.getArchiveId(),
				request.getSubjectId());

		StudentArchiveRecord record;
		if (StringUtils.hasText(request.getId())) {
			record = recordMapper.selectById(request.getId());
			if (record == null) {
				throw new ValidationException("上课记录不存在");
			}
		} else {
			record = recordMapper.selectOne(new LambdaQueryWrapper<StudentArchiveRecord>()
					.eq(StudentArchiveRecord::getArchiveId, archive.getId())
					.eq(StudentArchiveRecord::getRecordDate, request.getRecordDate())
					.last("limit 1"));
		}
		boolean isNew = record == null;
		if (isNew) {
			record = new StudentArchiveRecord();
			record.setArchiveId(archive.getId());
			record.setStudentId(request.getStudentId());
			record.setRecordDate(request.getRecordDate());
			record.setSource(SOURCE_MANUAL);
			record.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : STATUS_DRAFT);
		}
		record.setSubjectId(request.getSubjectId());
		record.setTitle(request.getTitle());
		record.setStudySummary(request.getStudySummary());
		record.setSolvedProblems(request.getSolvedProblems());
		record.setStrengthenedKps(request.getStrengthenedKps());
		record.setWeaknesses(request.getWeaknesses());
		record.setHomework(request.getHomework());
		record.setTeacherComment(request.getTeacherComment());
		record.setDurationMinutes(request.getDurationMinutes());
		record.setPoints(request.getPoints());
		record.setCoins(request.getCoins());
		if (StringUtils.hasText(request.getStatus())) {
			record.setStatus(request.getStatus());
		}
		if (isNew) {
			recordMapper.insert(record);
		} else {
			recordMapper.updateById(record);
		}
		// 成长档案：上课记录定稿后留痕（CLASS 事件，幂等；失败不阻断）
		if (REPORT_FINAL.equals(record.getStatus())) {
			try {
				GrowthEventContext growth = new GrowthEventContext();
				growth.setStudentId(record.getStudentId());
				growth.setSubjectId(StringUtils.hasText(record.getSubjectId())
						? record.getSubjectId() : archive.getSubjectId());
				growth.setEventType("CLASS");
				growth.setSourceType("archive_record");
				growth.setSourceId(record.getId());
				growth.setEventDate(record.getRecordDate());
				growth.setQuestionCount(0);
				growth.setCorrectCount(0);
				growth.setDurationMs(record.getDurationMinutes() == null ? 0L
						: record.getDurationMinutes() * 60000L);
				growth.setPoints(record.getPoints());
				growth.setCoins(record.getCoins());
				if (StringUtils.hasText(record.getStrengthenedKps())) {
					List<String> names = new ArrayList<>();
					names.add(record.getStrengthenedKps());
					growth.setKnowledgePoints(names);
				}
				growth.setTitle(StringUtils.hasText(record.getTitle()) ? record.getTitle() : "上课记录");
				growth.setRemark(record.getStudySummary());
				growthArchiveService.record(growth);
			}
			catch (Exception e) {
				log.warn("save archive record: growth record failed, ignored", e);
			}
		}
		return toRecordView(record);
	}

	@Override
	public void deleteRecord(String id) {
		if (!StringUtils.hasText(id)) {
			throw new ValidationException("记录ID不能为空");
		}
		recordMapper.deleteById(id);
	}

	// ------------------------------------------------------------------ 草稿 / 报告

	@Override
	public ArchiveRecordDraftView draft(String studentId, String date) {
		if (!StringUtils.hasText(studentId)) {
			throw new ValidationException("学员ID不能为空");
		}
		String draftDate = StringUtils.hasText(date) ? date : today();
		StudySummaryView summary = studySummaryService.preview(studentId, draftDate);
		ArchiveRecordDraftView view = new ArchiveRecordDraftView();
		view.setDate(draftDate);
		view.setTitle(draftDate + " 学习记录");
		if (summary != null) {
			view.setStudySummary(summary.getContent());
			view.setDurationMinutes(minutes(summary.getDurationMs()));
			view.setPracticeCount(summary.getPracticeCount());
			view.setQuestionCount(summary.getQuestionCount());
			view.setCorrectCount(summary.getCorrectCount());
			view.setAccuracy(summary.getAccuracy());
			view.setWrongCount(summary.getWrongCount());
			view.setPoints(summary.getPoints());
			view.setCoins(summary.getCoins());
			view.setKnowledgeCount(summary.getKnowledgeCount());
			view.setAvgMastery(summary.getAvgMastery());
			view.setStrengthenedKps(summary.getStrengthenedNames() == null
					? new ArrayList<>() : new ArrayList<>(summary.getStrengthenedNames()));
			view.setWeakNames(summary.getWeakNames() == null
					? new ArrayList<>() : new ArrayList<>(summary.getWeakNames()));
		}
		return view;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public StudentArchiveView generateReport(String studentId, String semester) {
		if (!StringUtils.hasText(studentId)) {
			throw new ValidationException("学员ID不能为空");
		}
		StudentArchive archive = resolveArchive(studentId, null, null);
		List<StudentArchiveRecordView> records = listRecords(archive, false);
		List<StudentWeakView> weakPoints = buildWeakPoints(studentId);
		List<String> weakNames = weakPoints.stream().map(StudentWeakView::getName)
				.filter(n -> n != null && !n.isEmpty()).collect(Collectors.toList());

		String content = studySummaryService.aiText(REPORT_SYSTEM_PROMPT,
				buildReportPrompt(archive, records, weakNames));
		if (!StringUtils.hasText(content)) {
			content = buildRuleReport(archive, records, weakNames);
		}
		archive.setReportContent(content);
		archive.setReportStatus(REPORT_DRAFT);
		archiveMapper.updateById(archive);
		return getArchive(studentId, archive.getSemester());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void bindBaseline(String studentId, String subjectId, String semester, String detectId,
			int accuracy, List<StudentWeakView> weakPoints) {
		if (!StringUtils.hasText(studentId) || !StringUtils.hasText(subjectId)
				|| !StringUtils.hasText(semester)) {
			return;
		}
		StudentArchive archive = archiveMapper.selectOne(new LambdaQueryWrapper<StudentArchive>()
				.eq(StudentArchive::getStudentId, studentId)
				.eq(StudentArchive::getSemester, semester)
				.eq(StudentArchive::getSubjectId, subjectId)
				.last("limit 1"));
		boolean isNew = archive == null;
		if (isNew) {
			Student student = studentMapper.selectById(studentId);
			if (student == null) {
				return;
			}
			Semester sem = deriveSemester();
			archive = new StudentArchive();
			archive.setStudentId(studentId);
			archive.setStudentNo(student.getStudentNo());
			archive.setStudentName(student.getName());
			archive.setSchoolYear(sem.schoolYear);
			archive.setSemester(semester);
			archive.setTermLabel(sem.termLabel);
			archive.setSubjectId(subjectId);
			archive.setStatus(STATUS_DRAFT);
			archive.setReportStatus(REPORT_NONE);
		}
		archive.setProfileSnapshot(buildBaselineSnapshot(detectId, accuracy, weakPoints));
		archive.setBaselineDetectId(detectId);
		archive.setBaselineAt(new Date());
		if (isNew) {
			archiveMapper.insert(archive);
		} else {
			archiveMapper.updateById(archive);
		}
	}

	// ------------------------------------------------------------------ 内部方法

	/** 定位学员本学期档案，不存在时自动建档（以当前薄弱点为初始快照）。 */
	private StudentArchive resolveArchive(String studentId, String archiveId, String subjectId) {
		StudentArchive archive = null;
		if (StringUtils.hasText(archiveId)) {
			archive = archiveMapper.selectById(archiveId);
		}
		if (archive == null) {
			Semester sem = deriveSemester();
			archive = archiveMapper.selectOne(new LambdaQueryWrapper<StudentArchive>()
					.eq(StudentArchive::getStudentId, studentId)
					.eq(StudentArchive::getSemester, sem.semester)
					.last("limit 1"));
		}
		if (archive != null) {
			return archive;
		}
		Student student = studentMapper.selectById(studentId);
		if (student == null) {
			throw new ValidationException("学员不存在");
		}
		Semester sem = deriveSemester();
		archive = new StudentArchive();
		archive.setStudentId(studentId);
		archive.setStudentNo(student.getStudentNo());
		archive.setStudentName(student.getName());
		archive.setSchoolYear(sem.schoolYear);
		archive.setSemester(sem.semester);
		archive.setTermLabel(sem.termLabel);
		archive.setSubjectId(subjectId);
		archive.setStatus(STATUS_DRAFT);
		archive.setReportStatus(REPORT_NONE);
		archive.setProfileSnapshot(buildSnapshotJson(studentId));
		archiveMapper.insert(archive);
		return archive;
	}

	private void fillView(StudentArchiveView view, StudentArchive archive) {
		view.setId(archive.getId());
		view.setStudentId(archive.getStudentId());
		view.setStudentNo(archive.getStudentNo());
		view.setStudentName(archive.getStudentName());
		view.setSchoolYear(archive.getSchoolYear());
		view.setSemester(archive.getSemester());
		view.setTermLabel(archive.getTermLabel());
		view.setSubjectId(archive.getSubjectId());
		view.setTeacherId(archive.getTeacherId());
		view.setTeacherName(archive.getTeacherName());
		view.setStatus(archive.getStatus());
		view.setGoalPlan(archive.getGoalPlan());
		view.setPromise(archive.getPromise());
		view.setReportContent(archive.getReportContent());
		view.setReportStatus(archive.getReportStatus());
		view.setRemark(archive.getRemark());
	}

	private List<StudentArchiveRecordView> listRecords(StudentArchive archive, boolean desc) {
		List<StudentArchiveRecord> records = recordMapper.selectList(
				new LambdaQueryWrapper<StudentArchiveRecord>()
						.eq(StudentArchiveRecord::getArchiveId, archive.getId())
						.orderBy(true, !desc, StudentArchiveRecord::getRecordDate)
						.orderBy(true, !desc, StudentArchiveRecord::getCreateAt));
		return records.stream().map(this::toRecordView).collect(Collectors.toList());
	}

	private StudentArchiveRecordView toRecordView(StudentArchiveRecord record) {
		StudentArchiveRecordView view = new StudentArchiveRecordView();
		view.setId(record.getId());
		view.setArchiveId(record.getArchiveId());
		view.setStudentId(record.getStudentId());
		view.setRecordDate(record.getRecordDate());
		view.setSubjectId(record.getSubjectId());
		view.setTitle(record.getTitle());
		view.setStudySummary(record.getStudySummary());
		view.setSolvedProblems(record.getSolvedProblems());
		view.setStrengthenedKps(record.getStrengthenedKps());
		view.setWeaknesses(record.getWeaknesses());
		view.setHomework(record.getHomework());
		view.setTeacherComment(record.getTeacherComment());
		view.setDurationMinutes(record.getDurationMinutes());
		view.setPoints(record.getPoints());
		view.setCoins(record.getCoins());
		view.setSource(record.getSource());
		view.setStatus(record.getStatus());
		view.setSort(record.getSort());
		return view;
	}

	/** 构建学员当前薄弱知识点列表（含掌握度）。 */
	private List<StudentWeakView> buildWeakPoints(String studentId) {
		List<UserWeakKnowledge> weaks = weakMapper.selectList(new LambdaQueryWrapper<UserWeakKnowledge>()
				.eq(UserWeakKnowledge::getUserId, studentId)
				.eq(UserWeakKnowledge::getStatus, "active")
				.orderByDesc(UserWeakKnowledge::getFirstWeakAt));
		List<StudentWeakView> result = new ArrayList<>();
		for (UserWeakKnowledge w : weaks) {
			KnowledgePoint kp = knowledgePointMapper.selectById(w.getKnowledgePointId());
			UserKnowledgeProgress p = progressMapper.selectOne(new LambdaQueryWrapper<UserKnowledgeProgress>()
					.eq(UserKnowledgeProgress::getUserId, studentId)
					.eq(UserKnowledgeProgress::getKnowledgePointId, w.getKnowledgePointId())
					.last("limit 1"));
			int mastery = p == null || p.getMastery() == null ? 0 : p.getMastery();
			result.add(new StudentWeakView(w.getKnowledgePointId(), kp == null ? null : kp.getName(),
					w.getSubjectId(), mastery));
		}
		return result;
	}

	/** 将当前薄弱点定格为初始档案快照（JSON 数组）。 */
	private String buildSnapshotJson(String studentId) {
		List<StudentWeakView> weakPoints = buildWeakPoints(studentId);
		List<Map<String, Object>> arr = new ArrayList<>();
		for (StudentWeakView w : weakPoints) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("kpId", w.getKpId());
			item.put("name", w.getName());
			item.put("subjectId", w.getSubjectId());
			item.put("mastery", w.getMastery());
			arr.add(item);
		}
		try {
			return objectMapper.writeValueAsString(arr);
		} catch (Exception e) {
			log.warn("序列化档案快照失败：{}", e.getMessage());
			return "[]";
		}
	}

	/** 构建基线快照 JSON：{"baseline":true,"detectId":"...","accuracy":73,"weakPoints":[...]}。 */
	private String buildBaselineSnapshot(String detectId, int accuracy, List<StudentWeakView> weakPoints) {
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("baseline", true);
		root.put("detectId", detectId);
		root.put("accuracy", accuracy);
		List<Map<String, Object>> arr = new ArrayList<>();
		if (weakPoints != null) {
			for (StudentWeakView w : weakPoints) {
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("kpId", w.getKpId());
				item.put("name", w.getName());
				item.put("subjectId", w.getSubjectId());
				item.put("accuracy", w.getMastery());
				arr.add(item);
			}
		}
		root.put("weakPoints", arr);
		try {
			return objectMapper.writeValueAsString(root);
		} catch (Exception e) {
			log.warn("序列化基线快照失败：{}", e.getMessage());
			return "{}";
		}
	}

	/** 解析档案快照中的薄弱知识点名称。 */
	private List<String> parseSnapshot(String snapshot) {
		List<String> names = new ArrayList<>();
		if (!StringUtils.hasText(snapshot)) {
			return names;
		}
		try {
			JsonNode root = objectMapper.readTree(snapshot);
			if (root.isArray()) {
				for (JsonNode node : root) {
					String name = node.path("name").asText("");
					if (!name.isEmpty()) {
						names.add(name);
					}
				}
			}
		} catch (Exception e) {
			log.warn("解析档案快照失败：{}", e.getMessage());
		}
		return names;
	}

	private String buildReportPrompt(StudentArchive archive, List<StudentArchiveRecordView> records,
			List<String> weakNames) {
		StringBuilder sb = new StringBuilder();
		sb.append("学员：").append(archive.getStudentName()).append("\n");
		sb.append("学期：").append(archive.getTermLabel()).append("（").append(archive.getSchoolYear()).append("学年）\n");
		if (StringUtils.hasText(archive.getGoalPlan())) {
			sb.append("本学期目标：").append(archive.getGoalPlan()).append("\n");
		}
		sb.append("上课记录数：").append(records.size()).append("\n");
		for (StudentArchiveRecordView r : records) {
			sb.append("- ").append(r.getRecordDate());
			if (StringUtils.hasText(r.getTitle())) {
				sb.append(" ").append(r.getTitle());
			}
			if (StringUtils.hasText(r.getStudySummary())) {
				sb.append("：").append(trim(r.getStudySummary(), 80));
			}
			sb.append("\n");
		}
		if (!weakNames.isEmpty()) {
			sb.append("当前薄弱知识点：").append(String.join("、", weakNames)).append("\n");
		}
		sb.append("\n请用 400 字以内的中文撰写本学期学习报告，包含：整体表现、主要收获、"
				+ "仍存在的薄弱点与下学期建议。直接输出正文，不要标题与 Markdown。");
		return sb.toString();
	}

	private String buildRuleReport(StudentArchive archive, List<StudentArchiveRecordView> records,
			List<String> weakNames) {
		StringBuilder sb = new StringBuilder();
		sb.append("【").append(archive.getTermLabel()).append(" 学习报告】");
		sb.append("本学期共记录上课 ").append(records.size()).append(" 次。");
		if (StringUtils.hasText(archive.getGoalPlan())) {
			sb.append("学期目标：").append(archive.getGoalPlan()).append("。");
		}
		if (records.isEmpty()) {
			sb.append("暂无上课记录，建议及时补充学习日志。");
		} else {
			sb.append("课堂内容涵盖：");
			String joined = records.stream().map(StudentArchiveRecordView::getTitle)
					.filter(t -> t != null && !t.isEmpty()).limit(6).collect(Collectors.joining("、"));
			sb.append(joined.isEmpty() ? "各单元知识点练习" : joined).append("。");
		}
		if (!weakNames.isEmpty()) {
			sb.append("当前仍需巩固的知识点：").append(String.join("、", weakNames)).append("，建议下阶段重点突破。");
		} else {
			sb.append("当前无明显薄弱知识点，建议保持练习节奏并拓展综合应用。");
		}
		return sb.toString();
	}

	/** 按当前日期推导学年/学期；传入 semester 时仅用其定位，其余按当前日期。 */
	private Semester deriveSemester() {
		LocalDate now = LocalDate.now();
		int year = now.getYear();
		int month = now.getMonthValue();
		Semester sem = new Semester();
		if (month >= 9) {
			sem.schoolYear = year + "-" + (year + 1);
			sem.semester = year + "-1";
			sem.termLabel = "第一学期";
		} else if (month == 1) {
			sem.schoolYear = (year - 1) + "-" + year;
			sem.semester = (year - 1) + "-1";
			sem.termLabel = "第一学期";
		} else {
			sem.schoolYear = (year - 1) + "-" + year;
			sem.semester = (year - 1) + "-2";
			sem.termLabel = "第二学期";
		}
		return sem;
	}

	private String today() {
		return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	private int minutes(Long ms) {
		return ms == null ? 0 : (int) Math.round(ms / 60000.0);
	}

	private String trim(String text, int max) {
		if (text == null) {
			return "";
		}
		String flat = text.replaceAll("\\s+", " ").trim();
		return flat.length() <= max ? flat : flat.substring(0, max) + "…";
	}

	/** 学年/学期推导结果 */
	private static class Semester {
		private String schoolYear;
		private String semester;
		private String termLabel;
	}

}
