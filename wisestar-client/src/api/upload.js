/**
 * upload.js - 文件上传 API
 *
 * 接口:
 *   POST /api/file/create    上传文件（图片、附件等）
 *
 * 说明:
 *   后端 FileView 返回 { id, originalName, previewUrl, content }
 *   题目图片使用 previewUrl 作为引用地址（存到题目 attribute.examImages）
 *
 * 调用方:
 *   - uploadImage  : QuestionEditModal.handleImageUpload（题目新建/编辑弹窗上传配图）
 *   - uploadImages : 批量上传（当前前端暂未直接调用，接口预留）
 *
 * 核心数据流:
 *   QuestionEditModal 选择图片 → uploadImage(file) → POST /api/file/create (FormData)
 *   → 返回 { id, originalName, previewUrl } → previewUrl 存入题目 attribute.examImages
 *   → 随题目 JSON 提交到 /api/template/create 或 /api/template/update
 */

import axios from 'axios';

/**
 * 上传图片文件
 * 后端接口: POST /api/file/create
 * @param {File} file - 用户选择的图片文件
 * @returns {Promise<{id: string, originalName: string, previewUrl: string}>}
 *   成功时解包返回 data 部分（去掉 { code, message } 外层）
 * 调用方: QuestionEditModal.handleImageUpload
 */
export async function uploadImage(file) {
  const formData = new FormData();
  formData.append('file', file);
  // 使用原生 axios（multipart/form-data 不经过 request 拦截器处理 JSON 格式）
  const response = await axios.post('/api/file/create', formData, {
    withCredentials: true,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  // 后端返回 { code: 200, data: { id, originalName, previewUrl } }
  // 手动校验 code 并解包出 data，与 request 拦截器的行为保持一致
  if (response.data?.code === 200) {
    return response.data.data;
  }
  return response.data;
}

/**
 * 批量上传图片
 * @param {File[]} files - 图片文件数组
 * @returns {Promise<Array<{id: string, originalName: string, previewUrl: string}>>}
 *   逐个调用 uploadImage，只保留上传成功且含 previewUrl 的结果
 * 调用方: 批量传图功能（当前前端暂未接入，接口预留）
 */
export async function uploadImages(files) {
  const results = [];
  for (const file of files) {
    const result = await uploadImage(file);
    // 过滤掉失败（无 previewUrl）的上传结果，避免脏数据
    if (result?.previewUrl) {
      results.push(result);
    }
  }
  return results;
}
