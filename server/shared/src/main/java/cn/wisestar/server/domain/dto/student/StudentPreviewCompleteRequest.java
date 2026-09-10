package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学员预习完成请求（知识点预习讲完后的「预习完成」按钮）。
 *
 * <p>小节预习传 sectionId，知识点预习传 knowledgePointId；同一目标仅首次结算奖励。</p>
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentPreviewCompleteRequest {

	/** 小节ID（小节预习入口） */
	private String sectionId;

	/** 知识点ID（知识点预习入口） */
	private String knowledgePointId;

	/** 来源题库ID（可空） */
	private String repoId;

}
