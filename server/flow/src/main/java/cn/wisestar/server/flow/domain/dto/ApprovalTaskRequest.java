package cn.wisestar.server.flow.domain.dto;

import cn.wisestar.server.domain.dto.AnswerRequest;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 审批任务请求 DTO。
 *
 * <p>职责：前端调用 <code>POST /workflow/approvalTask</code> 时提交的请求体，
 * 聚合了审批操作所需的全部参数：审批类型（save/agree/refuse/rollback/revert）、
 * 任务与流程实例定位信息、当前节点的表单答案、审批意见、附件、委托/驳回目标节点等。
 * 后端 {@code FlowServiceImpl.approvalTask} 依据 {@code type} 将本对象分发给对应
 * TaskHandler 处理。</p>
 *
 * <p>所属流程环节：审批处理环节（发起、同意、拒绝、驳回、撤回、转交等全部操作的统一入参）。</p>
 *
 * <p>被谁调用：FlowApi.approvalTask（HTTP 反序列化）、FlowServiceImpl.approvalTask、
 * 各 TaskHandler（AbstractTaskHandler 及其子类）。</p>
 *
 * <p>依赖什么：{@link AnswerRequest}（附件/答案更新请求，复用问卷模块的答案结构）。</p>
 *
 * @author javahuang
 * @date 2022/1/5
 */
@Data
public class ApprovalTaskRequest {

	/** 当前流程表单答案：本次操作提交的题目值（key 为题目 ID，value 为答案），用于合并更新表单答案 */
	private LinkedHashMap answer;

	/** 审批意见/备注：同意、拒绝、驳回等操作时填写的批注文字，写入操作记录 */
	private String comment;

	/** 附件列表：本次操作携带的附件（复用问卷答案的附件结构） */
	private List<AnswerRequest> attachment;

	/** 问卷/项目 ID：即流程定义 key，用于定位流程定义与相关数据 */
	private String projectId;

	/** 答案id：问卷表单答案主键，对应 t_answer 表记录，也是流程变量 answerId */
	private String answerId;

	/** 流程委托：指派/转交操作的目标用户 ID（预留字段） */
	private String assignee;

	/** 当前任务节点 ID（BPMN activityId）：本次操作所在节点的定义 key */
	private String activityId;

	/** 驳回到指定节点：rollback（驳回）操作时目标节点的 activityId，回退到发起节点时值为项目 ID 或 starter */
	private String newActivityId;

	/** 操作记录 ID：撤回（revert）操作时用于校验"仅最近操作人可撤回"（与任务 ID 语义复用） */
	private String id;

	/** 任务id：Flowable 运行时任务 ID，同意/驳回等操作通过它定位并完成待办任务 */
	private String taskId;

	/** 流程实例 ID：Flowable 流程实例 ID，对应 t_flow_instance.id */
	private String processInstanceId;

	/** 审批类型：save/agree/refuse/rollback/revert 等，见 {@link cn.wisestar.server.flow.constant.FlowApprovalType} */
	private String type;

}
