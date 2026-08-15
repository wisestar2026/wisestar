package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.student.OrderQuery;
import cn.wisestar.server.domain.dto.student.OrderRequest;
import cn.wisestar.server.domain.dto.student.OrderView;
import cn.wisestar.server.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员订单接口（学员管理模块）。
 *
 * <p><b>定位</b>：管理端「学员管理 → 订单管理」页面数据源——为学员创建订单
 * 开通 AI 自习室权限（学科多选 × 年级多选 × 教材版本 × 账号时长）。</p>
 */
@RestController
@RequestMapping("${api.prefix}/order")
@RequiredArgsConstructor
public class OrderApi {

	/**
	 * 订单管理服务（业务层入口，构造器注入）。
	 */
	private final OrderService orderService;

	/**
	 * 创建订单。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/order/create（如 /api/order/create）。</p>
	 *
	 * <p><b>功能</b>：校验学员/学科/年级/时长 → 服务端计算有效期 →
	 * 同一事务内写入订单主表与权限表（学科×年级笛卡尔积展开）。</p>
	 *
	 * <p><b>请求参数</b>：{@link OrderRequest}（@RequestBody JSON：
	 * studentId/subjectIds[]/grades[]/version/duration/durationUnit）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link OrderView}（含学员信息、学科名称、有效期）。</p>
	 *
	 * @param request 订单请求
	 * @return 订单视图
	 */
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('order:create')")
	public OrderView createOrder(@RequestBody OrderRequest request) {
		return orderService.createOrder(request);
	}

	/**
	 * 订单分页列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/order/list（如 /api/order/list）。</p>
	 *
	 * <p><b>请求参数</b>：{@link OrderQuery}（GET 参数：current/pageSize/studentId/studentName/status）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;{@link OrderView}&gt;（total + list）。</p>
	 *
	 * @param query 查询条件
	 * @return 分页的订单视图
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('order:list')")
	public PaginationResponse<OrderView> listOrders(OrderQuery query) {
		return orderService.pageOrders(query);
	}

	/**
	 * 作废订单。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/order/cancel（如 /api/order/cancel）。</p>
	 *
	 * <p><b>功能</b>：订单状态置 0 作废，并逻辑删除该订单的全部权限行。</p>
	 *
	 * @param request 订单请求（含 id）
	 */
	@PostMapping("/cancel")
	@PreAuthorize("hasAuthority('order:update')")
	public void cancelOrder(@RequestBody OrderRequest request) {
		orderService.cancelOrder(request);
	}

	/**
	 * 删除订单。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/order/delete（如 /api/order/delete）。</p>
	 *
	 * <p><b>功能</b>：逻辑删除订单与该订单的全部权限行。</p>
	 *
	 * @param request 订单请求（含 id）
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('order:delete')")
	public void deleteOrder(@RequestBody OrderRequest request) {
		orderService.deleteOrder(request);
	}

}
