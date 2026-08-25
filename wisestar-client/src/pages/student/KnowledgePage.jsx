/**
 * KnowledgePage.jsx - 知识点详情页（学海智习系统 V2.0）
 * 模式（?tab=）: preview 预习 / practice 练习 / trial 试炼 / wrong 错题
 *
 * 视觉规范: 大面积留白低干扰；题目选项全部大圆角卡片（摒弃单选框）；
 *           提交后逐题标记对错；顶部轻柔奖励提示 1.5s 自动消失（无弹窗）
 *
 * 奖励体系（基础首学奖励，7 天重复刷题无奖励——前端 mock 演示）:
 *   预习 币+5 积分+3 / 练习 币+12 积分+6 / 试炼 币+20 积分+10 / 错题订正 币+8 积分+4
 *   试炼正确率≥90% 额外 币+15 积分+8
 *
 * 被谁引用: App.jsx（/student/knowledge/:kpId）、研习页右栏四大按钮
 * 依赖: react-router-dom(useParams/useSearchParams/useNavigate)、useStudentStore、./KnowledgePage.css
 */

import { useEffect, useMemo, useState } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import useStudentStore, { SUBJECTS, masteryLevel, getQuestions, REWARDS } from '../../stores/useStudentStore';
import { getStudyPoints, getStudyQuestions, uploadActivity } from '../../api/student';
import { submitPractice } from '../../api/practice';
import './KnowledgePage.css';

// 四种模式 tab 配置
const TABS = [
  { key: 'preview',  label: '知识点预习', icon: '📖' },
  { key: 'practice', label: '专项练习湾', icon: '✏️' },
  { key: 'trial',    label: '试炼检测',   icon: '🎯' },
  { key: 'wrong',    label: '错题本',     icon: '📕' },
];

// 预习讲解内容（按知识点生成 mock 要点）
const PREVIEW_CONTENT = {
  c1k1: ['声母 23 个、韵母 24 个、整体认读音节 16 个', '韵母分为单韵母 a o e i u ü 与复韵母', '声调符号标在主要元音上，共 4 声'],
  c1k2: ['书写规则：从左到右、先横后竖、先撇后捺', '注意笔顺规范，避免倒插笔', '常用偏旁部首与结构（左右/上下/半包围）'],
  c1k3: ['形近字：字形相似、读音不同的字', '通过偏旁部首与字义区分易混字', '组词对比法记忆形近字'],
  c2k1: ['《静夜思》李白：床前明月光，疑是地上霜', '《春晓》孟浩然：春眠不觉晓，处处闻啼鸟', '《咏鹅》骆宾王：鹅鹅鹅，曲项向天歌'],
  c2k2: ['举头望明月，低头思故乡', '谁知盘中餐，粒粒皆辛苦', '两岸猿声啼不住，轻舟已过万重山'],
  c3k1: ['先通读全文，标出自然段', '找出每段中心句概括段落大意', '串联各段大意形成全文理解'],
  c3k2: ['比喻：像、好像、仿佛，把 A 比作 B', '拟人：把事物当成人来写，赋予人的动作情感', '排比：三个及以上结构相同的句子'],
  m1k1: ['进位加法：个位相加满十，向十位进 1', '口算技巧：凑十法快速计算', '竖式书写：相同数位对齐，从个位算起'],
  m1k2: ['退位减法：个位不够减，向十位借 1 当 10', '竖式书写：注意借位标记', '口算技巧：破十法快速计算'],
  m1k3: ['混合运算：从左到右依次计算', '有括号先算括号里面的', '加减混合注意运算顺序'],
  m2k1: ['三角形 3 条边 3 个角，长方形对边相等', '正方形四条边都相等', '圆：由曲线围成的封闭图形'],
  m2k2: ['长方体 6 个面，相对的面相同', '正方体 6 个面都是正方形', '圆柱上下两个圆面，球可以任意滚动'],
  m3k1: ['求总数用加法：部分 + 部分 = 总数', '求比多比少：大数 = 小数 + 相差数', '读题找关键词："一共"用加法'],
  m3k2: ['求剩余用减法：总数 - 部分 = 剩余', '求差：大数 - 小数 = 相差数', '读题找关键词："还剩"用减法'],
  e1k1: ['英语 26 个字母，5 个元音字母 A E I O U', '大写字母占上中两格，注意书写顺序', '字母歌辅助记忆字母顺序'],
  e1k2: ['元音字母在单词中的短音：a→/æ/ e→/e/ i→/ɪ/', '辅音字母一般发对应的辅音音', '拼读练习：c-a-t → cat'],
  e2k1: ['book 书、pen 笔、ruler 尺子、pencil 铅笔', 'desk 课桌、chair 椅子、bag 书包', '颜色：red 红、blue 蓝、green 绿'],
  e2k2: ['cat 猫、dog 狗、bird 鸟、fish 鱼', '动物单词注意复数形式', '结合图片记忆动物词汇'],
  e3k1: ['Hello! / Hi! 你好', 'How are you? I\'m fine, thank you.', 'Goodbye! / Bye! 再见'],
  e3k2: ['This is... 这是…（介绍他人）', 'I am... 我是…（自我介绍）', 'Nice to meet you. 很高兴认识你'],
};

