package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语句子实体（对应数据库表 t_english_sentence）。
 *
 * <p>独立的句库，与单词解耦，可单独维护与导入。</p>
 *
 * @author wisestar
 * @date 2026/9/15
 */
@Data
@TableName("t_english_sentence")
@EqualsAndHashCode(callSuper = false)
public class EnglishSentence extends BaseModel {

	/** 英文句子 */
	private String en;

	/** 中文释义 */
	private String zh;

	/** 音频 URL（为空时前端回退浏览器语音合成） */
	private String audioUrl;

	/** 教材版本（人教版/苏教版等） */
	private String version;

	/** 年级（一年级~六年级） */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

	/** 单元内排序 */
	private Integer sort;

}
