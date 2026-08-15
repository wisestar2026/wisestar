package cn.wisestar.server.domain.dto.student;

import cn.wisestar.server.domain.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学员分页查询（学员管理模块）。
 *
 * <p>GET 参数绑定：name/studentNo/phone 为可选筛选条件，均为模糊匹配。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class StudentQuery extends PageQuery {

	/**
	 * 姓名（模糊匹配）。
	 */
	private String name;

	/**
	 * 学号（模糊匹配）。
	 */
	private String studentNo;

	/**
	 * 联系号码（模糊匹配）。
	 */
	private String phone;

}
