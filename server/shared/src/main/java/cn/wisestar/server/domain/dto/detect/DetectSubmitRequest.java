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
