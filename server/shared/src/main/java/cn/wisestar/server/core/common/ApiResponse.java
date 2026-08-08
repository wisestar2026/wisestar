package cn.wisestar.server.core.common;

import cn.wisestar.server.core.uitls.JSONUtil;
import lombok.Data;

/**
 * 统一 API 响应包装对象（ApiResponse）。
 *
 * <p><b>所属模块</b>：shared 模块核心通用类（cn.wisestar.server.core.common）。</p>
 * <p><b>类职责</b>：系统所有 HTTP 接口的统一响应体结构，包含业务状态码 code、
 * 提示信息 message 与业务数据 data 三要素。前端据此统一处理成功/失败分支。</p>
 *
 * <p><b>响应约定</b>：</p>
 * <ul>
 *   <li>code：业务状态码（见 {@link cn.wisestar.server.core.constant.ResponseCode}，
 *       0 表示成功，非 0 表示各类业务失败，异常场景见 ErrorCode 体系）；</li>
 *   <li>message：可读提示信息（失败时展示给用户）；</li>
 *   <li>data：业务数据负载（成功时携带）。</li>
 * </ul>
 *
 * <p><b>谁在生成</b>：</p>
 * <ul>
 *   <li>成功场景：{@link cn.wisestar.server.core.mvc.advice.CustomResponseBodyAdvice}
 *       （ResponseBodyAdvice）自动把 Controller 返回值包装为
 *       {@code ApiResponse(SUCCESS.code, data)}；</li>
 *   <li>失败场景：{@link cn.wisestar.server.core.mvc.advice.GlobalExceptionHandler}
 *       与 {@link cn.wisestar.server.core.security.RestAuthenticationEntryPoint}
 *       统一构造。</li>
 * </ul>
 *
 * <p><b>数据流</b>：Controller 方法返回值 → CustomResponseBodyAdvice 包装 →
 * Jackson 序列化为 JSON → 前端读取 code/data 分支处理。</p>
 *
 * @param <T> 业务数据类型（data 字段的泛型）
 * @author javahuang
 * @date 2021/8/6
 */
@Data
public class ApiResponse<T> {

	/**
	 * 业务状态码：0 表示成功，非 0 表示失败（值域见 ResponseCode / ErrorCode）。
	 */
	private int code;

	/**
	 * 提示信息：成功时可为空，失败时提供可读的错误描述（可能用于前端国际化）。
	 */
	private String message;

	/**
	 * 业务数据：成功时携带的实际返回内容（由 Controller 返回值决定）。
	 */
	private T data;

	/**
	 * 空构造器（序列化 / 反序列化需要）。
	 */
	public ApiResponse() {
	}

	/**
	 * 构造"仅含状态码 + 数据"的响应（如成功响应，message 为空）。
	 *
	 * @param code 业务状态码
	 * @param data 业务数据
	 */
	public ApiResponse(int code, T data) {
		this.code = code;
		this.data = data;
	}

	/**
	 * 构造"仅含状态码 + 提示信息"的响应（如失败响应，data 为空）。
	 *
	 * @param code    业务状态码
	 * @param message 提示信息
	 */
	public ApiResponse(int code, String message) {
		this.code = code;
		this.message = message;
	}

	/**
	 * 以 JSON 字符串形式输出响应内容（便于日志打印排查）。
	 *
	 * @return 本对象的 JSON 序列化字符串
	 */
	@Override
	public String toString() {
		return JSONUtil.toJSONString(this);
	}

}
