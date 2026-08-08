package cn.wisestar.server.flow.service.impl.taskHandler;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.flow.constant.FlowConstant;
import cn.wisestar.server.flow.domain.dto.ApprovalTaskRequest;
import cn.wisestar.server.flow.domain.model.FlowEntry;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 保存（发起）任务处理器（Bean 名 "saveTaskHandler"）。
 *
 * <p>职责：处理"保存/提交"操作，即流程发起。申请人在问卷页填写完成并提交时触发：</p>
 * <ul>
 *   <li><b>首次提交</b>（processInstanceId 为空）：按项目 ID（流程定义 key）启动新的
 *       Flowable 流程实例，业务键为答案 ID，并把 answerId、发起人 ID 放入流程变量；
 *       引擎触发 PROCESS_STARTED 事件创建 t_flow_instance；</li>
 *   <li><b>再次提交</b>（processInstanceId 已存在，即"撤回后申请人完善"场景）：
 *       重新激活被挂起的实例，并把当前活跃任务移动回发起节点（starter），让流程重新走
 *       第一轮审批（引擎再次触发 ACTIVITY_STARTED 更新审批阶段）。</li>
 * </ul>
 *
 * <p>流程流转（发起分支）：申请人提交表单 → FlowServiceImpl.approvalTask 分发到本
 * 处理器 → 启动/重新激活实例 → 父类模板落库操作记录（approvalType=save）并更新答案。</p>
 *
 * <p>被谁调用：FlowServiceImpl.approvalTask（按 Bean 名动态获取）。</p>
 *
 * <p>依赖什么：父类 {@link AbstractTaskHandler}（模板方法、操作记录落库）、
 * Flowable RuntimeService（启动/激活实例）、{@link FlowConstant}（流程变量名）。</p>
 *
 * @author javahuang
 * @date 2022/1/9
 */
@Component("saveTaskHandler")
public class SaveTaskHandler extends AbstractTaskHandler {

	/**
	 * 执行"保存/发起"流转。
	 *
	 * <p>内部逻辑：</p>
	 * <ol>
	 *   <li>组装流程变量：answerId（答案主键）、starterUserId（当前登录用户即发起人）；</li>
	 *   <li>查询项目绑定的流程定义，未绑定（entry 为空）→ 返回 false（父类模板将跳过
	 *       操作记录落库与答案更新）；</li>
	 *   <li>首次提交：startProcessInstanceByKey 以项目 ID 为 key、答案 ID 为业务键启动
	 *       实例，并把生成的实例 ID 回写请求；</li>
	 *   <li>再次提交：activateProcessInstanceById 重新激活实例，然后把当前活跃任务移动
	 *       到发起节点 starter（流程重新从第一轮审批开始），并回写 newActivityId。</li>
	 * </ol>
	 *
	 * @param request 审批请求（含 projectId、answerId、processInstanceId）
	 * @return 流程定义存在时 true；未绑定流程时 false（跳过落库）
	 */
	@Override
	public boolean innerProcess(ApprovalTaskRequest request) {
		Map<String, Object> variables = new HashMap<>();
		variables.put(FlowConstant.VARIABLE_ANSWER_KEY, request.getAnswerId());
		variables.put(FlowConstant.VARIABLE_STARTER_USER, SecurityContextUtils.getUserId());
		FlowEntry entry = getFlowEntry(request.getProjectId());
		// 当前问卷未绑定工作流
		if (entry == null) {
			return false;
		}
		// 表示首次提交：按流程定义 key 启动新实例，业务键=答案 ID
		if (request.getProcessInstanceId() == null) {
			ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(request.getProjectId(),
					request.getAnswerId(), variables);
			request.setProcessInstanceId(processInstance.getId());
		}
		else {
			// 表示再次提交，首先需要再次激活当前任务（撤回后实例处于挂起状态）
			runtimeService.activateProcessInstanceById(request.getProcessInstanceId());
			// 然后将任务移动到开始节点，让流程重新走第一轮审批
			String starterActivityId = FlowConstant.STARTER_ACTIVITY_ID;
			String currentActivityId = getProcessInstanceActiveTaskList(request.getProcessInstanceId()).get(0)
					.getTaskDefinitionKey();
			request.setNewActivityId(starterActivityId);
			runtimeService.createChangeActivityStateBuilder().processInstanceId(request.getProcessInstanceId())
					.moveActivityIdTo(currentActivityId, starterActivityId).changeState();
		}
		return true;
	}

}
