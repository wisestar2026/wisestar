package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.task.StudentTaskView;
import cn.wisestar.server.domain.dto.task.TaskRequest;
import cn.wisestar.server.domain.dto.task.TaskView;
import cn.wisestar.server.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 今日任务接口（后台布置 + 学员端呈现）。
 *
 * @author wisestar
 * @date 2026/8/23
 */
@RestController
@RequestMapping("${api.prefix}/task")
@RequiredArgsConstructor
public class TaskApi {

	private final TaskService taskService;

	/**
	 * 后台任务列表（task:list）。
	 *
	 * @param taskDate 任务日期（可选）
	 * @param name     任务名称模糊（可选）
	 * @return 任务列表
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('task:list')")
	public List<TaskView> listTasks(@RequestParam(required = false) String taskDate,
			@RequestParam(required = false) String name) {
		return taskService.listTasks(taskDate, name);
	}

	/**
	 * 新增任务（task:create）。
	 */
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('task:create')")
	public void createTask(@RequestBody TaskRequest request) {
		taskService.createTask(request);
	}

	/**
	 * 批量布置任务（同一弹窗一次布置多条，task:create）。
	 *
	 * @param requests 任务请求列表
	 */
	@PostMapping("/batchCreate")
	@PreAuthorize("hasAuthority('task:create')")
	public void batchCreateTasks(@RequestBody List<TaskRequest> requests) {
		taskService.batchCreateTasks(requests);
	}

	/**
	 * 编辑任务（task:update）。
	 */
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('task:update')")
	public void updateTask(@RequestBody TaskRequest request) {
		taskService.updateTask(request);
	}

	/**
	 * 删除任务（task:delete）。
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('task:delete')")
	public void deleteTask(@RequestBody TaskRequest request) {
		taskService.deleteTask(request);
	}

	/**
	 * 学员端当日任务（含完成状态：当日交卷且正确率≥60%）。
	 *
	 * @param taskDate 任务日期（默认今天）
	 * @return 当日已发布任务 + 学员完成情况
	 */
	@GetMapping("/student/tasks")
	@PreAuthorize("isAuthenticated()")
	public List<StudentTaskView> studentTasks(@RequestParam(required = false) String taskDate) {
		return taskService.studentTasks(taskDate);
	}

}
