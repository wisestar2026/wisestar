package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.mall.MallOrderRequest;
import cn.wisestar.server.domain.dto.mall.MallOrderView;

import java.util.List;

/**
 * 学币兑换订单服务。
 *
 * <p>学员在商城兑换商品后生成订单并「暂时扣除」学币、下发随机 6 位核销码；
 * 老师在核销页凭核销码完成订单（学币正式扣除），订单记录永久保留。</p>
 *
 * @author wisestar
 * @date 2026/9/14
 */
public interface MallOrderService {

	/**
	 * 学员发起兑换（创建订单并暂时扣除学币，生成核销码）。
	 *
	 * @param request 兑换请求（goodsId 必填）
	 * @return 订单视图（含核销码）
	 */
	MallOrderView create(MallOrderRequest request);

	/**
	 * 当前登录学员的兑换记录（按申请时间倒序）。
	 *
	 * @return 订单视图列表
	 */
	List<MallOrderView> myOrders();

	/**
	 * 老师端核销申请列表（可按状态/关键字筛选，按申请时间倒序）。
	 *
	 * @param request 筛选请求（status/keyword 可选）
	 * @return 订单视图列表
	 */
	List<MallOrderView> listOrders(MallOrderRequest request);

	/**
	 * 老师端核销（凭核销码完成订单）。
	 *
	 * @param request 核销请求（id 或 verifyCode 至少其一）
	 * @return 完成后的订单视图
	 */
	MallOrderView verify(MallOrderRequest request);

}
