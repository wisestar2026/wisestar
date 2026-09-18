package cn.wisestar.server.domain.dto.english;

import cn.wisestar.server.domain.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语语法查询 DTO。
 *
 * @author wisestar
 * @date 2026/9/18
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EnglishGrammarQuery extends PageQuery {

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

	/** 标题（模糊） */
	private String title;

}
