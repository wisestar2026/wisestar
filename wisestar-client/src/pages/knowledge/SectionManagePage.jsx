/**
 * SectionManagePage.jsx - 小节管理页（知识管理板块）
 *
 * 功能:
 *   1. 面包屑定位: 知识管理 → 学科名 → 章节名（通过 URL query 携带 subjectId/chapterId）
 *   2. 小节 CRUD（mock 数据，存于 useKnowledgeStore）
 *   3. 「内容设置」: 编辑小节的学习目标 / 内容概述 / 讲解要点（Form.List 动态增删）
 *   4. 「练习设置」: 编辑小节的练习题量 / 难度 / 题型组合
 *   5. 「管理知识点」跳转 /knowledge/points（携带 subjectId/chapterId/sectionId）
 *
 * URL: /knowledge/sections（受 AuthGuard 保护）
 * 被谁引用: App.jsx 路由表；章节管理页「管理小节」按钮跳转进入
 *
 * 数据流:
 *   useSearchParams 读取 subjectId/chapterId → getChapter 定位章节 → sections 渲染
 *   内容/练习设置保存 → updateSectionContent / updateSectionPractice
 */

import { useState } from 'react';
import {
  Table, Space, Button, Input, InputNumber, Select, Modal, Form, Tag, Typography, Breadcrumb, Popconfirm, message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, FileTextOutlined, SettingOutlined, ApartmentOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import useKnowledgeStore, { QUESTION_TYPES, DIFFICULTY_OPTIONS } from '../../stores/useKnowledgeStore';

const { Text } = Typography;

export default function SectionManagePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const subjectId = searchParams.get('subjectId');
  const chapterId = searchParams.get('chapterId');

  const subjects = useKnowledgeStore((s) => s.subjects);
  const addSection = useKnowledgeStore((s) => s.addSection);
  const updateSection = useKnowledgeStore((s) => s.updateSection);
  const updateSectionContent = useKnowledgeStore((s) => s.updateSectionContent);
  const updateSectionPractice = useKnowledgeStore((s) => s.updateSectionPractice);
  const deleteSection = useKnowledgeStore((s) => s.deleteSection);

  const subject = subjects.find((s) => s.id === subjectId);
  const chapter = subject?.chapters.find((c) => c.id === chapterId);
  const sections = chapter?.sections || [];

  // ---- 新增/编辑弹窗 ----
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();

  // ---- 内容设置弹窗 ----
  const [contentOpen, setContentOpen] = useState(false);
  const [contentSection, setContentSection] = useState(null);
  const [contentForm] = Form.useForm();

  // ---- 练习设置弹窗 ----
  const [practiceOpen, setPracticeOpen] = useState(false);
  const [practiceSection, setPracticeSection] = useState(null);
  const [practiceForm] = Form.useForm();

  const openModal = (section = null) => {
    setEditing(section);
    setModalOpen(true);
    if (section) {
      form.setFieldsValue({ name: section.name, sort: section.sort });
    } else {
      form.resetFields();
      form.setFieldsValue({ sort: sections.length + 1 });
    }
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      if (editing) {
        updateSection(subjectId, chapterId, editing.id, values);
        message.success('小节已更新');
      } else {
        addSection(subjectId, chapterId, values);
        message.success('小节已新增');
      }
      setModalOpen(false);
    });
  };

  // ---- 内容设置 ----
  const openContent = (section) => {
    setContentSection(section);
    setContentOpen(true);
    contentForm.setFieldsValue({
      objective: section.content?.objective || '',
      overview: section.content?.overview || '',
      points: section.content?.points || [''],
    });
  };
  const saveContent = () => {
    contentForm.validateFields().then((values) => {
      updateSectionContent(subjectId, chapterId, contentSection.id, {
        objective: values.objective || '',
        overview: values.overview || '',
        points: (values.points || []).filter((p) => p && p.trim()),
      });
      message.success('内容设置已保存');
      setContentOpen(false);
    });
  };

  // ---- 练习设置 ----
  const openPractice = (section) => {
    setPracticeSection(section);
    setPracticeOpen(true);
    practiceForm.setFieldsValue({
      questionCount: section.practice?.questionCount ?? 10,
      difficulty: section.practice?.difficulty || '基础',
      types: section.practice?.types || ['Radio'],
    });
  };
  const savePractice = () => {
    practiceForm.validateFields().then((values) => {
      updateSectionPractice(subjectId, chapterId, practiceSection.id, values);
      message.success('练习设置已保存');
      setPracticeOpen(false);
    });
  };

  // ---- 表格列 ----
  const columns = [
    {
      title: '小节名称', dataIndex: 'name', width: 180,
      render: (n) => <Text strong>{n}</Text>,
    },
    {
      title: '排序', dataIndex: 'sort', width: 70, align: 'center',
    },
    {
      title: '内容设置', width: 110, align: 'center',
      render: (_, s) => {
        const done = s.content?.objective || s.content?.points?.length;
        return done ? <Tag color="green">已设置</Tag> : <Tag>未设置</Tag>;
      },
    },
    {
      title: '练习设置', width: 160, align: 'center',
      render: (_, s) => {
        const p = s.practice;
        return p
          ? <Tag color="blue">{p.questionCount}题 / {p.difficulty}</Tag>
          : <Tag>未设置</Tag>;
      },
    },
    {
      title: '知识点数', width: 90, align: 'center',
      render: (_, s) => <Tag color="green">{s.kps?.length || 0}</Tag>,
    },
    {
      title: '操作', key: 'action', width: 430,
      render: (_, s) => (
        <Space wrap>
          <Button
            type="primary" size="small" icon={<ApartmentOutlined />}
            onClick={() => navigate(`/knowledge/points?subjectId=${subjectId}&chapterId=${chapterId}&sectionId=${s.id}`)}
          >
            管理知识点
          </Button>
          <Button size="small" icon={<FileTextOutlined />} onClick={() => openContent(s)}>内容设置</Button>
          <Button size="small" icon={<SettingOutlined />} onClick={() => openPractice(s)}>练习设置</Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => openModal(s)}>编辑</Button>
          <Popconfirm
            title={`删除小节「${s.name}」？`}
            description="其下所有知识点将一并删除，删除后不可恢复。"
            onConfirm={() => { deleteSection(subjectId, chapterId, s.id); message.success('小节已删除'); }}
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
          <Button size="small" icon={<ArrowLeftOutlined />} onClick={() => navigate('/knowledge/chapters')} />
          <Breadcrumb
            items={[
              { title: <Text strong>知识管理</Text> },
              { title: <Text strong>{subject ? `${subject.icon} ${subject.name}` : '学科'}</Text> },
              { title: <Text strong>{chapter?.name || '章节'}</Text> },
            ]}
          />
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增小节</Button>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={sections}
        pagination={false}
        locale={{ emptyText: '该章节下暂无小节，点击右上角「新增小节」创建' }}
      />

      {/* 新增/编辑小节弹窗 */}
      <Modal title={editing ? '编辑小节' : '新增小节'} open={modalOpen} onOk={handleSave} onCancel={() => setModalOpen(false)} okText="保存" cancelText="取消" destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="小节名称" rules={[{ required: true, message: '请输入小节名称' }]}>
            <Input placeholder="如：加法小站" maxLength={30} />
          </Form.Item>
          <Form.Item name="sort" label="排序（数字越小越靠前）">
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 小节内容设置弹窗 */}
      <Modal
        title={`内容设置 - ${contentSection?.name || ''}`}
        open={contentOpen}
        onOk={saveContent}
        onCancel={() => setContentOpen(false)}
        okText="保存"
        cancelText="取消"
        width={680}
        destroyOnClose
      >
        <Form form={contentForm} layout="vertical">
          <Form.Item name="objective" label="学习目标">
            <Input.TextArea rows={2} placeholder="如：掌握两位数进位加法" maxLength={100} showCount />
          </Form.Item>
          <Form.Item name="overview" label="内容概述">
            <Input.TextArea rows={3} placeholder="本小节讲解内容的一段概述，将展示在学生端小节详情中" maxLength={200} showCount />
          </Form.Item>
          <Form.Item label="讲解要点（学生端以要点卡片逐条展示）">
            <Form.List name="points">
              {(fields, { add, remove }) => (
                <>
                  {fields.map(({ key, name }) => (
                    <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                      <Form.Item name={name} rules={[{ required: true, message: '请输入要点内容' }]} style={{ marginBottom: 0, flex: 1 }}>
                        <Input placeholder="输入一条讲解要点" maxLength={100} />
                      </Form.Item>
                      <Button danger size="small" onClick={() => remove(name)}>删除</Button>
                    </Space>
                  ))}
                  <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add('')}>添加要点</Button>
                </>
              )}
            </Form.List>
          </Form.Item>
        </Form>
      </Modal>

      {/* 小节练习设置弹窗 */}
      <Modal
        title={`练习设置 - ${practiceSection?.name || ''}`}
        open={practiceOpen}
        onOk={savePractice}
        onCancel={() => setPracticeOpen(false)}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={practiceForm} layout="vertical">
          <Form.Item name="questionCount" label="练习题量" rules={[{ required: true, message: '请输入题量' }]}>
            <InputNumber min={1} max={100} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="difficulty" label="难度" rules={[{ required: true, message: '请选择难度' }]}>
            <Select options={DIFFICULTY_OPTIONS.map((d) => ({ value: d, label: d }))} />
          </Form.Item>
          <Form.Item name="types" label="题型组合" rules={[{ required: true, message: '请至少选择一种题型' }]}>
            <Select mode="multiple" options={QUESTION_TYPES.map((t) => ({ value: t.value, label: t.label }))} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
