package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.student.StudentTaskDTO;
import cn.wisestar.server.domain.dto.student.StudentTaskPublishDTO;
import cn.wisestar.server.domain.dto.student.StudentTaskQuery;
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

    /**
     * 任务发布：向多个学员各发布一条纯文本任务。
     *
     * @param request 发布请求（学员 ID 列表 + 文本内容）
     * @return 实际发布条数（即学员数）
     */
    int publishTasks(StudentTaskPublishDTO request);

    /**
     * 管理端任务分页列表（发布记录）。
     *
     * @param query 分页与筛选条件
     * @return 分页结果
     */
    PaginationResponse<StudentTaskView> pageTasks(StudentTaskQuery query);

    /**
     * 删除（撤回）一条已发布任务。
     *
     * @param id 任务 ID
     * @return 是否成功
     */
    boolean deleteTask(String id);

    /**
     * 学员端：当前登录学员收到的全部任务（按发布时间倒序）。
     *
     * @return 任务列表
     */
    List<StudentTaskView> listMyTasks();

}
