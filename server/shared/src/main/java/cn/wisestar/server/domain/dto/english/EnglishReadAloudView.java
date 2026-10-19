package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 英语课文跟读篇章视图（一篇课文 + 逐句中英文）。
 *
 * @author wisestar
 * @date 2026/10/19
 */
@Data
public class EnglishReadAloudView {

	/** 篇章 ID */
	private String id;

	/** 课文标题/板块名 */
	private String title;

	/** 教材版本与来源说明 */
	private String source;

	/** 单元内排序 */
	private Integer sort;

	/** 逐句内容 */
	private List<Line> lines = new ArrayList<>();

	/** 跟读句子 */
	@Data
	public static class Line {

		/** 英文原文 */
		private String en;

		/** 中文翻译 */
		private String zh;

	}

}
