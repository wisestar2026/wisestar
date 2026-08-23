package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.Date;

/**
 * 学员实时位置视图（后台老师监控用）。
 *
 * @author wisestar
 * @date 2026/8/21
 */
@Data
public class StudentActivityView {

	private String studentId;

	/** 学号 */
	private String studentNo;

	/** 学员姓名 */
	private String studentName;

	/** 当前页面标识 */
	private String page;

	/** 当前习题ID（可为空） */
	private String questionId;

	/** 习题标题（questionId 有值时回填，供监控页直接展示） */
	private String questionTitle;

	/** 小节ID（可为空） */
	private String sectionId;

	/** 最后活跃时间 */
	private Date updateAt;

}
