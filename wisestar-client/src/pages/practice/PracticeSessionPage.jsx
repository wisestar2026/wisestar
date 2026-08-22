/**
 * PracticeSessionPage.jsx - 练习答题页（三种模式完整交互）
 *
 * 功能:
 *   1. 读取 URL 参数: mode（练习模式）+ ids（勾选的题目 id 列表）
 *   2. 按 id 批量加载题目
 *   3. 一题一屏答题交互:
 *      - 专项刷题 (practice): 两步作答——先选答（多选可累积、可修改），点「确认提交」后才判题锁定
 *      - 套卷模拟 (exam): 卷首确认页（题量/总分/限时/题型分布/考试须知）→ 点「开始作答」启动全局倒计时
 *        → 整卷顺序作答 + 可答题卡跳题，到点自动交卷，交卷后统一判分
 *      - 随机练习 (random): 题目随机排序，与专项相同两步作答
 *   4. 答题卡（题号网格跳题，区分已确认/已答未确认/未答）+ 顶部进度条
 *   5. 交卷: 未确认题目按当前答案自动判分 + 二次确认 → 结果页
 *
 * URL: /practice/session?mode=practice&ids=id1,id2,id3
 * 从哪进入: PracticeHomePage.handleStart（勾选题目后点击「开始练习」）
 *
 * 数据流:
 *   挂载 → 解析 mode/ids → getTemplate 逐个加载 → 随机模式打乱顺序
 *   → 作答（answers 记录选择，不判题）→ 确认提交（专项/随机，判题写入 confirmed 并锁定）
 *   → 交卷（未确认题按答案自动判分）→ 结果页（calculateScore 计算得分）
 *
 * 判分依据:
 *   evaluateAnswer（utils/practiceHelpers.js）——标准答案取题目 attribute.examCorrectAnswer，
 *   与后端 AnswerServiceImpl 判分规则一致（多选集合相等、其余文本等值）
 */

import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import {
  Card, Button, Space, Typography, Progress, Modal, message, Empty, Tag, Drawer,
} from 'antd';
import {
  ArrowLeftOutlined, CheckCircleOutlined, CloseCircleOutlined,
  ReloadOutlined, AppstoreOutlined, ClockCircleOutlined,
} from '@ant-design/icons';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { getTemplate } from '../../api/template';
import { submitPractice } from '../../api/practice';
import QuestionCard from '../../components/practice/QuestionCard';
import AnswerSheet from '../../components/practice/AnswerSheet';
import ExamIntro from '../../components/practice/ExamIntro';
import { evaluateAnswer, calculateScore } from '../../utils/practiceHelpers';

const { Title, Text } = Typography;

// 模式中文名映射
const MODE_LABELS = {
  practice: '专项刷题',
  exam: '套卷模拟',
  random: '随机练习',
};

// 题型中文映射（结果页展示用）
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Select: '下拉题',
  FillBlank: '填空题', Text: '多行文本', Score: '评分题',
  Remark: '备注说明', Judge: '判断题',
};

// 套卷模拟默认时长: 每题 60 秒（可后续改为可配置）
const EXAM_SECONDS_PER_QUESTION = 60;

