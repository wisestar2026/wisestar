package cn.wisestar.server.domain.dto.handraise;

import lombok.Data;

import java.util.Date;

/**
 * 举手反馈视图（教师端提醒列表 + 作答详情弹窗）。
 *
 * @author wisestar
 * @date 2026/10/22
 */
@Data
public class HandRaiseView {

	private String id;

	/** 反馈类型 explain / question_error / answer_error */
	private String raiseType;

	/** 处理状态 0待处理 2已处理 3已忽略 */
	private Integer status;

	/** 学员ID */
	private String studentId;

	/** 学员姓名 */
	private String studentName;

	/** 学号 */
	private String studentNo;

	/** 学员所属校区 */
	private String campus;

	/** 题目ID */
	private String questionId;

	/** 题型 */
	private String questionType;

	/** 学科 */
	private String subject;

	/** 章节 */
	private String chapter;

	/** 知识点 */
	private String knowledgePoint;

	/** 难度 */
	private String difficulty;

	/** 题干 */
	private String questionContent;

	/** 选项 JSON：[{"id":"...","title":"..."}] */
	private String optionsJson;

	/** 学员作答展示文本 */
	private String studentAnswer;

	/** 学员选中选项ID（逗号分隔） */
	private String studentOptionIds;

	/** 标准答案展示文本 */
	private String correctAnswer;

	/** 正确选项ID（逗号分隔） */
	private String correctOptionIds;

	/** 解析 */
	private String answerAnalysis;

	/** 提交页面 */
	private String page;

	/** 学员补充说明 */
	private String remark;

	/** 处理人姓名 */
	private String handlerName;

	/** 处理时间 */
	private Date handleAt;

	/** 处理备注 */
	private String handleNote;

	/** 提交时间 */
	private Date createAt;

}
