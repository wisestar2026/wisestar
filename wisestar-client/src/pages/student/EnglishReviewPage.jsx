/**
 * EnglishReviewPage.jsx - 学员端英语分类复习
 *
 * 功能:
 *   1. 按 单词 / 重点句子 / 语法 三类组织复习入口
 *   2. 单词：按单元统计 陌生 / 一般 / 熟悉 三档，点击进入单词练习
 *   3. 重点句子：按单元列出句子与熟练度，点击进入句子练习
 *   4. 语法：按单元列出语法点与掌握度，点击进入语法练习（回写掌握度）
 *
 * URL: /student/english/review
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心「智能复习」
 *
 * 数据流:
 *   GET /api/english/student/units      单元进度（单词总数）
 *   GET /api/english/word/word-book     单词本（含熟练度，pageSize=-1）
 *   GET /api/english/student/sentences  句子列表（含熟练度）
 *   GET /api/english/student/grammars   语法列表（含掌握度）
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Empty, Spin, Tabs, Tag, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import {
  getEnglishUnits,
  getEnglishWordBook,
  getEnglishSentences,
  getEnglishGrammars,
} from '../../api/englishStudent';
import './EnglishCenterPage.css';

const FAMILIARITY_LABELS = ['未学习', '生疏', '熟悉', '熟练', '精通'];
const FAMILIARITY_COLORS = ['default', 'orange', 'blue', 'cyan', 'green'];

/** 单词三档：陌生(0-1) / 一般(2) / 熟悉(3-4) */
function wordBucket(familiarity) {
  const f = Math.min(Math.max(familiarity || 0, 0), 4);
  if (f <= 1) return 'unfamiliar';
  if (f === 2) return 'normal';
  return 'familiar';
}

function famTag(familiarity) {
  const lv = Math.min(Math.max(familiarity || 0, 0), 4);
  return <Tag color={FAMILIARITY_COLORS[lv]}>{FAMILIARITY_LABELS[lv]}</Tag>;
}

/** 按单元分组（保持后端返回顺序） */
function groupByUnit(list) {
  const map = new Map();
  list.forEach((item) => {
    const key = item.unit || '未分单元';
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(item);
  });
  return Array.from(map.entries());
}

function Bucket({ label, value, color }) {
  return (
    <div style={{ flex: 1, textAlign: 'center', background: '#f4f9fc', borderRadius: 10, padding: '8px 4px' }}>
      <div style={{ fontSize: 18, fontWeight: 800, color }}>{value}</div>
      <div style={{ fontSize: 12, color: '#5b7f92' }}>{label}</div>
    </div>
  );
}

