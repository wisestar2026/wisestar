/**
 * RoleManagePage.jsx - 人事管理 · 角色权限管理页
 *
 * 功能:
 *   - 角色列表（名称/编码/备注/权限点数量/内置标识/创建时间）
 *   - 新增角色（名称 + 编码 + 备注 + 权限树勾选）
 *   - 编辑角色（基础信息 + 权限树勾选；内置角色编码不可改、不可删；管理员权限不可编辑）
 *   - 删除角色（内置角色禁用，Popconfirm 二次确认）
 *
 * 权限树:
 *   - 数据源 GET /api/system/permissionTree（按功能模块分组：模块 → 操作点）
 *   - 父子节点联动勾选；保存时仅提交叶节点（权限点编码）
 *
 * 被谁引用: App.jsx 路由（/hr/roles），MainLayout 侧边菜单「人事管理」
 * 数据来源/去向: api/hr.js（listRole/createRole/updateRole/deleteRole/getPermissionTree）
 */

import { useEffect, useMemo, useState } from 'react';
import {
  Table, Space, Button, Input, Modal, Form, Tree, Typography, Popconfirm, message, Tag,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import { listRole, createRole, updateRole, deleteRole, getPermissionTree } from '../../api/hr';

const { Text } = Typography;

export default function RoleManagePage() {
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null); // null=新增, 对象=编辑
  const [saving, setSaving] = useState(false);

  const [permissionTree, setPermissionTree] = useState([]);
  const [checkedKeys, setCheckedKeys] = useState([]);

  const [form] = Form.useForm();

  // 递归收集权限树中的叶节点（权限点编码）
  const leafKeys = useMemo(() => {
    const leaves = new Set();
    const walk = (nodes) => {
      (nodes || []).forEach((n) => {
        if (n.children && n.children.length > 0) {
          walk(n.children);
        } else {
          leaves.add(n.key);
        }
      });
    };
    walk(permissionTree);
    return leaves;
  }, [permissionTree]);

  // ============================================================
  // 加载角色列表
  // ============================================================
  const fetchList = async () => {
    setLoading(true);
    try {
      const res = await listRole({ name: keyword || undefined, current, pageSize });
      setList(res.data.list || []);
      setTotal(res.data.total || 0);
    } catch {
      // 错误提示已由拦截器统一处理
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current, pageSize]);

  // 加载权限树（页面挂载一次）
  useEffect(() => {
    getPermissionTree()
      .then((res) => setPermissionTree(res.data || []))
      .catch(() => {});
  }, []);

  // ============================================================
  // 新增 / 编辑弹窗
  // ============================================================
  const openModal = (role = null) => {
    setEditing(role);
    setCheckedKeys(role ? (role.authorities || []) : []);
    form.setFieldsValue(
      role
        ? { name: role.name, code: role.code, remark: role.remark }
        : { name: '', code: '', remark: '' },
    );
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    // 仅提交叶节点（权限点编码）
    const authorities = Array.from(new Set(checkedKeys.filter((k) => leafKeys.has(k))));
    setSaving(true);
    try {
      if (editing) {
        await updateRole({ id: editing.id, name: values.name, code: editing.code, remark: values.remark, authorities });
        message.success('角色已更新');
      } else {
        await createRole({ name: values.name, code: values.code, remark: values.remark, authorities });
        message.success('角色已创建');
      }
      setModalOpen(false);
      fetchList();
    } catch {
      // 错误提示已由拦截器统一处理
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (role) => {
    try {
      await deleteRole({ id: role.id });
      message.success('角色已删除');
      fetchList();
    } catch {
      // 错误提示已由拦截器统一处理
    }
  };

  // ============================================================
  // 表格列
  // ============================================================
  const columns = [
    { title: '角色名称', dataIndex: 'name', width: 130 },
    { title: '编码', dataIndex: 'code', width: 130, render: (v) => <Text code>{v}</Text> },
    {
      title: '内置角色',
      dataIndex: 'builtin',
      width: 100,
      render: (v) => (v === 1 ? <Tag color="gold">内置</Tag> : <Tag>自定义</Tag>),
    },
    {
      title: '权限点',
      dataIndex: 'authorities',
      width: 90,
      render: (v) => (Array.isArray(v) ? v.length : 0),
    },
    { title: '备注', dataIndex: 'remark', ellipsis: true, render: (v) => v || '-' },
    { title: '创建时间', dataIndex: 'createAt', width: 170, render: (v) => v || '-' },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space size={0}>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            disabled={record.code === 'admin'}
            onClick={() => openModal(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title={`确认删除角色「${record.name}」？`}
            description="删除后该角色关联的用户将失去对应权限"
            disabled={record.builtin === 1}
            onConfirm={() => handleDelete(record)}
          >
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
              disabled={record.builtin === 1}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // ============================================================
  // 渲染
  // ============================================================
  return (
    <div>
      <Space style={{ marginBottom: 16, justifyContent: 'space-between', width: '100%' }}>
        <Space>
          <Input.Search
            placeholder="按角色名称搜索"
            allowClear
            style={{ width: 240 }}
            onSearch={(v) => {
              setCurrent(1);
              setKeyword(v);
              // 搜索条件变化后重新查询
              setTimeout(fetchList, 0);
            }}
          />
          <Button icon={<ReloadOutlined />} onClick={fetchList}>刷新</Button>
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增角色</Button>
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
          showTotal: (t) => `共 ${t} 条`,
          onChange: (c, s) => {
            setCurrent(c);
            setPageSize(s);
          },
        }}
      />

      <Modal
        title={editing ? `编辑角色「${editing.name}」` : '新增角色'}
        open={modalOpen}
        width={560}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="name"
            label="角色名称"
            rules={[{ required: true, message: '请输入角色名称' }]}
          >
            <Input placeholder="如：值班教师" maxLength={50} />
          </Form.Item>
          <Form.Item
            name="code"
            label="角色编码"
            rules={[
              { required: true, message: '请输入角色编码' },
              { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '字母开头，仅含字母/数字/下划线' },
            ]}
          >
            <Input placeholder="如：duty_teacher" maxLength={50} disabled={!!editing} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input placeholder="角色职责说明（可选）" maxLength={100} />
          </Form.Item>
          <Form.Item label="功能权限" required>
            <div
              style={{
                border: '1px solid #f0f0f0',
                borderRadius: 6,
                padding: 12,
                maxHeight: 320,
                overflow: 'auto',
              }}
            >
              {permissionTree.length === 0 ? (
                <Text type="secondary">权限清单加载中...</Text>
              ) : (
                <Tree
                  checkable
                  defaultExpandAll
                  selectable={false}
                  treeData={permissionTree}
                  checkedKeys={checkedKeys}
                  onCheck={(keys) => setCheckedKeys(keys)}
                />
              )}
            </div>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
