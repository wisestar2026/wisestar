package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户学海积分/头衔实体（对应数据库表 t_user_points，终身、全学科，仅作荣誉评价）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
@TableName("t_user_points")
@EqualsAndHashCode(callSuper = false)
public class UserPoints extends BaseModel {

	/** 学员ID */
	private String userId;

	/** 累计学海积分（终身、全学科、无上限） */
	private Integer points;

	/** 头衔等级 1-5 */
	private Integer titleLevel;

	/** 头衔名称 */
	private String titleName;

}
