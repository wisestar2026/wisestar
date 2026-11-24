package cn.wisestar.research.auth;

public record CurrentUser(String id, String username, String name, AuthRole role, String source) {
}
