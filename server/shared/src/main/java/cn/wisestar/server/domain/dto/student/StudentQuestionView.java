package cn.wisestar.server.domain.dto.student;

import cn.wisestar.server.domain.dto.SurveySchema;
import lombok.Data;

/**
 * 学员端题目视图（剥离标准答案，防作弊）。
 *
 * <p>作答时前端提交 {@code questionId + answer} 到 /api/practice/submit，
 * 由后端基于题库标准答案判分。</p>
 *
 * @author wisestar
 * @date 2026/8/20
 */
@Data
public class StudentQuestionView {

	private String id;

	/** 题干 */
	private String name;

	/** 题型 */
	private SurveySchema.QuestionType questionType;

	/** 标签 */
	private String[] tag;

	/** 题目内容（选项等；已剥离 attribute.examCorrectAnswer 与选项级答案标记） */
	private SurveySchema schema;

	/** 所属知识点ID（t_knowledge_point.id；题目未绑定知识点时为空） */
	private String knowledgePointId;

	/** 所属知识点名称（交卷回顾按知识点归纳、掌握度统计展示用） */
	private String knowledgePointName;

}