const DEFAULT_PREVIEW = ['知识点核心概念讲解', '典型例题演示', '易错点提示'];

export default function KnowledgePage() {
  const { kpId } = useParams();
  const [searchParams] = useSearchParams();
  const tab = searchParams.get('tab') || 'preview';
  const sectionId = searchParams.get('sectionId'); // 小节练习入口
  const repoId = searchParams.get('repoId');         // 练习（题库）任务/直接练习入口
  const kpIdParam = searchParams.get('kpId');        // 知识点任务入口
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

  // 习题级上报：进入练习/试炼后上报「正在做哪道题」（供后台老师监控）
  useEffect(() => {
    if (!realMode || !realQuestions?.length) return;
    uploadActivity({
      page: `/student/knowledge?tab=${tab}`,
      sectionId: sectionId || undefined,
      questionId: realQuestions[0]?.id,
    }).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [realMode, tab, realQuestions]);

  useEffect(() => {
    if (!realMode) return;
    if (tab === 'preview' && sectionId) {
      getStudyPoints(sectionId).then((res) => setRealPoints(res?.data || [])).catch(() => setRealPoints([]));
    } else if (tab === 'practice' || tab === 'trial') {
      const params = { count: tab === 'trial' ? 2 : 3 };
      if (tab === 'trial') params.exposeAnswer = true; // 试炼实时出答案（本地即时判分）
      if (sectionId) params.sectionId = sectionId;
      if (repoId) params.repoId = repoId;
      if (kpIdParam) params.knowledgePointId = kpIdParam;
      getStudyQuestions(params)
        .then((res) => setRealQuestions(res?.data || []))
        .catch(() => setRealQuestions([]));
    }
    setRealAnswers({});
    setRealResult(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [realMode, tab, sectionId]);

  // 真实模式：试炼本地即时判分（题目带答案，比对选项文本）
  const realJudge = (q) => {
    const schema = q.schema || {};
    const answerText = schema.attribute?.examCorrectAnswer;
    if (!answerText) return null;
    const correctAnswers = answerText.split('\n').map((x) => x.trim()).filter(Boolean);
    const picked = realAnswers[q.id];
    if (!picked) return null;
    const titleOf = (id) => (schema.children || []).find((o) => o.id === id)?.title;
    const mine = picked.type === 'option'
      ? [titleOf(picked.optionId)]
      : (picked.optionIds || []).map(titleOf);
    const mineSet = new Set(mine.map((x) => String(x).trim()));
    const correctSet = new Set(correctAnswers);
    const isRight = mineSet.size === correctSet.size && [...mineSet].every((x) => correctSet.has(x));
    return { correct: isRight ? 1 : 0, answer: answerText };
  };

  // 真实模式：选择选项（按题型单选/多选；试炼选后即时判分锁定）
  const realPick = (q, optId) => {
    if (realResult) return;
    if (tab === 'trial' && realJudge(q)) return;
    const multi = q.questionType === 'Checkbox' || q.questionType === 'Multiple';
    setRealAnswers((prev) => {
      const cur = prev[q.id];
      if (!multi) return { ...prev, [q.id]: { type: 'option', optionId: optId } };
      const ids = cur?.type === 'options' ? cur.optionIds : [];
      return { ...prev, [q.id]: { type: 'options', optionIds: ids.includes(optId) ? ids.filter((x) => x !== optId) : [...ids, optId] } };
    });
  };

  // 真实模式：交卷（后端判分，返回对错 + 标准答案）
  const realSubmit = () => {
    if (realSubmitting || !realQuestions?.length) return;
    const items = realQuestions.map((q) => ({ questionId: q.id, answer: realAnswers[q.id] || null }));
    setRealSubmitting(true);
    submitPractice({ mode: tab, items, repoId: repoId || undefined, knowledgePointId: kpIdParam || undefined })
      .then((res) => setRealResult(res?.data || { items: [] }))
      .catch(() => setRealResult({ items: [], score: 0 }))
      .finally(() => setRealSubmitting(false));
  };

  // 真实模式：标准答案文本（题目 schema 中选项级答案标记在判分后由后端返回）
  const realCorrectOf = (qId) => {
    const item = (realResult?.items || []).find((i) => i.questionId === qId);
    return item ? item.correct : null;
  };
  const { pureMode, activeSubject } = useStudentStore();
  const subject = SUBJECTS.find((s) => s.key === activeSubject) || SUBJECTS[1];

  // 查找知识点（跨所有章节）
  const kp = useMemo(() => {
    for (const ch of subject.chapters) {
      const found = ch.kps.find((k) => k.id === kpId);
      if (found) return found;
    }
    return null;
  }, [subject, kpId]);

  // 题目池（练习取 3 题 / 试炼取 2 题）
  const questions = useMemo(() => getQuestions(kpId || ''), [kpId]);
  const activeQuestions = useMemo(
    () => (tab === 'trial' ? questions.slice(0, 2) : questions.slice(0, 3)),
    [questions, tab],
  );

  // 交互状态: 各题选择 / 是否已提交 / 顶部奖励提示
  const [answers, setAnswers] = useState({});
  const [submitted, setSubmitted] = useState(false);
  const [reward, setReward] = useState(null);

  // 切换 tab 时重置答题状态
  const switchTab = (t) => {
    setAnswers({});
    setSubmitted(false);
    setReward(null);
    navigate(`/student/knowledge/${kpId}?tab=${t.key}`);
  };

  // 选择选项（单选 / 多选）
  const pick = (qIndex, optKey) => {
    if (submitted) return;
    const q = activeQuestions[qIndex];
    setAnswers((prev) => {
      const cur = prev[qIndex] || [];
      if (q.type === 'radio') return { ...prev, [qIndex]: [optKey] };
      return {
        ...prev,
        [qIndex]: cur.includes(optKey) ? cur.filter((k) => k !== optKey) : [...cur, optKey],
      };
    });
  };

  // 判定单题对错
  const isRight = (qIndex) => {
    const q = activeQuestions[qIndex];
    if (!q) return false;
    const mine = answers[qIndex] || [];
    return q.answer.length === mine.length && q.answer.every((k) => mine.includes(k));
  };

  // 提交作答: 标记对错 + 顶部奖励提示
  const handleSubmit = () => {
    if (submitted) return;
    setSubmitted(true);
    const rightCount = activeQuestions.filter((_, i) => isRight(i)).length;
    const rate = Math.round((rightCount / activeQuestions.length) * 100);

    // 奖励（纯净模式不提示激励）
    if (!pureMode) {
      const base = REWARDS[tab] || REWARDS.practice;
      let text = `🎉 ${base.label}完成：币 +${base.coins} · 积分 +${base.points}`;
      if (tab === 'trial' && rate >= 90) {
        text += ' · 优质表现额外 币+15 积分+8';
      } else if (tab === 'practice' && rate === 100) {
        text += ' · 满分额外 币+5 积分+3';
      }
      setReward(text);
      setTimeout(() => setReward(null), 1600);
    }
  };

  // ============================================================
  // 真实模式渲染（后台配置内容）
  // ============================================================
  if (realMode) {
    return (
      <div className="sll-page-enter knowledge-page">
        <div className="sll-card" style={{ padding: 24, maxWidth: 720, margin: '0 auto' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ margin: 0 }}>
              {tab === 'preview' ? '📖 知识点预习' : tab === 'practice' ? '✏️ 专项练习湾' : tab === 'trial' ? '🎯 试炼检测' : '📕 知识点错题本'}
            </h3>
            <button className="knowledge-back" onClick={() => navigate(kpIdParam || repoId ? '/student' : '/student/study')}>返回</button>
          </div>

          {/* 预习：后台配置的知识点讲解要点 */}
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

          {/* 练习/试炼：真实题目 + 后端判分 */}
          {(tab === 'practice' || tab === 'trial') && (
            realQuestions === null ? <div>加载中…</div> : realQuestions.length === 0 ? (
              <div className="knowledge-empty">暂无可练习题目，请联系管理员配置练习/题目</div>
            ) : (
              <>
                {realQuestions.map((question, i) => {
                  const schema = question.schema || {};
                  const children = schema.children || [];
                  const multi = question.questionType === 'Checkbox' || question.questionType === 'Multiple';
                  const picked = realAnswers[question.id];
                  // 判定：试炼=本地实时判分；练习=交卷后后端判分
                  const judge = tab === 'trial' ? realJudge(question) : null;
                  const correct = tab === 'trial' ? (judge ? judge.correct : null) : realCorrectOf(question.id);
                  const answerText = tab === 'trial' ? (judge ? judge.answer : '') : (realResult?.items.find((x) => x.questionId === question.id)?.correctAnswer || '');
                  const judged = tab === 'trial' ? !!judge : !!realResult;
                  const isRightNow = correct === 1;
                  return (
                    <div key={question.id} style={{ border: '1px solid #e3f2fd', borderRadius: 12, padding: 14, marginBottom: 12, background: '#f8fcff' }}>
                      <div style={{ fontWeight: 600, marginBottom: 10 }}>
                        {i + 1}. {question.name || schema.title}
                      </div>
                      {children.map((opt) => {
                        const selected = picked?.type === 'option'
                          ? picked.optionId === opt.id
                          : (picked?.optionIds || []).includes(opt.id);
                        const showRight = realResult && isRightNow && selected;
                        const showWrong = realResult && correct === 0 && selected;
                        return (
                          <div
                            key={opt.id}
                            onClick={() => realPick(question, opt.id)}
                            style={{
                              padding: '8px 12px', marginBottom: 6, borderRadius: 8, cursor: judged ? 'default' : 'pointer',
                              border: selected ? '2px solid #29b6f6' : '1px solid #e0e0e0',
                              background: showRight ? '#e8f5e9' : showWrong ? '#ffebee' : selected ? '#e1f5fe' : '#fff',
                            }}
                          >
                            {multi ? (selected ? '☑ ' : '☐ ') : (selected ? '● ' : '○ ')}{opt.title || opt.id}
                          </div>
                        );
                      })}
                      {judged && (
                        <div style={{ marginTop: 8, fontSize: 13 }}>
                          {correct === 1 ? (
                            <span style={{ color: '#2e7d32' }}>✅ 回答正确</span>
                          ) : correct === 0 ? (
                            <span style={{ color: '#c62828' }}>❌ 回答错误 · 标准答案：{answerText || '—'}</span>
                          ) : (
                            <span style={{ color: '#90a4ae' }}>未作答</span>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
                {tab === 'trial' ? (
                  <div style={{ marginTop: 12, padding: 10, borderRadius: 10, background: '#fffbe6', textAlign: 'center', fontSize: 13, color: '#b26a00' }}>
                    试炼为实时判分：每题选择后立即显示对错与标准答案
                  </div>
                ) : !realResult ? (
                  <button className="knowledge-back" onClick={realSubmit} disabled={realSubmitting} style={{ marginTop: 8 }}>
                    {realSubmitting ? '提交中…' : '提交作答'}
                  </button>
                ) : (
                  <div style={{ marginTop: 12, padding: 12, borderRadius: 10, background: '#e8f5e9', textAlign: 'center' }}>
                    <div style={{ fontSize: 18, fontWeight: 700, color: '#2e7d32' }}>
                      🎉 得分 {realResult.score ?? 0} / {realResult.totalScore ?? 0} · 答对 {realResult.correctCount ?? 0}/{realResult.total ?? 0}
                    </div>
                    <button className="knowledge-back" onClick={() => { setRealResult(null); setRealAnswers({}); }} style={{ marginTop: 8 }}>
                      再练一次
                    </button>
                  </div>
                )}
                {tab === 'practice' && realResult && (
                  <div style={{ marginTop: 12 }}>
                    <div style={{ fontWeight: 600, marginBottom: 6 }}>📕 错题汇总（{realResult.items.filter((i) => i.correct === 0).length} 题）</div>
                    {realResult.items.filter((i) => i.correct === 0).length === 0 && (
                      <div style={{ color: '#2e7d32' }}>全部答对，无错题 🎉</div>
                    )}
                    {realResult.items.filter((i) => i.correct === 0).map((i) => {
                      const q = realQuestions.find((x) => x.id === i.questionId);
                      return (
                        <div key={i.questionId} style={{ padding: '8px 10px', marginBottom: 6, background: '#ffebee', borderRadius: 8, fontSize: 13 }}>
                          <b>{q?.name || i.questionId.slice(0, 8)}</b>
                          <div style={{ color: '#c62828' }}>标准答案：{i.correctAnswer || '—'}</div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </>
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
  if (!kp) {
    return (
      <div className="sll-page-enter knowledge-empty">
        <div className="sll-card knowledge-empty-card">
          <div className="knowledge-empty-icon">🌊</div>
          <div>未找到该知识点，请返回研习页重新选择</div>
          <button className="knowledge-back" onClick={() => navigate('/student/study')}>返回学海研习</button>
        </div>
      </div>
    );
  }

  const lv = masteryLevel(kp.mastery);
  const previewPoints = PREVIEW_CONTENT[kp.id] || DEFAULT_PREVIEW;

  return (
    <div className="sll-page-enter knowledge-page">
      {/* 顶部奖励提示（轻柔淡入，1.5s 自动消失） */}
      {reward && <div className="sll-reward">{reward}</div>}

      {/* 头部: 知识点信息 + 返回 */}
      <div className="knowledge-head">
        <button className="knowledge-back" onClick={() => navigate('/student/study')}>← 返回研习</button>
        <div className="knowledge-title">
          <span className="knowledge-title-icon">🌊</span>
          {kp.name}
          <span className="sll-level" style={{ background: lv.color }}>{lv.label}</span>
          <span className="knowledge-mastery">掌握度 {kp.mastery}%</span>
        </div>
        <div className="knowledge-desc">{kp.desc}</div>
      </div>

      {/* 模式 tab */}
      <div className="knowledge-tabs">
        {TABS.map((t) => (
          <button
            key={t.key}
            className={`knowledge-tab ${tab === t.key ? 'active' : ''}`}
            onClick={() => switchTab(t)}
          >
            <span>{t.icon}</span> {t.label}
          </button>
        ))}
      </div>

      {/* ---- 预习模式: 讲解卡片 ---- */}
      {tab === 'preview' && (
        <div className="sll-card knowledge-preview">
          <div className="knowledge-section-title">📖 知识点预习</div>
          {previewPoints.map((p, i) => (
            <div key={i} className="knowledge-point">
              <span className="knowledge-point-num">{i + 1}</span>
              <span>{p}</span>
            </div>
          ))}
          <div className="knowledge-preview-tip">
            💡 预习完成可获得 币+5 · 积分+3（同一知识点 7 天内仅奖励一次）
          </div>
        </div>
      )}

      {/* ---- 练习 / 试炼模式: 大圆角卡片选项 ---- */}
      {(tab === 'practice' || tab === 'trial') && (
        <div className="sll-card knowledge-quiz">
          <div className="knowledge-section-title">
            {tab === 'practice' ? '✏️ 专项练习湾' : '🎯 知识点试炼检测'}
            <span className="knowledge-quiz-sub">
              {tab === 'practice' ? `${activeQuestions.length} 题 · 完成 币+12 积分+6` : `${activeQuestions.length} 题 · 正确率≥90% 额外奖励`}
            </span>
          </div>
          {activeQuestions.map((q, qi) => {
            const right = submitted && isRight(qi);
            const wrong = submitted && !isRight(qi);
            return (
              <div key={qi} className={`knowledge-question ${right ? 'right' : ''} ${wrong ? 'wrong' : ''}`}>
                <div className="knowledge-q-head">
                  <span className="knowledge-q-no">第 {qi + 1} 题</span>
                  <span className="knowledge-q-type">{q.type === 'radio' ? '单选题' : '多选题'}</span>
                  {submitted && (
                    <span className={`knowledge-q-result ${right ? 'right' : 'wrong'}`}>
                      {right ? '✓ 回答正确' : '✗ 回答错误'}
                    </span>
                  )}
                </div>
                <div className="knowledge-q-text">{q.q}</div>
                <div className="knowledge-options">
                  {q.options.map((opt) => {
                    const selected = (answers[qi] || []).includes(opt.k);
                    const isAns = q.answer.includes(opt.k);
                    return (
                      <div
                        key={opt.k}
                        className={`knowledge-option ${selected ? 'selected' : ''} ${submitted && isAns ? 'answer' : ''}`}
                        onClick={() => pick(qi, opt.k)}
                      >
                        <span className="knowledge-opt-key">{opt.k.toUpperCase()}</span>
                        <span className="knowledge-opt-text">{opt.t}</span>
                        {submitted && isAns && <span className="knowledge-opt-mark">✓</span>}
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })}
          {/* 提交 / 重新作答 */}
          <div className="knowledge-submit-row">
            {!submitted ? (
              <button
                className="knowledge-submit-btn"
                disabled={Object.keys(answers).length < activeQuestions.length}
                onClick={handleSubmit}
              >
                提交作答
              </button>
            ) : (
              <>
                <div className="knowledge-result-summary">
                  {activeQuestions.filter((_, i) => isRight(i)).length} / {activeQuestions.length} 题正确
                </div>
                <button className="knowledge-retry-btn" onClick={() => { setAnswers({}); setSubmitted(false); }}>
                  再练一次
                </button>
              </>
            )}
          </div>
        </div>
      )}

      {/* ---- 错题本模式: 该知识点错题 + 重做 ---- */}
      {tab === 'wrong' && (
        <div className="sll-card knowledge-wrong">
          <div className="knowledge-section-title">📕 知识点错题本</div>
          {activeQuestions.map((q, qi) => (
            <div key={qi} className="knowledge-question wrong">
              <div className="knowledge-q-head">
                <span className="knowledge-q-no">错题 {qi + 1}</span>
                <span className="knowledge-q-type">曾答错</span>
              </div>
              <div className="knowledge-q-text">{q.q}</div>
              <div className="knowledge-options readonly">
                {q.options.map((opt) => {
                  const isAns = q.answer.includes(opt.k);
                  return (
                    <div key={opt.k} className={`knowledge-option ${isAns ? 'answer' : ''}`}>
                      <span className="knowledge-opt-key">{opt.k.toUpperCase()}</span>
                      <span className="knowledge-opt-text">{opt.t}</span>
                      {isAns && <span className="knowledge-opt-mark">✓</span>}
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
          <div className="knowledge-submit-row">
            <div className="knowledge-result-summary">全部订正可获得 币+8 · 积分+4</div>
            <button className="knowledge-retry-btn" onClick={() => switchTab(TABS[1])}>
              去练习重做
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
