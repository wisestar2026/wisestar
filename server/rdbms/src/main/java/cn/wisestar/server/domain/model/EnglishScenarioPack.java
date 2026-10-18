package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语单元情景练习内容包实体（对应数据库表 t_english_scenario_pack）。
 *
 * <p>一条记录对应 版本+年级+册别+单元，分别保存单词情景（{@link #wordContent}）与
 * 句式情景（{@link #patternContent}）两套分组 JSON，供学员端只读合并展示。</p>
 *
 * @author wisestar
 * @date 2026/10/13
 */
@Data
@TableName("t_english_scenario_pack")
@EqualsAndHashCode(callSuper = false)
public class EnglishScenarioPack extends BaseModel {

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

	/** 单元阅读理解 JSON（短文 + 选择题，首次访问时生成并缓存） */
	private String readingContent;

}
