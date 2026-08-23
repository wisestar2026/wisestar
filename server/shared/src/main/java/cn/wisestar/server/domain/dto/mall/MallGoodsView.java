package cn.wisestar.server.domain.dto.mall;

import lombok.Data;

import java.util.Date;

/**
 * 积分商城商品视图。
 *
 * @author wisestar
 * @date 2026/8/22
 */
@Data
public class MallGoodsView {

	private String id;

	/** 商品名称 */
	private String name;

	/** 商品描述 */
	private String description;

	/** 商品图片地址 */
	private String imageUrl;

	/** 兑换所需积分 */
	private Integer points;

	/** 排序 */
	private Integer sort;

	/** 状态 1上架 0下架 */
	private Integer status;

	/** 创建时间 */
	private Date createAt;

}
