package cn.wisestar.server.flow.constant;

/**
 * 任务查询类型常量。
 *
 * <p>职责：定义前端"任务列表"页签的查询类型，是 {@code FlowTaskQuery.type} 字段
 * 的取值字典。{@code FlowServiceImpl.getFlowTasks} 根据该值分发到不同的查询分支：
 * 待办走 Flowable 任务表，已办走 t_flow_operation 表，我发起的走 t_flow_instance 表。</p>
 *
 * <p>所属流程环节：待办中心/任务列表查询环节。</p>
 *
 * <p>被谁调用：FlowApi.getFlowTasks（请求参数）、FlowServiceImpl.getFlowTasks（查询分发）。</p>
 *
 * <p>依赖什么：无（纯常量类）。</p>
 *
 * @author javahuang
 * @date 2022/1/9
 */
public final class FlowTaskQueryType {

	/** 我的待办：当前用户待处理的审批任务（Flowable 运行时任务） */
	public static final int todo = 1;

	/** 已办事项：当前用户已处理过的审批记录（t_flow_operation 用户任务操作） */
	public static final int finished = 2;

	/** 我的抄送：抄送给当前用户的任务（预留，当前未实现查询分支） */
	public static final int copyTo = 3;

	/** 我发起的：当前用户发起的流程实例（t_flow_instance） */
	public static final int selfCreated = 4;

}
