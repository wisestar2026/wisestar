package cn.wisestar.server.flow.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 审批类型常量类。
 *
 * <p>职责：定义流程审批/流转中所有操作类型的标识字符串，是
 * {@code ApprovalTaskRequest.type} 字段的取值字典。前端审批按钮触发的每次操作
 * 都会携带一个 type，后端 {@code FlowServiceImpl.approvalTask} 依据该值
 * 通过 Spring 容器按 Bean 名称（type + "TaskHandler"）分发到对应的
 * TaskHandler 执行流转（如 agreeTaskHandler、refuseTaskHandler 等）。</p>
 *
 * <p>所属流程环节：审批处理环节的核心类型字典，同时被审核记录展示
 * （{@code DICT_MAP} 用于把类型码翻译成中文名）所使用。</p>
 *
 * <p>被谁调用：FlowApi（请求参数 type）、FlowServiceImpl（handler 分发、审核记录
 * 类型名翻译）、AbstractTaskHandler 及其子类（操作记录 approvalType 落库）。</p>
 *
 * <p>依赖什么：无（纯常量类，仅依赖 JDK 集合）。</p>
 *
 * @author javahuang
 * @date 2022/1/6
 */
public final class FlowApprovalType {

	/**
	 * 保存。发起人首次提交或再次保存表单，会启动（或重新激活）流程实例并走到第一个审批节点。
	 */
	public static final String SAVE = "save";

	/**
	 * 同意。当前审批人同意，任务完成并流转到下一节点；若已是最后节点则流程结束。
	 */
	public static final String AGREE = "agree";

	/**
	 * 拒绝，流程直接结束。删除流程实例，由 {@link cn.wisestar.server.flow.listener.ProcessCancelledListener}
	 * 将实例状态置为"已拒绝"。
	 */
	public static final String REFUSE = "refuse";

	/**
	 * 驳回到之前的某个节点。当前审批人主动将任务回退到指定（或上一）审批节点。
	 */
	public static final String ROLLBACK = "rollback";

	/**
	 * 撤销，发起人撤销任务。申请人撤回"已提交但尚未被审批"的待办任务。
	 */
	public static final String REVERT = "revert";

	/**
	 * 指派。将当前待办任务委托/转交给其他用户处理。
	 */
	public static final String TRANSFER = "transfer";

	/**
	 * 多实例会签。流程定义中的会签（多实例）环节，同时生成多个子任务。
	 */
	public static final String MULTI_SIGN = "multi_sign";

	/**
	 * 会签同意。某个会签参与者同意。
	 */
	public static final String MULTI_AGREE = "multi_agree";

	/**
	 * 会签拒绝。某个会签参与者拒绝。
	 */
	public static final String MULTI_REFUSE = "multi_refuse";

	/**
	 * 会签弃权。某个会签参与者弃权。
	 */
	public static final String MULTI_ABSTAIN = "multi_abstain";

	/**
	 * 多实例加签。在会签环节额外追加审批人。
	 */
	public static final String MULTI_CONSIGN = "multi_consign";

	/**
	 * 中止。提前终止当前流程实例。
	 */
	public static final String STOP = "stop";

	/**
	 * 待办。非真实操作类型，仅用于审核记录中标识"当前等待审批"的进行中节点。
	 */
	public static final String TODO = "todo";

	/** 类型码 → 中文名称映射，用于审核记录、前端展示时把审批类型翻译成中文 */
	public static final Map<Object, String> DICT_MAP = new HashMap<>(2);
	static {
		DICT_MAP.put(SAVE, "保存");
		DICT_MAP.put(AGREE, "同意");
		DICT_MAP.put(REFUSE, "拒绝");
		DICT_MAP.put(TRANSFER, "指派");
		DICT_MAP.put(MULTI_SIGN, "多实例会签");
		DICT_MAP.put(MULTI_AGREE, "会签同意");
		DICT_MAP.put(MULTI_REFUSE, "会签拒绝");
		DICT_MAP.put(MULTI_ABSTAIN, "会签弃权");
		DICT_MAP.put(MULTI_CONSIGN, "多实例加签");
		DICT_MAP.put(STOP, "中止");
		DICT_MAP.put(TODO, "待处理");
	}

}
