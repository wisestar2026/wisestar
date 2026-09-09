/**
 * WordAiManagePage.jsx - 英语 AI 单元内容生成页（AI 内容生成）
 *
 * 功能（URL 不变：/english/word-ai，菜单/权限保持既有 english:word:*）:
 *   1. 选择 版本/年级/单元(可加主题) 调用大模型生成整套单元学习内容
 *      （单词表 + 语法讲解 + 例句 + 配套单选题），先预览后入库
 *   2. 保存为内容包（同 版本+年级+单元 覆盖）
 *   3. 同步到正式词库 t_english_word 与语法库 t_english_grammar
 *   4. 历史内容包：筛选/详情查看/再次同步/删除
 *
 * 后端接口:
 *   POST /api/english/ai-pack/generate?version=&grade=&unit=&topic=
 *   POST /api/english/ai-pack/save            body {version,grade,unit,topic,title,content,wordCount}
 *   POST /api/english/ai-pack/sync?id=
 *   GET  /api/english/ai-pack/list?version=&grade=&unit=&current=&pageSize=
 *   GET  /api/english/ai-pack/detail?id=
 *   POST /api/english/ai-pack/delete?id=
 */

import { useEffect, useMemo, useState } from 'react';
import {
  Card, Button, Select, Input, Space, Table, Tag, Modal, Typography,
  message, Popconfirm, Divider, Alert,
} from 'antd';
import {
  ThunderboltOutlined, SaveOutlined, SyncOutlined, EyeOutlined,
  DeleteOutlined, ReloadOutlined,
} from '@ant-design/icons';
import request from '../../api/request';

const VERSIONS = ['人教版', '苏教版', '北师大版', '外研版'];
const GRADES = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级'];
const UNITS = ['Unit 1', 'Unit 2', 'Unit 3', 'Unit 4', 'Unit 5', 'Unit 6', 'Unit 7', 'Unit 8'];

const { Paragraph, Text } = Typography;

/** 后端返回 { code, data }，统一取 data */
const dataOf = (res) => res?.data;

/** 解析内容包 content 为结构化对象（words / grammar） */
function parseContent(view) {
  if (!view || !view.content) return null;
  try {
    const obj = JSON.parse(view.content);
    if (obj && typeof obj === 'object') return obj;
  } catch (e) {
    // 容错：解析失败返回 null，页面展示不可预览提示
  }
  return null;
}

/**
 * 内容预览区块：单词表 + 语法讲解 + 配套练习。
 * 供「生成预览」与「历史包详情弹窗」复用。
 */
