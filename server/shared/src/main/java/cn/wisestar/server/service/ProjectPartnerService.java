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
 * @author javahuang
 * @date 2022/1/28
 */
public interface ProjectPartnerService {

	PaginationResponse<ProjectPartnerView> listProjectPartner(ProjectPartnerQuery query);

	@CacheEvict(cacheNames = CacheConsts.projectPermissionCacheName,
			key = "T(cn.wisestar.server.core.uitls.SecurityContextUtils).getUserId()")
	void addProjectPartner(ProjectPartnerRequest request);

	@CacheEvict(cacheNames = CacheConsts.projectPermissionCacheName,
			key = "T(cn.wisestar.server.core.uitls.SecurityContextUtils).getUserId()")
	void deleteProjectPartner(ProjectPartnerRequest request);

	@Cacheable(cacheNames = CacheConsts.projectPermissionCacheName,
			key = "T(cn.wisestar.server.core.uitls.SecurityContextUtils).getUserId()")
	List<String> getProjectPerms();

	void downloadPartner(ProjectPartnerQuery request);

	void importPartner(WhiteListRequest request);

}
