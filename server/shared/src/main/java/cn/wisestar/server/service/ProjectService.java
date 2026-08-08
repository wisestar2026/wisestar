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
 * 项目（问卷/考试）服务接口（ProjectService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供问卷/考试项目（Project）的完整管理能力：项目分页、
 * 项目详情、新增/更新/删除项目、项目设置（ProjectSetting）获取、
 * 回收站（已删除列表/批量销毁/恢复）。实现类位于 rdbms 模块（ProjectServiceImpl）。</p>
 *
 * <p><b>缓存设计</b>：项目详情（getProject）使用 {@link CacheConsts#projectCache}
 * 缓存（key 为项目 id，参数非空且结果非空时才缓存），更新/删除时按 id
 * 精确失效缓存。</p>
 *
 * @author javahuang
 * @date 2021/8/2
 */
public interface ProjectService {

	/**
	 * 分页查询项目列表。
	 *
	 * @param filter 分页 + 筛选条件（名称、模式、状态、分组等，见 {@link ProjectQuery}）
	 * @return 项目分页列表
	 */
	PaginationResponse<ProjectView> listProject(ProjectQuery filter);

	/**
	 * 获取项目详情（带缓存：key=项目 id，参数非空且结果非空时生效）。
	 *
	 * @param id 项目 id
	 * @return 项目视图（含题目 schema 与设置）
	 */
	@Cacheable(value = CacheConsts.projectCache, key = "#id", condition = "#p0!=null", unless = "#result == null")
	ProjectView getProject(String id);

	/**
	 * 新增项目。
	 *
	 * @param request 项目创建请求（见 {@link ProjectRequest}）
	 * @return 新建项目视图
	 */
	ProjectView addProject(ProjectRequest request);

	/**
	 * 更新项目（更新后失效项目缓存）。
	 *
	 * @param request 项目更新请求（含 id）
	 */
	@CacheEvict(value = CacheConsts.projectCache, key = "#request.id")
	void updateProject(ProjectRequest request);

	/**
	 * 删除项目（逻辑删除，移入回收站；同时失效项目缓存）。
	 *
	 * @param id 项目删除请求（含 id）
	 */
	@CacheEvict(value = CacheConsts.projectCache, key = "#request.id")
	void deleteProject(ProjectRequest id);

	/**
	 * 获取项目设置（回收规则、答题频率、密码、随机问卷等）。
	 *
	 * @param filter 查询条件（含项目 id）
	 * @return 项目设置对象（见 {@link ProjectSetting}）
	 */
	ProjectSetting getSetting(ProjectQuery filter);

	/**
	 * 查询回收站中的已删除项目。
	 *
	 * @param query 查询条件
	 * @return 已删除项目列表
	 */
	List<ProjectView> getDeleted(ProjectQuery query);

	/**
	 * 批量彻底销毁项目（从回收站物理删除）。
	 *
	 * @param request 请求（含 id 列表）
	 */
	void batchDestroyProject(ProjectRequest request);

	/**
	 * 恢复回收站中的项目。
	 *
	 * @param request 请求（含 id）
	 */
	void restoreProject(ProjectRequest request);

}
