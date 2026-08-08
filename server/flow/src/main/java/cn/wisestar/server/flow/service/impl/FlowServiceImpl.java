package cn.wisestar.server.flow.service.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.core.uitls.SchemaHelper;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.flow.constant.*;
import cn.wisestar.server.flow.domain.dto.*;
import cn.wisestar.server.flow.domain.mapper.FlowEntryElementModelMapper;
import cn.wisestar.server.flow.domain.mapper.FlowEntryModelMapper;
import cn.wisestar.server.flow.domain.mapper.FlowOperationModelMapper;
import cn.wisestar.server.flow.domain.model.*;
import cn.wisestar.server.flow.exception.FlowableRuntimeException;
import cn.wisestar.server.flow.service.*;
import cn.wisestar.server.flow.service.impl.taskHandler.RevertTaskHandler;
import cn.wisestar.server.flow.service.impl.taskHandler.TaskHandler;
import cn.wisestar.server.service.AnswerService;
import cn.wisestar.server.service.ProjectService;
import cn.wisestar.server.service.UserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流业务门面实现（核心服务类）。
 *
 * <p>职责：实现 {@link FlowService} 定义的全部业务能力，是流程模块的大脑：</p>
 * <ul>
 *   <li><b>流程设计</b>：saveFlow 保存草稿、getFlowEntry 查询回显；</li>
 *   <li><b>流程部署</b>：deploy 将 BPMN XML 部署到 Flowable 引擎并同步节点配置与版本；</li>
 *   <li><b>发起预检</b>：beforeLaunchProcess 校验申请人是否有权发起并按字段权限过滤问卷；</li>
 *   <li><b>审批处理</b>：approvalTask 按 type 分发到对应 TaskHandler 执行流转；</li>
 *   <li><b>任务查询</b>：getFlowTasks 按待办/已办/我发起的三种口径分页查询；</li>
 *   <li><b>展示查询</b>：loadSchemaByPermission 权限过滤 schema、getAuditRecord 审核记录、
 *       getRevertNodes 可回退节点、statics 任务统计。</li>
 * </ul>
 *
 * <p>流程机制总览（流程从哪开始、如何流转、到哪结束）：</p>
 * <ol>
 *   <li><b>发起</b>：用户填写问卷提交 → SaveTaskHandler 启动流程实例（同时
 *       ProcessStartedListener 创建 t_flow_instance）；</li>
 *   <li><b>流转</b>：引擎进入用户任务节点 → TaskHelper.getUsers 计算审批人创建待办，
 *       ActivityStartedListener 更新审批阶段；审批人通过 approvalTask 操作，按 type
 *       分发（同意→下一节点/流程结束、拒绝→删除实例、驳回→跳到指定节点、撤回→挂起回发起节点）；</li>
 *   <li><b>结束</b>：最后节点同意 → ProcessCompletedListener 置"已结束"；拒绝 → 
 *       ProcessCancelledListener 置"已拒绝"；撤回 → ProcessSuspendedListener 置"完善中"，
 *       申请人重新提交后再次激活实例继续流转。</li>
 * </ol>
 *
 * <p>被谁调用：{@link cn.wisestar.server.flow.api.FlowApi}（HTTP 层）以及问卷
 * 提交前的权限校验逻辑。</p>
 *
 * <p>依赖什么：Flowable 引擎服务（RepositoryService / RuntimeService / TaskService）、
 * 本模块各实体 Service（FlowEntryService / FlowEntryNodeService / FlowEntryPublishService /
 * FlowInstanceService / FlowOperationService）、核心模块服务（UserService / AnswerService /
 * ProjectService）、MapStruct 转换器（FlowEntryModelMapper / FlowEntryElementModelMapper /
 * FlowOperationModelMapper）、{@link RevertTaskHandler}（回退节点计算）。</p>
 *
 * @author javahuang
 * @date 2022/1/5
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class FlowServiceImpl implements FlowService {

	/** 流程定义服务：保存/查询 t_flow_entry 流程定义（草稿态） */
	private final FlowEntryService entryService;

	/** 流程节点服务：保存/查询 t_flow_entry_node 节点配置（发布态） */
	private final FlowEntryNodeService entryNodeService;

	/** 流程发布版本服务：维护 t_flow_entry_publish 发布版本记录 */
	private final FlowEntryPublishService entryPublishService;

	/** 用户服务：加载用户信息、判断用户组/角色 */
	private final UserService userService;

	/** 答案服务：加载/更新问卷表单答案（t_answer） */
	private final AnswerService answerService;

	/** 项目服务：加载问卷/项目视图（含 survey 结构） */
	private final ProjectService projectService;

	/** Flowable 仓库服务：部署流程定义、查询 BPMN 模型 */
	private final RepositoryService repositoryService;

	/** 流程定义 MapStruct 转换器：FlowEntry → FlowEntryView */
	private final FlowEntryModelMapper entryModelMapper;

	/** 流程节点 MapStruct 转换器：FlowEntryNodeRequest/FlowEntryNode/FlowEntryNodeView 互转 */
	private final FlowEntryElementModelMapper entryNodeModelMapper;

	/** 操作记录 MapStruct 转换器：FlowOperation → FlowOperationView */
	private final FlowOperationModelMapper flowOperationModelMapper;

	/** 流程实例服务：查询/更新 t_flow_instance */
	private final FlowInstanceService flowInstanceService;

	/** 操作记录服务：查询 t_flow_operation */
	private final FlowOperationService flowOperationService;

	/** Flowable 任务服务：查询/完成待办任务 */
	private final TaskService taskService;

	/** Flowable 运行时服务：启动/挂起/激活/删除流程实例、变更活动状态 */
	private final RuntimeService runtimeService;

	/** 撤回任务处理器：用于计算可回退节点列表 */
	private final RevertTaskHandler revertTaskHandler;

	/**
	 * 问卷开始之前，根据权限过滤字段。
	 *
	 * <p>流程环节：发起预检。用户在问卷页开始填表前由问卷模块调用，判断当前问卷
	 * 是否绑定流程以及当前用户是否有权发起，并按发起节点的字段权限过滤问卷结构：</p>
	 * <ol>
	 *   <li>问卷未绑定流程（无节点配置）→ 直接放行；</li>
	 *   <li>发起节点未配置审批人（identity 为空）→ 仅按字段权限过滤 schema 放行；</li>
	 *   <li>开启工作流（节点配置了审批人）但用户未登录 → 标记 loginRequired 并置空 survey，阻止作答；</li>
	 *   <li>用户已登录：若其用户组包含发起节点的任一授权身份 → 按字段权限过滤 schema 放行；</li>
	 *   <li>否则视为无发起权限 → 置空 survey，前端提示无权发起。</li>
	 * </ol>
	 *
	 * @param projectView 问卷视图（含 survey），会被就地修改
	 */
	@Override
	public void beforeLaunchProcess(PublicProjectView projectView) {
		SurveySchema schema = projectView.getSurvey();
		if (schema == null) {
			return;
		}
		// 按问卷 schema 的 ID 查找发起节点配置（schema.getId 即发起节点 activityId）
		FlowEntryNode node = entryNodeService.getById(schema.getId());
		// 未设置流程
		if (node == null) {
			return;
		}
		// 流程申请人为空（未指定发起人身份），仅需按字段权限过滤即可
		if (node.getIdentity() == null) {
			SchemaHelper.updateSchemaByPermission(node.getFieldPermission(), schema);
			return;
		}
		// 需要登录，如开启了工作流或者设置了成员/部门题都需要登录才能答卷
		if (!SecurityContextUtils.isAuthenticated()) {
			projectView.setLoginRequired(true);
			projectView.setSurvey(null);
			return;
		}
		String userId = SecurityContextUtils.getUserId();
		Set<String> userGroups = userService.getUserGroups(userId);
		for (String identity : node.getIdentity()) {
			// 当前用户的用户组命中发起节点的任一授权身份，允许发起
			if (userGroups.contains(identity)) {
				SchemaHelper.updateSchemaByPermission(node.getFieldPermission(), schema);
				return;
			}
		}
		// 用户没有发起流程权限：置空问卷结构，前端提示无权发起
		projectView.setSurvey(null);
	}

	/**
	 * 保存流程设计（草稿态）。
	 *
	 * <p>流程环节：流程设计保存。将前端提交的 BPMN XML 与节点配置保存到
	 * t_flow_entry（bpmn_xml 与 nodes JSON 列）：首次保存则新建，已存在则覆盖更新。
	 * 注意此时节点仅以 JSON 形式存于流程定义的 nodes 列，尚未拆解到
	 * t_flow_entry_node 表（该步骤在 deploy 部署时执行）。</p>
	 *
	 * @param request 流程保存请求（bpmnXml、projectId、节点列表）
	 */
	@Override
	public void saveFlow(FlowEntryRequest request) {
		FlowEntry flow = entryService
				.getOne(Wrappers.<FlowEntry>lambdaQuery().eq(FlowEntry::getProjectId, request.getProjectId()));
		if (flow == null) {
			// 首次保存：新建流程定义记录
			flow = new FlowEntry();
			flow.setProjectId(request.getProjectId());
			flow.setBpmnXml(request.getBpmnXml());
			flow.setNodes(entryNodeModelMapper.fromRequest(request.getNodes()));
			entryService.save(flow);
		}
		else {
			// 已存在：覆盖 XML 与节点配置
			flow.setBpmnXml(request.getBpmnXml());
			flow.setNodes(entryNodeModelMapper.fromRequest(request.getNodes()));
			entryService.updateById(flow);
		}
	}

	/**
	 * 部署流程设计（发布为新版本）。
	 *
	 * <p>流程环节：流程发布。完整流程：</p>
	 * <ol>
	 *   <li>校验问卷是否已配置流程（无则抛异常）；</li>
	 *   <li>将 BPMN XML 解析为 BpmnModel，调用 Flowable RepositoryService 部署
	 *       （资源名 = 流程记录 ID + ".bpmn20.xml"），拿到部署 ID 与流程定义；</li>
	 *   <li>回填 flowEntry 的 deployId / processDefinitionId，并置 status=已发布；</li>
	 *   <li>将旧版本发布记录全部置为历史版本（mainVersion=false、activeStatus=false）；</li>
	 *   <li>写入新的发布版本记录（id=部署 ID、版本号=引擎版本号、主版本=true）；</li>
	 *   <li>将流程定义的 nodes（草稿节点）拆解保存到 t_flow_entry_node 表（saveFlowElement）。</li>
	 * </ol>
	 * 任何异常统一包装为 {@link FlowableRuntimeException} 抛出（事务回滚）。
	 *
	 * @param projectId 问卷/项目 ID
	 */
	@Override
	public void deploy(String projectId) {
		FlowEntry flowEntry = entryService
				.getOne(Wrappers.<FlowEntry>lambdaQuery().eq(FlowEntry::getProjectId, projectId));
		if (flowEntry == null) {
			throw new FlowableRuntimeException("该问卷未设置流程");
		}
		try {
			// 解析 BPMN XML 为 Flowable 的 BpmnModel
			InputStream xmlStream = new ByteArrayInputStream(flowEntry.getBpmnXml().getBytes(StandardCharsets.UTF_8));
			@Cleanup
			XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(xmlStream);
			BpmnXMLConverter converter = new BpmnXMLConverter();
			BpmnModel bpmnModel = converter.convertToBpmnModel(reader);

			// 部署到 Flowable 引擎
			Deployment deployment = repositoryService.createDeployment()
					.addBpmnModel(flowEntry.getId() + ".bpmn20.xml", bpmnModel).deploy();
			ProcessDefinition pd = getProcessDefinitionByDeployId(deployment.getId());

			// 回填部署 ID 与项目 ID
			flowEntry.setDeployId(deployment.getId());
			flowEntry.setProjectId(projectId);
			entryService.updateById(flowEntry);

			// 更新之前版本为历史版本（同一条流程定义的所有旧发布记录全部失效）
			FlowEntryPublish entryPublished = new FlowEntryPublish();
			entryPublished.setMainVersion(false);
			entryPublished.setActiveStatus(false);
			entryPublishService.update(entryPublished,
					Wrappers.<FlowEntryPublish>lambdaUpdate().eq(FlowEntryPublish::getEntryId, flowEntry.getId()));

			// 发布新版本（以部署 ID 作为发布记录主键，与引擎部署一一对应）
			FlowEntryPublish entryPublish = new FlowEntryPublish();
			entryPublish.setId(deployment.getId());
			entryPublish.setEntryId(flowEntry.getId());
			entryPublish.setPublishTime(new Date());
			entryPublish.setActiveStatus(true);
			entryPublish.setPublishVersion(pd.getVersion());
			entryPublish.setProcessDefinitionId(pd.getId());
			entryPublish.setMainVersion(true);
			entryPublish.setActiveStatus(true);
			entryPublishService.save(entryPublish);

			// 更新流程节点：把草稿 nodes 拆解落库到 t_flow_entry_node
			saveFlowElement(flowEntry);

			// 更新流程信息：回填引擎流程定义 ID 并标记已发布
			flowEntry.setProcessDefinitionId(pd.getId());
			flowEntry.setStatus(FlowEntryStatus.PUBLISHED);
			entryService.updateById(flowEntry);
		}
		catch (Exception e) {
			throw new FlowableRuntimeException("流程" + flowEntry.getId() + "部署失败", e);
		}
	}

	/**
	 * 获取流程定义信息（设计器回显）。
	 *
	 * <p>流程环节：流程设计读取。按 projectId 查询流程定义，经 MapStruct 转为
	 * FlowEntryView 并补充节点视图列表；问卷尚未配置流程时返回仅含 projectId 的空视图。</p>
	 *
	 * @param projectId 问卷/项目 ID
	 * @return 流程定义视图（含 bpmnXml 与节点列表）
	 */
	@Override
	public FlowEntryView getFlowEntry(String projectId) {
		FlowEntry flow = entryService.getOne(Wrappers.<FlowEntry>lambdaQuery().eq(FlowEntry::getProjectId, projectId));
		if (flow == null) {
			flow = new FlowEntry();
			flow.setProjectId(projectId);
		}
		FlowEntryView result = entryModelMapper.toView(flow);
		result.setNodes(entryNodeModelMapper.toView(flow.getNodes()));
		return result;
	}

	/**
	 * 审批任务：按类型分发到对应 TaskHandler 执行。
	 *
	 * <p>流程环节：审批处理的总分发入口。根据请求的 type（save/agree/refuse/
	 * rollback/revert）从 Spring 容器按 Bean 名（type + "TaskHandler"）获取处理器
	 * 并执行。各处理器都继承 {@link AbstractTaskHandler}，统一在 process 模板方法中
	 * 完成"引擎流转 + 操作记录落库 + 答案更新"。</p>
	 *
	 * @param request 审批请求（type、taskId、processInstanceId、答案、意见等）
	 */
	@Override
	public void approvalTask(ApprovalTaskRequest request) {
		TaskHandler taskHandler = (TaskHandler) ContextHelper.getBean(request.getType() + "TaskHandler");
		taskHandler.process(request);
	}

	/**
	 * 分页获取当前用户的任务列表。
	 *
	 * <p>流程环节：待办中心。按查询类型分发：</p>
	 * <ul>
	 *   <li>todo（我的待办）→ {@link #getTodo}：查 Flowable 运行时任务（候选人或指派人）；</li>
	 *   <li>finished（已办事项）→ {@link #getFinished}：查 t_flow_operation 中 latest=1
	 *       且操作人=当前用户 的操作记录；</li>
	 *   <li>selfCreated（我发起的）→ {@link #getSelfCreated}：查 t_flow_instance。</li>
	 * </ul>
	 * 查询后统一补充表单答案（按字段权限过滤）与任务状态信息。
	 *
	 * @param query 查询参数（type、projectId、分页等）
	 * @return 任务视图分页结果
	 */
	@Override
	public PaginationResponse<FlowTaskView> getFlowTasks(FlowTaskQuery query) {
		PaginationResponse<FlowTaskView> result = null;
		if (query.getType() == FlowTaskQueryType.todo) {
			result = getTodo(query);
		}
		if (query.getType() == FlowTaskQueryType.finished) {
			result = getFinished(query);
		}
		if (query.getType() == FlowTaskQueryType.selfCreated) {
			result = getSelfCreated(query);
		}
		if (result != null) {
			// 补充表单答案（含字段权限过滤）与任务状态
			setFlowTaskAnswer(result.getList());
			setTaskStatus(result.getList());
		}
		return result;
	}

	/**
	 * 查询"我的待办"任务列表。
	 *
	 * <p>数据链路：Flowable ACT_RU_TASK（运行时任务表）→ 组装 FlowTaskView →
	 * 上层统一补充答案与状态 → 返回前端。</p>
	 *
	 * <p>内部逻辑：按项目（processDefinitionKey）过滤活跃任务，条件为"用户是任务的
	 * 候选者（candidate）或指派人（assignee）"，按创建时间倒序分页；每条任务取流程
	 * 变量 answerId 用于后续加载表单答案。</p>
	 *
	 * @param query 查询参数
	 * @return 待办任务分页结果
	 */
	private PaginationResponse<FlowTaskView> getTodo(FlowTaskQuery query) {
		TaskQuery taskQuery = taskService.createTaskQuery().active();
		String userId = SecurityContextUtils.getUserId();
		// 查询条件：项目下当前用户作为候选者或指派人的活跃任务（附流程变量）
		// https://forum.flowable.org/t/sql-exception-with-task-query-after-upgrade-to-6-7-0/8334
		taskQuery.processDefinitionKey(query.getProjectId()).or().taskCandidateUser(userId).taskAssignee(userId).endOr()
				.includeProcessVariables().orderByTaskCreateTime().desc();
		int firstResult = (query.getCurrent() - 1) * query.getPageSize();
		List<Task> taskList = taskQuery.listPage(firstResult, query.getPageSize());
		long totalCount = taskQuery.count();

		// 组装任务视图：核心信息为任务 ID、节点 key、流程实例 ID 与 answerId
		List<FlowTaskView> viewList = taskList.stream().map(task -> {
			FlowTaskView taskView = new FlowTaskView();
			taskView.setId(task.getId());
			taskView.setCreateAt(task.getCreateTime());
			taskView.setProjectId(query.getProjectId());
			String answerId = (String) task.getProcessVariables().get(FlowConstant.VARIABLE_ANSWER_KEY);
			taskView.setAnswerId(answerId);
			taskView.setActivityId(task.getTaskDefinitionKey());
			taskView.setProcessInstanceId(task.getProcessInstanceId());
			return taskView;
		}).collect(Collectors.toList());

		return new PaginationResponse<>(totalCount, viewList);
	}

	/**
	 * 查询"已办事项"任务列表。
	 *
	 * <p>数据链路：t_flow_operation（操作记录表）+ t_flow_operation_user（操作人表）
	 * → 组装 FlowTaskView → 上层统一补充答案与状态 → 返回前端。</p>
	 *
	 * <p>内部逻辑：分页查询该项目的用户任务操作记录（排除 save 类型的草稿保存），
	 * 并用 exists 子查询限定"最新操作人（latest=1）中包含当前用户"的记录，
	 * 保证已办列表只展示当前用户最近参与过的节点（同一用户多次参与同一实例时只显示一次）。</p>
	 *
	 * @param query 查询参数
	 * @return 已办任务分页结果
	 */
	private PaginationResponse<FlowTaskView> getFinished(FlowTaskQuery query) {
		Page<FlowOperation> page = new Page<>(query.getCurrent(), query.getPageSize());

		flowOperationService.page(page, Wrappers.<FlowOperation>lambdaQuery()
				.eq(FlowOperation::getProjectId, query.getProjectId())
				.eq(FlowOperation::getTaskType, FlowTaskType.userTask)
				.ne(FlowOperation::getApprovalType, FlowApprovalType.SAVE)
				// 限定最新操作人中包含当前用户的操作记录
				.exists(String.format(
						"select 1 from t_flow_operation_user u where u.latest = 1 and u.operation_id = t_flow_operation.id and u.user_id = '%s'",
						SecurityContextUtils.getUserId()))
				.orderByDesc(FlowOperation::getCreateAt));
		List<FlowTaskView> viewList = page.getRecords().stream().map(opt -> {
			FlowTaskView flowTaskView = new FlowTaskView();
			flowTaskView.setId(opt.getId());
			flowTaskView.setActivityId(opt.getActivityId());
			flowTaskView.setProcessInstanceId(opt.getInstanceId());
			flowTaskView.setAnswerId(opt.getAnswerId());
			flowTaskView.setProjectId(opt.getProjectId());
			flowTaskView.setApprovalType(opt.getApprovalType());
			flowTaskView.setLatest(opt.getLatest());
			return flowTaskView;
		}).collect(Collectors.toList());
		return new PaginationResponse<>(page.getTotal(), viewList);
	}

	/**
	 * 查询"我发起的"任务列表。
	 *
	 * <p>数据链路：t_flow_instance（流程实例表）→ 组装 FlowTaskView → 上层统一
	 * 补充答案与状态 → 返回前端。</p>
	 *
	 * <p>内部逻辑：按项目分页查询全部流程实例（当前实现未按创建人过滤，即返回该项目
	 * 的所有实例），组装实例 ID、答案 ID、状态、审批阶段与时间信息。</p>
	 *
	 * @param query 查询参数
	 * @return 我发起的任务分页结果
	 */
	private PaginationResponse<FlowTaskView> getSelfCreated(FlowTaskQuery query) {
		Page<FlowInstance> page = new Page<>(query.getCurrent(), query.getPageSize());
		flowInstanceService.page(page, Wrappers.<FlowInstance>lambdaQuery()
				.eq(FlowInstance::getProjectId, query.getProjectId()).orderByDesc(FlowInstance::getCreateAt));
		List<FlowTaskView> viewList = page.getRecords().stream().map(instance -> {
			FlowTaskView flowTaskView = new FlowTaskView();
			flowTaskView.setCreateAt(instance.getCreateAt());
			flowTaskView.setApprovalStage(instance.getApprovalStage());
			flowTaskView.setStatus(instance.getStatus());
			flowTaskView.setAnswerId(instance.getAnswerId());
			flowTaskView.setProcessInstanceId(instance.getId());
			flowTaskView.setProjectId(instance.getProjectId());
			flowTaskView.setActivityId(instance.getProjectId());
			flowTaskView.setCreateAt(instance.getCreateAt());
			flowTaskView.setUpdateAt(instance.getUpdateAt());
			return flowTaskView;
		}).collect(Collectors.toList());
		return new PaginationResponse<>(page.getTotal(), viewList);
	}

	/**
	 * 按当前任务节点的字段权限过滤问卷 schema。
	 *
	 * <p>流程环节：审批表单加载。审批人打开表单时调用：先加载项目问卷结构，再按
	 * 任务节点（taskDefKey）配置的字段权限过滤（隐藏/只读/可编辑），使审批人只能
	 * 看到并编辑被授权的字段。</p>
	 *
	 * <p>数据链路：Controller → loadSchemaByPermission → ProjectService.getProject
	 * （加载问卷）+ FlowEntryNodeService.getById（加载节点权限）→ SchemaHelper
	 * 过滤 → 返回前端。</p>
	 *
	 * @param query 查询参数（projectId、taskDefKey）
	 * @return 过滤后的问卷结构；节点不存在时返回原始 schema
	 */
	@Override
	public SurveySchema loadSchemaByPermission(SchemaQuery query) {
		ProjectView projectView = projectService.getProject(query.getProjectId());
		SurveySchema schema = projectView.getSurvey();
		FlowEntryNode element = entryNodeService.getById(query.getTaskDefKey());
		if (element == null) {
			return schema;
		}
		SchemaHelper.updateSchemaByPermission(element.getFieldPermission(), schema);
		return schema;
	}

	/**
	 * 获取流程实例的审核记录（审批历史）。
	 *
	 * <p>流程环节：审批详情。完整逻辑：</p>
	 * <ol>
	 *   <li>查询该实例全部用户任务操作记录（按创建时间正序）；</li>
	 *   <li>若实例处于"审批中"：查询当前唯一子执行所在的节点，动态追加一条
	 *       "待审批（todo）"记录，并组装等待审批的用户列表（任务指派人 + 候选者）；</li>
	 *   <li>若实例处于"申请人完善中"：追加一条发起节点（starter）的待审批记录，
	 *       等待人即申请人；</li>
	 *   <li>为每条记录补充：操作人信息（auditUser）、节点名称（activityName /
	 *       newActivityName）、审批类型中文名；save 类型且无节点名时显示"申请人"。</li>
	 * </ol>
	 *
	 * @param processInstanceId 流程实例 ID
	 * @return 审核记录视图列表（含进行中的待审批节点）
	 */
	@Override
	public List<FlowOperationView> getAuditRecord(String processInstanceId) {
		List<FlowOperation> operations = flowOperationService
				.list(Wrappers.<FlowOperation>lambdaQuery().eq(FlowOperation::getInstanceId, processInstanceId)
						.eq(FlowOperation::getTaskType, FlowTaskType.userTask).orderByAsc(FlowOperation::getCreateAt));
		List<FlowOperationView> operationViews = flowOperationModelMapper.toView(operations);
		FlowInstance flowInstance = flowInstanceService.getById(processInstanceId);
		if (FlowInstanceStatus.APPROVING == flowInstance.getStatus()) {
			// 当前任务正在审批，设置审批人（查询当前唯一子执行定位当前所在节点）
			Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceId)
					.onlyChildExecutions().singleResult();
			if (execution != null) {
				FlowOperationView runningOperationView = new FlowOperationView();
				runningOperationView.setActivityId(execution.getActivityId());
				runningOperationView.setApprovalType(FlowApprovalType.TODO);
				operationViews.add(runningOperationView);
				// 汇总当前活跃任务的指派人 + 候选者作为等待审批用户
				List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId)
						.includeIdentityLinks().list();
				Set<String> waitAuditUserIds = new LinkedHashSet<>();
				tasks.forEach(t -> {
					if (t.getAssignee() != null) {
						waitAuditUserIds.add(t.getAssignee());
					}
					t.getIdentityLinks().forEach(link -> {
						waitAuditUserIds.add(link.getUserId());
					});
				});
				// 设置等待审核用户列表
				runningOperationView.setWaitAuditUserList(waitAuditUserIds.stream()
						.map(uid -> userService.loadUserById(uid)).collect(Collectors.toList()));
			}
		}
		else if (FlowInstanceStatus.SUSPENDED == flowInstance.getStatus()) {
			// 需要申请人完善：追加发起节点的待处理记录，等待人即申请人
			FlowOperationView runningOperationView = new FlowOperationView();
			runningOperationView.setActivityId(FlowConstant.STARTER_ACTIVITY_ID);
			runningOperationView.setActivityName(FlowConstant.STARTER_ACTIVITY_NAME);
			runningOperationView.setApprovalType(FlowApprovalType.TODO);
			runningOperationView.setWaitAuditUserList(
					Collections.singletonList(userService.loadUserById(flowInstance.getCreateBy())));
			operationViews.add(runningOperationView);
		}

		// 添加历史节点：补充操作人、节点名称、审批类型中文名等展示信息
		operationViews.forEach(view -> {
			if (view.getCreateBy() != null) {
				view.setAuditUser(userService.loadUserById(view.getCreateBy()));
			}
			if (view.getActivityId() != null) {
				FlowEntryNode node = entryNodeService.getById(view.getActivityId());
				if (node != null) {
					view.setActivityName(node.getName());
				}
			}
			if (view.getNewActivityId() != null) {
				if (FlowConstant.STARTER_ACTIVITY_ID.equals(view.getNewActivityId())) {
					// 回退目标是发起节点时显示"申请人"
					view.setNewActivityName(FlowConstant.STARTER_ACTIVITY_NAME);
				}
				else {
					FlowEntryNode node = entryNodeService.getById(view.getNewActivityId());
					if (node != null) {
						view.setNewActivityName(node.getName());
					}
				}
			}
			if (view.getApprovalType() != null) {
				view.setApprovalTypeName(FlowApprovalType.DICT_MAP.get(view.getApprovalType()));
			}
			// 保存类操作且无节点名时（发起保存），节点名显示为"申请人"
			if (!StringUtils.hasText(view.getActivityName()) && FlowApprovalType.SAVE.equals(view.getApprovalType())) {
				view.setActivityName("申请人");
			}
		});

		return operationViews;
	}

	/**
	 * 获取当前实例可回退的节点列表。
	 *
	 * <p>流程环节：驳回操作前置查询。委托 {@link RevertTaskHandler} 基于历史操作
	 * 记录构建节点树并取当前节点父链路，再逐个翻译节点名称后返回。</p>
	 *
	 * @param processInstanceId 流程实例 ID
	 * @return 可回退节点列表（不含当前节点）
	 */
	@Override
	public List<RevokeView> getRevertNodes(String processInstanceId) {
		return revertTaskHandler.getRevertNodes(processInstanceId).stream().map(node -> {
			RevokeView view = new RevokeView();
			view.setActivityId(node.getActivityId());
			view.setActivityName(entryNodeService.getById(node.getActivityId()).getName());
			return view;
		}).collect(Collectors.toList());
	}

	/**
	 * 获取当前用户的任务统计数量。
	 *
	 * <p>流程环节：待办中心统计。分别统计：</p>
	 * <ul>
	 *   <li>todo：当前用户作为候选者或指派人的活跃任务数（Flowable 任务表）；</li>
	 *   <li>finished：全局用户任务操作记录数（排除保存类型；当前统计口径未按用户过滤）；</li>
	 *   <li>selfCreated：当前用户发起且处于审批中的流程实例数；</li>
	 *   <li>copyTo：预留，未计算（保持 0）。</li>
	 * </ul>
	 *
	 * @return 任务统计视图
	 */
	@Override
	public FlowStaticsView statics() {
		FlowStaticsView statics = new FlowStaticsView();
		// 获取我的待办：候选人或指派人
		TaskQuery taskQuery = taskService.createTaskQuery().active();
		String userId = SecurityContextUtils.getUserId();
		taskQuery.or().taskCandidateUser(userId).taskAssignee(userId).endOr().orderByTaskCreateTime().desc();
		statics.setTodo(taskQuery.count());
		// 获取我的已办：全部用户任务操作记录（排除保存）
		statics.setFinished(flowOperationService
				.count(Wrappers.<FlowOperation>lambdaQuery().eq(FlowOperation::getTaskType, FlowTaskType.userTask)
						.ne(FlowOperation::getApprovalType, FlowApprovalType.SAVE)));
		// 获取我能发起的，任务在审批中的
		statics.setSelfCreated(flowInstanceService.count(
				Wrappers.<FlowInstance>lambdaQuery().eq(FlowInstance::getCreateBy, SecurityContextUtils.getUserId())
						.eq(FlowInstance::getStatus, FlowInstanceStatus.APPROVING)));
		// 获取抄送给我的（预留：当前返回 0）
		return statics;
	}

	/**
	 * 按部署 ID 查询最新版本的流程定义。
	 *
	 * @param deployId Flowable 部署 ID
	 * @return 该部署下的流程定义（latestVersion 语义为取该部署内的定义）
	 */
	private ProcessDefinition getProcessDefinitionByDeployId(String deployId) {
		return repositoryService.createProcessDefinitionQuery().deploymentId(deployId).latestVersion().singleResult();
	}

	/**
	 * 为任务视图补充表单答案与附件信息。
	 *
	 * <p>数据链路：FlowTaskView.answerId → AnswerService.getAnswer 加载答案 →
	 * 按节点字段权限过滤答案（隐藏字段剔除）→ 回填视图。</p>
	 *
	 * <p>内部逻辑：逐个任务：加载答案视图；取答案创建人作为 createUser（simpleMode
	 * 精简用户信息）；按节点字段权限过滤答案内容（filterAnswerByPermission）；回填
	 * 答案、附件、用户、部门列表与字段权限映射。</p>
	 *
	 * @param views 任务视图列表（被就地补充答案信息）
	 */
	private void setFlowTaskAnswer(List<FlowTaskView> views) {
		for (FlowTaskView view : views) {
			String answerId = view.getAnswerId();
			AnswerQuery answerQuery = new AnswerQuery();
			answerQuery.setId(answerId);
			AnswerView answerView = answerService.getAnswer(answerQuery);
			UserInfo createUser = userService.loadUserById(answerView.getCreateBy());
			if (createUser != null) {
				view.setCreateUser(createUser.simpleMode());
			}
			FlowEntryNode node = entryNodeService.getById(view.getActivityId());
			view.setFieldPermission(node.getFieldPermission());
			filterAnswerByPermission(answerView.getAnswer(), node.getFieldPermission());
			view.setAnswer(answerView.getAnswer());
			view.setAttachment(answerView.getAttachment());
			view.setUsers(answerView.getUsers());
			view.setDepts(answerView.getDepts());
		}
	}

	/**
	 * 设置当前任务的任务状态
	 *
	 * <p>内部逻辑：对状态尚未设置的视图（待办/已办场景），按流程实例 ID 加载
	 * t_flow_instance，回填状态、审批阶段、创建时间与更新时间。</p>
	 *
	 * @param views 任务视图列表（被就地补充状态信息）
	 */
	private void setTaskStatus(List<FlowTaskView> views) {
		views.forEach(view -> {
			if (view.getStatus() != null) {
				// 我发起的场景已设置状态，跳过
				return;
			}
			FlowInstance instance = flowInstanceService.getById(view.getProcessInstanceId());
			view.setStatus(instance.getStatus());
			view.setApprovalStage(instance.getApprovalStage());
			view.setCreateAt(instance.getCreateAt());
			view.setUpdateAt(instance.getUpdateAt());
		});
	}

	/**
	 * 保存流程节点，将 flowEntry 里面的临时节点保存到数据库。
	 *
	 * <p>内部逻辑：先把本次节点列表中已存在（部署过）的节点记录删除，再逐个落库。
	 * 注意注释中的约束：不能直接删除当前项目下的全部流程节点，因为多次部署后旧的
	 * 部署版本仍可能引用之前的节点配置，因此只清理本次涉及到的节点 ID。</p>
	 *
	 * @param flowEntry 流程定义（含待落库的 nodes 列表）
	 */
	private void saveFlowElement(FlowEntry flowEntry) {
		// 不能直接删除当前项目下面的所有流程节点，因为多次部署，旧的部署版本可能需要之前的流程节点
		entryNodeService
				.removeBatchByIds(flowEntry.getNodes().stream().map(x -> x.getId()).collect(Collectors.toList()));
		if (flowEntry.getNodes() != null) {
			flowEntry.getNodes().forEach(node -> {
				node.setProjectId(flowEntry.getProjectId());
				entryNodeService.save(node);
			});
		}
	}

	/**
	 * 按字段权限过滤表单答案（就地修改）。
	 *
	 * <p>内部逻辑：遍历答案条目，若对应题目的字段权限为 0（隐藏）则从答案中移除，
	 * 保证前端拿到的答案不包含当前节点无权查看的字段。</p>
	 *
	 * @param answer 答案映射（题目 ID → 值），被就地过滤
	 * @param fieldPermission 字段权限映射（题目 ID → 0/1/2）
	 */
	private void filterAnswerByPermission(LinkedHashMap<String, Object> answer,
			LinkedHashMap<String, Integer> fieldPermission) {
		answer.entrySet().removeIf(entry -> {
			Integer valuePermission = fieldPermission.get(entry.getKey());
			if (valuePermission == null) {
				return false;
			}
			if (valuePermission == 0) {
				// 隐藏字段：从答案中移除
				return true;
			}
			return false;
		});
	}

}
