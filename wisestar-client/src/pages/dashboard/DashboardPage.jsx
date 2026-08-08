/**
 * DashboardPage.jsx - 仪表盘页面
 *
 * 功能:
 *   显示用户的概览统计数据，包括:
 *   - 问卷数量（用户创建的调查问卷总数）
 *   - 考试数量（用户创建的考试总数）
 *   - 团队成员数
 *   - 今日答卷数
 *
 * 数据来源: GET /api/userOverview 接口（api/user.js getUserOverview）
 * 被谁引用: App.jsx（受保护路由 /，作为登录后的首页）
 *
 * 数据流:
 *   DashboardPage 挂载 → useEffect → getUserOverview() → GET /api/userOverview
 *   → 返回 { surveyCount, examCount, userCount, todayAnswerCount } → 渲染 4 个统计卡片
 *
 * 组件布局:
 *   欢迎语 + 4 个统计卡片（响应式网格：大屏 4 列，平板 2 列，手机 1 列）
 */

import { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, Typography } from 'antd';
import {
  ProjectOutlined,
  FileTextOutlined,
  TeamOutlined,
  RiseOutlined,
} from '@ant-design/icons';
import { getUserOverview } from '../../api/user';
import useUserStore from '../../stores/useUserStore';

const { Title } = Typography;

export default function DashboardPage() {
  // 当前登录用户
  const { user } = useUserStore();

  // 概览数据 { surveyCount, examCount, userCount, todayAnswerCount }
  const [overview, setOverview] = useState({});

  // 组件挂载时加载概览数据
  useEffect(() => {
    getUserOverview()
      .then((res) => setOverview(res.data))
      .catch(() => {}); // 错误已在拦截器处理，这里忽略
  }, []);

  return (
    <div>
      {/* ---- 欢迎语 ---- */}
      <Title level={4} style={{ marginBottom: 24 }}>
        欢迎回来，{user?.name || user?.username}
      </Title>

      {/* ---- 统计卡片 ---- */}
      {/* Row gutter={[水平间距, 垂直间距]} */}
      {/* Col: xs(手机), sm(平板), lg(桌面) 响应式列宽 */}
      <Row gutter={[24, 24]}>
        {/* 问卷数量 */}
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="问卷数量"
              value={overview.surveyCount || 0}
              prefix={<ProjectOutlined />}
            />
          </Card>
        </Col>

        {/* 考试数量 */}
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="考试数量"
              value={overview.examCount || 0}
              prefix={<FileTextOutlined />}
            />
          </Card>
        </Col>

        {/* 团队成员 */}
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="团队成员"
              value={overview.userCount || 0}
              prefix={<TeamOutlined />}
            />
          </Card>
        </Col>

        {/* 今日答卷 */}
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="今日答卷"
              value={overview.todayAnswerCount || 0}
              prefix={<RiseOutlined />}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
