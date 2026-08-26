/**
 * TaskManagePage.jsx - 今日任务管理页（后台，老师每日布置）
 *
 * 功能:
 *   1. 任务列表（日期/名称/类型/关联内容/状态）
 *   2. 新增/编辑任务：名称/描述/日期/类型（练习|知识点）/关联内容选择
 *   3. 删除任务
 *
 * URL: /tasks（受 AuthGuard 保护，需要 task:list 权限）
 * 被谁引用: App.jsx 路由；MainLayout 侧边栏「今日任务 → 任务管理」菜单进入
 *
 * 完成判定（学员端）: 当日交卷该关联内容且正确率≥60%
 */

import { useEffect, useState, useCallback } from 'react';
import {
  Table, Space, Button, Input, Select, DatePicker, Modal, Form, Typography, Popconfirm, message, Tag,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { listTasks, createTask, updateTask, deleteTask } from '../../api/task';
import { listRepo } from '../../api/repo';
import { listStudents } from '../../api/student';
import { listSubjects, listChapters, listSections, listKnowledgePoints } from '../../api/knowledge';
import { usePermission } from '../../utils/usePermission';

const { Title } = Typography;

export default function TaskManagePage() {
  const { can } = usePermission();
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [taskDate, setTaskDate] = useState(dayjs().format('YYYY-MM-DD'));
  const [name, setName] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  // 关联内容选项
  const [studentOptions, setStudentOptions] = useState([]);
  const [repoOptions, setRepoOptions] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [dlgChapters, setDlgChapters] = useState([]);
  const [dlgSections, setDlgSections] = useState([]);
  const [dlgPoints, setDlgPoints] = useState([]);
  const [contentType, setContentType] = useState('practice');

  const loadList = useCallback(() => {
    setLoading(true);
    listTasks({ taskDate: taskDate || undefined, name: name || undefined })
      .then((res) => setList(res?.data || []))
      .catch(() => setList([]))
      .finally(() => setLoading(false));
  }, [taskDate, name]);

  useEffect(() => {
    loadList();
  }, [loadList]);

  // 关联内容选项（练习列表 + 学科）
  useEffect(() => {
    listStudents({ current: 1, pageSize: 200 }).then((res) => {
      setStudentOptions((res?.data?.list || []).map((x) => ({ value: x.id, label: `${x.studentNo} ${x.name}` })));
    }).catch(() => setStudentOptions([]));
    listRepo({ current: 1, pageSize: 200 }).then((res) => {
      setRepoOptions((res?.data?.list || []).map((r) => ({ value: r.id, label: r.name })));
    }).catch(() => {});
    listSubjects().then((res) => setSubjects(res?.data || [])).catch(() => setSubjects([]));
  }, []);

  const openModal = (task = null) => {
    setEditing(task);
    setModalOpen(true);
    setDlgChapters([]);
    setDlgSections([]);
    setDlgPoints([]);
    if (task) {
      setContentType(task.contentType);
      form.setFieldsValue({
        studentId: task.studentId,
        name: task.name,
        description: task.description,
        taskDate: dayjs(task.taskDate),
        contentType: task.contentType,
        contentId: task.contentId,
        status: task.status === 0 ? false : true,
      });
    } else {
      form.resetFields();
      setContentType('practice');
      form.setFieldsValue({ taskDate: dayjs(), contentType: 'practice', status: true });
    }
  };

  // 类型切换
  const handleTypeChange = (type) => {
    setContentType(type);
    form.setFieldsValue({ contentId: undefined });
    setDlgChapters([]);
    setDlgSections([]);
    setDlgPoints([]);
  };

  // 知识点三级联动
  const loadChapters = (subjectId) => {
    if (!subjectId) { setDlgChapters([]); return; }
    listChapters({ subjectId }).then((res) => setDlgChapters(res?.data || [])).catch(() => setDlgChapters([]));
    setDlgSections([]);
    setDlgPoints([]);
  };
  const loadSections = (chapterId) => {
    if (!chapterId) { setDlgSections([]); return; }
    listSections({ chapterId }).then((res) => setDlgSections(res?.data || [])).catch(() => setDlgSections([]));
    setDlgPoints([]);
  };
  const loadPoints = (sectionId) => {
    if (!sectionId) { setDlgPoints([]); return; }
    listKnowledgePoints({ current: 1, pageSize: 200, sectionId }).then((res) => setDlgPoints(res?.data?.list || [])).catch(() => setDlgPoints([]));
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      const payload = {
        studentId: values.studentId,
        name: values.name || '今日任务',
        description: values.description,
        taskDate: dayjs(values.taskDate).format('YYYY-MM-DD'),
        contentType: values.contentType,
        contentId: values.contentId,
        status: values.status ? 1 : 0,
        sort: 1,
      };
      setSaving(true);
      if (editing) {
        updateTask({ ...payload, id: editing.id })
          .then(() => {
            message.success('任务已更新');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      } else {
        createTask(payload)
          .then(() => {
            message.success('任务已布置');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      }
    });
  };

  const handleDelete = (task) => {
    deleteTask({ id: task.id }).then(() => {
      message.success('任务已删除');
      loadList();
    });
  };

  const columns = [
    { title: '绑定学员', dataIndex: 'studentName', width: 100, render: (v) => v || '-' },
    { title: '任务名称', dataIndex: 'name', width: 160 },
    { title: '描述', dataIndex: 'description', ellipsis: true, render: (v) => v || '-' },
    { title: '任务日期', dataIndex: 'taskDate', width: 110 },
    {
      title: '类型', dataIndex: 'contentType', width: 90,
      render: (v) => (v === 'knowledge_point' ? <Tag color="purple">知识点</Tag> : <Tag color="blue">练习</Tag>),
    },
    { title: '关联内容ID', dataIndex: 'contentId', width: 120, render: (v) => v ? <Tag>{v.slice(0, 10)}</Tag> : '-' },
    {
      title: '状态', dataIndex: 'status', width: 80,
      render: (v) => (v === 1 ? <Tag color="green">发布</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作', key: 'action', width: 140,
      render: (_, record) => (
        <Space>
          {can('task:update') && (
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(record)}>编辑</Button>
          )}
          {can('task:delete') && (
            <Popconfirm title="确定删除该任务？" onConfirm={() => handleDelete(record)}>
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>今日任务管理</Title>
        {can('task:create') && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>布置任务</Button>
        )}
      </div>

      <Space wrap style={{ marginBottom: 16 }}>
        <DatePicker value={taskDate ? dayjs(taskDate) : null} onChange={(d) => setTaskDate(d ? d.format('YYYY-MM-DD') : '')} />
        <Input placeholder="任务名称" allowClear style={{ width: 160 }} value={name} onChange={(e) => setName(e.target.value)} onPressEnter={loadList} />
        <Button type="primary" onClick={loadList}>查询</Button>
        <Button icon={<ReloadOutlined />} onClick={() => { setTaskDate(dayjs().format('YYYY-MM-DD')); setName(''); }}>重置</Button>
      </Space>

      <Table rowKey="id" loading={loading} columns={columns} dataSource={list} pagination={false} />

      {/* ---- 布置/编辑任务弹窗 ---- */}
      <Modal
        title={editing ? '编辑任务' : '布置任务'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }}>
          <Form.Item name="studentId" label="绑定学员" rules={[{ required: true, message: '请选择绑定学员' }]}>
            <Select showSearch optionFilterProp="label" placeholder="选择学员（每人每日最多 3 个任务）" options={studentOptions} />
          </Form.Item>
          <Form.Item name="name" label="任务名称">
            <Input placeholder="选填，如：完成数学练习一" maxLength={128} />
          </Form.Item>
          <Form.Item name="description" label="任务描述">
            <Input.TextArea placeholder="选填" rows={2} maxLength={512} />
          </Form.Item>
          <Form.Item name="taskDate" label="任务日期" rules={[{ required: true, message: '请选择任务日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="contentType" label="任务类型" rules={[{ required: true, message: '请选择任务类型' }]}>
            <Select
              options={[
                { value: 'practice', label: '练习' },
                { value: 'knowledge_point', label: '知识点' },
              ]}
              onChange={handleTypeChange}
            />
          </Form.Item>

          {contentType === 'practice' ? (
            <Form.Item name="contentId" label="关联练习" rules={[{ required: true, message: '请选择关联练习' }]}>
              <Select showSearch optionFilterProp="label" placeholder="选择练习" options={repoOptions} />
            </Form.Item>
          ) : (
            <>
              <Form.Item name="subjectId" label="学科">
                <Select allowClear placeholder="选择学科" options={subjects.map((s) => ({ value: s.id, label: `${s.icon || ''} ${s.name}` }))}
                  onChange={loadChapters} showSearch optionFilterProp="label" />
              </Form.Item>
              <Form.Item name="chapterId" label="章节">
                <Select allowClear placeholder="选择章节" options={dlgChapters.map((c) => ({ value: c.id, label: c.name }))}
                  onChange={loadSections} showSearch optionFilterProp="label" />
              </Form.Item>
              <Form.Item name="sectionId" label="小节">
                <Select allowClear placeholder="选择小节" options={dlgSections.map((s) => ({ value: s.id, label: s.name }))}
                  onChange={loadPoints} showSearch optionFilterProp="label" />
              </Form.Item>
              <Form.Item name="contentId" label="关联知识点" rules={[{ required: true, message: '请选择关联知识点' }]}>
                <Select allowClear placeholder="选择知识点" options={dlgPoints.map((p) => ({ value: p.id, label: p.name }))}
                  showSearch optionFilterProp="label" />
              </Form.Item>
            </>
          )}

          <Form.Item name="status" label="发布">
            <Select options={[{ value: 1, label: '发布' }, { value: 0, label: '停用' }]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
