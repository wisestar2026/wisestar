package cn.wisestar.server.flow.service.impl.taskHandler;

import cn.wisestar.server.flow.domain.dto.ApprovalTaskRequest;

/**
 * 审批任务处理器接口。
 *
 * <p>职责：定义审批操作的统一处理入口。每种审批类型对应一个 Spring Bean
 * （Bean 名 = 类型 + "TaskHandler"，如 agreeTaskHandler、refuseTaskHandler），
 * 由 {@code FlowServiceImpl.approvalTask} 根据请求 type 动态获取并调用。</p>
 *
 * <p>所属流程环节：审批处理环节的总分派点。</p>
 *
 * <p>被谁调用：FlowServiceImpl.approvalTask（按类型分发）。</p>
 *
 * <p>依赖什么：{@link ApprovalTaskRequest}（审批请求参数）；
 * 实现类继承 {@link AbstractTaskHandler}。</p>
 *
 * @author javahuang
 * @date 2021/12/17
 */
public interface TaskHandler {

	/**
	 * 执行一次审批操作。
	 *
	 * <p>实现类需先执行引擎侧的任务流转（同意完成/拒绝删实例/驳回移动节点等），
	 * 再调用父类 {@link AbstractTaskHandler#process} 模板完成操作记录落库与答案更新。</p>
	 *
	 * @param request 审批请求（type、taskId、processInstanceId、答案、意见等）
	 */
	void process(ApprovalTaskRequest request);

}
