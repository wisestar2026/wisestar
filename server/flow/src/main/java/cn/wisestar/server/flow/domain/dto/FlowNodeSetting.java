package cn.wisestar.server.flow.domain.dto;

import lombok.Data;

/**
 * 流程节点行为设置 DTO。
 *
 * <p>职责：描述某个审批节点"允许/禁止做什么"的行为配置，是节点配置
 * （{@link FlowEntryNodeRequest} / {@code FlowEntryNode}）的组成部分，
 * 以 JSON 形式存储于 t_flow_entry_node.setting 列。前端流程设计器针对每个节点
 * 配置这些开关项，审批详情页按配置展示操作按钮与流程日志。</p>
 *
 * <p>所属流程环节：贯穿流程设计（配置）与审批处理/详情展示（读取）环节。</p>
 *
 * <p>被谁调用：流程设计器（配置）、审批详情页（按配置渲染操作项）。当前引擎流转
 * 逻辑中部分字段为预留配置，尚未参与运行时判断。</p>
 *
 * <p>依赖什么：无（纯数据结构）。</p>
 *
 * @author javahuang
 * @date 2021/11/9
 */
@Data
public class FlowNodeSetting {

	/** 是否需要密码：审批人操作时是否需要输入密码（预留配置项） */
	private Boolean passwordRequired;

	/** 密码：与 passwordRequired 配套的密码值（预留） */
	private String password;

	/** 允许撤回：是否允许发起人对本节点的任务执行撤回操作（预留） */
	private Boolean allowWithdraw;

	/** 是否可见流程日志：当前节点用户能否查看完整流程日志（预留） */
	private Boolean flowLogVisible;

	/** 允许结束流程：是否允许当前审批人直接结束流程（预留） */
	private Boolean allowFinishFlow;

	/** 留言板：是否展示留言板（预留） */
	private Boolean messageBoardVisible;

	/** 代办转交：是否允许转交代办任务（预留） */
	private String allowAssignee;

	/** 转交人：转交的目标用户（预留） */
	private String assigneeTo;

	/** 流程回退：是否允许当前审批人将任务驳回到前序节点（预留） */
	private Boolean allowGoBack;

	/** 1:常规审批 2:逐级审批：审批人处理方式（预留） */
	private Integer isSequential;

	/** 1:或签 2:会签：多审批人时是任一通过（或签）还是全部通过（会签）（预留） */
	private Integer approveType;

}
