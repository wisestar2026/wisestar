/**
 * EnglishPracticeResult.jsx - 英语练习结果区（单元练习 / 阅读理解共用）
 *
 * 功能:
 *   1. 只展示「得分 + 历史最高分 + 上次得分 + 错题数」与逐题解析，不展示诊断报告维度分析
 *   2. 支持「再做一次」「查看上次作答」「去错题本」
 *   3. 「查看上次作答」按需拉取上一次记录并在弹窗中回放
 *
 * 被谁引用: pages/student/EnglishUnitPracticePage.jsx、pages/student/EnglishReadingPage.jsx
 */

import { useState } from 'react';
import { Button, Modal, Progress, Spin, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import QuestionPreview from '../practice/QuestionPreview';
import RichContent from '../common/RichContent';

/** 单题解析行（本次结果与上次回放共用） */
function ResultItem({ item, index }) {
  const state = item.correct === 1 ? 'is-right' : item.correct === 0 ? 'is-wrong' : '';
  const flag = item.correct === 1 ? '答对' : item.correct === 0 ? '答错' : '未判分';
  return (
    <div className={`kd-detail-item ${state}`}>
      <div className="kd-detail-head">
        <span className="kd-detail-index">{index + 1}</span>
        <span className={`kd-detail-flag ${item.correct === 1 ? 'ok' : 'bad'}`}>{flag}</span>
        {item.knowledgePoint && <span className="kd-detail-kp">{item.knowledgePoint}</span>}
      </div>
      <div className="kd-detail-body">
        {item.question ? (
          <div className="kd-detail-question">
            <div className="kd-detail-question-label">原题</div>
            <QuestionPreview question={item.question} />
          </div>
        ) : item.stem ? (
          <div className="kd-detail-question">
            <div className="kd-detail-question-label">原题</div>
            <div>{item.stem}</div>
          </div>
        ) : null}
        <div className="kd-detail-line">
          <span className="kd-detail-label">正确答案：</span>
          <span className="kd-detail-correct">{item.correctAnswer || '（无）'}</span>
        </div>
        <div className="kd-detail-line">
          <span className="kd-detail-label">你的答案：</span>
          <span className={item.correct === 1 ? 'kd-detail-correct' : 'kd-detail-wrong'}>
            {item.studentAnswer || '（未作答）'}
          </span>
        </div>
        {item.analysis && (
          <div className="kd-detail-analysis">
            <b>解析：</b><RichContent text={item.analysis} />
          </div>
        )}
      </div>
    </div>
  );
}

/** 上次作答弹窗（结果页与阅读理解页共用） */
export function PracticeRecordModal({ open, onCancel, record, loading }) {
  return (
    <Modal title="上次作答" open={open} onCancel={onCancel} footer={null} width={720}>
      {loading ? (
        <div style={{ padding: 32, textAlign: 'center' }}><Spin /></div>
      ) : !record ? (
        <div className="kd-dim-empty">还没有更早的作答记录</div>
      ) : (
        <div>
          <div className="kd-report-line">
            得分 <b>{record.score}</b>
            <span className="kd-report-hint">
              {' '}· 答对 {record.correct}/{record.total}
              {' '}· {record.createAt ? String(record.createAt).replace('T', ' ').slice(0, 16) : ''}
            </span>
          </div>
          <div className="kd-detail-list">
            {(record.items || []).map((item, index) => (
              <ResultItem item={item} index={index} key={item.questionId || index} />
            ))}
          </div>
        </div>
      )}
    </Modal>
  );
}

export default function EnglishPracticeResult({
  title,
  summary,
  items = [],
  onRetry,
  onWrongBook,
  onBack,
  backText = '返回学习中心',
  extra,
  loadLast,
}) {
  const [lastOpen, setLastOpen] = useState(false);
  const [lastLoading, setLastLoading] = useState(false);
  const [lastRecord, setLastRecord] = useState(null);

  const score = summary?.score ?? summary?.lastScore ?? 0;
  const attempted = summary?.attempts || 0;

  const openLast = async () => {
    if (!loadLast) return;
    setLastOpen(true);
    if (lastRecord) return;
    setLastLoading(true);
    try {
      const res = await loadLast();
      setLastRecord(res?.data || null);
    } catch (e) {
      message.error(e?.message || '加载上次作答失败');
    } finally {
      setLastLoading(false);
    }
  };

  return (
    <div className="kd-wrap sll-page-enter eng-stage">
      <div className="kd-report-hero">
        <Progress
          type="circle"
          percent={score}
          width={120}
          strokeColor={score >= 80 ? '#34c759' : score >= 60 ? '#2196f3' : '#ff7043'}
          format={(p) => <span className="kd-report-percent">{p}<small>%</small></span>}
        />
        <div className="kd-report-summary">
          <div className="kd-report-title">{title}</div>
          <div className="kd-report-line">
            最高分 <b>{summary?.bestScore ?? score}</b>
            <span className="kd-report-hint">
              {summary?.previousScore != null ? ` · 上次 ${summary.previousScore} 分` : ' · 首次练习'}
              {attempted > 0 ? ` · 累计 ${attempted} 次` : ''}
            </span>
          </div>
          <div className="kd-report-line">
            错题本待消灭 <b>{summary?.wrongCount ?? 0}</b> 题
            <span className="kd-report-hint">（做错的题已自动收进英语错题本）</span>
          </div>
          <div className="kd-report-line kd-report-hint">
            {score >= 80 ? '这次很稳，换个单元或做一次诊断卷试试' : '对照下面的解析，把错题在错题本里重做一遍'}
          </div>
        </div>
      </div>

      <div className="kd-card">
        <div className="kd-card-head">
          <span className="kd-card-title">逐题解析</span>
          <span className="kd-dim-tip">本次作答 {items.length} 题</span>
        </div>
        <div className="kd-detail-list">
          {items.map((item, index) => (
            <ResultItem item={item} index={index} key={item.questionId || index} />
          ))}
          {!items.length && <div className="kd-dim-empty">暂无作答明细</div>}
        </div>
      </div>

      <div className="kd-actions">
        {onRetry && (
          <Button size="large" icon={<ReloadOutlined />} onClick={onRetry}>再做一次</Button>
        )}
        {loadLast && (
          <Button size="large" onClick={openLast}>查看上次作答</Button>
        )}
        {onWrongBook && (
          <Button size="large" onClick={onWrongBook}>去错题本</Button>
        )}
        {extra}
        {onBack && (
          <Button type="primary" size="large" onClick={onBack}>{backText}</Button>
        )}
      </div>

      <PracticeRecordModal
        open={lastOpen}
        onCancel={() => setLastOpen(false)}
        record={lastRecord}
        loading={lastLoading}
      />
    </div>
  );
}
