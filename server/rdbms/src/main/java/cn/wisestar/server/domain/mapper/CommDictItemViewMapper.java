package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.CommDictItemRequest;
import cn.wisestar.server.domain.dto.CommDictItemView;
import cn.wisestar.server.domain.model.CommDictItem;
import org.mapstruct.Mapper;

/**
 * @author javahuang
 * @date 2022/7/19
 */
@Mapper
public interface CommDictItemViewMapper extends BaseModelMapper<CommDictItemRequest, CommDictItemView, CommDictItem> {

}
