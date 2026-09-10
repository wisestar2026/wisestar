package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.student.RewardContext;
import cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView;

/**
 * 学员积分·学币统一结算服务。
 *
 * <p>所有奖励的合法性校验、7 天防刷、单科 3000 上限、头衔晋升均在本服务内完成；
 * 调用方只负责传入行为上下文并展示结果。</p>
 *
 * @author wisestar
 * @date 2026/9/10
 */
public interface RewardService {

	/**
	 * 结算一次学习行为奖励（幂等、防刷、上限裁剪、头衔晋升）。
	 *
	 * @param context 结算上下文
	 * @return 结算结果（firstTime/coins/points/title 等）；未知行为返回 ok=false
	 */
	StudentPreviewCompleteView settle(RewardContext context);

	/**
	 * 当前学员累计学海积分。
	 */
	int currentPoints(String userId);

}
