package cn.wisestar.server.core.mvc.advice;

import cn.wisestar.server.core.common.ApiResponse;
import cn.wisestar.server.core.constant.ResponseCode;
import cn.wisestar.server.core.constant.ErrorCode;
import cn.wisestar.server.core.exception.ErrorCodeException;
import cn.wisestar.server.core.exception.InternalServerError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;
import java.util.Map;

/**
 * 全局异常处理器（GlobalExceptionHandler）。
 *
 * <p><b>所属模块</b>：shared 模块 MVC 增强包（cn.wisestar.server.core.mvc.advice）。</p>
 * <p><b>类职责</b>：基于 @ControllerAdvice 捕获 Controller 层抛出的各类异常，
 * 统一转换为 {@link ApiResponse} JSON 响应，保证任何异常都不会以裸堆栈/HTML
 * 形式返回给前端。HTTP 状态码固定为 200，业务成败通过响应体 code 区分
 * （业务约定，便于前端统一拦截处理）。</p>
 *
 * <p><b>异常处理映射表</b>：</p>
 * <ul>
 *   <li>{@link NoHandlerFoundException} → 404：返回前端 SPA 的 index.html
 *       （单 jar 部署兜底：任意未匹配的前端路由都回到首页，由前端路由接管）；</li>
 *   <li>{@link ValidationException} → 参数校验异常（javax.validation），
 *       返回 FAIL + 异常 message；</li>
 *   <li>{@link MissingServletRequestParameterException} → 缺少必填请求参数；
 *       返回 FAIL + "Missing request parameter"；</li>
 *   <li>{@link MethodArgumentTypeMismatchException} → 参数类型不匹配（如传 abc 给 int），
 *       返回 FAIL + "Method argument type mismatch"；</li>
 *   <li>{@link MethodArgumentNotValidException} → @Valid 校验失败，
 *       返回 FAIL + "Method argument validation failed"；</li>
 *   <li>{@link AccessDeniedException} → 权限不足（已登录但无该操作权限），
 *       返回 FORBIDDEN（403 码）+ "Authentication failed"；</li>
 *   <li>{@link ErrorCodeException} → 业务错误码异常，透传 errorCode 的 code 与 message
 *       （见 {@link ErrorCode} 枚举定义）；</li>
 *   <li>{@link Exception}（兜底）→ 记录完整堆栈；若为 {@link InternalServerError}
 *       则透传其 message，否则返回通用文案"服务出了点问题"。</li>
 * </ul>
 *
 * <p><b>注意</b>：Spring Security 过滤器链内部抛出的异常（如认证失败）不会进入本类，
 * 由 {@link cn.wisestar.server.core.security.RestAuthenticationEntryPoint} 处理。</p>
 *
 * @author javahuang
 * @date 2021/08/13
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	/**
	 * 前端 SPA 首页资源（用于 404 兜底，避免资源不存在时报错）。
	 */
	@Value("classpath:/static/index.html")
	private Resource indexHtml;

	/**
	 * 处理 404（无对应 Handler）异常：返回 SPA 首页。
	 *
	 * <p>单 jar 部署模式下，未匹配 Controller 且非静态资源的路径（即前端
	 * 路由地址，如刷新 /survey/detail 页面）统一返回 index.html，由前端
	 * 路由接管渲染。</p>
	 *
	 * @param request 请求（未使用）
	 * @param e       404 异常
	 * @return index.html 资源
	 */
	@ExceptionHandler(NoHandlerFoundException.class)
	public Object handleError404(HttpServletRequest request, Exception e) {
		return ResponseEntity.ok().body(indexHtml);
	}

	/**
	 * 处理 javax 参数校验异常（ValidationException）。
	 *
	 * @param request 请求（仅用于记录日志）
	 * @param ex      校验异常
	 * @return FAIL 码 + 异常 message
	 */
	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ApiResponse<String>> handleValidationException(HttpServletRequest request,
			ValidationException ex) {
		log.error("ValidationException {}\n", request.getRequestURI(), ex);

		return ResponseEntity.ok().body(new ApiResponse<>(ResponseCode.FAIL.code, ex.getMessage()));
	}

	/**
	 * 处理缺少必填请求参数异常。
	 *
	 * @param request 请求（仅用于记录日志）
	 * @param ex      缺参异常
	 * @return FAIL 码 + "Missing request parameter"
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<String>> handleMissingServletRequestParameterException(HttpServletRequest request,
			MissingServletRequestParameterException ex) {
		log.error("handleMissingServletRequestParameterException {}\n", request.getRequestURI(), ex);

		return ResponseEntity.ok().body(new ApiResponse<>(ResponseCode.FAIL.code, "Missing request parameter"));
	}

	/**
	 * 处理请求参数类型不匹配异常（如字符串传给数字参数）。
	 *
	 * @param request 请求（仅用于记录日志）
	 * @param ex      类型不匹配异常
	 * @return FAIL 码 + "Method argument type mismatch"
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentTypeMismatchException(
			HttpServletRequest request, MethodArgumentTypeMismatchException ex) {
		log.error("handleMethodArgumentTypeMismatchException {}\n", request.getRequestURI(), ex);

		return ResponseEntity.ok().body(new ApiResponse<>(ResponseCode.FAIL.code, "Method argument type mismatch"));
	}

	/**
	 * 处理 @Valid 注解校验失败异常（含字段级错误明细）。
	 *
	 * @param request 请求（仅用于记录日志）
	 * @param ex      校验失败异常
	 * @return FAIL 码 + "Method argument validation failed"
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
			HttpServletRequest request, MethodArgumentNotValidException ex) {
		log.error("handleMethodArgumentNotValidException {}\n", request.getRequestURI(), ex);
		return ResponseEntity.ok().body(new ApiResponse<>(ResponseCode.FAIL.code, "Method argument validation failed"));
	}

	/**
	 * 处理权限不足异常（AccessDeniedException）。
	 *
	 * <p>触发场景：用户已登录但缺少目标接口所需的权限码
	 * （Controller @PreAuthorize 校验失败）。</p>
	 *
	 * @param request 请求（仅用于记录日志）
	 * @param ex      权限异常
	 * @return FORBIDDEN 码 + "Authentication failed"
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<String>> handleAccessDeniedException(HttpServletRequest request,
			AccessDeniedException ex) {
		log.error("handleAccessDeniedException {}\n", request.getRequestURI());

		return ResponseEntity.ok().body(new ApiResponse<>(ResponseCode.FORBIDDEN.code, "Authentication failed"));
	}

	/**
	 * 处理带错误码的业务异常（ErrorCodeException）。
	 *
	 * <p>把 ErrorCode 枚举的 code 与 message 原样透传给前端，
	 * 前端可据此做国际化或分支处理（如"问卷不存在"提示）。</p>
	 *
	 * @param request 请求（仅用于记录日志）
	 * @param ex      业务异常
	 * @return errorCode.code + errorCode.message
	 */
	@ExceptionHandler(ErrorCodeException.class)
	public ResponseEntity<ApiResponse<String>> handleErrorCodeException(HttpServletRequest request,
			ErrorCodeException ex) {
		ErrorCode errorCode = ex.getErrorCode();
		log.error(String.format("handleErrorCodeError %s errorCode=%d, errorMessage=%s", request.getRequestURI(),
				errorCode.code, errorCode.message));
		return ResponseEntity.ok().body(new ApiResponse<>(errorCode.code, errorCode.message));
	}

	/**
	 * 兜底异常处理（所有未被上面规则捕获的异常）。
	 *
	 * <p>记录完整堆栈；响应体策略：</p>
	 * <ul>
	 *   <li>异常是 {@link InternalServerError}：透传其 message（该异常的消息设计为
	 *       可安全展示给用户）；</li>
	 *   <li>其他未知异常：返回通用文案"服务出了点问题"，避免泄露内部错误细节。</li>
	 * </ul>
	 *
	 * @param request 请求（仅用于记录日志）
	 * @param ex      未知异常
	 * @return INTERNAL_SERVER_ERROR 码 + 错误消息
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<String>> handleInternalServerError(HttpServletRequest request, Exception ex) {
		log.error("handleInternalServerError {}\n", request.getRequestURI(), ex);
		return ResponseEntity.ok().body(new ApiResponse<>(ResponseCode.INTERNAL_SERVER_ERROR.code,
				ex instanceof InternalServerError ? ex.getMessage() : "服务出了点问题"));
	}

}
