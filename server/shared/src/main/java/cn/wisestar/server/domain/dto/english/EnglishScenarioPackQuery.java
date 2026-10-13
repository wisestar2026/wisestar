package cn.wisestar.server.domain.dto.english;

import cn.wisestar.server.domain.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语单元情景练习内容包查询 DTO。
 *
 * @author wisestar
 * @date 2026/10/13
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EnglishScenarioPackQuery extends PageQuery {

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

}
