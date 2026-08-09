/**
 * useStudentStore.js - 学生端全局状态（纯前端原型，数据均为本地 mock）
 *
 * 管理: 当前学科 / 教材版本 / 纯净学习模式开关 + 各学科教学数据
 * 数值体系（学海智习系统 V2.0 核心底层）:
 *   - 学海积分: 全学科永久累计，仅用于头衔晋升与证书
 *   - 学习币: 分学科产出，单科单学期上限 3000，学期清零，多科合并兑换
 *
 * 被谁引用: StudentLayout（学科 Tab / 版本下拉 / 纯净模式）、
 *           学生端各页面（首页/研习/知识点/档案/商城）
 * 依赖: zustand（全局状态）
 */

import { create } from 'zustand';

// ---- 五级头衔 + 证书体系（学海积分自动晋升，无降级） ----
export const TITLES = [
  { key: 'beginner', name: '学海初探者', need: 0,   cert: '学海初探·启航荣誉证书',   emoji: '🌱' },
  { key: 'diligent', name: '学海勤学者', need: 300, cert: '学海勤学者·阶段荣誉证书', emoji: '🐬' },
  { key: 'deep',     name: '学海深耕者', need: 1600, cert: '学海深耕者·阶段荣誉证书', emoji: '🪸' },
  { key: 'thinker',  name: '学海善思者', need: 5500, cert: '学海善思者·阶段荣誉证书', emoji: '🌟' },
  { key: 'pioneer',  name: '学海领航者', need: 9000, cert: '学海领航者·最高荣誉证书', emoji: '👑' },
];

// ---- 知识掌握度评级 ----
export const LEVELS = [
  { label: '精通',   min: 85, color: '#34c759' },
  { label: '熟练',   min: 70, color: '#2196f3' },
  { label: '夯实',   min: 55, color: '#ff9800' },
  { label: '待巩固', min: 40, color: '#ff7043' },
  { label: '待攻克', min: 0,  color: '#e53935' },
];

// 根据掌握度百分比返回评级
export const masteryLevel = (pct) => {
  const lv = LEVELS.find((l) => pct >= l.min);
  return lv || LEVELS[LEVELS.length - 1];
};

