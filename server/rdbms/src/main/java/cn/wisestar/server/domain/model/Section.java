package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 小节实体（对应数据库表 t_section，知识管理板块三级维度）。
 *
 * <p>挂载于章节（chapterId）下，其下包含若干知识点（t_knowledge_point）。
 * content/practice 为 JSON 字符串，由前端序列化提交、解析展示，后端仅透传存储。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
@TableName("t_section")
@EqualsAndHashCode(callSuper = false)
public class Section extends BaseModel {

	/**
	 * 所属章节ID（t_chapter.id）。
	 */
	private String chapterId;

	/**
	 * 小节名称（如 加法小站）。
	 */
	private String name;

	/**
	 * 排序（数字越小越靠前）。
	 */
	private Integer sort;

	/**
	 * 年级（如 一年级，选填）。
	 */
	private String grade;

	/**
	 * 学期（上/下，选填）。
	 */
	private String term;

	/**
	 * 重点程度（选填）：key=重点 / minor=次重点 / normal=一般；为空表示未标注。
	 */
	private String importance;

	/**
	 * 小节类型（选填）：normal=普通小节 / exam=章节测评（测试节点）。
	 * exam 节点以小节形式挂在章节下承载整章测评，出题范围覆盖整章知识点；为空按 normal 处理。
	 */
	private String type;

	/**
	 * 内容设置 JSON：{"objective":"学习目标","overview":"内容概述","points":["要点1"]}。
	 */
	private String content;

	/**
	 * 练习设置 JSON：{"questionCount":10,"difficulty":"基础","types":["Radio"]}。
	 */
	private String practice;

}
