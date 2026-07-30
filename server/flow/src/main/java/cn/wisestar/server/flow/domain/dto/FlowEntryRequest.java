package cn.wisestar.server.flow.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * @author javahuang
 * @date 2022/1/5
 */
@Data
public class FlowEntryRequest {

	/** xml 的 processId */
	private String bpmnXml;

	private String projectId;

	private List<FlowEntryNodeRequest> nodes;

}
