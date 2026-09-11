/**
 * SelectTemplateModal.jsx - 练习批量选择题目弹窗
 *
 * 功能:
 *   从题目管理（全局题目库）中批量勾选题目，绑定到当前练习。
 *   题目信息统一来源于题目管理板块，本弹窗只做"选择 + 绑定"，不提供创建/编辑题目入口。
 *
 * 筛选:
 *   级联维度按题目"有效归属"逐级收窄: 学科 → 年级 → 章节 → 小节 → 知识点
 *   （题目行无"上下册"字段，该级不设；每一级选项都从当前可选题目中统计真实出现的值并带数量）
 *   有效归属 = 题目自身 subject/chapter/section 与「其知识点名称回溯到的知识结构」的并集，
 *   因此按小节名也能筛出仅通过知识点关联到该小节的题目。
 *   另支持名称关键词 / 题型筛选。
 *
 * 数据流:
 *   打开: 并行 listTemplate({current:1, pageSize:500}) + listKnowledgePoints({current:1,pageSize:10000})
 *         → 用知识点列表构建 kpName → {subject,chapter,section} 映射并计算每题有效归属
 *   过滤: 前端排除已绑定当前练习的题目（record.repoId === repoId → 不在列表中）
 *   确认: bindTemplate({repoId, ids}) → POST /api/repo/bind → onSuccess() 刷新练习题目列表
 *
 * 被谁引用: RepoDetailPage（练习详情页「批量选择题目」按钮）
 *
 * URL: 无独立路由，Modal 形式挂载在 RepoDetailPage 内
 */

import { useState, useEffect, useMemo } from 'react';
import { Modal, Table, Input, Select, Tag, Button, message, Typography, Space } from 'antd';
import { SearchOutlined, ReloadOutlined, ClearOutlined } from '@ant-design/icons';
import { listTemplate } from '../../api/template';
import { listKnowledgePoints } from '../../api/knowledge';
import { bindTemplate } from '../../api/repo';
import { EXAM_TYPES, TYPE_LABELS } from '../../utils/questionTypes';

const { Text } = Typography;

// 题型筛选选项: 题库限定五类（题目管理只能新建五类，见 utils/questionTypes.js）
const TYPE_OPTIONS = EXAM_TYPES;

// 级联维度顺序（自上而下逐级收窄，去掉了题目行不存在的"上下册"级）
const LEVEL_KEYS = ['subject', 'grade', 'chapter', 'section', 'knowledgePoint'];
const LEVEL_LABELS = { subject: '学科', grade: '年级', chapter: '章节', section: '小节', knowledgePoint: '知识点' };

// 有效归属字段：学科/章节/小节在自身值基础上并集知识点回溯到的知识结构值
const EFFECTIVE_FIELD = { subject: '_effSubject', chapter: '_effChapter', section: '_effSection' };

// 统计给定字段在列表中出现的去重取值及数量（多值字段逐条命中计次）
function distinctValues(list, field) {
  const key = EFFECTIVE_FIELD[field] || field;
  const map = {};
  list.forEach((t) => {
    const raw = t[key];
    const vals = Array.isArray(raw) ? raw : [raw];
    (vals || []).forEach((v) => {
      const s = (v || '').toString().trim();
      if (s) map[s] = (map[s] || 0) + 1;
    });
  });
  return Object.keys(map)
    .sort((a, b) => a.localeCompare(b, 'zh'))
    .map((v) => ({ value: v, count: map[v] }));
}

// 行内字段是否命中某个值（多值字段任一命中即算匹配）
function matchField(t, field, val) {
  if (!val) return true;
  const key = EFFECTIVE_FIELD[field] || field;
  const raw = t[key];
  if (Array.isArray(raw)) return raw.some((x) => (x || '').toString().trim() === val);
  return (raw || '').toString().trim() === val;
}

// 构建 知识点名称 → 知识结构（学科/章节/小节）映射
function buildKpMap(points) {
  const map = {};
  (points || []).forEach((p) => {
    const name = (p.name || '').trim();
    if (!name) return;
    if (!map[name]) map[name] = [];
    map[name].push({ subject: p.subjectName || '', chapter: p.chapterName || '', section: p.sectionName || '' });
  });
  return map;
}

