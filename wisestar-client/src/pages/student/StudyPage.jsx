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
import { message } from 'antd';
import useStudentStore, { SUBJECTS, masteryLevel } from '../../stores/useStudentStore';
import { getStudySections, uploadActivity } from '../../api/student';
import IconTile from '../../components/common/IconTile';
import StarRating from '../../components/common/StarRating';
import './StudyPage.css';

// 四大核心功能按钮配置（short 用于小节行内的紧凑按钮；tone 为 3D 黏土图标色调）
const ACTION_BUTTONS = [
  { key: 'preview', label: '知识点预习', short: '预习', icon: '📖', color: 'blue', tone: 'blue' },
  { key: 'practice', label: '专项练习湾', short: '练习', icon: '✏️', color: 'orange', tone: 'orange' },
  { key: 'trial', label: '小节通关', short: '通关', icon: '🎯', color: 'green', tone: 'green' },
  { key: 'wrong', label: '知识点错题本', short: '错题', icon: '📕', color: 'purple', tone: 'purple' },
];

// 章节图标底座循环色调（让左栏章节有层次、不单调）
const CHAPTER_TONES = ['blue', 'orange', 'green', 'purple', 'teal', 'pink'];

export default function StudyPage() {
  const navigate = useNavigate();
  const {
    activeSubject, version, grade, pureMode,
    studyContent, fetchStudyChapters, fetchStudyProgress, getVisibleSubjects,
  } = useStudentStore();
  const subject = SUBJECTS.find((s) => s.key === activeSubject) || SUBJECTS[1];

  // 当前选中的章节（章节列表在中栏展开其小节）+ 选中的小节/知识点
  const [selectedChapterId, setSelectedChapterId] = useState(null);
  const [selectedSection, setSelectedSection] = useState(null);
  const [selectedKp, setSelectedKp] = useState(null);
  // 各章节的小节缓存（按章节 id 隔离，避免串数据）
  const [sectionsMap, setSectionsMap] = useState({});

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
    setSelectedChapterId(null);
    setSelectedSection(null);
    setSelectedKp(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeSubject, grade, realMode]);

  // 学科/版本切换 → 加载真实掌握度/薄弱（章节 → 知识点）
  useEffect(() => {
    if (realMode) {
      fetchStudyProgress(activeSubject, version);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeSubject, version, realMode]);

  // 选中章节：章节列表只负责选中，小节改由中栏以行列表展示（按章节缓存小节）
  const selectChapter = (chId) => {
    if (selectedChapterId !== chId) {
      setSelectedSection(null);
      setSelectedKp(null);
    }
    setSelectedChapterId(chId);
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
    ? { text: realMode ? '选中小节后，点击该行右侧的「预习 / 练习 / 通关 / 错题」进入对应功能。' : '已选中知识点，点击该行右侧的「预习 / 练习 / 通关 / 错题」开始学习。' }
    : { text: realMode ? `在「${realSubject?.name || subject.name}」的海域里，选择一个小节开始今天的研习吧。` : `在「${subject.name}」的海域里，挑选一个知识点开始今天的研习吧。` };

  // 小节行内快捷操作 → 知识点页（真实模式按小节进入；mock 按知识点进入）
  const navigateToAction = (target, action) => {
    if (!target) return;
    if (realMode) {
      const nm = target.name ? `&name=${encodeURIComponent(target.name)}` : '';
      navigate(`/student/knowledge?sectionId=${target.id}${nm}&tab=${action.key}`);
    } else {
      const nm = target.name ? `?name=${encodeURIComponent(target.name)}` : '';
      navigate(`/student/knowledge/${target.id}${nm}${nm ? '&' : '?'}tab=${action.key}`);
    }
  };

  // 选中真实小节：解析其内容设置（objective/overview/points）
  const sectionContent = (() => {
    if (!selectedSection?.content) return null;
    try { return JSON.parse(selectedSection.content); } catch { return null; }
  })();

  // 中栏当前章节与其小节（真实=接口缓存；mock=章节自带知识点）
  const activeChapter = chapters.find((c) => c.id === selectedChapterId) || null;
  const activeSections = realMode
    ? (selectedChapterId ? (sectionsMap[selectedChapterId] || []) : [])
    : (activeChapter?.kps || []);
  const sectionsLoading = realMode && !!selectedChapterId && sectionsMap[selectedChapterId] === undefined;

  return (
    <div className="sll-page-enter study-page">
      {/* ---- 左栏: 章节学海洲岛导航 ---- */}
      <aside className="sll-card study-left">
        <div className="study-left-title">
          <IconTile emoji="🗺️" tone="teal" size="sm" /> 学海洲岛 · {realSubject?.name || subject.name}
          {!pureMode && <span className="study-left-sub">{realMode ? `${chapters.length} 个章节` : `进度 ${avgProgress}%`}</span>}
        </div>
        {realMode && studyContent.loadFailed && (
          <div className="study-load-hint">⚠️ 内容加载失败，当前为演示数据</div>
        )}
        <div className="study-chapters">
          {realMode && realChapters !== null && realChapters.length === 0 && (
            <div className="study-empty">该学科暂无章节内容，请联系管理员配置</div>
          )}
          {chapters.map((ch, ci) => {
            const active = selectedChapterId === ch.id;
            return (
              <div key={ch.id} className={`sll-chapter study-chapter ${active ? 'open' : ''}`}>
                {/* 章节卡片头（点击选中，小节在中栏展开） */}
                <div className="study-chapter-head" onClick={() => selectChapter(ch.id)}>
                  <IconTile emoji={ch.icon || '📖'} tone={CHAPTER_TONES[ci % CHAPTER_TONES.length]} size="md" />
                  <div className="study-chapter-info">
                    <div className="study-chapter-name">
                      {ch.name}
                      {realMode && <StarRating value={ch.progress || 0} size={13} className="study-chapter-stars" />}
                    </div>
                    {realMode ? (
                      <div className="study-chapter-sub">
                        学习完成度 {ch.progress || 0}%
                      </div>
                    ) : (
                      <div className="study-chapter-progress">
                        <div className="study-chapter-progress-bar" style={{ width: `${ch.progress}%` }} />
                      </div>
                    )}
                  </div>
                  {!realMode && <span className="study-chapter-pct">{ch.progress}%</span>}
                  <span className={`study-chapter-arrow ${active ? 'open' : ''}`}>▾</span>
                </div>
              </div>
            );
          })}
        </div>
      </aside>

      {/* ---- 中栏: 主内容展示区 ---- */}
      <main className="sll-card study-center">
        {!selectedChapterId ? (
          realMode ? (
            /* 真实模式未选章节: 学科概览 */
            <div className="study-overview">
              <div className="study-overview-title">
                <IconTile emoji="🌊" tone="sky" size="sm" /> {realSubject?.name || subject.name} · 研习概览（{version}）
              </div>
              <div className="study-overview-body">
                <div className="study-ring-stats" style={{ width: '100%', justifyContent: 'center' }}>
                  <div className="study-ring-stat"><span className="study-ring-dot orange" />章节 {chapters.length} 个</div>
                  <div className="study-ring-stat"><span className="study-ring-dot blue" />小节 {chapters.reduce((n, c) => n + (c.sectionCount || 0), 0)} 个</div>
                </div>
                <div className="study-overview-chapters" style={{ width: '100%' }}>
                  {chapters.map((ch) => (
                    <div key={ch.id} className="study-ov-chapter">
                      <span className="study-ov-name"><IconTile emoji={ch.icon || '📖'} tone="blue" size="xs" /> {ch.name}</span>
                      <span className="study-ov-pct">小节 {ch.sectionCount ?? 0}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            /* mock 模式: 学科整体学习进度大图 + 环形统计 */
            <div className="study-overview">
              <div className="study-overview-title">
                <IconTile emoji="🌊" tone="sky" size="sm" /> {subject.name} · 整体学习进度（{version}）
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
                      <span className="study-ov-name"><IconTile emoji={ch.icon} tone="blue" size="xs" /> {ch.name}</span>
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
          )
        ) : (
          /* 已选章节: 中栏以横向行列出小节，选中小节后在其下方展开学习内容 */
          <div className="study-section-panel">
            <div className="study-section-head">
              <div className="study-section-title">
                <IconTile emoji={activeChapter?.icon || '📖'} tone="blue" size="md" /> {activeChapter?.name}
              </div>
              <span className="study-section-count">
                共 {activeSections.length} 个{realMode ? '小节' : '知识点'}
              </span>
            </div>

            <div className="study-sections">
              {sectionsLoading ? (
                <div className="study-empty">小节加载中…</div>
              ) : activeSections.length === 0 ? (
                <div className="study-empty">该章节暂无{realMode ? '小节' : '知识点'}内容</div>
              ) : realMode ? (
                activeSections.map((sec) => {
                  const locked = !!sec.locked;
                  // 通关进度 = 已通关题量（已答对的不同题目数）/ 该小节已有题量
                  const questionCount = sec.questionCount ?? 0;
                  const passedCount = sec.correctCount ?? 0;
                  const passRate = questionCount > 0 ? Math.round((passedCount / questionCount) * 100) : 0;
                  const sel = selectedSection && selectedSection.id === sec.id;
                  return (
                    <div
                      key={sec.id}
                      className={`study-kp ${sel ? 'selected' : ''} ${locked ? 'locked' : ''}`}
                      onClick={() => {
                        if (locked) {
                          message.warning('请先通关上一小节');
                          return;
                        }
                        setSelectedSection(sec);
                      }}
                    >
                      <div className="study-kp-head">
                        <div className="study-kp-main">
                          <span className="study-kp-name">
                            <IconTile emoji={locked ? '🔒' : '🌊'} tone={locked ? 'slate' : 'teal'} size="xs" />
                            <span className="study-kp-name-text">{sec.name}</span>
                            <StarRating value={passRate} size={13} className="study-kp-stars" />
                          </span>
                          <span className="study-kp-tags">
                            {sec.passed && <span className="study-kp-pass">已通关</span>}
                          </span>
                        </div>
                        <span className="study-kp-stats">
                          <span className="study-kp-stat">已通关 <b>{passedCount}</b>/{questionCount} 题</span>
                          <span className="study-kp-stat pct"><b>{passRate}%</b></span>
                        </span>
                      </div>
                      <div className="study-kp-progress">
                        <div className="study-kp-bar">
                          <div className="study-kp-bar-fill" style={{ width: `${passRate}%` }} />
                        </div>
                      </div>
                      <div className="study-kp-actions">
                        {ACTION_BUTTONS.map((a) => (
                          <button
                            key={a.key}
                            type="button"
                            title={a.label}
                            className={`study-kp-act ${a.color} ${locked ? 'disabled' : ''}`}
                            onClick={(e) => {
                              e.stopPropagation();
                              if (locked) {
                                message.warning('请先通关上一小节');
                                return;
                              }
                              navigateToAction(sec, a);
                            }}
                          >
                            <IconTile emoji={a.icon} tone={a.tone} size="xs" className="study-kp-act-ico" />
                            <span>{a.short}</span>
                          </button>
                        ))}
                      </div>
                    </div>
                  );
                })
              ) : (
                activeSections.map((kp) => {
                  const lv = masteryLevel(kp.mastery);
                  const sel = selectedKp && selectedKp.id === kp.id;
                  return (
                    <div
                      key={kp.id}
                      className={`study-kp ${sel ? 'selected' : ''}`}
                      onClick={() => setSelectedKp(kp)}
                    >
                      <div className="study-kp-head">
                        <div className="study-kp-main">
                          <span className="study-kp-name"><IconTile emoji="🌊" tone="teal" size="xs" /><span className="study-kp-name-text">{kp.name}</span></span>
                        </div>
                      </div>
                      <div className="study-kp-progress">
                        <span className="study-kp-pct">掌握率 {kp.mastery}%</span>
                        <div className="study-kp-bar">
                          <div className="study-kp-bar-fill" style={{ width: `${kp.mastery}%`, background: lv.color }} />
                        </div>
                        <span className="sll-level" style={{ background: lv.color }}>{lv.label}</span>
                      </div>
                      <div className="study-kp-actions">
                        {ACTION_BUTTONS.map((a) => (
                          <button
                            key={a.key}
                            type="button"
                            title={a.label}
                            className={`study-kp-act ${a.color}`}
                            onClick={(e) => {
                              e.stopPropagation();
                              navigateToAction(kp, a);
                            }}
                          >
                            <IconTile emoji={a.icon} tone={a.tone} size="xs" className="study-kp-act-ico" />
                            <span>{a.short}</span>
                          </button>
                        ))}
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* 选中小节（真实）/ 知识点（mock）：下方展开既有学习内容 */}
            {realMode && selectedSection && (
              <div className="study-kp-detail study-kp-detail-inline">
                <div className="study-kp-detail-title"><IconTile emoji="🌊" tone="teal" size="sm" /> {selectedSection.name}</div>
                {sectionContent ? (
                  <>
                    <div className="study-kp-detail-desc">
                      <b>学习目标：</b>{sectionContent.objective || '—'}
                    </div>
                    <div className="study-kp-detail-desc">
                      <b>内容概述：</b>{sectionContent.overview || '—'}
                    </div>
                    <div className="study-kp-detail-guide">
                      <div className="study-kp-guide-title"><IconTile emoji="📖" tone="blue" size="xs" /> 讲解要点</div>
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
                  <div className="study-kp-guide-title"><IconTile emoji="🧭" tone="teal" size="xs" /> 学习引导</div>
                  <div className="study-kp-guide-step">① 点击该小节行内的「预习」了解核心内容</div>
                  <div className="study-kp-guide-step">② 进入「专项练习湾」完成练习获得学习币</div>
                  <div className="study-kp-guide-step">③ 掌握度达标后「试炼检测」检验成果</div>
                  <div className="study-kp-guide-step">④ 订正「知识点错题本」中的错题</div>
                </div>
              </div>
            )}
            {!realMode && selectedKp && (
              <div className="study-kp-detail study-kp-detail-inline">
                <div className="study-kp-detail-title">
                  <IconTile emoji="🌊" tone="teal" size="sm" /> {selectedKp.name}
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
                  <div className="study-kp-guide-title"><IconTile emoji="🧭" tone="teal" size="xs" /> 学习引导</div>
                  <div className="study-kp-guide-step">① 点击该小节行内的「预习」了解核心内容</div>
                  <div className="study-kp-guide-step">② 进入「专项练习湾」完成练习获得学习币</div>
                  <div className="study-kp-guide-step">③ 掌握度达标后「试炼检测」检验成果</div>
                  <div className="study-kp-guide-step">④ 订正「知识点错题本」中的错题</div>
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      {/* ---- 右栏: 学习助手（四个快捷操作已移入各小节行内） ---- */}
      <aside className="sll-card study-right">
        <div className="study-right-title"><IconTile emoji="🐬" tone="sky" size="sm" /> 学习助手</div>

        {/* AI 小鲸向导 */}
        <div className="study-ai">
          <div className="study-ai-title"><IconTile emoji="🐬" tone="sky" size="xs" /> AI 小鲸向导</div>
          <div className="study-ai-text">{advice.text}</div>
        </div>
      </aside>
    </div>
  );
}
