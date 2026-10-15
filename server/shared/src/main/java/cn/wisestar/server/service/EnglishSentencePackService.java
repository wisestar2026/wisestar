package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishSentenceExercise;
import cn.wisestar.server.domain.dto.english.EnglishSentencePackQuery;
import cn.wisestar.server.domain.dto.english.EnglishSentencePackView;

import java.util.List;

/**
 * 英语单元句子同步练习内容包服务（预生成 + 固化落库 + 学员端只读）。
 *
 * <p>管理端按 版本+年级+册别+单元 分组调用 AI 生成多题型句子练习预览（不落库），
 * 确认后覆盖保存；学员端仅读取已固化内容，不再触发实时 AI。</p>
 *
 * @author wisestar
 * @date 2026/10/15
 */
public interface EnglishSentencePackService {

	/**
	 * 按单元生成句子练习内容包预览（分组调用 AI，不落库）。
	 */
	EnglishSentencePackView generate(String version, String grade, String term, String unit);

	/**
	 * 保存句子练习内容包（同 版本+年级+册别+单元 覆盖更新）。
	 */
	EnglishSentencePackView savePack(EnglishSentencePackView pack);

	/**
	 * 内容包分页列表（不含正文）。
	 */
	PaginationResponse<EnglishSentencePackView> listPacks(EnglishSentencePackQuery query);

	/**
	 * 内容包详情（含正文）。
	 */
	EnglishSentencePackView getPack(String id);

	/**
	 * 逻辑删除内容包。
	 */
	void deletePack(String id);

	/**
	 * 查询指定册下存在重点句子的单元列表（用于批量生成）。
	 */
	List<String> listUnits(String version, String grade, String term);

	/**
	 * 学员端读取：该单元已固化的句子练习题（无内容时返回空列表）。
	 */
	List<EnglishSentenceExercise> getExercises(String version, String grade, String term, String unit);

}
