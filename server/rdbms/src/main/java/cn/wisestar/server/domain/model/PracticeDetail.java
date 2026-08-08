package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 练习逐题明细（t_practice_detail）。
 *
 * <p>一次练习（{@link PracticeRecord}）下每道题的作答结果：学生答案、判分结果、得分。
 * 错题本的数据来源：is_correct = 0 的记录即为错题，按 question_id 关联回题目。</p>
 *
 * <p><b>判分结果约定</b>：is_correct = 1 正确 / 0 错误（含未作答）/ null 无标准答案不计分。</p>
 *
 * @see PracticeRecord
 */
@Data
@TableName(value = "t_practice_detail", autoResultMap = true)
@EqualsAndHashCode(callSuper = false)
public class PracticeDetail extends BaseModel {

	/**
	 * 所属练习会话 ID（t_practice_record.id）
	 */
	@TableField("practice_id")
	private String practiceId;

	/**
	 * 题目 ID（t_template.id）
	 */
	@TableField("question_id")
	private String questionId;

	/**
	 * 题型（Radio/Checkbox/Judge/FillBlank/Text 等）
	 */
	@TableField("question_type")
	private String questionType;

	/**
	 * 学生答案（单选/判断=选项标题；多选=选项标题逗号拼接；填空/文本=输入内容）
	 */
	@TableField("user_answer")
	private String userAnswer;

	/**
	 * 判分结果：1 正确 / 0 错误 / null 无标准答案
	 */
	@TableField("is_correct")
	private Integer isCorrect;

	/**
	 * 本题得分（题目 attribute.examScore，无则 1 分；答错 0 分）
	 */
	@TableField("score")
	private Double score;

}
