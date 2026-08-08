package cn.wisestar.server.flow.service.impl.taskHandler;

import cn.wisestar.server.flow.domain.dto.ApprovalTaskRequest;
import org.springframework.stereotype.Component;

/**
 * 拒绝任务处理器（Bean 名 "refuseTaskHandler"）。
 *
 * <p>职责：处理"拒绝"审批操作。拒绝之后直接结束流程：调用 Flowable RuntimeService
 * 删除整个流程实例（deleteProcessInstance，删除原因记录审批意见），引擎触发
 * PROCESS_CANCELLED 事件，由 {@link ProcessCancelledListener} 将 t_flow_instance
 * 状态同步为"已拒绝"。</p>
 *
 * <p>流程流转（拒绝分支）：审批人点击拒绝 → FlowServiceImpl.approvalTask 分发到本
 * 处理器 → innerProcess 删除流程实例（流程到此终止）→ 父类模板落库操作记录
 * （approvalType=refuse）。</p>
 *
 * <p>被谁调用：FlowServiceImpl.approvalTask（按 Bean 名动态获取）。</p>
 *
 * <p>依赖什么：父类 {@link AbstractTaskHandler}（模板方法、操作记录落库）、
 * Flowable RuntimeService（删除实例）。</p>
 *
 * @author javahuang
 * @date 2022/1/7
 */
@Component("refuseTaskHandler")
public class RefuseTaskHandler extends AbstractTaskHandler {

	/**
	 * 执行"拒绝"流转：删除整个流程实例。
	 *
	 * <p>删除原因传入审批意见 comment，供引擎历史与后续审计查看。</p>
	 *
	 * @param request 审批请求（含 processInstanceId、comment 审批意见）
	 * @return 恒为 true（删除成功即继续落库操作记录）
	 */
	@Override
	public boolean innerProcess(ApprovalTaskRequest request) {
		runtimeService.deleteProcessInstance(request.getProcessInstanceId(), request.getComment());
		return true;
	}

}
