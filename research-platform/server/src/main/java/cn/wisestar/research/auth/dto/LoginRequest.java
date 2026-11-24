package cn.wisestar.research.auth.dto;

import javax.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "请输入账号") String username,
		@NotBlank(message = "请输入密码") String password) {
}
