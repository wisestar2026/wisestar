/**
 * AnswerListPage.jsx - 全局答案管理页面
 *
 * 功能:
 *   1. 分页展示所有问卷的答卷（含问卷名称关联）
 *   2. 按问卷名称搜索过滤
 *   3. 按时间范围筛选
 *   4. 查看答卷详情（跳转详情页）
 *   5. 删除答卷
 *
 * 后端接口:
 *   GET  /api/answer/list?current=1&pageSize=15&projectId=xxx  答卷列表
 *   POST /api/answer/delete                                   删除答卷
 *
 * URL: /answers
 */

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Space, Input, Button, DatePicker, Popconfirm,
  Typography, Tag, message,
} from 'antd';
import {
  SearchOutlined, ReloadOutlined, EyeOutlined, DeleteOutlined,
} from '@ant-design/icons';
import { listAnswers, deleteAnswer } from '../../api/answer';
import { listProject } from '../../api/project';

const { Title } = Typography;
const { RangePicker } = DatePicker;

export default function AnswerListPage() {
  const navigate = useNavigate();

  // ---- 状态 ----
  const [loading, setLoading] = useState(false);
  const [answers, setAnswers] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const pageSize = 15;

  // 搜索条件
  const [searchProjectName, setSearchProjectName] = useState('');
  const [dateRange, setDateRange] = useState(null);

  // 项目名称映射表 { projectId: projectName }
  const [projectMap, setProjectMap] = useState({});

  // ---- 加载项目名称映射 ----
  useEffect(() => {
    (async () => {
      try {
        // 一次性拉取所有项目（不分页，pageSize=-1 表示全部）
        const res = await listProject({ current: 1, pageSize: -1 });
        const list = res.data?.list || [];
        const map = {};
        list.forEach((p) => { map[p.id] = p.name; });
        setProjectMap(map);
      } catch { /* ignore */ }
    })();
  }, []);

  // ---- 加载答卷列表 ----
  const fetchAnswers = async (p = page) => {
    setLoading(true);
    try {
      const params = { current: p, pageSize };
      if (dateRange && dateRange[0] && dateRange[1]) {
        params.startTime = dateRange[0].toISOString();
        params.endTime = dateRange[1].toISOString();
      }
      const res = await listAnswers(params);
      setAnswers(res?.data?.list || []);
      setTotal(res?.data?.total || 0);
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || '加载答卷列表失败';
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnswers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ---- 搜索 ----
  const handleSearch = () => {
    setPage(1);
    fetchAnswers(1);
  };

  // ---- 删除答卷 ----
  const handleDelete = async (answerId) => {
    try {
      await deleteAnswer({ id: answerId });
      message.success('已删除');
      fetchAnswers(page);
    } catch {
      message.error('删除失败');
    }
  };

  // ---- 前端过滤（按问卷名称） ----
  const filteredAnswers = searchProjectName
    ? answers.filter((a) => {
        const name = projectMap[a.projectId] || '';
        return name.includes(searchProjectName);
      })
    : answers;

  // ---- 表格列定义 ----
  const columns = [
    {
      title: '序号',
      width: 60,
      render: (_, __, idx) => (page - 1) * pageSize + idx + 1,
    },
    {
      title: '答卷 ID',
      dataIndex: 'id',
      width: 120,
      ellipsis: true,
    },
    {
      title: '问卷名称',
      dataIndex: 'projectId',
      width: 200,
      ellipsis: true,
      render: (projectId) => (
        <span>{projectMap[projectId] || projectId || '(未知问卷)'}</span>
      ),
    },
    {
      title: '提交时间',
      dataIndex: 'createAt',
      width: 170,
      render: (val) => val ? new Date(val).toLocaleString('zh-CN') : '-',
    },
    {
      title: '答题耗时',
      dataIndex: 'metaInfo',
      width: 100,
      render: (meta) => {
        if (!meta?.answerInfo?.startTime || !meta?.answerInfo?.endTime) return '-';
        const diff = (meta.answerInfo.endTime - meta.answerInfo.startTime) / 1000;
        if (diff < 60) return `${Math.round(diff)}秒`;
        return `${Math.floor(diff / 60)}分${Math.round(diff % 60)}秒`;
      },
    },
    {
      title: 'IP 地址',
      dataIndex: ['metaInfo', 'clientInfo', 'remoteIp'],
      width: 140,
      render: (val) => val || '-',
    },
    {
      title: '状态',
      dataIndex: 'tempSave',
      width: 80,
      render: (val) => (
        <Tag color={val === 1 ? 'green' : 'orange'}>
          {val === 1 ? '已完成' : '暂存'}
        </Tag>
      ),
    },
    {
      title: '操作',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          {/* 查看详情 */}
          <Button
            size="small"
            type="link"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/answers/${record.id}`)}
          >
            详情
          </Button>
          {/* 删除 */}
          <Popconfirm
            title="确定删除此答卷？"
            onConfirm={() => handleDelete(record.id)}
            okText="删除"
            cancelText="取消"
          >
            <Button size="small" type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* ---- 标题 ---- */}
      <Title level={4} style={{ marginBottom: 16 }}>答案管理</Title>

      {/* ---- 搜索/过滤栏 ---- */}
      <Space wrap style={{ marginBottom: 16 }}>
        {/* 问卷名称搜索 */}
        <Input
          placeholder="搜索问卷名称"
          value={searchProjectName}
          onChange={(e) => setSearchProjectName(e.target.value)}
          onPressEnter={handleSearch}
          style={{ width: 220 }}
          prefix={<SearchOutlined />}
        />

        {/* 时间范围选择 */}
        <RangePicker
          placeholder={['开始时间', '结束时间']}
          onChange={(dates) => setDateRange(dates)}
        />

        {/* 搜索按钮 */}
        <Button type="primary" onClick={handleSearch}>搜索</Button>

        {/* 刷新按钮 */}
        <Button icon={<ReloadOutlined />} onClick={() => {
          setSearchProjectName('');
          setDateRange(null);
          setPage(1);
          fetchAnswers(1);
        }}>
          重置
        </Button>
      </Space>

      {/* ---- 表格 ---- */}
      <Table
        columns={columns}
        dataSource={filteredAnswers}
        rowKey="id"
        loading={loading}
        size="small"
        pagination={{
          current: page,
          total: searchProjectName ? filteredAnswers.length : total,
          pageSize,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => {
            setPage(p);
            fetchAnswers(p);
          },
        }}
        scroll={{ y: 'calc(100vh - 320px)' }}
      />
    </div>
  );
}
