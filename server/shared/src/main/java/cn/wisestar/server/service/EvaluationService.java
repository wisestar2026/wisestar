package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.student.PracticeEvaluationContext;

/**
 * 学员薄弱点·学习结果评价服务。
 *
 * <p>以 `t_practice_detail` 为唯一数据源，按知识点刷新掌握度、研判薄弱、自动攻克；
 * 掌握度采用「最近 5 次练习正确率加权（越近权重越高）」计算。</p>
 *
 * @author wisestar
 * @date 2026/9/10
 */
public interface EvaluationService {

	/**
	 * 一次交卷落库后刷新评价。
	 *
	 * @param context 练习学情上下文（会话定位 + 逐题对错 + 题目知识点名称）
	 */
	void recordPractice(PracticeEvaluationContext context);

	/**
	 * 薄弱点攻克（显式提交）。
	 *
	 * @param userId          学员ID
	 * @param knowledgePointId 知识点ID
	 * @param correctRate     本次正确率 0-100
	 * @return 是否消除薄弱标记
	 */
	boolean conquer(String userId, String knowledgePointId, int correctRate);

	/**
	 * 错题订正后刷新薄弱状态（无未订正错题且掌握度达标则移出薄弱）。
	 *
	 * @param userId          学员ID
	 * @param knowledgePointId 知识点ID
	 */
	void refreshWeakAfterCorrection(String userId, String knowledgePointId);

}
