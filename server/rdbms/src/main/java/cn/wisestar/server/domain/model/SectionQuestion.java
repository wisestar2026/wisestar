package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 小节-测试题目绑定实体（对应数据库表 t_section_question）。
 *
 * <p>小节与题目（t_template）的多对多关联：一个小节可绑定多道题库测试题目，
 * 一道题目也可被多个小节引用。绑定题目只能从题目库选择，不能在本模块新增。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
@TableName("t_section_question")
@EqualsAndHashCode(callSuper = false)
public class SectionQuestion extends BaseModel {

	/**
	 * 小节ID（t_section.id）。
	 */
	private String sectionId;

	/**
	 * 题目ID（t_template.id）。
	 */
	private String questionId;

}
