package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

import java.util.Date;

/**
 * 学科视图（对应 t_subject，知识管理板块一级维度）。
 *
 * <p>查询/创建/更新共用本类：GET 参数绑定（list 时可不传）、
 * POST body（create/update 含 id）。字段与数据库列一一对应。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
public class SubjectRequest {

	/**
	 * 学科ID（新增为空，更新必传）。
	 */
	private String id;

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
	 * 主题色（学生端学科配色，如 orange/blue/green）。
	 */
	private String themeColor;

	/**
	 * 排序（数字越小越靠前）。
	 */
	private Integer sort;

	/**
	 * 创建时间（仅展示用，新增时忽略）。
	 */
	private Date createAt;

}
