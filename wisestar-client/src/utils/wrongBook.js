/**
 * wrongBook.js - 错题本纯函数工具（分组 / 统计 / 文案）
 *
 * 职责:
 *   1. 题型/时间等展示文案
 *   2. 从错题列表派生统计口径（知识点、题型、章节、小节）
 *   3. 章节/小节/知识点分组
 *
 * 被谁引用: WrongBookPanel、WrongDrillModal
 */

/** 题型中文映射（与 questionTypes / practiceHelpers 保持一致） */
export const WRONG_TYPE_LABEL = {
  Radio: '单选',
  Checkbox: '多选',
  Select: '下拉',
  Judge: '判断',
  FillBlank: '单项填空',
  MultipleBlank: '多项填空',
  Text: '多行文本',
  Score: '评分',
  Remark: '备注',
};

/** 题型展示名；未知题型回退原值 */
export function typeLabel(type) {
  return WRONG_TYPE_LABEL[type] || type || '题目';
}

/** 最后错误时间格式化 */
export function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-';
}

/** 按 key 函数分组，返回 [ [key, items] ]，保持首次出现顺序 */
export function groupBy(list, keyFn) {
  const map = new Map();
  (list || []).forEach((item) => {
    const key = keyFn(item);
    if (!map.has(key)) {
      map.set(key, []);
    }
    map.get(key).push(item);
  });
  return Array.from(map.entries());
}

/**
 * 按知识点归组（用于统计与「消灭知识点」）。
 * @returns {Array<{id, name, count, items}>} 按错题数倒序
 */
export function groupByKnowledgePoint(list) {
  return groupBy(list, (item) => item.knowledgePointId || '__none__')
    .map(([id, items]) => ({
      id: id === '__none__' ? '' : id,
      name: items[0].knowledgePointName || '未归知识点',
      count: items.length,
      items,
    }))
    .sort((a, b) => b.count - a.count);
}

/**
 * 按章节归组（用于第一栏）。
 * @returns {Array<{id, name, count}>} 按错题数倒序
 */
export function groupByChapter(list) {
  return groupBy(list, (item) => item.chapterId || '__none__')
    .map(([id, items]) => ({
      id: id === '__none__' ? '' : id,
      name: items[0].chapterName || '未归章节',
      count: items.length,
    }))
    .sort((a, b) => b.count - a.count);
}

/**
 * 指定章节下按小节归组（用于第二栏）。
 * @returns {Array<{id, name, count}>} 按错题数倒序
 */
export function groupBySection(list, chapterId) {
  const scoped = (list || []).filter((item) => (item.chapterId || '') === (chapterId || ''));
  return groupBy(scoped, (item) => item.sectionId || '__none__')
    .map(([id, items]) => ({
      id: id === '__none__' ? '' : id,
      name: items[0].sectionName || '未归小节',
      count: items.length,
    }))
    .sort((a, b) => b.count - a.count);
}

/**
 * 指定章节 + 小节下的错题列表。
 */
export function filterBySection(list, chapterId, sectionId) {
  return (list || []).filter(
    (item) => (item.chapterId || '') === (chapterId || '') && (item.sectionId || '') === (sectionId || ''),
  );
}

/**
 * 顶部统计区数据（知识点错题 + 题型错题）。
 */
export function buildWrongStats(list) {
  const kpStats = groupByKnowledgePoint(list).map(({ id, name, count, items }) => ({ id, name, count, items }));
  const typeStats = groupBy(list, (item) => item.questionType || '__unknown__')
    .map(([type, items]) => ({
      type,
      label: typeLabel(items[0].questionType),
      count: items.length,
    }))
    .sort((a, b) => b.count - a.count);
  return { kpStats, typeStats };
}
