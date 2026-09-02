package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 英语单词视图 DTO。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@Data
public class EnglishWordView {

	private String id;

	/** 单词拼写 */
	private String spell;

	/** 音标 */
	private String phonetic;

	/** 释义 */
	private String meaning;

	/** 图片 URL */
	private String imageUrl;

	/** 音频 URL */
	private String audioUrl;

	/** 例句 */
	private String exampleSentence;

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 单元 */
	private String unit;

	/** 熟练度（用户单词本） */
	private Integer familiarity;

}
