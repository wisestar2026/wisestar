package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author javahuang
 * @date 2021/10/12
 */
@Data
public class RoleView {

	private String id;

	private String name;

	private String code;

	private String remark;

	private List<String> authorities;

	/**
	 * 是否内置角色：1 内置（不可删除）；0 普通
	 */
	private Integer builtin;

	private Date createAt;

	private Integer status;

}
