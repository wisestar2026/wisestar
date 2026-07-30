package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.domain.dto.UserInfo;
import cn.wisestar.server.domain.dto.UserRequest;
import cn.wisestar.server.domain.dto.UserView;
import cn.wisestar.server.domain.model.User;
import cn.wisestar.server.domain.model.Account;
import cn.wisestar.server.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * @author javahuang
 * @date 2021/8/24
 */
@Mapper
public interface UserViewMapper extends BaseModelMapper<UserRequest, UserView, User> {

	@Mapping(target = "authorities", ignore = true)
	@Mapping(target = "userId", source = "id")
	UserInfo toUserInfo(User user);

	@Mapping(source = "authAccount", target = "username")
	@Mapping(source = "authSecret", target = "password")
	UserInfo toUserView(Account account);

	default UserInfo toUserViewById(String id) {
		if (id == null) {
			return null;
		}
		UserMapper userMapper = ContextHelper.getBean(UserMapper.class);
		return toUserInfo(userMapper.selectById(id));
	}

	@Mapping(target = "authAccount", source = "username")
	@Mapping(target = "userId", source = "id")
	Account toAccount(UserRequest request);

}
