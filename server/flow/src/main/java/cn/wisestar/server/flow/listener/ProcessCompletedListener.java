package cn.wisestar.server.flow.listener;

import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.flow.constant.FlowInstanceStatus;
import cn.wisestar.server.flow.domain.model.FlowInstance;
import cn.wisestar.server.flow.service.FlowInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.delegate.event.impl.FlowableProcessEventImpl;

/**
 * 流程实例完成事件监听器（PROCESS_COMPLETED）。
 *
 * <p>职责：当流程走完最后一个节点正常结束时（最后节点同意后 Flowable 触发
 * PROCESS_COMPLETED 事件），同步更新 t_flow_instance 表：把实例状态置为
 * "已结束"、审批阶段置为"已结束"。这是流程"正常到终点"的终止态。</p>
 *
 * <p>所属流程环节：审批流转环节的"同意-流程结束"分支。
 * （监听器注册见 {@link cn.wisestar.server.flow.config.WorkflowConfig}。）</p>
 *
 * <p>被谁调用：Flowable 引擎事件分发器（全局事件监听器）。</p>
 *
 * <p>依赖什么：{@link ContextHelper}（获取 Bean）、{@link FlowInstanceService}（更新实例）。</p>
 *
 * @author javahuang
 * @date 2022/1/6
 */
@Slf4j
public class ProcessCompletedListener implements FlowableEventListener {

	/**
	 * 事件回调：将流程实例状态同步为"已结束"。
	 *
	 * <p>内部逻辑：从事件中取出流程实例 ID，组装仅含 id、status、approvalStage
	 * 三个字段的 FlowInstance 执行局部更新，审批阶段显示为"已结束"。</p>
	 *
	 * @param event 流程完成事件（含已结束的流程实例 ID）
	 */
	@Override
	public void onEvent(FlowableEvent event) {
		FlowableProcessEventImpl entityEvent = (FlowableProcessEventImpl) event;
		FlowInstanceService flowInstanceService = ContextHelper.getBean(FlowInstanceService.class);
		FlowInstance instance = new FlowInstance();
		instance.setStatus(FlowInstanceStatus.FINISHED);
		instance.setApprovalStage(FlowInstanceStatus.getDictStatus(FlowInstanceStatus.FINISHED));
		instance.setId(entityEvent.getProcessInstanceId());
		flowInstanceService.updateById(instance);
	}

	/** 监听器内部异常是否向外抛出：false 表示事件处理失败不影响引擎主流程 */
	@Override
	public boolean isFailOnException() {
		return false;
	}

	/** 是否在事务生命周期事件时触发：false 表示仅在业务事件发生时触发 */
	@Override
	public boolean isFireOnTransactionLifecycleEvent() {
		return false;
	}

	/** 指定在事务的哪个阶段触发：null 表示不限制 */
	@Override
	public String getOnTransaction() {
		return null;
	}

}
