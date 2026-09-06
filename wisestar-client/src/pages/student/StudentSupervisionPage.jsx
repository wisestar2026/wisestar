/**
 * StudentSupervisionPage.jsx - 学员督学页（学管师/老师/管理员查看学员实时学习状态）
 *
 * 功能:
 *   1. 实时展示「在线」学员（最近 5 分钟内活跃），自动 10 秒轮询
 *   2. 展示当前研习位置：章节 / 小节 归属链（学员目录选中上报 / 知识点页上下文）
 *   3. 学员在做题（练习/试炼/预习例题）时，直接行内展示题干、标准答案与解析
 *
 * URL: /student/supervision 与 /students/activity（均受 AuthGuard student:supervision 保护）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「学员管理 → 学员督学」菜单
 *
 * 数据流:
 *   listSupervision() → GET /student/supervision/online-students（后端只返回在线学员，
 *     并已回填章节/小节名称与题目答案/解析字段）
 */

import { useEffect, useState, useCallback, useRef } from 'react';
import { Table, Tag, Button, Typography, Space, message, Badge } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { listSupervision } from '../../api/student';

const { Title, Text, Paragraph } = Typography;

// 题目类型 → 中文
const TYPE_LABELS = { Radio: '单选', Checkbox: '多选', FillBlank: '填空', Judge: '判断', Text: '简答', MultipleBlank: '多项填空' };

// 页面标识 → 场景描述（做题中时细分练习/试炼）
const pageScene = (page) => {
  if (!page) return '';
  if (page.includes('tab=practice')) return '专项练习';
  if (page.includes('tab=trial')) return '小节通关';
  if (page.includes('tab=preview')) return '知识点预习';
  if (page.includes('tab=lecture') || page.includes('tab=example')) return '知识点学习';
  if (page.startsWith('/student/study')) return '学海研习';
  return '';
};

const formatTime = (v) => {
  if (!v) return '-';
  const t = Number(v);
  return t ? new Date(t).toLocaleString('zh-CN', { hour12: false }) : '-';
};

export default function StudentSupervisionPage() {
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [lastRefresh, setLastRefresh] = useState(null);
  const timerRef = useRef(null);

  const loadList = useCallback((silent = false) => {
    if (!silent) setLoading(true);
    listSupervision()
      .then((res) => {
        setList(res?.data || []);
        setLastRefresh(new Date());
      })
      .catch(() => {
        if (!silent) message.error('在线学员加载失败');
        setList([]);
      })
      .finally(() => {
        if (!silent) setLoading(false);
      });
  }, []);

  useEffect(() => {
    loadList();
    timerRef.current = setInterval(() => loadList(true), 10000);
    return () => clearInterval(timerRef.current);
  }, [loadList]);

  const exercisingCount = list.filter((r) => r.status === 'exercising').length;

  const columns = [
    {
      title: '学员',
      key: 'student',
      width: 150,
      render: (_, r) => (
        <div>
          <div style={{ fontWeight: 500 }}>{r.studentName || '-'}</div>
          <Text type="secondary" style={{ fontSize: 12 }}>{r.studentNo || ''}</Text>
        </div>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 130,
      render: (status, r) => (
        <Space direction="vertical" size={2}>
          {status === 'exercising' ? (
            <Badge color="purple" text="做题中" />
          ) : (
            <Badge color="green" text="学习中" />
          )}
          {pageScene(r.page) && <Text type="secondary" style={{ fontSize: 12 }}>{pageScene(r.page)}</Text>}
        </Space>
      ),
    },
    {
      title: '研习位置',
      key: 'location',
      width: 220,
      render: (_, r) => {
        if (!r.chapterName && !r.sectionName) {
          return <Text type="secondary">{r.currentLocation || '浏览中'}</Text>;
        }
        return (
          <Space size={4} wrap>
            {r.chapterName && <Tag color="blue">{r.chapterName}</Tag>}
            {r.sectionName && <Tag color="cyan">{r.sectionName}</Tag>}
          </Space>
        );
      },
    },
    {
      title: '正在做的题目（含答案/解析）',
      key: 'question',
      render: (_, r) => {
        if (!r.questionContent) {
          return <Text type="secondary">-</Text>;
        }
        return (
          <div>
            <Space size={6} style={{ marginBottom: 6 }}>
              <Tag color="geekblue">{TYPE_LABELS[r.questionType] || r.questionType || '题目'}</Tag>
              <Paragraph strong style={{ margin: 0, flex: 1 }} ellipsis={{ rows: 2 }}>
                {r.questionContent}
              </Paragraph>
            </Space>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <div style={{ padding: '4px 10px', background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 6 }}>
                <Text strong style={{ color: '#389e0d' }}>标准答案：</Text>
                <Text style={{ color: '#389e0d' }}>{r.correctAnswer || '（未配置）'}</Text>
              </div>
              {r.answerAnalysis && (
                <div style={{ padding: '4px 10px', background: '#fffbe6', border: '1px solid #ffe58f', borderRadius: 6 }}>
                  <Text strong style={{ color: '#d48806' }}>解析：</Text>
                  <Text style={{ color: '#874d00' }} ellipsis={{ rows: 2 }}>{r.answerAnalysis}</Text>
                </div>
              )}
            </div>
          </div>
        );
      },
    },
    { title: '最后活跃', dataIndex: 'lastActiveTime', width: 165, render: formatTime },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>
          学员督学
          <Text type="secondary" style={{ fontSize: 13, marginLeft: 12 }}>
            仅显示最近 5 分钟在线学员 · 每 10 秒自动刷新 · 共 {list.length} 人
            {exercisingCount > 0 && `，其中 ${exercisingCount} 人做题中`}
          </Text>
          {lastRefresh && (
            <Text type="secondary" style={{ fontSize: 12, marginLeft: 8 }}>· {formatTime(lastRefresh.getTime())} 刷新</Text>
          )}
        </Title>
        <Button icon={<ReloadOutlined />} onClick={() => loadList()} loading={loading}>
          立即刷新
        </Button>
      </div>

      <Table
        rowKey="studentId"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={false}
        locale={{ emptyText: '暂无在线学员（学员登录并浏览后实时展示）' }}
      />
    </div>
  );
}
