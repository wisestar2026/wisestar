package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识点实体（对应数据库表 t_knowledge_point，知识管理板块最小学习单元）。
 *
 * <p>挂载于小节（sectionId）下；题目通过 t_knowledge_point_question 关联表
 * 从题目库（t_template）选择绑定（不能在本模块新增题目）。
 * imageUrl 为知识点配图地址（复用 /api/file/create 上传返回的 previewUrl）。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
@TableName("t_knowledge_point")
@EqualsAndHashCode(callSuper = false)
public class KnowledgePoint extends BaseModel {

	/**
	 * 所属小节ID（t_section.id）。
	 */
	private String sectionId;

	/**
	 * 知识点名称（如 进位加法）。
	 */
	private String name;

	/**
	 * 排序（数字越小越靠前）。
	 */
	private Integer sort;

	/**
	 * 内容设置 JSON：{"points":["讲解要点1"]}。
	 */
	private String content;

	/**
	 * 知识点图片地址（可为空）。
	 */
	private String imageUrl;

}
