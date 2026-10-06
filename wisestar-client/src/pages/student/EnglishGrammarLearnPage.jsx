/**
 * EnglishGrammarLearnPage.jsx - 学员端英语重点语法（只读学习）
 *
 * 功能:
 *   1. 展示单元重点语法：规则标题 + 讲解 + 例句（可朗读）
 *   2. 语法为只读知识呈现，不参与熟练度与复习队列
 *   3. 本单元练习只提供跳转入口，实际作答在独立界面 EnglishGrammarPracticePage
 *
 * URL: /student/english/grammar?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心单元卡片「重点语法」
 *
 * 数据流:
 *   GET /api/english/student/grammars?version&grade&term&unit → 单元语法
 *   GET /api/student/detect/units?subjectId&grade&term      → 取本单元可用练习题量（仅展示）
 */

import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishGrammars } from '../../api/englishStudent';
import { getDetectUnits } from '../../api/detect';
import { speakEnglish } from '../../utils/english';
import './EnglishCenterPage.css';

/** 解析后端 JSON 字符串列（examples/exercises），容错为空数组 */
function parseList(raw) {
  if (!raw) return [];
  try {
    const value = typeof raw === 'string' ? JSON.parse(raw) : raw;
    return Array.isArray(value) ? value : [];
  } catch {
    return [];
  }
}

/** 例句既可能是纯字符串，也可能是 {en, zh} / {text} 结构，统一取英文文本 */
function exampleText(item) {
  if (item == null) return '';
  if (typeof item === 'string') return item;
  return item.en || item.text || item.example || '';
}

/** 例句中文（可选） */
function exampleZh(item) {
  if (item == null || typeof item === 'string') return '';
  return item.zh || '';
}

export default function EnglishGrammarLearnPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);
  const activeSubject = useStudentStore((s) => s.activeSubject);

  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  // 本单元可用练习题量（用于练习入口展示，失败静默为 0）
  const [questionCount, setQuestionCount] = useState(0);

  const load = useCallback(() => {
    if (!unit) {
      setLoading(false);
      return;
    }
    setLoading(true);
    getEnglishGrammars({ version, grade, term, unit })
      .then((res) => setList(res?.data || []))
      .catch(() => message.error('语法加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term, unit]);

  useEffect(() => {
    load();
  }, [load]);

  // 仅取本单元可用练习题量用于入口展示；实际作答在独立的语法练习页完成
  useEffect(() => {
    if (!unit) {
      setQuestionCount(0);
      return undefined;
    }
    let cancelled = false;
    getDetectUnits({ subjectId: activeSubject || '1003', grade, term })
      .then((res) => {
        if (cancelled) return;
        const target = (res?.data || []).find((u) => u.name === unit);
        setQuestionCount(target?.questionCount || 0);
      })
      .catch(() => { if (!cancelled) setQuestionCount(0); });
    return () => { cancelled = true; };
  }, [activeSubject, grade, term, unit]);

  if (loading) {
    return <div className="eng-learn-wrap"><Spin /></div>;
  }

  if (!unit || list.length === 0) {
    return (
      <div className="eng-learn-wrap">
        <div className="eng-empty">
          <div className="eng-empty-emoji">🐚</div>
          <div className="eng-empty-title">{unit ? '该单元暂无重点语法' : '未选择单元'}</div>
          <button type="button" className="eng-btn eng-btn-primary" onClick={() => navigate('/student/english')}>
            返回英语学习中心
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="eng-learn-wrap">
      <div className="eng-hero eng-grammar-hero">
        <div>
          <div className="eng-hero-title">重点语法</div>
          <div className="eng-hero-sub">{unit} · 共 {list.length} 条语法点，学完单词和句子再来巩固一下</div>
        </div>
      </div>

      <div className="eng-grammar-list">
        {list.map((item, idx) => {
          const examples = parseList(item.examples);
          const showContent = item.content && item.content.trim() !== (item.title || '').trim();
          return (
            <div className="eng-grammar-card" key={item.id || `${unit}-${idx}`}>
              <div className="eng-grammar-head">
                <span className="eng-grammar-index">{idx + 1}</span>
                <span className="eng-grammar-title">{item.title}</span>
              </div>
              {showContent && <div className="eng-grammar-content">{item.content}</div>}
              {examples.length > 0 && (
                <div className="eng-grammar-examples">
                  <div className="eng-grammar-examples-label">例句</div>
                  <ul>
                    {examples.map((ex, i) => {
                      const en = exampleText(ex);
                      const zh = exampleZh(ex);
                      return (
                        <li key={`${item.id || idx}-ex-${i}`}>
                          <span className="eng-grammar-en">{en}</span>
                          {zh && <span className="eng-grammar-zh">{zh}</span>}
                          {en && (
                            <button
                              type="button"
                              className="eng-grammar-sound"
                              title="朗读例句"
                              onClick={() => speakEnglish(en)}
                            >
                              朗读
                            </button>
                          )}
                        </li>
                      );
                    })}
                  </ul>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* 本单元练习：只提供跳转入口，实际作答在独立的语法练习页完成 */}
      <div className="eng-grammar-list" style={{ marginTop: 20 }}>
        <div className="eng-grammar-card">
          <div className="eng-grammar-head">
            <span className="eng-grammar-index">📝</span>
            <span className="eng-grammar-title">本单元练习</span>
            {questionCount > 0 && (
              <span style={{ marginLeft: 8, color: '#8aa4bd', fontSize: 13 }}>共 {questionCount} 题</span>
            )}
          </div>
          <div className="eng-grammar-content">
            {questionCount > 0
              ? '语法学完了，去练习里实际做一遍，边做边巩固。'
              : '该单元暂无绑定练习，先去学习单词和句子吧。'}
          </div>
          {questionCount > 0 && (
            <div style={{ marginTop: 12 }}>
              <button
                type="button"
                className="eng-btn eng-btn-primary"
                onClick={() => navigate(`/student/english/grammar-practice?unit=${encodeURIComponent(unit)}&subjectId=${encodeURIComponent(activeSubject || '1003')}`)}
              >
                进入语法练习
              </button>
            </div>
          )}
        </div>
      </div>

      <div className="eng-actions">
        <button type="button" className="eng-btn eng-btn-ghost" onClick={() => navigate('/student/english')}>
          返回学习中心
        </button>
      </div>
    </div>
  );
}
