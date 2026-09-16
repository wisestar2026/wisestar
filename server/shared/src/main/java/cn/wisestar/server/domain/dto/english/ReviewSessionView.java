package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 英语智能复习条目视图 DTO（单词与句子混合队列）。
 *
 * @author wisestar
 * @date 2026/9/15
 */
@Data
public class ReviewSessionView {

	/** 类型：word / sentence */
	private String type;

	/** 词/句 ID */
	private String id;

	/** 题干（单词为拼写，句子为英文原文） */
	private String prompt;

	/** 答案释义（单词为释义，句子为中文翻译） */
	private String answer;

	/** 音标（仅单词） */
	private String phonetic;

	/** 例句（仅单词） */
	private String exampleSentence;

	/** 音频 URL */
	private String audioUrl;

	/** 图片 URL（仅单词） */
	private String imageUrl;

	/** 当前熟练度 */
	private Integer familiarity;

	/** 下次复习时间（用于复习队列排序） */
	private java.util.Date nextReviewTime;

}
