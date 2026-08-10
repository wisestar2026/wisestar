package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

/**
 * 知识点请求（对应 t_knowledge_point）。
 *
 * <p>content 为 JSON 字符串（讲解要点数组：{"points":["要点1"]}），
 * imageUrl 为知识点配图地址（复用 /api/file/create 上传返回的 previewUrl，可为空）。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
public class KnowledgePointRequest {

	/**
	 * 知识点ID（新增为空，更新必传）。
	 */
	private String id;

	/**
	 * 所属小节ID（t_section.id）。
	 */
	private String sectionId;

	/**
	 * 知识点名称。
	 */
	private String name;

	/**
	 * 排序（数字越小越靠前）。
	 */
	private Integer sort;

	/**
	 * 内容设置 JSON：{"points":["讲解要点1"]}。
	 */
	private String content;

	/**
	 * 知识点图片地址（FileView.previewUrl，可为空）。
	 */
	private String imageUrl;

}
