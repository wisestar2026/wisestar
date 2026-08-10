/**
 * ChapterManagePage.jsx - 章节管理页（知识管理板块）
 *
 * 功能:
 *   1. 顶部学科下拉 → 展示该学科下的章节列表（真实 API，含小节数统计）
 *   2. 章节 CRUD（新增/编辑/删除，删除级联其后小节/知识点）
 *   3. 「管理小节」跳转 /knowledge/sections（携带 subjectId / chapterId）
 *
 * URL: /knowledge/chapters（受 AuthGuard 保护）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「知识管理 → 章节管理」菜单进入
 *
 * 数据流:
 *   listSubjects() → 学科下拉；listChapters({ subjectId }) → 当前学科章节列表
 *   增删改 → createChapter / updateChapter / deleteChapter → 成功后刷新列表
 */

import { useEffect, useState } from 'react';
import {
  Table, Space, Button, Input, Select, InputNumber, Modal, Form, Tag, Typography, Popconfirm, message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ApartmentOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import {
  listSubjects, listChapters, createChapter, updateChapter, deleteChapter,
} from '../../api/knowledge';

const { Title, Text } = Typography;

export default function ChapterManagePage() {
  const navigate = useNavigate();

  const [subjects, setSubjects] = useState([]);
  const [subjectId, setSubjectId] = useState(undefined);
  const [chapters, setChapters] = useState([]);
  const [loading, setLoading] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null); // null=新增, 对象=编辑
  const [form] = Form.useForm();

  // ---- 加载学科（默认选中第一个） ----
  useEffect(() => {
    listSubjects().then((res) => {
      const list = res?.data || [];
      setSubjects(list);
      setSubjectId((prev) => prev || list[0]?.id);
    }).catch(() => { /* request 拦截器已提示 */ });
  }, []);

  // ---- 学科切换 → 加载章节 ----
  useEffect(() => {
    if (!subjectId) return;
    setLoading(true);
    listChapters({ subjectId }).then((res) => {
      setChapters(res?.data || []);
    }).catch(() => setChapters([])).finally(() => setLoading(false));
  }, [subjectId]);

  // ---- 打开新增/编辑弹窗 ----
  const openModal = (chapter = null) => {
    setEditing(chapter);
    setModalOpen(true);
    if (chapter) {
      form.setFieldsValue({ name: chapter.name, icon: chapter.icon, sort: chapter.sort });
    } else {
      form.resetFields();
      form.setFieldsValue({ icon: '📖', sort: chapters.length + 1 });
    }
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      if (editing) {
        updateChapter({ ...values, id: editing.id, subjectId }).then(() => {
          message.success('章节已更新');
          setModalOpen(false);
          setChapters((prev) => prev.map((c) => (c.id === editing.id ? { ...c, ...values } : c)));
        });
      } else {
        createChapter({ ...values, subjectId }).then(() => {
          message.success('章节已新增');
          setModalOpen(false);
          setSubjectId((s) => s); // 触发章节列表刷新
          listChapters({ subjectId }).then((res) => setChapters(res?.data || []));
        });
      }
    });
  };

  // ---- 删除章节（级联删除） ----
  const handleDelete = (chapter) => {
    deleteChapter({ id: chapter.id }).then(() => {
      message.success('章节已删除');
      setChapters((prev) => prev.filter((c) => c.id !== chapter.id));
    });
  };

  // ---- 表格列 ----
  const columns = [
    {
      title: '章节名称', dataIndex: 'name', width: 220,
      render: (n) => <Text strong>{n}</Text>,
    },
    {
      title: '图标', dataIndex: 'icon', width: 90, align: 'center',
      render: (icon) => <span style={{ fontSize: 18 }}>{icon}</span>,
    },
    {
      title: '排序', dataIndex: 'sort', width: 80, align: 'center',
    },
    {
      title: '小节数', dataIndex: 'sectionCount', width: 90, align: 'center',
      render: (count) => <Tag color="blue">{count || 0}</Tag>,
    },
    {
      title: '操作', key: 'action', width: 220,
      render: (_, c) => (
        <Space>
          <Button
            type="primary" size="small" icon={<ApartmentOutlined />}
            onClick={() => navigate(`/knowledge/sections?subjectId=${subjectId}&chapterId=${c.id}`)}
          >
            管理小节
          </Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => openModal(c)}>编辑</Button>
          <Popconfirm
            title={`删除章节「${c.name}」？`}
            description="其下所有小节与知识点将一并删除，删除后不可恢复。"
            onConfirm={() => handleDelete(c)}
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* ---- 页面标题 ---- */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Space align="center">
          <Title level={4} style={{ margin: 0 }}>章节管理</Title>
          <Text type="secondary">管理各学科下的大单元（章节），进入后可管理小节</Text>
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增章节</Button>
      </div>

      <Select
        style={{ width: 200, marginBottom: 16 }}
        value={subjectId}
        onChange={setSubjectId}
        placeholder="选择学科"
        options={subjects.map((s) => ({ value: s.id, label: `${s.icon} ${s.name}` }))}
      />

      <Table
        rowKey="id"
        columns={columns}
        dataSource={chapters}
        loading={loading}
        pagination={false}
        locale={{ emptyText: '该学科下暂无章节，点击右上角「新增章节」创建' }}
      />

      {/* 新增/编辑章节弹窗 */}
      <Modal
        title={editing ? '编辑章节' : '新增章节'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="章节名称" rules={[{ required: true, message: '请输入章节名称' }]}>
            <Input placeholder="如：100以内加减法" maxLength={30} />
          </Form.Item>
          <Form.Item name="icon" label="章节图标（emoji）" rules={[{ required: true, message: '请输入图标' }]}>
            <Input placeholder="如：🧮 / 📜 / 🖋️" maxLength={4} />
          </Form.Item>
          <Form.Item name="sort" label="排序（数字越小越靠前）">
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