// ---- 各学科 mock 数据（章节 → 知识点） ----
export const SUBJECTS = [
  {
    key: 'chinese',
    name: '语文',
    icon: '📚',
    theme: 'orange',
    coins: 1200,
    version: '人教版',
    versions: ['人教版', '苏教版'],
    chapters: [
      {
        id: 'c1', name: '识字与写字', icon: '🖋️', progress: 82,
        kps: [
          { id: 'c1k1', name: '拼音王国',       mastery: 92, desc: '声母、韵母与整体认读音节的正确读写' },
          { id: 'c1k2', name: '汉字笔顺',       mastery: 78, desc: '常用汉字书写笔顺规范与偏旁部首' },
          { id: 'c1k3', name: '形近字辨析',     mastery: 55, desc: '易混形近字的对比区分与组词' },
        ],
      },
      {
        id: 'c2', name: '古诗文诵读', icon: '📜', progress: 60,
        kps: [
          { id: 'c2k1', name: '唐诗三首',       mastery: 70, desc: '《静夜思》《春晓》《咏鹅》诵读理解' },
          { id: 'c2k2', name: '名句积累',       mastery: 48, desc: '经典名句背诵与默写' },
        ],
      },
      {
        id: 'c3', name: '阅读理解', icon: '🔎', progress: 35,
        kps: [
          { id: 'c3k1', name: '段落大意',       mastery: 42, desc: '概括自然段与全文主要内容的技巧' },
          { id: 'c3k2', name: '修辞手法',       mastery: 30, desc: '比喻、拟人、排比等修辞识别' },
        ],
      },
    ],
  },
  {
    key: 'math',
    name: '数学',
    icon: '🧮',
    theme: 'blue',
    coins: 800,
    version: '人教版',
    versions: ['人教版', '北师大版'],
    chapters: [
      {
        id: 'm1', name: '100以内加减法', icon: '🧮', progress: 75,
        kps: [
          { id: 'm1k1', name: '进位加法',       mastery: 88, desc: '两位数加一位数/两位数的进位加法' },
          { id: 'm1k2', name: '退位减法',       mastery: 66, desc: '两位数减一位数/两位数的退位减法' },
          { id: 'm1k3', name: '加减混合运算',   mastery: 45, desc: '两步式混合运算与运算顺序' },
        ],
      },
      {
        id: 'm2', name: '图形的认识', icon: '📐', progress: 50,
        kps: [
          { id: 'm2k1', name: '平面图形',       mastery: 72, desc: '三角形、长方形、正方形、圆的认识' },
          { id: 'm2k2', name: '立体图形',       mastery: 38, desc: '长方体、正方体、圆柱、球的认识' },
        ],
      },
      {
        id: 'm3', name: '应用题思维', icon: '🧠', progress: 28,
        kps: [
          { id: 'm3k1', name: '加法应用题',     mastery: 50, desc: '求总数、求比多比少的加法问题' },
          { id: 'm3k2', name: '减法应用题',     mastery: 25, desc: '求剩余、求差的减法问题' },
        ],
      },
    ],
  },
  {
    key: 'english',
    name: '英语',
    icon: '🔤',
    theme: 'green',
    coins: 500,
    version: '人教版',
    versions: ['人教版', '外研版'],
    chapters: [
      {
        id: 'e1', name: '字母与拼读', icon: '🔠', progress: 68,
        kps: [
          { id: 'e1k1', name: '26个字母',       mastery: 90, desc: '字母名称音、大小写与书写占格' },
          { id: 'e1k2', name: '自然拼读',       mastery: 52, desc: '元音字母在单词中的短音规律' },
        ],
      },
      {
        id: 'e2', name: '基础单词', icon: '🗣️', progress: 45,
        kps: [
          { id: 'e2k1', name: '校园词汇',       mastery: 60, desc: '教室、文具、颜色等常用词汇' },
          { id: 'e2k2', name: '动物词汇',       mastery: 35, desc: '常见动物单词的听说读写' },
        ],
      },
      {
        id: 'e3', name: '日常句型', icon: '💬', progress: 20,
        kps: [
          { id: 'e3k1', name: '问候句型',       mastery: 40, desc: 'Hello/How are you/Goodbye 等问答' },
          { id: 'e3k2', name: '介绍句型',       mastery: 22, desc: "This is... / I am... 自我介绍句型" },
        ],
      },
    ],
  },
];

// ---- 荣誉商城商品（学习币兑换） ----
export const GOODS = [
  { id: 'g1', name: '海洋橡皮',  emoji: '🧽', price: 80,  desc: '可爱海豚造型橡皮擦' },
  { id: 'g2', name: '荧光书签',  emoji: '🔖', price: 150, desc: '六色荧光材质书签套装' },
  { id: 'g3', name: '铅笔套装',  emoji: '✏️', price: 200, desc: '六支装 HB 卡通铅笔' },
  { id: 'g4', name: '小鲸笔记本', emoji: '📒', price: 500, desc: '小鲸主题 A5 线圈笔记本' },
  { id: 'g5', name: '海星文具盒', emoji: '🧰', price: 800, desc: '三层大容量海星文具盒' },
  { id: 'g6', name: '台灯护眼',  emoji: '💡', price: 1200, desc: '护眼阅读小台灯' },
];

// ---- 今日数据总览 + 今日待办（每日 0 点重置） ----
export const TODAY = {
  minutes: 45,     // 今日时长（分钟）
  kps: 3,          // 今日完成知识点
  points: 28,      // 今日获得学海积分
  coins: 45,       // 今日获得学习币
};

export const DAILY_TASKS = [
  { key: 't1', label: '完成 2 个知识点练习', reward: '币+8 积分+5', done: true },
  { key: 't2', label: '订正错题 ≥ 3 道',     reward: '币+6 积分+3', done: true },
  { key: 't3', label: '有效学习 30 分钟',     reward: '币+7 积分+4', done: false },
];

// ---- 知识点学习奖励（基础首学奖励） ----
export const REWARDS = {
  preview:  { coins: 5,  points: 3,  label: '知识点预习' },
  practice: { coins: 12, points: 6,  label: '专项练习' },
  trial:    { coins: 20, points: 10, label: '试炼检测' },
  wrong:    { coins: 8,  points: 4,  label: '错题订正' },
};

