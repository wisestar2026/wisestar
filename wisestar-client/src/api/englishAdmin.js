/**
 * englishAdmin.js - 英语后台内容管理 API（小节 / 语法）
 *
 * 说明:
 *   英语后台页面历史上多用原生 fetch + 硬编码 API_BASE；本文件集中封装
 *   本轮新增的「小节目录」「语法库」接口，统一走 axios 实例（自动解包
 *   { code, data, message }，并携带 sk-token 登录态）。
 *
 * 后端接口（EnglishSectionApi / EnglishGrammarApi，前缀 /api）:
 *   GET  /english/section/list   → 小节分页列表（version/grade/term/unit/section）
 *   POST /english/section/save   → 新增/更新小节
 *   POST /english/section/delete → 删除小节
 *   GET  /english/grammar/list   → 语法分页列表（version/grade/term/unit/section/title）
 *   POST /english/grammar/save   → 新增/更新语法
 *   POST /english/grammar/delete → 删除语法
 *
 * 调用方:
 *   - pages/english/SectionManagePage.jsx
 *   - pages/english/GrammarManagePage.jsx
 *   - pages/english/WordManagePage.jsx / SentenceManagePage.jsx（小节候选下拉）
 */

import request from './request';

/** 小节分页列表 */
export function getSections(params) {
  return request.get('/english/section/list', { params });
}

/** 新增/更新小节 */
export function saveSection(data) {
  return request.post('/english/section/save', data);
}

/** 删除小节 */
export function deleteSection(id) {
  return request.post('/english/section/delete', null, { params: { id } });
}

/** 语法分页列表 */
export function getGrammars(params) {
  return request.get('/english/grammar/list', { params });
}

/** 新增/更新语法 */
export function saveGrammar(data) {
  return request.post('/english/grammar/save', data);
}

/** 删除语法 */
export function deleteGrammar(id) {
  return request.post('/english/grammar/delete', null, { params: { id } });
}
