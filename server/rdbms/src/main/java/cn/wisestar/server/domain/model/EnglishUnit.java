package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语单元目录实体（对应数据库表 t_english_unit）。
 *
 * <p>单词与句子共用同一套单元目录，用于学习中心的单元列表展示与排序。</p>
 *
 * @author wisestar
 * @date 2026/9/15
 */
@Data
@TableName("t_english_unit")
@EqualsAndHashCode(callSuper = false)
public class EnglishUnit extends BaseModel {

	/** 教材版本（人教版/苏教版等） */
	private String version;

	/** 年级（一年级~六年级） */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元，如 Unit 1 Helping at home */
	private String unit;

	/** 排序 */
	private Integer sort;

}
