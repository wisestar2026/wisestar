/**
 * OrderManagePage.jsx - 订单管理页（学员管理模块）
 *
 * 功能:
 *   1. 订单分页列表（按学员姓名搜索、按状态筛选）
 *   2. 创建订单：选择学员 + 学科多选 + 年级多选 + 教材版本单选 + 账号时长
 *      （学科数据来自知识管理 /api/subject/list；年级/教材版本/时长单位为前端常量；
 *       服务端按学科×年级笛卡尔积展开写入权限表并计算有效期）
 *   3. 作废/删除订单（作废后学员对应权限失效）
 *
 * URL: /orders（受 AuthGuard 保护，管理端）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「学员管理 → 订单管理」菜单进入
 *
 * 数据流:
 *   listStudents({ name }) → 订单弹窗学员选择（模糊搜索）
 *   listSubjects() → 学科多选；createOrder(data) → 创建；listOrders(params) → 分页表格
 */

import { useEffect, useState } from 'react';
import {
  Table, Space, Button, Input, Select, InputNumber, Modal, Form, Typography, Tag, Popconfirm, message,
} from 'antd';
import { PlusOutlined, StopOutlined, DeleteOutlined } from '@ant-design/icons';
import {
  listOrders, createOrder, cancelOrder, deleteOrder,
} from '../../api/student';
import { listStudents } from '../../api/student';
import { listSubjects } from '../../api/knowledge';
import { usePermission } from '../../utils/usePermission';

const { Title } = Typography;

// 年级（前端常量：固定枚举）
const GRADE_OPTIONS = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级']
  .map((g) => ({ value: g, label: g }));

// 教材版本（前端常量：固定枚举）
const VERSION_OPTIONS = ['人教版', '苏教版', '北师大版', '外研版']
  .map((v) => ({ value: v, label: v }));

// 时长单位（前端常量：DAY/MONTH/YEAR）
const DURATION_UNIT_OPTIONS = [
  { value: 'DAY', label: '天' },
  { value: 'MONTH', label: '月' },
  { value: 'YEAR', label: '年' },
];

// 时长单位展示文案
const UNIT_TEXT = { DAY: '天', MONTH: '个月', YEAR: '年' };

