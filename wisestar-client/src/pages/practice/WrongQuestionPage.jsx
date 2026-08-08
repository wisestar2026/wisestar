/**
 * WrongQuestionPage.jsx - 错题库管理页
 *
 * 功能:
 *   1. 错题列表（题目 × 学员聚合）：展示题目标题、题型、所属题库、学员、错误次数、最近做错时间、最近答案、最近得分
 *   2. 筛选：题库 / 题型 / 关键词（题目标题或学员姓名）/ 做错时间范围
 *   3. 分页查询
 *
 * URL: /wrong-questions（受 AuthGuard 保护）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「题库管理 → 错题库管理」菜单进入
 *
 * 数据流:
 *   加载: fetchData → listWrongQuestions(params) → GET /api/practice/wrong-list → 渲染表格
 *   筛选: 任一条件变化（setPage(1) 归零）→ 自动重新拉取
 *
 * 数据来源说明:
 *   - 错题 = t_practice_detail.is_correct = 0（学员交卷落库时由后端复核判分写入）
 *   - 聚合口径: 同一学员反复做错同一题合并为一条，wrongCount 累计错误次数
 *   - 题库下拉用 listRepo 全量（供筛选）
 */

import { useState, useEffect, useCallback } from 'react';
import {
  Table, Space, Button, Input, Select, Typography, Tag, message, DatePicker,
} from 'antd';
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { listWrongQuestions } from '../../api/practice';
import { listRepo } from '../../api/repo';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

// 练习场景题型映射（错题库管理仅出现练习题型）
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Judge: '判断题',
  FillBlank: '填空题', Textarea: '多行文本',
};
const TYPE_OPTIONS = Object.entries(TYPE_LABELS).map(([value, label]) => ({ value, label }));

export default function WrongQuestionPage() {
  // ---- 列表状态 ----
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState([]);   // 当前页错题聚合数据
  const [total, setTotal] = useState(0);  // 符合条件的错题总数
  const [page, setPage] = useState(1);    // 当前页码
  const pageSize = 15;                    // 每页 15 条（固定）

  // ---- 筛选状态 ----
  const [repos, setRepos] = useState([]);      // 全量题库列表（供筛选下拉）
  const [filterRepoId, setFilterRepoId] = useState(undefined);
  const [filterType, setFilterType] = useState(undefined);
  const [keyword, setKeyword] = useState('');
  const [timeRange, setTimeRange] = useState(null); // [dayjs, dayjs] | null

  // ---- 加载题库列表（全量，供筛选下拉） ----
  useEffect(() => {
    (async () => {
      try {
        const res = await listRepo({ current: 1, pageSize: 200 });
        setRepos(res.data?.list || []);
      } catch { /* silent */ }
    })();
  }, []);

  // ---- 加载错题列表 ----
  // 所有筛选条件为 AND 关系，组装进 params 调用 GET /api/practice/wrong-list
  const fetchData = useCallback(async (p = page) => {
    setLoading(true);
    try {
      const params = { current: p, pageSize };
      if (filterRepoId) params.repoId = filterRepoId;                       // 题库过滤
      if (filterType) params.questionType = filterType;                     // 题型过滤
      if (keyword.trim()) params.keyword = keyword.trim();                  // 题目标题 / 学员姓名
      if (timeRange && timeRange[0]) params.startTime = timeRange[0].startOf('day').toISOString();
      if (timeRange && timeRange[1]) params.endTime = timeRange[1].endOf('day').toISOString();
      const res = await listWrongQuestions(params);
      setData(res.data?.list || []);
      setTotal(res.data?.total || 0);
    } catch {
      message.error('加载错题列表失败');
    } finally {
      setLoading(false);
    }
  }, [page, filterRepoId, filterType, keyword, timeRange]); // eslint-disable-line react-hooks/exhaustive-deps

  // 任一筛选条件 / 页码变化时自动重新拉取（输入框 onChange 同时 setPage(1) 保证从首页开始）
  useEffect(() => { fetchData(page); }, [page, filterRepoId, filterType, keyword, timeRange, fetchData]);

  // ---- 重置筛选 ----
  const handleReset = () => {
    setFilterRepoId(undefined);
    setFilterType(undefined);
    setKeyword('');
    setTimeRange(null);
    setPage(1);
  };

  // ---- 表格列 ----
  const columns = [
    {
      title: '题目标题', dataIndex: 'questionTitle', width: 260,
      render: (t) => t || <Text type="secondary">（题目已删除）</Text>,
    },
    {
      title: '题型', dataIndex: 'questionType', width: 100,
      render: (t) => <Tag>{TYPE_LABELS[t] || t}</Tag>,
    },
    {
      title: '所属题库', dataIndex: 'repoName', width: 140,
      render: (n) => n ? <Tag color="blue">{n}</Tag> : <Text type="secondary">未归属</Text>,
    },
    {
      title: '学员', dataIndex: 'userName', width: 120,
      render: (n) => n || <Text type="secondary">-</Text>,
    },
    {
      title: '错误次数', dataIndex: 'wrongCount', width: 100, align: 'center',
      render: (c) => c > 1
        ? <Tag color="red" icon={<Text strong style={{ color: 'inherit' }}>×</Text>}>{c} 次</Tag>
        : <Tag color="volcano">1 次</Tag>,
    },
    {
      title: '最近做错时间', dataIndex: 'lastWrongTime', width: 170,
      render: (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: '最近答案', dataIndex: 'lastAnswer', width: 180,
      render: (t) => t || <Text type="secondary">未作答</Text>,
    },
    {
      title: '最近得分', dataIndex: 'lastScore', width: 90, align: 'center',
      render: (s) => (s === null || s === undefined) ? '-' : <Text type="danger">{s} 分</Text>,
    },
  ];

  return (
    <div>
      {/* ---- 页面标题 ---- */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>错题库管理</Title>
      </div>

      {/* ---- 筛选栏 ---- */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          placeholder="题库筛选" allowClear style={{ width: 180 }} value={filterRepoId}
          options={repos.map((r) => ({ value: r.id, label: r.name }))}
          onChange={(v) => { setFilterRepoId(v); setPage(1); }}
        />
        <Select
          placeholder="题型筛选" allowClear style={{ width: 140 }} value={filterType}
          options={TYPE_OPTIONS}
          onChange={(v) => { setFilterType(v); setPage(1); }}
        />
        <Input
          placeholder="题目标题 / 学员姓名" allowClear style={{ width: 220 }}
          value={keyword}
          onChange={(e) => { setKeyword(e.target.value); setPage(1); }}
          prefix={<SearchOutlined />}
        />
        <DatePicker.RangePicker
          style={{ width: 260 }}
          value={timeRange}
          onChange={(v) => { setTimeRange(v); setPage(1); }}
        />
        <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
      </Space>

      {/* ---- 错题表格 ---- */}
      <Table
        rowKey={(r) => `${r.questionId}-${r.userId}`}
        loading={loading}
        columns={columns}
        dataSource={data}
        scroll={{ x: 1100 }}
        pagination={{
          current: page,
          pageSize,
          total,
          showTotal: (t) => `共 ${t} 道错题`,
          onChange: setPage,
        }}
      />
    </div>
  );
}
