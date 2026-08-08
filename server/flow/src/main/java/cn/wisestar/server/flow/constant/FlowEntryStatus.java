package cn.wisestar.server.flow.constant;

/**
 * 流程定义（t_flow_entry.status）发布状态常量。
 *
 * <p>职责：标识一份流程设计当前处于"草稿未发布"还是"已发布"状态。部署成功后才
 * 能被问卷发起流程时使用。</p>
 *
 * <p>所属流程环节：流程设计/发布环节。保存草稿时保持未发布；调用
 * {@code FlowServiceImpl.deploy} 部署成功后将状态置为已发布。</p>
 *
 * <p>被谁调用：{@code FlowServiceImpl.deploy}（部署成功置为 PUBLISHED）。</p>
 *
 * <p>依赖什么：无（纯常量类）。</p>
 *
 * @author Jerry
 * @date 2021-06-06
 */
public final class FlowEntryStatus {

	/**
	 * 未发布。流程设计仅保存为草稿，尚未部署到 Flowable 引擎，问卷无法发起审批流程。
	 */
	public static final int UNPUBLISHED = 0;

	/**
	 * 已发布。流程已部署到 Flowable 引擎并生成了流程定义，问卷可以发起审批流程。
	 */
	public static final int PUBLISHED = 1;

}
