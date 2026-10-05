/**
 * StudentArchivePage.jsx - 学员档案页（管理端，可打印）
 *
 * 功能:
 *   1. 目标规划表 / 学员承诺书填写与保存
 *   2. 初始档案快照 + 当前薄弱知识点 + 当日学习情况展示
 *   3. 上课记录/学习日志：按日期拉取当日学习数据生成草稿，老师编辑定稿
 *   4. 学期报告：AI 可用时润色、否则规则模板，支持定稿
 *   5. 浏览器打印（window.print + 打印样式）
 *
 * URL: /students/:studentId/archive（受 AuthGuard 保护，student:archive）
 * 被谁引用: StudentManagePage 学员列表行内「档案」按钮
 *
 * 数据流:
 *   getArchiveDetail → 档案详情；saveArchive → 规划表/承诺书/报告；
 *   getArchiveDraft → 上课记录草稿；saveArchiveRecord/deleteArchiveRecord → 记录；
 *   generateArchiveReport → 学期报告
 */

import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Button, Card, Descriptions, Form, Input, InputNumber, List, Modal, Popconfirm, Select, Space,
  Table, Tag, Typography, message,
} from 'antd';
import {
  ArrowLeftOutlined, DeleteOutlined, EditOutlined, FileTextOutlined,
  PlusOutlined, PrinterOutlined, ReloadOutlined, SaveOutlined,
} from '@ant-design/icons';
import {
  deleteArchiveRecord, generateArchiveReport, getArchiveDetail, getArchiveDraft,
  saveArchive, saveArchiveRecord,
} from '../../api/archive';
import { getArchiveCompare, getArchiveTimeline } from '../../api/growth';
import { usePermission } from '../../utils/usePermission';
import './StudentArchivePage.css';

const { Title, Text, Paragraph } = Typography;

const REPORT_STATUS_MAP = {
  none: { text: '未生成', color: 'default' },
  draft: { text: '草稿', color: 'orange' },
  final: { text: '已定稿', color: 'green' },
};

