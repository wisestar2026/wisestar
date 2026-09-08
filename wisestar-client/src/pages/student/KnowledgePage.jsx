/**
 * KnowledgePage.jsx - 知识点详情页（学海智习系统 V2.0）
 * 模式（?tab=）: preview 预习 / practice 练习 / trial 试炼 / wrong 错题
 *
 * 视觉规范: 大面积留白低干扰；题目选项全部大圆角卡片（摒弃单选框）；
 *           提交后逐题标记对错；顶部轻柔奖励提示 1.5s 自动消失（无弹窗）
 *
 * 交卷制（专项练习湾 practice / 小节通关 trial）:
 *   1. 出题不带答案（exposeAnswer=false），逐题作答不即时判分
 *   2. 末题出现「提交练习」：确认时提示未答题数，未答一律判错
 *   3. 后端统一判分返回逐题对错 → 强制逐题错误归因（选完才能看结果）
 *   4. 结果页：本次得分/正确率 + 逐题回顾（错因标签）+ 知识点掌握总结
 *      + 掌握变化（与上次练习对比）；可「再次练习」重新抽题
 *
 * 被谁引用: App.jsx（/student/knowledge/:kpId）、研习页右栏四大按钮
 * 依赖: react-router-dom(useParams/useSearchParams/useNavigate)、useStudentStore、./KnowledgePage.css
 */

import { useEffect, useRef, useState } from 'react';
import { Input, Button, Modal, Select, Tabs, Tag, Progress, message } from 'antd';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { getStudyPoints, getStudyQuestions, uploadActivity } from '../../api/student';
import {
  submitPractice, saveWrongReason, saveWrongReasons, getPracticeHistory,
} from '../../api/practice';
import './KnowledgePage.css';

// 错误归因选项（交卷后强制逐题标注；与错题本错因口径一致）
const WRONG_REASONS = ['知识点不熟', '题型不会', '计算错误', '粗心大意', '审题不清', '时间不足'];

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

// 标准答案原始文本 → 展示文本（多行答案/多空 | 分隔美化）
function prettyAnswer(raw) {
  return String(raw == null ? '' : raw)
    .split('\n')
    .map((line) => (line.includes('|') ? line.split('|').map((s) => s.trim()).filter(Boolean).join('；') : line.trim()))
    .filter(Boolean)
    .join('；');
}

/**
 * 交卷制结果页（专项练习湾/小节通关）
 * 顶部: 本次得分/答对数 + 掌握变化（相对上次练习）
 * 中部: 知识点掌握总结（按知识点聚合正确题数）
 * 底部: 逐题回顾（对错 + 我的答案 + 正确答案 + 解析 + 错因标签）
 */
