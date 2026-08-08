package cn.wisestar.server.flow.domain.dto;

import lombok.Data;

/**
 * 加载问卷 schema（表单结构）的查询参数 DTO。
 *
 * <p>职责：前端调用 <code>GET /workflow/loadSchema</code> 时的查询参数，用于定位
 * 要加载的问卷结构以及按哪个审批节点的字段权限过滤。{@code FlowServiceImpl
 * .loadSchemaByPermission} 据此加载项目问卷 schema 并应用节点字段权限。</p>
 *
 * <p>所属流程环节：审批处理/表单查看环节（审批人打开表单前的权限过滤）。</p>
 *
 * <p>被谁调用：FlowApi.loadSchema（HTTP 参数绑定）、FlowServiceImpl.loadSchemaByPermission。</p>
 *
 * <p>依赖什么：无（纯查询参数）。</p>
 *
 * @author javahuang
 * @date 2022/1/5
 */
@Data
public class SchemaQuery {

	/** 项目 ID：要加载的问卷/项目 ID（即流程定义 key） */
	private String projectId;

	/** 任务定义 key：当前审批节点的 activityId，用于获取该节点的字段权限并过滤 schema */
	private String taskDefKey;

}
