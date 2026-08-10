/**
 * ChapterManagePage.jsx - 章节管理页（知识管理板块）
 *
 * 功能:
 *   1. 学科切换（语文/数学/英语）→ 展示该学科下的章节列表
 *   2. 章节 CRUD（新增/编辑/删除，mock 数据，存于 useKnowledgeStore）
 *   3. 统计展示: 每章节包含的小节数 / 知识点数
 *   4. 「管理小节」跳转 /knowledge/sections（携带 subjectId / chapterId）
 *
 * URL: /knowledge/chapters（受 AuthGuard 保护）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「知识管理 → 章节管理」菜单进入
 *
 * 数据流:
 *   subjects 来自 useKnowledgeStore（zustand），增删改直接调用 store action
 *   当前学科默认取第一个（语文），通过 subjectId 定位章节数据
 */

import { useState } from 'react';
import {
  Table, Space, Button, Input, Select, InputNumber, Modal, Form, Tag, Typography, Popconfirm, message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ApartmentOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import useKnowledgeStore from '../../stores/useKnowledgeStore';

const { Title, Text } = Typography;

export default function ChapterManagePage() {
  const navigate = useNavigate();
  const subjects = useKnowledgeStore((s) => s.subjects);
  const addChapter = useKnowledgeStore((s) => s.addChapter);
  const updateChapter = useKnowledgeStore((s) => s.updateChapter);
  const deleteChapter = useKnowledgeStore((s) => s.deleteChapter);

  const [subjectId, setSubjectId] = useState(subjects[0]?.id);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null); // null=新增, 对象=编辑
  const [form] = Form.useForm();

  const subject = subjects.find((s) => s.id === subjectId) || subjects[0];
  const chapters = subject?.chapters || [];

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
        updateChapter(subjectId, editing.id, values);
        message.success('章节已更新');
      } else {
        addChapter(subjectId, values);
        message.success('章节已新增');
      }
      setModalOpen(false);
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
      title: '小节数', width: 90, align: 'center',
      render: (_, c) => <Tag color="blue">{c.sections?.length || 0}</Tag>,
    },
    {
      title: '知识点数', width: 100, align: 'center',
      render: (_, c) => {
        const total = (c.sections || []).reduce((sum, s) => sum + (s.kps?.length || 0), 0);
        return <Tag color="green">{total}</Tag>;
      },
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
            onConfirm={() => { deleteChapter(subjectId, c.id); message.success('章节已删除'); }}
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
        onChange={(v) => setSubjectId(v)}
        options={subjects.map((s) => ({ value: s.id, label: `${s.icon} ${s.name}` }))}
      />

      <Table
        rowKey="id"
        columns={columns}
        dataSource={chapters}
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
