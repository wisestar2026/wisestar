package cn.wisestar.server.flow.domain.dto;

import lombok.Data;

import java.util.LinkedHashMap;

/**
 * 流程节点展示视图 DTO。
 *
 * <p>职责：向前端返回流程设计器中单个节点的完整配置（与保存请求
 * {@link FlowEntryNodeRequest} 结构基本对应，另含 flowId、flowType 展示字段）。
 * 由 MapStruct 转换器（{@code FlowEntryElementModelMapper}）从持久化实体
 * {@code FlowEntryNode} 转换而来，供流程设计器回显、审核记录展示节点权限等使用。</p>
 *
 * <p>所属流程环节：流程设计环节（读取回显）与审批展示环节（节点信息查询）。</p>
 *
 * <p>被谁调用：FlowServiceImpl.getFlowEntry（组装节点列表返回前端）。</p>
 *
 * <p>依赖什么：{@link FlowNodeSetting}（节点行为设置）。</p>
 *
 * @author javahuang
 * @date 2022/1/5
 */
@Data
public class FlowEntryNodeView {

	/** 节点 ID：与 BPMN XML 中的 activityId 一致 */
	private String id;

	/** 节点名称：审批节点显示名称 */
	private String name;

	/** 流程 ID：所属流程定义（t_flow_entry.id） */
	private String flowId;

	/** 项目 ID：所属问卷/项目 ID（流程定义 key） */
	private String projectId;

	/** send/mail/sms/http：节点动作类型（预留展示字段，标注节点要执行的动作） */
	private String flowType;

	/** 字段权限 0隐藏 1读 2写：题目 ID → 权限值映射 */
	private LinkedHashMap<String, Integer> fieldPermission;

	/** 节点行为设置：是否可撤回、流程日志可见性、审批方式等 */
	private FlowNodeSetting setting;

	/** 授权用户：节点审批人集合（U:/R:/P: 前缀格式） */
	private String[] identity;

	/** 条件表达式（预留） */
	private String expression;

}
