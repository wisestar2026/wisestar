package cn.wisestar.server.domain.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author javahuang
 * @date 2021/10/15
 */
@Data
public class UserRequest {

	private String id;

	private String deptId;

	private String name;

	private String avatar;

	private String profile;

	/** 登录账号 */
	private String username;

	/** 密码 */
	private String password;

	/** 密码修改原密码 */
	private String oldPassword;

	private List<String> roles;

	private String phone;

	private String email;

	private String gender;

	private Integer status;

	private List<UserPositionRequest> userPositions;

	/**
	 * 校区ID多选（校区数据权限范围；通常仅校长/教务/学管师角色账号维护）。
	 */
	private List<String> campusIds;

	private MultipartFile file;

	/**
	 * 错题答对次数
	 */
	private Integer correctTimes;

}
