package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 校区数据权限范围（当前登录用户）。
 *
 * <p>规则：角色含 admin 或不属于校长/教务/学管师的账号 → ALL；
 * 属于校长/教务/学管师且绑定校区 → SCOPED(绑定校区名称集合，含停用校区)；
 * 属于上述角色但未绑定任何校区 → EMPTY。</p>
 *
 * <p>调用方在学员/订单/督学查询与单对象访问处消费该范围；范围过滤在服务层强制执行。</p>
 *
 * @author wisestar
 * @date 2026/9/8
 */
@Data
public class CampusScope {

	/** 全部可见（不受校区过滤） */
	public static final String MODE_ALL = "ALL";

	/** 空数据（看不到任何校区学员数据） */
	public static final String MODE_EMPTY = "EMPTY";

	/** 仅可见绑定校区数据 */
	public static final String MODE_SCOPED = "SCOPED";

	/**
	 * 角色数据范围配置值：全校可见（不施加校区过滤）。
	 */
	public static final String DATA_SCOPE_ALL = "ALL";

	/**
	 * 角色数据范围配置值：仅本人绑定校区可见（启用校区数据隔离）。
	 */
	public static final String DATA_SCOPE_CAMPUS = "CAMPUS";

	/**
	 * 模式：ALL / EMPTY / SCOPED。
	 */
	private String mode;

	/**
	 * 可见校区名称集合（仅 SCOPED 有效）。
	 */
	private Set<String> campusNames = new LinkedHashSet<>();

	public static CampusScope all() {
		CampusScope scope = new CampusScope();
		scope.setMode(MODE_ALL);
		return scope;
	}

	public static CampusScope empty() {
		CampusScope scope = new CampusScope();
		scope.setMode(MODE_EMPTY);
		return scope;
	}

	public static CampusScope scoped(Set<String> campusNames) {
		CampusScope scope = new CampusScope();
		scope.setMode(MODE_SCOPED);
		if (campusNames != null) {
			scope.setCampusNames(campusNames);
		}
		return scope;
	}

	public boolean isAll() {
		return MODE_ALL.equals(mode);
	}

	public boolean isEmpty() {
		return MODE_EMPTY.equals(mode);
	}

	public boolean isScoped() {
		return MODE_SCOPED.equals(mode);
	}

	public Set<String> getCampusNames() {
		return campusNames == null ? Collections.emptySet() : campusNames;
	}

}
