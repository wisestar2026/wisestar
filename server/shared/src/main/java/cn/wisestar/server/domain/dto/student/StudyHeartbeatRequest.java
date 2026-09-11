package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学员学习心跳请求。
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Data
public class StudyHeartbeatRequest {

	/** 当前学习学科ID（可空） */
	private String subjectId;

}
