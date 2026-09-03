/**
 * MainLayout.jsx - 主布局组件
 *
 * 页面结构:
 *   +--------------------------------------------------+
 *   |  Sider (侧边栏)  |  Header (顶栏)    用户菜单      |
 *   |  wisestar        |  Header (顶栏)    用户菜单      |
 *   |   仪表盘         |                                 |
 *   |   练习管理       |  Content (内容区)               |
 *   |   <Outlet />     |    <Outlet />                   |
 *   |   练习管理       |    (子路由页面在此渲染)          |
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
 *   "系统管理"为分组菜单（用户管理/部门管理/岗位管理/字典管理/字典条目管理，
 *   对应 App.jsx 的 /system/users|depts|positions|dicts|dict-items 五个页面）
 */

import { Layout, Menu, Avatar, Dropdown, Typography } from 'antd';
import {
  CalendarOutlined,
  ShoppingOutlined,  DashboardOutlined,
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
  TeamOutlined,
  SolutionOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import useUserStore from '../../stores/useUserStore';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

export default function MainLayout() {
  const { user, logout } = useUserStore();
  const navigate = useNavigate();
  const location = useLocation();

  // 当前用户权限点列表（无权限点的菜单项将被隐藏）
  const authorityList = user?.authorityList || [];

  // 判断是否拥有任一权限点；未配置 required 的菜单项对所有登录用户可见
  const hasAny = (perms) => !perms || perms.length === 0 || perms.some((p) => authorityList.includes(p));

  // ============================================================
  // 侧边栏菜单配置
  // ============================================================
  // key 对应路由路径，点击后 navigate(key) 跳转
  // required: 权限点列表（满足任一点即显示）；无 required 的菜单项始终显示
  const menuItems = [
    { key: '/',              icon: <DashboardOutlined />, label: '仪表盘', required: ['home'] },
    { key: '/student',       icon: <CompassOutlined />,   label: '学生端主页' },
    
    // 学员管理
    {
      key: 'student-mgmt-group', icon: <TeamOutlined />, label: '学员管理',
      children: [
        { key: '/students', label: '学员列表' },
        { key: '/orders',   label: '订单管理', required: ['order:list'] },
        { key: '/student/supervision', label: '学员督学' },
        { key: '/student/task-assignment', label: '任务分配' },
      ],
    },
    
    // 习题管理
    {
      key: 'exercise-group', icon: <BookOutlined />, label: '习题管理',
      children: [
        { key: '/questions', label: '题目管理', required: ['template:list'] },
        { key: '/exercise/list', label: '习题列表' },
        { key: '/exercise/wrong-book', label: '错题管理' },
      ],
    },
    
    // 知识管理
    {
      key: 'knowledge-group', icon: <ReadOutlined />, label: '知识管理',
      children: [
        { key: '/knowledge/chapters', icon: <ProfileOutlined />, label: '章节管理' },
        { key: '/knowledge/sections', icon: <PartitionOutlined />, label: '小节管理' },
        { key: '/knowledge/points',   icon: <BulbOutlined />,    label: '知识点管理' },
      ],
    },
    
    // 英语板块
    {
      key: 'english-group', icon: <BookOutlined />, label: '英语板块',
      children: [
        { key: '/english/unit', label: '单元管理' },
        { key: '/english/word', label: '单词管理', required: ['english:word:list'] },
        { key: '/english/sentence', label: '句式管理' },
        { key: '/english/study', label: '单词学习' },
        { key: '/english/word-book', label: '单词本' },
        { key: '/english/word-ai', label: 'AI 内容生成', required: ['english:word:ai'] },
      ],
    },
    
    // 积分管理
    {
      key: 'mall-group', icon: <ShoppingOutlined />, label: '积分管理',
      children: [
        { key: '/mall/goods', label: '商品管理', required: ['mall:list'] },
        { key: '/mall/verify', label: '商品核销' },
      ],
    },
    
    // 行政管理
    {
      key: 'admin-group', icon: <SolutionOutlined />, label: '行政管理',
      children: [
        { key: '/admin/roles', label: '角色权限', required: ['system:role:list'] },
        { key: '/admin/campus', label: '校区管理' },
      ],
    },
    
    // 系统管理
    {
      key: 'system-group', icon: <SettingOutlined />, label: '系统管理',
      children: [
        { key: '/system/users', label: '用户管理', required: ['system:user:list'] },
        { key: '/system/depts', label: '部门管理', required: ['system:dept:list'] },
        { key: '/system/positions', label: '岗位管理', required: ['system:position:list'] },
        { key: '/system/dicts', label: '字典管理', required: ['system:dict:list'] },
        { key: '/system/dict-items', label: '字典条目管理', required: ['system:dictItem:list'] },
      ],
    },
  ];

  // 按权限过滤菜单：无权限的子菜单整体隐藏，父菜单全部子项隐藏时父菜单也隐藏
  const filterMenu = (items) => items
    .filter((item) => hasAny(item.required))
    .map((item) => (item.children ? { ...item, children: filterMenu(item.children) } : item))
    .filter((item) => !item.children || item.children.length > 0);
  const visibleMenu = filterMenu(menuItems);

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
  const SUB_PATH_KEYS = ['/tasks', '/mall/goods', '/students/activity', '/repo-assign', '/wrong-questions', '/knowledge/chapters', '/knowledge/sections', '/knowledge/points', '/hr/roles', '/system/users', '/system/depts', '/system/positions', '/system/dicts', '/system/dict-items'];
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
          // 左侧菜单分组默认全部收起（用户手动展开；当前激活菜单项所在分组由 Menu 自动高亮，不自动展开）
          defaultOpenKeys={[]}
          items={visibleMenu}
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
