package cn.wisestar.server.flow.service.impl.taskHandler;

import cn.wisestar.server.flow.domain.dto.ApprovalTaskRequest;
import org.springframework.stereotype.Component;

/**
 * 同意任务处理器（Bean 名 "agreeTaskHandler"）。
 *
 * <p>职责：处理"同意"审批操作。内部逻辑最简单：直接调用 Flowable TaskService
 * 完成当前待办任务（complete），引擎会自动沿 BPMN 流转到下一节点；若当前是最后
 * 一个审批节点，流程将正常结束（触发 ProcessCompletedListener 将实例状态置为
 * "已结束"）。</p>
 *
 * <p>流程流转（同意分支）：审批人点击同意 → FlowServiceImpl.approvalTask 按 type
 * 分发到本处理器 → innerProcess 完成当前任务（引擎流转到下一节点或结束）→
 * 父类模板落库操作记录（approvalType=agree）并更新表单答案。</p>
 *
 * <p>被谁调用：FlowServiceImpl.approvalTask（按 Bean 名动态获取）。</p>
 *
 * <p>依赖什么：父类 {@link AbstractTaskHandler}（模板方法、操作记录落库）。</p>
 *
 * @author javahuang
 * @date 2022/1/7
 */
@Component("agreeTaskHandler")
public class AgreeTaskHandler extends AbstractTaskHandler {

	/**
	 * 执行"同意"流转：完成当前待办任务。
	 *
	 * @param request 审批请求（含 taskId 待完成的任务 ID）
	 * @return 恒为 true（完成成功即继续落库操作记录）
	 */
	@Override
	public boolean innerProcess(ApprovalTaskRequest request) {
		taskService.complete(request.getTaskId());
		return true;
	}

}
