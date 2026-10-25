package cn.wisestar.server.domain.dto.student;

import cn.wisestar.server.domain.dto.SurveySchema;
import lombok.Data;

import java.util.List;

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

	/**
	 * 所属知识点ID（专项练习按知识点分组时返回，用于学员端展示「当前知识点」单元）
	 */
	private String knowledgePointId;

	/** 知识点名称（解析中展示「本题知识点」用，无知识点时为空） */
	private String knowledgePointName;

	/** 知识点要点列表（取自知识点内容配置的 points，最多下发前若干条） */
	private List<String> knowledgePointPoints;

	/** 题目内容（选项等；已剥离 attribute.examCorrectAnswer 与选项级答案标记） */
	private SurveySchema schema;

}