export default function StudentArchivePage() {
  const { studentId } = useParams();
  const navigate = useNavigate();
  const { can } = usePermission();
  const canEdit = can('student:archive:edit');

  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [reportLoading, setReportLoading] = useState(false);
  const [growthCompare, setGrowthCompare] = useState(null);
  const [growthTimeline, setGrowthTimeline] = useState([]);

  // 可编辑文本
  const [goalPlan, setGoalPlan] = useState('');
  const [promise, setPromise] = useState('');
  const [reportContent, setReportContent] = useState('');
  const [teacherName, setTeacherName] = useState('');
  const [reportStatus, setReportStatus] = useState('none');

  // 上课记录弹窗
  const [recordOpen, setRecordOpen] = useState(false);
  const [recordSaving, setRecordSaving] = useState(false);
  const [draftLoading, setDraftLoading] = useState(false);
  const [recordForm] = Form.useForm();

  const applyDetail = useCallback((data) => {
    setDetail(data);
    setGoalPlan(data?.goalPlan || '');
    setPromise(data?.promise || '');
    setReportContent(data?.reportContent || '');
    setTeacherName(data?.teacherName || '');
    setReportStatus(data?.reportStatus || 'none');
  }, []);

  const load = useCallback(() => {
    if (!studentId) return;
    setLoading(true);
    getArchiveDetail(studentId)
      .then((res) => applyDetail(res?.data || null))
      .catch(() => applyDetail(null))
      .finally(() => setLoading(false));
  }, [studentId, applyDetail]);

  useEffect(() => { load(); }, [load]);

  // 成长对比 + 学习轨迹（按学员 + 学科）
  useEffect(() => {
    if (!studentId) return;
    const params = { studentId, subjectId: detail?.subjectId };
    getArchiveCompare(params)
      .then((res) => setGrowthCompare(res?.data || null))
      .catch(() => setGrowthCompare(null));
    getArchiveTimeline(params)
      .then((res) => setGrowthTimeline(res?.data || []))
      .catch(() => setGrowthTimeline([]));
  }, [studentId, detail?.subjectId]);

  // ---- 保存档案主体 ----
  const handleSave = (extra = {}) => {
    if (!detail) return;
    setSaving(true);
    saveArchive({
      id: detail.id,
      studentId,
      schoolYear: detail.schoolYear,
      semester: detail.semester,
      termLabel: detail.termLabel,
      subjectId: detail.subjectId,
      status: detail.status,
      teacherName,
      goalPlan,
      promise,
      reportContent,
      reportStatus,
      ...extra,
    })
      .then((res) => {
        message.success('档案已保存');
        applyDetail(res?.data || detail);
      })
      .catch(() => {})
      .finally(() => setSaving(false));
  };

  // ---- 生成学期报告 ----
  const handleGenerateReport = () => {
    setReportLoading(true);
    generateArchiveReport(studentId)
      .then((res) => {
        message.success('学期报告已生成');
        applyDetail(res?.data || detail);
      })
      .catch(() => {})
      .finally(() => setReportLoading(false));
  };

  // ---- 上课记录 ----
  const openRecord = (record = null) => {
    if (record) {
      recordForm.setFieldsValue(record);
    } else {
      recordForm.resetFields();
      recordForm.setFieldsValue({
        recordDate: new Date().toISOString().slice(0, 10),
        status: 'draft',
      });
    }
    setRecordOpen(true);
  };

  const handleDraft = () => {
    const date = recordForm.getFieldValue('recordDate');
    if (!date) {
      message.warning('请先选择上课日期');
      return;
    }
    setDraftLoading(true);
    getArchiveDraft(studentId, date)
      .then((res) => {
        const d = res?.data || {};
        recordForm.setFieldsValue({
          title: d.title || recordForm.getFieldValue('title'),
          studySummary: d.studySummary || '',
          durationMinutes: d.durationMinutes || 0,
          points: d.points || 0,
          coins: d.coins || 0,
          strengthenedKps: (d.strengthenedKps || []).join('、'),
          weaknesses: (d.weakNames || []).join('、'),
        });
        message.success('已拉取当日学习数据，请核对后定稿');
      })
      .catch(() => {})
      .finally(() => setDraftLoading(false));
  };

  const handleRecordSave = () => {
    recordForm.validateFields().then((values) => {
      setRecordSaving(true);
      saveArchiveRecord({ ...values, studentId })
        .then(() => {
          message.success('上课记录已保存');
          setRecordOpen(false);
          load();
        })
        .catch(() => {})
        .finally(() => setRecordSaving(false));
    });
  };

  const handleRecordDelete = (record) => {
    deleteArchiveRecord(record.id).then(() => {
      message.success('记录已删除');
      load();
    });
  };

  const reportMeta = REPORT_STATUS_MAP[reportStatus] || REPORT_STATUS_MAP.none;

  const recordColumns = [
    { title: '上课日期', dataIndex: 'recordDate', width: 120 },
    { title: '主题', dataIndex: 'title', width: 160, render: (v) => v || '-' },
    {
      title: '学习情况',
      dataIndex: 'studySummary',
      render: (v, r) => (
        <div>
          <div className="archive-record-summary">{v || '-'}</div>
          {r.homework && <Text type="secondary">作业：{r.homework}</Text>}
        </div>
      ),
    },
    { title: '时长(分)', dataIndex: 'durationMinutes', width: 90, render: (v) => v ?? '-' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (v) => (v === 'final' ? <Tag color="green">已定稿</Tag> : <Tag color="orange">草稿</Tag>),
    },
    {
      title: '操作',
      key: 'action',
      width: 130,
      className: 'no-print',
      render: (_, record) => (canEdit ? (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openRecord(record)}>编辑</Button>
          <Popconfirm title="确定删除该记录？" onConfirm={() => handleRecordDelete(record)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ) : '-'),
    },
  ];

  return (
    <div className="archive-page">
      {/* ---- 工具栏（打印隐藏） ---- */}
      <div className="archive-toolbar no-print">
        <div className="archive-toolbar-left">
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/students')}>返回列表</Button>
          <Title level={4} className="archive-title">学员学习档案</Title>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
          <Button icon={<PrinterOutlined />} onClick={() => window.print()}>打印</Button>
          {canEdit && (
            <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={() => handleSave()}>
              保存档案
            </Button>
          )}
        </Space>
      </div>

      {/* ---- 档案抬头 ---- */}
      <Card className="archive-section" loading={loading}>
        <Descriptions
          title="学员学习档案"
          column={3}
          items={[
            { key: 'name', label: '姓名', children: detail?.studentName || '-' },
            { key: 'no', label: '学号', children: detail?.studentNo || '-' },
            { key: 'class', label: '学期', children: detail?.termLabel ? `${detail.schoolYear} 学年 ${detail.termLabel}` : '-' },
            {
              key: 'teacher',
              label: '负责老师',
              children: canEdit
                ? <Input size="small" value={teacherName} onChange={(e) => setTeacherName(e.target.value)} placeholder="选填" style={{ width: 140 }} />
                : (teacherName || '-'),
            },
            {
              key: 'report',
              label: '学期报告',
              children: <Tag color={reportMeta.color}>{reportMeta.text}</Tag>,
            },
          ]}
        />
      </Card>

      {/* ---- 一、初始档案 ---- */}
      <Card title="一、初始档案（知识点检测 / 薄弱点）" className="archive-section">
        <Paragraph>
          <Text strong>初始薄弱知识点：</Text>
        </Paragraph>
        <div className="archive-kp-tags">
          {detail?.initialWeakPoints?.length
            ? detail.initialWeakPoints.map((n) => <Tag key={n} color="purple">{n}</Tag>)
            : <Text className="archive-empty">建案时暂无检测薄弱点</Text>}
        </div>
        <Paragraph>
          <Text strong>当前薄弱知识点：</Text>
        </Paragraph>
        <div className="archive-kp-tags">
          {detail?.weakPoints?.length
            ? detail.weakPoints.map((w) => (
              <Tag key={w.kpId} color="red">{w.name || w.kpId}（掌握度 {w.mastery}%）</Tag>
            ))
            : <Text className="archive-empty">暂无薄弱知识点</Text>}
        </div>
        <Paragraph>
          <Text strong>当日学习情况：</Text>
        </Paragraph>
        <Paragraph className="archive-record-summary">
          {detail?.today?.content || <Text className="archive-empty">今日暂无学习记录</Text>}
        </Paragraph>
      </Card>

      {/* ---- 二、成长对比（基线 vs 当前） ---- */}
      <Card title="二、成长对比（基线 vs 当前）" className="archive-section">
        {growthCompare?.hasBaseline ? (
          <>
            <List
              size="small"
              header={(
                <span>
                  学前检测正确率 {growthCompare.baselineAccuracy ?? '-'}% ·
                  基线薄弱 {growthCompare.baselineWeakCount || 0} 个 ·
                  已攻克 {growthCompare.resolvedCount || 0} 个 ·
                  仍需巩固 {growthCompare.remainingCount || 0} 个 ·
                  新增薄弱 {growthCompare.newlyWeakCount || 0} 个 ·
                  学习事件 {growthCompare.eventCount || 0} 次
                </span>
              )}
              dataSource={growthCompare.deltas || []}
              locale={{ emptyText: '暂无基线薄弱点' }}
              renderItem={(d) => (
                <List.Item>
                  <span>{d.name || d.kpId}</span>
                  <span>
                    {d.baselineAccuracy ?? 0}% → {d.currentMastery ?? 0}%
                    <Tag color={d.resolved ? 'green' : 'red'} style={{ marginLeft: 8 }}>
                      {d.resolved ? '已攻克' : '巩固中'}
                    </Tag>
                  </span>
                </List.Item>
              )}
            />
          </>
        ) : (
          <Text className="archive-empty">尚未定格成长基线（学员完成本学期首次全面检测后生成）</Text>
        )}
      </Card>

      {/* ---- 三、学习轨迹 ---- */}
      <Card title="三、学习轨迹" className="archive-section">
        {growthTimeline?.length ? (
          <List
            size="small"
            dataSource={growthTimeline}
            renderItem={(e) => (
              <List.Item>
                <List.Item.Meta
                  title={`${e.eventDate || ''} ${e.title || e.eventType || ''}`}
                  description={[
                    e.chapter,
                    e.questionCount ? `答题 ${e.correctCount || 0}/${e.questionCount} 题（${e.accuracy || 0}%）` : null,
                    e.knowledgePoints?.length ? e.knowledgePoints.join('、') : null,
                    e.points ? `积分 +${e.points}` : null,
                    e.coins ? `学币 +${e.coins}` : null,
                  ].filter(Boolean).join(' · ')}
                />
              </List.Item>
            )}
          />
        ) : (
          <Text className="archive-empty">暂无学习轨迹</Text>
        )}
      </Card>

      {/* ---- 四、目标规划表 ---- */}
      <Card title="四、本学期目标规划表" className="archive-section">
        {canEdit ? (
          <Input.TextArea
            value={goalPlan}
            onChange={(e) => setGoalPlan(e.target.value)}
            autoSize={{ minRows: 4, maxRows: 12 }}
            placeholder="填写本学期学习目标、阶段计划与达成标准"
            maxLength={2000}
            showCount
          />
        ) : (
          <Paragraph className="archive-report-text">{goalPlan || <Text className="archive-empty">暂无内容</Text>}</Paragraph>
        )}
      </Card>

      {/* ---- 五、承诺书 ---- */}
      <Card title="五、学员承诺书" className="archive-section">
        {canEdit ? (
          <Input.TextArea
            value={promise}
            onChange={(e) => setPromise(e.target.value)}
            autoSize={{ minRows: 4, maxRows: 12 }}
            placeholder="填写学员承诺内容"
            maxLength={2000}
            showCount
          />
        ) : (
          <Paragraph className="archive-report-text">{promise || <Text className="archive-empty">暂无内容</Text>}</Paragraph>
        )}
        <div className="archive-sign">
          <span>学员签名：___________</span>
          <span>家长签名：___________</span>
          <span>日期：___________</span>
        </div>
      </Card>

      {/* ---- 六、上课记录 / 学习日志 ---- */}
      <Card
        title="六、上课记录 / 学习日志"
        className="archive-section"
        extra={canEdit && (
          <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => openRecord()}>
            新增记录
          </Button>
        )}
      >
        <Table
          rowKey="id"
          size="small"
          pagination={false}
          columns={recordColumns}
          dataSource={detail?.records || []}
          locale={{ emptyText: '暂无上课记录' }}
        />
      </Card>

      {/* ---- 七、学期报告 ---- */}
      <Card
        title="七、学期报告"
        className="archive-section"
        extra={canEdit && (
          <Space className="no-print">
            <Button size="small" icon={<FileTextOutlined />} loading={reportLoading} onClick={handleGenerateReport}>
              生成报告
            </Button>
            <Button size="small" type="primary" loading={saving} onClick={() => handleSave({ reportStatus: 'final' })}>
              定稿
            </Button>
          </Space>
        )}
      >
        {canEdit ? (
          <Input.TextArea
            value={reportContent}
            onChange={(e) => setReportContent(e.target.value)}
            autoSize={{ minRows: 6, maxRows: 20 }}
            placeholder="点击「生成报告」由系统汇总，或手工填写"
            maxLength={4000}
            showCount
          />
        ) : (
          <Paragraph className="archive-report-text">{reportContent || <Text className="archive-empty">暂无内容</Text>}</Paragraph>
        )}
      </Card>

      {/* ---- 上课记录弹窗 ---- */}
      <Modal
        title="上课记录"
        open={recordOpen}
        width={720}
        onOk={handleRecordSave}
        onCancel={() => setRecordOpen(false)}
        confirmLoading={recordSaving}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={recordForm} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }}>
          <Form.Item label="快速填充" className="no-print">
            <Button size="small" loading={draftLoading} onClick={handleDraft}>拉取当日学习数据生成草稿</Button>
          </Form.Item>
          <Form.Item name="recordDate" label="上课日期" rules={[{ required: true, message: '请选择上课日期' }]}>
            <Input type="date" style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="title" label="主题">
            <Input placeholder="本次课主题" maxLength={100} />
          </Form.Item>
          <Form.Item name="studySummary" label="学习情况">
            <Input.TextArea autoSize={{ minRows: 2, maxRows: 6 }} maxLength={2000} />
          </Form.Item>
          <Form.Item name="solvedProblems" label="解决的问题">
            <Input.TextArea autoSize={{ minRows: 1, maxRows: 4 }} maxLength={1000} />
          </Form.Item>
          <Form.Item name="strengthenedKps" label="强化知识点">
            <Input placeholder="多个用、分隔" maxLength={500} />
          </Form.Item>
          <Form.Item name="weaknesses" label="暴露的弱点">
            <Input placeholder="多个用、分隔" maxLength={500} />
          </Form.Item>
          <Form.Item name="homework" label="课后作业">
            <Input.TextArea autoSize={{ minRows: 1, maxRows: 4 }} maxLength={1000} />
          </Form.Item>
          <Form.Item name="teacherComment" label="教师寄语">
            <Input.TextArea autoSize={{ minRows: 1, maxRows: 4 }} maxLength={1000} />
          </Form.Item>
          <Form.Item label="时长 / 积分 / 学币">
            <Space>
              <Form.Item name="durationMinutes" noStyle>
                <InputNumber min={0} addonAfter="分钟" style={{ width: 140 }} />
              </Form.Item>
              <Form.Item name="points" noStyle>
                <InputNumber addonAfter="积分" style={{ width: 140 }} />
              </Form.Item>
              <Form.Item name="coins" noStyle>
                <InputNumber addonAfter="学币" style={{ width: 140 }} />
              </Form.Item>
            </Space>
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue="draft">
            <Select
              style={{ width: 200 }}
              options={[{ value: 'draft', label: '草稿' }, { value: 'final', label: '定稿' }]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
