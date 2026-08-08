package cn.wisestar.server.flow.domain.dto;

import lombok.Data;

import java.util.LinkedHashMap;

/**
 * 流程节点保存请求 DTO（流程设计器的单个节点配置）。
 *
 * <p>职责：前端保存流程设计时，{@link FlowEntryRequest#getNodes()} 中每个节点的
 * 配置载体。包含节点 ID（对应 BPMN XML 中的 activityId）、名称、类型、字段权限、
 * 节点设置、授权用户与条件表达式。由 MapStruct 转换器
 * （{@code FlowEntryElementModelMapper}）转换为持久化实体 {@code FlowEntryNode}
 * 后存入 t_flow_entry（nodes JSON 列）或 t_flow_entry_node 表。</p>
 *
 * <p>所属流程环节：流程设计环节（保存/部署时由前端提交）。</p>
 *
 * <p>被谁调用：FlowApi.saveFlow（HTTP 反序列化）、FlowServiceImpl.saveFlow、
 * FlowEntryElementModelMapper（MapStruct 转换）。</p>
 *
 * <p>依赖什么：{@link FlowNodeSetting}（节点行为设置）。</p>
 *
 * @author javahuang
 * @date 2022/1/6
 */
@Data
public class FlowEntryNodeRequest {

	/** 节点 ID：与 BPMN XML 中该节点的 activityId 保持一致，用于引擎与配置表关联 */
	private String id;

	/**
	 * 节点名称：审批节点显示名称（如"部门主管审批"），展示在审批阶段与审核记录中
	 */
	private String name;

	/**
	 * 项目id：所属问卷/项目 ID（即流程定义 key）
	 */
	private String projectId;

	/**
	 * 流程节点类型：任务类型（发起/用户任务/抄送等），见 {@link cn.wisestar.server.flow.constant.FlowTaskType}
	 */
	private Integer taskType;

	/**
	 * 字段权限：题目 ID → 权限值（0 隐藏 / 1 只读 / 2 可编辑），
	 * 见 {@link cn.wisestar.server.flow.constant.FieldPermissionType}
	 */
	private LinkedHashMap<String, Integer> fieldPermission;

	/**
	 * 流程设置：该节点的行为配置（是否可撤回、是否可见流程日志、审批方式等）
	 */
	private FlowNodeSetting setting;

	/**
	 * 授权用户：该节点的审批人集合，元素格式支持 U:用户ID（普通用户）、
	 * R:角色ID / P:岗位ID（按组展开）
	 */
	private String[] identity;

	/**
	 * 表达式：节点流转的条件表达式（预留字段，当前未参与条件判断逻辑）
	 */
	private String expression;

}
