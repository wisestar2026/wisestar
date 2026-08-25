package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.student.StudentQuery;
import cn.wisestar.server.domain.dto.student.StudentRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterView;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointView;
import cn.wisestar.server.domain.dto.knowledge.SectionView;
import cn.wisestar.server.domain.dto.student.StudentActivityRequest;
import cn.wisestar.server.domain.dto.student.StudentActivityView;
import cn.wisestar.server.domain.dto.student.StudentPermissionView;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.domain.dto.student.StudentStatsView;
import cn.wisestar.server.domain.dto.student.StudentSubjectView;
import cn.wisestar.server.domain.dto.student.StudentView;
import cn.wisestar.server.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学员管理接口（学员管理模块）。
 *
 * <p><b>定位</b>：管理端「学员管理 → 学员列表」页面数据源——学员 CRUD；
 * 新增学员自动生成 8 位学号并创建学员登录账号（初始密码 123456）。</p>
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
	 * <p><b>功能</b>：校验姓名/联系号码必填与组合查重 → 自动生成 8 位唯一学号 →
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
	 * 学员端章节列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/study/chapters?subjectId=。</p>
	 *
	 * @param subjectId 学科ID
	 * @return 章节列表（含小节数）
	 */
	@GetMapping("/study/chapters")
	@PreAuthorize("isAuthenticated()")
	public List<ChapterView> studyChapters(@RequestParam(required = false) String subjectId) {
		return studentService.studyChapters(subjectId);
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
	 * 学员端练习/试炼题目（剥离标准答案）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/study/questions?sectionId=&knowledgePointId=&count=&types=&difficulty=。</p>
	 *
	 * @param sectionId        小节ID（小节练习数据源）
	 * @param knowledgePointId 知识点ID（知识点试炼数据源）
	 * @param count            返回题目数量（默认 10，上限 50）
	 * @param types            题型过滤（逗号分隔，可选）
	 * @param difficulty       难度过滤（可选）
	 * @return 题目列表（不含答案）
	 */
	@GetMapping("/study/questions")
	@PreAuthorize("isAuthenticated()")
	public List<StudentQuestionView> studyQuestions(@RequestParam(required = false) String sectionId,
			@RequestParam(required = false) String knowledgePointId,
			@RequestParam(required = false) String repoId,
			@RequestParam(required = false) Integer count,
			@RequestParam(required = false) List<String> types,
			@RequestParam(required = false) String difficulty) {
		return studentService.studyQuestions(sectionId, knowledgePointId, repoId, count, types, difficulty);
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

}
