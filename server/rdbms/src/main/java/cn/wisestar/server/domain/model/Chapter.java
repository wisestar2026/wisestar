package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 章节实体（对应数据库表 t_chapter，知识管理板块二级维度）。
 *
 * <p>挂载于学科（subjectId）下，其下包含若干小节（t_section）。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
@TableName("t_chapter")
@EqualsAndHashCode(callSuper = false)
public class Chapter extends BaseModel {

	/**
	 * 所属学科ID（t_subject.id）。
	 */
	private String subjectId;

	/**
	 * 章节名称（如 100以内加减法）。
	 */
	private String name;

	/**
	 * 图标（emoji）。
	 */
	private String icon;

	/**
	 * 排序（数字越小越靠前）。
	 */
	private Integer sort;

}
