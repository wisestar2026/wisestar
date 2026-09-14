package cn.wisestar.server.domain.dto.mall;

import lombok.Data;

/**
 * 学币兑换订单请求（学员发起兑换 / 老师核销 / 列表筛选）。
 *
 * @author wisestar
 * @date 2026/9/14
 */
@Data
public class MallOrderRequest {

	/** 商品ID（学员发起兑换时必填） */
	private String goodsId;

	/** 订单ID（可选，核销时优先按ID定位） */
	private String id;

	/** 核销码（老师核销时填写） */
	private String verifyCode;

	/** 状态筛选 0待核销 1已核销（列表用，可选） */
	private Integer status;

	/** 关键字（学员姓名模糊 / 核销码精确，列表用，可选） */
	private String keyword;

}
