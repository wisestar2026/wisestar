package cn.wisestar.research.flow;

import com.baomidou.mybatisplus.annotation.TableName;

import cn.wisestar.research.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("t_research_flow_log")
@EqualsAndHashCode(callSuper = false)
public class ResearchFlowLog extends BaseEntity {

	private String bizType;

	private String bizId;

	private String action;

	private String fromStatus;

	private String toStatus;

	private String remark;

	private String operatorId;

	private String operatorName;

}
