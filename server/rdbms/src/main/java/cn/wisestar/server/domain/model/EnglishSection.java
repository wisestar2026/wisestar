package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语小节目录实体（对应数据库表 t_english_section）。
 *
 * <p>维护某「版本 + 年级 + 学期 + 单元」下的小节名称与排序，词库、句库、语法共用。</p>
 *
 * @author wisestar
 * @date 2026/9/18
 */
@Data
@TableName("t_english_section")
@EqualsAndHashCode(callSuper = false)
public class EnglishSection extends BaseModel {

	/** 教材版本（人教版/苏教版等） */
	private String version;

	/** 年级（一年级~六年级） */
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
