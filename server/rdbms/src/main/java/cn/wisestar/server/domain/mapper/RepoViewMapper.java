package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.RepoRequest;
import cn.wisestar.server.domain.dto.RepoView;
import cn.wisestar.server.domain.model.Repo;
import org.mapstruct.Mapper;

/**
 * @author javahuang
 * @date 2022/4/27
 */
@Mapper(componentModel = "spring")
public interface RepoViewMapper extends BaseModelMapper<RepoRequest, RepoView, Repo> {

}
