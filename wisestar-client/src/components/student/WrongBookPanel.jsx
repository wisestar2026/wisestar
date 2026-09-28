/**
 * WrongBookPanel.jsx - 学员错题本内容面板（可复用）
 *
 * 功能:
 *   1. 顶部统计区：知识点错题环形图 + 按知识点错题数据 + 按错题类型错题数据
 *   2. 页签「知识点错题」（章节 / 小节 / 错题卡片三栏）与「错误类型错题」（按题型分组卡片）
 *   3. 错题卡片：知识点、错误次数、题目、我的答案（最近一次）、正确答案 + 消灭错题
 *   4. 页内弹层消灭错题 / 消灭知识点（整组全对才移除，不跳转学习页）
 *
 * 被谁引用:
 *   - WrongBookPage.jsx（独立路由 /student/wrong）
 *   - KnowledgePage.jsx（知识点详情 ?tab=wrong）
 */

import { useEffect, useState, useCallback, useMemo } from 'react';
import { Tabs, Button, Tag } from 'antd';
import { AimOutlined, BulbOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { listWrongQuestions } from '../../api/practice';
import WrongDrillModal from '../../pages/student/WrongDrillModal';
import {
  typeLabel, formatTime, groupByKnowledgePoint, groupByChapter, groupBySection,
  filterBySection, buildWrongStats,
} from '../../utils/wrongBook';
import '../../pages/student/WrongBookPage.css';

const DONUT_COLORS = ['#4bb7e8', '#ff9f6b', '#7ed3a4', '#b08bff', '#ffc53d', '#ff7ba9', '#6bd3d3', '#a0b4c4'];

/** 知识点错题环形图（内联 SVG，无第三方依赖） */
function WrongDonut({ data, total }) {
  const radius = 54;
  const circumference = 2 * Math.PI * radius;
  let offset = 0;
  return (
    <svg viewBox="0 0 140 140" className="wrong-donut" role="img" aria-label="知识点错题分布">
      <circle cx="70" cy="70" r={radius} fill="none" stroke="#eef4f8" strokeWidth="18" />
      {data.map((item, i) => {
        const len = total ? (item.count / total) * circumference : 0;
        const node = (
          <circle
            key={item.name}
            cx="70"
            cy="70"
            r={radius}
            fill="none"
            stroke={DONUT_COLORS[i % DONUT_COLORS.length]}
            strokeWidth="18"
            strokeDasharray={`${len} ${circumference - len}`}
            strokeDashoffset={-offset}
            transform="rotate(-90 70 70)"
          />
        );
        offset += len;
        return node;
      })}
      <text x="70" y="68" textAnchor="middle" className="wrong-donut-total">{total}</text>
      <text x="70" y="88" textAnchor="middle" className="wrong-donut-label">道错题</text>
    </svg>
  );
}

/** 统计区条形数据块 */
function StatBars({ title, items, emptyText }) {
  const max = items.reduce((m, it) => Math.max(m, it.count), 0) || 1;
  return (
    <div className="wrong-stat-block">
      <div className="wrong-stat-block-title">{title}</div>
      {items.length === 0 && <div className="wrong-stat-empty">{emptyText}</div>}
      <div className="wrong-stat-list">
        {items.map((item) => (
          <div className="wrong-stat-row" key={item.key}>
            <span className="wrong-stat-name" title={item.name}>{item.name}</span>
            <span className="wrong-stat-bar">
              <span style={{ width: `${Math.round((item.count / max) * 100)}%` }} />
            </span>
            <span className="wrong-stat-count">{item.count}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

/** 顶部统计区 */
function WrongStats({ stats, total }) {
  const legend = stats.kpStats.slice(0, 8);
  return (
    <div className="wrong-stats">
      <div className="wrong-stats-pie">
        <WrongDonut data={legend} total={total} />
        <div className="wrong-donut-legend">
          {legend.map((item, i) => (
            <div className="wrong-legend-row" key={item.name}>
              <span className="wrong-legend-dot" style={{ background: DONUT_COLORS[i % DONUT_COLORS.length] }} />
              <span className="wrong-legend-name" title={item.name}>{item.name}</span>
              <span className="wrong-legend-count">{item.count}</span>
            </div>
          ))}
        </div>
      </div>
      <StatBars
        title="按知识点划分的错题数据"
        emptyText="暂无错题"
        items={stats.kpStats.map((it) => ({ key: it.id || it.name, name: it.name, count: it.count }))}
      />
      <StatBars
        title="按错题类型划分的错题数据"
        emptyText="暂无错题"
        items={stats.typeStats.map((it) => ({ key: it.type, name: it.label, count: it.count }))}
      />
    </div>
  );
}

/** 错题卡片 */
function WrongCard({ item, onEliminate }) {
  const canEliminate = Boolean(item.questionId);
  return (
    <div className="wrong-card-item">
      <div className="wrong-card-head">
        <Tag color="blue" bordered={false}>{typeLabel(item.questionType)}</Tag>
        {item.repoName && <span className="wrong-repo">{item.repoName}</span>}
        <span className="wrong-kp"><BulbOutlined /> {item.knowledgePointName || '未归知识点'}</span>
      </div>
      <div className="wrong-question">{item.questionTitle || '（无题干）'}</div>
      <div className="wrong-ans wrong-ans-mine">我的答案：{item.lastAnswer || '—'}</div>
      <div className="wrong-ans wrong-ans-right">正确答案：{item.correctAnswer || '—'}</div>
      <div className="wrong-card-foot">
        <span className="wrong-meta">错误 {item.wrongCount ?? 0} 次 · 最后错误 {formatTime(item.lastWrongTime)}</span>
        <Button
          type="primary"
          disabled={!canEliminate}
          onClick={() => onEliminate(item)}
        >
          <AimOutlined /> 消灭错题
        </Button>
      </div>
    </div>
  );
}

/** 知识点页签：章节 / 小节 / 卡片 三栏 */
function KnowledgeTab({ list, onEliminateSingle, onEliminateKp }) {
  const chapters = useMemo(() => groupByChapter(list), [list]);
  const [chapterId, setChapterId] = useState(chapters[0]?.id ?? '');
  const sections = useMemo(() => groupBySection(list, chapterId), [list, chapterId]);
  const [sectionId, setSectionId] = useState(sections[0]?.id ?? '');

  // 章节变化时收敛小节选择
  useEffect(() => {
    if (!chapters.some((c) => c.id === chapterId)) {
      setChapterId(chapters[0]?.id ?? '');
    }
  }, [chapters, chapterId]);
  useEffect(() => {
    if (!sections.some((s) => s.id === sectionId)) {
      setSectionId(sections[0]?.id ?? '');
    }
  }, [sections, sectionId]);

  const items = useMemo(() => filterBySection(list, chapterId, sectionId), [list, chapterId, sectionId]);
  const kpGroups = useMemo(() => groupByKnowledgePoint(items), [items]);

  return (
    <div className="wrong-columns">
      <div className="wrong-col wrong-col-chapter">
        <div className="wrong-col-title">错题章节</div>
        <div className="wrong-col-scroll">
          {chapters.map((c) => (
            <button
              key={c.id || 'none'}
              type="button"
              className={`wrong-col-item ${c.id === chapterId ? 'active' : ''}`}
              onClick={() => setChapterId(c.id)}
            >
              <span className="wrong-col-name" title={c.name}>{c.name}</span>
              <span className="wrong-col-count">{c.count}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="wrong-col wrong-col-section">
        <div className="wrong-col-title">错题小节</div>
        <div className="wrong-col-scroll">
          {sections.map((s) => (
            <button
              key={s.id || 'none'}
              type="button"
              className={`wrong-col-item ${s.id === sectionId ? 'active' : ''}`}
              onClick={() => setSectionId(s.id)}
            >
              <span className="wrong-col-name" title={s.name}>{s.name}</span>
              <span className="wrong-col-count">{s.count}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="wrong-col wrong-col-cards">
        <div className="wrong-col-title">错题（{items.length}）</div>
        <div className="wrong-col-scroll">
          {items.length === 0 && <div className="wrong-empty-small">该小节暂无错题</div>}
          {kpGroups.map((group) => (
            <div className="wrong-kp-group" key={group.id || group.name}>
              <div className="wrong-kp-group-head">
                <span className="wrong-kp-group-name"><BulbOutlined /> {group.name} · {group.count} 题</span>
                <Button
                  type="primary"
                  ghost
                  onClick={() => onEliminateKp(group)}
                >
                  <CheckCircleOutlined /> 消灭知识点
                </Button>
              </div>
              {group.items.map((item) => (
                <WrongCard key={item.questionId} item={item} onEliminate={onEliminateSingle} />
              ))}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/** 错误类型页签：按题型分组卡片 */
function TypeTab({ list, onEliminateSingle }) {
  const groups = useMemo(
    () => buildWrongStats(list).typeStats.map((t) => ({
      ...t,
      items: list.filter((it) => (it.questionType || '__unknown__') === t.type),
    })),
    [list],
  );
  return (
    <div className="wrong-type-groups">
      {groups.length === 0 && <div className="wrong-empty-small">暂无错题</div>}
      {groups.map((group) => (
        <div className="wrong-type-group" key={group.type}>
          <div className="wrong-type-group-head">
            <Tag color="blue" bordered={false}>{group.label}</Tag>
            <span className="wrong-type-group-count">{group.count} 题</span>
          </div>
          {group.items.map((item) => (
            <WrongCard key={item.questionId} item={item} onEliminate={onEliminateSingle} />
          ))}
        </div>
      ))}
    </div>
  );
}

export default function WrongBookPanel() {
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [drill, setDrill] = useState(null);

  const loadList = useCallback(() => {
    setLoading(true);
    listWrongQuestions({ current: 1, pageSize: 200 })
      .then((res) => setList(res?.data?.list || []))
      .catch(() => setList([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadList();
  }, [loadList]);

  const stats = useMemo(() => buildWrongStats(list), [list]);

  const eliminateSingle = (item) => setDrill({ mode: 'single', originals: [item] });
  const eliminateKp = (group) => setDrill({ mode: 'kp', originals: group.items });

  const closeDrill = () => setDrill(null);
  const onEliminated = () => {
    setDrill(null);
    loadList();
  };

  return (
    <div className="wrong-panel">
      <div className="wrong-sub">共 {list.length} 题 · 按知识点 / 错题类型归纳，整组答对即可消灭错题</div>

      {loading && <div className="wrong-empty-small">加载中…</div>}
      {!loading && list.length === 0 && (
        <div className="wrong-empty">
          <CheckCircleOutlined className="wrong-empty-icon" />
          <div>暂无错题，继续保持！</div>
        </div>
      )}

      {!loading && list.length > 0 && (
        <>
          <WrongStats stats={stats} total={list.length} />
          <Tabs
            items={[
              {
                key: 'kp',
                label: '知识点错题',
                children: (
                  <KnowledgeTab
                    list={list}
                    onEliminateSingle={eliminateSingle}
                    onEliminateKp={eliminateKp}
                  />
                ),
              },
              {
                key: 'type',
                label: '错误类型错题',
                children: <TypeTab list={list} onEliminateSingle={eliminateSingle} />,
              },
            ]}
          />
        </>
      )}

      <WrongDrillModal
        open={Boolean(drill)}
        mode={drill?.mode}
        originals={drill?.originals || []}
        onClose={closeDrill}
        onEliminated={onEliminated}
      />
    </div>
  );
}
