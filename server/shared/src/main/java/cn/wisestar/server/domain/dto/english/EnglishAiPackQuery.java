package cn.wisestar.server.domain.dto.english;

import cn.wisestar.server.domain.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 单元内容包查询 DTO。
 *
 * @author wisestar
 * @date 2026/9/9
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EnglishAiPackQuery extends PageQuery {

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 单元 */
	private String unit;

}
