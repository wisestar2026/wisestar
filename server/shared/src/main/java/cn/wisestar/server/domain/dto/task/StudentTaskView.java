package cn.wisestar.server.domain.dto.task;

import lombok.Data;

/**
 * 学员端当日任务视图（含完成状态与正确率）。
 *
 * @author wisestar
 * @date 2026/8/23
 */
@Data
public class StudentTaskView {

	private String id;

	private String name;

	private String description;

	/** 内容类型 practice练习 / knowledge_point知识点 */
	private String contentType;

	/** 关联内容ID */
	private String contentId;

	/** 是否完成（当日交卷且正确率≥60%） */
	private boolean completed;

	/** 最近一次相关练习正确率（0-100，未做为 0） */
	private int correctRate;

}
