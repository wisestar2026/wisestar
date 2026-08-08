package cn.wisestar.server.core.constant;

/**
 * 通用响应状态码枚举（ResponseCode）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义系统统一响应体 {@link cn.wisestar.server.core.common.ApiResponse}
 * 中 code 字段的标准取值（HTTP 语义对齐），供成功响应包装
 * （{@link cn.wisestar.server.core.mvc.advice.CustomResponseBodyAdvice}）与
 * 异常响应构造（{@link cn.wisestar.server.core.mvc.advice.GlobalExceptionHandler}、
 * {@link cn.wisestar.server.core.security.RestAuthenticationEntryPoint}）使用。</p>
 *
 * <p><b>注意</b>：虽然 HTTP 状态码固定为 200，但业务 code 采用标准 HTTP
 * 语义编码，前端通过 code 值区分成功（200）与各类失败。</p>
 *
 * @author javahuang
 * @date 2021/8/6
 */
public enum ResponseCode {

	/** 成功 */
	SUCCESS(200),
	/** 业务失败 */
	FAIL(400),
	/** 未认证 */
	UNAUTHORIZED(401),
	/** 权限不足 */
	FORBIDDEN(403),
	/** 资源不存在 */
	NOT_FOUND(404),
	/** 服务器内部错误 */
	INTERNAL_SERVER_ERROR(500);

	/**
	 * 状态码数值（响应体 code 字段值）。
	 */
	public int code;

	ResponseCode(int code) {
		this.code = code;
	}

}
