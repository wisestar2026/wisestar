package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 练习提交判分结果。
 *
 * <p>返回总分/答对数与逐题对错（含标准答案，供学员端即时反馈）。</p>
 *
 * @author wisestar
 * @date 2026/8/20
 */
@Data
public class PracticeResultView {

	/** 得分 */
	private double score;

	/** 总分 */
	private double totalScore;

	/** 答对题数 */
	private int correctCount;

	/** 总题数 */
	private int total;

	/** 本次发放学习币 */
	private int coins;

	/** 本次发放学海积分 */
	private int points;

	/** 头衔是否晋升 */
	private boolean titleUpgraded;

	/** 当前头衔名称 */
	private String titleName;

	/** 学习币是否因单科上限被裁剪 */
	private boolean coinsCapped;

	/** 奖励提示信息（上限/防刷等） */
	private String message;

	/** 小节通关：本次是否通关（仅 trial 模式填充） */
	private Boolean passed;

	/** 小节通关：本次正确率（百分比） */
	private Integer rate;

	/** 小节通关：本次星级 0-5 */
	private Integer stars;

	/** 小节通关：历史最佳正确率（百分比） */
	private Integer bestRate;

	/** 小节通关：历史最佳星级 0-5 */
	private Integer bestStars;

	/** 小节通关：本次是否首次通关 */
	private Boolean firstPass;

	/** 小节通关：通关阈值（百分比） */
	private Integer passRate;

	/** 小节通关：是否已解锁下一小节 */
	private Boolean unlockedNext;

	/** 逐题结果 */
	private List<PracticeResultItem> items = new ArrayList<>();

	@Data
	public static class PracticeResultItem {

		/** 题目ID */
		private String questionId;

		/** 练习明细ID（t_practice_detail.id，错误归因用） */
		private String detailId;

		/** 1 对 / 0 错 / null 未判 */
		private Integer correct;

		/** 标准答案（判分后反馈） */
		private String correctAnswer;

		public PracticeResultItem() {
		}

		public PracticeResultItem(String questionId, Integer correct, String correctAnswer, String detailId) {
			this.questionId = questionId;
			this.correct = correct;
			this.correctAnswer = correctAnswer;
			this.detailId = detailId;
		}
	}
}
