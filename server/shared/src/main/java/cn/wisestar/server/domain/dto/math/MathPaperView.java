package cn.wisestar.server.domain.dto.math;

import cn.wisestar.server.domain.dto.SurveySchema;
import lombok.Data;

import java.util.List;

/**
 * 数学试卷视图（后台管理端，含标准答案与解析，供教师打印）。
 *
 * @author wisestar
 * @date 2026/10/26
 */
@Data
public class MathPaperView {

	/** 试卷标题 */
	private String title;

	/** 组卷方式：chapter / student */
	private String mode;

	/** 学员 ID（按学员组卷时有值） */
	private String studentId;

	/** 学号（按学员组卷时有值） */
	private String studentNo;

	/** 学员姓名（按学员组卷时有值） */
	private String studentName;

	/** 实际题量 */
	private Integer questionCount;

	/** 试卷总分 */
	private Double totalScore;

	/** 生成时间（yyyy-MM-dd HH:mm） */
	private String generatedAt;

	/** 组卷提示（范围、补充说明等） */
	private List<String> warnings;

	/** 卷内该学员曾做错的题目数（按学员组卷时有值） */
	private Integer wrongQuestionCount;

	/** 题目列表（按题型分组排序） */
	private List<Question> questions;

	/** 题型分布 */
	private List<StatItem> typeStats;

	/** 难度分布 */
	private List<StatItem> difficultyStats;

	/** 章节分布 */
	private List<StatItem> chapterStats;

	/** 知识点分布 */
	private List<StatItem> knowledgePointStats;

	/**
	 * 试卷中的单道题目。
	 */
	@Data
	public static class Question {

		/** 题目 ID */
		private String id;

		/** 题干 */
		private String name;

		/** 题型（SurveySchema.QuestionType 名称） */
		private String questionType;

		/** 题型中文名 */
		private String questionTypeLabel;

		/** 难度：easy/medium/hard */
		private String difficulty;

		/** 难度中文名 */
		private String difficultyLabel;

		/** 分值 */
		private Double score;

		/** 章节名 */
		private String chapter;

		/** 小节名 */
		private String section;

		/** 知识点名称列表 */
		private List<String> knowledgePoints;

		/** 题目内容（含选项、配图、分值等；标准答案单独通过 answerText 下发，不内嵌渲染） */
		private SurveySchema schema;

		/** 标准答案展示文本（如「B」「A、C」「12；25」） */
		private String answerText;

		/** 解析 */
		private String analysis;

		/** 该学员做错次数（按学员组卷时有值） */
		private Integer wrongCount;

	}

	/**
	 * 统计项。
	 */
	@Data
	public static class StatItem {

		private String name;

		private Integer count;

		public StatItem() {
		}

		public StatItem(String name, Integer count) {
			this.name = name;
			this.count = count;
		}

	}

}
