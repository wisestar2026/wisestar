/**
 * DeptManagePage.jsx - 部门管理页（系统管理模块）
 *
 * 功能:
 *   1. 部门树形列表（parentId 平铺结构 → 前端构建树）
 *   2. 新增/编辑部门（父部门用 TreeSelect；名称必填；简称/编码/备注选填）
 *   3. 删除部门（存在子部门时前端提示先删除子部门）
 *
 * URL: /system/depts（受 AuthGuard 保护，需要 system:dept:list 权限）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「系统管理 → 部门管理」菜单进入
 *
 * 数据流:
 *   listDepts() → 平铺数组 → buildTree() 成树 → Table 树形展示
 *   新增/编辑: addDept / updateDept（DeptRequest）
 *   删除: deleteDept({ id })
 */

import { useEffect, useState, useCallback } from 'react';
import {
  Table, Space, Button, Input, Modal, Form, Typography, Popconfirm, message, TreeSelect,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SortAscendingOutlined } from '@ant-design/icons';
import { listDepts, addDept, updateDept, deleteDept, sortDept } from '../../api/system';
import { usePermission } from '../../utils/usePermission';

const { Title } = Typography;

/** 平铺部门数组 → 树形（antd Table dataSource 使用） */
const buildTree = (list) => {
  const map = {};
  (list || []).forEach((d) => { map[d.id] = { ...d, children: [] }; });
  const roots = [];
  (list || []).forEach((d) => {
    if (d.parentId && map[d.parentId]) {
      map[d.parentId].children.push(map[d.id]);
    } else {
      roots.push(map[d.id]);
    }
  });
  return roots;
};

/** 树形 → TreeSelect options */
const toTreeSelectData = (nodes) => (nodes || []).map((n) => ({
  value: n.id,
  title: n.name,
  children: toTreeSelectData(n.children),
}));

/** 收集节点自身及全部子孙 id（编辑时排除，防止父部门成环） */
const collectSelfAndDesc = (nodes, set) => {
  (nodes || []).forEach((n) => {
    set.add(n.id);
    collectSelfAndDesc(n.children, set);
  });
};

export default function DeptManagePage() {
  const { can } = usePermission();
  const [tree, setTree] = useState([]);
  const [flat, setFlat] = useState([]);
  const [loading, setLoading] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  // ---- 加载部门 ----
  const loadList = useCallback(() => {
    setLoading(true);
    listDepts()
      .then((res) => {
        setFlat(res?.data || []);
        setTree(buildTree(res?.data || []));
      })
      .catch(() => { setFlat([]); setTree([]); })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadList();
  }, [loadList]);

  // ---- 打开新增/编辑弹窗 ----
  const openModal = (dept = null) => {
    setEditing(dept);
    setModalOpen(true);
    if (dept) {
      form.setFieldsValue({
        parentId: dept.parentId || undefined,
        name: dept.name,
        shortName: dept.shortName,
        code: dept.code,
        remark: dept.remark,
      });
    } else {
      form.resetFields();
    }
  };

  // 编辑时父部门选项排除自身及子孙，防止成环
  const parentOptions = useCallback(() => {
    const excluded = new Set();
    if (editing) collectSelfAndDesc(tree, excluded);
    const nodes = flat.filter((d) => !excluded.has(d.id));
    return toTreeSelectData(buildTree(nodes));
  }, [editing, tree, flat]);

  // ---- 保存 ----
  const handleSave = () => {
    form.validateFields().then((values) => {
      setSaving(true);
      if (editing) {
        updateDept({ ...values, id: editing.id })
          .then(() => {
            message.success('部门已更新');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      } else {
        addDept(values)
          .then(() => {
            message.success('部门已新增');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      }
    });
  };

  // ---- 删除 ----
  const handleDelete = (dept) => {
    const hasChildren = (dept.children || []).length > 0;
    if (hasChildren) {
      message.warning('请先删除该部门下的子部门');
      return;
    }
    deleteDept({ id: dept.id }).then(() => {
      message.success('部门已删除');
      loadList();
    });
  };

  // ---- 排序：同级上移/下移（交换 flat 中相邻同级项） ----
  const moveSibling = (record, dir) => {
    const idx = flat.findIndex((d) => d.id === record.id);
    const target = dir === 'up' ? idx - 1 : idx + 1;
    if (idx < 0 || target < 0 || target >= flat.length) return;
    const cur = flat[idx];
    const other = flat[target];
    if ((cur.parentId || null) !== (other.parentId || null)) return; // 仅同级可交换
    const next = [...flat];
    next[idx] = other;
    next[target] = cur;
    setFlat(next);
    setTree(buildTree(next));
  };

  // ---- 保存排序：按先序（树展示顺序）提交全部部门 id ----
  const saveSort = () => {
    const ids = [];
    const walk = (nodes) => (nodes || []).forEach((n) => { ids.push(n.id); walk(n.children); });
    walk(tree);
    sortDept(ids).then(() => {
      message.success('排序已保存');
      loadList();
    });
  };

  // ---- 表格列 ----
  const columns = [
    { title: '部门名称', dataIndex: 'name', width: 220 },
    { title: '简称', dataIndex: 'shortName', width: 120, render: (v) => v || '-' },
    { title: '编码', dataIndex: 'code', width: 140, render: (v) => v || '-' },
    { title: '负责人', dataIndex: 'managerName', width: 110, render: (v) => v || '-' },
    { title: '备注', dataIndex: 'remark', render: (v) => v || '-' },
    {
      title: '操作', key: 'action', width: 230,
      render: (_, record) => (
        <Space size={0}>
          {can('system:dept:update') && (
            <>
              <Button type="link" size="small" onClick={() => moveSibling(record, 'up')}>上移</Button>
              <Button type="link" size="small" onClick={() => moveSibling(record, 'down')}>下移</Button>
            </>
          )}
          {can('system:dept:update') && (
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(record)}>
              编辑
            </Button>
          )}
          {can('system:dept:delete') && (
            <Popconfirm title="确定删除该部门？" onConfirm={() => handleDelete(record)}>
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
        <Title level={4} style={{ margin: 0 }}>部门管理</Title>
        <Space>
          {can('system:dept:update') && (
            <Button icon={<SortAscendingOutlined />} onClick={saveSort}>保存排序</Button>
          )}
          {can('system:dept:create') && (<Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增部门</Button>)}
        </Space>
      </div>

      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={tree}
        pagination={false}
      />

      {/* ---- 新增/编辑弹窗 ---- */}
      <Modal
        title={editing ? '编辑部门' : '新增部门'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }}>
          <Form.Item name="parentId" label="上级部门">
            <TreeSelect
              allowClear placeholder="不选则为顶级部门" treeDefaultExpandAll
              treeData={parentOptions()}
            />
          </Form.Item>
          <Form.Item name="name" label="部门名称" rules={[{ required: true, message: '请输入部门名称' }]}>
            <Input placeholder="如：教学部" maxLength={50} />
          </Form.Item>
          <Form.Item name="shortName" label="简称">
            <Input placeholder="选填" maxLength={30} />
          </Form.Item>
          <Form.Item name="code" label="编码">
            <Input placeholder="选填（部门编码）" maxLength={50} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea placeholder="选填" rows={2} maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
