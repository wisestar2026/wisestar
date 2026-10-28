package cn.wisestar.server.domain.dto.math;

import lombok.Data;

import java.util.List;

/**
 * 数学组卷请求（后台管理端）。
 *
 * <p><b>两种组卷口径</b>：</p>
 * <ul>
 *   <li>{@code mode = chapter}：通用组卷，按章节/小节/知识点范围从数学题库抽题；</li>
 *   <li>{@code mode = student}：按学员组卷，优先取该学员数学错题，不足时用同范围练习题补齐。</li>
 * </ul>
 *
 * <p>章节/小节/知识点与难度、题型均为可选筛选；全部为空时从整个数学题库抽题。</p>
 *
 * @author wisestar
 * @date 2026/10/26
 */
@Data
public class MathPaperRequest {

	/** 组卷方式：chapter=通用组卷 / student=按学员错题组卷 */
	private String mode;

	/** 学员 ID（mode=student 时使用，优先于 studentNo） */
	private String studentId;

	/** 学号（mode=student 且未传 studentId 时使用） */
	private String studentNo;

	/** 章节 ID 列表（可选） */
	private List<String> chapterIds;

	/** 小节 ID 列表（可选，优先于章节） */
	private List<String> sectionIds;

	/** 知识点名称列表（可选，命中任一即纳入） */
	private List<String> knowledgePoints;

	/** 难度：easy/medium/hard（可选） */
	private String difficulty;

	/** 题型（可选，如 Radio/Checkbox/FillBlank/MultipleBlank/Judge） */
	private List<String> questionTypes;

	/** 题量，默认 20，上限 60 */
	private Integer questionCount;

	/** 试卷标题（可选，留空由服务端按范围自动生成） */
	private String title;

}
