package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

/**
 * 小节视图（返回前端展示用，含内容/练习设置状态与知识点数统计）。
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
public class SectionView {

	private String id;

	/**
	 * 所属章节ID。
	 */
	private String chapterId;

	/**
	 * 小节名称。
	 */
	private String name;

	/**
	 * 排序。
	 */
	private Integer sort;

	/**
	 * 年级（如 一年级）。
	 */
	private String grade;

	/**
	 * 学期（上/下）。
	 */
	private String term;

	/**
	 * 重点程度：core=核心 / key=重点 / normal=一般；为空表示未标注。
	 */
	private String importance;

	/**
	 * 内容设置 JSON 原文（可能为空，前端解析展示）。
	 */
	private String content;

	/**
	 * 练习设置 JSON 原文（可能为空，前端解析展示）。
	 */
	private String practice;

	/**
	 * 该小节下的知识点总数（列表展示用）。
	 */
	private Long knowledgePointCount;

	/**
	 * 该小节已绑定的题库数（t_section_repo 统计，题库来自题库管理）。
	 */
	private Long repoCount;

	/** 学习完成度（0-100，学员维度：相关练习最高正确率） */
	private Integer progress;

	/** 学员是否已通关该小节（t_section_pass.passed） */
	private Boolean passed;

	/** 学员历史最佳星级 0-5 */
	private Integer stars;

	/** 学员历史最佳正确率（百分比） */
	private Integer bestRate;

	/** 是否锁定（上一小节开启解锁开关且未通关时为 true） */
	private Boolean locked;

	/** 该小节可练习题目总数（绑定题库 + 知识点绑定/标签匹配，去重） */
	private Integer questionCount;

	/** 学员在该小节已作答的不同题目数 */
	private Integer answeredCount;

	/** 学员在该小节答对的不同题目数 */
	private Integer correctCount;

}
