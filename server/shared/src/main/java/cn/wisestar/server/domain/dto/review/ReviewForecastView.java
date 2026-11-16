package cn.wisestar.server.domain.dto.review;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 复习到期预测视图。
 *
 * @author wisestar
 * @date 2026/11/16
 */
@Data
public class ReviewForecastView {

	/** 调度项总数 */
	private int total;

	/** 今日到期数 */
	private int dueToday;

	/** 全体调度项平均可提取度 0~1 */
	private double averageRetrievability;

	/** 目标保持率 */
	private double targetRetention;

	/** 每日到期数 */
	private List<DayPoint> days = new ArrayList<>();

	/**
	 * 单日到期数。
	 */
	@Data
	public static class DayPoint {

		/** 日期 yyyy-MM-dd */
		private String date;

		/** 当日到期数 */
		private int due;

	}

}
