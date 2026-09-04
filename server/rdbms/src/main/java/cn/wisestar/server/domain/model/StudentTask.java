package cn.wisestar.server.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 学员任务分配表
 *
 * @author wisestar
 * @date 2026/9/3
 */
@Data
@TableName("t_student_task")
public class StudentTask {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 学校 ID */
    private String schoolId;

    /** 学员 ID */
    private String studentId;

    /** 任务内容（文本） */
    private String taskContent;

    /** 任务类型（chapter/section/knowledge/exercise） */
    private String taskType;

    /** 任务目标 ID（章节/小节/知识点/习题 ID） */
    private String taskTarget;

    /** 状态（pending-待完成，completed-已完成） */
    private String status;

    /** 创建人（教师 ID） */
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;

}
