package cn.wisestar.server.domain.dto.math;

import lombok.Data;

import java.util.List;

/**
 * 数学组卷配置元数据（后台管理端）。
 *
 * <p>一次性下发「年级 → 册别 → 章节 → 小节 → 知识点」整棵可选树，
 * 以及题型/难度可选项和题量统计，前端级联选择无需二次请求。</p>
 *
 * @author wisestar
 * @date 2026/10/26
 */
@Data
public class MathPaperMetaView {

	/** 数学题库题目总数 */
	private Integer totalQuestions;

	/** 难度选项（含题量） */
	private List<Option> difficulties;

	/** 题型选项（含题量） */
	private List<Option> questionTypes;

	/** 年级树 */
	private List<GradeNode> grades;

	/**
	 * 通用选项（难度/题型）。
	 */
	@Data
	public static class Option {

		private String value;

		private String label;

		private Integer count;

		public Option() {
		}

		public Option(String value, String label, Integer count) {
			this.value = value;
			this.label = label;
			this.count = count;
		}

	}

	/**
	 * 年级节点。
	 */
	@Data
	public static class GradeNode {

		private String grade;

		private List<BookNode> books;

	}

	/**
	 * 册别节点（上/下册）。
	 */
	@Data
	public static class BookNode {

		/** 学期原值：上 / 下 */
		private String term;

		/** 展示名：上册 / 下册 */
		private String label;

		private List<ChapterNode> chapters;

	}

	/**
	 * 章节节点。
	 */
	@Data
	public static class ChapterNode {

		private String id;

		private String name;

		/** 该章节可用题量 */
		private Integer questionCount;

		private List<SectionNode> sections;

	}

	/**
	 * 小节节点。
	 */
	@Data
	public static class SectionNode {

		private String id;

		private String name;

		/** normal=普通小节 / exam=章节测评 */
		private String type;

		/** 该小节可用题量 */
		private Integer questionCount;

		private List<KnowledgePointOption> knowledgePoints;

	}

	/**
	 * 知识点选项。
	 */
	@Data
	public static class KnowledgePointOption {

		private String id;

		private String name;

	}

}
