package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

/**
 * 章节视图（返回前端展示用，含小节数统计）。
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
public class ChapterView {

	private String id;

	/**
	 * 所属学科ID。
	 */
	private String subjectId;

	/**
	 * 章节名称。
	 */
	private String name;

	/**
	 * 图标（emoji）。
	 */
	private String icon;

	/**
	 * 排序。
	 */
	private Integer sort;

	/**
	 * 该章节下的小节总数（列表展示用）。
	 */
	private Long sectionCount;

	/**
	 * 该章节下已绑定的题库数（列表展示用，来自 t_chapter_repo）。
	 */
	private Long repoCount;

}
