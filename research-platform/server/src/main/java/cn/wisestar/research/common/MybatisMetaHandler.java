package cn.wisestar.research.common;

import java.time.LocalDateTime;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

import cn.wisestar.research.auth.CurrentUser;
import cn.wisestar.research.auth.UserContext;

@Component
public class MybatisMetaHandler implements MetaObjectHandler {

	@Override
	public void insertFill(MetaObject metaObject) {
		LocalDateTime now = LocalDateTime.now();
		String operator = operatorId();
		strictInsertFill(metaObject, "createAt", LocalDateTime.class, now);
		strictInsertFill(metaObject, "createBy", String.class, operator);
		strictInsertFill(metaObject, "updateAt", LocalDateTime.class, now);
		strictInsertFill(metaObject, "updateBy", String.class, operator);
		if (getFieldValByName("isDeleted", metaObject) == null) {
			setFieldValByName("isDeleted", 0, metaObject);
		}
	}

	@Override
	public void updateFill(MetaObject metaObject) {
		strictUpdateFill(metaObject, "updateAt", LocalDateTime.class, LocalDateTime.now());
		strictUpdateFill(metaObject, "updateBy", String.class, operatorId());
	}

	private String operatorId() {
		CurrentUser user = UserContext.get();
		return user == null ? "system" : user.id();
	}

}
