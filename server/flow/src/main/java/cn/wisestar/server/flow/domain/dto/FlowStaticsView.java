package cn.wisestar.server.flow.domain.dto;

import lombok.Data;

/**
 * 任务统计视图 DTO。
 *
 * <p>职责：向前端工作台/待办首页返回当前用户四类任务的数量统计，由
 * {@code FlowServiceImpl.statics} 组装：待办（Flowable 运行时任务）、已办
 * （t_flow_operation 用户任务）、抄送（预留）、我发起的（t_flow_instance 审批中实例）。</p>
 *
 * <p>所属流程环节：待办中心统计展示环节。</p>
 *
 * <p>被谁调用：FlowServiceImpl.statics（组装返回前端）。</p>
 *
 * <p>依赖什么：无（纯统计结果容器）。</p>
 *
 * @author javahuang
 * @date 2022/2/16
 */
@Data
public class FlowStaticsView {

	/**
	 * 待办：当前用户作为候选人或指派人的活跃任务数
	 */
	long todo;

	/**
	 * 已完成：当前所有用户任务操作记录数（未按用户过滤，统计口径为全局已办总量）
	 */
	long finished;

	/**
	 * 抄送给我的：抄送任务数量（当前实现未计算，保持默认 0）
	 */
	long copyTo;

	/**
	 * 我发起的：当前用户发起且处于审批中的流程实例数
	 */
	long selfCreated;

}
