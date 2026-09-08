/**
 * CampusManagePage.jsx - 校区管理页（行政管理模块）
 *
 * 功能:
 *   1. 校区档案列表（名称/状态/备注 + 引用学员数 + 绑定员工数）
 *   2. 新增/编辑校区（名称全库唯一；编辑时改名会同步学员主数据的校区引用）
 *   3. 启用/停用校区（停用后不可再作新学员取值，历史数据与绑定保留）
 *   4. 删除校区（被学员引用或有员工绑定时不允许删除，由后端强制校验）
 *
 * URL: /admin/campus（受 AuthGuard 保护，需要 campus:list 权限）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「行政管理 → 校区管理」菜单进入
 *
 * 数据流:
 *   listCampuses() → 分页/全量表格（数组）；createCampus/updateCampus/deleteCampus → CRUD
 *   数据隔离说明：校长/教务/学管师仅看到自己绑定的校区；本页列表随后端同样按范围返回
 */
import { useEffect, useState } from 'react';
import {
  Table, Space, Button, Input, Modal, Form, Typography, Popconfirm, message, Tag, Switch,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import {
  listCampuses, createCampus, updateCampus, deleteCampus,
} from '../../api/system';
import { usePermission } from '../../utils/usePermission';

const { Title, Text } = Typography;

/** 格式化时间 */
const formatTime = (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '-');

export default function CampusManagePage() {
  const { can } = usePermission();
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);

  // 新增/编辑弹窗
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  // ---- 加载校区列表（含统计；scope 账号仅返回所辖校区） ----
  const loadList = () => {
    setLoading(true);
    listCampuses()
      .then((res) => setList(res?.data || []))
      .catch(() => setList([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ---- 打开新增/编辑弹窗 ----
  const openModal = (campus = null) => {
    setEditing(campus);
    setModalOpen(true);
    if (campus) {
      form.setFieldsValue({ name: campus.name, remark: campus.remark });
    } else {
      form.resetFields();
    }
  };

  // ---- 保存（新增/编辑） ----
  const handleSave = () => {
    form.validateFields().then((values) => {
      setSaving(true);
      if (editing) {
        updateCampus({ ...values, id: editing.id, status: editing.status })
          .then(() => {
            message.success('校区已更新');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      } else {
        createCampus(values)
          .then(() => {
            message.success('校区已新增');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      }
    });
  };

  // ---- 启用 / 停用切换 ----
  const handleToggleStatus = (campus, checked) => {
    updateCampus({ id: campus.id, name: campus.name, remark: campus.remark, status: checked ? 1 : 0 })
      .then(() => {
        message.success(checked ? '校区已启用' : '校区已停用');
        loadList();
      })
      .catch(() => {});
  };

  // ---- 删除校区 ----
  const handleDelete = (campus) => {
    deleteCampus({ id: campus.id })
      .then(() => {
        message.success('校区已删除');
        loadList();
      })
      .catch(() => {});
  };

  // ---- 表格列 ----
  const columns = [
    { title: '校区名称', dataIndex: 'name', width: 200 },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (v, record) => (
        <Switch
          checkedChildren="启用" unCheckedChildren="停用"
          checked={v === 1} disabled={!can('campus:update')}
          onChange={(checked) => handleToggleStatus(record, checked)}
        />
      ),
    },
    {
      title: '学员数', dataIndex: 'studentCount', width: 90,
      render: (v) => <Tag color="blue">{v ?? 0}</Tag>,
    },
    {
      title: '绑定员工', dataIndex: 'roleCount', width: 100,
      render: (v) => <Tag color="geekblue">{v ?? 0}</Tag>,
    },
    { title: '备注', dataIndex: 'remark', render: (v) => v || '-' },
    { title: '创建时间', dataIndex: 'createAt', width: 170, render: formatTime },
    {
      title: '操作', key: 'action', width: 150,
      render: (_, record) => (
        <Space>
          {can('campus:update') && (
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(record)}>
              编辑
            </Button>
          )}
          {can('campus:delete') && (
            <Popconfirm
              title={`确定删除校区「${record.name}」？`}
              description={record.studentCount > 0 || record.roleCount > 0
                ? '该校区仍被学员引用或有员工绑定，系统将拒绝删除'
                : '删除后不可恢复'}
              onConfirm={() => handleDelete(record)}
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <Title level={4} style={{ margin: 0 }}>校区管理</Title>
        {can('campus:create') && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增校区</Button>
        )}
      </div>
      <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
        说明：校区名称唯一，学员主数据按校区名称归属。停用后不可再作新学员取值，已有学员与员工绑定保留。
      </Text>

      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={false}
        locale={{ emptyText: '暂无校区，点击右上角「新增校区」创建' }}
      />

      {/* ---- 新增/编辑弹窗 ---- */}
      <Modal
        title={editing ? `编辑校区：${editing.name}` : '新增校区'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }}>
          <Form.Item
            name="name" label="校区名称"
            rules={[{ required: true, message: '请输入校区名称' }, { max: 50, message: '最长 50 字' }]}
          >
            <Input placeholder="如：城东校区（全系统唯一）" maxLength={50} />
          </Form.Item>
          <Form.Item
            name="remark" label="备注"
            extra={editing ? '修改名称后将同步更新该校区下所有学员的归属' : '选填'}
          >
            <Input.TextArea placeholder="选填" maxLength={200} rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
