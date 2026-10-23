/**
 * EnglishPracticeHistoryModal.jsx - 英语练习「查看过往记录」弹窗
 *
 * 功能:
 *   1. 按模块 + 单元拉取历史作答列表（时间倒序，最多 20 条）
 *   2. 列表展示得分、答对题数、用时与交卷时间
 *   3. 点击某一次可查看该次的逐题作答与解析，可返回列表
 *
 * 被谁引用: pages/student/EnglishCenterPage.jsx（阅读理解 / 单元练习卡片）
 */

import { useEffect, useState } from 'react';
import { Button, Modal, Spin, message } from 'antd';
import { ArrowLeftOutlined, RightOutlined } from '@ant-design/icons';
import { getEnglishPracticeDetail, getEnglishPracticeHistory } from '../../api/englishStudent';
import { ResultItem } from './EnglishPracticeResult';

const totalQuestionsOf = (row) => row?.total || 0;

function formatTime(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 16);
}

function formatDuration(seconds) {
  const value = Number(seconds || 0);
  if (!value) return '';
  if (value < 60) return `${value} 秒`;
  const minutes = Math.floor(value / 60);
  const rest = value % 60;
  return rest ? `${minutes} 分 ${rest} 秒` : `${minutes} 分`;
}

export default function EnglishPracticeHistoryModal({ open, module, unit, title, onCancel }) {
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    setDetail(null);
    setLoading(true);
    getEnglishPracticeHistory({ module, unit, limit: 20 })
      .then((res) => setList(res?.data || []))
      .catch(() => message.error('历史记录加载失败'))
      .finally(() => setLoading(false));
  }, [open, module, unit]);

  const openDetail = (id) => {
    if (!id || detailLoading) return;
    setDetailLoading(true);
    getEnglishPracticeDetail(id)
      .then((res) => setDetail(res?.data || null))
      .catch(() => message.error('作答明细加载失败'))
      .finally(() => setDetailLoading(false));
  };

  const handleCancel = () => {
    setDetail(null);
    onCancel?.();
  };

  return (
    <Modal
      title={detail ? '作答详情' : `${title || '练习'} · 过往记录`}
      open={open}
      onCancel={handleCancel}
      footer={null}
      width={720}
      destroyOnHidden
    >
      {detailLoading ? (
        <div style={{ padding: 32, textAlign: 'center' }}><Spin /></div>
      ) : detail ? (
        <div>
          <div className="eng-hist-detail-head">
            <Button type="link" size="small" icon={<ArrowLeftOutlined />} onClick={() => setDetail(null)}>
              返回记录列表
            </Button>
            <span className="eng-hist-detail-meta">
              得分 <b>{detail.score}</b> · 答对 {detail.correct}/{detail.total}
              {' '}· {formatTime(detail.createAt)}
            </span>
          </div>
          <div className="kd-detail-list">
            {(detail.items || []).map((item, index) => (
              <ResultItem item={item} index={index} key={item.questionId || index} />
            ))}
            {!detail.items?.length && <div className="kd-dim-empty">这次作答没有留下逐题明细</div>}
          </div>
        </div>
      ) : loading ? (
        <div style={{ padding: 32, textAlign: 'center' }}><Spin /></div>
      ) : !list.length ? (
        <div className="kd-dim-empty">还没有作答记录，先练一次吧</div>
      ) : (
        <div className="eng-hist-list">
          {list.map((row) => (
            <button
              type="button"
              className="eng-hist-row"
              key={row.id}
              onClick={() => openDetail(row.id)}
            >
              <span className="eng-hist-score">
                {row.score}
                <small>分</small>
              </span>
              <span className="eng-hist-main">
                <span className="eng-hist-date">{formatTime(row.createAt)}</span>
                <span className="eng-hist-sub">
                  答对 {row.correct}/{totalQuestionsOf(row)} 题
                  {formatDuration(row.durationSeconds) ? ` · 用时 ${formatDuration(row.durationSeconds)}` : ''}
                </span>
              </span>
              <RightOutlined className="eng-hist-arrow" />
            </button>
          ))}
        </div>
      )}
    </Modal>
  );
}
