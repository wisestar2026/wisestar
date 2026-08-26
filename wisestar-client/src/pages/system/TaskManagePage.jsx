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
import { listTasks, updateTask, deleteTask, batchCreateTasks } from '../../api/task';
import { listRepo } from '../../api/repo';
import { listStudents } from '../../api/student';
import { listSubjects, listChapters, listSections, listKnowledgePoints } from '../../api/knowledge';
import { usePermission } from '../../utils/usePermission';

const { Title } = Typography;

/** 知识点选择（行内四级联动：学科→章节→小节→知识点） */
function KnowledgePicker({ value, onChange }) {
  const [subjects, setSubjects] = useState([]);
  const [chapters, setChapters] = useState([]);
  const [sections, setSections] = useState([]);
  const [points, setPoints] = useState([]);
  const [selSubject, setSelSubject] = useState();
  const [selChapter, setSelChapter] = useState();
  const [selSection, setSelSection] = useState();
  useEffect(() => {
    listSubjects().then((res) => setSubjects(res?.data || [])).catch(() => setSubjects([]));
  }, []);
  const loadChapters = (sid) => {
    setSelSubject(sid); setSelChapter(undefined); setSelSection(undefined); setPoints([]);
    onChange(undefined);
    listChapters({ subjectId: sid }).then((res) => setChapters(res?.data || [])).catch(() => setChapters([]));
  };
  const loadSections = (cid) => {
    setSelChapter(cid); setSelSection(undefined); setPoints([]);
    onChange(undefined);
    listSections({ chapterId: cid }).then((res) => setSections(res?.data || [])).catch(() => setSections([]));
  };
  const loadPoints = (sid) => {
    setSelSection(sid);
    listKnowledgePoints({ current: 1, pageSize: 200, sectionId: sid }).then((res) => setPoints(res?.data?.list || [])).catch(() => setPoints([]));
  };
  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Space style={{ width: '100%' }}>
        <Select style={{ flex: 1 }} placeholder="学科" value={selSubject} onChange={loadChapters} showSearch optionFilterProp="label"
          options={subjects.map((x) => ({ value: x.id, label: x.name }))} />
        <Select style={{ flex: 1 }} placeholder="章节" value={selChapter} onChange={loadSections} showSearch optionFilterProp="label"
          options={chapters.map((x) => ({ value: x.id, label: x.name }))} />
        <Select style={{ flex: 1 }} placeholder="小节" value={selSection} onChange={loadPoints} showSearch optionFilterProp="label"
          options={sections.map((x) => ({ value: x.id, label: x.name }))} />
      </Space>
      <Select style={{ width: '100%' }} placeholder="选择知识点" value={value} onChange={onChange} showSearch optionFilterProp="label"
        options={points.map((x) => ({ value: x.id, label: x.name }))} />
    </Space>
  );
}

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
  }, []);

  const openModal = (task = null) => {
    setEditing(task);
    setModalOpen(true);
    if (task) {
      form.setFieldsValue({
        studentId: task.studentId,
        taskDate: dayjs(task.taskDate),
        tasks: [{ name: task.name, contentType: task.contentType, contentId: task.contentId }],
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ taskDate: dayjs(), tasks: [{ contentType: 'practice' }] });
    }
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      if (!values.tasks || values.tasks.length === 0) {
        message.warning('请至少添加一个任务');
        return;
      }
      const requests = values.tasks.map((t) => ({
        studentId: values.studentId,
        taskDate: dayjs(values.taskDate).format('YYYY-MM-DD'),
        name: t.name || '今日任务',
        contentType: t.contentType,
        contentId: t.contentId,
        status: 1,
        sort: 1,
      }));
      setSaving(true);
      const action = editing
        ? (() => { const p = requests[0]; p.id = editing.id; p.name = values.tasks[0].name || '今日任务'; return updateTask(p); })()
        : batchCreateTasks(requests);
      action
        .then(() => {
          message.success(editing ? '任务已更新' : '任务已布置，学员端已同步');
          setModalOpen(false);
          loadList();
        })
        .catch(() => {})
        .finally(() => setSaving(false));
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
          <Form.Item name="taskDate" label="任务日期" rules={[{ required: true, message: '请选择任务日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          <Form.List name="tasks">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field, idx) => (
                  <div key={field.key} style={{ border: '1px solid #e3f2fd', borderRadius: 10, padding: 12, marginBottom: 12, background: '#f8fcff' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                      <b>任务 {idx + 1}</b>
                      {fields.length > 1 && (
                        <Button type="link" size="small" danger onClick={() => remove(field.name)}>移除</Button>
                      )}
                    </div>
                    <Form.Item {...field} name={[field.name, 'name']} label="任务名称" labelCol={{ span: 5 }} wrapperCol={{ span: 18 }} style={{ marginBottom: 8 }}>
                      <Input placeholder="选填，如：完成数学练习一" maxLength={128} />
                    </Form.Item>
                    <Form.Item {...field} name={[field.name, 'contentType']} label="任务类型" rules={[{ required: true, message: '请选择类型' }]} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }} style={{ marginBottom: 8 }}>
                      <Select options={[{ value: 'practice', label: '练习' }, { value: 'knowledge_point', label: '知识点' }]} placeholder="选择类型" />
                    </Form.Item>
                    <Form.Item shouldUpdate noStyle>
                      {() => {
                        const type = form.getFieldValue(['tasks', field.name, 'contentType']);
                        if (type === 'practice') {
                          return (
                            <Form.Item {...field} name={[field.name, 'contentId']} label="关联练习" rules={[{ required: true, message: '请选择练习' }]} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }} style={{ marginBottom: 0 }}>
                              <Select showSearch optionFilterProp="label" placeholder="选择练习" options={repoOptions} />
                            </Form.Item>
                          );
                        }
                        if (type === 'knowledge_point') {
                          return (
                            <Form.Item {...field} name={[field.name, 'contentId']} label="关联知识点" rules={[{ required: true, message: '请选择知识点' }]} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }} style={{ marginBottom: 0 }}>
                              <KnowledgePicker />
                            </Form.Item>
                          );
                        }
                        return null;
                      }}
                    </Form.Item>
                  </div>
                ))}
                <Button
                  block type="dashed" icon={<PlusOutlined />}
                  onClick={() => {
                    if (fields.length >= 3) {
                      message.warning('每人每日最多布置 3 个任务');
                      return;
                    }
                    add({ contentType: 'practice' });
                  }}
                >
                  新增任务（已添加 {fields.length}/3）
                </Button>
              </>
            )}
          </Form.List>
        </Form>
      </Modal>
    </div>
  );
}
