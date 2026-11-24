package cn.wisestar.research.plan;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import cn.wisestar.research.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("t_research_plan")
@EqualsAndHashCode(callSuper = false)
public class ResearchPlan extends BaseEntity {

	private String semester;

	private String subject;

	private String grade;

	private String classType;

	private String title;

	private String ownerName;

	private String goal;

	private String outline;

	private Integer classHours;

	private String status;

	private String publishedKey;

	private String reviewRemark;

	private String submitBy;

	private LocalDateTime submitAt;

	private String approveBy;

	private LocalDateTime approveAt;

}
