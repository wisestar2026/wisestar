package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语语法实体（对应数据库表 t_english_grammar）。
 *
 * @author wisestar
 * @date 2026/9/9
 */
@Data
@TableName("t_english_grammar")
@EqualsAndHashCode(callSuper = false)
public class EnglishGrammar extends BaseModel {

	/** 标题 */
	private String title;

	/** 讲解内容 */
	private String content;

	/** 例句（JSON：examples 数组） */
	private String examples;

	/** 练习题（JSON：exercises 数组） */
	private String exercises;

	/** 教材版本（人教版/苏教版等） */
	private String version;

	/** 年级（一年级~九年级） */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

	/** 小节 */
	private String section;

	/** 排序 */
	private Integer sort;

}
