package cn.wisestar.server.flow.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * 流程操作记录实体（对应表 t_flow_operation）。
 *
 * <p>职责：记录流程实例每次操作（发起保存、同意、拒绝、驳回、撤回等）的详细信息：
 * 操作人、操作类型、所在节点、目标节点、审批意见、提交的表单答案、委托对象等。
 * 是"已办事项"列表与"审核记录（审批历史）"的数据来源，并通过 latest 字段
 * 标记每个实例的最新一条操作（仅最新操作人可执行撤回）。</p>
 *
 * <p>所属流程环节：审批处理环节（AbstractTaskHandler.saveOperation 落库）与
 * 审批详情/已办列表展示环节（getAuditRecord / getFinished 查询）。</p>
 *
 * <p>被谁调用：FlowOperationService 及其实现（CRUD）、AbstractTaskHandler
 * （保存操作记录、查询操作历史、构建节点树）、FlowServiceImpl（已办列表、审核记录、
 * 已办统计）、RevertTaskHandler（撤回权限校验）。</p>
 *
 * <p>依赖什么：{@link JacksonTypeHandler}（answer 列 JSON 转换，存储节点提交的答案快照）。</p>
 *
 * @TableName t_flow_operation
 */
@TableName(value = "t_flow_operation", autoResultMap = true)
@Data
public class FlowOperation implements Serializable {

	/**
	 * 主键ID
	 */
	@TableId(value = "id")
	private String id;

	/**
	 * 流程实例Id：所属流程实例（对应 t_flow_instance.id）
	 */
	@TableField(value = "instance_id")
	private String instanceId;

	/**
	 * 项目Id：所属问卷/项目 ID（即流程定义 key）
	 */
	@TableField(value = "project_id")
	private String projectId;

	/**
	 * xml 节点Id：操作所在节点的 BPMN activityId
	 */
	@TableField(value = "activity_id")
	private String activityId;

	/**
	 * 新的任务节点 id：驳回/撤回操作的目标节点 activityId（同意操作时为空）
	 */
	@TableField(value = "new_activity_id")
	private String newActivityId;

	/**
	 * 任务Id：Flowable 运行时任务 ID（本次操作处理的待办任务）
	 */
	@TableField(value = "task_id")
	private String taskId;

	/**
	 * 当前答案ID：本次操作对应的表单答案主键
	 */
	@TableField(value = "answer_id")
	private String answerId;

	/**
	 * 任务名称：操作所在节点的名称
	 */
	@TableField(value = "task_name")
	private String taskName;

	/**
	 * 任务类型：任务类型码（当前为 userTask），见 {@link cn.wisestar.server.flow.constant.FlowTaskType}
	 */
	@TableField(value = "task_type")
	private Integer taskType;

	/**
	 * 审批类型：本次操作类型（save/agree/refuse/rollback/revert），见 {@link cn.wisestar.server.flow.constant.FlowApprovalType}
	 */
	@TableField(value = "approval_type")
	private String approvalType;

	/**
	 * 注释内容：审批人填写的审批意见/备注
	 */
	@TableField(value = "comment")
	private String comment;

	/**
	 * 委托指定人：转交/指派操作的目标用户 ID（预留字段）
	 */
	@TableField(value = "delegate_assignee")
	private String delegateAssignee;

	/**
	 * 当前节点答案：本次操作提交的表单答案快照（题目 ID → 值），JSON 存储，
	 * 用于审核记录回显审批人当时填写的内容
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, value = "answer")
	private LinkedHashMap<String, Object> answer;

	/**
	 * 是否是最新的一条操作记录：每个实例仅一条 latest=true，
	 * 用于"仅最新操作人可撤回"与已办列表去重（非表字段，查询时由 updateOperationLatest 逻辑维护）
	 */
	private Boolean latest;

	/**
	 * 创建时间：操作时间
	 */
	@TableField(value = "create_at")
	private Date createAt;

	/**
	 * 创建人：操作人用户 ID
	 */
	@TableField(value = "create_by")
	private String createBy;

	/**
	 * 更新时间
	 */
	@TableField(value = "update_at")
	private Date updateAt;

	/**
	 * 更新人
	 */
	@TableField(value = "update_by")
	private String updateBy;

	/** 序列化版本号（不参与表字段映射） */
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;

}
