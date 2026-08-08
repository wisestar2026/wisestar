/**
 * surveyHelpers.js - 问卷 JSON 结构工具函数
 *
 * 这是问卷/题目数据结构的核心工具模块，被多个组件和页面引用:
 *   - QUESTION_TYPES / TYPES_WITH_OPTIONS : QuestionEditModal、RepoDetailPage、
 *     TemplatePickerModal、ProjectEditPage（题型选项来源）
 *   - createEmptySurvey : ProjectEditPage（新建问卷时的空白骨架）
 *   - createQuestion    : QuestionEditModal、RepoDetailPage、ProjectEditPage（新问题节点）
 *   - createOption      : ProjectEditPage（新选项节点）
 *   - cloneSurvey       : ProjectEditPage（所有修改操作前的深拷贝，防止直接改状态）
 *   - templateToQuestion: TemplatePickerModal（系统题目 → 问卷问题节点，含知识点快照）
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
 *
 * 题目（template）与问卷问题（question）的关系:
 *   - 题目存在题库中（t_template 表），可被多次选入不同问卷
 *   - 选入问卷时通过 templateToQuestion 转换为问题节点，深拷贝属性并重新生成 ID，
 *     保证"一次入卷、快照固定"——后续修改题目不影响已生成的问卷
 */

// ============================================================
// 生成短 UUID
// ============================================================
// 返回: 形如 "q_a1b2c3d4" 的 8 位随机字符串（前缀 q_，模拟 Wisestar 原生 ID 格式）
// 为什么不用完整 UUID: 问卷 JSON 中 ID 只要求同卷内唯一，短 ID 可减小存储体积
const uuid = () => {
  // Math.random().toString(36) 转 36 进制字符串，取第 2~9 位（去掉 "0."）
  return 'q_' + Math.random().toString(36).substring(2, 10);
};

// ============================================================
// 问题类型列表（中文名 + JSON type 值）
// ============================================================
// 注意: 这里不含"判断题(Judge)"，使用方通常自行补充（如 EDITOR_TYPES / ALL_TYPES）
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
// 这些题型创建时自动预置 2 个空选项；FillBlank/Text/Remark 不需要选项
export const TYPES_WITH_OPTIONS = ['Radio', 'Checkbox', 'Select', 'Score'];

// ============================================================
// 创建空白问卷骨架
// ============================================================
// 返回: Wisestar 格式的空白问卷 JSON（children 为空，等待添加问题）
// 调用方: ProjectEditPage（问卷无 survey 数据时创建初始结构）
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
// 返回: 完整的问题节点 JSON 对象（id 提前生成，供调用方拿 ID 做选中操作）
// 调用方: QuestionEditModal.handleSave、RepoDetailPage.handleSave、ProjectEditPage.addQuestion
export function createQuestion(type) {
  const base = {
    id: uuid(),
    type,
    title: '',
    attribute: { required: false },
    children: [],
  };

  // 需要选项的题型 → 预置 2 个空选项（保证一创建即可编辑选项）
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
// 返回: 一个空选项节点（id 唯一，type 固定 Option）
// 调用方: ProjectEditPage.addOption
export function createOption() {
  return { id: uuid(), type: 'Option', title: '', attribute: {} };
}

// ============================================================
// 深拷贝问卷 JSON（避免直接修改原对象）
// ============================================================
// 为什么用 JSON 序列化实现: 问卷结构是纯 JSON 数据（无函数/循环引用），
// 这种方式最简单可靠；ProjectEditPage 所有 setSurvey 修改前都必须 clone，
// 因为 zustand 之外这里用的是 useState，直接改 prev 对象会破坏不可变性
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
//
// 快照机制（重点）:
//   attribute 中会复制题目的 examCorrectAnswer/examAnalysis/examScore/examScoreMode/
//   examImages（考试属性）以及 subject/chapter/knowledgePoint/difficulty（知识点属性）。
//   这些属性在入卷时被"快照"进问卷 JSON，之后即使题目在题库中被修改，
//   已生成的问卷仍按入卷时的答案/解析/知识点进行判分和分析。
//   这就是"历史答卷不受题目修改影响"的实现基础（见 ProjectEditPage 的说明文案）。
//
// 调用方: TemplatePickerModal.handleAdd（勾选题目确认时逐条转换）
export function templateToQuestion(templateView) {
  const schema = templateView.template || {};
  const schemaAttr = schema.attribute || {};

  // 问题节点：重新生成 ID 避免与问卷现有问题冲突
  // （题目 ID 与问卷节点 ID 是两套体系，不能共用，否则同卷重复添加会撞 ID）
  const question = {
    id: uuid(),
    // 题型以题目的 questionType 字段优先，缺失时回退到 schema.type
    type: templateView.questionType || schema.type || 'Radio',
    title: templateView.name || schema.title || '',
    attribute: {
      required: false,
      // 保留题目原有答案、解析、分值等考试属性（仅在存在时复制，避免污染空属性）
      ...(schemaAttr.examCorrectAnswer !== undefined ? { examCorrectAnswer: schemaAttr.examCorrectAnswer } : {}),
      ...(schemaAttr.examAnalysis !== undefined ? { examAnalysis: schemaAttr.examAnalysis } : {}),
      ...(schemaAttr.examScore !== undefined ? { examScore: schemaAttr.examScore } : {}),
      ...(schemaAttr.examScoreMode !== undefined ? { examScoreMode: schemaAttr.examScoreMode } : {}),
      ...(schemaAttr.examImages !== undefined ? { examImages: schemaAttr.examImages } : {}),
      // 学科/章节/知识点/难度快照（供答题明细分析使用）
      // 注意: 题目顶层字段（t_template）也存一份，这里读的是 template.attribute 快照，
      //       因为题目被转换时其顶层字段不一定带知识点（兼容旧数据）
      ...(schemaAttr.subject !== undefined ? { subject: schemaAttr.subject } : {}),
      ...(schemaAttr.chapter !== undefined ? { chapter: schemaAttr.chapter } : {}),
      // 知识点统一归一化为数组（兼容单值字符串的旧数据，保证消费方按数组处理）
      ...(schemaAttr.knowledgePoint !== undefined
        ? { knowledgePoint: Array.isArray(schemaAttr.knowledgePoint) ? schemaAttr.knowledgePoint : [schemaAttr.knowledgePoint] }
        : {}),
      ...(schemaAttr.difficulty !== undefined ? { difficulty: schemaAttr.difficulty } : {}),
    },
    // 选项保留，重新生成 ID（选项文本/属性深拷贝，避免共享引用）
    children: (schema.children || []).map((c) => ({
      id: uuid(),
      type: c.type || 'Option',
      title: c.title || '',
      attribute: c.attribute ? { ...c.attribute } : {},
    })),
  };

  return question;
}
