/**
 * SentencePackManagePage.jsx - 英语单元句子同步练习固化页（后台管理端）
 *
 * 功能（URL: /english/sentence-pack，权限复用 english:word:ai）:
 *   1. 选择 版本/年级/册别/单元，按单元生成多题型句子练习预览
 *      （连词成句 build / 句子默写 dictation / 句子选择 choice，逐组调用 AI，不落库）
 *   2. 预览确认后保存固化（同 版本+年级+册别+单元 覆盖）
 *   3. 支持「批量生成整册」：载入该册所有含句子的单元，逐个生成并固化，逐单元反馈成败
 *   4. 历史内容包：筛选/详情查看/删除
 *
 * 学员端读取该固化内容（按题型渲染），不再实时调用 AI。
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Card, Button, Select, Space, Table, Tag, Modal, Typography,
  message, Popconfirm, Alert, Progress,
} from 'antd';
import {
  ThunderboltOutlined, SaveOutlined, EyeOutlined,
  DeleteOutlined, ReloadOutlined, AppstoreAddOutlined,
} from '@ant-design/icons';
import {
  generateSentencePack, saveSentencePack, listSentencePacks,
  getSentencePackDetail, deleteSentencePack, listSentencePackUnits,
} from '../../api/sentencePack';

const VERSIONS = ['苏教版', '北师大版', '外研版'];
const GRADES = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级'];
const TERMS = ['上册', '下册'];
const FALLBACK_UNITS = ['Unit 1', 'Unit 2', 'Unit 3', 'Unit 4', 'Unit 5', 'Unit 6', 'Unit 7', 'Unit 8'];

const { Text } = Typography;

const KIND_LABELS = { build: '连词成句', dictation: '句子默写', choice: '句子选择', fill: '词汇填空' };
const STAGE_LABELS = { learn: '句子学习', practice: '句子练习' };
const DIFFICULTY_LABELS = { 1: '基础', 2: '提高', 3: '挑战' };

/** 后端返回 { code, data }，统一取 data */
const dataOf = (res) => res?.data;

/** 解析正文 JSON 为题目数组 */
function parseExercises(content) {
  if (!content) return [];
  try {
    const arr = JSON.parse(content);
    return Array.isArray(arr) ? arr : [];
  } catch {
    return [];
  }
}

/** 内容包预览（供生成预览与详情弹窗复用） */
function PackPreview({ view }) {
  const exercises = useMemo(() => parseExercises(view?.content), [view]);
  if (exercises.length === 0) {
    return <Alert type="warning" showIcon title="该单元暂无可用句子（缺少重点句子）" />;
  }
  return (
    <div>
      <Space style={{ marginBottom: 8 }}>
        <Text strong>句子同步练习</Text>
        <Tag color="blue">共 {exercises.length} 题</Tag>
        <Tag color="cyan">句子学习 {exercises.filter((e) => e.stage === 'learn').length}</Tag>
        <Tag color="geekblue">句子练习 {exercises.filter((e) => e.stage !== 'learn').length}</Tag>
        <Tag color="green">连词成句 {exercises.filter((e) => e.kind === 'build').length}</Tag>
        <Tag color="orange">句子默写 {exercises.filter((e) => e.kind === 'dictation').length}</Tag>
        <Tag color="purple">句子选择 {exercises.filter((e) => e.kind === 'choice').length}</Tag>
        <Tag color="gold">词汇填空 {exercises.filter((e) => e.kind === 'fill').length}</Tag>
      </Space>
      {exercises.map((ex, i) => (
        <Card key={i} size="small" style={{ marginBottom: 8 }} title={`第 ${i + 1} 题`}>
          <Tag color="blue" bordered={false}>{KIND_LABELS[ex.kind] || ex.kind || '题目'}</Tag>
          <Tag color="cyan" bordered={false}>{STAGE_LABELS[ex.stage] || '句子练习'}</Tag>
          <Tag color="gold" bordered={false}>{DIFFICULTY_LABELS[ex.difficulty || 1]}</Tag>
          {ex.theme && <Tag bordered={false}>{ex.theme}</Tag>}
          <div style={{ margin: '6px 0' }}>
            <Text strong>题干：</Text>{ex.zh || '（无中文题干）'}
          </div>
          {ex.kind === 'choice' && Array.isArray(ex.options) && (
            <div style={{ marginLeft: 8 }}>
              {ex.options.map((opt, oi) => (
                <div key={oi}>
                  <Text type={opt === (ex.answer || ex.en) ? 'success' : undefined}>
                    {String.fromCharCode(65 + oi)}. {opt}
                  </Text>
                </div>
              ))}
            </div>
          )}
          {ex.kind === 'fill' && (
            <div style={{ marginLeft: 8 }}>
              {ex.stem && <div><Text strong>填空：</Text>{ex.stem}</div>}
              <Text type="success">答案：{ex.answer || ex.en}</Text>
            </div>
          )}
          {(ex.kind === 'build' || ex.kind === 'dictation') && (
            <div style={{ marginLeft: 8 }}>
              <Text type="success">答案：{ex.en}</Text>
            </div>
          )}
          {ex.explanation && (
            <div style={{ marginLeft: 8 }}>
              <Text type="secondary">解析：{ex.explanation}</Text>
            </div>
          )}
        </Card>
      ))}
    </div>
  );
}

