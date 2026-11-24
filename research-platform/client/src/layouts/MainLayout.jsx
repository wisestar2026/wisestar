import { Layout, Menu, Space, Tag, Typography, Button } from 'antd'
import { FileTextOutlined, LogoutOutlined, ReadOutlined } from '@ant-design/icons'
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/auth'

const { Header, Content } = Layout

export default function MainLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)

  if (!localStorage.getItem('research-token')) {
    return <Navigate to="/login" replace />
  }

  const roleName = { ADMIN: '系统管理员', SUPERVISOR: '教学主管', TEACHER: '任课老师' }[user?.role] || '教研用户'

  return (
    <Layout className="app-shell">
      <Header className="app-header">
        <Space size={12}>
          <ReadOutlined className="app-logo" />
          <Typography.Title level={4} className="app-title">
            智慧星教研平台
          </Typography.Title>
        </Space>
        <Menu
          mode="horizontal"
          selectedKeys={[location.pathname.startsWith('/plans') ? '/plans' : '/plans']}
          className="app-menu"
          items={[{ key: '/plans', icon: <FileTextOutlined />, label: '班课方案' }]}
          onClick={({ key }) => navigate(key)}
        />
        <Space size={8}>
          <Typography.Text>{user?.name}</Typography.Text>
          <Tag color="blue">{roleName}</Tag>
          <Button
            type="text"
            icon={<LogoutOutlined />}
            onClick={async () => {
              await logout()
              navigate('/login', { replace: true })
            }}
          >
            退出
          </Button>
        </Space>
      </Header>
      <Content className="app-content">
        <Outlet />
      </Content>
    </Layout>
  )
}
