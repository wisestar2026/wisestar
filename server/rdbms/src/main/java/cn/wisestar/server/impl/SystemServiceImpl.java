package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.CacheConsts;
import cn.wisestar.server.core.security.PreAuthorizeAnnotationExtractor;
import cn.wisestar.server.core.uitls.RSAUtils;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.domain.mapper.RoleViewMapper;
import cn.wisestar.server.domain.model.Role;
import cn.wisestar.server.domain.model.SysInfo;
import cn.wisestar.server.domain.model.UserRole;
import cn.wisestar.server.mapper.SysInfoMapper;
import cn.wisestar.server.mapper.UserRoleMapper;
import cn.wisestar.server.service.SystemService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.validation.ValidationException;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @author javahuang
 * @date 2021/10/12
 */
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

	private final RoleServiceImpl roleService;

	private final RoleViewMapper roleViewMapper;

	private final CacheManager cacheManager;

	private final UserRoleMapper userRoleMapper;

	private final SysInfoMapper sysInfoMapper;

	@Override
	public SystemInfo getSystemInfo() {
		SystemInfo systemInfo = new SystemInfo();
		systemInfo.setPublicKey(RSAUtils.DEFAULT_PUBLIC_KEY);

		// 数据库只有一条记录，id为1
		SysInfo info = sysInfoMapper.selectById("1");
		if (info != null) {
			BeanUtils.copyProperties(info, systemInfo);
			if (info.getAiSetting() != null) {
				systemInfo.setAiEnabled(info.getAiSetting().getEnabled());
			}
		}
		return systemInfo;
	}

	@Override
	public void updateSystemInfo(SystemInfoRequest request) {
		SysInfo sysInfo = sysInfoMapper.selectById("1");
		boolean exists = sysInfo != null;
		if (!exists) {
			sysInfo = new SysInfo();
			sysInfo.setId("1");
		}
		mergeSysInfo(sysInfo, request);
		if (exists) {
			sysInfoMapper.updateById(sysInfo);
		} else {
			sysInfoMapper.insert(sysInfo);
		}
	}

	@Override
	public PaginationResponse<RoleView> getRoles(RoleQuery query) {
		Page<Role> rolePage = roleService.pageByQuery(query,
				Wrappers.<Role>lambdaQuery().like(isNotBlank(query.getName()), Role::getName, query.getName()));
		return new PaginationResponse<>(rolePage.getTotal(),
				rolePage.getRecords().stream().map(x -> roleViewMapper.toView(x)).collect(Collectors.toList()));
	}

	@Override
	public void createRole(RoleRequest request) {
		// 角色编码唯一性校验
		if (isNotBlank(request.getCode())) {
			long count = roleService.count(Wrappers.<Role>lambdaQuery().eq(Role::getCode, request.getCode()));
			if (count > 0) {
				throw new ValidationException("角色编码已存在");
			}
		}
		roleService.save(roleViewMapper.fromRequest(request));
	}

	@Override
	public void updateRole(RoleRequest request) {
		if (!CollectionUtils.isEmpty(request.getUserIds())) {
			// 批量添加角色用户
			for (String userId : request.getUserIds()) {
				UserRole userRole = new UserRole();
				userRole.setUserId(userId);
				userRole.setRoleId(request.getId());
				userRoleMapper.insert(userRole);
			}
		} else if (!CollectionUtils.isEmpty(request.getEvictUserIds())) {
			// 批量移除用户角色
			userRoleMapper.delete(Wrappers.<UserRole>lambdaUpdate().eq(UserRole::getRoleId, request.getId())
					.in(UserRole::getUserId, request.getEvictUserIds()));
		} else {
			Role exist = roleService.getById(request.getId());
			if (exist != null && exist.getBuiltin() != null && exist.getBuiltin() == 1) {
				// 内置角色编码不可修改
				if (isNotBlank(request.getCode()) && !exist.getCode().equals(request.getCode())) {
					throw new ValidationException("内置角色编码不可修改");
				}
				// 内置角色编码以数据库为准，防止覆盖为空
				request.setCode(exist.getCode());
			}
			// 编码唯一性校验（排除自身）
			if (isNotBlank(request.getCode())) {
				long count = roleService.count(Wrappers.<Role>lambdaQuery().eq(Role::getCode, request.getCode())
						.ne(Role::getId, request.getId()));
				if (count > 0) {
					throw new ValidationException("角色编码已存在");
				}
			}
			roleService.updateById(roleViewMapper.fromRequest(request));
			evictCache(request.getId());
		}
	}

	@Override
	public void deleteRole(RoleRequest request) {
		Role exist = roleService.getById(request.getId());
		if (exist != null && exist.getBuiltin() != null && exist.getBuiltin() == 1) {
			throw new ValidationException("内置角色不可删除");
		}
		roleService.removeById(request.getId());
		userRoleMapper.delete(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getRoleId, request.getId()));
		evictCache(request.getId());
	}

	/**
	 * 角色信息变化时，清除对应的 cache 缓存
	 * 
	 * @param roleId
	 */
	private void evictCache(String roleId) {
		userRoleMapper.selectList(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getRoleId, roleId)).forEach(
				userRole -> cacheManager.getCache(CacheConsts.userCacheName).evictIfPresent(userRole.getUserId()));
	}

	@Override
	public List<PermissionView> getPermissions() {
		return PreAuthorizeAnnotationExtractor.extractAllApiPermissions().stream().map(x -> new PermissionView(x))
				.collect(Collectors.toList());
	}

	@Override
	public void extractCodeDiffDbPermissions() {
		// TODO:
	}

	@Override
	public SystemInfo.AiSetting getSystemAiSetting() {
		// 数据库只有一条记录，id为1
		SysInfo info = sysInfoMapper.selectById("1");
		return info != null ? info.getAiSetting() : new SystemInfo.AiSetting();
	}

	private void mergeSysInfo(SysInfo target, SystemInfoRequest request) {
		if (isNotBlank(request.getName())) {
			target.setName(request.getName());
		}
		if (isNotBlank(request.getDescription())) {
			target.setDescription(request.getDescription());
		}
		if (isNotBlank(request.getAvatar())) {
			target.setAvatar(request.getAvatar());
		}
		if (isNotBlank(request.getLocale())) {
			target.setLocale(request.getLocale());
		}
		if (request.getRegisterInfo() != null) {
			SystemInfo.RegisterInfo source = request.getRegisterInfo();
			SystemInfo.RegisterInfo registerInfo = target.getRegisterInfo();
			if (registerInfo == null) {
				registerInfo = new SystemInfo.RegisterInfo();
				target.setRegisterInfo(registerInfo);
			}
			if (source.getRegisterEnabled() != null) {
				registerInfo.setRegisterEnabled(source.getRegisterEnabled());
			}
			if (source.getRoles() != null) {
				registerInfo.setRoles(source.getRoles());
			}
			if (source.getStrongPasswordEnabled() != null) {
				registerInfo.setStrongPasswordEnabled(source.getStrongPasswordEnabled());
			}
		}
		if (request.getSetting() != null) {
			SystemInfo.SystemSetting source = request.getSetting();
			SystemInfo.SystemSetting setting = target.getSetting();
			if (setting == null) {
				setting = new SystemInfo.SystemSetting();
				target.setSetting(setting);
			}
			if (source.getCaptchaEnabled() != null) {
				setting.setCaptchaEnabled(source.getCaptchaEnabled());
			}
			if (isNotBlank(source.getCopyright())) {
				setting.setCopyright(source.getCopyright());
			}
			if (isNotBlank(source.getRecordNum())) {
				setting.setRecordNum(source.getRecordNum());
			}
		}
		if (request.getAiSetting() != null) {
			SystemInfo.AiSetting source = request.getAiSetting();
			SystemInfo.AiSetting aiSetting = target.getAiSetting();
			if (aiSetting == null) {
				aiSetting = new SystemInfo.AiSetting();
				target.setAiSetting(aiSetting);
			}
			if (source.getEnabled() != null) {
				aiSetting.setEnabled(source.getEnabled());
			}
			if (source.getModels() != null) {
				aiSetting.setModels(source.getModels());
			}
			if (isNotBlank(source.getToken())) {
				aiSetting.setToken(source.getToken());
			}
			if (isNotBlank(source.getPrompt())) {
				aiSetting.setPrompt(source.getPrompt());
			}
		}
	}
}
