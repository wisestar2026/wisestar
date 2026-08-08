package cn.wisestar.server.core.mvc.advice;

import cn.wisestar.server.core.common.ApiResponse;
import cn.wisestar.server.core.constant.ResponseCode;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应体包装器（CustomResponseBodyAdvice）。
 *
 * <p><b>所属模块</b>：shared 模块 MVC 增强包（cn.wisestar.server.core.mvc.advice）。</p>
 * <p><b>类职责</b>：实现 {@link ResponseBodyAdvice}，在 Controller 方法返回后、
 * JSON 序列化之前，把返回值统一包装为 {@link ApiResponse}，
 * 实现"Controller 只写业务逻辑、响应格式全局统一"的目标。</p>
 *
 * <p><b>包装规则</b>（beforeBodyWrite）：</p>
 * <ul>
 *   <li>返回值是 {@link Resource}（文件下载、图片等流式资源）→ 原样返回，不包装；</li>
 *   <li>返回值已是 {@link ApiResponse}（如全局异常处理器的响应）→ 原样返回，避免二次包装；</li>
 *   <li>其他任意业务返回值 → 包装为 {@code ApiResponse(ResponseCode.SUCCESS.code, body)}。</li>
 * </ul>
 *
 * <p><b>与异常处理的分工</b>：本类负责"成功响应"包装；失败响应由
 * {@link GlobalExceptionHandler} 直接构造 ApiResponse（因此不会二次包装）。</p>
 *
 * <p><b>数据流</b>：Controller 方法返回业务对象 → 本类的 beforeBodyWrite
 * → ApiResponse{code=0, data=业务对象} → Jackson 序列化 → 前端。</p>
 *
 * @author javahuang
 * @date 2021/08/23
 */
@RestControllerAdvice
public class CustomResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	/**
	 * 是否启用包装。
	 *
	 * <p>当响应由 {@link ResourceRegionHttpMessageConverter} 处理时（大文件
	 * 分片/范围请求下载场景），返回 false 跳过包装，避免破坏流式响应；
	 * 其余转换器一律启用包装。</p>
	 *
	 * @param returnType    方法返回类型
	 * @param converterType 将要使用的消息转换器
	 * @return 是否执行 beforeBodyWrite
	 */
	@Override
	public boolean supports(MethodParameter returnType, Class converterType) {
		if (converterType.equals(ResourceRegionHttpMessageConverter.class)) {
			return false;
		}
		return true;
	}

	/**
	 * 响应体写出前的包装逻辑（详见类注释的包装规则）。
	 *
	 * @param body                 Controller 方法返回值
	 * @param returnType           方法返回类型
	 * @param selectedContentType  选中的 Content-Type
	 * @param selectedConverterType 选中的消息转换器
	 * @param request              HTTP 请求
	 * @param response             HTTP 响应
	 * @return 包装后的响应体（ApiResponse 或原对象）
	 */
	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
		if (body instanceof Resource) {
			return body;
		}
		if (body instanceof ApiResponse) {
			return body;
		}
		return new ApiResponse(ResponseCode.SUCCESS.code, body);
	}

}
