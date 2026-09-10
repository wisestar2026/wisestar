package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学习完成统一结算请求（预习/练习/试炼/错题订正等）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentLearningCompleteRequest {

	/** 学习行为类型（见 StudentRewardConstants.ACTION_*） */
	private String actionType;

	/** 知识点ID（可空） */
	private String knowledgePointId;

	/** 小节ID（可空） */
	private String sectionId;

	/** 章节ID（可空） */
	private String chapterId;

	/** 业务关联ID（可空，用于幂等） */
	private String refId;

	/** 学习时长（毫秒，可空） */
	private Long durationMs;

	/** 正确率（0-100，可空） */
	private Integer correctRate;

}