export default function PracticeSessionPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const mode = searchParams.get('mode') || 'practice'; // 练习模式
  // 模式判定（必须先于 useState 定义，避免 const 暂时性死区）
  const isExam = mode === 'exam';   // 套卷模拟
  const isRandom = mode === 'random'; // 随机练习
  // 来源练习 ID（从练习整库练习时带上，落库记录来源；可空）
  const repoId = searchParams.get('repoId') || undefined;
  // 题目 id 数组（useMemo 缓存，避免每次渲染生成新数组导致 loadQuestions 无限重触发）
  const ids = useMemo(
    () => (searchParams.get('ids') || '').split(',').filter(Boolean),
    [searchParams.get('ids')],
  );

  // ---- 题目数据 ----
  const [questions, setQuestions] = useState([]);   // 已加载题目（随机模式已打乱）
  const [loading, setLoading] = useState(true);     // 加载标记

  // ---- 答题状态 ----
  const [current, setCurrent] = useState(0);        // 当前题号（索引 0 起）
  const [answers, setAnswers] = useState({});       // 已选答案: { [questionId]: {type, ...} }（确认前可改）
  const [confirmed, setConfirmed] = useState({});   // 已确认判题结果: { [questionId]: {correct, correctAnswers} }（专项/随机）
  const [submitted, setSubmitted] = useState(false); // 是否已交卷（显示结果页）
  const [sheetOpen, setSheetOpen] = useState(false);  // 答题卡抽屉
  const [started, setStarted] = useState(!isExam);    // 是否已开始作答（套卷模式需在卷首确认后启动倒计时）

  // ---- 套卷倒计时 ----
  const [secondsLeft, setSecondsLeft] = useState(null); // 剩余秒数（套卷模式）
  const timerRef = useRef(null);

  // ---- 练习计时（落库用） ----
  const startRef = useRef(null);   // 开始作答时间戳
  const submitRef = useRef(false); // 交卷落库防重复标记（StrictMode 双调用保护）

  // 开始作答时记录开始时间（专项/随机初始即 started；套卷在卷首确认后 started=true）
  useEffect(() => {
    if (started && !submitted && !startRef.current) {
      startRef.current = Date.now();
    }
  }, [started, submitted]);

  // ---- 加载题目 ----
  const loadQuestions = useCallback(async () => {
    if (ids.length === 0) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const loaded = [];
      for (const id of ids) {
        const res = await getTemplate({ id });
        if (res?.data) loaded.push(res.data);
      }
      // 随机练习: 打乱题目顺序
      if (isRandom) {
        for (let i = loaded.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1));
          [loaded[i], loaded[j]] = [loaded[j], loaded[i]];
        }
      }
      setQuestions(loaded);
    } catch {
      message.error('加载题目失败');
    } finally {
      setLoading(false);
    }
  }, [ids, isRandom]);

  useEffect(() => { loadQuestions(); }, [loadQuestions]);

  // ---- 套卷倒计时（套卷模式 + 开始作答后 + 题目加载完成 + 未交卷时启动） ----
  useEffect(() => {
    if (!isExam || !started || questions.length === 0 || submitted) return;
    const total = questions.length * EXAM_SECONDS_PER_QUESTION;
    setSecondsLeft(total);
    timerRef.current = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timerRef.current);
          // 到点自动交卷
          setTimeout(() => handleSubmit(true), 0);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isExam, started, questions.length, submitted]);

  // ---- 格式化倒计时 mm:ss ----
  const formatTime = (sec) => {
    if (sec == null) return '--:--';
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  // ---- 单题答案变更（仅记录选择，不判题；确认提交后才判题） ----
  const handleAnswerChange = (value) => {
    const q = questions[current];
    if (!q) return;
    setAnswers((prev) => ({ ...prev, [q.id]: value }));
  };

  // ---- 确认提交（专项/随机模式）: 对当前题判题并锁定 ----
  const handleConfirm = () => {
    const q = questions[current];
    if (!q) return;
    const result = evaluateAnswer(q, answers[q.id]);
    setConfirmed((prev) => ({ ...prev, [q.id]: result }));
  };

  // ---- 已答题号集合（1 起，用于答题卡着色；已选答案即算） ----
  const answeredSet = useMemo(() => {
    const set = new Set();
    questions.forEach((q, i) => {
      if (answers[q.id] && isAnswered(answers[q.id])) set.add(i + 1);
    });
    return set;
  }, [questions, answers]); // eslint-disable-line react-hooks/exhaustive-deps

  // ---- 已确认判题题号集合（1 起，专项/随机模式） ----
  const confirmedSet = useMemo(() => {
    const set = new Set();
    questions.forEach((q, i) => {
      if (confirmed[q.id]) set.add(i + 1);
    });
    return set;
  }, [questions, confirmed]);

  // 判断某题的答案是否有效填写
  function isAnswered(ans) {
    if (!ans) return false;
    if (ans.type === 'option') return !!ans.optionId;
    if (ans.type === 'options') return ans.optionIds?.length > 0;
    if (ans.type === 'text') return !!String(ans.text || '').trim();
    return false;
  }

  // ---- 交卷 ----
  // 计算每题判分结果（套卷模式此时统一判分；专项/随机复用 confirmed，未确认题按当前答案自动判分）
  const finalResults = useMemo(() => {
    if (!submitted) return [];
    return questions.map((q) => {
      const saved = confirmed[q.id];
      if (saved) return { question: q, result: saved };
      return { question: q, result: evaluateAnswer(q, answers[q.id]) };
    });
  }, [submitted, questions, confirmed, answers]);

  const summary = useMemo(() => {
    if (!submitted) return null;
    return calculateScore(finalResults);
  }, [submitted, finalResults]);

  // ---- 交卷处理 ----
  const handleSubmit = (auto = false) => {
    // 非自动交卷时做未答确认
    if (!auto) {
      const unanswered = questions.length - answeredSet.size;
      // 专项/随机模式: 已答未确认的题数（交卷会按当前答案自动判分）
      const unconfirmed = confirmedSet.size < answeredSet.size
        ? answeredSet.size - confirmedSet.size : 0;
      const tip = [];
      if (unanswered > 0) tip.push(`还有 ${unanswered} 题未作答`);
      if (!isExam && unconfirmed > 0) tip.push(`${unconfirmed} 题已作答但未确认，将按当前答案自动判分`);
      Modal.confirm({
        title: '确定交卷？',
        content: tip.length ? tip.join('，') : '交卷后将显示本次练习结果',
        okText: '确定交卷',
        cancelText: '继续作答',
        onOk: () => setSubmitted(true),
      });
      return;
    }
    setSubmitted(true);
  };

  // ---- 交卷后落库（练习会话 + 逐题明细/错题标记） ----
  // 后端按 questionId 回源题目复核判分；提交失败不阻断结果页展示，仅提示
  useEffect(() => {
    if (!submitted || submitRef.current) return;
    submitRef.current = true;
    const durationMs = startRef.current ? Date.now() - startRef.current : 0;
    const modeSubmit = isExam ? 'exam' : isRandom ? 'random' : 'special';
    const items = questions.map((q) => ({ questionId: q.id, answer: answers[q.id] }));
    submitPractice({ mode: modeSubmit, repoId, durationMs, items })
      .then(() => {
        /* 落库成功，结果页照常展示，不打扰用户 */
      })
      .catch(() => {
        message.warning('本次练习记录保存失败，不影响本次结果');
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [submitted]);

  // ---- 再来一组 ----
  const handleRestart = () => {
    navigate('/practice');
  };

  // ---- 结果页 ----
  if (submitted && summary) {
    return (
      <div style={{ padding: 24, maxWidth: 800, margin: '0 auto' }}>
        <Card>
          {/* 结果统计区 */}
          <Title level={4} style={{ textAlign: 'center' }}>练习完成</Title>
          <div style={{ display: 'flex', justifyContent: 'center', gap: 40, margin: '24px 0' }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 36, fontWeight: 'bold', color: '#1677ff' }}>{summary.score}</div>
              <Text type="secondary">得分 / {summary.totalScore}</Text>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 36, fontWeight: 'bold', color: '#52c41a' }}>{summary.accuracy}%</div>
              <Text type="secondary">正确率</Text>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 36, fontWeight: 'bold', color: '#722ed1' }}>{summary.correctCount}</div>
              <Text type="secondary">答对</Text>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 36, fontWeight: 'bold', color: '#ff4d4f' }}>{summary.wrongCount}</div>
              <Text type="secondary">答错</Text>
            </div>
          </div>

          {/* 正确率进度条 */}
          <Progress percent={summary.accuracy} status={summary.accuracy >= 60 ? 'success' : 'exception'} />

          {/* 操作按钮 */}
          <div style={{ textAlign: 'center', marginTop: 24 }}>
            <Space>
              <Button size="large" icon={<ReloadOutlined />} onClick={handleRestart}>再来一组</Button>
              <Button size="large" type="primary" onClick={() => navigate('/practice')}>返回选题</Button>
            </Space>
          </div>
        </Card>

        {/* 逐题回顾 */}
        <Card title="题目回顾" style={{ marginTop: 16 }}>
          {finalResults.map(({ question, result }, idx) => {
            const qAttr = question?.template?.attribute || {};
            const isCorrect = result.correct === 1;
            const wrong = result.correct === 0;
            return (
              <div
                key={question.id}
                style={{
                  padding: '14px 16px', marginBottom: 12, borderRadius: 8,
                  border: `1px solid ${isCorrect ? '#b7eb8f' : wrong ? '#ffa39e' : '#d9d9d9'}`,
                  background: isCorrect ? '#f6ffed' : wrong ? '#fff2f0' : '#fafafa',
                }}
              >
                <Space style={{ marginBottom: 6 }}>
                  {isCorrect && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
                  {wrong && <CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
                  {result.correct === null && <Tag>未判分</Tag>}
                  <Text strong>{idx + 1}. {question?.name || question?.template?.title}</Text>
                  <Tag color="blue">{TYPE_LABELS[question?.questionType] || question?.questionType}</Tag>
                </Space>
                {wrong && result.correctAnswers.length > 0 && (
                  <div>
                    <Text type="secondary">正确答案：</Text>
                    <Text strong style={{ color: '#52c41a' }}>{result.correctAnswers.join('、')}</Text>
                  </div>
                )}
                {qAttr.examAnalysis && (
                  <Text type="secondary" style={{ display: 'block', marginTop: 4 }}>
                    解析：{qAttr.examAnalysis}
                  </Text>
                )}
              </div>
            );
          })}
        </Card>
      </div>
    );
  }

  // ---- 加载中 / 空数据 ----
  if (loading) {
    return <div style={{ padding: 48, textAlign: 'center' }}><Text type="secondary">题目加载中...</Text></div>;
  }

  if (questions.length === 0) {
    return (
      <div style={{ padding: 48 }}>
        <Card>
          <Empty description="未找到题目，请返回重新选题">
            <Button type="primary" onClick={() => navigate('/practice')}>返回选题</Button>
          </Empty>
        </Card>
      </div>
    );
  }

  // ---- 卷面总分（每题取 examScore，无则按 1 分） ----
  const totalScore = questions.reduce((sum, q) => {
    const s = Number(q?.template?.attribute?.examScore);
    return sum + (Number.isFinite(s) && s > 0 ? s : 1);
  }, 0);

  // ---- 套卷卷首确认页：开始作答后才进入答题、启动倒计时 ----
  if (isExam && !started) {
    return (
      <ExamIntro
        questions={questions}
        totalSeconds={questions.length * EXAM_SECONDS_PER_QUESTION}
        totalScore={totalScore}
        onStart={() => setStarted(true)}
        onBack={() => navigate('/practice')}
      />
    );
  }

  const q = questions[current];

  // ---- 答题区（一题一屏） ----
  return (
    <div style={{ padding: 24, maxWidth: 860, margin: '0 auto' }}>
      <Card>
        {/* ---- 顶部工具条 ---- */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/practice')}>退出</Button>
            <Title level={5} style={{ margin: 0 }}>{MODE_LABELS[mode]}</Title>
            {isExam && (
              <Tag
                icon={<ClockCircleOutlined />}
                color={secondsLeft <= 60 ? 'red' : 'blue'}
                style={{ fontSize: 14 }}
              >
                {formatTime(secondsLeft)}
              </Tag>
            )}
          </Space>
          <Button icon={<AppstoreOutlined />} onClick={() => setSheetOpen(true)}>
            答题卡 {isExam ? answeredSet.size : confirmedSet.size}/{questions.length}
          </Button>
        </div>

        {/* ---- 顶部进度条 ---- */}
        <Progress
          percent={Math.round(((current + 1) / questions.length) * 100)}
          showInfo={false}
          style={{ marginBottom: 24 }}
        />

        {/* ---- 当前题 ---- */}
        <QuestionCard
          question={q}
          index={current + 1}
          total={questions.length}
          value={answers[q.id]}
          onChange={handleAnswerChange}
          judgeMode={!isExam}
          confirmed={confirmed}
          onConfirm={handleConfirm}
        />

        {/* ---- 底部导航 ---- */}
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 32, borderTop: '1px solid #f0f0f0', paddingTop: 20 }}>
          <Button disabled={current === 0} onClick={() => setCurrent(current - 1)}>上一题</Button>
          <Space>
            {isExam && (
              <Button type="primary" danger onClick={() => handleSubmit(false)}>交卷</Button>
            )}
            {current < questions.length - 1 ? (
              <Button type="primary" onClick={() => setCurrent(current + 1)}>
                下一题
              </Button>
            ) : (
              <Button type="primary" onClick={() => handleSubmit(false)}>
                {isExam ? '交卷' : '完成并交卷'}
              </Button>
            )}
          </Space>
        </div>
      </Card>

      {/* ---- 答题卡抽屉 ---- */}
      <Drawer
        title={`答题卡（${isExam ? answeredSet.size : confirmedSet.size}/${questions.length}）`}
        open={sheetOpen}
        onClose={() => setSheetOpen(false)}
        width={300}
      >
        <AnswerSheet
          total={questions.length}
          current={current + 1}
          answeredSet={answeredSet}
          confirmedSet={confirmedSet}
          isExam={isExam}
          onJump={(idx) => { setCurrent(idx); setSheetOpen(false); }}
        />
        <div style={{ marginTop: 24 }}>
          <Button type="primary" block onClick={() => { setSheetOpen(false); handleSubmit(false); }}>
            交卷
          </Button>
        </div>
      </Drawer>
    </div>
  );
}
