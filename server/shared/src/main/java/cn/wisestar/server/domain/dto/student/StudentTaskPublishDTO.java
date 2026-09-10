package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.List;

/**
 * 学员任务发布 DTO（学管师向学员下发纯文本任务）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentTaskPublishDTO {

    /** 目标学员 ID 列表（支持一次发布给多个学员） */
    private List<String> studentIds;

    /** 任务内容（纯文本，不绑定任何练习/章节） */
    private String content;

}
