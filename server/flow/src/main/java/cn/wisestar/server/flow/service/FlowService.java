package cn.wisestar.server.flow.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.PublicProjectView;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.flow.domain.dto.*;

import java.util.List;

/**
 * 工作流业务门面接口。
 *
 * <p>职责：定义流程模块对外暴露的全部业务能力，是 {@link FlowApi} 的唯一业务依赖：
 * 流程设计（保存/查询）、流程部署、任务查询（待办/已办/我发起的）、审批操作
 * （审批/驳回/撤回分发）、表单 schema 权限过滤、审核记录查询、回退节点查询、任务统计，
 * 以及发起问卷前的权限预检。实现见 {@code FlowServiceImpl}。</p>
 *
 * <p>所属流程环节：流程模块的总入口服务，串联"设计 → 发布 → 发起 → 流转 → 结束"全链路。</p>
 *
 * <p>被谁调用：FlowApi（HTTP 层）、问卷提交前的权限校验（beforeLaunchProcess）。</p>
 *
 * <p>依赖什么：flow 包下全部 DTO 与核心模块的通用结构（PaginationResponse、
 * SurveySchema、PublicProjectView）。</p>
 *
 * @author javahuang
 * @date 2022/1/5
 */
public interface FlowService {

	/**
	 * 启动前过滤过滤当前问卷 schema
	 *
	 * <p>流程环节：问卷发起前。用户在问卷页开始填表前调用，检查当前问卷是否绑定
	 * 流程以及当前用户是否有权发起：有权则按发起节点字段权限过滤问卷结构；无权或
	 * 未登录（需要登录）则置空 survey 并标记需要登录，阻止继续作答。</p>
	 *
	 * @param projectView 问卷视图（含 survey 结构），会被就地修改（过滤或置空）
	 */
	void beforeLaunchProcess(PublicProjectView projectView);

	/**
	 * 流程设计保存
	 *
	 * <p>流程环节：流程设计。将前端流程设计器的 BPMN XML 与节点配置保存为草稿
	 * （t_flow_entry），已存在则覆盖更新。</p>
	 *
	 * @param request 流程保存请求（bpmnXml、projectId、节点列表）
	 */
	void saveFlow(FlowEntryRequest request);

	/**
	 * 部署设计部署
	 *
	 * <p>流程环节：流程发布。解析 BPMN XML 并部署到 Flowable 引擎，生成流程定义；
	 * 将节点配置刷入 t_flow_entry_node；发布新版本记录（t_flow_entry_publish）；
	 * 流程定义状态置为已发布。</p>
	 *
	 * @param projectId 问卷/项目 ID
	 */
	void deploy(String projectId);

	/**
	 * 完成任务
	 *
	 * <p>流程环节：审批处理。根据请求的 type 从 Spring 容器获取对应 TaskHandler
	 * Bean（Bean 名 = type + "TaskHandler"）并执行审批流转（同意/拒绝/驳回/撤回/保存）。</p>
	 *
	 * @param request 审批请求（type、taskId、processInstanceId、答案、意见等）
	 */
	void approvalTask(ApprovalTaskRequest request);

	/**
	 * 获取流程信息
	 *
	 * <p>流程环节：流程设计回显。返回问卷绑定的流程定义（XML 与节点配置）。</p>
	 *
	 * @param projectId 问卷/项目 ID
	 * @return 流程定义视图（未配置时返回仅含 projectId 的空视图）
	 */
	FlowEntryView getFlowEntry(String projectId);

	/**
	 * 获取用户任务列表
	 *
	 * <p>流程环节：待办中心。按查询类型（待办/已办/我发起的）分页返回任务列表，
	 * 并补充表单答案（按字段权限过滤）、任务状态与审批阶段。</p>
	 *
	 * @param query 查询参数（type、projectId、分页等）
	 * @return 任务视图分页结果
	 */
	PaginationResponse<FlowTaskView> getFlowTasks(FlowTaskQuery query);

	/**
	 * 查询当前任务节点根据权限过滤之后的 schema
	 *
	 * <p>流程环节：审批表单查看。加载问卷结构并按当前任务节点的字段权限过滤
	 * （隐藏/只读/可编辑），保证审批人只能看到被授权的字段。</p>
	 *
	 * @param query 查询参数（projectId、taskDefKey）
	 * @return 过滤后的问卷结构
	 */
	SurveySchema loadSchemaByPermission(SchemaQuery query);

	/**
	 * 获取当前流程实例的审核记录
	 *
	 * <p>流程环节：审批详情。按时间正序返回实例的全部用户任务操作记录，补充节点
	 * 名称、审批人信息；若实例处于审批中/完善中，末尾追加一条"待审批"进行中节点。</p>
	 *
	 * @param processInstanceId 流程实例 ID
	 * @return 审核记录视图列表
	 */
	List<FlowOperationView> getAuditRecord(String processInstanceId);

	/**
	 * 获取可以回退的任务节点
	 *
	 * <p>流程环节：驳回操作前置查询。基于历史操作记录构建的节点树，返回当前实例
	 * 可回退（驳回到）的节点列表（不含当前节点）。</p>
	 *
	 * @param processInstanceId 流程实例 ID
	 * @return 可回退节点列表
	 */
	List<RevokeView> getRevertNodes(String processInstanceId);

	/**
	 * @return 当前用户任务统计信息
	 *
	 * <p>流程环节：待办中心统计。返回当前用户的待办、已办、我发起的任务数量。</p>
	 */
	FlowStaticsView statics();

}
