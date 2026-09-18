package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 英语小节视图 DTO。
 *
 * @author wisestar
 * @date 2026/9/18
 */
@Data
public class EnglishSectionView {

	private String id;

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

	/** 小节 */
	private String section;

	/** 排序 */
	private Integer sort;

}
