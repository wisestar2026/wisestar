/**
 * WordManagePage.jsx - 英语单词管理页（后台管理端）
 *
 * 功能:
 *   1. 单词列表（按版本/年级/单元筛选）
 *   2. 单词 CRUD（新增/编辑/删除）
 *   3. Excel 批量导入
 *
 * URL: /english/word-manager（受 AuthGuard 保护）
 * 被谁引用：App.jsx 路由表
 *
 * 数据流:
 *   GET /api/english/word-manager/list → 单词列表
 *   POST /api/english/word-manager/create → 新增单词
 *   POST /api/english/word-manager/update → 编辑单词
 *   POST /api/english/word-manager/delete → 删除单词
 *   POST /api/english/word-manager/import → Excel 导入
 */

import { useEffect, useState, useRef } from 'react';
import { Table, Space, Button, Input, Select, Modal, Form, message, Upload, Progress } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ImportOutlined, DownloadOutlined } from '@ant-design/icons';

const API_BASE = '/api/english/word-manager';

export default function WordManagePage() {
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // 筛选条件
  const [version, setVersion] = useState('');
  const [grade, setGrade] = useState('');
  const [unit, setUnit] = useState('');
  const [spell, setSpell] = useState('');

  // 弹窗状态
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();

  // 导入状态
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const uploadRef = useRef(null);

  // 加载列表
  const loadList = () => {
    setLoading(true);
    const params = new URLSearchParams({
      current,
      pageSize,
      ...(version && { version }),
      ...(grade && { grade }),
      ...(unit && { unit }),
      ...(spell && { spell }),
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

  // 重置筛选
  const handleReset = () => {
    setVersion('');
    setGrade('');
    setUnit('');
    setSpell('');
    setCurrent(1);
  };

  // 打开新增/编辑弹窗
  const openModal = (record = null) => {
    setEditing(record);
    setModalOpen(true);
    if (record) {
      form.setFieldsValue(record);
    } else {
      form.resetFields();
      form.setFieldsValue({ version: '人教版', grade: '三年级' });
    }
  };

  // 保存单词
  const handleSave = () => {
    form.validateFields().then((values) => {
      const payload = { ...values, id: editing?.id };
      const api = editing ? '/update' : '/create';

      fetch(`${API_BASE}${api}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
        .then((res) => res.json())
        .then((res) => {
          if (res.code === 200) {
            message.success(editing ? '编辑成功' : '新增成功');
            setModalOpen(false);
            loadList();
          }
        })
        .catch(() => message.error(editing ? '编辑失败' : '新增失败'));
    });
  };

  // 删除单词
  const handleDelete = (id) => {
    fetch(`${API_BASE}/delete?id=${id}`, { method: 'POST' })
      .then((res) => res.json())
      .then((res) => {
        if (res.code === 200) {
          message.success('删除成功');
          loadList();
        }
      })
      .catch(() => message.error('删除失败'));
  };

  // Excel 导入
  const handleImport = (file) => {
    setImporting(true);
    setImportResult(null);

    const formData = new FormData();
    formData.append('file', file);

    fetch(`${API_BASE}/import`, {
      method: 'POST',
      body: formData,
    })
      .then((res) => res.json())
      .then((res) => {
        if (res.code === 200) {
          setImportResult(res.data);
          message.success(`导入完成：成功 ${res.data.success} 个，失败 ${res.data.failed} 个`);
          loadList();
        } else {
          message.error('导入失败');
        }
      })
      .catch(() => message.error('导入失败'))
      .finally(() => {
        setImporting(false);
        setTimeout(() => setImportResult(null), 5000);
      });

    return false; // 阻止自动上传
  };

  // 下载模板
  const downloadTemplate = () => {
    const template = [
      ['单词拼写', '音标', '释义', '图片 URL', '音频 URL', '例句', '版本', '年级', '单元'],
      ['apple', '/æpl/', '苹果', 'https://example.com/apple.jpg', 'https://example.com/apple.mp3', 'This is an apple.', '人教版', '三年级', 'Unit 1'],
    ];
    const csv = template.map((row) => row.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = '单词导入模板.csv';
    link.click();
  };

  // 表格列
  const columns = [
    { title: '单词', dataIndex: 'spell', width: 120 },
    { title: '音标', dataIndex: 'phonetic', width: 100 },
    { title: '释义', dataIndex: 'meaning', width: 200, ellipsis: true },
    { title: '版本', dataIndex: 'version', width: 80 },
    { title: '年级', dataIndex: 'grade', width: 80 },
    { title: '单元', dataIndex: 'unit', width: 80 },
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
      {/* 顶部工具栏 */}
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增单词</Button>
        <Upload
          ref={uploadRef}
          showUploadList={false}
          accept=".xlsx,.xls,.csv"
          beforeUpload={handleImport}
          disabled={importing}
        >
          <Button icon={<ImportOutlined />} loading={importing}>Excel 导入</Button>
        </Upload>
        <Button icon={<DownloadOutlined />} onClick={downloadTemplate}>下载模板</Button>
      </Space>

      {/* 导入结果提示 */}
      {importResult && (
        <div style={{ marginBottom: 16, padding: 12, background: '#f0f0f0', borderRadius: 4 }}>
          <div>导入总数：{importResult.total}</div>
          <div style={{ color: '#52c41a' }}>成功：{importResult.success}</div>
          {importResult.failed > 0 && (
            <div style={{ color: '#ff4d4f' }}>失败：{importResult.failed}</div>
          )}
          {importResult.errors && importResult.errors.length > 0 && (
            <div style={{ marginTop: 8, color: '#ff4d4f', fontSize: 12 }}>
              {importResult.errors.slice(0, 5).map((err, i) => (
                <div key={i}>{err}</div>
              ))}
              {importResult.errors.length > 5 && <div>... 还有 {importResult.errors.length - 5} 条错误</div>}
            </div>
          )}
        </div>
      )}

      {/* 筛选栏 */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Select placeholder="教材版本" allowClear style={{ width: 120 }} value={version} onChange={setVersion} options={[
          { value: '人教版', label: '人教版' },
          { value: '苏教版', label: '苏教版' },
          { value: '北师大版', label: '北师大版' },
          { value: '外研版', label: '外研版' },
        ]} />
        <Select placeholder="年级" allowClear style={{ width: 100 }} value={grade} onChange={setGrade} options={[
          { value: '一年级', label: '一年级' },
          { value: '二年级', label: '二年级' },
          { value: '三年级', label: '三年级' },
          { value: '四年级', label: '四年级' },
          { value: '五年级', label: '五年级' },
          { value: '六年级', label: '六年级' },
        ]} />
        <Input placeholder="单元" allowClear style={{ width: 100 }} value={unit} onChange={(e) => setUnit(e.target.value)} />
        <Input placeholder="单词拼写" allowClear style={{ width: 150 }} value={spell} onChange={(e) => setSpell(e.target.value)} onPressEnter={loadList} />
        <Button type="primary" onClick={loadList}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
      </Space>

      {/* 单词列表 */}
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

      {/* 新增/编辑弹窗 */}
      <Modal
        title={editing ? '编辑单词' : '新增单词'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        width={800}
      >
        <Form form={form} layout="vertical">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item name="spell" label="单词拼写" rules={[{ required: true, message: '请输入单词拼写' }]}>
              <Input />
            </Form.Item>
            <Form.Item name="phonetic" label="音标">
              <Input />
            </Form.Item>
          </div>
          <Form.Item name="meaning" label="释义" rules={[{ required: true, message: '请输入释义' }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item name="imageUrl" label="图片 URL">
              <Input />
            </Form.Item>
            <Form.Item name="audioUrl" label="音频 URL">
              <Input />
            </Form.Item>
          </div>
          <Form.Item name="exampleSentence" label="例句">
            <Input.TextArea rows={2} />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16 }}>
            <Form.Item name="version" label="版本">
              <Select options={[
                { value: '人教版', label: '人教版' },
                { value: '苏教版', label: '苏教版' },
                { value: '北师大版', label: '北师大版' },
                { value: '外研版', label: '外研版' },
              ]} />
            </Form.Item>
            <Form.Item name="grade" label="年级">
              <Select options={[
                { value: '一年级', label: '一年级' },
                { value: '二年级', label: '二年级' },
                { value: '三年级', label: '三年级' },
                { value: '四年级', label: '四年级' },
                { value: '五年级', label: '五年级' },
                { value: '六年级', label: '六年级' },
              ]} />
            </Form.Item>
            <Form.Item name="unit" label="单元">
              <Input />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  );
}
