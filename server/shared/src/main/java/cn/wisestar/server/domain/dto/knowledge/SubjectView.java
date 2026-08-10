package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

/**
 * 学科视图（返回前端展示用，含章节数统计）。
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
public class SubjectView {

	private String id;

	/**
	 * 学科名称。
	 */
	private String name;

	/**
	 * 学科编码。
	 */
	private String code;

	/**
	 * 图标（emoji）。
	 */
	private String icon;

	/**
	 * 主题色。
	 */
	private String themeColor;

	/**
	 * 排序。
	 */
	private Integer sort;

	/**
	 * 该学科下的章节总数（列表展示用）。
	 */
	private Long chapterCount;

}
