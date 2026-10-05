package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 学员档案（t_student_archive）。
 *
 * <p>按「学员 + 学期」唯一，承载本学期目标规划表、承诺书、初始档案快照（薄弱知识点）
 * 与学期报告正文；来源为知识点检测结果 + 老师填写，整体可打印交付家长。</p>
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Data
@TableName("t_student_archive")
@EqualsAndHashCode(callSuper = false)
public class StudentArchive extends BaseModel {

	/** 学员ID */
	private String studentId;

	/** 学号快照 */
	private String studentNo;

	/** 姓名快照 */
	private String studentName;

	/** 学年，如 2026-2027 */
	private String schoolYear;

	/** 学期键，如 2026-1 */
	private String semester;

	/** 学期名称，如 第一学期 */
	private String termLabel;

	/** 学科ID（空表示全科档案） */
	private String subjectId;

	/** 负责老师ID */
	private String teacherId;

	/** 负责老师姓名 */
	private String teacherName;

	/** 状态 draft/active/closed */
	private String status;

	/** 初始档案快照（JSON：基线快照，含基线薄弱知识点/正确率） */
	private String profileSnapshot;

	/** 定格为基线的检测记录ID */
	private String baselineDetectId;

	/** 基线定格时间 */
	private Date baselineAt;

	/** 本学期目标规划表正文 */
	private String goalPlan;

	/** 学员承诺书正文 */
	private String promise;

	/** 学期报告正文 */
	private String reportContent;

	/** 学期报告状态 none/draft/final */
	private String reportStatus;

	/** 报告生成模型（ai 模型名或 rule） */
	private String reportModel;

	/** 报告生成时间 */
	private Date reportGeneratedAt;

	/** 备注 */
	private String remark;

}
