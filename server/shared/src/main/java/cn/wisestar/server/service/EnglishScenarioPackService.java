package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishPatternScenarioView;
import cn.wisestar.server.domain.dto.english.EnglishScenarioPackQuery;
import cn.wisestar.server.domain.dto.english.EnglishScenarioPackView;
import cn.wisestar.server.domain.dto.english.EnglishScenarioView;

import java.util.List;

/**
 * 英语单元情景练习内容包服务（预生成 + 固化落库 + 学员端只读）。
 *
 * <p>管理端按 版本+年级+册别+单元 生成单词情景与句式情景预览（不落库），确认后覆盖保存；
 * 学员端仅读取已固化内容并按组顺序合并展示，不再触发实时 AI。</p>
 *
 * @author wisestar
 * @date 2026/10/13
 */
public interface EnglishScenarioPackService {

	/**
	 * 按单元生成情景内容包预览（分组调用 AI，不落库）。
	 *
	 * @param version 教材版本
	 * @param grade   年级
	 * @param term    学期/册别
	 * @param unit    单元
	 * @return 预览内容包（无 id）
	 */
	EnglishScenarioPackView generate(String version, String grade, String term, String unit);

	/**
	 * 只重新生成已固化内容包的句式情景，保留单词情景与阅读理解缓存。
	 *
	 * @param version 教材版本
	 * @param grade   年级
	 * @param term    册别
	 * @param unit    单元
	 * @return 更新后的内容包
	 */
	EnglishScenarioPackView regeneratePattern(String version, String grade, String term, String unit);

	/**
	 * 保存情景内容包（同 版本+年级+册别+单元 覆盖更新）。
	 *
	 * @param pack 内容包（含分组 JSON 正文）
	 * @return 保存后的内容包
	 */
	EnglishScenarioPackView savePack(EnglishScenarioPackView pack);

	/**
	 * 情景内容包分页列表（不含正文）。
	 */
	PaginationResponse<EnglishScenarioPackView> listPacks(EnglishScenarioPackQuery query);

	/**
	 * 情景内容包详情（含正文）。
	 */
	EnglishScenarioPackView getPack(String id);

	/**
	 * 逻辑删除情景内容包。
	 */
	void deletePack(String id);

	/**
	 * 查询指定册下存在单词或句式的单元列表（用于批量生成）。
	 */
	List<String> listUnits(String version, String grade, String term);

	/**
	 * 学员端读取：合并返回该单元已固化的单词情景（按组顺序展平），无内容时返回空列表。
	 */
	List<EnglishScenarioView> getWordScenarios(String version, String grade, String term, String unit);

	/**
	 * 学员端读取：合并返回该单元已固化的句式情景（按组顺序展平），无内容时返回空列表。
	 */
	List<EnglishPatternScenarioView> getPatternScenarios(String version, String grade, String term, String unit);

}
