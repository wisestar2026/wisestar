/**
 * MathSymbolBar.jsx - 填空答题辅助数学符号工具条
 *
 * 用途: 学员作答填空类题目时，普通键盘难以输入 ×、÷、√、² 等数学符号，
 *       点击本工具条的符号即可插入到目标输入框的光标处（插入逻辑由父组件处理）。
 *
 * Props:
 *   onPick: (symbol: string) => void  点击某个符号的回调
 *   disabled: boolean                 锁定态（判题后不可再插入）
 *   label: string                     工具条前缀说明文字（可选）
 *
 * 被谁引用: QuestionCard（单项填空 / 多项填空的文本空位）
 */

import { Button, Space } from 'antd';

// 与题库答案中实际出现的符号保持一致（×、÷、√、²、−、≈、°、·、… 等）
const MATH_SYMBOLS = ['×', '÷', '√', '²', '³', '−', '≈', '≠', '°', '·', '…'];

export default function MathSymbolBar({ onPick, disabled, label }) {
  return (
    <Space size={4} wrap style={{ marginTop: 6 }}>
      {label && <span style={{ color: '#999', fontSize: 12 }}>{label}</span>}
      {MATH_SYMBOLS.map((s) => (
        <Button
          key={s}
          size="small"
          disabled={disabled}
          onClick={() => onPick?.(s)}
          style={{ minWidth: 32, padding: '0 6px', fontSize: 15, lineHeight: 1.4 }}
        >
          {s}
        </Button>
      ))}
    </Space>
  );
}
