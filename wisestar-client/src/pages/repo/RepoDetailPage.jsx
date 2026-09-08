/**
 * RepoDetailPage.jsx - 练习详情 & 组题管理页面
 *
 * 功能:
 *   1. 练习信息展示（名称、学科/年级/难度、标签、题目总数）
 *   2. 题目列表（分页、显示答案/解析与分值）
 *   3. 设置分值：分值列直接行内填写（整题类填整题分；多项填空按每空填，
 *      失焦/回车自动保存，未设置时判分按 整题分÷空位数 均摊兜底）
 *   4. 批量选择题目：从题目管理（全局题目库）勾选已有题目加入本练习
 *   5. 移除题目：单个/批量解绑（题目保留在题目管理中，不删除模板本身）
 *
 * 题目来源约定（重要）:
 *   题目信息的创建/编辑/导入唯一入口是「题目管理」板块（QuestionListPage）。
 *   本页面只负责组题（选择题目加入练习 / 从练习移除）与分值设置（写 template.attribute）。
 *
 * 被谁引用: App.jsx 路由表（/repos/:id）；从 RepoListPage 点击练习名称进入
 *
 * 数据流:
 *   练习信息: listRepo({id, pageSize:1}) → GET /api/repo/list → find 出当前练习
 *   题目列表: fetchTemplates → listTemplate({current, pageSize, repoId}) → GET /api/template/list
 *   批量选择: SelectTemplateModal → bindTemplate({repoId, ids}) → POST /api/repo/bind
 *   设置分值: 分值列行内输入，失焦时 updateTemplate({id, template: {...schema, attribute}})
 *            → POST /api/template/update
 *   移除: handleRemoveTemplate / handleBatchRemove → unbindTemplate({repoId, ids})
 *         → POST /api/repo/unbind（仅清空题目 repoId，题目保留在题目管理）
 *
 * URL: /repos/:id
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Table, Space, Button, Popconfirm, Typography, Tag, message, Card, Descriptions, Upload,
  InputNumber, Tooltip,
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, ArrowLeftOutlined, ImportOutlined, LoadingOutlined,
  BookOutlined, CheckCircleOutlined, BulbOutlined, ApartmentOutlined, PartitionOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons';
import { listTemplate, updateTemplate } from '../../api/template';
import { listRepo, unbindTemplate, importTemplate, listRepoLocations } from '../../api/repo';
import SelectTemplateModal from '../../components/repo/SelectTemplateModal';

const { Title, Text, Paragraph } = Typography;

// 完整题型映射（含判断题）
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Select: '下拉题',
  FillBlank: '填空题', Text: '多行文本', Score: '评分题',
  Remark: '备注说明', Judge: '判断题', MultipleBlank: '多项填空',
};

function round2(n) {
  return Math.round(n * 100) / 100;
}

/**
 * 分值列行内编辑器。
 *
 * - 整题类题型（单选/判断/普通填空等）: 一个输入框直接填整题分值；
 * - 多项填空: 每个空一个输入框，全部填完失焦后自动保存（整题分=各空之和）。
 *
 * 未设置分值时输入框为空，判分端按 1 分（整题）或 整题分÷空位数（填空均摊）兜底。
 */
