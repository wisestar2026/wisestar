/**
 * project.js - 问卷管理相关 API 接口
 *
 * 包含问卷的完整 CRUD 操作:
 *   - 列表查询（支持分页和名称搜索）
 *   - 获取详情
 *   - 创建问卷（文件夹/调查问卷/考试）
 *   - 更新问卷
 *   - 删除问卷（移入回收站，非物理删除）
 *   - 回收站管理（查看/恢复/彻底删除）
 *
 * 后端接口约定:
 *   GET    /api/project/list     - 分页查询问卷列表
 *   GET    /api/project          - 获取单个问卷详情
 *   POST   /api/project/create   - 创建问卷
 *   POST   /api/project/update   - 更新问卷
 *   POST   /api/project/delete   - 软删除（移入回收站）
 *   GET    /api/project/trash    - 查看回收站
 *   POST   /api/project/restore  - 从回收站恢复
 *   POST   /api/project/destroy  - 物理删除（不可恢复）
 */

import request from './request';

// ============================================================
// 获取问卷列表（分页）
// ============================================================
// 参数: { current: 页码, pageSize: 每页条数, name?: 搜索名称 }
// 返回: { code: 200, data: { list: [...], total: 总数 } }
export async function listProject(params) {
  return request.get('/project/list', { params });
}

// ============================================================
// 获取单个问卷详情
// ============================================================
// 参数: id - 问卷 ID（如 "ThQhZ9"）
// 返回: { code: 200, data: { id, name, survey, setting, ... } }
export async function getProject(id) {
  return request.get('/project', { params: { id } });
}

// ============================================================
// 创建问卷
// ============================================================
// 参数: { name: "问卷名称", mode: "survey|exam|folder" }
// 返回: { code: 200, data: { id: "新问卷ID", name: "问卷名称", ... } }
export async function createProject(data) {
  return request.post('/project/create', data);
}

// ============================================================
// 更新问卷
// ============================================================
// 参数: { id: "问卷ID", name?: "新名称", survey?: {...}, setting?: {...} }
export async function updateProject(data) {
  return request.post('/project/update', data);
}

// ============================================================
// 删除问卷（软删除，移入回收站）
// ============================================================
// 参数: id - 问卷 ID
// 注意: 这是软删除，问卷不会立即从数据库删除，可以在回收站恢复
export async function deleteProject(id) {
  return request.post('/project/delete', { id });
}

// ============================================================
// 获取回收站列表
// ============================================================
// 参数: { current: 页码, pageSize: 每页条数 }
export async function getTrashList(params) {
  return request.get('/project/trash', { params });
}

// ============================================================
// 从回收站恢复问卷
// ============================================================
// 参数: ids - 要恢复的问卷 ID 数组
export async function restoreProject(ids) {
  return request.post('/project/restore', { ids });
}

// ============================================================
// 彻底删除问卷（物理删除，不可恢复）
// ============================================================
// 参数: ids - 要删除的问卷 ID 数组
export async function destroyProject(ids) {
  return request.post('/project/destroy', { ids });
}
