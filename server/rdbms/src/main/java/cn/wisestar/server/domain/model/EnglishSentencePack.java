package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语单元句子同步练习内容包实体（对应数据库表 t_english_sentence_pack）。
 *
 * <p>一条记录对应 版本+年级+册别+单元，{@link #content} 保存多题型题目 JSON 数组，
 * 管理端预生成固化、学员端只读。</p>
 *
 * @author wisestar
 * @date 2026/10/15
 */
@Data
@TableName("t_english_sentence_pack")
@EqualsAndHashCode(callSuper = false)
public class EnglishSentencePack extends BaseModel {

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

	/** 题目 JSON 数组 */
	private String content;

	/** 题目总数 */
	private Integer count;

}
