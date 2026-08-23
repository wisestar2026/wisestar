package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学员实时位置实体（对应数据库表 t_student_activity，学员动态监控模块）。
 *
 * <p><b>用途</b>：学员端在路由变化/进入习题时上报当前位置（page/questionId），
 * 后台老师按学员实时查看其在哪个页面、哪道习题，并可查看该习题答案与解析。</p>
 *
 * @author wisestar
 * @date 2026/8/21
 */
@Data
@TableName("t_student_activity")
@EqualsAndHashCode(callSuper = false)
public class StudentActivity extends BaseModel {

	/**
	 * 学员ID（t_student.id）。
	 */
	private String studentId;

	/**
	 * 当前页面标识（学员端路由路径，如 /student/study、/student/knowledge 等）。
	 */
	private String page;

	/**
	 * 当前习题ID（t_template.id，可为空——不在习题中时为空）。
	 */
	private String questionId;

	/**
	 * 小节ID（习题上下文，可为空）。
	 */
	private String sectionId;

}
