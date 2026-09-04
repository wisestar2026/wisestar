package cn.wisestar.server.api;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.student.StudentTaskDTO;
import cn.wisestar.server.domain.dto.student.StudentTaskView;
import cn.wisestar.server.service.StudentTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学员任务分配接口
 *
 * @author wisestar
 * @date 2026/9/3
 */
@RestController
@RequestMapping("${api.prefix}/student/task")
@RequiredArgsConstructor
public class StudentTaskApi {

    private final StudentTaskService taskService;

    /**
     * 给学生分配任务（同一学生最多 3 个任务）
     *
     * @param request 任务请求
     * @return 是否成功
     */
    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('student:task:assign')")
    public boolean assignTasks(@RequestBody StudentTaskDTO request) {
        String schoolId = ""; // TODO: 从登录用户获取学校 ID
        return taskService.assignTasks(schoolId, request);
    }

    /**
     * 获取学员今日任务
     *
     * @param studentId 学员 ID
     * @return 任务列表
     */
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public List<StudentTaskView> getStudentTasks(@RequestParam String studentId) {
        return taskService.getStudentTasks(studentId);
    }

}
