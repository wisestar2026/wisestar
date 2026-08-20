/**
 * StudentManagePage.jsx - 学员管理页（学员管理模块）
 *
 * 功能:
 *   1. 学员分页列表（姓名/学号/联系号码搜索）
 *   2. 新增/编辑学员（新增时学号由系统自动生成；编辑时学号只读不可改）
 *   3. 删除学员（逻辑删除）
 *
 * URL: /students（受 AuthGuard 保护，管理端）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「学员管理 → 学员列表」菜单进入
 *
 * 数据流:
 *   listStudents(params) → 分页表格；createStudent/updateStudent/deleteStudent → CRUD
 *   学号 8 位数字由后端自动生成，前端不可编辑；初始密码固定 123456（学员端登录用）
 */

import { useEffect, useState } from 'react';
import {
  Table, Space, Button, Input, Select, InputNumber, Modal, Form, Typography, Popconfirm, message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  listStudents, createStudent, updateStudent, deleteStudent,
} from '../../api/student';
import { usePermission } from '../../utils/usePermission';

const { Title } = Typography;

// 校区下拉（本迭代仅占位，业务逻辑后续迭代）
const CAMPUS_OPTIONS = [
  { value: '城东校区', label: '城东校区' },
  { value: '城西校区', label: '城西校区' },
  { value: '城南校区', label: '城南校区' },
  { value: '城北校区', label: '城北校区' },
];

export default function StudentManagePage() {
  const { can } = usePermission();
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // 搜索条件
  const [name, setName] = useState('');
  const [studentNo, setStudentNo] = useState('');
  const [phone, setPhone] = useState('');

  // 新增/编辑弹窗
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null); // null=新增, 对象=编辑
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  // ---- 加载学员分页 ----
  const loadList = () => {
    setLoading(true);
    listStudents({ current, pageSize, name: name || undefined, studentNo: studentNo || undefined, phone: phone || undefined })
      .then((res) => {
        setList(res?.data?.list || []);
        setTotal(res?.data?.total || 0);
      })
      .catch(() => setList([]))
      .finally(() => setLoading(false));
  };

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
    setStudentNo('');
    setPhone('');
    setCurrent(1);
  };

  // ---- 打开新增/编辑弹窗 ----
  const openModal = (student = null) => {
    setEditing(student);
    setModalOpen(true);
    if (student) {
      form.setFieldsValue({
        name: student.name, age: student.age, phone: student.phone,
        school: student.school, campus: student.campus,
      });
    } else {
      form.resetFields();
    }
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      setSaving(true);
      if (editing) {
        updateStudent({ ...values, id: editing.id }).then(() => {
          message.success('学员已更新');
          setModalOpen(false);
          loadList();
        }).finally(() => setSaving(false));
      } else {
        createStudent(values).then((res) => {
          message.success(`学员已新增，学号：${res?.data?.studentNo || ''}（初始密码 123456）`);
          setModalOpen(false);
          loadList();
        }).finally(() => setSaving(false));
      }
    });
  };

  // ---- 删除学员 ----
  const handleDelete = (student) => {
    deleteStudent({ id: student.id }).then(() => {
      message.success('学员已删除');
      loadList();
    });
  };

  // ---- 表格列 ----
  const columns = [
    { title: '学号', dataIndex: 'studentNo', width: 110 },
    { title: '姓名', dataIndex: 'name', width: 100 },
    { title: '年龄', dataIndex: 'age', width: 70, render: (v) => v ?? '-' },
    { title: '联系号码', dataIndex: 'phone', width: 130 },
    { title: '学校', dataIndex: 'school', render: (v) => v || '-' },
    { title: '校区', dataIndex: 'campus', width: 110, render: (v) => v || '-' },
    { title: '创建时间', dataIndex: 'createAt', width: 170 },
    {
      title: '操作', key: 'action', width: 140,
      render: (_, record) => (
        <Space>
          {can('student:update') && (
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(record)}>
              编辑
            </Button>
          )}
          {can('student:delete') && (
            <Popconfirm title="确定删除该学员？" description="删除后学员端将无法登录" onConfirm={() => handleDelete(record)}>
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
        <Title level={4} style={{ margin: 0 }}>学员列表</Title>
        {can('student:create') && (<Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增学员</Button>)}
      </div>

      {/* ---- 搜索栏 ---- */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          placeholder="姓名" allowClear style={{ width: 140 }} value={name}
          onChange={(e) => setName(e.target.value)} onPressEnter={handleSearch}
        />
        <Input
          placeholder="学号" allowClear style={{ width: 140 }} value={studentNo}
          onChange={(e) => setStudentNo(e.target.value)} onPressEnter={handleSearch}
        />
        <Input
          placeholder="联系号码" allowClear style={{ width: 150 }} value={phone}
          onChange={(e) => setPhone(e.target.value)} onPressEnter={handleSearch}
        />
        <Button type="primary" onClick={handleSearch}>搜索</Button>
        <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
      </Space>

      {/* ---- 学员表格 ---- */}
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
        title={editing ? '编辑学员' : '新增学员'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }}>
          {/* 学号：新增时自动生成，编辑时只读 */}
          <Form.Item label="学号">
            <Input value={editing?.studentNo || '系统自动生成'} disabled />
          </Form.Item>
          <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input placeholder="学员真实姓名" maxLength={50} />
          </Form.Item>
          <Form.Item name="age" label="年龄">
            <InputNumber min={1} max={100} style={{ width: '100%' }} placeholder="选填" />
          </Form.Item>
          <Form.Item name="phone" label="联系号码" rules={[{ required: true, message: '请输入联系号码' }]}>
            <Input placeholder="学员联系手机号" maxLength={20} />
          </Form.Item>
          <Form.Item name="school" label="学校">
            <Input placeholder="选填" maxLength={100} />
          </Form.Item>
          <Form.Item name="campus" label="校区">
            <Select
              allowClear placeholder="选填（本迭代仅占位）" options={CAMPUS_OPTIONS}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
