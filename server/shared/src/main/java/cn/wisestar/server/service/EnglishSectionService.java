package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishSectionQuery;
import cn.wisestar.server.domain.dto.english.EnglishSectionView;

import java.util.List;

/**
 * 英语小节目录服务（管理端与学员端共用）。
 *
 * @author wisestar
 * @date 2026/9/18
 */
public interface EnglishSectionService {

	/**
	 * 小节分页列表。
	 *
	 * @param query 查询条件
	 * @return 分页小节列表
	 */
	PaginationResponse<EnglishSectionView> list(EnglishSectionQuery query);

	/**
	 * 按单元查询全部小节（不分页，用于下拉候选与学员端分组）。
	 *
	 * @param version 版本
	 * @param grade 年级
	 * @param term 学期/册别
	 * @param unit 单元
	 * @return 小节列表
	 */
	List<EnglishSectionView> listByUnit(String version, String grade, String term, String unit);

	/**
	 * 新增/更新小节（有 id 为更新，否则新增；同一单元下重名将拒绝）。
	 *
	 * @param view 小节
	 */
	void saveOrUpdate(EnglishSectionView view);

	/**
	 * 删除小节（仅删除目录项，不级联删除内容）。
	 *
	 * @param id 小节 ID
	 */
	void delete(String id);

}
