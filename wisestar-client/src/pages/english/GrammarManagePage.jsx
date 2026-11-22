/**
 * GrammarManagePage.jsx - 英语语法管理页（后台管理端）
 *
 * 功能:
 *   1. 语法列表（按版本/年级/册别/单元/小节/标题筛选）
 *   2. 语法 CRUD（标题/讲解/结构/课本原文/例句/例题/排序）
 *   3. AI 批量补齐单元语法点：先预览，再确认入库
 *
 * URL: /english/grammar（受 AuthGuard 保护，权限 english:grammar:list）
 * 被谁引用：App.jsx 路由表
 *
 * 数据流:
 *   GET  /api/english/grammar/list   → 语法分页列表
 *   POST /api/english/grammar/save   → 新增/更新语法
 *   POST /api/english/grammar/delete → 删除语法
 *   POST /api/english/grammar/ai-enrich → AI 批量补齐（仅预览）
 *
 * 说明: textbook/samples/exercises 以 JSON 字符串入库，表单内用动态行编辑。
 */

import { useEffect, useState } from 'react';
import { Table, Space, Button, Input, Select, Modal, Form, message, Typography, Alert, Tag } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, RobotOutlined } from '@ant-design/icons';
import { usePermission } from '../../utils/usePermission';
import { getGrammars, saveGrammar, deleteGrammar, enrichGrammar } from '../../api/englishAdmin';

const { Title } = Typography;

/** 解析后端 JSON 字符串列，失败返回空数组 */
function parseArray(raw) {
  if (!raw) return [];
  try {
    const value = typeof raw === 'string' ? JSON.parse(raw) : raw;
    return Array.isArray(value) ? value : [];
  } catch {
    return [];
  }
}

function toTextbookRows(raw) {
  return parseArray(raw).map((item) => (typeof item === 'string'
    ? { en: item, zh: '', source: '' }
    : { en: item.en || item.text || '', zh: item.zh || '', source: item.source || '' }));
}

function toSampleRows(raw) {
  return parseArray(raw).map((item) => (typeof item === 'string'
    ? { en: item, zh: '', note: '' }
    : { en: item.en || item.text || '', zh: item.zh || '', note: item.note || item.explanation || '' }));
}

function toExerciseRows(raw) {
  return parseArray(raw).map((item) => ({
    type: item.type === 'build' || (!item.options?.length) ? 'build' : 'choice',
    q: item.q || item.stem || '',
    options: Array.isArray(item.options) ? item.options.join('\n') : '',
    answer: Array.isArray(item.answer) ? item.answer.join(' / ') : (item.answer || ''),
    explanation: item.explanation || '',
  }));
}

/** 表单动态行转回后端 JSON 字符串 */
function rowsToJson(rows, kind) {
  const list = (rows || [])
    .filter((row) => row && String(row.en || row.q || '').trim())
    .map((row) => {
      if (kind === 'textbook') {
        return { en: row.en.trim(), zh: (row.zh || '').trim(), source: (row.source || '').trim() };
      }
      if (kind === 'samples') {
        return { en: row.en.trim(), zh: (row.zh || '').trim(), note: (row.note || '').trim() };
      }
      const options = String(row.options || '')
        .split(/[\n|]/)
        .map((option) => option.trim())
        .filter(Boolean);
      return {
        type: row.type || (options.length ? 'choice' : 'build'),
        q: (row.q || '').trim(),
        options,
        answer: (row.answer || '').trim(),
        explanation: (row.explanation || '').trim(),
      };
    });
  return JSON.stringify(list);
}

const VERSION_OPTIONS = ['人教版', '苏教版', '北师大版', '外研版'].map((v) => ({ value: v, label: v }));
const GRADE_OPTIONS = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级', '七年级', '八年级', '九年级'].map((g) => ({ value: g, label: g }));
const TERM_OPTIONS = ['上册', '下册'].map((t) => ({ value: t, label: t }));

