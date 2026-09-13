/**
 * KnowledgePage.jsx - 知识点详情页（学海智习系统 V2.0）
 * 模式（?tab=）: preview 预习 / practice 练习 / trial 试炼 / wrong 错题
 *
 * 视觉规范: 大面积留白低干扰；题目选项全部大圆角卡片（摒弃单选框）；
 *           提交后逐题标记对错；顶部轻柔奖励提示 1.5s 自动消失（无弹窗）
 *
 * 奖励体系（内容型奖励每学期每内容只发一次——前端 mock 演示）:
 *   预习 币+5 积分+3 / 练习 币+20 积分+6 / 试炼 币+30 积分+10 / 错题订正 币+5 积分+4
 *   消灭知识点（掌握度≥85）币+60；试炼正确率≥90% 额外 币+15 积分+8
 *
 * 被谁引用: App.jsx（/student/knowledge/:kpId）、研习页右栏四大按钮
 * 依赖: react-router-dom(useParams/useSearchParams/useNavigate)、useStudentStore、./KnowledgePage.css
 */

import { useEffect, useState } from 'react';
import { Input, Button, Modal, Select, Image, Progress, message } from 'antd';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { getStudyPoints, getStudyQuestions, getSectionPracticeConfig, uploadActivity, completePreview, getKnowledgeDetail, wrongRedo } from '../../api/student';
import { submitPractice, saveWrongReason } from '../../api/practice';
import { extractCorrectAnswers } from '../../utils/practiceHelpers';
import WrongBookPanel from '../../components/student/WrongBookPanel';
import RichContent from '../../components/common/RichContent';
import IconTile from '../../components/common/IconTile';
import './KnowledgePage.css';

// 专项练习可选题型（与后端 questionTypes 对齐）
const DRILL_TYPE_OPTIONS = [
  { value: 'Judge', label: '判断题' },
  { value: 'Radio', label: '单选题' },
  { value: 'FillBlank', label: '填空题' },
  { value: 'Checkbox', label: '多选题' },
  { value: 'MultipleBlank', label: '多空填空' },
];

// 题型标签（题面卡片标题，参考 image-2 的题型标题布局）
const QUESTION_TYPE_LABELS = {
  Judge: '判断题',
  Radio: '单选题',
  Checkbox: '多选题',
  FillBlank: '填空题',
  MultipleBlank: '多空填空',
  Text: '简答题',
};

// 各 tab 头部标题图标（3D 黏土底座：emoji + 色调 + 文案）
const TAB_META = {
  preview: { icon: '📖', tone: 'blue', title: '知识点预习/复习' },
  preview_practice: { icon: '📝', tone: 'blue', title: '例题检测' },
  practice: { icon: '✏️', tone: 'orange', title: '专项练习湾' },
  trial: { icon: '🎯', tone: 'green', title: '小节通关' },
  wrong: { icon: '📕', tone: 'pink', title: '知识点错题本' },
  redo: { icon: '✏️', tone: 'orange', title: '错题重做订正' },
};

// 填空比较归一化（与后端 AnswerJudgeUtil / utils/practiceHelpers 对齐）:
// 全角空格/零宽字符/全角字母数字符号（含 ＜＞＝）转半角、连续空白折叠、去首尾
function normBlank(s) {
  return String(s == null ? '' : s)
    .replace(/[\u00A0\u1680\u2000-\u200A\u202F\u205F\u3000\uFEFF]/g, ' ')
    .replace(/[\u200B\u200C\u200D]/g, '')
    .replace(/[\uFF01-\uFF5E]/g, (ch) => String.fromCharCode(ch.charCodeAt(0) - 0xFEE0))
    .replace(/\s+/g, ' ')
    .trim();
}

function blankEq(a, b) {
  const na = normBlank(a);
  const nb = normBlank(b);
  if (na === nb) return true;
  // 纯 ASCII 字母串（如选项字母 A/B/C）忽略大小写
  if (/^[A-Za-z]+$/.test(na) && /^[A-Za-z]+$/.test(nb)) return na.toLowerCase() === nb.toLowerCase();
  return false;
}

// 讲解要点富文本高亮：步骤标记 ①②③ / 引号内术语 “xx” / 数字（含千分位）加粗
function renderRichText(text) {
  const s = String(text == null ? '' : text);
  const re = /(“[^”]{1,24}”|「[^」]{1,24}」|\d[\d,.·]*)|(①|②|③|④|⑤|⑥|⑦|⑧|⑨)/g;
  const nodes = [];
  let last = 0;
  let m;
  let k = 0;
  while ((m = re.exec(s)) !== null) {
    if (m.index > last) nodes.push(s.slice(last, m.index));
    if (m[2]) nodes.push(<em key={k++} className="kp-mark-step">{m[2]}</em>);
    else nodes.push(<strong key={k++} className="kp-mark-key">{m[1]}</strong>);
    last = m.index + m[0].length;
  }
  if (last < s.length) nodes.push(s.slice(last));
  return nodes.length ? nodes : s;
}

