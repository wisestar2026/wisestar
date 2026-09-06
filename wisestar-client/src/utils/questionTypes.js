/**
 * questionTypes.js - 题目（题库）题型常量
 *
 * 与「问卷题型」的区别:
 *   surveyHelpers.QUESTION_TYPES 是全量问卷题型（单选/多选/下拉/填空/多行文本/评分/备注，
 *   不含判断题），供问卷编辑器（ProjectEditPage）出卷使用。
 *   本文件是「题库/题目管理」维度的题型集合——按用户约定，题库仅保留五种题型:
 *   判断题 / 单选题 / 单项填空 / 多选题 / 多项填空，其余问卷题型不可作为题库题目新建。
 *
 * 使用方:
 *   - EXAM_TYPES     : QuestionEditModal（新建/编辑题型下拉）、QuestionListPage（题型筛选）、
 *                      SelectTemplateModal / TemplatePickerModal（选题目题型筛选）
 *   - TYPE_LABELS    : 各列表/卡片/结果页的题型中文展示（含历史问卷题型 key，兼容存量数据）
 *   - FORM_BLANK_VALUES : 填空类题型集合（单项填空/多项填空），渲染空位输入时使用
 *
 * 后端题型值（SurveySchema.QuestionType）与前端一致:
 *   Judge / Radio / FillBlank / Checkbox / MultipleBlank
 */

// 题库可新建的五种题型（顺序即下拉展示顺序）
export const EXAM_TYPES = [
  { label: '判断题', value: 'Judge' },
  { label: '单选题', value: 'Radio' },
  { label: '单项填空', value: 'FillBlank' },
  { label: '多选题', value: 'Checkbox' },
  { label: '多项填空', value: 'MultipleBlank' },
];

// 五类题型的 value 集合（判断题型是否属于题库类型）
export const EXAM_TYPE_VALUES = EXAM_TYPES.map((t) => t.value);

// 填空题类（题干带空位，答案自由文本；MultipleBlank 多空答案以 | 分隔存储/判分）
export const BLANK_TYPE_VALUES = ['FillBlank', 'MultipleBlank'];

// 完整题型中文标签（含历史问卷题型，保证存量题目仍可显示中文）
export const TYPE_LABELS = {
  Judge: '判断题',
  Radio: '单选题',
  FillBlank: '单项填空',
  Checkbox: '多选题',
  MultipleBlank: '多项填空',
  Select: '下拉题',
  Text: '多行文本',
  Textarea: '简答题',
  Score: '评分题',
  Remark: '备注说明',
};

/**
 * 取题型中文名；未知类型原样返回。
 * @param {string} type 题型 value（如 Radio / MultipleBlank）
 * @returns {string} 中文标签或原值
 */
export function typeLabel(type) {
  return TYPE_LABELS[type] || type || '';
}
