package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.student.StudentQuery;
import cn.wisestar.server.domain.dto.student.StudentRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterView;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointView;
import cn.wisestar.server.domain.dto.knowledge.SectionPracticeConfig;
import cn.wisestar.server.domain.dto.knowledge.SectionView;
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
import cn.wisestar.server.domain.dto.student.StudentView;
import cn.wisestar.server.domain.dto.student.StudentWeakConquerRequest;
import cn.wisestar.server.domain.dto.student.StudentWeakConquerView;
import cn.wisestar.server.domain.dto.student.StudentWeakView;
import cn.wisestar.server.domain.dto.student.StudentWrongRedoRequest;
import cn.wisestar.server.domain.dto.student.StudentWrongRedoView;
import cn.wisestar.server.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 学员管理接口（学员管理模块）。
 *
 * <p><b>定位</b>：管理端「学员管理 → 学员列表」页面数据源——学员 CRUD；
 * 新增学员自动生成「字母 + 6 位数字」学号（如 a000001）并创建学员登录账号（初始密码 123456）。</p>
 */
@RestController
@RequestMapping("${api.prefix}/student")
@RequiredArgsConstructor
public class StudentApi {

	/**
	 * 学员管理服务（业务层入口，构造器注入）。
	 */
	private final StudentService studentService;

	/**
	 * 新增学员。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/create（如 /api/student/create）。</p>
	 *
	 * <p><b>功能</b>：校验姓名/联系号码必填与组合查重 → 自动生成「字母 + 6 位数字」唯一学号 →
	 * 同一事务内写入学员主数据与登录账号（学号即账号，初始密码 123456）。</p>
	 *
	 * <p><b>请求参数</b>：{@link StudentRequest}（@RequestBody JSON：name/age/phone/school/campus）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link StudentView}（含系统生成的学号）。</p>
	 *
	 * @param request 学员请求
	 * @return 学员视图（含学号）
	 */
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('student:create')")
	public StudentView addStudent(@RequestBody StudentRequest request) {
		return studentService.createStudent(request);
	}

	/**
	 * 学员分页列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/list（如 /api/student/list）。</p>
	 *
	 * <p><b>请求参数</b>：{@link StudentQuery}（GET 参数：current/pageSize/name/studentNo/phone）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;{@link StudentView}&gt;（total + list）。</p>
	 *
	 * @param query 查询条件
	 * @return 分页的学员视图
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('student:list')")
	public PaginationResponse<StudentView> listStudents(StudentQuery query) {
		return studentService.pageStudents(query);
	}

	/**
	 * 更新学员（学号不可修改）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/update（如 /api/student/update）。</p>
	 *
	 * @param request 学员请求（含 id）
	 */
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('student:update')")
	public void updateStudent(@RequestBody StudentRequest request) {
		studentService.updateStudent(request);
	}

	/**
	 * 删除学员（逻辑删除）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/delete（如 /api/student/delete）。</p>
	 *
	 * @param request 学员请求（含 id）
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('student:delete')")
	public void deleteStudent(@RequestBody StudentRequest request) {
		studentService.deleteStudent(request);
	}

	/**
	 * 当前登录学员信息（学员端档案展示）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/me（如 /api/student/me）。</p>
	 *
	 * <p><b>功能</b>：按当前登录用户ID查询 t_student 返回学员视图（学号/姓名/年龄/
	 * 联系号码/学校/校区）；系统用户调用返回 400 校验异常。</p>
	 *
	 * <p><b>返回值结构</b>：{@link StudentView}。</p>
	 *
	 * @return 当前登录学员信息
	 */
	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public StudentView myStudentInfo() {
		return studentService.me();
	}