function PackPreview({ view }) {
  const content = useMemo(() => parseContent(view), [view]);
  if (!content) {
    return <Alert type="warning" showIcon message="该内容包无可预览的结构化内容" />;
  }

  const words = Array.isArray(content.words) ? content.words : [];
  const grammar = content.grammar && typeof content.grammar === 'object' ? content.grammar : {};
  const examples = Array.isArray(grammar.examples) ? grammar.examples : [];
  const exercises = Array.isArray(grammar.exercises) ? grammar.exercises : [];

  const wordColumns = [
    { title: '单词', dataIndex: 'spell', width: 130 },
    { title: '音标', dataIndex: 'phonetic', width: 140 },
    { title: '释义', dataIndex: 'meaning', width: 200 },
    {
      title: '例句',
      dataIndex: 'example',
      render: (text, record) => (
        <div>
          <div>{text}</div>
          {record.exampleZh && <div style={{ color: '#888', fontSize: 12 }}>{record.exampleZh}</div>}
        </div>
      ),
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 8 }}>
        <Typography.Title level={5} style={{ margin: 0 }}>
          {content.title || '单元学习内容'}
        </Typography.Title>
        {words.length > 0 && <Tag color="blue">单词 {words.length} 个</Tag>}
        {grammar.title && <Tag color="purple">{grammar.title}</Tag>}
      </Space>

      <Divider style={{ margin: '12px 0' }} orientation="left" plain>
        核心词汇
      </Divider>
      <Table
        rowKey={(row, i) => `${row.spell}-${i}`}
        size="small"
        dataSource={words}
        columns={wordColumns}
        pagination={false}
        locale={{ emptyText: '未生成单词' }}
      />

      <Divider style={{ margin: '12px 0' }} orientation="left" plain>
        语法讲解
      </Divider>
      {grammar.title && <Paragraph strong style={{ marginBottom: 4 }}>{grammar.title}</Paragraph>}
      {grammar.content ? (
        <Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 8 }}>{grammar.content}</Paragraph>
      ) : (
        <Text type="secondary">未生成语法讲解</Text>
      )}
      {examples.length > 0 && (
        <ul style={{ paddingLeft: 18, margin: 0 }}>
          {examples.map((ex, i) => (
            <li key={i} style={{ marginBottom: 4 }}>
              {ex.en}
              {ex.zh && <div style={{ color: '#888', fontSize: 12 }}>{ex.zh}</div>}
            </li>
          ))}
        </ul>
      )}

      <Divider style={{ margin: '12px 0' }} orientation="left" plain>
        配套练习
      </Divider>
      {exercises.length === 0 ? (
        <Text type="secondary">未生成练习</Text>
      ) : (
        <div>
          {exercises.map((ex, i) => (
            <Card key={i} size="small" style={{ marginBottom: 8 }}>
              <div>
                <Text strong>{i + 1}. {ex.q}</Text>
              </div>
              <div style={{ margin: '6px 0 6px 18px' }}>
                {(Array.isArray(ex.options) ? ex.options : []).map((opt, j) => (
                  <div key={j}>{opt}</div>
                ))}
              </div>
              <Space size="middle">
                <span>
                  答案：<Tag color="green">{ex.answer}</Tag>
                </span>
                {ex.explanation && (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    解析：{ex.explanation}
                  </Text>
                )}
              </Space>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

export default function WordAiManagePage() {
  // ---- 生成/筛选条件 ----
  const [version, setVersion] = useState('');
  const [grade, setGrade] = useState('');
  const [unit, setUnit] = useState('');
  const [topic, setTopic] = useState('');

  // ---- 生成与预览 ----
  const [generating, setGenerating] = useState(false);
  const [preview, setPreview] = useState(null);

  // ---- 历史内容包列表 ----
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(8);
  const [loadingList, setLoadingList] = useState(false);
  const [syncingId, setSyncingId] = useState(null);

  // ---- 详情弹窗 ----
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailPack, setDetailPack] = useState(null);
  const [loadingDetail, setLoadingDetail] = useState(false);

  const paramsReady = version && grade && unit;

  const loadList = async (cur = page, size = pageSize, silent = false) => {
    if (!silent) setLoadingList(true);
    try {
      const params = { current: cur, pageSize: size };
      if (version) params.version = version;
      if (grade) params.grade = grade;
      if (unit) params.unit = unit;
      const res = await request.get('/english/ai-pack/list', { params, timeout: 30000 });
      const data = dataOf(res);
      setList(data?.list || []);
      setTotal(data?.total || 0);
    } catch (e) {
      // 拦截器已统一提示
    } finally {
      if (!silent) setLoadingList(false);
    }
  };

  useEffect(() => {
    loadList(1, pageSize, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ---- 生成 ----
  const handleGenerate = async () => {
    if (!version || !grade || !unit) {
      message.warning('请选择教材版本、年级与单元');
      return;
    }
    setGenerating(true);
    setPreview(null);
    try {
      const res = await request.post('/english/ai-pack/generate', null, {
        params: { version, grade, unit, topic: topic.trim() },
        timeout: 240000,
      });
      setPreview(dataOf(res));
      message.success('AI 内容生成完成，请预览后保存');
    } catch (e) {
      // 拦截器已统一提示（含 AI 未配置等业务错误）
    } finally {
      setGenerating(false);
    }
  };

  // ---- 保存为内容包 ----
  const [saving, setSaving] = useState(false);
  const handleSave = async () => {
    if (!preview) return;
    setSaving(true);
    try {
      const res = await request.post('/english/ai-pack/save', {
        version: preview.version,
        grade: preview.grade,
        unit: preview.unit,
        topic: preview.topic,
        title: preview.title,
        content: preview.content,
        wordCount: preview.wordCount,
      }, { timeout: 60000 });
      setPreview(dataOf(res));
      message.success('内容包已保存（同单元重复保存将覆盖旧包）');
      loadList(page, pageSize, true);
    } catch (e) {
      // 拦截器已统一提示
    } finally {
      setSaving(false);
    }
  };

  // ---- 同步到词库/语法库 ----
  const handleSync = async (id) => {
    setSyncingId(id);
    try {
      const res = await request.post('/english/ai-pack/sync', null, {
        params: { id },
        timeout: 120000,
      });
      const r = dataOf(res);
      message.success(
        `同步完成：新增单词 ${r.wordsAdded} 个，更新 ${r.wordsUpdated} 个，语法 ${r.grammarSynced} 条`,
      );
    } catch (e) {
      // 拦截器已统一提示
    } finally {
      setSyncingId(null);
    }
  };

  // ---- 删除 ----
  const handleDelete = async (id) => {
    try {
      await request.post('/english/ai-pack/delete', null, { params: { id } });
      message.success('内容包已删除');
      if (list.length === 1 && page > 1) {
        setPage(page - 1);
        loadList(page - 1, pageSize, true);
      } else {
        loadList(page, pageSize, true);
      }
    } catch (e) {
      // 拦截器已统一提示
    }
  };

  // ---- 查看详情 ----
  const openDetail = async (id) => {
    setLoadingDetail(true);
    setDetailVisible(true);
    try {
      const res = await request.get('/english/ai-pack/detail', { params: { id } });
      setDetailPack(dataOf(res));
    } catch (e) {
      setDetailVisible(false);
    } finally {
      setLoadingDetail(false);
    }
  };

  const columns = [
    { title: '版本', dataIndex: 'version', width: 90 },
    { title: '年级', dataIndex: 'grade', width: 80 },
    { title: '单元', dataIndex: 'unit', width: 90 },
    { title: '标题 / 主题', dataIndex: 'title', ellipsis: true },
    {
      title: '单词数',
      dataIndex: 'wordCount',
      width: 80,
      render: (v) => (v == null ? '-' : v),
    },
    {
      title: '创建时间',
      dataIndex: 'createAt',
      width: 160,
      render: (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-'),
    },
    {
      title: '操作',
      width: 210,
      render: (_, row) => (
        <Space>
          <Button size="small" icon={<EyeOutlined />} onClick={() => openDetail(row.id)}>
            查看
          </Button>
          <Button
            size="small"
            icon={<SyncOutlined />}
            loading={syncingId === row.id}
            onClick={() => handleSync(row.id)}
          >
            同步
          </Button>
          <Popconfirm title="确认删除该内容包？" onConfirm={() => handleDelete(row.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 20 }}>
      {/* 生成条件 */}
      <Card
        title="AI 单元内容生成"
        extra={<ThunderboltOutlined style={{ fontSize: 20 }} />}
        style={{ marginBottom: 16 }}
      >
        <Space wrap style={{ marginBottom: 12 }}>
          <Select
            placeholder="教材版本"
            allowClear
            style={{ width: 140 }}
            value={version || undefined}
            onChange={(v) => setVersion(v || '')}
            options={VERSIONS.map((v) => ({ value: v, label: v }))}
          />
          <Select
            placeholder="年级"
            allowClear
            style={{ width: 120 }}
            value={grade || undefined}
            onChange={(v) => setGrade(v || '')}
            options={GRADES.map((v) => ({ value: v, label: v }))}
          />
          <Select
            placeholder="单元"
            allowClear
            style={{ width: 120 }}
            value={unit || undefined}
            onChange={(v) => setUnit(v || '')}
            options={UNITS.map((v) => ({ value: v, label: v }))}
          />
          <Input
            placeholder="主题（可选，如：Animals）"
            style={{ width: 220 }}
            value={topic}
            maxLength={80}
            onChange={(e) => setTopic(e.target.value)}
            onPressEnter={handleGenerate}
          />
          <Button
            type="primary"
            icon={<ThunderboltOutlined />}
            loading={generating}
            disabled={!paramsReady}
            onClick={handleGenerate}
          >
            生成整套内容
          </Button>
        </Space>
        <Alert
          type="info"
          showIcon
          message="AI 将依据所选版本/年级/单元生成同步学习内容（核心词汇+音标释义例句、语法讲解、配套单选题），并自动衔接本单元词库已有单词。生成内容可先预览确认，再保存并在确认后一键同步入正式词库与语法库。"
        />
      </Card>

      {/* 生成结果预览与操作 */}
      {preview && (
        <Card
          title="生成结果预览"
          style={{ marginBottom: 16 }}
          extra={
            <Space>
              <Button icon={<SaveOutlined />} loading={saving} onClick={handleSave}>
                保存为内容包
              </Button>
              {preview.id && (
                <Button
                  type="primary"
                  icon={<SyncOutlined />}
                  loading={syncingId === preview.id}
                  onClick={() => handleSync(preview.id)}
                >
                  同步到词库与语法库
                </Button>
              )}
            </Space>
          }
        >
          <PackPreview view={preview} />
        </Card>
      )}

      {/* 历史内容包 */}
      <Card
        title="历史内容包"
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={() => loadList(page, pageSize)}
          >
            刷新
          </Button>
        }
      >
        <Space style={{ marginBottom: 12 }}>
          <Select
            placeholder="版本筛选"
            allowClear
            style={{ width: 130 }}
            value={version || undefined}
            onChange={(v) => {
              setVersion(v || '');
              const next = 1;
              setPage(next);
              setTimeout(() => loadList(next, pageSize, true), 0);
            }}
            options={VERSIONS.map((v) => ({ value: v, label: v }))}
          />
          <Select
            placeholder="年级筛选"
            allowClear
            style={{ width: 110 }}
            value={grade || undefined}
            onChange={(v) => {
              setGrade(v || '');
              const next = 1;
              setPage(next);
              setTimeout(() => loadList(next, pageSize, true), 0);
            }}
            options={GRADES.map((v) => ({ value: v, label: v }))}
          />
          <Select
            placeholder="单元筛选"
            allowClear
            style={{ width: 110 }}
            value={unit || undefined}
            onChange={(v) => {
              setUnit(v || '');
              const next = 1;
              setPage(next);
              setTimeout(() => loadList(next, pageSize, true), 0);
            }}
            options={UNITS.map((v) => ({ value: v, label: v }))}
          />
        </Space>
        <Table
          rowKey="id"
          size="middle"
          loading={loadingList}
          columns={columns}
          dataSource={list}
          pagination={{
            current: page,
            pageSize,
            total,
            showSizeChanger: false,
            onChange: (cur, size) => {
              setPage(cur);
              setPageSize(size);
              loadList(cur, size, true);
            },
          }}
        />
      </Card>

      {/* 详情弹窗 */}
      <Modal
        title="内容包详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={920}
      >
        {detailPack && <PackPreview view={detailPack} />}
      </Modal>
    </div>
  );
}
