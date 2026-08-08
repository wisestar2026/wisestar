package cn.wisestar.server.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 错题列表查询条件。
 *
 * <p><b>用途</b>：管理端「错题库管理」页面的筛选条件 + 分页参数
 * （GET /api/practice/wrong-list）。</p>
 *
 * <p><b>筛选维度</b>：题库、题型、关键词（题目标题/学员姓名模糊）、
 * 做错时间范围；keyword 同时匹配题目名与学员姓名，便于按人查错题。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WrongQuestionQuery extends PageQuery {

	/**
	 * 题库 ID（练习会话 repo_id 或题目归属 repo_id 命中即匹配）
	 */
	private String repoId;

	/**
	 * 题型（Radio/Checkbox/Judge/FillBlank/Textarea 等）
	 */
	private String questionType;

	/**
	 * 关键词：题目标题 或 学员姓名 模糊匹配
	 */
	private String keyword;

	/**
	 * 做错时间范围起（含）
	 */
	private Date startTime;

	/**
	 * 做错时间范围止（含）
	 */
	private Date endTime;
}
