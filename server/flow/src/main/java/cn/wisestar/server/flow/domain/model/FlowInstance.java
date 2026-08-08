package cn.wisestar.server.flow.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程实例实体（对应表 t_flow_instance）。
 *
 * <p>职责：记录一次审批流程的运行实例：申请人、答案、当前状态、当前审批阶段等。
 * 实例 ID 与 Flowable 流程实例 ID 完全一致（ProcessStartedListener 创建时直接使用
 * 引擎的 processInstanceId），用于业务侧（任务列表、统计、审核记录）与引擎侧关联。</p>
 *
 * <p>所属流程环节：贯穿整个流程生命周期——发起时创建（ProcessStartedListener），
 * 流转中由各监听器维护状态（审批中/完善中/已拒绝/已结束），结束态由引擎事件同步。</p>
 *
 * <p>被谁调用：FlowInstanceService 及其实现（CRUD）、ProcessStartedListener
 * （创建实例）、ActivityStartedListener / ProcessCompletedListener /
 * ProcessCancelledListener / ProcessSuspendedListener（状态同步）、
 * FlowServiceImpl（我发起的列表/统计/审核记录状态判断）。</p>
 *
 * <p>依赖什么：无（纯实体，无特殊类型处理器）。</p>
 *
 * @TableName t_flow_instance
 */
@TableName(value = "t_flow_instance", autoResultMap = true)
@Data
public class FlowInstance implements Serializable {

	/**
	 * 流程实例ID：与 Flowable 流程实例 ID 一致，作为业务与引擎之间的关联键
	 */
	@TableId(value = "id")
	private String id;

	/**
	 * 项目id：所属问卷/项目 ID（即流程定义 key）
	 */
	@TableField(value = "project_id")
	private String projectId;

	/**
	 * 答案id：本次申请对应的表单答案主键（t_answer.id）
	 */
	@TableField(value = "answer_id")
	private String answerId;

	/**
	 * 当前状态：流程实例生命周期状态，见 {@link cn.wisestar.server.flow.constant.FlowInstanceStatus}
	 */
	@TableField(value = "status")
	private Integer status;

	/**
	 * 当前所处的审批阶段，审批节点名称：如"部门主管审批"、"已结束"、"已拒绝"等
	 */
	@TableField(value = "approval_stage")
	private String approvalStage;

	/**
	 * 创建时间
	 */
	@TableField(value = "create_at")
	private Date createAt;

	/**
	 * 创建人：申请人（流程发起人）用户 ID
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
