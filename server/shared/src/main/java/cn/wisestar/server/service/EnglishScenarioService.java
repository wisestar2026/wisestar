package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.EnglishScenarioView;

import java.util.List;

/**
 * 英语情景对话生成服务（学员端「场景对话练习」）。
 *
 * <p>依据指定 版本+年级+册别+单元 的单词，调用系统 AI 设置实时生成若干生活情景对话，
 * 并把单元目标单词挖空形成填空练习；结果不落库，每次调用重新生成。</p>
 *
 * @author wisestar
 * @date 2026/10/12
 */
public interface EnglishScenarioService {

	/**
	 * 按单元单词生成情景对话练习。
	 *
	 * @param version 教材版本
	 * @param grade   年级
	 * @param term    学期/册别（上册/下册）
	 * @param unit    单元
	 * @return 情景对话列表（3-4 个场景），无可用单词时返回空列表
	 */
	List<EnglishScenarioView> generateScenarios(String version, String grade, String term, String unit);

}
