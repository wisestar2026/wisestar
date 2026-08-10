/**
 * useKnowledgeStore.js - 知识管理板块全局状态（管理端 mock）
 *
 * 管理结构: 学科 → 章节 → 小节 → 知识点
 *   章节: 学科下的大单元（如「100以内加减法」）
 *   小节: 章节内的学习小站（含: 小节内容设置 + 小节练习设置）
 *   知识点: 小节下的最小学习单元（含: 知识点内容设置 + 测试题型）
 *
 * 数据为纯前端 mock（与《Wisestar智习系统学生端需求文档V2.0》第十章的
 * t_subject / t_textbook_version / t_user_knowledge_progress 概念对应，
 * 后续接入后端时替换为 api 调用，页面无需大改）。
 *
 * 被谁引用: pages/knowledge/ 三个管理页
 * 依赖: zustand
 */

import { create } from 'zustand';

// ---- 题型映射（与学生端/练习判分语义对齐） ----
export const QUESTION_TYPES = [
  { value: 'Radio',    label: '单选题' },
  { value: 'Checkbox', label: '多选题' },
  { value: 'Judge',    label: '判断题' },
  { value: 'FillBlank', label: '填空题' },
];

export const DIFFICULTY_OPTIONS = ['基础', '进阶', '挑战'];

