package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.DashboardRequest;
import cn.wisestar.server.domain.dto.DashboardView;
import cn.wisestar.server.domain.model.Dashboard;
import org.mapstruct.Mapper;

/**
 * @author javahuang
 * @date 2022/1/28
 */
@Mapper
public interface DashboardViewMapper extends BaseModelMapper<DashboardRequest, DashboardView, Dashboard> {

}
