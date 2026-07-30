/**
 * QuestionListPage.jsx - 题目管理主页
 *
 * 功能:
 *   1. 全量题目列表（跨所有题库），支持分页、搜索、筛选
 *   2. 新建/编辑题目弹窗（带答案、解析、图片上传）
 *   3. Excel 批量导入题目
 *   4. Excel 批量导出题目
 *   5. 批量删除题目
 *   6. 表格中展示题目信息：标题、图片、题型、所属题库、分值、正确答案、标签
 *
 * URL: /questions
 */

import { useState, useEffect, useCallback } from 'react';
import {
  Table, Space, Button, Input, Select, Typography, Tag, Popconfirm,
  message, Image, Tooltip,
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, EditOutlined, ImportOutlined,
  ExportOutlined, SearchOutlined, PictureOutlined, CheckCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { listTemplate, createTemplate, updateTemplate, deleteTemplate } from '../../api/template';
import { listRepo, exportTemplate } from '../../api/repo';
import QuestionEditModal from '../../components/question/QuestionEditModal';
import ImportModal from '../../components/question/ImportModal';
import { QUESTION_TYPES } from '../../utils/surveyHelpers';

const { Title, Text } = Typography;

// 完整题型映射（含判断题）
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Select: '下拉题',
  FillBlank: '填空题', Text: '多行文本', Score: '评分题',
  Remark: '备注说明', Judge: '判断题',
};

