package cn.wisestar.server.core.constant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台系统权限点清单（权限树数据源）。
 *
 * <p>角色权限管理（人事管理）模块使用：角色编辑页的权限树按功能模块分组展示
 * 权限点，勾选结果以权限编码列表保存到 {@code t_role.authority}（逗号分隔），
 * 后端接口通过 {@code @PreAuthorize("hasAuthority('module:action')")} 拦截。</p>
 *
 * <p>本类集中定义后台全部功能模块的权限点，是权限树、内置角色默认权限、
 * 管理员全量权限的唯一权威清单。</p>
 *
 * @author wisestar
 * @date 2026/08/13
 */
public final class PermissionConsts {

	private PermissionConsts() {
	}

	// ---------------------------------------------------------------
	// 权限点常量
	// ---------------------------------------------------------------

	/** 仪表盘 */
	public static final String HOME = "home";

	/** 在线练习 */
	public static final String EXERCISE_LIST = "exercise:list";

	/** 问卷管理 */
	public static final String PROJECT_LIST = "project:list";
	public static final String PROJECT_DETAIL = "project:detail";
	public static final String PROJECT_CREATE = "project:create";
	public static final String PROJECT_UPDATE = "project:update";
	public static final String PROJECT_DELETE = "project:delete";
	public static final String PROJECT_REPORT = "project:report";

	/** 答案管理 */
	public static final String ANSWER_LIST = "answer:list";
	public static final String ANSWER_DETAIL = "answer:detail";
	public static final String ANSWER_CREATE = "answer:create";
	public static final String ANSWER_UPDATE = "answer:update";
	public static final String ANSWER_DELETE = "answer:delete";
	public static final String ANSWER_EXPORT = "answer:export";
	public static final String ANSWER_UPLOAD = "answer:upload";

	/** 题库管理 */
	public static final String REPO_LIST = "repo:list";
	public static final String REPO_DETAIL = "repo:detail";
	public static final String REPO_CREATE = "repo:create";
	public static final String REPO_UPDATE = "repo:update";
	public static final String REPO_DELETE = "repo:delete";
	public static final String REPO_EXPORT = "repo:export";
	public static final String REPO_BOOK = "repo:book";

	/** 题目管理（模板库，接口权限点沿用 template:*） */
	public static final String QUESTION_LIST = "template:list";
	public static final String QUESTION_CREATE = "template:create";
	public static final String QUESTION_UPDATE = "template:update";
	public static final String QUESTION_DELETE = "template:delete";

	/** 知识管理（章节/小节/知识点/学科） */
	public static final String KNOWLEDGE_LIST = "knowledge:list";
	public static final String KNOWLEDGE_CREATE = "knowledge:create";
	public static final String KNOWLEDGE_UPDATE = "knowledge:update";
	public static final String KNOWLEDGE_DELETE = "knowledge:delete";

	/** 学员管理 */
	public static final String STUDENT_LIST = "student:list";
	public static final String STUDENT_CREATE = "student:create";
	public static final String STUDENT_UPDATE = "student:update";
	public static final String STUDENT_DELETE = "student:delete";

	/** 订单管理 */
	public static final String ORDER_LIST = "order:list";
	public static final String ORDER_CREATE = "order:create";
	public static final String ORDER_UPDATE = "order:update";
	public static final String ORDER_DELETE = "order:delete";

	/** 系统管理-用户 */
	public static final String SYSTEM_USER_LIST = "system:user:list";
	public static final String SYSTEM_USER_CREATE = "system:user:create";
	public static final String SYSTEM_USER_UPDATE = "system:user:update";
	public static final String SYSTEM_USER_DELETE = "system:user:delete";
	public static final String SYSTEM_USER_UPDATE_POSITION = "system:user:updatePosition";

