/**
 * ChapterManagePage.jsx - 章节管理页（知识管理板块）
 *
 * 功能:
 *   1. 顶部学科下拉 → 展示该学科下的章节列表（真实 API，含小节数/题库数统计）
 *   2. 章节 CRUD（新增/编辑/删除，删除级联其后小节/知识点/绑定）
 *   3. 「小节数」列可点击 → 弹窗查看该章节下的小节列表（数据来自小节管理 t_section），
 *      弹窗内可直接跳转小节管理页维护
 *   4. 「绑定题库」→ 从题库库（t_repo）选择题库绑定到章节（全量替换保存）
 *
 * URL: /knowledge/chapters（受 AuthGuard 保护）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「知识管理 → 章节管理」菜单进入
 *
 * 数据流:
 *   listSubjects() → 学科下拉；listChapters({ subjectId }) → 当前学科章节列表
 *   listSections({ chapterId }) → 小节查看弹窗（小节数据由小节管理表 t_section 提供）
 *   listRepo() → 绑定题库弹窗题库库；saveChapterRepos / listChapterRepos 绑定回显
 */

import { useEffect, useState } from 'react';
import {
  Table, Space, Button, Input, Select, InputNumber, Modal, Form, Tag, Typography, Popconfirm, message,
  Upload,
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, ApartmentOutlined, LinkOutlined, EyeOutlined,
  ImportOutlined,
  DownloadOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import {
  listSubjects, listChapters, createChapter, updateChapter, deleteChapter,
  listSections, saveChapterRepos, listChapterRepos,
  importChapters,
} from '../../api/knowledge';
import { listRepo } from '../../api/repo';
import { usePermission } from '../../utils/usePermission';

const { Title, Text } = Typography;

export default function ChapterManagePage() {
  const { can } = usePermission();
  const navigate = useNavigate();

  const [subjects, setSubjects] = useState([]);
  const [subjectId, setSubjectId] = useState(undefined);
  const [chapters, setChapters] = useState([]);
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null); // null=新增, 对象=编辑
  const [form] = Form.useForm();

  // ---- 绑定题库弹窗（题库库选择） ----
  const [bindOpen, setBindOpen] = useState(false);
  const [bindChapter, setBindChapter] = useState(null);
  const [bindKeyword, setBindKeyword] = useState('');
  const [repoList, setRepoList] = useState([]);
  const [repoTotal, setRepoTotal] = useState(0);
  const [repoCurrent, setRepoCurrent] = useState(1);
  const [repoLoading, setRepoLoading] = useState(false);
  const [selectedIds, setSelectedIds] = useState([]);
  const [savingBind, setSavingBind] = useState(false);

  // ---- 小节查看弹窗（数据来自小节管理 t_section） ----
  const [secOpen, setSecOpen] = useState(false);
  const [secChapter, setSecChapter] = useState(null);
  const [secList, setSecList] = useState([]);
  const [secLoading, setSecLoading] = useState(false);

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

  // ---- Excel 批量导入 ----
  const handleImport = (file) => {
    if (!subjectId) { message.warning('请先选择学科后再导入'); return false; }
    setImporting(true);
    importChapters(file, subjectId)
      .then((res) => {
        message.success(`导入成功，新增 ${res?.data ?? 0} 个章节（同名已跳过）`);
        listChapters({ subjectId }).then((res) => setChapters(res?.data || [])).catch(() => setChapters([]));
      })
      .catch((err) => message.error(err?.message || '导入失败'))
      .finally(() => setImporting(false));
    return false; // 阻止 antd 自动上传
  };

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

  // ---- 绑定题库（从题库库选题，全量替换） ----
  const openBind = (chapter) => {
    setBindChapter(chapter);
    setBindOpen(true);
    setBindKeyword('');
    setRepoCurrent(1);
    setSelectedIds([]);
    listChapterRepos(chapter.id).then((res) => {
      setSelectedIds((res?.data || []).map((r) => r.id));
    }).catch(() => { /* 已提示 */ });
    fetchRepos(1, '');
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

  const saveBind = () => {
    setSavingBind(true);
    saveChapterRepos({ chapterId: bindChapter.id, repoIds: selectedIds }).then(() => {
      message.success('题库绑定已保存');
      setBindOpen(false);
      setChapters((prev) => prev.map((c) => (c.id === bindChapter.id ? { ...c, repoCount: selectedIds.length } : c)));
    }).finally(() => setSavingBind(false));
  };

  // ---- 查看章节下小节（数据来自小节管理 t_section） ----
  const openSecList = (chapter) => {
    setSecChapter(chapter);
    setSecOpen(true);
    setSecLoading(true);
    listSections({ chapterId: chapter.id }).then((res) => {
      setSecList(res?.data || []);
    }).catch(() => setSecList([])).finally(() => setSecLoading(false));
  };

  // ---- 表格列 ----
  const columns = [
    {
      title: '章节名称', dataIndex: 'name', width: 200,
      render: (n) => <Text strong>{n}</Text>,
    },
    {
      title: '图标', dataIndex: 'icon', width: 80, align: 'center',
      render: (icon) => <span style={{ fontSize: 18 }}>{icon}</span>,
    },
    {
      title: '排序', dataIndex: 'sort', width: 70, align: 'center',
    },
    {
      title: '小节数', dataIndex: 'sectionCount', width: 100, align: 'center',
      render: (count, c) => (
        <Button type="link" size="small" style={{ padding: 0 }} icon={<EyeOutlined />}
          onClick={() => openSecList(c)}>
          {count || 0} 个
        </Button>
      ),
    },
    {
      title: '题库数', dataIndex: 'repoCount', width: 100, align: 'center',
      render: (count) => (count > 0 ? <Tag color="blue">{count} 个题库</Tag> : <Tag>未绑定</Tag>),
    },
    {
      title: '操作', key: 'action', width: 380,
      render: (_, c) => (
        <Space wrap>
          <Button
            type="primary" size="small" icon={<ApartmentOutlined />}
            onClick={() => navigate(`/knowledge/sections?subjectId=${subjectId}&chapterId=${c.id}`)}
          >
            管理小节
          </Button>
          <Button size="small" icon={<LinkOutlined />} onClick={() => openBind(c)}>绑定题库</Button>
{can('knowledge:update') && (
            <Button size="small" icon={<EditOutlined />} onClick={() => openModal(c)}>编辑</Button>
          )}
          {can('knowledge:delete') && (
          <Popconfirm
            title={`删除章节「${c.name}」？`}
            description="其下所有小节与知识点将一并删除，删除后不可恢复。"
            onConfirm={() => handleDelete(c)}
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
          )}
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
        <Space>
          {can('knowledge:create') && (
            <Button icon={<DownloadOutlined />} href="/templates/chapter-import-template.xlsx" download>
              模板下载
            </Button>
          )}
          {can('knowledge:create') && (
            <Upload beforeUpload={handleImport} showUploadList={false} accept=".xlsx,.xls">
              <Button icon={<ImportOutlined />} loading={importing}>Excel 导入</Button>
            </Upload>
          )}
          {can('knowledge:create') && (<Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增章节</Button>)}
        </Space>
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

      {/* 绑定题库弹窗（题库库选择，全量替换保存） */}
      <Modal
        title={`绑定题库 - ${bindChapter?.name || ''}`}
        open={bindOpen}
        onOk={saveBind}
        onCancel={() => setBindOpen(false)}
        okText="保存绑定"
        cancelText="取消"
        width={820}
        confirmLoading={savingBind}
        destroyOnClose
      >
        <Space style={{ marginBottom: 12 }} align="center">
          <Input.Search
            style={{ width: 320 }}
            placeholder="按题库名称搜索题库库"
            value={bindKeyword}
            onChange={(e) => setBindKeyword(e.target.value)}
            onSearch={onBindKeywordSearch}
            allowClear
          />
          <Text type="secondary">已选 {selectedIds.length} 个题库（题库来自题库管理，不能在此新增）</Text>
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
      </Modal>

      {/* 小节查看弹窗（数据来自小节管理 t_section） */}
      <Modal
        title={`小节列表 - ${secChapter?.name || ''}`}
        open={secOpen}
        onCancel={() => setSecOpen(false)}
        footer={
          <Space>
            <Button onClick={() => setSecOpen(false)}>关闭</Button>
            <Button type="primary" icon={<ApartmentOutlined />}
              onClick={() => navigate(`/knowledge/sections?subjectId=${subjectId}&chapterId=${secChapter?.id}`)}>
              进入小节管理
            </Button>
          </Space>
        }
        width={720}
        destroyOnClose
      >
        <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
          以下小节来自「小节管理」数据（t_section），可在小节管理页维护
        </Text>
        <Table
          rowKey="id"
          size="small"
          loading={secLoading}
          dataSource={secList}
          pagination={false}
          locale={{ emptyText: '该章节下暂无小节，点击右上角「进入小节管理」创建' }}
          columns={[
            { title: '小节名称', dataIndex: 'name', width: 200, render: (n) => <Text strong>{n}</Text> },
            { title: '排序', dataIndex: 'sort', width: 70, align: 'center' },
            {
              title: '内容设置', width: 120, align: 'center',
              render: (_, s) => {
                let content = {};
                try { content = s.content ? JSON.parse(s.content) : {}; } catch { content = {}; }
                const done = content.objective || content.points?.length;
                return done ? <Tag color="green">已设置</Tag> : <Tag>未设置</Tag>;
              },
            },
            {
              title: '练习设置', width: 130, align: 'center',
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
              render: (count) => (count > 0 ? <Tag color="blue">{count} 个</Tag> : <Tag>0 个</Tag>),
            },
            {
              title: '题库数', dataIndex: 'repoCount', width: 90, align: 'center',
              render: (count) => (count > 0 ? <Tag color="blue">{count} 个</Tag> : <Tag>未绑定</Tag>),
            },
          ]}
        />
      </Modal>
    </div>
  );
}
