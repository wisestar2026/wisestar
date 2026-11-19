/**
 * engScale.js - 英语学习中心 rem 试点：视口缩放单位
 *
 * 思路对齐成熟教育平台的做法：把「根单位」绑定到视口宽度，让板块、字号、间距
 * 随屏幕等比放大，而不是靠媒体查询一档一档跳。
 *
 * 与目标平台的区别只在作用域：它改的是 html 根字号，影响整站；
 * 这里只在 /student/english 页容器上提供 --eng-u（CSS 变量），
 * 其它页面继续用 px，互不影响。
 *
 * 基准：1366px 视口下 1 个单位 = 1px（现有学员机尺寸，保持原视觉大小）；
 * 宽屏按 视口宽 / 1366 放大，封顶 1.75，避免 4K 下元素过大。
 *
 * - CSS 侧：--eng-u: clamp(1px, calc(100vw * 0.0007321), 1.75px)
 * - JS 侧：useEngUnit() 返回同一个比例，供 SVG 等无法用 CSS calc 的组件使用
 */

import { useEffect, useState } from 'react';

export const ENG_BASE_WIDTH = 1366;
export const ENG_UNIT_MIN = 1;
export const ENG_UNIT_MAX = 1.75;

/** 根据视口宽度计算缩放单位（1 = 1366 视口下的 1px） */
export function engUnit(width = typeof window === 'undefined' ? ENG_BASE_WIDTH : window.innerWidth) {
  if (!width) {
    return ENG_UNIT_MIN;
  }
  const scale = width / ENG_BASE_WIDTH;
  return Math.min(ENG_UNIT_MAX, Math.max(ENG_UNIT_MIN, scale));
}

/** 响应窗口变化的缩放比例（低配机只在 resize 后一帧更新） */
export function useEngUnit() {
  const [unit, setUnit] = useState(() => engUnit());

  useEffect(() => {
    let frame = 0;
    const update = () => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(() => setUnit(engUnit()));
    };
    window.addEventListener('resize', update);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener('resize', update);
    };
  }, []);

  return unit;
}
