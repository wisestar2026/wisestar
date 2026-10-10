package cn.wisestar.server.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author javahuang
 * @date 2022/2/1
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SelectUserRequest extends PageQuery {

	private String name;

	private String projectId;

	private String deptId;

	private List<String> selected = new ArrayList<>();

	/**
	 * 获取用户名，并以模糊匹配方式返回：非空时前后拼接 %。
	 *
	 * @return 模糊匹配用的用户名；原值为 null 时返回 null
	 */
	public String getName() {
		if (name != null) {
			return "%" + name + "%";
		}
		return null;
	}

}
