package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.Date;

/**
 * 英语单元情景练习内容包视图 DTO。
 *
 * <p>一条记录对应 版本+年级+册别+单元，分别保存单词情景（{@link #wordContent}）与
 * 句式情景（{@link #patternContent}）两套分组 JSON；学员端读取时按组顺序合并展示。</p>
 *
 * @author wisestar
 * @date 2026/10/13
 */
@Data
public class EnglishScenarioPackView {

	private String id;

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

	/** 单词情景分组 JSON */
	private String wordContent;

	/** 单词情景场景总数 */
	private Integer wordCount;

	/** 句式情景分组 JSON */
	private String patternContent;

	/** 句式情景情景总数 */
	private Integer patternCount;

	/** 创建时间 */
	private Date createAt;

}
