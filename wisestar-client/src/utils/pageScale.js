/**
 * pageScale.js - 学员端视口缩放单位
 *
 * 把「根单位」绑定到视口宽度，让学员端板块、字号、间距随屏幕等比放大，
 * 而不是靠媒体查询一档一档跳。管理端不使用该单位。
 *
 * 基准：1366px 视口下 1 个单位 = 1px（现有学员机尺寸，保持原视觉大小）；
 * 宽屏按 视口宽 / 1366 放大，封顶 1.75，避免 4K 下元素过大。
 *
 * - CSS 侧：--page-u: clamp(1px, calc(100vw * 0.0007321), 1.75px)
 * - JS 侧：usePageUnit() 返回同一个比例，供 SVG 等无法用 CSS calc 的组件使用
 */

import { useEffect, useState } from 'react';

export const PAGE_BASE_WIDTH = 1366;
export const PAGE_UNIT_MIN = 1;
export const PAGE_UNIT_MAX = 1.75;

/** 根据视口宽度计算缩放单位（1 = 1366 视口下的 1px） */
export function pageUnit(width = typeof window === 'undefined' ? PAGE_BASE_WIDTH : window.innerWidth) {
  if (!width) {
    return PAGE_UNIT_MIN;
  }
  const scale = width / PAGE_BASE_WIDTH;
  return Math.min(PAGE_UNIT_MAX, Math.max(PAGE_UNIT_MIN, scale));
}

/** 响应窗口变化的缩放比例（低配机只在 resize 后一帧更新） */
export function usePageUnit() {
  const [unit, setUnit] = useState(() => pageUnit());

  useEffect(() => {
    let frame = 0;
    const update = () => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(() => setUnit(pageUnit()));
    };
    window.addEventListener('resize', update);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener('resize', update);
    };
  }, []);

  return unit;
}
