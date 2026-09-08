/**
 * WrongBookPage.jsx - 学员端错题本（按研习学科隔离 + 多级筛选 + 消灭错题）
 *
 * 功能:
 *   1. 展示学员练习中的错题（/api/practice/wrong-list，含我的答案/正确答案/错误归因）
 *      - 错题归属跟随「研习章节学科」口径（与章节链路联动，非题库 subject 口径）
 *      - 自动带上当前学员与当前研习学科（useStudentStore.activeSubject）
 *   2. 支持筛选：年级 / 章节 / 小节 / 知识点 / 错因（级联刷新）
 *   3. 按「知识点」「错题原因」归纳展示
 *   4. 消灭功能（交卷制题目源，重新抽题）：
 *      - 消灭易错知识点：随机匹配 5 道该知识点的题目
 *      - 消灭错题：匹配 3 道与该题同题型的题目
 *
 * 历史错题兼容: 旧练习记录未强制错因标注，错因为空时展示「待标注」状态
 * （新交卷制练习强制逐题归因，此后错题均带错因）。
 *
 * URL: /student/wrong（受 AuthGuard 保护，学员端）
 * 被谁引用: App.jsx 路由；学员端底部导航「错题本」
 *
 * 数据流:
 *   listWrongQuestions({ userId, subjectId, grade?, chapterId?, sectionId?,
 *                        knowledgePointId?, wrongReason?, current, pageSize })
 *   → 错题列表（含知识点/章节/小节/学科/归因）
 * 消灭: navigate(/student/knowledge?repoId=/kpId=&types=&count=3&tab=practice)
 */

import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Select, Space, Spin, Tabs } from 'antd';
import { listWrongQuestions } from '../../api/practice';
import { getStudyChapters, getStudySections, getStudyPoints } from '../../api/student';
import useStudentStore, { GRADES } from '../../stores/useStudentStore';
import useUserStore from '../../stores/useUserStore';
import './WrongBookPage.css';

const TYPE_LABEL = {
  Radio: '单选', Checkbox: '多选', FillBlank: '填空', Judge: '判断', Text: '简答',
};
const REASONS = ['知识点不熟', '题型不会', '计算错误', '粗心大意', '审题不清', '时间不足'];
const formatTime = (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '-');

/** 错题卡片（含归因状态 + 消灭错题按钮） */
function WrongItem({ item, onEliminate }) {
  return (
    <div className="wrong-item">
      <div className="wrong-item-head">
        <Space size={6}>
          <span className="wrong-type">{TYPE_LABEL[item.questionType] || item.questionType || '题目'}</span>
          <span className="wrong-repo">{item.repoName || '练习'}</span>
          <span className="wrong-kp">{item.knowledgePointName || item.sectionName || item.chapterName || '未归知识点'}</span>
        </Space>
      </div>
      <div className="wrong-question">{item.questionTitle}</div>
      <div className="wrong-answer">我的答案：{item.lastAnswer || '未作答'}</div>
      <div className="wrong-meta">
        错误 {item.wrongCount ?? 0} 次 · 最后错误 {formatTime(item.lastWrongTime)}
        {item.wrongReason
          ? <span className="wrong-reason-tag">错因：{item.wrongReason}</span>
          : <span className="wrong-reason-pending">错因待标注</span>}
      </div>
      <button className="wrong-eliminate-btn" onClick={() => onEliminate(item)}>
        消灭错题（3 道同题型，重新抽题）
      </button>
    </div>
  );
}

