/**
 * RepoAssignPage.jsx - 练习分配管理页（老师端）
 *
 * 功能:
 *   1. 学员列表（分页，姓名搜索）——点击选中学员
 *   2. 选中学员后管理其练习练习:
 *      - 学员标签（系统按标签自动分配练习的规则）: 保存后，练习 tag 与学员标签有交集即自动可见
 *      - 已分配练习列表（手动分配记录）: 可移除
 *      - 分配新练习: 勾选练习 → 分配（幂等）
 *
 * URL: /repo-assign（受 AuthGuard 保护，侧边栏「练习分配」菜单进入）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏
 *
 * 数据流:
 *   挂载 → listUser 拉取学员 → 选中学员 → listAssign 拉已分配 + getUserTags 拉标签 + listRepo 拉可选练习
 *   分配 → assignRepo({userId, repoIds}) → 刷新已分配列表
 *   移除 → deleteAssign({ids}) → 刷新
 *   保存标签 → saveUserTags({userId, tags}) → 提示按标签自动分配生效
 *
 * 权限: 接口均需 repo:list / system:user:list（admin 角色已含）
 */

import { useState, useEffect, useCallback } from 'react';
import {
  Card, Table, Button, Tag, Typography, Space, message, Select, Row, Col, Popconfirm,
} from 'antd';
import {
  UserOutlined, TagsOutlined, PlusOutlined, DeleteOutlined, BookOutlined,
} from '@ant-design/icons';
import { listUser } from '../../api/user';
import { listRepo, listAssign, assignRepo, deleteAssign, getUserTags, saveUserTags } from '../../api/repo';

const { Title, Text } = Typography;

