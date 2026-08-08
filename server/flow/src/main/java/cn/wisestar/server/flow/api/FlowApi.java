package cn.wisestar.server.flow.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.flow.domain.dto.*;
import cn.wisestar.server.flow.service.FlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流（审批流程）对外 HTTP 接口层（Controller）。
 *
 * <p>职责：暴露 AI 自习室系统中"问卷/表单审批流程"相关的 REST 接口，包括流程设计保存、
 * 流程部署发布、任务查询、审批操作、审核记录查询、表单 schema 权限过滤、回退节点查询、
 * 任务统计等。所有请求统一以 <code>${api.prefix}/workflow</code> 为前缀。</p>
 *
 * <p>所属流程环节：本类是流程模块的入口，前端页面（流程设计器、待办中心、审批详情页）通过
 * 这些接口与后端交互；不直接操作数据库，全部逻辑委托给 {@link FlowService} 处理。</p>
 *
 * <p>被谁调用：前端（浏览器）通过 HTTP 调用；无其它服务依赖本类。</p>
 *
 * <p>依赖什么：{@link FlowService}（核心业务门面）、{@link PaginationResponse}（统一分页返回体）、
 * {@link SurveySchema}（问卷结构定义）、以及 flow 包下的各类 DTO（请求/响应参数对象）。</p>
 *
 * @author javahuang
 * @date 2022/1/5
 */
@RestController
@RequestMapping("${api.prefix}/workflow")
@RequiredArgsConstructor
public class FlowApi {

	/** 工作流业务门面：所有接口实现均委托给该服务，本类不做任何业务逻辑 */
	private final FlowService flowService;

	/**
	 * 获取某个问卷（项目）绑定的流程定义信息。
	 *
	 * <p>流程环节：流程设计环节的"读取"。前端打开流程设计器时调用，用于回显已保存的
	 * BPMN XML 和节点配置；若该问卷尚未配置流程，返回一个仅含 projectId 的空视图。</p>
	 *
	 * @param projectId 问卷/项目 ID（即流程定义 key）
	 * @return 流程定义视图 {@link FlowEntryView}（含 bpmnXml 与节点列表）
	 */
	@GetMapping("/getFlow")
	public FlowEntryView getFlowEntry(String projectId) {
		return flowService.getFlowEntry(projectId);
	}

	/**
	 * 保存流程设计（草稿态）。
	 *
	 * <p>流程环节：流程设计环节的"写入"。前端流程设计器点击保存时调用，将 BPMN XML
	 * 与节点配置（含字段权限、审批人、节点设置等）落库到 t_flow_entry 表；此时仅保存
	 * 草稿，不会真正部署到 Flowable 引擎。</p>
	 *
	 * @param request 流程保存请求体（bpmnXml + projectId + 节点列表）
	 */
	@PostMapping("/saveFlow")
	public void saveFlow(@RequestBody FlowEntryRequest request) {
		flowService.saveFlow(request);
	}

	/**
	 * 部署流程设计（发布为新版本）。
	 *
	 * <p>流程环节：流程设计环节的"发布"。前端点击发布时调用，将草稿态的 BPMN XML 转换为
	 * BpmnModel 并部署到 Flowable 引擎，同时将节点配置刷入 t_flow_entry_node 表、
	 * 生成 t_flow_entry_publish 发布版本记录，并把流程定义标记为已发布。</p>
	 *
	 * @param projectId 问卷/项目 ID
	 */
	@PostMapping("/deploy")
	public void deploy(String projectId) {
		flowService.deploy(projectId);
	}

	/**
	 * 查询某个流程实例的完整审核（操作）记录。
	 *
	 * <p>流程环节：审批详情环节。前端审批详情页展示"审批历史/流转记录"时调用，返回
	 * 按时间正序排列的用户任务操作记录，并补充节点名称、审批人信息、待审批人列表等展示字段。</p>
	 *
	 * @param processInstanceId Flowable 流程实例 ID（对应 t_flow_instance.id）
	 * @return 审核记录视图列表 {@link FlowOperationView}
	 */
	@GetMapping("/getAuditRecord")
	public List<FlowOperationView> getAuditRecord(String processInstanceId) {
		return flowService.getAuditRecord(processInstanceId);
	}

