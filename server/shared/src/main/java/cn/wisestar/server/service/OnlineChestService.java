package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.student.StudentOnlineChestClaimView;
import cn.wisestar.server.domain.dto.student.StudentOnlineChestView;

/**
 * 在线时长宝箱服务。
 *
 * <p>以自然日心跳在线会话累计时长为进度，30/60/120 分钟三档；领取复用统一奖励账本，
 * 按「学员 + 档位 + 日期」幂等，不新增表、不新增心跳链路。</p>
 *
 * @author wisestar
 * @date 2026/9/12
 */
public interface OnlineChestService {

	/**
	 * 当日宝箱视图（在线时长 + 三档状态）。
	 *
	 * @param userId 学员ID
	 * @return 宝箱视图；学员ID为空时返回未达标状态
	 */
	StudentOnlineChestView view(String userId);

	/**
	 * 领取指定档位宝箱。
	 *
	 * @param userId       学员ID
	 * @param tierMinutes  档位分钟数（30/60/120）
	 * @param subjectId    学科ID（可空，缺省回退学员有效权限学科）
	 * @return 领取结果（含最新宝箱状态）
	 */
	StudentOnlineChestClaimView claim(String userId, Integer tierMinutes, String subjectId);

}