export default function RepoAssignPage() {
  // ---- 学员列表 ----
  const [userLoading, setUserLoading] = useState(false);
  const [users, setUsers] = useState([]);
  const [userTotal, setUserTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const pageSize = 10;

  // ---- 选中学员 ----
  const [selectedUser, setSelectedUser] = useState(null);

  // ---- 学员标签（自动分配规则） ----
  const [userTags, setUserTags] = useState([]);
  const [tagSaving, setTagSaving] = useState(false);

  // ---- 分配管理 ----
  const [assignLoading, setAssignLoading] = useState(false);
  const [assigns, setAssigns] = useState([]);       // 已分配记录
  const [allRepos, setAllRepos] = useState([]);     // 全部练习（可选）
  const [checkedRepos, setCheckedRepos] = useState([]); // 勾选待分配
  const [assigning, setAssigning] = useState(false);

  // ---- 加载学员列表 ----
  const fetchUsers = useCallback(async (p = page, kw = keyword) => {
    setUserLoading(true);
    try {
      const res = await listUser({ current: p, pageSize, name: kw || undefined });
      setUsers(res.data?.list || []);
      setUserTotal(res.data?.total || 0);
    } catch {
      message.error('加载学员列表失败');
    } finally {
      setUserLoading(false);
    }
  }, [pageSize, page, keyword]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { fetchUsers(page, keyword); }, [page, keyword, fetchUsers]); // eslint-disable-line react-hooks/exhaustive-deps

  // ---- 选中学员：加载标签 + 已分配 + 全部练习 ----
  const loadAssign = useCallback(async (userId) => {
    if (!userId) return;
    setAssignLoading(true);
    try {
      const [assignRes, tagRes, repoRes] = await Promise.all([
        listAssign(userId),
        getUserTags(userId),
        listRepo({ current: 1, pageSize: 200 }),
      ]);
      setAssigns(assignRes.data || []);
      setUserTags(tagRes.data || []);
      setAllRepos(repoRes.data?.list || []);
    } catch {
      message.error('加载分配数据失败');
    } finally {
      setAssignLoading(false);
    }
  }, []);

  const handleSelectUser = (user) => {
    setSelectedUser(user);
    setCheckedRepos([]);
    loadAssign(user.id);
  };

  // ---- 分配练习 ----
  const handleAssign = async () => {
    if (!selectedUser || checkedRepos.length === 0) {
      message.warning('请先勾选要分配的练习');
      return;
    }
    setAssigning(true);
    try {
      await assignRepo({ userId: selectedUser.id, repoIds: checkedRepos });
      message.success('分配成功，学员端「我的练习」已更新');
      setCheckedRepos([]);
      loadAssign(selectedUser.id);
    } catch {
      message.error('分配失败');
    } finally {
      setAssigning(false);
    }
  };

  // ---- 移除分配 ----
  const handleRemove = async (id) => {
    try {
      await deleteAssign({ ids: [id] });
      message.success('已移除分配');
      loadAssign(selectedUser.id);
    } catch {
      message.error('移除失败');
    }
  };

  // ---- 保存学员标签 ----
  const handleSaveTags = async () => {
    if (!selectedUser) return;
    setTagSaving(true);
    try {
      await saveUserTags({ userId: selectedUser.id, tags: userTags });
      message.success('标签已保存，按标签自动分配已生效');
      loadAssign(selectedUser.id);
    } catch {
      message.error('保存标签失败');
    } finally {
      setTagSaving(false);
    }
  };

  // ---- 学员表格列 ----
  const userColumns = [
    {
      title: '学员姓名',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <Space>
          <UserOutlined style={{ color: '#1677ff' }} />
          <Text strong>{text || '-'}</Text>
          {selectedUser?.id === record.id && <Tag color="blue">已选中</Tag>}
        </Space>
      ),
    },
    {
      title: '登录账号',
      dataIndex: 'username',
      key: 'username',
      render: (t) => <Text type="secondary">{t || '-'}</Text>,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      render: (t) => <Text type="secondary">{t || '-'}</Text>,
    },
  ];

  // ---- 已分配列 ----
  const assignColumns = [
    {
      title: '练习名称',
      dataIndex: 'repoName',
      key: 'repoName',
      render: (t) => <Text strong>{t || '-'}</Text>,
    },
    {
      title: '分配方式',
      dataIndex: 'assignType',
      key: 'assignType',
      width: 110,
      render: (t) => (
        t === 'auto'
          ? <Tag color="cyan">标签自动</Tag>
          : <Tag color="blue">手动分配</Tag>
      ),
    },
    {
      title: '分配时间',
      dataIndex: 'createAt',
      key: 'createAt',
      width: 170,
      render: (t) => <Text type="secondary">{t ? String(t).slice(0, 19).replace('T', ' ') : '-'}</Text>,
    },
    {
      title: '操作',
      key: 'action',
      width: 90,
      render: (_, record) => (
        <Popconfirm title="确定移除该练习分配？" onConfirm={() => handleRemove(record.id)}>
          <Button size="small" danger icon={<DeleteOutlined />}>移除</Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={4} style={{ marginBottom: 4 }}>练习分配</Title>
      <Text type="secondary">
        为学员分配习练习/测试卷（手动分配），或为学员设置标签按标签自动分配练习
      </Text>

      <Row gutter={16} style={{ marginTop: 16 }}>
        {/* ---- 左侧：学员列表 ---- */}
        <Col xs={24} lg={10}>
          <Card title="学员列表">
            <Table
              rowKey="id"
              size="small"
              loading={userLoading}
              columns={userColumns}
              dataSource={users}
              pagination={{
                current: page, pageSize, total: userTotal,
                showTotal: (t) => `共 ${t} 人`,
                onChange: setPage,
              }}
              onRow={(record) => ({
                onClick: () => handleSelectUser(record),
                style: { cursor: 'pointer' },
              })}
            />
          </Card>
        </Col>

        {/* ---- 右侧：选中学员的分配管理 ---- */}
        <Col xs={24} lg={14}>
          {!selectedUser ? (
            <Card>
              <div style={{ padding: 40, textAlign: 'center' }}>
                <Text type="secondary">点击左侧学员查看并管理其练习分配</Text>
              </div>
            </Card>
          ) : (
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              {/* 学员标签（自动分配规则） */}
              <Card
                title={
                  <Space>
                    <TagsOutlined style={{ color: '#13c2c2' }} />
                    学员标签（按标签自动分配练习）
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      练习标签与学员标签有交集时自动出现在学员「我的练习」
                    </Text>
                  </Space>
                }
              >
                <Space wrap style={{ width: '100%' }}>
                  <Select
                    mode="tags"
                    style={{ minWidth: 360, flex: 1 }}
                    placeholder="输入标签后回车（如：三年级、数学、基础）"
                    value={userTags}
                    onChange={setUserTags}
                    tokenSeparators={[',', '，']}
                  />
                  <Button type="primary" loading={tagSaving} onClick={handleSaveTags}>保存标签</Button>
                </Space>
              </Card>

              {/* 已分配练习 */}
              <Card
                title={
                  <Space>
                    <BookOutlined style={{ color: '#1677ff' }} />
                    已分配练习（{assigns.length}）
                  </Space>
                }
                styles={{ body: { padding: assigns.length ? 16 : 8 } }}
              >
                <Table
                  rowKey="id"
                  size="small"
                  loading={assignLoading}
                  columns={assignColumns}
                  dataSource={assigns}
                  pagination={false}
                  locale={{ emptyText: '暂未分配练习，请在下方分配' }}
                />
              </Card>

              {/* 分配新练习 */}
              <Card
                title={
                  <Space>
                    <PlusOutlined style={{ color: '#52c41a' }} />
                    分配练习
                  </Space>
                }
              >
                <Space wrap style={{ width: '100%' }}>
                  <Select
                    mode="multiple"
                    style={{ minWidth: 360, flex: 1 }}
                    placeholder="选择要分配给该学员的练习"
                    value={checkedRepos}
                    onChange={setCheckedRepos}
                    optionFilterProp="label"
                    options={allRepos.map((r) => ({
                      label: `${r.name}（${r.mode === 'exam' ? '测试卷' : '习练习'}·${r.total ?? 0}题）`,
                      value: r.id,
                    }))}
                  />
                  <Button type="primary" icon={<PlusOutlined />} loading={assigning} onClick={handleAssign}>
                    分配
                  </Button>
                </Space>
              </Card>
            </Space>
          )}
        </Col>
      </Row>
    </div>
  );
}
