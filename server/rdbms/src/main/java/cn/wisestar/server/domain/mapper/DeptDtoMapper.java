package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.DeptRequest;
import cn.wisestar.server.domain.dto.DeptView;
import cn.wisestar.server.domain.model.Dept;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @author javahuang
 * @date 2021/11/2
 */
@Mapper
public interface DeptDtoMapper extends BaseModelMapper<DeptRequest, DeptView, Dept> {

}
