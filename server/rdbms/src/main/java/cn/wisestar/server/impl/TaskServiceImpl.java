package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.task.StudentTaskView;
import cn.wisestar.server.domain.dto.task.TaskRequest;
import cn.wisestar.server.domain.dto.task.TaskView;
import cn.wisestar.server.domain.mapper.TaskViewMapper;
import cn.wisestar.server.domain.model.PracticeRecord;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.Task;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.PracticeRecordMapper;
import cn.wisestar.server.mapper.RepoMapper;
import cn.wisestar.server.mapper.StudentMapper;
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
import java.util.HashMap;
import java.util.Map;
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

	private final StudentMapper studentMapper;

	private final RepoMapper repoMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	@Override
	public List<TaskView> listTasks(String taskDate, String name) {
		List<Task> tasks = this.baseMapper.selectList(Wrappers.<Task>lambdaQuery()
				.eq(StringUtils.hasText(taskDate), Task::getTaskDate, taskDate)
				.like(StringUtils.hasText(name), Task::getName, name)
				.orderByDesc(Task::getTaskDate)
				.orderByAsc(Task::getSort));
		List<TaskView> views = taskViewMapper.toView(tasks);
		fillStudentName(views);
		return views;
	}

	/** 回填绑定学员姓名 */
	private void fillStudentName(List<TaskView> views) {
		java.util.Set<String> studentIds = views.stream().map(TaskView::getStudentId)
				.filter(StringUtils::hasText).collect(Collectors.toSet());
		if (studentIds.isEmpty()) {
			return;
		}
		Map<String, String> nameMap = studentMapper.selectBatchIds(studentIds).stream()
				.collect(Collectors.toMap(Student::getId, Student::getName, (a, b) -> a));
		views.forEach(v -> v.setStudentName(nameMap.get(v.getStudentId())));
	}

	@Override
	public void createTask(TaskRequest request) {
		validate(request);
		// 每学员每日最多 3 个任务
		Long count = this.baseMapper.selectCount(Wrappers.<Task>lambdaQuery()
				.eq(Task::getStudentId, request.getStudentId())
				.eq(Task::getTaskDate, request.getTaskDate()));
		if (count != null && count >= MAX_DAILY_TASKS) {
			throw new ValidationException("该学员当日任务已达上限（最多 3 个）");
		}
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
	public void batchCreateTasks(List<TaskRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			throw new ValidationException("请至少布置一个任务");
		}
		String firstStudentId = requests.get(0).getStudentId();
		String firstDate = requests.get(0).getTaskDate();
		boolean sameTarget = requests.stream().allMatch(r ->
				firstStudentId.equals(r.getStudentId()) && firstDate.equals(r.getTaskDate()));
		if (!sameTarget) {
			requests.forEach(this::createTask);
			return;
		}
		Long existing = this.baseMapper.selectCount(Wrappers.<Task>lambdaQuery()
				.eq(Task::getStudentId, firstStudentId)
				.eq(Task::getTaskDate, firstDate));
		long total = (existing == null ? 0 : existing) + requests.size();
		if (total > MAX_DAILY_TASKS) {
			throw new ValidationException("该学员当日任务已达上限（最多 3 个）");
		}
		for (TaskRequest request : requests) {
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

	/** 每学员每日最多任务数 */
	private static final int MAX_DAILY_TASKS = 3;

	private void validate(TaskRequest request) {
		if (!StringUtils.hasText(request.getStudentId())) {
			throw new ValidationException("请选择绑定学员");
		}
		if (!StringUtils.hasText(request.getTaskDate())) {
			throw new ValidationException("任务日期不能为空");
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
				.eq(Task::getStudentId, userId)
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
		java.util.Map<String, String> repoNameMap = new java.util.HashMap<>();
		java.util.Map<String, String> kpNameMap = new java.util.HashMap<>();
		java.util.Set<String> repoIds = tasks.stream().filter(t -> "practice".equals(t.getContentType()))
				.map(Task::getContentId).filter(StringUtils::hasText).collect(Collectors.toSet());
		if (!repoIds.isEmpty()) {
			repoMapper.selectBatchIds(repoIds).forEach(r -> repoNameMap.put(r.getId(), r.getName()));
		}
		java.util.Set<String> kpIds = tasks.stream().filter(t -> "knowledge_point".equals(t.getContentType()))
				.map(Task::getContentId).filter(StringUtils::hasText).collect(Collectors.toSet());
		if (!kpIds.isEmpty()) {
			knowledgePointMapper.selectBatchIds(kpIds).forEach(k -> kpNameMap.put(k.getId(), k.getName()));
		}
		return tasks.stream().map(task -> {
			StudentTaskView view = new StudentTaskView();
			view.setId(task.getId());
			view.setStudentId(task.getStudentId());
			view.setName(task.getName());
			view.setDescription(task.getDescription());
			view.setContentType(task.getContentType());
			view.setContentId(task.getContentId());
			view.setContentName("practice".equals(task.getContentType())
					? repoNameMap.get(task.getContentId())
					: kpNameMap.get(task.getContentId()));
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
