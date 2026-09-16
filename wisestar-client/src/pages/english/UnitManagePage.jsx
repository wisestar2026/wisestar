/**
 * UnitManagePage.jsx - 英语单元管理页（后台管理端）
 *
 * 功能:
 *   1. 单元列表（按版本/年级/册别/单元筛选，带单词数/句子数）
 *   2. 单元 CRUD（新增/编辑/删除排序）
 *
 * URL: /english/unit（受 AuthGuard 保护，权限 english:unit:list）
 * 被谁引用：App.jsx 路由表
 *
 * 数据流:
 *   GET  /api/english/unit/list   → 单元分页列表
 *   POST /api/english/unit/save   → 新增/更新单元
 *   POST /api/english/unit/delete → 删除单元
 *
 * 说明: 单元列表由「词库单元 ∪ 句库单元 ∪ 单元目录表」派生；
 *       词库/句库自带的单元无 id，保存后生成目录记录用于排序。
 */

import { useEffect, useState } from 'react';
import { Table, Space, Button, Input, Select, Modal, Form, message, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';

const API_BASE = '/api/english/unit';
const { Title } = Typography;

const VERSION_OPTIONS = ['人教版', '苏教版', '北师大版', '外研版'].map((v) => ({ value: v, label: v }));
const GRADE_OPTIONS = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级'].map((g) => ({ value: g, label: g }));
const TERM_OPTIONS = ['上册', '下册'].map((t) => ({ value: t, label: t }));

export default function UnitManagePage() {
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const [version, setVersion] = useState('');
  const [grade, setGrade] = useState('');
  const [term, setTerm] = useState('');
  const [unit, setUnit] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();

  const loadList = () => {
    setLoading(true);
    const params = new URLSearchParams({
      current,
      pageSize,
      ...(version && { version }),
      ...(grade && { grade }),
      ...(term && { term }),
      ...(unit && { unit }),
    });
    fetch(`${API_BASE}/list?${params}`)
      .then((res) => res.json())
      .then((res) => {
        if (res.code === 200) {
          setList(res.data?.list || []);
          setTotal(res.data?.total || 0);
        }
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
    setCurrent(1);
  };

  const openModal = (record = null) => {
    setEditing(record);
    setModalOpen(true);
    if (record) {
      form.setFieldsValue(record);
    } else {
      form.resetFields();
      form.setFieldsValue({ version: version || '人教版', grade: grade || '三年级', term: term || '上册', sort: 0 });
    }
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      const payload = { ...values, id: editing?.id };
      fetch(`${API_BASE}/save`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
        .then((res) => res.json())
        .then((res) => {
          if (res.code === 200) {
            message.success(editing?.id ? '编辑成功' : '保存成功');
            setModalOpen(false);
            loadList();
          } else {
            message.error(res.message || '保存失败');
          }
        })
        .catch(() => message.error('保存失败'));
    });
  };

  const handleDelete = (id) => {
    fetch(`${API_BASE}/delete?id=${id}`, { method: 'POST' })
      .then((res) => res.json())
      .then((res) => {
        if (res.code === 200) {
          message.success('删除成功');
          loadList();
        } else {
          message.error(res.message || '删除失败');
        }
      })
      .catch(() => message.error('删除失败'));
  };

  const columns = [
    { title: '单元', dataIndex: 'unit', ellipsis: true },
    { title: '版本', dataIndex: 'version', width: 100 },
    { title: '年级', dataIndex: 'grade', width: 100 },
    { title: '册别', dataIndex: 'term', width: 80 },
    { title: '单词数', dataIndex: 'wordCount', width: 90 },
    { title: '句子数', dataIndex: 'sentenceCount', width: 90 },
    { title: '排序', dataIndex: 'sort', width: 80 },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(record)}>编辑</Button>
          {record.id && (
            <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>删除</Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ maxWidth: 1400, margin: '0 auto', padding: 20 }}>
      <Title level={4}>单元管理</Title>

      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增单元</Button>
      </Space>

      <Space wrap style={{ marginBottom: 16 }}>
        <Select placeholder="教材版本" allowClear style={{ width: 120 }} value={version || undefined} onChange={setVersion} options={VERSION_OPTIONS} />
        <Select placeholder="年级" allowClear style={{ width: 100 }} value={grade || undefined} onChange={setGrade} options={GRADE_OPTIONS} />
        <Select placeholder="册别" allowClear style={{ width: 90 }} value={term || undefined} onChange={setTerm} options={TERM_OPTIONS} />
        <Input placeholder="单元" allowClear style={{ width: 160 }} value={unit} onChange={(e) => setUnit(e.target.value)} onPressEnter={loadList} />
        <Button type="primary" onClick={loadList}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
      </Space>

      <Table
        rowKey={(record) => record.id || `${record.version}-${record.grade}-${record.term}-${record.unit}`}
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
        title={editing?.id ? '编辑单元' : '新增单元'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="version" label="教材版本" rules={[{ required: true, message: '请选择教材版本' }]}>
            <Select options={VERSION_OPTIONS} />
          </Form.Item>
          <Form.Item name="grade" label="年级" rules={[{ required: true, message: '请选择年级' }]}>
            <Select options={GRADE_OPTIONS} />
          </Form.Item>
          <Form.Item name="term" label="册别" rules={[{ required: true, message: '请选择册别' }]}>
            <Select options={TERM_OPTIONS} />
          </Form.Item>
          <Form.Item name="unit" label="单元" rules={[{ required: true, message: '请输入单元名' }]}>
            <Input placeholder="如 Unit 1 Helping at home" />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <Input type="number" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
