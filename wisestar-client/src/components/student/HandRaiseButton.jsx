/**
 * HandRaiseButton.jsx - 学员题目页「举手」悬浮按钮
 *
 * 功能:
 *   1. 仅在题目页（KnowledgePage 逐题答题态）渲染，固定于右下角
 *   2. 点击弹出三个选项：寻求老师讲解 / 此题题干有误 / 此题答案有误
 *   3. 可填写补充说明，提交时把题目快照（题干、题型、知识点四维、选项、当前作答）一并上传
 *      → POST /api/hand-raise/create
 *
 * 分支处理（后端）:
 *   - explain        → 学管师/教师端「举手提醒」，点开查看学员当时的多选作答
 *   - question_error / answer_error → 题目管理端（教研）待修任务
 *
 * 被谁引用: pages/student/KnowledgePage.jsx（realMode 逐题作答区）
 */

import { useState } from 'react';
import { Modal, Radio, Input, message, Button } from 'antd';
import { createHandRaise } from '../../api/handRaise';

const TYPE_LABELS = {
  Radio: '单选',
  Checkbox: '多选',
  Multiple: '多选',
  Judge: '判断',
  FillBlank: '填空',
  MultipleBlank: '多项填空',
  Text: '简答',
};

const RAISE_OPTIONS = [
  { value: 'explain', label: '寻求老师讲解', desc: '老师会看到你当前这道题和你的作答，过来给你讲。' },
  { value: 'question_error', label: '此题题干有误', desc: '题干表述或条件有问题，提交给教研核对修改。' },
  { value: 'answer_error', label: '此题答案有误', desc: '标准答案或解析有问题，提交给教研核对修改。' },
];

/** 选项字母（A/B/C…） */
const letterOf = (index) => String.fromCharCode(65 + index);

/** 举手图标（内联 SVG，避免额外图标依赖） */
function HandIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M8 13V5.5a1.5 1.5 0 0 1 3 0V12m0-6.5a1.5 1.5 0 0 1 3 0V12m0-5.5a1.5 1.5 0 0 1 3 0V13m0-4.5a1.5 1.5 0 0 1 3 0V15a6 6 0 0 1-6 6h-1.2a5 5 0 0 1-3.6-1.5L5.4 16"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

/**
 * 汇总学员当前作答的选项 ID 与展示文本。
 */
function resolveAnswer(question, answer) {
  const options = question?.schema?.children || [];
  if (!answer) {
    return { optionIds: [], text: '' };
  }
  if (answer.type === 'option') {
    const index = options.findIndex((opt) => opt.id === answer.optionId);
    return {
      optionIds: answer.optionId ? [answer.optionId] : [],
      text: index >= 0 ? `${letterOf(index)}. ${options[index].title || ''}`.trim() : '',
    };
  }
  if (answer.type === 'options') {
    const ids = answer.optionIds || [];
    const text = ids
      .map((id) => {
        const index = options.findIndex((opt) => opt.id === id);
        return index >= 0 ? `${letterOf(index)}. ${options[index].title || ''}`.trim() : '';
      })
      .filter(Boolean)
      .join('；');
    return { optionIds: ids, text };
  }
  const raw = answer.text || '';
  return { optionIds: [], text: raw.split('|').join(' | ') };
}

/**
 * 知识点名称（优先取当前练习知识点，回退题目自带知识点）
 */
function resolveKnowledgePoint(question, knowledgePointName) {
  return knowledgePointName || question?.knowledgePointName || '';
}

/**
 * 标准答案展示文本：优先用调用方传入的答案，其次从题目 schema 的整题答案推导（选项转「A. 文本」）
 */
function resolveCorrectAnswer(question, correctAnswer) {
  if (correctAnswer) return correctAnswer;
  const raw = question?.schema?.attribute?.examCorrectAnswer;
  if (!raw) return '';
  const options = question?.schema?.children || [];
  return String(raw).split('\n').map((item) => {
    const text = String(item || '').trim();
    const index = options.findIndex((opt) => String(opt.title || '').trim() === text);
    return index >= 0 ? `${letterOf(index)}. ${options[index].title}` : text;
  }).join('；');
}

