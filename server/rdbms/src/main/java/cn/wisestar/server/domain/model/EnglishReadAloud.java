package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语课文跟读篇章实体（对应数据库表 t_english_read_aloud）。
 *
 * <p>一条记录对应一篇课本课文/板块，逐句中英文以 JSON 数组保存在 {@link #content}，
 * 与句子练习库解耦，避免跟读课文进入句子默写/连词成句题池。</p>
 *
 * @author wisestar
 * @date 2026/10/19
 */
@Data
@TableName("t_english_read_aloud")
@EqualsAndHashCode(callSuper = false)
public class EnglishReadAloud extends BaseModel {

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

	/** 课文标题/板块名 */
	private String title;

	/** 教材版本与来源说明 */
	private String source;

	/** 单元内排序 */
	private Integer sort;

	/** 逐句内容 JSON：[{en, zh}] */
	private String content;

	/** 句子数量 */
	private Integer lineCount;

}
