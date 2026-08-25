package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.task.StudentTaskView;
import cn.wisestar.server.domain.dto.task.TaskRequest;
import cn.wisestar.server.domain.dto.task.TaskView;

import java.util.List;

/**
 * 今日任务服务。
 *
 * @author wisestar
 * @date 2026/8/23
 */
public interface TaskService {

	/**
	 * 后台任务列表（按日期/名称过滤）。
	 *
	 * @param taskDate 任务日期（可选）
	 * @param name     任务名称模糊（可选）
	 * @return 任务列表（按日期倒序、sort 升序）
	 */
	List<TaskView> listTasks(String taskDate, String name);

	/**
	 * 新增任务。
	 *
	 * @param request 任务请求
	 */
	void createTask(TaskRequest request);

	/**
	 * 编辑任务。
	 *
	 * @param request 任务请求（含 id）
	 */
	void updateTask(TaskRequest request);

	/**
	 * 删除任务（逻辑删除）。
	 *
	 * @param request 任务请求（含 id）
	 */
	void deleteTask(TaskRequest request);

	/**
	 * 学员端当日任务（含完成状态：当日交卷且正确率≥60%）。
	 *
	 * @param taskDate 任务日期（默认今天）
	 * @return 当日已发布任务 + 学员完成情况
	 */
	List<StudentTaskView> studentTasks(String taskDate);

}
