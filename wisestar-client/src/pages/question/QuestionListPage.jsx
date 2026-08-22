/**
 * QuestionListPage.jsx - 题目管理主页
 *
 * 功能:
 *   1. 全量题目列表（跨所有练习），支持分页、搜索、筛选
 *   2. 新建/编辑题目弹窗（带答案、解析、图片上传）
 *   3. Excel 批量导入题目
 *   4. Excel 批量导出题目
 *   5. 批量删除题目
 *   6. 表格中展示题目信息：标题、图片、题型、所属练习、分值、正确答案、标签
 *
 * URL: /questions（受 AuthGuard 保护）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏"题目管理"菜单进入
 *
 * 筛选维度说明（重点）:
 *   支持"学科 / 年级 / 章节 / 难度 / 知识点"五维筛选（加上题型、练习、名称共 8 个条件），
 *   全部通过 GET /api/template/list 的 query 参数下发给后端做 AND 组合查询。
 *   知识点属性字段在题目对象上的来源（两处均可能）:
 *     - 顶层字段: record.subject / record.grade / record.chapter / record.knowledgePoint / record.difficulty
 *       （t_template 表字段，QuestionEditModal 保存时写入）
 *     - template.attribute 快照: record.template.attribute.subject 等
 *       （兼容旧数据；表格"知识点"列两者都读，优先顶层字段）
 *
 * 数据流:
 *   加载: fetchData → listTemplate(params) → GET /api/template/list → 渲染表格
 *   新建/编辑: QuestionEditModal → onSave(payload) → createTemplate/updateTemplate
 *   → POST /api/template/create|update → 刷新列表
 *   导入: ImportModal → importTemplate → POST /api/repo/import
 *   导出: handleExport → exportTemplate(全部筛选条件) → GET /api/repo/export?… → 下载 xlsx
 *   （导出内容与当前筛选结果一致，含名称/题型/学科/年级/章节/难度/知识点条件）
 *   删除: deleteTemplate({ids}) → POST /api/template/delete（单个/批量）
 */

import { useState, useEffect, useCallback } from 'react';
import {
  Table, Space, Button, Input, Select, Typography, Tag, Popconfirm,
  message, Image, Tooltip,
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, EditOutlined, ImportOutlined,
  ExportOutlined, SearchOutlined, PictureOutlined, CheckCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { listTemplate, createTemplate, updateTemplate, deleteTemplate } from '../../api/template';
import { listRepo, exportTemplate } from '../../api/repo';
import QuestionEditModal from '../../components/question/QuestionEditModal';
import ImportModal from '../../components/question/ImportModal';
import { QUESTION_TYPES } from '../../utils/surveyHelpers';
import { usePermission } from '../../utils/usePermission';

const { Title, Text } = Typography;

// 完整题型映射（含判断题）
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Select: '下拉题',
  FillBlank: '填空题', Text: '多行文本', Score: '评分题',
  Remark: '备注说明', Judge: '判断题',
};

