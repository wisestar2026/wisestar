package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.List;

/**
 * 单词复习概览视图 DTO：复习页卡片展示「应复习多少词、按熟练度怎么分布、涉及哪些单元」。
 *
 * @author wisestar
 * @date 2026/10/25
 */
@Data
public class EnglishWordReviewSummaryView {

	/** 应复习单词数：已到复习时间、按拼写去重后的总数（不受单次上限影响） */
	private int due;

	/** 陌生单词数：熟练度 0~1（未学习 / 生疏） */
	private int unfamiliar;

	/** 一般单词数：熟练度 2（熟悉） */
	private int ordinary;

	/** 熟练单词数：熟练度 3~4（熟练 / 精通） */
	private int familiar;

	/** 涉及单元数 */
	private int unitCount;

	/** 涉及的单元名（最多 4 个，按单元顺序） */
	private List<String> units;

	/** 单次复习上限（词） */
	private int batchSize;
}
