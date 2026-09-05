package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.student.StudentTaskDTO;
import cn.wisestar.server.domain.dto.student.StudentTaskView;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.StudentTask;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.StudentTaskMapper;
import cn.wisestar.server.service.StudentTaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 学员任务服务实现
 *
 * @author wisestar
 * @date 2026/9/3
 */
@Service
@RequiredArgsConstructor
public class StudentTaskServiceImpl implements StudentTaskService {

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
            task.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
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

        List<StudentTaskView> result = new ArrayList<>();
        for (StudentTask task : tasks) {
            StudentTaskView view = new StudentTaskView();
            view.setId(task.getId());
            view.setStudentId(task.getStudentId());
            Student student = studentMapper.selectById(task.getStudentId());
            if (student != null) {
                view.setStudentName(student.getName());
            }
            view.setTaskContent(task.getTaskContent());
            view.setTaskType(task.getTaskType());
            view.setTaskTarget(task.getTaskTarget());
            view.setStatus(task.getStatus());
            if (task.getCreateTime() != null) {
                view.setCreateTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(task.getCreateTime()));
            }
            result.add(view);
        }

        return result;
    }

}
