/**
 * sentencePack.js - 英语单元句子同步练习固化 API（后台管理端）
 *
 * 后端接口（EnglishSentencePackApi，前缀 /api，权限 english:word:ai）：
 *   POST /english/sentence-pack/generate?version=&grade=&term=&unit=  生成预览（分组调 AI，不落库）
 *   POST /english/sentence-pack/save    body {version,grade,term,unit,content,count}  覆盖保存
 *   GET  /english/sentence-pack/list?version=&grade=&term=&unit=&current=&pageSize=  分页列表（不含正文）
 *   GET  /english/sentence-pack/detail?id=  详情（含正文）
 *   POST /english/sentence-pack/delete?id=  删除
 *   GET  /english/sentence-pack/units?version=&grade=&term=  该册可用单元（批量用）
 *
 * 生成类请求单独放大超时：一次生成包含多组 AI 调用，默认 30s 不够。
 */

import request from './request';

/** 生成单元句子练习内容包预览（不落库） */
export function generateSentencePack(params) {
  return request.post('/english/sentence-pack/generate', null, { params, timeout: 900000 });
}

/** 保存（覆盖）单元句子练习内容包 */
export function saveSentencePack(data) {
  return request.post('/english/sentence-pack/save', data, { timeout: 60000 });
}

/** 句子练习内容包分页列表（不含正文） */
export function listSentencePacks(params) {
  return request.get('/english/sentence-pack/list', { params });
}

/** 句子练习内容包详情（含正文） */
export function getSentencePackDetail(id) {
  return request.get('/english/sentence-pack/detail', { params: { id } });
}

/** 删除句子练习内容包 */
export function deleteSentencePack(id) {
  return request.post('/english/sentence-pack/delete', null, { params: { id } });
}

/** 该册可用单元列表（用于批量生成） */
export function listSentencePackUnits(params) {
  return request.get('/english/sentence-pack/units', { params });
}
