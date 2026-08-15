package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 学员权限实体（对应数据库表 t_student_permission，学员管理模块）。
 *
 * <p><b>用途</b>：订单中多选学科×多选年级按笛卡尔积展开为权限行，
 * 学员端鉴权按 student_id + subject_id + grade 且 expire_at &gt; NOW() 查询。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Data
@TableName("t_student_permission")
@EqualsAndHashCode(callSuper = false)
public class StudentPermission extends BaseModel {

	/**
	 * 学员ID（t_student.id）。
	 */
	private String studentId;

	/**
	 * 来源订单ID（t_student_order.id）。
	 */
	private String orderId;

	/**
	 * 学科ID（t_subject.id）。
	 */
	private String subjectId;

	/**
	 * 年级。
	 */
	private String grade;

	/**
	 * 教材版本。
	 */
	private String version;

	/**
	 * 有效期至（服务端按订单时长计算）。
	 */
	private Date expireAt;

}
