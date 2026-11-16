package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.review.ReviewAnswerCommand;
import cn.wisestar.server.domain.dto.review.ReviewAnswerResult;
import cn.wisestar.server.domain.dto.review.ReviewForecastView;
import cn.wisestar.server.domain.dto.review.ReviewPlanView;
import cn.wisestar.server.domain.dto.review.ReviewQueueItemView;
import cn.wisestar.server.domain.dto.review.ReviewQueueRequest;
import cn.wisestar.server.domain.dto.review.WeakBoardItemView;
import cn.wisestar.server.domain.dto.review.WeakSessionRequest;

import java.util.List;

/**
 * 记忆调度（遗忘曲线）服务。
 *
 * <p>以 FSRS-5 维护每个知识点的难度、稳定度、可提取度与到期时间，历史不足时用 SM-2 冷启动；
 * 负责复习计划、复习队列、到期预测、薄弱看板与攻克训练。英语按 word/sentence/grammar
 * item 级接入，其他学科按知识点级接入。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
public interface MemoryReviewService {

	/**
	 * 单次作答入账。
	 *
	 * @param userId 学员ID
	 * @param command 作答入参
	 * @return 调度结果
	 */
	ReviewAnswerResult recordAnswer(String userId, ReviewAnswerCommand command);

	/**
	 * 知识点级会话入账（按会话正确率折算评分）。
	 *
	 * @param userId 学员ID
	 * @param subjectId 学科ID
	 * @param knowledgePointId 知识点ID
	 * @param knowledgeName 知识点名称
	 * @param correctRate 会话正确率 0-100
	 * @param total 会话题量
	 * @return 调度结果
	 */
	ReviewAnswerResult recordKpPractice(String userId, String subjectId, String knowledgePointId,
			String knowledgeName, int correctRate, int total);

	/**
	 * 今日复习计划。
	 *
	 * @param userId 学员ID
	 * @param subjectId 学科ID，可空
	 * @return 计划视图
	 */
	ReviewPlanView plan(String userId, String subjectId);

	/**
	 * 复习队列。
	 *
	 * @param userId 学员ID
	 * @param request 查询条件
	 * @return 队列条目
	 */
	List<ReviewQueueItemView> queue(String userId, ReviewQueueRequest request);

	/**
	 * 到期预测。
	 *
	 * @param userId 学员ID
	 * @param subjectId 学科ID，可空
	 * @param days 预测天数
	 * @return 预测视图
	 */
	ReviewForecastView forecast(String userId, String subjectId, int days);

	/**
	 * 薄弱看板。
	 *
	 * @param userId 学员ID
	 * @param subjectId 学科ID，可空
	 * @param limit 条数上限
	 * @return 看板条目
	 */
	List<WeakBoardItemView> weakBoard(String userId, String subjectId, int limit);

	/**
	 * 攻克训练组卷。
	 *
	 * @param userId 学员ID
	 * @param request 组卷条件
	 * @return 训练条目
	 */
	List<ReviewQueueItemView> weakSession(String userId, WeakSessionRequest request);

	/**
	 * 幂等回填既有学习记录到调度表。
	 *
	 * @param userId 学员ID
	 * @return 新增条数
	 */
	int backfill(String userId);

}
