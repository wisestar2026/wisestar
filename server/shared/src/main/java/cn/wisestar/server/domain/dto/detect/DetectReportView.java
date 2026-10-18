package cn.wisestar.server.domain.dto.detect;

import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识点检测报告。
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Data
public class DetectReportView {

	/** 作答总题数 */
	private int total;

	/** 正确题数 */
	private int correct;

	/** 正确率（0-100 整数） */
	private int accuracy;

	/** 按单元统计 */
	private List<ChapterStat> chapterStats = new ArrayList<>();

	/** 薄弱知识点（按正确率升序；内容包阶梯卷按三态加权得分低于 55 收录） */
	private List<WeakPoint> weakPoints = new ArrayList<>();

	/** 掌握题数（行为三态） */
	private int mastered;

	/** 不稳定题数（行为三态） */
	private int unstable;

	/** 漏洞题数（行为三态） */
	private int flawed;

	/** 加权得分（掌握 100 / 不稳定 60 / 漏洞 0，0-100 整数） */
	private int masteryScore;

	/** 知识模块维度（英语：句子/单词/句式） */
	private List<DimensionStat> moduleStats = new ArrayList<>();

	/** 语言技能维度（词汇/语法/语用） */
	private List<DimensionStat> skillStats = new ArrayList<>();

	/** 认知层级维度（记忆/理解/应用） */
	private List<DimensionStat> cognitiveStats = new ArrayList<>();

	/** 题型维度 */
	private List<DimensionStat> typeStats = new ArrayList<>();

	/** 难度维度（D1~D5） */
	private List<DimensionStat> difficultyStats = new ArrayList<>();

	/** 逐题明细（含标准答案与解析） */
	private List<Detail> details = new ArrayList<>();

	/** 检测类型 PRE（学前/基线）/ STAGE（阶段） */
	private String detectType;

	/** 本次是否定格为成长基线 */
	private boolean baseline;

	/** 检测记录ID（未落库时为 null） */
	private String recordId;

	/** 学习趋势（按诊断时间升序；仅学习诊断报告返回） */
	private List<TrendPoint> trend = new ArrayList<>();

	/**
	 * 单元维度统计。
	 */
	@Data
	public static class ChapterStat {

		private String name;

		private int total;

		private int correct;

		private int accuracy;

	}

	/**
	 * 维度聚合项（语言技能/认知层级/题型/难度）。
	 */
	@Data
	public static class DimensionStat {

		/** 维度名称 */
		private String name;

		private int total;

		private int correct;

		/** 全对率（0-100 整数） */
		private int accuracy;

		/** 三态加权得分（0-100 整数） */
		private int masteryScore;

	}

	/**
	 * 薄弱知识点。
	 */
	@Data
	public static class WeakPoint {

		/** 知识点标签（题目 knowledge_point 标签） */
		private String name;

		/** 所属单元 */
		private String chapter;

		private int total;

		private int correct;

		private int wrong;

		/** 正确率（0-100 整数） */
		private int accuracy;

	}

	/**
	 * 学习趋势点。
	 */
	@Data
	public static class TrendPoint {

		/** 诊断日期（MM-dd） */
		private String date;

		/** 当次加权得分（0-100 整数） */
		private int masteryScore;

		/** 当次正确率（0-100 整数） */
		private int accuracy;

		/** 当次题量 */
		private int total;

	}

	/**
	 * 逐题作答明细。
	 */
	@Data
	public static class Detail {

		private String questionId;

		private String chapter;

		private String knowledgePoint;

		private String questionType;

		/** 1 正确 / 0 错误 / null 未判分 */
		private Integer correct;

		/** 行为三态：掌握 / 不稳定 / 漏洞（内容包阶梯卷生效，其余报告为 null） */
		private String mastery;

		/** 单题得分（掌握 100 / 不稳定 60 / 漏洞 0） */
		private Integer score;

		/** 原题（题干 + 选项，已剥离标准答案与解析，供解析页对照题目信息） */
		private StudentQuestionView question;

		/** 标准答案（选项标题或文本） */
		private List<String> correctAnswers;

		/** 学生答案可读文本 */
		private String studentAnswer;

		/** 答案解析 */
		private String analysis;

	}

}
