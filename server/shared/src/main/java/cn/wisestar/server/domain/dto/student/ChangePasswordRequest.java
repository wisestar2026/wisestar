package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学员修改密码请求。
 *
 * @author wisestar
 * @date 2026/8/19
 */
@Data
public class ChangePasswordRequest {

	/** 原密码（必填，与当前账号密码比对） */
	private String oldPassword;

	/** 新密码（至少 6 位） */
	private String newPassword;

}
