package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.List;

/**
 * 英语小节三阶进度视图：单词记忆 / 单词练习 / 单词运用三个阶段在小节内的完成与通关情况。
 *
 * <p>统计粒度为小节，通关口径见 {@code EnglishMastery} 常量与需求文档 R5。</p>
 *
 * @author wisestar
 * @date 2026/11/05
 */
@Data
public class EnglishSectionProgressView {

	/** 小节名称（未分节时为「未分节」） */
	private String section;

	/** 小节内单词总数 */
	private Integer wordCount;

	/** 记忆阶段：已作答单词数 */
	private Integer memoryDone;

	/** 记忆阶段：答对单词数 */
	private Integer memoryCorrect;

	/** 记忆阶段正确率（百分比） */
	private Integer memoryRate;

	/** 记忆阶段是否达标 */
	private Boolean memoryPass;

	/** 练习阶段：已作答次数 */
	private Integer practiceDone;

	/** 练习阶段：答对次数 */
	private Integer practiceCorrect;

	/** 练习阶段正确率（百分比） */
	private Integer practiceRate;

	/** 练习阶段是否达标 */
	private Boolean practicePass;

	/** 运用阶段：已作答次数 */
	private Integer useDone;

	/** 运用阶段：答对次数 */
	private Integer useCorrect;

	/** 运用阶段正确率（百分比） */
	private Integer useRate;

	/** 运用阶段是否达标 */
	private Boolean usePass;

	/** 三态分布：熟练词数 */
	private Integer familiarCount;

	/** 三态分布：一般词数 */
	private Integer ordinaryCount;

	/** 三态分布：陌生词数 */
	private Integer unfamiliarCount;

	/** 小节是否三阶全部达标 */
	private Boolean sectionPass;
}
