/**
 * DictManagePage.jsx - 字典管理页（系统管理模块）
 *
 * 功能:
 *   1. 字典分页列表（名称搜索）
 *   2. 新增/编辑字典（编码/名称必填；备注选填；类型默认"问卷字典"）
 *   3. 删除字典（后端会同步删除该字典全部条目）
 *   4. 跳转字典条目管理（携带 dictCode）
 *
 * URL: /system/dicts（受 AuthGuard 保护，需要 system:dict:list 权限）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「系统管理 → 字典管理」菜单进入
 *
 * 数据流:
 *   listDicts(params) → 分页表格；addDict/updateDict/deleteDict → CRUD
 *   字典条目入口: navigate(`/system/dict-items?dictCode=${code}`)
 */

import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Space, Button, Input, Select, Modal, Form, Typography, Popconfirm, message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined, ApartmentOutlined } from '@ant-design/icons';
import { listDicts, addDict, updateDict, deleteDict } from '../../api/system';

const { Title } = Typography;

/** 字典类型：1 问卷字典（当前仅此一类） */
const DICT_TYPE_OPTIONS = [
  { value: 1, label: '问卷字典' },
];

export default function DictManagePage() {
  const navigate = useNavigate();
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [name, setName] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  // ---- 加载分页列表 ----
  const loadList = useCallback(() => {
    setLoading(true);
    listDicts({ current, pageSize, name: name || undefined })
      .then((res) => {
        setList(res?.data?.list || []);
        setTotal(res?.data?.total || 0);
      })
      .catch(() => setList([]))
      .finally(() => setLoading(false));
  }, [current, pageSize, name]);

  useEffect(() => {
    loadList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current, pageSize]);

  // ---- 搜索 ----
  const handleSearch = () => {
    setCurrent(1);
    loadList();
  };

  const handleReset = () => {
    setName('');
    setCurrent(1);
  };

  // ---- 打开新增/编辑弹窗 ----
  const openModal = (dict = null) => {
    setEditing(dict);
    setModalOpen(true);
    if (dict) {
      form.setFieldsValue({
        code: dict.code,
        name: dict.name,
        remark: dict.remark,
        dictType: dict.dictType ?? 1,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ dictType: 1 });
    }
  };

  // ---- 保存 ----
  const handleSave = () => {
    form.validateFields().then((values) => {
      setSaving(true);
      if (editing) {
        updateDict({ ...values, id: editing.id })
          .then(() => {
            message.success('字典已更新');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      } else {
        addDict(values)
          .then(() => {
            message.success('字典已新增');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      }
    });
  };

  // ---- 删除 ----
  const handleDelete = (dict) => {
    deleteDict({ id: dict.id }).then(() => {
      message.success('字典已删除（条目同步删除）');
      loadList();
    });
  };

  // ---- 表格列 ----
  const columns = [
    { title: '字典编码', dataIndex: 'code', width: 160 },
    { title: '字典名称', dataIndex: 'name', width: 180 },
    { title: '类型', dataIndex: 'dictType', width: 100, render: (v) => (v === 1 ? '问卷字典' : v ?? '-') },
    { title: '备注', dataIndex: 'remark', render: (v) => v || '-' },
    { title: '创建时间', dataIndex: 'createAt', width: 170 },
    {
      title: '操作', key: 'action', width: 230,
      render: (_, record) => (
        <Space>
          <Button
            type="link" size="small" icon={<ApartmentOutlined />}
            onClick={() => navigate(`/system/dict-items?dictCode=${encodeURIComponent(record.code)}`)}
          >
            条目管理
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除该字典？" description="该字典下全部条目将同步删除" onConfirm={() => handleDelete(record)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>字典管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增字典</Button>
      </div>

      {/* ---- 搜索栏 ---- */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          placeholder="字典名称" allowClear style={{ width: 160 }} value={name}
          onChange={(e) => setName(e.target.value)} onPressEnter={handleSearch}
        />
        <Button type="primary" onClick={handleSearch}>搜索</Button>
        <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
      </Space>

      {/* ---- 字典表格 ---- */}
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
        title={editing ? '编辑字典' : '新增字典'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }}>
          <Form.Item name="code" label="字典编码" rules={[{ required: true, message: '请输入字典编码' }]}>
            <Input placeholder="如：grade（字母数字下划线）" maxLength={50} />
          </Form.Item>
          <Form.Item name="name" label="字典名称" rules={[{ required: true, message: '请输入字典名称' }]}>
            <Input placeholder="如：年级" maxLength={50} />
          </Form.Item>
          <Form.Item name="dictType" label="类型" rules={[{ required: true }]}>
            <Select options={DICT_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea placeholder="选填" rows={2} maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
