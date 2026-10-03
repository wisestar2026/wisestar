package cn.wisestar.server.domain.dto.archive;

import cn.wisestar.server.domain.dto.student.StudySummaryView;
import cn.wisestar.server.domain.dto.student.StudentWeakView;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学员档案详情视图（管理端档案页 / 学员端我的档案）。
 *
 * <p>聚合档案主数据、当前薄弱知识点、初始档案快照、上课记录时间线与当日学习情况，
 * 供前端分区展示与打印。</p>
 *
 * @author wisestar
 * @date 2026/10/3
 */
@Data
public class StudentArchiveView {

	/** 档案ID（尚未创建时为 null） */
	private String id;

	private String studentId;

	private String studentNo;

	private String studentName;

	/** 学年，如 2026-2027 */
	private String schoolYear;

	/** 学期键，如 2026-1 */
	private String semester;

	/** 学期名称，如 第一学期 */
	private String termLabel;

	private String subjectId;

	private String teacherId;

	private String teacherName;

	/** draft/active/closed */
	private String status;

	/** 本学期目标规划表正文 */
	private String goalPlan;

	/** 学员承诺书正文 */
	private String promise;

	/** 学期报告正文 */
	private String reportContent;

	/** none/draft/final */
	private String reportStatus;

	private String remark;

	/** 初始档案快照中的薄弱知识点名称（创建档案时定格） */
	private List<String> initialWeakPoints = new ArrayList<>();

	/** 当前薄弱知识点（实时） */
	private List<StudentWeakView> weakPoints = new ArrayList<>();

	/** 上课记录/学习日志（按日期倒序） */
	private List<StudentArchiveRecordView> records = new ArrayList<>();

	/** 当日学习情况（复用学习总结，尚未生成时为 null） */
	private StudySummaryView today;

}
