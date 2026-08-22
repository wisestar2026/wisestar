/**
 * UserManagePage.jsx - 用户管理页（系统管理模块）
 *
 * 功能:
 *   1. 用户分页列表（用户名/姓名/手机/邮箱/部门/角色/岗位/状态）
 *   2. 新增/编辑用户（登录账号唯一性校验；密码留空不改；角色/岗位多选）
 *   3. 删除用户（连同登录账号一并删除）
 *
 * URL: /system/users（受 AuthGuard 保护，需要 system:user:list 权限）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「系统管理 → 用户管理」菜单进入
 *
 * 数据流:
 *   listUsers(params) → 分页表格
 *   新增: checkUsernameExist 查重 → createUser({ username, name, password, roles, userPositions })
 *   编辑: updateUser({ id, ... })（用户名只读，避免查重误报）
 *   岗位: 表单岗位多选 → userPositions: [{ deptId, positionId }]
 */

import { useEffect, useState, useCallback } from 'react';
import {
  Table, Space, Button, Input, Select, Modal, Form, Typography, Popconfirm, message, Tag,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined, SwapOutlined } from '@ant-design/icons';
import {
  listUsers, createUser, updateUser, deleteUser, checkUsernameExist, updateUserPosition,
} from '../../api/system';
import { listDepts } from '../../api/system';
import { listPositions } from '../../api/system';
import { listAllRoles } from '../../api/system';
import { usePermission } from '../../utils/usePermission';

const { Title } = Typography;

/** 状态渲染：1 启用 / 0 禁用 */
const STATUS_MAP = { 1: { text: '启用', color: 'green' }, 0: { text: '禁用', color: 'red' } };

/** 格式化时间（UserView.createAt 无 @JsonFormat，统一原生格式化） */
const formatTime = (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '-');

