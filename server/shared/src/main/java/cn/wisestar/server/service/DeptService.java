package cn.wisestar.server.service;

import cn.wisestar.server.core.constant.CacheConsts;
import cn.wisestar.server.domain.dto.DeptRequest;
import cn.wisestar.server.domain.dto.DeptSortRequest;
import cn.wisestar.server.domain.dto.DeptView;
import cn.wisestar.server.domain.dto.SelectDeptRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * 组织机构（部门）服务接口（DeptService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供组织机构的树形管理能力：部门列表（树）、部门详情、
 * 新增/修改/删除部门、部门排序。部门用于组织用户归属与数据权限范围。
 * 实现类位于 rdbms 模块（DeptServiceImpl）。</p>
 *
 * <p><b>缓存设计</b>：部门树使用 {@link CacheConsts#deptCacheName} 缓存：
 * <em>仅查询全部机构</em>（参数为 null）时启用缓存（key 由
 * {@link cn.wisestar.server.core.cache.DeptKeyGenerator} 生成），
 * 任何增删改/排序操作均全量清空该缓存（allEntries=true）保证一致性。</p>
 *
 * @author javahuang
 * @date 2021/11/2
 */
public interface DeptService {

	/**
	 * 查询所有的组织机构信息，只有查询所有机构的时候才启用缓存
	 * @param request
	 */
	@Cacheable(cacheNames = CacheConsts.deptCacheName, keyGenerator = "deptKeyGenerator", condition = "#p0 == null")
	List<DeptView> listDept(SelectDeptRequest request);

	/**
	 * 获取单个部门详情。
	 *
	 * @param id 部门 id
	 * @return 部门视图
	 */
	DeptView getDept(String id);

	/**
	 * 新增部门（新增后清空部门树缓存）。
	 *
	 * @param request 部门创建请求（见 {@link DeptRequest}）
	 */
	@CacheEvict(cacheNames = CacheConsts.deptCacheName, allEntries = true)
	void addDept(DeptRequest request);

	/**
	 * 更新部门（更新后清空部门树缓存）。
	 *
	 * @param request 部门更新请求（含 id）
	 */
	@CacheEvict(cacheNames = CacheConsts.deptCacheName, allEntries = true)
	void updateDept(DeptRequest request);

	/**
	 * 删除部门（删除后清空部门树缓存）。
	 *
	 * @param id 部门 id
	 */
	@CacheEvict(cacheNames = CacheConsts.deptCacheName, allEntries = true)
	void deleteDept(String id);

	/**
	 * 部门排序（排序后清空部门树缓存）。
	 *
	 * @param request 排序请求（含部门 id 与排序列表，见 {@link DeptSortRequest}）
	 */
	@CacheEvict(cacheNames = CacheConsts.deptCacheName, allEntries = true)
	void sortDept(DeptSortRequest request);

}
