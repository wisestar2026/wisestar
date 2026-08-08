package cn.wisestar.server.core.constant;

/**
 * 标签分类枚举（TagCategoryEnum）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义标签（Tag）的业务归属分类，同一标签体系下通过
 * 分类区分标签的使用场景：模板广场标签、题目模板库标签、问卷标签、考试标签、
 * 学员标签（用于按标签自动分配题库）。</p>
 *
 * @author javahuang
 * @date 2022/4/27
 */
public enum TagCategoryEnum {

	/** 模板标签 */
	template,
	/** 模板库标签 */
	repo,
	/** 问卷 */
	survey,
	/** 考试 */
	exam,
	/** 学员标签（t_tag.entity_id=用户ID，用于按标签自动分配题库） */
	user;

}
