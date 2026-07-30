/**
 * RepoDetailPage.jsx - 题库详情 & 题目管理页面
 *
 * 功能:
 *   1. 题库信息展示（名称、类型、标签、题目总数）
 *   2. 题目列表（分页、显示是否有答案和解析）
 *   3. 新建/编辑题目：标题、类型、选项、「正确答案」、分值、「答案解析」、计分方式
 *   4. 删除题目
 *
 * 答案存储方案:
 *   题目 JSON 的 attribute 字段中扩展了 examCorrectAnswer（正确答案）、
 *   examAnalysis（答案解析）、examScore（分值）、examScoreMode（计分方式），
 *   这些字段已在后端 SurveySchema.Attribute 中定义，无需改数据库。
 *
 * URL: /repos/:id
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Table, Space, Button, Modal, Input, Select, Switch,
  Popconfirm, Typography, Tag, message, Card, Descriptions, InputNumber,
  Collapse,
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, ArrowLeftOutlined, EditOutlined,
  BookOutlined, CheckCircleOutlined, BulbOutlined,
} from '@ant-design/icons';
import { listTemplate, createTemplate, updateTemplate, deleteTemplate } from '../../api/template';
import { listRepo } from '../../api/repo';
import { QUESTION_TYPES, TYPES_WITH_OPTIONS, createQuestion } from '../../utils/surveyHelpers';

const { Title, Text, Paragraph } = Typography;

// 题目类型（含判断题）
const TEMPLATE_TYPES = [
  ...QUESTION_TYPES,
  { label: '判断题', value: 'Judge' },
];

// 判断题固定选项
const JUDGE_OPTIONS = [
  { id: 'true', type: 'Option', title: '正确', attribute: {} },
  { id: 'false', type: 'Option', title: '错误', attribute: {} },
];

// 需要展示正确答案选择题型的题目类型
const CHOICE_TYPES = ['Radio', 'Checkbox', 'Select', 'Judge'];

export default function RepoDetailPage() {
  const { id: repoId } = useParams();
  const navigate = useNavigate();

  // ---- 状态 ----
  const [repo, setRepo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [templates, setTemplates] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const pageSize = 12;

  // 题目弹窗状态
  const [modalOpen, setModalOpen] = useState(false);
  const [modalLoading, setModalLoading] = useState(false);
  const [editId, setEditId] = useState(null);
  const [qType, setQType] = useState('Radio');
  const [qTitle, setQTitle] = useState('');
  const [qOptions, setQOptions] = useState(['', '']);      // 选项文本数组
  const [qTags, setQTags] = useState('');
  const [qRequired, setQRequired] = useState(false);
  const [qCategory, setQCategory] = useState('');

  // ---- 答案 & 解析字段 ----
  const [answer, setAnswer] = useState('');           // 正确答案（单选=选项标题, 多选=逗号分隔）
  const [analysis, setAnalysis] = useState('');       // 答案解析（Markdown 纯文本）
  const [score, setScore] = useState(5);              // 分值（默认 5 分）
  const [scoreMode, setScoreMode] = useState('onlyOne'); // 计分方式

  // ---- 加载题库信息 ----
  useEffect(() => {
    (async () => {
      try {
        const res = await listRepo({ id: repoId, pageSize: 1 });
        const r = (res.data?.list || []).find((r) => r.id === repoId);
        setRepo(r || null);
      } catch { /* silent */ }
    })();
  }, [repoId]);

  // ---- 加载题目列表 ----
  const fetchTemplates = async (p = page) => {
    setLoading(true);
    try {
      const res = await listTemplate({ current: p, pageSize, repoId });
      setTemplates(res.data?.list || []);
      setTotal(res.data?.total || 0);
    } catch {
      message.error('加载题目失败');
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { fetchTemplates(); }, [repoId]); // eslint-disable-line

  // ---- 重置弹窗 ----
  const resetModal = () => {
    setEditId(null);
    setQType('Radio');
    setQTitle('');
    setQOptions(['', '']);
    setQTags('');
    setQRequired(false);
    setQCategory('');
    setAnswer('');
    setAnalysis('');
    setScore(5);
    setScoreMode('onlyOne');
  };

  // ---- 打开新建弹窗 ----
  const openCreateModal = () => { resetModal(); setModalOpen(true); };

  // ---- 打开编辑弹窗 ----
  const openEditModal = (record) => {
    setEditId(record.id);
    setQType(record.questionType || 'Radio');
    setQTitle(record.name || '');
    setQTags((record.tag || []).join(','));
    setQCategory(record.category || '');

    const tmpl = record.template;
    if (tmpl?.children?.length > 0) {
      setQOptions(tmpl.children.map((c) => c.title || ''));
    } else {
      setQOptions(['', '']);
    }
    setQRequired(tmpl?.attribute?.required || false);

    // 回填答案和解析
    const attr = tmpl?.attribute || {};
    setAnswer(attr.examCorrectAnswer || '');
    setAnalysis(attr.examAnalysis || '');
    setScore(attr.examScore || 5);
    setScoreMode(attr.examScoreMode || 'onlyOne');

    setModalOpen(true);
  };

  // ---- 保存题目 ----
  const handleSave = async () => {
    if (!qTitle.trim()) { message.warning('请输入题目标题'); return; }

    const needsOptions = TYPES_WITH_OPTIONS.includes(qType) || qType === 'Judge';
    if (needsOptions && qOptions.some((o) => !o.trim())) {
      message.warning('请填写所有选项');
      return;
    }

    setModalLoading(true);
    try {
      const templateJson = createQuestion(qType);
      templateJson.title = qTitle;

      // 构建 Attribute（包含答案和解析）
      templateJson.attribute = {
        required: qRequired,
        examCorrectAnswer: answer || undefined,
        examAnalysis: analysis || undefined,
        examScore: score,
        examScoreMode: scoreMode,
      };

      if (qType === 'Judge') {
        templateJson.children = JUDGE_OPTIONS;
      } else if (TYPES_WITH_OPTIONS.includes(qType)) {
        templateJson.children = qOptions.filter((o) => o.trim()).map((title) => ({
          id: 'opt_' + Math.random().toString(36).substring(2, 10),
          type: 'Option',
          title,
          attribute: {},
        }));
      }

      const tags = qTags ? qTags.split(',').map((t) => t.trim()).filter(Boolean) : [];
      const payload = {
        name: qTitle,
        questionType: qType,
        template: templateJson,
        tag: tags,
        category: qCategory || undefined,
        repoId,
        mode: repo?.mode === 'exam' ? 'exam' : 'survey',
      };

      if (editId) {
        await updateTemplate({ ...payload, id: editId });
        message.success('题目已更新');
      } else {
        await createTemplate(payload);
        message.success('题目已创建');
      }
      setModalOpen(false);
      fetchTemplates(page);
    } catch {
      message.error('保存失败');
    } finally {
      setModalLoading(false);
    }
  };

  // ---- 删除题目 ----
  const handleDeleteTemplate = async (id) => {
    try {
      await deleteTemplate({ id });
      message.success('已删除');
      fetchTemplates(page);
    } catch {
      message.error('删除失败');
    }
  };

  // ---- 渲染正确答案预览 ----
  const renderAnswer = (record) => {
    const attr = record.template?.attribute || {};
    const correct = attr.examCorrectAnswer;
    if (!correct) return <Text type="secondary">-</Text>;
    return <Tag color="green" icon={<CheckCircleOutlined />}>{correct}</Tag>;
  };

  // ---- 表格列 ----
  const columns = [
    { title: '#', width: 50, render: (_, __, idx) => (page - 1) * pageSize + idx + 1 },
    {
      title: '题目',
      dataIndex: 'name',
      ellipsis: true,
      render: (text, record) => {
        const attr = record.template?.attribute || {};
        const hasAnswer = !!attr.examCorrectAnswer;
        const hasAnalysis = !!attr.examAnalysis;
        return (
          <Space size={4}>
            <span>{text}</span>
            {hasAnswer && <Tag color="green" style={{ fontSize: 10, lineHeight: '16px' }}>答案</Tag>}
            {hasAnalysis && <Tag color="orange" style={{ fontSize: 10, lineHeight: '16px' }}>解析</Tag>}
          </Space>
        );
      },
    },
    {
      title: '题型', dataIndex: 'questionType', width: 90,
      render: (t) => <Tag>{TEMPLATE_TYPES.find((x) => x.value === t)?.label || t}</Tag>,
    },
    {
      title: '分值', width: 60, align: 'center',
      render: (_, r) => r.template?.attribute?.examScore || '-',
    },
    {
      title: '正确答案', width: 120, render: (_, r) => renderAnswer(r),
    },
    {
      title: '标签', dataIndex: 'tag', width: 150,
      render: (tags) => (!tags?.length ? '-' : tags.slice(0, 2).map((t) => <Tag key={t} color="blue">{t}</Tag>)),
    },
    {
      title: '操作', width: 140,
      render: (_, record) => (
        <Space size="small">
          <Button size="small" type="link" icon={<EditOutlined />} onClick={() => openEditModal(record)}>编辑</Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDeleteTemplate(record.id)} okText="删除" cancelText="取消">
            <Button size="small" type="link" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // ---- 展开行：显示答案解析 ----
  const expandedRowRender = (record) => {
    const attr = record.template?.attribute || {};
    if (!attr.examAnalysis) return null;
    return (
      <Card size="small" style={{ background: '#fffbe6', border: '1px solid #ffe58f' }}>
        <Space>
          <BulbOutlined style={{ color: '#faad14' }} />
          <Text strong>答案解析：</Text>
        </Space>
        <Paragraph style={{ marginTop: 8, marginBottom: 0, whiteSpace: 'pre-wrap' }}>
          {attr.examAnalysis}
        </Paragraph>
      </Card>
    );
  };

  return (
    <div>
      {/* ---- 顶部导航 ---- */}
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/repos')}>返回题库列表</Button>
        <Title level={4} style={{ margin: 0 }}>
          <BookOutlined style={{ marginRight: 8 }} />{repo?.name || '题库详情'}
        </Title>
      </Space>

      {/* ---- 题库信息 ---- */}
      {repo && (
        <Card size="small" style={{ marginBottom: 16 }}>
          <Descriptions size="small" column={4}>
            <Descriptions.Item label="类型">
              <Tag color={repo.mode === 'exam' ? 'red' : 'blue'}>{repo.mode === 'exam' ? '考试' : '问卷'}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="题目总数">{repo.total || 0}</Descriptions.Item>
            <Descriptions.Item label="共享">
              <Tag color={repo.shared ? 'green' : 'default'}>{repo.shared ? '是' : '否'}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="描述">{repo.description || '-'}</Descriptions.Item>
          </Descriptions>
          {repo.tag?.length > 0 && (
            <div style={{ marginTop: 8 }}>{repo.tag.map((t) => <Tag key={t} color="blue">{t}</Tag>)}</div>
          )}
        </Card>
      )}

      {/* ---- 题目列表 ---- */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
        <Text strong style={{ fontSize: 15 }}>题目列表</Text>
        <Button type="primary" size="small" icon={<PlusOutlined />} onClick={openCreateModal}>新建题目</Button>
      </div>

      <Table
        columns={columns}
        dataSource={templates}
        rowKey="id"
        loading={loading}
        size="small"
        expandable={{
          expandedRowRender,
          rowExpandable: (r) => !!(r.template?.attribute?.examAnalysis),
        }}
        pagination={{
          current: page, total, pageSize, showTotal: (t) => `共 ${t} 题`,
          onChange: (p) => { setPage(p); fetchTemplates(p); },
        }}
        scroll={{ y: 'calc(100vh - 420px)' }}
      />

      {/* ---- 新建/编辑题目弹窗 ---- */}
      <Modal
        title={editId ? '编辑题目' : '新建题目'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSave}
        confirmLoading={modalLoading}
        okText="保存"
        cancelText="取消"
        width={650}
        destroyOnHidden
      >
        <Space orientation="vertical" style={{ width: '100%' }} size="small">
          {/* -- 基础信息区 -- */}
          <Text type="secondary" style={{ fontSize: 11 }}>基础信息</Text>
          <Input value={qTitle} onChange={(e) => setQTitle(e.target.value)} placeholder="请输入题目内容" />

          <Select value={qType}
            onChange={(val) => {
              setQType(val);
              if (val === 'Judge') { setQOptions(['正确', '错误']); setAnswer(''); }
              else if (!TYPES_WITH_OPTIONS.includes(val)) { setQOptions([]); setAnswer(''); }
              else if (qOptions.length === 0) setQOptions(['', '']);
            }}
            options={TEMPLATE_TYPES} style={{ width: 160 }}
          />

          {/* -- 选项编辑区 -- */}
          {(TYPES_WITH_OPTIONS.includes(qType) || qType === 'Judge') && (
            <div>
              <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 4 }}>
                选项
              </Text>
              {qOptions.map((opt, idx) => (
                <div key={idx} style={{ display: 'flex', gap: 8, marginBottom: 4 }}>
                  <Input value={opt}
                    onChange={(e) => { const n = [...qOptions]; n[idx] = e.target.value; setQOptions(n); }}
                    placeholder={`选项 ${String.fromCharCode(65 + idx)}`}
                    disabled={qType === 'Judge'}
                  />
                  {qType !== 'Judge' && qOptions.length > 2 && (
                    <Button danger size="small" onClick={() => setQOptions(qOptions.filter((_, i) => i !== idx))}>删</Button>
                  )}
                </div>
              ))}
              {qType !== 'Judge' && (
                <Button type="dashed" size="small" block onClick={() => setQOptions([...qOptions, ''])}>+ 添加选项</Button>
              )}
            </div>
          )}

          <Space>
            <Switch checked={qRequired} onChange={setQRequired} size="small" />
            <Text type="secondary" style={{ fontSize: 11 }}>此题必填</Text>
          </Space>

          {/* -- 答案 & 解析区 -- */}
          <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 8, marginTop: 4 }}>
            <Text strong style={{ fontSize: 12, display: 'block', marginBottom: 8 }}>
              <BulbOutlined /> 答案与解析
            </Text>

            {/* 正确答案 —— 选择题型用 Select，填空题用 Input */}
            <div style={{ marginBottom: 8 }}>
              <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 2 }}>正确答案</Text>
              {CHOICE_TYPES.includes(qType) ? (
                <Select
                  value={answer || undefined}
                  onChange={setAnswer}
                  placeholder="请选择正确答案"
                  style={{ width: '100%' }}
                  allowClear
                  options={(qType === 'Judge' ? ['正确', '错误'] : qOptions.filter((o) => o.trim()))
                    .map((title, i) => ({ label: `${String.fromCharCode(65 + i)}. ${title}`, value: title }))
                  }
                />
              ) : (
                <Input
                  value={answer}
                  onChange={(e) => setAnswer(e.target.value)}
                  placeholder="输入正确答案（填空题可模糊匹配）"
                />
              )}
            </div>

            {/* 分值 + 计分方式 */}
            <div style={{ display: 'flex', gap: 12, marginBottom: 8 }}>
              <div style={{ flex: 1 }}>
                <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 2 }}>分值</Text>
                <InputNumber min={0} max={100} value={score} onChange={setScore} style={{ width: '100%' }} />
              </div>
              <div style={{ flex: 1 }}>
                <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 2 }}>计分方式</Text>
                <Select value={scoreMode} onChange={setScoreMode} style={{ width: '100%' }}
                  options={[
                    { label: '完全匹配得分', value: 'onlyOne' },
                    { label: '答对任一得分', value: 'selectCorrect' },
                    { label: '全选才得分', value: 'selectAll' },
                    { label: '人工评分', value: 'manual' },
                  ]}
                />
              </div>
            </div>

            {/* 答案解析 */}
            <div>
              <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 2 }}>答案解析</Text>
              <Input.TextArea
                value={analysis}
                onChange={(e) => setAnalysis(e.target.value)}
                placeholder="输入答案解析（支持 Markdown，如：**关键点**：xxx）"
                rows={3}
              />
            </div>
          </div>

          {/* -- 标签 / 分类 -- */}
          <Input value={qTags} onChange={(e) => setQTags(e.target.value)} placeholder="标签（逗号分隔）如：通用,单选" />
          <Input value={qCategory} onChange={(e) => setQCategory(e.target.value)} placeholder="分类（如：数学、语文）" />
        </Space>
      </Modal>
    </div>
  );
}
