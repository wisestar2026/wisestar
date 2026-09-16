package cn.wisestar.server.domain.dto.english;

import cn.wisestar.server.domain.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语句子查询 DTO。
 *
 * @author wisestar
 * @date 2026/9/15
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EnglishSentenceQuery extends PageQuery {

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

	/** 关键字（英文或中文模糊） */
	private String keyword;

}
