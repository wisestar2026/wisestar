package cn.wisestar.server.domain.dto.review;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 今日复习计划视图。
 *
 * @author wisestar
 * @date 2026/11/16
 */
@Data
public class ReviewPlanView {

	/** 到期总数（含逾期） */
	private int due;

	/** 逾期总数 */
	private int overdue;

	/** 薄弱总数 */
	private int weak;

	/** 新项总数 */
	private int fresh;

	/** 预计耗时（分钟） */
	private int estimatedMinutes;

	/** 目标保持率 */
	private double targetRetention;

	/** 分模块统计 */
	private List<ModuleStat> modules = new ArrayList<>();

	/**
	 * 单模块统计。
	 */
	@Data
	public static class ModuleStat {

		/** 模块 */
		private String module;

		/** 到期数 */
		private int due;

		/** 逾期数 */
		private int overdue;

		/** 薄弱数 */
		private int weak;

		/** 新项数 */
		private int fresh;

	}

}
