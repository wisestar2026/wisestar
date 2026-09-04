package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学员任务视图
 *
 * @author wisestar
 * @date 2026/9/3
 */
@Data
public class StudentTaskView {

    /** 任务 ID */
    private String id;

    /** 学员 ID */
    private String studentId;

    /** 学员姓名 */
    private String studentName;

    /** 任务内容 */
    private String taskContent;

    /** 任务类型 */
    private String taskType;

    /** 任务目标 ID */
    private String taskTarget;

    /** 状态 */
    private String status;

    /** 创建时间 */
    private String createTime;

}
