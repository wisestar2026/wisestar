/**
 * ExerciseListPage.jsx - 习题列表页（知识树 × 练习总览）
 *
 * URL: /exercise/list（习题管理 → 习题列表）
 *
 * 功能:
 *   1. 学科 → 章节 → 小节 → 知识点 逐级下钻（级联选择器）
 *   2. 小节节点：展示直绑练习（名称/题量/标签），支持绑定练习、解绑、进练习详情组题
 *      （业务规则：练习仅支持绑定到小节；章节只作为小节的分组维度，不支持直接绑定练习）
 *   3. 知识点节点：展示直绑题目，支持绑定/解绑题目
 *   4. 任一节点可「预览学员端刷题内容」（与学员端 study/questions 同一取题语义），
 *      预览支持开关标准答案，管理端核验绑定配置
 *   5. 练习详情回显（/repos/:id）的绑定位置点击后经 ?chapterId=&sectionId= 定位本页
 *
 * 数据流:
 *   树: listSubjects → listChapters({subjectId}) → listSections({chapterId})
 *       → listKnowledgePoints({sectionId})
 *   绑练习: listSectionRepos 回显（含 total 题量）；saveSectionRepos 全量替换保存
 *   绑题目: listKnowledgePointQuestions 回显 / saveKnowledgePointQuestions 全量替换
 *   预览:   listNodeQuestions({nodeType, nodeId, withAnswer})
 *
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「习题管理 → 习题列表」
 */
import { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Card, Table, Tag, Space, Button, Select, Breadcrumb, Switch, Empty,
  Modal, Input, Popconfirm, Spin, Typography, Alert, message,
} from 'antd';
import {
  ApartmentOutlined, PartitionOutlined, BulbOutlined, BookOutlined,
  EyeOutlined, LinkOutlined, DeleteOutlined, ReloadOutlined, SearchOutlined,
} from '@ant-design/icons';
import { listSubjects, listChapters, listSections, listKnowledgePoints,
  listSectionRepos, saveSectionRepos,
  listKnowledgePointQuestions, saveKnowledgePointQuestions } from '../../api/knowledge';
import { listRepo, listNodeQuestions } from '../../api/repo';
import { listTemplate } from '../../api/template';

const { Text } = Typography;

// ---- 展示映射 ----
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', FillBlank: '单项填空',
  Judge: '判断题', MultipleBlank: '多项填空',
};
const DIFFICULTY_LABELS = {
  easy: ['简单', 'green'], medium: ['中等', 'orange'], hard: ['困难', 'red'],
};

/** 拆分标准答案 token（兼容换行/中英文逗号/顿号/分号/竖线） */
function splitTokens(text) {
  if (!text) return [];
  return String(text).split(/[\n,，、|;；]/).map((s) => s.trim()).filter(Boolean);
}

/** 答案文本 → 选项下标集合（支持字母 A~H、选项标题/ID） */
function tokenToOptionIndexes(token, children) {
  const list = [];
  const letters = splitTokens(token);
  letters.forEach((t) => {
    let idx = -1;
    if (t.length === 1 && /[A-Za-z]/.test(t)) {
      const li = t.toUpperCase().charCodeAt(0) - 65;
      if (li >= 0 && li < children.length) idx = li;
    } else {
      idx = children.findIndex((c) => c.title === t || c.id === t);
    }
    if (idx >= 0) list.push(idx);
  });
  return list;
}

