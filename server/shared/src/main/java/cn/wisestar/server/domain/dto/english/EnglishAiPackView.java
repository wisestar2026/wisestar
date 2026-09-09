package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.Date;

/**
 * AI 单元内容包视图 DTO。
 *
 * <p>content 为 AI 生成的整套内容 JSON（含单词/语法讲解/例句/练习），
 * 列表接口为控制传输体积会置空，详情接口返回完整内容。</p>
 *
 * @author wisestar
 * @date 2026/9/9
 */
@Data
public class EnglishAiPackView {

	private String id;

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 单元 */
	private String unit;

	/** 主题 */
	private String topic;

	/** 内容包标题 */
	private String title;

	/** AI 生成的整套内容 JSON */
	private String content;

	/** 单词数量 */
	private Integer wordCount;

	/** 创建时间 */
	private Date createAt;

}
