package cn.wisestar.server.impl;

import java.util.Calendar;
import java.util.Date;

/**
 * 英语复习调度器（单词与句子共用）。
 *
 * <p>熟练度规则：答对 +1（上限 4），答错 -1（下限 0）。
 * 下次复习间隔沿用艾宾浩斯曲线：
 * 0→5 分钟、1→30 分钟、2→12 小时、3→1 天、4→2 天、5+→4 天。</p>
 *
 * @author wisestar
 * @date 2026/9/15
 */
public final class EnglishReviewScheduler {

	private EnglishReviewScheduler() {
	}

	/**
	 * 根据当前熟练度与作答结果计算新的熟练度。
	 *
	 * @param current 当前熟练度
	 * @param correct 是否答对
	 * @return 新熟练度（0~4）
	 */
	public static int adjustFamiliarity(int current, boolean correct) {
		int familiarity = Math.max(current, 0);
		if (correct) {
			return Math.min(familiarity + 1, 4);
		}
		return Math.max(familiarity - 1, 0);
	}

	/**
	 * 根据熟练度计算下次复习时间。
	 *
	 * @param familiarity 熟练度
	 * @return 下次复习时间
	 */
	public static Date nextReviewTime(int familiarity) {
		Calendar cal = Calendar.getInstance();
		int minutes;
		switch (familiarity) {
			case 0: minutes = 5; break;          // 首次复习：5 分钟
			case 1: minutes = 30; break;         // 第 2 次：30 分钟
			case 2: minutes = 720; break;        // 第 3 次：12 小时
			case 3: minutes = 1440; break;       // 第 4 次：1 天
			case 4: minutes = 2880; break;       // 第 5 次：2 天
			default: minutes = 5760; break;      // 第 6 次+：4 天
		}
		cal.add(Calendar.MINUTE, minutes);
		return cal.getTime();
	}

}
