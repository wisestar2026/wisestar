/**
 * SectionManagePage.jsx - 小节管理页（知识管理板块）
 *
 * 功能:
 *   1. 顶部学科/章节下拉联动定位（URL query 携带 subjectId/chapterId 时优先回填）
 *   2. 小节 CRUD（真实 API，删除级联其后知识点/题目绑定）
 *   3. 「内容设置」: 编辑小节学习目标 / 内容概述 / 讲解要点（存 t_section.content JSON）
 *   4. 「练习设置」: 编辑小节练习题量 / 难度 / 题型组合（存 t_section.practice JSON）
 *   5. 「管理知识点」跳转 /knowledge/points（携带 subjectId/chapterId/sectionId）
 *
 * URL: /knowledge/sections（受 AuthGuard 保护）
 * 被谁引用: App.jsx 路由表；章节管理页「管理小节」按钮跳转进入
 *
 * 数据流:
 *   listSubjects / listChapters / listSections 三级联动；
 *   内容/练习设置为 JSON 字符串透传（前端 stringify/parse），后端仅存储
 */

import { useEffect, useState } from 'react';
import {
  Table, Space, Button, Input, InputNumber, Select, Modal, Form, Tag, Typography, Breadcrumb, Popconfirm, message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, FileTextOutlined, SettingOutlined, ApartmentOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  listSubjects, listChapters, listSections, createSection, updateSection, deleteSection,
} from '../../api/knowledge';
import { QUESTION_TYPES, DIFFICULTY_OPTIONS } from '../../stores/useKnowledgeStore';

const { Text } = Typography;

export default function SectionManagePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const urlSubjectId = searchParams.get('subjectId');
  const urlChapterId = searchParams.get('chapterId');

  const [subjects, setSubjects] = useState([]);
  const [chapters, setChapters] = useState([]);
  const [subjectId, setSubjectId] = useState(urlSubjectId || undefined);
  const [chapterId, setChapterId] = useState(urlChapterId || undefined);
  const [sections, setSections] = useState([]);
  const [loading, setLoading] = useState(false);

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

  // ---- 加载学科（默认选中第一个） ----
  useEffect(() => {
    listSubjects().then((res) => {
      const list = res?.data || [];
      setSubjects(list);
      setSubjectId((prev) => prev || list[0]?.id);
    }).catch(() => { /* request 拦截器已提示 */ });
  }, []);

  // ---- 学科切换 → 加载章节（默认选第一个） ----
  useEffect(() => {
    if (!subjectId) return;
    listChapters({ subjectId }).then((res) => {
      const list = res?.data || [];
      setChapters(list);
      setChapterId((prev) => {
        // URL 指定的章节不属于当前学科时，回退到第一个章节
        if (prev && list.some((c) => c.id === prev)) return prev;
        return list[0]?.id;
      });
    }).catch(() => setChapters([]));
  }, [subjectId]);

  // ---- 章节切换 → 加载小节 ----
  useEffect(() => {
    if (!chapterId) return;
    setLoading(true);
    listSections({ chapterId }).then((res) => {
      setSections(res?.data || []);
    }).catch(() => setSections([])).finally(() => setLoading(false));
  }, [chapterId]);

  const subject = subjects.find((s) => s.id === subjectId);
  const chapter = chapters.find((c) => c.id === chapterId);

  // ---- 新增/编辑弹窗 ----
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
        updateSection({ ...values, id: editing.id, chapterId }).then(() => {
          message.success('小节已更新');
          setModalOpen(false);
          setSections((prev) => prev.map((s) => (s.id === editing.id ? { ...s, ...values } : s)));
        });
      } else {
        createSection({ ...values, chapterId }).then(() => {
          message.success('小节已新增');
          setModalOpen(false);
          listSections({ chapterId }).then((res) => setSections(res?.data || []));
        });
      }
    });
  };

  // ---- 内容设置（JSON 透传） ----
  const openContent = (section) => {
    setContentSection(section);
    setContentOpen(true);
    let content = {};
    try { content = section.content ? JSON.parse(section.content) : {}; } catch { content = {}; }
    contentForm.setFieldsValue({
      objective: content.objective || '',
      overview: content.overview || '',
      points: content.points?.length ? content.points : [''],
    });
  };
  const saveContent = () => {
    contentForm.validateFields().then((values) => {
      const payload = JSON.stringify({
        objective: values.objective || '',
        overview: values.overview || '',
        points: (values.points || []).filter((p) => p && p.trim()),
      });
      updateSection({ id: contentSection.id, chapterId, content: payload }).then(() => {
        message.success('内容设置已保存');
        setContentOpen(false);
        setSections((prev) => prev.map((s) => (s.id === contentSection.id ? { ...s, content: payload } : s)));
      });
    });
  };

  // ---- 练习设置（JSON 透传） ----
  const openPractice = (section) => {
    setPracticeSection(section);
    setPracticeOpen(true);
    let practice = {};
    try { practice = section.practice ? JSON.parse(section.practice) : {}; } catch { practice = {}; }
    practiceForm.setFieldsValue({
      questionCount: practice.questionCount ?? 10,
      difficulty: practice.difficulty || '基础',
      types: practice.types || ['Radio'],
    });
  };
  const savePractice = () => {
    practiceForm.validateFields().then((values) => {
      const payload = JSON.stringify(values);
      updateSection({ id: practiceSection.id, chapterId, practice: payload }).then(() => {
        message.success('练习设置已保存');
        setPracticeOpen(false);
        setSections((prev) => prev.map((s) => (s.id === practiceSection.id ? { ...s, practice: payload } : s)));
      });
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
        let content = {};
        try { content = s.content ? JSON.parse(s.content) : {}; } catch { content = {}; }
        const done = content.objective || content.points?.length;
        return done ? <Tag color="green">已设置</Tag> : <Tag>未设置</Tag>;
      },
    },
    {
      title: '练习设置', width: 160, align: 'center',
      render: (_, s) => {
        let practice = null;
        try { practice = s.practice ? JSON.parse(s.practice) : null; } catch { practice = null; }
        return practice
          ? <Tag color="blue">{practice.questionCount}题 / {practice.difficulty}</Tag>
          : <Tag>未设置</Tag>;
      },
    },
    {
      title: '知识点数', dataIndex: 'knowledgePointCount', width: 90, align: 'center',
      render: (count) => <Tag color="green">{count || 0}</Tag>,
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
            onConfirm={() => {
              deleteSection({ id: s.id }).then(() => {
                message.success('小节已删除');
                setSections((prev) => prev.filter((x) => x.id !== s.id));
              });
            }}
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

      {/* ---- 三级联动下拉（前两级） ---- */}
      <Space style={{ marginBottom: 16 }}>
        <Select
          style={{ width: 180 }}
          value={subjectId}
          onChange={setSubjectId}
          placeholder="选择学科"
          options={subjects.map((s) => ({ value: s.id, label: `${s.icon} ${s.name}` }))}
        />
        <Select
          style={{ width: 220 }}
          value={chapterId}
          onChange={setChapterId}
          placeholder="选择章节"
          options={chapters.map((c) => ({ value: c.id, label: `${c.icon} ${c.name}` }))}
        />
      </Space>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={sections}
        loading={loading}
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
