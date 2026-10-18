package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.EnglishPatternScenarioView;
import cn.wisestar.server.domain.dto.english.EnglishPatternSentence;

import java.util.List;

/**
 * 英语重点句式情景生成服务（按分组子集生成，供内容包预生成调用）。
 *
 * <p>依据给定的一组单元重点句式调用系统 AI 设置生成若干生活情景对话：给出部分英文台词，
 * 若干轮次仅给中文情景提示，由学员从候选项中选出符合情景的重点句式。结果不落库，由调用方
 * （情景内容包服务）负责分组编排与固化。</p>
 *
 * @author wisestar
 * @date 2026/10/12
 */
public interface EnglishPatternScenarioService {

	/**
	 * 依据一组单元重点句式生成情景练习。
	 *
	 * @param grade     年级
	 * @param unit      单元
	 * @param sentences 该分组内的重点句式（含释义），不能为空
	 * @param count      本组需要生成的情景数量
	 * @param avoidTitles 本单元已使用的情景标题，生成时需避开
	 * @return 情景列表，无可用句式时返回空列表
	 */
	List<EnglishPatternScenarioView> generateFromSentences(String grade, String unit,
			List<EnglishPatternSentence> sentences, int count, List<String> avoidTitles);

}
