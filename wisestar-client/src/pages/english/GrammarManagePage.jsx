/**
 * GrammarManagePage.jsx - 英语语法管理页（后台管理端）
 *
 * 功能:
 *   1. 语法列表（按版本/年级/册别/单元/小节/标题筛选）
 *   2. 语法 CRUD（标题/讲解/例句/练习/排序）
 *
 * URL: /english/grammar（受 AuthGuard 保护，权限 english:grammar:list）
 * 被谁引用：App.jsx 路由表
 *
 * 数据流:
 *   GET  /api/english/grammar/list   → 语法分页列表
 *   POST /api/english/grammar/save   → 新增/更新语法
 *   POST /api/english/grammar/delete → 删除语法
 *
 * 说明: examples/exercises 为 JSON 字符串（由 AI 内容生成写入，也可手工维护）。
 */

import { useEffect, useState } from 'react';
import { Table, Space, Button, Input, Select, Modal, Form, message, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { getGrammars, saveGrammar, deleteGrammar } from '../../api/englishAdmin';

const { Title } = Typography;

const VERSION_OPTIONS = ['人教版', '苏教版', '北师大版', '外研版'].map((v) => ({ value: v, label: v }));
const GRADE_OPTIONS = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级'].map((g) => ({ value: g, label: g }));
const TERM_OPTIONS = ['上册', '下册'].map((t) => ({ value: t, label: t }));

export default function GrammarManagePage() {
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const [version, setVersion] = useState('');
  const [grade, setGrade] = useState('');
  const [term, setTerm] = useState('');
  const [unit, setUnit] = useState('');
  const [section, setSection] = useState('');
  const [title, setTitle] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();

  const loadList = () => {
    setLoading(true);
    getGrammars({
      current,
      pageSize,
      ...(version && { version }),
      ...(grade && { grade }),
      ...(term && { term }),
      ...(unit && { unit }),
      ...(section && { section }),
      ...(title && { title }),
    })
      .then((res) => {
        setList(res.data?.list || []);
        setTotal(res.data?.total || 0);
      })
      .catch(() => message.error('加载失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadList();
  }, [current, pageSize]);

  const handleReset = () => {
    setVersion('');
    setGrade('');
    setTerm('');
    setUnit('');
    setSection('');
    setTitle('');
    setCurrent(1);
  };

  const openModal = (record = null) => {
    setEditing(record);
    setModalOpen(true);
    if (record) {
      form.setFieldsValue(record);
    } else {
      form.resetFields();
      form.setFieldsValue({
        version: version || '人教版',
        grade: grade || '三年级',
        term: term || '上册',
        unit: unit || '',
        section: section || '',
        sort: 0,
      });
    }
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      const payload = { ...values, id: editing?.id };
      saveGrammar(payload)
        .then(() => {
          message.success(editing?.id ? '编辑成功' : '保存成功');
          setModalOpen(false);
          loadList();
        })
        .catch(() => {});
    });
  };

  const handleDelete = (id) => {
    deleteGrammar(id)
      .then(() => {
        message.success('删除成功');
        loadList();
      })
      .catch(() => {});
  };

  const columns = [
    { title: '标题', dataIndex: 'title', width: 200, ellipsis: true },
    { title: '版本', dataIndex: 'version', width: 80 },
    { title: '年级', dataIndex: 'grade', width: 80 },
    { title: '册别', dataIndex: 'term', width: 70 },
    { title: '单元', dataIndex: 'unit', width: 180, ellipsis: true },
    { title: '小节', dataIndex: 'section', width: 140, ellipsis: true },
    { title: '排序', dataIndex: 'sort', width: 70 },
    {
      title: '更新时间',
      dataIndex: 'updateAt',
      width: 170,
      render: (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '—'),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(record)}>编辑</Button>
          <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ maxWidth: 1400, margin: '0 auto', padding: 20 }}>
      <Title level={4}>语法管理</Title>

      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增语法</Button>
      </Space>

      <Space wrap style={{ marginBottom: 16 }}>
        <Select placeholder="教材版本" allowClear style={{ width: 120 }} value={version || undefined} onChange={setVersion} options={VERSION_OPTIONS} />
        <Select placeholder="年级" allowClear style={{ width: 100 }} value={grade || undefined} onChange={setGrade} options={GRADE_OPTIONS} />
        <Select placeholder="册别" allowClear style={{ width: 90 }} value={term || undefined} onChange={setTerm} options={TERM_OPTIONS} />
        <Input placeholder="单元" allowClear style={{ width: 150 }} value={unit} onChange={(e) => setUnit(e.target.value)} onPressEnter={loadList} />
        <Input placeholder="小节" allowClear style={{ width: 120 }} value={section} onChange={(e) => setSection(e.target.value)} onPressEnter={loadList} />
        <Input placeholder="标题关键字" allowClear style={{ width: 150 }} value={title} onChange={(e) => setTitle(e.target.value)} onPressEnter={loadList} />
        <Button type="primary" onClick={loadList}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
      </Space>

      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={{
          current,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (c, s) => {
            setCurrent(c);
            setPageSize(s);
          },
        }}
      />

      <Modal
        title={editing?.id ? '编辑语法' : '新增语法'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        width={720}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入语法标题' }]}>
            <Input placeholder="如 be 动词的用法" />
          </Form.Item>
          <Form.Item name="content" label="讲解内容">
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item name="examples" label="例句（JSON 数组）">
            <Input.TextArea rows={3} placeholder='["This is a book.", "These are books."]' />
          </Form.Item>
          <Form.Item name="exercises" label="练习（JSON 数组）">
            <Input.TextArea rows={3} />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item name="version" label="版本" rules={[{ required: true, message: '请选择版本' }]}>
              <Select options={VERSION_OPTIONS} />
            </Form.Item>
            <Form.Item name="grade" label="年级" rules={[{ required: true, message: '请选择年级' }]}>
              <Select options={GRADE_OPTIONS} />
            </Form.Item>
            <Form.Item name="term" label="册别">
              <Select allowClear options={TERM_OPTIONS} />
            </Form.Item>
            <Form.Item name="sort" label="排序">
              <Input type="number" />
            </Form.Item>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item name="unit" label="单元">
              <Input placeholder="如 Unit 1 Helping at home" />
            </Form.Item>
            <Form.Item name="section" label="小节">
              <Input placeholder="如 Part A Let's talk" />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  );
}
