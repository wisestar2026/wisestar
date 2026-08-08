package cn.wisestar.server.flow.constant;

/**
 * 工作流通用常量。
 *
 * <p>职责：集中定义流程运转过程中使用的关键字符串常量，包括流程变量名
 * （answerId、发起人 ID）与发起节点的 ID / 名称。这些常量被流程发起
 * （{@code SaveTaskHandler}）、流程实例同步（{@code ProcessStartedListener}）、
 * 审核记录展示（{@code FlowServiceImpl.getAuditRecord}）等环节共享，避免魔法字符串散落各处。</p>
 *
 * <p>所属流程环节：贯穿"发起 → 流转 → 展示"全过程，是流程变量与引擎节点之间的约定。</p>
 *
 * <p>被谁调用：SaveTaskHandler、ProcessStartedListener、FlowServiceImpl、
 * AbstractTaskHandler 等。</p>
 *
 * <p>依赖什么：无（纯常量类）。</p>
 *
 * @author javahuang
 * @date 2022/1/9
 */
public class FlowConstant {

	/**
	 * 流程变量：问卷答案 ID。
	 * 发起流程时放入流程变量，之后在查询任务、加载答案、更新答案等环节从流程变量中取出。
	 */
	public final static String VARIABLE_ANSWER_KEY = "answerId";

	/**
	 * 流程变量：发起用户 ID。
	 * 发起流程时记录申请人，用于后续"我发起的"任务查询、申请人完善等场景。
	 */
	public final static String VARIABLE_STARTER_USER = "starterUserId";

	/**
	 * 发起节点 id。BPMN 流程模型中代表"申请人"这一虚拟开始节点的活动 ID
	 * （流程回退到发起人、展示申请人节点名称时使用）。
	 */
	public final static String STARTER_ACTIVITY_ID = "starter";

	/** 发起节点显示名称（审核记录中"申请人"环节的中文名）。 */
	public final static String STARTER_ACTIVITY_NAME = "申请人";

}
