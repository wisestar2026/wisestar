/**
 * ProjectListPage.jsx - 问卷列表页面
 *
 * 功能:
 *   1. 分页展示问卷列表（名称、类型、状态、答卷数、创建时间、操作按钮）
 *   2. 搜索功能：按问卷名称模糊搜索
 *   3. 新建问卷：弹窗表单，选择名称和类型（调查问卷/考试/文件夹）
 *   4. 删除问卷：确认后软删除（移入回收站）
 *   5. 编辑问卷：点击名称或编辑按钮跳转到编辑页
 *
 * 被谁引用: App.jsx 路由表（/projects）；MainLayout 侧边栏"问卷管理"菜单进入
 *
 * 问卷类型映射:
 *   survey  - 调查问卷（蓝色标签）
 *   exam    - 在线考试（红色标签）
 *   folder  - 文件夹（灰色标签，不显示状态和答卷数列）
 *   注意: 文件夹类型目前点击名称无响应（TODO 待实现文件夹子页面）
 *
 * 发布状态:
 *   0 - 未发布（灰色标签）
 *   1 - 已发布（绿色标签）
 *
 * 数据流:
 *   列表: fetchProjects → listProject({current, pageSize, name}) → GET /api/project/list
 *   创建: handleCreate → createProject(values) → POST /api/project/create
 *   删除: handleDelete → deleteProject(id) → POST /api/project/delete（软删除）
 *   编辑/答卷/预览: 跳转对应路由或新窗口（/survey/:id 为公开填写页）
 *
 * 数据来源: GET /api/project/list?current=1&pageSize=10&name=搜索词
 */

import { useState, useEffect, useCallback } from 'react';
import {
  Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm, Typography,
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined,
  SearchOutlined, ReloadOutlined,
} from '@ant-design/icons';
import { listProject, createProject, deleteProject } from '../../../api/project';
import { useNavigate } from 'react-router-dom';
import { usePermission } from '../../../utils/usePermission';

const { Title } = Typography;

