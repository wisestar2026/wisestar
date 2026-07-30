package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.UserPositionRequest;
import cn.wisestar.server.domain.dto.UserPositionView;
import cn.wisestar.server.domain.model.UserPosition;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @author javahuang
 * @date 2021/11/2
 */
@Mapper
public interface UserPositionDtoMapper extends BaseModelMapper<UserPositionRequest, UserPositionView, UserPosition> {

}
