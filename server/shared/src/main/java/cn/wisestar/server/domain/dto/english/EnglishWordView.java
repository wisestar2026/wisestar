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

	/** 掌握层级（四会/三会） */
	private String level;

	/** 熟练度（用户单词本） */
	private Integer familiarity;

	/** 累计答对（认识）次数 */
	private Integer correctCount;

	/** 累计答错（不认识）次数 */
	private Integer wrongCount;

	/** 是否需加强（多次不认识） */
	private Boolean weak;

	/** 练习首答总次数（仅练习/巩固的首答） */
	private Integer quizTotal;

	/** 练习首答答对次数 */
	private Integer quizFirstCorrect;

	/** 练习首答正确率（0~100） */
	private Integer firstCorrectRate;

	/** 掌握度等级：0 未掌握 / 1 待巩固 / 2 熟悉 / 3 掌握 */
	private Integer masteryLevel;

	/** 掌握度标签：未掌握 / 待巩固 / 熟悉 / 掌握 */
	private String masteryLabel;

	/** 下次复习时间（为空表示尚未进入复习队列） */
	private java.util.Date nextReviewTime;

}
