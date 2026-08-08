/**
 * SystemPage.jsx - 系统管理占位页
 *
 * 功能:
 *   1. 作为侧边栏"系统管理"菜单（/system）的路由落点
 *   2. 当前系统管理模块尚未开发，本页仅展示"建设中"提示，避免路由 404
 *
 * 被谁引用: App.jsx（受保护路由 /system，需登录）
 *
 * 后续规划:
 *   - 系统管理模块包含：用户管理、角色管理、部门管理、岗位管理、
 *     字典管理、系统设置等（后端 SystemApi 已提供全部接口，
 *     前端页面待开发时替换本占位页）
 */

import { Card, Typography } from 'antd';
import { ToolOutlined } from '@ant-design/icons';

const { Title, Paragraph } = Typography;

export default function SystemPage() {
  return (
    <Card>
      <div style={{ textAlign: 'center', padding: '60px 0' }}>
        <ToolOutlined style={{ fontSize: 48, color: '#999' }} />
        <Title level={3} style={{ marginTop: 16 }}>系统管理建设中</Title>
        <Paragraph type="secondary">
          用户、角色、部门、岗位、字典等管理功能正在开发中，
          敬请期待。
        </Paragraph>
      </div>
    </Card>
  );
}