export default function ProjectListPage() {
  const { can } = usePermission();
  // ---- 状态 ----
  const [projects, setProjects] = useState([]);                 // 问卷列表数据
  const [loading, setLoading] = useState(false);                // 列表加载状态
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 }); // 分页信息
  const [searchName, setSearchName] = useState('');             // 搜索关键词
  const [createOpen, setCreateOpen] = useState(false);          // 新建弹窗开关
  const [createLoading, setCreateLoading] = useState(false);    // 新建按钮加载状态

  // Ant Design 表单实例（用于新建弹窗）
  const [form] = Form.useForm();

  // React Router 导航
  const navigate = useNavigate();

  // ============================================================
  // 获取问卷列表
  // ============================================================
  // useCallback 避免每次渲染都创建新函数（性能优化）
  const fetchProjects = useCallback(async (page = 1, size = 10, name = '') => {
    setLoading(true);
    try {
      const res = await listProject({ current: page, pageSize: size, name });
      setProjects(res.data?.list || []);
      setPagination({ current: page, pageSize: size, total: res.data?.total || 0 });
    } catch {
      // 错误在 request.js 拦截器中已弹出提示，此处无需额外处理
    } finally {
      setLoading(false);
    }
  }, []);

  // 组件首次加载时获取数据
  useEffect(() => {
    fetchProjects();
  }, [fetchProjects]);

  // ============================================================
  // 创建问卷
  // ============================================================
  const handleCreate = async (values) => {
    setCreateLoading(true);
    try {
      await createProject(values);
      message.success('创建成功');
      // 关闭弹窗并重置表单
      setCreateOpen(false);
      form.resetFields();
      // 刷新列表
      fetchProjects(pagination.current, pagination.pageSize, searchName);
    } catch {
      // 错误已处理
    } finally {
      setCreateLoading(false);
    }
  };

  // ============================================================
  // 删除问卷
  // ============================================================
  const handleDelete = async (id) => {
    try {
      await deleteProject(id);
      message.success('已移入回收站');
      // 刷新列表
      fetchProjects(pagination.current, pagination.pageSize, searchName);
    } catch {
      // 错误已处理
    }
  };

  // ============================================================
  // 搜索
  // ============================================================
  const handleSearch = () => {
    // 搜索时重置到第 1 页
    fetchProjects(1, pagination.pageSize, searchName);
  };

  // ============================================================
  // 问卷类型 → 标签颜色/文字映射
  // ============================================================
  const modeMap = {
    survey: { color: 'blue',    label: '问卷' },
    exam:   { color: 'red',     label: '考试' },
    folder: { color: 'default', label: '文件夹' },
  };

  // ============================================================
  // 表格列定义
  // ============================================================
  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      // 自定义渲染：点击名称跳转到编辑页
      render: (text, record) => (
        <a onClick={() => {
          if (record.mode === 'folder') {
            // TODO: 进入文件夹子页面（待实现）
          } else {
            navigate(`/projects/${record.id}/edit`);
          }
        }}>
          {text}
        </a>
      ),
    },
    {
      title: '类型',
      dataIndex: 'mode',
      key: 'mode',
      width: 100,
      // 根据 mode 显示不同颜色的标签
      render: (mode) => {
        const config = modeMap[mode] || { color: 'default', label: mode };
        return <Tag color={config.color}>{config.label}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      // status: 0=未发布, 1=已发布
      render: (status) => (
        <Tag color={status === 1 ? 'green' : 'default'}>
          {status === 1 ? '已发布' : '未发布'}
        </Tag>
      ),
    },
    {
      title: '答卷数',
      dataIndex: 'total',
      key: 'total',
      width: 100,
    },
    {
      title: '创建时间',
      dataIndex: 'createAt',
      key: 'createAt',
      width: 180,
      // 格式化时间戳为本地时间字符串
      render: (text) => text ? new Date(text).toLocaleString() : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      render: (_, record) => (
        <Space>
          {/* 文件夹类型不显示这些操作按钮 */}
          {record.mode !== 'folder' && (
            <>
              {/* 编辑按钮 → 跳转到编辑页面 */}
              {can('project:update') && (
                <Button
                  type="link"
                  size="small"
                  icon={<EditOutlined />}
                  onClick={() => navigate(`/projects/${record.id}/edit`)}
                >
                  编辑
                </Button>
              )}

              {/* 答卷列表按钮 → 跳转到答卷管理页面 */}
              <Button
                type="link"
                size="small"
                onClick={() => navigate(`/projects/${record.id}/answers`)}
              >
                答卷
              </Button>

              {/* 预览按钮 → 打开公开填写页面 */}
              <Button
                type="link"
                size="small"
                icon={<SearchOutlined />}
                onClick={() => {
                  // 在新窗口打开公开问卷页面
                  window.open(`/survey/${record.id}`, '_blank');
                }}
              >
                预览
              </Button>
            </>
          )}

          {/* 删除按钮（带二次确认弹窗） */}
          {can('project:delete') && (
            <Popconfirm
              title="确定要删除吗？"
              description="删除后将移入回收站，可在回收站恢复"
              onConfirm={() => handleDelete(record.id)}
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* ---- 标题栏 + 新建按钮 ---- */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>问卷管理</Title>
        {can('project:create') && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            新建问卷
          </Button>
        )}
      </div>

      {/* ---- 搜索栏 ---- */}
      <div style={{ marginBottom: 16, display: 'flex', gap: 8 }}>
        <Input
          placeholder="搜索问卷名称"
          value={searchName}
          onChange={(e) => setSearchName(e.target.value)}
          // 按回车触发搜索
          onPressEnter={handleSearch}
          style={{ width: 300 }}
          prefix={<SearchOutlined />}
        />
        <Button onClick={handleSearch} type="primary">搜索</Button>
        <Button
          onClick={() => { setSearchName(''); fetchProjects(); }}
          icon={<ReloadOutlined />}
        >
          刷新
        </Button>
      </div>

      {/* ---- 问卷列表表格 ---- */}
      <Table
        columns={columns}
        dataSource={projects}
        rowKey="id"
        loading={loading}
        pagination={{
          ...pagination,
          showSizeChanger: true,                    // 显示每页条数切换器
          showTotal: (total) => `共 ${total} 条`,    // 显示总数
          onChange: (page, size) => fetchProjects(page, size, searchName), // 翻页时重新请求
        }}
      />

      {/* ---- 新建问卷弹窗 ---- */}
      <Modal
        title="新建问卷"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); form.resetFields(); }}
        footer={null} // 自定义底部按钮（放在 Form.Item 中）
      >
        <Form form={form} onFinish={handleCreate} layout="vertical">
          {/* 问卷名称 */}
          <Form.Item
            name="name"
            label="问卷名称"
            rules={[{ required: true, message: '请输入问卷名称' }]}
          >
            <Input placeholder="例如：员工满意度调查" />
          </Form.Item>

          {/* 问卷类型（默认"调查问卷"） */}
          <Form.Item
            name="mode"
            label="问卷类型"
            initialValue="survey"
          >
            <Select
              options={[
                { label: '调查问卷', value: 'survey' },
                { label: '在线考试', value: 'exam' },
                { label: '文件夹',   value: 'folder' },
              ]}
            />
          </Form.Item>

          {/* 创建按钮 */}
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={createLoading} block>
              创建
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
