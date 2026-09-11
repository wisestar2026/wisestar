package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.student.StudentTaskDTO;
import cn.wisestar.server.domain.dto.student.StudentTaskPublishDTO;
import cn.wisestar.server.domain.dto.student.StudentTaskQuery;
import cn.wisestar.server.domain.dto.student.StudentTaskView;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.StudentTask;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.StudentTaskMapper;
import cn.wisestar.server.service.StudentTaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ValidationException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 学员任务服务实现
 *
 * @author wisestar
 * @date 2026/9/3
 */
@Service
@RequiredArgsConstructor
public class StudentTaskServiceImpl implements StudentTaskService {

    /** 任务内容最大长度（与 t_student_task.task_content varchar(500) 对齐） */
    private static final int MAX_CONTENT_LENGTH = 500;

    /** 单个学员每日最多任务数 */
    private static final int MAX_DAILY_TASKS = 3;

    private final StudentTaskMapper studentTaskMapper;
    private final StudentMapper studentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignTasks(String schoolId, StudentTaskDTO request) {
        // 检查任务数量（最多 3 个）
        if (request.getTaskContents() == null || request.getTaskContents().length > 3) {
            return false;
        }

        // 检查该学员今日已有任务数
        String todayStart = LocalDate.now().atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Long todayTaskCount = studentTaskMapper.selectCount(
            new LambdaQueryWrapper<StudentTask>()
                .eq(StudentTask::getStudentId, request.getStudentId())
                .ge(StudentTask::getCreateTime, todayStart)
        );

        // 如果已有任务，检查总数是否超过 3 个
        if (todayTaskCount + request.getTaskContents().length > 3) {
            return false;
        }

        // 批量插入任务
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<StudentTask> tasks = new ArrayList<>();

        for (int i = 0; i < request.getTaskContents().length; i++) {
            StudentTask task = new StudentTask();
            task.setId(UUID.randomUUID().toString().replace("-", ""));
            task.setSchoolId(schoolId);
            task.setStudentId(request.getStudentId());
            task.setTaskContent(request.getTaskContents()[i]);
            task.setTaskType(request.getTaskTypes() != null && i < request.getTaskTypes().length 
                ? request.getTaskTypes()[i] : null);
            task.setTaskTarget(request.getTaskTargets() != null && i < request.getTaskTargets().length 
                ? request.getTaskTargets()[i] : null);
            task.setStatus("pending");
            task.setCreateBy(""); // TODO: 从登录用户获取
            task.setCreateTime(java.sql.Timestamp.valueOf(today));
            tasks.add(task);
        }

        for (StudentTask task : tasks) {
            studentTaskMapper.insert(task);
        }

        return true;
    }

