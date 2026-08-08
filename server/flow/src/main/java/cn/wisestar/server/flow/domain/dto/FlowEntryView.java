package cn.wisestar.server.flow.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 流程定义展示视图 DTO。
 *
 * <p>职责：向前端流程设计器返回某个问卷绑定的完整流程定义（BPMN XML、图标、节点列表）。
 * 由 MapStruct 转换器（{@code FlowEntryModelMapper}）从实体 {@code FlowEntry} 转换而来；
 * 若问卷尚未配置流程，返回仅含 projectId 的空视图。</p>
 *
 * <p>所属流程环节：流程设计环节（读取回显）。</p>
 *
 * <p>被谁调用：FlowServiceImpl.getFlowEntry（组装返回前端）。</p>
 *
 * <p>依赖什么：{@link FlowEntryNodeView}（节点视图列表）。</p>
 *
 * @author javahuang
 * @date 2022/1/5
 */
@Data
public class FlowEntryView {

	/** 主键：流程定义记录 ID（t_flow_entry.id） */
	private String id;

	/** 项目 ID：所属问卷/项目 ID（即流程定义 key） */
	private String projectId;

	/** 流程 BPMN XML：流程设计器的原始定义内容 */
	private String bpmnXml;

	/** 流程图标：前端展示用图标（预留字段） */
	private String icon;

	/** 节点视图列表：流程设计器画布上的全部节点配置 */
	private List<FlowEntryNodeView> nodes;

}
