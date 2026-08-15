/**
 * system.js - 系统管理 API 封装（用户/部门/岗位/字典/字典条目）
 *
 * 接口来源: SystemApi（/api/system/*）+ UserApi（用户相关复用）
 *   用户:   /system/user/list|create|update|delete|updatePosition、/system/checkUsernameExist
 *   部门:   /system/dept/list|create|update|delete|sort（list 返回平铺数组，parentId 引用成树）
 *   岗位:   /system/position/list|create|update|delete
 *   字典:   /system/dict/list|create|update|delete
 *   字典条目: /system/dictItem/list|create|update|delete、/system/dictItem/import（multipart）
 *   角色:   /system/role/list（用户管理页角色下拉复用）
 *
 * 数据流:
 *   页面调用 api 函数 → request.js（统一解包 { code, data }）→ 后端 SystemApi → Service
 *   字典条目导入走原生 axios + multipart/form-data（不经过 JSON 拦截器）
 *
 * 被谁引用:
 *   - pages/system/UserManagePage.jsx
 *   - pages/system/DeptManagePage.jsx
 *   - pages/system/PositionManagePage.jsx
 *   - pages/system/DictManagePage.jsx
 *   - pages/system/DictItemManagePage.jsx
 */

import request from './request';
import axios from 'axios';

// ------------------------------------------------------------
// 用户管理
// ------------------------------------------------------------

/** 用户分页列表：params { current, pageSize, name, deptId, roleId } */
export const listUsers = (params) => request.get('/system/user/list', { params });

/** 新增用户：UserRequest（username/password/status 必填；roles[]、userPositions[]） */
export const createUser = (data) => request.post('/system/user/create', data);

/** 编辑用户：UserRequest（username 可改；password 留空不改） */
export const updateUser = (data) => request.post('/system/user/update', data);

/** 删除用户（连同登录账号）：{ id } */
export const deleteUser = (data) => request.post('/system/user/delete', data);

/** 登录账号唯一性检查：?username=xx → true 已存在 */
export const checkUsernameExist = (username) => request.get('/system/checkUsernameExist', { params: { username } });

/** 调整用户岗位 */
export const updateUserPosition = (data) => request.post('/system/user/updatePosition', data);

// ------------------------------------------------------------
// 部门管理
// ------------------------------------------------------------

/** 部门列表（平铺数组，含 parentId 层级引用） */
export const listDepts = () => request.get('/system/dept/list');

/** 新增部门：{ parentId, name, shortName, code, managerId, remark } */
export const addDept = (data) => request.post('/system/dept/create', data);

/** 编辑部门 */
export const updateDept = (data) => request.post('/system/dept/update', data);

/** 删除部门：{ id } */
export const deleteDept = (data) => request.post('/system/dept/delete', data);

/** 部门排序：{ nodes: string[] } */
export const sortDept = (nodes) => request.post('/system/dept/sort', { nodes });

// ------------------------------------------------------------
// 岗位管理
// ------------------------------------------------------------

/** 岗位分页列表：params { current, pageSize, name } */
export const listPositions = (params) => request.get('/system/position/list', { params });

/** 新增岗位：{ name, code, isVirtual, dataPermissionType } */
export const addPosition = (data) => request.post('/system/position/create', data);

/** 编辑岗位 */
export const updatePosition = (data) => request.post('/system/position/update', data);

/** 删除岗位：{ id } */
export const deletePosition = (data) => request.post('/system/position/delete', data);

// ------------------------------------------------------------
// 字典管理
// ------------------------------------------------------------

/** 字典分页列表：params { current, pageSize, name } */
export const listDicts = (params) => request.get('/system/dict/list', { params });

/** 新增字典：{ code, name, remark, dictType } */
export const addDict = (data) => request.post('/system/dict/create', data);

/** 编辑字典 */
export const updateDict = (data) => request.post('/system/dict/update', data);

/** 删除字典（同步删除其全部条目）：{ id } */
export const deleteDict = (data) => request.post('/system/dict/delete', data);

// ------------------------------------------------------------
// 字典条目管理
// ------------------------------------------------------------

/** 字典条目分页列表：params { current, pageSize, dictCode } */
export const listDictItems = (params) => request.get('/system/dictItem/list', { params });

/** 新增/编辑字典条目（saveOrUpdate） */
export const saveDictItem = (data) => request.post('/system/dictItem/create', data);

/** 编辑字典条目 */
export const updateDictItem = (data) => request.post('/system/dictItem/update', data);

/** 删除字典条目：{ id } */
export const deleteDictItem = (data) => request.post('/system/dictItem/delete', data);

/**
 * 字典条目 Excel 导入
 * Excel 列（首行为表头跳过）：itemName / itemValue / parentItemValue / itemLevel / itemOrder
 * @param {File} file - Excel 文件（.xlsx）
 * @param {string} dictCode - 目标字典编码
 */
export async function importDictItems(file, dictCode) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('dictCode', dictCode);
  // 使用原生 axios（multipart/form-data 不经过 request 拦截器）
  const response = await axios.post('/api/system/dictItem/import', formData, {
    withCredentials: true,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  if (response.data?.code === 200) {
    return response.data;
  }
  throw new Error(response.data?.message || '导入失败');
}

// ------------------------------------------------------------
// 角色（用户管理页角色多选下拉）
// ------------------------------------------------------------

/** 角色列表（pageSize=-1 取全量，供下拉选择） */
export const listAllRoles = (params = {}) => request.get('/system/role/list', { params: { pageSize: -1, ...params } });
