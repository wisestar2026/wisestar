package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.mall.MallGoodsRequest;
import cn.wisestar.server.domain.dto.mall.MallGoodsView;
import cn.wisestar.server.service.MallGoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 积分商城商品接口。
 *
 * <p><b>定位</b>：商品列表学员端/后台共用（学员端仅看上架）；商品增删改由后台老师维护。</p>
 *
 * @author wisestar
 * @date 2026/8/22
 */
@RestController
@RequestMapping("${api.prefix}/mall")
@RequiredArgsConstructor
public class MallGoodsApi {

	private final MallGoodsService mallGoodsService;

	/**
	 * 商品列表（学员端传 status=1 仅上架；后台不传返回全部）。
	 *
	 * @param status 状态过滤（可选）
	 * @return 商品列表（按 sort 升序）
	 */
	@GetMapping("/goods")
	@PreAuthorize("isAuthenticated()")
	public List<MallGoodsView> listGoods(@RequestParam(required = false) Integer status) {
		return mallGoodsService.listGoods(status);
	}

	/**
	 * 新增商品。
	 *
	 * @param request 商品请求
	 */
	@PostMapping("/goods/create")
	@PreAuthorize("hasAuthority('mall:create')")
	public void createGoods(@RequestBody MallGoodsRequest request) {
		mallGoodsService.createGoods(request);
	}

	/**
	 * 编辑商品。
	 *
	 * @param request 商品请求（含 id）
	 */
	@PostMapping("/goods/update")
	@PreAuthorize("hasAuthority('mall:update')")
	public void updateGoods(@RequestBody MallGoodsRequest request) {
		mallGoodsService.updateGoods(request);
	}

	/**
	 * 删除商品（逻辑删除）。
	 *
	 * @param request 商品请求（含 id）
	 */
	@PostMapping("/goods/delete")
	@PreAuthorize("hasAuthority('mall:delete')")
	public void deleteGoods(@RequestBody MallGoodsRequest request) {
		mallGoodsService.deleteGoods(request);
	}

}
