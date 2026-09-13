package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学员学习心跳响应。
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Data
public class StudyHeartbeatView {

	/** 当前会话累计时长（毫秒） */
	private Long durationMs;

	/** 本次心跳是否触发生成了当日学习总结 */
	private Boolean generated;

	/** 在线时长宝箱（当日在线分钟 + 三档状态） */
	private StudentOnlineChestView onlineChest;

}
