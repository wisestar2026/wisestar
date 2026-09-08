package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.CampusRequest;
import cn.wisestar.server.domain.dto.CampusScope;
import cn.wisestar.server.domain.dto.CampusView;
import cn.wisestar.server.domain.mapper.CampusDtoMapper;
import cn.wisestar.server.domain.model.Campus;
import cn.wisestar.server.domain.model.Role;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.UserCampus;
import cn.wisestar.server.domain.model.UserRole;
import cn.wisestar.server.mapper.CampusMapper;
import cn.wisestar.server.mapper.RoleMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.UserCampusMapper;
import cn.wisestar.server.mapper.UserRoleMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.CampusScopeService;
import cn.wisestar.server.service.CampusService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 校区服务实现（行政管理-校区管理）。
 *
 * <p>校区列表/下拉遵守当前账号数据权限范围；改名在同事务内同步学员引用
 * （t_student.campus 文本 = 校区名，保证引用跟随）；删除仅允许零引用并物理删除，
 * 使同一名称可重新创建。</p>
 *
 * @author wisestar
 * @date 2026/9/8
 */
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class CampusServiceImpl extends BaseService<CampusMapper, Campus> implements CampusService {

	private static final Integer STATUS_ENABLED = 1;

	/** 校区管理统计关注的员工角色编码（校长/教务/学管师） */
	private static final Set<String> ROLE_COUNT_CODES = new LinkedHashSet<>(
			Arrays.asList("principal", "consultant", "academic"));

	private final CampusDtoMapper campusDtoMapper;

	private final CampusScopeService campusScopeService;

	private final StudentMapper studentMapper;

	private final UserCampusMapper userCampusMapper;

	private final UserRoleMapper userRoleMapper;

	private final RoleMapper roleMapper;

	@Override
	public List<CampusView> listCampuses() {
		List<Campus> campuses = campusesInScope();
		return campuses.stream().map(this::toViewWithStats).collect(Collectors.toList());
	}

	@Override
	public List<CampusView> campusOptions(boolean includeDisabled) {
		List<Campus> campuses = campusesInScope();
		if (!includeDisabled) {
			campuses = campuses.stream().filter(c -> STATUS_ENABLED.equals(c.getStatus())).collect(Collectors.toList());
		}
		return campusDtoMapper.toView(campuses);
	}

	@Override
	public void createCampus(CampusRequest request) {
		if (request == null || !StringUtils.hasText(request.getName())) {
			throw new ValidationException("校区名称不能为空");
		}
		Long duplicateCount = count(Wrappers.<Campus>lambdaQuery().eq(Campus::getName, request.getName().trim()));
		if (duplicateCount != null && duplicateCount > 0) {
			throw new ValidationException("校区名称已存在，请勿重复创建");
		}
		Campus campus = campusDtoMapper.fromRequest(request);
		campus.setName(request.getName().trim());
		if (campus.getStatus() == null) {
			campus.setStatus(STATUS_ENABLED);
		}
		save(campus);
	}

	@Override
	public void updateCampus(CampusRequest request) {
		if (request == null || !StringUtils.hasText(request.getId())) {
			throw new ValidationException("校区ID不能为空");
		}
		Campus exist = getById(request.getId());
		if (exist == null) {
			throw new ValidationException("校区不存在");
		}
		String newName = StringUtils.hasText(request.getName()) ? request.getName().trim() : exist.getName();
		if (!newName.equals(exist.getName())) {
			// 名称唯一校验（排除自身）
			Long duplicateCount = count(Wrappers.<Campus>lambdaQuery().eq(Campus::getName, newName)
					.ne(Campus::getId, request.getId()));
			if (duplicateCount != null && duplicateCount > 0) {
				throw new ValidationException("校区名称已存在，请勿重复创建");
			}
			// 同步学员引用（同事务）：将引用旧名称的学员改为新名称
			studentMapper.update(null, Wrappers.<Student>lambdaUpdate().eq(Student::getCampus, exist.getName())
					.set(Student::getCampus, newName));
		}
		Campus campus = campusDtoMapper.fromRequest(request);
		campus.setName(newName);
		updateById(campus);
	}

	@Override
	public void deleteCampus(String id) {
		if (!StringUtils.hasText(id)) {
			throw new ValidationException("校区ID不能为空");
		}
		Campus campus = getById(id);
		if (campus == null) {
			throw new ValidationException("校区不存在");
		}
		Long studentCount = studentMapper
				.selectCount(Wrappers.<Student>lambdaQuery().eq(Student::getCampus, campus.getName()));
		Long bindingCount = userCampusMapper
				.selectCount(Wrappers.<UserCampus>lambdaQuery().eq(UserCampus::getCampusId, campus.getId()));
		if ((studentCount != null && studentCount > 0) || (bindingCount != null && bindingCount > 0)) {
			throw new ValidationException("校区下存在学员或绑定员工，请先停用");
		}
		// 零引用物理删除（逻辑删除会占用唯一名称，禁止使用）
		baseMapper.physicallyDelete(campus.getId());
	}

	@Override
	public void checkAssignable(String name, String currentName) {
		if (!StringUtils.hasText(name)) {
			return;
		}
		String target = name.trim();
		if (StringUtils.hasText(currentName) && currentName.equals(target)) {
			// 保持历史校区（允许停用校区原值回填）
			return;
		}
		Long enabledCount = count(Wrappers.<Campus>lambdaQuery().eq(Campus::getName, target)
				.eq(Campus::getStatus, STATUS_ENABLED));
		if (enabledCount == null || enabledCount == 0) {
			throw new ValidationException("校区不存在或已停用：请选择有效校区");
		}
	}

	// ---------------------------------------------------------------
	// private
	// ---------------------------------------------------------------

	/**
	 * 当前数据范围内的校区实体（ALL 全量；EMPTY 空；SCOPED 仅绑定名称命中行）。
	 */
	private List<Campus> campusesInScope() {
		CampusScope scope = campusScopeService.resolveScope();
		if (scope.isEmpty()) {
			return Collections.emptyList();
		}
		if (scope.isScoped()) {
			Set<String> names = scope.getCampusNames();
			if (CollectionUtils.isEmpty(names)) {
				return Collections.emptyList();
			}
			return list(Wrappers.<Campus>lambdaQuery().in(Campus::getName, names).orderByAsc(Campus::getCreateAt));
		}
		return list(Wrappers.<Campus>lambdaQuery().orderByAsc(Campus::getCreateAt));
	}

	/**
	 * 组装带统计的校区视图（引用学员数 + 绑定三角色员工数）。
	 */
	private CampusView toViewWithStats(Campus campus) {
		CampusView view = campusDtoMapper.toView(campus);
		Long studentCount = studentMapper
				.selectCount(Wrappers.<Student>lambdaQuery().eq(Student::getCampus, campus.getName()));
		view.setStudentCount(studentCount == null ? 0L : studentCount);
		view.setRoleCount(countBoundRoleUsers(campus.getId()));
		return view;
	}

	/**
	 * 统计某校区绑定的校长/教务/学管师（多角色去重按用户数）。
	 */
	private Long countBoundRoleUsers(String campusId) {
		List<UserCampus> bindings = userCampusMapper
				.selectList(Wrappers.<UserCampus>lambdaQuery().eq(UserCampus::getCampusId, campusId));
		if (CollectionUtils.isEmpty(bindings)) {
			return 0L;
		}
		Set<String> userIds = bindings.stream().map(UserCampus::getUserId).collect(Collectors.toSet());
		Set<String> roleIds = roleMapper
				.selectList(Wrappers.<Role>lambdaQuery().in(Role::getCode, ROLE_COUNT_CODES)).stream().map(Role::getId)
				.collect(Collectors.toSet());
		if (CollectionUtils.isEmpty(roleIds)) {
			return 0L;
		}
		return (long) userRoleMapper
				.selectList(Wrappers.<UserRole>lambdaQuery().in(UserRole::getUserId, userIds)
						.in(UserRole::getRoleId, roleIds))
				.stream().map(UserRole::getUserId).distinct().count();
	}

}
