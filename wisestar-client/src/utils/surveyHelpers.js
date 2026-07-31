/**
 * surveyHelpers.js - 问卷 JSON 结构工具函数
 *
 * Wisestar 问卷采用树形 JSON 结构：
 * {
 *   type: "Survey",              // 根节点类型固定为 Survey
 *   title: "问卷标题",
 *   description: "问卷说明",
 *   attribute: { suffix: "谢谢参与", submitButton: "提交" },
 *   children: [                  // 问题列表
 *     {
 *       id: "q_xxxxx",           // 问题唯一 ID（UUID 生成）
 *       type: "Radio",           // 问题类型
 *       title: "问题文本",
 *       attribute: { required: true },
 *       children: [              // 选项列表（选择类题型）
 *         { id: "opt_xxxxx", type: "Option", title: "选项A", attribute: {} }
 *       ]
 *     }
 *   ]
 * }
 */

// ============================================================
// 生成短 UUID
// ============================================================
const uuid = () => {
  // 生成形如 "q_a1b2c3d4" 的短 ID，类似 Wisestar 原生 ID 格式
  return 'q_' + Math.random().toString(36).substring(2, 10);
};

// ============================================================
// 问题类型列表（中文名 + JSON type 值）
// ============================================================
export const QUESTION_TYPES = [
  { label: '单选题',   value: 'Radio' },
  { label: '多选题',   value: 'Checkbox' },
  { label: '下拉题',   value: 'Select' },
  { label: '填空题',   value: 'FillBlank' },
  { label: '多行文本', value: 'Text' },
  { label: '评分题',   value: 'Score' },
  { label: '备注说明', value: 'Remark' },
];

// ============================================================
// 需要选项的问题类型
// ============================================================
export const TYPES_WITH_OPTIONS = ['Radio', 'Checkbox', 'Select', 'Score'];

// ============================================================
// 创建空白问卷骨架
// ============================================================
// 返回: Wisestar 格式的空白问卷 JSON
export function createEmptySurvey(name) {
  return {
    type: 'Survey',
    title: name || '未命名问卷',
    description: '',
    attribute: {
      suffix: '感谢您的参与！',
      submitButton: '提交',
    },
    children: [],
  };
}

// ============================================================
// 创建新问题
// ============================================================
// 参数: type - 问题类型（Radio/Checkbox/FillBlank 等）
// 返回: 完整的问题节点 JSON 对象
export function createQuestion(type) {
  const base = {
    id: uuid(),
    type,
    title: '',
    attribute: { required: false },
    children: [],
  };

  // 需要选项的题型 → 预置 2 个空选项
  if (TYPES_WITH_OPTIONS.includes(type)) {
    base.children = [
      { id: uuid(), type: 'Option', title: '', attribute: {} },
      { id: uuid(), type: 'Option', title: '', attribute: {} },
    ];
  }

  return base;
}

// ============================================================
// 创建新选项
// ============================================================
// 返回: 一个空选项节点
export function createOption() {
  return { id: uuid(), type: 'Option', title: '', attribute: {} };
}

// ============================================================
// 深拷贝问卷 JSON（避免直接修改原对象）
// ============================================================
export function cloneSurvey(survey) {
  return JSON.parse(JSON.stringify(survey));
}

// ============================================================
// 系统题目（模板）转换为问卷问题节点
// ============================================================
// 输入: 题目管理中的一条记录（TemplateView，见 /api/template/list）
//   { id, name, questionType, template: SurveySchema, tag, repoName }
// 输出: 问卷 children 中的问题节点
//   { id: "q_xxx", type, title, attribute, children: [Option...] }
export function templateToQuestion(templateView) {
  const schema = templateView.template || {};
  const schemaAttr = schema.attribute || {};

  // 问题节点：重新生成 ID 避免与问卷现有问题冲突
  const question = {
    id: uuid(),
    type: templateView.questionType || schema.type || 'Radio',
    title: templateView.name || schema.title || '',
    attribute: {
      required: false,
      // 保留题目原有答案、解析、分值等考试属性
      ...(schemaAttr.examCorrectAnswer !== undefined ? { examCorrectAnswer: schemaAttr.examCorrectAnswer } : {}),
      ...(schemaAttr.examAnalysis !== undefined ? { examAnalysis: schemaAttr.examAnalysis } : {}),
      ...(schemaAttr.examScore !== undefined ? { examScore: schemaAttr.examScore } : {}),
      ...(schemaAttr.examScoreMode !== undefined ? { examScoreMode: schemaAttr.examScoreMode } : {}),
      ...(schemaAttr.examImages !== undefined ? { examImages: schemaAttr.examImages } : {}),
    },
    // 选项保留，重新生成 ID
    children: (schema.children || []).map((c) => ({
      id: uuid(),
      type: c.type || 'Option',
      title: c.title || '',
      attribute: c.attribute ? { ...c.attribute } : {},
    })),
  };

  return question;
}
