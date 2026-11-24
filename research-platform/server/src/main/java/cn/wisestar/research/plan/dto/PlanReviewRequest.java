package cn.wisestar.research.plan.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public record PlanReviewRequest(@NotNull(message = "请选择审核结果") Boolean approve,
		@Size(max = 500, message = "审核意见不超过 500 字") String remark) {
}
