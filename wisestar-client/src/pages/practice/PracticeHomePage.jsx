/**
 * PracticeHomePage.jsx - 在线练习首页 / 选题页（学员端核心入口）
 *
 * 功能:
 *   1. 三种练习模式选择（专项刷题 / 套卷模拟 / 随机练习）
 *   2. 「我的练习」列表（老师手动分配 ∪ 系统按标签自动匹配）
 *   3. 选择练习/测试卷 → 「开始练习」→ 该练习全部题目进入答题页
 *
 * 【学员端限制】学员只能选择分配到的习练习/测试卷，无法对单个题目勾选。
 *   练习来源: 老师手动分配（管理端「练习分配」页）+ 系统按标签自动分配
 *
 * URL: /practice（受 AuthGuard 保护，侧边栏「在线练习」菜单进入）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏"在线练习"菜单
 *
 * 数据流:
 *   挂载 → myRepos() 拉取我的练习（GET /api/repo/my）
 *   开始练习 → 选择练习 + 模式 → listTemplate({repoId, pageSize: total}) 拉取全部题目 ids
 *     → navigate(`/practice/session?mode=xx&ids=xx`) → PracticeSessionPage
 *
 * 三种模式说明:
 *   - 专项刷题: 一题一屏，两步作答（确认提交后判题），侧重日常巩固
 *   - 套卷模拟: 卷首确认 + 整卷作答 + 全局倒计时 + 到点自动交卷
 *   - 随机练习: 题目随机排序，两步作答即时判题
 */

