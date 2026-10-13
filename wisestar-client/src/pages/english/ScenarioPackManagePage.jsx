/**
 * ScenarioPackManagePage.jsx - 英语单元情景练习固化页（后台管理端）
 *
 * 功能（URL: /english/scenario-pack，权限复用 english:word:ai）:
 *   1. 选择 版本/年级/册别/单元，按单元生成单词情景 + 句式情景预览
 *      （单元单词每 8 个一组、句式每 6 个一组自动分组，逐组调用 AI，不落库）
 *   2. 预览确认后保存固化（同 版本+年级+册别+单元 覆盖）
 *   3. 支持「批量生成整册」：载入该册所有可用单元，逐个生成并固化，逐单元反馈成败
 *   4. 历史内容包：筛选/详情查看/删除
 *
 * 学员端读取该固化内容（按组合并展示），不再实时调用 AI。
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Card, Button, Select, Space, Table, Tag, Modal, Typography,
  message, Popconfirm, Divider, Alert, Progress,
} from 'antd';
import {
  ThunderboltOutlined, SaveOutlined, EyeOutlined,
  DeleteOutlined, ReloadOutlined, AppstoreAddOutlined,
} from '@ant-design/icons';
import {
  generateScenarioPack, saveScenarioPack, listScenarioPacks,
  getScenarioPackDetail, deleteScenarioPack, listScenarioPackUnits,
} from '../../api/scenarioPack';

const VERSIONS = ['苏教版', '北师大版', '外研版'];
const GRADES = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级'];
const TERMS = ['上册', '下册'];
const FALLBACK_UNITS = ['Unit 1', 'Unit 2', 'Unit 3', 'Unit 4', 'Unit 5', 'Unit 6', 'Unit 7', 'Unit 8'];

const { Text } = Typography;

/** 后端返回 { code, data }，统一取 data */
const dataOf = (res) => res?.data;

/** 解析分组 JSON 为 groups 数组 */
function parseGroups(content) {
  if (!content) return [];
  try {
    const obj = JSON.parse(content);
    return Array.isArray(obj?.groups) ? obj.groups : [];
  } catch {
    return [];
  }
}

/** 单词情景预览区块 */
function WordScenarios({ groups }) {
  const total = groups.reduce((n, g) => n + (Array.isArray(g.scenarios) ? g.scenarios.length : 0), 0);
  return (
    <div>
      <Space style={{ marginBottom: 8 }}>
        <Text strong>单词情景对话</Text>
        <Tag color="blue">场景 {total} 个</Tag>
        <Tag>分组 {groups.length} 组</Tag>
      </Space>
      {groups.map((g, gi) => (
        <Card
          key={`wg-${gi}`}
          size="small"
          style={{ marginBottom: 8 }}
          title={`第 ${g.groupNo ?? gi + 1} 组`}
          extra={<Text type="secondary">覆盖单词：{Array.isArray(g.words) ? g.words.join('、') : ''}</Text>}
        >
          {(g.scenarios || []).map((s, si) => (
            <div key={`ws-${si}`} style={{ marginBottom: 8 }}>
              <div><Text strong>{s.title || `情景 ${si + 1}`}</Text></div>
              {(s.lines || []).map((line, li) => (
                <div key={`wl-${li}`} style={{ marginLeft: 8 }}>
                  <Text strong>{line.speaker || 'A'}: </Text>
                  <span>{line.en}</span>
                  {line.zh && <Text type="secondary" style={{ marginLeft: 8 }}>{line.zh}</Text>}
                  {Array.isArray(line.answers) && line.answers.length > 0 && (
                    <Text type="success" style={{ marginLeft: 8 }}>（{line.answers.join(' / ')}）</Text>
                  )}
                </div>
              ))}
            </div>
          ))}
          {(g.scenarios || []).length === 0 && <Text type="secondary">该组未生成情景</Text>}
        </Card>
      ))}
      {groups.length === 0 && <Text type="secondary">无单词情景</Text>}
    </div>
  );
}

