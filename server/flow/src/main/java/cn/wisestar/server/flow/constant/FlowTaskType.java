package cn.wisestar.server.flow.constant;

/**
 * 流程任务类型常量。
 *
 * <p>职责：定义流程中各类任务节点的类型标识，主要记录在 t_flow_operation.task_type
 * （本次操作所属的任务类型）。当前业务中"用户任务"（userTask）是核心，抄送、
 * 邮件、短信、HTTP 等任务类型为流程设计器预留能力。</p>
 *
 * <p>所属流程环节：审批处理环节。保存操作记录、查询已办列表、统计已办数量时，
 * 通过该类型区分"人工审批操作"与其它自动任务。</p>
 *
 * <p>被谁调用：AbstractTaskHandler（操作记录落库）、FlowServiceImpl（已办列表、
 * 已办统计、审核记录过滤）。</p>
 *
 * <p>依赖什么：无（纯常量类）。</p>
 *
 * @author javahuang
 * @date 2022/1/6
 */
public final class FlowTaskType {

	/** 发起任务：流程发起节点（申请人提交表单） */
	public static final int starter = 1;

	/** 用户任务：人工审批节点，本模块最核心的任务类型 */
	public static final int userTask = 2;

	/** 抄送：抄送节点，仅知会指定用户，不参与审批（预留） */
	public static final int copyTo = 3;

	/** 邮件任务：自动发送邮件的节点（预留） */
	public static final int mail = 4;

	/** 短信任务：自动发送短信的节点（预留） */
	public static final int sms = 5;

	/** api调用任务：自动调用外部 HTTP 接口的节点（预留） */
	public static final int http = 6;

}
