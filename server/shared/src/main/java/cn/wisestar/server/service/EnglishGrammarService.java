package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishGrammarQuery;
import cn.wisestar.server.domain.dto.english.EnglishGrammarView;

/**
 * 英语语法服务（管理端与 AI 同步共用）。
 *
 * @author wisestar
 * @date 2026/9/18
 */
public interface EnglishGrammarService {

	/**
	 * 语法分页列表。
	 *
	 * @param query 查询条件
	 * @return 分页语法列表
	 */
	PaginationResponse<EnglishGrammarView> list(EnglishGrammarQuery query);

	/**
	 * 新增/更新语法（有 id 为更新，否则新增）。
	 *
	 * @param view 语法条目
	 */
	void saveOrUpdate(EnglishGrammarView view);

	/**
	 * 删除语法。
	 *
	 * @param id 语法 ID
	 */
	void delete(String id);

	/**
	 * AI 内容包同步语法：按「版本 + 年级 + 学期 + 单元 + 小节 + 标题」判定新增或更新。
	 *
	 * @param view 语法条目（至少包含版本/年级/学期/单元/小节/标题）
	 * @return 是否同步成功
	 */
	boolean upsertFromAi(EnglishGrammarView view);

}
