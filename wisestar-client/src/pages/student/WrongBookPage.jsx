/**
 * WrongBookPage.jsx - 学员端错题本（知识点/错题原因归纳 + 消灭错题）
 *
 * 功能:
 *   1. 展示学员练习中的错题（/api/practice/wrong-list，含我的答案/正确答案/错误归因）
 *   2. 按「知识点」「错题原因」两大方面归纳错题
 *   3. 消灭功能：
 *      - 消灭易错知识点：随机匹配 5 道该知识点的题目
 *      - 消灭错题：匹配 3 道与该题同题型的题目
 *
 * URL: /student/wrong（受 AuthGuard 保护，学员端）
 * 被谁引用: App.jsx 路由；学员端底部导航「错题本」
 *
 * 数据流: listWrongQuestions({ current, pageSize }) → 错题列表（含 knowledgePoint/题型/归因）
 * 消灭: navigate(/student/knowledge?kpId=&count=5 或 ?types=&count=3&tab=practice)
 */

import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Tabs, Button, Space } from 'antd';
import { listWrongQuestions } from '../../api/practice';
import './WrongBookPage.css';

const TYPE_LABEL = {
  Radio: '单选', Checkbox: '多选', FillBlank: '填空', Judge: '判断', Text: '简答',
};
const formatTime = (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '-');

/** 错题卡片（含消灭错题按钮） */
function WrongItem({ item, onEliminate }) {
  return (
    <div className="wrong-item">
      <div className="wrong-item-head">
        <Space size={6}>
          <span className="wrong-type">{TYPE_LABEL[item.questionType] || item.questionType || '题目'}</span>
          <span className="wrong-repo">{item.repoName || '练习'}</span>
          {item.knowledgePointName && <span className="wrong-kp">📌 {item.knowledgePointName}</span>}
        </Space>
      </div>
      <div className="wrong-question">{item.questionTitle}</div>
      <div className="wrong-answer">我的答案：{item.lastAnswer || '—'}</div>
      <div className="wrong-meta">
        错误 {item.wrongCount ?? 0} 次 · 最后错误 {formatTime(item.lastWrongTime)}
        {item.wrongReason && <span className="wrong-reason-tag">归因：{item.wrongReason}</span>}
      </div>
      <Button type="primary" size="small" style={{ marginTop: 8 }} onClick={() => onEliminate(item)}>
        🎯 消灭错题（3 道同题型）
      </Button>
    </div>
  );
}

export default function WrongBookPage() {
  const navigate = useNavigate();
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);

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

  // 消灭错题：3 道同题型（从错题所属练习或知识点出题）
  const eliminateWrong = (item) => {
    const base = item.repoId
      ? `repoId=${item.repoId}`
      : (item.knowledgePointId ? `kpId=${item.knowledgePointId}` : `sectionId=${encodeURIComponent('')}`);
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
  // 按错题原因归纳
  const byReason = {};
  list.forEach((item) => {
    const key = item.wrongReason || '未标注原因';
    (byReason[key] = byReason[key] || []).push(item);
  });

  const allTab = (
    <div>
      {loading && <div className="wrong-empty">加载中…</div>}
      {!loading && list.length === 0 && (
        <div className="wrong-empty">
          <div className="wrong-empty-icon">🎉</div>
          <div>暂无错题，继续保持！</div>
        </div>
      )}
      {list.map((item) => <WrongItem key={item.questionId} item={item} onEliminate={eliminateWrong} />)}
    </div>
  );

  const kpTab = (
    <div>
      {Object.keys(byKp).length === 0 && <div className="wrong-empty">暂无错题</div>}
      {Object.entries(byKp).map(([name, group]) => (
        <div key={name} className="wrong-group">
          <div className="wrong-group-head">
            <b>📌 {name}</b>
            <span style={{ fontSize: 12, color: '#90a4ae' }}>{group.items.length} 题</span>
            <Button type="primary" size="small" onClick={() => eliminateKp(group.id)} disabled={!group.id}>
              🎯 消灭易错知识点（5 道）
            </Button>
          </div>
          {group.items.map((item) => <WrongItem key={item.questionId} item={item} onEliminate={eliminateWrong} />)}
        </div>
      ))}
    </div>
  );

  const reasonTab = (
    <div>
      {Object.keys(byReason).length === 0 && <div className="wrong-empty">暂无错题</div>}
      {Object.entries(byReason).map(([reason, items]) => (
        <div key={reason} className="wrong-group">
          <div className="wrong-group-head">
            <b>🔍 {reason}</b>
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
        <div className="wrong-sub">共 {list.length} 题 · 按知识点/错题原因归纳，消灭错题</div>
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
