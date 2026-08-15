package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 学员订单实体（对应数据库表 t_student_order，学员管理模块）。
 *
 * <p><b>权限载体</b>：创建订单时服务端在同一事务内写入 t_student_order
 * 与 t_student_permission（多选学科×年级笛卡尔积展开），权限有效期 expireAt
 * 由服务端按 duration + durationUnit 计算。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Data
@TableName("t_student_order")
@EqualsAndHashCode(callSuper = false)
public class StudentOrder extends BaseModel {

	/**
	 * 学员ID（t_student.id）。
	 */
	private String studentId;

	/**
	 * 学科ID多选（逗号分隔，t_subject.id）。
	 */
	private String subjectIds;

	/**
	 * 年级多选（逗号分隔）。
	 */
	private String grades;

	/**
	 * 教材版本（如 人教版/苏教版/北师大版/外研版）。
	 */
	private String version;

	/**
	 * 账号时长数值。
	 */
	private Integer duration;

	/**
	 * 时长单位（DAY/MONTH/YEAR）。
	 */
	private String durationUnit;

	/**
	 * 有效期至（服务端按时长计算）。
	 */
	private Date expireAt;

	/**
	 * 状态（1 生效 0 作废）。
	 */
	private Integer status;

}