export default function QuestionListPage() {
  const { can } = usePermission();
  // ---- 列表状态 ----
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState([]);   // 当前页题目数据（TemplateView 数组）
  const [total, setTotal] = useState(0);  // 符合条件的题目总数
  const [page, setPage] = useState(1);    // 当前页码
  const pageSize = 20;                    // 每页 15 条（固定）

  // ---- 筛选状态 ----
  // 名称搜索 / 题型 / 练习 三个常规条件
  const [keyword, setKeyword] = useState('');
  const [filterType, setFilterType] = useState(undefined);
  const [filterRepoId, setFilterRepoId] = useState(undefined);
  // 知识点属性筛选: 学科 / 章节 / 年级 / 难度 / 知识点
  const [filterSubject, setFilterSubject] = useState('');
  const [filterGrade, setFilterGrade] = useState('');
  const [filterChapter, setFilterChapter] = useState('');
  const [filterDifficulty, setFilterDifficulty] = useState(undefined);
  const [filterKnowledgePoint, setFilterKnowledgePoint] = useState('');
  const [repos, setRepos] = useState([]);               // 全量练习列表（供筛选下拉）
  const [allReposCache, setAllReposCache] = useState([]); // 编辑弹窗用的练习列表（全量）

  // ---- 选中行 ----
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);

  // ---- 弹窗状态 ----
  const [editOpen, setEditOpen] = useState(false);   // 新建/编辑弹窗
  const [editRecord, setEditRecord] = useState(null); // 编辑时传入的题目记录（null=新建）
  const [importOpen, setImportOpen] = useState(false); // Excel 导入弹窗

  // ---- 加载练习列表（全量，供筛选下拉 + 编辑弹窗练习选择） ----
  // pageSize=200 一次性拉取，业务上练习数量级不大，够用
  useEffect(() => {
    (async () => {
      try {
        const res = await listRepo({ current: 1, pageSize: 200 });
        const list = res.data?.list || [];
        setRepos(list);
        setAllReposCache(list);
      } catch { /* silent */ }
    })();
  }, []);

  // ---- 加载题目列表 ----
  // 核心查询函数: 把 7 个筛选条件组装进 params 后调用 GET /api/template/list
  // 所有条件都为 AND 关系（由后端 SQL 组合查询）
  const fetchData = useCallback(async (p = page) => {
    setLoading(true);
    try {
      const params = { current: p, pageSize };
      if (keyword.trim()) params.name = keyword.trim();                    // 名称模糊搜索
      if (filterType) params.questionType = filterType;                    // 题型过滤
      if (filterRepoId) params.repoId = filterRepoId;                      // 练习过滤
      if (filterSubject.trim()) params.subject = filterSubject.trim();     // 学科过滤
      if (filterGrade.trim()) params.grade = filterGrade.trim();           // 年级过滤
      if (filterChapter.trim()) params.chapter = filterChapter.trim();     // 章节过滤
      if (filterDifficulty) params.difficulty = filterDifficulty;          // 难度过滤（easy/medium/hard）
      if (filterKnowledgePoint.trim()) params.knowledgePoint = filterKnowledgePoint.trim(); // 知识点过滤
      const res = await listTemplate(params);
      setData(res.data?.list || []);
      setTotal(res.data?.total || 0);
    } catch {
      message.error('加载题目失败');
    } finally {
      setLoading(false);
    }
  }, [page, keyword, filterType, filterRepoId, filterSubject, filterGrade, filterChapter, filterDifficulty, filterKnowledgePoint]); // eslint-disable-line react-hooks/exhaustive-deps

  // 任一筛选条件 / 页码变化时自动重新拉取（输入框 onChange 同时 setPage(1) 保证从首页开始）
  useEffect(() => { fetchData(page); }, [page, keyword, filterType, filterRepoId, filterSubject, filterGrade, filterChapter, filterDifficulty, filterKnowledgePoint, fetchData]);

  // ---- 新建 ----
  // editRecord 置 null → 弹窗进入"新建模式"（清空表单）
  const handleCreate = () => {
    setEditRecord(null);
    setEditOpen(true);
  };

  // ---- 编辑 ----
  // editRecord 置为该行记录 → 弹窗进入"编辑模式"（回填表单）
  const handleEdit = (record) => {
    setEditRecord(record);
    setEditOpen(true);
  };

  // ---- 保存回调 ----
  // 由 QuestionEditModal.handleSave 触发，payload 为组装好的 TemplateRequest
  // 有 id → updateTemplate，无 id → createTemplate
  const handleSave = async (payload) => {
    try {
      if (payload.id) {
        await updateTemplate(payload);
        message.success('题目已更新');
      } else {
        await createTemplate(payload);
        message.success('题目已创建');
      }
      setEditOpen(false);
      fetchData(page);
    } catch {
      message.error('保存失败');
    }
  };

  // ---- 删除单个 ----
  // deleteTemplate 统一接收 { ids: [] }，单删也包装成数组
  const handleDelete = async (id) => {
    try {
      await deleteTemplate({ ids: [id] });
      message.success('已删除');
      // 同步清理选中集合，避免删除的行仍留在勾选状态
      setSelectedRowKeys((prev) => prev.filter((k) => k !== id));
      fetchData(page);
    } catch {
      message.error('删除失败');
    }
  };

  // ---- 批量删除 ----
  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) { message.warning('请先选择题目'); return; }
    try {
      await deleteTemplate({ ids: selectedRowKeys });
      message.success(`已删除 ${selectedRowKeys.length} 道题目`);
      setSelectedRowKeys([]);
      fetchData(page);
    } catch {
      message.error('批量删除失败');
    }
  };

  // ---- 导出当前筛选结果 ----
  // 把题目管理页的筛选条件（练习/名称/题型/学科/年级/章节/难度/知识点）全部传给
  // 导出接口，保证导出内容与当前筛选结果一致（后端 exportRepoQuestions 支持这些条件）
  // 数据流: 本页 → exportTemplate(filters) → GET /api/repo/export?… → 浏览器下载 xlsx
  const handleExport = () => {
    exportTemplate({
      repoId: filterRepoId,
      name: keyword.trim() || undefined,
      questionType: filterType,
      subject: filterSubject.trim() || undefined,
      grade: filterGrade.trim() || undefined,
      chapter: filterChapter.trim() || undefined,
      difficulty: filterDifficulty,
      knowledgePoint: filterKnowledgePoint.trim() || undefined,
    });
    message.info('正在导出...');
  };

  // ---- 导入成功回调 ----
  // 导入完成后回到第 1 页刷新，保证新导入的题目可见
  const handleImportSuccess = () => {
    fetchData(1);
  };

  // ---- 表格列配置 ----
  const columns = [
    { title: '#', width: 50, align: 'center', render: (_, __, idx) => (page - 1) * pageSize + idx + 1 },
    {
      title: '题目', dataIndex: 'name', ellipsis: true,
      render: (text, record) => {
        const attr = record.template?.attribute || {};
        const hasAnswer = !!attr.examCorrectAnswer;
        const hasAnalysis = !!attr.examAnalysis;
        const hasImages = (attr.examImages || []).length > 0;
        return (
          <Space size={4} wrap>
            <span>{text}</span>
            {hasAnswer && <Tag color="green" style={{ fontSize: 10, lineHeight: '16px' }}>答案</Tag>}
            {hasAnalysis && <Tag color="orange" style={{ fontSize: 10, lineHeight: '16px' }}>解析</Tag>}
            {hasImages && (
              <Tooltip
                title={<Image src={attr.examImages[0]} width={160} />}
                placement="right"
              >
                <Tag color="purple" style={{ fontSize: 10, lineHeight: '16px' }}>
                  <PictureOutlined /> 图
                </Tag>
              </Tooltip>
            )}
          </Space>
        );
      },
    },
    {
      title: '题型', dataIndex: 'questionType', width: 90,
      render: (t) => <Tag>{TYPE_LABELS[t] || t}</Tag>,
    },
    {
      title: '所属练习', dataIndex: 'repoId', width: 120,
      render: (repoId) => {
        const r = repos.find((x) => x.id === repoId);
        return r ? <Tag color="blue">{r.name}</Tag> : <Text type="secondary">未分配</Text>;
      },
    },
    {
      title: '知识点', width: 220,
      // 知识点列: 展示学科/章节/知识点/年级/难度 5 类标签
      // 数据来源优先顶层字段（t_template 表），其次 template.attribute 快照（兼容旧数据）
      render: (_, r) => {
        const subject = r.subject || r.template?.attribute?.subject;
        const grade = r.grade || r.template?.attribute?.grade;
        const chapter = r.chapter || r.template?.attribute?.chapter;
        const kps = r.knowledgePoint || r.template?.attribute?.knowledgePoint || [];
        const difficulty = r.difficulty || r.template?.attribute?.difficulty;
        // 5 项全空时显示占位符 "-"
        if (!subject && !grade && !chapter && kps.length === 0 && !difficulty) {
          return <Text type="secondary">-</Text>;
        }
        return (
          <Space size={4} wrap>
            {/* 学科标签（青色） */}
            {subject && <Tag color="cyan" style={{ fontSize: 10, lineHeight: '16px' }}>{subject}</Tag>}
            {/* 年级标签（紫色） */}
            {grade && <Tag color="purple" style={{ fontSize: 10, lineHeight: '16px' }}>{grade}</Tag>}
            {/* 章节标签（蓝灰色） */}
            {chapter && <Tag color="geekblue" style={{ fontSize: 10, lineHeight: '16px' }}>{chapter}</Tag>}
            {/* 知识点: 最多展示 2 个，超出显示 "+N"（防止列宽撑爆） */}
            {kps.slice(0, 2).map((kp) => (
              <Tag key={kp} style={{ fontSize: 10, lineHeight: '16px' }}>{kp}</Tag>
            ))}
            {kps.length > 2 && <Tag style={{ fontSize: 10, lineHeight: '16px' }}>+{kps.length - 2}</Tag>}
            {/* 难度标签: 简单=绿 / 中等=橙 / 困难=红 */}
            {difficulty && (
              <Tag color={difficulty === 'hard' ? 'red' : difficulty === 'medium' ? 'orange' : 'green'}
                style={{ fontSize: 10, lineHeight: '16px' }}>
                {difficulty === 'easy' ? '简单' : difficulty === 'medium' ? '中等' : '困难'}
              </Tag>
            )}
          </Space>
        );
      },
    },
    {
      title: '分值', width: 60, align: 'center',
      render: (_, r) => {
        const s = r.template?.attribute?.examScore;
        return s ? <Text strong>{s}</Text> : '-';
      },
    },
    {
      title: '正确答案', width: 120, render: (_, r) => {
        const correct = r.template?.attribute?.examCorrectAnswer;
        if (!correct) return <Text type="secondary">-</Text>;
        return <Tag color="green" icon={<CheckCircleOutlined />}>{correct}</Tag>;
      },
    },
    {
      title: '标签', dataIndex: 'tag', width: 160,
      render: (tags) => {
        if (!tags?.length) return '-';
        return tags.slice(0, 3).map((t) => <Tag key={t} color="blue" style={{ fontSize: 11 }}>{t}</Tag>);
      },
    },
    {
      title: '操作', width: 120, fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          {can('template:update') && (
            <Button size="small" type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
              编辑
            </Button>
          )}
          {can('template:delete') && (
            <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)} okText="删除" cancelText="取消">
              <Button size="small" type="link" danger icon={<DeleteOutlined />}>删除</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* ---- 页面标题 ---- */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>题目管理</Title>
        <Space>
          <Button icon={<ImportOutlined />} onClick={() => setImportOpen(true)}>Excel 导入</Button>
          <Button icon={<ExportOutlined />} onClick={handleExport}>导出</Button>
          {can('template:create') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新建题目</Button>
          )}
        </Space>
      </div>

      {/* ---- 筛选栏 ---- */}
      {/* 7 个筛选条件 + 重置 + 批量删除；所有 Input/Select onChange 都同时 setPage(1)，
          保证筛选生效后回到第 1 页（避免停在无数据的深页码） */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 12, flexWrap: 'wrap', alignItems: 'center' }}>
        {/* 名称搜索 */}
        <Input
          prefix={<SearchOutlined />}
          placeholder="搜索题目名称"
          value={keyword}
          onChange={(e) => { setKeyword(e.target.value); setPage(1); }}
          style={{ width: 240 }}
          allowClear
        />
        {/* 按题型筛选 */}
        <Select
          value={filterType}
          onChange={(v) => { setFilterType(v); setPage(1); }}
          placeholder="按题型筛选"
          allowClear
          style={{ width: 140 }}
          options={[
            ...QUESTION_TYPES,
            { label: '判断题', value: 'Judge' },
          ]}
        />
        {/* 按练习筛选 */}
        <Select
          value={filterRepoId}
          onChange={(v) => { setFilterRepoId(v); setPage(1); }}
          placeholder="按练习筛选"
          allowClear
          style={{ width: 180 }}
          options={repos.map((r) => ({ label: r.name, value: r.id }))}
        />
        {/* 学科筛选（五维筛选之一） */}
        <Input
          prefix={<SearchOutlined />}
          placeholder="学科"
          value={filterSubject}
          onChange={(e) => { setFilterSubject(e.target.value); setPage(1); }}
          style={{ width: 120 }}
          allowClear
        />
        {/* 年级筛选（五维筛选之一） */}
        <Input
          prefix={<SearchOutlined />}
          placeholder="年级"
          value={filterGrade}
          onChange={(e) => { setFilterGrade(e.target.value); setPage(1); }}
          style={{ width: 120 }}
          allowClear
        />
        {/* 章节筛选（五维筛选之一） */}
        <Input
          prefix={<SearchOutlined />}
          placeholder="章节"
          value={filterChapter}
          onChange={(e) => { setFilterChapter(e.target.value); setPage(1); }}
          style={{ width: 120 }}
          allowClear
        />
        {/* 难度筛选（五维筛选之一） */}
        <Select
          value={filterDifficulty}
          onChange={(v) => { setFilterDifficulty(v); setPage(1); }}
          placeholder="难度"
          allowClear
          style={{ width: 100 }}
          options={[
            { label: '简单', value: 'easy' },
            { label: '中等', value: 'medium' },
            { label: '困难', value: 'hard' },
          ]}
        />
        {/* 知识点筛选（五维筛选之一，模糊匹配） */}
        <Input
          prefix={<SearchOutlined />}
          placeholder="知识点"
          value={filterKnowledgePoint}
          onChange={(e) => { setFilterKnowledgePoint(e.target.value); setPage(1); }}
          style={{ width: 140 }}
          allowClear
        />
        {/* 重置: 清空全部筛选条件并回到第 1 页 */}
        <Button icon={<ReloadOutlined />} onClick={() => {
          setKeyword(''); setFilterType(undefined); setFilterRepoId(undefined);
          setFilterSubject(''); setFilterGrade(''); setFilterChapter(''); setFilterDifficulty(undefined); setFilterKnowledgePoint('');
          setPage(1);
        }}>
          重置
        </Button>

        {/* 批量操作 */}
          {selectedRowKeys.length > 0 && can('template:delete') && (
            <Popconfirm
              title={`确定删除选中的 ${selectedRowKeys.length} 道题目？`}
              onConfirm={handleBatchDelete}
              okText="删除" cancelText="取消"
            >
              <Button danger icon={<DeleteOutlined />}>
                批量删除 ({selectedRowKeys.length})
              </Button>
            </Popconfirm>
          )}
      </div>

      {/* ---- 数据表格 ---- */}
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        size="small"
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        pagination={{
          current: page,
          total,
          pageSize,
          showTotal: (t) => `共 ${t} 道题目`,
          showSizeChanger: false,
          onChange: (p) => setPage(p),
        }}
        scroll={{ x: 900, y: 'calc(100vh - 420px)' }}
      />

      {/* ---- 新建/编辑弹窗 ---- */}
      <QuestionEditModal
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onSave={handleSave}
        record={editRecord}
        repos={allReposCache}
      />

      {/* ---- 导入弹窗 ---- */}
      <ImportModal
        open={importOpen}
        onCancel={() => setImportOpen(false)}
        onSuccess={handleImportSuccess}
        repos={repos}
      />
    </div>
  );
}
