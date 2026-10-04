/**
 * StudentMyArchivePage.jsx - 学员端「我的学习档案」（只读，可打印）
 *
 * 功能:
 *   1. 本学期学习目标与承诺（老师填写）
 *   2. 学习知识点：初始检测薄弱点 + 当前薄弱点（含掌握度）
 *   3. 当日学习情况（当日总结 + 数据）
 *   4. 上课记录 / 学习日志时间线
 *   5. 学期报告
 *
 * URL: /student/archive
 * 被谁引用: App.jsx 路由表；首页左侧入口「学习档案」
 *
 * 数据流: getMyArchive() → 档案详情（后端按当前登录学员返回）
 */

import { useEffect, useState } from 'react';
import { Button } from 'antd';
import { PrinterOutlined } from '@ant-design/icons';
import { getMyArchive } from '../../api/archive';
import './StudentMyArchivePage.css';

export default function StudentMyArchivePage() {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMyArchive()
      .then((res) => setDetail(res?.data || null))
      .catch(() => setDetail(null))
      .finally(() => setLoading(false));
  }, []);

  const today = detail?.today;

  return (
    <div className="sll-page-enter my-archive-page">
      <div className="my-archive-head">
        <div>
          <h2 className="my-archive-title">我的学习档案</h2>
          <div className="my-archive-meta">
            {detail?.studentName || ''} · {detail?.termLabel ? `${detail.schoolYear} 学年 ${detail.termLabel}` : ''}
            {detail?.teacherName ? ` · 负责老师 ${detail.teacherName}` : ''}
          </div>
        </div>
        <Button className="no-print" icon={<PrinterOutlined />} onClick={() => window.print()} loading={loading}>
          打印
        </Button>
      </div>

      {/* 一、学习目标 + 承诺（合并为一张卡） */}
      <div className="sll-card my-archive-section">
        <h3 className="my-archive-section-title">本学期学习目标</h3>
        <div className="my-archive-text">
          {detail?.goalPlan || <span className="my-archive-empty">老师暂未填写学习目标</span>}
        </div>
        {detail?.promise && (
          <div className="my-archive-text my-archive-promise">
            <span className="my-archive-promise-label">我的承诺：</span>
            {detail.promise}
          </div>
        )}
      </div>

      {/* 二、学习知识点 */}
      <div className="sll-card my-archive-section">
        <h3 className="my-archive-section-title">学习知识点</h3>
        <div className="my-archive-meta" style={{ marginBottom: 8 }}>检测初始薄弱点</div>
        <div className="my-archive-tags">
          {detail?.initialWeakPoints?.length
            ? detail.initialWeakPoints.map((n) => <span key={n} className="my-archive-chip">{n}</span>)
            : <span className="my-archive-empty">暂无检测记录</span>}
        </div>
        <div className="my-archive-meta" style={{ margin: '14px 0 8px' }}>当前薄弱点</div>
        <div className="my-archive-tags">
          {detail?.weakPoints?.length
            ? detail.weakPoints.map((w) => (
              <span key={w.kpId} className="my-archive-chip">{w.name || w.kpId} · 掌握度 {w.mastery}%</span>
            ))
            : <span className="my-archive-empty">暂无薄弱知识点，继续保持！</span>}
        </div>
      </div>

      {/* 三、当日情况 */}
      <div className="sll-card my-archive-section">
        <h3 className="my-archive-section-title">当日学习情况</h3>
        {today ? (
          <>
            <div className="my-archive-text">{today.content}</div>
            <div className="my-archive-meta" style={{ marginTop: 10 }}>
              在线 {Math.round((today.durationMs || 0) / 60000)} 分钟 ·
              练习 {today.practiceCount || 0} 次 ·
              答题 {today.questionCount || 0} 题 ·
              答对 {today.correctCount || 0} 题 ·
              正确率 {today.accuracy || 0}% ·
              覆盖知识点 {today.knowledgeCount || 0} 个 ·
              积分 +{today.points || 0} · 学币 +{today.coins || 0}
            </div>
          </>
        ) : (
          <span className="my-archive-empty">今日暂无学习记录</span>
        )}
      </div>

      {/* 四、上课记录 */}
      <div className="sll-card my-archive-section">
        <h3 className="my-archive-section-title">上课记录 / 学习日志</h3>
        {detail?.records?.length ? detail.records.map((r) => (
          <div key={r.id} className="my-archive-record">
            <span className="my-archive-record-date">{r.recordDate}</span>
            <span className="my-archive-record-title">{r.title || '学习记录'}</span>
            <div className="my-archive-record-body">{r.studySummary || '（无详情）'}</div>
            {r.strengthenedKps && <div className="my-archive-meta">强化知识点：{r.strengthenedKps}</div>}
            {r.homework && <div className="my-archive-meta">课后作业：{r.homework}</div>}
            {r.teacherComment && <div className="my-archive-meta">教师寄语：{r.teacherComment}</div>}
          </div>
        )) : <span className="my-archive-empty">暂无上课记录</span>}
      </div>

      {/* 五、学期报告 */}
      <div className="sll-card my-archive-section">
        <h3 className="my-archive-section-title">学期报告</h3>
        <div className="my-archive-text">
          {detail?.reportContent || <span className="my-archive-empty">学期报告尚未生成</span>}
        </div>
      </div>
    </div>
  );
}
