/**
 * mall.js - 积分商城 API
 *
 * 接口（后端 MallGoodsApi，前缀 /api/mall）:
 *   GET  /mall/goods?status=1   商品列表（学员端传 status=1 仅上架；后台不传全部）
 *   POST /mall/goods/create      新增商品（mall:create）
 *   POST /mall/goods/update      编辑商品（mall:update）
 *   POST /mall/goods/delete      删除商品（mall:delete）
 *
 * 被谁引用: pages/system/MallGoodsManagePage（后台商品管理）、
 *           pages/student/MallPage（学员端商城展示）
 */

import request from './request';

/** 商品列表（status=1 上架；不传返回全部） */
export async function listGoods(status) {
  return request.get('/mall/goods', { params: status === undefined ? {} : { status } });
}

/** 新增商品：{ name, description, imageUrl, points, sort, status } */
export async function createGoods(data) {
  return request.post('/mall/goods/create', data);
}

/** 编辑商品：{ id, ... } */
export async function updateGoods(data) {
  return request.post('/mall/goods/update', data);
}

/** 删除商品：{ id } */
export async function deleteGoods(data) {
  return request.post('/mall/goods/delete', data);
}
