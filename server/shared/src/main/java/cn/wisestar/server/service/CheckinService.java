package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.student.StudentCheckinView;

/**
 * 学员每日签到服务。
 *
 * <p>签到固定发放学习币，每个自然日仅一次，无连续签到天数概念。</p>
 *
 * @author wisestar
 * @date 2026/9/12
 */
public interface CheckinService {

	/**
	 * 查询当日签到状态。
	 *
	 * @param userId 学员 ID
	 * @return 签到状态（含签到可得学习币）
	 */
	StudentCheckinView view(String userId);

	/**
	 * 领取当日签到奖励。
	 *
	 * @param userId 学员 ID
	 * @return 领取结果
	 */
	StudentCheckinView checkin(String userId);

}