// ---- mock 种子数据：学科 → 章节 → 小节 → 知识点 ----
// 章节/知识点命名与学生端 mock（useStudentStore）呼应，便于演示「管理端设置 → 学生端展示」闭环
const seedSubjects = [
  {
    id: 'chinese', name: '语文', icon: '📚', theme: 'orange',
    chapters: [
      {
        id: 'c1', name: '识字与写字', icon: '🖋️', sort: 1,
        sections: [
          {
            id: 'c1s1', name: '拼音入门', sort: 1,
            content: {
              objective: '掌握拼音字母读写与拼读规则',
              overview: '本小节学习声母、韵母与整体认读音节，打好拼音基础。',
              points: ['声母 23 个 / 韵母 24 个 / 整体认读音节 16 个', '四声标调规则', '常见拼读组合训练'],
            },
            practice: { questionCount: 10, difficulty: '基础', types: ['Radio', 'FillBlank'] },
            kps: [
              {
                id: 'c1s1k1', name: '拼音王国', sort: 1,
                content: { points: ['单韵母 a o e i u ü 的认读', '声母与韵母拼读方法', '整体认读音节 zh ch sh r 等'] },
                questions: [
                  { id: 'q1', type: 'Radio', q: '下列哪个是单韵母？', options: ['a', 'b', 'ch'], answer: ['a'], score: 5 },
                  { id: 'q2', type: 'Judge', q: '"mā" 是第一声。', options: ['正确', '错误'], answer: ['正确'], score: 5 },
                  { id: 'q3', type: 'FillBlank', q: '写出 "b-a" 的拼读：____', options: [], answer: ['ba'], score: 5 },
                ],
              },
              {
                id: 'c1s1k2', name: '汉字笔顺', sort: 2,
                content: { points: ['先横后竖、先撇后捺书写规则', '常见偏旁部首', '左右/上下/半包围结构'] },
                questions: [
                  { id: 'q4', type: 'Radio', q: '"口" 字共有几画？', options: ['2画', '3画', '4画'], answer: ['3画'], score: 5 },
                ],
              },
            ],
          },
          {
            id: 'c1s2', name: '字词辨析', sort: 2,
            content: {
              objective: '区分易混字形近字',
              overview: '通过偏旁与字义对比，掌握形近字辨析方法。',
              points: ['形近字概念', '偏旁部首辨义', '组词对比记忆'],
            },
            practice: { questionCount: 8, difficulty: '基础', types: ['Radio', 'Judge'] },
            kps: [
              {
                id: 'c1s2k1', name: '形近字辨析', sort: 1,
                content: { points: ['日与目 / 人入八等易混字', '借助偏旁区别字义', '组词法巩固'] },
                questions: [
                  { id: 'q5', type: 'Radio', q: '下面哪组是形近字？', options: ['日、目', '口、田', '山、水'], answer: ['日、目'], score: 5 },
                ],
              },
            ],
          },
        ],
      },
      {
        id: 'c2', name: '古诗文诵读', icon: '📜', sort: 2,
        sections: [
          {
            id: 'c2s1', name: '唐诗赏读', sort: 1,
            content: { objective: '背诵理解三首经典唐诗', overview: '精读《静夜思》《春晓》《咏鹅》。', points: ['作者与朝代', '诗句大意', '名句赏析'] },
            practice: { questionCount: 10, difficulty: '进阶', types: ['Radio', 'FillBlank'] },
            kps: [
              {
                id: 'c2s1k1', name: '唐诗三首', sort: 1,
                content: { points: ['《静夜思》李白：床前明月光', '《春晓》孟浩然：春眠不觉晓', '《咏鹅》骆宾王：曲项向天歌'] },
                questions: [
                  { id: 'q6', type: 'Radio', q: '《静夜思》的作者是？', options: ['李白', '杜甫', '白居易'], answer: ['李白'], score: 5 },
                ],
              },
            ],
          },
          {
            id: 'c2s2', name: '名句积累', sort: 2,
            content: { objective: '积累经典名句', overview: '背诵常用名句并理解含义。', points: ['举头望明月，低头思故乡', '谁知盘中餐，粒粒皆辛苦'] },
            practice: { questionCount: 8, difficulty: '基础', types: ['FillBlank'] },
            kps: [
              {
                id: 'c2s2k1', name: '名句积累', sort: 1,
                content: { points: ['名句与出处对应', '名句含义理解'] },
                questions: [
                  { id: 'q7', type: 'FillBlank', q: '"举头望明月" 的下一句是____', options: [], answer: ['低头思故乡'], score: 5 },
                ],
              },
            ],
          },
        ],
      },
    ],
  },
  {
    id: 'math', name: '数学', icon: '🧮', theme: 'blue',
    chapters: [
      {
        id: 'm1', name: '100以内加减法', icon: '🧮', sort: 1,
        sections: [
          {
            id: 'm1s1', name: '加法小站', sort: 1,
            content: {
              objective: '掌握两位数进位加法',
              overview: '学习个位相加满十进一的规则与口算技巧。',
              points: ['进位加法竖式书写', '凑十法口算', '相同数位对齐'],
            },
            practice: { questionCount: 10, difficulty: '基础', types: ['Radio', 'FillBlank'] },
            kps: [
              {
                id: 'm1s1k1', name: '进位加法', sort: 1,
                content: { points: ['个位相加满十向十位进 1', '竖式书写规范', '凑十法快速口算'] },
                questions: [
                  { id: 'q8', type: 'Radio', q: '38 + 25 = ?', options: ['53', '63', '73'], answer: ['63'], score: 5 },
                  { id: 'q9', type: 'FillBlank', q: '47 + 36 = ____', options: [], answer: ['83'], score: 5 },
                ],
              },
              {
                id: 'm1s1k2', name: '口算技巧', sort: 2,
                content: { points: ['凑十法', '破十法', '视算与听算训练'] },
                questions: [
                  { id: 'q10', type: 'Radio', q: '9 + 5 用凑十法结果是？', options: ['13', '14', '15'], answer: ['14'], score: 5 },
                ],
              },
            ],
          },
          {
            id: 'm1s2', name: '减法小站', sort: 2,
            content: {
              objective: '掌握两位数退位减法与混合运算',
              overview: '学习个位不够减向十位借一的规则。',
              points: ['退位减法竖式', '破十法口算', '加减混合运算顺序'],
            },
            practice: { questionCount: 10, difficulty: '进阶', types: ['Radio', 'Judge'] },
            kps: [
              {
                id: 'm1s2k1', name: '退位减法', sort: 1,
                content: { points: ['个位不够减向十位借 1 当 10', '借位标记写法', '破十法口算'] },
                questions: [
                  { id: 'q11', type: 'Radio', q: '62 - 18 = ?', options: ['44', '54', '64'], answer: ['44'], score: 5 },
                ],
              },
              {
                id: 'm1s2k2', name: '加减混合运算', sort: 2,
                content: { points: ['从左到右依次计算', '有括号先算括号内', '两步式混合运算'] },
                questions: [
                  { id: 'q12', type: 'Radio', q: '35 + 27 - 19 = ?', options: ['33', '43', '53'], answer: ['43'], score: 5 },
                ],
              },
            ],
          },
        ],
      },
      {
        id: 'm2', name: '图形的认识', icon: '📐', sort: 2,
        sections: [
          {
            id: 'm2s1', name: '平面图形', sort: 1,
            content: { objective: '认识常见平面图形', overview: '三角形、长方形、正方形、圆的特征。', points: ['边与角的数量', '图形分类'] },
            practice: { questionCount: 8, difficulty: '基础', types: ['Radio'] },
            kps: [
              {
                id: 'm2s1k1', name: '平面图形', sort: 1,
                content: { points: ['三角形 3 条边 3 个角', '长方形对边相等', '圆由曲线围成'] },
                questions: [
                  { id: 'q13', type: 'Radio', q: '长方形有几条边？', options: ['3条', '4条', '5条'], answer: ['4条'], score: 5 },
                ],
              },
            ],
          },
          {
            id: 'm2s2', name: '立体图形', sort: 2,
            content: { objective: '认识常见立体图形', overview: '长方体、正方体、圆柱、球的特征。', points: ['面与棱', '图形与实物对应'] },
            practice: { questionCount: 8, difficulty: '基础', types: ['Radio', 'Judge'] },
            kps: [
              {
                id: 'm2s2k1', name: '立体图形', sort: 1,
                content: { points: ['长方体 6 个面', '正方体 6 个面都是正方形', '球可任意滚动'] },
                questions: [
                  { id: 'q14', type: 'Radio', q: '下面哪个是立体图形？', options: ['正方形', '圆柱', '三角形'], answer: ['圆柱'], score: 5 },
                ],
              },
            ],
          },
        ],
      },
    ],
  },
  {
    id: 'english', name: '英语', icon: '🔤', theme: 'green',
    chapters: [
      {
        id: 'e1', name: '字母与拼读', icon: '🔠', sort: 1,
        sections: [
          {
            id: 'e1s1', name: '字母乐园', sort: 1,
            content: { objective: '掌握 26 个字母', overview: '字母名称音、大小写与书写占格。', points: ['26 个字母顺序', '大小写对应', '5 个元音字母'] },
            practice: { questionCount: 10, difficulty: '基础', types: ['Radio', 'FillBlank'] },
            kps: [
              {
                id: 'e1s1k1', name: '26个字母', sort: 1,
                content: { points: ['A-Z 字母顺序', '元音字母 A E I O U', '书写占格规范'] },
                questions: [
                  { id: 'q15', type: 'Radio', q: '字母表中第 26 个字母是？', options: ['X', 'Y', 'Z'], answer: ['Z'], score: 5 },
                ],
              },
            ],
          },
          {
            id: 'e1s2', name: '拼读魔法', sort: 2,
            content: { objective: '掌握自然拼读', overview: '元音字母在单词中的短音规律。', points: ['a→/æ/ e→/e/ i→/ɪ/', '辅音发音', '拼读练习'] },
            practice: { questionCount: 8, difficulty: '进阶', types: ['Radio'] },
            kps: [
              {
                id: 'e1s2k1', name: '自然拼读', sort: 1,
                content: { points: ['短音发音规律', 'c-a-t 拼读', '单词拼读训练'] },
                questions: [
                  { id: 'q16', type: 'Radio', q: '"cat" 中元音 "a" 发什么音？', options: ['/æ/', '/eɪ/'], answer: ['/æ/'], score: 5 },
                ],
              },
            ],
          },
        ],
      },
      {
        id: 'e2', name: '基础单词', icon: '🗣️', sort: 2,
        sections: [
          {
            id: 'e2s1', name: '校园词汇', sort: 1,
            content: { objective: '掌握校园常用词汇', overview: '教室、文具、颜色等词汇。', points: ['book/pen/ruler', 'red/blue/green'] },
            practice: { questionCount: 10, difficulty: '基础', types: ['Radio', 'FillBlank'] },
            kps: [
              {
                id: 'e2s1k1', name: '校园词汇', sort: 1,
                content: { points: ['学习用品词汇', '颜色词汇', '看图说词'] },
                questions: [
                  { id: 'q17', type: 'Radio', q: '"book" 的意思是？', options: ['书', '笔', '尺子'], answer: ['书'], score: 5 },
                ],
              },
            ],
          },
        ],
      },
    ],
  },
];

