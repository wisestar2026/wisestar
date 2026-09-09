package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishAiPackQuery;
import cn.wisestar.server.domain.dto.english.EnglishAiPackView;
import cn.wisestar.server.domain.dto.english.PackSyncResult;

/**
 * 英语 AI 单元内容包服务（生成/保存/同步词库与语法库）。
 *
 * @author wisestar
 * @date 2026/9/9
 */
public interface EnglishAiPackService {

	/**
	 * 内容包分页列表（按版本/年级/单元筛选，不含 content 正文）。
	 */
	PaginationResponse<EnglishAiPackView> listPacks(EnglishAiPackQuery query);

	/**
	 * 内容包详情（含 content 正文）。
	 */
	EnglishAiPackView getPack(String id);

	/**
	 * 调用大模型为指定版本/年级/单元生成整套学习内容（单词/语法/例句/练习），
	 * 仅返回预览，不落库。
	 *
	 * @param version 教材版本
	 * @param grade   年级
	 * @param unit    单元
	 * @param topic   主题（可选，用于引导生成方向）
	 * @return 生成的预览内容包（无 id）
	 */
	EnglishAiPackView generateUnit(String version, String grade, String unit, String topic);

	/**
	 * 保存内容包（人工确认生成结果后落库）。同 版本+年级+单元 已存在内容包时覆盖更新。
	 *
	 * @param pack 内容包（含 content 正文）
	 * @return 保存后的内容包
	 */
	EnglishAiPackView savePack(EnglishAiPackView pack);

	/**
	 * 将内容包内容同步到正式词库（t_english_word）与语法库（t_english_grammar）。
	 *
	 * @param id 内容包 ID
	 * @return 同步结果统计
	 */
	PackSyncResult syncToBank(String id);

	/**
	 * 删除内容包。
	 */
	void deletePack(String id);

}
