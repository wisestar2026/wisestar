package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.AppConsts;
import cn.wisestar.server.core.constant.SectionRepoUsage;
import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.core.security.PasswordEncoder;
import cn.wisestar.server.core.uitls.AnswerJudgeUtil;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.CampusScope;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.dto.knowledge.ChapterView;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointView;
import cn.wisestar.server.domain.dto.knowledge.SectionPracticeConfig;
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
import cn.wisestar.server.domain.model.SectionPass;
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
import cn.wisestar.server.mapper.SectionPassMapper;
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
import cn.wisestar.server.service.SectionPracticeService;
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
import java.util.Comparator;
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
@Slf4j
@Transactional
@RequiredArgsConstructor
public class StudentServiceImpl extends BaseService<StudentMapper, Student> implements StudentService {

	/**
	 * 学员初始密码（固定 123456，学员端登录后自行修改）。
	 */
	private static final String DEFAULT_PASSWORD = "123456";

	/**
	 * 学号生成最大重试次数（顺序递增，冲突时顺延直至唯一）。
	 */
	private static final int STUDENT_NO_RETRY_TIMES = 10;

	/** 学号首位字母（从 a 开始）。 */
	private static final char STUDENT_NO_FIRST_LETTER = 'a';

	/** 学号序号上限（6 位，000001-999999，满则进位到下一字母）。 */
	private static final int STUDENT_NO_SEQ_MAX = 999999;

	/** 章节测评小节名称标记（名称含该字样的章节小节以整章知识点为出题范围）。 */
	private static final String CHAPTER_ASSESSMENT_SECTION_NAME = "章节测评";

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

	/** 小节练习配置服务（专项练习/小节通关策略来源）。 */
	private final SectionPracticeService sectionPracticeService;

