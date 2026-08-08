package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.CacheConsts;
import cn.wisestar.server.domain.dto.ProjectPartnerQuery;
import cn.wisestar.server.domain.dto.ProjectPartnerRequest;
import cn.wisestar.server.domain.dto.ProjectPartnerView;
import cn.wisestar.server.domain.dto.WhiteListRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * 项目参与者服务接口（ProjectPartnerService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供项目参与者（答卷人/协作者）的管理能力：参与者分页、
 * 添加/删除参与者、获取当前用户的可见项目权限集合（数据权限核心）、
 * 参与者导出与批量导入。实现类位于 rdbms 模块（ProjectPartnerServiceImpl）。</p>
 *
 * <p><b>缓存设计</b>：当前用户的"可见项目权限集合"（getProjectPerms）使用
 * {@link CacheConsts#projectPermissionCacheName} 缓存（key 为用户 id），
 * 添加/删除参与者时按当前用户 id 精确失效该缓存，保证权限变更实时生效。
 * 该缓存被 {@link cn.wisestar.server.core.aop.DataPermAspect} 数据权限切面调用。</p>
 *
 * @author javahuang
 * @date 2022/1/28
 */
public interface ProjectPartnerService {

	/**
	 * 分页查询项目参与者列表。
	 *
	 * @param query 分页 + 筛选条件（按项目、类型、状态等，见 {@link ProjectPartnerQuery}）
	 * @return 参与者分页列表
	 */
	PaginationResponse<ProjectPartnerView> listProjectPartner(ProjectPartnerQuery query);

	/**
	 * 添加项目参与者（同时清除当前用户的项目权限缓存）。
	 *
	 * @param request 参与者添加请求（含项目、用户、类型等，见 {@link ProjectPartnerRequest}）
	 */
	@CacheEvict(cacheNames = CacheConsts.projectPermissionCacheName,
			key = "T(cn.wisestar.server.core.uitls.SecurityContextUtils).getUserId()")
	void addProjectPartner(ProjectPartnerRequest request);

	/**
	 * 删除项目参与者（同时清除当前用户的项目权限缓存）。
	 *
	 * @param request 参与者删除请求（含 id）
	 */
	@CacheEvict(cacheNames = CacheConsts.projectPermissionCacheName,
			key = "T(cn.wisestar.server.core.uitls.SecurityContextUtils).getUserId()")
	void deleteProjectPartner(ProjectPartnerRequest request);

	/**
	 * 获取当前用户可访问的项目权限集合（带缓存）。
	 *
	 * <p>返回当前用户有权访问的项目 id 集合，是数据权限校验的核心数据源：
	 * {@link cn.wisestar.server.core.aop.DataPermAspect} 通过它判断用户能否
	 * 访问指定问卷/项目。缓存 key 为当前用户 id。</p>
	 *
	 * @return 可访问项目 id 列表（String）
	 */
	@Cacheable(cacheNames = CacheConsts.projectPermissionCacheName,
			key = "T(cn.wisestar.server.core.uitls.SecurityContextUtils).getUserId()")
	List<String> getProjectPerms();

	/**
	 * 导出项目参与者列表（Excel/CSV）。
	 *
	 * @param request 导出查询条件
	 */
	void downloadPartner(ProjectPartnerQuery request);

	/**
	 * 批量导入参与者（白名单方式）。
	 *
	 * @param request 导入请求（含外部用户名单等，见 {@link WhiteListRequest}）
	 */
	void importPartner(WhiteListRequest request);

}
