package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author javahuang
 * @date 2021/10/15
 */
@Data
public class UserView {

	private String id;

	private String name;

	private String username;

	private String phone;

	private String email;

	private String avatar;

	private String gender;

	private Integer status;

	private Date createAt;

	private String deptId;

	private String deptName;

	private List<RoleView> roles;

	/** 用户岗位 */
	private List<UserPositionView> userPositions;

	/**
	 * 绑定校区（id/name/status；构成校长/教务/学管师的数据权限范围）。
	 */
	private List<CampusView> campuses;

}
