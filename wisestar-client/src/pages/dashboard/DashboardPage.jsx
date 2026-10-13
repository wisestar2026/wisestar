/**
 * DashboardPage.jsx - 仪表盘页面
 *
 * 功能:
 *   显示管理端概览统计数据，当前包含:
 *   - 学员数（t_student 状态正常的学员总数）
 *   - 科次（按学科分别统计的学员数及其合计）
 *   - AI 接口未配置提醒（功能提醒小板块，仅未配置时显示）
 *
 * 数据来源:
 *   - GET /api/userOverview 接口（api/user.js getUserOverview）
 *   - GET /api/system/aiStatus 接口（api/system.js getAiStatus）
 * 被谁引用: App.jsx（受保护路由 /，作为登录后的首页）
 *
 * 数据流:
 *   DashboardPage 挂载 → useEffect → getUserOverview() → GET /api/userOverview
 *   → 返回 { studentCount, courseCount, subjectStudentCounts } → 渲染统计卡片
 *   同时 → getAiStatus() → GET /api/system/aiStatus
 *   → 返回 { enabled, hasToken, configured, baseUrl, model } → 未配置时显示提醒板块
 *
 * 组件布局:
 *   AI 提醒板块（可选）+ 欢迎语 + 统计卡片（响应式网格：大屏 3 列，平板 2 列，手机 1 列）
 */

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Row, Col, Statistic, Typography, Space, Tag, Alert, Button } from 'antd';
import { TeamOutlined, RiseOutlined } from '@ant-design/icons';
import { getUserOverview } from '../../api/user';
import { getAiStatus } from '../../api/system';
import useUserStore from '../../stores/useUserStore';

const { Title } = Typography;

export default function DashboardPage() {
  // 当前登录用户
  const { user } = useUserStore();

  const navigate = useNavigate();

  // 概览数据 { studentCount, courseCount, subjectStudentCounts }
  const [overview, setOverview] = useState({});

  // AI 配置状态 { enabled, hasToken, configured, baseUrl, model }；null 表示未取到
  const [aiStatus, setAiStatus] = useState(null);

  // 组件挂载时加载概览数据
  useEffect(() => {
    getUserOverview()
      .then((res) => setOverview(res.data))
      .catch(() => {}); // 错误已在拦截器处理，这里忽略
  }, []);

  // 组件挂载时加载 AI 配置状态（用于功能提醒）
  useEffect(() => {
    getAiStatus()
      .then((res) => setAiStatus(res.data))
      .catch(() => {}); // 无权限/未取到时静默忽略，不显示提醒
  }, []);

  return (
    <div>
      {/* ---- AI 接口未配置提醒（功能提醒小板块） ---- */}
      {aiStatus && !aiStatus.configured && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 24 }}
          title="AI 接口未配置"
          description="尚未启用 AI 或未填写 API Token，AI 问答、英语内容生成、学习轨迹 AI 叙述等功能暂不可用。请前往「系统管理 → AI 服务设置」完成配置。"
          action={
            <Button size="small" type="primary" onClick={() => navigate('/system/ai')}>
              去配置
            </Button>
          }
        />
      )}

      {/* ---- 欢迎语 ---- */}
      <Title level={4} style={{ marginBottom: 24 }}>
        欢迎回来，{user?.name || user?.username}
      </Title>

      {/* ---- 统计卡片 ---- */}
      <Row gutter={[24, 24]}>
        {/* 学员数 */}
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="学员数"
              value={overview.studentCount || 0}
              prefix={<TeamOutlined />}
            />
          </Card>
        </Col>

        {/* 科次（按学科分别统计学员数） */}
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="科次"
              value={overview.courseCount || 0}
              prefix={<RiseOutlined />}
            />
            <Space size={[8, 8]} wrap style={{ marginTop: 12 }}>
              {(overview.subjectStudentCounts || []).map((item) => (
                <Tag key={item.subjectId}>
                  {item.subjectName} {item.studentCount || 0}
                </Tag>
              ))}
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
