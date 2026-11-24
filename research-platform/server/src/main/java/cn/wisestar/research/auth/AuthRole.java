package cn.wisestar.research.auth;

public enum AuthRole {

	/** 系统管理员：全部数据、全部操作 */
	ADMIN,
	/** 教学主管：全部数据、可审核 */
	SUPERVISOR,
	/** 任课老师：仅本人创建的数据 */
	TEACHER;

	public boolean seesAllData() {
		return this == ADMIN || this == SUPERVISOR;
	}

	public boolean canReview() {
		return this == ADMIN || this == SUPERVISOR;
	}

}
