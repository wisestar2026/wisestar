package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 英语单元视图 DTO。
 *
 * @author wisestar
 * @date 2026/9/15
 */
@Data
public class EnglishUnitView {

	private String id;

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

	/** 排序 */
	private Integer sort;

	/** 该单元单词数 */
	private Integer wordCount;

	/** 该单元句子数 */
	private Integer sentenceCount;

}
