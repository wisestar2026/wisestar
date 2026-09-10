package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.AppConsts;
import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.core.security.PasswordEncoder;
import cn.wisestar.server.core.uitls.AnswerJudgeUtil;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.CampusScope;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.dto.knowledge.ChapterView;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointView;
import cn.wisestar.server.domain.dto.knowledge.SectionView;
import cn.wisestar.server.domain.dto.student.RewardContext;
import cn.wisestar.server.domain.dto.student.StudentActivityRequest;
import cn.wisestar.server.domain.dto.student.StudentActivityView;
import cn.wisestar.server.domain.dto.student.StudentCoinRequest;
import cn.wisestar.server.domain.dto.student.StudentCoinsView;
import cn.wisestar.server.domain.dto.student.StudentKnowledgeDetailView;
import cn.wisestar.server.domain.dto.student.StudentLearningCompleteRequest;
import cn.wisestar.server.domain.dto.student.StudentPermissionView;
import cn.wisestar.server.domain.dto.student.StudentPointsView;
import cn.wisestar.server.domain.dto.student.StudentPreviewCompleteRequest;
import cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView;
import cn.wisestar.server.domain.dto.student.StudentProfileView;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.domain.dto.student.StudentStatsView;
import cn.wisestar.server.domain.dto.student.StudentStudyProgressView;
import cn.wisestar.server.domain.dto.student.StudentSubjectView;
import cn.wisestar.server.domain.dto.student.StudentTodayView;
import cn.wisestar.server.domain.dto.student.StudentWeakView;
import cn.wisestar.server.domain.dto.student.StudentWeakConquerRequest;
import cn.wisestar.server.domain.dto.student.StudentWeakConquerView;
import cn.wisestar.server.domain.dto.student.StudentWrongRedoRequest;
import cn.wisestar.server.domain.dto.student.StudentWrongRedoView;
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
import cn.wisestar.server.domain.model.PracticeDetail;
import cn.wisestar.server.domain.model.RepoTemplate;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.SectionRepo;
import cn.wisestar.server.domain.model.StudentActivity;
import cn.wisestar.server.domain.model.PracticeRecord;
import cn.wisestar.server.domain.model.Repo;
import cn.wisestar.server.domain.model.StudentCoin;
import cn.wisestar.server.domain.model.StudentPermission;
import cn.wisestar.server.domain.model.SubjectSemester;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.domain.model.UserKnowledgeProgress;
import cn.wisestar.server.domain.model.UserLearningRecord;
import cn.wisestar.server.domain.model.UserPoints;
import cn.wisestar.server.domain.model.UserWeakKnowledge;
import cn.wisestar.server.mapper.AccountMapper;
import cn.wisestar.server.mapper.PracticeDetailMapper;
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
import cn.wisestar.server.mapper.StudentCoinMapper;
import cn.wisestar.server.mapper.StudentPermissionMapper;
import cn.wisestar.server.mapper.SubjectSemesterMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.mapper.UserKnowledgeProgressMapper;
import cn.wisestar.server.mapper.UserLearningRecordMapper;
import cn.wisestar.server.mapper.UserPointsMapper;
import cn.wisestar.server.mapper.UserWeakKnowledgeMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.CampusScopeService;
import cn.wisestar.server.service.CampusService;
import cn.wisestar.server.service.EvaluationService;
import cn.wisestar.server.service.RewardService;
import cn.wisestar.server.service.StudentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Stream;
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
@Slf4j
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

	private final StudentCoinMapper studentCoinMapper;

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

	private final CampusScopeService campusScopeService;

	private final CampusService campusService;

	private final RewardService rewardService;

	private final EvaluationService evaluationService;

	private final UserPointsMapper userPointsMapper;

	private final SubjectSemesterMapper subjectSemesterMapper;

	private final UserLearningRecordMapper learningRecordMapper;

	private final UserKnowledgeProgressMapper progressMapper;

	private final UserWeakKnowledgeMapper weakMapper;

	private final PracticeDetailMapper practiceDetailMapper;

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
		// 校区赋值校验：仅可分配到启用的校区
		campusService.checkAssignable(request.getCampus(), null);

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

	/**
	 * 当前学员全部有效权限行（expire_at > NOW()）；非学员抛校验异常。
	 */
	private List<StudentPermission> validPermissions() {
		String userId = SecurityContextUtils.getUserId();
		if (getById(userId) == null) {
			throw new ValidationException("当前用户不是学员");
		}
		return studentPermissionMapper.selectList(Wrappers.<StudentPermission>lambdaQuery()
				.eq(StudentPermission::getStudentId, userId)
				.gt(StudentPermission::getExpireAt, new Date()));
	}

	/** 当前学员有效权限的学科 id 集合（expire_at > NOW()）；非学员抛校验异常 */
	private Set<String> validSubjectIds() {
		return validPermissions().stream().map(StudentPermission::getSubjectId).collect(Collectors.toSet());
	}

	/** 当前学员在指定学科下有效授权的年级集合（去重；未标年级的权限行忽略） */
	private Set<String> validGrades(String subjectId) {
		return validPermissions().stream()
				.filter(p -> subjectId.equals(p.getSubjectId()))
				.map(StudentPermission::getGrade)
				.filter(StringUtils::hasText)
				.collect(Collectors.toSet());
	}

	/**
	 * 当前学员对指定归属（学科+年级）是否有有效权限。
	 * grade 为空时仅按学科判定（兼容历史未标年级内容）。
	 */
	private boolean hasPermission(String subjectId, String grade) {
		return validPermissions().stream().anyMatch(p -> subjectId.equals(p.getSubjectId())
				&& (!StringUtils.hasText(grade) || grade.equals(p.getGrade())));
	}

	@Override
	public List<StudentSubjectView> studySubjects() {
		List<StudentPermission> perms = validPermissions();
		Set<String> subjectIds = perms.stream().map(StudentPermission::getSubjectId).collect(Collectors.toSet());
		if (subjectIds.isEmpty()) {
			return Collections.emptyList();
		}
		List<Subject> subjects = subjectMapper.selectBatchIds(subjectIds);
		// 各学科有权限的教材版本 / 年级（去重）
		Map<String, Set<String>> versionsBySubject = perms.stream()
				.filter(p -> StringUtils.hasText(p.getVersion()))
				.collect(Collectors.groupingBy(StudentPermission::getSubjectId,
						Collectors.mapping(StudentPermission::getVersion, Collectors.toSet())));
		Map<String, Set<String>> gradesBySubject = perms.stream()
				.filter(p -> StringUtils.hasText(p.getGrade()))
				.collect(Collectors.groupingBy(StudentPermission::getSubjectId,
						Collectors.mapping(StudentPermission::getGrade, Collectors.toSet())));
		return subjects.stream().map(sub -> {
			StudentSubjectView view = new StudentSubjectView();
			view.setId(sub.getId());
			view.setName(sub.getName());
			view.setIcon(sub.getIcon());
			view.setVersions(new ArrayList<>(versionsBySubject.getOrDefault(sub.getId(), Collections.emptySet())));
			view.setGrades(new ArrayList<>(gradesBySubject.getOrDefault(sub.getId(), Collections.emptySet())));
			return view;
		}).collect(Collectors.toList());
	}

	@Override
	public List<ChapterView> studyChapters(String subjectId, String grade) {
		if (!StringUtils.hasText(subjectId)) {
			return Collections.emptyList();
		}
		// 学科下授权年级集合；空则无任何可访问章节
		Set<String> permittedGrades = validGrades(subjectId);
		if (permittedGrades.isEmpty()) {
			return Collections.emptyList();
		}
		// 指定了年级但不在该学科授权内 → 越权，返回空
		if (StringUtils.hasText(grade) && !permittedGrades.contains(grade)) {
			return Collections.emptyList();
		}
		// 章节按学科 + 订单授权年级过滤（未传 grade 时返回该学科全部授权年级章节）
		com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Chapter> query =
				Wrappers.<Chapter>lambdaQuery().eq(Chapter::getSubjectId, subjectId);
		if (StringUtils.hasText(grade)) {
			query.eq(Chapter::getGrade, grade);
		} else {
			query.in(Chapter::getGrade, permittedGrades);
		}
		List<Chapter> chapters = chapterMapper.selectList(query.orderByAsc(Chapter::getSort));
		List<ChapterView> views = chapterViewMapper.toView(chapters);
		views.forEach(v -> v.setSectionCount(
				sectionMapper.selectCount(Wrappers.<Section>lambdaQuery().eq(Section::getChapterId, v.getId()))));
		// 学习完成度：章节下各小节完成度平均（有练习数据的小节）
		fillChapterProgress(views);
		return views;
	}

	/** 章节完成度 = 章节下各小节完成度的平均值 */
	private void fillChapterProgress(List<ChapterView> views) {
		if (views.isEmpty()) {
			return;
		}
		String userId = SecurityContextUtils.getUserId();
		for (ChapterView chapter : views) {
			List<SectionView> sections = sectionViewMapper.toView(sectionMapper.selectList(
					Wrappers.<Section>lambdaQuery().eq(Section::getChapterId, chapter.getId())));
			fillProgress(sections);
			// 章节进度 = 全部小节完成度平均值（含未完成小节的 0，避免单个小节完成即整章点亮）
			List<Integer> rates = sections.stream().map(SectionView::getProgress)
					.map(p -> p == null ? 0 : p).collect(Collectors.toList());
			chapter.setProgress(rates.isEmpty() ? 0
					: rates.stream().mapToInt(Integer::intValue).sum() / rates.size());
		}
	}

	@Override
	public List<SectionView> studySections(String chapterId) {
		if (!StringUtils.hasText(chapterId)) {
			return Collections.emptyList();
		}
		Chapter chapter = chapterMapper.selectById(chapterId);
		if (chapter == null || !hasPermission(chapter.getSubjectId(), chapter.getGrade())) {
			return Collections.emptyList();
		}
		List<Section> sections = sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
				.eq(Section::getChapterId, chapterId).orderByAsc(Section::getSort));
		List<SectionView> views = sectionViewMapper.toView(sections);
		views.forEach(v -> v.setKnowledgePointCount(knowledgePointMapper.selectCount(
				Wrappers.<KnowledgePoint>lambdaQuery().eq(KnowledgePoint::getSectionId, v.getId()))));
		// 学习完成度：各小节绑定练习的学员最高正确率
		fillProgress(views);
		return views;
	}

	/** 按小节绑定练习 + 小节知识点练习计算学员学习完成度（最高正确率） */
	private void fillProgress(List<SectionView> views) {
		if (views.isEmpty()) {
			return;
		}
		String userId = SecurityContextUtils.getUserId();
		List<String> sectionIds = views.stream().map(SectionView::getId).collect(Collectors.toList());
		// 小节绑定练习
		Map<String, List<String>> reposBySection = sectionRepoMapper.selectList(
						Wrappers.<SectionRepo>lambdaQuery().in(SectionRepo::getSectionId, sectionIds))
				.stream().collect(Collectors.groupingBy(SectionRepo::getSectionId,
						Collectors.mapping(SectionRepo::getRepoId, Collectors.toList())));
		// 小节下知识点
		Map<String, List<String>> kpsBySection = knowledgePointMapper.selectList(
						Wrappers.<KnowledgePoint>lambdaQuery().in(KnowledgePoint::getSectionId, sectionIds))
				.stream().collect(Collectors.groupingBy(KnowledgePoint::getSectionId,
						Collectors.mapping(KnowledgePoint::getId, Collectors.toList())));
		Set<String> allRepoIds = reposBySection.values().stream().flatMap(List::stream).collect(Collectors.toSet());
		Set<String> allKpIds = kpsBySection.values().stream().flatMap(List::stream).collect(Collectors.toSet());
		// 学员练习记录：按小节/练习/知识点任一匹配（一次查询）
		Map<String, Integer> sectionRate = new HashMap<>();
		Map<String, Integer> repoRate = new HashMap<>();
		Map<String, Integer> kpRate = new HashMap<>();
		if (userId != null && (!allRepoIds.isEmpty() || !allKpIds.isEmpty())) {
			com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PracticeRecord> wrapper =
					Wrappers.<PracticeRecord>lambdaQuery().eq(PracticeRecord::getUserId, userId);
			boolean or = false;
			if (!allRepoIds.isEmpty()) { wrapper.in(PracticeRecord::getRepoId, allRepoIds); or = true; }
			if (!allKpIds.isEmpty()) {
				if (or) { wrapper.or().in(PracticeRecord::getKnowledgePointId, allKpIds); } else { wrapper.in(PracticeRecord::getKnowledgePointId, allKpIds); }
			}
			wrapper.or().in(PracticeRecord::getSectionId, sectionIds);
			List<PracticeRecord> records = practiceRecordMapper.selectList(wrapper);
			for (PracticeRecord r : records) {
				if (r.getTotalScore() == null || r.getTotalScore() <= 0) continue;
				int rate = (int) Math.round((r.getScore() == null ? 0 : r.getScore()) * 100.0 / r.getTotalScore());
				if (r.getSectionId() != null) sectionRate.merge(r.getSectionId(), rate, Math::max);
				if (r.getRepoId() != null) repoRate.merge(r.getRepoId(), rate, Math::max);
				if (r.getKnowledgePointId() != null) kpRate.merge(r.getKnowledgePointId(), rate, Math::max);
			}
		}
		views.forEach(v -> {
			int sectionBest = sectionRate.getOrDefault(v.getId(), 0);
			int repoBest = reposBySection.getOrDefault(v.getId(), Collections.emptyList()).stream()
					.mapToInt(r -> repoRate.getOrDefault(r, 0)).max().orElse(0);
			int kpBest = kpsBySection.getOrDefault(v.getId(), Collections.emptyList()).stream()
					.mapToInt(k -> kpRate.getOrDefault(k, 0)).max().orElse(0);
			v.setProgress(Math.max(sectionBest, Math.max(repoBest, kpBest)));
		});
	}

	/** 学员各知识点（knowledgePointId）最高正确率（0-100） */
	private Map<String, Integer> kpRateMap(String userId, Set<String> kpIds) {
		Map<String, Integer> kpRate = new HashMap<>();
		if (userId == null || kpIds.isEmpty()) {
			return kpRate;
		}
		List<PracticeRecord> records = practiceRecordMapper.selectList(Wrappers.<PracticeRecord>lambdaQuery()
				.eq(PracticeRecord::getUserId, userId)
				.in(PracticeRecord::getKnowledgePointId, kpIds));
		for (PracticeRecord r : records) {
			if (r.getKnowledgePointId() != null && r.getTotalScore() != null && r.getTotalScore() > 0) {
				int rate = (int) Math.round((r.getScore() == null ? 0 : r.getScore()) * 100.0 / r.getTotalScore());
				kpRate.merge(r.getKnowledgePointId(), rate, Math::max);
			}
		}
		return kpRate;
	}

	/** 学员各练习（repoId）最高正确率（0-100） */
	private Map<String, Integer> repoRateMap(String userId, Set<String> repoIds) {
		Map<String, Integer> repoRate = new HashMap<>();
		if (userId == null || repoIds.isEmpty()) {
			return repoRate;
		}
		List<PracticeRecord> records = practiceRecordMapper.selectList(Wrappers.<PracticeRecord>lambdaQuery()
				.eq(PracticeRecord::getUserId, userId)
				.in(PracticeRecord::getRepoId, repoIds));
		for (PracticeRecord r : records) {
			if (r.getRepoId() != null && r.getTotalScore() != null && r.getTotalScore() > 0) {
				int rate = (int) Math.round((r.getScore() == null ? 0 : r.getScore()) * 100.0 / r.getTotalScore());
				repoRate.merge(r.getRepoId(), rate, Math::max);
			}
		}
		return repoRate;
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
		if (chapter == null || !hasPermission(chapter.getSubjectId(), chapter.getGrade())) {
			return Collections.emptyList();
		}
		return knowledgePointViewMapper.toView(knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
				.eq(KnowledgePoint::getSectionId, sectionId).orderByAsc(KnowledgePoint::getSort)));
	}

	/**
	 * 学科学习进度：章节 → 知识点掌握度/评级/薄弱（真实评价值，不返回 mock）。
	 */
	@Override
	public StudentStudyProgressView studyProgress(String subjectId, String versionId) {
		String userId = currentStudentId();
		StudentStudyProgressView view = new StudentStudyProgressView();
		if (!StringUtils.hasText(subjectId) || validGrades(subjectId).isEmpty()) {
			return view;
		}
		com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Chapter> cq = Wrappers
				.<Chapter>lambdaQuery().eq(Chapter::getSubjectId, subjectId);
		if (StringUtils.hasText(versionId)) {
			cq.eq(Chapter::getVersion, versionId);
		}
		List<Chapter> chapters = chapterMapper.selectList(cq.orderByAsc(Chapter::getSort));
		if (chapters.isEmpty()) {
			return view;
		}
		// 学员该学科掌握度 + 活跃薄弱点
		List<UserKnowledgeProgress> progresses = progressMapper.selectList(
				Wrappers.<UserKnowledgeProgress>lambdaQuery().eq(UserKnowledgeProgress::getUserId, userId)
						.eq(UserKnowledgeProgress::getSubjectId, subjectId));
		Map<String, UserKnowledgeProgress> progressMap = new HashMap<>();
		for (UserKnowledgeProgress p : progresses) {
			if (StringUtils.hasText(p.getKnowledgePointId())) {
				progressMap.put(p.getKnowledgePointId(), p);
			}
		}
		Set<String> weakKpIds = weakMapper
				.selectList(Wrappers.<UserWeakKnowledge>lambdaQuery()
						.eq(UserWeakKnowledge::getUserId, userId)
						.eq(UserWeakKnowledge::getStatus, "active"))
				.stream().map(UserWeakKnowledge::getKnowledgePointId).filter(StringUtils::hasText)
				.collect(Collectors.toSet());
		for (Chapter chapter : chapters) {
			StudentStudyProgressView.Chapter cv = new StudentStudyProgressView.Chapter();
			cv.setId(chapter.getId());
			cv.setName(chapter.getName());
			cv.setIcon(chapter.getIcon());
			List<Section> sections = sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
					.eq(Section::getChapterId, chapter.getId()).orderByAsc(Section::getSort));
			for (Section section : sections) {
				List<KnowledgePoint> kps = knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
						.eq(KnowledgePoint::getSectionId, section.getId()).orderByAsc(KnowledgePoint::getSort));
				for (KnowledgePoint kp : kps) {
					StudentStudyProgressView.Kp kv = new StudentStudyProgressView.Kp();
					kv.setId(kp.getId());
					kv.setName(kp.getName());
					kv.setSectionId(section.getId());
					UserKnowledgeProgress p = progressMap.get(kp.getId());
					int mastery = p == null || p.getMastery() == null ? 0 : p.getMastery();
					kv.setMastery(mastery);
					kv.setLevel(p != null && StringUtils.hasText(p.getLevel()) ? p.getLevel() : levelOf(mastery));
					kv.setWeak(weakKpIds.contains(kp.getId()));
					cv.getKps().add(kv);
				}
			}
			view.getChapters().add(cv);
		}
		return view;
	}

	/** 掌握度 → 五级评级。 */
	private static String levelOf(int mastery) {
		if (mastery >= 85) {
			return "精通";
		}
		if (mastery >= 70) {
			return "熟练";
		}
		if (mastery >= 55) {
			return "夯实";
		}
		if (mastery >= 40) {
			return "待巩固";
		}
		return "待攻克";
	}

	@Override
	public List<StudentQuestionView> studyQuestions(String sectionId, String knowledgePointId, String repoId, String questionId,
			Integer count, List<String> types, String difficulty, Boolean exposeAnswer) {
		boolean expose = Boolean.TRUE.equals(exposeAnswer);
		// 单题重做：仅允许取本人未订正的错题（校验归属，防越权取题）
		if (StringUtils.hasText(questionId)) {
			if (findUncorrectedWrong(SecurityContextUtils.getUserId(), questionId) == null) {
				return Collections.emptyList();
			}
			Template single = templateMapper.selectById(questionId);
			if (single == null) {
				return Collections.emptyList();
			}
			return Collections.singletonList(expose ? toStudentQuestionViewWithAnswer(single)
					: toStudentQuestionView(single));
		}
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
			if (chapter == null || !hasPermission(chapter.getSubjectId(), chapter.getGrade())) {
				return Collections.emptyList();
			}
		}
		else if (StringUtils.hasText(knowledgePointId)) {
			KnowledgePoint point = knowledgePointMapper.selectById(knowledgePointId);
			Section section = point == null ? null : sectionMapper.selectById(point.getSectionId());
			Chapter chapter = section == null ? null : chapterMapper.selectById(section.getChapterId());
			if (chapter == null || !hasPermission(chapter.getSubjectId(), chapter.getGrade())) {
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
		// count 为空时返回全部命中题目（练习/小节通关应覆盖绑定题库的全部题）；
		// 显式传 count（消灭错题等）时按指定数量截断（上限 50）
		Stream<Template> stream = templateMapper.selectBatchIds(templateIds).stream()
				.filter(t -> types == null || types.isEmpty()
						|| (t.getQuestionType() != null && types.contains(t.getQuestionType().name())))
				.filter(t -> !StringUtils.hasText(difficulty) || difficulty.equals(t.getDifficulty()));
		if (count != null) {
			stream = stream.limit(Math.min(count, 50));
		}
		return stream.map(t -> expose ? toStudentQuestionViewWithAnswer(t) : toStudentQuestionView(t))
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
		view.setPracticeCount(records.size());
		view.setTotalQuestions(totalQuestions);
		view.setTotalCorrect(totalCorrect);
		view.setAccuracy(totalQuestions == 0 ? 0 : (int) Math.round(totalCorrect * 100.0 / totalQuestions));
		today.setAccuracy(today.getQuestionCount() == 0 ? 0
				: (int) Math.round(today.getCorrectCount() * 100.0 / today.getQuestionCount()));
		// 积分/学币改读账本（唯一事实源）
		UserPoints up = findPoints(userId);
		view.setTotalPoints(up == null || up.getPoints() == null ? 0 : up.getPoints());
		List<UserLearningRecord> ledger = learningRecordMapper.selectList(Wrappers.<UserLearningRecord>lambdaQuery()
				.eq(UserLearningRecord::getUserId, userId).ge(UserLearningRecord::getLearnedAt, todayStart));
		today.setPoints(ledger.stream().mapToInt(r -> r.getPoints() == null ? 0 : r.getPoints()).sum());
		today.setCoins(ledger.stream().mapToInt(r -> r.getCoins() == null ? 0 : r.getCoins()).sum());
		// 分科学币：本学期账本（手动发币单列 manualCoins）
		String semester = StudentRewardConstants.currentSemester();
		subjectSemesterMapper.selectList(Wrappers.<SubjectSemester>lambdaQuery()
				.eq(SubjectSemester::getUserId, userId).eq(SubjectSemester::getSemester, semester))
				.forEach(ss -> view.getCoinsBySubject().add(new StudentStatsView.SubjectCoins(
						subjectName(ss.getSubjectId()), ss.getCoins() == null ? 0 : ss.getCoins())));
		view.setManualCoins(studentCoinMapper.selectList(Wrappers.<StudentCoin>lambdaQuery()
						.eq(StudentCoin::getStudentId, userId))
				.stream().mapToInt(c -> c.getCoins() == null ? 0 : c.getCoins()).sum());
		return view;
	}

	/**
	 * 老师给学员发放学币。
	 */
	@Override
	public void addCoin(StudentCoinRequest request) {
		if (request.getStudentId() == null || getById(request.getStudentId()) == null) {
			throw new ValidationException("学员不存在");
		}
		if (request.getCoins() == null || request.getCoins() == 0) {
			throw new ValidationException("学币数量不能为空且不能为 0");
		}
		StudentCoin coin = new StudentCoin();
		coin.setStudentId(request.getStudentId());
		coin.setCoins(request.getCoins());
		coin.setReason(request.getReason());
		studentCoinMapper.insert(coin);
	}

	/**
	 * 学员预习完成：标记该小节/知识点预习完成并结算奖励（同一目标仅首次发放）。
	 */
	@Override
	public StudentPreviewCompleteView completePreview(StudentPreviewCompleteRequest request) {
		String userId = SecurityContextUtils.getUserId();
		if (userId == null || getById(userId) == null) {
			throw new ValidationException("当前用户不是学员");
		}
		boolean byKp = StringUtils.hasText(request.getKnowledgePointId());
		boolean bySection = StringUtils.hasText(request.getSectionId());
		if (!byKp && !bySection) {
			throw new ValidationException("缺少小节或知识点");
		}
		// 权限收敛：预习目标必须落在学员有效订单权限内（防止越权刷奖励）
		Section section;
		if (byKp) {
			KnowledgePoint kp = knowledgePointMapper.selectById(request.getKnowledgePointId());
			section = kp == null ? null : sectionMapper.selectById(kp.getSectionId());
		} else {
			section = sectionMapper.selectById(request.getSectionId());
		}
		Chapter chapter = section == null ? null : chapterMapper.selectById(section.getChapterId());
		if (chapter == null || !hasPermission(chapter.getSubjectId(), chapter.getGrade())) {
			throw new ValidationException("预习内容不在权限范围");
		}
		// 奖励结算：统一走积分·学币账本（refId 幂等 + 7 天防刷，与内容配置无关）
		RewardContext context = new RewardContext();
		context.setUserId(userId);
		context.setActionType(StudentRewardConstants.ACTION_PREVIEW);
		context.setSubjectId(chapter.getSubjectId());
		context.setKnowledgePointId(byKp ? request.getKnowledgePointId() : null);
		context.setSectionId(section.getId());
		context.setRefId("preview:" + (byKp ? request.getKnowledgePointId() : section.getId()));
		return rewardService.settle(context);
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
			activity.setUpdateAt(new Date());
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

	/** 题目转学员端视图（含标准答案，试炼实时判分用） */
	private StudentQuestionView toStudentQuestionViewWithAnswer(Template template) {
		StudentQuestionView view = new StudentQuestionView();
		view.setId(template.getId());
		view.setName(template.getName());
		view.setQuestionType(template.getQuestionType());
		view.setTag(template.getTag());
		view.setSchema(template.getTemplate());
		return view;
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
		CampusScope scope = campusScopeService.resolveScope();
		if (scope.isEmpty()) {
			return new PaginationResponse<>(0L, Collections.emptyList());
		}
		LambdaQueryWrapper<Student> wrapper = Wrappers.<Student>lambdaQuery()
				.like(StringUtils.hasText(query.getName()), Student::getName, query.getName())
				.like(StringUtils.hasText(query.getStudentNo()), Student::getStudentNo, query.getStudentNo())
				.like(StringUtils.hasText(query.getPhone()), Student::getPhone, query.getPhone())
				.eq(StringUtils.hasText(query.getCampus()), Student::getCampus, query.getCampus())
				.orderByDesc(Student::getCreateAt);
		if (scope.isScoped()) {
			Set<String> names = scope.getCampusNames();
			if (names.isEmpty()) {
				return new PaginationResponse<>(0L, Collections.emptyList());
			}
			wrapper.in(Student::getCampus, names);
		}
		Page<Student> page = pageByQuery(query, wrapper);
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
		assertStudentInScope(exist);
		// 校区赋值校验：可保留历史停用校区原值，其余必须为启用校区
		campusService.checkAssignable(request.getCampus(), exist.getCampus());
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
	 * 删除学员（逻辑删除；校区数据权限范围内）。
	 */
	@Override
	public void deleteStudent(StudentRequest request) {
		Student exist = getById(request.getId());
		if (exist == null) {
			throw new ValidationException("学员不存在");
		}
		assertStudentInScope(exist);
		removeById(request.getId());
	}

	/**
	 * 校验当前账号对该学员具备数据可见/操作权限（SCOPED/EMPTY 时执行校区匹配）。
	 */
	private void assertStudentInScope(Student student) {
		CampusScope scope = campusScopeService.resolveScope();
		if (scope.isAll()) {
			return;
		}
		if (scope.isEmpty() || student.getCampus() == null
				|| !scope.getCampusNames().contains(student.getCampus())) {
			throw new ValidationException("无权访问该学员（校区数据权限）");
		}
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
	 * 校验并返回当前登录学员ID。
	 */
	private String currentStudentId() {
		String userId = SecurityContextUtils.getUserId();
		if (userId == null || getById(userId) == null) {
			throw new ValidationException("当前用户不是学员");
		}
		return userId;
	}

	/**
	 * 个人中心档案。
	 */
	@Override
	public StudentProfileView profile() {
		String userId = currentStudentId();
		Student student = getById(userId);
		StudentProfileView view = new StudentProfileView();
		view.setName(student.getName());
		view.setStudentNo(student.getStudentNo());
		UserPoints up = findPoints(userId);
		if (up != null) {
			int level = up.getTitleLevel() == null ? 1 : up.getTitleLevel();
			view.setPoints(up.getPoints() == null ? 0 : up.getPoints());
			view.setTitleLevel(level);
			view.setTitleName(up.getTitleName() == null
					? StudentRewardConstants.titleName(level) : up.getTitleName());
		}
		List<UserKnowledgeProgress> progresses = progressMapper.selectList(
				Wrappers.<UserKnowledgeProgress>lambdaQuery().eq(UserKnowledgeProgress::getUserId, userId));
		view.setKps(progresses.size());
		view.setChapters((int) progresses.stream()
				.filter(p -> p.getChapterId() != null && p.getMastery() != null && p.getMastery() >= 55)
				.map(UserKnowledgeProgress::getChapterId).distinct().count());
		view.setWeak(activeWeakCount(userId));
		return view;
	}

	/**
	 * 个人中心-积分板块。
	 */
	@Override
	public StudentPointsView points() {
		String userId = currentStudentId();
		UserPoints up = findPoints(userId);
		int points = up == null || up.getPoints() == null ? 0 : up.getPoints();
		int level = up == null || up.getTitleLevel() == null ? 1 : up.getTitleLevel();
		StudentPointsView view = new StudentPointsView();
		view.setPoints(points);
		view.setTitleLevel(level);
		view.setTitleName(up == null || up.getTitleName() == null
				? StudentRewardConstants.titleName(level) : up.getTitleName());
		int next = StudentRewardConstants.nextTitlePoints(level);
		view.setNextTitlePoints(next);
		if (next > 0) {
			view.setNextTitleName(StudentRewardConstants.titleName(level + 1));
			view.setPointsToNextTitle(Math.max(0, next - points));
		}
		for (String action : RULE_ACTIONS) {
			int[] r = StudentRewardConstants.reward(action, null);
			if (r == null) {
				continue;
			}
			view.getRules().add(new StudentPointsView.RuleItem(action,
					StudentRewardConstants.actionLabel(action), r[1], r[0]));
		}
		List<UserLearningRecord> records = learningRecordMapper.selectList(
				Wrappers.<UserLearningRecord>lambdaQuery().eq(UserLearningRecord::getUserId, userId)
						.orderByDesc(UserLearningRecord::getLearnedAt).last("limit 20"));
		for (UserLearningRecord record : records) {
			view.getRecent().add(new StudentPointsView.RecordItem(
					StudentRewardConstants.actionLabel(record.getActionType()),
					record.getPoints() == null ? 0 : record.getPoints(),
					record.getCoins() == null ? 0 : record.getCoins(), record.getLearnedAt()));
		}
		return view;
	}

	/**
	 * 本学期学习币（分学科 + 手动发币合计）。
	 */
	@Override
	public StudentCoinsView coins() {
		String userId = currentStudentId();
		String semester = StudentRewardConstants.currentSemester();
		StudentCoinsView view = new StudentCoinsView();
		List<SubjectSemester> list = subjectSemesterMapper.selectList(Wrappers.<SubjectSemester>lambdaQuery()
				.eq(SubjectSemester::getUserId, userId).eq(SubjectSemester::getSemester, semester));
		int total = 0;
		for (SubjectSemester ss : list) {
			StudentCoinsView.SubjectCoin sc = new StudentCoinsView.SubjectCoin();
			sc.setSubjectId(ss.getSubjectId());
			sc.setSubjectName(subjectName(ss.getSubjectId()));
			sc.setCoins(ss.getCoins() == null ? 0 : ss.getCoins());
			sc.setReachedLimit(Boolean.TRUE.equals(ss.getReachedLimit()));
			view.getList().add(sc);
			total += sc.getCoins();
		}
		int manual = studentCoinMapper.selectList(Wrappers.<StudentCoin>lambdaQuery()
						.eq(StudentCoin::getStudentId, userId)).stream()
				.mapToInt(c -> c.getCoins() == null ? 0 : c.getCoins()).sum();
		view.setTotal(total + manual);
		return view;
	}

	/**
	 * 学员端主页今日总览 + 积分获取引导。
	 */
	@Override
	public StudentTodayView today() {
		String userId = currentStudentId();
		Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		List<PracticeRecord> records = practiceRecordMapper.selectList(Wrappers.<PracticeRecord>lambdaQuery()
				.eq(PracticeRecord::getUserId, userId).ge(PracticeRecord::getCreateAt, todayStart));
		List<UserLearningRecord> ledger = learningRecordMapper.selectList(Wrappers.<UserLearningRecord>lambdaQuery()
				.eq(UserLearningRecord::getUserId, userId).ge(UserLearningRecord::getLearnedAt, todayStart));
		StudentTodayView view = new StudentTodayView();
		view.setMinutes(records.stream()
				.mapToLong(r -> r.getDurationMs() == null ? 0 : r.getDurationMs() / 60000).sum());
		view.setKps((int) records.stream()
				.map(r -> StringUtils.hasText(r.getKnowledgePointId()) ? r.getKnowledgePointId() : r.getSectionId())
				.filter(StringUtils::hasText).distinct().count());
		view.setPoints(ledger.stream().mapToInt(r -> r.getPoints() == null ? 0 : r.getPoints()).sum());
		view.setCoins(ledger.stream().mapToInt(r -> r.getCoins() == null ? 0 : r.getCoins()).sum());
		Set<String> done = ledger.stream().map(UserLearningRecord::getActionType)
				.filter(StringUtils::hasText).collect(Collectors.toSet());
		view.getGuides().add(guide(StudentRewardConstants.ACTION_PREVIEW, "study", done));
		view.getGuides().add(guide(StudentRewardConstants.ACTION_PRACTICE, "study", done));
		view.getGuides().add(guide(StudentRewardConstants.ACTION_TRIAL, "study", done));
		view.getGuides().add(guide(StudentRewardConstants.ACTION_WRONG_CORRECT, "wrong", done));
		view.getGuides().add(guide(StudentRewardConstants.ACTION_WEAK_CONQUER, "weak", done));
		return view;
	}

	/**
	 * 薄弱知识点列表。
	 */
	@Override
	public List<StudentWeakView> weakList() {
		String userId = currentStudentId();
		List<UserWeakKnowledge> weaks = weakMapper.selectList(Wrappers.<UserWeakKnowledge>lambdaQuery()
				.eq(UserWeakKnowledge::getUserId, userId).eq(UserWeakKnowledge::getStatus, "active")
				.orderByDesc(UserWeakKnowledge::getFirstWeakAt));
		List<StudentWeakView> result = new ArrayList<>();
		for (UserWeakKnowledge w : weaks) {
			KnowledgePoint kp = knowledgePointMapper.selectById(w.getKnowledgePointId());
			UserKnowledgeProgress p = progressMapper.selectOne(Wrappers.<UserKnowledgeProgress>lambdaQuery()
					.eq(UserKnowledgeProgress::getUserId, userId)
					.eq(UserKnowledgeProgress::getKnowledgePointId, w.getKnowledgePointId()).last("limit 1"));
			int mastery = p == null || p.getMastery() == null ? 0 : p.getMastery();
			result.add(new StudentWeakView(w.getKnowledgePointId(), kp == null ? null : kp.getName(),
					w.getSubjectId(), mastery));
		}
		return result;
	}

	/**
	 * 知识点详情（掌握度/评级/薄弱/预习状态）。
	 */
	@Override
	public StudentKnowledgeDetailView knowledgeDetail(String knowledgePointId) {
		String userId = currentStudentId();
		if (!StringUtils.hasText(knowledgePointId)) {
			throw new ValidationException("知识点ID不能为空");
		}
		KnowledgePoint kp = knowledgePointMapper.selectById(knowledgePointId);
		if (kp == null) {
			throw new ValidationException("知识点不存在");
		}
		StudentKnowledgeDetailView view = new StudentKnowledgeDetailView();
		view.setId(knowledgePointId);
		view.setName(kp.getName());
		view.setDesc(kp.getContent());
		UserKnowledgeProgress p = progressMapper.selectOne(Wrappers.<UserKnowledgeProgress>lambdaQuery()
				.eq(UserKnowledgeProgress::getUserId, userId)
				.eq(UserKnowledgeProgress::getKnowledgePointId, knowledgePointId).last("limit 1"));
		if (p != null) {
			int mastery = p.getMastery() == null ? 0 : p.getMastery();
			view.setMastery(mastery);
			view.setLevel(StringUtils.hasText(p.getLevel()) ? p.getLevel() : levelOf(mastery));
		}
		else {
			view.setMastery(0);
			view.setLevel("待攻克");
		}
		Long weakCount = weakMapper.selectCount(Wrappers.<UserWeakKnowledge>lambdaQuery()
				.eq(UserWeakKnowledge::getUserId, userId)
				.eq(UserWeakKnowledge::getKnowledgePointId, knowledgePointId)
				.eq(UserWeakKnowledge::getStatus, "active"));
		view.setWeak(weakCount != null && weakCount > 0);
		Long previewCount = learningRecordMapper.selectCount(Wrappers.<UserLearningRecord>lambdaQuery()
				.eq(UserLearningRecord::getUserId, userId)
				.eq(UserLearningRecord::getActionType, StudentRewardConstants.ACTION_PREVIEW)
				.and(w -> w.eq(UserLearningRecord::getKnowledgePointId, knowledgePointId)
						.or().eq(UserLearningRecord::getSectionId, kp.getSectionId())));
		view.setPreviewed(previewCount != null && previewCount > 0);
		return view;
	}

	/**
	 * 学习完成统一结算。
	 */
	@Override
	public StudentPreviewCompleteView completeLearning(StudentLearningCompleteRequest request) {
		String userId = currentStudentId();
		if (request == null || !StringUtils.hasText(request.getActionType())) {
			throw new ValidationException("缺少学习行为类型");
		}
		RewardContext context = new RewardContext();
		context.setUserId(userId);
		context.setActionType(request.getActionType());
		context.setKnowledgePointId(request.getKnowledgePointId());
		context.setSectionId(request.getSectionId());
		context.setChapterId(request.getChapterId());
		context.setRefId(request.getRefId());
		context.setDurationMs(request.getDurationMs());
		context.setCorrectRate(request.getCorrectRate());
		context.setSubjectId(resolveSubjectId(request.getKnowledgePointId(), request.getSectionId()));
		return rewardService.settle(context);
	}

	/**
	 * 错题重做：答对则订正、移出错题本、刷新薄弱并结算奖励。
	 */
	@Override
	public StudentWrongRedoView wrongRedo(StudentWrongRedoRequest request) {
		String userId = currentStudentId();
		if (request == null || !StringUtils.hasText(request.getQuestionId())) {
			throw new ValidationException("题目ID不能为空");
		}
		PracticeDetail detail = findUncorrectedWrong(userId, request.getQuestionId());
		if (detail == null) {
			throw new ValidationException("未找到该错题");
		}
		Template template = templateMapper.selectById(request.getQuestionId());
		if (template == null || template.getTemplate() == null) {
			throw new ValidationException("题目不存在");
		}
		StudentWrongRedoView view = new StudentWrongRedoView();
		view.setOk(true);
		Integer correct;
		try {
			correct = AnswerJudgeUtil.evaluate(template.getTemplate(), request.getAnswer());
		}
		catch (Exception e) {
			throw new ValidationException("作答内容无法判分");
		}
		if (!Integer.valueOf(1).equals(correct)) {
			return view;
		}
		detail.setCorrected(true);
		detail.setCorrectedAt(new Date());
		practiceDetailMapper.updateById(detail);
		view.setRemoved(true);

		PracticeRecord record = practiceRecordMapper.selectById(detail.getPracticeId());
		String kpId = resolveKpIdFromTemplate(template, record);
		if (StringUtils.hasText(kpId)) {
			try {
				evaluationService.refreshWeakAfterCorrection(userId, kpId);
			}
			catch (Exception e) {
				log.warn("wrong redo refresh weak failed: user={}, kp={}", userId, kpId, e);
			}
		}
		RewardContext rc = new RewardContext();
		rc.setUserId(userId);
		rc.setActionType(StudentRewardConstants.ACTION_WRONG_CORRECT);
		rc.setKnowledgePointId(kpId);
		rc.setSectionId(record == null ? null : record.getSectionId());
		rc.setSubjectId(resolveSubjectId(kpId, record == null ? null : record.getSectionId()));
		rc.setRefId(detail.getId());
		try {
			StudentPreviewCompleteView reward = rewardService.settle(rc);
			view.setCoins(reward.getCoins());
			view.setPoints(reward.getPoints());
		}
		catch (Exception e) {
			log.warn("wrong redo reward failed", e);
		}
		return view;
	}

	/**
	 * 薄弱知识点攻克。
	 */
	@Override
	public StudentWeakConquerView weakConquer(StudentWeakConquerRequest request) {
		String userId = currentStudentId();
		if (request == null || !StringUtils.hasText(request.getKnowledgePointId())) {
			throw new ValidationException("知识点ID不能为空");
		}
		StudentWeakConquerView view = new StudentWeakConquerView();
		view.setOk(true);
		Long active = weakMapper.selectCount(Wrappers.<UserWeakKnowledge>lambdaQuery()
				.eq(UserWeakKnowledge::getUserId, userId)
				.eq(UserWeakKnowledge::getKnowledgePointId, request.getKnowledgePointId())
				.eq(UserWeakKnowledge::getStatus, "active"));
		if (active == null || active == 0) {
			return view;
		}
		int rate = request.getCorrectRate() == null ? 0 : request.getCorrectRate();
		boolean cleared = evaluationService.conquer(userId, request.getKnowledgePointId(), rate);
		view.setWeakCleared(cleared);
		if (cleared) {
			int[] r = StudentRewardConstants.reward(StudentRewardConstants.ACTION_WEAK_CONQUER, null);
			if (r != null) {
				view.setCoins(r[0]);
				view.setPoints(r[1]);
			}
		}
		return view;
	}

	/** 积分规则展示顺序。 */
	private static final String[] RULE_ACTIONS = { StudentRewardConstants.ACTION_PREVIEW,
			StudentRewardConstants.ACTION_PRACTICE, StudentRewardConstants.ACTION_TRIAL,
			StudentRewardConstants.ACTION_TRIAL_BONUS, StudentRewardConstants.ACTION_WRONG_CORRECT,
			StudentRewardConstants.ACTION_WEAK_CONQUER, StudentRewardConstants.ACTION_DAILY_KP,
			StudentRewardConstants.ACTION_DAILY_WRONG, StudentRewardConstants.ACTION_DAILY_TIME,
			StudentRewardConstants.ACTION_CHAPTER_STAGE };

	/**
	 * 生成首页积分引导项。
	 */
	private StudentTodayView.Guide guide(String action, String target, Set<String> done) {
		int[] r = StudentRewardConstants.reward(action, null);
		int coins = r == null ? 0 : r[0];
		int points = r == null ? 0 : r[1];
		return new StudentTodayView.Guide(action, StudentRewardConstants.actionLabel(action), points, coins,
				done.contains(action), null, target);
	}

	private int activeWeakCount(String userId) {
		Long count = weakMapper.selectCount(Wrappers.<UserWeakKnowledge>lambdaQuery()
				.eq(UserWeakKnowledge::getUserId, userId).eq(UserWeakKnowledge::getStatus, "active"));
		return count == null ? 0 : count.intValue();
	}

	private UserPoints findPoints(String userId) {
		return userPointsMapper.selectOne(
				Wrappers.<UserPoints>lambdaQuery().eq(UserPoints::getUserId, userId).last("limit 1"));
	}

	private String subjectName(String subjectId) {
		if (!StringUtils.hasText(subjectId)) {
			return "综合";
		}
		Subject subject = subjectMapper.selectById(subjectId);
		return subject == null || !StringUtils.hasText(subject.getName()) ? "综合" : subject.getName();
	}

	/**
	 * 由知识点/小节反查所属学科ID。
	 */
	private String resolveSubjectId(String knowledgePointId, String sectionId) {
		String effectiveSectionId = sectionId;
		if (StringUtils.hasText(knowledgePointId)) {
			KnowledgePoint kp = knowledgePointMapper.selectById(knowledgePointId);
			if (kp != null && StringUtils.hasText(kp.getSectionId())) {
				effectiveSectionId = kp.getSectionId();
			}
		}
		if (!StringUtils.hasText(effectiveSectionId)) {
			return null;
		}
		Section section = sectionMapper.selectById(effectiveSectionId);
		if (section == null || !StringUtils.hasText(section.getChapterId())) {
			return null;
		}
		Chapter chapter = chapterMapper.selectById(section.getChapterId());
		return chapter == null ? null : chapter.getSubjectId();
	}

	/**
	 * 查找当前学员某题未订正的错题明细（最近一条）。
	 */
	private PracticeDetail findUncorrectedWrong(String userId, String questionId) {
		List<PracticeRecord> records = practiceRecordMapper.selectList(
				Wrappers.<PracticeRecord>lambdaQuery().eq(PracticeRecord::getUserId, userId));
		if (records.isEmpty()) {
			return null;
		}
		List<String> practiceIds = records.stream().map(PracticeRecord::getId).collect(Collectors.toList());
		return practiceDetailMapper.selectOne(Wrappers.<PracticeDetail>lambdaQuery()
				.in(PracticeDetail::getPracticeId, practiceIds)
				.eq(PracticeDetail::getQuestionId, questionId)
				.eq(PracticeDetail::getIsCorrect, 0)
				.eq(PracticeDetail::getCorrected, false)
				.orderByDesc(PracticeDetail::getCreateAt).last("limit 1"));
	}

	/**
	 * 由题目知识点名解析知识点ID（回退到会话级知识点ID）。
	 */
	private String resolveKpIdFromTemplate(Template template, PracticeRecord record) {
		if (template != null && template.getKnowledgePoint() != null) {
			for (String name : template.getKnowledgePoint()) {
				if (!StringUtils.hasText(name)) {
					continue;
				}
				List<KnowledgePoint> list = knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
						.eq(KnowledgePoint::getName, name.trim()).last("limit 1"));
				if (!list.isEmpty() && StringUtils.hasText(list.get(0).getId())) {
					return list.get(0).getId();
				}
			}
		}
		return record == null ? null : record.getKnowledgePointId();
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
