/**
 * EnglishUnitPracticePage.jsx - 学员端英语「单元练习」（仅本单元题目）
 *
 * 功能:
 *   1. 进入某单元的单元练习：只组本单元题目（不提供单元选择，杜绝跨单元串题）
 *   2. 逐题作答（套卷模式），交卷后生成得分与逐题解析
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
import { ReloadOutlined } from '@ant-design/icons';
import useStudentStore from '../../stores/useStudentStore';
import { getDetectUnits } from '../../api/detect';
import { getEnglishUnitPaper, submitEnglishUnitPaper } from '../../api/englishStudent';
import QuestionCard from '../../components/practice/QuestionCard';
import QuestionPreview from '../../components/practice/QuestionPreview';
import RichContent from '../../components/common/RichContent';
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
        version, grade, term, unit, chapterId: chapterId || undefined, items,
      });
      setReport(res?.data || null);
      setStep('report');
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
      </div>
    );
  }

  // ============================ 报告 ============================
  const rep = report || { total: 0, correct: 0, accuracy: 0, details: [] };
  return (
    <div className="kd-wrap sll-page-enter eng-stage">
      <div className="kd-report-hero">
        <Progress
          type="circle"
          percent={rep.accuracy}
          width={120}
          strokeColor={rep.accuracy >= 80 ? '#34c759' : rep.accuracy >= 60 ? '#2196f3' : '#ff7043'}
          format={(p) => <span className="kd-report-percent">{p}<small>%</small></span>}
        />
        <div className="kd-report-summary">
          <div className="kd-report-title">单元练习完成 · {unit}</div>
          <div className="kd-report-line">答对 <b>{rep.correct}</b> / {rep.total} 题</div>
          <div className="kd-report-line">
            掌握 <b>{rep.mastered ?? 0}</b> · 不稳定 <b>{rep.unstable ?? 0}</b> · 漏洞 <b>{rep.flawed ?? 0}</b>
            <span className="kd-report-hint">（加权得分 {rep.masteryScore ?? rep.accuracy}）</span>
          </div>
          <div className="kd-report-line kd-report-hint">
            {rep.accuracy >= 60 ? '不错，继续保持' : '把错题对应的小节再练一遍吧'}
          </div>
        </div>
      </div>

      <div className="kd-card">
        <div className="kd-card-head">
          <span className="kd-card-title">逐题解析</span>
        </div>
        <div className="kd-detail-list">
          {rep.details?.map((d, idx) => (
            <div key={d.questionId} className={`kd-detail-item ${d.correct === 1 ? 'is-right' : 'is-wrong'}`}>
              <div className="kd-detail-head">
                <span className="kd-detail-index">{idx + 1}</span>
                <span className={`kd-detail-flag ${d.mastery === '掌握' ? 'ok' : d.mastery === '不稳定' ? 'warn' : d.mastery === '漏洞' ? 'bad' : d.correct === 1 ? 'ok' : 'bad'}`}>
                  {d.mastery || (d.correct === 1 ? '答对' : d.correct === 0 ? '答错' : '未判分')}
                </span>
                <span className="kd-detail-kp">{d.knowledgePoint}</span>
              </div>
              <div className="kd-detail-body">
                {d.question && (
                  <div className="kd-detail-question">
                    <div className="kd-detail-question-label">原题</div>
                    <QuestionPreview question={d.question} />
                  </div>
                )}
                <div className="kd-detail-line">
                  <span className="kd-detail-label">正确答案：</span>
                  <span className="kd-detail-correct">
                    {formatCorrectAnswers(d.questionType, d.correctAnswers) || '（无）'}
                  </span>
                </div>
                <div className="kd-detail-line">
                  <span className="kd-detail-label">你的答案：</span>
                  <span className={d.correct === 1 ? 'kd-detail-correct' : 'kd-detail-wrong'}>
                    {d.studentAnswer || '（未作答）'}
                  </span>
                </div>
                {d.analysis && (
                  <div className="kd-detail-analysis">
                    <b>解析：</b><RichContent text={d.analysis} />
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="kd-actions">
        <Button size="large" icon={<ReloadOutlined />} onClick={loadPaper}>再练一次</Button>
        <Button size="large" onClick={() => navigate('/student/english/review')}>返回复习</Button>
        <Button type="primary" size="large" onClick={() => navigate('/student/english')}>返回学习中心</Button>
      </div>
    </div>
  );
}
