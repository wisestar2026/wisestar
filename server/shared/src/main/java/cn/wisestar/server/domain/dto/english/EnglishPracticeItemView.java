package cn.wisestar.server.domain.dto.english;

import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import lombok.Data;

/**
 * 英语练习逐题作答快照。
 *
 * <p>既用于前端提交（{@code question} 可空），也用于落库回放「上一次答题情况」。
 * 题目快照含标准答案，仅供本人查看与错题重做。</p>
 *
 * @author wisestar
 * @date 2026/10/23
 */
@Data
public class EnglishPracticeItemView {

	/** 题目内容键（阅读理解为 reading:单元:篇号:题号） */
	private String questionId;

	/** 题型（Radio/FillBlank/MultipleBlank 等） */
	private String questionType;

	/** 章节（单元） */
	private String chapter;

	/** 知识点 */
	private String knowledgePoint;

	/** 学员作答展示文本 */
	private String studentAnswer;

	/** 标准答案展示文本（多空以 | 分隔） */
	private String correctAnswer;

	/** 解析 */
	private String analysis;

	/** 1 正确 / 0 错误 / null 未判分 */
	private Integer correct;

	/** 题目快照（含标准答案，用于错题重做） */
	private StudentQuestionView question;

}
