/**
 * QuestionCard.jsx - 练习单题渲染组件（一题一屏核心）
 *
 * 功能:
 *   1. 渲染题干 + 配图 + 题型/难度标签
 *   2. 按题型渲染交互控件:
 *      - 单选/判断 (Radio/Judge): 大按钮式单选
 *      - 多选 (Checkbox): 大按钮式多选（可连续勾选多个）
 *      - 单项填空 (FillBlank): 单输入框
 *      - 多项填空 (MultipleBlank): 多个输入框（空位数 = 标准答案按 | 拆分的空位数），
 *        答案按空位顺序以 | 拼接后判分（与后端 AnswerJudgeUtil 语义一致）
 *   3. 两步作答交互（专项刷题/随机练习）:
 *      第一步「作答」: 选择/修改答案（多选可累积），不判题、不锁定
 *      第二步「确认提交」: 点击确认按钮后判题，锁定答案，显示对错/正确答案/解析
 *   4. 套卷模拟模式: 只记录答案，无确认按钮，交卷后统一判分
 *
 * 被谁引用: PracticeSessionPage（练习答题页，一题一屏渲染）
 *
 * Props:
 *   question: Object  - 当前题目（TemplateView，含 template.attribute/children）
 *   index: number     - 题号（从 1 开始，展示用）
 *   total: number     - 总题数（展示用）
 *   value: Object     - 当前已选答案:
 *      单选/判断: { type: 'option', optionId }
 *      多选:      { type: 'options', optionIds: [] }
 *      填空/文本: { type: 'text', text }（多项填空的 text 为各空位答案以 | 拼接）
 *   onChange: (value) => void - 答案变化回调
 *   confirmed: Object   - 已确认判题结果缓存 { [questionId]: {correct, correctAnswers} }
 *   onConfirm: () => void - 确认提交回调（点击「确认提交」按钮触发判题）
 *   judgeMode: boolean - 是否需要确认判题（true=专项/随机，false=套卷）
 *
 * 专注力交互要点（对齐路线图 5.2/5.4）:
 *   - 大按钮式选项，悬停/选中高亮
 *   - 多选可连续勾选，确认提交后才判题（避免误触立即出答案打断思路）
 *   - 确认后正确绿/错误红 + 正确答案高亮 + 解析
 *   - 全屏聚焦，单题一屏
 */

import { Typography, Tag, Button, Space, Input, Image } from 'antd';
import {
  CheckCircleOutlined, CloseCircleOutlined,
} from '@ant-design/icons';
import { formatCorrectAnswers } from '../../utils/practiceHelpers';

const { Title, Text, Paragraph } = Typography;

// 题型中文映射（与 practiceHelpers / questionTypes 保持一致）
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Select: '下拉题',
  FillBlank: '单项填空', Text: '多行文本', Score: '评分题',
  Remark: '备注说明', Judge: '判断题', MultipleBlank: '多项填空',
};

// 难度映射
const DIFFICULTY_LABELS = {
  easy: ['简单', 'green'], medium: ['中等', 'orange'], hard: ['困难', 'red'],
};

