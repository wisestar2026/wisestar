package cn.wisestar.server.domain.dto.student;

import cn.wisestar.server.domain.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单分页查询（学员管理模块）。
 *
 * <p>GET 参数绑定：studentId 精确筛选；studentName 模糊匹配学员姓名；
 * status 状态筛选（1 生效 0 作废，不传查全部）。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OrderQuery extends PageQuery {

	/**
	 * 学员ID（精确匹配）。
	 */
	private String studentId;

	/**
	 * 学员姓名（模糊匹配）。
	 */
	private String studentName;

	/**
	 * 状态（1 生效 0 作废）。
	 */
	private Integer status;

}