function ScoreCell({ record, onSaveAttr }) {
  const attr = record.template?.attribute || {};
  const isMB = record.questionType === 'MultipleBlank';
  const answer = attr.examCorrectAnswer;
  const blankCount = isMB ? Math.max(1, String(answer || '').split('|').filter(Boolean).length) : 1;

  // 行内输入值（仅在题目首次进入本页时初始化一次；保存成功后本地值与后端一致）
  const [whole, setWhole] = useState(() => {
    if (isMB) return null;
    return typeof attr.examScore === 'number' ? attr.examScore : null;
  });
  const [blanks, setBlanks] = useState(() => {
    if (!isMB) return [];
    if (Array.isArray(attr.examBlankScores) && attr.examBlankScores.length === blankCount) {
      return attr.examBlankScores.map((n) => (typeof n === 'number' ? n : Number(n)));
    }
    // 旧数据未配每空分但有整题分: 预填均摊值，便于直接微调
    if (typeof attr.examScore === 'number' && attr.examScore > 0) {
      const per = round2(attr.examScore / blankCount);
      return Array.from({ length: blankCount }, () => per);
    }
    return Array.from({ length: blankCount }, () => null);
  });
  const [saving, setSaving] = useState(false);

  const prevWhole = typeof attr.examScore === 'number' ? attr.examScore : null;
  const prevBlanks = Array.isArray(attr.examBlankScores) ? attr.examBlankScores : null;

  // 写入后端（父组件负责局部更新当前行），结束后恢复输入态
  const commit = async (nextAttr) => {
    setSaving(true);
    try {
      await onSaveAttr(nextAttr);
    } finally {
      setSaving(false);
    }
  };

  // 整题类型: 失焦/回车即保存
  const handleWholeBlur = (val) => {
    const next = typeof val === 'number' && val > 0 ? round2(val) : null;
    if (next === prevWhole) return;
    const nextAttr = { ...attr };
    if (next) {
      nextAttr.examScore = next;
    } else {
      delete nextAttr.examScore;
    }
    delete nextAttr.examBlankScores;
    commit(nextAttr);
  };

  // 多项填空: 某一空失焦，若所有空均为有效值则整题保存
  const handleBlankBlur = () => {
    if (!blanks.length || blanks.some((v) => !(typeof v === 'number' && v > 0))) return;
    const nums = blanks.map((v) => round2(v));
    if (JSON.stringify(nums) === JSON.stringify(prevBlanks)) return;
    const nextAttr = { ...attr, examBlankScores: nums, examScore: round2(nums.reduce((s, v) => s + v, 0)) };
    commit(nextAttr);
  };

  const changeBlank = (i, val) => {
    const next = [...blanks];
    next[i] = typeof val === 'number' && val > 0 ? val : null;
    setBlanks(next);
  };

  if (saving) {
    return <LoadingOutlined style={{ color: '#1890ff' }} />;
  }

  if (isMB) {
    const allSet = blanks.length > 0 && blanks.every((v) => typeof v === 'number' && v > 0);
    const total = allSet ? round2(blanks.reduce((s, v) => s + v, 0)) : null;
    return (
      <Space size={4} wrap>
        {blanks.map((v, i) => (
          <InputNumber
            key={i}
            size="small"
            controls={false}
            min={0.01}
            step={0.5}
            precision={2}
            value={v}
            placeholder={`空${i + 1}`}
            style={{ width: 62 }}
            onChange={(val) => changeBlank(i, val)}
            onBlur={() => handleBlankBlur()}
          />
        ))}
        {allSet && <Text type="secondary" style={{ fontSize: 12 }}>= {total} 分</Text>}
      </Space>
    );
  }

  return (
    <InputNumber
      size="small"
      controls={false}
      min={0.01}
      step={0.5}
      precision={2}
      value={whole}
      placeholder="未设置"
      style={{ width: 90 }}
      onChange={(val) => setWhole(typeof val === 'number' && val > 0 ? round2(val) : null)}
      onBlur={() => handleWholeBlur(whole)}
    />
  );
}