	// 获取当前用户的任务列表（待办/已办/我发起的）
	/**
	 * 分页获取当前用户的任务列表。
	 *
	 * <p>流程环节：待办中心环节。前端"待办/已办/我发起的"页面调用，根据查询类型
	 * （{@code todo}、{@code finished}、{@code selfCreated}）从 Flowable 任务表或
	 * 本模块的 t_flow_operation / t_flow_instance 表分别组装分页数据，并附带表单答案、
	 * 字段权限过滤后的可见内容与任务状态。</p>
	 *
	 * @param query 任务查询参数（type 查询类型、projectId、status、分页信息）
	 * @return 任务视图分页结果 {@link PaginationResponse}<{@link FlowTaskView}>
	 */
	@GetMapping("/getFlowTasks")
	public PaginationResponse<FlowTaskView> getFlowTasks(FlowTaskQuery query) {
		return flowService.getFlowTasks(query);
	}

	/**
	 * 加载问卷 schema（表单结构），并按当前任务节点的字段权限过滤。
	 *
	 * <p>流程环节：审批处理环节的前置步骤。审批人打开审批详情页时调用，返回当前任务节点
	 * 配置的字段权限（隐藏/只读/可编辑）过滤之后的问卷结构，保证审批人只能看到被授权的字段。</p>
	 *
	 * @param query 查询参数（projectId 问卷 ID、taskDefKey 当前任务节点 ID）
	 * @return 过滤后的问卷结构 {@link SurveySchema}
	 */
	@GetMapping("/loadSchema")
	public SurveySchema loadSchema(SchemaQuery query) {
		return flowService.loadSchemaByPermission(query);
	}

	/**
	 * 获取任务信息（占位接口）。
	 *
	 * <p>当前未实现任何逻辑：方法体为空，保留作为扩展点。若后续需要在前端直接拉取
	 * 单个任务的完整信息（而非通过 getFlowTasks / getAuditRecord 组合获取），可在
	 * 此接口补充实现。</p>
	 */
	@GetMapping("/getTaskInfo")
	public void getTaskInfo() {

	}

	/**
	 * 查询当前流程实例可以回退（驳回）的历史节点列表。
	 *
	 * <p>流程环节：审批处理环节的"驳回到指定节点"前置查询。前端"驳回"对话框需要列出
	 * 可选节点时调用，返回基于历史操作记录构建的节点链路中可回退的节点（不含当前节点）。</p>
	 *
	 * @param processInstanceId Flowable 流程实例 ID
	 * @return 可回退节点视图列表 {@link RevokeView}（节点 ID + 节点名称）
	 */
	@GetMapping("/getRevertNodes")
	public List<RevokeView> getRevertNodes(String processInstanceId) {
		return flowService.getRevertNodes(processInstanceId);
	}

	/**
	 * 审批任务：处理用户对任务的一次操作（保存/同意/拒绝/驳回/撤回等）。
	 *
	 * <p>流程环节：审批处理环节的核心入口。前端在任何审批节点点击"同意/拒绝/驳回/
	 * 保存/撤回"等按钮时统一调用本接口，后端根据 {@code type} 字段分发到对应的
	 * TaskHandler（如 agreeTaskHandler、refuseTaskHandler）执行任务流转，并落库
	 * 操作记录（t_flow_operation）与更新表单答案。</p>
	 *
	 * @param request 审批请求体（type 审批类型、taskId、processInstanceId、答案、备注等）
	 */
	@PostMapping("/approvalTask")
	public void approvalTask(@RequestBody ApprovalTaskRequest request) {
		flowService.approvalTask(request);
	}

	/**
	 * 获取当前用户的待办/已办/我发起任务的统计数量。
	 *
	 * <p>流程环节：待办中心首页统计。前端工作台/待办页面顶部展示各类任务数量时调用，
	 * 返回 todo（我的待办）、finished（已办）、copyTo（抄送）、selfCreated（我发起的）
	 * 四个维度的计数。</p>
	 *
	 * @return 任务统计视图 {@link FlowStaticsView}
	 */
	@GetMapping("/statics")
	public FlowStaticsView statics() {
		return flowService.statics();
	}

}