export default function HandRaiseButton({
  question,
  answer,
  page,
  sectionId,
  knowledgePointName,
  correctAnswer,
  analysis,
  subject,
  chapter,
}) {
  const [open, setOpen] = useState(false);
  const [raiseType, setRaiseType] = useState('explain');
  const [remark, setRemark] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!question?.id) {
    return null;
  }

  const submit = () => {
    if (submitting) return;
    const { optionIds, text } = resolveAnswer(question, answer);
    const options = (question.schema?.children || []).map((opt) => ({ id: opt.id, title: opt.title || '' }));
    setSubmitting(true);
    createHandRaise({
      raiseType,
      questionId: question.id,
      questionType: question.questionType,
      knowledgePoint: resolveKnowledgePoint(question, knowledgePointName),
      questionContent: question.name || question.schema?.title || '',
      optionsJson: JSON.stringify(options),
      studentAnswer: text,
      studentOptionIds: optionIds.join(','),
      // 英语练习题为内容键、题库里没有同 ID 记录，答案与解析随提交一并带上
      correctAnswer: resolveCorrectAnswer(question, correctAnswer) || undefined,
      answerAnalysis: analysis || undefined,
      subject: subject || undefined,
      chapter: chapter || undefined,
      page,
      sectionId: sectionId || undefined,
      remark: remark.trim() || undefined,
    })
      .then(() => {
        message.success(
          raiseType === 'explain' ? '已举手，老师马上过来' : '已提交，教研会尽快核对这道题',
        );
        setOpen(false);
        setRemark('');
        setRaiseType('explain');
      })
      .catch(() => message.error('提交失败，请重试'))
      .finally(() => setSubmitting(false));
  };

  return (
    <>
      <button
        type="button"
        aria-label="举手"
        onClick={() => setOpen(true)}
        style={{
          position: 'fixed',
          right: 28,
          bottom: 96,
          zIndex: 1200,
          width: 64,
          height: 64,
          borderRadius: '50%',
          border: 'none',
          cursor: 'pointer',
          background: 'linear-gradient(135deg, #ff9a44 0%, #fc6076 100%)',
          color: '#fff',
          boxShadow: '0 8px 22px rgba(252, 96, 118, 0.42)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 2,
          fontSize: 12,
          fontWeight: 600,
          lineHeight: 1.1,
        }}
      >
        <HandIcon />
        <span>举手</span>
      </button>

      <Modal
        title="举手"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={submit}
        okText="提交"
        cancelText="取消"
        confirmLoading={submitting}
        width={480}
        destroyOnHidden
      >
        <div style={{ marginBottom: 10, color: '#607d8b', fontSize: 13 }}>
          {TYPE_LABELS[question.questionType] || '题目'} · {(question.name || '').slice(0, 40)}
          {(question.name || '').length > 40 ? '…' : ''}
        </div>
        <Radio.Group
          value={raiseType}
          onChange={(e) => setRaiseType(e.target.value)}
          style={{ display: 'flex', flexDirection: 'column', gap: 10, width: '100%' }}
        >
          {RAISE_OPTIONS.map((opt) => (
            <Radio key={opt.value} value={opt.value} style={{ alignItems: 'flex-start' }}>
              <span style={{ fontWeight: 500 }}>{opt.label}</span>
              <div style={{ color: '#90a4ae', fontSize: 12 }}>{opt.desc}</div>
            </Radio>
          ))}
        </Radio.Group>
        <Input.TextArea
          style={{ marginTop: 14 }}
          rows={3}
          maxLength={200}
          showCount
          value={remark}
          onChange={(e) => setRemark(e.target.value)}
          placeholder="补充说明（选填，例如：第三步没看懂 / 选项 C 明显不对）"
        />
        <div style={{ marginTop: 10, fontSize: 12, color: '#90a4ae' }}>
          提交后会带上当前题目与你的作答，方便老师直接定位问题。
        </div>
        <Button type="link" size="small" style={{ paddingLeft: 0 }} onClick={() => setOpen(false)}>
          先不举手
        </Button>
      </Modal>
    </>
  );
}