export default function SentencePackManagePage() {
  // ---- 生成/筛选条件 ----
  const [version, setVersion] = useState('');
  const [grade, setGrade] = useState('');
  const [term, setTerm] = useState('');
  const [unit, setUnit] = useState('');
  const [unitOptions, setUnitOptions] = useState(FALLBACK_UNITS);

  // ---- 生成与预览 ----
  const [generating, setGenerating] = useState(false);
  const [preview, setPreview] = useState(null);
  const [saving, setSaving] = useState(false);

  // ---- 批量生成 ----
  const [batchRunning, setBatchRunning] = useState(false);
  const [batchResults, setBatchResults] = useState([]);

  // ---- 历史内容包列表 ----
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(8);
  const [loadingList, setLoadingList] = useState(false);

  // ---- 详情弹窗 ----
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailPack, setDetailPack] = useState(null);

  const paramsReady = version && grade && term && unit;
  const batchReady = version && grade && term;

  const loadList = useCallback(async (cur = 1, size = 8) => {
    setLoadingList(true);
    try {
      const params = { current: cur, pageSize: size };
      if (version) params.version = version;
      if (grade) params.grade = grade;
      if (term) params.term = term;
      if (unit) params.unit = unit;
      const res = await listSentencePacks(params);
      const data = dataOf(res);
      setList(data?.list || []);
      setTotal(data?.total || 0);
    } catch {
      // 拦截器已统一提示
    } finally {
      setLoadingList(false);
    }
  }, [version, grade, term, unit]);

  useEffect(() => {
    loadList(1, pageSize);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ---- 载入该册可用单元 ----
  const loadUnits = useCallback(async () => {
    if (!version || !grade || !term) {
      setUnitOptions(FALLBACK_UNITS);
      return;
    }
    try {
      const res = await listSentencePackUnits({ version, grade, term });
      const units = dataOf(res) || [];
      setUnitOptions(units.length ? units : FALLBACK_UNITS);
    } catch {
      setUnitOptions(FALLBACK_UNITS);
    }
  }, [version, grade, term]);

  useEffect(() => {
    loadUnits();
  }, [loadUnits]);

  // ---- 生成预览 ----
  const handleGenerate = async () => {
    if (!paramsReady) {
      message.warning('请选择教材版本、年级、册别与单元');
      return;
    }
    setGenerating(true);
    setPreview(null);
    try {
      const res = await generateSentencePack({ version, grade, term, unit });
      setPreview(dataOf(res));
      message.success('生成完成，请预览后保存固化');
    } catch {
      // 拦截器已统一提示（含 AI 未配置等业务错误）
    } finally {
      setGenerating(false);
    }
  };

  // ---- 保存固化 ----
  const handleSave = async () => {
    if (!preview) return;
    setSaving(true);
    try {
      const res = await saveSentencePack({
        version: preview.version,
        grade: preview.grade,
        term: preview.term,
        unit: preview.unit,
        content: preview.content,
        count: preview.count,
      });
      setPreview(dataOf(res));
      message.success('已保存固化（同单元重复保存将覆盖）');
      loadList(page, pageSize);
    } catch {
      // 拦截器已统一提示
    } finally {
      setSaving(false);
    }
  };

  // ---- 批量生成整册 ----
  const handleBatch = async () => {
    if (!batchReady) {
      message.warning('请先选择教材版本、年级与册别');
      return;
    }
    setBatchRunning(true);
    setBatchResults([]);
    let units = unitOptions;
    try {
      const res = await listSentencePackUnits({ version, grade, term });
      const fetched = dataOf(res) || [];
      if (fetched.length) units = fetched;
    } catch {
      // 回退用当前单元选项
    }
    if (!units.length) {
      message.warning('该册未找到可用单元');
      setBatchRunning(false);
      return;
    }

    for (const u of units) {
      setBatchResults((prev) => [...prev, { unit: u, status: 'running' }]);
      try {
        const g = await generateSentencePack({ version, grade, term, unit: u });
        const gen = dataOf(g);
        await saveSentencePack({
          version, grade, term, unit: u,
          content: gen?.content,
          count: gen?.count,
        });
        setBatchResults((prev) => prev.map((r) => (
          r.unit === u ? { ...r, status: 'done', count: gen?.count } : r
        )));
      } catch (e) {
        setBatchResults((prev) => prev.map((r) => (
          r.unit === u ? { ...r, status: 'error', message: e?.message || '生成失败' } : r
        )));
      }
    }
    setBatchRunning(false);
    message.success('批量生成完成');
    loadList(1, pageSize);
  };

  // ---- 删除 ----
  const handleDelete = async (id) => {
    try {
      await deleteSentencePack(id);
      message.success('已删除');
      if (list.length === 1 && page > 1) {
        setPage(page - 1);
        loadList(page - 1, pageSize);
      } else {
        loadList(page, pageSize);
      }
    } catch {
      // 拦截器已统一提示
    }
  };

  // ---- 查看详情 ----
  const openDetail = async (id) => {
    setDetailVisible(true);
    try {
      const res = await getSentencePackDetail(id);
      setDetailPack(dataOf(res));
    } catch {
      setDetailVisible(false);
    }
  };

  const columns = [
    { title: '版本', dataIndex: 'version', width: 90 },
    { title: '年级', dataIndex: 'grade', width: 80 },
    { title: '册别', dataIndex: 'term', width: 80 },
    { title: '单元', dataIndex: 'unit', width: 90 },
    { title: '题目数', dataIndex: 'count', width: 90, render: (v) => (v == null ? '-' : v) },
    {
      title: '更新时间',
      dataIndex: 'createAt',
      width: 160,
      render: (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-'),
    },
    {
      title: '操作',
      width: 150,
      render: (_, row) => (
        <Space>
          <Button size="small" icon={<EyeOutlined />} onClick={() => openDetail(row.id)}>查看</Button>
          <Popconfirm title="确认删除该句子练习内容包？" onConfirm={() => handleDelete(row.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const batchDone = batchResults.filter((r) => r.status !== 'running').length;
  const batchProgress = batchResults.length ? Math.round((batchDone / batchResults.length) * 100) : 0;

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 20 }}>
      {/* 生成条件 */}
      <Card
        title="英语单元句子同步练习固化"
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
            placeholder="册别"
            allowClear
            style={{ width: 100 }}
            value={term || undefined}
            onChange={(v) => setTerm(v || '')}
            options={TERMS.map((v) => ({ value: v, label: v }))}
          />
          <Select
            placeholder="单元"
            allowClear
            style={{ width: 120 }}
            value={unit || undefined}
            onChange={(v) => setUnit(v || '')}
            options={unitOptions.map((v) => ({ value: v, label: v }))}
          />
          <Button
            type="primary"
            icon={<ThunderboltOutlined />}
            loading={generating}
            disabled={!paramsReady}
            onClick={handleGenerate}
          >
            生成预览
          </Button>
          <Button
            icon={<AppstoreAddOutlined />}
            loading={batchRunning}
            disabled={!batchReady}
            onClick={handleBatch}
          >
            批量生成整册
          </Button>
        </Space>
        <Alert
          type="info"
          showIcon
          title="系统会一次性读取本单元全部重点句式与核心词汇，调用 AI 生成 10-15 道「句子学习」（课本句式连词成句）和 20-25 道「句子练习」（选择/填空/连词成句/默写，按基础→提高→挑战排序，同一主题最多 2 题）。生成内容先预览确认，再保存固化；学员端只读取已固化内容。"
        />

        {/* 批量进度 */}
        {batchResults.length > 0 && (
          <div style={{ marginTop: 12 }}>
            <Progress percent={batchProgress} status={batchRunning ? 'active' : 'success'} />
            <Space wrap size={[8, 8]}>
              {batchResults.map((r) => (
                <Tag
                  key={r.unit}
                  color={r.status === 'done' ? 'green' : r.status === 'error' ? 'red' : 'blue'}
                >
                  {r.unit}
                  {r.status === 'done' ? ` ✓ ${r.count ?? 0} 题` : ''}
                  {r.status === 'error' ? ` ✗ ${r.message}` : ''}
                  {r.status === 'running' ? ' 生成中…' : ''}
                </Tag>
              ))}
            </Space>
          </div>
        )}
      </Card>

      {/* 生成结果预览与操作 */}
      {preview && (
        <Card
          title="生成结果预览"
          style={{ marginBottom: 16 }}
          extra={
            <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={handleSave}>
              保存固化
            </Button>
          }
        >
          <PackPreview view={preview} />
        </Card>
      )}

      {/* 历史内容包 */}
      <Card
        title="已固化内容包"
        extra={<Button icon={<ReloadOutlined />} onClick={() => loadList(page, pageSize)}>刷新</Button>}
      >
        <Space style={{ marginBottom: 12 }} wrap>
          <Select
            placeholder="版本筛选"
            allowClear
            style={{ width: 130 }}
            value={version || undefined}
            onChange={(v) => { setVersion(v || ''); setPage(1); setTimeout(() => loadList(1, pageSize), 0); }}
            options={VERSIONS.map((v) => ({ value: v, label: v }))}
          />
          <Select
            placeholder="年级筛选"
            allowClear
            style={{ width: 110 }}
            value={grade || undefined}
            onChange={(v) => { setGrade(v || ''); setPage(1); setTimeout(() => loadList(1, pageSize), 0); }}
            options={GRADES.map((v) => ({ value: v, label: v }))}
          />
          <Select
            placeholder="册别筛选"
            allowClear
            style={{ width: 100 }}
            value={term || undefined}
            onChange={(v) => { setTerm(v || ''); setPage(1); setTimeout(() => loadList(1, pageSize), 0); }}
            options={TERMS.map((v) => ({ value: v, label: v }))}
          />
          <Select
            placeholder="单元筛选"
            allowClear
            style={{ width: 110 }}
            value={unit || undefined}
            onChange={(v) => { setUnit(v || ''); setPage(1); setTimeout(() => loadList(1, pageSize), 0); }}
            options={unitOptions.map((v) => ({ value: v, label: v }))}
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
              loadList(cur, size);
            },
          }}
        />
      </Card>

      {/* 详情弹窗 */}
      <Modal
        title="句子练习内容包详情"
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
