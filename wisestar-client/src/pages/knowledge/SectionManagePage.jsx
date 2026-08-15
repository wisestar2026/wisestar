/**
 * SectionManagePage.jsx - 小节管理页（知识管理板块）
 *
 * 功能:
 *   1. 顶部学科/章节下拉联动定位（URL query 携带 subjectId/chapterId 时优先回填）
 *   2. 小节 CRUD（真实 API，删除级联其后知识点/题库绑定）
 *   3. 「内容设置」: 编辑小节学习目标 / 内容概述 / 讲解要点（存 t_section.content JSON）
 *   4. 「练习设置」: 合并练习配置与题库绑定——上方从题库管理（t_repo）勾选绑定题库
 *      （可直接看到题库数据，全量替换保存），下方配置题量/难度/题型组合自动出题
 *      （存 t_section.practice JSON），保存时一起提交
 *   5. 「管理知识点」跳转 /knowledge/points（携带 subjectId/chapterId/sectionId）
 *
 * URL: /knowledge/sections（受 AuthGuard 保护）
 * 被谁引用: App.jsx 路由表；章节管理页「管理小节」按钮跳转进入
 *
 * 数据流:
 *   listSubjects / listChapters / listSections 三级联动；
 *   内容/练习设置为 JSON 字符串透传（前端 stringify/parse），后端仅存储；
 *   题库绑定: listRepo() 题库库 → saveSectionRepos / listSectionRepos 全量替换回显
 */

