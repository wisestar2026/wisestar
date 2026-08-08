package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.CacheConsts;
import cn.wisestar.server.domain.dto.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.List;

/**
 * 系统管理服务接口（SystemService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供系统级管理能力：系统信息（名称/Logo/AI 设置）查询与更新、
 * 角色 CRUD、权限码查询、角色与权限初始化。实现类位于 rdbms 模块
 * （SystemServiceImpl）。</p>
 *
 * <p><b>缓存设计</b>：系统信息与 AI 设置使用 {@link CacheConsts#commonCacheName}
 * 缓存（key 分别为 'systemInfo' 与 'aiInfo'），更新系统信息时通过 @Caching
 * 同时失效这两个缓存 key。</p>
 *
 * @author javahuang
 * @date 2021/10/12
 */
public interface SystemService {

	/**
	 * 获取系统信息（带缓存，key='systemInfo'）。
	 *
	 * @return 系统信息视图（含系统名称、Logo 等，见 {@link SystemInfo}）
	 */
	@Cacheable(value = CacheConsts.commonCacheName, key = "'systemInfo'")
	SystemInfo getSystemInfo();

	/**
	 * 更新系统信息（同时失效 systemInfo 与 aiInfo 缓存）。
	 *
	 * @param request 系统信息更新请求（见 {@link SystemInfoRequest}）
	 */
	@Caching(evict = {
			@CacheEvict(value = CacheConsts.commonCacheName, key = "'systemInfo'"),
			@CacheEvict(value = CacheConsts.commonCacheName, key = "'aiInfo'")
	})
	void updateSystemInfo(SystemInfoRequest request);

	/**
	 * 分页查询角色列表。
	 *
	 * @param query 分页 + 筛选条件（见 {@link RoleQuery}）
	 * @return 角色分页列表
	 */
	PaginationResponse<RoleView> getRoles(RoleQuery query);

	/**
	 * 创建角色。
	 *
	 * @param request 角色创建请求（含权限码集合，见 {@link RoleRequest}）
	 */
	void createRole(RoleRequest request);

	/**
	 * 更新角色。
	 *
	 * @param request 角色更新请求（含 id）
	 */
	void updateRole(RoleRequest request);

	/**
	 * 删除角色。
	 *
	 * @param request 角色删除请求（含 id）
	 */
	void deleteRole(RoleRequest request);

	/**
	 * 获取全部权限码列表。
	 *
	 * @return 权限视图列表（含权限码、层级、类型等）
	 */
	List<PermissionView> getPermissions();

	/**
	 * 把代码中的权限定义（@PreAuthorize 注解）与数据库权限字典做比对同步。
	 *
	 * <p>基于 {@link cn.wisestar.server.core.security.PreAuthorizeAnnotationExtractor}
	 * 扫描出的权限码，自动补录数据库中缺失的权限定义，保证权限字典与代码一致。</p>
	 */
	void extractCodeDiffDbPermissions();

	/**
	 * 获取系统 AI 设置（带缓存，key='aiInfo'）。
	 *
	 * @return AI 设置（模型、接口地址、开关等，见 {@link SystemInfo.AiSetting}）
	 */
	@Cacheable(value = CacheConsts.commonCacheName, key = "'aiInfo'")
	SystemInfo.AiSetting getSystemAiSetting();
}
