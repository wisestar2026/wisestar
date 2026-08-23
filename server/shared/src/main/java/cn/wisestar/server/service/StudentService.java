package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.knowledge.ChapterView;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointView;
import cn.wisestar.server.domain.dto.knowledge.SectionView;
import cn.wisestar.server.domain.dto.student.StudentActivityRequest;
import cn.wisestar.server.domain.dto.student.StudentActivityView;
import cn.wisestar.server.domain.dto.student.StudentPermissionView;
import cn.wisestar.server.domain.dto.student.StudentQuery;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.domain.dto.student.StudentRequest;
import cn.wisestar.server.domain.dto.student.StudentSubjectView;
import cn.wisestar.server.domain.dto.student.StudentView;

import java.util.List;

/**
 * 学员管理服务（学员管理模块）。
 *
 * <p><b>定位</b>：学员主数据 CRUD。新增学员时自动生成 8 位学号并创建
 * 学员登录账号（t_account，user_type=Student，初始密码 123456）。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
public interface StudentService {

	/**
	 * 新增学员（自动生成学号 + 创建登录账号，同一事务）。
	 *
	 * @param request 学员请求（name/phone 必填）
	 * @return 学员视图（含系统生成的学号）
	 */
	StudentView createStudent(StudentRequest request);

	/**
	 * 学员分页查询。
	 *
	 * @param query 查询条件（name/studentNo/phone 模糊匹配）
	 * @return 分页的学员视图
	 */
	PaginationResponse<StudentView> pageStudents(StudentQuery query);

	/**
	 * 更新学员（学号不可修改，姓名+联系号码组合查重排除自身）。
	 *
	 * @param request 学员请求（含 id）
	 */
	void updateStudent(StudentRequest request);

	/**
	 * 删除学员（逻辑删除）。
	 *
	 * @param request 学员请求（含 id）
	 */
	void deleteStudent(StudentRequest request);

	/**
	 * 当前登录学员信息（学员端档案展示用，按登录用户ID查 t_student）。
	 *
	 * @return 学员视图；当前用户非学员时抛校验异常
	 */
	StudentView me();

	/**
	 * 当前学员的有效权限（多条有效订单合并，expire_at > NOW()）。
	 *
	 * <p>返回可访问的学科（含名称）、年级、教材版本，供学员端过滤内容。</p>
	 *
	 * @return 有效权限视图（无权限时各列表为空）
	 */
	StudentPermissionView permissions();

	/**
	 * 学员端学科列表（按订单有效权限过滤）。
	 *
	 * @return 可访问学科（含该学科有权限的教材版本）
	 */
	List<StudentSubjectView> studySubjects();

	/**
	 * 学员端章节列表（按订单权限校验学科）。
	 *
	 * @param subjectId 学科ID
	 * @return 章节列表（含小节数）；学科不在权限内返回空列表
	 */
	List<ChapterView> studyChapters(String subjectId);

	/**
	 * 学员端小节列表（含内容设置 JSON 与知识点数）。
	 *
	 * @param chapterId 章节ID
	 * @return 小节列表；归属学科不在权限内返回空列表
	 */
	List<SectionView> studySections(String chapterId);

	/**
	 * 学员端知识点列表（含讲解要点与配图）。
	 *
	 * @param sectionId 小节ID
	 * @return 知识点列表；归属学科不在权限内返回空列表
	 */
	List<KnowledgePointView> studyPoints(String sectionId);

	/**
	 * 学员端实时位置上报（当前学员，路由变化/进入习题时调用）。
	 *
	 * @param request 位置上报（page/questionId/sectionId）
	 */
	void uploadActivity(StudentActivityRequest request);

	/**
	 * 后台学员实时位置列表（老师监控用，含学员姓名/学号与习题标题）。
	 *
	 * @return 各学员最后上报位置，按最后活跃时间倒序
	 */
	List<StudentActivityView> listActivities();

	/**
	 * 学员端练习/试炼题目（剥离标准答案，防作弊）。
	 *
	 * <p>题目来源二选一：sectionId → 小节绑定题库题目；knowledgePointId → 知识点绑定题目。
	 * 按题型/难度过滤，count 限制返回数量。</p>
	 *
	 * @param sectionId        小节ID（小节练习数据源）
	 * @param knowledgePointId 知识点ID（知识点试炼数据源）
	 * @param count            返回题目数量（默认 10，上限 50）
	 * @param types            题型过滤（可选）
	 * @param difficulty       难度过滤（可选）
	 * @return 题目列表（不含答案）；无数据返回空列表
	 */
	List<StudentQuestionView> studyQuestions(String sectionId, String knowledgePointId, Integer count,
			List<String> types, String difficulty);

}
