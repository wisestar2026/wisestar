/**
 * CycleDecimalInput.jsx - 循环小数循环点输入器（需求 §4 方案 B）
 *
 * 交互:
 *   1. 输入整数部分与小数部分（仅数字）；
 *   2. 小数部分每一位可点选，标记「循环节」起点：被标记位及其后所有位构成循环节，
 *      其余为不循环部分（非循环 + 循环两段，与教材简便记法一致）；
 *   3. 再次点击同一起点或「清除循环点」可取消；「全部循环」用于 `0.1212…` 之类场景；
 *   4. 输出为 DISPLAY 循环点文本（如 `0.6̇`），判题时由 cycleDecimal 归一化后做数学等价判定。
 *
 * 约束（§4.3）: 循环点只允许加在小数位；循环节至少 1 位；同一位不会重复加点。
 *
 * Props:
 *   value: string    当前答案文本（STD / 循环点 / 省略号 / 有限小数均可解析回填）
 *   onChange: (string) => void  值变化回调，输出 DISPLAY 文本
 *   disabled: boolean 锁定态
 *
 * 被谁引用: QuestionCard（标准答案为 STD 循环小数的填空空位）
 */

import { useEffect, useRef, useState } from 'react';
import { Input, Button, Space, Typography } from 'antd';
import { parseInput, stdToDisplayText } from '../../utils/cycleDecimal';

const { Text } = Typography;

const onlyDigits = (v) => String(v || '').replace(/[^\d]/g, '');

export default function CycleDecimalInput({ value, onChange, disabled }) {
  const [intPart, setIntPart] = useState('0');
  const [fracPart, setFracPart] = useState('');
  // 循环节起始下标（null = 无循环点，即有限小数）
  const [cycleStart, setCycleStart] = useState(null);
  const lastEmitted = useRef(null);

  // 外部值变化时回填（跳过本组件刚发出的值，避免打断输入）
  useEffect(() => {
    if (value === lastEmitted.current) return;
    const obj = parseInput(value || '');
    if (!obj) {
      setIntPart('0');
      setFracPart('');
      setCycleStart(null);
      return;
    }
    setIntPart(obj.int || '0');
    if (obj.kind === 'cycle') {
      setFracPart((obj.nonrep || '') + (obj.rep || ''));
      setCycleStart((obj.nonrep || '').length);
    } else {
      setFracPart(obj.frac || '');
      setCycleStart(null);
    }
  }, [value]);

  // 由「整数 / 小数 / 循环节起点」生成 OBJ 与 DISPLAY 文本
  const buildAndEmit = (int, frac, start) => {
    const hasCycle = start != null && start >= 0 && start < frac.length;
    const obj = hasCycle
      ? { kind: 'cycle', int: int || '0', nonrep: frac.slice(0, start), rep: frac.slice(start) }
      : { kind: 'finite', int: int || '0', frac };
    const text = stdToDisplayText(obj);
    lastEmitted.current = text;
    onChange?.(text);
  };

  const handleIntChange = (e) => {
    const v = onlyDigits(e.target.value);
    setIntPart(v);
    buildAndEmit(v, fracPart, cycleStart);
  };

  const handleFracChange = (e) => {
    const v = onlyDigits(e.target.value);
    let nextStart = cycleStart;
    if (nextStart != null && nextStart >= v.length) nextStart = v.length ? v.length - 1 : null;
    setFracPart(v);
    setCycleStart(nextStart);
    buildAndEmit(intPart, v, nextStart);
  };

  // 点击第 i 位：以其为循环节起点；再次点击取消
  const toggleCycleAt = (i) => {
    if (disabled) return;
    const next = cycleStart === i ? null : i;
    setCycleStart(next);
    buildAndEmit(intPart, fracPart, next);
  };

  const setAllCycle = () => {
    if (disabled || !fracPart.length) return;
    setCycleStart(0);
    buildAndEmit(intPart, fracPart, 0);
  };

  const clearCycle = () => {
    if (disabled) return;
    setCycleStart(null);
    buildAndEmit(intPart, fracPart, null);
  };

  const hasCycle = cycleStart != null && cycleStart < fracPart.length;

  return (
    <div>
      <Space.Compact style={{ width: '100%' }}>
        <Input
          style={{ width: 96, flex: '0 0 96px' }}
          size="large"
          disabled={disabled}
          value={intPart}
          onChange={handleIntChange}
          placeholder="整数"
          inputMode="numeric"
        />
        <Input
          style={{ width: 32, flex: '0 0 32px', textAlign: 'center', pointerEvents: 'none' }}
          size="large"
          value="."
          readOnly
          tabIndex={-1}
        />
        <Input
          size="large"
          disabled={disabled}
          value={fracPart}
          onChange={handleFracChange}
          placeholder="小数部分（如 6、4897）"
          inputMode="numeric"
        />
      </Space.Compact>

      {/* 数字点选区：点击某位 → 该位起进入循环节 */}
      {fracPart.length > 0 && (
        <div style={{ marginTop: 8 }}>
          <Space size={[6, 6]} wrap>
            <Text type="secondary" style={{ fontSize: 12 }}>{intPart || '0'}.</Text>
            {[...fracPart].map((d, i) => (
              <span
                key={i}
                role="button"
                tabIndex={0}
                onClick={() => toggleCycleAt(i)}
                onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') toggleCycleAt(i); }}
                onContextMenu={(e) => { e.preventDefault(); toggleCycleAt(i); }}
                style={{
                  position: 'relative', display: 'inline-block', minWidth: 26, textAlign: 'center',
                  padding: '2px 6px', borderRadius: 6, fontSize: 18, lineHeight: 1.4,
                  border: `1px solid ${hasCycle && i >= cycleStart ? '#1677ff' : '#d9d9d9'}`,
                  background: hasCycle && i >= cycleStart ? '#e6f4ff' : '#fff',
                  cursor: disabled ? 'default' : 'pointer', userSelect: 'none',
                }}
              >
                {/* 循环点 */}
                {hasCycle && i >= cycleStart && (
                  <span
                    style={{
                      position: 'absolute', left: '50%', top: -2, width: 5, height: 5,
                      marginLeft: -2.5, borderRadius: '50%', background: '#1677ff',
                    }}
                  />
                )}
                {d}
              </span>
            ))}
          </Space>
          <div style={{ marginTop: 8 }}>
            <Space>
              <Button size="small" disabled={disabled || !fracPart.length} onClick={setAllCycle}>
                全部循环
              </Button>
              <Button size="small" disabled={disabled || cycleStart == null} onClick={clearCycle}>
                清除循环点
              </Button>
            </Space>
          </div>
        </div>
      )}

      {/* 即时预览 */}
      <div style={{ marginTop: 8 }}>
        <Text type="secondary" style={{ fontSize: 12 }}>
          预览：
          <Text code style={{ fontSize: 12 }}>
            {hasCycle
              ? `${intPart || '0'}.${fracPart.slice(0, cycleStart)}(${fracPart.slice(cycleStart)})`
              : `${intPart || '0'}${fracPart ? `.${fracPart}` : ''}`}
          </Text>
          <span style={{ marginLeft: 8 }}>点击小数位可标记循环节起点（可长按）</span>
        </Text>
      </div>
    </div>
  );
}