    @Override
    public List<StudentTaskView> getStudentTasks(String studentId) {
        String todayStart = LocalDate.now().atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<StudentTask> tasks = studentTaskMapper.selectList(
            new LambdaQueryWrapper<StudentTask>()
                .eq(StudentTask::getStudentId, studentId)
                .ge(StudentTask::getCreateTime, todayStart)
                .orderByAsc(StudentTask::getCreateTime)
        );
        return toViews(tasks);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publishTasks(StudentTaskPublishDTO request) {
        if (request == null || request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
            throw new ValidationException("请至少选择一名学员");
        }
        // 清洗任务内容：去空白、丢弃空条目
        List<String> contents = new ArrayList<>();
        if (request.getContents() != null) {
            for (String raw : request.getContents()) {
                String text = raw == null ? "" : raw.trim();
                if (!text.isEmpty()) {
                    contents.add(text);
                }
            }
        }
        if (contents.isEmpty()) {
            throw new ValidationException("请至少填写一条任务内容");
        }
        if (contents.size() > MAX_DAILY_TASKS) {
            throw new ValidationException("单个学员每日最多发布 " + MAX_DAILY_TASKS + " 个任务");
        }
        for (String text : contents) {
            if (text.length() > MAX_CONTENT_LENGTH) {
                throw new ValidationException("任务内容不能超过 " + MAX_CONTENT_LENGTH + " 字");
            }
        }

        String operator = SecurityContextUtils.getUserId();
        // 去重且保持前端选择顺序
        Set<String> studentIds = new LinkedHashSet<>(request.getStudentIds());

        // 任一学员当日配额将超限则整批拒绝（事务回滚）
        Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        for (String studentId : studentIds) {
            if (studentId == null || studentId.trim().isEmpty()) {
                continue;
            }
            Long todayCount = studentTaskMapper.selectCount(new LambdaQueryWrapper<StudentTask>()
                    .eq(StudentTask::getStudentId, studentId.trim())
                    .ge(StudentTask::getCreateTime, todayStart));
            long existing = todayCount == null ? 0L : todayCount;
            if (existing + contents.size() > MAX_DAILY_TASKS) {
                throw new ValidationException("存在学员今日任务已达上限（每人每日最多 " + MAX_DAILY_TASKS + " 个），请调整后重试");
            }
        }

        int count = 0;
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        for (String studentId : studentIds) {
            if (studentId == null || studentId.trim().isEmpty()) {
                continue;
            }
            for (String content : contents) {
                StudentTask task = new StudentTask();
                task.setId(UUID.randomUUID().toString().replace("-", ""));
                task.setStudentId(studentId.trim());
                task.setTaskContent(content);
                task.setStatus("pending");
                task.setCreateBy(operator);
                task.setCreateTime(now);
                studentTaskMapper.insert(task);
                count++;
            }
        }
        return count;
    }

    @Override
    public PaginationResponse<StudentTaskView> pageTasks(StudentTaskQuery query) {
        LambdaQueryWrapper<StudentTask> wrapper = new LambdaQueryWrapper<StudentTask>()
            .eq(query.getStudentId() != null && !query.getStudentId().isEmpty(),
                StudentTask::getStudentId, query.getStudentId())
            .like(query.getContent() != null && !query.getContent().isEmpty(),
                StudentTask::getTaskContent, query.getContent())
            .eq(query.getStatus() != null && !query.getStatus().isEmpty(),
                StudentTask::getStatus, query.getStatus())
            .orderByDesc(StudentTask::getCreateTime);

        Page<StudentTask> page = new Page<>(query.getCurrent(), query.getPageSize());
        Page<StudentTask> result = studentTaskMapper.selectPage(page, wrapper);
        return new PaginationResponse<>(result.getTotal(), toViews(result.getRecords()));
    }

    @Override
    public boolean deleteTask(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        return studentTaskMapper.deleteById(id) > 0;
    }

    @Override
    public List<StudentTaskView> listMyTasks() {
        String studentId = SecurityContextUtils.getUserId();
        if (studentId == null || studentId.isEmpty()) {
            return new ArrayList<>();
        }
        // 仅返回当日任务，按创建时间升序
        Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<StudentTask> tasks = studentTaskMapper.selectList(
            new LambdaQueryWrapper<StudentTask>()
                .eq(StudentTask::getStudentId, studentId)
                .ge(StudentTask::getCreateTime, todayStart)
                .orderByAsc(StudentTask::getCreateTime)
        );
        return toViews(tasks);
    }

    /**
     * 批量转换为视图，一次性查询学员姓名/学号，避免 N+1。
     */
    private List<StudentTaskView> toViews(List<StudentTask> tasks) {
        List<StudentTaskView> result = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) {
            return result;
        }

        Set<String> studentIds = tasks.stream()
            .map(StudentTask::getStudentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<String, Student> studentMap = studentIds.isEmpty()
            ? Collections.emptyMap()
            : studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (StudentTask task : tasks) {
            StudentTaskView view = new StudentTaskView();
            view.setId(task.getId());
            view.setStudentId(task.getStudentId());
            Student student = studentMap.get(task.getStudentId());
            if (student != null) {
                view.setStudentName(student.getName());
                view.setStudentNo(student.getStudentNo());
            }
            view.setTaskContent(task.getTaskContent());
            view.setTaskType(task.getTaskType());
            view.setTaskTarget(task.getTaskTarget());
            view.setStatus(task.getStatus());
            if (task.getCreateTime() != null) {
                view.setCreateTime(formatter.format(task.getCreateTime()));
            }
            result.add(view);
        }
        return result;
    }

}
