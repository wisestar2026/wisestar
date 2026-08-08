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
 *
 * 调用方一览:
 *   - listProject   : ProjectListPage（问卷列表）、AnswerListPage（拉全量问卷做名称映射）
 *   - getProject    : ProjectEditPage（加载问卷编辑器）、ProjectAnswersPage（显示问卷名称）
 *   - createProject : ProjectListPage（新建问卷弹窗）
 *   - updateProject : ProjectEditPage（保存问卷）
 *   - deleteProject : ProjectListPage（删除问卷）
 *   - getTrashList / restoreProject / destroyProject : 回收站管理（当前前端暂未接入，接口预留）
 *
 * 问卷对象字段:
 *   { id, name, mode(survey|exam|folder), status(0未发布/1已发布), total(答卷数),
 *     survey(问卷JSON，可能为字符串或对象), setting, createAt }
 */

import request from './request';

// ============================================================
// 获取问卷列表（分页）
// ============================================================
// 后端接口: GET /api/project/list
// 参数: { current: 页码, pageSize: 每页条数, name?: 搜索名称 }
//   注意: AnswerListPage 传 pageSize: -1 表示不分页拉取全部问卷
// 返回: { code: 200, data: { list: [...问卷对象], total: 总数 } }
// 调用方: ProjectListPage.fetchProjects、AnswerListPage（项目名称映射表）
export async function listProject(params) {
  return request.get('/project/list', { params });
}

// ============================================================
// 获取单个问卷详情
// ============================================================
// 后端接口: GET /api/project?id=xxx
// 参数: id - 问卷 ID（如 "ThQhZ9"）
// 返回: { code: 200, data: { id, name, survey, setting, ... } }
//   survey 字段可能为 JSON 字符串或对象，调用方需统一解析（见 ProjectEditPage.loadProject）
// 调用方: ProjectEditPage.loadProject、ProjectAnswersPage（取问卷名称）
export async function getProject(id) {
  return request.get('/project', { params: { id } });
}

// ============================================================
// 创建问卷
// ============================================================
// 后端接口: POST /api/project/create
// 参数: { name: "问卷名称", mode: "survey|exam|folder" }
// 返回: { code: 200, data: { id: "新问卷ID", name: "问卷名称", ... } }
// 调用方: ProjectListPage.handleCreate（新建问卷弹窗）
export async function createProject(data) {
  return request.post('/project/create', data);
}

// ============================================================
// 更新问卷
// ============================================================
// 后端接口: POST /api/project/update
// 参数: { id: "问卷ID", name?: "新名称", survey?: {问卷JSON}, setting?: {...} }
// 调用方: ProjectEditPage.handleSave（保存问卷，将编辑器中的 survey JSON 序列化提交）
export async function updateProject(data) {
  return request.post('/project/update', data);
}

// ============================================================
// 删除问卷（软删除，移入回收站）
// ============================================================
// 后端接口: POST /api/project/delete
// 参数: id - 问卷 ID
// 注意: 这是软删除，问卷不会立即从数据库删除，可以在回收站恢复
// 调用方: ProjectListPage.handleDelete
export async function deleteProject(id) {
  return request.post('/project/delete', { id });
}

// ============================================================
// 获取回收站列表
// ============================================================
// 后端接口: GET /api/project/trash
// 参数: { current: 页码, pageSize: 每页条数 }
// 调用方: 回收站管理页面（当前前端暂未接入，接口预留）
export async function getTrashList(params) {
  return request.get('/project/trash', { params });
}

// ============================================================
// 从回收站恢复问卷
// ============================================================
// 后端接口: POST /api/project/restore
// 参数: ids - 要恢复的问卷 ID 数组
// 调用方: 回收站管理页面（当前前端暂未接入，接口预留）
export async function restoreProject(ids) {
  return request.post('/project/restore', { ids });
}

// ============================================================
// 彻底删除问卷（物理删除，不可恢复）
// ============================================================
// 后端接口: POST /api/project/destroy
// 参数: ids - 要删除的问卷 ID 数组
// 调用方: 回收站管理页面（当前前端暂未接入，接口预留）
export async function destroyProject(ids) {
  return request.post('/project/destroy', { ids });
}
