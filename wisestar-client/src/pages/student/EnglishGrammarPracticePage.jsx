/**
 * EnglishGrammarPracticePage.jsx - 学员端英语语法练习
 *
 * 功能:
 *   1. 进入单元语法练习：只取本单元「语法」板块的融合题（由单元语法例句派生）
 *   2. 逐题作答（套卷模式），交卷后生成得分与逐题解析
 *   3. 按整体正确率回写该单元语法点掌握度（POST /english/student/grammar/record）
 *
 * URL: /student/english/grammar-practice?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语复习页「语法」分类与重点语法页
 *
 * 数据流:
 *   POST /api/english/student/unit-paper       只生成「语法」板块题目（无答案）
 *   POST /api/english/student/unit-paper/submit 交卷判分并生成报告
 *   GET  /api/english/student/grammars      单元语法点（写掌握度前取 ID）
 *   POST /api/english/student/grammar/record 记录语法作答 { grammarId, correct }
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button, Empty, Progress, Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import {
  getEnglishGrammars,
  getEnglishPracticeLast,
  getEnglishPracticeSummary,
  getEnglishUnitPaper,
  recordEnglishGrammar,
  submitEnglishUnitPaper,
} from '../../api/englishStudent';
import QuestionCard from '../../components/practice/QuestionCard';
import EnglishPracticeResult from '../../components/student/EnglishPracticeResult';
import HandRaiseButton from '../../components/student/HandRaiseButton';
import { formatCorrectAnswers } from '../../utils/practiceHelpers';
import './KnowledgeDetectPage.css';
import './EnglishCenterPage.css';

export default function EnglishGrammarPracticePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);
  const version = useStudentStore((s) => s.version);

  // 步骤：loading 加载 → answer 作答 → report 报告 → empty 无题
  const [step, setStep] = useState('loading');
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [current, setCurrent] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [report, setReport] = useState(null);
  const [summary, setSummary] = useState(null);

  // 组卷：找到同名单元 → 生成题目
  const loadPaper = useCallback(async () => {
    if (!unit) {
      setStep('empty');
      return;
    }
    setStep('loading');
    try {
      const genRes = await getEnglishUnitPaper({
        version,
        grade,
        term,
        unit,
        module: '语法',
        questionCount: 20,
      });
      const list = genRes?.data || [];
      if (list.length === 0) {
        setStep('empty');
        return;
      }
      setQuestions(list);
      setAnswers({});
      setCurrent(0);
      setReport(null);
      setSummary(null);
      setStep('answer');
    } catch (e) {
      message.error(e?.message || '组卷失败，请稍后重试');
      setStep('empty');
    }
  }, [unit, version, grade, term]);

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

  const handleSubmit = async () => {
    const items = questions.map((q) => ({ contentKey: q.id, answer: answers[q.id] || {} }));
    setSubmitting(true);
    try {
      const res = await submitEnglishUnitPaper({ version, grade, term, unit, module: 'grammar', items });
      const rep = res?.data || null;
      setReport(rep);
      setStep('report');
      getEnglishPracticeSummary({ module: 'grammar', unit })
        .then((info) => setSummary(info?.data || null))
        .catch(() => setSummary(null));
      // 按整体正确率回写单元语法点掌握度
      try {
        const gRes = await getEnglishGrammars({ version, grade, term, unit });
        const list = gRes?.data || [];
        const correct = (rep?.accuracy || 0) >= 60;
        await Promise.all(list.map((g) => recordEnglishGrammar({ grammarId: g.id, correct }).catch(() => {})));
        // 掌握度回写后由语法学习页展示，这里不再重复渲染报告
      } catch {
        // 掌握度回写失败不影响本次练习结果
      }
      message.success('语法练习完成');
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
          description={unit ? `${unit} 暂无可用语法练习，先去学习单词和句子吧` : '未选择单元'}
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
            语法练习 · {unit}
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

        <div className="kd-question-box quiz-stage">
          <QuestionCard
            question={{ ...q, template: q.schema }}
            index={current + 1}
            total={questions.length}
            value={answers[q.id]}
            onChange={(v) => setAnswers((prev) => ({ ...prev, [q.id]: v }))}
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
          page="英语语法练习"
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
      title={`语法练习完成 · ${unit}`}
      summary={resultSummary}
      items={items}
      onRetry={loadPaper}
      onWrongBook={() => navigate('/student/english/review')}
      onBack={() => navigate('/student/english')}
      loadLast={() => getEnglishPracticeLast({
        module: 'grammar', unit, excludeId: resultSummary.recordId || undefined,
      })}
    />
  );
}
