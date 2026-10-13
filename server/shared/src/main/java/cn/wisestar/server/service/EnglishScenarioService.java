package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.EnglishScenarioView;
import cn.wisestar.server.domain.dto.english.EnglishScenarioWord;

import java.util.List;

/**
 * 英语情景对话生成服务（按分组子集生成，供内容包预生成调用）。
 *
 * <p>依据给定的一组单元单词调用系统 AI 设置生成若干生活情景对话，并把目标单词挖空形成
 * 填空练习；结果不落库，由调用方（情景内容包服务）负责分组编排与固化。</p>
 *
 * @author wisestar
 * @date 2026/10/12
 */
public interface EnglishScenarioService {

	/**
	 * 依据一组单元单词生成情景对话练习。
	 *
	 * @param grade 年级
	 * @param unit  单元
	 * @param words 该分组内的单词（含释义），不能为空
	 * @return 情景对话列表（3-4 个场景），无可用单词时返回空列表
	 */
	List<EnglishScenarioView> generateFromWords(String grade, String unit, List<EnglishScenarioWord> words);

}
