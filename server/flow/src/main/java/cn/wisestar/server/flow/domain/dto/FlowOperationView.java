package cn.wisestar.server.flow.domain.dto;

import cn.wisestar.server.domain.dto.UserInfo;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 审核（操作）记录展示视图 DTO。
 *
 * <p>职责：向前端审批详情页返回一条完整的历史操作记录（谁在哪个节点做了什么操作、
 * 意见是什么、当前等待谁来审批、抄送给了谁等），由 MapStruct 转换器
 * （{@code FlowOperationModelMapper}）从实体 {@code FlowOperation} 转换后，
 * 再经 {@code FlowServiceImpl.getAuditRecord} 补充节点名称、审批人、待审批人、
 * 抄送人等信息。</p>
 *
 * <p>所属流程环节：审批详情/审核记录展示环节。</p>
 *
 * <p>被谁调用：FlowServiceImpl.getAuditRecord（组装返回前端）。</p>
 *
 * <p>依赖什么：{@link UserInfo}（用户信息，含审批人/待审批人/抄送人）。</p>
 *
 * @author javahuang
 * @date 2022/1/13
 */
@Data
public class FlowOperationView {

	/** 操作记录 ID（t_flow_operation.id） */
	private String id;

	/** 操作所在节点 ID（BPMN activityId） */
	private String activityId;

	/** 操作目标节点 ID（如驳回、撤回操作指向的节点 activityId） */
	private String newActivityId;

	/**
	 * 节点名称：操作所在节点的中文名（由 activityId 从节点表翻译而来）
	 */
	private String activityName;

	/** 目标节点名称：newActivityId 对应的中文名 */
	private String newActivityName;

	/** 审批意见/备注：操作人填写的批注文字 */
	private String comment;

	/**
	 * 创建时间：操作发生时间，序列化为 yyyy-MM-dd HH:mm 格式
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")
	private Date createAt;

	/** 操作人 ID（t_flow_operation.create_by） */
	private String createBy;

	/** 操作人信息：由 createBy 加载的用户详情（姓名、头像等） */
	private UserInfo auditUser;

	/** 审批类型码（save/agree/refuse/rollback/todo 等），见 {@link cn.wisestar.server.flow.constant.FlowApprovalType} */
	private String approvalType;

	/** 审批类型中文名：由 approvalType 经 DICT_MAP 翻译 */
	private String approvalTypeName;

	/**
	 * 待审批用户：当前等待审批的用户列表（进行中的节点动态组装，含任务指派人与候选者）
	 */
	private List<UserInfo> waitAuditUserList;

	/**
	 * 抄送用户：本节点的抄送人员列表（预留字段）
	 */
	private List<UserInfo> ccUserList;

}
