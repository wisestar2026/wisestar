package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.task.StudentTaskView;
import cn.wisestar.server.domain.dto.task.TaskRequest;
import cn.wisestar.server.domain.dto.task.TaskView;
import cn.wisestar.server.domain.mapper.TaskViewMapper;
import cn.wisestar.server.domain.model.PracticeRecord;
import cn.wisestar.server.domain.model.Task;
import cn.wisestar.server.mapper.PracticeRecordMapper;
import cn.wisestar.server.mapper.TaskMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.TaskService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 今日任务服务实现。
 *
 * @author wisestar
 * @date 2026/8/23
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends BaseService<TaskMapper, Task> implements TaskService {

	/** 及格线：正确率 60% */
	private static final double PASS_RATE = 0.6;

	private final TaskViewMapper taskViewMapper;

	private final PracticeRecordMapper practiceRecordMapper;

	@Override
	public List<TaskView> listTasks(String taskDate, String name) {
		List<Task> tasks = this.baseMapper.selectList(Wrappers.<Task>lambdaQuery()
				.eq(StringUtils.hasText(taskDate), Task::getTaskDate, taskDate)
				.like(StringUtils.hasText(name), Task::getName, name)
				.orderByDesc(Task::getTaskDate)
				.orderByAsc(Task::getSort));
		return taskViewMapper.toView(tasks);
	}

	@Override
	public void createTask(TaskRequest request) {
		validate(request);
		Task task = taskViewMapper.fromRequest(request);
		if (task.getStatus() == null) {
			task.setStatus(1);
		}
		if (task.getSort() == null) {
			task.setSort(1);
		}
		save(task);
	}

	@Override
	public void updateTask(TaskRequest request) {
		if (request.getId() == null) {
			throw new ValidationException("任务 ID 不能为空");
		}
		validate(request);
		updateById(taskViewMapper.fromRequest(request));
	}

	@Override
	public void deleteTask(TaskRequest request) {
		if (request.getId() == null) {
			throw new ValidationException("任务 ID 不能为空");
		}
		removeById(request.getId());
	}

	private void validate(TaskRequest request) {
		if (!StringUtils.hasText(request.getName())) {
			throw new ValidationException("任务名称不能为空");
		}
		if (!StringUtils.hasText(request.getTaskDate())) {
			throw new ValidationException("任务日期不能为空");
		}
		if (!StringUtils.hasText(request.getContentType()) || !StringUtils.hasText(request.getContentId())) {
			throw new ValidationException("请选择关联的练习或知识点");
		}
	}

	@Override
	public List<StudentTaskView> studentTasks(String taskDate) {
		String userId = SecurityContextUtils.getUserId();
		if (userId == null) {
			throw new ValidationException("请先登录");
		}
		String date = StringUtils.hasText(taskDate) ? taskDate : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
		List<Task> tasks = this.baseMapper.selectList(Wrappers.<Task>lambdaQuery()
				.eq(Task::getTaskDate, date)
				.eq(Task::getStatus, 1)
				.orderByAsc(Task::getSort));
		if (tasks.isEmpty()) {
			return java.util.Collections.emptyList();
		}
		// 今日 0 点
		Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		// 学员当日练习记录（一次查询，内存判定）
		List<PracticeRecord> records = practiceRecordMapper.selectList(Wrappers.<PracticeRecord>lambdaQuery()
				.eq(PracticeRecord::getUserId, userId)
				.ge(PracticeRecord::getCreateAt, todayStart));
		return tasks.stream().map(task -> {
			StudentTaskView view = new StudentTaskView();
			view.setId(task.getId());
			view.setName(task.getName());
			view.setDescription(task.getDescription());
			view.setContentType(task.getContentType());
			view.setContentId(task.getContentId());
			// 匹配相关练习记录（练习型按 repoId，知识点型按 knowledgePointId）
			List<PracticeRecord> matched = records.stream()
					.filter(r -> {
						if ("knowledge_point".equals(task.getContentType())) {
							return task.getContentId().equals(r.getKnowledgePointId());
						}
						return task.getContentId().equals(r.getRepoId());
					})
					.collect(Collectors.toList());
			// 最近一次正确率
			int rate = 0;
			if (!matched.isEmpty()) {
				PracticeRecord latest = matched.get(matched.size() - 1);
				if (latest.getTotalScore() != null && latest.getTotalScore() > 0) {
					rate = (int) Math.round((latest.getScore() == null ? 0 : latest.getScore()) * 100.0 / latest.getTotalScore());
				}
			}
			view.setCorrectRate(rate);
			view.setCompleted(rate >= PASS_RATE * 100);
			return view;
		}).collect(Collectors.toList());
	}
}
