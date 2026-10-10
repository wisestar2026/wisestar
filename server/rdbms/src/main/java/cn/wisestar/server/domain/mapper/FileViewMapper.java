package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.FileView;
import cn.wisestar.server.domain.model.File;
import org.mapstruct.Mapper;


/**
 * @author javahuang
 * @date 2021/9/8
 */
@Mapper
public interface FileViewMapper extends BaseModelMapper<Void, FileView, File> {

}
