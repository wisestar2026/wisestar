package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishUnitQuery;
import cn.wisestar.server.domain.dto.english.EnglishUnitView;

import java.util.List;

/**
 * 英语单元目录服务（管理端与学员端共用）。
 *
 * @author wisestar
 * @date 2026/9/15
 */
public interface EnglishUnitService {

	/**
	 * 单元分页列表（带单词数/句子数）。
	 *
	 * @param query 查询条件
	 * @return 分页单元列表
	 */
	PaginationResponse<EnglishUnitView> list(EnglishUnitQuery query);

	/**
	 * 按教材维度查询全部单元（不分页，学员端用）。
	 *
	 * @param version 版本
	 * @param grade 年级
	 * @param term 学期/册别
	 * @return 单元列表
	 */
	List<EnglishUnitView> listByBook(String version, String grade, String term);

	/**
	 * 新增/更新单元（有 id 为更新，否则新增）。
	 *
	 * @param view 单元
	 */
	void saveOrUpdate(EnglishUnitView view);

	/**
	 * 删除单元。
	 *
	 * @param id 单元 ID
	 */
	void delete(String id);

}
