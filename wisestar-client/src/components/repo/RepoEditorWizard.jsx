/**
 * RepoEditorWizard.jsx - 题库编辑向导（分步：基本信息 → 选题编辑）
 *
 * 步骤:
 *   1. 基本信息：题库名称/描述/学科/年级/难度
 *   2. 选题编辑：题目卡片列表（标签/题目/类型，勾选选择），支持搜索/筛选
 *
 * 被谁引用: RepoDetailPage（编辑/新增题库）
 * 数据流: onCreate({name, description, ...}) / onSelectTemplates(questionIds)
 */

import { useState, useEffect } from 'react';
import { Modal, Steps, Form, Input, Select, Button, Card, Checkbox, Space, message } from 'antd';
import { listTemplate } from '../../api/template';
import { listSubjects } from '../../api/knowledge';

const { TextArea } = Input;

/** 题目卡片（可勾选） */
function QuestionCard({ q, checked, onChange }) {
  const typeLabel = { Radio: '单选', Checkbox: '多选', FillBlank: '填空', Judge: '判断', Text: '简答' }[q.questionType] || q.questionType;
  return (
    <Card
      size="small"
      hoverable
      style={{ borderColor: checked ? '#1890ff' : '#f0f0f0', backgroundColor: checked ? '#e6f7ff' : '#fff' }}
      onClick={() => onChange(q.id, !checked)}
      extra={<Checkbox checked={checked} onClick={(e) => e.stopPropagation()} onChange={(e) => onChange(q.id, e.target.checked)} />}
    >
      <Card.Meta
        title={
          <Space size={6}>
            <span style={{ fontSize: 12, color: '#666' }}>{typeLabel}</span>
            {q.tag && q.tag.map((t, i) => (
              <span key={i} style={{ fontSize: 11, color: '#fff', background: '#1890ff', borderRadius: 6, padding: '1px 6px' }}>{t}</span>
            ))}
          </Space>
        }
        description={<div style={{ fontSize: 13, color: '#333', maxHeight: 60, overflow: 'hidden', textOverflow: 'ellipsis' }}>{q.name || q.template?.title || '无标题'}</div>}
      />
    </Card>
  );
}

export default function RepoEditorWizard({ open, onCancel, repoId, _onSave, onTemplatesSelect }) {
  const [step, setStep] = useState(0);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [questions, setQuestions] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [repoInfo, setRepoInfo] = useState(null);
  const [subjects, setSubjects] = useState([]);

  // 加载学科
  useEffect(() => {
    listSubjects().then((res) => setSubjects(res?.data || [])).catch(() => setSubjects([]));
  }, []);

  // 加载已有题库信息（编辑模式）
  useEffect(() => {
    if (open && repoId) {
      setLoading(true);
      // TODO: 通过 API 加载题库详情（name/description/subjectId/grade/difficulty/templateIds）
      // 当前由父组件传入或后续扩展
      setLoading(false);
    }
  }, [open, repoId]);

  // 加载题目列表（选题步骤）
  useEffect(() => {
    if (open && step === 1) {
      setLoading(true);
      listTemplate({ current: 1, pageSize: 200 })
        .then((res) => {
          setQuestions(res?.data?.list || []);
          // 编辑模式：已选题目勾选
          if (repoInfo?.templateIds) {
            setSelectedIds(repoInfo.templateIds);
          }
        })
        .catch(() => setQuestions([]))
        .finally(() => setLoading(false));
    }
  }, [open, step, repoInfo]);

  // 上一步/下一步
  const handlePrev = () => setStep((s) => s - 1);
  const handleNext = () => {
    if (step === 0) {
      form.validateFields().then((values) => {
        setRepoInfo(values);
        setStep(1);
      });
    } else {
      // 完成选题
      if (selectedIds.length === 0) {
        message.warning('请至少选择一道题目');
        return;
      }
      onTemplatesSelect(selectedIds);
    }
  };

  // 题目勾选切换
  const toggleQuestion = (qid, checked) => {
    setSelectedIds((prev) => checked ? [...prev, qid] : prev.filter((id) => id !== qid));
  };

  return (
    <Modal
      title="题库编辑向导"
      open={open}
      onCancel={onCancel}
      footer={
        <Space>
          {step > 0 && <Button onClick={handlePrev}>上一步</Button>}
          <Button type="primary" onClick={handleNext} loading={loading}>{step === 0 ? '下一步：选题' : '完成'}</Button>
        </Space>
      }
      width={800}
      destroyOnClose
    >
      <Steps current={step} items={[
        { title: '基本信息', description: '填写题库名称/描述/学科/年级/难度' },
        { title: '选题编辑', description: '从题库中选择题目（卡片形式）' },
      ]} style={{ marginBottom: 20 }} />

      {step === 0 && (
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="题库名称" rules={[{ required: true, message: '请输入题库名称' }]}>
            <Input placeholder="如：一年级上册数学期末复习" />
          </Form.Item>
          <Form.Item name="description" label="题库描述">
            <TextArea rows={3} placeholder="选填，描述本题库的用途或范围" />
          </Form.Item>
          <Form.Item name="subjectId" label="所属学科" rules={[{ required: true, message: '请选择学科' }]}>
            <Select placeholder="选择学科" options={[subjects.map((s) => ({ value: s.id, label: s.name }))]} />
          </Form.Item>
          <Form.Item name="grade" label="年级" rules={[{ required: true, message: '请选择年级' }]}>
            <Select placeholder="选择年级" options={[
              { value: '一年级', label: '一年级' }, { value: '二年级', label: '二年级' },
              { value: '三年级', label: '三年级' }, { value: '四年级', label: '四年级' },
              { value: '五年级', label: '五年级' }, { value: '六年级', label: '六年级' },
            ]} />
          </Form.Item>
          <Form.Item name="difficulty" label="难度" initialValue="中等">
            <Select options={[{ value: '简单', label: '简单' }, { value: '中等', label: '中等' }, { value: '困难', label: '困难' }]} />
          </Form.Item>
        </Form>
      )}

      {step === 1 && (
        <div>
          <div style={{ marginBottom: 12, color: '#666', fontSize: 13 }}>
            已选择 <b style={{ color: '#1890ff' }}>{selectedIds.length}</b> 道题目，请点击题目卡片或勾选复选框进行选择
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 12, maxHeight: 480, overflowY: 'auto' }}>
            {questions.map((q) => (
              <QuestionCard key={q.id} q={q} checked={selectedIds.includes(q.id)} onChange={toggleQuestion} />
            ))}
          </div>
        </div>
      )}
    </Modal>
  );
}
