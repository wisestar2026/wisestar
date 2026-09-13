package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
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
import cn.wisestar.server.domain.dto.student.StudentQuery;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.domain.dto.student.StudentRequest;
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

import java.util.List;

/**
 * 学员管理服务（学员管理模块）。
 *
 * <p><b>定位</b>：学员主数据 CRUD。新增学员时自动生成「字母 + 6 位数字」学号并创建
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
	 * @return 可访问学科（含该学科有权限的教材版本与年级）
	 */
	List<StudentSubjectView> studySubjects();

	/**
	 * 学员端章节列表（按订单权限校验学科与年级）。
	 *
	 * <p>仅返回订单授予年级下的章节：grade 为空时返回该学科全部授权年级的章节；
	 * grade 有值且不在该学科授权年级内时返回空列表。</p>
	 *
	 * @param subjectId 学科ID
	 * @param grade     年级（可选，如 三年级；不传按该学科全部授权年级返回）
	 * @return 章节列表（含小节数）；学科不在权限内返回空列表
	 */
	List<ChapterView> studyChapters(String subjectId, String grade);

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
	 * 学员端学科学习进度（章节 → 知识点掌握度/评级/薄弱）。
	 *
	 * @param subjectId 学科ID
	 * @param versionId 教材版本ID（可选）
	 * @return 章节及其知识点的真实评价值；学科不在权限内返回空列表
	 */
	StudentStudyProgressView studyProgress(String subjectId, String versionId);

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
	 * 学员学习统计（首页真实化，基于练习记录聚合）。
	 *
	 * @return 累计/今日/分科学币统计（含老师手动发放学币）
	 */
	StudentStatsView stats();

	/**
	 * 老师给学员发放学币（手动加/扣）。
	 *
	 * @param request 学币发放请求
	 */
	void addCoin(StudentCoinRequest request);

	/**
	 * 学员端练习/试炼题目（剥离标准答案，防作弊）。
	 *
	 * <p>题目来源可组合：sectionId → 小节绑定题库题目；knowledgePointIds → 知识点绑定题目并集；
	 * repoId → 题库直练。按题型/难度过滤。count 为空时返回全部命中题目；显式传 count 时按指定数量返回（上限 50）。
	 * perKp 有值时按每个知识点抽取指定题数（专项练习多知识点场景）。</p>
	 *
	 * @param sectionId        小节ID（小节练习数据源）
	 * @param knowledgePointIds 知识点ID集合（专项练习多选，可空）
	 * @param repoId           题库ID（题库直练数据源）
	 * @param questionId       题目ID（单题重做数据源，优先于其他来源）
	 * @param count            返回题目数量（为空=全部，上限 50）
	 * @param perKp            每个知识点抽取题数（可空）
	 * @param types            题型过滤（可选）
	 * @param difficulty       难度过滤（可选）
	 * @param random           是否随机排序
	 * @param exposeAnswer     是否返回标准答案
	 * @param usage            用途场景（preview 预习 / practice 专项练习 / trial 小节通关），
	 *                         用于按做题库用途收敛范围；为空表示不过滤（历史行为）
	 * @return 题目列表；无数据返回空列表
	 */
	List<StudentQuestionView> studyQuestions(String sectionId, List<String> knowledgePointIds, String repoId,
			String questionId, Integer count, Integer perKp, List<String> types, String difficulty,
			Boolean random, Boolean exposeAnswer, String usage);

	/**
	 * 学员端小节练习配置（出题策略来源）。
	 *
	 * @param sectionId 小节ID
	 * @return 练习配置（缺失或非法时返回缺省配置）
	 */
	SectionPracticeConfig sectionPracticeConfig(String sectionId);

	/**
	 * 学员预习完成（「预习完成」按钮）：标记该小节/知识点预习完成（学习完成度 100%）
	 * 并结算奖励（学习币 +5 / 学海积分 +3）。
	 *
	 * <p>防刷：同一小节/知识点仅首次结算奖励，重复调用返回 firstTime=false 且不再发放。</p>
	 *
	 * @param request 预习完成请求（sectionId 或 knowledgePointId 至少其一）
	 * @return 奖励结算结果（firstTime/coins/points）
	 */
	StudentPreviewCompleteView completePreview(StudentPreviewCompleteRequest request);

	/**
	 * 个人中心档案（姓名/学号/头衔/累计积分/知识点/薄弱数）。
	 */
	StudentProfileView profile();

	/**
	 * 个人中心-积分板块（当前积分/头衔/下一目标/规则/最近明细）。
	 */
	StudentPointsView points();

	/**
	 * 本学期学习币（分学科 + 手动发币合计，单科上限 10000）。
	 */
	StudentCoinsView coins();

	/**
	 * 学员端主页今日总览 + 积分获取引导。
	 */
	StudentTodayView today();

	/**
	 * 薄弱知识点列表（status=active，含掌握度与攻克奖励）。
	 */
	List<StudentWeakView> weakList();

	/**
	 * 知识点详情（掌握度/评级/薄弱/是否已预习）。
	 *
	 * @param knowledgePointId 知识点ID
	 */
	StudentKnowledgeDetailView knowledgeDetail(String knowledgePointId);

	/**
	 * 学习完成统一结算（预习/练习/试炼/错题订正等，走积分·学币账本）。
	 *
	 * @param request 结算请求
	 * @return 结算结果
	 */
	StudentPreviewCompleteView completeLearning(StudentLearningCompleteRequest request);

	/**
	 * 错题重做：答对则标记订正、移出错题本、刷新薄弱并结算奖励。
	 *
	 * @param request 错题重做请求
	 * @return 重做结果
	 */
	StudentWrongRedoView wrongRedo(StudentWrongRedoRequest request);

	/**
	 * 薄弱知识点攻克（复测达标后消除薄弱标记并结算奖励）。
	 *
	 * @param request 攻克请求
	 * @return 攻克结果
	 */
	StudentWeakConquerView weakConquer(StudentWeakConquerRequest request);

}
