/**
 * EnglishWrongBookPanel.jsx - 英语错题列表（学员端可复用）
 *
 * 功能:
 *   1. 按单元分组展示英语错题：知识点标签、错误次数、题目预览
 *   2. 「消灭错题」打开页内做题弹层，答对即从错题本移除
 *
 * 数据源: GET /api/english/student/wrong-questions（列表）
 *         GET /api/english/student/wrong-question（单题详情，重做用）
 *         POST /api/english/student/wrong/redo（答对消灭）
 *
 * 被谁引用:
 *   - EnglishReviewPage（「我的错题本」页签，传入 items 复用已加载数据）
 *   - WrongBookPage（英语学科下的 /student/wrong，自行拉取）
 */

import { useCallback, useEffect, useState } from 'react';
import { Empty, Tag } from 'antd';
import QuestionPreview from '../practice/QuestionPreview';
import WrongDrillModal from '../../pages/student/WrongDrillModal';
import {
  getEnglishWrongQuestions,
  getEnglishWrongQuestion,
  redoEnglishWrong,
} from '../../api/englishStudent';

/** 英语错题消灭不增补提高题（提高题数据源为数学专用），显式返回 null */
const noopHarder = async () => null;

/** 按单元分组，保留单元出现顺序 */
function groupByUnit(list) {
  const map = new Map();
  list.forEach((item) => {
    const key = item.unit || '未分单元';
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(item);
  });
  return Array.from(map.entries());
}

/**
 * @param {Object} props
 * @param {Array} [props.items] 外部已加载的错题列表；不传则组件自行请求
 * @param {Function} [props.onEliminated] 消灭一道错题后的回调
 * @param {string} [props.emptyText] 空态文案
 */
export default function EnglishWrongBookPanel({ items, onEliminated, emptyText = '暂无错题，继续加油' }) {
  const [list, setList] = useState(items || []);
  const [drillItem, setDrillItem] = useState(null);

  const load = useCallback(() => {
    getEnglishWrongQuestions({ limit: 100 })
      .then((res) => setList(res?.data || []))
      .catch(() => setList([]));
  }, []);

  useEffect(() => {
    if (items) {
      setList(items);
    } else {
      load();
    }
  }, [items, load]);

  const fetchWrongQuestion = useCallback(async (item) => {
    const res = await getEnglishWrongQuestion({ questionId: item.questionId });
    return res?.data;
  }, []);

  const handleEliminated = useCallback(() => {
    setDrillItem(null);
    if (!items) load();
    onEliminated?.();
  }, [items, load, onEliminated]);

  if (list.length === 0) {
    return <Empty description={emptyText} image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  return (
    <>
      <div className="eng-wrong-list">
        {groupByUnit(list.map((w) => ({ ...w, unit: w.chapter }))).map(([unit, group]) => (
          <div className="eng-rv-wrong-group" key={unit}>
            <div className="eng-rv-wrong-head">
              <span className="eng-rv-wrong-unit">{unit}</span>
              <span className="eng-rv-wrong-count">错题 {group.length} 道</span>
            </div>
            {group.map((w) => (
              <div className="eng-wrong-item" key={w.questionId}>
                <div className="eng-wrong-head">
                  {w.knowledgePoint && <Tag color="blue">{w.knowledgePoint}</Tag>}
                  <span className="eng-wrong-times">做错 {(w.wrongCount || 1)} 次</span>
                  <span style={{ marginLeft: 'auto' }}>
                    <button type="button" className="eng-btn-eliminate eng-btn" onClick={() => setDrillItem(w)}>
                      消灭错题
                    </button>
                  </span>
                </div>
                <QuestionPreview question={w.question} />
              </div>
            ))}
          </div>
        ))}
      </div>

      <WrongDrillModal
        open={Boolean(drillItem)}
        mode="single"
        originals={drillItem ? [drillItem] : []}
        onClose={() => setDrillItem(null)}
        onEliminated={handleEliminated}
        fetchOriginal={fetchWrongQuestion}
        redoQuestion={redoEnglishWrong}
        fetchHarder={noopHarder}
      />
    </>
  );
}
