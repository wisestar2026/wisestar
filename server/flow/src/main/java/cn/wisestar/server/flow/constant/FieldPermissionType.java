package cn.wisestar.server.flow.constant;

/**
 * 问卷字段权限类型常量。
 *
 * <p>职责：定义流程节点中"字段权限"（{@code fieldPermission}）的取值，该权限在
 * 问卷提交、审批人打开表单时被用于过滤字段（隐藏 / 只读 / 可编辑）。</p>
 *
 * <p>所属流程环节：贯穿"发起申请 → 逐级审批"全过程。发起人配置流程节点时以这些
 * 常量记录字段权限（存储于 t_flow_entry_node.field_permission JSON 列），
 * {@code SchemaHelper.updateSchemaByPermission} 与审批处理逻辑读取该值决定
 * 每个问卷题目对当前用户是否可见、可编辑。</p>
 *
 * <p>被谁调用：{@code SchemaHelper}（schema 过滤）、审批/保存 TaskHandler
 * （答案合并权限判断）、流程节点保存逻辑。</p>
 *
 * <p>依赖什么：无（纯常量类）。</p>
 *
 * @author javahuang
 * @date 2022/1/6
 */
public final class FieldPermissionType {

	/** 默认隐藏：该字段对当前节点用户完全不可见，提交时也会被过滤掉 */
	public static final int hidden = 0;

	/** 仅可见：该字段对当前节点用户只读展示，不允许编辑 */
	public static final int visible = 1;

	/** 可编辑：该字段对当前节点用户可修改，保存后合并进表单答案 */
	public static final int editable = 2;

}
