/**
 * RepoListPage.jsx - 练习列表页面
 *
 * 功能:
 *   1. 分页展示练习（名称、类型、题目数、标签、创建时间）
 *   2. 搜索练习名称
 *   3. 创建练习（名称、类型、描述、标签、练习标记）
 *   4. 编辑练习（属性编辑 + 组题：批量选择题目加入 / 移除题目）
 *   5. 删除练习（级联删除题目）
 *   6. 点击名称进入练习详情
 *
 * URL: /repos（受 AuthGuard 保护）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏"练习管理"菜单进入
 *
 * 数据流:
 *   列表: fetchRepos → listRepo({current, pageSize, name}) → GET /api/repo/list
 *   创建: handleSave → createRepo(values) → POST /api/repo/create → 刷新列表
 *   编辑: handleSave → updateRepo({...values, id}) → POST /api/repo/update → 刷新列表
 *   组题: 编辑弹窗内 SelectTemplateModal → bindTemplate({repoId, ids}) → POST /api/repo/bind
 *        移除: unbindTemplate({repoId, ids}) → POST /api/repo/unbind（题目保留在题目管理）
 *   删除: handleDelete → deleteRepo({id}) → POST /api/repo/delete（级联删题目）→ 刷新列表
 *   进入详情: 点击练习名称 → navigate(`/repos/${id}`) → RepoDetailPage
 */

import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Space, Input, Button, Modal, Form, Select, Tag,
  Popconfirm, Typography, message, Divider,
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined,
  BookOutlined, EditOutlined, CheckCircleOutlined,
} from '@ant-design/icons';
import { listRepo, createRepo, updateRepo, deleteRepo, unbindTemplate } from '../../api/repo';
import { listTemplate } from '../../api/template';
import { usePermission } from '../../utils/usePermission';
import SelectTemplateModal from '../../components/repo/SelectTemplateModal';

const { Title, Text } = Typography;

// 完整题型映射（含判断题）
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Select: '下拉题',
  FillBlank: '填空题', Text: '多行文本', Score: '评分题',
  Remark: '备注说明', Judge: '判断题',
};

// 练习类型映射
const MODE_MAP = {
  survey: { color: 'blue', label: '问卷' },
  exam: { color: 'red', label: '考试' },
};

