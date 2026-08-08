package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.constant.ExamExerciseTypeEnum;
import cn.wisestar.server.core.model.BaseModel;
import cn.wisestar.server.domain.dto.AnswerExamInfo;
import cn.wisestar.server.domain.dto.AnswerMetaInfo;
import cn.wisestar.server.domain.dto.SurveySchema;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

import java.util.Date;
import java.util.LinkedHashMap;

/**
 * 答卷实体（对应数据库表 t_answer，autoResultMap=true 支持 JSON 类型字段自动映射）。
 *
 * 【类职责】
 * 记录一次问卷/考试/练习的完整答卷：暂存答案（未提交）、最终答案、问卷快照、
 * 考试得分与答题元信息（作答起止时间、客户端信息等）。是系统"答卷"概念的核心载体，
 * 也是答题明细（t_answer_detail）生成的数据源。
 *
 * 【被谁调用】
 * - 数据访问层：AnswerMapper、AnswerDetailMapper（MyBatis-Plus）
 * - 业务层：AnswerServiceImpl（保存/查询/导出/删除答卷）、SurveyServiceImpl（答卷相关
 *   公开操作）、RepoServiceImpl（错题本保存临时答案）、RandomSurveyProcessor（随机抽题
 *   时保存随机 schema 到答案表）、UserServiceImpl（历史任务查询）
 *
 * 【依赖什么】
 * - 继承 BaseModel：id（主键）、createAt、createBy、updateAt、updateBy、deleted
 * - 题目/问卷结构复用 SurveySchema；答案结构 LinkedHashMap（questionId -> optionId -> value）
 * - AnswerMetaInfo：作答元信息（开始/结束时间、客户端等）
 * - AnswerExamInfo：考试信息（每题得分 questionScore）
 *
 * 【核心数据流】
 * 学生作答（Controller）→ SurveyServiceImpl.saveAnswer → AnswerServiceImpl.saveAnswer/
 * updateAnswer → 计算考试分值 computeExamScore → 更新关联问卷答案 updateLinkSurveyAnswer
 * → 保存本实体 → generateAnswerDetails 按题生成明细到 t_answer_detail → 返回 AnswerView。
 *
 * @author javahuang
 * @date 2021/8/6
 */
@Data
@TableName(value = "t_answer", autoResultMap = true)
@EqualsAndHashCode(callSuper = false)
public class Answer extends BaseModel {

	/**
	 * 主键（对应列 id，ASSIGN_UUID 雪花/唯一ID）。
	 * 注意：公开答卷提交时 AnswerServiceImpl.saveAnswer 会强制覆盖为 UUID 字符串，
	 * 用作外部公开查询引用，防暴力破解。
	 */
	@TableId(type = IdType.ASSIGN_UUID)
	private String id;

	/**
	 * 所属问卷/考试项目 ID（对应列 project_id，关联 t_project.id）。
	 * 练习模式的固定项目为 ExerciseProjectTemplate.EXERCISE_PROJECT_ID。
	 */
	private String projectId;

	/**
	 * 所属题库 ID（对应列 repo_id，关联 t_repo.id，练习模式才有值）。
	 */
	private String repoId;

	/**
	 * 暂存答案（对应列 temp_answer，LONGVARCHAR JSON，Jackson 序列化）。
	 * 结构：{questionId: {optionId: value}}；未提交（tempSave=0）时保存的中间答案，
	 * 提交时合并进 answer。
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.LONGVARCHAR)
	private LinkedHashMap tempAnswer;

	/**
	 * 最终答案（对应列 answer，LONGVARCHAR JSON）。
	 * 结构：{questionId: {optionId: value}}；问卷题 value 为选项ID映射，签名/上传题 value
	 * 为 {optionId: [fileId,...]}。listAnswer 查询以该字段非空作为"有效答卷"过滤条件。
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.LONGVARCHAR)
	private LinkedHashMap answer;

	/**
	 * 问卷快照（对应列 survey，LONGVARCHAR JSON，Jackson 序列化 SurveySchema 全量结构）。
	 * 保存作答当时的完整问卷/随机抽题结构（随机卷每人一份），是答题明细生成
	 * （generateAnswerDetails 遍历）与答案回显（getAnswer / loadProject）的数据源。
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.LONGVARCHAR)
	private SurveySchema survey;

	/**
	 * 作答元信息（对应列 meta_info，LONGVARCHAR JSON）。
	 * AnswerMetaInfo：开始/结束时间、客户端信息（浏览器、IP 等），
	 * count 查询可按 metaInfo 字段 like 匹配 ip/cookie 做限制校验。
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.LONGVARCHAR)
	private AnswerMetaInfo metaInfo;

	/**
	 * 考试分数（对应列 exam_score，Double）。
	 * 考试模式下由 computeExamScore 调用 AnswerScoreEvaluator 计算得出，用于排名/成绩单。
	 */
	private Double examScore;

	/**
	 * 考试信息（对应列 exam_info，LONGVARCHAR JSON）。
	 * AnswerExamInfo.questionScore：每题得分 Map{questionId: score}，
	 * 前端用于展示每道题得分/对错，也是生成答题明细分值的数据源之一。
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.LONGVARCHAR)
	private AnswerExamInfo examInfo;

	/**
	 * 答卷状态（对应列 temp_save）：0 暂存（未提交）、1 已完成（提交）。
	 * generateAnswerDetails 仅对 tempSave=1 的答卷生成明细。
	 */
	private Integer tempSave;

	/**
	 * 考试练习模式（对应列 exam_exercise_type，枚举 name 存储）。
	 * ExamExerciseTypeEnum：O 顺序练习 / R 随机练习 / W 错题练习；
	 * historyExercise 按该字段非空过滤练习历史。
	 */
	private ExamExerciseTypeEnum examExerciseType;

	/**
	 * 创建人（对应列 create_by，FieldFill.INSERT 自动填充）。
	 * 白名单导入用户答卷时被 updateProjectPartnerByAnswer 更新为参与表（t_project_partner）ID。
	 */
	@TableField(fill = FieldFill.INSERT)
	private String createBy;

	/**
	 * 更新时间（对应列 update_at，FieldFill.UPDATE 自动填充）。
	 */
	@TableField(fill = FieldFill.UPDATE)
	private Date updateAt;

}
