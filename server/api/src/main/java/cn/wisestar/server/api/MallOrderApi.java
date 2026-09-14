package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.mall.MallOrderRequest;
import cn.wisestar.server.domain.dto.mall.MallOrderView;
import cn.wisestar.server.service.MallOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学币兑换订单接口。
 *
 * <p><b>定位</b>：学员端商城兑换与兑换记录（isAuthenticated）；
 * 老师端核销申请列表与核销操作（mall:list / mall:update）。</p>
 *
 * @author wisestar
 * @date 2026/9/14
 */
@RestController
@RequestMapping("${api.prefix}/mall/order")
@RequiredArgsConstructor
public class MallOrderApi {

	private final MallOrderService mallOrderService;

	/**
	 * 学员发起兑换（创建订单，暂时扣除学币并生成核销码）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/mall/order/create。</p>
	 *
	 * <p><b>权限</b>：isAuthenticated()（服务层校验学员身份）。</p>
	 *
	 * @param request 兑换请求（goodsId 必填）
	 * @return 订单视图（含核销码）
	 */
	@PostMapping("/create")
	@PreAuthorize("isAuthenticated()")
	public MallOrderView create(@RequestBody MallOrderRequest request) {
		return mallOrderService.create(request);
	}

	/**
	 * 学员兑换记录（我的订单）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/mall/order/mine。</p>
	 *
	 * <p><b>权限</b>：isAuthenticated()（服务层校验学员身份）。</p>
	 *
	 * @return 订单列表（按申请时间倒序）
	 */
	@GetMapping("/mine")
	@PreAuthorize("isAuthenticated()")
	public List<MallOrderView> mine() {
		return mallOrderService.myOrders();
	}

	/**
	 * 老师端核销申请列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/mall/order/list?status=&keyword=。</p>
	 *
	 * <p><b>权限</b>：hasAuthority('mall:list')。</p>
	 *
	 * @param request 筛选请求（status/keyword 可选）
	 * @return 订单列表（待核销优先，按申请时间倒序）
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('mall:list')")
	public List<MallOrderView> list(MallOrderRequest request) {
		return mallOrderService.listOrders(request);
	}

	/**
	 * 老师端核销（凭核销码完成订单，学币正式扣除）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/mall/order/verify。</p>
	 *
	 * <p><b>权限</b>：hasAuthority('mall:update')。</p>
	 *
	 * @param request 核销请求（verifyCode 或 id）
	 * @return 完成后的订单视图
	 */
	@PostMapping("/verify")
	@PreAuthorize("hasAuthority('mall:update')")
	public MallOrderView verify(@RequestBody MallOrderRequest request) {
		return mallOrderService.verify(request);
	}

}