// 计算题目有效归属：自身字段 ∪ 知识点回溯字段（按名称去重）
function enrichTemplate(t, kpMap) {
  const kpVals = Array.isArray(t.knowledgePoint) ? t.knowledgePoint : [t.knowledgePoint];
  const subjects = new Set([t.subject].filter(Boolean));
  const chapters = new Set([t.chapter].filter(Boolean));
  const sections = new Set([t.section].filter(Boolean));
  kpVals.forEach((k) => {
    const name = (k || '').toString().trim();
    (kpMap[name] || []).forEach((m) => {
      if (m.subject) subjects.add(m.subject);
      if (m.chapter) chapters.add(m.chapter);
      if (m.section) sections.add(m.section);
    });
  });
  return { ...t, _effSubject: [...subjects], _effChapter: [...chapters], _effSection: [...sections] };
}

export default function SelectTemplateModal({ open, repoId, onCancel, onSuccess }) {
  // ---- 状态 ----
  const [allTemplates, setAllTemplates] = useState([]);   // 全量题目（不含已绑定本练习）
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState('');             // 名称搜索
  const [qType, setQType] = useState(undefined);          // 题型筛选
  const [filters, setFilters] = useState({ subject: '', grade: '', chapter: '', section: '', knowledgePoint: '' });
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [confirmLoading, setConfirmLoading] = useState(false);

  // ---- 加载全量题目 + 知识点结构（题目管理全局库） ----
  const fetchAll = async () => {
    setLoading(true);
    try {
      const [tplRes, kpRes] = await Promise.all([
        listTemplate({ current: 1, pageSize: 500 }),
        listKnowledgePoints({ current: 1, pageSize: 10000 }).catch(() => ({ data: { list: [] } })),
      ]);
      const kpMap = buildKpMap(kpRes.data?.list || []);
      const list = tplRes.data?.list || [];
      // 排除已绑定当前练习的题目（前端过滤；数据量可控时一次性加载更利于勾选跨页）
      const others = list.filter((t) => t.repoId !== repoId).map((t) => enrichTemplate(t, kpMap));
      setAllTemplates(others);
      // 关闭已有选中（题目归属可能已变化）
      setSelectedRowKeys((prev) => prev.filter((id) => others.some((t) => t.id === id)));
    } catch {
      message.error('加载题目失败');
    } finally {
      setLoading(false);
    }
  };

  // 打开时重新加载并重置全部筛选
  useEffect(() => {
    if (open) {
      setKeyword('');
      setQType(undefined);
      setFilters({ subject: '', grade: '', chapter: '', section: '', knowledgePoint: '' });
      setSelectedRowKeys([]);
      fetchAll();
    }
  }, [open]); // eslint-disable-line

  // ---- 基础筛选（关键词 + 题型） ----
  const base = useMemo(() => {
    let list = allTemplates;
    if (keyword.trim()) {
      const kw = keyword.trim().toLowerCase();
      list = list.filter((t) => (t.name || '').toLowerCase().includes(kw));
    }
    if (qType) {
      list = list.filter((t) => t.questionType === qType);
    }
    return list;
  }, [allTemplates, keyword, qType]);

  // ---- 级联收窄 ----
  // 第 i 级选项 = 前 i-1 级已选条件下，base 中第 i 级字段的去重取值；
  // 全部级选完后的行 = 最终展示的数据源。
  const cascade = useMemo(() => {
    const stageRows = {};
    let rows = base;
    LEVEL_KEYS.forEach((key) => {
      stageRows[key] = rows;
      const val = filters[key];
      if (val) rows = rows.filter((t) => matchField(t, key, val));
    });
    return { stageRows, finalRows: rows };
  }, [base, filters]);

  // 每一级 Select 的 options（带数量提示）
  const levelOptions = useMemo(() => {
    const opts = {};
    LEVEL_KEYS.forEach((key) => {
      opts[key] = distinctValues(cascade.stageRows[key], key).map(({ value, count }) => ({
        value,
        label: `${value}（${count}）`,
      }));
    });
    return opts;
  }, [cascade]);

  // 级联选择: 修改某级后清空其下级
  const handleLevelChange = (key) => (val) => {
    const idx = LEVEL_KEYS.indexOf(key);
    setFilters((prev) => {
      const next = { ...prev, [key]: val };
      LEVEL_KEYS.slice(idx + 1).forEach((k) => { next[k] = ''; });
      return next;
    });
  };

  const clearAllFilters = () => {
    setKeyword('');
    setQType(undefined);
    setFilters({ subject: '', grade: '', chapter: '', section: '', knowledgePoint: '' });
  };

  // ---- 确认绑定 ----
  const handleConfirm = async () => {
    if (!selectedRowKeys.length) { message.warning('请先勾选要加入练习的题目'); return; }
    setConfirmLoading(true);
    try {
      await bindTemplate({ repoId, ids: selectedRowKeys });
      message.success(`已加入 ${selectedRowKeys.length} 道题目`);
      onSuccess();
    } catch {
      message.error('绑定失败');
    } finally {
      setConfirmLoading(false);
    }
  };

  // ---- 表格列 ----
  const columns = [
    {
      title: '题目', dataIndex: 'name', ellipsis: true,
      render: (text, r) => {
        const attr = r.template?.attribute || {};
        return (
          <Space size={4}>
            <span>{text}</span>
            {attr.examCorrectAnswer && <Tag color="green" style={{ fontSize: 10, lineHeight: '16px' }}>答案</Tag>}
            {attr.examAnalysis && <Tag color="orange" style={{ fontSize: 10, lineHeight: '16px' }}>解析</Tag>}
          </Space>
        );
      },
    },
    {
      title: '题型', dataIndex: 'questionType', width: 92,
      render: (t) => <Tag>{TYPE_LABELS[t] || t}</Tag>,
    },
    {
      title: '学科', dataIndex: 'subject', width: 70, align: 'center',
      render: (v) => v || '-',
    },
    {
      title: '年级', dataIndex: 'grade', width: 82, align: 'center',
      render: (v) => v || '-',
    },
    {
      title: '分值', width: 70, align: 'center',
      render: (_, r) => r.template?.attribute?.examScore || '-',
    },
    {
      title: '归属', dataIndex: 'repoId', width: 90, align: 'center',
      render: (rid) => (rid ? <Tag color="blue">其他练习</Tag> : <Text type="secondary">未绑定</Text>),
    },
  ];

  // 级联 Select 渲染（同一行，自动换行）
  const levelSelects = LEVEL_KEYS.map((key) => {
    const used = filters[key];
    const value = used || undefined;
    return (
      <Select
        key={key}
        allowClear
        showSearch
        value={value}
        onChange={handleLevelChange(key)}
        placeholder={LEVEL_LABELS[key]}
        options={levelOptions[key]}
        style={{ width: key === 'knowledgePoint' ? 170 : 132 }}
        optionFilterProp="label"
        notFoundContent="无可用选项"
      />
    );
  });

  const hasActiveFilter = keyword.trim() || qType || LEVEL_KEYS.some((k) => filters[k]);

  return (
    <Modal
      title="批量选择题目"
      open={open}
      onCancel={onCancel}
      onOk={handleConfirm}
      confirmLoading={confirmLoading}
      okText={`加入练习（${selectedRowKeys.length}）`}
      cancelText="取消"
      width={920}
      destroyOnHidden
    >
      {/* ---- 说明 ---- */}
      <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
        从题目管理中勾选已有题目加入本练习。可依次按 学科 → 年级 → 章节 → 小节 → 知识点 级联筛选，不选则显示全部。
      </Text>

      {/* ---- 级联筛选栏 ---- */}
      <Space wrap style={{ marginBottom: 8, display: 'flex' }}>
        {levelSelects}
        {hasActiveFilter && (
          <Button size="small" icon={<ClearOutlined />} onClick={clearAllFilters}>
            清除筛选
          </Button>
        )}
      </Space>

      {/* ---- 关键词 / 题型 / 刷新 / 计数 ---- */}
      <Space style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
        <Space>
          <Input
            allowClear
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="搜索题目名称"
            prefix={<SearchOutlined />}
            style={{ width: 220 }}
          />
          <Select
            allowClear
            value={qType}
            onChange={setQType}
            placeholder="题型"
            style={{ width: 130 }}
            options={TYPE_OPTIONS.map((t) => ({ label: t.label, value: t.value }))}
          />
          <Button icon={<ReloadOutlined />} onClick={fetchAll}>刷新</Button>
        </Space>
        <Text type="secondary">可选 {cascade.finalRows.length} 题（已绑定本练习的题目不在列表中）</Text>
      </Space>

      {/* ---- 题目表格 ---- */}
      <Table
        rowKey="id"
        size="small"
        loading={loading}
        dataSource={cascade.finalRows}
        columns={columns}
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        pagination={{ pageSize: 10, showTotal: (t) => `共 ${t} 题` }}
        scroll={{ y: 380 }}
      />

      {/* ---- 空状态提示 ---- */}
      {!loading && cascade.finalRows.length === 0 && (
        <div style={{ textAlign: 'center', padding: '24px 0', color: '#999' }}>
          {allTemplates.length === 0
            ? '题目管理中没有可选题目，请先到「题目管理」创建题目后再回来选择。'
            : '当前筛选条件下没有题目，请调整学科/年级/章节/小节/知识点或清除筛选。'}
        </div>
      )}
    </Modal>
  );
}
