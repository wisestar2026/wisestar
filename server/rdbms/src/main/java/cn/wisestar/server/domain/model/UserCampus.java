package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工-校区绑定关系（对应表 t_user_campus）。
 *
 * <p>一个员工账号可绑定多个校区；校长/教务/学管师账号的绑定校区
 * 构成其学员业务数据可见范围（停用校区仍保留绑定与范围）。</p>
 *
 * @author wisestar
 * @date 2026/9/8
 */
@Data
@TableName("t_user_campus")
@EqualsAndHashCode(callSuper = false)
public class UserCampus extends BaseModel {

	/**
	 * 员工用户ID（t_user.id）。
	 */
	private String userId;

	/**
	 * 校区ID（t_campus.id）。
	 */
	private String campusId;

}
