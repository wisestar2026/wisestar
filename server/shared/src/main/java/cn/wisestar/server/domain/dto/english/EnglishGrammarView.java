package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.Date;

/**
 * 英语语法视图 DTO。
 *
 * @author wisestar
 * @date 2026/9/18
 */
@Data
public class EnglishGrammarView {

	private String id;

	/** 标题 */
	private String title;

	/** 讲解内容 */
	private String content;

	/** 例句（JSON：examples 数组） */
	private String examples;

	/** 练习题（JSON：exercises 数组） */
	private String exercises;

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

	/** 更新时间 */
	private Date updateAt;

}
