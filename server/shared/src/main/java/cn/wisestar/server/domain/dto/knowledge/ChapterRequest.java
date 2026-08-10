package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

/**
 * 章节请求/查询（对应 t_chapter）。
 *
 * <p>GET /list 时以 GET 参数绑定（subjectId 筛选）；POST create/update 时以 body 提交。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
public class ChapterRequest {

	/**
	 * 章节ID（新增为空，更新必传）。
	 */
	private String id;

	/**
	 * 所属学科ID（t_subject.id）。
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
	 * 排序（数字越小越靠前）。
	 */
	private Integer sort;

}
