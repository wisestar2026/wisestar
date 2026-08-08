package cn.wisestar.server.flow.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程实例状态（t_flow_instance.status）常量。
 *
 * <p>职责：定义流程实例的完整生命周期状态，并维护状态码 → 中文名称的字典
 * （{@link #getDictStatus(int)}）。流程实例状态由全局事件监听器在引擎事件发生时
 * 自动同步维护，也用于"我发起的"任务列表的状态展示与统计。</p>
 *
 * <p>状态流转总览（流程从哪开始、如何流转、到哪结束）：</p>
 * <ul>
 *   <li>发起（SaveTaskHandler 启动流程实例）→ {@link ProcessStartedListener} 置为 APPROVING；</li>
 *   <li>每次进入审批用户任务节点 → {@link ActivityStartedListener} 置为 APPROVING 并更新审批阶段；</li>
 *   <li>最后节点同意、流程正常结束 → {@link ProcessCompletedListener} 置为 FINISHED；</li>
 *   <li>审批人拒绝（删除流程实例）→ {@link ProcessCancelledListener} 置为 REFUSED；</li>
 *   <li>发起人撤回并回退到发起节点 → 实例被挂起（suspend），{@link ProcessSuspendedListener} 置为 SUSPENDED（申请人完善中）；</li>
 *   <li>申请人完善后再次提交 → SaveTaskHandler 重新激活实例，状态回到审批中。</li>
 * </ul>
 *
 * <p>被谁调用：5 个流程监听器、FlowServiceImpl（任务列表/统计/审核记录）、前端状态展示。</p>
 *
 * <p>依赖什么：无（纯常量类，仅依赖 JDK 集合）。</p>
 *
 * @author javahuang
 * @date 2022/1/6
 */
public final class FlowInstanceStatus {

	/**
	 * 已提交。申请人的表单已提交（历史兼容状态，当前流程实例主要使用审批中状态）。
	 */
	public static final int SUBMITTED = 0;

	/**
	 * 审批中。流程实例已启动，当前正处于某个审批节点等待处理，也是"我发起的"统计口径。
	 */
	public static final int APPROVING = 1;

	/**
	 * 被拒绝。审批人拒绝后流程实例被删除，此状态由 PROCESS_CANCELLED 事件同步。
	 */
	public static final int REFUSED = 2;

	/**
	 * 已结束。流程走完最后一个节点正常完成，此状态由 PROCESS_COMPLETED 事件同步。
	 */
	public static final int FINISHED = 3;

	/**
	 * 提前停止。流程被主动中止（预留状态，当前主要通过删除实例实现拒绝）。
	 */
	public static final int STOPPED = 4;

	/**
	 * 已取消。流程被取消（预留状态）。
	 */
	public static final int CANCELLED = 5;

	/**
	 * 申请人完善中。发起人撤回任务后流程实例被挂起，等待申请人完善表单后重新提交，
	 * 此状态由 ENTITY_SUSPENDED / ENTITY_ACTIVATED 事件同步。
	 */
	public static final int SUSPENDED = 6;

	/** 状态码 → 中文名称映射，供"我发起的"列表、审批阶段展示使用 */
	private static final Map<Object, String> DICT_MAP = new HashMap<>(2);
	static {
		DICT_MAP.put(SUBMITTED, "已提交");
		DICT_MAP.put(APPROVING, "审批中");
		DICT_MAP.put(REFUSED, "已拒绝");
		DICT_MAP.put(FINISHED, "已结束");
		DICT_MAP.put(STOPPED, "提前停止");
		DICT_MAP.put(CANCELLED, "已取消");
		DICT_MAP.put(SUSPENDED, "申请人完善中");
	}

	/**
	 * 将状态码翻译成中文名称。
	 *
	 * @param status 状态码（{@code FlowInstanceStatus} 常量之一）
	 * @return 中文状态名；未知状态码返回 null
	 */
	public static String getDictStatus(int status) {
		return DICT_MAP.get(status);
	}

}
