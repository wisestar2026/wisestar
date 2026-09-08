/**
 * StudyPage.jsx - 学海研习页（学员端）
 *
 * 功能:
 *   1. 左栏章节学海洲岛导航：后台配置的章节 → 小节（真实数据，按订单权限过滤）
 *   2. 中栏：学科概览 / 选中小节的学习内容（学习目标/内容概述/讲解要点）
 *   3. 右栏：快捷操作（进入知识点页预习/练习/试炼/错题）
 *
 * 数据源:
 *   真实模式（studyContent.subjects 已加载且非空）：
 *     章节 = /api/student/study/chapters；小节 = /api/student/study/sections
 *   回退模式（接口异常/未开通）：展示既有 mock 学科内容并提示
 *
 * 被谁引用: App.jsx 路由表（/student/study）；StudentLayout 子路由
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useStudentStore, { SUBJECTS, masteryLevel } from '../../stores/useStudentStore';
import { getStudySections, uploadActivity } from '../../api/student';
import { getPracticeMastery } from '../../api/practice';
import './StudyPage.css';

// 四大核心功能按钮配置
const ACTION_BUTTONS = [
  { key: 'preview', label: '知识点预习', icon: '📖', color: 'blue' },
  { key: 'practice', label: '专项练习湾', icon: '✏️', color: 'orange' },
  { key: 'trial', label: '小节通关', icon: '🎯', color: 'green' },
  { key: 'wrong', label: '知识点错题本', icon: '📕', color: 'purple' },
];

// 完成度 → 星星（5 颗，金色点亮；完成练习且正确率达标：≥80 五颗 / ≥60 四颗 / ≥40 三颗 / ≥20 两颗 / >0 一颗）
const stars = (rate) => {
  const n = rate >= 80 ? 5 : rate >= 60 ? 4 : rate >= 40 ? 3 : rate >= 20 ? 2 : rate > 0 ? 1 : 0;
  return '⭐'.repeat(n) + '☆'.repeat(5 - n);
};

const fmtMasteryTime = (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false, month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '');

/** 小节「上次掌握情况卡」：上次练习结果 + 知识点维度掌握 + 再次练习入口（交卷制历史双粒度保留） */
function SectionMasteryCard({ mastery, onPractice, onReplay }) {
  const last = mastery?.lastRecord;
  const hasHistory = (mastery?.practiceCount || 0) > 0 || !!last;
  const kps = mastery?.kps || [];
  return (
    <div className="study-mastery-card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
        <b style={{ fontSize: 15 }}>📈 上次练习掌握情况</b>
        <span style={{ fontSize: 12, color: '#90a4ae' }}>
          练习 {mastery?.practiceCount || 0} 次 · 累计掌握度 {mastery?.accuracy ?? 0}%
        </span>
      </div>
      {hasHistory ? (
        <>
          <div className="study-mastery-body">
            <div className="study-mastery-stat">
              <div className="study-mastery-num">{last?.rate != null ? `${last.rate}%` : '—'}</div>
              <div className="study-mastery-label">上次正确率</div>
            </div>
            <div className="study-mastery-stat">
              <div className="study-mastery-num">{last?.correctCount != null ? `${last.correctCount}/${last.totalQuestions || '-'}` : '—'}</div>
              <div className="study-mastery-label">上次答对 / 题数</div>
            </div>
            <div className="study-mastery-stat">
              <div className="study-mastery-num">{mastery?.accuracy ?? 0}%</div>
              <div className="study-mastery-label">小节掌握度</div>
            </div>
            <div className="study-mastery-stat">
              <div className="study-mastery-num" style={{ fontSize: 13, color: '#78909c' }}>{fmtMasteryTime(last?.createAt) || '—'}</div>
              <div className="study-mastery-label">上次练习时间</div>
            </div>
          </div>
          {kps.length > 0 && (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 6 }}>
              {kps.slice(0, 8).map((kp) => (
                <span key={kp.knowledgePointId} style={{
                  fontSize: 12, padding: '2px 8px', borderRadius: 12,
                  background: kp.accuracy >= 80 ? '#e8f5e9' : kp.accuracy >= 50 ? '#fff8e1' : '#ffebee',
                  color: kp.accuracy >= 80 ? '#2e7d32' : kp.accuracy >= 50 ? '#b26a00' : '#c62828',
                }}>
                  {kp.knowledgePointName} {kp.accuracy}%
                </span>
              ))}
            </div>
          )}
          <div style={{ marginTop: 10, display: 'flex', gap: 8 }}>
            <button className="study-action-btn study-action-green" style={{ width: 'auto', padding: '6px 14px' }} onClick={onPractice}>
              ✏️ 开始练习
            </button>
            <button className="study-action-btn study-action-orange" style={{ width: 'auto', padding: '6px 14px' }} onClick={onReplay}>
              🔁 再次练习（重新抽题）
            </button>
          </div>
        </>
      ) : (
        <div style={{ marginTop: 8, fontSize: 13, color: '#90a4ae' }}>
          本小节还没有练习记录，完成「专项练习湾」后这里会展示上次掌握情况与知识点总结
        </div>
      )}
    </div>
  );
}