/** 提取题目标准答案信息（供只读预览渲染） */
function parseAnswer(q) {
  const schema = q?.schema || {};
  const attr = schema.attribute || {};
  const children = schema.children || [];
  const qtype = q?.questionType;
  const top = attr.examCorrectAnswer;
  const correctIndexes = [];
  children.forEach((c, i) => {
    const mark = c?.attribute?.examCorrectAnswer;
    if (mark && String(mark).trim()) correctIndexes.push(i);
  });

  // 选项级答案优先（导入题目的规范存储）
  if (correctIndexes.length > 0) {
    if (qtype === 'Judge') {
      const text = children[correctIndexes[0]]?.title
        || children[correctIndexes[0]]?.attribute?.examCorrectAnswer || '';
      return { type: 'judge', text };
    }
    return { type: 'choice', indexes: correctIndexes, text: '' };
  }
  if (!top || !String(top).trim()) {
    if (qtype === 'Judge') return { type: 'judge', text: '' };
    if (qtype === 'FillBlank' || qtype === 'MultipleBlank') return { type: 'fill', text: '' };
    return { type: 'choice', indexes: [], text: '' };
  }
  if (qtype === 'FillBlank' || qtype === 'MultipleBlank') {
    return { type: 'fill', text: String(top).trim() };
  }
  if (qtype === 'Judge') {
    return { type: 'judge', text: String(top).trim() };
  }
  return { type: 'choice', indexes: tokenToOptionIndexes(top, children), text: String(top).trim() };
}

/** 只读题目渲染（预览弹窗用，支持五类题型） */
function ReadonlyQuestion({ q, withAnswer }) {
  const schema = q?.schema || {};
  const children = schema.children || [];
  const qtype = q?.questionType;
  const answer = parseAnswer(q);
  const typeLabel = TYPE_LABELS[qtype] || qtype;
  const diff = DIFFICULTY_LABELS[q?.difficulty];

  return (
    <div style={{ border: '1px solid #f0f0f0', borderRadius: 8, padding: '12px 16px', marginBottom: 12, background: '#fff' }}>
      <Space size={8} style={{ marginBottom: 6 }} wrap>
        <Tag color="blue">{typeLabel}</Tag>
        {diff ? <Tag color={diff[1]}>{diff[0]}</Tag> : null}
      </Space>
      <div style={{ fontSize: 15, marginBottom: 8 }}>{schema.title || q?.name}</div>

      {(qtype === 'Radio' || qtype === 'Checkbox' || (qtype === 'Judge' && children.length > 0)) && (
        <div>
          {children.map((opt, i) => {
            const letter = String.fromCharCode(65 + i);
            const isCorrect = withAnswer && answer.indexes.includes(i);
            return (
              <div key={opt.id || i} style={{
                border: isCorrect ? '2px solid #52c41a' : '1px solid #d9d9d9',
                background: isCorrect ? '#f6ffed' : '#fff',
                borderRadius: 6, padding: '6px 12px', marginBottom: 6, fontSize: 14,
              }}>
                <Text strong>{letter}.</Text> {opt.title || opt.description || ''}
              </div>
            );
          })}
          {withAnswer && answer.text ? (
            <div style={{ marginTop: 4, color: '#52c41a' }}>
              <Text strong>正确答案：</Text>{answer.text}
            </div>
          ) : null}
        </div>
      )}

      {qtype === 'Judge' && children.length === 0 && (
        <div>{withAnswer && answer.text
          ? <div style={{ color: '#52c41a' }}><Text strong>正确答案：</Text>{answer.text}</div>
          : <Text type="secondary">判断题</Text>}</div>
      )}

      {(qtype === 'FillBlank' || qtype === 'MultipleBlank') && (
        <div>
          {withAnswer && answer.text ? (
            qtype === 'MultipleBlank'
              ? <div style={{ color: '#52c41a' }}><Text strong>答案：</Text>
                  {answer.text.split('|').map((p, i) => <span key={i}>空{i + 1}: {p}　</span>)}
                </div>
              : <div style={{ color: '#52c41a' }}><Text strong>答案：</Text>{answer.text}</div>
          ) : (
            <Text type="secondary">填空题（答案预览已隐藏）</Text>
          )}
        </div>
      )}
    </div>
  );
}

