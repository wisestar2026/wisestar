package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.*;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Set;

/**
 * 用户服务接口（UserService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供用户（系统用户）的完整管理能力，并实现 Spring Security
 * 的 {@link UserDetailsService} 作为认证用户来源：用户信息加载、分页查询、
 * 新增/修改/删除、用户名查重、岗位分配、用户组、选择器、注册、用户概览、
 * 导入、任务查询、验证码校验、历史任务查询等。实现类位于 rdbms 模块
 * （UserServiceImpl）。</p>
 *
 * <p><b>安全相关</b>：loadUserByUsername / loadUserById 被 Spring Security
 * 认证链路（{@link cn.wisestar.server.core.config.WebSecurityConfig} 与
 * {@link cn.wisestar.server.core.security.JwtTokenFilter}）调用，返回的
 * UserDetails 包含用户权限（角色）列表。</p>
 *
 * @author javahuang
 * @date 2021/8/24
 */
public interface UserService extends UserDetailsService {

	/**
	 * 按用户 id 加载用户信息（JWT 认证链路使用）。
	 *
	 * @param userId 用户 id
	 * @return 用户详情（含权限，见 {@link UserInfo}）
	 */
	UserInfo loadUserById(String userId);

	/**
	 * 分页查询用户列表。
	 *
	 * @param query 分页 + 筛选条件（名称、部门、岗位、状态等，见 {@link UserQuery}）
	 * @return 用户分页列表
	 */
	PaginationResponse<UserView> getUsers(UserQuery query);

	/**
	 * 新增用户。
	 *
	 * @param request 用户创建请求（见 {@link UserRequest}）
	 */
	void createUser(UserRequest request);

	/**
	 * 更新用户。
	 *
	 * @param request 用户更新请求（含 id）
	 */
	void updateUser(UserRequest request);

	/**
	 * 删除用户。
	 *
	 * @param id 用户 id
	 */
	void deleteUser(String id);

	/**
	 * 检查用户名是否已存在。
	 *
	 * @param username 用户名
	 * @return true 表示已存在
	 */
	boolean checkUsernameExist(String username);

	/**
	 * 更新用户岗位（关联用户-岗位）。
	 *
	 * @param request 用户请求（含用户 id 与岗位信息）
	 */
	void updateUserPosition(UserRequest request);

	/**
	 * 获取用户所属分组（组 id 集合）。
	 *
	 * @param userId 用户 id
	 * @return 分组 id 集合
	 */
	Set<String> getUserGroups(String userId);

	/**
	 * 获取指定分组下的用户（用于按组圈定用户范围）。
	 *
	 * @param groupId     分组 id
	 * @param currentUser 当前用户 id（用于排除自身等场景）
	 * @return 用户 id 集合
	 */
	Set<String> getUsersByGroup(String groupId, String currentUser);

	/**
	 * 用户选择器数据源。
	 *
	 * @param request 查询条件（见 {@link SelectUserRequest}）
	 * @return 用户信息列表
	 */
	List<UserInfo> selectUsers(SelectUserRequest request);

	/**
	 * 用户注册（公开注册接口）。
	 *
	 * @param request 注册请求（用户名、密码、角色等，见 {@link RegisterRequest}）
	 */
	void register(RegisterRequest request);

	/**
	 * 获取注册页可选的初始角色列表。
	 *
	 * @return 注册角色视图列表
	 */
	List<RegisterRoleView> getRegisterRoles();

	/**
	 * 获取当前用户的概览数据（如我的任务统计）。
	 *
	 * @return 用户概览视图（见 {@link UserOverview}）
	 */
	UserOverview getUserOverviewData();

	/**
	 * 批量导入用户。
	 *
	 * @param request 用户请求（含批量用户数据）
	 */
	void importUser(UserRequest request);

	/**
	 * 分页查询当前用户的任务列表。
	 *
	 * @param query 分页 + 筛选条件（见 {@link MyTaskQuery}）
	 * @return 任务分页列表（MyTaskView）
	 */
	PaginationResponse<MyTaskView> queryTask(MyTaskQuery query);

	/**
	 * 校验验证码（登录/注册场景）。
	 *
	 * @param request 认证请求（含验证码，见 {@link AuthRequest}）
	 */
	void validateCaptcha(AuthRequest request);

	/**
	 * 分页查询历史任务。
	 *
	 * @param query 分页 + 筛选条件
	 * @return 历史任务分页列表
	 */
	PaginationResponse<MyTaskView> queryHistoryTask(MyTaskQuery query);

}
