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