export default function EnglishReviewPage() {
  const navigate = useNavigate();
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);
  const activeSubject = useStudentStore((s) => s.activeSubject);

  const [loading, setLoading] = useState(true);
  const [units, setUnits] = useState([]);
  const [words, setWords] = useState([]);
  const [sentences, setSentences] = useState([]);
  const [grammars, setGrammars] = useState([]);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      getEnglishUnits({ version, grade, term }).catch(() => ({ data: [] })),
      getEnglishWordBook({ version, grade, term, pageSize: -1 }).catch(() => ({ data: { list: [] } })),
      getEnglishSentences({ version, grade, term }).catch(() => ({ data: [] })),
      getEnglishGrammars({ version, grade, term }).catch(() => ({ data: [] })),
    ])
      .then(([uRes, wRes, sRes, gRes]) => {
        setUnits(uRes?.data || []);
        setWords(wRes?.data?.list || []);
        setSentences(sRes?.data || []);
        setGrammars(gRes?.data || []);
      })
      .catch(() => message.error('复习数据加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term]);

  useEffect(() => {
    load();
  }, [load]);

  // 单词按单元三档统计
  const wordBuckets = useMemo(() => {
    const map = {};
    words.forEach((w) => {
      const key = w.unit || '未分单元';
      if (!map[key]) map[key] = { total: 0, unfamiliar: 0, normal: 0, familiar: 0 };
      map[key].total += 1;
      map[key][wordBucket(w.familiarity)] += 1;
    });
    return map;
  }, [words]);

  const wordUnits = useMemo(() => {
    const names = units.map((u) => u.unit).filter(Boolean);
    Object.keys(wordBuckets).forEach((k) => {
      if (!names.includes(k)) names.push(k);
    });
    return names;
  }, [units, wordBuckets]);

  const sentenceGroups = useMemo(() => groupByUnit(sentences), [sentences]);
  const grammarGroups = useMemo(() => groupByUnit(grammars), [grammars]);

  const goPractice = (unit) => navigate(`/student/english/practice?unit=${encodeURIComponent(unit)}`);
  const goSentence = (unit) => navigate(`/student/english/sentence?unit=${encodeURIComponent(unit)}`);
  const goGrammar = (unit) => {
    const sid = activeSubject || '1003';
    navigate(`/student/english/grammar-practice?unit=${encodeURIComponent(unit)}&subjectId=${encodeURIComponent(sid)}`);
  };

  if (loading) {
    return <div className="eng-learn-wrap"><Spin /></div>;
  }

  const wordTab = wordUnits.length === 0 ? (
    <Empty description="暂无单词数据，先去学习中心学习单词吧" image={Empty.PRESENTED_IMAGE_SIMPLE} />
  ) : (
    <div className="eng-grid">
      {wordUnits.map((unit) => {
        const b = wordBuckets[unit] || { total: 0, unfamiliar: 0, normal: 0, familiar: 0 };
        const canPractice = b.total >= 4;
        return (
          <div className="eng-unit-card" key={unit}>
            <div className="eng-unit-name">{unit}</div>
            <div style={{ display: 'flex', gap: 8, margin: '12px 0' }}>
              <Bucket label="陌生" value={b.unfamiliar} color="#ff7043" />
              <Bucket label="一般" value={b.normal} color="#f5a623" />
              <Bucket label="熟悉" value={b.familiar} color="#34c759" />
            </div>
            <div className="eng-unit-actions" style={{ gridTemplateColumns: '1fr' }}>
              <button
                type="button"
                className="eng-btn-practice"
                disabled={!canPractice}
                title={canPractice ? '' : '本单元单词太少，暂不能练习'}
                onClick={() => goPractice(unit)}
              >
                去练习{canPractice ? ` · ${b.total}` : ''}
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );

  const sentenceTab = sentenceGroups.length === 0 ? (
    <Empty description="暂无句子数据，先去学习中心学习句子吧" image={Empty.PRESENTED_IMAGE_SIMPLE} />
  ) : (
    <div className="eng-grammar-list">
      {sentenceGroups.map(([unit, list]) => (
        <div className="eng-grammar-card" key={unit}>
          <div className="eng-grammar-head">
            <span className="eng-grammar-index">💬</span>
            <span className="eng-grammar-title">{unit}</span>
            <span style={{ marginLeft: 'auto' }}>
              <button type="button" className="eng-btn-sentence eng-btn" onClick={() => goSentence(unit)}>
                复习该单元
              </button>
            </span>
          </div>
          <div className="eng-grammar-examples">
            <ul>
              {list.map((s) => (
                <li
                  key={s.id}
                  style={{ cursor: 'pointer' }}
                  onClick={() => goSentence(unit)}
                  title="点击进入该单元句子练习"
                >
                  <span className="eng-grammar-en">{s.en}</span>
                  {s.zh && <span className="eng-grammar-zh">{s.zh}</span>}
                  {famTag(s.familiarity)}
                </li>
              ))}
            </ul>
          </div>
        </div>
      ))}
    </div>
  );

  const grammarTab = grammarGroups.length === 0 ? (
    <Empty description="暂无语法数据，先去学习中心学习重点语法吧" image={Empty.PRESENTED_IMAGE_SIMPLE} />
  ) : (
    <div className="eng-grammar-list">
      {grammarGroups.map(([unit, list]) => (
        <div className="eng-grammar-card" key={unit}>
          <div className="eng-grammar-head">
            <span className="eng-grammar-index">📘</span>
            <span className="eng-grammar-title">{unit}</span>
            <span style={{ marginLeft: 'auto' }}>
              <button type="button" className="eng-btn-grammar eng-btn" onClick={() => goGrammar(unit)}>
                语法练习
              </button>
            </span>
          </div>
          <div className="eng-grammar-examples">
            <ul>
              {list.map((g) => (
                <li key={g.id}>
                  <span className="eng-grammar-en">{g.title}</span>
                  {famTag(g.familiarity)}
                </li>
              ))}
            </ul>
          </div>
        </div>
      ))}
    </div>
  );

  return (
    <div className="eng-page">
      <div className="eng-hero">
        <div>
          <div className="eng-hero-title">英语智能复习</div>
          <div className="eng-hero-sub">
            {version} · {grade} · 按 单词 / 重点句子 / 语法 分类复习，点击进入对应练习
          </div>
        </div>
      </div>

      <Tabs
        items={[
          { key: 'word', label: `单词（${words.length}）`, children: wordTab },
          { key: 'sentence', label: `重点句子（${sentences.length}）`, children: sentenceTab },
          { key: 'grammar', label: `语法（${grammars.length}）`, children: grammarTab },
        ]}
      />

      <div className="eng-actions" style={{ marginTop: 20 }}>
        <button type="button" className="eng-btn eng-btn-ghost" onClick={() => navigate('/student/english')}>
          返回学习中心
        </button>
      </div>
    </div>
  );
}
