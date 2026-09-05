package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

/**
 * 知识点视图（返回前端展示用）。
 *
 * <p>含三级归属名称（学科/章节/小节，列表直接展示）与绑定题目数统计；
 * 题目从题目库（t_template）选择绑定，不能在此新增。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
public class KnowledgePointView {

	private String id;

	/**
	 * 所属小节ID。
	 */
	private String sectionId;

	/**
	 * 知识点名称。
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
	 * 内容设置 JSON 原文（可能为空）。
	 */
	private String content;

	/**
	 * 知识点图片地址（可能为空）。
	 */
	private String imageUrl;

	/**
	 * 学科名称（列表展示）。
	 */
	private String subjectName;

	/**
	 * 章节名称（列表展示）。
	 */
	private String chapterName;

	/**
	 * 小节名称（列表展示）。
	 */
	private String sectionName;

	/**
	 * 已绑定题目数（t_knowledge_point_question 统计）。
	 */
	private Long questionCount;

}
