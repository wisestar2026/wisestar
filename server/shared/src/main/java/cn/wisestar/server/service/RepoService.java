package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.*;

import java.util.List;
import java.util.Set;

/**
 * 题目模板库（题库）服务接口（RepoService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供题库（Repo）与题库题目（模板）的管理能力：题库分页、
 * 题库 CRUD、题库绑定/解绑模板、从题库挑题（随机问卷数据源）、从模板导入、
 * 用户错题本（UserBook）管理、题库选择器、题库题目导出。
 * 题库是 AI 自习室系统中知识点（subject/chapter/knowledgePoint/difficulty）
 * 组织与复用试题的核心载体。实现类位于 rdbms 模块（RepoServiceImpl）。</p>
 *
 * @author javahuang
 * @date 2022/4/27
 */
public interface RepoService {

	/**
	 * @param query
	 * @return 题库列表
	 */
	PaginationResponse<RepoView> listRepo(RepoQuery query);

	/**
	 * 获取单个模板库
	 * 
	 * @param id
	 * @return
	 */
	RepoView getRpo(String id);

	/**
	 * 添加题库
	 * 
	 * @param request 题库
	 */
	void addRepo(RepoRequest request);

	/**
	 * 更新题库
	 * 
	 * @param request 题库
	 */
	void updateRepo(RepoRequest request);

	/**
	 * 删除题库
	 * 
	 * @param request 题库
	 */
	void deleteRepo(RepoRequest request);

	/**
	 * 批量添加题库模板
	 * 
	 * @param request
	 */
	void batchAddRepoTemplate(RepoTemplateRequest request);

	/**
	 * 解除题库与模板的绑定关系
	 * 
	 * @param request
	 */
	void batchUnBindTemplate(RepoTemplateRequest request);

	/**
	 * 批量绑定已有题目到题库（题目管理中的题目 → 指定题库）。
	 * 仅更新题目归属（repoId），不修改题目内容；已在目标题库的题目自动跳过。
	 *
	 * @param request 含 repoId（目标题库）+ ids（题目 ID 列表）
	 */
	void bindTemplates(RepoTemplateRequest request);

	/**
	 * 从题库里面挑选试题
	 * 
	 * @param repos
	 * @return
	 */
	List<SurveySchema> pickQuestionFromRepo(List<ProjectSetting.RandomSurveyCondition> repos);

	/**
	 * 从模板导入到题库
	 * 
	 * @param request
	 */
	void importFromTemplate(RepoTemplateRequest request);

	/**
	 * 分页查询用户错题本。
	 *
	 * @param query 分页 + 筛选条件（见 {@link UserBookQuery}）
	 * @return 错题本分页列表（UserBookView）
	 */
	PaginationResponse<UserBookView> listUserBook(UserBookQuery query);

	/**
	 * 查询当前登录学员「我的题库」。
	 *
	 * <p><b>分配来源</b>（两者并集，去重）：</p>
	 * <ul>
	 *   <li>老师手动分配：t_user_repo 中 user_id=当前用户 的题库</li>
	 *   <li>系统按标签自动分配：题库 tag 与学员标签（t_tag, category=user）有交集</li>
	 * </ul>
	 *
	 * @return 可练习题库列表（每项回填题目总数 total）
	 */
	List<RepoView> myRepos();

	/**
	 * 老师手动分配题库给学员（批量）。
	 *
	 * @param userId  学员用户 ID
	 * @param repoIds 题库 ID 列表
	 */
	void assignRepo(String userId, List<String> repoIds);

	/**
	 * 删除分配记录（批量）。
	 *
	 * @param ids 分配记录 ID 列表
	 */
	void deleteAssign(List<String> ids);

	/**
	 * 查询某学员的分配记录（管理端）。
	 *
	 * @param userId 学员用户 ID
	 * @return 分配记录列表（含学员/题库信息）
	 */
	List<RepoAssignView> listAssign(String userId);

	/**
	 * 查询学员标签（t_tag, category=user）。
	 *
	 * @param userId 学员用户 ID
	 * @return 学员标签集合
	 */
	Set<String> getUserTags(String userId);

	/**
	 * 保存学员标签（覆盖式，category=user），用于按标签自动分配题库。
	 *
	 * @param userId 学员用户 ID
	 * @param tags   标签数组（为空则清除全部）
	 */
	void saveUserTags(String userId, String[] tags);

	/**
	 * 创建错题本记录。
	 *
	 * @param request 错题本请求（见 {@link UserBookRequest}）
	 */
	void createUserBook(UserBookRequest request);

	/**
	 * 更新错题本记录。
	 *
	 * @param request 错题本请求（含 id）
	 * @return 更新后的错题本视图
	 */
	UserBookView updateUserBook(UserBookRequest request);

	/**
	 * 删除错题本记录。
	 *
	 * @param request 错题本请求（含 id）
	 */
	void deleteUserBook(UserBookRequest request);

	/**
	 * 题库选择器数据源。
	 *
	 * @param request 查询条件（见 {@link SelectRepoRequest}）
	 * @return 题库视图列表
	 */
	List<RepoView> selectRepo(SelectRepoRequest request);

	/**
	 * 导出题库题目
	 * 
	 * @param request
	 */
	void exportRepoQuestions(RepoRequest request);
}
