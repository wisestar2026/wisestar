package cn.wisestar.research.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

	private int code;

	private T data;

	private String message;

	public static <T> R<T> ok(T data) {
		return new R<>(200, data, null);
	}

	public static R<Void> ok() {
		return new R<>(200, null, null);
	}

	public static <T> R<T> error(int code, String message) {
		return new R<>(code, null, message);
	}

}
