package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.AppConsts;
import cn.wisestar.server.core.security.PasswordEncoder;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.dto.knowledge.ChapterView;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointView;
import cn.wisestar.server.domain.dto.knowledge.SectionView;
import cn.wisestar.server.domain.dto.student.StudentActivityRequest;
import cn.wisestar.server.domain.dto.student.StudentActivityView;
import cn.wisestar.server.domain.dto.student.StudentPermissionView;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.domain.dto.student.StudentStatsView;
import cn.wisestar.server.domain.dto.student.StudentSubjectView;
import cn.wisestar.server.domain.dto.student.StudentQuery;
import cn.wisestar.server.domain.dto.student.StudentRequest;
import cn.wisestar.server.domain.dto.student.StudentView;
import cn.wisestar.server.domain.mapper.ChapterViewMapper;
import cn.wisestar.server.domain.mapper.KnowledgePointViewMapper;
import cn.wisestar.server.domain.mapper.SectionViewMapper;
import cn.wisestar.server.domain.mapper.StudentViewMapper;
import cn.wisestar.server.domain.model.Account;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.KnowledgePointQuestion;
import cn.wisestar.server.domain.model.RepoTemplate;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.SectionRepo;
import cn.wisestar.server.domain.model.StudentActivity;
import cn.wisestar.server.domain.model.PracticeRecord;
import cn.wisestar.server.domain.model.Repo;
import cn.wisestar.server.domain.model.StudentPermission;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.mapper.AccountMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.KnowledgePointQuestionMapper;
import cn.wisestar.server.mapper.RepoTemplateMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.SectionRepoMapper;
import cn.wisestar.server.mapper.PracticeRecordMapper;
import cn.wisestar.server.mapper.RepoMapper;
import cn.wisestar.server.mapper.StudentActivityMapper;
import cn.wisestar.server.mapper.StudentPermissionMapper;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.StudentService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 学员管理业务实现（学员管理模块）。
 *
 * <p>【被谁调用】StudentApi（管理端学员管理）。</p>
 * <p>【依赖什么】StudentMapper（t_student CRUD）、AccountMapper（t_account 登录账号）、
 * StudentViewMapper（MapStruct 转换）、PasswordEncoder（初始密码 bcrypt 加密）。</p>
 * <p>【核心逻辑】新增学员：校验姓名+联系号码组合查重 → 生成 8 位唯一学号 →
 * 同一事务内写 t_student + t_account（user_type=Student、auth_account=学号、
 * 初始密码 123456）。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StudentServiceImpl extends BaseService<StudentMapper, Student> implements StudentService {

	/**
	 * 学员初始密码（固定 123456，学员端登录后自行修改）。
	 */
	private static final String DEFAULT_PASSWORD = "123456";

	/**
	 * 学号生成最大重试次数（随机 8 位数字，冲突时重新生成直至唯一）。
	 */
	private static final int STUDENT_NO_RETRY_TIMES = 10;

	private final StudentViewMapper studentViewMapper;

	private final AccountMapper accountMapper;

	private final PasswordEncoder passwordEncoder;

	private final StudentPermissionMapper studentPermissionMapper;

	private final StudentActivityMapper studentActivityMapper;

	private final PracticeRecordMapper practiceRecordMapper;

	private final RepoMapper repoMapper;

	private final SubjectMapper subjectMapper;

	private final ChapterMapper chapterMapper;

	private final SectionMapper sectionMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	private final TemplateMapper templateMapper;

	private final SectionRepoMapper sectionRepoMapper;

	private final RepoTemplateMapper repoTemplateMapper;

	private final KnowledgePointQuestionMapper knowledgePointQuestionMapper;

	private final ChapterViewMapper chapterViewMapper;

	private final SectionViewMapper sectionViewMapper;

	private final KnowledgePointViewMapper knowledgePointViewMapper;

	/**
	 * 新增学员：自动生成学号 + 创建登录账号（同一事务）。
	 */
	@Override
	public StudentView createStudent(StudentRequest request) {
		if (!StringUtils.hasText(request.getName())) {
			throw new ValidationException("学员姓名不能为空");
		}
		if (!StringUtils.hasText(request.getPhone())) {
			throw new ValidationException("联系号码不能为空");
		}
		// 姓名 + 联系号码组合查重，防止同一学员重复录入
		Long duplicateCount = count(Wrappers.<Student>lambdaQuery().eq(Student::getName, request.getName())
				.eq(Student::getPhone, request.getPhone()));
		if (duplicateCount != null && duplicateCount > 0) {
			throw new ValidationException("已存在同姓名、同联系号码的学员，请勿重复录入");
		}

		Student student = studentViewMapper.fromRequest(request);
		student.setStudentNo(generateUniqueStudentNo());
		save(student);

		// 创建学员登录账号（学号即账号，初始密码 123456）
		Account account = new Account();
		account.setUserId(student.getId());
		account.setUserType(AppConsts.USER_TYPE.Student.toString());
		account.setAuthType(AppConsts.AUTH_TYPE.PWD.name());
		account.setAuthAccount(student.getStudentNo());
		account.setAuthSecret(passwordEncoder.encode(DEFAULT_PASSWORD));
		account.setStatus(AppConsts.USER_STATUS.VALID);
		accountMapper.insert(account);

		return studentViewMapper.toView(student);
	}

	/**
	 * 学员有效权限（多条有效订单合并，expire_at > NOW()）。
	 */
	@Override
	public StudentPermissionView permissions() {
		String userId = SecurityContextUtils.getUserId();
		if (getById(userId) == null) {
			throw new ValidationException("当前用户不是学员");
		}
		List<StudentPermission> perms = studentPermissionMapper.selectList(Wrappers.<StudentPermission>lambdaQuery()
				.eq(StudentPermission::getStudentId, userId)
				.gt(StudentPermission::getExpireAt, new Date()));
		StudentPermissionView view = new StudentPermissionView();
		if (perms.isEmpty()) {
			return view;
		}
		// 学科去重并补名称
		Map<String, String> subjectNames = new LinkedHashMap<>();
		perms.forEach(p -> subjectNames.put(p.getSubjectId(), null));
		subjectMapper.selectBatchIds(subjectNames.keySet())
				.forEach(sub -> subjectNames.put(sub.getId(), sub.getName()));
		subjectNames.forEach((id, name) -> view.getSubjects().add(new StudentPermissionView.SubjectItem(id, name)));
		// 年级 / 教材版本去重
		perms.stream().map(StudentPermission::getGrade).filter(StringUtils::hasText).distinct()
				.forEach(view.getGrades()::add);
		perms.stream().map(StudentPermission::getVersion).filter(StringUtils::hasText).distinct()
				.forEach(view.getVersions()::add);
		return view;
	}

	// ============================================================
	// 学员端内容（study/*，按订单有效权限过滤）
	// ============================================================

	/** 当前学员有效权限的学科 id 集合（expire_at > NOW()）；非学员抛校验异常 */
	private Set<String> validSubjectIds() {
		String userId = SecurityContextUtils.getUserId();
		if (getById(userId) == null) {
			throw new ValidationException("当前用户不是学员");
		}
		return studentPermissionMapper.selectList(Wrappers.<StudentPermission>lambdaQuery()
				.eq(StudentPermission::getStudentId, userId)
				.gt(StudentPermission::getExpireAt, new Date()))
				.stream().map(StudentPermission::getSubjectId).collect(Collectors.toSet());
	}

	@Override
	public List<StudentSubjectView> studySubjects() {
		Set<String> subjectIds = validSubjectIds();
		if (subjectIds.isEmpty()) {
			return Collections.emptyList();
		}
		List<Subject> subjects = subjectMapper.selectBatchIds(subjectIds);
		// 各学科有权限的教材版本（去重）
		String userId = SecurityContextUtils.getUserId();
		Map<String, Set<String>> versionsBySubject = studentPermissionMapper.selectList(
						Wrappers.<StudentPermission>lambdaQuery()
								.eq(StudentPermission::getStudentId, userId)
								.gt(StudentPermission::getExpireAt, new Date()))
				.stream().filter(p -> StringUtils.hasText(p.getVersion()))
				.collect(Collectors.groupingBy(StudentPermission::getSubjectId,
						Collectors.mapping(StudentPermission::getVersion, Collectors.toSet())));
		return subjects.stream().map(sub -> {
			StudentSubjectView view = new StudentSubjectView();
			view.setId(sub.getId());
			view.setName(sub.getName());
			view.setIcon(sub.getIcon());
			view.setVersions(new ArrayList<>(versionsBySubject.getOrDefault(sub.getId(), Collections.emptySet())));
			return view;
		}).collect(Collectors.toList());
	}

	@Override
	public List<ChapterView> studyChapters(String subjectId) {
		if (!StringUtils.hasText(subjectId) || !validSubjectIds().contains(subjectId)) {
			return Collections.emptyList();
		}
		List<Chapter> chapters = chapterMapper.selectList(Wrappers.<Chapter>lambdaQuery()
				.eq(Chapter::getSubjectId, subjectId).orderByAsc(Chapter::getSort));
		List<ChapterView> views = chapterViewMapper.toView(chapters);
		views.forEach(v -> v.setSectionCount(
				sectionMapper.selectCount(Wrappers.<Section>lambdaQuery().eq(Section::getChapterId, v.getId()))));
		return views;
	}

	@Override
	public List<SectionView> studySections(String chapterId) {
		if (!StringUtils.hasText(chapterId)) {
			return Collections.emptyList();
		}
		Chapter chapter = chapterMapper.selectById(chapterId);
		if (chapter == null || !validSubjectIds().contains(chapter.getSubjectId())) {
			return Collections.emptyList();
		}
		List<Section> sections = sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
				.eq(Section::getChapterId, chapterId).orderByAsc(Section::getSort));
		List<SectionView> views = sectionViewMapper.toView(sections);
		views.forEach(v -> v.setKnowledgePointCount(knowledgePointMapper.selectCount(
				Wrappers.<KnowledgePoint>lambdaQuery().eq(KnowledgePoint::getSectionId, v.getId()))));
		return views;
	}

	@Override
	public List<KnowledgePointView> studyPoints(String sectionId) {
		if (!StringUtils.hasText(sectionId)) {
			return Collections.emptyList();
		}
		Section section = sectionMapper.selectById(sectionId);
		if (section == null) {
			return Collections.emptyList();
		}
		Chapter chapter = chapterMapper.selectById(section.getChapterId());
		if (chapter == null || !validSubjectIds().contains(chapter.getSubjectId())) {
			return Collections.emptyList();
		}
		return knowledgePointViewMapper.toView(knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
				.eq(KnowledgePoint::getSectionId, sectionId).orderByAsc(KnowledgePoint::getSort)));
	}

	@Override
	public List<StudentQuestionView> studyQuestions(String sectionId, String knowledgePointId, String repoId, Integer count,
			List<String> types, String difficulty) {
		// 归属校验（学科须在学员有效权限内）
		if (StringUtils.hasText(repoId)) {
			Repo repo = repoMapper.selectById(repoId);
			if (repo == null) {
				return Collections.emptyList();
			}
		}
		else if (StringUtils.hasText(sectionId)) {
			Section section = sectionMapper.selectById(sectionId);
			Chapter chapter = section == null ? null : chapterMapper.selectById(section.getChapterId());
			if (chapter == null || !validSubjectIds().contains(chapter.getSubjectId())) {
				return Collections.emptyList();
			}
		}
		else if (StringUtils.hasText(knowledgePointId)) {
			KnowledgePoint point = knowledgePointMapper.selectById(knowledgePointId);
			Section section = point == null ? null : sectionMapper.selectById(point.getSectionId());
			Chapter chapter = section == null ? null : chapterMapper.selectById(section.getChapterId());
			if (chapter == null || !validSubjectIds().contains(chapter.getSubjectId())) {
				return Collections.emptyList();
			}
		}
		else {
			return Collections.emptyList();
		}
		// 题目 id 集合（练习直接出题 + 小节绑定题库 + 知识点绑定题目，去重）
		Set<String> templateIds = new LinkedHashSet<>();
		if (StringUtils.hasText(repoId)) {
			templateMapper.selectList(Wrappers.<Template>lambdaQuery().eq(Template::getRepoId, repoId))
					.forEach(t -> templateIds.add(t.getId()));
		}
		else if (StringUtils.hasText(sectionId)) {
			List<String> repoIds = sectionRepoMapper.selectList(Wrappers.<SectionRepo>lambdaQuery()
							.eq(SectionRepo::getSectionId, sectionId))
					.stream().map(SectionRepo::getRepoId).collect(Collectors.toList());
			if (!repoIds.isEmpty()) {
				// 题目按所属题库（t_template.repo_id）回源，覆盖种子与组题两套关联
				templateMapper.selectList(Wrappers.<Template>lambdaQuery().in(Template::getRepoId, repoIds))
						.forEach(t -> templateIds.add(t.getId()));
			}
		}
		if (StringUtils.hasText(knowledgePointId)) {
			knowledgePointQuestionMapper.selectList(Wrappers.<KnowledgePointQuestion>lambdaQuery()
							.eq(KnowledgePointQuestion::getKnowledgePointId, knowledgePointId))
					.forEach(kq -> templateIds.add(kq.getQuestionId()));
		}
		if (templateIds.isEmpty()) {
			return Collections.emptyList();
		}
		int limit = count == null ? 10 : Math.min(count, 50);
		return templateMapper.selectBatchIds(templateIds).stream()
				.filter(t -> types == null || types.isEmpty()
						|| (t.getQuestionType() != null && types.contains(t.getQuestionType().name())))
				.filter(t -> !StringUtils.hasText(difficulty) || difficulty.equals(t.getDifficulty()))
				.limit(limit)
				.map(this::toStudentQuestionView)
				.collect(Collectors.toList());
	}

	/**
	 * 学员学习统计（基于真实练习记录聚合：累计/今日/分科学币）。
	 */
	@Override
	public StudentStatsView stats() {
		String userId = SecurityContextUtils.getUserId();
		if (getById(userId) == null) {
			throw new ValidationException("当前用户不是学员");
		}
		List<PracticeRecord> records = practiceRecordMapper.selectList(
				Wrappers.<PracticeRecord>lambdaQuery().eq(PracticeRecord::getUserId, userId));
		StudentStatsView view = new StudentStatsView();
		if (records.isEmpty()) {
			return view;
		}
		// 今日起点（0 点）
		Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		// 练习所属学科（record.repoId → t_repo.subject）
		Set<String> repoIds = records.stream().map(PracticeRecord::getRepoId)
				.filter(StringUtils::hasText).collect(Collectors.toSet());
		Map<String, String> repoSubjectMap = new HashMap<>();
		if (!repoIds.isEmpty()) {
			repoMapper.selectBatchIds(repoIds).forEach(r -> repoSubjectMap.put(r.getId(), r.getSubject()));
		}
		Map<String, Integer> coinsMap = new LinkedHashMap<>();
		int totalQuestions = 0, totalCorrect = 0;
		double totalPoints = 0;
		StudentStatsView.TodayStats today = view.getToday();
		for (PracticeRecord record : records) {
			int q = record.getTotalQuestions() == null ? 0 : record.getTotalQuestions();
			int c = record.getCorrectCount() == null ? 0 : record.getCorrectCount();
			double sc = record.getScore() == null ? 0 : record.getScore();
			totalQuestions += q;
			totalCorrect += c;
			totalPoints += sc;
			boolean isToday = record.getCreateAt() != null && !record.getCreateAt().before(todayStart);
			if (isToday) {
				today.setPracticeCount(today.getPracticeCount() + 1);
				today.setQuestionCount(today.getQuestionCount() + q);
				today.setCorrectCount(today.getCorrectCount() + c);
				today.setPoints(today.getPoints() + sc);
				today.setMinutes(today.getMinutes() + (record.getDurationMs() == null ? 0 : record.getDurationMs() / 60000));
			}
			String subject = repoSubjectMap.getOrDefault(record.getRepoId(), "综合练习");
			coinsMap.merge(subject, c, Integer::sum);
		}
		view.setTotalPoints(Math.round(totalPoints * 100) / 100.0);
		view.setPracticeCount(records.size());
		view.setTotalQuestions(totalQuestions);
		view.setTotalCorrect(totalCorrect);
		view.setAccuracy(totalQuestions == 0 ? 0 : (int) Math.round(totalCorrect * 100.0 / totalQuestions));
		today.setAccuracy(today.getQuestionCount() == 0 ? 0
				: (int) Math.round(today.getCorrectCount() * 100.0 / today.getQuestionCount()));
		today.setCoins(today.getCorrectCount());
		coinsMap.forEach((name, coins) -> view.getCoinsBySubject().add(new StudentStatsView.SubjectCoins(name, coins)));
		return view;
	}

	/**
	 * 学员端实时位置上报（按学员覆盖，记录最后活跃时间）。
	 */
	@Override
	public void uploadActivity(StudentActivityRequest request) {
		String userId = SecurityContextUtils.getUserId();
		if (getById(userId) == null) {
			throw new ValidationException("当前用户不是学员");
		}
		StudentActivity existing = studentActivityMapper.selectOne(
				Wrappers.<StudentActivity>lambdaQuery().eq(StudentActivity::getStudentId, userId));
		if (existing == null) {
			StudentActivity activity = new StudentActivity();
			activity.setStudentId(userId);
			activity.setPage(request.getPage());
			activity.setQuestionId(request.getQuestionId());
			activity.setSectionId(request.getSectionId());
			studentActivityMapper.insert(activity);
		}
		else {
			existing.setPage(request.getPage());
			existing.setQuestionId(request.getQuestionId());
			existing.setSectionId(request.getSectionId());
			existing.setUpdateAt(new Date());
			studentActivityMapper.updateById(existing);
		}
	}

	/**
	 * 后台学员实时位置列表（含学员姓名/学号与习题标题）。
	 */
	@Override
	public List<StudentActivityView> listActivities() {
		List<StudentActivity> activities = studentActivityMapper.selectList(
				Wrappers.<StudentActivity>lambdaQuery().orderByDesc(StudentActivity::getUpdateAt));
		if (activities.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, Student> studentMap = this.baseMapper.selectBatchIds(activities.stream()
				.map(StudentActivity::getStudentId).collect(Collectors.toList())).stream()
				.collect(Collectors.toMap(Student::getId, java.util.function.Function.identity(), (a, b) -> a));
		Set<String> questionIds = activities.stream().map(StudentActivity::getQuestionId)
				.filter(StringUtils::hasText).collect(Collectors.toSet());
		Map<String, String> questionTitleMap = new HashMap<>();
		if (!questionIds.isEmpty()) {
			templateMapper.selectBatchIds(questionIds)
					.forEach(t -> questionTitleMap.put(t.getId(), t.getName()));
		}
		return activities.stream().map(activity -> {
			StudentActivityView view = new StudentActivityView();
			view.setStudentId(activity.getStudentId());
			Student student = studentMap.get(activity.getStudentId());
			if (student != null) {
				view.setStudentNo(student.getStudentNo());
				view.setStudentName(student.getName());
			}
			view.setPage(activity.getPage());
			view.setQuestionId(activity.getQuestionId());
			view.setQuestionTitle(questionTitleMap.get(activity.getQuestionId()));
			view.setSectionId(activity.getSectionId());
			view.setUpdateAt(activity.getUpdateAt());
			return view;
		}).collect(Collectors.toList());
	}

	/** 题目转学员端视图（剥离标准答案与选项级答案标记，防作弊） */
	private StudentQuestionView toStudentQuestionView(Template template) {
		StudentQuestionView view = new StudentQuestionView();
		view.setId(template.getId());
		view.setName(template.getName());
		view.setQuestionType(template.getQuestionType());
		view.setTag(template.getTag());
		SurveySchema schema = template.getTemplate();
		if (schema != null) {
			stripAnswer(schema);
		}
		view.setSchema(schema);
		return view;
	}

	/** 递归清除 schema 及其选项的标准答案字段 */
	private void stripAnswer(SurveySchema schema) {
		if (schema.getAttribute() != null) {
			schema.getAttribute().setExamCorrectAnswer(null);
		}
		if (schema.getChildren() != null) {
			schema.getChildren().forEach(this::stripAnswer);
		}
	}

	/**
	 * 学员分页查询（姓名/学号/联系号码模糊匹配）。
	 */
	@Override
	public PaginationResponse<StudentView> pageStudents(StudentQuery query) {
		Page<Student> page = pageByQuery(query,
				Wrappers.<Student>lambdaQuery().like(StringUtils.hasText(query.getName()), Student::getName,
						query.getName())
						.like(StringUtils.hasText(query.getStudentNo()), Student::getStudentNo, query.getStudentNo())
						.like(StringUtils.hasText(query.getPhone()), Student::getPhone, query.getPhone())
						.orderByDesc(Student::getCreateAt));
		return new PaginationResponse<>(page.getTotal(), studentViewMapper.toView(page.getRecords()));
	}

	/**
	 * 更新学员（学号不可修改；姓名+联系号码组合查重排除自身）。
	 */
	@Override
	public void updateStudent(StudentRequest request) {
		if (!StringUtils.hasText(request.getId())) {
			throw new ValidationException("学员ID不能为空");
		}
		Student exist = getById(request.getId());
		if (exist == null) {
			throw new ValidationException("学员不存在");
		}
		// 组合查重（排除自身）
		Long duplicateCount = count(Wrappers.<Student>lambdaQuery().eq(Student::getName, request.getName())
				.eq(Student::getPhone, request.getPhone()).ne(Student::getId, request.getId()));
		if (duplicateCount != null && duplicateCount > 0) {
			throw new ValidationException("已存在同姓名、同联系号码的学员，请勿重复录入");
		}
		Student student = studentViewMapper.fromRequest(request);
		// 学号仅系统生成，update 不允许改动
		student.setStudentNo(exist.getStudentNo());
		updateById(student);
	}

	/**
	 * 删除学员（逻辑删除）。
	 */
	@Override
	public void deleteStudent(StudentRequest request) {
		removeById(request.getId());
	}

	/**
	 * 当前登录学员信息（学员端档案展示，按登录用户ID查 t_student；学员ID即 t_student.id）。
	 */
	@Override
	public StudentView me() {
		String userId = SecurityContextUtils.getUserId();
		Student student = getById(userId);
		if (student == null) {
			throw new ValidationException("当前用户不是学员");
		}
		return studentViewMapper.toView(student);
	}

	/**
	 * 生成 8 位唯一学号：随机 [10000000, 99999999]，与已有学号冲突则重新生成，
	 * 最多重试 {@value #STUDENT_NO_RETRY_TIMES} 次（数据库唯一索引 uk_student_no 兜底）。
	 *
	 * @return 唯一学号
	 */
	private String generateUniqueStudentNo() {
		for (int i = 0; i < STUDENT_NO_RETRY_TIMES; i++) {
			String studentNo = String.valueOf(ThreadLocalRandom.current().nextInt(10000000, 100000000));
			Long existCount = count(Wrappers.<Student>lambdaQuery().eq(Student::getStudentNo, studentNo));
			if (existCount == null || existCount == 0) {
				return studentNo;
			}
		}
		throw new ValidationException("学号生成失败，请重试");
	}

}
