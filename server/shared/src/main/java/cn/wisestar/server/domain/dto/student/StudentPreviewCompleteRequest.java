package cn.wisestar.server.domain.dto.student;

import cn.wisestar.server.domain.dto.PracticeSubmitRequest;
import lombok.Data;

import java.util.List;

/**
 * 学员预习完成请求（知识点预习讲完后的「预习完成」按钮）。
 *
 * <p>小节预习传 sectionId，知识点预习传 knowledgePointId；同一目标仅首次结算奖励。</p>
 *
 * <p>例题检测结果通过 {@link #items} 一并上报：后端回源题目复核判分后刷新知识点掌握度
 * （仅供学情评价，不写练习会话/错题本，也不额外发放练习奖励）。</p>
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

	/** 例题检测作答（可空；仅用于刷新掌握度，不落练习会话） */
	private List<PracticeSubmitRequest.PracticeItem> items;

}
