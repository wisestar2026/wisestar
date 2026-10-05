package cn.wisestar.server.domain.dto.detect;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 知识点检测交卷请求。
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Data
public class DetectSubmitRequest {

	/** 逐题作答结果 */
	private List<Item> items;

	/** 学科ID（WPB 落库与基线判定；可空，为空时不参与基线） */
	private String subjectId;

	/** 年级快照 */
	private String grade;

	/** 册别快照 */
	private String term;

	/** 学期键（可空，服务端按当前学期兜底） */
	private String semester;

	/** 勾选章节ID */
	private List<String> chapterIds;

	/** 勾选章节名称 */
	private List<String> chapterNames;

	/** 组卷题量 */
	private Integer questionCount;

	/** 作答耗时（ms） */
	private Long durationMs;

	/** 客户端幂等令牌（重复提交返回既有记录） */
	private String clientToken;

	/**
	 * 单题作答。
	 */
	@Data
	public static class Item {

		/** 题目 ID（t_template.id） */
		private String questionId;

		/** 学生答案（{type:'option',optionId} / {type:'options',optionIds} / {type:'text',text}） */
		private Map<String, Object> answer;

	}

}
