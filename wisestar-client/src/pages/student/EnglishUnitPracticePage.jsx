/**
 * EnglishUnitPracticePage.jsx - 学员端英语「单元练习」（仅本单元题目）
 *
 * 功能:
 *   1. 进入某单元的单元练习：只组本单元题目（不提供单元选择，杜绝跨单元串题）
 *   2. 逐题作答（套卷模式），交卷后只展示得分与逐题解析（诊断报告仅在「学习诊断」页产出）
 *
 * URL: /student/english/unit-practice?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心单元卡片「单元练习」
 *
 * 数据流:
 *   GET  /api/student/detect/units          找到同名单元章节 ID
 *   POST /api/english/student/unit-paper        内容包阶梯组卷（知识点 D1→D3，剥离答案）
 *   POST /api/english/student/unit-paper/submit 交卷判分并生成报告
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button, Empty, Progress, Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getDetectUnits } from '../../api/detect';
import {
  getEnglishPracticeLast,
  getEnglishPracticeSummary,
  getEnglishUnitPaper,
  submitEnglishUnitPaper,
} from '../../api/englishStudent';
import QuestionCard from '../../components/practice/QuestionCard';
import EnglishPracticeResult from '../../components/student/EnglishPracticeResult';
import HandRaiseButton from '../../components/student/HandRaiseButton';
import { formatCorrectAnswers } from '../../utils/practiceHelpers';
import './KnowledgeDetectPage.css';
import './EnglishCenterPage.css';

export default function EnglishUnitPracticePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);
  const version = useStudentStore((s) => s.version);
  const activeSubject = useStudentStore((s) => s.activeSubject);
  const subjectId = searchParams.get('subjectId') || activeSubject || '1003';

  // 步骤：loading 加载 → answer 作答 → report 报告 → empty 无题
  const [step, setStep] = useState('loading');
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [changes, setChanges] = useState({});
  const [current, setCurrent] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [report, setReport] = useState(null);
  const [summary, setSummary] = useState(null);
  const [chapterId, setChapterId] = useState('');

  // 组卷：找到同名单元 → 仅该单元生成题目
  const loadPaper = useCallback(async () => {
    if (!unit) {
      setStep('empty');
      return;
    }
    setStep('loading');
    try {
      const unitsRes = await getDetectUnits({ subjectId, grade, term });
      const target = (unitsRes?.data || []).find((u) => u.name === unit);
      if (!target) {
        setStep('empty');
        return;
      }
      const genRes = await getEnglishUnitPaper({
        version,
        grade,
        term,
        unit,
        chapterId: target.id,
        questionCount: 20,
      });
      const list = genRes?.data || [];
      if (list.length === 0) {
        setStep('empty');
        return;
      }
      setChapterId(target.id);
      setQuestions(list);
      setAnswers({});
      setChanges({});
      setCurrent(0);
      setReport(null);
      setSummary(null);
      setStep('answer');
    } catch (e) {
      message.error(e?.message || '组卷失败，请稍后重试');
      setStep('empty');
    }
  }, [unit, subjectId, grade, term, version]);

  useEffect(() => {
    loadPaper();
  }, [loadPaper]);

  const answeredCount = useMemo(
    () => questions.filter((item) => {
      const a = answers[item.id];
      return a && ((a.type === 'option' && a.optionId)
        || (a.type === 'options' && a.optionIds?.length > 0)
        || (a.type === 'text' && String(a.text || '').trim()));
    }).length,
    [questions, answers],
  );

  // 记录选项改选次数：交给后端做「掌握 / 不稳定 / 漏洞」三态判定
  const handleAnswerChange = (id, value) => {
    const before = answers[id];
    if (value?.type === 'option' && before?.type === 'option'
      && before.optionId && before.optionId !== value.optionId) {
      setChanges((prev) => ({ ...prev, [id]: (prev[id] || 0) + 1 }));
    }
    setAnswers((prev) => ({ ...prev, [id]: value }));
  };

  const handleSubmit = async () => {
    const items = questions.map((q) => ({
      contentKey: q.id,
      answer: answers[q.id] || {},
      changes: changes[q.id] || 0,
    }));
    setSubmitting(true);
    try {
      // 带上学科/年级/册别，便于检测记录归属英语学科（复习错题据此聚合）
      const res = await submitEnglishUnitPaper({
        version, grade, term, unit, chapterId: chapterId || undefined, module: 'unit', items,
      });
      setReport(res?.data || null);
      setStep('report');
      // 成绩概览单独取：最高分、上次得分、错题数与诊断报告分开维护
      getEnglishPracticeSummary({ module: 'unit', unit })
        .then((info) => setSummary(info?.data || null))
        .catch(() => setSummary(null));
      message.success('单元练习完成');
    } catch (e) {
      message.error(e?.message || '交卷失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  };

  // ============================ 加载 / 无题 ============================
  if (step === 'loading') {
    return <div className="kd-wrap sll-page-enter eng-stage"><div className="kd-loading"><Spin /></div></div>;
  }

  if (step === 'empty') {
    return (
      <div className="kd-wrap sll-page-enter eng-stage">
        <Empty
          description={unit ? `${unit} 暂无可用单元练习，先去学习单词和句子吧` : '未选择单元'}
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
        <div className="kd-actions">
          <Button onClick={() => navigate('/student/english/review')}>返回复习</Button>
          <Button type="primary" onClick={() => navigate('/student/english')}>返回学习中心</Button>
        </div>
      </div>
    );
  }

  // ============================ 作答 ============================
  if (step === 'answer') {
    const q = questions[current];
    return (
      <div className="kd-wrap kd-answer-wrap sll-page-enter eng-stage">
        <div className="kd-answer-top">
          <div className="kd-answer-title">
            <span className="kd-answer-emoji">📘</span>
            单元练习 · {unit}
          </div>
          <div className="kd-answer-meta">已作答 {answeredCount} / {questions.length}</div>
        </div>
        <Progress
          percent={Math.round((answeredCount / questions.length) * 100)}
          showInfo={false}
          strokeColor="#2f8ae0"
          style={{ marginBottom: 16 }}
        />

        <div className="kd-nav-strip">
          {questions.map((item, idx) => {
            const a = answers[item.id];
            const done = a && ((a.type === 'option' && a.optionId)
              || (a.type === 'options' && a.optionIds?.length > 0)
              || (a.type === 'text' && String(a.text || '').trim()));
            return (
              <button
                key={item.id}
                type="button"
                className={`kd-nav-dot ${idx === current ? 'is-current' : ''} ${done ? 'is-done' : ''}`}
                onClick={() => setCurrent(idx)}
              >
                {idx + 1}
              </button>
            );
          })}
        </div>

        <div className="kd-question-box">
          <QuestionCard
            question={{ ...q, template: q.schema }}
            index={current + 1}
            total={questions.length}
            value={answers[q.id]}
            onChange={(v) => handleAnswerChange(q.id, v)}
            judgeMode={false}
            keyboardDefaultTab="letter"
          />
        </div>

        <div className="kd-answer-actions">
          <Button size="large" disabled={current === 0} onClick={() => setCurrent((c) => c - 1)}>
            上一题
          </Button>
          {current < questions.length - 1 ? (
            <Button type="primary" size="large" onClick={() => setCurrent((c) => c + 1)}>
              下一题
            </Button>
          ) : (
            <Button type="primary" size="large" loading={submitting} onClick={handleSubmit}>
              交卷查看结果
            </Button>
          )}
        </div>
        <div className="kd-answer-foot">
          <Button type="link" onClick={handleSubmit} loading={submitting}>提前交卷</Button>
        </div>
        <HandRaiseButton
          question={q}
          answer={answers[q.id]}
          page="英语单元练习"
          sectionId={chapterId || undefined}
          knowledgePointName={q?.tag?.[0]}
          subject="英语"
          chapter={unit}
        />
      </div>
    );
  }

  // ============================ 结果（只展示题目与解析） ============================
  const rep = report || { total: 0, correct: 0, accuracy: 0, details: [] };
  const items = (rep.details || []).map((d) => ({
    questionId: d.questionId,
    knowledgePoint: d.knowledgePoint,
    studentAnswer: d.studentAnswer,
    correctAnswer: formatCorrectAnswers(d.questionType, d.correctAnswers),
    correct: d.correct,
    analysis: d.analysis,
    question: d.question,
  }));
  const resultSummary = summary || {
    score: rep.accuracy,
    bestScore: rep.accuracy,
    previousScore: null,
    attempts: 1,
    wrongCount: items.filter((item) => item.correct === 0).length,
  };

  return (
    <EnglishPracticeResult
      title={`单元练习完成 · ${unit}`}
      summary={resultSummary}
      items={items}
      onRetry={loadPaper}
      onWrongBook={() => navigate('/student/english/review')}
      onBack={() => navigate('/student/english')}
      loadLast={() => getEnglishPracticeLast({
        module: 'unit', unit, excludeId: resultSummary.recordId || undefined,
      })}
    />
  );
}
