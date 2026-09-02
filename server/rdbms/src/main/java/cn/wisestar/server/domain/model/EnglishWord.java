package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语单词实体（对应数据库表 t_english_word）。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@Data
@TableName("t_english_word")
@EqualsAndHashCode(callSuper = false)
public class EnglishWord extends BaseModel {

	/** 单词拼写 */
	private String spell;

	/** 音标 */
	private String phonetic;

	/** 释义 */
	private String meaning;

	/** 图片 URL */
	private String imageUrl;

	/** 音频 URL */
	private String audioUrl;

	/** 例句 */
	private String exampleSentence;

	/** 教材版本（人教版/苏教版等） */
	private String version;

	/** 年级（一年级~六年级） */
	private String grade;

	/** 单元 */
	private String unit;

}
