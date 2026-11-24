package cn.wisestar.research.auth.dto;

public record LoginResponse(String token, UserView user) {

	public record UserView(String id, String username, String name, String role, String roleName, String source) {
	}

}