export default function StudyPage() {
  const navigate = useNavigate();
  const {
    activeSubject, version, grade, pureMode,
    studyContent, fetchStudyChapters, getVisibleSubjects,
  } = useStudentStore();
  const subject = SUBJECTS.find((s) => s.key === activeSubject) || SUBJECTS[1];

  // 展开的章节（可多开）+ 选中的小节/知识点
  const [openChapters, setOpenChapters] = useState([]);
  const [selectedSection, setSelectedSection] = useState(null);
  const [selectedKp, setSelectedKp] = useState(null);
  // 各章节的小节缓存（按章节 id 隔离，避免串数据）
  const [sectionsMap, setSectionsMap] = useState({});
  // 当前小节练习掌握情况（上次结果 + 知识点维度；研习页顶部「上次掌握情况卡」）
  const [sectionMastery, setSectionMastery] = useState(null);

  // 选中小节 → 拉取该小节掌握度汇总（无记录返回空视图）
  useEffect(() => {
    if (!realMode || !selectedSection?.id) {
      setSectionMastery(null);
      return;
    }
    let alive = true;
    getPracticeMastery({ sectionId: selectedSection.id })
      .then((res) => { if (alive) setSectionMastery(res?.data || null); })
      .catch(() => { if (alive) setSectionMastery(null); });
    return () => { alive = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [realMode, selectedSection]);

  // 真实学科模式：已加载真实学科（有权限）且当前学科为真实学科 id
  const visibleSubjects = getVisibleSubjects();
  const realMode = (studyContent.subjects?.length ?? 0) > 0;
  const realSubject = visibleSubjects.find((s) => s.key === activeSubject);
  const realChapters = studyContent.chapters; // null=未加载 / [] = 无数据

  // 章节数据：真实模式用真实章节；否则用 mock 学科章节
  const chapters = realMode ? (realChapters || []) : subject.chapters;

  // 学科/年级切换 → 按订单授权年级加载真实章节
  useEffect(() => {
    if (realMode) {
      fetchStudyChapters(activeSubject, grade);
    }
    setOpenChapters([]);
    setSelectedSection(null);
    setSelectedKp(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeSubject, grade, realMode]);

  // 展开章节：真实模式加载该章节的小节（按章节缓存）
  const toggleChapter = (chId) => {
    setOpenChapters((prev) => (prev.includes(chId) ? prev.filter((id) => id !== chId) : [...prev, chId]));
    if (realMode && !sectionsMap[chId]) {
      getStudySections(chId)
        .then((res) => setSectionsMap((m) => ({ ...m, [chId]: res?.data || [] })))
        .catch(() => setSectionsMap((m) => ({ ...m, [chId]: [] })));
    }
  };

  // 真实模式选中小节 → 上报学习位置（章节/小节上下文，供后台督学定位）
  useEffect(() => {
    if (!realMode || !selectedSection?.id) return;
    uploadActivity({ page: '/student/study', sectionId: selectedSection.id }).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [realMode, selectedSection]);

  // 章节完成度（mock）/ 小节数（真实）
  const avgProgress = chapters.length
    ? Math.round(chapters.reduce((sum, c) => sum + (c.progress || 0), 0) / chapters.length)
    : 0;

  // AI 建议
  const advice = (realMode ? selectedSection : selectedKp)
    ? { text: realMode ? '选中小节后，点击右侧按钮进入「知识点预习 / 专项练习 / 试炼检测」。' : '已选中知识点，点击右侧按钮开始学习。' }
    : { text: realMode ? `在「${realSubject?.name || subject.name}」的海域里，选择一个小节开始今天的研习吧。` : `在「${subject.name}」的海域里，挑选一个知识点开始今天的研习吧。` };

  // 右栏按钮 → 知识点页（真实模式按小节进入；mock 按知识点进入）
  const goAction = (action) => {
    if (realMode) {
      if (!selectedSection) return;
      navigate(`/student/knowledge?sectionId=${selectedSection.id}&tab=${action.key}`);
    } else {
      if (!selectedKp) return;
      navigate(`/student/knowledge/${selectedKp.id}?tab=${action.key}`);
    }
  };

  // 选中真实小节：解析其内容设置（objective/overview/points）
  const sectionContent = (() => {
    if (!selectedSection?.content) return null;
    try { return JSON.parse(selectedSection.content); } catch { return null; }
  })();

  return (
    <div className="sll-page-enter study-page">
      {/* ---- 左栏: 章节学海洲岛导航 ---- */}
      <aside className="sll-card study-left">
        <div className="study-left-title">
          <span className="study-left-icon">🗺️</span> 学海洲岛 · {realSubject?.name || subject.name}
          {!pureMode && <span className="study-left-sub">{realMode ? `${chapters.length} 个章节` : `进度 ${avgProgress}%`}</span>}
        </div>
        {realMode && studyContent.loadFailed && (
          <div className="study-load-hint">⚠️ 内容加载失败，当前为演示数据</div>
        )}
        <div className="study-chapters">
          {realMode && realChapters !== null && realChapters.length === 0 && (
            <div className="study-empty">该学科暂无章节内容，请联系管理员配置</div>
          )}
          {chapters.map((ch) => {
            const open = openChapters.includes(ch.id);
            return (
              <div key={ch.id} className={`sll-chapter study-chapter ${open ? 'open' : ''}`}>
                {/* 章节卡片头 */}
                <div className="study-chapter-head" onClick={() => toggleChapter(ch.id)}>
                  <span className="study-chapter-icon">{ch.icon || '📖'}</span>
                  <div className="study-chapter-info">
                    <div className="study-chapter-name">
                      {ch.name}
                      {realMode && <span className="study-chapter-stars">{stars(ch.progress || 0)}</span>}
                    </div>
                    {realMode ? (
                      <div className="study-chapter-sub">学习完成度 {ch.progress || 0}%</div>
                    ) : (
                      <div className="study-chapter-progress">
                        <div className="study-chapter-progress-bar" style={{ width: `${ch.progress}%` }} />
                      </div>
                    )}
                  </div>
                  {!realMode && <span className="study-chapter-pct">{ch.progress}%</span>}
                  <span className={`study-chapter-arrow ${open ? 'open' : ''}`}>▾</span>
                </div>
                {/* 展开区：真实=小节列表；mock=知识点条目 */}
                <div className={`study-kp-list ${open ? 'open' : ''}`}>
                  {realMode ? (
                    (sectionsMap[ch.id] || []).map((sec) => (
                      <div
                        key={sec.id}
                        className={`study-kp ${selectedSection && selectedSection.id === sec.id ? 'selected' : ''}`}
                        onClick={() => setSelectedSection(sec)}
                      >
                        <span className="study-kp-name">🌊 {sec.name}</span>
                        <span className="study-kp-meta">
                          <span className="study-kp-stars">{stars(sec.progress || 0)}</span>
                          <span className="study-kp-pct">{sec.progress || 0}%</span>
                        </span>
                      </div>
                    ))
                  ) : (
                    ch.kps.map((kp) => {
                      const lv = masteryLevel(kp.mastery);
                      const sel = selectedKp && selectedKp.id === kp.id;
                      return (
                        <div
                          key={kp.id}
                          className={`study-kp ${sel ? 'selected' : ''}`}
                          onClick={() => setSelectedKp(kp)}
                        >
                          <span className="study-kp-name">🌊 {kp.name}</span>
                          <span className="study-kp-meta">
                            <span className="study-kp-pct">{kp.mastery}%</span>
                            <span className="sll-level" style={{ background: lv.color }}>{lv.label}</span>
                          </span>
                        </div>
                      );
                    })
                  )}
                  {realMode && open && (sectionsMap[ch.id] || []).length === 0 && (
                    <div className="study-empty">该章节暂无小节内容</div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </aside>

      {/* ---- 中栏: 主内容展示区 ---- */}
      <main className="sll-card study-center">
        {realMode ? (
          !selectedSection ? (
            /* 真实模式未选中小节: 学科概览 */
            <div className="study-overview">
              <div className="study-overview-title">
                🌊 {realSubject?.name || subject.name} · 研习概览（{version}）
              </div>
              <div className="study-overview-body">
                <div className="study-ring-stats" style={{ width: '100%', justifyContent: 'center' }}>
                  <div className="study-ring-stat"><span className="study-ring-dot orange" />章节 {chapters.length} 个</div>
                  <div className="study-ring-stat"><span className="study-ring-dot blue" />小节 {chapters.reduce((n, c) => n + (c.sectionCount || 0), 0)} 个</div>
                </div>
                <div className="study-overview-chapters" style={{ width: '100%' }}>
                  {chapters.map((ch) => (
                    <div key={ch.id} className="study-ov-chapter">
                      <span className="study-ov-name">{ch.icon || '📖'} {ch.name}</span>
                      <span className="study-ov-pct">小节 {ch.sectionCount ?? 0}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            /* 选中真实小节: 学习内容 */
            <div className="study-kp-detail">
              {sectionMastery && realMode && (
                <SectionMasteryCard
                  mastery={sectionMastery}
                  onPractice={() => navigate(`/student/knowledge?sectionId=${selectedSection.id}&tab=practice`)}
                  onReplay={() => navigate(`/student/knowledge?sectionId=${selectedSection.id}&tab=practice&random=1`)}
                />
              )}
              <div className="study-kp-detail-title">🌊 {selectedSection.name}</div>
              {sectionContent ? (
                <>
                  <div className="study-kp-detail-desc">
                    <b>学习目标：</b>{sectionContent.objective || '—'}
                  </div>
                  <div className="study-kp-detail-desc">
                    <b>内容概述：</b>{sectionContent.overview || '—'}
                  </div>
                  <div className="study-kp-detail-guide">
                    <div className="study-kp-guide-title">📖 讲解要点</div>
                    {(sectionContent.points || []).map((p, i) => (
                      <div key={i} className="study-kp-guide-step">• {p}</div>
                    ))}
                    {(!sectionContent.points || sectionContent.points.length === 0) && (
                      <div className="study-empty">该小节暂未配置讲解要点</div>
                    )}
                  </div>
                </>
              ) : (
                <div className="study-empty">该小节暂未配置学习内容</div>
              )}
              <div className="study-kp-detail-guide" style={{ marginTop: 16 }}>
                <div className="study-kp-guide-title">🧭 学习引导</div>
                <div className="study-kp-guide-step">① 点击右侧「知识点预习」了解核心内容</div>
                <div className="study-kp-guide-step">② 进入「专项练习湾」完成练习获得学习币</div>
                <div className="study-kp-guide-step">③ 掌握度达标后「试炼检测」检验成果</div>
                <div className="study-kp-guide-step">④ 订正「知识点错题本」中的错题</div>
              </div>
            </div>
          )
        ) : (
          !selectedKp ? (
            /* mock 模式: 学科整体学习进度大图 + 环形统计 */
            <div className="study-overview">
              <div className="study-overview-title">
                🌊 {subject.name} · 整体学习进度（{version}）
              </div>
              <div className="study-overview-body">
                <div className="study-ring">
                  <svg viewBox="0 0 120 120" width="150" height="150">
                    <circle cx="60" cy="60" r="50" fill="none" stroke="#e8f2fa" strokeWidth="14" />
                    <circle
                      cx="60" cy="60" r="50" fill="none"
                      stroke={`var(--study-theme-${subject.theme})`}
                      strokeWidth="14" strokeLinecap="round"
                      strokeDasharray={`${avgProgress * 3.14} ${100 * 3.14}`}
                      transform="rotate(-90 60 60)"
                      style={{ transition: 'stroke-dasharray 0.8s ease' }}
                    />
                    <text x="60" y="57" textAnchor="middle" className="study-ring-num">{avgProgress}%</text>
                    <text x="60" y="75" textAnchor="middle" className="study-ring-label">总进度</text>
                  </svg>
                  <div className="study-ring-stats">
                    <div className="study-ring-stat"><span className="study-ring-dot orange" />章节 {subject.chapters.length} 个</div>
                    <div className="study-ring-stat"><span className="study-ring-dot blue" />知识点 {subject.chapters.reduce((n, c) => n + c.kps.length, 0)} 个</div>
                    <div className="study-ring-stat"><span className="study-ring-dot green" />精通 {subject.chapters.reduce((n, c) => n + c.kps.filter((k) => k.mastery >= 85).length, 0)} 个</div>
                  </div>
                </div>
                <div className="study-overview-chapters">
                  {subject.chapters.map((ch) => (
                    <div key={ch.id} className="study-ov-chapter">
                      <span className="study-ov-name">{ch.icon} {ch.name}</span>
                      <div className="study-ov-bar">
                        <div
                          className={`study-ov-bar-inner ${subject.theme}`}
                          style={{ width: `${ch.progress}%` }}
                        />
                      </div>
                      <span className="study-ov-pct">{ch.progress}%</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            /* mock 选中知识点详情 */
            <div className="study-kp-detail">
              <div className="study-kp-detail-title">
                🌊 {selectedKp.name}
                <span className="sll-level" style={{ background: masteryLevel(selectedKp.mastery).color }}>
                  {masteryLevel(selectedKp.mastery).label}
                </span>
              </div>
              <div className="study-kp-detail-desc">{selectedKp.desc}</div>
              <div className="study-kp-detail-state">
                <div className="study-kp-state-item">
                  <span className="study-kp-state-label">掌握度</span>
                  <div className="study-kp-state-bar">
                    <div
                      className={`study-kp-state-fill ${subject.theme}`}
                      style={{ width: `${selectedKp.mastery}%` }}
                    />
                  </div>
                  <span className="study-kp-state-val">{selectedKp.mastery}%</span>
                </div>
                <div className="study-kp-state-item">
                  <span className="study-kp-state-label">学习状态</span>
                  <span className="study-kp-state-tag">
                    {selectedKp.mastery >= 85 ? '🟢 建议进入试炼冲高分' : selectedKp.mastery >= 55 ? '🟡 建议专项练习巩固' : '🔴 建议先预习再练习'}
                  </span>
                </div>
              </div>
              <div className="study-kp-detail-guide">
                <div className="study-kp-guide-title">🧭 学习引导</div>
                <div className="study-kp-guide-step">① 点击右侧「知识点预习」了解核心内容</div>
                <div className="study-kp-guide-step">② 进入「专项练习湾」完成练习获得学习币</div>
                <div className="study-kp-guide-step">③ 掌握度达标后「试炼检测」检验成果</div>
                <div className="study-kp-guide-step">④ 订正「知识点错题本」中的错题</div>
              </div>
            </div>
          )
        )}
      </main>

      {/* ---- 右栏: 悬浮快捷操作面板（固定跟随） ---- */}
      <aside className="sll-card study-right">
        <div className="study-right-title">🎯 快捷操作</div>
        <div className="study-actions">
          {ACTION_BUTTONS.map((a) => (
            <button
              key={a.key}
              className={`study-action-btn study-action-${a.color} ${(realMode ? !selectedSection : !selectedKp) ? 'disabled' : ''}`}
              onClick={goAction.bind(null, a)}
            >
              <span className="study-action-icon">{a.icon}</span>
              <span>{a.label}</span>
              <span className="study-action-arrow">›</span>
            </button>
          ))}
        </div>
        {(realMode ? !selectedSection : !selectedKp) && (
          <div className="study-action-hint">👆 请先选择左侧{realMode ? '小节' : '知识点'}</div>
        )}

        {/* AI 小鲸向导 */}
        <div className="study-ai">
          <div className="study-ai-title">🐬 AI 小鲸向导</div>
          <div className="study-ai-text">{advice.text}</div>
        </div>
      </aside>
    </div>
  );
}
