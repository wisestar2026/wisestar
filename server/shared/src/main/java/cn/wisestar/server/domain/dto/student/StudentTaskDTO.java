package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学员任务分配 DTO
 *
 * @author wisestar
 * @date 2026/9/3
 */
@Data
public class StudentTaskDTO {

    /** 学员 ID */
    private String studentId;

    /** 任务内容数组（最多 3 个任务） */
    private String[] taskContents;

    /** 任务类型数组 */
    private String[] taskTypes;

    /** 任务目标 ID 数组 */
    private String[] taskTargets;

}
