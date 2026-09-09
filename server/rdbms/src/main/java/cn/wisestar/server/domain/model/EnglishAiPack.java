package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 单元内容包实体（对应数据库表 t_english_ai_pack）。
 *
 * @author wisestar
 * @date 2026/9/9
 */
@Data
@TableName("t_english_ai_pack")
@EqualsAndHashCode(callSuper = false)
public class EnglishAiPack extends BaseModel {

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

}
