/**
 * KnowledgePointManagePage.jsx - 知识点管理页（知识管理板块）
 *
 * 功能:
 *   1. 面包屑定位: 知识管理 → 学科 → 章节 → 小节（URL query 携带三级 id）
 *   2. 知识点 CRUD（mock 数据，存于 useKnowledgeStore）
 *   3. 「内容设置」: 编辑知识点讲解要点（学生端知识点详情逐条展示）
 *   4. 「测试题型」: 管理知识点随堂测验题目（题型/题干/选项/答案/分值）
 *      - Radio 单选 / Checkbox 多选 / Judge 判断 / FillBlank 填空
 *      - 题目编辑使用本地 state（QuestionRow 组件内部联动答案与选项），保存时整体写回 store
 *
 * URL: /knowledge/points（受 AuthGuard 保护）
 * 被谁引用: App.jsx 路由表；小节管理页「管理知识点」按钮跳转进入
 */

import { useState } from 'react';
import {
  Card, Table, Space, Button, Input, InputNumber, Select, Modal, Form, Tag, Typography, Breadcrumb, Popconfirm, message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, FileTextOutlined, QuestionCircleOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import useKnowledgeStore, { QUESTION_TYPES } from '../../stores/useKnowledgeStore';

const { Text } = Typography;

// ==================== 单题编辑行（内部自管选项/答案联动） ====================
function QuestionRow({ question, onChange, onRemove }) {
  const [q, setQ] = useState(question);

  const patch = (p) => {
    const next = { ...q, ...p };
    setQ(next);
    onChange(next);
  };
  const patchOption = (idx, value) => {
    const options = [...(q.options || [])];
    options[idx] = value;
    // 答案引用选项文本：改选项后同步修正答案
    let answer = q.answer || [];
    if (q.type === 'FillBlank') answer = q.answer || [];
    else {
      answer = (q.answer || []).map((a) => {
        const i = (q.options || []).indexOf(a);
        return i >= 0 ? (options[i] ?? a) : a;
      });
    }
    patch({ options, answer });
  };

  // 题型切换：重置选项与答案
  const changeType = (type) => {
    const base = { type };
    if (type === 'Judge') {
      base.options = ['正确', '错误'];
      base.answer = ['正确'];
    } else if (type === 'FillBlank') {
      base.options = [];
      base.answer = [''];
    } else {
      base.options = ['选项A', '选项B'];
      base.answer = ['选项A'];
    }
    patch(base);
  };

  const isChoice = q.type === 'Radio' || q.type === 'Checkbox';
  const isJudge = q.type === 'Judge';

  return (
    <Card
      size="small"
      style={{ marginBottom: 12 }}
      title={<Text strong>题目 {q.order}</Text>}
      extra={<Button danger size="small" icon={<DeleteOutlined />} onClick={onRemove}>删除此题</Button>}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Space style={{ width: '100%' }} align="start">
          <Select
            style={{ width: 130 }}
            value={q.type}
            onChange={changeType}
            options={QUESTION_TYPES.map((t) => ({ value: t.value, label: t.label }))}
          />
          <Input.TextArea
            style={{ flex: 1 }}
            rows={2}
            placeholder="题干内容"
            value={q.q}
            onChange={(e) => patch({ q: e.target.value })}
          />
        </Space>

        {/* 选项编辑（单选/多选/判断） */}
        {isChoice && (
          <div>
            <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>选项（判断题为固定「正确/错误」）</Text>
            {(q.options || []).map((opt, idx) => (
              <Space key={idx} style={{ display: 'flex', marginBottom: 6 }} align="baseline">
                <Text type="secondary">{String.fromCharCode(65 + idx)}.</Text>
                <Input
                  style={{ width: 240 }}
                  value={opt}
                  disabled={isJudge}
                  onChange={(e) => patchOption(idx, e.target.value)}
                />
                {!isJudge && (
                  <Button
                    danger size="small"
                    onClick={() => {
                      const options = q.options.filter((_, i) => i !== idx);
                      patch({ options, answer: (q.answer || []).filter((a) => options.includes(a)) });
                    }}
                  >
                    删除
                  </Button>
                )}
              </Space>
            ))}
            {!isJudge && (
              <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={() => patch({ options: [...(q.options || []), `选项${String.fromCharCode(65 + q.options.length)}`] })}>
                添加选项
              </Button>
            )}
          </div>
        )}

        {/* 答案编辑 */}
        <Space align="center">
          <Text>标准答案</Text>
          {q.type === 'Checkbox'
            ? (
              <Select
                mode="multiple" style={{ width: 240 }} placeholder="选择答案"
                value={q.answer || []}
                options={(q.options || []).map((o) => ({ value: o, label: o }))}
                onChange={(ans) => patch({ answer: ans })}
              />
            )
            : q.type === 'FillBlank'
              ? (
                <Input
                  style={{ width: 240 }} placeholder="填空标准答案"
                  value={q.answer?.[0] || ''}
                  onChange={(e) => patch({ answer: [e.target.value] })}
                />
              )
              : (
                <Select
                  style={{ width: 240 }} placeholder="选择答案"
                  value={q.answer?.[0]}
                  options={(q.options || []).map((o) => ({ value: o, label: o }))}
                  onChange={(ans) => patch({ answer: [ans] })}
                />
              )}
          <Text>分值</Text>
          <InputNumber min={1} max={20} value={q.score ?? 5} onChange={(v) => patch({ score: v })} />
          <Text type="secondary">分</Text>
        </Space>
      </Space>
    </Card>
  );
}

export default function KnowledgePointManagePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const subjectId = searchParams.get('subjectId');
  const chapterId = searchParams.get('chapterId');
  const sectionId = searchParams.get('sectionId');

  const subjects = useKnowledgeStore((s) => s.subjects);
  const addKp = useKnowledgeStore((s) => s.addKp);
  const updateKp = useKnowledgeStore((s) => s.updateKp);
  const updateKpContent = useKnowledgeStore((s) => s.updateKpContent);
  const updateKpQuestions = useKnowledgeStore((s) => s.updateKpQuestions);
  const deleteKp = useKnowledgeStore((s) => s.deleteKp);

  const subject = subjects.find((s) => s.id === subjectId);
  const chapter = subject?.chapters.find((c) => c.id === chapterId);
  const section = chapter?.sections.find((s) => s.id === sectionId);
  const kps = section?.kps || [];

  // ---- 新增/编辑弹窗 ----
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();

  // ---- 内容设置弹窗 ----
  const [contentOpen, setContentOpen] = useState(false);
  const [contentKp, setContentKp] = useState(null);
  const [contentForm] = Form.useForm();

  // ---- 测试题型弹窗（本地 state 编辑，保存整体写回） ----
  const [quizOpen, setQuizOpen] = useState(false);
  const [quizKp, setQuizKp] = useState(null);
  const [localQuestions, setLocalQuestions] = useState([]);

  const openModal = (kp = null) => {
    setEditing(kp);
    setModalOpen(true);
    if (kp) {
      form.setFieldsValue({ name: kp.name, sort: kp.sort });
    } else {
      form.resetFields();
      form.setFieldsValue({ sort: kps.length + 1 });
    }
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      if (editing) {
        updateKp(subjectId, chapterId, sectionId, editing.id, values);
        message.success('知识点已更新');
      } else {
        addKp(subjectId, chapterId, sectionId, values);
        message.success('知识点已新增');
      }
      setModalOpen(false);
    });
  };

  // ---- 内容设置 ----
  const openContent = (kp) => {
    setContentKp(kp);
    setContentOpen(true);
    contentForm.setFieldsValue({ points: kp.content?.points || [''] });
  };
  const saveContent = () => {
    contentForm.validateFields().then((values) => {
      updateKpContent(subjectId, chapterId, sectionId, contentKp.id, {
        points: (values.points || []).filter((p) => p && p.trim()),
      });
      message.success('内容设置已保存');
      setContentOpen(false);
    });
  };

  // ---- 测试题型 ----
  const openQuiz = (kp) => {
    setQuizKp(kp);
    setLocalQuestions((kp.questions || []).map((qq, i) => ({ ...qq, order: i + 1 })));
    setQuizOpen(true);
  };
  const saveQuiz = () => {
    const cleaned = localQuestions
      .filter((qq) => qq.q && qq.q.trim())
      .map(({ order: _order, ...rest }) => rest);
    if (cleaned.length === 0) {
      message.warning('请至少保留一题');
      return;
    }
    updateKpQuestions(subjectId, chapterId, sectionId, quizKp.id, cleaned);
    message.success('测试题型已保存');
    setQuizOpen(false);
  };

  // ---- 表格列 ----
  const columns = [
    {
      title: '知识点名称', dataIndex: 'name', width: 200,
      render: (n) => <Text strong>{n}</Text>,
    },
    {
      title: '排序', dataIndex: 'sort', width: 70, align: 'center',
    },
    {
      title: '内容设置', width: 110, align: 'center',
      render: (_, k) => (k.content?.points?.length
        ? <Tag color="green">{k.content.points.length} 条要点</Tag>
        : <Tag>未设置</Tag>),
    },
    {
      title: '测试题数', width: 110, align: 'center',
      render: (_, k) => (k.questions?.length
        ? <Tag color="blue">{k.questions.length} 题</Tag>
        : <Tag>未设置</Tag>),
    },
    {
      title: '操作', key: 'action', width: 360,
      render: (_, k) => (
        <Space wrap>
          <Button type="primary" size="small" icon={<FileTextOutlined />} onClick={() => openContent(k)}>内容设置</Button>
          <Button size="small" icon={<QuestionCircleOutlined />} onClick={() => openQuiz(k)}>测试题型</Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => openModal(k)}>编辑</Button>
          <Popconfirm
            title={`删除知识点「${k.name}」？`}
            description="其下测试题目将一并删除，删除后不可恢复。"
            onConfirm={() => { deleteKp(subjectId, chapterId, sectionId, k.id); message.success('知识点已删除'); }}
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* ---- 页面标题 ---- */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Space align="center">
          <Button size="small" icon={<ArrowLeftOutlined />} onClick={() => navigate(`/knowledge/sections?subjectId=${subjectId}&chapterId=${chapterId}`)} />
          <Breadcrumb
            items={[
              { title: <Text strong>知识管理</Text> },
              { title: <Text strong>{subject ? `${subject.icon} ${subject.name}` : '学科'}</Text> },
              { title: <Text strong>{chapter?.name || '章节'}</Text> },
              { title: <Text strong>{section?.name || '小节'}</Text> },
            ]}
          />
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增知识点</Button>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={kps}
        pagination={false}
        locale={{ emptyText: '该小节下暂无知识点，点击右上角「新增知识点」创建' }}
      />

      {/* 新增/编辑知识点弹窗 */}
      <Modal title={editing ? '编辑知识点' : '新增知识点'} open={modalOpen} onOk={handleSave} onCancel={() => setModalOpen(false)} okText="保存" cancelText="取消" destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="知识点名称" rules={[{ required: true, message: '请输入知识点名称' }]}>
            <Input placeholder="如：进位加法" maxLength={30} />
          </Form.Item>
          <Form.Item name="sort" label="排序（数字越小越靠前）">
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 知识点内容设置弹窗 */}
      <Modal
        title={`内容设置 - ${contentKp?.name || ''}`}
        open={contentOpen}
        onOk={saveContent}
        onCancel={() => setContentOpen(false)}
        okText="保存"
        cancelText="取消"
        width={680}
        destroyOnClose
      >
        <Form form={contentForm} layout="vertical">
          <Form.Item label="讲解要点（学生端知识点详情逐条展示）">
            <Form.List name="points">
              {(fields, { add, remove }) => (
                <>
                  {fields.map(({ key, name }) => (
                    <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                      <Form.Item name={name} rules={[{ required: true, message: '请输入要点内容' }]} style={{ marginBottom: 0, flex: 1 }}>
                        <Input placeholder="输入一条讲解要点" maxLength={100} />
                      </Form.Item>
                      <Button danger size="small" onClick={() => remove(name)}>删除</Button>
                    </Space>
                  ))}
                  <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add('')}>添加要点</Button>
                </>
              )}
            </Form.List>
          </Form.Item>
        </Form>
      </Modal>

      {/* 测试题型设置弹窗 */}
      <Modal
        title={`测试题型 - ${quizKp?.name || ''}`}
        open={quizOpen}
        onOk={saveQuiz}
        onCancel={() => setQuizOpen(false)}
        okText="保存全部题目"
        cancelText="取消"
        width={760}
        destroyOnClose
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          {localQuestions.map((qq, idx) => (
            <QuestionRow
              key={qq.id || idx}
              question={qq}
              onChange={(next) => {
                setLocalQuestions((prev) => prev.map((p, i) => (i === idx ? next : p)));
              }}
              onRemove={() => setLocalQuestions((prev) => prev.filter((_, i) => i !== idx).map((p, i) => ({ ...p, order: i + 1 })))}
            />
          ))}
          <Button
            type="dashed" block icon={<PlusOutlined />}
            onClick={() => setLocalQuestions((prev) => [
              ...prev,
              { id: `q${Date.now().toString(36)}`, type: 'Radio', q: '', options: ['选项A', '选项B'], answer: ['选项A'], score: 5, order: prev.length + 1 },
            ])}
          >
          添加题目
          </Button>
        </Space>
      </Modal>
    </div>
  );
}
