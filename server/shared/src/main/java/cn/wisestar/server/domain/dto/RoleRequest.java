package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * @author javahuang
 * @date 2021/10/12
 */
@Data
public class RoleRequest {

	private String id;

	private String name;

	private String code;

	private String remark;

	/** 权限编码列表 */
	private List<String> authorities;

	private List<String> userIds;

	private List<String> evictUserIds;

	private Integer status;

	/**
	 * 数据范围：ALL 全校可见；CAMPUS 仅本人绑定校区可见。
	 * 为空时新增默认 ALL，编辑时不改动原值。
	 */
	private String dataScope;

}
