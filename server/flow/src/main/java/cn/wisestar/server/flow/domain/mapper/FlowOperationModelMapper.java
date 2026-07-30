package cn.wisestar.server.flow.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.flow.domain.dto.FlowOperationView;
import cn.wisestar.server.flow.domain.model.FlowOperation;
import org.mapstruct.Mapper;

/**
 * @author javahuang
 * @date 2022/1/13
 */
@Mapper
public interface FlowOperationModelMapper extends BaseModelMapper<Void, FlowOperationView, FlowOperation> {

}