export default function UserManagePage() {
  const { can } = usePermission();
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [name, setName] = useState('');

  // 下拉数据
  const [deptOptions, setDeptOptions] = useState([]);
  const [positionOptions, setPositionOptions] = useState([]);
  const [roleOptions, setRoleOptions] = useState([]);

  // 新增/编辑弹窗
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  // 调整岗位弹窗
  const [positionModalOpen, setPositionModalOpen] = useState(false);
  const [positionTarget, setPositionTarget] = useState(null);
  const [positionSaving, setPositionSaving] = useState(false);
  const [positionForm] = Form.useForm();

  // ---- 加载分页列表 ----
  const loadList = useCallback(() => {
    setLoading(true);
    listUsers({ current, pageSize, name: name || undefined })
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

  // ---- 加载下拉数据（部门/岗位/角色） ----
  useEffect(() => {
    listDepts().then((res) => {
      setDeptOptions((res?.data || []).map((d) => ({ value: d.id, label: d.name })));
    }).catch(() => setDeptOptions([]));
    listPositions({ pageSize: -1 }).then((res) => {
      setPositionOptions((res?.data?.list || []).map((p) => ({ value: p.id, label: p.name })));
    }).catch(() => setPositionOptions([]));
    listAllRoles().then((res) => {
      setRoleOptions((res?.data?.list || []).map((r) => ({ value: r.id, label: r.name })));
    }).catch(() => setRoleOptions([]));
  }, []);

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
  const openModal = (user = null) => {
    setEditing(user);
    setModalOpen(true);
    if (user) {
      form.setFieldsValue({
        username: user.username,
        name: user.name,
        phone: user.phone,
        email: user.email,
        deptId: user.deptId,
        roles: (user.roles || []).map((r) => r.id),
        positions: (user.userPositions || []).map((p) => p.positionId),
        status: user.status ?? 1,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ status: 1 });
    }
  };

  // ---- 保存（新增/编辑） ----
  const handleSave = () => {
    form.validateFields().then(async (values) => {
      // 新增时校验登录账号唯一性
      if (!editing) {
        const exists = await checkUsernameExist(values.username).then((res) => !!res?.data).catch(() => false);
        if (exists) {
          message.error('登录账号已存在');
          return;
        }
      }
      // 岗位多选 → userPositions（与表单部门关联；未选部门则 deptId 留空）
      const userPositions = (values.positions || []).map((positionId) => ({
        deptId: values.deptId || undefined,
        positionId,
      }));
      const payload = { ...values, userPositions };
      delete payload.positions;
      if (editing) {
        // 编辑：密码留空不修改；用户名不提交（只读，避免查重误报）
        if (!payload.password) delete payload.password;
        delete payload.username;
        payload.id = editing.id;
        setSaving(true);
        updateUser(payload)
          .then(() => {
            message.success('用户已更新');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      } else {
        setSaving(true);
        createUser(payload)
          .then(() => {
            message.success('用户已新增');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      }
    });
  };

  // ---- 删除用户 ----
  const handleDelete = (user) => {
    deleteUser({ id: user.id }).then(() => {
      message.success('用户已删除');
      loadList();
    });
  };

  // ---- 打开调整岗位弹窗 ----
  const openPositionModal = (user) => {
    setPositionTarget(user);
    setPositionModalOpen(true);
    positionForm.setFieldsValue({
      deptId: user.deptId || undefined,
      positions: (user.userPositions || []).map((p) => p.positionId),
    });
  };

  // ---- 保存调整岗位（updateUserPosition 全量替换该用户岗位） ----
  const handleSavePosition = () => {
    positionForm.validateFields().then((values) => {
      const userPositions = (values.positions || []).map((positionId) => ({
        deptId: values.deptId || undefined,
        positionId,
      }));
      setPositionSaving(true);
      updateUserPosition({ id: positionTarget.id, userPositions })
        .then(() => {
          message.success('岗位已调整');
          setPositionModalOpen(false);
          loadList();
        })
        .finally(() => setPositionSaving(false));
    });
  };

  // ---- 表格列 ----
  const columns = [
    { title: '登录账号', dataIndex: 'username', width: 130 },
    { title: '姓名', dataIndex: 'name', width: 100 },
    { title: '手机', dataIndex: 'phone', width: 120, render: (v) => v || '-' },
    { title: '邮箱', dataIndex: 'email', width: 150, render: (v) => v || '-' },
    { title: '部门', dataIndex: 'deptName', width: 110, render: (v) => v || '-' },
    {
      title: '角色', dataIndex: 'roles', width: 160,
      render: (roles) => (roles && roles.length > 0
        ? roles.map((r) => <Tag key={r.id} color="blue">{r.name}</Tag>)
        : '-'),
    },
    {
      title: '岗位', dataIndex: 'userPositions', width: 140,
      render: (positions) => (positions && positions.length > 0
        ? positions.map((p, i) => <Tag key={`${p.positionId}-${i}`}>{p.positionName}</Tag>)
        : '-'),
    },
    {
      title: '状态', dataIndex: 'status', width: 80,
      render: (v) => {
        const s = STATUS_MAP[v] || { text: v ?? '-', color: 'default' };
        return <Tag color={s.color}>{s.text}</Tag>;
      },
    },
    { title: '创建时间', dataIndex: 'createAt', width: 170, render: formatTime },
    {
      title: '操作', key: 'action', width: 200,
      render: (_, record) => (
        <Space>
          {can('system:user:update') && (
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(record)}>
              编辑
            </Button>
          )}
          {can('system:user:update') && (
            <Button type="link" size="small" icon={<SwapOutlined />} onClick={() => openPositionModal(record)}>
              调整岗位
            </Button>
          )}
          {can('system:user:delete') && (
            <Popconfirm title="确定删除该用户？" description="删除后该登录账号将无法使用" onConfirm={() => handleDelete(record)}>
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
        <Title level={4} style={{ margin: 0 }}>用户管理</Title>
        {can('system:user:create') && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增用户</Button>
        )}
      </div>

      {/* ---- 搜索栏 ---- */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          placeholder="姓名" allowClear style={{ width: 160 }} value={name}
          onChange={(e) => setName(e.target.value)} onPressEnter={handleSearch}
        />
        <Button type="primary" onClick={handleSearch}>搜索</Button>
        <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
      </Space>

      {/* ---- 用户表格 ---- */}
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
        title={editing ? '编辑用户' : '新增用户'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }}>
          <Form.Item
            name="username" label="登录账号"
            rules={editing ? [] : [{ required: true, message: '请输入登录账号' }]}
          >
            <Input placeholder={editing ? '编辑时不可修改' : '登录账号（唯一）'} maxLength={50} disabled={!!editing} />
          </Form.Item>
          <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input placeholder="用户姓名" maxLength={50} />
          </Form.Item>
          <Form.Item
            name="password" label="密码"
            rules={editing
              ? [{ min: 6, message: '密码至少 6 位' }]
              : [{ required: true, message: '请输入初始密码' }, { min: 6, message: '密码至少 6 位' }]}
          >
            <Input.Password placeholder={editing ? '留空则不修改' : '初始密码（至少 6 位）'} autoComplete="new-password" />
          </Form.Item>
          <Form.Item name="phone" label="手机">
            <Input placeholder="选填" maxLength={20} />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input placeholder="选填" maxLength={100} />
          </Form.Item>
          <Form.Item name="deptId" label="部门">
            <Select allowClear placeholder="选填" options={deptOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="roles" label="角色">
            <Select
              mode="multiple" allowClear placeholder="选填（控制菜单与接口权限）" options={roleOptions}
              showSearch optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="positions" label="岗位">
            <Select
              mode="multiple" allowClear placeholder="选填（与所选部门关联）" options={positionOptions}
              showSearch optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 1, label: '启用' },
                { value: 0, label: '禁用' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* ---- 调整岗位弹窗 ---- */}
      <Modal
        title={`调整岗位：${positionTarget?.name || ''}`}
        open={positionModalOpen}
        onOk={handleSavePosition}
        onCancel={() => setPositionModalOpen(false)}
        confirmLoading={positionSaving}
        destroyOnClose
      >
        <Form form={positionForm} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }}>
          <Form.Item name="deptId" label="部门">
            <Select allowClear placeholder="选填（岗位可关联部门）" options={deptOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="positions" label="岗位">
            <Select
              mode="multiple" allowClear placeholder="选填（保存后全量替换该用户岗位）" options={positionOptions}
              showSearch optionFilterProp="label"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