	/**
	 * 学员有效权限（多条有效订单合并）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/permissions。</p>
	 *
	 * <p><b>功能</b>：返回可访问的学科（含名称）、年级、教材版本（expire_at > NOW()），
	 * 供学员端按订单授予范围过滤内容。</p>
	 *
	 * <p><b>权限</b>：isAuthenticated()（学员端登录即可）。</p>
	 *
	 * @return 有效权限视图
	 */
	@GetMapping("/permissions")
	@PreAuthorize("isAuthenticated()")
	public StudentPermissionView permissions() {
		return studentService.permissions();
	}

	/**
	 * 学员学习统计（首页真实化，基于练习记录聚合）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/stats。</p>
	 *
	 * <p><b>权限</b>：isAuthenticated()（学员端登录即可）。</p>
	 *
	 * @return 累计/今日/分科学币统计
	 */
	@GetMapping("/stats")
	@PreAuthorize("isAuthenticated()")
	public StudentStatsView stats() {
		return studentService.stats();
	}

	/**
	 * 学员端学科列表（按订单有效权限过滤）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/study/subjects。</p>
	 *
	 * <p><b>权限</b>：isAuthenticated()（服务层校验学员身份与订单权限）。</p>
	 *
	 * @return 可访问学科（含该学科有权限的教材版本）
	 */
	@GetMapping("/study/subjects")
	@PreAuthorize("isAuthenticated()")
	public List<StudentSubjectView> studySubjects() {
		return studentService.studySubjects();
	}

	/**
	 * 学员端章节列表（按订单权限过滤学科与年级）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/study/chapters?subjectId=&grade=。</p>
	 *
	 * @param subjectId 学科ID
	 * @param grade     年级（可选，如 三年级；不传按该学科全部授权年级返回）
	 * @return 章节列表（含小节数）
	 */
	@GetMapping("/study/chapters")
	@PreAuthorize("isAuthenticated()")
	public List<ChapterView> studyChapters(@RequestParam(required = false) String subjectId,
			@RequestParam(required = false) String grade) {
		return studentService.studyChapters(subjectId, grade);
	}

	/**
	 * 学员端小节列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/study/sections?chapterId=。</p>
	 *
	 * @param chapterId 章节ID
	 * @return 小节列表（含内容设置与知识点数）
	 */
	@GetMapping("/study/sections")
	@PreAuthorize("isAuthenticated()")
	public List<SectionView> studySections(@RequestParam(required = false) String chapterId) {
		return studentService.studySections(chapterId);
	}

	/**
	 * 学员端知识点列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/study/points?sectionId=。</p>
	 *
	 * @param sectionId 小节ID
	 * @return 知识点列表（含讲解要点与配图）
	 */
	@GetMapping("/study/points")
	@PreAuthorize("isAuthenticated()")
	public List<KnowledgePointView> studyPoints(@RequestParam(required = false) String sectionId) {
		return studentService.studyPoints(sectionId);
	}

	/**
	 * 学员端学科学习进度（章节 → 知识点掌握度/评级/薄弱）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/study/progress?subjectId=&versionId=。</p>
	 *
	 * @param subjectId 学科ID
	 * @param versionId 教材版本ID（可选）
	 * @return 章节及其知识点的真实评价值
	 */
	@GetMapping("/study/progress")
	@PreAuthorize("isAuthenticated()")
	public StudentStudyProgressView studyProgress(@RequestParam(required = false) String subjectId,
			@RequestParam(required = false) String versionId) {
		return studentService.studyProgress(subjectId, versionId);
	}

