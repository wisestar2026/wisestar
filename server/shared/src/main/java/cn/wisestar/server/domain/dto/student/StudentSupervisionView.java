package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学员督学视图（教师端查看学员学习状态）
 *
 * @author wisestar
 * @date 2026/9/3
 */
@Data
public class StudentSupervisionView {

    /** 学员 ID */
    private String studentId;

    /** 学员姓名 */
    private String studentName;

    /** 学号 */
    private String studentNo;

    /** 当前状态：learning-学习中，exercising-做题中，offline-离线 */
    private String status;

    /** 当前学习位置（章节/小节/知识点/题目 ID） */
    private String currentLocation;

    /** 章节名称（如果在学习章节） */
    private String chapterName;

    /** 小节名称（如果在学习小节） */
    private String sectionName;

    /** 知识点名称（如果在学习知识点） */
    private String knowledgePointName;

    /** 题目内容（如果在做题） */
    private String questionContent;

    /** 学员答案（如果在做题） */
    private String studentAnswer;

    /** 正确答案（如果在做题） */
    private String correctAnswer;

    /** 答案解析（如果在做题） */
    private String answerAnalysis;

    /** 最后活跃时间 */
    private String lastActiveTime;

}
