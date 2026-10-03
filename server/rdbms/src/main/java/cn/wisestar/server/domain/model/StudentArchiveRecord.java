package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学员上课记录/学习日志（t_student_archive_record）。
 *
 * <p>每次课一条，记录当日学习情况、解决的问题、强化的知识点、暴露的弱点与教师寄语；
 * 系统可按日期自动拉取当日学习数据作为草稿，老师编辑后定稿，逐条汇入学员档案。</p>
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Data
@TableName("t_student_archive_record")
@EqualsAndHashCode(callSuper = false)
public class StudentArchiveRecord extends BaseModel {

	/** 档案ID */
	private String archiveId;

	/** 学员ID */
	private String studentId;

	/** 上课日期 yyyy-MM-dd */
	private String recordDate;

	/** 学科ID */
	private String subjectId;

	/** 本次主题 */
	private String title;

	/** 当日学习情况（系统自动/老师编辑） */
	private String studySummary;

	/** 解决的问题 */
	private String solvedProblems;

	/** 强化的知识点 */
	private String strengthenedKps;

	/** 暴露的弱点 */
	private String weaknesses;

	/** 课后作业/任务 */
	private String homework;

	/** 教师寄语 */
	private String teacherComment;

	/** 本次时长（分钟） */
	private Integer durationMinutes;

	/** 获得积分 */
	private Integer points;

	/** 获得学习币 */
	private Integer coins;

	/** 来源 auto/manual */
	private String source;

	/** 状态 draft/final */
	private String status;

	/** 排序 */
	private Integer sort;

}
