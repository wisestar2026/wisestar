/**
 * StudentActivityPage.jsx - 学员动态监控页（后台老师查看学员实时位置）
 *
 * 功能:
 *   1. 实时展示各学员当前所在页面（首页/学习/知识点/错题本/个人中心等）与正在做的习题
 *   2. 自动轮询刷新（10 秒），查看学员最后活跃时间
 *   3. 行操作「查看题目」：弹窗展示该习题的题干/选项/标准答案/解析（老师视角）
 *
 * URL: /students/activity（受 AuthGuard 保护，student:list 权限）
 * 被谁引用: App.jsx 路由；MainLayout 侧边栏「学员管理 → 学员动态」菜单
 *
 * 数据流:
 *   listActivities() → 学员实时位置列表（含 questionId/questionTitle）
 *   查看题目: getTemplate({id}) → 题目详情（含答案/解析）→ Modal 展示
 */

import { useEffect, useState, useCallback, useRef } from 'react';
import { Table, Tag, Button, Modal, Typography, Space, message } from 'antd';
import { EyeOutlined, ReloadOutlined } from '@ant-design/icons';
import { listActivities } from '../../api/student';
import { getTemplate } from '../../api/template';

const { Title, Paragraph, Text } = Typography;

// 页面标识 → 中文
const PAGE_LABELS = {
  '/student': '首页',
  '/student/study': '学习',
  '/student/profile': '个人中心',
  '/student/wrong': '错题本',
  '/student/mall': '商城',
};
const pageLabel = (page) => {
  if (!page) return '-';
  if (PAGE_LABELS[page]) return PAGE_LABELS[page];
  if (page.startsWith('/student/knowledge')) {
    return page.includes('tab=practice') ? '练习中' : page.includes('tab=trial') ? '试炼中' : '知识点学习';
  }
  return page;
};

const TYPE_LABELS = { Radio: '单选', Checkbox: '多选', FillBlank: '填空', Judge: '判断', Text: '简答' };
const formatTime = (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '-');

export default function StudentActivityPage() {
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [lastRefresh, setLastRefresh] = useState(null);

  // 题目查看弹窗
  const [question, setQuestion] = useState(null);
  const [qOpen, setQOpen] = useState(false);
  const [qLoading, setQLoading] = useState(false);

  const timerRef = useRef(null);

  const loadList = useCallback(() => {
    setLoading(true);
    listActivities()
      .then((res) => {
        setList(res?.data || []);
        setLastRefresh(new Date());
      })
      .catch(() => setList([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadList();
    // 每 10 秒自动轮询刷新（实时监控）
    timerRef.current = setInterval(loadList, 10000);
    return () => clearInterval(timerRef.current);
  }, [loadList]);

  // 查看题目（题干/选项/答案/解析）
  const openQuestion = (record) => {
    if (!record.questionId) {
      message.info('该学员当前不在习题中');
      return;
    }
    setQOpen(true);
    setQLoading(true);
    setQuestion(null);
    getTemplate({ id: record.questionId })
      .then((res) => setQuestion(res?.data || null))
      .catch(() => message.error('题目加载失败'))
      .finally(() => setQLoading(false));
  };

  const schema = question?.template || {};
  const attr = schema.attribute || {};
  const children = schema.children || [];

  const columns = [
    { title: '学员', dataIndex: 'studentName', width: 100, render: (v) => v || '-' },
    { title: '学号', dataIndex: 'studentNo', width: 110, render: (v) => v || '-' },
    {
      title: '当前页面', dataIndex: 'page', width: 120,
      render: (v) => <Tag color="blue">{pageLabel(v)}</Tag>,
    },
    {
      title: '正在做的习题', dataIndex: 'questionTitle', width: 240,
      render: (v, r) => (r.questionId ? v || r.questionId.slice(0, 8) : <Text type="secondary">-</Text>),
    },
    { title: '最后活跃', dataIndex: 'updateAt', width: 170, render: formatTime },
    {
      title: '操作', key: 'action', width: 110,
      render: (_, record) => (
        <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => openQuestion(record)}>
          查看题目
        </Button>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>
          学员动态（实时位置）
          {lastRefresh && <Text type="secondary" style={{ fontSize: 13, marginLeft: 12 }}>每 10 秒自动刷新 · {formatTime(lastRefresh)}</Text>}
        </Title>
        <Button icon={<ReloadOutlined />} onClick={loadList} loading={loading}>立即刷新</Button>
      </div>

      <Table
        rowKey="studentId"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={false}
        locale={{ emptyText: '暂无学员在线数据，学员登录并浏览后实时展示' }}
      />

      {/* ---- 题目详情弹窗（老师查看答案/解析） ---- */}
      <Modal
        title={`题目详情 · ${question?.name || ''}`}
        open={qOpen}
        onCancel={() => setQOpen(false)}
        footer={null}
        width={640}
      >
        {qLoading ? <div style={{ textAlign: 'center', padding: 24 }}>加载中…</div> : question ? (
          <div>
            <Space style={{ marginBottom: 8 }}>
              <Tag color="blue">{TYPE_LABELS[question.questionType] || question.questionType}</Tag>
              {attr.examScore && <Tag>分值 {attr.examScore}</Tag>}
            </Space>
            <Paragraph style={{ fontWeight: 600, fontSize: 15 }}>{question.name}</Paragraph>
            {children.map((opt) => (
              <div key={opt.id} style={{ padding: '6px 10px', marginBottom: 6, background: '#f5f9fc', borderRadius: 8 }}>
                {opt.title || opt.id}
              </div>
            ))}
            {attr.examCorrectAnswer && (
              <div style={{ marginTop: 12, padding: 10, background: '#e8f5e9', borderRadius: 8 }}>
                <Text strong style={{ color: '#2e7d32' }}>✅ 标准答案：</Text>
                <Text style={{ color: '#2e7d32' }}>{attr.examCorrectAnswer}</Text>
              </div>
            )}
            {attr.examAnalysis && (
              <div style={{ marginTop: 8, padding: 10, background: '#fffbe6', borderRadius: 8 }}>
                <Text strong style={{ color: '#b26a00' }}>📝 解析：</Text>
                <Paragraph style={{ marginTop: 4, marginBottom: 0, whiteSpace: 'pre-wrap' }}>{attr.examAnalysis}</Paragraph>
              </div>
            )}
            {!attr.examCorrectAnswer && !attr.examAnalysis && (
              <Text type="secondary">该题目未配置答案/解析</Text>
            )}
          </div>
        ) : null}
      </Modal>
    </div>
  );
}
