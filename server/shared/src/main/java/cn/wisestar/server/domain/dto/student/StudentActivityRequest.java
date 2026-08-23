package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学员实时位置上报请求（学员端路由变化/进入习题时调用）。
 *
 * @author wisestar
 * @date 2026/8/21
 */
@Data
public class StudentActivityRequest {

	/** 当前页面标识（学员端路由路径，如 /student/study） */
	private String page;

	/** 当前习题ID（可为空） */
	private String questionId;

	/** 小节ID（习题上下文，可为空） */
	private String sectionId;

}
