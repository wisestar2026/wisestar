/**
 * ProjectAnswersPage.jsx - 答卷管理页面
 *
 * 功能:
 *   1. 列表展示指定问卷的所有答卷（分页）
 *   2. 查看单条答卷详情（弹窗展示）
 *   3. 删除答卷（移入回收站）
 *
 * URL: /projects/:id/answers
 *
 * 后端接口:
 *   GET  /api/answer/list?projectId=xxx  答卷列表
 *   GET  /api/answer?id=xxx              答卷详情
 *   POST /api/answer/delete              删除答卷
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Table, Space, Button, Modal, Popconfirm, message, Typography, Spin } from 'antd';
import {
  ArrowLeftOutlined, DeleteOutlined, EyeOutlined, BarChartOutlined,
} from '@ant-design/icons';
import { listAnswers, getAnswer, deleteAnswer } from '../../../api/answer';
import { getProject } from '../../../api/project';

const { Text, Title } = Typography;

export default function ProjectAnswersPage() {
  const { id: projectId } = useParams();
  const navigate = useNavigate();

  // ---- 状态 ----
  const [loading, setLoading] = useState(false);
  const [project, setProject] = useState(null);
  const [answers, setAnswers] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const pageSize = 15;

  // 详情弹窗
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [answerDetail, setAnswerDetail] = useState(null);

  // ---- 加载项目信息 ----
  useEffect(() => {
    (async () => {
      try {
        const res = await getProject(projectId);
      setProject(res.data);
      } catch { /* ignore */ }
    })();
  }, [projectId]);

  // ---- 加载答卷列表 ----
  const fetchAnswers = async (p = page) => {
    setLoading(true);
    try {
      const res = await listAnswers({
        projectId,
        current: p,
        pageSize,
      });
      setAnswers(res.data?.list || []);
      setTotal(res.data?.total || 0);
    } catch {
      message.error('加载答卷列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnswers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId]);

  // ---- 查看详情 ----
  const handleViewDetail = async (answerId) => {
    setDetailVisible(true);
    setDetailLoading(true);
    try {
      const res = await getAnswer(answerId);
      setAnswerDetail(res.data);
    } catch {
      message.error('加载答卷详情失败');
    } finally {
      setDetailLoading(false);
    }
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
      title: '提交时间',
      dataIndex: 'createAt',
      width: 180,
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
      title: '操作',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          {/* 查看详情 */}
          <Button
            size="small"
            type="link"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record.id)}
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

  // ---- 渲染 ----
  return (
    <div style={{ padding: '0 0 24px' }}>
      {/* ---- 顶部 ---- */}
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(`/projects/${projectId}/edit`)}>
          返回编辑
        </Button>
        <Title level={4} style={{ margin: 0 }}>
          {project ? `答卷列表 — ${project.name}` : '答卷列表'}
        </Title>
      </Space>

      {/* ---- 表格 ---- */}
      <Table
        columns={columns}
        dataSource={answers}
        rowKey="id"
        loading={loading}
        size="small"
        pagination={{
          current: page,
          total,
          pageSize,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => {
            setPage(p);
            fetchAnswers(p);
          },
        }}
        scroll={{ y: 'calc(100vh - 260px)' }}
      />

      {/* ---- 详情弹窗 ---- */}
      <Modal
        title="答卷详情"
        open={detailVisible}
        onCancel={() => {
          setDetailVisible(false);
          setAnswerDetail(null);
        }}
        footer={null}
        width={600}
      >
        {detailLoading ? (
          <div style={{ textAlign: 'center', padding: 40 }}>
            <Spin tip="加载中..." />
          </div>
        ) : answerDetail ? (
          <div style={{ maxHeight: 500, overflow: 'auto' }}>
            <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
              提交时间：{answerDetail.createAt ? new Date(answerDetail.createAt).toLocaleString('zh-CN') : '-'}
            </Text>

            {/* 答卷内容 */}
            {answerDetail.answer && Object.keys(answerDetail.answer).length > 0 ? (
              Object.entries(answerDetail.answer).map(([qid, val]) => {
                // val 的结构是 { optionId: value }，也可能直接是 value
                let display = '-';
                if (typeof val === 'object' && val !== null) {
                  const entries = Object.entries(val);
                  display = entries.map(([k, v]) => v != null ? `${v}` : k).join('，');
                } else if (val != null) {
                  display = String(val);
                }

                return (
                  <div key={qid} style={{ marginBottom: 12, padding: '8px 12px', background: '#fafafa', borderRadius: 6 }}>
                    <Text strong style={{ fontSize: 13 }}>问题 {qid}</Text>
                    <div style={{ marginTop: 4 }}>
                      <Text>{display}</Text>
                    </div>
                  </div>
                );
              })
            ) : (
              <Text type="secondary">无答卷内容</Text>
            )}
          </div>
        ) : null}
      </Modal>
    </div>
  );
}
