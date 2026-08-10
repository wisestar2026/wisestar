package cn.wisestar.server.service;

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
	 * @return 章节视图列表（含小节数统计）
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
	 * 删除章节（级联逻辑删除其下小节与知识点及题目绑定）。
	 *
	 * @param request 章节请求（含 id）
	 */
	void deleteChapter(ChapterRequest request);

}
