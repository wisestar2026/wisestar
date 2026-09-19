/**
 * blankMarkers.js - 填空类题目空位标记工具
 *
 * 约定（题库填空题规范）:
 *   - 单项填空（FillBlank）: 题干空位统一写为 __①__
 *   - 多项填空（MultipleBlank）: 每个空位与单项填空格式一致，编号按出现顺序递延
 *     （__①__、__②__、__③__ …）
 *
 * 兼容的输入写法（均会被规范化为 __①__ 系列）:
 *   - 全角空括号（　）/（ ）/（）
 *   - 半角空括号 ( ) / ()
 *   - 已规范的 __①__ 系列（重新按顺序编号）
 *
 * 为什么统一: 判分按空位顺序逐个比对，题干标记与「正确答案」按序一一对应；
 * 统一编号能让学生与老师清晰对应「第 N 个空」。
 */

// CJK 圈号 1~20，与后端/题库导入模板的「正确答案1~12」一致
const CIRCLED_NUMBERS = '①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳';

// 空括号（全角/半角，可含全角空格）或已规范的 __N__ 标记
const BLANK_MARKER_RE = /（\s*[\u3000\s]*）|\(\s*\)|__[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]__/g;

/**
 * 将题干中的空位标记按出现顺序规范化为 __①__、__②__ …
 *
 * @param {string} title 题干文本
 * @returns {string} 规范化后的题干；空值原样返回
 */
export function normalizeBlankMarkers(title) {
  if (!title) return title || '';
  let index = 0;
  return String(title).replace(BLANK_MARKER_RE, () => {
    const marker = CIRCLED_NUMBERS[index];
    index += 1;
    return marker ? `__${marker}__` : `__${index}__`;
  });
}

/**
 * 返回第 n 个空位的题面标记（n 从 0 开始），如 __①__。
 *
 * @param {number} index 空位下标（0 起）
 * @returns {string}
 */
export function blankMarkerLabel(index) {
  const marker = CIRCLED_NUMBERS[index];
  return marker ? `__${marker}__` : `__${index + 1}__`;
}

export { CIRCLED_NUMBERS };
