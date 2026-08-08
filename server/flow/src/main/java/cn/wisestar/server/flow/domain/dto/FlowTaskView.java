package cn.wisestar.server.flow.domain.dto;

import cn.wisestar.server.domain.dto.DeptView;
import cn.wisestar.server.domain.dto.FileView;
import cn.wisestar.server.domain.dto.UserInfo;
import lombok.Data;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 任务列表展示视图 DTO。
 *
 * <p>职责：向前端待办中心/任务列表返回单条任务的完整展示数据：任务定位信息
 * （任务 ID、流程实例 ID、答案 ID）、当前状态与审批阶段、按字段权限过滤后的表单
 * 答案与附件、创建人/审批人信息等。由 {@code FlowServiceImpl.getFlowTasks} 的
 * 三个查询分支组装，并经 setFlowTaskAnswer / setTaskStatus 补充答案、权限与状态。</p>
 *
 * <p>所属流程环节：待办中心/任务列表展示环节。</p>
 *
 * <p>被谁调用：FlowServiceImpl.getFlowTasks（组装返回前端）。</p>
 *
 * <p>依赖什么：{@link UserInfo}（用户信息）、{@link FileView}（附件）、
 * {@link DeptView}（部门信息，复用问卷模块的数据结构）。</p>
 *
 * @author javahuang
 * @date 2022/1/5
 */
@Data
public class FlowTaskView {

	/** 任务标识：待办为 Flowable 任务 ID，已办为操作记录 ID（t_flow_operation.id） */
	private String id;

	/** 项目 ID：所属问卷/项目 ID（流程定义 key） */
	private String projectId;

	/** 任务状态：流程实例状态（0 已提交 / 1 审批中 / 2 已拒绝 / 3 已结束 / 6 完善中） */
	private Integer status;

	/** 审批阶段：当前所处审批节点的中文名（如"部门主管审批"） */
	private String approvalStage;

	/** 审批类型：最近一次操作的类型码（save/agree/refuse 等），见 {@link cn.wisestar.server.flow.constant.FlowApprovalType} */
	private String approvalType;

	/** 任务定义 key：当前任务的 BPMN 节点 activityId */
	private String activityId;

	/** 创建时间：任务/实例的创建时间 */
	private Date createAt;

	/** 流程更新时间：流程实例最近更新时间 */
	private Date updateAt;

	/** 创建人信息：申请人（由答案的 createBy 加载的用户详情） */
	private UserInfo createUser;

	/** 流程实例 ID：对应 t_flow_instance.id */
	private String processInstanceId;

	/** 答案 ID：对应表单答案主键（t_answer.id） */
	private String answerId;

	/**
	 * 字段权限：当前任务节点的题目权限映射（题目 ID → 0 隐藏 / 1 只读 / 2 可编辑），
	 * 前端据此控制答案展示与编辑
	 */
	LinkedHashMap<String, Integer> fieldPermission;

	/** 当前实例最新的操作记录：已办列表中标识该操作是否为该实例的最新操作 */
	private Boolean latest;

	/** 表单答案：按字段权限过滤后的答案内容（题目 ID → 值） */
	private LinkedHashMap<String, Object> answer;

	/** 附件列表：表单答案的附件 */
	private List<FileView> attachment;

	/** 相关用户列表：答案中的用户信息 */
	private List<UserInfo> users;

	/** 相关部门列表：答案中的部门信息 */
	private List<DeptView> depts;

}
