package cn.wisestar.server.core.constant;

/**
 * 字段权限类型常量（FieldPermissionType）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义"问卷/考试中的字段（问题）"对特定用户角色的可见/可编辑
 * 权限级别。用于项目参与者（答卷人/协作者）的字段级权限控制：
 * 不同角色对同一问卷字段拥有不同的展示与编辑能力。</p>
 *
 * <p><b>取值说明</b>：hidden=0（默认隐藏）、visible=1（仅可见，不可编辑）、
 * editable=2（可编辑）。</p>
 *
 * @author javahuang
 * @date 2022/1/6
 */
public final class FieldPermissionType {

	/** 默认隐藏 */
	public static final Integer hidden = 0;

	/** 仅可见 */
	public static final Integer visible = 1;

	/** 可编辑 */
	public static final Integer editable = 2;

}