	/** 系统管理-角色 */
	public static final String SYSTEM_ROLE_LIST = "system:role:list";
	public static final String SYSTEM_ROLE_CREATE = "system:role:create";
	public static final String SYSTEM_ROLE_UPDATE = "system:role:update";
	public static final String SYSTEM_ROLE_DELETE = "system:role:delete";

	/** 系统管理-部门 */
	public static final String SYSTEM_DEPT_LIST = "system:dept:list";
	public static final String SYSTEM_DEPT_CREATE = "system:dept:create";
	public static final String SYSTEM_DEPT_UPDATE = "system:dept:update";
	public static final String SYSTEM_DEPT_DELETE = "system:dept:delete";

	/** 系统管理-岗位 */
	public static final String SYSTEM_POSITION_LIST = "system:position:list";
	public static final String SYSTEM_POSITION_CREATE = "system:position:create";
	public static final String SYSTEM_POSITION_UPDATE = "system:position:update";
	public static final String SYSTEM_POSITION_DELETE = "system:position:delete";

	/** 系统管理-字典 */
	public static final String SYSTEM_DICT_LIST = "system:dict:list";
	public static final String SYSTEM_DICT_CREATE = "system:dict:create";
	public static final String SYSTEM_DICT_UPDATE = "system:dict:update";
	public static final String SYSTEM_DICT_DELETE = "system:dict:delete";

	/** 系统管理-字典条目 */
	public static final String SYSTEM_DICT_ITEM_LIST = "system:dictItem:list";
	public static final String SYSTEM_DICT_ITEM_CREATE = "system:dictItem:create";
	public static final String SYSTEM_DICT_ITEM_UPDATE = "system:dictItem:update";
	public static final String SYSTEM_DICT_ITEM_DELETE = "system:dictItem:delete";
	public static final String SYSTEM_DICT_ITEM_IMPORT = "system:dictItem:import";

	// ---------------------------------------------------------------
	// 权限树结构
	// ---------------------------------------------------------------

	/** 权限树节点 */
	public static class Node {
		/** 节点标识（叶节点为权限编码） */
		private String key;
		/** 节点名称 */
		private String name;
		/** 子节点 */
		private List<Node> children;

		public Node() {
		}

		public Node(String key, String name) {
			this.key = key;
			this.name = name;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Node> getChildren() {
			return children;
		}

		public void setChildren(List<Node> children) {
			this.children = children;
		}
	}

	private static Node node(String key, String name, List<Node> children) {
		Node node = new Node(key, name);
		node.setChildren(children);
		return node;
	}

	private static List<Node> ops(Object... keyAndName) {
		List<Node> list = new ArrayList<>();
		for (int i = 0; i < keyAndName.length; i += 2) {
			list.add(new Node((String) keyAndName[i], (String) keyAndName[i + 1]));
		}
		return list;
	}

	private static final List<Node> TREE = buildTree();

	/**
	 * 返回权限树（模块 → 操作点）。
	 */
	public static List<Node> tree() {
		return TREE;
	}

