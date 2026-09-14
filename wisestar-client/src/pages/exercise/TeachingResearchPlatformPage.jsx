/**
 * TeachingResearchPlatformPage.jsx - 教研平台主页面
 *
 * 挂载路由: /exercise/list（菜单「教研平台」），整体替换原「习题列表 / 练习总览」入口。
 *
 * 功能：
 *   - 学科 + 年级 双筛选（学科级 + 年级来自章节数据）
 *   - 章节 → 小节 → 知识点 懒加载树；章节/小节点名称即编辑
 *   - 知识点：点名称查看直绑题目；节点右侧 ✎ 按钮编辑（含简介 content.intro / 讲解要点 content.points）
 *   - 右侧：直绑题目 Label 面板（题目/选项/答案和解析/题目的图片 分块带字段标签），「编辑」复用题目管理弹窗；
 *     支持从题库勾选新题绑定（全量替换合并旧绑定，不覆盖）
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Card, Select, Tree, Button, Tag, Tooltip, Typography, Space, Empty,
  Spin, message,
} from 'antd';
import { EditOutlined, ReloadOutlined, PlusOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import {
  listSubjects, listChapters, listSections, listKnowledgePoints,
  updateChapter, updateSection, updateKnowledgePoint,
  listKnowledgePointQuestions, saveKnowledgePointQuestions,
  listMatchedKnowledgePointQuestions,
} from '../../api/knowledge';
import { updateTemplate } from '../../api/template';
import NodeEditModal from '../../components/research/NodeEditModal';
import AddQuestionModal from '../../components/research/AddQuestionModal';
import QuestionEditModal from '../../components/question/QuestionEditModal';
import { extractCorrectAnswers, formatCorrectAnswers } from '../../utils/practiceHelpers';
import './TeachingResearchPlatformPage.css';

const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Judge: '判断题', Fill: '填空题',
  ShortAnswer: '简答题', MultipleBlank: '多空填空题', Select: '下拉题', Essay: '作文题',
};
const DIFF_LABELS = { 1: '容易', 2: '中等', 3: '困难' };

/* ---------- 工具：知识点 content 解析 / 答案文本 ---------- */
function parseKpIntro(content) {
  try {
    const obj = content ? JSON.parse(content) : {};
    if (typeof obj.intro === 'string' && obj.intro.trim()) return obj.intro;
  } catch { /* ignore */ }
  return '';
}

function extractAttr(template) {
  try {
    const obj = typeof template === 'string' ? JSON.parse(template) : template || {};
    return obj.attribute || {};
  } catch { return {}; }
}

/** 拦截器返回 { code, data } 整包 → 统一取业务 data 字段（兼容历史裸返回） */
function unwrap(res) {
  return res && res.code !== undefined ? res.data : res;
}

/** 选择题选项：导入数据中 children 的选项节点带 title、无显式 type，故按 title 识别 */
function parseOptions(schema) {
  return ((schema && schema.children) || [])
    .filter((c) => c && c.title)
    .map((c) => ({ id: c.id, title: c.title }));
}

/** 题干中的 {{IMG:xxx}} 占位符键（图片落库前的映射键） */
function imagePlaceholders(text) {
  return (String(text || '').match(/\{\{IMG:([^}]+)\}\}/g) || []).map((m) => m.slice(6, -2));
}

/** 去除题干中的图片占位符，返回纯文本题干 */
function stripImagePlaceholders(text) {
  return String(text || '').replace(/\{\{IMG:[^}]+\}\}/g, '').replace(/\s{2,}/g, ' ').trim();
}

