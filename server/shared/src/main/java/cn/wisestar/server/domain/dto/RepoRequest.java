package cn.wisestar.server.domain.dto;

import lombok.Data;

/**
 * 题库请求 DTO（同时复用为"题库题目导出"的查询参数）。
 *
 * 【类职责】
 * 承载题库的创建/更新请求参数，以及题库题目导出（RepoApi.exportRepoQuestions →
 * RepoServiceImpl.exportRepoQuestions）的筛选条件。
 *
 * 【被谁调用】
 * - RepoApi.createRepo / updateRepo：题库 CRUD（@RequestBody JSON）
 * - RepoApi.exportRepoQuestions：导出参数（GET query 参数绑定，除 id 外其余为题目筛选）
 *
 * 【数据流】
 * 前端题库管理/题目管理页 → RepoApi → RepoService/RepoServiceImpl → t_repo / t_template 表
 *
 * 【导出筛选说明】
 * 导出时除 id（题库 id，可空=导出全部）外，name/questionType/subject/chapter/
 * knowledgePoint/difficulty 作为题目维度筛选条件，与题目管理页筛选栏一致（AND 关系）。
 *
 * @author javahuang
 * @date 2022/4/27
 */
@Data
public class RepoRequest {

	/** 题库 id（导出时可为空，空=导出全部题目） */
	private String id;

	/** 标题（导出时作为题目名称模糊搜索） */
	private String name;

	/** 备注 */
	private String description;

	/** 标签 */
	private String[] tag;

	/** 排序优先级 */
	private Integer priority;

	/** 设置 */
	private String setting;

	/** 题库类型 */
	private String mode;

	private Boolean shared;

	/** 题库分类 */
	private String category;

	/** 是否是练习题库 */
	private Boolean isPractice;

	/** 题型筛选（导出用）：Radio 单选 / Checkbox 多选 / Judge 判断 / FillBlank 填空 / Textarea 简答 */
	private String questionType;

	/** 学科筛选（导出用）：知识点体系一级维度 */
	private String subject;

	/** 章节筛选（导出用）：知识点体系二级维度 */
	private String chapter;

	/** 知识点筛选（导出用）：知识点体系三级维度 */
	private String knowledgePoint;

	/** 难度筛选（导出用）：easy 简单 / medium 中等 / hard 困难 */
	private String difficulty;

}
