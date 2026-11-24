package cn.wisestar.research.plan;

public final class PlanStatus {

	public static final String DRAFT = "DRAFT";

	public static final String PENDING = "PENDING";

	public static final String APPROVED = "APPROVED";

	public static final String REJECTED = "REJECTED";

	private PlanStatus() {
	}

	public static String name(String status) {
		return switch (status == null ? "" : status) {
			case DRAFT -> "草稿";
			case PENDING -> "待审核";
			case APPROVED -> "已发布";
			case REJECTED -> "已驳回";
			default -> status;
		};
	}

}
