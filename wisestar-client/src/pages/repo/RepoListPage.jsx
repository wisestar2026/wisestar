/**
 * RepoListPage.jsx - 题库列表页面
 *
 * 功能:
 *   1. 分页展示题库（名称、类型、题目数、标签、创建时间）
 *   2. 搜索题库名称
 *   3. 创建题库（名称、类型、描述、标签）
 *   4. 删除题库（级联删除题目）
 *   5. 点击名称进入题库详情
 *
 * URL: /repos
 */

import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Space, Input, Button, Modal, Form, Select, Tag,
  Popconfirm, Typography, message,
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined,
  BookOutlined,
} from '@ant-design/icons';
import { listRepo, createRepo, deleteRepo } from '../../api/repo';

const { Title } = Typography;

// 题库类型映射
const MODE_MAP = {
  survey: { color: 'blue', label: '问卷' },
  exam: { color: 'red', label: '考试' },
};

export default function RepoListPage() {
  const navigate = useNavigate();
  const [form] = Form.useForm();

  // ---- 状态 ----
  const [loading, setLoading] = useState(false);
  const [repos, setRepos] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const pageSize = 15;
  const [searchName, setSearchName] = useState('');

  // 创建弹窗
  const [createOpen, setCreateOpen] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);

  // ---- 加载题库列表 ----
  const fetchRepos = useCallback(async (p = 1) => {
    setLoading(true);
    try {
      const res = await listRepo({ current: p, pageSize, name: searchName || undefined });
      setRepos(res.data?.list || []);
      setTotal(res.data?.total || 0);
    } catch {
      message.error('加载题库列表失败');
    } finally {
      setLoading(false);
    }
  }, [searchName]);

  useEffect(() => {
    fetchRepos();
  }, [fetchRepos]);

  // ---- 创建题库 ----
  const handleCreate = async (values) => {
    setCreateLoading(true);
    try {
      await createRepo(values);
      message.success('题库已创建');
      setCreateOpen(false);
      form.resetFields();
      fetchRepos(page);
    } catch {
      message.error('创建失败');
    } finally {
      setCreateLoading(false);
    }
  };

  // ---- 删除题库 ----
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
      title: '题库名称',
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
      width: 100,
      render: (_, record) => (
        <Popconfirm
          title="删除题库将同时删除其中所有题目，确定？"
          onConfirm={() => handleDelete(record.id)}
          okText="删除"
          cancelText="取消"
        >
          <Button size="small" type="link" danger icon={<DeleteOutlined />}>
            删除
          </Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      {/* ---- 标题栏 ---- */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>
          <BookOutlined style={{ marginRight: 8 }} />
          题库管理
        </Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
          新建题库
        </Button>
      </div>

      {/* ---- 搜索栏 ---- */}
      <Space style={{ marginBottom: 16 }}>
        <Input
          placeholder="搜索题库名称"
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
          showTotal: (t) => `共 ${t} 个题库`,
          onChange: (p) => { setPage(p); fetchRepos(p); },
        }}
        scroll={{ y: 'calc(100vh - 320px)' }}
      />

      {/* ---- 新建题库弹窗 ---- */}
      <Modal
        title="新建题库"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); form.resetFields(); }}
        footer={null}
      >
        <Form form={form} onFinish={handleCreate} layout="vertical">
          <Form.Item
            name="name"
            label="题库名称"
            rules={[{ required: true, message: '请输入题库名称' }]}
          >
            <Input placeholder="例如：通用单选题库" />
          </Form.Item>

          <Form.Item name="mode" label="题库类型" initialValue="survey">
            <Select
              options={[
                { label: '调查问卷', value: 'survey' },
                { label: '在线考试', value: 'exam' },
              ]}
            />
          </Form.Item>

          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} placeholder="题库说明" />
          </Form.Item>

          <Form.Item name="tag" label="标签（逗号分隔）">
            <Input placeholder="例如：通用,单选,基础" />
          </Form.Item>

          <Form.Item name="shared" label="是否公开" initialValue={false} valuePropName="checked">
            <Select
              options={[
                { label: '私有', value: false },
                { label: '公开', value: true },
              ]}
            />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={createLoading} block>
              创建
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
