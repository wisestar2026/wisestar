package cn.wisestar.server.flow.service.impl.taskHandler;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.AnswerQuery;
import cn.wisestar.server.domain.dto.AnswerRequest;
import cn.wisestar.server.domain.dto.AnswerView;
import cn.wisestar.server.flow.constant.FieldPermissionType;
import cn.wisestar.server.flow.constant.FlowApprovalType;
import cn.wisestar.server.flow.constant.FlowConstant;
import cn.wisestar.server.flow.constant.FlowTaskType;
import cn.wisestar.server.flow.domain.dto.ApprovalTaskRequest;
import cn.wisestar.server.flow.domain.dto.UpdateFlowOperationUserRequest;
import cn.wisestar.server.flow.domain.model.FlowEntry;
import cn.wisestar.server.flow.domain.model.FlowEntryNode;
import cn.wisestar.server.flow.domain.model.FlowOperation;
import cn.wisestar.server.flow.domain.model.FlowOperationUser;
import cn.wisestar.server.flow.mapper.FlowOperationMapper;
import cn.wisestar.server.flow.service.*;
import cn.wisestar.server.service.AnswerService;
import cn.wisestar.server.service.UserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.Data;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 审批任务处理器抽象基类。
 *
 * <p>职责：为所有具体 TaskHandler（同意/拒绝/驳回/撤回/保存）提供公共能力与统一
 * 的处理模板：</p>
 * <ul>
 *   <li><b>处理模板</b> {@link #process}：执行子类 {@link #innerProcess} 完成引擎侧
 *       任务流转，成功后统一落库操作记录（saveOperation）并更新表单答案（updateTaskAnswer）；</li>
 *   <li><b>答案合并</b> {@link #mergeAnswer}：按字段权限合并当前节点提交的答案与原始答案；</li>
 *   <li><b>操作记录维护</b> {@link #saveOperation} / {@link #saveOperationUser}：
 *       维护 t_flow_operation 与 t_flow_operation_user 的 latest 标记与记录写入；</li>
 *   <li><b>查询工具</b>：当前任务、历史活动、活跃任务、操作历史、流程定义、开始节点等查询；</li>
 *   <li><b>撤回/回退支持</b> {@link #rollbackToStartEvent}（撤回至发起节点）、
 *       {@link #getHistoricTree}（构建操作节点树）与内部类 {@link TaskTreeNode}。</li>
 * </ul>
 *
 * <p>所属流程环节：审批处理环节的公共服务层，被全部 5 个具体处理器继承。</p>
 *
 * <p>被谁调用：AgreeTaskHandler / RefuseTaskHandler / RollbackTaskHandler /
 * RevertTaskHandler / SaveTaskHandler（继承调用模板方法），RevertTaskHandler
 * 的节点树逻辑亦供 FlowServiceImpl 使用。</p>
 *
 * <p>依赖什么：Flowable 引擎服务（TaskService / RuntimeService / HistoryService /
 * RepositoryService）、各实体 Service（AnswerService / UserService / FlowInstanceService /
 * FlowEntryNodeService / FlowEntryService / FlowOperationService / FlowOperationUserService）、
 * {@link FlowOperationMapper}（latest 标记更新 SQL）。</p>
 *
 * @author javahuang
 * @date 2021/12/17
 */
public abstract class AbstractTaskHandler implements TaskHandler {

	/** 答案服务：加载/更新问卷表单答案 */
	@Autowired
	protected AnswerService answerService;

	/** Flowable 任务服务：查询/完成待办任务 */
	@Autowired
	protected TaskService taskService;

	/** Flowable 运行时服务：启动/挂起/激活/删除实例、变更活动状态 */
	@Autowired
	protected RuntimeService runtimeService;

	/** 用户服务：加载用户信息 */
	@Autowired
	protected UserService userService;

	/** 流程实例服务：查询 t_flow_instance */
	@Autowired
	protected FlowInstanceService flowInstanceService;

	/** 流程节点服务：查询节点配置（字段权限、名称等） */
	@Autowired
	protected FlowEntryNodeService entryNodeService;

	/** 流程定义服务：查询 t_flow_entry */
	@Autowired
	protected FlowEntryService entryService;

	/** 操作记录服务：查询/保存 t_flow_operation */
	@Autowired
	protected FlowOperationService flowOperationService;

	/** Flowable 历史服务：查询历史活动实例 */
	@Autowired
	private HistoryService historyService;

	/** 操作人服务：保存 t_flow_operation_user */
	@Autowired
	protected FlowOperationUserService flowOperationUserService;

	/** Flowable 仓库服务：查询 BPMN 模型、流程定义 */
	@Autowired
	protected RepositoryService repositoryService;

	/**
	 * 执行引擎侧的任务流转（子类实现）。
	 *
	 * <p>由各具体处理器实现：保存→启动/重新激活流程；同意→完成当前任务；拒绝→删除
	 * 流程实例；驳回→移动当前活动到目标节点；撤回→移动/挂起到发起节点。</p>
	 *
	 * @param request 审批请求
	 * @return 流转是否成功（false 时跳过操作记录落库与答案更新，如无流程绑定）
	 */
	public abstract boolean innerProcess(ApprovalTaskRequest request);

	/**
	 * 审批处理模板方法。
	 *
	 * <p>内部逻辑（流程从请求到落库的完整链路）：</p>
	 * <ol>
	 *   <li>调用子类 {@link #innerProcess(request)} 执行 Flowable 引擎侧流转
	 *       （启动实例/完成任务/删实例/移动节点/挂起实例等）；</li>
	 *   <li>流转失败（返回 false）则直接返回，不产生操作记录；</li>
	 *   <li>流转成功 → {@link #saveOperation} 落库操作记录（先置旧记录 latest=false，
	 *       再写入新记录与操作人记录）；</li>
	 *   <li>{@link #updateTaskAnswer} 按字段权限合并本次提交的答案并更新表单答案。</li>
	 * </ol>
	 *
	 * @param request 审批请求（含本次操作的全部参数）
	 */
	@Override
	public void process(ApprovalTaskRequest request) {
		boolean success = innerProcess(request);
		if (!success) {
			return;
		}
		saveOperation(request);
		updateTaskAnswer(request);
	}

	/**
	 * 合并当前答案与原始答案（按字段权限）。
	 *
	 * <p>内部逻辑：以原始答案为基础副本，将原始答案中"当前节点可编辑"（editable）
	 * 的字段移除（因为这些字段会以审批人本次提交的答案为准），再合并本次提交的答案，
	 * 最终得到：历史不可编辑字段保留原值 + 可编辑字段以本次提交值为准。</p>
	 *
	 * @param target 当前答案（本次操作提交的答案）
	 * @param source 原始答案（表单已存答案）
	 * @param fieldPermission 当前答案的数据权限（题目 ID → 0/1/2）
	 * @return 合并后的完整答案
	 */
	protected LinkedHashMap mergeAnswer(LinkedHashMap target, LinkedHashMap source,
			LinkedHashMap<String, Integer> fieldPermission) {
		LinkedHashMap result = new LinkedHashMap(source);
		if (fieldPermission == null) {
			return target;
		}
		// 原始答案里面过滤掉当前权限允许编辑的答案
		fieldPermission.entrySet().forEach(entry -> {
			String qId = entry.getKey();
			Integer permType = entry.getValue();
			if (permType == FieldPermissionType.editable) {
				result.remove(qId);
			}
		});
		// 合并当前答案
		result.putAll(target);
		return result;
	}

	/**
	 * 更新任务对应的表单答案。
	 *
	 * <p>内部逻辑：若本次请求携带了答案，则加载该节点配置获取字段权限，用
	 * {@link #mergeAnswer} 合并本次提交答案与原始答案后，调用 AnswerService 更新
	 * 表单答案记录。数据链路：ApprovalTaskRequest.answer → mergeAnswer → AnswerService
	 * .updateAnswer → t_answer 表。</p>
	 *
	 * @param request 审批请求（含 answer、activityId、answerId）
	 */
	protected void updateTaskAnswer(ApprovalTaskRequest request) {
		if (request.getAnswer() == null) {
			return;
		}
		FlowEntryNode flowElement = entryNodeService.getById(request.getActivityId());
		AnswerQuery answerQuery = new AnswerQuery();
		answerQuery.setId(request.getAnswerId());
		AnswerView answerView = answerService.getAnswer(answerQuery);
		LinkedHashMap mergedAnswer = mergeAnswer(request.getAnswer(), answerView.getAnswer(),
				flowElement.getFieldPermission());

		AnswerRequest answerRequest = new AnswerRequest();
		answerRequest.setId(request.getAnswerId());
		answerRequest.setAnswer(mergedAnswer);
		answerService.updateAnswer(answerRequest);
	}

	/**
	 * 保存操作记录（含 latest 标记维护）。
	 *
	 * <p>内部逻辑：</p>
	 * <ol>
	 *   <li>调用 FlowOperationMapper.updateOperationLatest 把该实例全部旧操作记录
	 *       置为 latest=false（目的：只有最新操作记录的对应人才能进行撤回操作）；</li>
	 *   <li>组装并保存新的 FlowOperation 记录（approvalType、节点、答案、意见、
	 *       操作人等，latest=true）；</li>
	 *   <li>调用 updateOperationUserLatest 把当前用户在该实例的历史操作人记录置为
	 *       latest=false（目的：一人参与多节点时已办只显示最近参与的节点）；</li>
	 *   <li>saveOperationUser 写入当前操作人记录（latest=true）。</li>
	 * </ol>
	 *
	 * @param request 审批请求（操作参数）
	 */
	private void saveOperation(ApprovalTaskRequest request) {
		// 更新审批记录为历史审批记录，目的是只有最新操作记录的对应的人才能进行撤回操作
		FlowOperationMapper flowOperationMapper = (FlowOperationMapper) flowOperationService.getBaseMapper();
		flowOperationMapper.updateOperationLatest(request.getProcessInstanceId());

		// 添加新的操作记录
		FlowOperation operation = new FlowOperation();
		operation.setAnswerId(request.getAnswerId());
		operation.setAnswer(request.getAnswer());
		operation.setComment(request.getComment());
		operation.setApprovalType(request.getType());
		operation.setTaskType(FlowTaskType.userTask);
		operation.setTaskId(request.getTaskId());
		operation.setProjectId(request.getProjectId());
		operation.setActivityId(request.getActivityId());
		operation.setNewActivityId(request.getNewActivityId());
		operation.setInstanceId(request.getProcessInstanceId());
		operation.setCreateAt(new Date());
		operation.setCreateBy(SecurityContextUtils.getUserId());
		operation.setLatest(true);
		flowOperationService.save(operation);

		// 更新操作人历史
		// 如果一个人参与了多个流程节点，只显示最近参与的流程节点
		UpdateFlowOperationUserRequest updateFlowOperationUserRequest = new UpdateFlowOperationUserRequest();
		updateFlowOperationUserRequest.setTaskType(FlowTaskType.userTask);
		updateFlowOperationUserRequest.setUserId(SecurityContextUtils.getUserId());
		updateFlowOperationUserRequest.setInstanceId(request.getProcessInstanceId());
		flowOperationMapper.updateOperationUserLatest(updateFlowOperationUserRequest);
		// 更新操作人为最新的操作人
		saveOperationUser(operation);
	}

	/**
	 * 保存当前操作人记录（latest=true）。
	 *
	 * @param operation 已保存的操作记录（取其 ID 关联操作人）
	 */
	private void saveOperationUser(FlowOperation operation) {
		FlowOperationUser user = new FlowOperationUser();
		user.setLatest(true);
		String userId = SecurityContextUtils.getUserId();
		user.setUserId(userId);
		user.setOperationId(operation.getId());
		user.setCreateAt(new Date());
		user.setCreateBy(userId);
		flowOperationUserService.save(user);
	}

	/**
	 * 查询当前运行中的任务。
	 *
	 * @param taskId Flowable 任务 ID
	 * @return 任务对象；不存在时返回 null
	 */
	protected Task getCurrentRunningTask(String taskId) {
		return taskService.createTaskQuery().taskId(taskId).singleResult();
	}

	/**
	 * 查询流程实例的全部历史活动实例。
	 *
	 * @param processInstanceId 流程实例 ID
	 * @return 历史活动实例列表
	 */
	protected List<HistoricActivityInstance> getHistoricActivityInstanceList(String processInstanceId) {
		return historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();
	}

	/**
	 * 查询流程实例当前活跃（待办）任务列表。
	 *
	 * @param processInstanceId 流程实例 ID
	 * @return 活跃任务列表
	 */
	protected List<Task> getProcessInstanceActiveTaskList(String processInstanceId) {
		return taskService.createTaskQuery().processInstanceId(processInstanceId).list();
	}

	/**
	 * 查询流程实例的用户任务操作记录（按创建时间倒序）。
	 *
	 * @param processInstanceId 流程实例 ID
	 * @return 操作记录列表（最新在前）
	 */
	protected List<FlowOperation> getOperations(String processInstanceId) {
		return flowOperationService
				.list(Wrappers.<FlowOperation>lambdaQuery().eq(FlowOperation::getInstanceId, processInstanceId)
						.eq(FlowOperation::getTaskType, FlowTaskType.userTask).orderByDesc(FlowOperation::getCreateAt));
	}

	/**
	 * 获取流程定义的开始节点（StartEvent）。
	 *
	 * <p>内部逻辑：按项目 ID 查询流程定义，取引擎 BPMN 模型中的第一个 Process，
	 * 返回其开始事件（当前方法未被使用，保留为回退/撤回扩展点）。</p>
	 *
	 * @param projectId 问卷/项目 ID
	 * @return 流程开始事件
	 */
	private StartEvent getStartNode(String projectId) {
		FlowEntry flowEntry = entryService
				.getOne(Wrappers.<FlowEntry>lambdaQuery().eq(FlowEntry::getProjectId, projectId));
		BpmnModel bpmnModel = repositoryService.getBpmnModel(flowEntry.getProcessDefinitionId());
		org.flowable.bpmn.model.Process process = bpmnModel.getProcesses().get(0);
		List<StartEvent> startEvents = process.findFlowElementsOfType(StartEvent.class);
		return startEvents.get(0);
	}

	/**
	 * 按项目 ID 查询流程定义。
	 *
	 * @param projectId 问卷/项目 ID
	 * @return 流程定义实体
	 */
	protected FlowEntry getFlowEntry(String projectId) {
		return entryService.getOne(Wrappers.<FlowEntry>lambdaQuery().eq(FlowEntry::getProjectId, projectId));
	}

	/**
	 * 撤回/回退到发起节点（申请人）。
	 *
	 * <p>内部逻辑：当请求的目标节点 ID（newActivityId）等于项目 ID（流程定义 key，
	 * 前端约定"回退到发起人"的标志）时：</p>
	 * <ol>
	 *   <li>取当前活跃任务所在节点；</li>
	 *   <li>将 newActivityId 置为固定的发起节点 ID（starter）；</li>
	 *   <li>挂起流程实例（原实现注释了 moveActivityIdTo 移动节点，改为直接挂起：
	 *       回滚至开始节点之后任务会自动流转到下一节点，因此需暂停任务等待申请人完善）；</li>
	 *   <li>返回 true 表示已处理回退到发起节点。</li>
	 * </ol>
	 *
	 * @param request 审批请求（newActivityId 为项目 ID 时触发）
	 * @return 是否执行了回退到发起节点的逻辑
	 */
	protected boolean rollbackToStartEvent(ApprovalTaskRequest request) {
		if (!request.getNewActivityId().equals(request.getProjectId())) {
			return false;
		}
		// StartEvent event = getStartNode(request.getProjectId());
		String starterActivityId = FlowConstant.STARTER_ACTIVITY_ID;
		String currentActivityId = getProcessInstanceActiveTaskList(request.getProcessInstanceId()).get(0)
				.getTaskDefinitionKey();
		request.setNewActivityId(starterActivityId);
		// runtimeService.createChangeActivityStateBuilder().processInstanceId(request.getProcessInstanceId())
		// .moveActivityIdTo(currentActivityId, starterActivityId);
		// 回滚至开始节点之后，任务自动流程到了下一节点，所以需要暂停任务
		runtimeService.suspendProcessInstanceById(request.getProcessInstanceId());
		return true;
	}

	/**
	 * 将任务流转记录转换成一棵节点树。
	 *
	 * <p>用途：回退（驳回）节点计算的数据基础。遍历实例的用户任务操作记录
	 * （仅保存/同意/驳回/撤回四种类型，按时间正序），按规则构建有向树：</p>
	 * <ul>
	 *   <li>第一条记录作为根节点；</li>
	 *   <li>连续相同节点记录去重跳过；</li>
	 *   <li>save（发起保存）：回到根节点继续；</li>
	 *   <li>agree（同意）：作为最后一个节点的子节点加入（同节点去重）；</li>
	 *   <li>rollback（驳回）：将游标回退到目标节点（findParentByKey），本记录不入树；</li>
	 *   <li>revert（撤回）：节点的 taskDefKey 与已完成节点一致，不改变树结构。</li>
	 * </ul>
	 *
	 * @param processInstanceId 任务实例 ID
	 * @return 最后一个操作节点（其 parent 链路即"可回退节点"）
	 */
	protected TaskTreeNode getHistoricTree(String processInstanceId) {
		List<String> approvalTypes = new ArrayList<>();
		approvalTypes.add(FlowApprovalType.SAVE);
		approvalTypes.add(FlowApprovalType.AGREE);
		approvalTypes.add(FlowApprovalType.ROLLBACK);
		approvalTypes.add(FlowApprovalType.REVERT);
		List<FlowOperation> operations = flowOperationService
				.list(Wrappers.<FlowOperation>lambdaQuery().eq(FlowOperation::getInstanceId, processInstanceId)
						.in(FlowOperation::getApprovalType, approvalTypes).orderByAsc(FlowOperation::getCreateAt));
		TaskTreeNode rootNode = null, lastNode = null;
		for (int i = 0; i < operations.size(); i++) {
			FlowOperation operation = operations.get(i);
			TaskTreeNode currNode = new TaskTreeNode();
			currNode.setId(operation.getId());
			currNode.setActivityId(operation.getActivityId());

			if (i == 0) {
				// 第一条记录作为根节点
				rootNode = currNode;
				lastNode = currNode;
				continue;
			}
			// 连续相同节点（同一节点重复操作）去重
			if (lastNode != null && lastNode.getActivityId() != null
					&& lastNode.getActivityId().equals(currNode.getActivityId())) {
				continue;
			}
			if (FlowApprovalType.SAVE.equals(operation.getApprovalType())) {
				// 保存（重新发起）：游标回到根节点
				lastNode = rootNode;
				continue;
			}
			else if (FlowApprovalType.AGREE.equals(operation.getApprovalType())) {
				// 同意：挂到最后一个节点下作为子节点（同节点去重）
				currNode.setParent(lastNode);
				if (lastNode.getChildren().stream()
						.noneMatch(node -> node.getActivityId().equals(currNode.getActivityId()))) {
					lastNode.getChildren().add(currNode);
				}
			}
			else if (FlowApprovalType.ROLLBACK.equals(operation.getApprovalType())) {
				// 驳回：游标回退到目标节点（newActivityId），当前记录不入树
				lastNode = lastNode.findParentByKey(operation.getNewActivityId());
				continue;
			}
			else if (FlowApprovalType.REVERT.equals(operation.getApprovalType())) {
				// 撤回节点的 taskDefKey 和已完成节点是一致的（树结构不变，无需处理）
			}
			lastNode = currNode;
		}

		return lastNode;
	}

	/**
	 * 任务节点树节点（内部数据结构）。
	 *
	 * <p>职责：表示由操作记录构建的审批流转树中的一个节点，携带节点 ID、活动 ID、
	 * 父节点与子节点列表，提供按活动 ID 向上查找祖先节点的能力
	 * （{@link #findParentByKey}），供"可回退节点"计算使用。</p>
	 */
	@Data
	public static class TaskTreeNode {

		/** 节点唯一标识：对应操作记录 ID */
		private String id;

		/** 活动节点 ID：对应 BPMN 节点 activityId */
		private String activityId;

		/** 父节点：流转路径上的上一节点 */
		private TaskTreeNode parent;

		/** 子节点列表：该节点同意后流转到的后续节点 */
		private List<TaskTreeNode> children = new ArrayList<>();

		/**
		 * 沿父链向上查找指定活动 ID 的节点。
		 *
		 * <p>用途：驳回操作时定位"要回退到的目标节点"在树中的位置。</p>
		 *
		 * @param key 目标活动 ID
		 * @return 匹配的节点；沿父链未找到返回 null
		 */
		public TaskTreeNode findParentByKey(String key) {
			if (this.getActivityId().equals(key)) {
				return this;
			}
			if (this.parent == null) {
				return null;
			}
			return this.parent.findParentByKey(key);
		}

		/**
		 * 调试用字符串表示。
		 *
		 * @return "TaskTreeNode{id='...', activityId='...'}"
		 */
		@Override
		public String toString() {
			return "TaskTreeNode{" + "id='" + id + '\'' + ", activityId='" + activityId + '\'' + '}';
		}

	}

}
