package cn.wisestar.server.flow.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 流程设计保存请求 DTO。
 *
 * <p>职责：前端调用 <code>POST /workflow/saveFlow</code> 保存流程设计时提交的请求体，
 * 包含流程 BPMN XML、项目 ID 与节点配置列表。后端 {@code FlowServiceImpl.saveFlow}
 * 将其保存到 t_flow_entry 表（bpmn_xml 与 nodes JSON 列）。</p>
 *
 * <p>所属流程环节：流程设计环节（草稿保存，不触发引擎部署）。</p>
 *
 * <p>被谁调用：FlowApi.saveFlow（HTTP 反序列化）、FlowServiceImpl.saveFlow。</p>
 *
 * <p>依赖什么：{@link FlowEntryNodeRequest}（节点配置列表）。</p>
 *
 * @author javahuang
 * @date 2022/1/5
 */
@Data
public class FlowEntryRequest {

	/** xml 的 processId：BPMN 流程定义 XML，由前端流程设计器生成，部署时解析为 BpmnModel */
	private String bpmnXml;

	/** 项目 ID：所属问卷/项目 ID（即流程定义 key） */
	private String projectId;

	/** 节点配置列表：流程设计器画布上的全部节点（审批人、字段权限、设置等） */
	private List<FlowEntryNodeRequest> nodes;

}
