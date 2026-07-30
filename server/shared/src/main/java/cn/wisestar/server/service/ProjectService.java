package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.CacheConsts;
import cn.wisestar.server.domain.dto.ProjectQuery;
import cn.wisestar.server.domain.dto.ProjectRequest;
import cn.wisestar.server.domain.dto.ProjectSetting;
import cn.wisestar.server.domain.dto.ProjectView;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * @author javahuang
 * @date 2021/8/2
 */
public interface ProjectService {

	PaginationResponse<ProjectView> listProject(ProjectQuery filter);

	@Cacheable(value = CacheConsts.projectCache, key = "#id", condition = "#p0!=null", unless = "#result == null")
	ProjectView getProject(String id);

	ProjectView addProject(ProjectRequest request);

	@CacheEvict(value = CacheConsts.projectCache, key = "#request.id")
	void updateProject(ProjectRequest request);

	@CacheEvict(value = CacheConsts.projectCache, key = "#request.id")
	void deleteProject(ProjectRequest id);

	ProjectSetting getSetting(ProjectQuery filter);

	List<ProjectView> getDeleted(ProjectQuery query);

	void batchDestroyProject(ProjectRequest request);

	void restoreProject(ProjectRequest request);

}
