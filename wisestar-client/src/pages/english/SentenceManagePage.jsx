/**
 * SentenceManagePage.jsx - 英语句库管理页（后台管理端）
 *
 * 功能:
 *   1. 句子列表（按版本/年级/册别/单元/关键字筛选）
 *   2. 句子 CRUD（英文/中文/音频/排序）
 *   3. Excel 批量导入 + 模板下载
 *   4. 音频试听（无音频时回退浏览器语音合成）
 *
 * URL: /english/sentence（受 AuthGuard 保护，权限 english:sentence:list）
 * 被谁引用：App.jsx 路由表
 *
 * 数据流:
 *   GET  /api/english/sentence/list   → 句子分页列表
 *   POST /api/english/sentence/save   → 新增/更新句子
 *   POST /api/english/sentence/delete → 删除句子
 *   POST /api/english/sentence/import → Excel 导入
 */

import { useEffect, useState } from 'react';
import { Table, Space, Button, Input, Select, Modal, Form, message, Upload, Typography } from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, ImportOutlined, DownloadOutlined, SoundOutlined,
} from '@ant-design/icons';
import { speakEnglish } from '../../utils/english';

const API_BASE = '/api/english/sentence';
const { Title } = Typography;

const VERSION_OPTIONS = ['人教版', '苏教版', '北师大版', '外研版'].map((v) => ({ value: v, label: v }));
const GRADE_OPTIONS = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级'].map((g) => ({ value: g, label: g }));
const TERM_OPTIONS = ['上册', '下册'].map((t) => ({ value: t, label: t }));

export default function SentenceManagePage() {
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const [version, setVersion] = useState('');
  const [grade, setGrade] = useState('');
  const [term, setTerm] = useState('');
  const [unit, setUnit] = useState('');
  const [keyword, setKeyword] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();

  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);

  const loadList = () => {
    setLoading(true);
    const params = new URLSearchParams({
      current,
      pageSize,
      ...(version && { version }),
      ...(grade && { grade }),
      ...(term && { term }),
      ...(unit && { unit }),
      ...(keyword && { keyword }),
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
    setKeyword('');
    setCurrent(1);
  };

  const openModal = (record = null) => {
    setEditing(record);
    setModalOpen(true);
    if (record) {
      form.setFieldsValue(record);
    } else {
      form.resetFields();
      form.setFieldsValue({ version: version || '人教版', grade: grade || '三年级', term: term || '上册', unit: unit || '', sort: 0 });
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
            message.success(editing?.id ? '编辑成功' : '新增成功');
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

  const handleImport = (file) => {
    setImporting(true);
    setImportResult(null);
    const formData = new FormData();
    formData.append('file', file);
    fetch(`${API_BASE}/import`, { method: 'POST', body: formData })
      .then((res) => res.json())
      .then((res) => {
        if (res.code === 200) {
          setImportResult(res.data);
          message.success(`导入完成：成功 ${res.data.success} 条，失败 ${res.data.failed} 条`);
          loadList();
        } else {
          message.error(res.message || '导入失败');
        }
      })
      .catch(() => message.error('导入失败'))
      .finally(() => {
        setImporting(false);
        setTimeout(() => setImportResult(null), 5000);
      });
    return false;
  };

  const downloadTemplate = () => {
    const template = [
      ['版本', '年级', '册别', '单元', '英文', '中文', '音频'],
      ['人教版', '四年级', '上册', 'Unit 1 Helping at home', 'What would you like to eat?', '你想吃什么？', ''],
    ];
    const csv = template.map((row) => row.join(',')).join('\n');
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = '句子导入模板.csv';
    link.click();
  };

  const columns = [
    { title: '英文', dataIndex: 'en', ellipsis: true },
    { title: '中文', dataIndex: 'zh', width: 220, ellipsis: true },
    { title: '单元', dataIndex: 'unit', width: 160, ellipsis: true },
    { title: '册别', dataIndex: 'term', width: 70 },
    { title: '排序', dataIndex: 'sort', width: 70 },
    {
      title: '音频',
      key: 'audio',
      width: 90,
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          icon={<SoundOutlined />}
          onClick={() => speakEnglish(record.en, record.audioUrl)}
        >
          试听
        </Button>
      ),
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
      <Title level={4}>句库管理</Title>

      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增句子</Button>
        <Upload showUploadList={false} accept=".xlsx,.xls,.csv" beforeUpload={handleImport} disabled={importing}>
          <Button icon={<ImportOutlined />} loading={importing}>Excel 导入</Button>
        </Upload>
        <Button icon={<DownloadOutlined />} onClick={downloadTemplate}>下载模板</Button>
      </Space>

      {importResult && (
        <div style={{ marginBottom: 16, padding: 12, background: '#f0f0f0', borderRadius: 4 }}>
          <div>导入总数：{importResult.total}</div>
          <div style={{ color: '#52c41a' }}>成功：{importResult.success}</div>
          {importResult.failed > 0 && <div style={{ color: '#ff4d4f' }}>失败：{importResult.failed}</div>}
          {importResult.errors && importResult.errors.length > 0 && (
            <div style={{ marginTop: 8, color: '#ff4d4f', fontSize: 12 }}>
              {importResult.errors.slice(0, 5).map((err, i) => <div key={i}>{err}</div>)}
              {importResult.errors.length > 5 && <div>... 还有 {importResult.errors.length - 5} 条错误</div>}
            </div>
          )}
        </div>
      )}

      <Space wrap style={{ marginBottom: 16 }}>
        <Select placeholder="教材版本" allowClear style={{ width: 120 }} value={version || undefined} onChange={setVersion} options={VERSION_OPTIONS} />
        <Select placeholder="年级" allowClear style={{ width: 100 }} value={grade || undefined} onChange={setGrade} options={GRADE_OPTIONS} />
        <Select placeholder="册别" allowClear style={{ width: 90 }} value={term || undefined} onChange={setTerm} options={TERM_OPTIONS} />
        <Input placeholder="单元" allowClear style={{ width: 160 }} value={unit} onChange={(e) => setUnit(e.target.value)} />
        <Input placeholder="英文/中文关键字" allowClear style={{ width: 180 }} value={keyword} onChange={(e) => setKeyword(e.target.value)} onPressEnter={loadList} />
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
        title={editing ? '编辑句子' : '新增句子'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        width={720}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="en" label="英文句子" rules={[{ required: true, message: '请输入英文句子' }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="zh" label="中文释义">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="audioUrl" label="音频 URL">
            <Input />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: 16 }}>
            <Form.Item name="version" label="版本">
              <Select options={VERSION_OPTIONS} />
            </Form.Item>
            <Form.Item name="grade" label="年级">
              <Select options={GRADE_OPTIONS} />
            </Form.Item>
            <Form.Item name="term" label="册别">
              <Select options={TERM_OPTIONS} />
            </Form.Item>
            <Form.Item name="unit" label="单元">
              <Input />
            </Form.Item>
          </div>
          <Form.Item name="sort" label="排序">
            <Input type="number" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
