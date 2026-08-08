package cn.wisestar.server.flow.exception;

/**
 * 流程模块自定义运行时异常。
 *
 * <p>职责：包装流程处理过程中的业务异常（如"该问卷未设置流程"、"当前节点不能进行
 * 驳回操作"、"流程部署失败"等），由全局异常处理器统一捕获并转为友好的 HTTP 错误响应。
 * 继承 RuntimeException，因此不强制调用方显式声明捕获。</p>
 *
 * <p>所属流程环节：贯穿流程设计（部署）、审批处理（撤回校验）等环节的异常出口。</p>
 *
 * <p>被谁调用：FlowServiceImpl（部署/未设置流程校验）、RevertTaskHandler
 * （撤回权限校验）、AbstractTaskHandler 及其子类。</p>
 *
 * <p>依赖什么：无（纯异常类，仅依赖 JDK）。</p>
 *
 * @author javahuang
 * @date 2021/11/18
 */
public class FlowableRuntimeException extends RuntimeException {

	/**
	 * 构造仅含消息的异常。
	 *
	 * @param message 异常描述（面向用户的可读错误信息）
	 */
	public FlowableRuntimeException(String message) {
		super(message);
	}

	/**
	 * 构造含消息与根因的异常。
	 *
	 * @param message 异常描述
	 * @param cause 底层根因（如解析/部署引擎异常）
	 */
	public FlowableRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

}
