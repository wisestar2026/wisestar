package cn.wisestar.research.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BizException.class)
	public ResponseEntity<R<Void>> handleBiz(BizException e) {
		return ResponseEntity.status(e.getStatus()).body(R.error(e.getStatus().value(), e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<R<Void>> handleValid(MethodArgumentNotValidException e) {
		FieldError fieldError = e.getBindingResult().getFieldError();
		String message = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
		return ResponseEntity.badRequest().body(R.error(400, message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<R<Void>> handleOther(Exception e) {
		log.error("未处理异常", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(R.error(500, "服务器内部错误，请稍后重试"));
	}

}
