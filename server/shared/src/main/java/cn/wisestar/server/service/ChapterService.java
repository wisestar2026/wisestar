package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.RepoView;
import cn.wisestar.server.domain.dto.knowledge.ChapterRepoRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterView;

import java.util.List;

/**
 * 章节管理服务（知识管理板块二级维度）。
 *
 * @author wisestar
 * @date 2026/8/10
 */
public interface ChapterService {

	/**
	 * 章节列表（按学科过滤，sort 升序）。
	 *
	 * @param query 查询条件（subjectId 可选；为空返回全部章节）
	 * @return 章节视图列表（含小节数统计与已绑定题库数统计）
	 */
	List<ChapterView> listChapters(ChapterRequest query);

	/**
	 * 新增章节。
	 *
	 * @param request 章节请求
	 * @return 新章节 id
	 */
	String addChapter(ChapterRequest request);

	/**
	 * 更新章节。
	 *
	 * @param request 章节请求（含 id）
	 */
	void updateChapter(ChapterRequest request);

	/**
	 * 删除章节（级联逻辑删除其下小节、知识点、知识点题目绑定及章节题库绑定）。
	 *
	 * @param request 章节请求（含 id）
	 */
	void deleteChapter(ChapterRequest request);

	/**
	 * 保存章节-题库绑定（全量替换：先清空旧绑定再写入新绑定）。
	 *
	 * <p>章节题库只能从题库管理（t_repo）选择，不能在此新增。</p>
	 *
	 * @param request 绑定请求（chapterId + repoIds）
	 */
	void saveRepos(ChapterRepoRequest request);

	/**
	 * 查询章节已绑定的题库列表（题库管理 t_repo 数据，保持绑定顺序）。
	 *
	 * @param chapterId 章节ID
	 * @return 已绑定题库视图列表
	 */
	List<RepoView> listRepos(String chapterId);

}
