package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.ProjectPartnerRequest;
import cn.wisestar.server.domain.dto.ProjectPartnerView;
import cn.wisestar.server.domain.model.ProjectPartner;
import org.mapstruct.Mapper;

/**
 * @author javahuang
 * @date 2022/9/6
 */
@Mapper
public interface ProjectPartnerViewMapper
		extends BaseModelMapper<ProjectPartnerRequest, ProjectPartnerView, ProjectPartner> {

}
