package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 英语单元进度视图 DTO（学员端学习中心）。
 *
 * @author wisestar
 * @date 2026/9/15
 */
@Data
public class EnglishUnitProgressView {

	/** 单元名 */
	private String unit;

	/** 排序 */
	private Integer sort;

	/** 单元单词总数 */
	private Integer wordCount;

	/** 单元句子总数 */
	private Integer sentenceCount;

	/** 单元重点语法总数 */
	private Integer grammarCount;

	/** 已学习单词数（熟练度 >= 1） */
	private Integer wordFinished;

	/** 已学习句子数（熟练度 >= 1） */
	private Integer sentenceFinished;

	/** 单元综合熟练度（单词 + 句子 familiarity 均值，0~100） */
	private Integer mastery;

	/** 待复习数（单词本 + 句子本中已到期记录） */
	private Integer reviewDue;

}