export default function QuestionCard({
  question, index, total, value, onChange, confirmed, onConfirm, judgeMode,
}) {
  // 题目 schema（template 内嵌问卷 JSON）
  const schema = question?.template || {};
  const attr = schema.attribute || {};
  const children = schema.children || [];
  const qtype = question?.questionType;

  // 当前题判题结果（已确认的题存在，用于展示对错）
  const result = confirmed?.[question?.id];

  // 是否已锁定（确认提交后锁定不可改；回看只读）
  const locked = judgeMode && !!result && result.correct !== undefined;

  // 当前是否已选答案（确认按钮可用性依据）
  const hasValue = value != null && (
    (value.type === 'option' && !!value.optionId)
    || (value.type === 'options' && value.optionIds?.length > 0)
    || (value.type === 'text' && !!String(value.text || '').trim())
  );

  // ---- 选项样式 ----
  // 已确认时: 正确绿/错选红三态；未判分(null)仅选中高亮；未确认时: 仅选中高亮
  const optionStyle = (opt, selected) => {
    let border = selected ? '2px solid #1677ff' : '1px solid #d9d9d9';
    let bg = selected ? '#e6f4ff' : '#fff';
    if (judgeMode && result && result.correct !== null) {
      const optTitle = opt.title || '';
      const isCorrectOpt = result.correctAnswers.includes(optTitle);
      const isSelected = selected;
      if (isCorrectOpt) {
        // 正确答案恒绿（无论是否选中）
        border = '2px solid #52c41a';
        bg = '#f6ffed';
      } else if (isSelected) {
        // 选错的红
        border = '2px solid #ff4d4f';
        bg = '#fff2f0';
      }
    }
    return {
      border, background: bg, borderRadius: 8, padding: '12px 16px',
      cursor: locked ? 'default' : 'pointer', marginBottom: 12,
      display: 'flex', alignItems: 'center', gap: 10, fontSize: 15,
      transition: 'all .2s',
    };
  };

  // ---- 单选/判断选项点击（未锁定时可改选） ----
  const handleOptionClick = (opt) => {
    if (locked) return;
    onChange({ type: 'option', optionId: opt.id });
  };

  // ---- 多选选项点击（未锁定时可累积/取消勾选） ----
  const handleMultiClick = (opt) => {
    if (locked) return;
    const current = value?.type === 'options' ? [...value.optionIds] : [];
    const idx = current.indexOf(opt.id);
    if (idx >= 0) {
      current.splice(idx, 1);
    } else {
      current.push(opt.id);
    }
    onChange({ type: 'options', optionIds: current });
  };

  // ---- 填空/文本输入 ----
  const handleTextChange = (text) => {
    if (locked) return;
    onChange({ type: 'text', text });
  };

  // ---- 多项填空: 空位数与当前空位值 ----
  // 空位数优先取标准答案按 | 拆分的数量（题目编辑时空位必须与答案一致），
  // 无标准答案时回退到已填空位数 / 默认 2，保证练习端始终可作答
  const stdAnswerRaw = String(attr.examCorrectAnswer || '');
  const stdParts = stdAnswerRaw ? stdAnswerRaw.split('|') : [];
  const blankCount = qtype === 'MultipleBlank'
    ? (stdParts.length > 0 ? stdParts.length
      : ((value?.type === 'text' && String(value.text || '').split('|').length > 0)
        ? String(value.text).split('|').length : 2))
    : 0;
  const blankValues = qtype === 'MultipleBlank'
    ? (value?.type === 'text' ? String(value.text || '') : '').split('|')
    : [];

  // 多项填空某个空位变更: 各空位按序拼成 "空1|空2|..." 存入 text（与后端判分分隔一致）
  const handleBlankChange = (idx, text) => {
    if (locked) return;
    const arr = Array(blankCount).fill('');
    blankValues.forEach((v, i) => { arr[i] = v || ''; });
    arr[idx] = text;
    onChange({ type: 'text', text: arr.join('|') });
  };

  return (
    <div style={{ maxWidth: 760, margin: '0 auto' }}>
      {/* ---- 题号 + 题型 + 难度 ---- */}
      <Space style={{ marginBottom: 16 }}>
        <Tag color="blue">{TYPE_LABELS[qtype] || qtype}</Tag>
        {(() => {
          const d = DIFFICULTY_LABELS[question?.difficulty || attr.difficulty];
          return d ? <Tag color={d[1]}>{d[0]}</Tag> : null;
        })()}
        <Text type="secondary">第 {index} / {total} 题</Text>
      </Space>

      {/* ---- 题干 ---- */}
      <Title level={4} style={{ marginTop: 0, marginBottom: attr.examImages?.length ? 12 : 20, lineHeight: 1.6 }}>
        {index}. {schema.title || question?.name || '(未命名题目)'}
      </Title>

      {/* 配图（如有） */}
      {attr.examImages?.length > 0 && (
        <div style={{ marginBottom: 16 }}>
          {attr.examImages.map((img, i) => (
            <Image key={i} src={img} alt="题目配图" style={{ maxWidth: 480, marginBottom: 8 }} />
          ))}
        </div>
      )}

      {/* ---- 选项区 ---- */}
      {(qtype === 'Radio' || qtype === 'Judge' || qtype === 'Select') && (
        <div>
          {children.map((opt) => {
            const selected = value?.type === 'option' && value.optionId === opt.id;
            return (
              <div key={opt.id} style={optionStyle(opt, selected)} onClick={() => handleOptionClick(opt)}>
                {judgeMode && result && result.correct !== null && (
                  <>
                    {result.correctAnswers.includes(opt.title) && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
                    {selected && !result.correctAnswers.includes(opt.title) && <CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
                  </>
                )}
                <span>{opt.title}</span>
              </div>
            );
          })}
        </div>
      )}

      {(qtype === 'Checkbox') && (
        <div>
          {children.map((opt) => {
            const selected = value?.type === 'options' && value.optionIds.includes(opt.id);
            return (
              <div key={opt.id} style={optionStyle(opt, selected)} onClick={() => handleMultiClick(opt)}>
                {judgeMode && result && result.correct !== null && (
                  <>
                    {result.correctAnswers.includes(opt.title) && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
                    {selected && !result.correctAnswers.includes(opt.title) && <CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
                  </>
                )}
                <span>{opt.title}</span>
              </div>
            );
          })}
        </div>
      )}

      {(qtype === 'MultipleBlank') && (
        <div>
          {Array.from({ length: blankCount }).map((_, i) => {
            // 判题后逐空命中标记（1 对 / 0 错 / undefined 未判）
            const hit = locked ? result?.blankHits?.[i] : undefined;
            const inputStyle = hit === 1
              ? { borderColor: '#52c41a', background: '#f6ffed' }
              : hit === 0
                ? { borderColor: '#ff4d4f', background: '#fff2f0' }
                : undefined;
            return (
              <div key={i} style={{ marginBottom: 12 }}>
                <Input
                  size="large"
                  style={inputStyle}
                  prefix={<span style={{ color: '#999', fontSize: 13 }}>空位 {i + 1}</span>}
                  placeholder={`请输入第 ${i + 1} 个空位的答案`}
                  value={blankValues[i] || ''}
                  onChange={(e) => handleBlankChange(i, e.target.value)}
                  disabled={locked}
                />
              </div>
            );
          })}
          <Text type="secondary" style={{ fontSize: 12 }}>
            按题目空位顺序作答，全部空位填写后提交判分
          </Text>
        </div>
      )}

      {(qtype === 'FillBlank' || qtype === 'Text') && (
        <div>
          {qtype === 'FillBlank' ? (
            <Input
              size="large"
              placeholder="请输入答案"
              value={value?.type === 'text' ? value.text : ''}
              onChange={(e) => handleTextChange(e.target.value)}
              disabled={locked}
            />
          ) : (
            <Input.TextArea
              rows={3}
              placeholder="请输入答案"
              value={value?.type === 'text' ? value.text : ''}
              onChange={(e) => handleTextChange(e.target.value)}
              disabled={locked}
            />
          )}
        </div>
      )}

      {/* ---- 确认提交按钮（专项/随机模式，作答后手动确认才判题） ---- */}
      {judgeMode && !result && (
        <Button
          type="primary"
          size="large"
          block
          disabled={!hasValue}
          onClick={onConfirm}
          style={{ marginTop: 20, height: 44 }}
        >
          确认提交
        </Button>
      )}

      {/* ---- 判题反馈区（确认后显示） ---- */}
      {judgeMode && result && (() => {
        // 多空填空部分命中（答对 ≥1 空但未全对）单独呈现为"部分正确"
        const hitCount = result.blankTotal
          ? (result.blankHits || []).filter((h) => h === 1).length : 0;
        const partial = result.correct === 0 && result.blankTotal > 0 && hitCount > 0;
        return (
          <div
            style={{
              marginTop: 20, padding: '14px 18px', borderRadius: 8,
              background: result.correct === 1 ? '#f6ffed' : partial ? '#fffbe6' : result.correct === 0 ? '#fff2f0' : '#fafafa',
              border: `1px solid ${result.correct === 1 ? '#b7eb8f' : partial ? '#ffe58f' : result.correct === 0 ? '#ffa39e' : '#e8e8e8'}`,
            }}
          >
            <Space style={{ marginBottom: result.correctAnswers.length ? 8 : 0 }}>
              {result.correct === 1 ? (
                <Text strong style={{ color: '#52c41a', fontSize: 15 }}>
                  <CheckCircleOutlined /> 回答正确
                </Text>
              ) : partial ? (
                <Text strong style={{ color: '#fa8c16', fontSize: 15 }}>
                  部分正确：答对 {hitCount}/{result.blankTotal} 空，本题得分 {result.earnedScore ?? 0} 分
                </Text>
              ) : result.correct === 0 ? (
                <Text strong style={{ color: '#ff4d4f', fontSize: 15 }}>
                  <CloseCircleOutlined /> 回答错误
                </Text>
              ) : (
                <Text strong style={{ color: '#faad14', fontSize: 15 }}>未判分（无标准答案）</Text>
              )}
            </Space>
            {(partial || result.correct === 0) && result.correctAnswers.length > 0 && (
              <div style={{ marginBottom: 8 }}>
                <Text type="secondary">正确答案：</Text>
                <Text strong style={{ color: '#52c41a' }}>
                  {formatCorrectAnswers(qtype, result.correctAnswers)}
                </Text>
              </div>
            )}
            {attr.examAnalysis && (
              <Paragraph style={{ margin: 0, color: '#666' }}>
                <Text strong>解析：</Text>{attr.examAnalysis}
              </Paragraph>
            )}
          </div>
        );
      })()}

      {/* ---- 操作提示 ---- */}
      {judgeMode && !result && (
        <Text type="secondary" style={{ display: 'block', marginTop: 12, textAlign: 'center' }}>
          {qtype === 'Checkbox' ? '可多选，确认后提交判题' : '作答后点击「确认提交」判题'}
        </Text>
      )}
      {judgeMode && result && (
        <Text type="secondary" style={{ display: 'block', marginTop: 12, textAlign: 'center' }}>
          本题已判题，点击「下一题」继续
        </Text>
      )}
      {!judgeMode && (
        <Text type="secondary" style={{ display: 'block', marginTop: 12 }}>
          作答后点击「下一题」继续，全部完成后交卷统一判分
        </Text>
      )}
    </div>
  );
}
