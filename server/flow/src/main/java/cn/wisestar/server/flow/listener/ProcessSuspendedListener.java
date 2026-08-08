package cn.wisestar.server.flow.listener;

import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.flow.constant.FlowInstanceStatus;
import cn.wisestar.server.flow.domain.model.FlowInstance;
import cn.wisestar.server.flow.service.FlowInstanceService;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.delegate.event.impl.FlowableEntityEventImpl;

/**
 * 流程实例挂起/激活事件监听器（ENTITY_SUSPENDED / ENTITY_ACTIVATED）。
 *
 * <p>职责：当流程实例被挂起或激活时，同步更新 t_flow_instance 表状态为"申请人
 * 完善中"。业务场景：发起人"撤回"任务时（RevertTaskHandler.rollbackToStartEvent
 * 调用 suspendProcessInstanceById），实例被挂起等待申请人完善表单；此时"我发起的"
 * 列表应展示"完善中"状态。</p>
 *
 * <p>所属流程环节：流程流转环节的"撤回/完善"分支。
 * （监听器注册见 {@link cn.wisestar.server.flow.config.WorkflowConfig}，
 * 挂起与激活事件都挂载本监听器，统一置为"申请人完善中"。）</p>
 *
 * <p>被谁调用：Flowable 引擎事件分发器（全局事件监听器）。</p>
 *
 * <p>依赖什么：{@link ContextHelper}（获取 Bean）、{@link FlowInstanceService}（更新实例）。</p>
 *
 * @author javahuang
 * @date 2022/1/23
 */
public class ProcessSuspendedListener implements FlowableEventListener {

	/**
	 * 事件回调：将流程实例状态同步为"申请人完善中"。
	 *
	 * <p>内部逻辑：从事件中取出流程实例 ID，组装仅含 id、status、approvalStage
	 * 三个字段的 FlowInstance 执行局部更新，审批阶段显示为"申请人完善中"。</p>
	 *
	 * @param event 实体挂起/激活事件（含流程实例 ID）
	 */
	@Override
	public void onEvent(FlowableEvent event) {
		FlowableEntityEventImpl entityEvent = (FlowableEntityEventImpl) event;
		FlowInstanceService flowInstanceService = ContextHelper.getBean(FlowInstanceService.class);
		FlowInstance instance = new FlowInstance();
		instance.setStatus(FlowInstanceStatus.SUSPENDED);
		// 更新当前任务阶段为待申请人完善中
		instance.setApprovalStage(FlowInstanceStatus.getDictStatus(FlowInstanceStatus.SUSPENDED));
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