import { useState, useEffect } from 'react';
import {
  Card, Row, Col, Button, Tag, Typography, Space, message, Empty,
} from 'antd';
import {
  PlayCircleOutlined, ThunderboltOutlined, ExperimentOutlined, FireOutlined,
  BookOutlined, FileTextOutlined, TagsOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { listTemplate } from '../../api/template';
import { myRepos } from '../../api/repo';

const { Title, Text, Paragraph } = Typography;

// 三种练习模式的配置（卡片展示用）
const MODES = [
  {
    key: 'practice',
    title: '专项刷题',
    desc: '按知识点逐题巩固，确认提交后即时判分',
    icon: <ThunderboltOutlined />,
    color: '#1677ff',
  },
  {
    key: 'exam',
    title: '套卷模拟',
    desc: '整卷作答，全局倒计时，模拟真实考试',
    icon: <ExperimentOutlined />,
    color: '#722ed1',
  },
  {
    key: 'random',
    title: '随机练习',
    desc: '题目随机排序，即时判对错，碎片化热身',
    icon: <FireOutlined />,
    color: '#13c2c2',
  },
];

export default function PracticeHomePage() {
  const navigate = useNavigate();

  // ---- 练习模式 ----
  const [mode, setMode] = useState('practice');

  // ---- 我的练习状态 ----
  const [loading, setLoading] = useState(true);
  const [repos, setRepos] = useState([]);        // 我的练习列表（RepoView）
  const [selectedRepoId, setSelectedRepoId] = useState(null); // 选中的练习 id
  const [starting, setStarting] = useState(false); // 开始练习请求中

  // ---- 加载我的练习 ----
  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const res = await myRepos();
        setRepos(res.data || []);
      } catch {
        message.error('加载我的练习失败');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  // ---- 开始练习 ----
  // 学员端只能选择练习：拉取该练习全部题目 id 后进入答题页
  const handleStart = async () => {
    if (!selectedRepoId) {
      message.warning('请先选择一个习练习或测试卷');
      return;
    }
    const repo = repos.find((r) => r.id === selectedRepoId);
    setStarting(true);
    try {
      const total = repo?.total || 0;
      const res = await listTemplate({ repoId: selectedRepoId, current: 1, pageSize: Math.max(total, 100) });
      const list = res.data?.list || [];
      if (list.length === 0) {
        message.warning('该练习暂无题目，请联系老师补充');
        return;
      }
      const ids = list.map((q) => q.id).join(',');
      navigate(`/practice/session?mode=${mode}&ids=${ids}&repoId=${selectedRepoId || ''}`);
    } catch {
      message.error('加载题目失败');
    } finally {
      setStarting(false);
    }
  };

  // ---- 练习类型标识 ----
  const repoType = (repo) => (
    repo?.mode === 'exam'
      ? { label: '测试卷', color: 'purple', icon: <FileTextOutlined /> }
      : { label: '习练习', color: 'blue', icon: <BookOutlined /> }
  );

  return (
    <div style={{ padding: 24 }}>
      <Card style={{ marginBottom: 16 }}>
        <Title level={4} style={{ marginBottom: 4 }}>在线练习</Title>
        <Text type="secondary">选择练习模式与老师分配的习练习/测试卷开始学习</Text>
      </Card>

      {/* ---- 三种练习模式选择 ---- */}
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          {MODES.map((m) => (
            <Col key={m.key} xs={24} sm={8}>
              <Card
                hoverable
                onClick={() => setMode(m.key)}
                style={{
                  border: mode === m.key ? `2px solid ${m.color}` : '1px solid #f0f0f0',
                  marginBottom: 12,
                }}
                styles={{ body: { padding: 16 } }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <span style={{ fontSize: 28, color: m.color }}>{m.icon}</span>
                  <div>
                    <Text strong style={{ fontSize: 16 }}>{m.title}</Text>
                    <br />
                    <Text type="secondary" style={{ fontSize: 12 }}>{m.desc}</Text>
                  </div>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      </Card>

      {/* ---- 我的练习列表 ---- */}
      <Card
        title={
          <Space>
            <span>我的练习 / 测试卷</span>
            <Text type="secondary" style={{ fontSize: 12 }}>
              （共 {repos.length} 个，由老师分配或系统按标签自动分配）
            </Text>
          </Space>
        }
      >
        {loading ? (
          <div style={{ padding: 48, textAlign: 'center' }}><Text type="secondary">练习加载中...</Text></div>
        ) : repos.length === 0 ? (
          <Empty description="暂无分配的练习，请联系老师为您分配习练习或测试卷" />
        ) : (
          <Row gutter={[16, 16]}>
            {repos.map((repo) => {
              const selected = repo.id === selectedRepoId;
              const type = repoType(repo);
              const tags = Array.isArray(repo.tag) ? repo.tag : [];
              return (
                <Col key={repo.id} xs={24} sm={12} lg={8}>
                  <Card
                    hoverable
                    onClick={() => setSelectedRepoId(repo.id)}
                    style={{
                      border: selected ? `2px solid #1677ff` : '1px solid #f0f0f0',
                      background: selected ? '#f0f7ff' : '#fff',
                      transition: 'all .2s',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                      <Text strong style={{ fontSize: 16 }} ellipsis={{ tooltip: repo.name }}>
                        {repo.name}
                      </Text>
                      <Tag color={type.color} style={{ marginRight: 0 }}>{type.icon} {type.label}</Tag>
                    </div>
                    <Paragraph type="secondary" style={{ fontSize: 13, marginBottom: 8, minHeight: 40 }} ellipsis={{ rows: 2 }}>
                      {repo.description || '暂无描述'}
                    </Paragraph>
                    <Space wrap size={4}>
                      <Tag>共 {repo.total ?? 0} 题</Tag>
                      {tags.length > 0 && (
                        <Tag icon={<TagsOutlined />} color="cyan">
                          {tags.join('、')}
                        </Tag>
                      )}
                    </Space>
                  </Card>
                </Col>
              );
            })}
          </Row>
        )}

        {/* ---- 底部操作区 ---- */}
        {repos.length > 0 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 24 }}>
            <Text type="secondary">
              已选
              <Text strong style={{ color: '#1677ff' }}>
                {' '}{repos.find((r) => r.id === selectedRepoId)?.name || '未选择'}{' '}
              </Text>
              ｜当前模式：<Text strong>{MODES.find((m) => m.key === mode)?.title}</Text>
            </Text>
            <Button
              type="primary"
              size="large"
              icon={<PlayCircleOutlined />}
              loading={starting}
              onClick={handleStart}
            >
              开始练习
            </Button>
          </div>
        )}
      </Card>
    </div>
  );
}
