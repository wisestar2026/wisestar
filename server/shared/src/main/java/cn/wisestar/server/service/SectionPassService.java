package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.student.SectionPassResult;

/**
 * 小节通关服务。
 *
 * <p>交卷后按正确率与通关阈值判定是否通关，写入或更新 t_section_pass，
 * 返回本次结果与历史最佳（最佳值单调、通关状态不可逆）。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
public interface SectionPassService {

	/**
	 * 记录一次小节通关交卷。
	 *
	 * @param userId   学员ID
	 * @param sectionId 小节ID
	 * @param rate     本次正确率（百分比）
	 * @param score    本次得分
	 * @param totalScore 本次总分
	 * @param passRate 通关阈值（百分比）
	 * @param unlockedNext 本节通关后是否解锁下一小节
	 * @return 本次通关结果与历史最佳
	 */
	SectionPassResult record(String userId, String sectionId, int rate, double score, double totalScore,
			int passRate, boolean unlockedNext);

}
