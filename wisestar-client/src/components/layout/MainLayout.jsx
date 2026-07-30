/**
 * MainLayout.jsx - 主布局组件
 *
 * 页面结构:
 *   +--------------------------------------------------+
 *   |  Sider (侧边栏)  |  Header (顶栏)    用户菜单      |
 *   |  wisestar        |  Header (顶栏)    用户菜单      |
 *   |   仪表盘         |                                 |
 *   |   问卷管理       |  Content (内容区)               |
 *   |   答案管理       |    <Outlet />                   |
 *   |   题库管理       |    (子路由页面在此渲染)          |
 *   |   模板管理       |                                 |
 *   |   系统管理       |                                 |
 *   +-----------------+---------------------------------+
 *
 * 技术方案:
 *   - 使用 Ant Design 的 Layout / Sider / Header / Content 组件
 *   - 侧边栏 Menu 的 selectedKeys 根据当前 URL 路径计算
 *   - 点击菜单项通过 react-router-dom 的 navigate() 跳转
 *   - 右上角用户下拉菜单（显示用户名 + 退出登录）
 *   - <Outlet /> 是 React Router 的子路由插槽，渲染匹配的子页面
 */

import { Layout, Menu, Avatar, Dropdown, Typography } from 'antd';
import {
  DashboardOutlined,
  ProjectOutlined,
  FileTextOutlined,
  SettingOutlined,
  LogoutOutlined,
  UserOutlined,
  BookOutlined,
  AppstoreOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import useUserStore from '../../stores/useUserStore';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

export default function MainLayout() {
  const { user, logout } = useUserStore();
  const navigate = useNavigate();
  const location = useLocation();

  // ============================================================
  // 侧边栏菜单配置
  // ============================================================
  // key 对应路由路径，点击后 navigate(key) 跳转
  const menuItems = [
    { key: '/',              icon: <DashboardOutlined />, label: '仪表盘' },
    { key: '/projects',      icon: <ProjectOutlined />,   label: '问卷管理' },
    { key: '/answers',       icon: <FileTextOutlined />,  label: '答案管理' },
    { key: '/repos',         icon: <BookOutlined />,      label: '题库管理' },
    { key: '/questions',     icon: <AppstoreOutlined />,  label: '题目管理' },
    { key: '/system',        icon: <SettingOutlined />,   label: '系统管理' },
  ];

  // ============================================================
  // 退出登录处理
  // ============================================================
  const handleLogout = async () => {
    await logout();       // 调用后端登出 + 清空 Zustand 状态
    navigate('/login');   // 跳转到登录页
  };

  // ============================================================
  // 右上角用户下拉菜单
  // ============================================================
  const userMenu = {
    items: [
      // 显示用户名（不可点击，仅展示）
      { key: 'info', label: `${user?.name || user?.username || '用户'}` },
      { type: 'divider' },
      { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: handleLogout },
    ],
  };

  // ============================================================
  // 计算当前选中的菜单项
  // ============================================================
  // 从 URL 路径提取第一段作为选中的 key（如 /projects/list → /projects）
  const selectedKey = location.pathname === '/' ? '/' : '/' + location.pathname.split('/')[1];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* ---- 左侧边栏 ---- */}
      <Sider width={220} theme="dark">
        {/* 系统标题 */}
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Text style={{ color: 'white', fontSize: 18, fontWeight: 'bold' }}>
             wisestar
          </Text>
        </div>
        {/* 菜单 */}
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>

      {/* ---- 右侧主体 ---- */}
      <Layout>
        {/* 顶部栏（右侧放置用户下拉菜单） */}
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            borderBottom: '1px solid #f0f0f0',
          }}
        >
          {/* 用户头像 + 用户名 → 点击展开下拉菜单 */}
          <Dropdown menu={userMenu} placement="bottomRight">
            <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Avatar size="small" icon={<UserOutlined />} />
              <span>{user?.name || user?.username || '用户'}</span>
            </div>
          </Dropdown>
        </Header>

        {/* 内容区：渲染子路由页面 */}
        <Content style={{ margin: 24, background: '#fff', borderRadius: 8, padding: 24, minHeight: 280 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
