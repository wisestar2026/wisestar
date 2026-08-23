package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.mall.MallGoodsRequest;
import cn.wisestar.server.domain.dto.mall.MallGoodsView;

import java.util.List;

/**
 * 积分商城商品服务。
 *
 * @author wisestar
 * @date 2026/8/22
 */
public interface MallGoodsService {

	/**
	 * 商品列表（学员端传 status=1 仅上架；后台传 null 全部）。
	 *
	 * @param status 状态过滤（1 上架 / 0 下架 / null 全部）
	 * @return 商品视图列表（按 sort 升序）
	 */
	List<MallGoodsView> listGoods(Integer status);

	/**
	 * 新增商品。
	 *
	 * @param request 商品请求（name 必填）
	 */
	void createGoods(MallGoodsRequest request);

	/**
	 * 编辑商品。
	 *
	 * @param request 商品请求（含 id）
	 */
	void updateGoods(MallGoodsRequest request);

	/**
	 * 删除商品（逻辑删除）。
	 *
	 * @param request 商品请求（含 id）
	 */
	void deleteGoods(MallGoodsRequest request);

}
