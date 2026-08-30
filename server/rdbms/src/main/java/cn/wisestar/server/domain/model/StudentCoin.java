package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学员学币发放记录实体（对应数据库表 t_student_coin，老师手动加学币）。
 *
 * @author wisestar
 * @date 2026/8/29
 */
@Data
@TableName("t_student_coin")
@EqualsAndHashCode(callSuper = false)
public class StudentCoin extends BaseModel {

	/** 学员ID（t_student.id） */
	private String studentId;

	/** 学币数量（正加负扣） */
	private Integer coins;

	/** 发放原因 */
	private String reason;

	@TableLogic
	private Boolean deleted = false;

}
