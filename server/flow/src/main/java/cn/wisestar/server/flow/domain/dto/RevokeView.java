package cn.wisestar.server.flow.domain.dto;

import lombok.Data;

/**
 * 可回退节点展示视图 DTO。
 *
 * <p>职责：向前端"驳回"对话框返回当前流程实例可回退的历史审批节点列表
 * （节点 ID + 节点名称），由 {@code RevertTaskHandler.getRevertNodes} 基于历史
 * 操作记录构建的节点树（TaskTreeNode 链路）计算得出，并经
 * {@code FlowServiceImpl.getRevertNodes} 翻译节点名称后返回。</p>
 *
 * <p>所属流程环节：审批处理环节（驳回操作的前置节点选择）。</p>
 *
 * <p>被谁调用：FlowServiceImpl.getRevertNodes（组装返回前端）。</p>
 *
 * <p>依赖什么：无（纯展示结构）。</p>
 *
 * @author javahuang
 * @date 2022/1/20
 */
@Data
public class RevokeView {

	/**
	 * 节点 id：可回退的审批节点 activityId
	 */
	private String activityId;

	/**
	 * 节点名称：该节点的中文显示名
	 */
	private String activityName;

}