	/**
	 * 学员端练习/试炼题目（剥离标准答案）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/study/questions?sectionId=&knowledgePointIds=&count=&types=&difficulty=&random=&perKp=。</p>
	 *
	 * @param sectionId         小节ID（小节练习数据源）
	 * @param knowledgePointId  知识点ID（兼容单值调用）
	 * @param knowledgePointIds 知识点ID集合（专项练习多选，逗号分隔或重复参数）
	 * @param repoId            题库ID（题库直练数据源）
	 * @param questionId        题目ID（单题重做数据源，优先于其他来源）
	 * @param count             返回题目数量（为空=返回绑定内容全部题目，显式传值时上限 50）
	 * @param perKp             每个知识点抽取题数（可空）
	 * @param groupByKp         是否按知识点分组全量出题（专项练习「尽可能多且全覆盖」；可空）
	 * @param types             题型过滤（逗号分隔，可选）
	 * @param difficulty        难度过滤（可选）
	 * @param random            是否随机排序
	 * @param usage             用途场景（preview 预习 / practice 专项练习 / trial 小节通关），
	 *                          按做题库用途收敛范围；为空时不过滤
	 * @return 题目列表（不含答案）
	 */
	@GetMapping("/study/questions")
	@PreAuthorize("isAuthenticated()")
	public List<StudentQuestionView> studyQuestions(@RequestParam(required = false) String sectionId,
			@RequestParam(required = false) String knowledgePointId,
			@RequestParam(required = false) List<String> knowledgePointIds,
			@RequestParam(required = false) String repoId,
			@RequestParam(required = false) String questionId,
			@RequestParam(required = false) Integer count,
			@RequestParam(required = false) Integer perKp,
			@RequestParam(required = false) Boolean groupByKp,
			@RequestParam(required = false) List<String> types,
			@RequestParam(required = false) String difficulty,
			@RequestParam(required = false) Boolean random,
			@RequestParam(required = false) Boolean exposeAnswer,
			@RequestParam(required = false) String usage) {
		List<String> kpIds = new ArrayList<>();
		if (knowledgePointIds != null) {
			kpIds.addAll(knowledgePointIds);
		}
		if (knowledgePointId != null && !knowledgePointId.isEmpty() && !kpIds.contains(knowledgePointId)) {
			kpIds.add(knowledgePointId);
		}
		return studentService.studyQuestions(sectionId, kpIds, repoId, questionId, count, perKp, groupByKp, types,
				difficulty, random, exposeAnswer, usage);
	}

	/**
	 * 学员端小节练习配置（出题策略来源）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/practice/config?sectionId=。</p>
	 *
	 * @param sectionId 小节ID
	 * @return 练习配置（模式/题量/难度/题型/通关阈值/解锁开关）
	 */
	@GetMapping("/practice/config")
	@PreAuthorize("isAuthenticated()")
	public SectionPracticeConfig practiceConfig(@RequestParam(required = false) String sectionId) {
		return studentService.sectionPracticeConfig(sectionId);
	}

	/**
	 * 学员端实时位置上报（路由变化/进入习题时调用）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/activity。</p>
	 *
	 * <p><b>权限</b>：isAuthenticated()（服务层校验学员身份）。</p>
	 *
	 * @param request 位置上报（page/questionId/sectionId）
	 */
	@PostMapping("/activity")
	@PreAuthorize("isAuthenticated()")
	public void uploadActivity(@RequestBody StudentActivityRequest request) {
		studentService.uploadActivity(request);
	}

	/**
	 * 后台学员实时位置列表（老师监控）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/activities。</p>
	 *
	 * <p><b>权限</b>：hasAuthority('student:list')（后台老师/管理员）。</p>
	 *
	 * @return 各学员最后上报位置（含姓名/学号/习题标题），按最后活跃时间倒序
	 */
	@GetMapping("/activities")
	@PreAuthorize("hasAuthority('student:list')")
	public List<StudentActivityView> activities() {
		return studentService.listActivities();
	}

	/**
	 * 老师给学员发放学币（student:update）。
	 *
	 * @param request 学币发放请求（studentId + coins + reason）
	 */
	@PostMapping("/coin")
	@PreAuthorize("hasAuthority('student:update')")
	public void addCoin(@RequestBody StudentCoinRequest request) {
		studentService.addCoin(request);
	}

