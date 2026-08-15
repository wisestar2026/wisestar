/**
 * hr.js - 人事管理（角色权限）相关 API 接口
 *
 * 包含:
 *   - 获取角色权限树（权限树数据源，按功能模块分组）
 *   - 角色列表（分页 + 名称筛选）
 *   - 新增角色
 *   - 编辑角色（基础信息 + 权限点）
 *   - 删除角色
 *
 * 后端接口: /api/system/role/*（SystemApi）、/api/system/permissionTree
 * 调用方: pages/hr/RoleManagePage.jsx（人事管理 · 角色权限）
 */

import request from './request';

// ============================================================
// 获取角色权限树
// ============================================================
// 后端接口: GET /api/system/permissionTree（仅管理员）
// 返回: { code, data: [{ key, name, children: [{ key, name, children? }] }] }
// 用途: 角色新增/编辑弹窗中的权限树勾选数据源
export async function getPermissionTree() {
  return request.get('/system/permissionTree');
}

// ============================================================
// 角色列表
// ============================================================
// 后端接口: GET /api/system/role/list
// 参数: { name, current, pageSize }
// 返回: { code, data: { records: [...], total, ... } }
// 用途: 角色管理页表格数据
export async function listRole(params) {
  return request.get('/system/role/list', { params });
}

// ============================================================
// 新增角色
// ============================================================
// 后端接口: POST /api/system/role/create
// 参数: { name, code, remark, authorities: [权限点...] }
export async function createRole(data) {
  return request.post('/system/role/create', data);
}

// ============================================================
// 编辑角色
// ============================================================
// 后端接口: POST /api/system/role/update
// 参数: { id, name, code, remark, authorities: [权限点...] }
// 说明: 内置角色编码不可修改；管理员角色权限不可编辑
export async function updateRole(data) {
  return request.post('/system/role/update', data);
}

// ============================================================
// 删除角色
// ============================================================
// 后端接口: POST /api/system/role/delete
// 参数: { id }
// 说明: 内置角色不可删除（后端校验），系统至少保留一个角色
export async function deleteRole(data) {
  return request.post('/system/role/delete', data);
}
