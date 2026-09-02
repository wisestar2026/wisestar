package cn.wisestar.server.domain.dto.english;

import cn.wisestar.server.domain.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语单词查询 DTO。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EnglishWordQuery extends PageQuery {

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 单元 */
	private String unit;

	/** 单词拼写（模糊） */
	private String spell;

}
