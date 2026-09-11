package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.student.StudyHeartbeatRequest;
import cn.wisestar.server.domain.dto.student.StudyHeartbeatView;

/**
 * 学习会话服务。
 *
 * <p>接收学员端心跳，按 30 分钟间隔续会话或开启新会话，累计时长达到 60 分钟时触发当日学习总结。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
public interface StudySessionService {

	/**
	 * 记录一次学习心跳。
	 *
	 * @param request 心跳请求（可携带当前学科ID）
	 * @return 当前会话累计时长与本次是否生成总结
	 */
	StudyHeartbeatView heartbeat(StudyHeartbeatRequest request);

}
