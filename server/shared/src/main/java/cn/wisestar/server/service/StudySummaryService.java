package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.student.StudySummaryView;

/**
 * 当日学习总结服务。
 *
 * <p>聚合学员当日练习记录、答题明细、学习行为、掌握度与薄弱点数据，调用系统 AI 生成总结；
 * AI 未启用或失败时降级为规则模板（model=rule）。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
public interface StudySummaryService {

	/**
	 * 生成或刷新指定学员指定日期的学习总结。
	 *
	 * @param studentId   学员ID
	 * @param summaryDate 总结日期 yyyy-MM-dd
	 * @param sessionId   触发总结的学习会话ID（可空）
	 * @return 生成的总结视图
	 */
	StudySummaryView generate(String studentId, String summaryDate, String sessionId);

	/**
	 * 查询当前登录学员本人当日学习总结。
	 *
	 * @return 总结视图，尚未生成时返回 null
	 */
	StudySummaryView getMySummary();

	/**
	 * 查询指定学员指定日期（默认当天）学习总结。
	 *
	 * @param studentId 学员ID
	 * @param date      日期 yyyy-MM-dd，为空取当天
	 * @return 总结视图，尚未生成时返回 null
	 */
	StudySummaryView getStudentSummary(String studentId, String date);

}
