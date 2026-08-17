/**
 * DictItemManagePage.jsx - 字典条目管理页（系统管理模块）
 *
 * 功能:
 *   1. 按字典编码查询条目分页列表（URL 参数 ?dictCode=xx 预填，可改）
 *   2. 新增/编辑条目（名称/值必填；父值/层级/顺序选填）
 *   3. 删除条目
 *   4. Excel 批量导入（列格式：名称/值/父值/层级/顺序，首行表头跳过）
 *
 * URL: /system/dict-items（受 AuthGuard 保护，需要 system:dictItem:list 权限）
 * 被谁引用: App.jsx 路由表；MainLayout「系统管理 → 字典条目管理」菜单；DictManagePage 条目管理跳转
 *
 * 数据流:
 *   listDictItems({ current, pageSize, dictCode }) → 分页表格
 *   saveDictItem（create 接口 saveOrUpdate，带 id 即更新）/ deleteDictItem
 *   importDictItems(file, dictCode) → 原生 axios multipart
 */

import { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Table, Space, Button, Input, InputNumber, Upload, Modal, Form, Typography, Popconfirm, message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined, ImportOutlined } from '@ant-design/icons';
import {
  listDictItems, saveDictItem, deleteDictItem, importDictItems,
} from '../../api/system';

const { Title } = Typography;

export default function DictItemManagePage() {
  const { can } = usePermission();
  const [searchParams] = useSearchParams();
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [dictCode, setDictCode] = useState(searchParams.get('dictCode') || '');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const [importing, setImporting] = useState(false);

  // ---- 加载分页列表 ----
  const loadList = useCallback(() => {
    setLoading(true);
    listDictItems({ current, pageSize, dictCode: dictCode || undefined })
      .then((res) => {
        setList(res?.data?.list || []);
        setTotal(res?.data?.total || 0);
      })
      .catch(() => setList([]))
      .finally(() => setLoading(false));
  }, [current, pageSize, dictCode]);

  useEffect(() => {
    loadList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current, pageSize]);

  // ---- 查询 ----
  const handleSearch = () => {
    setCurrent(1);
    loadList();
  };

  // ---- 打开新增/编辑弹窗 ----
  const openModal = (item = null) => {
    setEditing(item);
    setModalOpen(true);
    if (item) {
      form.setFieldsValue({
        dictCode: item.dictCode,
        itemName: item.itemName,
        itemValue: item.itemValue,
        parentItemValue: item.parentItemValue,
        itemLevel: item.itemLevel ?? 1,
        itemOrder: item.itemOrder ?? 1,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ dictCode: dictCode || undefined, itemLevel: 1, itemOrder: 1 });
    }
  };

  // ---- 保存（create 接口为 saveOrUpdate，编辑时带 id） ----
  const handleSave = () => {
    form.validateFields().then((values) => {
      setSaving(true);
      const payload = { ...values, id: editing?.id };
      saveDictItem(payload)
        .then(() => {
          message.success(editing ? '条目已更新' : '条目已新增');
          setModalOpen(false);
          loadList();
        })
        .finally(() => setSaving(false));
    });
  };

  // ---- 删除 ----
  const handleDelete = (item) => {
    deleteDictItem({ id: item.id }).then(() => {
      message.success('条目已删除');
      loadList();
    });
  };

  // ---- Excel 导入（beforeUpload 拦截自动上传，手动提交） ----
  const handleImport = (file) => {
    if (!dictCode) {
      message.warning('请先输入字典编码再导入');
      return false;
    }
    setImporting(true);
    importDictItems(file, dictCode)
      .then(() => {
        message.success('导入成功');
        setCurrent(1);
        loadList();
      })
      .catch((err) => message.error(err?.message || '导入失败'))
      .finally(() => setImporting(false));
    return false; // 阻止 antd 自动上传
  };

  // ---- 表格列 ----
  const columns = [
    { title: '条目名称', dataIndex: 'itemName', width: 180 },
    { title: '条目值', dataIndex: 'itemValue', width: 160 },
    { title: '父值', dataIndex: 'parentItemValue', width: 140, render: (v) => v || '-' },
    { title: '层级', dataIndex: 'itemLevel', width: 80 },
    { title: '顺序', dataIndex: 'itemOrder', width: 80 },
    { title: '创建时间', dataIndex: 'createAt', width: 170 },
    {
      title: '操作', key: 'action', width: 130,
      render: (_, record) => (
        <Space>
          {can('system:dictItem:update') && (
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(record)}>
              编辑
            </Button>
          )}
          {can('system:dictItem:delete') && (
            <Popconfirm title="确定删除该条目？" onConfirm={() => handleDelete(record)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>字典条目管理</Title>
        <Space>
          {can('system:dictItem:import') && (
            <Upload beforeUpload={handleImport} showUploadList={false} accept=".xlsx,.xls" disabled={importing}>
              <Button icon={<ImportOutlined />} loading={importing}>Excel 导入</Button>
            </Upload>
          )}
          {can('system:dictItem:create') && (<Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增条目</Button>)}
        </Space>
      </div>

      {/* ---- 查询栏 ---- */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          placeholder="字典编码（如 grade）" allowClear style={{ width: 200 }} value={dictCode}
          onChange={(e) => setDictCode(e.target.value)} onPressEnter={handleSearch}
        />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button icon={<ReloadOutlined />} onClick={() => { setDictCode(''); setCurrent(1); }}>重置</Button>
      </Space>

      {/* ---- 导入模板说明 ---- */}
      <Typography.Paragraph type="secondary" style={{ marginBottom: 16 }}>
        导入 Excel 列格式（首行为表头自动跳过）：条目名称 / 条目值 / 父值(选填) / 层级(默认1) / 顺序(默认1)
      </Typography.Paragraph>

      {/* ---- 条目表格 ---- */}
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={{
          current, pageSize, total,
          showSizeChanger: true, showTotal: (t) => `共 ${t} 条`,
          onChange: (c, s) => { setCurrent(c); setPageSize(s); },
        }}
      />

      {/* ---- 新增/编辑弹窗 ---- */}
      <Modal
        title={editing ? '编辑条目' : '新增条目'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 6 }} wrapperCol={{ span: 17 }}>
          <Form.Item
            name="dictCode" label="字典编码"
            rules={[{ required: true, message: '请输入字典编码' }]}
          >
            <Input placeholder="所属字典编码" maxLength={50} disabled={!!editing} />
          </Form.Item>
          <Form.Item name="itemName" label="条目名称" rules={[{ required: true, message: '请输入条目名称' }]}>
            <Input placeholder="如：一年级" maxLength={100} />
          </Form.Item>
          <Form.Item name="itemValue" label="条目值" rules={[{ required: true, message: '请输入条目值' }]}>
            <Input placeholder="存储值，如：grade_1" maxLength={100} />
          </Form.Item>
          <Form.Item name="parentItemValue" label="父值">
            <Input placeholder="选填（多级字典时填上级条目值）" maxLength={100} />
          </Form.Item>
          <Form.Item name="itemLevel" label="层级">
            <InputNumber min={1} max={10} style={{ width: '100%' }} placeholder="默认 1" />
          </Form.Item>
          <Form.Item name="itemOrder" label="顺序">
            <InputNumber min={1} style={{ width: '100%' }} placeholder="默认 1" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
