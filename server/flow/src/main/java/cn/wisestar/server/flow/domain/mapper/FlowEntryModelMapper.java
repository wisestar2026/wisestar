package cn.wisestar.server.flow.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.flow.domain.dto.FlowEntryView;
import cn.wisestar.server.flow.domain.model.FlowEntry;
import org.mapstruct.Mapper;

/**
 * @author javahuang
 * @date 2022/1/5
 */
@Mapper
public interface FlowEntryModelMapper extends BaseModelMapper<Void, FlowEntryView, FlowEntry> {

}
