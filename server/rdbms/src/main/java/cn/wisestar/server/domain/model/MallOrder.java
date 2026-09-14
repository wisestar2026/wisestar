package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 学币兑换订单实体（对应数据库表 t_mall_order）。
 *
 * <p>学员在商城发起兑换时生成订单并「暂时扣除」学币，同时下发随机 6 位核销码；
 * 老师在核销页凭核销码确认后订单完成（学币正式扣除）。订单记录永久保留。</p>
 *
 * @author wisestar
 * @date 2026/9/14
 */
@Data
@TableName("t_mall_order")
@EqualsAndHashCode(callSuper = false)
public class MallOrder extends BaseModel {

	/** 学员ID（t_student.id） */
	private String studentId;

	/** 学号（冗余快照，便于核销页展示） */
	private String studentNo;

	/** 学员姓名（冗余快照） */
	private String studentName;

	/** 商品ID（t_mall_goods.id） */
	private String goodsId;

	/** 商品名称（冗余快照） */
	private String goodsName;

	/** 商品图片（冗余快照） */
	private String goodsImage;

	/** 消耗学币数量 */
	private Integer coins;

	/** 核销码（随机 6 位数字） */
	private String verifyCode;

	/** 状态 0待核销 1已核销 */
	private Integer status;

	/** 核销时间 */
	private Date verifyAt;

	/** 核销人（老师账号ID） */
	private String verifyBy;

}
