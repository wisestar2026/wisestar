package cn.wisestar.server.flow.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.flow.domain.dto.FlowEntryNodeRequest;
import cn.wisestar.server.flow.domain.dto.FlowEntryNodeView;
import cn.wisestar.server.flow.domain.model.FlowEntryNode;
import org.mapstruct.Mapper;

/**
 * @author javahuang
 * @date 2022/1/6
 */
@Mapper(componentModel = "spring")
public interface FlowEntryElementModelMapper
		extends BaseModelMapper<FlowEntryNodeRequest, FlowEntryNodeView, FlowEntryNode> {

}
