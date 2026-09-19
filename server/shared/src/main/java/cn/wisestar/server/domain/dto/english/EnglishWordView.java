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

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

	/** 小节 */
	private String section;

	/** 熟练度（用户单词本） */
	private Integer familiarity;

	/** 累计答对（认识）次数 */
	private Integer correctCount;

	/** 累计答错（不认识）次数 */
	private Integer wrongCount;

	/** 是否需加强（多次不认识） */
	private Boolean weak;

}
