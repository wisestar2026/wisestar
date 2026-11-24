import { Button, Card, Form, Input, Typography, App as AntApp, Alert } from 'antd'
import { LockOutlined, ReadOutlined, UserOutlined } from '@ant-design/icons'
import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/auth'

export default function LoginPage() {
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const login = useAuthStore((s) => s.login)
  const loading = useAuthStore((s) => s.loading)
  const [error, setError] = useState('')

  if (localStorage.getItem('research-token')) {
    return <Navigate to="/plans" replace />
  }

  const onFinish = async (values) => {
    setError('')
    try {
      await login(values)
      message.success('登录成功')
      navigate('/plans', { replace: true })
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div className="login-page">
      <Card className="login-card" variant="borderless">
        <div className="login-brand">
          <ReadOutlined />
          <Typography.Title level={3}>智慧星教研平台</Typography.Title>
          <Typography.Paragraph type="secondary">
            班课方案 · 课次备课 · 真题组卷 · 资源导航
          </Typography.Paragraph>
        </div>
        {error && <Alert type="error" showIcon message={error} className="login-alert" />}
        <Form layout="vertical" onFinish={onFinish} initialValues={{ username: 'admin' }} size="large">
          <Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}>
            <Input prefix={<UserOutlined />} placeholder="请输入账号" autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            登录
          </Button>
        </Form>
        <Typography.Paragraph type="secondary" className="login-tip">
          管理员账号登录不受远端服务影响；老师账号由智慧星系统统一校验。
        </Typography.Paragraph>
      </Card>
    </div>
  )
}
