package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 练习会话记录（t_practice_record）。
 *
 * <p>学员每次交卷生成一条记录，保存本次练习的模式、来源题库、题目数、得分与用时，
 * 作为错题本、学习统计等后续功能的数据底座。</p>
 *
 * <p><b>判分结果约定</b>：每题正确与否记录在 {@link PracticeDetail}（t_practice_detail），
 * 本表只存汇总字段；错题查询 = t_practice_detail 中 is_correct=0 的记录。</p>
 *
 * @see PracticeDetail
 */
@Data
@TableName(value = "t_practice_record", autoResultMap = true)
@EqualsAndHashCode(callSuper = false)
public class PracticeRecord extends BaseModel {

	/**
	 * 练习学员用户 ID
	 */
	@TableField("user_id")
	private String userId;

	/**
	 * 练习模式：special 专项刷题 / exam 套卷模拟 / random 随机练习
	 */
	@TableField("mode")
	private String mode;

	/**
	 * 来源题库 ID（从题库整库练习时记录，可空）
	 */
	@TableField("repo_id")
	private String repoId;

	/**
	 * 题目总数
	 */
	@TableField("total_questions")
	private Integer totalQuestions;

	/**
	 * 答对题数（is_correct=1 的题数）
	 */
	@TableField("correct_count")
	private Integer correctCount;

	/**
	 * 得分（每题分值之和，无标准答案题不计分）
	 */
	@TableField("score")
	private Double score;

	/**
	 * 总分（全部题分值之和）
	 */
	@TableField("total_score")
	private Double totalScore;

	/**
	 * 练习用时（毫秒）
	 */
	@TableField("duration_ms")
	private Long durationMs;

	/** 知识点ID（知识点练习提交时记录，供知识点型任务完成判定） */
	private String knowledgePointId;

}
