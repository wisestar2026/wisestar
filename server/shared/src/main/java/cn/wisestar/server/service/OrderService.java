package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.student.OrderQuery;
import cn.wisestar.server.domain.dto.student.OrderRequest;
import cn.wisestar.server.domain.dto.student.OrderView;

/**
 * 学员订单服务（学员管理模块）。
 *
 * <p><b>定位</b>：为已注册学员创建订单并配置学科/年级/教材版本/账号时长，
 * 订单写入同时按学科×年级笛卡尔积展开写入学员权限表（t_student_permission），
 * 供学员端按有效期鉴权。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
public interface OrderService {

	/**
	 * 创建订单（订单主表 + 权限展开行，同一事务）。
	 *
	 * @param request 订单请求（studentId/subjectIds/grades/duration/durationUnit 必填）
	 * @return 订单视图（含学员信息与学科名称）
	 */
	OrderView createOrder(OrderRequest request);

	/**
	 * 订单分页查询。
	 *
	 * @param query 查询条件（studentId 精确、studentName 模糊、status 状态）
	 * @return 分页的订单视图
	 */
	PaginationResponse<OrderView> pageOrders(OrderQuery query);

	/**
	 * 作废订单（status=0 + 该订单权限逻辑删除）。
	 *
	 * @param request 订单请求（含 id）
	 */
	void cancelOrder(OrderRequest request);

	/**
	 * 删除订单（逻辑删除 + 该订单权限逻辑删除）。
	 *
	 * @param request 订单请求（含 id）
	 */
	void deleteOrder(OrderRequest request);

}
