package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.PositionRequest;
import cn.wisestar.server.domain.dto.PositionView;
import cn.wisestar.server.domain.model.Position;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @author javahuang
 * @date 2021/11/2
 */
@Mapper
public interface PositionDtoMapper extends BaseModelMapper<PositionRequest, PositionView, Position> {

}
