package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.CampusRequest;
import cn.wisestar.server.domain.dto.CampusView;
import cn.wisestar.server.domain.model.Campus;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 校区 请求DTO ↔ 实体 ↔ 视图DTO 转换（MapStruct 生成实现）。
 *
 * @author wisestar
 * @date 2026/9/8
 */
@Mapper
public interface CampusDtoMapper extends BaseModelMapper<CampusRequest, CampusView, Campus> {

}