// ---- 知识点 mock 题目（练习/试炼共用，大圆角卡片选项） ----
// 按知识点 id 提供题目池；answer 为正确选项 key 数组
const QUESTION_BANK = {
  c1k1: [
    { q: '下列哪个是单韵母？', options: [{ k: 'a', t: 'a' }, { k: 'b', t: 'b' }, { k: 'c', t: 'ch' }], answer: ['a'], type: 'radio' },
    { q: '"mā" 的声调是？', options: [{ k: 'a', t: '第一声' }, { k: 'b', t: '第二声' }, { k: 'c', t: '第三声' }], answer: ['a'], type: 'radio' },
  ],
  c1k2: [
    { q: '"口" 字共有几画？', options: [{ k: 'a', t: '2画' }, { k: 'b', t: '3画' }, { k: 'c', t: '4画' }], answer: ['b'], type: 'radio' },
  ],
  c1k3: [
    { q: '下面哪个字的笔画数相同？', options: [{ k: 'a', t: '日、目' }, { k: 'b', t: '口、田' }], answer: ['a'], type: 'radio' },
  ],
  c2k1: [
    { q: '《静夜思》的作者是？', options: [{ k: 'a', t: '李白' }, { k: 'b', t: '杜甫' }, { k: 'c', t: '白居易' }], answer: ['a'], type: 'radio' },
    { q: '"举头望明月" 下一句是？', options: [{ k: 'a', t: '低头思故乡' }, { k: 'b', t: '月是故乡明' }], answer: ['a'], type: 'radio' },
  ],
  c2k2: [
    { q: '"春眠不觉晓" 出自哪首诗？', options: [{ k: 'a', t: '《春晓》' }, { k: 'b', t: '《咏鹅》' }], answer: ['a'], type: 'radio' },
  ],
  c3k1: [
    { q: '概括段落大意应抓住什么？', options: [{ k: 'a', t: '中心句' }, { k: 'b', t: '标点符号' }], answer: ['a'], type: 'radio' },
  ],
  c3k2: [
    { q: '"弯弯的月亮像小船" 用了什么修辞？', options: [{ k: 'a', t: '比喻' }, { k: 'b', t: '夸张' }, { k: 'c', t: '反问' }], answer: ['a'], type: 'radio' },
  ],
  m1k1: [
    { q: '38 + 25 = ?', options: [{ k: 'a', t: '53' }, { k: 'b', t: '63' }, { k: 'c', t: '73' }], answer: ['b'], type: 'radio' },
    { q: '47 + 36 = ?', options: [{ k: 'a', t: '73' }, { k: 'b', t: '83' }, { k: 'c', t: '93' }], answer: ['b'], type: 'radio' },
  ],
  m1k2: [
    { q: '62 - 18 = ?', options: [{ k: 'a', t: '44' }, { k: 'b', t: '54' }, { k: 'c', t: '64' }], answer: ['a'], type: 'radio' },
    { q: '83 - 47 = ?', options: [{ k: 'a', t: '26' }, { k: 'b', t: '36' }, { k: 'c', t: '46' }], answer: ['b'], type: 'radio' },
  ],
  m1k3: [
    { q: '35 + 27 - 19 = ?', options: [{ k: 'a', t: '33' }, { k: 'b', t: '43' }, { k: 'c', t: '53' }], answer: ['b'], type: 'radio' },
    { q: '下面哪道算式先算加法？', options: [{ k: 'a', t: '8 + (12 - 5)' }, { k: 'b', t: '(8 + 12) - 5' }], answer: ['b'], type: 'radio' },
  ],
  m2k1: [
    { q: '长方形有几条边？', options: [{ k: 'a', t: '3条' }, { k: 'b', t: '4条' }, { k: 'c', t: '5条' }], answer: ['b'], type: 'radio' },
  ],
  m2k2: [
    { q: '下面哪个是立体图形？', options: [{ k: 'a', t: '正方形' }, { k: 'b', t: '圆柱' }], answer: ['b'], type: 'radio' },
  ],
  m3k1: [
    { q: '小明有 12 个苹果，小红有 9 个，一共有几个？', options: [{ k: 'a', t: '21个' }, { k: 'b', t: '3个' }, { k: 'c', t: '19个' }], answer: ['a'], type: 'radio' },
  ],
  m3k2: [
    { q: '书架上原有 15 本书，拿走 6 本，还剩几本？', options: [{ k: 'a', t: '9本' }, { k: 'b', t: '21本' }], answer: ['a'], type: 'radio' },
  ],
  e1k1: [
    { q: '字母 "A" 的小写是？', options: [{ k: 'a', t: 'a' }, { k: 'b', t: 'e' }, { k: 'c', t: 'i' }], answer: ['a'], type: 'radio' },
    { q: '字母表中第 26 个字母是？', options: [{ k: 'a', t: 'X' }, { k: 'b', t: 'Y' }, { k: 'c', t: 'Z' }], answer: ['c'], type: 'radio' },
  ],
  e1k2: [
    { q: '"cat" 中元音 "a" 发什么音？', options: [{ k: 'a', t: '/æ/' }, { k: 'b', t: '/eɪ/' }], answer: ['a'], type: 'radio' },
  ],
  e2k1: [
    { q: '"book" 的意思是？', options: [{ k: 'a', t: '书' }, { k: 'b', t: '笔' }, { k: 'c', t: '尺子' }], answer: ['a'], type: 'radio' },
  ],
  e2k2: [
    { q: '"dog" 的意思是？', options: [{ k: 'a', t: '猫' }, { k: 'b', t: '狗' }], answer: ['b'], type: 'radio' },
  ],
  e3k1: [
    { q: '见面打招呼说？', options: [{ k: 'a', t: 'Hello!' }, { k: 'b', t: 'Goodbye!' }], answer: ['a'], type: 'radio' },
  ],
  e3k2: [
    { q: '向别人介绍自己说？', options: [{ k: 'a', t: 'I am Lily.' }, { k: 'b', t: 'Good morning.' }], answer: ['a'], type: 'radio' },
  ],
};

