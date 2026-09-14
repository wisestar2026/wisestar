package cn.wisestar.server.domain.dto.mall;

import lombok.Data;

import java.util.Date;

/**
 * 学币兑换订单视图（学员端兑换记录 / 老师端核销申请）。
 *
 * @author wisestar
 * @date 2026/9/14
 */
@Data
public class MallOrderView {

	private String id;

	/** 学员ID */
	private String studentId;

	/** 学号 */
	private String studentNo;

	/** 学员姓名 */
	private String studentName;

	/** 商品ID */
	private String goodsId;

	/** 商品名称 */
	private String goodsName;

	/** 商品图片 */
	private String goodsImage;

	/** 消耗学币数量 */
	private Integer coins;

	/** 核销码（随机 6 位数字） */
	private String verifyCode;

	/** 状态 0待核销 1已核销 */
	private Integer status;

	/** 核销时间 */
	private Date verifyAt;

	/** 核销人 */
	private String verifyBy;

	/** 申请（创建）时间 */
	private Date createAt;

}
