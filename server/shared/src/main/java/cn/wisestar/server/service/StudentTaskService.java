package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.student.StudentTaskDTO;
import cn.wisestar.server.domain.dto.student.StudentTaskView;

import java.util.List;

/**
 * 学员任务服务
 *
 * @author wisestar
 * @date 2026/9/3
 */
public interface StudentTaskService {

    /**
     * 给学生分配任务（同一学生最多 3 个任务）
     *
     * @param schoolId 学校 ID
     * @param request 任务请求
     * @return 是否成功
     */
    boolean assignTasks(String schoolId, StudentTaskDTO request);

    /**
     * 获取学员今日任务
     *
     * @param studentId 学员 ID
     * @return 任务列表
     */
    List<StudentTaskView> getStudentTasks(String studentId);

}
