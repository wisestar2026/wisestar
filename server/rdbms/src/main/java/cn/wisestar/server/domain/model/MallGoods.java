package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 积分商城商品实体（对应数据库表 t_mall_goods）。
 *
 * @author wisestar
 * @date 2026/8/22
 */
@Data
@TableName("t_mall_goods")
@EqualsAndHashCode(callSuper = false)
public class MallGoods extends BaseModel {

	/** 商品名称 */
	private String name;

	/** 商品描述 */
	private String description;

	/** 商品图片地址（FileView.previewUrl） */
	private String imageUrl;

	/** 兑换所需积分 */
	private Integer points;

	/** 排序（数字越小越靠前） */
	private Integer sort;

	/** 状态 1上架 0下架 */
	private Integer status;

}
