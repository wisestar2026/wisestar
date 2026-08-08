/**
 * RepoDetailPage.jsx - 题库详情 & 组题管理页面
 *
 * 功能:
 *   1. 题库信息展示（名称、类型、标签、题目总数）
 *   2. 题目列表（分页、显示是否有答案和解析）
 *   3. 批量选择题目：从题目管理（全局题目库）勾选已有题目加入本题库
 *   4. 移除题目：单个/批量解绑（题目保留在题目管理中，不删除模板本身）
 *
 * 题目来源约定（重要）:
 *   题目信息的创建/编辑/导入唯一入口是「题目管理」板块（QuestionListPage）。
 *   本页面不再提供"新建题目"入口，只负责组题（选择题目加入题库 / 从题库移除）。
 *
 * 被谁引用: App.jsx 路由表（/repos/:id）；从 RepoListPage 点击题库名称进入
 *
 * 数据流:
 *   题库信息: listRepo({id, pageSize:1}) → GET /api/repo/list → find 出当前题库
 *   题目列表: fetchTemplates → listTemplate({current, pageSize, repoId}) → GET /api/template/list
 *   批量选择: SelectTemplateModal → bindTemplate({repoId, ids}) → POST /api/repo/bind
 *   移除: handleRemoveTemplate / handleBatchRemove → unbindTemplate({repoId, ids})
 *         → POST /api/repo/unbind（仅清空题目 repoId，题目保留在题目管理）
 *
 * URL: /repos/:id
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Table, Space, Button, Modal, Popconfirm, Typography, Tag, message, Card, Descriptions,
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, ArrowLeftOutlined,
  BookOutlined, CheckCircleOutlined, BulbOutlined,
} from '@ant-design/icons';
import { listTemplate } from '../../api/template';
import { listRepo, unbindTemplate } from '../../api/repo';
import SelectTemplateModal from '../../components/repo/SelectTemplateModal';

const { Title, Text, Paragraph } = Typography;

// 完整题型映射（含判断题）
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Select: '下拉题',
  FillBlank: '填空题', Text: '多行文本', Score: '评分题',
  Remark: '备注说明', Judge: '判断题',
};

export default function RepoDetailPage() {
  const { id: repoId } = useParams();
  const navigate = useNavigate();

  // ---- 状态 ----
  const [repo, setRepo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [templates, setTemplates] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const pageSize = 12;

  // 批量选择题目弹窗
  const [selectOpen, setSelectOpen] = useState(false);

  // 表格勾选（批量移除）
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [removing, setRemoving] = useState(false);

  // ---- 加载题库信息 ----
  // 复用 listRepo 列表接口，传入 id + pageSize:1 后从返回列表中 find 出当前题库
  useEffect(() => {
    (async () => {
      try {
        const res = await listRepo({ id: repoId, pageSize: 1 });
        const r = (res.data?.list || []).find((r) => r.id === repoId);
        setRepo(r || null);
      } catch { /* silent */ }
    })();
  }, [repoId]);

  // ---- 加载题目列表 ----
  // 只加载当前题库（repoId）下的题目
  // 数据流: 本页 → listTemplate({current, pageSize, repoId}) → GET /api/template/list
  const fetchTemplates = async (p = page) => {
    setLoading(true);
    try {
      const res = await listTemplate({ current: p, pageSize, repoId });
      setTemplates(res.data?.list || []);
      setTotal(res.data?.total || 0);
      // 刷新后清除失效的勾选（被移除的题）
      setSelectedRowKeys((prev) => prev.filter((id) => (res.data?.list || []).some((t) => t.id === id)));
    } catch {
      message.error('加载题目失败');
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { fetchTemplates(); }, [repoId]); // eslint-disable-line

  // ---- 刷新题库信息（题目总数等） ----
  const refreshRepoInfo = () => {
    listRepo({ id: repoId, pageSize: 1 }).then((res) => {
      const r = (res.data?.list || []).find((x) => x.id === repoId);
      if (r) setRepo(r);
    }).catch(() => {});
  };

  // ---- 批量选择题目成功回调 ----
  const handleSelectSuccess = () => {
    setSelectOpen(false);
    fetchTemplates(page);
    refreshRepoInfo();
  };

  // ---- 移除单题（解绑，题目保留在题目管理） ----
  const handleRemoveTemplate = async (id) => {
    try {
      await unbindTemplate({ repoId, ids: [id] });
      message.success('已从题库移除');
      fetchTemplates(page);
      refreshRepoInfo();
    } catch {
      message.error('移除失败');
    }
  };

  // ---- 批量移除 ----
  const handleBatchRemove = async () => {
    if (!selectedRowKeys.length) { message.warning('请先勾选要移除的题目'); return; }
    setRemoving(true);
    try {
      await unbindTemplate({ repoId, ids: selectedRowKeys });
      message.success(`已移除 ${selectedRowKeys.length} 道题目`);
      setSelectedRowKeys([]);
      fetchTemplates(page);
      refreshRepoInfo();
    } catch {
      message.error('批量移除失败');
    } finally {
      setRemoving(false);
    }
  };

  // ---- 渲染正确答案预览 ----
  const renderAnswer = (record) => {
    const attr = record.template?.attribute || {};
    const correct = attr.examCorrectAnswer;
    if (!correct) return <Text type="secondary">-</Text>;
    return <Tag color="green" icon={<CheckCircleOutlined />}>{correct}</Tag>;
  };

  // ---- 表格列 ----
  const columns = [
    { title: '#', width: 50, render: (_, __, idx) => (page - 1) * pageSize + idx + 1 },
    {
      title: '题目',
      dataIndex: 'name',
      ellipsis: true,
      render: (text, record) => {
        const attr = record.template?.attribute || {};
        const hasAnswer = !!attr.examCorrectAnswer;
        const hasAnalysis = !!attr.examAnalysis;
        return (
          <Space size={4}>
            <span>{text}</span>
            {hasAnswer && <Tag color="green" style={{ fontSize: 10, lineHeight: '16px' }}>答案</Tag>}
            {hasAnalysis && <Tag color="orange" style={{ fontSize: 10, lineHeight: '16px' }}>解析</Tag>}
          </Space>
        );
      },
    },
    {
      title: '题型', dataIndex: 'questionType', width: 90,
      render: (t) => <Tag>{TYPE_LABELS[t] || t}</Tag>,
    },
    {
      title: '分值', width: 60, align: 'center',
      render: (_, r) => r.template?.attribute?.examScore || '-',
    },
    {
      title: '正确答案', width: 120, render: (_, r) => renderAnswer(r),
    },
    {
      title: '标签', dataIndex: 'tag', width: 150,
      render: (tags) => (!tags?.length ? '-' : tags.slice(0, 2).map((t) => <Tag key={t} color="blue">{t}</Tag>)),
    },
    {
      title: '操作', width: 120,
      render: (_, record) => (
        <Popconfirm
          title="确定从题库移除该题？"
          description="题目仍保留在「题目管理」中，可从题库重新选择加入"
          onConfirm={() => handleRemoveTemplate(record.id)}
          okText="移除" cancelText="取消"
        >
          <Button size="small" type="link" danger icon={<DeleteOutlined />}>移除</Button>
        </Popconfirm>
      ),
    },
  ];

  // ---- 展开行：显示答案解析 ----
  const expandedRowRender = (record) => {
    const attr = record.template?.attribute || {};
    if (!attr.examAnalysis) return null;
    return (
      <Card size="small" style={{ background: '#fffbe6', border: '1px solid #ffe58f' }}>
        <Space>
          <BulbOutlined style={{ color: '#faad14' }} />
          <Text strong>答案解析：</Text>
        </Space>
        <Paragraph style={{ marginTop: 8, marginBottom: 0, whiteSpace: 'pre-wrap' }}>
          {attr.examAnalysis}
        </Paragraph>
      </Card>
    );
  };

  return (
    <div>
      {/* ---- 顶部导航 ---- */}
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/repos')}>返回题库列表</Button>
        <Title level={4} style={{ margin: 0 }}>
          <BookOutlined style={{ marginRight: 8 }} />{repo?.name || '题库详情'}
        </Title>
      </Space>

      {/* ---- 题库信息 ---- */}
      {repo && (
        <Card size="small" style={{ marginBottom: 16 }}>
          <Descriptions size="small" column={4}>
            <Descriptions.Item label="类型">
              <Tag color={repo.mode === 'exam' ? 'red' : 'blue'}>{repo.mode === 'exam' ? '考试' : '问卷'}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="题目总数">{repo.total || 0}</Descriptions.Item>
            <Descriptions.Item label="共享">
              <Tag color={repo.shared ? 'green' : 'default'}>{repo.shared ? '是' : '否'}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="描述">{repo.description || '-'}</Descriptions.Item>
          </Descriptions>
          {repo.tag?.length > 0 && (
            <div style={{ marginTop: 8 }}>{repo.tag.map((t) => <Tag key={t} color="blue">{t}</Tag>)}</div>
          )}
        </Card>
      )}

      {/* ---- 题目列表 ---- */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
        <Text strong style={{ fontSize: 15 }}>题目列表（{total}）</Text>
        <Space>
          {selectedRowKeys.length > 0 && (
            <Button
              danger size="small" icon={<DeleteOutlined />} loading={removing}
              onClick={handleBatchRemove}
            >
              批量移除（{selectedRowKeys.length}）
            </Button>
          )}
          <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => setSelectOpen(true)}>
            批量选择题目
          </Button>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={templates}
        rowKey="id"
        loading={loading}
        size="small"
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        expandable={{
          expandedRowRender,
          rowExpandable: (r) => !!(r.template?.attribute?.examAnalysis),
        }}
        pagination={{
          current: page, total, pageSize, showTotal: (t) => `共 ${t} 题`,
          onChange: (p) => { setPage(p); fetchTemplates(p); },
        }}
        scroll={{ y: 'calc(100vh - 400px)' }}
      />

      {/* ---- 空状态提示 ---- */}
      {!loading && templates.length === 0 && (
        <div style={{ textAlign: 'center', padding: '16px 0', color: '#999' }}>
          题库暂无题目。题目统一在「题目管理」中创建，创建后点击「批量选择题目」加入本题库。
        </div>
      )}

      {/* ---- 批量选择题目弹窗 ---- */}
      <SelectTemplateModal
        open={selectOpen}
        repoId={repoId}
        onCancel={() => setSelectOpen(false)}
        onSuccess={handleSelectSuccess}
      />
    </div>
  );
}
