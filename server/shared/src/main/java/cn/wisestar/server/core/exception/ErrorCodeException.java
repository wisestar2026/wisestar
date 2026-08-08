package cn.wisestar.server.core.exception;

import cn.wisestar.server.core.constant.ErrorCode;

/**
 * 带错误码的业务异常（ErrorCodeException）。
 *
 * <p><b>所属模块</b>：shared 模块核心异常体系（cn.wisestar.server.core.exception）。</p>
 * <p><b>类职责</b>：业务代码抛出的"带预定义错误码"的运行时异常。与普通
 * RuntimeException 的区别是携带了 {@link ErrorCode} 枚举（错误码 + 默认消息），
 * 供前端按错误码做国际化/分支处理。</p>
 *
 * <p><b>使用场景</b>：Service 层校验失败、业务规则不满足时抛出，例如
 * "问卷不存在""无权限访问"等。抛出后由
 * {@link cn.wisestar.server.core.mvc.advice.GlobalExceptionHandler#handleErrorCodeException}
 * 捕获，把 errorCode.code 与 errorCode.message 透传给前端。</p>
 *
 * <p><b>数据流</b>：Service 抛 ErrorCodeException → GlobalExceptionHandler 捕获
 * → ApiResponse{code=errorCode.code, message=errorCode.message} → 前端按 code 处理。</p>
 *
 * @author javahuang
 * @date 2022/2/23
 */
public class ErrorCodeException extends RuntimeException {

	/**
	 * 预定义错误码（code + message 见 {@link ErrorCode} 枚举）。
	 */
	private ErrorCode errorCode;

	/**
	 * 构造异常。
	 *
	 * @param errorCode 预定义错误码
	 */
	public ErrorCodeException(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * 构造异常（附带根因）。
	 *
	 * @param errorCode 预定义错误码
	 * @param cause     底层原因
	 */
	public ErrorCodeException(ErrorCode errorCode, Throwable cause) {
		super(cause);
		this.errorCode = errorCode;
	}

	/**
	 * 获取错误码对象。
	 *
	 * @return 预定义错误码（含 code 与 message）
	 */
	public ErrorCode getErrorCode() {
		return errorCode;
	}

}
