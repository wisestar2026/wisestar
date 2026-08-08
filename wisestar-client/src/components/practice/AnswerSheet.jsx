/**
 * AnswerSheet.jsx - 练习答题卡（题号网格，快速跳题）
 *
 * 功能:
 *   1. 以网格展示全部题号
 *   2. 状态着色:
 *      - 已确认判题（蓝底白字）: 专项/随机模式下已点「确认提交」
 *      - 已作答未确认（白底蓝框）: 已选答案但未确认提交
 *      - 已作答（蓝底白字）: 套卷模式下只要选了答案即算已答
 *      - 未答（灰）
 *      - 当前题（深色描边）
 *   3. 点击题号跳转到对应题目
 *
 * 被谁引用: PracticeSessionPage（练习答题页侧栏/弹层）
 *
 * Props:
 *   total: number - 总题数
 *   current: number - 当前题号（从 1 开始）
 *   answeredSet: Set<number> - 已答题号集合（从 1 开始，已选答案即算）
 *   confirmedSet: Set<number> - 已确认判题题号集合（从 1 开始，仅专项/随机模式）
 *   isExam: boolean - 是否套卷模式（套卷无确认步骤）
 *   onJump: (index) => void - 跳题回调（index 从 0 开始）
 */

import { Typography } from 'antd';

const { Text } = Typography;

export default function AnswerSheet({ total, current, answeredSet, confirmedSet, isExam, onJump }) {
  // 生成题号列表 1..total
  const numbers = Array.from({ length: total }, (_, i) => i + 1);

  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
      {numbers.map((n) => {
        const answered = answeredSet.has(n);
        const confirmed = confirmedSet?.has(n) || false;
        const isCurrent = n === current;

        // 状态判定: 专项/随机模式下「确认」优先于「已答」
        const isBlue = isExam ? answered : confirmed;
        const isOutline = !isExam && answered && !confirmed;

        const style = {
          width: 36, height: 36, borderRadius: 6, cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 14, userSelect: 'none', transition: 'all .15s',
          background: isBlue ? '#1677ff' : '#f5f5f5',
          color: isBlue ? '#fff' : '#666',
          border: isCurrent
            ? '2px solid #722ed1'
            : isBlue
              ? '2px solid #1677ff'
              : isOutline
                ? '2px solid #1677ff'
                : '1px solid #d9d9d9',
        };
        return (
          <div key={n} style={style} onClick={() => onJump(n - 1)}>
            {n}
          </div>
        );
      })}
    </div>
  );
}
