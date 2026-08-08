package cn.wisestar.server.core.exception;

/**
 * 服务器内部错误异常（InternalServerError）。
 *
 * <p><b>所属模块</b>：shared 模块核心异常体系（cn.wisestar.server.core.exception）。</p>
 * <p><b>类职责</b>：表示"服务器内部错误"的运行时异常，通常由业务代码在
 * 遇到不可预期/不可恢复的错误时主动抛出（如依赖服务失败、数据状态非法等）。</p>
 *
 * <p><b>与普通异常的区别</b>：{@link cn.wisestar.server.core.mvc.advice.GlobalExceptionHandler}
 * 的兜底 ExceptionHandler 会特判本类型：<em>只有本类型的 message 才会被透传给前端</em>，
 * 其他未知异常一律返回通用文案"服务出了点问题"，避免把内部实现细节泄露给客户端。</p>
 *
 * <p><b>使用场景示例</b>：{@link cn.wisestar.server.core.aop.DataPermAspect} 在
 * 数据权限校验失败时抛出本异常（如"没有权限访问本问卷"）。</p>
 *
 * @author javahuang
 * @date 2021/8/6
 */
public class InternalServerError extends RuntimeException {

	/**
	 * 构造空异常。
	 */
	public InternalServerError() {
		super();
	}

	/**
	 * 构造带消息的异常（消息会透传给前端）。
	 *
	 * @param message 错误描述
	 */
	public InternalServerError(String message) {
		super(message);
	}

	/**
	 * 构造带消息与根因的异常。
	 *
	 * @param message 错误描述
	 * @param cause   底层原因
	 */
	public InternalServerError(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 构造仅带根因的异常。
	 *
	 * @param cause 底层原因
	 */
	public InternalServerError(Throwable cause) {
		super(cause);
	}

}
