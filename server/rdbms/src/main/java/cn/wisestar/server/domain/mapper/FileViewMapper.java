package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.FileView;
import cn.wisestar.server.domain.model.File;
import cn.wisestar.server.storage.StorePath;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @author javahuang
 * @date 2021/9/8
 */
@Mapper
public interface FileViewMapper extends BaseModelMapper<Void, FileView, File> {

}
