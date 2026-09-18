/**
 * SectionManagePage.jsx - 英语小节管理页（后台管理端）
 *
 * 功能:
 *   1. 小节目录列表（按版本/年级/册别/单元/小节筛选）
 *   2. 小节 CRUD（新增/编辑/删除排序）
 *
 * URL: /english/section（受 AuthGuard 保护，权限 english:section:list）
 * 被谁引用：App.jsx 路由表
 *
 * 数据流:
 *   GET  /api/english/section/list   → 小节分页列表
 *   POST /api/english/section/save   → 新增/更新小节
 *   POST /api/english/section/delete → 删除小节
 *
 * 说明: 小节目录只维护名称与排序；单词/句子/语法上的 section 为自由文本，
 *       下拉候选由「小节目录 ∪ 内容已出现的 section」汇总。
 */

import { useEffect, useState } from 'react';
import { Table, Space, Button, Input, Select, Modal, Form, message, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { getSections, saveSection, deleteSection } from '../../api/englishAdmin';

const { Title } = Typography;

const VERSION_OPTIONS = ['人教版', '苏教版', '北师大版', '外研版'].map((v) => ({ value: v, label: v }));
const GRADE_OPTIONS = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级'].map((g) => ({ value: g, label: g }));
const TERM_OPTIONS = ['上册', '下册'].map((t) => ({ value: t, label: t }));

export default function SectionManagePage() {
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

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();

  const loadList = () => {
    setLoading(true);
    getSections({
      current,
      pageSize,
      ...(version && { version }),
      ...(grade && { grade }),
      ...(term && { term }),
      ...(unit && { unit }),
      ...(section && { section }),
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
        sort: 0,
      });
    }
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      const payload = { ...values, id: editing?.id };
      saveSection(payload)
        .then(() => {
          message.success(editing?.id ? '编辑成功' : '保存成功');
          setModalOpen(false);
          loadList();
        })
        .catch(() => {});
    });
  };

  const handleDelete = (id) => {
    deleteSection(id)
      .then(() => {
        message.success('删除成功');
        loadList();
      })
      .catch(() => {});
  };

  const columns = [
    { title: '小节', dataIndex: 'section', ellipsis: true },
    { title: '单元', dataIndex: 'unit', width: 200, ellipsis: true },
    { title: '版本', dataIndex: 'version', width: 90 },
    { title: '年级', dataIndex: 'grade', width: 90 },
    { title: '册别', dataIndex: 'term', width: 70 },
    { title: '排序', dataIndex: 'sort', width: 70 },
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
      <Title level={4}>小节管理</Title>

      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增小节</Button>
      </Space>

      <Space wrap style={{ marginBottom: 16 }}>
        <Select placeholder="教材版本" allowClear style={{ width: 120 }} value={version || undefined} onChange={setVersion} options={VERSION_OPTIONS} />
        <Select placeholder="年级" allowClear style={{ width: 100 }} value={grade || undefined} onChange={setGrade} options={GRADE_OPTIONS} />
        <Select placeholder="册别" allowClear style={{ width: 90 }} value={term || undefined} onChange={setTerm} options={TERM_OPTIONS} />
        <Input placeholder="单元" allowClear style={{ width: 160 }} value={unit} onChange={(e) => setUnit(e.target.value)} onPressEnter={loadList} />
        <Input placeholder="小节" allowClear style={{ width: 140 }} value={section} onChange={(e) => setSection(e.target.value)} onPressEnter={loadList} />
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
        title={editing?.id ? '编辑小节' : '新增小节'}
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
          <Form.Item name="section" label="小节名称" rules={[{ required: true, message: '请输入小节名称' }]}>
            <Input placeholder="如 Part A Let's talk" />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <Input type="number" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
