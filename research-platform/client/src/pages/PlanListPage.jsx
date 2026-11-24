import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  App as AntApp,
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { CLASS_TYPES, GRADES, PLAN_STATUS, SEMESTERS, SUBJECTS, statusMeta } from '../constants'
import {
  createPlan,
  deletePlan,
  fetchPlan,
  fetchPlanLogs,
  fetchPlans,
  reviewPlan,
  submitPlan,
  updatePlan,
  withdrawPlan,
} from '../api/plan'
import { useAuthStore } from '../store/auth'

const { Text } = Typography

const semesterLabel = (value) => SEMESTERS.find((item) => item.value === value)?.label || value

const fmt = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

export default function PlanListPage() {
  const { message, modal } = AntApp.useApp()
  const [form] = Form.useForm()
  const [reviewForm] = Form.useForm()
  const user = useAuthStore((s) => s.user)
  const canReview = user?.role === 'ADMIN' || user?.role === 'SUPERVISOR'

  const [data, setData] = useState([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [query, setQuery] = useState({ current: 1, pageSize: 10 })
  const [filters, setFilters] = useState({})

  const [editOpen, setEditOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)
  const [reviewOpen, setReviewOpen] = useState(false)
  const [reviewing, setReviewing] = useState(null)
  const [logsOpen, setLogsOpen] = useState(false)
  const [logs, setLogs] = useState([])
  const [logTitle, setLogTitle] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const result = await fetchPlans({ ...query, ...filters })
      setData(result.list)
      setTotal(result.total)
    } catch (e) {
      message.error(e.message)
    } finally {
      setLoading(false)
    }
  }, [query, filters, message])

  useEffect(() => {
    load()
  }, [load])

  const applyFilter = (patch) => {
    setFilters((prev) => ({ ...prev, ...patch }))
    setQuery((prev) => ({ ...prev, current: 1 }))
  }

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({
      semester: SEMESTERS[0].value,
      subject: '数学',
      grade: '七年级',
      classType: '同步班',
      classHours: 16,
    })
    setEditOpen(true)
  }

  const openEdit = async (record) => {
    try {
      const detail = await fetchPlan(record.id)
      setEditing(detail)
      form.setFieldsValue(detail)
      setEditOpen(true)
    } catch (e) {
      message.error(e.message)
    }
  }

  const save = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)
      if (editing) {
        await updatePlan(editing.id, values)
        message.success('方案已更新')
      } else {
        await createPlan(values)
        message.success('方案已创建')
      }
      setEditOpen(false)
      load()
    } catch (e) {
      if (e?.errorFields) return
      message.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  const confirmAction = (title, content, action) => {
    modal.confirm({
      title,
      content,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await action()
          message.success('操作成功')
          load()
        } catch (e) {
          message.error(e.message)
          throw e
        }
      },
    })
  }

  const openReview = (record) => {
    setReviewing(record)
    reviewForm.resetFields()
    reviewForm.setFieldsValue({ approve: true, remark: '' })
    setReviewOpen(true)
  }

  const submitReview = async () => {
    try {
      const values = await reviewForm.validateFields()
      setSaving(true)
      await reviewPlan(reviewing.id, values)
      message.success(values.approve ? '已审核通过并发布' : '已驳回')
      setReviewOpen(false)
      load()
    } catch (e) {
      if (e?.errorFields) return
      message.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  const openLogs = async (record) => {
    setLogTitle(record.title)
    setLogsOpen(true)
    setLogs([])
    try {
      setLogs(await fetchPlanLogs(record.id))
    } catch (e) {
      message.error(e.message)
    }
  }

  const columns = useMemo(
    () => [
      {
        title: '方案',
        dataIndex: 'title',
        render: (value, record) => (
          <Space orientation="vertical" size={0}>
            <Text strong>{value}</Text>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {record.subject} · {record.grade} · {record.classType} · {record.classHours} 课时
            </Text>
          </Space>
        ),
      },
      { title: '学期', dataIndex: 'semester', width: 190, render: semesterLabel },
      { title: '创建人', dataIndex: 'ownerName', width: 100 },
      {
        title: '状态',
        dataIndex: 'status',
        width: 100,
        render: (value) => {
          const meta = statusMeta(value)
          return <Tag color={meta.color}>{meta.label}</Tag>
        },
      },
      { title: '更新时间', dataIndex: 'updateAt', width: 150, render: fmt },
      {
        title: '操作',
        width: 300,
        render: (_, record) => {
          const editable = record.status === 'DRAFT' || record.status === 'REJECTED'
          return (
            <Space size={4} wrap>
              {editable && <Button size="small" onClick={() => openEdit(record)}>编辑</Button>}
              {editable && (
                <Button
                  size="small"
                  type="primary"
                  ghost
                  onClick={() =>
                    confirmAction('提交审核', `确认提交《${record.title}》进入审核？`, () => submitPlan(record.id))
                  }
                >
                  提交
                </Button>
              )}
              {record.status === 'PENDING' && canReview && (
                <Button size="small" type="primary" onClick={() => openReview(record)}>
                  审核
                </Button>
              )}
              {record.status === 'APPROVED' && (
                <Button
                  size="small"
                  onClick={() =>
                    confirmAction(
                      '撤回方案',
                      '撤回后该方案恢复为草稿，同组合可重新发布其他方案。确认撤回？',
                      () => withdrawPlan(record.id),
                    )
                  }
                >
                  撤回
                </Button>
              )}
              <Button size="small" onClick={() => openLogs(record)}>日志</Button>
              {editable && (
                <Button
                  size="small"
                  danger
                  onClick={() =>
                    confirmAction('删除方案', `确认删除《${record.title}》？删除后不可恢复。`, () =>
                      deletePlan(record.id),
                    )
                  }
                >
                  删除
                </Button>
              )}
            </Space>
          )
        },
      },
    ],
    [canReview],
  )

  return (
    <Card variant="borderless">
      <div className="plan-toolbar">
        <Select
          allowClear
          placeholder="学期"
          style={{ width: 210 }}
          options={SEMESTERS}
          onChange={(value) => applyFilter({ semester: value })}
        />
        <Select
          allowClear
          placeholder="学科"
          style={{ width: 110 }}
          options={SUBJECTS.map((s) => ({ value: s, label: s }))}
          onChange={(value) => applyFilter({ subject: value })}
        />
        <Select
          allowClear
          placeholder="年级"
          style={{ width: 110 }}
          options={GRADES.map((s) => ({ value: s, label: s }))}
          onChange={(value) => applyFilter({ grade: value })}
        />
        <Select
          allowClear
          placeholder="班级类型"
          style={{ width: 130 }}
          options={CLASS_TYPES.map((s) => ({ value: s, label: s }))}
          onChange={(value) => applyFilter({ classType: value })}
        />
        <Select
          allowClear
          placeholder="状态"
          style={{ width: 110 }}
          options={PLAN_STATUS.map((s) => ({ value: s.value, label: s.label }))}
          onChange={(value) => applyFilter({ status: value })}
        />
        <Input.Search
          allowClear
          placeholder="标题 / 创建人"
          style={{ width: 200 }}
          onSearch={(value) => applyFilter({ keyword: value || undefined })}
        />
        <span className="spacer" />
        <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建方案</Button>
      </div>

      <Table
        rowKey="id"
        size="middle"
        loading={loading}
        dataSource={data}
        columns={columns}
        scroll={{ x: 1080 }}
        pagination={{
          current: query.current,
          pageSize: query.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (value) => `共 ${value} 条`,
          onChange: (current, pageSize) => setQuery({ current, pageSize }),
        }}
      />

      <Modal
        title={editing ? '编辑班课方案' : '新建班课方案'}
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={save}
        confirmLoading={saving}
        okText="保存"
        width={720}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Space.Compact block>
            <Form.Item name="semester" label="学期" rules={[{ required: true }]} style={{ flex: 2, marginRight: 8 }}>
              <Select options={SEMESTERS} />
            </Form.Item>
            <Form.Item name="subject" label="学科" rules={[{ required: true }]} style={{ flex: 1, marginRight: 8 }}>
              <Select options={SUBJECTS.map((s) => ({ value: s, label: s }))} />
            </Form.Item>
            <Form.Item name="grade" label="年级" rules={[{ required: true }]} style={{ flex: 1, marginRight: 8 }}>
              <Select options={GRADES.map((s) => ({ value: s, label: s }))} />
            </Form.Item>
            <Form.Item name="classType" label="班级类型" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Select options={CLASS_TYPES.map((s) => ({ value: s, label: s }))} />
            </Form.Item>
          </Space.Compact>
          <Form.Item name="title" label="方案标题" rules={[{ required: true, message: '请填写方案标题' }]}>
            <Input maxLength={200} showCount placeholder="如：七年级数学秋季同步班课程方案" />
          </Form.Item>
          <Form.Item name="classHours" label="总课时" rules={[{ required: true, message: '请填写课时数' }]}>
            <InputNumber min={1} max={200} style={{ width: 160 }} addonAfter="课时" />
          </Form.Item>
          <Form.Item name="goal" label="教学目标">
            <Input.TextArea rows={3} maxLength={2000} showCount placeholder="本学期要达到的教学目标" />
          </Form.Item>
          <Form.Item name="outline" label="方案纲要">
            <Input.TextArea rows={5} maxLength={4000} showCount placeholder="分阶段教学安排、重难点与测评计划" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`审核方案：${reviewing?.title || ''}`}
        open={reviewOpen}
        onCancel={() => setReviewOpen(false)}
        onOk={submitReview}
        confirmLoading={saving}
        okText="提交审核结果"
        destroyOnHidden
      >
        <Form form={reviewForm} layout="vertical">
          <Form.Item name="approve" label="审核结果" rules={[{ required: true }]}>
            <Radio.Group>
              <Radio.Button value>通过并发布</Radio.Button>
              <Radio.Button value={false}>驳回</Radio.Button>
            </Radio.Group>
          </Form.Item>
          <Form.Item
            noStyle
            shouldUpdate={(prev, next) => prev.approve !== next.approve}
          >
            {({ getFieldValue }) =>
              getFieldValue('approve') === false ? (
                <Form.Item name="remark" label="驳回意见" rules={[{ required: true, message: '驳回时必须填写审核意见' }]}>
                  <Input.TextArea rows={3} maxLength={500} showCount placeholder="说明需要修改的地方" />
                </Form.Item>
              ) : (
                <Form.Item name="remark" label="审核意见（选填）">
                  <Input.TextArea rows={3} maxLength={500} showCount placeholder="通过时可补充说明" />
                </Form.Item>
              )
            }
          </Form.Item>
        </Form>
      </Modal>

      <Drawer title={`方案日志：${logTitle}`} open={logsOpen} size="large" onClose={() => setLogsOpen(false)}>
        <Descriptions column={1} size="small" bordered>
          {logs.map((log) => (
            <Descriptions.Item
              key={log.id}
              label={
                <Space orientation="vertical" size={0}>
                  <Text strong>{log.action}</Text>
                  <Text type="secondary" style={{ fontSize: 12 }}>{fmt(log.createAt)}</Text>
                </Space>
              }
            >
              <Space orientation="vertical" size={0}>
                <Text>{log.operatorName || '-'}</Text>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {log.fromStatus || '-'} → {log.toStatus || '-'}
                </Text>
                {log.remark && <Text type="secondary" style={{ fontSize: 12 }}>{log.remark}</Text>}
              </Space>
            </Descriptions.Item>
          ))}
        </Descriptions>
      </Drawer>
    </Card>
  )
}
