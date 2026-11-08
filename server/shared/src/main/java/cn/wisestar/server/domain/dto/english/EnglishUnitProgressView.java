package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.List;

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

	/** 已学习语法点数（熟练度 >= 1） */
	private Integer grammarFinished;

	/** 已掌握单词数（首答正确率 >= 80% 且熟练度 >= 3） */
	private Integer wordMastered;

	/** 待巩固单词数（有练习记录但未达熟悉门槛） */
	private Integer wordWeak;

	/** 本单元单词是否通关（掌握 >= 80% 且待巩固 <= 10%） */
	private Boolean wordPass;

	/** 单元综合熟练度（单词 + 句子 + 语法 familiarity 均值，0~100） */
	private Integer mastery;

	/** 待复习知识点数（与复习页「单元复习」Tab 同一口径：薄弱 / 做错 / 到期） */
	private Integer reviewDue;

	/** 单词小节通关率（已通关小节 / 小节总数，百分比） */
	private Integer wordPassRate;

	/** 句式通关率（已通关句子 / 句子总数，百分比） */
	private Integer sentencePassRate;

	/** 待复习单词数 */
	private Integer reviewDueWord;

	/** 待复习句子数 */
	private Integer reviewDueSentence;

	/** 待复习语法数 */
	private Integer reviewDueGrammar;

	/** 小节级三阶进度 */
	private List<EnglishSectionProgressView> sections;

}
