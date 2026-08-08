package cn.wisestar.server.flow.listener;

import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.flow.constant.FlowConstant;
import cn.wisestar.server.flow.constant.FlowInstanceStatus;
import cn.wisestar.server.flow.domain.model.FlowInstance;
import cn.wisestar.server.flow.service.FlowInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.delegate.event.impl.FlowableProcessStartedEventImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;

import java.util.Date;

/**
 * 流程实例启动事件监听器（PROCESS_STARTED）。
 *
 * <p>职责：当申请人发起流程（SaveTaskHandler 调用 startProcessInstanceByKey）时，
 * 同步创建 t_flow_instance 流程实例记录：实例 ID 取引擎的 processInstanceId、
 * 状态置为"审批中"、记录申请人（当前登录用户）与答案 ID、项目 ID。该记录是
 * "我发起的"任务列表与统计的数据来源。</p>
 *
 * <p>所属流程环节：流程发起环节，是整个流程生命周期的起点。
 * （监听器注册见 {@link cn.wisestar.server.flow.config.WorkflowConfig}。）</p>
 *
 * <p>被谁调用：Flowable 引擎事件分发器（全局事件监听器）。</p>
 *
 * <p>依赖什么：{@link ContextHelper}（获取 Bean）、{@link SecurityContextUtils}
 * （获取当前登录用户）、{@link FlowConstant}（流程变量名）、{@link FlowInstanceService}（保存实例）。</p>
 *
 * @author javahuang
 * @date 2022/1/10
 */
@Slf4j
public class ProcessStartedListener implements FlowableEventListener {

	/**
	 * 事件回调：创建流程实例记录。
	 *
	 * <p>内部逻辑：</p>
	 * <ol>
	 *   <li>从启动事件中取流程变量 answerId（发起时 SaveTaskHandler 放入）；</li>
	 *   <li>从执行实体中取流程定义 key 作为 projectId、取 processInstanceId 作为实例 ID；</li>
	 *   <li>组装 FlowInstance（状态"审批中"、创建人取当前登录用户）并入库。</li>
	 * </ol>
	 *
	 * @param event 流程启动事件（含执行实体与启动变量）
	 */
	@Override
	public void onEvent(FlowableEvent event) {
		FlowableProcessStartedEventImpl entityEvent = (FlowableProcessStartedEventImpl) event;
		ExecutionEntityImpl entity = (ExecutionEntityImpl) entityEvent.getEntity();
		// 添加流程实例
		FlowInstance instance = new FlowInstance();
		String answerId = (String) entityEvent.getVariables().get(FlowConstant.VARIABLE_ANSWER_KEY);
		instance.setAnswerId(answerId);
		instance.setProjectId(entity.getProcessDefinitionKey());
		instance.setId(entity.getProcessInstanceId());
		instance.setStatus(FlowInstanceStatus.APPROVING);
		instance.setCreateAt(new Date());
		instance.setCreateBy(SecurityContextUtils.getUserId());
		FlowInstanceService flowInstanceService = ContextHelper.getBean(FlowInstanceService.class);
		flowInstanceService.save(instance);
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
