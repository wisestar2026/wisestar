package cn.wisestar.server.core.constant;

/**
 * 应用全局常量定义（AppConsts）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：集中管理系统内广泛使用的常量：认证令牌名、Cookie 名、
 * 文件类型、项目状态、字典编码、权限类型、用户状态、数据权限类型、
 * 仪表盘类型等，避免魔法值散落各业务代码。</p>
 *
 * <p><b>使用说明</b>：内部按业务语义组织为多个内部接口/枚举：</p>
 * <ul>
 *   <li>{@link FileType}：存储文件类型（与 {@link StorageTypeEnum} 数值一致）；</li>
 *   <li>{@link DICTCODE_PERMISSION_TYPE}：字典编码-权限类型；</li>
 *   <li>{@link RESOURCE_PERMISSION_DISPLAY_TYPE}：前端权限展示类型；</li>
 *   <li>{@link AUTH_TYPE} / {@link USER_TYPE} / {@link USER_STATUS}：认证与用户相关；</li>
 *   <li>{@link DataPermissionTypeEnum}：岗位数据权限范围；</li>
 *   <li>{@link DispositionTypeEnum}：文件下载方式（预览/附件）；</li>
 *   <li>{@link DashboardType}：仪表盘类型；</li>
 *   <li>{@link PermType}：数据权限类型（默认 project）；</li>
 *   <li>{@link ProjectPartnerStatus}：项目参与者状态。</li>
 * </ul>
 *
 * @author javahuang
 * @date 2021/8/6
 */
public class AppConsts {

	/**
	 * token name（JWT 令牌的 Cookie 名 / URL 参数名，见 JwtTokenFilter）
	 */
	public static final String TOKEN_NAME = "sk-token";

	/**
	 * 提交次数限制 Cookie 名（用于问卷作答频率限制）
	 */
	public static final String COOKIE_LIMIT_NAME = "sk-limit";

	/**
	 * 随机问卷答案 ID 前缀
	 */
	public static final String COOKIE_RANDOM_PROJECT_PREFIX = "rd_answer_id_";

	/**
	 * 支持的图片类型
	 */
	public static final String[] SUPPORT_IMAGE_TYPE = { "JPG", "JPEG", "PNG", "GIF", "BMP", "WBMP" };

	/**
	 * 逻辑删除列名
	 */
	public static final String COLUMN_IS_DELETED = "is_deleted";

	/**
	 * 管理员角色标识（Spring Security 权限码，含 ROLE_ 前缀）
	 */
	public static final String ROLE_ADMIN = "ROLE_admin";

	/**
	 * 匿名用户 ID
	 */
	public static final String ANONYMOUS_USER_ID = "guest";

	/**
	 * 当前机构 ID 变量名
	 */
	public static final String VARIABLE_CURRENT_ORG_ID = "currentOrgId";

	/**
	 * 父机构 ID 变量名
	 */
	public static final String VARIABLE_PARENT_ORG_ID = "parentOrgId";

	/**
	 * 项目状态，运行
	 */
	public static final Integer PROJECT_STATUS_RUNNING = 1;

	/**
	 * 项目状态，暂停
	 */
	public static final Integer PROJECT_STATUS_SUSPEND = 0;

	/**
	 * 公开查询密码校验字段名称
	 */
	public static final String PUBLIC_QUERY_PASSWORD_FIELD_ID = "password";

	/**
	 * 存储的文件类型 TODO: 文件添加权限控制
	 */
	public interface FileType {

		/** 背景图片 */
		int BACKGROUND_IMAGE = 1;

		/** 顶部图片 */
		int HEADER_IMAGE = 2;

		/** 问题图片 */
		int QUESTION_IMAGE = 3;

		/** 答卷附件 */
		int ANSWER_ATTACHMENT = 4;

		/** 问题模板预览图 */
		int TEMPLATE_PREVIEW_IMAGE = 5;

		/** 条码图片 */
		int BARCODE = 6;

	}

	/**
	 * 字典编码 - 权限类型
	 */
	public enum DICTCODE_PERMISSION_TYPE {

		MENU, OPERATION, OTHER

	}

	/**
	 * 前端权限类型
	 */
	public enum RESOURCE_PERMISSION_DISPLAY_TYPE {

		MODULE, MENU, PERMISSION

	}

	/**
	 * 认证方式枚举（当前仅支持密码认证 PWD）。
	 */
	public enum AUTH_TYPE {

		PWD

	}

	/**
	 * 用户类型枚举（当前仅系统用户 SysUser）。
	 */
	public enum USER_TYPE {

		/** 系统用户 */
		SysUser

	}

	/**
	 * 用户状态常量：VALID=1 正常、INVALID=0 失活。
	 */
	public interface USER_STATUS {

		/** 正常用户 */
		int VALID = 1;

		/** 失活用户 */
		int INVALID = 0;

	}

	/**
	 * 岗位对应的数据权限
	 */
	public enum DataPermissionTypeEnum {

		/** 本人、本人及下属、本部门、本部门及下属部门、全部 */
		SELF, SELF_AND_SUB, DEPT, DEPT_AND_SUB, ALL;

	}

	/**
	 * 下载文件方式
	 */
	public enum DispositionTypeEnum {

		/**
		 * 预览
		 */
		inline,
		/**
		 * 附件方式下载
		 */
		attachment

	}

	/**
	 * 仪表盘类型
	 */
	public interface DashboardType {

		/** 首页 */
		int HOMEPAGE = 1;

		/** 项目概要页面 */
		int PROJECT_OVERVIEW = 2;

	}

	/**
	 * 数据权限类型（@EnableDataPerm 注解 permType 取值，当前仅支持项目维度）。
	 */
	public interface PermType {

		String PROJECT = "project";

	}

	/**
	 * 项目参与者状态
	 */
	public interface ProjectPartnerStatus {

		/**
		 * 未访问
		 */
		int UNVISITED = 0;

		/**
		 * 已访问
		 */
		int VISITED = 1;

		/**
		 * 已答题
		 */
		int ANSWERED = 2;

		/**
		 * 将状态数值转换为中文描述。
		 *
		 * @param status 状态数值（0 未访问 / 1 已访问 / 2 已答题）
		 * @return 中文状态描述
		 */
		static String getStatusStr(int status) {
			if (status == VISITED) {
				return "已访问";
			}
			else if (status == ANSWERED) {
				return "已答题";
			}
			else {
				return "未访问";
			}
		}

	}

}
