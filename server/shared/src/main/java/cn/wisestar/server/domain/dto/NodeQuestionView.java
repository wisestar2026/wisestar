package cn.wisestar.server.domain.dto;

import lombok.Data;

/**
 * 习题列表页「节点刷题内容预览」题目视图（与学员端刷题同一题目语义）。
 *
 * <p>schema 保留 SurveySchema 原结构：withAnswer=true 时含标准答案与解析；
 * withAnswer=false 时递归剥离答案字段（含子题），防误展示。</p>
 *
 * @author wisestar
 */
@Data
public class NodeQuestionView {

	/**
	 * 题目 ID（t_template.id）。
	 */
	private String id;

	/**
	 * 题目名称/题干摘要。
	 */
	private String name;

	/**
	 * 题型（五种业务题型之一）。
	 */
	private SurveySchema.QuestionType questionType;

	/**
	 * 题目标签。
	 */
	private String[] tag;

	/**
	 * 难易程度（简单/中等/困难）。
	 */
	private String difficulty;

	/**
	 * 题目完整结构（含选项/空位/答案/解析）。
	 */
	private SurveySchema schema;

}
