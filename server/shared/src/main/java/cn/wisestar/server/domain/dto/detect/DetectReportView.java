package cn.wisestar.server.domain.dto.detect;

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

	/** 薄弱知识点（按正确率升序，仅含有错题的项） */
	private List<WeakPoint> weakPoints = new ArrayList<>();

	/** 逐题明细（含标准答案与解析） */
	private List<Detail> details = new ArrayList<>();

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

		/** 标准答案（选项标题或文本） */
		private List<String> correctAnswers;

		/** 学生答案可读文本 */
		private String studentAnswer;

		/** 答案解析 */
		private String analysis;

	}

}