export default function GrammarManagePage() {
  const { can } = usePermission();
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const [version, setVersion] = useState('');
  const [grade, setGrade] = useState('');
  const [term, setTerm] = useState('');
  const [unit, setUnit] = useState('');
  const [section, setSection] = useState('');
  const [title, setTitle] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();

  const [enrichOpen, setEnrichOpen] = useState(false);
  const [enrichLoading, setEnrichLoading] = useState(false);
  const [previews, setPreviews] = useState([]);
  const [aiForm] = Form.useForm();

  const loadList = () => {
    setLoading(true);
    getGrammars({
      current,
      pageSize,
      ...(version && { version }),
      ...(grade && { grade }),
      ...(term && { term }),
      ...(unit && { unit }),
      ...(section && { section }),
      ...(title && { title }),
    })
      .then((res) => {
        setList(res.data?.list || []);
        setTotal(res.data?.total || 0);
      })
      .catch(() => message.error('加载失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current, pageSize]);

  const handleReset = () => {
    setVersion('');
    setGrade('');
    setTerm('');
    setUnit('');
    setSection('');
    setTitle('');
    setCurrent(1);
  };

  const openModal = (record = null) => {
    setEditing(record);
    setModalOpen(true);
    if (record) {
      form.setFieldsValue({
        ...record,
        textbookRows: toTextbookRows(record.textbook),
        sampleRows: toSampleRows(record.samples).length ? toSampleRows(record.samples) : toSampleRows(record.examples),
        exerciseRows: toExerciseRows(record.exercises),
      });
    } else {
      form.resetFields();
      form.setFieldsValue({
        version: version || '外研版',
        grade: grade || '三年级',
        term: term || '上册',
        unit: unit || '',
        section: section || '',
        sort: 0,
        textbookRows: [],
        sampleRows: [],
        exerciseRows: [],
      });
    }
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      const { textbookRows, sampleRows, exerciseRows, ...rest } = values;
      const samples = rowsToJson(sampleRows, 'samples');
      const payload = {
        ...rest,
        id: editing?.id,
        textbook: rowsToJson(textbookRows, 'textbook'),
        samples,
        examples: samples,
        exercises: rowsToJson(exerciseRows, 'exercises'),
      };
      saveGrammar(payload)
        .then(() => {
          message.success(editing?.id ? '编辑成功' : '保存成功');
          setModalOpen(false);
          loadList();
        })
        .catch(() => {});
    });
  };

  const handleDelete = (id) => {
    deleteGrammar(id)
      .then(() => {
        message.success('删除成功');
        loadList();
      })
      .catch(() => {});
  };

  const openEnrich = () => {
    aiForm.setFieldsValue({
      version: version || '外研版',
      grade: grade || '三年级',
      term: term || '上册',
      unit: unit || '',
    });
    setPreviews([]);
    setEnrichOpen(true);
  };

  const handleEnrich = () => {
    aiForm.validateFields().then((values) => {
      setEnrichLoading(true);
      enrichGrammar(values)
        .then((res) => setPreviews(res.data || []))
        .catch(() => {})
        .finally(() => setEnrichLoading(false));
    });
  };

  // 入库成功即从预览里移除，避免重复点击或重试时再次新增
  const savePreview = (item) => saveGrammar(item).then(() => {
    message.success(`已入库：${item.title}`);
    setPreviews((list) => list.filter((preview) => preview !== item));
    loadList();
  });

  const saveAllPreviews = async () => {
    if (previews.length === 0) return;
    setEnrichLoading(true);
    try {
      for (const item of [...previews]) {
        // eslint-disable-next-line no-await-in-loop
        await savePreview(item);
      }
      message.success('预览已全部入库');
      setEnrichOpen(false);
    } catch {
      // 单条失败由拦截器提示，保留未入库预览便于重试
    } finally {
      setEnrichLoading(false);
    }
  };

  const columns = [
    { title: '标题', dataIndex: 'title', width: 200, ellipsis: true },
    { title: '版本', dataIndex: 'version', width: 80 },
    { title: '年级', dataIndex: 'grade', width: 80 },
    { title: '册别', dataIndex: 'term', width: 70 },
    { title: '单元', dataIndex: 'unit', width: 180, ellipsis: true },
    { title: '小节', dataIndex: 'section', width: 140, ellipsis: true },
    { title: '排序', dataIndex: 'sort', width: 70 },
    {
      title: '更新时间',
      dataIndex: 'updateAt',
      width: 170,
      render: (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '—'),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space>
          {can('english:grammar:update') && (
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(record)}>编辑</Button>
          )}
          {can('english:grammar:delete') && (
            <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>删除</Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ maxWidth: 1400, margin: '0 auto', padding: 20 }}>
      <Title level={4}>语法管理</Title>

      <Space style={{ marginBottom: 16 }}>
        {can('english:grammar:create') && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增语法</Button>
        )}
        {can('english:grammar:update') && (
          <Button icon={<RobotOutlined />} onClick={openEnrich}>AI 批量补齐</Button>
        )}
      </Space>

      <Space wrap style={{ marginBottom: 16 }}>
        <Select placeholder="教材版本" allowClear style={{ width: 120 }} value={version || undefined} onChange={setVersion} options={VERSION_OPTIONS} />
        <Select placeholder="年级" allowClear style={{ width: 100 }} value={grade || undefined} onChange={setGrade} options={GRADE_OPTIONS} />
        <Select placeholder="册别" allowClear style={{ width: 90 }} value={term || undefined} onChange={setTerm} options={TERM_OPTIONS} />
        <Input placeholder="单元" allowClear style={{ width: 150 }} value={unit} onChange={(e) => setUnit(e.target.value)} onPressEnter={loadList} />
        <Input placeholder="小节" allowClear style={{ width: 120 }} value={section} onChange={(e) => setSection(e.target.value)} onPressEnter={loadList} />
        <Input placeholder="标题关键字" allowClear style={{ width: 150 }} value={title} onChange={(e) => setTitle(e.target.value)} onPressEnter={loadList} />
        <Button type="primary" onClick={loadList}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
      </Space>

      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={{
          current,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (c, s) => {
            setCurrent(c);
            setPageSize(s);
          },
        }}
      />

      <Modal
        title={editing?.id ? '编辑语法' : '新增语法'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        width={900}
        styles={{ body: { maxHeight: '68vh', overflowY: 'auto' } }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入语法标题' }]}>
            <Input placeholder="如 be 动词的用法" />
          </Form.Item>
          <Form.Item name="content" label="详细讲解（可换行分段）">
            <Input.TextArea rows={4} placeholder="说明什么时候用、怎么变、常见错误" />
          </Form.Item>
          <Form.Item name="structure" label="结构 / 句式模板">
            <Input.TextArea rows={2} placeholder="如：主语 + be 动词 + 表语" />
          </Form.Item>

          <Form.Item label="课本原文（建议 3 句）">
            <Form.List name="textbookRows">
              {(fields, { add, remove }) => (
                <Space direction="vertical" style={{ width: '100%' }}>
                  {fields.map((field) => (
                    <Space key={field.key} align="baseline" wrap>
                      <Form.Item name={[field.name, 'en']} noStyle><Input style={{ width: 300 }} placeholder="英文原句" /></Form.Item>
                      <Form.Item name={[field.name, 'zh']} noStyle><Input style={{ width: 220 }} placeholder="中文" /></Form.Item>
                      <Form.Item name={[field.name, 'source']} noStyle><Input style={{ width: 150 }} placeholder="出处（可选）" /></Form.Item>
                      <Button size="small" danger onClick={() => remove(field.name)}>删除</Button>
                    </Space>
                  ))}
                  <Button size="small" onClick={() => add({})}>+ 添加课本原句</Button>
                </Space>
              )}
            </Form.List>
          </Form.Item>

          <Form.Item label="典型例句（建议 3 句，带规律说明）">
            <Form.List name="sampleRows">
              {(fields, { add, remove }) => (
                <Space direction="vertical" style={{ width: '100%' }}>
                  {fields.map((field) => (
                    <Space key={field.key} align="baseline" wrap>
                      <Form.Item name={[field.name, 'en']} noStyle><Input style={{ width: 300 }} placeholder="英文例句" /></Form.Item>
                      <Form.Item name={[field.name, 'zh']} noStyle><Input style={{ width: 220 }} placeholder="中文" /></Form.Item>
                      <Form.Item name={[field.name, 'note']} noStyle><Input style={{ width: 220 }} placeholder="规律说明" /></Form.Item>
                      <Button size="small" danger onClick={() => remove(field.name)}>删除</Button>
                    </Space>
                  ))}
                  <Button size="small" onClick={() => add({})}>+ 添加例句</Button>
                </Space>
              )}
            </Form.List>
          </Form.Item>

          <Form.Item label="例题（建议 3 题：选择 + 连词成句混排）">
            <Form.List name="exerciseRows">
              {(fields, { add, remove }) => (
                <Space direction="vertical" style={{ width: '100%' }}>
                  {fields.map((field) => (
                    <div key={field.key} style={{ border: '1px solid #eee', borderRadius: 8, padding: 10 }}>
                      <Space align="baseline" wrap>
                        <Form.Item name={[field.name, 'type']} noStyle>
                          <Select style={{ width: 130 }} options={[{ value: 'choice', label: '选正确形式' }, { value: 'build', label: '连词成句' }]} />
                        </Form.Item>
                        <Form.Item name={[field.name, 'q']} noStyle><Input style={{ width: 420 }} placeholder="题干 / 中文提示" /></Form.Item>
                        <Button size="small" danger onClick={() => remove(field.name)}>删除</Button>
                      </Space>
                      <Form.Item name={[field.name, 'options']} noStyle>
                        <Input.TextArea rows={2} style={{ marginTop: 8 }} placeholder="选项，每行一个；连词成句可留空" />
                      </Form.Item>
                      <Space align="baseline" wrap style={{ marginTop: 8 }}>
                        <Form.Item name={[field.name, 'answer']} noStyle><Input style={{ width: 420 }} placeholder="答案（选择填选项原文；连词成句填完整句子）" /></Form.Item>
                        <Form.Item name={[field.name, 'explanation']} noStyle><Input style={{ width: 420 }} placeholder="答案解析" /></Form.Item>
                      </Space>
                    </div>
                  ))}
                  <Button size="small" onClick={() => add({ type: 'choice' })}>+ 添加例题</Button>
                </Space>
              )}
            </Form.List>
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item name="version" label="版本" rules={[{ required: true, message: '请选择版本' }]}>
              <Select options={VERSION_OPTIONS} />
            </Form.Item>
            <Form.Item name="grade" label="年级" rules={[{ required: true, message: '请选择年级' }]}>
              <Select options={GRADE_OPTIONS} />
            </Form.Item>
            <Form.Item name="term" label="册别">
              <Select allowClear options={TERM_OPTIONS} />
            </Form.Item>
            <Form.Item name="sort" label="排序">
              <Input type="number" />
            </Form.Item>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item name="unit" label="单元">
              <Input placeholder="如 Unit 1 Helping at home" />
            </Form.Item>
            <Form.Item name="section" label="小节">
              <Input placeholder="如 Part A Let's talk" />
            </Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal
        title="AI 批量补齐语法点"
        open={enrichOpen}
        width={980}
        onCancel={() => setEnrichOpen(false)}
        footer={[
          <Button key="close" onClick={() => setEnrichOpen(false)}>关闭</Button>,
          <Button
            key="save"
            type="primary"
            disabled={previews.length === 0}
            loading={enrichLoading}
            onClick={saveAllPreviews}
          >
            全部入库（{previews.length}）
          </Button>,
        ]}
      >
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message="AI 只负责生成预览，确认无误后再入库；同标题会覆盖已有语法点，新标题会新增。"
        />
        <Form form={aiForm} layout="inline" style={{ marginBottom: 12 }}>
          <Form.Item name="version" rules={[{ required: true }]}><Select style={{ width: 110 }} options={VERSION_OPTIONS} /></Form.Item>
          <Form.Item name="grade" rules={[{ required: true }]}><Select style={{ width: 100 }} options={GRADE_OPTIONS} /></Form.Item>
          <Form.Item name="term"><Select style={{ width: 90 }} allowClear options={TERM_OPTIONS} /></Form.Item>
          <Form.Item name="unit" rules={[{ required: true, message: '请填写单元' }]}>
            <Input style={{ width: 240 }} placeholder="单元，如 Unit 1 Helping at home" />
          </Form.Item>
          <Button type="primary" icon={<RobotOutlined />} loading={enrichLoading} onClick={handleEnrich}>生成预览</Button>
        </Form>

        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          {previews.map((item, index) => {
            const samples = parseArray(item.samples);
            const textbook = parseArray(item.textbook);
            const exercises = parseArray(item.exercises);
            return (
              <div key={`${item.title}-${index}`} style={{ border: '1px solid #e6eef5', borderRadius: 10, padding: 12 }}>
                <Space wrap>
                  <Tag color={item.id ? 'blue' : 'green'}>{item.id ? '覆盖' : '新增'}</Tag>
                  <Typography.Text strong>{item.title}</Typography.Text>
                  <Typography.Text type="secondary">课本 {textbook.length} 句 · 例句 {samples.length} 句 · 例题 {exercises.length} 题</Typography.Text>
                  <Button size="small" type="link" onClick={() => savePreview(item)}>单独入库</Button>
                </Space>
                {item.structure && <div style={{ marginTop: 6, color: '#1c6b45' }}>结构：{item.structure}</div>}
                <div style={{ marginTop: 6, color: '#43647a', whiteSpace: 'pre-wrap' }}>{item.content}</div>
                {samples.length > 0 && (
                  <ul style={{ margin: '6px 0 0', paddingLeft: 20 }}>
                    {samples.map((sample, i) => (
                      <li key={`${item.title}-s-${i}`}>{sample.en} {sample.zh && <span style={{ color: '#8aa4bd' }}>{sample.zh}</span>}</li>
                    ))}
                  </ul>
                )}
              </div>
            );
          })}
          {previews.length === 0 && !enrichLoading && (
            <Typography.Text type="secondary">选择教材维度并填写单元后，点击「生成预览」。</Typography.Text>
          )}
        </Space>
      </Modal>
    </div>
  );
}
