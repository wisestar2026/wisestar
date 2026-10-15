package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.Date;

/**
 * 英语·单元句子同步练习内容包视图 DTO。
 *
 * <p>一条记录对应 版本+年级+册别+单元，{@link #content} 保存题目 JSON 数组；
 * 管理端生成预览后确认固化，学员端只读。</p>
 *
 * @author wisestar
 * @date 2026/10/15
 */
@Data
public class EnglishSentencePackView {

	private String id;

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

	/** 题目 JSON 数组（EnglishSentenceExercise 列表序列化） */
	private String content;

	/** 题目总数 */
	private Integer count;

	/** 创建时间 */
	private Date createAt;

}
