package cn.wisestar.server.flow.service.impl.taskHandler;

import cn.wisestar.server.flow.domain.dto.ApprovalTaskRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

/**
 * 驳回任务处理器（Bean 名 "rollbackTaskHandler"）。
 *
 * <p>职责：处理"驳回"审批操作：当前审批人主动将待办任务驳回到指定（或上一个）
 * 审批节点。只有当前待办任务的指派人或者候选者才能完成该操作（由 Flowable 任务
 * 权限天然保证）。</p>
 *
 * <p>流程流转（驳回分支）：审批人点击驳回到某节点 → FlowServiceImpl.approvalTask
 * 分发到本处理器 → 定位当前任务节点与目标节点 → 引擎状态变更 API 将当前活动移动到
 * 目标节点（重新生成目标节点的待办任务）→ 父类模板落库操作记录（approvalType=rollback）。</p>
 *
 * <p>内部逻辑细节：</p>
 * <ol>
 *   <li>若目标为发起节点（newActivityId = 项目 ID）→ 走 {@link #rollbackToStartEvent}
 *       挂起实例；</li>
 *   <li>未指定目标节点时默认驳回到上一节点（取节点树当前节点的 parent）；</li>
 *   <li>指定了目标节点则直接移动（moveActivityIdTo）。</li>
 * </ol>
 *
 * <p>被谁调用：FlowServiceImpl.approvalTask（按 Bean 名动态获取）。</p>
 *
 * <p>依赖什么：父类 {@link AbstractTaskHandler}（模板方法、操作记录落库、节点树构建）。</p>
 *
 * @author javahuang
 * @date 2022/1/7
 */
@Component("rollbackTaskHandler")
@Slf4j
public class RollbackTaskHandler extends AbstractTaskHandler {

	/**
	 * 执行"驳回"流转。
	 *
	 * @param request 审批请求（含 taskId、newActivityId 目标节点，可选）
	 * @return 恒为 true
	 */
	@Override
	public boolean innerProcess(ApprovalTaskRequest request) {
		// 定位当前待办任务所在节点
		Task task = getCurrentRunningTask(request.getTaskId());
		String currentActivityId = task.getTaskDefinitionKey();
		String newActivityId = request.getNewActivityId();
		// 目标为发起节点：挂起实例等待申请人完善
		if (rollbackToStartEvent(request)) {
			return true;
		}
		// 构建节点树，用于默认驳回上一节点
		TaskTreeNode taskTreeNode = getHistoricTree(task.getProcessInstanceId());
		if (StringUtils.isEmpty(request.getNewActivityId())) {
			// 如果未指定，默认驳回到上一个节点
			newActivityId = taskTreeNode.getParent().getActivityId();
			request.setActivityId(newActivityId);
		}
		if (StringUtils.isNotBlank(newActivityId)) {
			log.info("驳回任务 {} -> {}", currentActivityId, newActivityId);
			// 引擎状态变更：把当前活动移动到目标节点（重新生成目标节点的待办任务）
			runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId())
					.moveActivityIdTo(currentActivityId, newActivityId).changeState();
		}
		return true;
	}

}