	private static List<Node> buildTree() {
		List<Node> tree = new ArrayList<>();
		tree.add(node("dashboard", "仪表盘", ops(HOME, "查看")));
		tree.add(node("exercise", "在线练习", ops(EXERCISE_LIST, "查看")));
		tree.add(node("project", "问卷管理",
				ops(PROJECT_LIST, "查看", PROJECT_DETAIL, "详情", PROJECT_CREATE, "新增", PROJECT_UPDATE, "修改",
						PROJECT_DELETE, "删除", PROJECT_REPORT, "报表")));
		tree.add(node("answer", "答案管理",
				ops(ANSWER_LIST, "查看", ANSWER_DETAIL, "详情", ANSWER_CREATE, "新增", ANSWER_UPDATE, "修改",
						ANSWER_DELETE, "删除", ANSWER_EXPORT, "导出", ANSWER_UPLOAD, "上传")));
		tree.add(node("repo", "题库管理",
				ops(REPO_LIST, "查看", REPO_DETAIL, "详情", REPO_CREATE, "新增", REPO_UPDATE, "修改", REPO_DELETE, "删除",
						REPO_EXPORT, "导出", REPO_BOOK, "教材绑定")));
		tree.add(node("question", "题目管理",
				ops(QUESTION_LIST, "查看", QUESTION_CREATE, "新增", QUESTION_UPDATE, "修改", QUESTION_DELETE, "删除")));
		tree.add(node("knowledge", "知识管理",
				ops(KNOWLEDGE_LIST, "查看", KNOWLEDGE_CREATE, "新增", KNOWLEDGE_UPDATE, "修改", KNOWLEDGE_DELETE, "删除")));
		tree.add(node("student", "学员管理",
				ops(STUDENT_LIST, "查看", STUDENT_CREATE, "新增", STUDENT_UPDATE, "修改", STUDENT_DELETE, "删除")));
		tree.add(node("order", "订单管理",
				ops(ORDER_LIST, "查看", ORDER_CREATE, "新增", ORDER_UPDATE, "修改", ORDER_DELETE, "删除")));

		// 系统管理：二级子组（用户/角色/部门/岗位/字典/字典条目）
		Node system = node("system", "系统管理", new ArrayList<>());
		system.getChildren().add(node("system-user", "用户管理",
				ops(SYSTEM_USER_LIST, "查看", SYSTEM_USER_CREATE, "新增", SYSTEM_USER_UPDATE, "修改", SYSTEM_USER_DELETE,
						"删除", SYSTEM_USER_UPDATE_POSITION, "调整岗位")));
		system.getChildren().add(node("system-role", "角色管理",
				ops(SYSTEM_ROLE_LIST, "查看", SYSTEM_ROLE_CREATE, "新增", SYSTEM_ROLE_UPDATE, "修改", SYSTEM_ROLE_DELETE,
						"删除")));
		system.getChildren().add(node("system-dept", "部门管理",
				ops(SYSTEM_DEPT_LIST, "查看", SYSTEM_DEPT_CREATE, "新增", SYSTEM_DEPT_UPDATE, "修改", SYSTEM_DEPT_DELETE,
						"删除")));
		system.getChildren().add(node("system-position", "岗位管理",
				ops(SYSTEM_POSITION_LIST, "查看", SYSTEM_POSITION_CREATE, "新增", SYSTEM_POSITION_UPDATE, "修改",
						SYSTEM_POSITION_DELETE, "删除")));
		system.getChildren().add(node("system-dict", "字典管理",
				ops(SYSTEM_DICT_LIST, "查看", SYSTEM_DICT_CREATE, "新增", SYSTEM_DICT_UPDATE, "修改", SYSTEM_DICT_DELETE,
						"删除")));
		system.getChildren().add(node("system-dictItem", "字典条目管理",
				ops(SYSTEM_DICT_ITEM_LIST, "查看", SYSTEM_DICT_ITEM_CREATE, "新增", SYSTEM_DICT_ITEM_UPDATE, "修改",
						SYSTEM_DICT_ITEM_DELETE, "删除", SYSTEM_DICT_ITEM_IMPORT, "导入")));
		tree.add(system);
		return tree;
	}

	// ---------------------------------------------------------------
	// 内置角色默认权限
	// ---------------------------------------------------------------

	/** 内置角色编码 */
	public static final String ROLE_ADMIN = "admin";
	public static final String ROLE_PRINCIPAL = "principal";
	public static final String ROLE_TEACHER = "teacher";
	public static final String ROLE_CONSULTANT = "consultant";
	public static final String ROLE_ACADEMIC = "academic";

