package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.core.constant.AppConsts;
import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.domain.dto.UserInfo;
import cn.wisestar.server.domain.dto.UserRequest;
import cn.wisestar.server.domain.dto.UserView;
import cn.wisestar.server.domain.model.User;
import cn.wisestar.server.domain.model.Account;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.mapper.StudentMapper;
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
	@Mapping(target = "name", expression = "java(resolveName(account))")
	@Mapping(target = "phone", expression = "java(resolvePhone(account))")
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

	/**
	 * 学员（user_type=Student）回填学员姓名；系统用户不在此回填（保持原行为）。
	 */
	default String resolveName(Account account) {
		if (account == null || !isStudent(account)) {
			return null;
		}
		Student student = findStudent(account);
		return student == null ? null : student.getName();
	}

	/**
	 * 学员（user_type=Student）回填联系号码；系统用户不在此回填（保持原行为）。
	 */
	default String resolvePhone(Account account) {
		if (account == null || !isStudent(account)) {
			return null;
		}
		Student student = findStudent(account);
		return student == null ? null : student.getPhone();
	}

	/**
	 * 判断账号是否为学员类型。
	 */
	default boolean isStudent(Account account) {
		return AppConsts.USER_TYPE.Student.toString().equals(account.getUserType());
	}

	/**
	 * 按账号 userId 查询学员主数据。
	 */
	default Student findStudent(Account account) {
		StudentMapper studentMapper = ContextHelper.getBean(StudentMapper.class);
		return studentMapper.selectById(account.getUserId());
	}

}
