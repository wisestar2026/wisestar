package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 英语单元练习组卷请求（内容包阶梯卷）。
 *
 * <p>按标签表中的知识点分组，组内按 D1→D3 难度递进取题；
 * 某一难度缺失时用最近难度补位，题量不足则继续从下一知识点取题。</p>
 *
 * @author wisestar
 * @date 2026/10/17
 */
@Data
public class EnglishUnitPaperRequest {

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别 */
	private String term;

	/** 单元 */
	private String unit;

	/** 章节 ID（用于收敛同名单元的薄弱点） */
	private String chapterId;

	/** 题量，默认 20，最大 30 */
	private Integer questionCount;

}