export default function WrongBookPage() {
  const navigate = useNavigate();
  const { user } = useUserStore();
  const { activeSubject, grade, getVisibleSubjects } = useStudentStore();
  const visibleSubjects = getVisibleSubjects();
  const subjectName = (visibleSubjects.find((s) => s.id === activeSubject))?.name
    || (visibleSubjects.find((s) => String(s.id) === String(activeSubject)))?.name || '当前学科';

  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  // 级联筛选（错题归属按研习学科，学科取当前 activeSubject）
  const [fGrade, setFGrade] = useState(grade);
  const [chapterId, setChapterId] = useState(undefined);
  const [sectionId, setSectionId] = useState(undefined);
  const [kpId, setKpId] = useState(undefined);
  const [reason, setReason] = useState(undefined);
  // 级联选项
  const [chapters, setChapters] = useState([]);
  const [sections, setSections] = useState([]);
  const [points, setPoints] = useState([]);

  // 学科/年级变化 → 重置级联并重新拉章节
  useEffect(() => {
    setChapterId(undefined);
    setSectionId(undefined);
    setKpId(undefined);
    setFGrade(grade);
    setSections([]);
    setPoints([]);
    if (!activeSubject) { setChapters([]); return; }
    getStudyChapters(activeSubject, grade)
      .then((res) => setChapters(res?.data || []))
      .catch(() => setChapters([]));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeSubject, grade]);

  // 筛选年级变化 → 刷新章节并重置下级
  const onGradeChange = (g) => {
    setFGrade(g);
    setChapterId(undefined);
    setSectionId(undefined);
    setKpId(undefined);
    setSections([]);
    setPoints([]);
    if (activeSubject) {
      getStudyChapters(activeSubject, g)
        .then((res) => setChapters(res?.data || []))
        .catch(() => setChapters([]));
    }
  };

  const onChapterChange = (id) => {
    setChapterId(id);
    setSectionId(undefined);
    setKpId(undefined);
    setPoints([]);
    if (id) {
      getStudySections(id).then((res) => setSections(res?.data || [])).catch(() => setSections([]));
    } else {
      setSections([]);
    }
  };

  const onSectionChange = (id) => {
    setSectionId(id);
    setKpId(undefined);
    if (id) {
      getStudyPoints(id).then((res) => setPoints(res?.data || [])).catch(() => setPoints([]));
    } else {
      setPoints([]);
    }
  };

  // 拉错题列表（当前学员 + 当前学科 + 级联筛选）
  const loadList = useCallback(() => {
    setLoading(true);
    const params = {
      current: 1,
      pageSize: 200,
      userId: user?.id || undefined,
      subjectId: activeSubject || undefined,
    };
    if (fGrade && fGrade !== grade) params.grade = fGrade;
    if (chapterId) params.chapterId = chapterId;
    if (sectionId) params.sectionId = sectionId;
    if (kpId) params.knowledgePointId = kpId;
    if (reason) params.wrongReason = reason;
    listWrongQuestions(params)
      .then((res) => setList(res?.data?.list || []))
      .catch(() => setList([]))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id, activeSubject, fGrade, grade, chapterId, sectionId, kpId, reason]);

  useEffect(() => {
    loadList();
  }, [loadList]);

  // 消灭错题：3 道同题型（从错题所属题库/知识点/小节出题）
  const eliminateWrong = (item) => {
    const base = item.repoId
      ? `repoId=${item.repoId}`
      : (item.knowledgePointId ? `kpId=${item.knowledgePointId}` : (item.sectionId ? `sectionId=${item.sectionId}` : `kpId=`));
    navigate(`/student/knowledge?${base}&types=${encodeURIComponent(item.questionType || 'Radio')}&count=3&tab=practice`);
  };

  // 消灭易错知识点：5 道该知识点题目
  const eliminateKp = (kpId) => {
    navigate(`/student/knowledge?kpId=${kpId}&count=5&tab=practice`);
  };

  // 按知识点归纳
  const byKp = {};
  list.forEach((item) => {
    const key = item.knowledgePointName || item.knowledgePointId || '未归知识点';
    (byKp[key] = byKp[key] || { id: item.knowledgePointId, name: key, items: [] }).items.push(item);
  });
  // 按错题原因归纳（未归因错题合并到「待标注」组）
  const byReason = {};
  list.forEach((item) => {
    const key = item.wrongReason || '待标注';
    (byReason[key] = byReason[key] || []).push(item);
  });

  const filterBar = (
    <div className="wrong-filter-bar">
      <Space wrap>
        <span className="wrong-filter-subject">{subjectName}</span>
        <Select
          allowClear placeholder="全部年级" style={{ width: 120 }} value={fGrade}
          onChange={onGradeChange}
          options={GRADES.map((g) => ({ value: g, label: g }))}
        />
        <Select
          allowClear placeholder="全部章节" style={{ width: 150 }} value={chapterId}
          onChange={onChapterChange}
          options={chapters.map((c) => ({ value: c.id, label: c.name }))}
        />
        <Select
          allowClear placeholder="全部小节" style={{ width: 150 }} value={sectionId}
          onChange={onSectionChange} disabled={!chapterId}
          options={sections.map((s) => ({ value: s.id, label: s.name }))}
        />
        <Select
          allowClear placeholder="全部知识点" style={{ width: 160 }} value={kpId}
          onChange={(v) => { setKpId(v); }} disabled={!sectionId}
          options={points.map((p) => ({ value: p.id, label: p.name }))}
        />
        <Select
          allowClear placeholder="全部错因" style={{ width: 130 }} value={reason}
          onChange={(v) => { setReason(v); }}
          options={REASONS.map((r) => ({ value: r, label: r }))}
        />
      </Space>
    </div>
  );

  const emptyEl = (
    <div className="wrong-empty">
      <div className="wrong-empty-icon">🎉</div>
      <div>当前筛选下暂无错题，继续保持！</div>
    </div>
  );

  const allTab = (
    <div>
      {loading && <div className="wrong-loading"><Spin /></div>}
      {!loading && list.length === 0 && emptyEl}
      {list.map((item) => <WrongItem key={item.questionId} item={item} onEliminate={eliminateWrong} />)}
    </div>
  );

  const kpTab = (
    <div>
      {loading && <div className="wrong-loading"><Spin /></div>}
      {!loading && Object.keys(byKp).length === 0 && <div className="wrong-empty">当前筛选下暂无错题</div>}
      {Object.entries(byKp).map(([name, group]) => (
        <div key={name} className="wrong-group">
          <div className="wrong-group-head">
            <b>📌 {name}</b>
            <span style={{ fontSize: 12, color: '#90a4ae' }}>{group.items.length} 题</span>
            <button
              className="wrong-eliminate-btn small"
              onClick={() => eliminateKp(group.id)}
              disabled={!group.id}
            >
              消灭易错知识点（5 道）
            </button>
          </div>
          {group.items.map((item) => <WrongItem key={item.questionId} item={item} onEliminate={eliminateWrong} />)}
        </div>
      ))}
    </div>
  );

  const reasonTab = (
    <div>
      {loading && <div className="wrong-loading"><Spin /></div>}
      {!loading && Object.keys(byReason).length === 0 && <div className="wrong-empty">当前筛选下暂无错题</div>}
      {Object.entries(byReason).map(([reasonName, items]) => (
        <div key={reasonName} className="wrong-group">
          <div className="wrong-group-head">
            <b>🔍 {reasonName}</b>
            <span style={{ fontSize: 12, color: '#90a4ae' }}>{items.length} 题</span>
          </div>
          {items.map((item) => <WrongItem key={item.questionId} item={item} onEliminate={eliminateWrong} />)}
        </div>
      ))}
    </div>
  );

  return (
    <div className="sll-page-enter wrong-book">
      <div className="sll-card wrong-card">
        <div className="wrong-title">📕 我的错题本</div>
        <div className="wrong-sub">
          共 {list.length} 题 · 错题归属跟随研习学科（{subjectName}）
        </div>
        {filterBar}
        <Tabs
          items={[
            { key: 'all', label: '全部错题', children: allTab },
            { key: 'kp', label: '按知识点', children: kpTab },
            { key: 'reason', label: '按错题原因', children: reasonTab },
          ]}
        />
      </div>
    </div>
  );
}
