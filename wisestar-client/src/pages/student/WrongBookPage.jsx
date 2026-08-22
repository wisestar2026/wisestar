/**
 * WrongBookPage.jsx - 学员端错题本
 *
 * 功能:
 *   1. 展示当前学员的错题列表（练习判分后自动记录，/api/practice/wrong-list）
 *   2. 每题展示：题干/题型/我的答案/错误次数/最后错误时间/所属练习
 *   3. 空态提示（无错题）
 *
 * URL: /student/wrong（受 AuthGuard 保护，学员端）
 * 被谁引用: App.jsx 路由；学员端底部导航「错题本」
 *
 * 数据流: listWrongQuestions({ current, pageSize }) → 错题分页列表
 */

import { useEffect, useState, useCallback } from 'react';
import { listWrongQuestions } from '../../api/practice';
import './WrongBookPage.css';

const TYPE_LABEL = {
  Radio: '单选',
  Checkbox: '多选',
  FillBlank: '填空',
  Judge: '判断',
  Text: '简答',
};

const formatTime = (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '-');

export default function WrongBookPage() {
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const loadList = useCallback(() => {
    setLoading(true);
    listWrongQuestions({ current, pageSize })
      .then((res) => {
        setList(res?.data?.list || []);
        setTotal(res?.data?.total || 0);
      })
      .catch(() => setList([]))
      .finally(() => setLoading(false));
  }, [current, pageSize]);

  useEffect(() => {
    loadList();
  }, [loadList]);

  return (
    <div className="sll-page-enter wrong-book">
      <div className="sll-card wrong-card">
        <div className="wrong-title">📕 我的错题本</div>
        <div className="wrong-sub">练习中答错的题目自动收录，共 {total} 题</div>

        {loading && <div className="wrong-empty">加载中…</div>}
        {!loading && list.length === 0 && (
          <div className="wrong-empty">
            <div className="wrong-empty-icon">🎉</div>
            <div>暂无错题，继续保持！</div>
          </div>
        )}

        {list.map((item) => (
          <div key={item.questionId} className="wrong-item">
            <div className="wrong-item-head">
              <span className="wrong-type">{TYPE_LABEL[item.questionType] || item.questionType || '题目'}</span>
              <span className="wrong-repo">{item.repoName || '练习'}</span>
            </div>
            <div className="wrong-question">{item.questionTitle}</div>
            <div className="wrong-answer">我的答案：{item.lastAnswer || '—'}</div>
            <div className="wrong-meta">
              错误 {item.wrongCount ?? 0} 次 · 最后错误 {formatTime(item.lastWrongTime)}
            </div>
          </div>
        ))}

        {total > pageSize && (
          <div className="wrong-pager">
            <button disabled={current <= 1} onClick={() => setCurrent((c) => c - 1)}>上一页</button>
            <span>{current} / {Math.ceil(total / pageSize)}</span>
            <button disabled={current >= Math.ceil(total / pageSize)} onClick={() => setCurrent((c) => c + 1)}>下一页</button>
          </div>
        )}
      </div>
    </div>
  );
}
