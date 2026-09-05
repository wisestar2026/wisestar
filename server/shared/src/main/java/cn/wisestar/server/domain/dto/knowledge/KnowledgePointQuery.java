package cn.wisestar.server.domain.dto.knowledge;

import cn.wisestar.server.domain.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识点分页查询参数（对应 t_knowledge_point）。
 *
 * <p>GET /list 以 GET 参数绑定。三级下拉条件均可选：
 * 都不传 → 全量分页；只传 subjectId → 该学科全部知识点；
 * 再传 chapterId → 缩小到该章节；传全 sectionId → 该小节知识点。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class KnowledgePointQuery extends PageQuery {

	/**
	 * 学科ID（可选）。
	 */
	private String subjectId;

	/**
	 * 章节ID（可选）。
	 */
	private String chapterId;

	/**
	 * 小节ID（可选）。
	 */
	private String sectionId;

	/**
	 * 年级（可选等值过滤）。
	 */
	private String grade;

	/**
	 * 学期（可选等值过滤）。
	 */
	private String term;

}
