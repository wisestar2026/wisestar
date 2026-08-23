package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学员学习统计视图（首页学习统计真实化，基于真实练习记录聚合）。
 *
 * @author wisestar
 * @date 2026/8/22
 */
@Data
public class StudentStatsView {

	/** 累计学海积分（练习得分合计） */
	private double totalPoints;

	/** 累计练习次数 */
	private int practiceCount;

	/** 累计答题数 */
	private int totalQuestions;

	/** 累计答对数 */
	private int totalCorrect;

	/** 累计正确率（0-100） */
	private int accuracy;

	/** 今日统计 */
	private TodayStats today = new TodayStats();

	/** 分科学币（按练习所属学科聚合答对数，每题 1 币） */
	private List<SubjectCoins> coinsBySubject = new ArrayList<>();

	@Data
	public static class TodayStats {

		/** 今日练习次数 */
		private int practiceCount;

		/** 今日答题数 */
		private int questionCount;

		/** 今日答对数 */
		private int correctCount;

		/** 今日正确率（0-100） */
		private int accuracy;

		/** 今日练习时长（分钟） */
		private long minutes;

		/** 今日获得积分（练习得分） */
		private double points;

		/** 今日获得学币（答对数） */
		private int coins;

	}

	@Data
	public static class SubjectCoins {

		private String subjectName;

		private int coins;

		public SubjectCoins() {
		}

		public SubjectCoins(String subjectName, int coins) {
			this.subjectName = subjectName;
			this.coins = coins;
		}
	}
}
