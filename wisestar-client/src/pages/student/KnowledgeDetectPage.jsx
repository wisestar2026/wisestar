/**
 * KnowledgeDetectPage.jsx - 学员端「知识点检测」
 *
 * 功能（覆盖全部学科，入口在学员首页左侧按钮列）:
 *   1. 配置：在当前学科/年级/册别下勾选单元、题量、难度，自动组卷
 *   2. 作答：逐题作答（套卷模式，不即时判题），可前后翻题、跳题
 *   3. 报告：交卷后展示正确率、单元维度统计、薄弱知识点与逐题解析
 *
 * 定位：检测为诊断性质，后端不发放学习币/积分，也不写入练习记录，
 *       因此不会污染学习进度与奖励结算。
 *
 * URL: /student/detect
 * 被谁引用: App.jsx 路由表；入口来自学员首页 StudentHomePage 左侧按钮列
 *
 * 数据流:
 *   GET  /api/student/detect/units    可选单元（subjectId/grade/term）
 *   POST /api/student/detect/generate 自动组卷（无答案）
 *   POST /api/student/detect/submit   交卷生成诊断报告
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Button, Checkbox, Empty, Progress, Radio, Spin, Tag, message,
} from 'antd';
import { ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons';
import useStudentStore from '../../stores/useStudentStore';
import { getDetectUnits, generateDetectPaper, submitDetectPaper } from '../../api/detect';
import QuestionCard from '../../components/practice/QuestionCard';
import RichContent from '../../components/common/RichContent';
import { formatCorrectAnswers } from '../../utils/practiceHelpers';
import './KnowledgeDetectPage.css';

const COUNT_OPTIONS = [5, 10, 15, 20];
const DIFFICULTY_OPTIONS = [
  { label: '不限', value: '' },
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' },
];

export default function KnowledgeDetectPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const presetUnit = searchParams.get('unit') || '';
  const activeSubject = useStudentStore((s) => s.activeSubject);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);
  const version = useStudentStore((s) => s.version);

  // 步骤：config 配置 → answer 作答 → report 报告
  const [step, setStep] = useState('config');

  // ---- 配置态 ----
  const [units, setUnits] = useState([]);
  const [unitsLoading, setUnitsLoading] = useState(false);
  const [selectedUnits, setSelectedUnits] = useState([]);
  const [count, setCount] = useState(10);
  const [difficulty, setDifficulty] = useState('');

  // ---- 作答态 ----
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [current, setCurrent] = useState(0);
  const [generating, setGenerating] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // ---- 报告态 ----
  const [report, setReport] = useState(null);

  // 加载可选单元
  const loadUnits = useCallback(() => {
    if (!activeSubject) {
      setUnits([]);
      return;
    }
    setUnitsLoading(true);
    getDetectUnits({ subjectId: activeSubject, grade, term })
      .then((res) => {
        const list = res?.data || [];
        setUnits(list);
        // 入口携带 ?unit= 时预选同名单元（英语学习中心「单元练习」跳转）
        if (presetUnit) {
          const matched = list.find((u) => u.name === presetUnit);
          if (matched) setSelectedUnits([matched.id]);
        }
      })
      .catch(() => message.error('单元加载失败'))
      .finally(() => setUnitsLoading(false));
  }, [activeSubject, grade, term, presetUnit]);

  useEffect(() => {
    loadUnits();
  }, [loadUnits]);

  // 切换年级/册别/学科后清空已选单元，避免越界
  useEffect(() => {
    setSelectedUnits([]);
  }, [activeSubject, grade, term]);

  const totalQuestions = useMemo(
    () => units.filter((u) => selectedUnits.includes(u.id))
      .reduce((sum, u) => sum + (u.questionCount || 0), 0),
    [units, selectedUnits],
  );

  // ---- 开始检测：自动组卷 ----
  const handleStart = async () => {
    if (selectedUnits.length === 0) {
      message.warning('请至少勾选一个单元');
      return;
    }
    setGenerating(true);
    try {
      const res = await generateDetectPaper({
        subjectId: activeSubject,
        grade,
        term,
        chapterIds: selectedUnits,
        questionCount: count,
        difficulty: difficulty || undefined,
      });
      const list = res?.data || [];
      if (list.length === 0) {
        message.warning('所选条件下暂无题目，请调整单元或难度');
        return;
      }
      setQuestions(list);
      setAnswers({});
      setCurrent(0);
      setReport(null);
      setStep('answer');
    } catch (e) {
      message.error(e?.message || '组卷失败，请稍后重试');
    } finally {
      setGenerating(false);
    }
  };

  // ---- 交卷 ----
  const handleSubmit = async () => {
    const items = questions.map((q) => ({
      questionId: q.id,
      answer: answers[q.id] || {},
    }));
    setSubmitting(true);
    try {
      const res = await submitDetectPaper({ items });
      setReport(res?.data || null);
      setStep('report');
      message.success('检测完成，已生成诊断报告');
    } catch (e) {
      message.error(e?.message || '交卷失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  };

  // ============================ 渲染：配置 ============================
  if (step === 'config') {
    return (
      <div className="kd-wrap sll-page-enter">
        <div className="kd-hero">
          <div className="kd-hero-emoji">🎯</div>
          <div>
            <div className="kd-hero-title">知识点检测</div>
            <div className="kd-hero-sub">
              在当前学科与册别下勾选单元，系统自动组卷，测出你的薄弱知识点
            </div>
          </div>
        </div>

        <div className="kd-context">
          <Tag color="blue">{version || '教材'}</Tag>
          <Tag color="geekblue">{grade}</Tag>
          <Tag color="cyan">{term}</Tag>
          <span className="kd-context-tip">如需切换，请回到上一页顶部调整</span>
        </div>

        <div className="kd-card">
          <div className="kd-card-head">
            <span className="kd-step-dot">1</span>
            <span className="kd-card-title">选择检测单元</span>
            <span className="kd-card-extra">
              <Button type="link" size="small" onClick={() => setSelectedUnits(units.map((u) => u.id))}>
                全选
              </Button>
              <Button type="link" size="small" onClick={() => setSelectedUnits([])}>
                清空
              </Button>
            </span>
          </div>
          {unitsLoading ? (
            <div className="kd-loading"><Spin /></div>
          ) : units.length === 0 ? (
            <Empty
              description={`${grade}${term}暂无可用检测单元，请尝试切换年级或册别`}
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          ) : (
            <Checkbox.Group
              value={selectedUnits}
              onChange={(vals) => setSelectedUnits(vals)}
              className="kd-unit-grid"
            >
              {units.map((u) => (
                <Checkbox key={u.id} value={u.id} className="kd-unit-item">
                  <span className="kd-unit-name">{u.name}</span>
                  <span className="kd-unit-count">{u.questionCount} 题</span>
                </Checkbox>
              ))}
            </Checkbox.Group>
          )}
        </div>

        <div className="kd-card">
          <div className="kd-card-head">
            <span className="kd-step-dot">2</span>
            <span className="kd-card-title">设置题量与难度</span>
            {totalQuestions > 0 && (
              <span className="kd-card-extra kd-extra-text">所选单元共 {totalQuestions} 题</span>
            )}
          </div>
          <div className="kd-field">
            <span className="kd-field-label">题量</span>
            <Radio.Group
              value={count}
              onChange={(e) => setCount(e.target.value)}
              optionType="button"
              buttonStyle="solid"
              options={COUNT_OPTIONS.map((n) => ({ label: `${n} 题`, value: n }))}
            />
          </div>
          <div className="kd-field">
            <span className="kd-field-label">难度</span>
            <Radio.Group
              value={difficulty}
              onChange={(e) => setDifficulty(e.target.value)}
              optionType="button"
              buttonStyle="solid"
              options={DIFFICULTY_OPTIONS}
            />
          </div>
        </div>

        <div className="kd-actions">
          <Button type="primary" size="large" icon={<ThunderboltOutlined />} loading={generating} onClick={handleStart}>
            开始检测
          </Button>
        </div>
      </div>
    );
  }

  // ============================ 渲染：作答 ============================
  if (step === 'answer') {
    const q = questions[current];
    const answeredCount = questions.filter((item) => {
      const a = answers[item.id];
      return a && ((a.type === 'option' && a.optionId)
        || (a.type === 'options' && a.optionIds?.length > 0)
        || (a.type === 'text' && String(a.text || '').trim()));
    }).length;
    return (
      <div className="kd-wrap kd-answer-wrap sll-page-enter">
        <div className="kd-answer-top">
          <div className="kd-answer-title">
            <span className="kd-answer-emoji">🎯</span>
            知识点检测
          </div>
          <div className="kd-answer-meta">
            已作答 {answeredCount} / {questions.length}
          </div>
        </div>
        <Progress
          percent={Math.round((answeredCount / questions.length) * 100)}
          showInfo={false}
          strokeColor="#2f8ae0"
          style={{ marginBottom: 16 }}
        />

        {/* 题号导航 */}
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
            onChange={(v) => setAnswers((prev) => ({ ...prev, [q.id]: v }))}
            judgeMode={false}
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
              交卷查看报告
            </Button>
          )}
        </div>
        <div className="kd-answer-foot">
          <Button type="link" onClick={handleSubmit} loading={submitting}>
            提前交卷
          </Button>
        </div>
      </div>
    );
  }

  // ============================ 渲染：报告 ============================
  const rep = report || { total: 0, correct: 0, accuracy: 0, chapterStats: [], weakPoints: [], details: [] };
  return (
    <div className="kd-wrap sll-page-enter">
      <div className="kd-report-hero">
        <Progress
          type="circle"
          percent={rep.accuracy}
          width={120}
          strokeColor={rep.accuracy >= 80 ? '#34c759' : rep.accuracy >= 60 ? '#2196f3' : '#ff7043'}
          format={(p) => <span className="kd-report-percent">{p}<small>%</small></span>}
        />
        <div className="kd-report-summary">
          <div className="kd-report-title">
            检测完成
            {rep.detectType === 'PRE' && <Tag color="gold" style={{ marginLeft: 8 }}>学前检测 · 已定格基线</Tag>}
            {rep.detectType === 'STAGE' && <Tag color="blue" style={{ marginLeft: 8 }}>阶段检测</Tag>}
          </div>
          <div className="kd-report-line">
            答对 <b>{rep.correct}</b> / {rep.total} 题
          </div>
          {rep.baseline && (
            <div className="kd-report-line kd-report-hint">
              本次为学期首次全面检测，已作为成长基线。此后薄弱点逐个攻克即可看到成长对比。
            </div>
          )}
          <div className="kd-report-line kd-report-hint">
            {rep.accuracy >= 80 ? '掌握不错，继续保持！' : rep.accuracy >= 60 ? '还有提升空间，针对薄弱点再练一练' : '基础需巩固，建议回到单元重点重新学习'}
          </div>
        </div>
      </div>

      {rep.chapterStats?.length > 0 && (
        <div className="kd-card">
          <div className="kd-card-head">
            <span className="kd-card-title">单元得分</span>
          </div>
          <div className="kd-chapter-list">
            {rep.chapterStats.map((c) => (
              <div key={c.name} className="kd-chapter-row">
                <span className="kd-chapter-name">{c.name}</span>
                <div className="kd-chapter-bar">
                  <Progress
                    percent={c.accuracy}
                    showInfo={false}
                    strokeColor={c.accuracy >= 80 ? '#34c759' : c.accuracy >= 60 ? '#2196f3' : '#ff7043'}
                    size="small"
                  />
                </div>
                <span className="kd-chapter-stat">{c.correct}/{c.total}（{c.accuracy}%）</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="kd-card">
        <div className="kd-card-head">
          <span className="kd-card-title">薄弱知识点</span>
          {rep.weakPoints?.length === 0 && <span className="kd-card-extra kd-extra-text">太棒了，没有薄弱点</span>}
        </div>
        {rep.weakPoints?.length > 0 ? (
          <div className="kd-weak-list">
            {rep.weakPoints.map((w) => (
              <div key={`${w.chapter}-${w.name}`} className="kd-weak-item">
                <Tag color="red">错 {w.wrong}/{w.total}</Tag>
                <span className="kd-weak-name">{w.name}</span>
                <span className="kd-weak-chapter">{w.chapter}</span>
              </div>
            ))}
          </div>
        ) : (
          <div className="kd-weak-empty">本次检测没有发现薄弱知识点，继续保持！</div>
        )}
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
                <span className={`kd-detail-flag ${d.correct === 1 ? 'ok' : 'bad'}`}>
                  {d.correct === 1 ? '答对' : d.correct === 0 ? '答错' : '未判分'}
                </span>
                <span className="kd-detail-kp">{d.knowledgePoint}</span>
              </div>
              <div className="kd-detail-body">
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
        <Button size="large" icon={<ReloadOutlined />} onClick={() => { setStep('config'); setReport(null); }}>
          再测一次
        </Button>
        <Button type="primary" size="large" onClick={() => navigate('/student')}>
          返回首页
        </Button>
      </div>
    </div>
  );
}
