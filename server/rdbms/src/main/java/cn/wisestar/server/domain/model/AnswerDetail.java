package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 答题明细表实体（对应数据库表 t_answer_detail）：一次答卷中单道题的作答记录。
 *
 * 【类职责】
 * 把一次答卷（t_answer）按题目拆成一条条明细，每条记录"某学生在某次答卷中对某道题的
 * 作答结果"，是学生答题情况分析（优势/薄弱知识点，AnalysisServiceImpl）的数据基础。
 * 由 AnswerServiceImpl.generateAnswerDetails 在提交答卷（tempSave=1）时按题生成。
 *
 * 【被谁调用】
 * - 写入方：AnswerServiceImpl.generateAnswerDetails（先按 answerId 删除旧明细再逐条插入，
 *   保证重复提交幂等）
 * - 读取方：AnalysisServiceImpl.aggregate（按学生/学科/章节筛选聚合）、
 *   AnswerDetailMapper（MyBatis-Plus 基础 CRUD）
 *
 * 【依赖什么】
 * - 继承 BaseModel：id、createAt（答题时间）、createBy（学生ID，MyBatis-Plus 自动填充）、
 *   updateAt、updateBy、deleted 逻辑删除字段
 * - 数据来源：提交答卷时从 Answer（survey 问卷快照 + answer 答案 Map）中按题提取
 *
 * 【核心数据流】
 * 学生提交答卷（Controller）→ AnswerServiceImpl.saveAnswer/updateAnswer
 * → generateAnswerDetails：遍历问卷快照中的每一题，取题目知识点四维信息（attribute 快照）、
 * 学生答案、对错判定结果、分值 → 逐条 insert 到 t_answer_detail
 * → AnalysisServiceImpl 按 createBy(学生) + subject/chapter 筛选，knowledgePoint 逗号拆分后聚合统计。
 *
 * 【存储格式说明】
 * - knowledgePoint 列：多值知识点以英文逗号分隔存为字符串（如 "函数单调性,奇偶性"）
 * - isCorrect 列：null=无标准答案不计分，1=正确，0=错误
 * - createBy 列即学生ID（答题人），聚合分析时以此为归属依据
 *
 * @author zhanghaiyang
 * @date 2026/8/1
 */
@Data
@TableName("t_answer_detail")
@EqualsAndHashCode(callSuper = false)
public class AnswerDetail extends BaseModel {

	/**
	 * 答卷 ID（对应 t_answer.id）。
	 * 一条答卷明细记录归属的整卷；generateAnswerDetails 先按此字段删除旧明细，
	 * 实现"先删后插"的幂等更新。
	 */
	private String answerId;

	/**
	 * 问卷/考试项目 ID（对应 t_project.id）。
	 * 冗余存储答卷所属项目，便于按项目维度筛选分析数据。
	 */
	private String projectId;

	/**
	 * 题目节点 ID（问卷 schema 内的 q_xxx 标识）。
	 * 对应 SurveySchema 中某一题（flatten 后）的 id，用于关联回问卷/题库中的原始题目。
	 */
	private String questionId;

	/**
	 * 题型（对应列 question_type，字符串存枚举 name）。
	 * 如 Radio/Checkbox/Judge/FillBlank/Textarea，来源于问卷快照中题目的 type。
	 */
	private String questionType;

	/**
	 * 学科快照（对应列 subject）：答题时对题目学科维度的快照拷贝。
	 * 即使题目后续被修改，历史明细仍保留作答当时的学科值，保证分析口径一致。
	 */
	private String subject;

	/**
	 * 章节快照（对应列 chapter）：答题时对题目章节维度的快照拷贝。
	 */
	private String chapter;

	/**
	 * 知识点快照（对应列 knowledge_point，多值逗号分隔字符串，如 "函数,不等式"）。
	 * 来源于题目 attribute.knowledgePoint 数组 join 逗号；聚合分析时再按逗号拆开，
	 * 一道题挂多个知识点时会计入多个知识点统计。
	 */
	private String knowledgePoint;

	/**
	 * 学生答案（对应列 user_answer，字符串）。
	 * 由 formatAnswerValue 格式化：Map 结构（原生结构 optionId->value）取 values 逗号连接、
	 * 集合按逗号连接、普通值 toString。
	 */
	private String userAnswer;

	/**
	 * 是否正确（对应列 is_correct）：null=无标准答案不计分，1=正确，0=错误。
	 * 由 evaluateQuestionCorrect 判定：单选/判断/填空按文本匹配，多选按选项集合相等（与顺序无关）。
	 */
	private Integer isCorrect;

	/**
	 * 得分（对应列 score，Double）。
	 * 优先取考试模式 examInfo.questionScore 中该题的每题分值，其次取题目自带
	 * attribute.examScore；非考试题可为 null。
	 */
	private Double score;

	/**
	 * 用时（对应列 duration_ms，Long 毫秒，可选）。
	 * 预留字段，用于后续作答时长维度的分析；当前生成明细时未写入。
	 */
	private Long durationMs;

}