export default function RepoListPage() {
  const { can } = usePermission();
  const navigate = useNavigate();
  const [form] = Form.useForm();

  // ---- 状态 ----
  const [loading, setLoading] = useState(false);
  const [repos, setRepos] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const pageSize = 20;
  const [searchName, setSearchName] = useState('');

  // 创建/编辑弹窗（共用表单，editId 为空 = 新建）
  const [createOpen, setCreateOpen] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [editId, setEditId] = useState(null);

  // 编辑弹窗内组题（题目管理）
  const [editTemplates, setEditTemplates] = useState([]);   // 当前练习题目列表
  const [editLoading, setEditLoading] = useState(false);    // 题目列表加载
  const [selectOpen, setSelectOpen] = useState(false);      // 批量选择题目弹窗

  // ---- 加载练习列表 ----
  // useCallback 依赖 searchName: 名称变化时重建函数；翻页/搜索/删除后复用
  // 数据流: 本页 → listRepo(params) → GET /api/repo/list → 渲染表格
  const fetchRepos = useCallback(async (p = 1) => {
    setLoading(true);
    try {
      // searchName 为空时传 undefined，后端忽略该条件（查全部）
      const res = await listRepo({ current: p, pageSize, name: searchName || undefined });
      setRepos(res.data?.list || []);
      setTotal(res.data?.total || 0);
    } catch {
      message.error('加载练习列表失败');
    } finally {
      setLoading(false);
    }
  }, [searchName]);

  // 首次挂载 / fetchRepos 重建时加载
  useEffect(() => {
    fetchRepos();
  }, [fetchRepos]);

  // ---- 创建/编辑练习 ----
  // values 为表单提交值: { name, mode, description, tag, shared, isPractice }
  // tag 前端为逗号分隔字符串，提交时转换为数组（后端 RepoRequest.tag 为 String[]）
  // 数据流: 弹窗表单 → createRepo / updateRepo → 刷新列表
  const handleSave = async (values) => {
    setCreateLoading(true);
    try {
      const payload = {
        ...values,
        tag: values.tag ? values.tag.split(',').map((t) => t.trim()).filter(Boolean) : [],
      };
      if (editId) {
        await updateRepo({ ...payload, id: editId });
        message.success('练习已更新');
      } else {
        await createRepo(payload);
        message.success('练习已创建，可在列表中点击「编辑」加入题目');
      }
      setCreateOpen(false);
      setEditId(null);
      form.resetFields();       // 关闭后清空表单，下次打开是干净状态
      fetchRepos(page);
    } catch {
      message.error(editId ? '更新失败' : '创建失败');
    } finally {
      setCreateLoading(false);
    }
  };

  // ---- 加载编辑弹窗内练习题目 ----
  const fetchEditTemplates = async (rid) => {
    setEditLoading(true);
    try {
      const res = await listTemplate({ current: 1, pageSize: 500, repoId: rid });
      setEditTemplates(res.data?.list || []);
    } catch {
      message.error('加载练习题目失败');
    } finally {
      setEditLoading(false);
    }
  };

  // ---- 打开编辑弹窗 ----
  // record 为练习行数据（RepoView），回填到表单；tag 数组转逗号分隔字符串
  const openEdit = (record) => {
    setEditId(record.id);
    form.setFieldsValue({
      name: record.name,
      mode: record.mode,
      description: record.description,
      tag: (record.tag || []).join(','),
      shared: record.shared,
      isPractice: record.isPractice,
      subject: record.subject,
      grade: record.grade,
      difficulty: record.difficulty,
    });
    setCreateOpen(true);
    fetchEditTemplates(record.id);
  };

  // ---- 编辑弹窗内移除单题（解绑，题目保留在题目管理） ----
  const handleRemoveFromEdit = async (id) => {
    try {
      await unbindTemplate({ repoId: editId, ids: [id] });
      message.success('已从练习移除');
      fetchEditTemplates(editId);
    } catch {
      message.error('移除失败');
    }
  };

  // ---- 编辑弹窗内批量选择题目成功回调 ----
  const handleSelectSuccess = () => {
    setSelectOpen(false);
    fetchEditTemplates(editId);
  };

  // ---- 删除练习 ----
  // 注意: 删除是级联的（练习内所有题目一并删除），弹窗文案已明确提示
  const handleDelete = async (id) => {
    try {
      await deleteRepo({ id });
      message.success('已删除');
      fetchRepos(page);
    } catch {
      message.error('删除失败');
    }
  };

  // ---- 搜索 ----
  const handleSearch = () => {
    setPage(1);
    fetchRepos(1);
  };

  // ---- 表格列 ----
  const columns = [
    {
      title: '练习名称',
      dataIndex: 'name',
      ellipsis: true,
      render: (text, record) => (
        <a onClick={() => navigate(`/repos/${record.id}`)}>{text}</a>
      ),
    },
    {
      title: '类型',
      dataIndex: 'mode',
      width: 80,
      render: (mode) => {
        const cfg = MODE_MAP[mode] || { color: 'default', label: mode };
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
    },
    {
      title: '学科',
      dataIndex: 'subject',
      width: 80,
      align: 'center',
      render: (s) => (s ? <Tag color="geekblue">{s}</Tag> : '-'),
    },
    {
      title: '年级',
      dataIndex: 'grade',
      width: 80,
      align: 'center',
      render: (g) => (g ? <Tag color="purple">{g}</Tag> : '-'),
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      width: 80,
      align: 'center',
      render: (d) => (d ? <Tag color={d === 'hard' ? 'red' : d === 'medium' ? 'orange' : 'green'}>
        {d === 'easy' ? '简单' : d === 'medium' ? '中等' : '困难'}
      </Tag> : '-'),
    },
    {
      title: '题目数',
      dataIndex: 'total',
      width: 80,
      align: 'center',
      render: (val) => val || 0,
    },
    {
      title: '标签',
      dataIndex: 'tag',
      width: 200,
      render: (tags) => {
        if (!tags || tags.length === 0) return '-';
        return tags.map((t) => <Tag key={t}>{t}</Tag>);
      },
    },
    {
      title: '共享',
      dataIndex: 'shared',
      width: 70,
      align: 'center',
      render: (val) => <Tag color={val ? 'green' : 'default'}>{val ? '是' : '否'}</Tag>,
    },
    {
      title: '创建时间',
      dataIndex: 'createAt',
      width: 170,
      render: (val) => val ? new Date(val).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      width: 130,
      render: (_, record) => (
        <Space size="small">
          {can('repo:update') && (
            <Button size="small" type="link" icon={<EditOutlined />} onClick={() => openEdit(record)}>
              编辑
            </Button>
          )}
          {can('repo:delete') && (
            <Popconfirm
              title="删除练习将同时删除其中所有题目，确定？"
              onConfirm={() => handleDelete(record.id)}
              okText="删除"
              cancelText="取消"
            >
              <Button size="small" type="link" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* ---- 标题栏 ---- */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>
          <BookOutlined style={{ marginRight: 8 }} />
          练习管理
        </Title>
        {can('repo:create') && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditId(null); form.resetFields(); setCreateOpen(true); }}>
            新建练习
          </Button>
        )}
      </div>

      {/* ---- 搜索栏 ---- */}
      <Space style={{ marginBottom: 16 }}>
        <Input
          placeholder="搜索练习名称"
          value={searchName}
          onChange={(e) => setSearchName(e.target.value)}
          onPressEnter={handleSearch}
          style={{ width: 250 }}
          prefix={<SearchOutlined />}
        />
        <Button type="primary" onClick={handleSearch}>搜索</Button>
        <Button icon={<ReloadOutlined />} onClick={() => { setSearchName(''); setPage(1); fetchRepos(1); }}>
          重置
        </Button>
      </Space>

      {/* ---- 表格 ---- */}
      <Table
        columns={columns}
        dataSource={repos}
        rowKey="id"
        loading={loading}
        size="small"
        pagination={{
          current: page,
          total,
          pageSize,
          showTotal: (t) => `共 ${t} 个练习`,
          onChange: (p) => { setPage(p); fetchRepos(p); },
        }}
        scroll={{ y: 'calc(100vh - 320px)' }}
      />

      {/* ---- 新建/编辑练习弹窗 ---- */}
      <Modal
        title={editId ? '编辑练习' : '新建练习'}
        open={createOpen}
        onCancel={() => { setCreateOpen(false); setEditId(null); setEditTemplates([]); form.resetFields(); }}
        footer={null}
        width={editId ? 720 : 480}
        destroyOnHidden
      >
        <Form form={form} onFinish={handleSave} layout="vertical">
          <Form.Item
            name="name"
            label="练习名称"
            rules={[{ required: true, message: '请输入练习名称' }]}
          >
            <Input placeholder="例如：通用单选练习" />
          </Form.Item>

          <Form.Item name="mode" label="练习类型" initialValue="survey">
            <Select
              options={[
                { label: '调查问卷', value: 'survey' },
                { label: '在线考试', value: 'exam' },
              ]}
            />
          </Form.Item>

          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} placeholder="练习说明" />
          </Form.Item>

          <Form.Item name="tag" label="标签（逗号分隔）">
            <Input placeholder="例如：通用,单选,基础" />
          </Form.Item>

          {/* 学科/年级/难度标签: 与题目管理知识点属性保持一致，
              供章节/小节绑定练习时按学科/年级/难度识别；均为可选项 */}
          <Form.Item name="subject" label="学科">
            <Input placeholder="如：数学 / 语文" />
          </Form.Item>

          <Form.Item name="grade" label="年级">
            <Input placeholder="如：一年级 / 三年级" />
          </Form.Item>

          <Form.Item name="difficulty" label="难度">
            <Select
              allowClear
              placeholder="选择难度"
              options={[
                { label: '简单', value: 'easy' },
                { label: '中等', value: 'medium' },
                { label: '困难', value: 'hard' },
              ]}
            />
          </Form.Item>

          <Form.Item name="shared" label="是否公开" initialValue={false}>
            <Select
              options={[
                { label: '私有', value: false },
                { label: '公开', value: true },
              ]}
            />
          </Form.Item>

          <Form.Item
            name="isPractice"
            label="练习练习"
            extra="开启后该练习可供学员端练习使用"
            initialValue={false}
          >
            <Select
              options={[
                { label: '否', value: false },
                { label: '是', value: true },
              ]}
            />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={createLoading} block>
              {editId ? '保存' : '创建'}
            </Button>
          </Form.Item>
        </Form>

        {/* ---- 编辑模式：组题管理（从题目管理批量选择题目 / 移除） ---- */}
        {editId && (
          <>
            <Divider style={{ margin: '4px 0 12px' }} />
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
              <Text strong>练习题目（{editTemplates.length}）</Text>
              <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => setSelectOpen(true)}>
                批量选择题目
              </Button>
            </div>
            <Table
              rowKey="id"
              size="small"
              loading={editLoading}
              dataSource={editTemplates}
              pagination={false}
              scroll={{ y: 220 }}
              columns={[
                {
                  title: '题目', dataIndex: 'name', ellipsis: true,
                  render: (text, r) => {
                    const hasAnswer = !!r.template?.attribute?.examCorrectAnswer;
                    return (
                      <Space size={4}>
                        <span>{text}</span>
                        {hasAnswer && <Tag color="green" style={{ fontSize: 10, lineHeight: '16px' }}>答案</Tag>}
                      </Space>
                    );
                  },
                },
                {
                  title: '题型', dataIndex: 'questionType', width: 80,
                  render: (t) => <Tag>{TYPE_LABELS[t] || t}</Tag>,
                },
                {
                  title: '正确答案', width: 90,
                  render: (_, r) => {
                    const c = r.template?.attribute?.examCorrectAnswer;
                    return c ? <Tag color="green" icon={<CheckCircleOutlined />} style={{ fontSize: 10 }}>{c}</Tag> : <Text type="secondary">-</Text>;
                  },
                },
                {
                  title: '', width: 56,
                  render: (_, r) => (
                    <Popconfirm title="从练习移除该题？" onConfirm={() => handleRemoveFromEdit(r.id)} okText="移除" cancelText="取消">
                      <Button size="small" type="link" danger icon={<DeleteOutlined />}>移除</Button>
                    </Popconfirm>
                  ),
                },
              ]}
            />
            {!editLoading && editTemplates.length === 0 && (
              <Text type="secondary" style={{ display: 'block', textAlign: 'center', padding: '12px 0' }}>
                练习暂无题目，点击「批量选择题目」从题目管理中添加
              </Text>
            )}
          </>
        )}

        {/* ---- 批量选择题目弹窗 ---- */}
        <SelectTemplateModal
          open={selectOpen}
          repoId={editId}
          onCancel={() => setSelectOpen(false)}
          onSuccess={handleSelectSuccess}
        />
      </Modal>
    </div>
  );
}
