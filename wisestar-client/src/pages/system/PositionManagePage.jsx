/**
 * PositionManagePage.jsx - 岗位管理页（系统管理模块）
 *
 * 功能:
 *   1. 岗位分页列表（名称搜索）
 *   2. 新增/编辑岗位（名称必填；编码/虚拟岗/数据权限类型选填）
 *   3. 删除岗位
 *
 * URL: /system/positions（受 AuthGuard 保护，需要 system:position:list 权限）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「系统管理 → 岗位管理」菜单进入
 *
 * 数据流:
 *   listPositions(params) → 分页表格；addPosition/updatePosition/deletePosition → CRUD
 *   isVirtual: 是否虚拟岗；dataPermissionType: self 仅本人
 */

import { useEffect, useState, useCallback } from 'react';
import {
  Table, Space, Button, Input, Select, Switch, Modal, Form, Typography, Popconfirm, message, Tag,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import { listPositions, addPosition, updatePosition, deletePosition } from '../../api/system';
import { usePermission } from '../../utils/usePermission';

const { Title } = Typography;

export default function PositionManagePage() {
  const { can } = usePermission();
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [name, setName] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  // ---- 加载分页列表 ----
  const loadList = useCallback(() => {
    setLoading(true);
    listPositions({ current, pageSize, name: name || undefined })
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
  const openModal = (position = null) => {
    setEditing(position);
    setModalOpen(true);
    if (position) {
      form.setFieldsValue({
        name: position.name,
        code: position.code,
        isVirtual: !!position.isVirtual,
        dataPermissionType: position.dataPermissionType || 'self',
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ isVirtual: false, dataPermissionType: 'self' });
    }
  };

  // ---- 保存 ----
  const handleSave = () => {
    form.validateFields().then((values) => {
      setSaving(true);
      if (editing) {
        updatePosition({ ...values, id: editing.id })
          .then(() => {
            message.success('岗位已更新');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      } else {
        addPosition(values)
          .then(() => {
            message.success('岗位已新增');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      }
    });
  };

  // ---- 删除 ----
  const handleDelete = (position) => {
    deletePosition({ id: position.id }).then(() => {
      message.success('岗位已删除');
      loadList();
    });
  };

  // ---- 表格列 ----
  const columns = [
    { title: '岗位名称', dataIndex: 'name', width: 180 },
    { title: '编码', dataIndex: 'code', width: 150, render: (v) => v || '-' },
    {
      title: '虚拟岗', dataIndex: 'isVirtual', width: 90,
      render: (v) => (v ? <Tag color="orange">虚拟</Tag> : <Tag>正式</Tag>),
    },
    {
      title: '数据权限', dataIndex: 'dataPermissionType', width: 110,
      render: (v) => (v === 'self' ? '仅本人' : v || '-'),
    },
    {
      title: '操作', key: 'action', width: 130,
      render: (_, record) => (
        <Space>
          {can('system:position:update') && (
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(record)}>
              编辑
            </Button>
          )}
          {can('system:position:delete') && (
            <Popconfirm title="确定删除该岗位？" onConfirm={() => handleDelete(record)}>
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
        <Title level={4} style={{ margin: 0 }}>岗位管理</Title>
        {can('system:position:create') && (<Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增岗位</Button>)}
      </div>

      {/* ---- 搜索栏 ---- */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          placeholder="岗位名称" allowClear style={{ width: 160 }} value={name}
          onChange={(e) => setName(e.target.value)} onPressEnter={handleSearch}
        />
        <Button type="primary" onClick={handleSearch}>搜索</Button>
        <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
      </Space>

      {/* ---- 岗位表格 ---- */}
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
        title={editing ? '编辑岗位' : '新增岗位'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 6 }} wrapperCol={{ span: 17 }}>
          <Form.Item name="name" label="岗位名称" rules={[{ required: true, message: '请输入岗位名称' }]}>
            <Input placeholder="如：学科教师" maxLength={50} />
          </Form.Item>
          <Form.Item name="code" label="岗位编码">
            <Input placeholder="选填" maxLength={50} />
          </Form.Item>
          <Form.Item name="isVirtual" label="虚拟岗" valuePropName="checked">
            <Switch checkedChildren="虚拟" unCheckedChildren="正式" />
          </Form.Item>
          <Form.Item name="dataPermissionType" label="数据权限">
            <Select
              options={[
                { value: 'self', label: '仅本人' },
                { value: 'dept', label: '本部门' },
                { value: 'all', label: '全部' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
