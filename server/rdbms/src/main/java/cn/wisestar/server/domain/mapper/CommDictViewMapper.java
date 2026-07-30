package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.CommDictRequest;
import cn.wisestar.server.domain.dto.CommDictView;
import cn.wisestar.server.domain.model.CommDict;
import org.mapstruct.Mapper;

/**
 * @author javahuang
 * @date 2022/7/19
 */
@Mapper
public interface CommDictViewMapper extends BaseModelMapper<CommDictRequest, CommDictView, CommDict> {

}
