package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.student.StudentTaskCompleteView;
import cn.wisestar.server.domain.dto.student.StudentTaskDTO;
import cn.wisestar.server.domain.dto.student.StudentTaskPublishDTO;
import cn.wisestar.server.domain.dto.student.StudentTaskQuery;
import cn.wisestar.server.domain.dto.student.StudentTaskView;
import cn.wisestar.server.service.StudentTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学员任务接口（管理端任务发布 + 学员端任务展示）
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

    /**
     * 任务发布：向多个学员各发布一条纯文本任务（学管师后台）。
     *
     * @param request 发布请求
     * @return 实际发布条数
     */
    @PostMapping("/publish")
    @PreAuthorize("hasAuthority('task:create')")
    public int publishTasks(@RequestBody StudentTaskPublishDTO request) {
        return taskService.publishTasks(request);
    }

    /**
     * 管理端任务分页列表（发布记录）。
     *
     * @param query 分页与筛选条件
     * @return 分页结果
     */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('task:list')")
    public PaginationResponse<StudentTaskView> pageTasks(StudentTaskQuery query) {
        return taskService.pageTasks(query);
    }

    /**
     * 删除（撤回）一条已发布任务。
     *
     * @param id 任务 ID
     * @return 是否成功
     */
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('task:delete')")
    public boolean deleteTask(@RequestParam String id) {
        return taskService.deleteTask(id);
    }

    /**
     * 学员端：当前登录学员收到的全部任务（按发布时间倒序）。
     *
     * @return 任务列表
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public List<StudentTaskView> listMyTasks() {
        return taskService.listMyTasks();
    }

    /**
     * 学员端：完成一条任务并结算学习币。
     *
     * @param id 任务 ID
     * @return 完成结果（含到账学习币）
     */
    @PostMapping("/complete")
    @PreAuthorize("isAuthenticated()")
    public StudentTaskCompleteView completeTask(@RequestParam String id) {
        return taskService.completeTask(id);
    }

}
