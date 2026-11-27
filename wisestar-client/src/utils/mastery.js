/**
 * mastery.js - 掌握度勋章档位单一来源
 *
 * 七档与 public/student-assets/mastery/ 位图一一对应，legend / 列表徽章 / 文字评级共用，
 * 避免三处各写一套阈值与名称：
 *   未学 0%（灰） / 入门 1–19%（铜） / 及格 20–39%（银） / 掌握 40–59%（金）
 *   熟练 60–79%（蓝） / 变通 80–99%（紫） / 优秀 100%（红金）
 */

export const MASTERY_TIERS = [
  { key: 'none', file: 'mastery-none', label: '未学', range: '0%', min: 0, max: 0, color: '#94a3b8' },
  { key: 'r1', file: 'mastery-0', label: '入门', range: '1–19%', min: 1, max: 19, color: '#c2703d' },
  { key: 'r2', file: 'mastery-20', label: '及格', range: '20–39%', min: 20, max: 39, color: '#7c8fa6' },
  { key: 'r3', file: 'mastery-40', label: '掌握', range: '40–59%', min: 40, max: 59, color: '#d99a20' },
  { key: 'r4', file: 'mastery-60', label: '熟练', range: '60–79%', min: 60, max: 79, color: '#2196f3' },
  { key: 'r5', file: 'mastery-80', label: '变通', range: '80–99%', min: 80, max: 99, color: '#8b5cf6' },
  { key: 'r6', file: 'mastery-100', label: '优秀', range: '100%', min: 100, max: 100, color: '#e53935' },
];

export function masteryTier(value) {
  const v = Math.max(0, Math.min(100, Math.round(Number(value) || 0)));
  return MASTERY_TIERS.find((t) => v >= t.min && v <= t.max) || MASTERY_TIERS[0];
}
