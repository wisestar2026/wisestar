package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 奖励结算上下文（统一结算入参）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class RewardContext {

	/** 学员ID */
	private String userId;

	/** 学习行为类型（见 StudentRewardConstants.ACTION_*） */
	private String actionType;

	/** 学科ID（可空） */
	private String subjectId;

	/** 知识点ID（可空） */
	private String knowledgePointId;

	/** 小节ID（可空） */
	private String sectionId;

	/** 章节ID（可空） */
	private String chapterId;

	/** 业务关联ID（幂等键，可空） */
	private String refId;

	/** 章节阶段（仅 chapter_stage） */
	private Integer stage;

	/** 学习时长（毫秒，可空） */
	private Long durationMs;

	/** 正确率（0-100，可空） */
	private Integer correctRate;

}