// 四种模式 tab 配置
// 预习讲解内容（按知识点生成 mock 要点）
export default function KnowledgePage() {
  const [searchParams] = useSearchParams();
  const tab = searchParams.get('tab') || 'preview';
  const sectionId = searchParams.get('sectionId'); // 小节练习入口
  const repoId = searchParams.get('repoId');         // 练习（题库）任务/直接练习入口
  const kpIdParam = searchParams.get('kpId');        // 知识点任务入口
  const questionIdParam = searchParams.get('questionId'); // 错题重做入口（单题）
  const typesParam = searchParams.get('types');      // 题型过滤（逗号分隔，消灭错题用）
  const countParam = searchParams.get('count');      // 出题数量（消灭易错知识点/错题用）
  const realMode = !!(sectionId || repoId || kpIdParam || questionIdParam);
  const navigate = useNavigate();

  // ============================================================
  // 真实模式（后台配置内容：知识点/题目来自 /api/student/study/*）
  // ============================================================
  const [realPoints, setRealPoints] = useState(null);      // 知识点（预习）
  const [realQuestions, setRealQuestions] = useState(null); // 题目（练习/试炼）
  const [realAnswers, setRealAnswers] = useState({});       // 作答 {questionId: {type, optionId/optionIds}}
  const [realResult, setRealResult] = useState(null);       // 判分结果
  const [redoResult, setRedoResult] = useState(null);       // 错题重做订正结果
  const [realSubmitting, setRealSubmitting] = useState(false);
  const [currentQ, setCurrentQ] = useState(0);          // 逐题模式当前题索引
  const [judgeState, setJudgeState] = useState({}); // 每题是否已提交判定 {qid: true}
  const [activeKpIdx, setActiveKpIdx] = useState(0);   // 预习：当前知识点索引
  const [readKpIds, setReadKpIds] = useState(() => new Set()); // 预习：已浏览过的知识点
  const [wrongOpen, setWrongOpen] = useState(false);        // 查看错题弹窗
  const [wrongReasons, setWrongReasons] = useState({});     // 各错题归因 {questionId: reason}
  const [wrongList, setWrongList] = useState([]);          // 当前错题列表（查看错题弹窗）
  const [previewCompleting, setPreviewCompleting] = useState(false); // 预习完成提交中
  const [previewDone, setPreviewDone] = useState(false);             // 本次会话预习已完成（进度已保留）
  const [kpDetail, setKpDetail] = useState(null);                    // 知识点真实掌握度/评级/薄弱

  // 专项练习（按知识点为单位逐个过题）与小节通关（按后台配置组卷）
  const [drillStarted, setDrillStarted] = useState(false);           // 专项练习是否已开始答题
  const [trialStarted, setTrialStarted] = useState(false);          // 小节通关是否已开始答题
  const [practiceConfig, setPracticeConfig] = useState(null);       // 小节练习配置（题量/难度/题型/通过线）

  // 预习（含例题检测）为同一学习闭环
  const isPreviewFlow = tab === 'preview' || tab === 'preview_practice';
  const sectionNameParam = searchParams.get('name');
  const previewTitle = sectionNameParam
    || (realPoints?.length === 1 ? realPoints[0].name : '知识点预习');
  // 当前 tab 的头部图标文案（3D 黏土图标底座）
  const tabMeta = TAB_META[tab] || TAB_META.preview;

  // 预习讲解 ↔ 例题检测（保留小节/知识点上下文与标题）
  const goKnowledgeTab = (targetTab) => {
    const qs = new URLSearchParams();
    if (sectionId) qs.set('sectionId', sectionId);
    if (repoId) qs.set('repoId', repoId);
    if (kpIdParam) qs.set('kpId', kpIdParam);
    if (sectionNameParam) qs.set('name', sectionNameParam);
    qs.set('tab', targetTab);
    navigate(`/student/knowledge?${qs.toString()}`);
  };
  const goPreviewPractice = () => goKnowledgeTab('preview_practice');
  const goPreviewLecture = () => goKnowledgeTab('preview');

  // 统一取题：数组参数用逗号拼接（后端 @RequestParam List<String> 绑定）
  const fetchQuestions = (params) => {
    const query = { ...params };
    if (Array.isArray(query.knowledgePointIds)) query.knowledgePointIds = query.knowledgePointIds.join(',');
    if (Array.isArray(query.types)) query.types = query.types.join(',');
    setRealQuestions(null);
    return getStudyQuestions(query)
      .then((res) => {
        setRealQuestions(res?.data || []);
        setRealResult(null);
        setRealAnswers({});
        setJudgeState({});
        setCurrentQ(0);
      })
      .catch(() => setRealQuestions([]));
  };

  // 小节练习配置是否随机模式（兼容后端 mode 字段与派生的 random 布尔）
  const isRandomMode = (cfg) => String(cfg?.mode || '').toLowerCase() === 'random' || cfg?.random === true;

  // 开始专项练习：以知识点为单位，逐个知识点抽题（每个知识点至少 1 题，全部覆盖）
  const startDrill = () => {
    const kpIds = (realPoints || []).map((p) => p.id);
    if (kpIds.length === 0) {
      message.warning('该小节暂未配置知识点，无法进行专项练习');
      return;
    }
    setDrillStarted(true);
    fetchQuestions({
      exposeAnswer: true,
      knowledgePointIds: kpIds,
      perKp: 1,
      random: true,
      sectionId: sectionId || undefined,
      repoId: repoId || undefined,
      usage: 'practice',
    });
  };

  // 开始小节通关：按后台配置组卷（常规=整节全部题；随机=按配置题量/难度/题型）
  const startTrial = () => {
    const cfg = practiceConfig || {};
    const random = isRandomMode(cfg);
    setTrialStarted(true);
    fetchQuestions({
      exposeAnswer: true,
      sectionId: sectionId || undefined,
      repoId: repoId || undefined,
      knowledgePointId: kpIdParam || undefined,
      random,
      types: random ? (cfg.types || []) : [],
      difficulty: random ? (cfg.difficulty || undefined) : undefined,
      count: random && cfg.questionCount ? cfg.questionCount : undefined,
      usage: 'trial',
    });
  };

  // 浏览过的知识点打勾（当前卡自动标记）
  useEffect(() => {
    if (tab !== 'preview' || !realPoints?.length) return;
    const cur = realPoints[activeKpIdx];
    if (cur) setReadKpIds((prev) => (prev.has(cur.id) ? prev : new Set(prev).add(cur.id)));
  }, [tab, activeKpIdx, realPoints]);

  // 真实掌握度/薄弱：带知识点入口时按 kpId 拉取（失败保留展示，不影响答题）
  useEffect(() => {
    if (!realMode || !kpIdParam) {
      setKpDetail(null);
      return;
    }
    getKnowledgeDetail(kpIdParam)
      .then((res) => {
        const d = res?.data || null;
        setKpDetail(d);
        if (d?.previewed) setPreviewDone(true);
      })
      .catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [realMode, kpIdParam]);

  // 习题级上报：进入练习/试炼后上报「当前正在做的题」，随 currentQ 前进实时更新（供督学）
  useEffect(() => {
    if (!realMode || !realQuestions?.length) return;
    // 预习为整页浏览（无逐题游标），取首题做上下文；练习/试炼跟随当前题游标
    const q = tab === 'preview' ? realQuestions[0] : realQuestions[currentQ] || realQuestions[realQuestions.length - 1];
    if (!q) return;
    uploadActivity({
      page: `/student/knowledge?tab=${tab}`,
      sectionId: sectionId || undefined,
      questionId: q.id,
    }).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [realMode, tab, realQuestions, currentQ]);

  useEffect(() => {
    if (!realMode) return;
    if (tab === 'preview' || tab === 'preview_practice' || tab === 'practice') {
      // 知识点列表（预习导学 / 专项练习选题）
      if (sectionId) {
        getStudyPoints(sectionId).then((res) => setRealPoints(res?.data || [])).catch(() => setRealPoints([]));
      }
    }
    if (tab === 'preview' || tab === 'preview_practice') {
      // 预习讲解页与例题检测：题材来自「预习专用/通用」题库，题量/题型缺省由后端按小节预习配置补全
      const params = { exposeAnswer: true, usage: 'preview' };
      const explicitCount = Number(countParam);
      if (Number.isFinite(explicitCount) && explicitCount > 0) params.count = explicitCount;
      if (typesParam) params.types = typesParam.split(',');
      if (sectionId) params.sectionId = sectionId;
      if (repoId) params.repoId = repoId;
      if (kpIdParam) params.knowledgePointId = kpIdParam;
      getStudyQuestions(params)
        .then((res) => setRealQuestions(res?.data || []))
        .catch(() => setRealQuestions([]));
    } else if (tab === 'trial') {
      // 小节通关：进入先读取后台配置，学员点击「开始通关」后再组卷
      if (sectionId) {
        getSectionPracticeConfig(sectionId)
          .then((res) => setPracticeConfig(res?.data || null))
          .catch(() => setPracticeConfig(null));
      }
    } else if (tab === 'redo') {
      // 错题重做：按题目ID取单题（含答案）本地即时判分
      getStudyQuestions({ questionId: questionIdParam, exposeAnswer: true })
        .then((res) => setRealQuestions(res?.data || []))
        .catch(() => setRealQuestions([]));
    }
    // 切换 tab 时重置练习状态；practice/trial 由「开始」按钮触发取题，此处不自动拉题
    setDrillStarted(false);
    setTrialStarted(false);
    setRealAnswers({});
    setRealResult(null);
    setJudgeState({});
    setCurrentQ(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [realMode, tab, sectionId]);

  // 题目选项：判断题等无 children 时补「正确/错误」
  function questionOptions(question) {
    const children = question.schema?.children || [];
    if (children.length > 0) return children;
    if (question.questionType === 'Judge' || question.questionType === 'Radio') {
      return [{ id: 'judge_true', title: '正确' }, { id: 'judge_false', title: '错误' }];
    }
    return [];
  }

  // 填空类题目的空位数：
  //  - 多项填空(MultipleBlank)：schema.children 每个空位一个节点 → 空位数即 children 长度；
  //    无 children（历史数据）时回退到标准答案按 | 拆分的数量（>1 视为多空）
  //  - 其余填空(单空/文本)：attribute.blankCount || 1
  function blankCountOf(question) {
    const schema = question.schema || {};
    const type = question.questionType;
    const children = schema.children || [];
    if (type === 'MultipleBlank') {
      if (children.length > 0) return children.length;
      const parts = String(schema.attribute?.examCorrectAnswer || '').split('|').filter(Boolean);
      return parts.length > 1 ? parts.length : 1;
    }
    return schema.attribute?.blankCount || 1;
  }

  // 真实模式：试炼本地即时判分（题目带答案，比对选项文本）
  // 填空类（单项/多项填空）标准答案含 | 分隔多空时逐空判分（每空独立归一化比较），
  // 返回 blankTotal/blankHits 供"答对部分空也给分"的展示与统计
  const realJudge = (q) => {
    const schema = q.schema || {};
    // 标准答案提取与后端 AnswerJudgeUtil 一致：优先整题级 examCorrectAnswer，
    // 缺省回退选项级（收集带 examCorrectAnswer 的选项标题），避免漏判/标准答案显示为空
    const correctAnswers = extractCorrectAnswers({ template: schema });
    if (!correctAnswers || correctAnswers.length === 0) return null;
    const picked = realAnswers[q.id];
    if (!picked) return null;

    // ---- 填空多空：逐空判分 ----
    const blankish = q.questionType === 'MultipleBlank' || q.questionType === 'FillBlank';
    const joined = blankish ? correctAnswers.find((a) => a.includes('|')) : null;
    if (joined) {
      const stdBlanks = joined.split('|');
      const total = stdBlanks.length;
      const studentText = picked.type === 'text' ? String(picked.text || '') : '';
      const stuBlanks = studentText.trim() ? studentText.split('|') : [];
      const hits = Array(total).fill(0);
      if (stuBlanks.length === total) {
        for (let i = 0; i < total; i++) {
          if (blankEq(stdBlanks[i], stuBlanks[i])) hits[i] = 1;
        }
      } else if (studentText.trim() && correctAnswers.some((ca) => blankEq(ca, studentText))) {
        hits.fill(1);
      }
      const hitCount = hits.filter((h) => h === 1).length;
      return {
        correct: hitCount === total ? 1 : 0,
        answer: joined,
        blankTotal: total,
        blankHits: hits,
        blankRight: hitCount,
      };
    }

    // ---- 选择题 / 单空文本：归一化集合比对 ----
    const options = questionOptions(q);
    const titleOf = (id) => options.find((o) => o.id === id)?.title;
    const mine = picked.type === 'option'
      ? [titleOf(picked.optionId)]
      : picked.type === 'options'
        ? (picked.optionIds || []).map(titleOf)
        : [picked.text || ''];
    const mineSet = new Set(mine.map((x) => normBlank(String(x))).filter(Boolean));
    // 标准答案归一化：支持 选项文本 / 选项字母(A/B/C…) / 选项序号(1/2/3…)
    const LETTERS = 'ABCDEFGHIJ';
    const answerSet = new Set();
    correctAnswers.forEach((ans) => {
      const direct = options.find((o) => normBlank(String(o.title || '')) === normBlank(ans));
      if (direct) { answerSet.add(normBlank(String(direct.title))); return; }
      const li = LETTERS.indexOf(ans.trim().toUpperCase());
      if (li >= 0 && options[li]) { answerSet.add(normBlank(String(options[li].title || ''))); return; }
      const ni = parseInt(ans.trim(), 10) - 1;
      if (!Number.isNaN(ni) && options[ni]) { answerSet.add(normBlank(String(options[ni].title || ''))); return; }
      answerSet.add(normBlank(ans));
    });
    const isRight = mineSet.size > 0 && mineSet.size === answerSet.size && [...mineSet].every((x) => answerSet.has(x));
    return { correct: isRight ? 1 : 0, answer: [...answerSet].join('、'), blankTotal: 0 };
  };

  // 标准答案展示文本：多项填空按空位分行（空1: …；空2: …），其余原样
  const answerDisplayOf = (judge) => {
    if (!judge) return '—';
    if (judge.blankTotal > 1 && judge.answer) {
      const parts = String(judge.answer).split('|');
      return parts.map((p, i) => `空${i + 1}: ${p.trim()}`).join('；');
    }
    return judge.answer;
  };

  // 本地判分统计（与后端判分对齐后展示一致）
  const localStats = () => {
    const list = realQuestions || [];
    const correct = list.filter((q) => realJudge(q)?.correct === 1).length;
    return { total: list.length, correct, wrong: list.length - correct };
  };

  // 保存错题归因
  const saveReason = (qid) => {
    const reason = wrongReasons[qid];
    if (!reason) { return; }
    const item = (realResult?.items || []).find((x) => x.questionId === qid);
    if (!item?.detailId) { return; }
    saveWrongReason({ detailId: item.detailId, reason })
      .then(() => message.success('错因已记录，该题已计入错题本'))
      .catch(() => {});
  };

  // 学生答案文本（查看错题用）
  const answerTextOf = (q) => {
    const picked = realAnswers[q.id];
    if (!picked) return '未作答';
    const titleOf = (id) => questionOptions(q).find((o) => o.id === id)?.title;
    if (picked.type === 'option') return titleOf(picked.optionId) || '—';
    if (picked.type === 'options') return (picked.optionIds || []).map(titleOf).join('、');
    return picked.text || '—';
  };

  // 真实模式：填空作答（单空/多空统一：按空位顺序以 | 拼接存入 text）
  const realInput = (q, blankIdx, text) => {
    if (realResult) return;
    if (judgeState[q.id] || (tab === 'preview' && realJudge(q))) return;
    setRealAnswers((prev) => {
      const cur = prev[q.id] || { type: 'text', text: '' };
      const bc = blankCountOf(q);
      const arr = Array.from({ length: bc }, () => '');
      String(cur.text || '').split('|').forEach((v, i) => { if (i < bc) arr[i] = v; });
      arr[Math.max(0, Math.min(blankIdx, bc - 1))] = text;
      return { ...prev, [q.id]: { type: 'text', text: arr.join('|') } };
    });
  };

  // 真实模式：选择选项（按题型单选/多选；试炼选后即时判分锁定）
  const realPick = (q, optId) => {
    if (realResult) return;
    if (judgeState[q.id] || (tab === 'preview' && realJudge(q))) return;
    const multi = q.questionType === 'Checkbox' || q.questionType === 'Multiple';
    setRealAnswers((prev) => {
      const cur = prev[q.id];
      if (!multi) return { ...prev, [q.id]: { type: 'option', optionId: optId } };
      const ids = cur?.type === 'options' ? cur.optionIds : [];
      return { ...prev, [q.id]: { type: 'options', optionIds: ids.includes(optId) ? ids.filter((x) => x !== optId) : [...ids, optId] } };
    });
  };

  // 全部题判定完成后自动落库（错题自动进错题本；错题重做走 wrong/redo 订正）
  useEffect(() => {
    if (!realQuestions?.length || tab === 'preview' || tab === 'preview_practice') return;
    if (!realQuestions.every((q) => judgeState[q.id])) return;
    if (tab === 'redo') {
      if (!redoResult && !realSubmitting) realRedo();
      return;
    }
    if (!realResult && !realSubmitting) {
      realSubmit();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [judgeState]);

  // 真实模式：交卷（后端判分，返回对错 + 标准答案）
  const realSubmit = () => {
    if (realSubmitting || !realQuestions?.length) return;
    const items = realQuestions.map((q) => ({ questionId: q.id, answer: realAnswers[q.id] || null }));
    // 专项练习以 special 记录（练习奖励）；小节通关保持 trial（试炼奖励+通关判定）
    const mode = tab === 'practice' ? 'special' : tab;
    setRealSubmitting(true);
    submitPractice({
      mode,
      items,
      repoId: repoId || undefined,
      knowledgePointId: kpIdParam || realPoints?.[0]?.id || undefined,
      sectionId: sectionId || undefined,
    })
      .then((res) => setRealResult(res?.data || { items: [] }))
      .catch(() => setRealResult({ items: [], score: 0 }))
      .finally(() => setRealSubmitting(false));
  };

  // 错题重做：提交单题到 wrong/redo 完成订正（答对则移出错题本并结算奖励）
  const realRedo = () => {
    if (realSubmitting || !realQuestions?.length) return;
    const q = realQuestions[0];
    const answer = realAnswers[q.id];
    if (!answer) { setRedoResult({ ok: false, message: '请先作答' }); return; }
    setRealSubmitting(true);
    wrongRedo({ questionId: q.id, answer })
      .then((res) => {
        const d = res?.data || {};
        setRedoResult(d);
        if (d.removed) {
          message.success(`订正成功！学习币 +${d.coins || 0} · 积分 +${d.points || 0}`);
        } else {
          message.warning('本次未订正正确，请再试一次');
        }
      })
      .catch(() => setRedoResult({ ok: false, message: '提交失败，请重试' }))
      .finally(() => setRealSubmitting(false));
  };

  // 预习完成：标记该小节/知识点预习完成（完成度 100%）并结算奖励，进度落库保留
  const finishPreview = () => {
    if (previewCompleting || previewDone) return;
    setPreviewCompleting(true);
    completePreview({
      sectionId: sectionId || undefined,
      knowledgePointId: kpIdParam || undefined,
      repoId: repoId || undefined,
    })
      .then((res) => {
        const d = res?.data || {};
        setPreviewDone(true);
        if (d.firstTime) {
          message.success(`预习完成！学习币 +${d.coins} · 积分 +${d.points}`);
        } else {
          message.info('预习已完成，进度已保留');
        }
      })
      .catch(() => message.error('预习完成失败，请重试'))
      .finally(() => setPreviewCompleting(false));
  };

  // ============================================================
  // 真实模式渲染（后台配置内容）
  // ============================================================
  if (realMode) {
    return (
      <div className={`sll-page-enter knowledge-page${tab === 'preview' ? ' knowledge-page-wide' : ''}`}>
        <div className="sll-card" style={{ padding: 24, maxWidth: tab === 'preview' ? 960 : 720, margin: '0 auto' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <div>
              {tab !== 'preview' && (
                <h3 style={{ margin: 0, display: 'inline-flex', alignItems: 'center', gap: 8 }}>
                  <IconTile emoji={tabMeta.icon} tone={tabMeta.tone} size="sm" /> {tabMeta.title}
                </h3>
              )}
              {kpDetail && tab !== 'preview' && (
                <span style={{ marginLeft: 10, fontSize: 13 }}>
                  <span className="sll-level" style={{ background: '#90a4ae', color: '#fff', padding: '1px 8px', borderRadius: 10 }}>
                    {kpDetail.level || '待攻克'}
                  </span>
                  <span style={{ marginLeft: 8, color: '#607d8b' }}>掌握度 {kpDetail.mastery ?? 0}%</span>
                  {kpDetail.weak && <span style={{ marginLeft: 8, color: '#e53935' }}>薄弱 ⚠️</span>}
                </span>
              )}

            </div>
            <button
              className="knowledge-back"
              onClick={() => {
                if (tab === 'preview_practice') { goPreviewLecture(); return; }
                navigate(kpIdParam || repoId ? '/student' : '/student/study');
              }}
            >
              {tab === 'preview_practice' ? '← 回顾讲解' : '返回'}
            </button>
          </div>

          {/* 查看错题弹窗（学生答案/正确答案/解析/错误归因） */}
          <Modal title={<span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}><IconTile emoji="📕" tone="pink" size="xs" /> 错题详情</span>} open={wrongOpen} onCancel={() => setWrongOpen(false)} footer={null} width={640}>
            {wrongList.map((q) => {
              const judge = realJudge(q);
              const schema = q.schema || {};
              const analysis = schema.attribute?.examAnalysis;
              return (
                <div key={q.id} style={{ border: '1px solid #ffcdd2', borderRadius: 10, padding: 12, marginBottom: 10, background: '#fff8f8' }}>
                  <div style={{ fontWeight: 600, marginBottom: 6 }}><RichContent text={q.name || schema.title} /></div>
                  <div style={{ fontSize: 13, marginBottom: 4 }}>
                    <b>你的答案：</b><span style={{ color: '#c62828' }}>{answerTextOf(q)}</span>
                  </div>
                  <div style={{ fontSize: 13, marginBottom: 4 }}>
                    <b>正确答案：</b><span style={{ color: '#2e7d32' }}>{answerDisplayOf(judge)}</span>
                  </div>
                  {analysis && (
                    <div style={{ fontSize: 13, marginBottom: 6, padding: 8, background: '#fffbe6', borderRadius: 6 }}>
                      <b>📝 解析：</b><span style={{ whiteSpace: 'pre-wrap' }}><RichContent text={analysis} /></span>
                    </div>
                  )}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <b style={{ fontSize: 13 }}>错误归因：</b>
                    <Select
                      style={{ width: 200 }} size="small" placeholder="选择错误原因"
                      value={wrongReasons[q.id]}
                      onChange={(v) => { setWrongReasons((p) => ({ ...p, [q.id]: v })); }}
                      options={['大意', '计算错误', '知识点不熟', '题型不会'].map((r) => ({ value: r, label: r }))}
                    />
                    <Button size="small" type="primary" disabled={!wrongReasons[q.id]} onClick={() => saveReason(q.id)}>
                      保存
                    </Button>
                  </div>
                </div>
              );
            })}
          </Modal>

          {/* 预习：知识点导学（概览头 + 步骤 + 目录/讲解卡 + 检测入口） */}
          {tab === 'preview' && (
            <div className="kp-preview">
              {/* 概览头 */}
              <div className="kp-hero">
                <div className="kp-hero-main">
                  <div className="kp-hero-kicker">知识点预习</div>
                  <h2 className="kp-hero-title">{previewTitle}</h2>
                  <div className="kp-hero-meta">
                    <span className="kp-chip">📚 {realPoints?.length || 0} 个知识点</span>
                    {(realPoints?.[0]?.grade || realPoints?.[0]?.term) && (
                      <span className="kp-chip">
                        🎓 {realPoints[0].grade || ''}{realPoints[0].term ? ` · ${realPoints[0].term}学期` : ''}
                      </span>
                    )}
                    <span className={`kp-chip ${previewDone ? 'done' : 'pending'}`}>
                      {previewDone ? '✅ 已完成预习' : '⏳ 待完成'}
                    </span>
                    {kpDetail?.weak && <span className="kp-chip weak">⚠️ 薄弱知识点</span>}
                  </div>
                </div>
                <div className="kp-hero-ring">
                  <Progress
                    type="circle"
                    size={96}
                    strokeWidth={8}
                    percent={kpDetail?.mastery ?? 0}
                    strokeColor={{ '0%': '#64b5f6', '100%': '#1976d2' }}
                    format={(p) => <span className="kp-ring-num">{p}<i>%</i></span>}
                  />
                  <div className="kp-ring-label">当前掌握度</div>
                  {kpDetail?.level && <span className="kp-ring-level">{kpDetail.level}</span>}
                </div>
              </div>

              {/* 学习步骤 */}
              <div className="kp-steps">
                <div className="kp-step active"><b>1</b><span>讲解导读</span></div>
                <div className="kp-step-line" />
                <div className="kp-step"><b>2</b><span>例题检测</span></div>
                <div className="kp-step-line" />
                <div className="kp-step"><b>3</b><span>完成预习</span></div>
              </div>

              {/* 讲解区：左目录 + 右讲解卡 */}
              {realPoints === null ? (
                <div className="kp-loading">内容加载中…</div>
              ) : realPoints.length === 0 ? (
                <div className="knowledge-empty">该小节暂未配置知识点，请联系管理员</div>
              ) : (
                <div className="kp-body">
                  <aside className="kp-catalog">
                    <div className="kp-catalog-title">本节知识点</div>
                    {realPoints.map((p, i) => (
                      <button
                        key={p.id}
                        type="button"
                        className={`kp-catalog-item ${i === activeKpIdx ? 'active' : ''}`}
                        onClick={() => setActiveKpIdx(i)}
                      >
                        <span className="kp-catalog-num">{i + 1}</span>
                        <span className="kp-catalog-name">{p.name}</span>
                        {readKpIds.has(p.id) && <span className="kp-catalog-done">✓</span>}
                      </button>
                    ))}
                  </aside>

                  <div className="kp-detail">
                    {(() => {
                      const p = realPoints[activeKpIdx] || realPoints[0];
                      let content = null;
                      try { content = p.content ? JSON.parse(p.content) : null; } catch { content = null; }
                      const pts = content?.points || [];
                      return (
                        <>
                          {p.imageUrl && (
                            <div className="kp-detail-img">
                              <Image src={p.imageUrl} alt={p.name} />
                            </div>
                          )}
                          <div className="kp-detail-head">
                            <span className="kp-detail-badge">知识点 {activeKpIdx + 1}</span>
                            <h3 className="kp-detail-name">{p.name}</h3>
                          </div>
                          {pts.length > 0 ? (
                            <div className="kp-points">
                              {pts.map((pt, i) => (
                                <div className="kp-point-card" key={i}>
                                  <span className="kp-point-index">{i + 1}</span>
                                  <div className="kp-point-text">{renderRichText(pt)}</div>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <div className="kp-point-empty">该知识点暂未配置讲解要点</div>
                          )}
                          <div className="kp-whale">
                            <IconTile emoji="🐳" tone="sky" size="md" />
                            <div className="kp-whale-body">
                              <b>小鲸导读</b>
                              <p>先理解上面每一条要点；读完本节后点击「开始例题检测」，检测通过即可完成预习并领取学习币。</p>
                            </div>
                          </div>
                          <div className="kp-detail-nav">
                            <button
                              type="button"
                              className="kp-nav-btn"
                              disabled={activeKpIdx === 0}
                              onClick={() => setActiveKpIdx((i) => Math.max(0, i - 1))}
                            >
                              上一个
                            </button>
                            <span className="kp-nav-pos">{activeKpIdx + 1} / {realPoints.length}</span>
                            {activeKpIdx < realPoints.length - 1 ? (
                              <button
                                type="button"
                                className="kp-nav-btn primary"
                                onClick={() => setActiveKpIdx((i) => Math.min(realPoints.length - 1, i + 1))}
                              >
                                下一个知识点
                              </button>
                            ) : (
                              <button type="button" className="kp-nav-btn primary" onClick={goPreviewPractice}>
                                开始例题检测
                              </button>
                            )}
                          </div>
                        </>
                      );
                    })()}
                  </div>
                </div>
              )}

              {/* 底部 CTA */}
              {realPoints?.length > 0 && (
                <div className="kp-cta">
                  <div className="kp-cta-text">读完讲解后，做 3 道小检测巩固一下吧</div>
                  <Button type="primary" size="large" className="kp-cta-btn" onClick={goPreviewPractice}>
                    开始例题检测
                  </Button>
                </div>
              )}
            </div>
          )}

          {/* 专项练习：按知识点为单位逐个过关（无需选择知识点/题型） */}
          {tab === 'practice' && !drillStarted && (
            <div className="drill-panel">
              <div className="drill-panel-head">
                <h3><IconTile emoji="✏️" tone="orange" size="sm" /> 专项练习湾</h3>
                <p>按知识点逐个练习，每个知识点至少 1 题，全部知识点都会过一遍。</p>
              </div>
              {(realPoints?.length || 0) > 0 ? (
                <div className="drill-section">
                  <div className="drill-label">本节知识点（共 {realPoints.length} 个 · 全部覆盖）</div>
                  <div className="drill-kp-list">
                    {realPoints.map((p, idx) => (
                      <span key={p.id} className="drill-kp-chip active">
                        {idx + 1}. {p.name}
                      </span>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="knowledge-empty">该小节暂未配置知识点，无法进行专项练习，请联系管理员配置</div>
              )}
              <div className="drill-footer">
                <Button type="primary" size="large" className="kp-cta-btn" onClick={startDrill}>开始专项练习</Button>
              </div>
            </div>
          )}

          {/* 小节通关：配置摘要 + 开始 */}
          {tab === 'trial' && !trialStarted && (
            <div className="drill-panel">
              <div className="drill-panel-head">
                <h3><IconTile emoji="🎯" tone="green" size="sm" /> 小节通关</h3>
                <p>按老师配置对小节进行通关检验，正确率达标即可通关。</p>
              </div>
              <div className="trial-summary">
                <div className="trial-summary-item">
                  <span>出题模式</span>
                  <b>{isRandomMode(practiceConfig) ? '随机出题' : '常规（整节全部题）'}</b>
                </div>
                {isRandomMode(practiceConfig) && (
                  <>
                    <div className="trial-summary-item"><span>题量</span><b>{practiceConfig?.questionCount || '不限'}</b></div>
                    <div className="trial-summary-item"><span>难度</span><b>{practiceConfig?.difficulty || '不限'}</b></div>
                    <div className="trial-summary-item">
                      <span>题型</span>
                      <b>{(practiceConfig?.types || []).map((t) => DRILL_TYPE_OPTIONS.find((o) => o.value === t)?.label || t).join('、') || '不限'}</b>
                    </div>
                  </>
                )}
                <div className="trial-summary-item pass"><span>通关线</span><b>{practiceConfig?.passRate ?? 80}%</b></div>
              </div>
              <div className="drill-footer">
                <Button type="primary" size="large" className="kp-cta-btn" onClick={startTrial}>开始通关</Button>
              </div>
            </div>
          )}

          {/* 例题检测 / 错题重做 / 已开始的专项练习与小节通关：逐题模式（每题一页 + 答题指示器） */}
          {(tab === 'example' || tab === 'preview_practice' || tab === 'redo'
            || (tab === 'practice' && drillStarted) || (tab === 'trial' && trialStarted)) && (
            realQuestions === null ? <div>加载中…</div> : realQuestions.length === 0 ? (
              <div className="knowledge-empty">暂无可练习题目，请联系管理员配置练习/题目</div>
            ) : (
              <div style={{ display: 'flex', gap: 16 }}>
                {/* 左侧：当前题目 + 答题区 */}
                <div style={{ flex: 1 }}>
                  {(() => {
                    const question = realQuestions[currentQ];
                    const schema = question.schema || {};
                    const options = questionOptions(question);
                    const multi = question.questionType === 'Checkbox' || question.questionType === 'Multiple';
                    const picked = realAnswers[question.id];
                    const judge = realJudge(question);
                    const showResult = judgeState[question.id] === true;
                    const correct = judge ? judge.correct : null;
                    const analysis = schema.attribute?.examAnalysis;
                    return (
                      <div className="knowledge-question-card">
                        <div className="knowledge-q-head">
                          <span className="knowledge-q-type">{QUESTION_TYPE_LABELS[question.questionType] || '题目'}</span>
                          <span className="knowledge-q-progress">第 {currentQ + 1} / {realQuestions.length} 题</span>
                        </div>
                        {tab === 'practice' && question.knowledgePointId && (() => {
                          const kpIndex = (realPoints || []).findIndex((p) => p.id === question.knowledgePointId);
                          const kpName = kpIndex >= 0 ? realPoints[kpIndex].name : '';
                          return (
                            <div style={{ display: 'inline-block', marginBottom: 10, padding: '2px 10px', borderRadius: 10, background: '#e3f2fd', color: '#1565c0', fontSize: 13 }}>
                              {kpIndex >= 0 ? `知识点 ${kpIndex + 1}/${realPoints.length}` : '知识点'}：{kpName || '未命名'}
                            </div>
                          );
                        })()}
                        <div className="knowledge-q-text">{currentQ + 1}. <RichContent text={question.name || schema.title} /></div>
                        {/* 老师配图（题干图片，与题干 Markdown 图片互补） */}
                        {schema.attribute?.examImages?.length > 0 && (
                          <div className="knowledge-q-images">
                            {schema.attribute.examImages.map((img, i) => (
                              <Image key={i} src={img} alt="题目配图" style={{ maxWidth: 480, marginBottom: 8 }} />
                            ))}
                          </div>
                        )}
                        {/* 填空类输入（单空/多行文本/多项填空）；判断题无选项时补 正确/错误，其余走选项 */}
                        {['FillBlank', 'Text', 'MultipleBlank'].includes(question.questionType) ? (() => {
                          const blankCount = blankCountOf(question);
                          const isMultiBlank = question.questionType === 'MultipleBlank';
                          const curText = picked?.text || '';
                          const texts = curText.split('|');
                          return (
                            <div>
                              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                                {Array.from({ length: blankCount }).map((_, idx) => {
                                  // 判题后按逐空命中标色（1 绿 / 0 红），未判或未命中映射不标
                                  const hit = showResult ? (judge?.blankHits || [])[idx] : undefined;
                                  const inputStyle = hit === 1
                                    ? { borderColor: '#4caf50', background: '#e8f5e9' }
                                    : hit === 0
                                      ? { borderColor: '#ef5350', background: '#ffebee' }
                                      : undefined;
                                  return (
                                    <Input
                                      key={idx}
                                      placeholder={`空${idx + 1}`}
                                      disabled={showResult}
                                      style={{ width: isMultiBlank ? 170 : 140, fontSize: 15, padding: '8px 10px', ...inputStyle }}
                                      value={texts[idx] || ''}
                                      onChange={(e) => realInput(question, idx, e.target.value)}
                                    />
                                  );
                                })}
                              </div>
                              {isMultiBlank && (
                                <div style={{ marginTop: 8, fontSize: 12, color: '#90a4ae' }}>
                                  本题为多项填空，共 {blankCount} 个空位：请按题目空位顺序，在每个空位下方的输入框中填写答案
                                </div>
                              )}
                            </div>
                          );
                        })() : (
                          <div className={`knowledge-options ${showResult ? 'readonly' : ''}`}>
                            {options.map((opt, oi) => {
                              const selected = picked?.type === 'option'
                                ? picked.optionId === opt.id
                                : (picked?.optionIds || []).includes(opt.id);
                              const showRight = showResult && correct === 1 && selected;
                              const showWrong = showResult && correct === 0 && selected;
                              return (
                                <div
                                  key={opt.id}
                                  className={`knowledge-option ${selected ? 'selected' : ''} ${showRight ? 'right' : ''} ${showWrong ? 'wrong' : ''}`}
                                  onClick={() => realPick(question, opt.id)}
                                >
                                  <span className={`knowledge-opt-key ${multi ? 'multi' : ''}`}>{String.fromCharCode(65 + oi)}</span>
                                  <span className="knowledge-opt-text"><RichContent text={opt.title} /></span>
                                  {showRight && <span className="knowledge-opt-mark">✓</span>}
                                  {showWrong && <span className="knowledge-opt-mark wrong">✗</span>}
                                </div>
                              );
                            })}
                          </div>
                        )}
                        {/* 答案与解析：先答案，再解析，字号与题目一致 */}
                        {showResult && (
                          <div className="knowledge-q-analysis">
                            <div className={`knowledge-q-verdict ${correct === 1 ? 'right' : (judge?.blankTotal > 1 && (judge.blankRight || 0) > 0) ? 'part' : 'wrong'}`}>
                              {correct === 1
                                ? '✅ 回答正确'
                                : (judge?.blankTotal > 1 && (judge.blankRight || 0) > 0)
                                  ? `⚠️ 部分正确：答对 ${judge.blankRight}/${judge.blankTotal} 空`
                                  : '❌ 回答错误'}
                            </div>
                            <div className="knowledge-q-answer">
                              <b>正确答案：</b>{answerDisplayOf(judge)}
                            </div>
                            {analysis && (
                              <div className="knowledge-q-analysis-body">
                                <b>📝 解析：</b><RichContent text={analysis} />
                              </div>
                            )}
                          </div>
                        )}
                        {/* 提交答案按钮：点击后才判定（参考 image-2：右下角常驻，未作答置灰） */}
                        {!showResult && (
                          <div className="knowledge-q-submit">
                            <Button type="primary" disabled={!picked} onClick={() => setJudgeState((p) => ({ ...p, [question.id]: true }))}>
                              提交答案
                            </Button>
                          </div>
                        )}
                        {/* 导航 */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 14 }}>
                          <button className="knowledge-back" disabled={currentQ === 0} onClick={() => setCurrentQ((c) => c - 1)}>上一题</button>
                          <div style={{ fontSize: 13, color: '#90a4ae' }}>
                            已判定 {Object.keys(judgeState).length}/{realQuestions.length} 题
                          </div>
                          {currentQ < realQuestions.length - 1 ? (
                            <button className="knowledge-back" onClick={() => setCurrentQ((c) => c + 1)}>下一题</button>
                          ) : (
                            realQuestions.every((q) => judgeState[q.id]) && !isPreviewFlow && tab !== 'redo' ? (
                              <button className="knowledge-back" onClick={realSubmit} disabled={realSubmitting}>
                                {realSubmitting ? '提交中…' : '完成'}
                              </button>
                            ) : (
                              <span />
                            )
                          )}
                        </div>
                        {!showResult && (
                          <div style={{ marginTop: 8, fontSize: 12, color: '#b26a00' }}>
                            选择题/填空作答后，点击「提交答案」才会判定
                          </div>
                        )}
                        {currentQ === realQuestions.length - 1 && realQuestions.every((q) => judgeState[q.id]) && tab !== 'redo' && (
                          (() => {
                            const st = localStats();
                            const wrongList = (realQuestions || []).filter((q) => realJudge(q)?.correct === 0);
                            return (
                              <div style={{ marginTop: 12, padding: 12, borderRadius: 10, background: '#e8f5e9', textAlign: 'center' }}>
                                <div style={{ fontSize: 18, fontWeight: 700, color: '#2e7d32' }}>
                                  🎉 完成 · 答对 {st.correct}/{st.total}
                                </div>
                                {st.wrong > 0 && (
                                  <Button type="primary" size="small" style={{ marginTop: 10 }} onClick={() => { setWrongList(wrongList); setWrongOpen(true); }}>
                                    📕 查看错题（{st.wrong}）
                                  </Button>
                                )}
                                {/* 小节通关：通关判定结果 + 星级 + 再练/返回 */}
                                {tab === 'trial' && (
                                  realResult ? (
                                    <div className="trial-result">
                                      <div className={`trial-result-title ${realResult.passed ? 'pass' : 'fail'}`}>
                                        {realResult.passed ? '🎉 通关成功' : '💪 本次未通关'}
                                      </div>
                                      <div className="trial-stars">
                                        {'⭐'.repeat(realResult.stars || 0)}{'☆'.repeat(5 - (realResult.stars || 0))}
                                      </div>
                                      <div className="trial-result-meta">
                                        正确率 {realResult.rate ?? 0}% · 通关线 {realResult.passRate ?? 80}% · 历史最佳 {realResult.bestRate ?? 0}%（{realResult.bestStars ?? 0} 星）
                                      </div>
                                      <div className="trial-result-tags">
                                        {realResult.firstPass && <span className="trial-tag first">首次通关</span>}
                                        {realResult.passed && realResult.unlockedNext && <span className="trial-tag unlock">已解锁下一小节</span>}
                                      </div>
                                      <div style={{ display: 'flex', gap: 10, justifyContent: 'center', marginTop: 12, flexWrap: 'wrap' }}>
                                        {!realResult.passed && <Button type="primary" onClick={startTrial}>再练一次</Button>}
                                        <Button onClick={() => navigate('/student/study')}>返回研习页</Button>
                                      </div>
                                    </div>
                                  ) : (
                                    <div style={{ marginTop: 8, color: '#607d8b' }}>通关判定中…</div>
                                  )
                                )}
                                {/* 预习完成：标记完成度 100% 并结算奖励，进度落库保留 */}
                                {isPreviewFlow && (
                                  previewDone ? (
                                    <div style={{ marginTop: 12 }}>
                                      <div style={{ color: '#2e7d32', fontWeight: 600 }}>✅ 预习已完成 · 进度已保留</div>
                                      <div style={{ display: 'flex', gap: 10, justifyContent: 'center', marginTop: 10, flexWrap: 'wrap' }}>
                                        <Button type="primary" onClick={() => navigate('/student/study')}>返回研习页</Button>
                                        <Button onClick={goPreviewPractice}>再练一遍</Button>
                                      </div>
                                    </div>
                                  ) : (
                                    <div style={{ marginTop: 12 }}>
                                      <div style={{ color: '#5b7a99', fontSize: 13, marginBottom: 6 }}>
                                        完成例题检测即可结束本次预习并领取奖励
                                      </div>
                                      <Button
                                        type="primary"
                                        size="large"
                                        loading={previewCompleting}
                                        onClick={finishPreview}
                                        style={{ height: 44, borderRadius: 8, fontSize: 15 }}
                                      >
                                        📖 完成预习
                                      </Button>
                                    </div>
                                  )
                                )}
                              </div>
                            );
                          })()
                        )}
                        {/* 错题重做：订正结果 */}
                        {tab === 'redo' && realQuestions.length > 0 && judgeState[realQuestions[0].id] && (
                          <div style={{ marginTop: 12, padding: 12, borderRadius: 10, background: '#f1f8e9', textAlign: 'center' }}>
                            {!redoResult ? (
                              <div style={{ color: '#607d8b' }}>提交订正中…</div>
                            ) : redoResult.removed ? (
                              <>
                                <div style={{ fontSize: 16, fontWeight: 700, color: '#2e7d32' }}>
                                  ✅ 订正成功，已移出错题本
                                </div>
                                <div style={{ color: '#2e7d32', marginTop: 4 }}>
                                  学习币 +{redoResult.coins || 0} · 积分 +{redoResult.points || 0}
                                </div>
                              </>
                            ) : (
                              <div style={{ fontSize: 15, fontWeight: 600, color: '#c62828' }}>
                                ❌ 本次未订正正确，请返回错题本再次挑战
                              </div>
                            )}
                            <Button type="primary" size="small" style={{ marginTop: 10 }} onClick={() => navigate('/student/wrong')}>
                              返回错题本
                            </Button>
                          </div>
                        )}
                      </div>
                    );
                  })()}
                </div>

                {/* 右侧：答题指示器（题号卡） */}
                <div style={{ width: 120, flexShrink: 0 }}>
                  <div style={{ fontWeight: 600, marginBottom: 8, fontSize: 13 }}>答题指示器</div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 6 }}>
                    {realQuestions.map((q, i) => {
                      const answered = !!realAnswers[q.id];
                      const active = i === currentQ;
                      return (
                        <div key={q.id} onClick={() => setCurrentQ(i)}
                          style={{ width: 26, height: 26, display: 'flex', alignItems: 'center', justifyContent: 'center',
                            borderRadius: 6, fontSize: 12, cursor: 'pointer',
                            background: active ? '#0288d1' : answered ? '#c8e6c9' : '#e0e0e0',
                            color: active ? '#fff' : '#333' }}>
                          {i + 1}
                        </div>
                      );
                    })}
                  </div>
                  <div style={{ marginTop: 10, fontSize: 11, color: '#90a4ae' }}>
                    <span style={{ display: 'inline-block', width: 10, height: 10, background: '#c8e6c9', borderRadius: 2, marginRight: 4 }} />已答
                  </div>
                </div>
              </div>
            )
          )}
          {/* 知识点错题本：本人错题（/api/practice/wrong-list），与底部导航「错题本」同源 */}
          {tab === 'wrong' && <WrongBookPanel />}
        </div>
      </div>
    );
  }

  // 未找到知识点
  // 非真实模式（无 sectionId/repoId/kpId 参数）：提示从研习页或任务进入
  if (!realMode) {
    return (
      <div className="sll-page-enter knowledge-empty">
        <div className="sll-card knowledge-empty-card">
          <div className="knowledge-empty-icon">🌊</div>
          <div>请从「研习页」选择小节或从「今日任务」进入学习</div>
          <button className="knowledge-back" onClick={() => navigate('/student/study')}>返回学海研习</button>
        </div>
      </div>
    );
  }
}
