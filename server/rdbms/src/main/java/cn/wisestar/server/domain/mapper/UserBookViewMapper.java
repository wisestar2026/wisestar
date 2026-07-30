package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.UserBookRequest;
import cn.wisestar.server.domain.dto.UserBookView;
import cn.wisestar.server.domain.model.UserBook;
import org.mapstruct.Mapper;

/**
 * @author javahuang
 * @date 2022/9/9
 */
@Mapper
public interface UserBookViewMapper extends BaseModelMapper<UserBookRequest, UserBookView, UserBook> {

}
