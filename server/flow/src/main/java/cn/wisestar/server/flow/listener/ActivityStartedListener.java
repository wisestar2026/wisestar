package cn.wisestar.server.flow.listener;

import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.flow.constant.FlowInstanceStatus;
import cn.wisestar.server.flow.domain.model.FlowEntryNode;
import cn.wisestar.server.flow.domain.model.FlowInstance;
import cn.wisestar.server.flow.service.FlowEntryNodeService;
import cn.wisestar.server.flow.service.FlowInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.delegate.event.FlowableActivityEvent;

/**
 * 活动节点开始事件监听器（ACTIVITY_STARTED）。
 *
 * <p>职责：当流程执行进入某个活动节点时（每次流转都会触发），若进入的是用户任务
 * 节点（userTask），则同步更新 t_flow_instance 表：把流程实例状态置为"审批中"、
 * 把当前审批阶段更新为该节点的名称。这是"审批中"状态与"当前阶段"展示的数据来源。</p>
 *
 * <p>所属流程环节：审批流转环节。每次任务流转到新的审批节点时由 Flowable 引擎触发
 * （监听器注册见 {@link cn.wisestar.server.flow.config.WorkflowConfig}）。</p>
 *
 * <p>被谁调用：Flowable 引擎事件分发器（全局事件监听器，非业务代码直接调用）。</p>
 *
 * <p>依赖什么：{@link ContextHelper}（从 Spring 容器获取 Bean）、
 * {@link FlowInstanceService}（更新实例）、{@link FlowEntryNodeService}（按节点 ID
 * 查询节点名称）。</p>
 *
 * @author javahuang
 * @date 2022/1/9
 */
@Slf4j
public class ActivityStartedListener implements FlowableEventListener {

	/**
	 * 事件回调：更新流程实例的当前审批阶段与状态。
	 *
	 * <p>内部逻辑：</p>
	 * <ol>
	 *   <li>仅处理 userTask 类型的活动事件（忽略开始事件、网关、服务任务等其它节点）；</li>
	 *   <li>从事件中取出流程实例 ID 与活动节点 ID（taskDefKey）；</li>
	 *   <li>按节点 ID 查询节点配置，取节点名称作为审批阶段名，查询不到时回退为
	 *       "审批中"；</li>
	 *   <li>组装 FlowInstance（仅含 id、approvalStage、status 三个字段）执行局部更新。</li>
	 * </ol>
	 *
	 * @param event 活动事件对象（FlowableActivityEvent，含流程实例 ID、活动类型、活动 ID）
	 */
	@Override
	public void onEvent(FlowableEvent event) {
		FlowableActivityEvent activityEvent = (FlowableActivityEvent) event;
		if ("userTask".equals(activityEvent.getActivityType())) {
			String processInstanceId = activityEvent.getProcessInstanceId();
			String taskDefKey = activityEvent.getActivityId();
			// 从 Spring 容器获取服务（监听器由引擎 new 出来，不走依赖注入）
			FlowInstanceService flowInstanceService = ContextHelper.getBean(FlowInstanceService.class);
			FlowEntryNodeService flowEntryNodeService = ContextHelper.getBean(FlowEntryNodeService.class);

			FlowInstance flowInstance = new FlowInstance();
			flowInstance.setId(processInstanceId);
			// 查询当前进入的审批节点配置，取节点名称作为审批阶段
			FlowEntryNode flowEntryNode = flowEntryNodeService.getById(taskDefKey);
			String approvalStage = flowEntryNode.getName();
			if (StringUtils.isBlank(approvalStage)) {
				// 节点不存在或名称为空时，兜底显示"审批中"
				approvalStage = FlowInstanceStatus.getDictStatus(FlowInstanceStatus.APPROVING);
			}
			flowInstance.setApprovalStage(approvalStage);
			flowInstance.setStatus(FlowInstanceStatus.APPROVING);
			flowInstanceService.updateById(flowInstance);
		}
	}

	/** 监听器内部异常是否向外抛出：true 表示事件处理失败时中断引擎事务 */
	@Override
	public boolean isFailOnException() {
		return true;
	}

	/** 是否在事务生命周期事件（提交/回滚）时触发：false 表示仅在业务事件发生时触发 */
	@Override
	public boolean isFireOnTransactionLifecycleEvent() {
		return false;
	}

	/** 指定在事务的哪个阶段触发（commit/rollback/complete）：null 表示不限制 */
	@Override
	public String getOnTransaction() {
		return null;
	}

}
