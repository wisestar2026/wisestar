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

}