import { useEffect, useState } from 'react';
import {
  Table, Space, Button, Input, InputNumber, Select, Modal, Form, Tag, Typography, Breadcrumb, Popconfirm, Divider, message,
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, FileTextOutlined, SettingOutlined,
  ApartmentOutlined, ArrowLeftOutlined, EyeOutlined,
} from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  listSubjects, listChapters, listSections, createSection, updateSection, deleteSection,
  saveSectionRepos, listSectionRepos, listKnowledgePoints,
} from '../../api/knowledge';
import { listRepo } from '../../api/repo';
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

  // ---- 练习设置弹窗（含题库选题绑定 + 题量/难度/题型自动出题配置） ----
  const [practiceOpen, setPracticeOpen] = useState(false);
  const [practiceSection, setPracticeSection] = useState(null);
  const [practiceForm] = Form.useForm();
  const [bindKeyword, setBindKeyword] = useState('');
  const [repoList, setRepoList] = useState([]);
  const [repoTotal, setRepoTotal] = useState(0);
  const [repoCurrent, setRepoCurrent] = useState(1);
  const [repoLoading, setRepoLoading] = useState(false);
  const [selectedIds, setSelectedIds] = useState([]);
  const [savingBind, setSavingBind] = useState(false);

  // ---- 知识点查看弹窗（数据来自知识点管理 t_knowledge_point） ----
  const [kpOpen, setKpOpen] = useState(false);
  const [kpSection, setKpSection] = useState(null);
  const [kpList, setKpList] = useState([]);
  const [kpLoading, setKpLoading] = useState(false);

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

  // ---- 练习设置（题库选题绑定 + 题量/难度/题型自动出题配置，合并保存） ----
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
    // 回显已绑定题库 + 加载题库库（数据来自题库管理 t_repo）
    setBindKeyword('');
    setRepoCurrent(1);
    setSelectedIds([]);
    listSectionRepos(section.id).then((res) => {
      setSelectedIds((res?.data || []).map((r) => r.id));
    }).catch(() => { /* 已提示 */ });
    fetchRepos(1, '');
  };
  const savePractice = () => {
    practiceForm.validateFields().then((values) => {
      const payload = JSON.stringify(values);
      const sectionId = practiceSection.id;
      setSavingBind(true);
      Promise.all([
        updateSection({ id: sectionId, chapterId, practice: payload }),
        saveSectionRepos({ sectionId, repoIds: selectedIds }),
      ]).then(() => {
        message.success('练习设置已保存');
        setPracticeOpen(false);
        setSections((prev) => prev.map((s) => (s.id === sectionId
          ? { ...s, practice: payload, repoCount: selectedIds.length } : s)));
      }).finally(() => setSavingBind(false));
    });
  };

  const fetchRepos = (page, keyword) => {
    setRepoLoading(true);
    listRepo({ current: page, pageSize: 8, name: keyword || undefined })
      .then((res) => {
        setRepoList(res?.data?.list || []);
        setRepoTotal(res?.data?.total || 0);
      }).catch(() => { setRepoList([]); setRepoTotal(0); }).finally(() => setRepoLoading(false));
  };

  const onBindKeywordSearch = () => {
    setRepoCurrent(1);
    fetchRepos(1, bindKeyword);
  };

  // ---- 查看小节知识点（数据来自知识点管理 t_knowledge_point） ----
  const openKpList = (section) => {
    setKpSection(section);
    setKpOpen(true);
    setKpLoading(true);
    listKnowledgePoints({ sectionId: section.id, current: 1, pageSize: 100 }).then((res) => {
      setKpList(res?.data?.list || []);
    }).catch(() => setKpList([])).finally(() => setKpLoading(false));
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
      title: '知识点数', dataIndex: 'knowledgePointCount', width: 110, align: 'center',
      render: (count, s) => (
        <Button type="link" size="small" style={{ padding: 0 }} icon={<EyeOutlined />}
          onClick={() => openKpList(s)}>
          {count || 0} 个
        </Button>
      ),
    },
    {
      title: '题库数', dataIndex: 'repoCount', width: 100, align: 'center',
      render: (count) => (count > 0 ? <Tag color="blue">{count} 个题库</Tag> : <Tag>未绑定</Tag>),
    },
    {
      title: '操作', key: 'action', width: 500,
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
            description="其下所有知识点与题库绑定将一并删除，删除后不可恢复。"
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

      {/* 练习设置弹窗（题库选题绑定 + 题量/难度/题型自动出题配置） */}
      <Modal
        title={`练习设置 - ${practiceSection?.name || ''}`}
        open={practiceOpen}
        onOk={savePractice}
        onCancel={() => setPracticeOpen(false)}
        okText="保存"
        cancelText="取消"
        width={860}
        confirmLoading={savingBind}
        destroyOnClose
      >
        <Divider orientation="left" plain>绑定题库（来自题库管理，勾选绑定）</Divider>
        <Space style={{ marginBottom: 12 }} align="center">
          <Input.Search
            style={{ width: 320 }}
            placeholder="按题库名称搜索题库库"
            value={bindKeyword}
            onChange={(e) => setBindKeyword(e.target.value)}
            onSearch={onBindKeywordSearch}
            allowClear
          />
          <Text type="secondary">已选 {selectedIds.length} 个题库（题库数据来自题库管理 t_repo，不能在此新增）</Text>
        </Space>
        <Table
          rowKey="id"
          size="small"
          loading={repoLoading}
          dataSource={repoList}
          rowSelection={{
            selectedRowKeys: selectedIds,
            onChange: (keys) => setSelectedIds(keys),
          }}
          pagination={{
            current: repoCurrent,
            pageSize: 8,
            total: repoTotal,
            size: 'small',
            onChange: (c) => { setRepoCurrent(c); fetchRepos(c, bindKeyword); },
          }}
          columns={[
            { title: '题库名称', dataIndex: 'name', ellipsis: true, render: (n) => <Text strong>{n}</Text> },
            {
              title: '学科', dataIndex: 'subject', width: 80, align: 'center',
              render: (s) => (s ? <Tag color="geekblue">{s}</Tag> : '-'),
            },
            {
              title: '年级', dataIndex: 'grade', width: 80, align: 'center',
              render: (g) => (g ? <Tag color="purple">{g}</Tag> : '-'),
            },
            {
              title: '难度', dataIndex: 'difficulty', width: 80, align: 'center',
              render: (d) => (d ? <Tag>{d}</Tag> : '-'),
            },
            {
              title: '题目数', dataIndex: 'total', width: 80, align: 'center',
              render: (t) => (t > 0 ? t : 0),
            },
          ]}
        />
        <Divider orientation="left" plain>自动出题配置</Divider>
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

      {/* 知识点查看弹窗（数据来自知识点管理 t_knowledge_point） */}
      <Modal
        title={`知识点列表 - ${kpSection?.name || ''}`}
        open={kpOpen}
        onCancel={() => setKpOpen(false)}
        footer={
          <Space>
            <Button onClick={() => setKpOpen(false)}>关闭</Button>
            <Button type="primary" icon={<ApartmentOutlined />}
              onClick={() => navigate(`/knowledge/points?subjectId=${subjectId}&chapterId=${chapterId}&sectionId=${kpSection?.id}`)}>
              进入知识点管理
            </Button>
          </Space>
        }
        width={720}
        destroyOnClose
      >
        <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
          以下知识点来自「知识点管理」数据（t_knowledge_point），可在知识点管理页维护
        </Text>
        <Table
          rowKey="id"
          size="small"
          loading={kpLoading}
          dataSource={kpList}
          pagination={false}
          locale={{ emptyText: '该小节下暂无知识点，点击右上角「进入知识点管理」创建' }}
          columns={[
            { title: '知识点名称', dataIndex: 'name', width: 200, render: (n) => <Text strong>{n}</Text> },
            { title: '排序', dataIndex: 'sort', width: 70, align: 'center' },
            {
              title: '内容设置', width: 120, align: 'center',
              render: (_, k) => {
                let content = {};
                try { content = k.content ? JSON.parse(k.content) : {}; } catch { content = {}; }
                const pointCount = content.points?.length || 0;
                return pointCount > 0 ? <Tag color="green">{pointCount} 条要点</Tag> : <Tag>未设置</Tag>;
              },
            },
            {
              title: '绑定题目', dataIndex: 'questionCount', width: 90, align: 'center',
              render: (count) => (count > 0 ? <Tag color="blue">{count} 题</Tag> : <Tag>未绑定</Tag>),
            },
          ]}
        />
      </Modal>
    </div>
  );
}
