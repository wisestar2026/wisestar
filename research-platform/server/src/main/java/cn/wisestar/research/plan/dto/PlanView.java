package cn.wisestar.research.plan.dto;

import java.time.LocalDateTime;

import cn.wisestar.research.plan.PlanStatus;
import cn.wisestar.research.plan.ResearchPlan;

public record PlanView(String id, String semester, String subject, String grade, String classType, String title,
		String goal, String outline, Integer classHours, String status, String statusName, String reviewRemark,
		String submitBy, LocalDateTime submitAt, String approveBy, LocalDateTime approveAt, String createBy,
		LocalDateTime createAt, LocalDateTime updateAt, String ownerName) {

	public static PlanView of(ResearchPlan plan, String ownerName) {
		return new PlanView(plan.getId(), plan.getSemester(), plan.getSubject(), plan.getGrade(),
				plan.getClassType(), plan.getTitle(), plan.getGoal(), plan.getOutline(), plan.getClassHours(),
				plan.getStatus(), PlanStatus.name(plan.getStatus()), plan.getReviewRemark(), plan.getSubmitBy(),
				plan.getSubmitAt(), plan.getApproveBy(), plan.getApproveAt(), plan.getCreateBy(),
				plan.getCreateAt(), plan.getUpdateAt(), ownerName);
	}

}
