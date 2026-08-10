package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学科字典实体（对应数据库表 t_subject，知识管理板块一级维度）。
 *
 * <p><b>层级关系</b>：学科 → 章节（t_chapter）→ 小节（t_section）→ 知识点（t_knowledge_point）。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
@TableName("t_subject")
@EqualsAndHashCode(callSuper = false)
public class Subject extends BaseModel {

	/**
	 * 学科名称（如 语文/数学/英语）。
	 */
	private String name;

	/**
	 * 学科编码（如 CHINESE/MATH/ENGLISH）。
	 */
	private String code;

	/**
	 * 图标（emoji）。
	 */
	private String icon;

	/**
	 * 主题色（学生端学科配色）。
	 */
	private String themeColor;

	/**
	 * 排序（数字越小越靠前）。
	 */
	private Integer sort;

}
