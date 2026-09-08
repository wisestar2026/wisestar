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
	private String userId;

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

	/** 学科ID（研习章节归属学科，t_chapter.subject_id；学员端错题本按研习学科隔离） */
	private String subjectId;

	/** 章节ID（t_chapter.id） */
	private String chapterId;

	/** 小节ID（t_section.id） */
	private String sectionId;

	/** 知识点ID（t_knowledge_point.id） */
	private String knowledgePointId;

	/** 年级（如 一年级；取小节/章节配置年级） */
	private String grade;

	/** 错误归因精确筛选（大意/计算错误/知识点不熟/题型不会等；空=全部含未标注） */
	private String wrongReason;
}
