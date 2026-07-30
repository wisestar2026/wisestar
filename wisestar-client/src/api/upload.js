/**
 * upload.js - 文件上传 API
 *
 * 接口:
 *   POST /api/file/create    上传文件（图片、附件等）
 *
 * 说明:
 *   后端 FileView 返回 { id, originalName, previewUrl, content }
 *   题目图片使用 previewUrl 作为引用地址
 */

import axios from 'axios';

/**
 * 上传图片文件
 * @param {File} file - 用户选择的图片文件
 * @returns {Promise<{id: string, originalName: string, previewUrl: string}>}
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
  if (response.data?.code === 200) {
    return response.data.data;
  }
  return response.data;
}

/**
 * 批量上传图片
 * @param {File[]} files - 图片文件数组
 * @returns {Promise<Array<{id: string, originalName: string, previewUrl: string}>>}
 */
export async function uploadImages(files) {
  const results = [];
  for (const file of files) {
    const result = await uploadImage(file);
    if (result?.previewUrl) {
      results.push(result);
    }
  }
  return results;
}