/* ---------- 题目 Label（标签式面板，答案显隐状态独立） ---------- */
function ResearchQuestionCard({ q, onEdit, action }) {
  const [show, setShow] = useState(false);
  const attr = extractAttr(q.template);
  const qtype = q.questionType;
  const schema = useMemo(() => {
    try { return typeof q.template === 'string' ? JSON.parse(q.template) : (q.template || {}); }
    catch { return {}; }
  }, [q.template]);
  const options = useMemo(() => parseOptions(schema), [schema]);
  const stemRaw = schema.title || q.name || '';
  const stem = stripImagePlaceholders(stemRaw) || '（题干为图片）';
  const images = Array.isArray(attr.examImages) ? attr.examImages.filter(Boolean) : [];
  const placeholders = imagePlaceholders(stemRaw);
  const hasImage = images.length > 0 || placeholders.length > 0;
  // 标准答案兼容整题级与选项级：编辑器写入根 attribute，历史导入题把答案标记在正确选项的 attribute 上
  const answerText = formatCorrectAnswers(qtype, extractCorrectAnswers({ template: schema }) || []);

  return (
    <div className="trp-lbl">
      <div className="trp-lbl-head">
        <Space size={4} wrap>
          <Tag color="blue">{TYPE_LABELS[qtype] || qtype || '未知题型'}</Tag>
          <Tag color={q.difficulty === 3 ? 'red' : q.difficulty === 2 ? 'orange' : 'green'}>
            {DIFF_LABELS[q.difficulty] || '难度未知'}
          </Tag>
        </Space>
        <Space size={4}>
          {action}
          <Button size="small" type="link" icon={show ? <EyeInvisibleOutlined /> : <EyeOutlined />}
            onClick={() => setShow(!show)}>
            {show ? '收起答案' : '查看答案'}
          </Button>
          <Button size="small" type="link" icon={<EditOutlined />} onClick={() => onEdit(q)}>
            编辑
          </Button>
        </Space>
      </div>

      <div className={`trp-lbl-body${hasImage ? ' has-image' : ''}`}>
        <div className="trp-lbl-main">
          <div className="trp-lbl-field">
            <span className="trp-lbl-tag">题目</span>
            <div className="trp-lbl-text">{stem}</div>
          </div>

          {options.length > 0 && (
            <div className="trp-lbl-field">
              <span className="trp-lbl-tag">选项</span>
              <div className="trp-lbl-text">
                {options.map((o, i) => (
                  <div key={o.id || i} className="trp-lbl-opt">
                    <b>{String.fromCharCode(65 + i)}.</b> {o.title}
                  </div>
                ))}
              </div>
            </div>
          )}

          <div className="trp-lbl-field">
            <span className="trp-lbl-tag">答案和解析</span>
            {show ? (
              <div className="trp-q-answer" style={{ marginTop: 0 }}>
                <div><b>参考答案：</b>{answerText || '（未配置）'}</div>
                {attr.examAnalysis && <div style={{ marginTop: 4 }}><b>解析：</b>{attr.examAnalysis}</div>}
              </div>
            ) : (
              <div className="trp-lbl-hint">点击右上角「查看答案」显示参考答案与解析</div>
            )}
          </div>
        </div>

        {hasImage && (
          <div className="trp-lbl-side">
            <div className="trp-lbl-field trp-lbl-field-img">
              <span className="trp-lbl-tag">题目的图片</span>
              {images.length > 0 ? (
                <div className="trp-lbl-imgs">
                  {images.map((src, i) => (
                    <img key={i} src={src} alt={`题目图片${i + 1}`} className="trp-lbl-img" />
                  ))}
                </div>
              ) : (
                <div className="trp-lbl-hint">
                  图片待补充（{placeholders.length} 张）：{placeholders.join('、')}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default function TeachingResearchPlatformPage() {
  const [subjects, setSubjects] = useState([]);
  const [subjectId, setSubjectId] = useState(undefined);
  const [gradeOptions, setGradeOptions] = useState([]);
  const [grade, setGrade] = useState(undefined);

  const [chapters, setChapters] = useState([]);
  const [secMap, setSecMap] = useState({});   // chapterId -> 小节
  const [kpMap, setKpMap] = useState({});     // sectionId -> 知识点
  const [loadingRoot, setLoadingRoot] = useState(false);
  const [loadingSubjects, setLoadingSubjects] = useState(true);
  const [expandedKeys, setExpandedKeys] = useState([]);

  const [editState, setEditState] = useState(null); // { nodeType, record } | null
  const [kpQuestions, setKpQuestions] = useState({ pid: null, list: [], matched: [], loading: false, editing: null });
  const [addOpen, setAddOpen] = useState(false);
  const expandedRef = useRef([]);

  /* ---------- 初始化：学科（失败给出空态 + 重试，避免无限转圈） ---------- */
  const loadSubjects = () => {
    setLoadingSubjects(true);
    listSubjects()
      .then((res) => {
        const raw = unwrap(res);
        const arr = Array.isArray(raw) ? raw : raw?.list || [];
        setSubjects(arr);
        if (arr.length > 0 && !subjectId) setSubjectId(arr[0].id);
      })
      .catch((e) => {
        message.error('加载学科失败：' + (e?.message || e));
        setSubjects([]);
      })
      .finally(() => setLoadingSubjects(false));
  };

  useEffect(() => {
    loadSubjects();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /* ---------- 学科/年级变化 → 重载章节树 ---------- */
  const resetAll = () => {
    setChapters([]);
    setSecMap({});
    setKpMap({});
    setGradeOptions([]);
    setExpandedKeys([]);
    expandedRef.current = [];
    setKpQuestions({ pid: null, list: [], loading: false, editing: null });
  };

  const loadChapters = (subject, keepGrade) => {
    setLoadingRoot(true);
    listChapters({ subjectId: subject })
      .then((res) => {
        const raw = unwrap(res);
        const list = Array.isArray(raw) ? raw : raw?.list || [];
        setChapters(list);
        const gs = [...new Set(list.map((c) => c.grade).filter(Boolean))];
        setGradeOptions(gs);
        if (!keepGrade || !gs.includes(grade)) {
          setGrade(gs.length > 0 ? gs[0] : undefined);
          setExpandedKeys([]);
          expandedRef.current = [];
        }
      })
      .catch((e) => message.error('加载章节失败：' + (e?.message || e)))
      .finally(() => setLoadingRoot(false));
  };

  useEffect(() => {
    if (!subjectId) return;
    setGrade(undefined);
    setExpandedKeys([]);
    expandedRef.current = [];
    loadChapters(subjectId, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [subjectId]);

  /* ---------- 章节/小节懒加载 ---------- */
  const ensureSections = (chapterId) => {
    if (secMap[chapterId]) return;
    listSections({ chapterId })
      .then((res) => {
        const raw = unwrap(res);
        const list = Array.isArray(raw) ? raw : raw?.list || [];
        setSecMap((m) => ({ ...m, [chapterId]: list }));
      })
      .catch((e) => message.error('加载小节失败：' + (e?.message || e)));
  };

  const ensureKps = (sectionId) => {
    if (kpMap[sectionId]) return;
    listKnowledgePoints({ sectionId })
      .then((res) => {
        // 后端返回 { list, total }
        const raw = unwrap(res);
        const list = Array.isArray(raw) ? raw : raw?.list || [];
        setKpMap((m) => ({ ...m, [sectionId]: list }));
      })
      .catch((e) => message.error('加载知识点失败：' + (e?.message || e)));
  };

  const onExpand = (keys) => {
    const newKeys = keys.filter((k) => !expandedRef.current.includes(k));
    expandedRef.current = keys;
    setExpandedKeys(keys);
    newKeys.forEach((k) => {
      if (k.startsWith('ch:')) ensureSections(k.slice(3));
      else if (k.startsWith('sec:')) ensureKps(k.slice(4));
    });
  };

  const inGrade = (row) => (grade && row.grade ? row.grade === grade : true);

  /* ---------- 树数据（含简介摘要） ---------- */
  const treeData = useMemo(() => {
    const chaptersInGrade = chapters.filter((c) => !grade || !c.grade || c.grade === grade);
    return chaptersInGrade.map((ch) => {
      const sections = (secMap[ch.id] || []).filter(inGrade);
      return {
        key: 'ch:' + ch.id,
        title: ch.name,
        isLeaf: false,
        children: sections.map((sec) => {
          const kps = (kpMap[sec.id] || []).filter(inGrade);
          return {
            key: 'sec:' + sec.id,
            title: sec.name,
            isLeaf: false,
            children: kps.map((kp) => {
              const intro = parseKpIntro(kp.content);
              return {
                key: 'kp:' + kp.id,
                isLeaf: true,
                title: (
                  <Tooltip title={intro ? `简介：${intro}` : '暂无简介'} placement="right">
                    <span className="trp-kp-node">
                      <span className="trp-kp-name">{kp.name}</span>
                      <span className="trp-kp-desc">{intro || '暂无简介'}</span>
                      <Button
                        size="small"
                        type="text"
                        icon={<EditOutlined />}
                        className="trp-node-edit"
                        onClick={(e) => { e.stopPropagation(); setEditState({ nodeType: 'knowledgePoint', record: kp }); }}
                      />
                    </span>
                  </Tooltip>
                ),
              };
            }),
          };
        }),
      };
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chapters, secMap, kpMap, grade]);

  const onNodeSelect = (_keys, info) => {
    const key = String(info?.node?.key ?? '');
    if (key.startsWith('ch:')) {
      const ch = chapters.find((c) => c.id === key.slice(3));
      if (ch) setEditState({ nodeType: 'chapter', record: ch });
    } else if (key.startsWith('sec:')) {
      const sec = Object.values(secMap).flat().find((s) => s.id === key.slice(4));
      if (sec) setEditState({ nodeType: 'section', record: sec });
    } else if (key.startsWith('kp:')) {
      openQuestions(key.slice(3));
    }
  };

  /* ---------- 编辑保存（仅回写缓存，不重置整树） ---------- */
  const handleSaved = async (patch) => {
    const { nodeType, id } = patch;
    try {
      let payload = { ...patch };
      if (nodeType === 'chapter') {
        await updateChapter(payload);
        setChapters((arr) => arr.map((c) => (c.id === id ? { ...c, ...payload } : c)));
      } else if (nodeType === 'section') {
        await updateSection(payload);
        setSecMap((m) => {
          const updated = {};
          Object.entries(m).forEach(([cid, list]) => {
            updated[cid] = list.map((s) => (s.id === id ? { ...s, ...payload } : s));
          });
          return updated;
        });
      } else {
        await updateKnowledgePoint(payload);
        setKpMap((m) => {
          const updated = {};
          Object.entries(m).forEach(([sid, list]) => {
            updated[sid] = list.map((k) => (k.id === id ? { ...k, ...payload } : k));
          });
          return updated;
        });
      }
      message.success('保存成功');
      setEditState(null);
    } catch (e) {
      message.error('保存失败：' + (e?.message || e));
    }
  };

  /* ---------- 右侧：知识点直绑题目 ---------- */
  // 同时拉两批：已显式绑定的题目 + 题库中知识点标签匹配的题目（用于自动关联）
  const loadQuestions = (pid) => {
    setKpQuestions((s) => ({ ...s, pid, loading: true }));
    Promise.all([
      listKnowledgePointQuestions(pid),
      listMatchedKnowledgePointQuestions(pid).catch(() => null),
    ])
      .then(([boundRes, matchedRes]) => {
        const boundRaw = unwrap(boundRes);
        const list = Array.isArray(boundRaw) ? boundRaw : boundRaw?.list || [];
        const matchedRaw = matchedRes ? unwrap(matchedRes) : [];
        const matched = Array.isArray(matchedRaw) ? matchedRaw : matchedRaw?.list || [];
        setKpQuestions({ pid, list, matched, loading: false, editing: null });
      })
      .catch((e) => {
        message.error('加载题目失败：' + (e?.message || e));
        setKpQuestions((s) => ({ ...s, loading: false }));
      });
  };

  const openQuestions = (pid) => {
    if (kpQuestions.pid === pid && kpQuestions.list.length) return;
    loadQuestions(pid);
  };

  const refreshQuestions = () => {
    if (kpQuestions.pid) loadQuestions(kpQuestions.pid);
  };

  // 题库标签匹配、但尚未显式绑定的题目（用于一键/逐题绑定）
  const unboundMatched = useMemo(() => {
    const boundIds = new Set((kpQuestions.list || []).map((q) => q.id));
    return (kpQuestions.matched || []).filter((q) => !boundIds.has(q.id));
  }, [kpQuestions.list, kpQuestions.matched]);

  const selectedKp = useMemo(() => {
    if (!kpQuestions.pid) return null;
    let found = null;
    Object.values(kpMap).forEach((list) => {
      if (!found) found = list.find((k) => k.id === kpQuestions.pid);
    });
    return found || null;
  }, [kpMap, kpQuestions.pid]);

  const bindQuestionIds = async (ids) => {
    const pid = kpQuestions.pid;
    if (!pid || !ids || ids.length === 0) return;
    const old = kpQuestions.list.map((q) => q.id);
    const merged = [...old, ...ids.filter((i) => !old.includes(i))];
    const added = merged.length - old.length;
    if (added === 0) {
      message.info('所选题目已在绑定列表中');
      return;
    }
    try {
      await saveKnowledgePointQuestions({ knowledgePointId: pid, questionIds: merged });
      message.success(`已绑定 ${added} 道题目`);
      setAddOpen(false);
      loadQuestions(pid);
    } catch (e) {
      message.error('绑定失败：' + (e?.message || e));
    }
  };

  const handleAddQuestions = (ids) => bindQuestionIds(ids);

  const handleEditQuestionSaved = async (data) => {
    const row = kpQuestions.list.find((q) => q.id === kpQuestions.editing);
    if (!row) return;
    try {
      await updateTemplate({ ...data, id: row.id });
      message.success('题目保存成功');
      setKpQuestions((s) => ({ ...s, editing: null }));
      refreshQuestions();
    } catch (e) {
      message.error('题目保存失败：' + (e?.message || e));
    }
  };

  /* ---------- JSX ---------- */
  return (
    <div className="trp-root">
      {/* 左：知识树 */}
      <Card
        title={(
          <div className="trp-header">
            <Select
              style={{ width: 120 }}
              value={subjectId}
              onChange={(v) => { resetAll(); setSubjectId(v); }}
              options={subjects.map((s) => ({ value: s.id, label: s.name }))}
            />
            <Select
              style={{ width: 110 }}
              value={grade}
              placeholder="年级"
              onChange={(v) => { setGrade(v); setExpandedKeys([]); expandedRef.current = []; }}
              options={gradeOptions.map((g) => ({ value: g, label: g }))}
            />
            <Button size="small" icon={<ReloadOutlined />} onClick={() => loadChapters(subjectId, true)}>
              刷新
            </Button>
          </div>
        )}
        className="trp-tree-card"
        bodyStyle={{ padding: 8, overflow: 'auto', maxHeight: 'calc(100vh - 260px)' }}
      >
        <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block', margin: '4px 8px 8px' }}>
          点「章节 / 小节」名称修改信息；点「知识点」查看直绑题目，右侧 ✎ 可编辑简介。
        </Typography.Text>
        <Spin spinning={loadingSubjects || (!!subjectId && loadingRoot)}>
          {!loadingSubjects && subjects.length === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="学科加载失败或未配置"
            >
              <Button type="primary" size="small" onClick={loadSubjects}>重新加载</Button>
            </Empty>
          ) : (
            <>
              {treeData.length ? (
                <Tree
                  treeData={treeData}
                  expandedKeys={expandedKeys}
                  onExpand={onExpand}
                  onSelect={onNodeSelect}
                  selectable
                  blockNode
                />
              ) : (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={subjectId ? '该学科暂无章节，请先在知识管理中录入' : '请选择学科'}
                />
              )}
            </>
          )}
        </Spin>
      </Card>

      {/* 右：直绑题目 */}
      <Card
        title={(
          <div className="trp-header">
            <div className="trp-panel-title">
              {selectedKp ? (
                <>
                  <Typography.Text strong>{selectedKp.name}</Typography.Text>
                  <Tooltip title={parseKpIntro(selectedKp.content) || '暂无简介'}>
                    <Typography.Text type="secondary" style={{ fontSize: 12, marginLeft: 8 }} ellipsis>
                      {parseKpIntro(selectedKp.content) || '暂无简介'}
                    </Typography.Text>
                  </Tooltip>
                </>
              ) : '直绑题目'}
            </div>
            <Space style={{ marginLeft: 'auto' }}>
              {selectedKp && (
                <>
                  <Tag color="purple" style={{ marginRight: 0 }}>
                    {kpQuestions.list.length} 题
                  </Tag>
                  <Button size="small" type="primary" icon={<PlusOutlined />} onClick={() => setAddOpen(true)}>
                    绑定新题
                  </Button>
                  <Button size="small" icon={<ReloadOutlined />} onClick={refreshQuestions} />
                </>
              )}
            </Space>
          </div>
        )}
        className="trp-qpanel-card"
        bodyStyle={{ padding: 12, overflow: 'auto', maxHeight: 'calc(100vh - 260px)' }}
      >
        {!kpQuestions.pid ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="点左侧知识点，查看其直绑题目" />
        ) : (
          <Spin spinning={kpQuestions.loading}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {kpQuestions.list.length === 0 && unboundMatched.length === 0 && (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="该知识点还没有关联题目，可点右上角「绑定新题」从题库加入" />
              )}
              {kpQuestions.list.map((q) => (
                <ResearchQuestionCard key={q.id} q={q} onEdit={(row) => setKpQuestions((s) => ({ ...s, editing: row.id }))} />
              ))}

              {unboundMatched.length > 0 && (
                <div className="trp-matched-block">
                  <div className="trp-matched-head">
                    <Typography.Text type="secondary">
                      题库中标记了该知识点的题目（{unboundMatched.length} 题未绑定）
                    </Typography.Text>
                    <Button
                      size="small"
                      type="primary"
                      ghost
                      onClick={() => bindQuestionIds(unboundMatched.map((q) => q.id))}
                    >
                      全部绑定
                    </Button>
                  </div>
                  {unboundMatched.map((q) => (
                    <ResearchQuestionCard
                      key={q.id}
                      q={q}
                      onEdit={(row) => setKpQuestions((s) => ({ ...s, editing: row.id }))}
                      action={(
                        <Button size="small" type="link" onClick={() => bindQuestionIds([q.id])}>
                          绑定
                        </Button>
                      )}
                    />
                  ))}
                </div>
              )}
            </div>
          </Spin>
        )}
      </Card>

      {/* 节点编辑弹窗（章节/小节/知识点共用） */}
      <NodeEditModal
        open={!!editState}
        nodeType={editState?.nodeType}
        record={editState?.record}
        onCancel={() => setEditState(null)}
        onSaved={handleSaved}
      />

      {/* 从题库绑定新题 */}
      <AddQuestionModal
        open={addOpen}
        onCancel={() => setAddOpen(false)}
        onAdd={handleAddQuestions}
      />

      {/* 题目编辑：复用题目管理页弹窗 */}
      {kpQuestions.editing && (
        <QuestionEditModal
          open
          onCancel={() => setKpQuestions((s) => ({ ...s, editing: null }))}
          onSave={handleEditQuestionSaved}
          record={[...kpQuestions.list, ...(kpQuestions.matched || [])].find((q) => q.id === kpQuestions.editing)}
          repos={[]}
        />
      )}
    </div>
  );
}
