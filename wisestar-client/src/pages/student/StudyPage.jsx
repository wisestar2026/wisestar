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

import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { message } from 'antd';
import useStudentStore, { SUBJECTS, masteryLevel } from '../../stores/useStudentStore';
import { getStudySections, uploadActivity, getStudentStats } from '../../api/student';
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
    activeSubject, version, grade, term, pureMode,
    studyContent, fetchStudyChapters, fetchStudyProgress, getVisibleSubjects,
  } = useStudentStore();
  const subject = SUBJECTS.find((s) => s.key === activeSubject) || SUBJECTS[1];

  // 全局册别（上册/下册）→ 数学章节 term 口径（上/下）
  const termShort = term === '下册' ? '下' : '上';

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

  // 章节数据：真实模式用真实章节（按全局册别过滤，无 term 的章节保留）；否则用 mock 学科章节
  const chapters = realMode
    ? (realChapters || []).filter((c) => !c.term || c.term === termShort)
    : subject.chapters;

  // 本学期学习情况：学习统计（练习量/正确率/积分/学币）
  const [stats, setStats] = useState(null);
  useEffect(() => {
    if (!realMode) {
      setStats(null);
      return;
    }
    getStudentStats()
      .then((res) => setStats(res?.data || null))
      .catch(() => setStats(null));
  }, [realMode]);

  // 学科/年级/册别切换 → 按订单授权年级加载真实章节
  useEffect(() => {
    if (realMode) {
      fetchStudyChapters(activeSubject, grade);
    }
    setSelectedChapterId(null);
    setSelectedSection(null);
    setSelectedKp(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeSubject, grade, termShort, realMode]);

  // 学科/版本/册别切换 → 加载真实掌握度/薄弱（章节 → 知识点）
  useEffect(() => {
    if (realMode) {
      fetchStudyProgress(activeSubject, version, grade, termShort);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeSubject, version, grade, termShort, realMode]);

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

  // 研习首页看板：本学期重点（管理员标注重点程度）+ 学习情况 + 优势/不足
  const dash = useMemo(() => {
    const IMP_RANK = { core: 3, key: 2, normal: 1 };
    const rankLabel = (r) => (r >= 3 ? 'core' : r === 2 ? 'key' : r === 1 ? 'normal' : '');
    const sectionMap = new Map();
    const strengths = [];
    const weaknesses = [];
    let totalKps = 0;
    let masterySum = 0;
    let weakCount = 0;
    (studyContent.progress?.chapters || []).forEach((ch) => {
      (ch.kps || []).forEach((kp) => {
        const mastery = kp.mastery || 0;
        totalKps += 1;
        masterySum += mastery;
        if (kp.weak) weakCount += 1;
        if (!sectionMap.has(kp.sectionId)) {
          sectionMap.set(kp.sectionId, {
            id: kp.sectionId,
            name: kp.sectionName || '未命名小节',
            chapterId: ch.id,
            chapterName: ch.name,
            icon: ch.icon,
            rank: 0,
            kps: [],
            masterySum: 0,
          });
        }
        const sec = sectionMap.get(kp.sectionId);
        sec.kps.push(kp);
        sec.masterySum += mastery;
        sec.rank = Math.max(sec.rank, IMP_RANK[kp.sectionImportance] || 0, IMP_RANK[kp.importance] || 0);
        if (mastery >= 70) strengths.push({ ...kp, chapterName: ch.name, icon: ch.icon });
        if (kp.weak || mastery < 40) weaknesses.push({ ...kp, chapterName: ch.name, icon: ch.icon });
      });
    });
    const sections = [...sectionMap.values()].map((s) => ({
      ...s,
      importance: rankLabel(s.rank),
      mastery: s.kps.length ? Math.round(s.masterySum / s.kps.length) : 0,
    }));
    const completedSections = sections.filter((s) => s.mastery >= 60).length;
    let keySections = sections.filter((s) => s.rank >= 2).sort((a, b) => b.rank - a.rank || a.mastery - b.mastery);
    const keyFallback = keySections.length === 0;
    if (keyFallback) {
      // 管理员尚未标注重点时，按知识点密度给出参考重点，避免空白
      keySections = [...sections].sort((a, b) => b.kps.length - a.kps.length).slice(0, 6);
    }
    const keyChapterMap = new Map();
    keySections.forEach((s) => {
      if (!keyChapterMap.has(s.chapterId)) {
        keyChapterMap.set(s.chapterId, { id: s.chapterId, name: s.chapterName, icon: s.icon, sections: [] });
      }
      keyChapterMap.get(s.chapterId).sections.push(s);
    });
    strengths.sort((a, b) => b.mastery - a.mastery);
    weaknesses.sort((a, b) => a.mastery - b.mastery);
    return {
      totalKps,
      weakCount,
      avgMastery: totalKps ? Math.round(masterySum / totalKps) : 0,
      sectionTotal: sections.length,
      completedSections,
      keyChapters: [...keyChapterMap.values()],
      keyFallback,
      strengths,
      weaknesses,
    };
  }, [studyContent.progress]);

  const coinsTotal = (stats?.coinsBySubject || []).reduce((n, c) => n + (c.coins || 0), 0);

  // 看板点击：定位到某小节的章节列表并展开该小节
  const focusSection = (s) => {
    setSelectedChapterId(s.chapterId);
    setSelectedKp(null);
    const pick = (list) => setSelectedSection((list || []).find((x) => x.id === s.id) || null);
    if (!realMode) {
      setSelectedSection(null);
      return;
    }
    if (sectionsMap[s.chapterId]) {
      pick(sectionsMap[s.chapterId]);
      return;
    }
    getStudySections(s.chapterId)
      .then((res) => {
        const list = res?.data || [];
        setSectionsMap((m) => ({ ...m, [s.chapterId]: list }));
        pick(list);
      })
      .catch(() => setSectionsMap((m) => ({ ...m, [s.chapterId]: [] })));
  };

  // 看板点击：进入某知识点小节的预习
  const openKp = (kp) => {
    const nm = kp.name ? `&name=${encodeURIComponent(kp.name)}` : '';
    navigate(`/student/knowledge?sectionId=${kp.sectionId}${nm}&tab=preview`);
  };

  return (
    <div className="sll-page-enter study-page">
      {/* ---- 左栏: 章节学海洲岛导航 ---- */}
      <aside className="sll-card study-left">
        <div className="study-left-title">
          <IconTile emoji="🗺️" tone="teal" size="sm" /> 学海洲岛 · {realSubject?.name || subject.name}
          {!pureMode && <span className="study-left-sub">{realMode ? `${chapters.length} 个章节` : `进度 ${avgProgress}%`}</span>}
        </div>
        {realMode && studyContent.chaptersFailed && (
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
            /* 真实模式未选章节: 研习首页看板（重点 / 学习情况 / 优势与不足） */
            <div className="study-dashboard">
              <div className="study-overview-title">
                <IconTile emoji="🌊" tone="sky" size="sm" /> {realSubject?.name || subject.name} · 研习首页（{version} · {grade} · {term}）
              </div>

              <section className="study-dash-block">
                <div className="study-dash-heading">本学期学习情况</div>
                <div className="study-stat-grid">
                  <div className="study-stat-card">
                    <div className="study-stat-num">{dash.completedSections}<i>/{dash.sectionTotal}</i></div>
                    <div className="study-stat-label">完成小节</div>
                  </div>
                  <div className="study-stat-card">
                    <div className="study-stat-num">{dash.avgMastery}<i>%</i></div>
                    <div className="study-stat-label">平均掌握度</div>
                  </div>
                  <div className="study-stat-card">
                    <div className="study-stat-num">{stats?.totalQuestions ?? 0}<i> 题</i></div>
                    <div className="study-stat-label">累计练习 · 正确率 {stats?.accuracy ?? 0}%</div>
                  </div>
                  <div className="study-stat-card">
                    <div className="study-stat-num">{stats?.totalPoints ?? 0}<i> 分</i></div>
                    <div className="study-stat-label">学海积分 · 学习币 {coinsTotal}</div>
                  </div>
                  <div className="study-stat-card">
                    <div className="study-stat-num">{dash.weakCount}<i> 个</i></div>
                    <div className="study-stat-label">薄弱知识点</div>
                  </div>
                </div>
              </section>

              <div className="study-dash-cols">
                <section className="study-dash-block">
                  <div className="study-dash-heading">
                    本学期重点
                    {dash.keyFallback && <span className="study-dash-note">暂无标注，按知识点密度推荐</span>}
                  </div>
                  {dash.keyChapters.length === 0 ? (
                    <div className="study-empty">本学期暂无可推荐的重点内容</div>
                  ) : (
                    dash.keyChapters.map((kc) => (
                      <div key={kc.id} className="study-dash-chapter">
                        <div className="study-dash-chapter-head" onClick={() => selectChapter(kc.id)}>
                          <IconTile emoji={kc.icon || '📖'} tone="blue" size="xs" /> {kc.name}
                        </div>
                        <div className="study-dash-secs">
                          {kc.sections.map((s) => (
                            <span key={s.id} className="study-dash-sec" onClick={() => focusSection(s)}>
                              {s.importance === 'core' ? <b className="study-dash-tag core">核心</b> : s.importance === 'key' ? <b className="study-dash-tag key">重点</b> : null}
                              {s.name}
                            </span>
                          ))}
                        </div>
                      </div>
                    ))
                  )}
                </section>

                <div className="study-dash-col">
                  <section className="study-dash-block">
                    <div className="study-dash-heading">优势 · 掌握较好</div>
                    {dash.strengths.length === 0 ? (
                      <div className="study-empty">继续学习后可查看优势知识点</div>
                    ) : (
                      dash.strengths.slice(0, 6).map((kp) => (
                        <div key={kp.id} className="study-dash-item good" onClick={() => openKp(kp)}>
                          <span className="study-dash-item-name">{kp.name}</span>
                          <span className="study-dash-item-meta">{kp.chapterName} · {kp.mastery}%</span>
                        </div>
                      ))
                    )}
                  </section>

                  <section className="study-dash-block">
                    <div className="study-dash-heading">不足 · 待巩固 / 薄弱</div>
                    {dash.weaknesses.length === 0 ? (
                      <div className="study-empty">暂无明显薄弱环节</div>
                    ) : (
                      dash.weaknesses.slice(0, 6).map((kp) => (
                        <div key={kp.id} className="study-dash-item bad" onClick={() => openKp(kp)}>
                          <span className="study-dash-item-name">{kp.name}</span>
                          <span className="study-dash-item-meta">{kp.chapterName} · {kp.mastery}%{kp.weak ? ' · 薄弱' : ''}</span>
                        </div>
                      ))
                    )}
                  </section>
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
                            <IconTile emoji={locked ? '🔒' : '🌊'} tone={locked ? 'slate' : 'teal'} size="sm" />
                            <span className="study-kp-name-text">{sec.name}</span>
                            <StarRating value={passRate} size={20} className="study-kp-stars" />
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
                            <IconTile emoji={a.icon} tone={a.tone} size="sm" className="study-kp-act-ico" />
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
                          <span className="study-kp-name"><IconTile emoji="🌊" tone="teal" size="sm" /><span className="study-kp-name-text">{kp.name}</span></span>
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
                            <IconTile emoji={a.icon} tone={a.tone} size="sm" className="study-kp-act-ico" />
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
