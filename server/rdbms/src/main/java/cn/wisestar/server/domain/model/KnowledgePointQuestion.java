package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识点-题目绑定实体（对应数据库表 t_knowledge_point_question）。
 *
 * <p>知识点与题目（t_template）的多对多关联：一个知识点可绑定多道题库题目，
 * 一道题目也可被多个知识点引用。绑定题目只能从题目库选择，不能在本模块新增。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
@TableName("t_knowledge_point_question")
@EqualsAndHashCode(callSuper = false)
public class KnowledgePointQuestion extends BaseModel {

	/**
	 * 知识点ID（t_knowledge_point.id）。
	 */
	private String knowledgePointId;

	/**
	 * 题目ID（t_template.id）。
	 */
	private String questionId;

}
