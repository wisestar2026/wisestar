package cn.wisestar.server.impl;

import cn.wisestar.server.domain.model.StudentPermission;
import cn.wisestar.server.mapper.StudentPermissionMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 学员有效学科权限解析。
 *
 * <p>签到、任务完成等无学科语义的奖励，回退到学员有效订单权限中的首个学科，
 * 使这些奖励同样计入该学科单学期学习币上限。</p>
 *
 * @author wisestar
 * @date 2026/9/12
 */
@Component
@RequiredArgsConstructor
public class StudentSubjectResolver {

	private final StudentPermissionMapper studentPermissionMapper;

	/**
	 * 取学员首个有效权限学科。
	 *
	 * @param userId 学员 ID
	 * @return 学科 ID；无有效权限返回 null
	 */
	public String firstActiveSubject(String userId) {
		if (!StringUtils.hasText(userId)) {
			return null;
		}
		List<StudentPermission> permissions = studentPermissionMapper.selectList(
				Wrappers.<StudentPermission>lambdaQuery()
						.eq(StudentPermission::getStudentId, userId)
						.gt(StudentPermission::getExpireAt, new Date())
						.orderByAsc(StudentPermission::getId));
		for (StudentPermission permission : permissions) {
			if (StringUtils.hasText(permission.getSubjectId())) {
				return permission.getSubjectId();
			}
		}
		return null;
	}

}
