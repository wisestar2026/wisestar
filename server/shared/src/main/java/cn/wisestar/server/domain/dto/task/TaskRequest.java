package cn.wisestar.server.domain.dto.task;

import lombok.Data;

/**
 * 今日任务请求（后台老师布置）。
 *
 * @author wisestar
 * @date 2026/8/23
 */
@Data
public class TaskRequest {

	private String id;

	/** 任务名称（必填） */
	private String name;

	/** 任务描述 */
	private String description;

	/** 任务日期（YYYY-MM-DD，必填） */
	private String taskDate;

	/** 内容类型 practice练习 / knowledge_point知识点 */
	private String contentType;

	/** 关联内容ID（练习=t_repo.id / 知识点=t_knowledge_point.id） */
	private String contentId;

	/** 状态 1发布 0停用 */
	private Integer status;

	/** 排序 */
	private Integer sort;

}
