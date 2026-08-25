package cn.wisestar.server.domain.dto.task;

import lombok.Data;

import java.util.Date;

/**
 * 今日任务视图（后台管理）。
 *
 * @author wisestar
 * @date 2026/8/23
 */
@Data
public class TaskView {

	private String id;

	private String name;

	private String description;

	private String taskDate;

	private String contentType;

	private String contentId;

	private Integer status;

	private Integer sort;

	private Date createAt;

}
