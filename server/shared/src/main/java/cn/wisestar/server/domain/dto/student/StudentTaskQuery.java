package cn.wisestar.server.domain.dto.student;

import cn.wisestar.server.domain.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学员任务分页查询 DTO（管理端任务发布列表）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class StudentTaskQuery extends PageQuery {

    /** 学员 ID（精确） */
    private String studentId;

    /** 任务内容关键字（模糊） */
    private String content;

    /** 状态（pending/completed） */
    private String status;

}
