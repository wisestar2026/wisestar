/**
 * TaskAssignmentPage.jsx - 任务发布页（学管师后台）
 *
 * 功能:
 *   1. 选择一名或多名学员
 *   2. 逐条填写最多 3 条任务内容（每个输入框一条独立任务）
 *   3. 一键发布（每个学员 × 每条任务各生成一条记录，每人每日最多 3 条）
 *   4. 查看 / 撤回已发布任务
 *
 * URL: /student/task-assignment（学员管理 → 任务发布）
 */

import { useCallback, useEffect, useState } from 'react';
import { Form, Input, Select, Button, message, Card, Space, Table, Popconfirm, Tag } from 'antd';
import { listStudents } from '../../api/student';
import {
  publishStudentTask,
  pageStudentTasks,
  deleteStudentTask,
} from '../../api/studentTask';

const STATUS_MAP = {
  pending: { color: 'processing', text: '待完成' },
  completed: { color: 'success', text: '已完成' },
};

export default function TaskAssignmentPage() {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  // 学员下拉数据
  const [students, setStudents] = useState([]);
  const [studentLoading, setStudentLoading] = useState(false);

  // 发布记录
  const [tasks, setTasks] = useState([]);
  const [total, setTotal] = useState(0);
  const [tableLoading, setTableLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [keyword, setKeyword] = useState('');

  // 加载学员列表
  useEffect(() => {
    setStudentLoading(true);
    listStudents({ current: 1, pageSize: 1000 })
      .then((res) => setStudents(res?.data?.list || []))
      .catch(() => setStudents([]))
      .finally(() => setStudentLoading(false));
  }, []);

  // 加载发布记录
  const loadTasks = useCallback(() => {
    setTableLoading(true);
    pageStudentTasks({ current: page, pageSize, content: keyword || undefined })
      .then((res) => {
        setTasks(res?.data?.list || []);
        setTotal(res?.data?.total || 0);
      })
      .catch(() => {
        setTasks([]);
        setTotal(0);
      })
      .finally(() => setTableLoading(false));
  }, [page, pageSize, keyword]);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  const handlePublish = () => {
    form.validateFields().then((values) => {
      const contents = (values.contents || [])
        .map((c) => (c || '').trim())
        .filter(Boolean);
      setSubmitting(true);
      publishStudentTask({
        studentIds: values.studentIds,
        contents,
      })
        .then((res) => {
          const count = res?.data ?? (contents.length * values.studentIds.length);
          message.success(`已发布 ${contents.length} 条任务，共 ${count} 条记录`);
          form.resetFields();
          form.setFieldsValue({ contents: [''] });
          setPage(1);
          loadTasks();
        })
        .catch((err) => {
          message.error(err?.response?.data?.message || '发布失败，请重试');
        })
        .finally(() => setSubmitting(false));
    });
  };

  const handleDelete = (id) => {
    deleteStudentTask(id)
      .then(() => {
        message.success('已撤回任务');
        loadTasks();
      })
      .catch(() => message.error('撤回失败，请重试'));
  };

  const columns = [
    {
      title: '学员',
      dataIndex: 'studentName',
      key: 'studentName',
      width: 180,
      render: (_, record) => (
        <span>
          {record.studentName || '—'}
          {record.studentNo && <span style={{ color: '#999' }}>（{record.studentNo}）</span>}
        </span>
      ),
    },
    { title: '任务内容', dataIndex: 'taskContent', key: 'taskContent' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        const item = STATUS_MAP[status] || { color: 'default', text: status || '—' };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    { title: '发布时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Popconfirm title="确认撤回该任务？" onConfirm={() => handleDelete(record.id)}>
          <Button type="link" danger size="small">
            撤回
          </Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div style={{ padding: 20 }}>
      <Card title="任务发布" style={{ marginBottom: 16 }}>
        <Form form={form} layout="vertical" initialValues={{ contents: [''] }}>
          <Form.Item
            name="studentIds"
            label="选择学员"
            rules={[{ required: true, message: '请至少选择一名学员' }]}
          >
            <Select
              mode="multiple"
              allowClear
              showSearch
              loading={studentLoading}
              placeholder="可搜索并多选学员"
              optionFilterProp="label"
              options={students.map((s) => ({
                value: s.id,
                label: `${s.name}${s.studentNo ? `（${s.studentNo}）` : ''}`,
              }))}
            />
          </Form.Item>

          <Form.List name="contents">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name }, index) => (
                  <Form.Item
                    key={key}
                    label={index === 0 ? '任务内容（每个输入框一条任务，最多 3 条）' : ''}
                    required
                    style={{ marginBottom: 8 }}
                  >
                    <Space align="baseline" style={{ display: 'flex' }}>
                      <Form.Item
                        name={name}
                        noStyle
                        rules={[{ required: true, whitespace: true, message: '请填写任务内容' }]}
                      >
                        <Input
                          maxLength={500}
                          placeholder="请输入任务内容（如：完成第 1 单元单词背诵并朗读三遍）"
                        />
                      </Form.Item>
                      {fields.length > 1 && (
                        <Button danger type="text" onClick={() => remove(name)}>
                          删除
                        </Button>
                      )}
                    </Space>
                  </Form.Item>
                ))}
                {fields.length < 3 && (
                  <Form.Item style={{ marginBottom: 16 }}>
                    <Button type="dashed" block onClick={() => add('')}>
                      + 添加任务
                    </Button>
                  </Form.Item>
                )}
              </>
            )}
          </Form.List>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" loading={submitting} onClick={handlePublish}>
              发布任务
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card title="发布记录">
        <Space style={{ marginBottom: 12 }}>
          <Input.Search
            allowClear
            placeholder="按任务内容搜索"
            style={{ width: 260 }}
            onSearch={(value) => {
              setKeyword(value.trim());
              setPage(1);
            }}
          />
        </Space>
        <Table
          rowKey="id"
          loading={tableLoading}
          columns={columns}
          dataSource={tasks}
          pagination={{
            current: page,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (p, ps) => {
              setPage(p);
              setPageSize(ps);
            },
          }}
        />
      </Card>
    </div>
  );
}
