/**
 * mall.js - 积分商城 API
 *
 * 接口（后端 MallGoodsApi / MallOrderApi，前缀 /api/mall）:
 *   GET  /mall/goods?status=1   商品列表（学员端传 status=1 仅上架；后台不传全部）
 *   POST /mall/goods/create      新增商品（mall:create）
 *   POST /mall/goods/update      编辑商品（mall:update）
 *   POST /mall/goods/delete      删除商品（mall:delete）
 *   POST /mall/order/create      学员兑换下单（暂时扣学币，返回核销码）
 *   GET  /mall/order/mine        学员兑换记录
 *   GET  /mall/order/list        老师端核销申请列表（mall:list）
 *   POST /mall/order/verify      老师端核销（mall:update）
 *
 * 被谁引用: pages/system/MallGoodsManagePage（后台商品管理）、
 *           pages/student/MallPage（学员端商城展示/兑换）、
 *           pages/mall/VerifyPage（老师端核销）
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

/** 学员发起兑换：{ goodsId } → 返回订单（含 verifyCode 核销码） */
export async function createMallOrder(data) {
  return request.post('/mall/order/create', data);
}

/** 学员兑换记录（我的订单） */
export async function listMyMallOrders() {
  return request.get('/mall/order/mine');
}

/** 老师端核销申请列表：{ status?, keyword? } */
export async function listMallOrders(params = {}) {
  return request.get('/mall/order/list', { params });
}

/** 老师端核销：{ id, verifyCode }（verifyCode 必填，且须与该订单一致） */
export async function verifyMallOrder(data) {
  return request.post('/mall/order/verify', data);
}