	/**
	 * 学员预习完成（知识点预习讲完后的「预习完成」按钮）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/preview/complete。</p>
	 *
	 * <p><b>功能</b>：标记该小节/知识点预习完成（学习完成度 100%）并结算奖励
	 * （学习币 +5 / 学海积分 +3）；同一目标仅首次结算，重复调用不重复发放。</p>
	 *
	 * <p><b>权限</b>：isAuthenticated()（服务层校验学员身份）。</p>
	 *
	 * @param request 预习完成请求（sectionId 或 knowledgePointId 至少其一）
	 * @return 奖励结算结果（firstTime/coins/points）
	 */
	@PostMapping("/preview/complete")
	@PreAuthorize("isAuthenticated()")
	public StudentPreviewCompleteView completePreview(@RequestBody StudentPreviewCompleteRequest request) {
		return studentService.completePreview(request);
	}

	/**
	 * 个人中心档案（姓名/学号/头衔/累计积分/知识点/薄弱数）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/profile。</p>
	 *
	 * @return 学员档案视图
	 */
	@GetMapping("/profile")
	@PreAuthorize("isAuthenticated()")
	public StudentProfileView profile() {
		return studentService.profile();
	}

	/**
	 * 个人中心-积分板块（积分/头衔/下一目标/规则/最近明细）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/points。</p>
	 *
	 * @return 积分板块视图
	 */
	@GetMapping("/points")
	@PreAuthorize("isAuthenticated()")
	public StudentPointsView points() {
		return studentService.points();
	}

	/**
	 * 本学期学习币（分学科 + 手动发币，单科上限 10000）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/coins。</p>
	 *
	 * @return 学习币视图
	 */
	@GetMapping("/coins")
	@PreAuthorize("isAuthenticated()")
	public StudentCoinsView coins() {
		return studentService.coins();
	}

	/**
	 * 学员端主页今日总览 + 积分获取引导。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/today。</p>
	 *
	 * @return 今日总览与引导
	 */
	@GetMapping("/today")
	@PreAuthorize("isAuthenticated()")
	public StudentTodayView today() {
		return studentService.today();
	}

	/**
	 * 薄弱知识点列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/weak/list。</p>
	 *
	 * @return 薄弱知识点列表
	 */
	@GetMapping("/weak/list")
	@PreAuthorize("isAuthenticated()")
	public List<StudentWeakView> weakList() {
		return studentService.weakList();
	}

	/**
	 * 知识点详情（掌握度/评级/薄弱/预习状态）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/knowledge/detail?knowledgePointId=。</p>
	 *
	 * @param knowledgePointId 知识点ID
	 * @return 知识点详情
	 */
	@GetMapping("/knowledge/detail")
	@PreAuthorize("isAuthenticated()")
	public StudentKnowledgeDetailView knowledgeDetail(@RequestParam String knowledgePointId) {
		return studentService.knowledgeDetail(knowledgePointId);
	}

	/**
	 * 学习完成统一结算（预习/练习/试炼/错题订正等）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/learning/complete。</p>
	 *
	 * @param request 结算请求
	 * @return 结算结果
	 */
	@PostMapping("/learning/complete")
	@PreAuthorize("isAuthenticated()")
	public StudentPreviewCompleteView completeLearning(@RequestBody StudentLearningCompleteRequest request) {
		return studentService.completeLearning(request);
	}

	/**
	 * 错题重做（答对则订正、移出错题本、刷新薄弱并结算奖励）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/wrong/redo。</p>
	 *
	 * @param request 错题重做请求
	 * @return 重做结果
	 */
	@PostMapping("/wrong/redo")
	@PreAuthorize("isAuthenticated()")
	public StudentWrongRedoView wrongRedo(@RequestBody StudentWrongRedoRequest request) {
		return studentService.wrongRedo(request);
	}

	/**
	 * 薄弱知识点攻克（复测达标后消除薄弱并结算奖励）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/weak/conquer。</p>
	 *
	 * @param request 攻克请求
	 * @return 攻克结果
	 */
	@PostMapping("/weak/conquer")
	@PreAuthorize("isAuthenticated()")
	public StudentWeakConquerView weakConquer(@RequestBody StudentWeakConquerRequest request) {
		return studentService.weakConquer(request);
	}

}