/** 节点刷题内容预览弹窗 */
function PreviewModal({ open, scope, nodeId, nodeName, onClose }) {
  const [loading, setLoading] = useState(false);
  const [withAnswer, setWithAnswer] = useState(false);
  const [questions, setQuestions] = useState([]);

  const fetchData = useCallback((showAnswer) => {
    if (!open || !nodeId) return;
    setLoading(true);
    listNodeQuestions({ nodeType: scope, nodeId, withAnswer: showAnswer })
      .then((res) => setQuestions(res?.data || []))
      .catch(() => setQuestions([]))
      .finally(() => setLoading(false));
  }, [open, scope, nodeId]);

  useEffect(() => { fetchData(withAnswer); }, [fetchData, withAnswer]);

  return (
    <Modal
      title={`预览「${nodeName || ''}」学员端刷题内容`}
      open={open} onCancel={onClose} width={760} footer={null}
    >
      <Space style={{ marginBottom: 12 }}>
        <Text type="secondary">
          与学员端同源取题：{scope === 'knowledgePoint' ? '知识点直绑题目'
            : scope === 'repo' ? '练习内题目' : '该范围绑定练习的题目 + 知识点直绑题目'}（最多 100 题）
        </Text>
        <Switch checked={withAnswer} checkedChildren="含答案" unCheckedChildren="隐藏答案"
          onChange={setWithAnswer} />
      </Space>
      {loading ? <div style={{ textAlign: 'center', padding: 30 }}><Spin /></div>
        : questions.length === 0
          ? <Empty description="该节点暂无绑定练习/题目，请先绑定" />
          : <div style={{ maxHeight: '58vh', overflow: 'auto' }}>
              {questions.map((q, i) => (
                <ReadonlyQuestion key={q.id || i} q={q} withAnswer={withAnswer} />
              ))}
            </div>}
    </Modal>
  );
}

/** 绑定练习弹窗（小节：练习库多选，全量替换；练习仅支持绑定到小节） */
function BindReposModal({ open, node, onClose, onSaved }) {
  const [repos, setRepos] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [selected, setSelected] = useState([]);

  const fetchRepos = (p, kw) => {
    setLoading(true);
    listRepo({ current: p, pageSize: 8, name: kw || undefined })
      .then((res) => { setRepos(res?.data?.list || []); setTotal(res?.data?.total || 0); })
      .catch(() => { setRepos([]); setTotal(0); })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (!open || !node?.id) return;
    setSelected([]);
    setPage(1); setKeyword('');
    fetchRepos(1, '');
    listSectionRepos(node.id).then((res) => setSelected((res?.data || []).map((r) => r.id))).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, node?.id]);

  const save = () => {
    setSaving(true);
    saveSectionRepos({ sectionId: node.id, repoIds: selected })
      .then(() => { message.success('练习绑定已保存'); onClose(); onSaved && onSaved(); })
      .finally(() => setSaving(false));
  };

  const columns = [{
    title: '练习名称', dataIndex: 'name',
    render: (n, r) => <Space><BookOutlined /><Text strong>{n}</Text></Space>,
  }, {
    title: '题量', dataIndex: 'total', width: 80, align: 'center',
    render: (t) => (t || 0),
  }];

  return (
    <Modal
      title={`绑定练习（小节「${node?.name || ''}」）`}
      open={open} onCancel={onClose} width={640} onOk={save} confirmLoading={saving}
      okText="保存（全量替换）"
    >
      <Space style={{ marginBottom: 10 }} wrap>
        <Input
          prefix={<SearchOutlined />} placeholder="搜索练习名称" allowClear value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onPressEnter={() => { setPage(1); fetchRepos(1, keyword); }}
          style={{ width: 260 }}
        />
        <Button icon={<SearchOutlined />} onClick={() => { setPage(1); fetchRepos(1, keyword); }}>查询</Button>
        <Text type="secondary">已选 {selected.length} 个练习</Text>
      </Space>
      <Table
        size="small" rowKey="id" loading={loading} columns={columns} dataSource={repos}
        pagination={{ current: page, pageSize: 8, total, showSizeChanger: false,
          onChange: (p) => { setPage(p); fetchRepos(p, keyword); } }}
        rowSelection={{ selectedRowKeys: selected, onChange: setSelected }}
      />
      <Alert type="info" showIcon style={{ marginTop: 10 }}
        message="保存为全量替换：勾选结果即该节点最终绑定的练习，解绑练习请取消勾选后保存。" />
    </Modal>
  );
}

