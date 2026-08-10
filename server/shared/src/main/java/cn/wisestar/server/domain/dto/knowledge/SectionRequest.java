package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

/**
 * 小节请求/查询（对应 t_section）。
 *
 * <p>content/practice 为 JSON 字符串（内容设置与练习设置），
 * 由前端序列化提交、前端解析展示，后端仅做透传存储。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
public class SectionRequest {

	/**
	 * 小节ID（新增为空，更新必传）。
	 */
	private String id;

	/**
	 * 所属章节ID（t_chapter.id）。
	 */
	private String chapterId;

	/**
	 * 小节名称。
	 */
	private String name;

	/**
	 * 排序（数字越小越靠前）。
	 */
	private Integer sort;

	/**
	 * 小节内容设置 JSON：{"objective":"学习目标","overview":"内容概述","points":["要点1"]}。
	 */
	private String content;

	/**
	 * 小节练习设置 JSON：{"questionCount":10,"difficulty":"基础","types":["Radio"]}。
	 */
	private String practice;

}
