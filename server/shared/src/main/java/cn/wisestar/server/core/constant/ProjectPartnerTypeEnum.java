package cn.wisestar.server.core.constant;

/**
 * 项目参与者类型枚举（ProjectPartnerTypeEnum）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义"项目参与者"的角色类型（对应项目 - 用户关联关系
 * t_project_partner 表）。不同类型的参与者拥有不同的项目操作权限
 * （所有者可管理、协作者可协作、答卷人仅可答题）。</p>
 *
 * <p><b>取值说明</b>：OWNER=1（所有者）、COLLABORATOR=2（协作者）、
 * RESPONDENT_SYS_USER=3（系统用户身份的答卷人）、
 * RESPONDENT_IMP_USER=4（外部导入的答卷人）。</p>
 *
 * @author javahuang
 * @date 2022/6/11
 */
public enum ProjectPartnerTypeEnum {

	/**
	 * 所有者
	 */
	OWNER(1),
	/**
	 * 协作者
	 */
	COLLABORATOR(2),
	/**
	 * 答卷人(系统用户)
	 */
	RESPONDENT_SYS_USER(3),
	/**
	 * 答卷人(外部导入用户)
	 */
	RESPONDENT_IMP_USER(4);

	/**
	 * 类型数值（数据库存储值）。
	 */
	private int type;

	ProjectPartnerTypeEnum(int type) {
		this.type = type;
	}

	/**
	 * 获取类型数值。
	 *
	 * @return 类型 int 值
	 */
	public int getType() {
		return type;
	}

}
