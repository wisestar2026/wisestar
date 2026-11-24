package cn.wisestar.research.plan.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public record PlanSaveRequest(@NotBlank(message = "请选择学期") String semester,
		@NotBlank(message = "请选择学科") String subject, @NotBlank(message = "请选择年级") String grade,
		@NotBlank(message = "请选择班级类型") String classType,
		@NotBlank(message = "请填写方案标题") @Size(max = 200, message = "标题不超过 200 字") String title,
		@Size(max = 2000, message = "教学目标不超过 2000 字") String goal,
		@Size(max = 4000, message = "方案纲要不超过 4000 字") String outline,
		@NotNull(message = "请填写课时数") @Min(value = 1, message = "课时数至少 1") @Max(value = 200, message = "课时数不超过 200") Integer classHours) {
}
