/**
 * RegisterPage.jsx - 注册页面
 *
 * 功能:
 *   1. 收集昵称、用户名、密码
 *   2. 调用 api/user.js register() 执行注册（POST /api/public/register）
 *   3. 注册成功后提示并跳转回登录页
 *
 * 页面布局:
 *   - 全屏渐变背景（紫色渐变，与登录页一致）
 *   - 居中白色卡片（400px 宽）
 *   - 表单包含：昵称输入框、用户名输入框、密码输入框、注册按钮、登录链接
 *
 * 被谁引用: App.jsx（公开路由 /register，无需登录）
 *
 * 注册流程:
 *   RegisterPage → api/user.js register({name, username, password})
 *           ↓ 成功（后端创建账号）
 *           message.success → navigate('/login') 回登录页
 *
 * 安全说明:
 *   - 后端 register 接口接收明文密码（与 login 接口的 RSA 加密不同，见
 *     UserApi.register 实现），故此处直接传明文，不调用 jsencrypt
 *   - 是否开放注册由后端 SystemInfo.registerInfo.registerEnabled 控制，
 *     未开放时后端返回业务错误，由 request.js 拦截器统一提示
 */

import { useState } from 'react';
import { Form, Input, Button, Card, Typography, message, Space } from 'antd';
import { UserOutlined, LockOutlined, SmileOutlined } from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '../../api/user';

const { Title, Text } = Typography;

export default function RegisterPage() {
  // 注册按钮加载状态（防止重复提交）
  const [loading, setLoading] = useState(false);

  // React Router 导航
  const navigate = useNavigate();

  // ============================================================
  // 表单提交处理
  // ============================================================
  // values = { name: "张三", username: "zhangsan", password: "123456" }
  const onFinish = async (values) => {
    setLoading(true);
    try {
      // 调用后端注册接口（明文密码，见文件头安全说明）
      await register({
        name: values.name,
        username: values.username,
        password: values.password,
      });
      message.success('注册成功，请登录');
      // 注册成功后跳回登录页
      navigate('/login');
    } catch (error) {
      // 错误已在 request.js 拦截器中处理，这里做兜底显示
      message.error(error?.response?.data?.message || '注册失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    // 全屏容器：居中布局 + 渐变背景（与登录页保持一致）
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      }}
    >
      {/* 注册卡片 */}
      <Card style={{ width: 400, boxShadow: '0 8px 24px rgba(0,0,0,0.15)' }}>
        {/* ---- 标题区 ---- */}
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <Title level={2} style={{ marginBottom: 8 }}>wisestar</Title>
          <Text type="secondary">创建新账号</Text>
        </div>

        {/* ---- 注册表单 ---- */}
        <Form name="register" onFinish={onFinish} size="large">
          {/* 昵称 */}
          <Form.Item
            name="name"
            rules={[{ required: true, message: '请输入昵称' }]}
          >
            <Input prefix={<SmileOutlined />} placeholder="昵称" />
          </Form.Item>

          {/* 用户名 */}
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>

          {/* 密码 */}
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>

          {/* 注册按钮（loading 状态防止重复点击） */}
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              注册
            </Button>
          </Form.Item>

          {/* 返回登录链接 */}
          <div style={{ textAlign: 'center' }}>
            <Space>
              <Text type="secondary">已有账号？</Text>
              <Link to="/login">去登录</Link>
            </Space>
          </div>
        </Form>
      </Card>
    </div>
  );
}
