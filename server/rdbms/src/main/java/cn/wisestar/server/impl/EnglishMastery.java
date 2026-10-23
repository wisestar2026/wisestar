package cn.wisestar.server.impl;

import cn.wisestar.server.domain.model.EnglishWordBook;

/**
 * 英语单词掌握度派生规则。
 *
 * <p>掌握度不落库，读取时由单词本的三类证据计算：</p>
 * <ul>
 *   <li>曝光：familiarity >= 1（学习环节点过「认识」）</li>
 *   <li>练习：练习/巩固的首答正确率（重做与学习环节点击不计入）</li>
 *   <li>运用：场景对话命中该词（当前版本未归因，见开发维护日志）</li>
 * </ul>
 *
 * <p>状态：0 未掌握（无任何记录）、1 待巩固、2 熟悉、3 掌握。</p>
 *
 * @author wisestar
 * @date 2026/10/23
 */
public final class EnglishMastery {

	/** 未掌握：单词本无记录 */
	public static final int LEVEL_NONE = 0;

	/** 待巩固：有练习记录但未达熟悉门槛 */
	public static final int LEVEL_WEAK = 1;

	/** 熟悉：首答正确率 >= 60% 且熟练度 >= 2 */
	public static final int LEVEL_FAMILIAR = 2;

	/** 掌握：首答正确率 >= 80% 且熟练度 >= 3 */
	public static final int LEVEL_MASTERED = 3;

	/** 熟悉门槛：首答正确率（百分比） */
	public static final int FAMILIAR_RATE = 60;

	/** 掌握门槛：首答正确率（百分比） */
	public static final int MASTERED_RATE = 80;

	/** 单词通关门槛：掌握词占单元词数比例（百分比） */
	public static final int UNIT_PASS_RATE = 80;

	/** 单词通关门槛：待巩固词占单元词数比例（百分比，上限） */
	public static final int UNIT_WEAK_RATE = 10;

	private EnglishMastery() {
	}

	/**
	 * 计算单词掌握度等级。
	 *
	 * @param book 单词本记录，可为空
	 * @return 0 未掌握 / 1 待巩固 / 2 熟悉 / 3 掌握
	 */
	public static int level(EnglishWordBook book) {
		if (book == null) {
			return LEVEL_NONE;
		}
		int familiarity = intValue(book.getFamiliarity());
		int quizTotal = intValue(book.getQuizTotal());
		if (quizTotal <= 0) {
			// 兼容上线前的历史数据：只有学习/复习点击、没有首答统计时，视为待巩固
			boolean touched = familiarity > 0
					|| intValue(book.getCorrectCount()) + intValue(book.getWrongCount()) > 0;
			return touched ? LEVEL_WEAK : LEVEL_NONE;
		}
		int rate = firstCorrectRate(book);
		if (rate >= MASTERED_RATE && familiarity >= 3) {
			return LEVEL_MASTERED;
		}
		if (rate >= FAMILIAR_RATE && familiarity >= 2) {
			return LEVEL_FAMILIAR;
		}
		return LEVEL_WEAK;
	}

	/**
	 * 掌握度中文标签。
	 *
	 * @param level 掌握度等级
	 * @return 标签
	 */
	public static String label(int level) {
		switch (level) {
			case LEVEL_MASTERED: return "掌握";
			case LEVEL_FAMILIAR: return "熟悉";
			case LEVEL_WEAK: return "待巩固";
			default: return "未掌握";
		}
	}

	/**
	 * 练习首答正确率（百分比，无首答记录返回 0）。
	 *
	 * @param book 单词本记录
	 * @return 0~100
	 */
	public static int firstCorrectRate(EnglishWordBook book) {
		if (book == null) {
			return 0;
		}
		int total = intValue(book.getQuizTotal());
		if (total <= 0) {
			return 0;
		}
		int correct = Math.min(intValue(book.getQuizFirstCorrect()), total);
		return (int) Math.round(correct * 100.0 / total);
	}

	/**
	 * 空值安全取整。
	 *
	 * @param value 可空值
	 * @return 非空整数
	 */
	public static int intValue(Integer value) {
		return value == null ? 0 : value;
	}
}