	/** 小节通关记录 Mapper（小节列表通关状态批量查询）。 */
	private final SectionPassMapper sectionPassMapper;

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
		// 通关状态/星级/解锁
		fillSectionPass(views);
		// 小节题目统计：题量 / 已答 / 答对
		fillQuestionStats(views);
		return views;
	}

	/** 填充小节通关状态、历史最佳星级与锁定状态（按 sort 升序顺序解锁） */
	private void fillSectionPass(List<SectionView> views) {
		if (views.isEmpty()) {
			return;
		}
		String userId = SecurityContextUtils.getUserId();
		List<String> sectionIds = views.stream().map(SectionView::getId).collect(Collectors.toList());
		Map<String, SectionPass> passMap = new HashMap<>();
		if (userId != null) {
			sectionPassMapper.selectList(Wrappers.<SectionPass>lambdaQuery()
							.eq(SectionPass::getUserId, userId).in(SectionPass::getSectionId, sectionIds))
					.forEach(p -> passMap.put(p.getSectionId(), p));
		}
		views.forEach(v -> {
			SectionPass pass = passMap.get(v.getId());
			boolean passed = pass != null && Boolean.TRUE.equals(pass.getPassed());
			v.setPassed(passed);
			v.setStars(pass == null || pass.getStars() == null ? 0 : pass.getStars());
			v.setBestRate(pass == null || pass.getBestRate() == null ? 0 : pass.getBestRate());
			v.setLocked(Boolean.FALSE);
		});
		// 上一小节开启解锁开关且未通关时，锁定当前小节
		for (int i = 1; i < views.size(); i++) {
			SectionView previous = views.get(i - 1);
			SectionPracticeConfig config = sectionPracticeService.parse(previous.getPractice());
			if (Boolean.TRUE.equals(config.getUnlockNext()) && !Boolean.TRUE.equals(previous.getPassed())) {
				views.get(i).setLocked(Boolean.TRUE);
			}
		}
	}

	/**
	 * 按「该小节发起的练习/通关正确率」计算学员学习完成度（最高正确率）。
	 *
	 * <p>练习归属以 {@code practice_record.section_id} 为准，不再按绑定题库/知识点反查，
	 * 避免同题库绑多小节或知识点名称跨小节时把完成度串到别的小节。历史无 section_id 的记录才回退匹配。</p>
	 */
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
		// 该小节发起的练习记录正确率（权威口径，不按题库/知识点串到别的小节）
		Map<String, Integer> sectionRate = new HashMap<>();
		// 历史数据未记录小节时，才回退按题库/知识点匹配
		Map<String, Integer> fallbackRepoRate = new HashMap<>();
		Map<String, Integer> fallbackKpRate = new HashMap<>();
		if (userId != null && !sectionIds.isEmpty()) {
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
				if (StringUtils.hasText(r.getSectionId())) {
					sectionRate.merge(r.getSectionId(), rate, Math::max);
				}
				else {
					if (StringUtils.hasText(r.getRepoId())) fallbackRepoRate.merge(r.getRepoId(), rate, Math::max);
					if (StringUtils.hasText(r.getKnowledgePointId())) fallbackKpRate.merge(r.getKnowledgePointId(), rate, Math::max);
				}
			}
		}
		views.forEach(v -> {
			if (sectionRate.containsKey(v.getId())) {
				v.setProgress(sectionRate.get(v.getId()));
				return;
			}
			int repoBest = reposBySection.getOrDefault(v.getId(), Collections.emptyList()).stream()
					.mapToInt(r -> fallbackRepoRate.getOrDefault(r, 0)).max().orElse(0);
			int kpBest = kpsBySection.getOrDefault(v.getId(), Collections.emptyList()).stream()
					.mapToInt(k -> fallbackKpRate.getOrDefault(k, 0)).max().orElse(0);
			v.setProgress(Math.max(repoBest, kpBest));
		});
	}

	/**
	 * 填充小节题目统计：题量（绑定题库 + 知识点绑定/标签匹配，去重）、
	 * 学员已作答的不同题目数、其中答对的不同题目数。
	 */
	private void fillQuestionStats(List<SectionView> views) {
		if (views.isEmpty()) {
			return;
		}
		String userId = SecurityContextUtils.getUserId();
		List<String> sectionIds = views.stream().map(SectionView::getId).collect(Collectors.toList());
		// 小节 → 绑定题库
		Map<String, List<String>> reposBySection = sectionRepoMapper.selectList(
						Wrappers.<SectionRepo>lambdaQuery().in(SectionRepo::getSectionId, sectionIds))
				.stream().filter(b -> StringUtils.hasText(b.getRepoId()))
				.collect(Collectors.groupingBy(SectionRepo::getSectionId,
						Collectors.mapping(SectionRepo::getRepoId, Collectors.toList())));
		Set<String> allRepoIds = reposBySection.values().stream().flatMap(List::stream)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		// 小节 → 知识点
		Map<String, List<KnowledgePoint>> kpsBySection = knowledgePointMapper.selectList(
						Wrappers.<KnowledgePoint>lambdaQuery().in(KnowledgePoint::getSectionId, sectionIds))
				.stream().collect(Collectors.groupingBy(KnowledgePoint::getSectionId));
		Set<String> allKpIds = kpsBySection.values().stream().flatMap(List::stream)
				.map(KnowledgePoint::getId).collect(Collectors.toCollection(LinkedHashSet::new));
		// 题库 → 题目（t_template.repo_id 回源）
		Map<String, Set<String>> questionIdsByRepo = new HashMap<>();
		if (!allRepoIds.isEmpty()) {
			templateMapper.selectList(Wrappers.<Template>lambdaQuery().in(Template::getRepoId, allRepoIds))
					.forEach(t -> questionIdsByRepo
							.computeIfAbsent(t.getRepoId(), k -> new LinkedHashSet<>()).add(t.getId()));
		}
		// 知识点 → 题目（显式绑定 + 名称标签匹配）
		Map<String, Set<String>> questionIdsByKp = new HashMap<>();
		for (KnowledgePoint kp : kpsBySection.values().stream().flatMap(List::stream)
				.collect(Collectors.toList())) {
			Set<String> ids = new LinkedHashSet<>();
			knowledgePointQuestionMapper.selectList(Wrappers.<KnowledgePointQuestion>lambdaQuery()
							.eq(KnowledgePointQuestion::getKnowledgePointId, kp.getId()))
					.stream().map(KnowledgePointQuestion::getQuestionId).filter(StringUtils::hasText)
					.forEach(ids::add);
			if (StringUtils.hasText(kp.getName())) {
				String name = kp.getName().trim();
				templateMapper.selectList(Wrappers.<Template>lambdaQuery()
								.and(w -> w.like(Template::getKnowledgePoint, name)
										.or().like(Template::getTemplate, name)))
						.stream().filter(t -> matchesKnowledgePointName(t, name))
						.forEach(t -> ids.add(t.getId()));
			}
			questionIdsByKp.put(kp.getId(), ids);
		}
		// 小节 → 可练习题集合；同时建立题库/知识点 → 小节反查
		Map<String, Set<String>> sectionQuestionIds = new HashMap<>();
		Map<String, List<String>> sectionsByRepo = new HashMap<>();
		Map<String, List<String>> sectionsByKp = new HashMap<>();
		for (SectionView v : views) {
			Set<String> ids = new LinkedHashSet<>();
			for (String repoId : reposBySection.getOrDefault(v.getId(), Collections.emptyList())) {
				ids.addAll(questionIdsByRepo.getOrDefault(repoId, Collections.emptySet()));
				sectionsByRepo.computeIfAbsent(repoId, k -> new ArrayList<>()).add(v.getId());
			}
			for (KnowledgePoint kp : kpsBySection.getOrDefault(v.getId(), Collections.emptyList())) {
				ids.addAll(questionIdsByKp.getOrDefault(kp.getId(), Collections.emptySet()));
				sectionsByKp.computeIfAbsent(kp.getId(), k -> new ArrayList<>()).add(v.getId());
			}
			sectionQuestionIds.put(v.getId(), ids);
			v.setQuestionCount(ids.size());
			v.setAnsweredCount(0);
			v.setCorrectCount(0);
		}
		if (userId == null) {
			return;
		}
		// 学员练习记录：小节/题库/知识点任一命中（一次查询）
		LambdaQueryWrapper<PracticeRecord> wrapper = Wrappers.<PracticeRecord>lambdaQuery()
				.eq(PracticeRecord::getUserId, userId);
		wrapper.and(w -> {
			w.in(PracticeRecord::getSectionId, sectionIds);
			if (!allRepoIds.isEmpty()) {
				w.or().in(PracticeRecord::getRepoId, allRepoIds);
			}
			if (!allKpIds.isEmpty()) {
				w.or().in(PracticeRecord::getKnowledgePointId, allKpIds);
			}
		});
		List<PracticeRecord> records = practiceRecordMapper.selectList(wrapper);
		if (records.isEmpty()) {
			return;
		}
		// 练习记录 → 归属小节
		Map<String, Set<String>> sectionsByPractice = new HashMap<>();
		for (PracticeRecord r : records) {
			Set<String> secs = new LinkedHashSet<>();
			if (StringUtils.hasText(r.getSectionId()) && sectionQuestionIds.containsKey(r.getSectionId())) {
				// 练习归属以发起小节为准，避免按题库/知识点串到别的小节
				secs.add(r.getSectionId());
			}
			else {
				if (StringUtils.hasText(r.getRepoId())) {
					secs.addAll(sectionsByRepo.getOrDefault(r.getRepoId(), Collections.emptyList()));
				}
				if (StringUtils.hasText(r.getKnowledgePointId())) {
					secs.addAll(sectionsByKp.getOrDefault(r.getKnowledgePointId(), Collections.emptyList()));
				}
			}
			if (!secs.isEmpty()) {
				sectionsByPractice.put(r.getId(), secs);
			}
		}
		if (sectionsByPractice.isEmpty()) {
			return;
		}
		// 逐题明细 → 各小节已答/答对题目集合
		List<PracticeDetail> details = practiceDetailMapper.selectList(Wrappers.<PracticeDetail>lambdaQuery()
				.in(PracticeDetail::getPracticeId, sectionsByPractice.keySet()));
		Map<String, Set<String>> answeredBySection = new HashMap<>();
		Map<String, Set<String>> correctBySection = new HashMap<>();
		for (PracticeDetail d : details) {
			if (!StringUtils.hasText(d.getQuestionId())) {
				continue;
			}
			for (String sid : sectionsByPractice.getOrDefault(d.getPracticeId(), Collections.emptySet())) {
				answeredBySection.computeIfAbsent(sid, k -> new LinkedHashSet<>()).add(d.getQuestionId());
				if (Integer.valueOf(1).equals(d.getIsCorrect())) {
					correctBySection.computeIfAbsent(sid, k -> new LinkedHashSet<>()).add(d.getQuestionId());
				}
			}
		}
		views.forEach(v -> {
			v.setAnsweredCount(answeredBySection.getOrDefault(v.getId(), Collections.emptySet()).size());
			v.setCorrectCount(correctBySection.getOrDefault(v.getId(), Collections.emptySet()).size());
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
	public List<StudentQuestionView> studyQuestions(String sectionId, List<String> knowledgePointIds, String repoId,
			String questionId, Integer count, Integer perKp, Boolean groupByKp, List<String> types, String difficulty,
			Boolean random, Boolean exposeAnswer, String usage) {
		boolean expose = Boolean.TRUE.equals(exposeAnswer);
		boolean group = Boolean.TRUE.equals(groupByKp);
		List<String> kpIds = knowledgePointIds == null ? new ArrayList<>()
				: knowledgePointIds.stream().filter(StringUtils::hasText).distinct()
						.collect(Collectors.toCollection(ArrayList::new));
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
		if (!hasQuestionScopePermission(sectionId, kpIds, repoId)) {
			return Collections.emptyList();
		}
		// 章节测评小节（名称含「章节测评」）：以本章全部知识点为出题范围（覆盖整章），
		// 且允许重复做本章其他小节做过的题（不做小节级去重）
		boolean chapterAssessment = false;
		if (StringUtils.hasText(sectionId)) {
			Section scopeSection = sectionMapper.selectById(sectionId);
			chapterAssessment = isChapterAssessmentSection(scopeSection);
			if (chapterAssessment) {
				for (String chapterKpId : knowledgePointIdsOfChapter(scopeSection.getChapterId())) {
					if (!kpIds.contains(chapterKpId)) {
						kpIds.add(chapterKpId);
					}
				}
			}
		}
		// 题目 id 集合（题库直练 + 小节绑定题库 + 知识点绑定题目，去重）
		Set<String> templateIds = new LinkedHashSet<>();
		// 知识点练习未显式带小节时，按知识点反查所属小节，作为「按知识点标签匹配」的范围
		String scopeSectionId = sectionId;
		if (!StringUtils.hasText(scopeSectionId) && !kpIds.isEmpty()) {
			KnowledgePoint point = knowledgePointMapper.selectById(kpIds.get(0));
			if (point != null) {
				scopeSectionId = point.getSectionId();
			}
		}
		// 用途场景（preview 预习 / practice 专项 / trial 通关）按做题库用途收敛范围；
		// usage 为空时不过滤，保持历史行为
		String usageScope = normalizeUsage(usage);
		// 小节通关：以本小节全部知识点为范围之一（与绑定题库取并集），保证通关题尽量覆盖知识点；
		// 章节测评小节不在此列（已在上面按整章知识点聚合）
		if (!chapterAssessment && "trial".equals(usageScope) && kpIds.isEmpty() && StringUtils.hasText(sectionId)) {
			knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
							.eq(KnowledgePoint::getSectionId, sectionId))
					.stream().map(KnowledgePoint::getId).filter(StringUtils::hasText).forEach(kpIds::add);
		}
		// 用途约束的题库集合（非空时用于候选集统一过滤，含知识点标签匹配结果）
		Set<String> usageRepoFilter = null;
		if (StringUtils.hasText(repoId)) {
			templateMapper.selectList(Wrappers.<Template>lambdaQuery().eq(Template::getRepoId, repoId))
					.forEach(t -> templateIds.add(t.getId()));
		}
		else if (StringUtils.hasText(scopeSectionId)) {
			List<SectionRepo> sectionBindings = sectionRepoMapper.selectList(Wrappers.<SectionRepo>lambdaQuery()
					.eq(SectionRepo::getSectionId, scopeSectionId));
			Set<String> allowedRepoIds = resolveAllowedRepoIds(sectionBindings, usageScope);
			// 预习缺省策略：请求未显式传题量/题型时采用小节预习配置（缺省题量 3、题型不限）
			if (SectionRepoUsage.PREVIEW.equals(usageScope)) {
				SectionPracticeConfig.PreviewConfig preview = sectionPracticeService.getConfig(scopeSectionId)
						.getPreview();
				if (preview != null) {
					if (count == null) {
						count = preview.getQuestionCount();
					}
					if ((types == null || types.isEmpty()) && preview.getTypes() != null
							&& !preview.getTypes().isEmpty()) {
						types = preview.getTypes();
					}
				}
			}
			if (!allowedRepoIds.isEmpty()) {
				// 题目按所属题库（t_template.repo_id）回源，覆盖种子与组题两套关联
				templateMapper.selectList(Wrappers.<Template>lambdaQuery().in(Template::getRepoId, allowedRepoIds))
						.forEach(t -> templateIds.add(t.getId()));
			}
			usageRepoFilter = usageScope == null || allowedRepoIds.isEmpty() ? null : allowedRepoIds;
		}
		// 知识点 → 题目（显式绑定 + 题目知识点标签匹配；perKp 时按知识点分组抽题）
		Map<String, Set<String>> questionsByKp = new LinkedHashMap<>();
		Map<String, String> kpNameById = new LinkedHashMap<>();
		for (String kpId : kpIds) {
			questionsByKp.put(kpId, new LinkedHashSet<>());
		}
		if (!kpIds.isEmpty()) {
			knowledgePointMapper.selectBatchIds(kpIds).forEach(kp -> kpNameById.put(kp.getId(), kp.getName()));
		}
		// 显式绑定（兼容管理端「知识点管理」逐题绑定）
		for (String kpId : kpIds) {
			knowledgePointQuestionMapper.selectList(Wrappers.<KnowledgePointQuestion>lambdaQuery()
							.eq(KnowledgePointQuestion::getKnowledgePointId, kpId))
					.stream().map(KnowledgePointQuestion::getQuestionId).filter(StringUtils::hasText)
					.forEach(id -> {
						questionsByKp.get(kpId).add(id);
						templateIds.add(id);
					});
		}
		// 题目知识点标签匹配（专项练习主数据源：按知识点名称精确命中题目 knowledge_point 标签）
		for (String kpId : kpIds) {
			String kpName = kpNameById.get(kpId);
			if (!StringUtils.hasText(kpName)) {
				continue;
			}
			String name = kpName.trim();
			templateMapper.selectList(Wrappers.<Template>lambdaQuery()
							.and(w -> w.like(Template::getKnowledgePoint, name)
									.or().like(Template::getTemplate, name)))
					.stream().filter(t -> matchesKnowledgePointName(t, name))
					.forEach(t -> {
						questionsByKp.get(kpId).add(t.getId());
						templateIds.add(t.getId());
					});
		}
		if (templateIds.isEmpty()) {
			return Collections.emptyList();
		}
		// 小节通关不重复：排除本小节已做过的题（章节测评允许重复做本章题）
		Set<String> doneQuestionIds = Collections.emptySet();
		if ("trial".equals(usageScope) && !chapterAssessment && StringUtils.hasText(scopeSectionId)) {
			doneQuestionIds = loadSectionDoneQuestionIds(SecurityContextUtils.getUserId(), scopeSectionId);
		}
		// 题型/难度/做题库用途/已做题目过滤后按策略排序
		final List<String> typeFilter = types;
		final Set<String> repoFilter = usageRepoFilter;
		final Set<String> doneFilter = doneQuestionIds;
		List<Template> candidates = templateMapper.selectBatchIds(templateIds).stream()
				.filter(t -> typeFilter == null || typeFilter.isEmpty()
						|| (t.getQuestionType() != null && typeFilter.contains(t.getQuestionType().name())))
				.filter(t -> !StringUtils.hasText(difficulty) || difficulty.equals(t.getDifficulty()))
				.filter(t -> repoFilter == null
						|| (t.getRepoId() != null && repoFilter.contains(t.getRepoId())))
				.filter(t -> !doneFilter.contains(t.getId()))
				.collect(Collectors.toList());
		if (candidates.isEmpty()) {
			return Collections.emptyList();
		}
		int limit = count == null ? Integer.MAX_VALUE : Math.min(count, 50);
		Map<String, String> assignedKp = new HashMap<>();
		List<Template> picked = pickByCoverage(candidates, questionsByKp, limit, perKp, group, random, assignedKp);
		return picked.stream().map(t -> {
			StudentQuestionView view = expose ? toStudentQuestionViewWithAnswer(t) : toStudentQuestionView(t);
			if (assignedKp.containsKey(t.getId())) {
				view.setKnowledgePointId(assignedKp.get(t.getId()));
			}
			return view;
		}).collect(Collectors.toList());
	}

	/**
	 * 覆盖优先组卷：按知识点分桶分配题量并抽题，保证有题的知识点都被覆盖、题目不重复。
	 *
	 * <p>组卷规则：
	 * <ul>
	 *   <li>目标题量不限（limit=MAX）且需要分组时：每个知识点出全部候选题；</li>
	 *   <li>目标题量有限时：每知识点保底 1 题，余量在知识点间轮询补足，直到用满目标题量或无题可用
	 *       （某知识点题量不足时其配额自然溢出给其他知识点）；</li>
	 *   <li>抽取时小桶优先，避免受限知识点的专属题被大桶抢占；</li>
	 *   <li>未被任何知识点命中的候选题（如小节绑定题库题）作为兜底在末尾补足；</li>
	 *   <li>结果全局去重，分组输出时同知识点题目相邻，并回填所属知识点。</li>
	 * </ul>
	 *
	 * @param candidates    过滤后的候选题（顺序即检索顺序）
	 * @param questionsByKp 知识点 → 题目ID集合（一题可挂多个知识点）
	 * @param limit         目标题量上限（Integer.MAX_VALUE 表示不限）
	 * @param perKp         每知识点题量上限（可空）
	 * @param groupByKp     是否按知识点分组输出
	 * @param random        是否随机抽题/输出
	 * @param assignedKp    出参：题目ID → 归属知识点ID
	 * @return 组卷结果
	 */
	private List<Template> pickByCoverage(List<Template> candidates, Map<String, Set<String>> questionsByKp,
			int limit, Integer perKp, boolean groupByKp, Boolean random, Map<String, String> assignedKp) {
		boolean shuffle = Boolean.TRUE.equals(random);
		List<Template> ordered = new ArrayList<>(candidates);
		if (shuffle) {
			Collections.shuffle(ordered, ThreadLocalRandom.current());
		}
		Map<String, Template> byId = new LinkedHashMap<>();
		Map<String, Integer> orderIndex = new HashMap<>();
		for (int i = 0; i < ordered.size(); i++) {
			Template t = ordered.get(i);
			byId.put(t.getId(), t);
			orderIndex.put(t.getId(), i);
		}
		boolean needsCoverage = groupByKp || limit != Integer.MAX_VALUE;
		List<Template> picked = new ArrayList<>();
		Set<String> used = new LinkedHashSet<>();
		if (needsCoverage && !questionsByKp.isEmpty()) {
			List<String> kpOrder = new ArrayList<>(questionsByKp.keySet());
			// 各知识点在当前候选集内的题目（保持候选顺序；random 时即随机顺序）
			Map<String, List<String>> pools = new LinkedHashMap<>();
			for (String kpId : kpOrder) {
				List<String> ids = new ArrayList<>();
				for (String id : questionsByKp.get(kpId)) {
					if (byId.containsKey(id)) {
						ids.add(id);
					}
				}
				ids.sort(Comparator.comparingInt(id -> orderIndex.getOrDefault(id, Integer.MAX_VALUE)));
				pools.put(kpId, ids);
			}
			int maxPerKp = (perKp != null && perKp > 0) ? perKp : Integer.MAX_VALUE;
			Map<String, Integer> quota = new LinkedHashMap<>();
			kpOrder.forEach(kpId -> quota.put(kpId, 0));
			if (limit == Integer.MAX_VALUE) {
				// 不限总量：每知识点全出（受 perKp 限制）
				for (String kpId : kpOrder) {
					quota.put(kpId, Math.min(maxPerKp, pools.get(kpId).size()));
				}
			}
			else {
				// 保底覆盖：每个有题知识点先各取 1 题
				int allocated = 0;
				for (String kpId : kpOrder) {
					if (allocated >= limit) {
						break;
					}
					if (!pools.get(kpId).isEmpty()) {
						quota.put(kpId, 1);
						allocated++;
					}
				}
				// 余量轮询：在仍有剩余题目的知识点间逐个补足，直到用满目标题量
				boolean progress = true;
				while (allocated < limit && progress) {
					progress = false;
					for (String kpId : kpOrder) {
						if (allocated >= limit) {
							break;
						}
						int cap = Math.min(maxPerKp, pools.get(kpId).size());
						if (quota.get(kpId) < cap) {
							quota.put(kpId, quota.get(kpId) + 1);
							allocated++;
							progress = true;
						}
					}
				}
			}
			// 小池优先抽取，保证专属题先满足受限知识点；结果按知识点顺序分组输出
			Map<String, List<Template>> buckets = new LinkedHashMap<>();
			kpOrder.forEach(kpId -> buckets.put(kpId, new ArrayList<>()));
			List<String> pickOrder = new ArrayList<>(kpOrder);
			pickOrder.sort(Comparator.comparingInt(kpId -> pools.get(kpId).size()));
			for (String kpId : pickOrder) {
				int need = quota.get(kpId);
				int taken = 0;
				for (String id : pools.get(kpId)) {
					if (taken >= need) {
						break;
					}
					if (used.contains(id)) {
						continue;
					}
					Template t = byId.get(id);
					if (t == null) {
						continue;
					}
					buckets.get(kpId).add(t);
					used.add(id);
					assignedKp.put(id, kpId);
					taken++;
				}
			}
			for (String kpId : kpOrder) {
				picked.addAll(buckets.get(kpId));
			}
		}
		// 兜底补足：未被知识点命中的候选题（或在有题量上限时仍缺额）在末尾补齐
		for (Template t : ordered) {
			if (picked.size() >= limit) {
				break;
			}
			if (used.contains(t.getId())) {
				continue;
			}
			picked.add(t);
			used.add(t.getId());
		}
		if (!groupByKp) {
			if (shuffle) {
				Collections.shuffle(picked, ThreadLocalRandom.current());
			}
			else {
				picked.sort(Comparator.comparingInt(t -> orderIndex.getOrDefault(t.getId(), Integer.MAX_VALUE)));
			}
		}
		return picked;
	}

	/** 章节测评小节识别：小节名称包含「章节测评」。 */
	private boolean isChapterAssessmentSection(Section section) {
		return section != null && section.getName() != null
				&& section.getName().contains(CHAPTER_ASSESSMENT_SECTION_NAME);
	}

	/** 章节下全部小节的知识点ID（章节测评出题范围，覆盖整章知识点）。 */
	private List<String> knowledgePointIdsOfChapter(String chapterId) {
		if (!StringUtils.hasText(chapterId)) {
			return Collections.emptyList();
		}
		List<String> sectionIds = sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
						.eq(Section::getChapterId, chapterId))
				.stream().map(Section::getId).filter(StringUtils::hasText).collect(Collectors.toList());
		if (sectionIds.isEmpty()) {
			return Collections.emptyList();
		}
		return knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
						.in(KnowledgePoint::getSectionId, sectionIds))
				.stream().map(KnowledgePoint::getId).filter(StringUtils::hasText).collect(Collectors.toList());
	}

	/** 某学员在某小节内已做过的题目ID（小节通关去重数据源）。 */
	private Set<String> loadSectionDoneQuestionIds(String userId, String sectionId) {
		if (!StringUtils.hasText(userId) || !StringUtils.hasText(sectionId)) {
			return Collections.emptySet();
		}
		List<String> practiceIds = practiceRecordMapper.selectList(Wrappers.<PracticeRecord>lambdaQuery()
						.eq(PracticeRecord::getUserId, userId).eq(PracticeRecord::getSectionId, sectionId))
				.stream().map(PracticeRecord::getId).filter(StringUtils::hasText).collect(Collectors.toList());
		if (practiceIds.isEmpty()) {
			return Collections.emptySet();
		}
		return practiceDetailMapper.selectList(Wrappers.<PracticeDetail>lambdaQuery()
						.in(PracticeDetail::getPracticeId, practiceIds))
				.stream().map(PracticeDetail::getQuestionId).filter(StringUtils::hasText)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 归一化用途场景：仅识别 preview/practice/trial，其余（含空）返回 null 表示不过滤。
	 */
	private String normalizeUsage(String usage) {
		if (!StringUtils.hasText(usage)) {
			return null;
		}
		String scope = usage.trim().toLowerCase();
		if (SectionRepoUsage.PREVIEW.equals(scope) || SectionRepoUsage.PRACTICE.equals(scope)
				|| "trial".equals(scope)) {
			return scope;
		}
		return null;
	}

	/**
	 * 计算某用途场景下允许的题库集合。
	 *
	 * <p>usage 为空时返回全部绑定题库；usage 非空时按用途筛选（preview 取预习专用+通用，
	 * 其余取练习专用+通用）；筛选结果为空时退回全部绑定题库，避免空卷。</p>
	 */
	private Set<String> resolveAllowedRepoIds(List<SectionRepo> bindings, String usageScope) {
		Set<String> all = bindings.stream().map(SectionRepo::getRepoId).filter(StringUtils::hasText)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (usageScope == null) {
			return all;
		}
		boolean preview = SectionRepoUsage.PREVIEW.equals(usageScope);
		Set<String> filtered = bindings.stream()
				.filter(b -> StringUtils.hasText(b.getRepoId()))
				.filter(b -> preview ? SectionRepoUsage.availableForPreview(b.getUsageType())
						: SectionRepoUsage.availableForPractice(b.getUsageType()))
				.map(SectionRepo::getRepoId)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		return filtered.isEmpty() ? all : filtered;
	}

	/**
	 * 题目范围权限校验：题库直练校验题库存在；小节/知识点范围校验归属章节在学员有效权限内。
	 */
	private boolean hasQuestionScopePermission(String sectionId, List<String> knowledgePointIds, String repoId) {
		if (StringUtils.hasText(repoId)) {
			return repoMapper.selectById(repoId) != null;
		}
		String effectiveSectionId = sectionId;
		if (!StringUtils.hasText(effectiveSectionId)) {
			for (String kpId : knowledgePointIds) {
				KnowledgePoint point = knowledgePointMapper.selectById(kpId);
				if (point != null) {
					effectiveSectionId = point.getSectionId();
					break;
				}
			}
		}
		if (!StringUtils.hasText(effectiveSectionId)) {
			return false;
		}
		Section section = sectionMapper.selectById(effectiveSectionId);
		Chapter chapter = section == null ? null : chapterMapper.selectById(section.getChapterId());
		return chapter != null && hasPermission(chapter.getSubjectId(), chapter.getGrade());
	}

	/**
	 * 题目是否归属某知识点名称：题目顶层 knowledge_point 列或 template JSON 内
	 * attribute.knowledgePoint 命中该名称即视为匹配（忽略大小写/首尾空格）。
	 */
	private boolean matchesKnowledgePointName(Template template, String name) {
		if (containsKnowledgePointName(template.getKnowledgePoint(), name)) {
			return true;
		}
		SurveySchema schema = template.getTemplate();
		return schema != null && schema.getAttribute() != null
				&& containsKnowledgePointName(schema.getAttribute().getKnowledgePoint(), name);
	}

	private boolean containsKnowledgePointName(String[] values, String name) {
		if (values == null) {
			return false;
		}
		for (String value : values) {
			if (value != null && name.equalsIgnoreCase(value.trim())) {
				return true;
			}
		}
		return false;
	}

	private boolean containsKnowledgePointName(List<String> values, String name) {
		if (values == null) {
			return false;
		}
		for (String value : values) {
			if (value != null && name.equalsIgnoreCase(value.trim())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public SectionPracticeConfig sectionPracticeConfig(String sectionId) {
		return sectionPracticeService.getConfig(sectionId);
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
		rc.setRefId("wrong:" + request.getQuestionId());
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
			StudentRewardConstants.ACTION_TRIAL_BONUS, StudentRewardConstants.ACTION_KP_MASTER,
			StudentRewardConstants.ACTION_WRONG_CORRECT, StudentRewardConstants.ACTION_WEAK_CONQUER,
			StudentRewardConstants.ACTION_DAILY_CHECKIN, StudentRewardConstants.ACTION_TASK_DONE,
			StudentRewardConstants.ACTION_DAILY_KP, StudentRewardConstants.ACTION_DAILY_WRONG,
			StudentRewardConstants.ACTION_DAILY_TIME, StudentRewardConstants.ACTION_CHAPTER_STAGE };

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
	 * 生成「字母 + 6 位数字」的唯一学号：从 a000001 起顺序递增
	 * （a000001 → a000002 → … → a999999 → b000001 → …），冲突自动顺延，
	 * 数据库唯一索引 uk_student_no 兜底。
	 *
	 * @return 唯一学号
	 */
	private String generateUniqueStudentNo() {
		String candidate = nextStudentNo(findMaxStudentNo());
		for (int i = 0; i < STUDENT_NO_RETRY_TIMES; i++) {
			Long existCount = count(Wrappers.<Student>lambdaQuery().eq(Student::getStudentNo, candidate));
			if (existCount == null || existCount == 0) {
				return candidate;
			}
			candidate = incrementStudentNo(candidate);
		}
		throw new ValidationException("学号生成失败，请重试");
	}

	/** 取当前最大字母格式学号（字母 > 数字，降序首条即字母序列最大值）。 */
	private String findMaxStudentNo() {
		Student last = getOne(Wrappers.<Student>lambdaQuery()
				.orderByDesc(Student::getStudentNo)
				.last("limit 1"), false);
		return last == null ? null : last.getStudentNo();
	}

	/** 由当前学号推算下一个；非「字母 + 6 位数字」或为空时从 a000001 开始。 */
	private String nextStudentNo(String current) {
		return isLetterStudentNo(current) ? incrementStudentNo(current)
				: STUDENT_NO_FIRST_LETTER + "000001";
	}

	/** 学号序号 +1；满 6 位则进位到下一个字母（a999999 → b000001）。 */
	private String incrementStudentNo(String current) {
		if (!isLetterStudentNo(current)) {
			return STUDENT_NO_FIRST_LETTER + "000001";
		}
		char letter = current.charAt(0);
		int seq = Integer.parseInt(current.substring(1));
		if (seq >= STUDENT_NO_SEQ_MAX) {
			if (letter >= 'z') {
				throw new ValidationException("学号已用尽");
			}
			return (char) (letter + 1) + "000001";
		}
		return letter + String.format("%06d", seq + 1);
	}

	/** 是否形如「字母 + 6 位数字」的学号（如 a000001）。 */
	private boolean isLetterStudentNo(String value) {
		if (value == null || value.length() != 7) {
			return false;
		}
		char letter = value.charAt(0);
		if (letter < 'a' || letter > 'z') {
			return false;
		}
		for (int i = 1; i < 7; i++) {
			char digit = value.charAt(i);
			if (digit < '0' || digit > '9') {
				return false;
			}
		}
		return true;
	}

}