function SubmissionResultView({
  result, questions, answers, history,
  reasonOf, prettyAnswer, answerTextOf, onRetry, onBack,
}) {
  const items = result?.items || [];
  const byId = {};
  items.forEach((it) => { byId[it.questionId] = it; });
  const total = questions.length || result?.total || 0;
  const rightCount = items.filter((it) => it.correct === 1).length;
  const wrongCount = total - rightCount;
  const accuracy = total > 0 ? Math.round((rightCount / total) * 100) : 0;
  const score = Number(result?.score);
  const hasScore = Number.isFinite(score) && Number(result?.totalScore) > 0;
  const totalScore = Number(result?.totalScore) || 0;
  const fmtTime = (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '-');

  // 掌握变化：history[0]=本次练习；history[1]=上次练习（按记录 rate 对比）
  let delta = null;
  let prevRate = null;
  if (Array.isArray(history) && history.length >= 2 && history[0]?.rate != null && history[1]?.rate != null) {
    prevRate = history[1].rate;
    delta = history[0].rate - history[1].rate;
  }
  const lastTime = Array.isArray(history) && history[0] ? history[0].createAt : null;

  // 知识点掌握总结（结果题按后端归属知识点聚合：答对 x / 共 y）
  const kpAgg = {};
  items.forEach((it) => {
    if (!it.knowledgePointId) return;
    const key = it.knowledgePointId;
    kpAgg[key] = kpAgg[key] || { name: it.knowledgePointName || '知识点', right: 0, attempts: 0 };
    kpAgg[key].attempts += 1;
    if (it.correct === 1) kpAgg[key].right += 1;
  });
  const kpList = Object.values(kpAgg);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      {/* 本次结果统计 */}
      <div style={{ border: '1px solid #e3f2fd', borderRadius: 12, padding: 18, background: '#f8fcff' }}>
        <div style={{ fontWeight: 700, fontSize: 17, marginBottom: 14 }}>练习完成 · 本次结果</div>
        <div style={{ display: 'flex', justifyContent: 'center', gap: 48, textAlign: 'center' }}>
          {hasScore && (
            <div>
              <div style={{ fontSize: 34, fontWeight: 700, color: '#1677ff' }}>{score}</div>
              <div style={{ color: '#90a4ae', fontSize: 12 }}>得分 / {totalScore}</div>
            </div>
          )}
          <div>
            <div style={{ fontSize: 34, fontWeight: 700, color: '#52c41a' }}>{accuracy}%</div>
            <div style={{ color: '#90a4ae', fontSize: 12 }}>正确率</div>
          </div>
          <div>
            <div style={{ fontSize: 34, fontWeight: 700, color: '#722ed1' }}>{rightCount}</div>
            <div style={{ color: '#90a4ae', fontSize: 12 }}>答对 / {total}</div>
          </div>
          <div>
            <div style={{ fontSize: 34, fontWeight: 700, color: '#ff4d4f' }}>{wrongCount}</div>
            <div style={{ color: '#90a4ae', fontSize: 12 }}>答错（含未答）</div>
          </div>
        </div>
      </div>

      {/* 掌握变化 + 知识点掌握总结 */}
      {(delta != null || lastTime || kpList.length > 0) && (
        <div style={{ border: '1px solid #ede7f6', borderRadius: 12, padding: 16, background: '#fcfaff' }}>
          <div style={{ fontWeight: 700, marginBottom: 10 }}>掌握情况</div>
          <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap', marginBottom: kpList.length ? 12 : 0 }}>
            {delta != null && (
              <div style={{ fontSize: 14 }}>
                <span style={{ color: '#90a4ae' }}>掌握变化（较上次）</span>{' '}
                <b style={{ color: delta >= 0 ? '#52c41a' : '#ff4d4f' }}>
                  {delta >= 0 ? '+' : ''}{delta}%
                </b>
              </div>
            )}
            {lastTime && (
              <div style={{ fontSize: 14 }}>
                <span style={{ color: '#90a4ae' }}>最近练习</span>{' '}
                <b>{fmtTime(lastTime)}</b>
              </div>
            )}
            {prevRate != null && (
              <div style={{ fontSize: 14 }}>
                <span style={{ color: '#90a4ae' }}>上次正确率</span>{' '}
                <b>{prevRate}%</b>
              </div>
            )}
          </div>
          {kpList.length > 0 && (
            <div>
              <div style={{ fontSize: 13, color: '#90a4ae', marginBottom: 8 }}>知识点掌握总结</div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                {kpList.map((kp) => (
                  <span key={kp.name} style={{
                    display: 'inline-flex', alignItems: 'center', gap: 6,
                    padding: '4px 10px', borderRadius: 20,
                    background: kp.right === kp.attempts ? '#e8f5e9' : kp.right > 0 ? '#fff8e1' : '#ffebee',
                    color: kp.right === kp.attempts ? '#2e7d32' : kp.right > 0 ? '#b26a00' : '#c62828',
                    fontSize: 13,
                  }}>
                    {kp.name} · {kp.right}/{kp.attempts}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* 逐题回顾 */}
      <div style={{ border: '1px solid #ececec', borderRadius: 12, padding: 16, background: '#fff' }}>
        <div style={{ fontWeight: 700, marginBottom: 10 }}>逐题回顾</div>
        {questions.map((q, idx) => {
          const it = byId[q.id];
          const schema = q.schema || {};
          const right = it?.correct === 1;
          const wrong = it?.correct === 0 || it?.correct == null;
          const reason = it && reasonOf ? reasonOf(it) : null;
          const analysis = schema.attribute?.examAnalysis;
          return (
            <div key={q.id} style={{
              padding: '12px 14px', marginBottom: 10, borderRadius: 10,
              border: `1px solid ${right ? '#c8e6c9' : '#ffcdd2'}`,
              background: right ? '#fafff7' : '#fffafa',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6, flexWrap: 'wrap' }}>
                <span style={{ fontWeight: 600 }}>{idx + 1}. {q.name || schema.title || '题目'}</span>
                <span style={{ fontSize: 12, color: '#90a4ae' }}>{q.questionType}</span>
                <Tag color={right ? 'green' : 'red'} style={{ marginInlineEnd: 0 }}>
                  {right ? '回答正确' : '回答错误'}
                </Tag>
                {reason && <Tag color="orange">{reason}</Tag>}
              </div>
              <div style={{ fontSize: 13, marginBottom: 4 }}>
                <b>我的答案：</b>
                <span style={{ color: right ? '#2e7d32' : '#c62828' }}>{answerTextOf(q)}</span>
              </div>
              {wrong && it?.correctAnswer && (
                <div style={{ fontSize: 13, marginBottom: 4 }}>
                  <b>正确答案：</b>
                  <span style={{ color: '#2e7d32' }}>{prettyAnswer(it.correctAnswer)}</span>
                </div>
              )}
              {analysis && (
                <div style={{ fontSize: 12, marginTop: 6, padding: '8px 10px', background: '#fffbe6', borderRadius: 6, color: '#6d4c00' }}>
                  <b>解析：</b>
                  <span style={{ whiteSpace: 'pre-wrap' }}>{analysis}</span>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* 操作 */}
      <div style={{ textAlign: 'center', marginTop: 4 }}>
        <Button type="primary" size="large" style={{ marginRight: 12 }} onClick={onRetry}>
          再次练习
        </Button>
        <Button size="large" onClick={onBack}>返回研习页</Button>
      </div>
    </div>
  );
}

// 四种模式 tab 配置
// 预习讲解内容（按知识点生成 mock 要点）
export default function KnowledgePage() {
  const [searchParams] = useSearchParams();
  const tab = searchParams.get('tab') || 'preview';
  const sectionId = searchParams.get('sectionId'); // 小节练习入口
  const repoId = searchParams.get('repoId');         // 练习（题库）任务/直接练习入口
  const kpIdParam = searchParams.get('kpId');        // 知识点任务入口
  const typesParam = searchParams.get('types');      // 题型过滤（逗号分隔，消灭错题用）
  const countParam = searchParams.get('count');      // 出题数量（消灭易错知识点/错题用）
  const realMode = !!(sectionId || repoId || kpIdParam);
  const navigate = useNavigate();

  // ============================================================
  // 真实模式（后台配置内容：知识点/题目来自 /api/student/study/*）
  // ============================================================
  const [realPoints, setRealPoints] = useState(null);      // 知识点（预习）
  const [realQuestions, setRealQuestions] = useState(null); // 题目（练习/试炼）
  const [realAnswers, setRealAnswers] = useState({});       // 作答 {questionId: {type, optionId/optionIds}}
  const [realResult, setRealResult] = useState(null);       // 判分结果
  const [realSubmitting, setRealSubmitting] = useState(false);
  const [currentQ, setCurrentQ] = useState(0);          // 逐题模式当前题索引
  const [judgeState, setJudgeState] = useState({}); // 每题是否已提交判定 {qid: true}
  const [activeTab, setActiveTab] = useState('lecture'); // 预习：lecture 讲解 | example 例题
  const [wrongOpen, setWrongOpen] = useState(false);        // 查看错题弹窗
  const [wrongReasons, setWrongReasons] = useState({});     // 各错题归因 {questionId: reason}
  const [wrongList, setWrongList] = useState([]);          // 当前错题列表（查看错题弹窗）

  // ===== 交卷制（专项练习湾/小节通关）：提交 → 强制错因 → 结果 =====
  const isSubmissionFlow = tab === 'practice' || tab === 'trial';
  const submissionStart = useRef(null);        // 本次作答开始时间（交卷用时）
  const [submitPhase, setSubmitPhase] = useState('idle'); // idle 答题 | cause 归因 | result 结果
  const [practiceResult, setPracticeResult] = useState(null); // PracticeResultView（交卷判分结果）
  const [causeOpen, setCauseOpen] = useState(false);        // 强制错因弹窗
  const [causeDrafts, setCauseDrafts] = useState({});       // {detailId: reason}
  const [causeItems, setCauseItems] = useState([]);         // 待归因错题（含题面快照）
  const [resultHistory, setResultHistory] = useState(null); // 交卷结果页掌握变化（近 20 条记录）

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

  // 交卷制「再次练习」重抽计数（>0 时向出题接口传 random=true 打乱整卷）
  const [retryNo, setRetryNo] = useState(0);

  useEffect(() => {
    if (!realMode) return;
    if (tab === 'preview') {
      // 预习/复习：讲解要点 + 知识点预习检测（实时判分）
      if (sectionId) {
        getStudyPoints(sectionId).then((res) => setRealPoints(res?.data || [])).catch(() => setRealPoints([]));
      }
      // 预习讲解页：保持轻量探测题（默认 3 题，可显式 count 覆盖）
      const params = { count: Number(countParam) || 3, exposeAnswer: true };
      if (typesParam) params.types = typesParam.split(',');
      if (sectionId) params.sectionId = sectionId;
      if (repoId) params.repoId = repoId;
      if (kpIdParam) params.knowledgePointId = kpIdParam;
      getStudyQuestions(params)
        .then((res) => setRealQuestions(res?.data || []))
        .catch(() => setRealQuestions([]));
    } else if (tab === 'practice' || tab === 'trial') {
      // 交卷制（专项练习湾/小节通关）：题目不带答案，交卷后由后端统一判分；
      // 交卷后再也不逐题即时判分。再次练习（retryNo>0）传 random 重抽整卷。
      const params = {};
      if (countParam) params.count = Number(countParam);
      if (typesParam) params.types = typesParam.split(',');
      if (sectionId) params.sectionId = sectionId;
      if (repoId) params.repoId = repoId;
      if (kpIdParam) params.knowledgePointId = kpIdParam;
      if (retryNo > 0 || searchParams.get('random') === '1') params.random = true;
      getStudyQuestions(params)
        .then((res) => setRealQuestions(res?.data || []))
        .catch(() => setRealQuestions([]));
    }
    // 重进入答题态：清空作答/结果/归因，开始计时
    setRealAnswers({});
    setRealResult(null);
    setJudgeState({});
    setSubmitPhase('idle');
    setPracticeResult(null);
    setCauseOpen(false);
    setCauseItems([]);
    setCauseDrafts({});
    setResultHistory(null);
    if (isSubmissionFlow) {
      submissionStart.current = Date.now();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [realMode, tab, sectionId, retryNo]);

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
    const answerText = schema.attribute?.examCorrectAnswer;
    if (!answerText) return null;
    const correctAnswers = answerText.split('\n').map((x) => x.trim()).filter(Boolean);
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
        answer: answerText,
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
    return { correct: isRight ? 1 : 0, answer: answerText, blankTotal: 0 };
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
    if (isSubmissionFlow && submitPhase !== 'idle') return;
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
    if (isSubmissionFlow && submitPhase !== 'idle') return;
    if (judgeState[q.id] || (tab === 'preview' && realJudge(q))) return;
    const multi = q.questionType === 'Checkbox' || q.questionType === 'Multiple';
    setRealAnswers((prev) => {
      const cur = prev[q.id];
      if (!multi) return { ...prev, [q.id]: { type: 'option', optionId: optId } };
      const ids = cur?.type === 'options' ? cur.optionIds : [];
      return { ...prev, [q.id]: { type: 'options', optionIds: ids.includes(optId) ? ids.filter((x) => x !== optId) : [...ids, optId] } };
    });
  };

  // 全部题判定完成后自动落库（旧即时判分流；交卷制不再逐题判定，该流不触发）
  useEffect(() => {
    if (!realQuestions?.length || tab === 'preview' || isSubmissionFlow) return;
    if (realQuestions.every((q) => judgeState[q.id]) && !realResult && !realSubmitting) {
      realSubmit();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [judgeState]);

  // 真实模式：交卷（后端判分，返回对错 + 标准答案）——旧即时判分流末题「完成」用
  const realSubmit = () => {
    if (realSubmitting || !realQuestions?.length) return;
    const items = realQuestions.map((q) => ({ questionId: q.id, answer: realAnswers[q.id] || null }));
    setRealSubmitting(true);
    submitPractice({ mode: tab, items, repoId: repoId || undefined, knowledgePointId: kpIdParam || undefined, sectionId: sectionId || undefined })
      .then((res) => setRealResult(res?.data || { items: [] }))
      .catch(() => setRealResult({ items: [], score: 0 }))
      .finally(() => setRealSubmitting(false));
  };

  // ============================================================
  // 交卷制（专项练习湾 practice / 小节通关 trial）
  // 流程: 逐题作答（不判分）→ 末题「提交练习」（未答判错）
  //       → 后端统一判分 → 强制逐题错因 → 结果页（掌握变化）
  // ============================================================

  // 某题是否已有效作答（选答有值/填空非空；空作答视为未答，交卷判错）
  const isAnswered = (q) => {
    const a = realAnswers[q.id];
    if (!a) return false;
    if (a.type === 'option') return !!a.optionId;
    if (a.type === 'options') return (a.optionIds || []).length > 0;
    if (a.type === 'text') return !!String(a.text || '').trim();
    return false;
  };

  // 结果页「掌握变化」数据：范围内近 20 条练习记录（第 0 条=本次）
  const loadHistory = () => {
    const params = sectionId ? { sectionId }
      : kpIdParam ? { knowledgePointId: kpIdParam } : null;
    if (!params) return;
    getPracticeHistory(params)
      .then((res) => setResultHistory(res?.data || []))
      .catch(() => setResultHistory([]));
  };

  // 交卷成功 → 归因弹窗（有错题）或直接结果页（全对）
  const onSubmitDone = (result) => {
    setRealSubmitting(false);
    setPracticeResult(result || {});
    const wrongs = (result?.items || []).filter((it) => it.correct === 0 || it.correct == null);
    const snapshots = wrongs.map((it) => ({
      ...it,
      question: (realQuestions || []).find((q) => q.id === it.questionId),
    }));
    setCauseItems(snapshots);
    setCauseDrafts({});
    if (snapshots.length === 0) {
      setSubmitPhase('result');
      loadHistory();
      return;
    }
    // 强制错因：未选完不允许进入结果页
    setSubmitPhase('cause');
    setCauseOpen(true);
  };

  // 末题「提交练习」：二次确认（提示未答题数，未答判错）后交卷
  const requestSubmitPractice = () => {
    if (realSubmitting || !realQuestions?.length || submitPhase !== 'idle') return;
    const unanswered = realQuestions.filter((q) => !isAnswered(q)).length;
    const tip = unanswered > 0
      ? `还有 ${unanswered} 题未作答，交卷后按答错处理。确定提交练习吗？`
      : '交卷后将由系统统一判分，并逐题确认错因。确定提交吗？';
    Modal.confirm({
      title: tab === 'practice' ? '提交练习' : '提交通关',
      content: tip,
      okText: '提交',
      cancelText: '再检查一下',
      onOk: performSubmitPractice,
    });
  };

  // 调后端统一判分（本卷题目不带答案，判分以后端为准）
  const performSubmitPractice = () => {
    if (realSubmitting) return;
    const durationMs = submissionStart.current ? Date.now() - submissionStart.current : 0;
    const items = realQuestions.map((q) => ({ questionId: q.id, answer: realAnswers[q.id] || null }));
    setRealSubmitting(true);
    submitPractice({
      mode: tab === 'trial' ? 'exam' : 'special',
      sectionId: sectionId || undefined,
      knowledgePointId: kpIdParam || undefined,
      repoId: repoId || undefined,
      durationMs,
      items,
    })
      .then((res) => onSubmitDone(res?.data))
      .catch(() => {
        setRealSubmitting(false);
        message.error('交卷失败，请稍后重试');
      });
  };

  // 强制错因弹窗「保存并查看结果」：全部错题标注完成后放行
  const confirmCauses = () => {
    const missing = causeItems.filter((it) => !causeDrafts[it.detailId]);
    if (missing.length > 0) {
      message.warning(`还有 ${missing.length} 道错题未选择原因`);
      return;
    }
    setCauseOpen(false);
    saveWrongReasons({ items: causeItems.map((it) => ({ detailId: it.detailId, reason: causeDrafts[it.detailId] })) })
      .then(() => { setSubmitPhase('result'); loadHistory(); })
      .catch(() => {
        // 归因保存失败不阻断结果（不影响判分展示），仅提示
        message.warning('错因保存失败，仍可查看本次结果');
        setSubmitPhase('result');
        loadHistory();
      });
  };

  // 再次练习：整卷重抽（random=true），回到答题态
  const retryPractice = () => {
    setRetryNo((n) => n + 1);
  };

  // ============================================================
  // 真实模式渲染（后台配置内容）
  // ============================================================
  if (realMode) {
    return (
      <div className="sll-page-enter knowledge-page">
        <div className="sll-card" style={{ padding: 24, maxWidth: 720, margin: '0 auto' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <div>
              <h3 style={{ margin: 0, display: 'inline-block' }}>
                {tab === 'preview' ? '📖 知识点讲解' : tab === 'practice' ? '✏️ 专项练习湾' : tab === 'trial' ? ' 小节通关' : tab === 'wrong' ? ' 知识点错题本' : ' 知识点预习/复习'}
              </h3>

            </div>
            <button className="knowledge-back" onClick={() => navigate(kpIdParam || repoId ? '/student' : '/student/study')}>返回</button>
          </div>

          {/* 查看错题弹窗（学生答案/正确答案/解析/错误归因） */}
          <Modal title="📕 错题详情" open={wrongOpen} onCancel={() => setWrongOpen(false)} footer={null} width={640}>
            {wrongList.map((q) => {
              const judge = realJudge(q);
              const schema = q.schema || {};
              const analysis = schema.attribute?.examAnalysis;
              return (
                <div key={q.id} style={{ border: '1px solid #ffcdd2', borderRadius: 10, padding: 12, marginBottom: 10, background: '#fff8f8' }}>
                  <div style={{ fontWeight: 600, marginBottom: 6 }}>{q.name || schema.title}</div>
                  <div style={{ fontSize: 13, marginBottom: 4 }}>
                    <b>你的答案：</b><span style={{ color: '#c62828' }}>{answerTextOf(q)}</span>
                  </div>
                  <div style={{ fontSize: 13, marginBottom: 4 }}>
                    <b>正确答案：</b><span style={{ color: '#2e7d32' }}>{answerDisplayOf(judge)}</span>
                  </div>
                  {analysis && (
                    <div style={{ fontSize: 13, marginBottom: 6, padding: 8, background: '#fffbe6', borderRadius: 6 }}>
                      <b>📝 解析：</b><span style={{ whiteSpace: 'pre-wrap' }}>{analysis}</span>
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

          {/* ============ 交卷制：强制错因标注弹窗 ============ */}
          {isSubmissionFlow && causeItems.length > 0 && (
            <Modal
              title={`交卷成功 · 请为 ${causeItems.length} 道错题选择错误原因`}
              open={causeOpen}
              maskClosable={false}
              keyboard={false}
              onCancel={() => {
                if (submitPhase === 'cause') message.warning('请先完成错题原因标注，才能查看本次结果');
              }}
              footer={[
                <Button key="view" type="primary" loading={realSubmitting} onClick={confirmCauses}>
                  保存并查看结果
                </Button>,
              ]}
              width={700}
            >
              <div style={{ color: '#8c8c8c', fontSize: 12, marginBottom: 12 }}>
                错题原因将用于学习掌握度总结与错题本归纳，请逐题选择（共 {causeItems.length} 题）
              </div>
              <div style={{ maxHeight: 440, overflowY: 'auto', paddingRight: 8 }}>
                {causeItems.map((it, idx) => {
                  const q = it.question;
                  if (!q) return null;
                  return (
                    <div key={it.detailId || it.questionId} style={{
                      border: causeDrafts[it.detailId] ? '1px solid #b7eb8f' : '1px solid #ffa39e',
                      borderRadius: 10, padding: 12, marginBottom: 10,
                      background: causeDrafts[it.detailId] ? '#fcfff8' : '#fff8f8',
                    }}>
                      <div style={{ fontWeight: 600, marginBottom: 6, fontSize: 14 }}>
                        {idx + 1}. {q.name || q.schema?.title || '（题目未加载）'}
                      </div>
                      <div style={{ fontSize: 13, marginBottom: 4 }}>
                        <b>我的答案：</b>
                        <span style={{ color: '#c62828' }}>{answerTextOf(q)}</span>
                      </div>
                      {it.correctAnswer && (
                        <div style={{ fontSize: 13, marginBottom: 8 }}>
                          <b>正确答案：</b>
                          <span style={{ color: '#2e7d32' }}>{prettyAnswer(it.correctAnswer)}</span>
                        </div>
                      )}
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <b style={{ fontSize: 13, flexShrink: 0 }}>错误原因：</b>
                        <Select
                          style={{ flex: 1 }} size="small" placeholder="请选择错误原因"
                          value={causeDrafts[it.detailId]}
                          onChange={(v) => setCauseDrafts((p) => ({ ...p, [it.detailId]: v }))}
                          options={WRONG_REASONS.map((r) => ({ value: r, label: r }))}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            </Modal>
          )}

          {/* ============ 交卷制：结果页（掌握总结 + 掌握变化 + 逐题回顾） ============ */}
          {isSubmissionFlow && submitPhase === 'result' && practiceResult && (
            <SubmissionResultView
              result={practiceResult}
              questions={realQuestions}
              answers={realAnswers}
              history={resultHistory}
              reasonOf={(item) => causeDrafts[item.detailId]}
              prettyAnswer={prettyAnswer}
              answerTextOf={answerTextOf}
              onRetry={retryPractice}
              onBack={() => navigate(kpIdParam || repoId ? '/student' : '/student/study')}
            />
          )}

          {/* 预习：后台配置的知识点讲解要点 */}
          {tab === 'preview' && (
            <div style={{ marginBottom: 12, fontWeight: 600 }}>📖 知识点讲解（预习/复习）</div>
          )}
          {tab === 'preview' && (
            realPoints === null ? <div>加载中…</div> : realPoints.length === 0 ? (
              <div className="knowledge-empty">该小节暂未配置知识点，请联系管理员</div>
            ) : (
              realPoints.map((p) => {
                let content = null;
                try { content = p.content ? JSON.parse(p.content) : null; } catch { content = null; }
                return (
                  <div key={p.id} style={{ border: '1px solid #e3f2fd', borderRadius: 12, padding: 14, marginBottom: 12, background: '#f8fcff' }}>
                    <div style={{ fontWeight: 600, marginBottom: 8 }}>🌊 {p.name}</div>
                    {p.imageUrl && <img src={p.imageUrl} alt={p.name} style={{ maxWidth: '100%', borderRadius: 8, marginBottom: 8 }} />}
                    {(content?.points || []).map((pt, i) => (
                      <div key={i} style={{ color: '#455a64', marginBottom: 4 }}>• {pt}</div>
                    ))}
                    {(!content?.points || content.points.length === 0) && (
                      <div style={{ color: '#90a4ae' }}>该知识点暂未配置讲解要点</div>
                    )}
                  </div>
                );
              })
            )
          )}

          {/* 预习：知识点讲解 + 例题展示（tabs 切换） */}
          {tab === 'preview' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {realPoints === null || realPoints.length === 0 ? (
                <div className="knowledge-empty">该小节暂未配置知识点</div>
              ) : (
                realPoints.slice(0, 2).map((p, i) => (
                  <div
                    key={p.id}
                    style={{
                      border: '1px solid #e3f2fd', borderRadius: 12, padding: 20, background: '#f8fcff',
                      cursor: 'pointer', minHeight: 80, display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}
                    onClick={() => {}}
                  >
                    <div style={{ fontWeight: 600, fontSize: 16, color: '#1890ff' }}>🌊 {p.name || `知识点${i + 1}`}</div>
                  </div>
                ))
              )}
              <Button
                type="primary"
                size="large"
                onClick={() => navigate(`/student/knowledge?sectionId=${sectionId}&tab=preview_practice`)}
                style={{ width: 180, height: 44, borderRadius: 8, fontSize: 15, marginTop: 8, alignSelf: 'center' }}
              >
                 例题练习
              </Button>
            </div>
          )}

          {/* 专项练习湾 / 试炼检测：逐题模式（每题一页 + 答题指示器；交卷制结果页时隐藏作答区） */}
          {(tab === 'practice' || tab === 'trial' || tab === 'example' || tab === 'preview_practice')
            && (!isSubmissionFlow || submitPhase !== 'result') && (
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
                      <div style={{ border: '1px solid #e3f2fd', borderRadius: 12, padding: 16, background: '#f8fcff' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
                          <span style={{ fontWeight: 700 }}>第 {currentQ + 1} / {realQuestions.length} 题</span>
                          <span style={{ color: '#90a4ae', fontSize: 13 }}>{tab === 'practice' ? '专项练习湾' : tab === 'example' ? '知识点例题' : tab === 'preview_practice' ? '知识点预习' : tab === 'preview' ? '预习练习' : '小节通关'}</span>
                        </div>
                        <div style={{ fontWeight: 600, marginBottom: 12, fontSize: 15 }}>{question.name || schema.title}</div>
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
                        })() : options.map((opt) => {
                          const selected = picked?.type === 'option'
                            ? picked.optionId === opt.id
                            : (picked?.optionIds || []).includes(opt.id);
                          const showRight = showResult && correct === 1 && selected;
                          const showWrong = showResult && correct === 0 && selected;
                          return (
                            <div key={opt.id} onClick={() => realPick(question, opt.id)}
                              style={{ padding: '10px 14px', marginBottom: 8, borderRadius: 8, cursor: showResult ? 'default' : 'pointer',
                                border: selected ? '2px solid #29b6f6' : '1px solid #e0e0e0',
                                background: showRight ? '#e8f5e9' : showWrong ? '#ffebee' : selected ? '#e1f5fe' : '#fff' }}>
                              {multi ? (selected ? '☑ ' : '☐ ') : (selected ? '● ' : '○ ')}{opt.title}
                            </div>
                          );
                        })}
                        {/* 答案与解析 */}
                        {showResult && (
                          <div style={{ marginTop: 12, fontSize: 13 }}>
                            {(() => {
                              if (correct !== 1 && judge?.blankTotal > 1 && (judge.blankRight || 0) > 0) {
                                // 多项填空部分命中：橙色提示答对空数，不再一律红叉
                                return (
                                  <div>
                                    <div style={{ color: '#e65100', fontWeight: 600 }}>
                                      ⚠️ 部分正确：答对 {judge.blankRight}/{judge.blankTotal} 空
                                    </div>
                                    <div style={{ color: '#2e7d32', marginTop: 4 }}>
                                      标准答案：{answerDisplayOf(judge)}
                                    </div>
                                  </div>
                                );
                              }
                              if (correct === 1) {
                                return <div style={{ color: '#2e7d32', fontWeight: 600 }}>✅ 回答正确</div>;
                              }
                              return <div style={{ color: '#c62828', fontWeight: 600 }}>❌ 回答错误 · 标准答案：{answerDisplayOf(judge)}</div>;
                            })()}
                            {analysis && (
                              <div style={{ marginTop: 6, padding: 10, background: '#fffbe6', borderRadius: 8 }}>
                                <b style={{ color: '#b26a00' }}>📝 解析：</b>
                                <div style={{ whiteSpace: 'pre-wrap' }}>{analysis}</div>
                              </div>
                            )}
                          </div>
                        )}
                        {/* 提交答案按钮：旧即时判分流专用；交卷制不逐题判分 */}
                        {!isSubmissionFlow && !showResult && picked && (
                          <Button type="primary" size="small" style={{ marginTop: 12 }} onClick={() => setJudgeState((p) => ({ ...p, [question.id]: true }))}>
                            提交答案
                          </Button>
                        )}
                        {/* 导航 */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 14 }}>
                          <button className="knowledge-back" disabled={currentQ === 0} onClick={() => setCurrentQ((c) => c - 1)}>上一题</button>
                          <div style={{ fontSize: 13, color: '#90a4ae' }}>
                            {isSubmissionFlow
                              ? `已作答 ${realQuestions.filter((q) => isAnswered(q)).length}/${realQuestions.length} 题`
                              : `已判定 ${Object.keys(judgeState).length}/${realQuestions.length} 题`}
                          </div>
                          {currentQ < realQuestions.length - 1 ? (
                            <button className="knowledge-back" onClick={() => setCurrentQ((c) => c + 1)}>下一题</button>
                          ) : isSubmissionFlow ? (
                            <Button
                              type="primary"
                              size="small"
                              disabled={realSubmitting || submitPhase !== 'idle'}
                              onClick={requestSubmitPractice}
                            >
                              {realSubmitting ? '提交中…' : (tab === 'practice' ? '提交练习' : '提交通关')}
                            </Button>
                          ) : (
                            realQuestions.every((q) => judgeState[q.id]) && tab !== 'preview' ? (
                              <button className="knowledge-back" onClick={realSubmit} disabled={realSubmitting}>
                                {realSubmitting ? '提交中…' : '完成'}
                              </button>
                            ) : (
                              <span />
                            )
                          )}
                        </div>
                        {!isSubmissionFlow && !showResult && (
                          <div style={{ marginTop: 8, fontSize: 12, color: '#b26a00' }}>
                            选择题/填空作答后，点击「提交答案」才会判定
                          </div>
                        )}
                        {isSubmissionFlow && (
                          <div style={{ marginTop: 8, fontSize: 12, color: '#90a4ae' }}>
                            作答过程中不判分，完成全部题目后点击「提交练习」统一判分；未作答题目按答错处理
                          </div>
                        )}
                        {!isSubmissionFlow && currentQ === realQuestions.length - 1 && realQuestions.every((q) => judgeState[q.id]) && (
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
                              </div>
                            );
                          })()
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
          {/* 错题本：真实错题（/api/practice/wrong-list）后续接入 */}
          {tab === 'wrong' && (
            <div className="knowledge-empty">错题本功能建设中，练习错题已自动记录</div>
          )}
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
