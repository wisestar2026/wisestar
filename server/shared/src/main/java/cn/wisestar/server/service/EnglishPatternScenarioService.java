package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.EnglishPatternScenarioView;

import java.util.List;

/**
 * 英语重点句式情景生成服务（学员端「句式情景」）。
 *
 * <p>依据指定 版本+年级+册别+单元 的重点句式（{@code t_english_sentence}），调用系统 AI 设置
 * 实时生成若干生活情景对话：给出部分英文台词，若干轮次仅给中文情景提示，由学员从候选项
 * 中选出符合情景的重点句式。结果不落库，每次调用按需重新生成。</p>
 *
 * @author wisestar
 * @date 2026/10/12
 */
public interface EnglishPatternScenarioService {

	/**
	 * 按单元重点句式生成情景练习。
	 *
	 * @param version 教材版本
	 * @param grade   年级
	 * @param term    学期/册别（可空）
	 * @param unit    单元
	 * @return 情景列表（3-4 个场景）；无可用句式时返回空列表
	 */
	List<EnglishPatternScenarioView> generateScenarios(String version, String grade, String term, String unit);

}