// 兜底题目池（知识点无专属题时使用）
const FALLBACK_QUESTIONS = [
  { q: '下列选项中，哪个是正确的？', options: [{ k: 'a', t: '选项 A' }, { k: 'b', t: '选项 B' }], answer: ['a'], type: 'radio' },
];

// 获取知识点题目（练习 3 题 / 试炼 2 题可复用）
export const getQuestions = (kpId) => QUESTION_BANK[kpId] || FALLBACK_QUESTIONS;

// ---- 学生个人档案（mock） ----
export const PROFILE = {
  name: '小海星',
  emoji: '🐬',
  title: '学海勤学者',
  points: 320,      // 总学海积分（终身）
  certCount: 2,     // 已解锁证书数
  certTotal: 5,
  subjects: 3,      // 已绑定学科数
  versions: 1,      // 每科教材版本
};

const useStudentStore = create((set, get) => ({
  // ---- 基础状态 ----
  activeSubject: localStorage.getItem('sh-active-subject') || 'math', // 当前学科（记忆上次选择）
  version: localStorage.getItem('sh-version') || '人教版',            // 当前教材版本（记忆上次选择）
  pureMode: localStorage.getItem('sh-pure-mode') === '1',             // 纯净学习模式（迎检专用）

  // ---- actions ----
  setSubject: (key) => {
    const subject = SUBJECTS.find((s) => s.key === key);
    const version = subject ? subject.version : get().version;
    localStorage.setItem('sh-active-subject', key);
    localStorage.setItem('sh-version', version);
    set({ activeSubject: key, version });
  },
  setVersion: (v) => {
    localStorage.setItem('sh-version', v);
    set({ version: v });
  },
  togglePureMode: () => {
    set((s) => {
      localStorage.setItem('sh-pure-mode', s.pureMode ? '0' : '1');
      return { pureMode: !s.pureMode };
    });
  },

  // ---- 派生数据 helper ----
  getSubject: () => SUBJECTS.find((s) => s.key === get().activeSubject) || SUBJECTS[1],
  getChapters: () => {
    const sub = SUBJECTS.find((s) => s.key === get().activeSubject) || SUBJECTS[1];
    return sub.chapters;
  },
}));

export default useStudentStore;
