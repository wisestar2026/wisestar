package cn.wisestar.server.domain.dto.mall;

import lombok.Data;

/**
 * 积分商城商品请求（后台商品管理）。
 *
 * @author wisestar
 * @date 2026/8/22
 */
@Data
public class MallGoodsRequest {

	private String id;

	/** 商品名称（必填） */
	private String name;

	/** 商品描述 */
	private String description;

	/** 商品图片地址（FileView.previewUrl，上传后回填） */
	private String imageUrl;

	/** 兑换所需积分 */
	private Integer points;

	/** 排序（数字越小越靠前） */
	private Integer sort;

	/** 状态 1上架 0下架 */
	private Integer status;

}
