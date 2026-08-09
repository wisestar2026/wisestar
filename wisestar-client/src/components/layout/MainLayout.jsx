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
 *
 * 被谁引用: App.jsx（受保护路由组的布局骨架，包裹所有二级页面）
 * 数据来源/去向:
 *   - 用户信息: useUserStore.user（来自 getCurrentUser，登录时填充）
 *   - 退出登录: useUserStore.logout → POST /api/public/logout → 清空状态 → navigate('/login')
 *
 * 菜单项说明:
 *   key 同时作为路由路径，点击时直接 navigate(key)；
 *   "系统管理"（/system）已注册到 App.jsx 的 SystemPage 占位页
 *   （后端 SystemApi 接口已就绪，前端功能页面待开发）
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
  PlayCircleOutlined,
  CompassOutlined,
  ReadOutlined,
  ProfileOutlined,
  PartitionOutlined,
  BulbOutlined,
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
  // 注意: App.jsx 已注册全部菜单路由；/system 目前指向 SystemPage 占位页
  const menuItems = [
    { key: '/',              icon: <DashboardOutlined />, label: '仪表盘' },
    { key: '/student',       icon: <CompassOutlined />,   label: '学生端主页' },
    { key: '/practice',      icon: <PlayCircleOutlined />, label: '在线练习' },
    { key: '/projects',      icon: <ProjectOutlined />,   label: '问卷管理' },
    { key: '/answers',       icon: <FileTextOutlined />,  label: '答案管理' },
    {
      key: 'repo-group', icon: <BookOutlined />, label: '题库管理',
      children: [
        { key: '/repos',         label: '题库列表' },
        { key: '/repo-assign',   label: '题库分配' },
        { key: '/wrong-questions', label: '错题库管理' },
      ],
    },
    { key: '/questions',     icon: <AppstoreOutlined />,  label: '题目管理' },
    {
      key: 'knowledge-group', icon: <ReadOutlined />, label: '知识管理',
      children: [
        { key: '/knowledge/chapters', icon: <ProfileOutlined />, label: '章节管理' },
        { key: '/knowledge/sections', icon: <PartitionOutlined />, label: '小节管理' },
        { key: '/knowledge/points',   icon: <BulbOutlined />,    label: '知识点管理' },
      ],
    },
    { key: '/system',        icon: <SettingOutlined />,   label: '系统管理' },
  ];

  // ============================================================
  // 退出登录处理
  // ============================================================
  // 数据流向: Dropdown 菜单点击 → handleLogout → useUserStore.logout
  //   → api/user.js logout() → POST /api/public/logout（清除服务端 session）
  //   → set({user:null, isLoggedIn:false}) → navigate('/login')
  const handleLogout = async () => {
    await logout();       // 调用后端登出 + 清空 Zustand 状态
    navigate('/login');   // 跳转到登录页（此时 AuthGuard 已放行 /login 公开路由）
  };

  // ============================================================
  // 右上角用户下拉菜单
  // ============================================================
  // user.name / user.username 由登录时 GET /api/currentUser 返回并存入 Zustand
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
  // 从 URL 路径计算选中的 key（让子页面也能高亮对应菜单项）
  // ============================================================
  // 顶层菜单项 key = 第一段路径（如 /projects、/practice）；
  // 子菜单项 key = 完整路径（如 /repo-assign、/knowledge/chapters）。
  // 知识管理三个子页 key 为完整路径，需先精确匹配再回退到第一段路径。
  const SUB_PATH_KEYS = ['/repo-assign', '/wrong-questions', '/knowledge/chapters', '/knowledge/sections', '/knowledge/points'];
  const selectedKey = location.pathname === '/'
    ? '/'
    : (SUB_PATH_KEYS.includes(location.pathname)
      ? location.pathname
      : '/' + location.pathname.split('/')[1]);

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
          defaultOpenKeys={['repo-group', 'knowledge-group']}
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