/** 句式情景预览区块 */
function PatternScenarios({ groups }) {
  const total = groups.reduce((n, g) => n + (Array.isArray(g.scenarios) ? g.scenarios.length : 0), 0);
  return (
    <div>
      <Space style={{ marginBottom: 8 }}>
        <Text strong>重点句式情景</Text>
        <Tag color="purple">情景 {total} 个</Tag>
        <Tag>分组 {groups.length} 组</Tag>
      </Space>
      {groups.map((g, gi) => (
        <Card
          key={`pg-${gi}`}
          size="small"
          style={{ marginBottom: 8 }}
          title={`第 ${g.groupNo ?? gi + 1} 组`}
          extra={<Text type="secondary">覆盖句式 {Array.isArray(g.sentences) ? g.sentences.length : 0} 条</Text>}
        >
          {(g.scenarios || []).map((s, si) => (
            <div key={`ps-${si}`} style={{ marginBottom: 10 }}>
              <div>
                <Text strong>{s.title || `情景 ${si + 1}`}</Text>
                {s.context && <Text type="secondary" style={{ marginLeft: 8 }}>{s.context}</Text>}
              </div>
              {(s.turns || []).map((turn, ti) => (
                <div key={`pt-${ti}`} style={{ marginLeft: 8 }}>
                  <Text strong>{turn.speaker || 'A'}: </Text>
                  {typeof turn.answerIndex === 'number' && turn.answerIndex >= 0 ? (
                    <>
                      <Text type="warning">[情景] {turn.cue}</Text>
                      <Text type="success" style={{ marginLeft: 8 }}>
                        答案：{(turn.options || [])[turn.answerIndex]}
                      </Text>
                    </>
                  ) : (
                    <>
                      <span>{turn.en}</span>
                      {turn.zh && <Text type="secondary" style={{ marginLeft: 8 }}>{turn.zh}</Text>}
                    </>
                  )}
                </div>
              ))}
            </div>
          ))}
          {(g.scenarios || []).length === 0 && <Text type="secondary">该组未生成情景</Text>}
        </Card>
      ))}
      {groups.length === 0 && <Text type="secondary">无句式情景</Text>}
    </div>
  );
}

/** 内容包预览（供生成预览与详情弹窗复用） */
function PackPreview({ view }) {
  const groups = useMemo(() => ({
    word: parseGroups(view?.wordContent),
    pattern: parseGroups(view?.patternContent),
  }), [view]);

  if (groups.word.length === 0 && groups.pattern.length === 0) {
    return <Alert type="warning" showIcon title="该单元暂无可用内容（缺少单词或重点句式）" />;
  }

  return (
    <div>
      <WordScenarios groups={groups.word} />
      <Divider style={{ margin: '12px 0' }} />
      <PatternScenarios groups={groups.pattern} />
    </div>
  );
}

export default function ScenarioPackManagePage() {
  // ---- 生成/筛选条件 ----
  const [version, setVersion] = useState('');
  const [grade, setGrade] = useState('');
  const [term, setTerm] = useState('');
  const [unit, setUnit] = useState('');
  const [unitOptions, setUnitOptions] = useState(FALLBACK_UNITS);

  // ---- 生成与预览 ----
  const [generating, setGenerating] = useState(false);
  const [preview, setPreview] = useState(null);

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
      const res = await listScenarioPacks(params);
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
      const res = await listScenarioPackUnits({ version, grade, term });
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
      const res = await generateScenarioPack({ version, grade, term, unit });
      setPreview(dataOf(res));
      message.success('生成完成，请预览后保存固化');
    } catch {
      // 拦截器已统一提示（含 AI 未配置等业务错误）
    } finally {
      setGenerating(false);
    }
  };

  // ---- 保存固化 ----
  const [saving, setSaving] = useState(false);
  const handleSave = async () => {
    if (!preview) return;
    setSaving(true);
    try {
      const res = await saveScenarioPack({
        version: preview.version,
        grade: preview.grade,
        term: preview.term,
        unit: preview.unit,
        wordContent: preview.wordContent,
        wordCount: preview.wordCount,
        patternContent: preview.patternContent,
        patternCount: preview.patternCount,
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
      const res = await listScenarioPackUnits({ version, grade, term });
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
        const g = await generateScenarioPack({ version, grade, term, unit: u });
        const gen = dataOf(g);
        await saveScenarioPack({
          version, grade, term, unit: u,
          wordContent: gen?.wordContent,
          wordCount: gen?.wordCount,
          patternContent: gen?.patternContent,
          patternCount: gen?.patternCount,
        });
        setBatchResults((prev) => prev.map((r) => (
          r.unit === u ? { ...r, status: 'done', wordCount: gen?.wordCount, patternCount: gen?.patternCount } : r
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
      await deleteScenarioPack(id);
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
      const res = await getScenarioPackDetail(id);
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
    { title: '单词情景', dataIndex: 'wordCount', width: 90, render: (v) => (v == null ? '-' : v) },
    { title: '句式情景', dataIndex: 'patternCount', width: 90, render: (v) => (v == null ? '-' : v) },
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
          <Popconfirm title="确认删除该情景内容包？" onConfirm={() => handleDelete(row.id)}>
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
        title="英语单元情景练习固化"
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
          title="系统会把该单元单词每 8 个一组、重点句式每 6 个一组自动分组，逐组调用 AI 生成情景对话。生成内容先预览确认，再保存固化；学员端将只读取已固化内容（按组合并展示），不再实时生成。"
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
                  {r.status === 'done' ? ` ✓ 单词${r.wordCount ?? 0}/句式${r.patternCount ?? 0}` : ''}
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
        title="情景内容包详情"
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
