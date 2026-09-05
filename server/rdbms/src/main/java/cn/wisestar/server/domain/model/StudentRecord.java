package cn.wisestar.server.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 学员学习记录（记录学员学习轨迹）
 *
 * @author wisestar
 * @date 2026/9/3
 */
@Data
@TableName("t_student_record")
public class StudentRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 学员 ID */
    private String studentId;

    /** 当前学习位置（chapter/ID, section/ID, knowledge/ID, exercise/ID） */
    private String currentLocation;

    /** 最后活跃时间 */
    private String lastActiveTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateAt;

    @TableLogic
    private Integer isDeleted;

}
