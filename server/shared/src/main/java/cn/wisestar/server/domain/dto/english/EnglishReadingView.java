package cn.wisestar.server.domain.dto.english;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 英语阅读理解视图（单元短文 + 选择题）。
 *
 * @author wisestar
 * @date 2026/10/18
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnglishReadingView {

	/** 文章序号（同一单元内从 1 开始） */
	private Integer articleNo;

	/** 内容来源说明 */
	private String source;

	/** 短文标题 */
	private String title;

	/** 英文短文 */
	private String passage;

	/** 短文中文参考译文 */
	private String passageZh;

	/** 阅读理解题目 */
	private List<Question> questions = new ArrayList<>();

	/** 单道阅读选择题 */
	@Data
	public static class Question {

		/** 题干 */
		private String stem;

		/** 选项文本（4 个） */
		private List<String> options = new ArrayList<>();

		/** 正确选项下标（0-3） */
		private Integer answerIndex;

		/** 中文解析 */
		private String explanation;

	}

}