	/**
	 * 管理员全量权限（历史保留 + 新增模块权限点）。
	 */
	public static final String ADMIN_AUTHORITY = "answer,answer:list,answer:detail,answer:create,answer:update,answer:delete,"
			+ "answer:export,file,file:detail,file:list,file:import,file:delete,project,project:list,project:detail,"
			+ "project:create,project:update,project:delete,project:report,system,system:role,system:role:list,system:user,"
			+ "system:user:list,system:role:create,system:role:update,system:role:delete,system:user:create,system:user:update,"
			+ "system:user:updatePosition,system:user:delete,position,position:list,position:create,system:position,"
			+ "system:position:update,system:position:delete,system:org,system:org:list,system:org:create,system:org:update,"
			+ "system:org:delete,template,template:list,template:create,template:update,template:delete,system:position:list,"
			+ "system:position:create,system:dept,system:dept:list,system:dept:create,system:dept:update,system:dept:delete,"
			+ "repo,repo:list,repo:detail,repo:create,repo:update,repo:delete,user,user:update,answer:upload,system:dict,"
			+ "system:dict:update,system:dict:delete,system:dictItem,system:dictItem:list,system:dictItem:create,"
			+ "system:dictItem:import,system:dictItem:delete,system:dict:list,system:dict:create,exercise,exercise:list,"
			+ "repo:book,system:dictItem:update,home,"
			+ "knowledge:list,knowledge:create,knowledge:update,knowledge:delete,"
			+ "student:list,student:create,student:update,student:delete,"
			+ "order:list,order:create,order:update,order:delete";

	/** 校长：决策层，查看全局 + 学员/订单运营管理 */
	public static final String PRINCIPAL_AUTHORITY = "home,exercise:list,"
			+ "project:list,project:detail,answer:list,answer:detail,"
			+ "repo:list,repo:detail,template:list,knowledge:list,"
			+ "student:list,student:create,student:update,student:delete,"
			+ "order:list,order:create,order:update,order:delete,"
			+ "system:user:list,system:role:list,system:dept:list,system:position:list,system:dict:list,system:dictItem:list";

	/** 教师：教学执行，知识/题库维护 + 查看学员与订单 */
	public static final String TEACHER_AUTHORITY = "home,exercise:list,"
			+ "repo:list,repo:detail,repo:create,repo:update,repo:delete,"
			+ "template:list,template:create,template:update,template:delete,"
			+ "knowledge:list,knowledge:create,knowledge:update,knowledge:delete,"
			+ "student:list,order:list,"
			+ "project:list,project:detail,answer:list,answer:detail";

	/** 学管师：学员运营，学员/订单全操作 + 知识查看 */
	public static final String CONSULTANT_AUTHORITY = "home,exercise:list,"
			+ "student:list,student:create,student:update,student:delete,"
			+ "order:list,order:create,order:update,order:delete,"
			+ "knowledge:list,repo:list,repo:detail";

	/** 教务：教务管理，知识/题库维护 + 查看学员与订单 */
	public static final String ACADEMIC_AUTHORITY = "home,exercise:list,"
			+ "repo:list,repo:detail,repo:create,repo:update,repo:delete,"
			+ "template:list,template:create,template:update,template:delete,"
			+ "knowledge:list,knowledge:create,knowledge:update,knowledge:delete,"
			+ "student:list,order:list,"
			+ "project:list,project:detail,answer:list,answer:detail,"
			+ "system:dict:list,system:dictItem:list";

	/**
	 * 内置角色清单（名称 → 默认权限）。
	 */
	public static final Map<String, String> BUILTIN_ROLES = new LinkedHashMap<>();

	static {
		BUILTIN_ROLES.put("管理员", "admin");
		BUILTIN_ROLES.put("校长", "principal");
		BUILTIN_ROLES.put("教师", "teacher");
		BUILTIN_ROLES.put("学管师", "consultant");
		BUILTIN_ROLES.put("教务", "academic");
	}

	/**
	 * 是否为内置角色编码。
	 */
	public static boolean isBuiltin(String code) {
		return ROLE_ADMIN.equals(code) || ROLE_PRINCIPAL.equals(code) || ROLE_TEACHER.equals(code)
				|| ROLE_CONSULTANT.equals(code) || ROLE_ACADEMIC.equals(code);
	}
}
