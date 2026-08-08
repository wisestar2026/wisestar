package cn.wisestar.server.flow.service.impl.taskHandler;

import cn.wisestar.server.flow.domain.dto.ApprovalTaskRequest;
import cn.wisestar.server.flow.domain.model.FlowOperation;
import cn.wisestar.server.flow.exception.FlowableRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 撤回任务处理器（Bean 名 "revertTaskHandler"）。
 *
 * <p>职责：处理"撤回"审批操作：撤回当前用户提交的、但尚未被审批的待办任务。
 * 仅"已办任务的最新操作人"才能执行该操作（{@link #canRevert} 校验）。撤回后：</p>
 * <ul>
 *   <li>若目标是发起节点（newActivityId = 项目 ID）→ {@link #rollbackToStartEvent}
 *       挂起流程实例，等待申请人完善表单后重新提交（实例状态"申请人完善中"）；</li>
 *   <li>否则 → 使用 Flowable 状态变更 API 将当前活跃任务移动到指定历史节点。</li>
 * </ul>
 *
 * <p>流程流转（撤回分支）：申请人点击撤回 → FlowServiceImpl.approvalTask 分发到本
 * 处理器 → canRevert 校验 → 移动/挂起到目标节点 → 父类模板落库操作记录
 * （approvalType=revert）。</p>
 *
 * <p>被谁调用：FlowServiceImpl.approvalTask（按 Bean 名动态获取）；
 * {@link #getRevertNodes} 同时被 FlowServiceImpl.getRevertNodes 调用（可回退节点计算）。</p>
 *
 * <p>依赖什么：父类 {@link AbstractTaskHandler}（模板方法、操作记录落库、节点树构建）。</p>
 *
 * @author javahuang
 * @date 2022/1/7
 */
@Component("revertTaskHandler")
@Slf4j
public class RevertTaskHandler extends AbstractTaskHandler {

	/**
	 * 执行"撤回"流转。
	 *
	 * <p>内部逻辑：</p>
	 * <ol>
	 *   <li>校验当前用户是否为最近操作人（{@link #canRevert}），否则抛异常拒绝操作；</li>
	 *   <li>若请求目标为发起节点（newActivityId = 项目 ID）→ 走 {@link #rollbackToStartEvent}
	 *       挂起实例，结束；</li>
	 *   <li>否则（TODO：会签撤回未实现），取当前活跃任务，通过引擎状态变更 API 将当前
	 *       节点移动到请求指定的活动节点（request.getActivityId），完成撤回。</li>
	 * </ol>
	 *
	 * @param request 审批请求（含 taskId、processInstanceId、newActivityId / activityId）
	 * @return 恒为 true
	 */
	@Override
	public boolean innerProcess(ApprovalTaskRequest request) {
		if (!canRevert(request.getTaskId(), request.getProcessInstanceId())) {
			throw new FlowableRuntimeException("当前节点不能进行驳回操作");
		}
		// 回退到发起节点（挂起实例等待申请人完善）
		if (rollbackToStartEvent(request)) {
			return true;
		}
		// TODO: 会签撤回
		List<Task> tasks = getProcessInstanceActiveTaskList(request.getProcessInstanceId());
		runtimeService.createChangeActivityStateBuilder().processInstanceId(request.getProcessInstanceId())
				.moveActivityIdTo(tasks.get(0).getTaskDefinitionKey(), request.getActivityId()).changeState();
		return true;
	}

	/**
	 * 校验当前用户是否可撤回：只有最近一条的操作记录是自己才能进行驳回操作。
	 *
	 * <p>内部逻辑：查询实例的用户任务操作记录（倒序），若最新一条记录的 ID 与请求的
	 * taskId 一致（说明最新操作人就是当前用户），则允许撤回。注意此处 taskId 语义
	 * 复用为"最近操作记录 ID"。</p>
	 *
	 * @param taskId 当前操作记录的 id
	 * @param processInstanceId 流程实例 ID
	 * @return 是否允许撤回
	 */
	public boolean canRevert(String taskId, String processInstanceId) {
		List<FlowOperation> operationList = getOperations(processInstanceId);
		if (operationList.size() > 0 && taskId.equals(operationList.get(0).getId())) {
			return true;
		}
		return false;
	}

	/**
	 * 获取当前可以回退的节点列表。
	 *
	 * <p>内部逻辑：基于历史操作记录构建节点树（{@link AbstractTaskHandler#getHistoricTree}），
	 * 取当前节点的 parent 开始沿父链向上收集全部祖先节点（不能包含当前节点），即为
	 * 可回退的节点链路。</p>
	 *
	 * @param processInstanceId 流程实例 ID
	 * @return 可回退节点列表（不含当前节点）
	 */
	public List<TaskTreeNode> getRevertNodes(String processInstanceId) {
		// 不能包含当前节点
		TaskTreeNode lastNode = getHistoricTree(processInstanceId).getParent();
		List<TaskTreeNode> nodes = new ArrayList<>();
		while (true) {
			if (lastNode == null) {
				return nodes;
			}
			nodes.add(lastNode);
			lastNode = lastNode.getParent();
		}
	}

}