export default function RepoDetailPage() {
  const { id: repoId } = useParams();
  const navigate = useNavigate();

  // ---- 状态 ----
  const [repo, setRepo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [templates, setTemplates] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const pageSize = 20;

  // 知识绑定位置（本练习被投放到的所属小节，回显用；绑定维护在小节管理/教研平台）
  const [bindings, setBindings] = useState([]);
  const [bindingsLoading, setBindingsLoading] = useState(false);

  // 批量选择题目弹窗
  const [selectOpen, setSelectOpen] = useState(false);

  // 导入状态
  const [importing, setImporting] = useState(false);

  // 表格勾选（批量移除）
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [removing, setRemoving] = useState(false);

  // ---- 加载练习信息 ----
  // 复用 listRepo 列表接口，传入 id + pageSize:1 后从返回列表中 find 出当前练习
  useEffect(() => {
    (async () => {
      try {
        const res = await listRepo({ id: repoId, pageSize: 1 });
        const r = (res.data?.list || []).find((r) => r.id === repoId);
        setRepo(r || null);
      } catch { /* silent */ }
    })();
  }, [repoId]);

  // ---- 加载本练习的绑定位置（章节/小节） ----
  useEffect(() => {
    (async () => {
      if (!repo?.id) return;
      setBindingsLoading(true);
      try {
        const res = await listRepoLocations(repo.id);
        setBindings(res?.data?.bindings || []);
      } catch {
        setBindings([]);
      } finally {
        setBindingsLoading(false);
      }
    })();
  }, [repo?.id]);

  // ---- 加载题目列表 ----
  // 只加载当前练习（repoId）下的题目
  // 数据流: 本页 → listTemplate({current, pageSize, repoId}) → GET /api/template/list
  const fetchTemplates = async (p = page) => {
    setLoading(true);
    try {
      const res = await listTemplate({ current: p, pageSize, repoId });
      setTemplates(res.data?.list || []);
      setTotal(res.data?.total || 0);
      // 刷新后清除失效的勾选（被移除的题）
      setSelectedRowKeys((prev) => prev.filter((id) => (res.data?.list || []).some((t) => t.id === id)));
    } catch {
      message.error('加载题目失败');
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { fetchTemplates(); }, [repoId]); // eslint-disable-line

  // ---- 刷新练习信息（题目总数等） ----
  const refreshRepoInfo = () => {
    listRepo({ id: repoId, pageSize: 1 }).then((res) => {
      const r = (res.data?.list || []).find((x) => x.id === repoId);
      if (r) setRepo(r);
    }).catch(() => {});
  };

  // ---- 批量选择题目成功回调 ----
  const handleSelectSuccess = () => {
    setSelectOpen(false);
    fetchTemplates(page);
    refreshRepoInfo();
  };

  // ---- 移除单题（解绑，题目保留在题目管理） ----
  const handleRemoveTemplate = async (id) => {
    try {
      await unbindTemplate({ repoId, ids: [id] });
      message.success('已从练习移除');
      fetchTemplates(page);
      refreshRepoInfo();
    } catch {
      message.error('移除失败');
    }
  };

  // ---- 批量移除 ----
  const handleBatchRemove = async () => {
    if (!selectedRowKeys.length) { message.warning('请先勾选要移除的题目'); return; }
    setRemoving(true);
    try {
      await unbindTemplate({ repoId, ids: selectedRowKeys });
      message.success(`已移除 ${selectedRowKeys.length} 道题目`);
      setSelectedRowKeys([]);
      fetchTemplates(page);
      refreshRepoInfo();
    } catch {
      message.error('批量移除失败');
    } finally {
      setRemoving(false);
    }
  };

  // ---- Excel 导入题目（归属当前练习） ----
  const handleImport = (file) => {
    setImporting(true);
    importTemplate({ file, repoId })
      .then(() => {
        message.success('题目导入成功');
        fetchTemplates(1);
        refreshRepoInfo();
      })
      .catch((err) => message.error(err?.message || '导入失败'))
      .finally(() => setImporting(false));
    return false; // 阻止 antd 自动上传
  };

  // ---- 保存分值（分值列行内编辑失焦触发，写题目 template.attribute） ----
  // 返回是否保存成功；成功后仅局部更新当前行，避免整页刷新打断连续编辑
  const persistScore = async (record, newAttr) => {
    const schema = record.template || {};
    try {
      await updateTemplate({
        id: record.id,
        questionType: record.questionType,
        template: { ...schema, attribute: newAttr },
      });
      setTemplates((prev) => prev.map((t) => (
        t.id === record.id ? { ...t, template: { ...(t.template || {}), attribute: newAttr } } : t
      )));
      message.success('分值已保存');
      return true;
    } catch {
      message.error('保存失败');
      return false;
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
      render: (t) => <Tag>{TYPE_LABELS[t] || t}</Tag>,
    },
    {
      title: (
        <Space size={2}>
          分值
          <Tooltip title="直接在格内填写，失焦/回车自动保存。多项填空请按每个空分别填写，全部填完自动保存（整题分=各空之和）。">
            <QuestionCircleOutlined style={{ color: '#999' }} />
          </Tooltip>
        </Space>
      ),
      width: 190,
      render: (_, record) => <ScoreCell record={record} onSaveAttr={(attr) => persistScore(record, attr)} />,
    },
    {
      title: '正确答案', width: 120, render: (_, r) => renderAnswer(r),
    },
    {
      title: '标签', dataIndex: 'tag', width: 150,
      render: (tags) => (!tags?.length ? '-' : tags.slice(0, 2).map((t) => <Tag key={t} color="blue">{t}</Tag>)),
    },
    {
      title: '操作', width: 90,
      render: (_, record) => (
        <Popconfirm
          title="确定从练习移除该题？"
          description="题目仍保留在「题目管理」中，可从练习重新选择加入"
          onConfirm={() => handleRemoveTemplate(record.id)}
          okText="移除" cancelText="取消"
        >
          <Button size="small" type="link" danger icon={<DeleteOutlined />}>移除</Button>
        </Popconfirm>
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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/repos')}>返回练习列表</Button>
          <Title level={4} style={{ margin: 0 }}>
            <BookOutlined style={{ marginRight: 8 }} />{repo?.name || '练习详情'}
          </Title>
        </Space>
        <Upload
          beforeUpload={handleImport}
          showUploadList={false}
          accept=".xlsx,.xls"
          disabled={importing}
        >
          <Button type="primary" icon={<ImportOutlined />} loading={importing}>导入题目</Button>
        </Upload>
      </div>

      {/* ---- 练习信息 ---- */}
      {repo && (
        <Card size="small" style={{ marginBottom: 16 }}>
          <Descriptions size="small" column={4}>
            <Descriptions.Item label="题目总数">{repo.total || 0}</Descriptions.Item>
            <Descriptions.Item label="学科">{repo.subject ? <Tag color="geekblue">{repo.subject}</Tag> : '-'}</Descriptions.Item>
            <Descriptions.Item label="年级">{repo.grade ? <Tag color="purple">{repo.grade}</Tag> : '-'}</Descriptions.Item>
            <Descriptions.Item label="难度">
              {repo.difficulty ? (
                <Tag color={repo.difficulty === 'hard' ? 'red' : repo.difficulty === 'medium' ? 'orange' : 'green'}>
                  {repo.difficulty === 'easy' ? '简单' : repo.difficulty === 'medium' ? '中等' : '困难'}
                </Tag>
              ) : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="描述" span={3}>{repo.description || '-'}</Descriptions.Item>
          </Descriptions>
          {repo.tag?.length > 0 && (
            <div style={{ marginTop: 8 }}>{repo.tag.map((t) => <Tag key={t} color="blue">{t}</Tag>)}</div>
          )}
        </Card>
      )}

      {/* ---- 知识绑定（投放位置回显） ---- */}
      <Card
        size="small"
        style={{ marginBottom: 12 }}
        loading={bindingsLoading}
        title={(
          <Space>
            <ApartmentOutlined />
            <Text strong>知识绑定（该练习投放在哪些小节）</Text>
          </Space>
        )}
        extra={<a onClick={() => navigate('/exercise/list')}>前往教研平台维护绑定</a>}
      >
        {bindings.length === 0 ? (
          <Text type="secondary">
            暂未绑定到任何小节。
          </Text>
        ) : (
          bindings.map((b) => {
            const ctx = [b.grade, b.term, b.version].filter(Boolean).join(' · ');
            return (
              <div key={`SECTION-${b.nodeId}`} style={{ marginBottom: 6 }}>
                <Tag color="purple" icon={<PartitionOutlined />}>小节</Tag>
                <a onClick={() => navigate(`/exercise/list?chapterId=${b.parentNodeId || ''}&sectionId=${b.nodeId}`)}>
                  {b.parentNodeName ? `${b.parentNodeName} / ${b.nodeName}` : b.nodeName}
                </a>
                {ctx ? <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>{ctx}</Text> : null}
              </div>
            );
          })
        )}
      </Card>

      {/* ---- 题目列表 ---- */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
        <Text strong style={{ fontSize: 15 }}>题目列表（{total}）</Text>
        <Space>
          {selectedRowKeys.length > 0 && (
            <Button
              danger size="small" icon={<DeleteOutlined />} loading={removing}
              onClick={handleBatchRemove}
            >
              批量移除（{selectedRowKeys.length}）
            </Button>
          )}
          <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => setSelectOpen(true)}>
            批量选择题目
          </Button>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={templates}
        rowKey="id"
        loading={loading}
        size="small"
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        expandable={{
          expandedRowRender,
          rowExpandable: (r) => !!(r.template?.attribute?.examAnalysis),
        }}
        pagination={{
          current: page, total, pageSize, showTotal: (t) => `共 ${t} 题`,
          onChange: (p) => { setPage(p); fetchTemplates(p); },
        }}
        scroll={{ y: 'calc(100vh - 400px)' }}
      />

      {/* ---- 空状态提示 ---- */}
      {!loading && templates.length === 0 && (
        <div style={{ textAlign: 'center', padding: '16px 0', color: '#999' }}>
          练习暂无题目。题目统一在「题目管理」中创建，创建后点击「批量选择题目」加入本练习。
        </div>
      )}

      {/* ---- 批量选择题目弹窗 ---- */}
      <SelectTemplateModal
        open={selectOpen}
        repoId={repoId}
        onCancel={() => setSelectOpen(false)}
        onSuccess={handleSelectSuccess}
      />
    </div>
  );
}
