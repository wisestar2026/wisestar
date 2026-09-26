/**
 * DashboardPage.jsx - 仪表盘页面
 *
 * 功能:
 *   显示管理端概览统计数据，当前包含:
 *   - 学员数（t_student 状态正常的学员总数）
 *   - 科次（按学科分别统计的学员数及其合计）
 *
 * 数据来源: GET /api/userOverview 接口（api/user.js getUserOverview）
 * 被谁引用: App.jsx（受保护路由 /，作为登录后的首页）
 *
 * 数据流:
 *   DashboardPage 挂载 → useEffect → getUserOverview() → GET /api/userOverview
 *   → 返回 { studentCount, courseCount, subjectStudentCounts } → 渲染统计卡片
 *
 * 组件布局:
 *   欢迎语 + 统计卡片（响应式网格：大屏 3 列，平板 2 列，手机 1 列）
 */

import { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, Typography, Space, Tag } from 'antd';
import { TeamOutlined, RiseOutlined } from '@ant-design/icons';
import { getUserOverview } from '../../api/user';
import useUserStore from '../../stores/useUserStore';

const { Title } = Typography;

export default function DashboardPage() {
  // 当前登录用户
  const { user } = useUserStore();

  // 概览数据 { studentCount, courseCount, subjectStudentCounts }
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