export default function QuestionListPage() {
  // ---- 列表状态 ----
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const pageSize = 15;

  // ---- 筛选状态 ----
  const [keyword, setKeyword] = useState('');
  const [filterType, setFilterType] = useState(undefined);
  const [filterRepoId, setFilterRepoId] = useState(undefined);
  const [repos, setRepos] = useState([]);               // 全量题库列表（供筛选）
  const [allReposCache, setAllReposCache] = useState([]); // 编辑弹窗用的题库列表（全量）

  // ---- 选中行 ----
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);

  // ---- 弹窗状态 ----
  const [editOpen, setEditOpen] = useState(false);
  const [editRecord, setEditRecord] = useState(null);
  const [importOpen, setImportOpen] = useState(false);

  // ---- 加载题库列表（全量，供筛选下拉 + 编辑弹窗题库选择） ----
  useEffect(() => {
    (async () => {
      try {
        const res = await listRepo({ current: 1, pageSize: 200 });
        const list = res.data?.list || [];
        setRepos(list);
        setAllReposCache(list);
      } catch { /* silent */ }
    })();
  }, []);

  // ---- 加载题目列表 ----
  const fetchData = useCallback(async (p = page) => {
    setLoading(true);
    try {
      const params = { current: p, pageSize };
      if (keyword.trim()) params.name = keyword.trim();
      if (filterType) params.questionType = filterType;
      if (filterRepoId) params.repoId = filterRepoId;
      const res = await listTemplate(params);
      setData(res.data?.list || []);
      setTotal(res.data?.total || 0);
    } catch {
      message.error('加载题目失败');
    } finally {
      setLoading(false);
    }
  }, [page, keyword, filterType, filterRepoId]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { fetchData(page); }, [page, keyword, filterType, filterRepoId, fetchData]);

  // ---- 新建 ----
  const handleCreate = () => {
    setEditRecord(null);
    setEditOpen(true);
  };

  // ---- 编辑 ----
  const handleEdit = (record) => {
    setEditRecord(record);
    setEditOpen(true);
  };

  // ---- 保存回调 ----
  const handleSave = async (payload) => {
    try {
      if (payload.id) {
        await updateTemplate(payload);
        message.success('题目已更新');
      } else {
        await createTemplate(payload);
        message.success('题目已创建');
      }
      setEditOpen(false);
      fetchData(page);
    } catch {
      message.error('保存失败');
    }
  };

  // ---- 删除单个 ----
  const handleDelete = async (id) => {
    try {
      await deleteTemplate({ ids: [id] });
      message.success('已删除');
      setSelectedRowKeys((prev) => prev.filter((k) => k !== id));
      fetchData(page);
    } catch {
      message.error('删除失败');
    }
  };

  // ---- 批量删除 ----
  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) { message.warning('请先选择题目'); return; }
    try {
      await deleteTemplate({ ids: selectedRowKeys });
      message.success(`已删除 ${selectedRowKeys.length} 道题目`);
      setSelectedRowKeys([]);
      fetchData(page);
    } catch {
      message.error('批量删除失败');
    }
  };

  // ---- 导出当前筛选结果 ----
  const handleExport = () => {
    exportTemplate({ repoId: filterRepoId });
    message.info('正在导出...');
  };

  // ---- 导入成功回调 ----
  const handleImportSuccess = () => {
    fetchData(1);
  };

  // ---- 表格列配置 ----
  const columns = [
    { title: '#', width: 50, align: 'center', render: (_, __, idx) => (page - 1) * pageSize + idx + 1 },
    {
      title: '题目', dataIndex: 'name', ellipsis: true,
      render: (text, record) => {
        const attr = record.template?.attribute || {};
        const hasAnswer = !!attr.examCorrectAnswer;
        const hasAnalysis = !!attr.examAnalysis;
        const hasImages = (attr.examImages || []).length > 0;
        return (
          <Space size={4} wrap>
            <span>{text}</span>
            {hasAnswer && <Tag color="green" style={{ fontSize: 10, lineHeight: '16px' }}>答案</Tag>}
            {hasAnalysis && <Tag color="orange" style={{ fontSize: 10, lineHeight: '16px' }}>解析</Tag>}
            {hasImages && (
              <Tooltip
                title={<Image src={attr.examImages[0]} width={160} />}
                placement="right"
              >
                <Tag color="purple" style={{ fontSize: 10, lineHeight: '16px' }}>
                  <PictureOutlined /> 图
                </Tag>
              </Tooltip>
            )}
          </Space>
        );
      },
    },
    {
      title: '题型', dataIndex: 'questionType', width: 90,
      render: (t) => <Tag>{TYPE_LABELS[t] || t}</Tag>,
    },
    {
      title: '所属题库', dataIndex: 'repoId', width: 120,
      render: (repoId) => {
        const r = repos.find((x) => x.id === repoId);
        return r ? <Tag color="blue">{r.name}</Tag> : <Text type="secondary">未分配</Text>;
      },
    },
    {
      title: '分值', width: 60, align: 'center',
      render: (_, r) => {
        const s = r.template?.attribute?.examScore;
        return s ? <Text strong>{s}</Text> : '-';
      },
    },
    {
      title: '正确答案', width: 120, render: (_, r) => {
        const correct = r.template?.attribute?.examCorrectAnswer;
        if (!correct) return <Text type="secondary">-</Text>;
        return <Tag color="green" icon={<CheckCircleOutlined />}>{correct}</Tag>;
      },
    },
    {
      title: '标签', dataIndex: 'tag', width: 160,
      render: (tags) => {
        if (!tags?.length) return '-';
        return tags.slice(0, 3).map((t) => <Tag key={t} color="blue" style={{ fontSize: 11 }}>{t}</Tag>);
      },
    },
    {
      title: '操作', width: 120, fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button size="small" type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)} okText="删除" cancelText="取消">
            <Button size="small" type="link" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* ---- 页面标题 ---- */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>题目管理</Title>
        <Space>
          <Button icon={<ImportOutlined />} onClick={() => setImportOpen(true)}>Excel 导入</Button>
          <Button icon={<ExportOutlined />} onClick={handleExport}>导出</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新建题目</Button>
        </Space>
      </div>

      {/* ---- 筛选栏 ---- */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 12, flexWrap: 'wrap', alignItems: 'center' }}>
        <Input
          prefix={<SearchOutlined />}
          placeholder="搜索题目名称"
          value={keyword}
          onChange={(e) => { setKeyword(e.target.value); setPage(1); }}
          style={{ width: 240 }}
          allowClear
        />
        <Select
          value={filterType}
          onChange={(v) => { setFilterType(v); setPage(1); }}
          placeholder="按题型筛选"
          allowClear
          style={{ width: 140 }}
          options={[
            ...QUESTION_TYPES,
            { label: '判断题', value: 'Judge' },
          ]}
        />
        <Select
          value={filterRepoId}
          onChange={(v) => { setFilterRepoId(v); setPage(1); }}
          placeholder="按题库筛选"
          allowClear
          style={{ width: 180 }}
          options={repos.map((r) => ({ label: r.name, value: r.id }))}
        />
        <Button icon={<ReloadOutlined />} onClick={() => { setKeyword(''); setFilterType(undefined); setFilterRepoId(undefined); setPage(1); }}>
          重置
        </Button>

        {/* 批量操作 */}
        {selectedRowKeys.length > 0 && (
          <Popconfirm
            title={`确定删除选中的 ${selectedRowKeys.length} 道题目？`}
            onConfirm={handleBatchDelete}
            okText="删除" cancelText="取消"
          >
            <Button danger icon={<DeleteOutlined />}>
              批量删除 ({selectedRowKeys.length})
            </Button>
          </Popconfirm>
        )}
      </div>

      {/* ---- 数据表格 ---- */}
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        size="small"
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        pagination={{
          current: page,
          total,
          pageSize,
          showTotal: (t) => `共 ${t} 道题目`,
          showSizeChanger: false,
          onChange: (p) => setPage(p),
        }}
        scroll={{ x: 900, y: 'calc(100vh - 420px)' }}
      />

      {/* ---- 新建/编辑弹窗 ---- */}
      <QuestionEditModal
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onSave={handleSave}
        record={editRecord}
        repos={allReposCache}
      />

      {/* ---- 导入弹窗 ---- */}
      <ImportModal
        open={importOpen}
        onCancel={() => setImportOpen(false)}
        onSuccess={handleImportSuccess}
        repos={repos}
      />
    </div>
  );
}
