package cn.wisestar.server.domain.dto.handraise;

import lombok.Data;

/**
 * 学员举手反馈提交请求。
 *
 * <p>学员端在题目页点击「举手」时提交，携带题目快照与当前作答；
 * 服务端会按 questionId 重新回填题干/选项/标准答案/解析，客户端字段仅作为题目已删除时的兜底。</p>
 *
 * @author wisestar
 * @date 2026/10/22
 */
@Data
public class HandRaiseRequest {

	/** 反馈类型 explain / question_error / answer_error */
	private String raiseType;

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

	/** 题干快照 */
	private String questionContent;

	/** 选项快照 JSON */
	private String optionsJson;

	/** 学员作答展示文本 */
	private String studentAnswer;

	/** 学员选中选项ID（逗号分隔） */
	private String studentOptionIds;

	/** 标准答案展示文本 */
	private String correctAnswer;

	/** 正确选项ID（逗号分隔） */
	private String correctOptionIds;

	/** 解析快照 */
	private String answerAnalysis;

	/** 提交时页面标识 */
	private String page;

	/** 小节ID */
	private String sectionId;

	/** 学员补充说明 */
	private String remark;

}
