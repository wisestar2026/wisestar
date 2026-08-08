package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.Date;

/**
 * 错题聚合视图（错题库管理列表项）。
 *
 * <p><b>数据来源</b>：t_practice_detail（is_correct = 0）按
 * 「题目 + 学员」聚合：同一学员反复做错同一题，合并为一条并累计错误次数。</p>
 *
 * <p><b>用途</b>：管理端「错题库管理」页面列表数据；学员端错题本（阶段二）
 * 可复用同一聚合语义，仅增加按当前用户过滤。</p>
 */
@Data
public class WrongQuestionView {

	/**
	 * 题目 ID（t_template.id）
	 */
	private String questionId;

	/**
	 * 题型（Radio/Checkbox/Judge/FillBlank/Textarea 等）
	 */
	private String questionType;

	/**
	 * 题目标题（t_template.name）
	 */
	private String questionTitle;

	/**
	 * 所属题库 ID（优先练习会话 repo_id，其次题目当前归属 repo_id）
	 */
	private String repoId;

	/**
	 * 所属题库名称（t_repo.name，题库已删或未归属时为空）
	 */
	private String repoName;

	/**
	 * 学员 ID（t_user.id）
	 */
	private String userId;

	/**
	 * 学员姓名（t_user.name）
	 */
	private String userName;

	/**
	 * 累计错误次数（同题同人做错次数）
	 */
	private Long wrongCount;

	/**
	 * 最近一次做错时间
	 */
	private Date lastWrongTime;

	/**
	 * 最近一次做错时的学生答案（选项标题/文本，展示用）
	 */
	private String lastAnswer;

	/**
	 * 最近一次做错时的本题得分（答错恒为 0）
	 */
	private Double lastScore;
}