export default function OrderManagePage() {
  const { can } = usePermission();
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // 搜索/筛选条件
  const [studentName, setStudentName] = useState('');
  const [status, setStatus] = useState(undefined);

  // 创建订单弹窗
  const [modalOpen, setModalOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [subjects, setSubjects] = useState([]);
  const [studentOptions, setStudentOptions] = useState([]);
  const [form] = Form.useForm();

  // ---- 加载订单分页 ----
  const loadList = () => {
    setLoading(true);
    listOrders({
      current, pageSize,
      studentName: studentName || undefined,
      status,
    }).then((res) => {
      setList(res?.data?.list || []);
      setTotal(res?.data?.total || 0);
    }).catch(() => setList([])).finally(() => setLoading(false));
  };

  useEffect(() => {
    loadList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current, pageSize, status]);

  // ---- 学科下拉数据（打开弹窗时加载一次） ----
  const loadSubjects = () => {
    listSubjects().then((res) => setSubjects(res?.data || [])).catch(() => setSubjects([]));
  };

  // ---- 学员下拉搜索 ----
  const handleStudentSearch = (keyword) => {
    if (!keyword) return;
    listStudents({ current: 1, pageSize: 10, name: keyword }).then((res) => {
      const students = res?.data?.list || [];
      setStudentOptions(students.map((s) => ({
        value: s.id, label: `${s.studentNo} ${s.name}`,
      })));
    }).catch(() => setStudentOptions([]));
  };

  // ---- 打开创建弹窗 ----
  const openModal = () => {
    setModalOpen(true);
    setStudentOptions([]);
    form.resetFields();
    form.setFieldsValue({ durationUnit: 'MONTH', duration: 12 });
    loadSubjects();
  };

  const handleCreate = () => {
    form.validateFields().then((values) => {
      setSaving(true);
      createOrder({
        studentId: values.studentId,
        subjectIds: values.subjectIds,
        grades: values.grades,
        version: values.version,
        duration: values.duration,
        durationUnit: values.durationUnit,
      }).then((res) => {
        const order = res?.data || {};
        message.success(`订单已创建，有效期至 ${order.expireAt || ''}`);
        setModalOpen(false);
        loadList();
      }).finally(() => setSaving(false));
    });
  };

  // ---- 作废订单 ----
  const handleCancel = (order) => {
    cancelOrder({ id: order.id }).then(() => {
      message.success('订单已作废，对应权限已失效');
      loadList();
    });
  };

  // ---- 删除订单 ----
  const handleDelete = (order) => {
    deleteOrder({ id: order.id }).then(() => {
      message.success('订单已删除');
      loadList();
    });
  };

  // ---- 表格列 ----
  const columns = [
    { title: '学号', dataIndex: 'studentNo', width: 110 },
    { title: '学员姓名', dataIndex: 'studentName', width: 110 },
    {
      title: '学科', dataIndex: 'subjects', width: 140,
      render: (v) => (v || []).map((s) => s.name).join(' / ') || '-',
    },
    {
      title: '年级', dataIndex: 'grades', width: 130,
      render: (v) => (v || []).join(' / ') || '-',
    },
    { title: '教材版本', dataIndex: 'version', width: 100, render: (v) => v || '-' },
    {
      title: '时长', dataIndex: 'duration', width: 100,
      render: (v, r) => `${v} ${UNIT_TEXT[r.durationUnit] || r.durationUnit || ''}`,
    },
    { title: '有效期至', dataIndex: 'expireAt', width: 170 },
    {
      title: '状态', dataIndex: 'status', width: 80,
      render: (v) => (v === 1 ? <Tag color="green">生效</Tag> : <Tag color="default">作废</Tag>),
    },
    { title: '创建时间', dataIndex: 'createAt', width: 170 },
    {
      title: '操作', key: 'action', width: 140,
      render: (_, record) => (
        <Space>
          {record.status === 1 && can('order:delete') && (
            <Popconfirm title="确定作废该订单？" description="作废后对应学员权限将失效" onConfirm={() => handleCancel(record)}>
              <Button type="link" size="small" icon={<StopOutlined />}>作废</Button>
            </Popconfirm>
          )}
          {can('order:delete') && (
            <Popconfirm title="确定删除该订单？" onConfirm={() => handleDelete(record)}>
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
        <Title level={4} style={{ margin: 0 }}>订单管理</Title>
        {can('order:create') && (
          <Button type="primary" icon={<PlusOutlined />} onClick={openModal}>创建订单</Button>
        )}
      </div>

      {/* ---- 搜索栏 ---- */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          placeholder="学员姓名" allowClear style={{ width: 150 }} value={studentName}
          onChange={(e) => setStudentName(e.target.value)} onPressEnter={() => { setCurrent(1); loadList(); }}
        />
        <Select
          placeholder="状态" allowClear style={{ width: 120 }} value={status}
          onChange={(v) => { setStatus(v); setCurrent(1); }}
          options={[{ value: 1, label: '生效' }, { value: 0, label: '作废' }]}
        />
        <Button type="primary" onClick={() => { setCurrent(1); loadList(); }}>搜索</Button>
      </Space>

      {/* ---- 订单表格 ---- */}
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

      {/* ---- 创建订单弹窗 ---- */}
      <Modal
        title="创建订单"
        open={modalOpen}
        onOk={handleCreate}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        destroyOnClose
        width={520}
      >
        <Form form={form} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }}>
          <Form.Item
            name="studentId" label="学员" rules={[{ required: true, message: '请选择学员' }]}
          >
            <Select
              showSearch placeholder="输入姓名搜索学员" filterOption={false}
              onSearch={handleStudentSearch} options={studentOptions} notFoundContent="输入姓名搜索"
            />
          </Form.Item>
          <Form.Item
            name="subjectIds" label="学科" rules={[{ required: true, message: '请至少选择一个学科' }]}
          >
            <Select mode="multiple" placeholder="可多选" options={subjects.map((s) => ({ value: s.id, label: s.name }))} />
          </Form.Item>
          <Form.Item
            name="grades" label="年级" rules={[{ required: true, message: '请至少选择一个年级' }]}
          >
            <Select mode="multiple" placeholder="可多选" options={GRADE_OPTIONS} />
          </Form.Item>
          <Form.Item name="version" label="教材版本">
            <Select allowClear placeholder="选填" options={VERSION_OPTIONS} />
          </Form.Item>
          <Form.Item
            label="账号时长" required
            style={{ marginBottom: 0 }}
          >
            <Space.Compact block>
              <Form.Item name="duration" noStyle rules={[{ required: true, message: '请输入时长' }]}>
                <InputNumber min={1} max={3650} style={{ width: '60%' }} placeholder="时长数值" />
              </Form.Item>
              <Form.Item name="durationUnit" noStyle>
                <Select style={{ width: '40%' }} options={DURATION_UNIT_OPTIONS} />
              </Form.Item>
            </Space.Compact>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
