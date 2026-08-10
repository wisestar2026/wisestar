package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.knowledge.SectionRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionView;

import java.util.List;

/**
 * 小节管理服务（知识管理板块三级维度）。
 *
 * @author wisestar
 * @date 2026/8/10
 */
public interface SectionService {

	/**
	 * 小节列表（按章节过滤，sort 升序）。
	 *
	 * @param query 查询条件（chapterId 可选；为空返回全部小节）
	 * @return 小节视图列表（含知识点数统计）
	 */
	List<SectionView> listSections(SectionRequest query);

	/**
	 * 新增小节。
	 *
	 * @param request 小节请求
	 * @return 新小节 id
	 */
	String addSection(SectionRequest request);

	/**
	 * 更新小节（含内容设置/练习设置 JSON）。
	 *
	 * @param request 小节请求（含 id）
	 */
	void updateSection(SectionRequest request);

	/**
	 * 删除小节（级联逻辑删除其下知识点与题目绑定）。
	 *
	 * @param request 小节请求（含 id）
	 */
	void deleteSection(SectionRequest request);

}