const useKnowledgeStore = create((set, get) => ({
  subjects: seedSubjects,

  // ---- 查找辅助 ----
  getSubject: (subjectId) => get().subjects.find((s) => s.id === subjectId),
  getSection: (subjectId, chapterId, sectionId) => {
    const sub = get().getSubject(subjectId);
    const ch = sub?.chapters.find((c) => c.id === chapterId);
    return ch?.sections.find((s) => s.id === sectionId);
  },

  // ---- 通用不可变更新：按 subjectId → chapterId → sectionId 定位修改 ----
  // fn(subject) 返回新的 subject
  _updateSubject: (subjectId, fn) => set((s) => ({
    subjects: s.subjects.map((sub) => (sub.id === subjectId ? fn(sub) : sub)),
  })),

  // ==================== 章节管理 ====================
  addChapter: (subjectId, chapter) => {
    const chId = `c${Date.now().toString(36)}`;
    get()._updateSubject(subjectId, (sub) => ({
      ...sub,
      chapters: [...sub.chapters, { ...chapter, id: chId, sections: chapter.sections || [] }],
    }));
    return chId;
  },
  updateChapter: (subjectId, chapterId, patch) => {
    get()._updateSubject(subjectId, (sub) => ({
      ...sub,
      chapters: sub.chapters.map((c) => (c.id === chapterId ? { ...c, ...patch, id: chapterId } : c)),
    }));
  },
  deleteChapter: (subjectId, chapterId) => {
    get()._updateSubject(subjectId, (sub) => ({
      ...sub,
      chapters: sub.chapters.filter((c) => c.id !== chapterId),
    }));
  },

  // ==================== 小节管理 ====================
  addSection: (subjectId, chapterId, section) => {
    const secId = `s${Date.now().toString(36)}`;
    get()._updateSubject(subjectId, (sub) => ({
      ...sub,
      chapters: sub.chapters.map((c) => (c.id === chapterId
        ? { ...c, sections: [...c.sections, { ...section, id: secId, kps: section.kps || [] }] }
        : c)),
    }));
    return secId;
  },
  updateSection: (subjectId, chapterId, sectionId, patch) => {
    get()._updateSubject(subjectId, (sub) => ({
      ...sub,
      chapters: sub.chapters.map((c) => (c.id === chapterId
        ? { ...c, sections: c.sections.map((s) => (s.id === sectionId ? { ...s, ...patch, id: sectionId } : s)) }
        : c)),
    }));
  },
  // 小节内容设置
  updateSectionContent: (subjectId, chapterId, sectionId, content) => {
    get().updateSection(subjectId, chapterId, sectionId, { content });
  },
  // 小节练习设置
  updateSectionPractice: (subjectId, chapterId, sectionId, practice) => {
    get().updateSection(subjectId, chapterId, sectionId, { practice });
  },
  deleteSection: (subjectId, chapterId, sectionId) => {
    get()._updateSubject(subjectId, (sub) => ({
      ...sub,
      chapters: sub.chapters.map((c) => (c.id === chapterId
        ? { ...c, sections: c.sections.filter((s) => s.id !== sectionId) }
        : c)),
    }));
  },

  // ==================== 知识点管理 ====================
  addKp: (subjectId, chapterId, sectionId, kp) => {
    const kpId = `k${Date.now().toString(36)}`;
    get()._updateSubject(subjectId, (sub) => ({
      ...sub,
      chapters: sub.chapters.map((c) => (c.id === chapterId
        ? {
            ...c,
            sections: c.sections.map((s) => (s.id === sectionId
              ? { ...s, kps: [...s.kps, { ...kp, id: kpId, questions: kp.questions || [], content: kp.content || { points: [] } }] }
              : s)),
          }
        : c)),
    }));
    return kpId;
  },
  updateKp: (subjectId, chapterId, sectionId, kpId, patch) => {
    get()._updateSubject(subjectId, (sub) => ({
      ...sub,
      chapters: sub.chapters.map((c) => (c.id === chapterId
        ? {
            ...c,
            sections: c.sections.map((s) => (s.id === sectionId
              ? { ...s, kps: s.kps.map((k) => (k.id === kpId ? { ...k, ...patch, id: kpId } : k)) }
              : s)),
          }
        : c)),
    }));
  },
  // 知识点内容设置
  updateKpContent: (subjectId, chapterId, sectionId, kpId, content) => {
    get().updateKp(subjectId, chapterId, sectionId, kpId, { content });
  },
  // 测试题型设置（整体替换题目数组）
  updateKpQuestions: (subjectId, chapterId, sectionId, kpId, questions) => {
    get().updateKp(subjectId, chapterId, sectionId, kpId, { questions });
  },
  deleteKp: (subjectId, chapterId, sectionId, kpId) => {
    get()._updateSubject(subjectId, (sub) => ({
      ...sub,
      chapters: sub.chapters.map((c) => (c.id === chapterId
        ? {
            ...c,
            sections: c.sections.map((s) => (s.id === sectionId
              ? { ...s, kps: s.kps.filter((k) => k.id !== kpId) }
              : s)),
          }
        : c)),
    }));
  },
}));

export default useKnowledgeStore;