/** 知识点绑题弹窗（题目库多选，全量替换） */
function BindQuestionsModal({ open, kp, onClose, onSaved }) {
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [selected, setSelected] = useState([]);

  const fetchTemplates = (p, kw) => {
    setLoading(true);
    listTemplate({ current: p, pageSize: 8, name: kw || undefined })
      .then((res) => { setRows(res?.data?.list || []); setTotal(res?.data?.total || 0); })
      .catch(() => { setRows([]); setTotal(0); })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (!open || !kp?.id) return;
    setSelected([]);
    setPage(1); setName('');
    fetchTemplates(1, '');
    listKnowledgePointQuestions(kp.id)
      .then((res) => setSelected((res?.data || []).map((q) => q.id))).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, kp?.id]);

  const save = () => {
    setSaving(true);
    saveKnowledgePointQuestions({ knowledgePointId: kp.id, questionIds: selected })
      .then(() => { message.success('知识点题目绑定已保存'); onClose(); onSaved && onSaved(); })
      .finally(() => setSaving(false));
  };

  const columns = [{
    title: '题干', dataIndex: 'name',
    render: (n) => <span style={{ display: 'inline-block', maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', verticalAlign: 'bottom' }}>{n}</span>,
  }, {
    title: '题型', dataIndex: 'questionType', width: 110,
    render: (t) => (TYPE_LABELS[t] || t || '-'),
  }, {
    title: '难度', dataIndex: 'difficulty', width: 90,
    render: (d) => (DIFFICULTY_LABELS[d] ? <Tag color={DIFFICULTY_LABELS[d][1]}>{DIFFICULTY_LABELS[d][0]}</Tag> : '-'),
  }];

  return (
    <Modal
      title={`绑定题目（知识点「${kp?.name || ''}」）`}
      open={open} onCancel={onClose} width={720} onOk={save} confirmLoading={saving}
      okText="保存（全量替换）"
    >
      <Space style={{ marginBottom: 10 }} wrap>
        <Input
          prefix={<SearchOutlined />} placeholder="搜索题干" allowClear value={name}
          onChange={(e) => setName(e.target.value)}
          onPressEnter={() => { setPage(1); fetchTemplates(1, name); }}
          style={{ width: 260 }}
        />
        <Button icon={<SearchOutlined />} onClick={() => { setPage(1); fetchTemplates(1, name); }}>查询</Button>
        <Text type="secondary">已选 {selected.length} 题</Text>
      </Space>
      <Table
        size="small" rowKey="id" loading={loading} columns={columns} dataSource={rows}
        pagination={{ current: page, pageSize: 8, total, showSizeChanger: false,
          onChange: (p) => { setPage(p); fetchTemplates(p, name); } }}
        rowSelection={{ selectedRowKeys: selected, onChange: setSelected }}
      />
      <Alert type="info" showIcon style={{ marginTop: 10 }}
        message="保存为全量替换：勾选结果即该知识点直绑题目（学员端「知识点刷题」的内容来源）。" />
    </Modal>
  );
}

export default function ExerciseListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // 定位参数（练习详情跳转 / 跨页回显）：?chapterId=&sectionId=&kpId=
  const initChapter = searchParams.get('chapterId') || '';
  const initSection = searchParams.get('sectionId') || '';
  const initKp = searchParams.get('kpId') || '';

  // ---- 树状态 ----
  const [subjects, setSubjects] = useState([]);
  const [subjectId, setSubjectId] = useState(initChapter ? undefined : '');
  const [chapters, setChapters] = useState([]);
  const [chapterId, setChapterId] = useState(initChapter);
  const [sections, setSections] = useState([]);
  const [sectionId, setSectionId] = useState(initSection);
  const [knowledgePoints, setKnowledgePoints] = useState([]);
  const [kpId, setKpId] = useState(initKp);

  // ---- 内容状态 ----
  const [sectionRepos, setSectionRepos] = useState([]);   // 小节直绑练习
  const [kpQuestions, setKpQuestions] = useState([]);     // 知识点直绑题目
  const [loading, setLoading] = useState(false);

  // ---- 弹窗状态 ----
  const [bindOpen, setBindOpen] = useState(false);
  const [bindTarget, setBindTarget] = useState(null);     // { node: 小节 }
  const [bindQOpen, setBindQOpen] = useState(false);
  const [bindQKp, setBindQKp] = useState(null);
  const [preview, setPreview] = useState(null);           // { scope, nodeId, nodeName }

  const loadSectionData = useCallback((id) => {
    if (!id) return;
    setLoading(true);
    listSectionRepos(id).then((res) => setSectionRepos(res?.data || []))
      .catch(() => setSectionRepos([])).finally(() => setLoading(false));
    listKnowledgePoints({ sectionId: id }).then((res) => setKnowledgePoints(res?.data || []))
      .catch(() => setKnowledgePoints([]));
  }, []);

  const loadKpData = useCallback((id) => {
    if (!id) return;
    setLoading(true);
    listKnowledgePointQuestions(id).then((res) => setKpQuestions(res?.data || []))
      .catch(() => setKpQuestions([])).finally(() => setLoading(false));
  }, []);

  // ---- 初始加载（学科；如带定位参数则逐级展开） ----
  useEffect(() => {
    listSubjects().then(async (res) => {
      const list = res?.data || [];
      setSubjects(list);
      if (initChapter) {
        // 由定位参数驱动（练习详情 → 习题列表定位）：全量章节反查学科后交给学科 effect 载入
        try {
          const all = (await listChapters({})) || [];
          const target = all.find((c) => c.id === initChapter);
          if (target) {
            setSubjectId(target.subjectId);
          } else {
            message.warning('定位章节不存在或已删除，已展示学科列表');
          }
        } catch { /* silent */ }
      } else if (list.length > 0) {
        setSubjectId(list[0].id);
      }
    }).catch(() => setSubjects([]));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 用户显式切换学科：先清空章节级下游选择与数据（避免旧下游数据跨学科误用）
  const changeSubject = (value) => {
    setSubjectId(value);
    setChapterId(''); setSectionId(''); setKpId('');
    setSectionRepos([]); setKnowledgePoints([]); setKpQuestions([]);
    setChapters([]);
    setSearchParams({}, { replace: true });
  };

  // 学科变化 → 载该学科章节（仅加载；换学科后的下游清理由 changeSubject 负责）
  useEffect(() => {
    if (!subjectId) return;
    listChapters({ subjectId }).then((res) => setChapters(res?.data || [])).catch(() => setChapters([]));
  }, [subjectId]);

  // 章节变化 → 载该章节下的小节（练习只在小节层级绑定，章节不再有直绑练习）
  useEffect(() => {
    if (!chapterId) return;
    listSections({ chapterId }).then((res) => setSections(res?.data || [])).catch(() => setSections([]));
    setSearchParams({ chapterId }, { replace: true });
  }, [chapterId, setSearchParams]);

  // 小节变化 → 载知识点 + 小节直绑练习
  useEffect(() => {
    if (!sectionId) return;
    loadSectionData(sectionId);
    setSearchParams({ chapterId, sectionId }, { replace: true });
  }, [sectionId, chapterId, loadSectionData, setSearchParams]);

  // 知识点变化
  useEffect(() => {
    if (!kpId) return;
    loadKpData(kpId);
    setSearchParams({ chapterId, sectionId, kpId }, { replace: true });
  }, [kpId, chapterId, sectionId, loadKpData, setSearchParams]);

  // 用户切换章节：先清空小节级下游选择与数据（URL 参数由各加载 effect 重写）
  const changeChapter = (value) => {
    setChapterId(value);
    setSectionId(''); setKpId('');
    setSectionRepos([]); setKnowledgePoints([]); setKpQuestions([]);
    if (!value) setSearchParams({}, { replace: true });
  };

  // 用户切换小节：先清空知识点级下游
  const changeSection = (value) => {
    setSectionId(value);
    setKpId('');
    setKpQuestions([]);
    setKnowledgePoints([]);
  };

  // 用户切换知识点：叶子节点，仅确保旧题目数据先行清空
  const changeKp = (value) => {
    setKpId(value);
    setKpQuestions([]);
  };

  // ---- 解绑（练习从小节移除；练习只在小节层级绑定） ----
  const unbindRepo = (repo) => {
    const remainIds = sectionRepos.filter((r) => r.id !== repo.id).map((r) => r.id);
    saveSectionRepos({ sectionId, repoIds: remainIds })
      .then(() => { message.success('已解绑'); loadSectionData(sectionId); })
      .catch(() => message.error('解绑失败'));
  };

  const repoColumns = [
    {
      title: '练习名称', dataIndex: 'name', width: 260,
      render: (n, r) => (
        <Space>
          <BookOutlined style={{ color: '#1677ff' }} />
          <Button type="link" style={{ padding: 0 }} onClick={() => navigate(`/repos/${r.id}`)}>{n}</Button>
        </Space>
      ),
    },
    { title: '题量', dataIndex: 'total', width: 90, align: 'center', render: (t) => <Text strong>{t || 0}</Text> },
    { title: '标签', dataIndex: 'tag', render: (tag) => (Array.isArray(tag) && tag.length > 0
      ? <Space size={4} wrap>{tag.slice(0, 3).map((t) => <Tag key={t}>{t}</Tag>)}</Space> : '-') },
    {
      title: '操作', key: 'action', width: 300,
      render: (_, r) => (
        <Space wrap>
          <Button size="small" icon={<EyeOutlined />}
            onClick={() => setPreview({ scope: 'repo', nodeId: r.id, nodeName: r.name })}>预览题目</Button>
          <Button size="small" type="link" style={{ padding: 0 }} onClick={() => navigate(`/repos/${r.id}`)}>进详情组题</Button>
          <Popconfirm title={`将练习「${r.name}」从小节解绑？`} okText="解绑" cancelText="取消"
            onConfirm={() => unbindRepo(r)}>
            <Button size="small" danger type="link" icon={<DeleteOutlined />} style={{ padding: 0 }}>解绑</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // ---- 面包屑（当前定位） ----
  const crumbChapter = chapters.find((c) => c.id === chapterId);
  const crumbSection = sections.find((s) => s.id === sectionId);
  const crumbKp = knowledgePoints.find((k) => k.id === kpId);

  return (
    <Card
      title="习题列表（按知识树查看绑定练习/题目）"
      extra={(
        <Space>
          <Text type="secondary">绑定/解绑也可在知识管理中操作，两处实时一致</Text>
          <Button icon={<ReloadOutlined />}
            onClick={() => { sectionId && loadSectionData(sectionId); kpId && loadKpData(kpId); }}>
            刷新
          </Button>
        </Space>
      )}
    >
      {/* 定位指示 */}
      <Breadcrumb style={{ marginBottom: 12 }} items={[
        { title: '学科' },
        ...(chapterId ? [{ title: crumbChapter?.name || '章节' }] : []),
        ...(sectionId ? [{ title: crumbSection?.name || '小节' }] : []),
        ...(kpId ? [{ title: crumbKp?.name || '知识点' }] : []),
      ]} />

      {/* 级联选择：学科 → 章节 → 小节 → 知识点 */}
      <Space size="middle" wrap style={{ marginBottom: 16 }}>
        <Select
          placeholder="学科" style={{ width: 200 }} value={subjectId} onChange={changeSubject}
          options={subjects.map((s) => ({ label: s.name, value: s.id }))} allowClear
        />
        <Select
          placeholder="章节" style={{ width: 240 }} value={chapterId} onChange={changeChapter} allowClear
          options={chapters.map((c) => ({ label: `${c.name}（${c.grade || ''}${c.term || ''}）`, value: c.id }))}
          notFoundContent="先选择学科"
        />
        <Select
          placeholder="小节" style={{ width: 260 }} value={sectionId} onChange={changeSection} allowClear
          options={sections.map((s) => ({ label: `${s.name}（绑 ${s.repoCount || 0} 练习）`, value: s.id }))}
          notFoundContent="先选择章节"
        />
        <Select
          placeholder="知识点" style={{ width: 280 }} value={kpId} onChange={changeKp} allowClear
          options={knowledgePoints.map((k) => ({ label: k.name, value: k.id }))}
          notFoundContent="先选择小节"
        />
      </Space>

      {!chapterId ? (
        // 学科下无章节选择：展示章节总览表
        <Table
          rowKey="id" loading={loading} dataSource={chapters}
          pagination={false} locale={{ emptyText: <Empty description="该学科暂无章节" /> }}
          columns={[
            { title: '章节名称', dataIndex: 'name', render: (n, c) => <Space><ApartmentOutlined style={{ color: '#1677ff' }} /><Text strong>{n}</Text>{c?.grade ? <Tag>{c.grade}{c.term || ''}</Tag> : null}</Space> },
            { title: '小节数', dataIndex: 'sectionCount', width: 100, align: 'center', render: (v) => v || 0 },
            {
              title: '操作', key: 'act', width: 200,
              render: (_, c) => (
                <Space wrap>
                  <Button type="primary" size="small" onClick={() => changeChapter(c.id)}>进入章节</Button>
                  <Button size="small" icon={<EyeOutlined />}
                    onClick={() => setPreview({ scope: 'chapter', nodeId: c.id, nodeName: c.name })}>预览刷题</Button>
                </Space>
              ),
            },
          ]}
        />
      ) : (
        <Spin spinning={loading}>
          {/* 章节层：展示下属小节（练习只在小节上绑定，章节不支持直绑） */}
          {!sectionId && (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '4px 0 8px' }}>
                <Text strong>
                  <ApartmentOutlined /> 章节「{crumbChapter?.name}」下属小节（{sections.length}）
                  <Text type="secondary" style={{ fontWeight: 400, marginLeft: 10, fontSize: 12 }}>
                    练习仅支持绑定到小节：在下方小节中「进入小节」后绑定
                  </Text>
                </Text>
                <Space>
                  <Button size="small" icon={<EyeOutlined />}
                    onClick={() => setPreview({ scope: 'chapter', nodeId: chapterId, nodeName: crumbChapter?.name })}>
                    预览本章刷题（聚合小节）
                  </Button>
                </Space>
              </div>
              <Table
                rowKey="id" size="middle" dataSource={sections} pagination={false}
                locale={{ emptyText: <Empty description="本章暂无小节，请先在「小节管理」中创建" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                columns={[
                  { title: '小节名称', dataIndex: 'name', render: (n, s) => <Space><Text>{n}</Text>{s?.grade ? <Tag>{s.grade}{s.term || ''}</Tag> : null}</Space> },
                  { title: '绑练习', dataIndex: 'repoCount', width: 90, align: 'center', render: (v) => (v ? <Tag color="blue">{v}</Tag> : '-') },
                  { title: '知识点', dataIndex: 'knowledgePointCount', width: 90, align: 'center', render: (v) => v || 0 },
                  {
                    title: '操作', key: 'act', width: 240,
                    render: (_, s) => (
                      <Space wrap>
                        <Button size="small" type="primary" onClick={() => changeSection(s.id)}>进入小节</Button>
                        <Button size="small" icon={<EyeOutlined />}
                          onClick={() => setPreview({ scope: 'section', nodeId: s.id, nodeName: s.name })}>预览刷题</Button>
                      </Space>
                    ),
                  },
                ]}
              />
            </>
          )}

          {/* 小节层：直绑练习 + 知识点列表 */}
          {sectionId && !kpId && (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '4px 0 8px' }}>
                <Text strong><PartitionOutlined /> 小节「{crumbSection?.name}」绑定练习（{sectionRepos.length}）</Text>
                <Space>
                  <Button type="primary" size="small" icon={<LinkOutlined />}
                    onClick={() => { setBindTarget({ node: crumbSection }); setBindOpen(true); }}>
                    绑定练习
                  </Button>
                  <Button size="small" icon={<EyeOutlined />}
                    onClick={() => setPreview({ scope: 'section', nodeId: sectionId, nodeName: crumbSection?.name })}>
                    预览本节刷题
                  </Button>
                </Space>
              </div>
              <Table rowKey="id" columns={repoColumns} dataSource={sectionRepos} pagination={false}
                style={{ marginBottom: 16 }}
                locale={{ emptyText: <Empty description="本节未绑定练习（学员端本节刷题无内容）" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }} />

              <Text strong style={{ display: 'block', marginBottom: 8 }}><BulbOutlined /> 小节下知识点（{knowledgePoints.length}）</Text>
              {knowledgePoints.length === 0 ? (
                <Empty description="本节暂无知识点" image={Empty.PRESENTED_IMAGE_SIMPLE} />
              ) : (
                <Table
                  rowKey="id" size="middle" dataSource={knowledgePoints} pagination={false}
                  columns={[
                    { title: '知识点名称', dataIndex: 'name' },
                    {
                      title: '操作', key: 'act', width: 320,
                      render: (_, k) => (
                        <Space wrap>
                          <Button size="small" type="primary" onClick={() => changeKp(k.id)}>查看直绑题目</Button>
                          <Button size="small" icon={<LinkOutlined />}
                            onClick={() => { setBindQKp(k); setBindQOpen(true); }}>绑定题目</Button>
                          <Button size="small" icon={<EyeOutlined />}
                            onClick={() => setPreview({ scope: 'knowledgePoint', nodeId: k.id, nodeName: k.name })}>
                            预览刷题
                          </Button>
                        </Space>
                      ),
                    },
                  ]}
                />
              )}
            </>
          )}

          {/* 知识点层：直绑题目 */}
          {kpId && (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '4px 0 8px' }}>
                <Text strong><BulbOutlined /> 知识点「{crumbKp?.name}」直绑题目（{kpQuestions.length}）</Text>
                <Space>
                  <Button type="primary" size="small" icon={<LinkOutlined />}
                    onClick={() => { setBindQKp(crumbKp); setBindQOpen(true); }}>绑定题目</Button>
                  <Button size="small" icon={<EyeOutlined />}
                    onClick={() => setPreview({ scope: 'knowledgePoint', nodeId: kpId, nodeName: crumbKp?.name })}>
                    预览刷题
                  </Button>
                </Space>
              </div>
              <Table
                rowKey="id" dataSource={kpQuestions} pagination={false}
                locale={{ emptyText: <Empty description="该知识点暂无直绑题目（学员端「知识点刷题」将无内容）" /> }}
                columns={[
                  { title: '题干', dataIndex: 'name',
                    render: (n) => <span style={{ display: 'inline-block', maxWidth: 420, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', verticalAlign: 'bottom' }}>{n}</span> },
                  { title: '题型', dataIndex: 'questionType', width: 110, render: (t) => (TYPE_LABELS[t] || t || '-') },
                  { title: '难度', dataIndex: 'difficulty', width: 90, render: (d) => (DIFFICULTY_LABELS[d] ? <Tag color={DIFFICULTY_LABELS[d][1]}>{DIFFICULTY_LABELS[d][0]}</Tag> : '-') },
                  { title: '标签', dataIndex: 'tag', width: 180, render: (tag) => (Array.isArray(tag) && tag.length ? <Space size={4} wrap>{tag.slice(0, 2).map((t) => <Tag key={t}>{t}</Tag>)}</Space> : '-') },
                  {
                    title: '操作', key: 'act', width: 120,
                    render: (_, q) => (
                      <Button size="small" icon={<EyeOutlined />}
                        onClick={() => setPreview({ scope: 'knowledgePoint', nodeId: kpId, nodeName: crumbKp?.name })}>
                        预览
                      </Button>
                    ),
                  },
                ]}
              />
            </>
          )}
        </Spin>
      )}

      {/* 弹窗区 */}
      <BindReposModal
        open={bindOpen}
        node={bindTarget?.node}
        onClose={() => setBindOpen(false)}
        onSaved={() => { if (sectionId) loadSectionData(sectionId); }}
      />
      <BindQuestionsModal
        open={bindQOpen} kp={bindQKp}
        onClose={() => setBindQOpen(false)}
        onSaved={() => loadKpData(kpId)}
      />
      <PreviewModal
        open={!!preview}
        scope={preview?.scope}
        nodeId={preview?.nodeId}
        nodeName={preview?.nodeName}
        onClose={() => setPreview(null)}
      />
    </Card>
  );
}
