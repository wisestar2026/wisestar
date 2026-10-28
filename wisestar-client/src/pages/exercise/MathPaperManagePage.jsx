/**
 * MathPaperManagePage.jsx - 数学「组卷」与打印（后台管理端）
 *
 * 功能（URL: /math-paper，权限复用 template:list）:
 *   1. 通用组卷：按 年级/册别 → 章节 → 小节 → 知识点 圈定范围，叠加难度、题型、题量
 *   2. 按学员组卷：搜索学号/姓名，优先取该学员数学错题，不足时用同范围练习题补齐
 *   3. 生成可打印试卷（含参考答案与解析），浏览器打印可另存为 PDF
 *   4. 展示题型/难度/章节/知识点分布，便于教研核对
 *
 * 数据流:
 *   GET  /api/math/paper/meta      年级→册别→章节→小节→知识点树 + 题量统计
 *   GET  /api/math/paper/students  按学号/姓名搜索学员
 *   POST /api/math/paper/generate  生成试卷（含答案）
 */

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Empty,
  InputNumber,
  Radio,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from 'antd';
import { PrinterOutlined, ThunderboltOutlined } from '@ant-design/icons';
import {
  generateMathPaper,
  getMathPaperMeta,
  searchMathPaperStudents,
} from '../../api/mathPaper';
import RichContent from '../../components/common/RichContent';
import './MathPaperManagePage.css';

const { Text } = Typography;

const COUNTS = [10, 15, 20, 25, 30];

const OPTION_TYPES = ['Radio', 'Judge', 'Select', 'Checkbox'];

const CIRCLED = '①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳';

const dataOf = (res) => res?.data;

/** 选项字母 */
const letterOf = (index) => String.fromCharCode(65 + index);

/** 卷面作答区的空数：多项填空按空子项数，其余给 1~3 行 */
function blankLines(q) {
  const schema = q.schema || {};
  const children = schema.children || [];
  if (q.questionType === 'MultipleBlank') {
    return Math.max(children.length, schema.attribute?.blankCount || 1);
  }
  if (q.questionType === 'FillBlank') {
    return Math.max(children.length, 1);
  }
  return 3;
}

/** 单题渲染（试卷区） */
function PaperQuestion({ q, index, withAnswers }) {
  const schema = q.schema || {};
  const attr = schema.attribute || {};
  const children = schema.children || [];
  const isOption = OPTION_TYPES.includes(q.questionType);
  const lines = isOption ? 0 : blankLines(q);

  return (
    <div className="paper-question">
      <div className="paper-q-head">
        <span className="paper-q-no">{index + 1}.</span>
        {q.wrongCount ? (
          <span
            className={`paper-q-wrong${withAnswers ? '' : ' no-print'}`}
            title={`该学员此前做错 ${q.wrongCount} 次`}
          >
            错题×{q.wrongCount}
          </span>
        ) : null}
        <span className="paper-q-stem">
          <RichContent text={q.name || schema.title || '(未命名题目)'} />
        </span>
        <span className="paper-q-tags no-print">
          <Tag bordered={false} color="blue">{q.questionTypeLabel}</Tag>
          {q.difficultyLabel ? <Tag bordered={false}>{q.difficultyLabel}</Tag> : null}
        </span>
        <span className="paper-q-score no-print">{Number(q.score) || 0} 分</span>
      </div>

      {attr.examImages?.length > 0 && (
        <div className="paper-q-images">
          {attr.examImages.map((img, i) => (
            <img key={i} src={img} alt="题目配图" />
          ))}
        </div>
      )}

      {isOption && (
        <div className={`paper-q-options${q.questionType === 'Judge' ? ' is-judge' : ''}`}>
          {children.map((opt, i) => (
            <div className="paper-option" key={opt.id || i}>
              <span className="paper-option-letter">{letterOf(i)}.</span>
              <span className="paper-option-text"><RichContent text={opt.title || ''} /></span>
            </div>
          ))}
        </div>
      )}

      {lines > 0 && (
        <div className="paper-blank-area">
          {Array.from({ length: lines }).map((_, i) => (
            <div className="paper-blank-row" key={i}>
              <span className="paper-blank-label">{CIRCLED[i] || `(${i + 1})`}</span>
              <span className="paper-blank-line" />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default function MathPaperManagePage() {
  const [meta, setMeta] = useState(null);
  const [loadingMeta, setLoadingMeta] = useState(false);

  const [mode, setMode] = useState('chapter');
  const [grade, setGrade] = useState('');
  const [book, setBook] = useState('');
  const [chapterIds, setChapterIds] = useState([]);
  const [sectionIds, setSectionIds] = useState([]);
  const [pointNames, setPointNames] = useState([]);

  const [students, setStudents] = useState([]);
  const [student, setStudent] = useState(null);
  const [searching, setSearching] = useState(false);
  const searchTimer = useRef(null);

  const [difficulty, setDifficulty] = useState('');
  const [types, setTypes] = useState([]);
  const [count, setCount] = useState(20);

  const [generating, setGenerating] = useState(false);
  const [paper, setPaper] = useState(null);
  const [withAnswers, setWithAnswers] = useState(true);

  // 载入组卷元数据
  useEffect(() => {
    let alive = true;
    setLoadingMeta(true);
    getMathPaperMeta()
      .then((res) => {
        if (alive) setMeta(dataOf(res) || null);
      })
      .catch(() => {
        if (alive) setMeta(null);
      })
      .finally(() => {
        if (alive) setLoadingMeta(false);
      });
    return () => {
      alive = false;
      if (searchTimer.current) clearTimeout(searchTimer.current);
    };
  }, []);

  const grades = useMemo(() => meta?.grades || [], [meta]);

  const allChapters = useMemo(
    () => grades.flatMap((g) => (g.books || []).flatMap((b) => b.chapters || [])),
    [grades],
  );

  const bookChapters = useMemo(() => {
    if (!grade) return allChapters;
    const gradeNode = grades.find((g) => g.grade === grade);
    if (!gradeNode) return allChapters;
    const books = book
      ? (gradeNode.books || []).filter((b) => b.term === book)
      : gradeNode.books || [];
    return books.flatMap((b) => b.chapters || []);
  }, [grades, grade, book, allChapters]);

  const chapterOptions = useMemo(
    () => bookChapters.map((c) => ({
      value: c.id,
      label: `${c.name}（${c.questionCount || 0} 题）`,
      name: c.name,
    })),
    [bookChapters],
  );

  const sectionOptions = useMemo(() => {
    const scope = chapterIds.length
      ? bookChapters.filter((c) => chapterIds.includes(c.id))
      : bookChapters;
    return scope.flatMap((c) => (c.sections || []).map((s) => ({
      value: s.id,
      label: `${s.type === 'exam' ? '[章节测评] ' : ''}${s.name}（${s.questionCount || 0} 题）`,
      chapterId: c.id,
    })));
  }, [bookChapters, chapterIds]);

  const pointOptions = useMemo(() => {
    const scopeSections = sectionIds.length
      ? sectionOptions.filter((s) => sectionIds.includes(s.id))
      : sectionOptions;
    const names = new Set();
    scopeSections.forEach((option) => {
      const chapter = bookChapters.find((c) => c.id === option.chapterId);
      const section = (chapter?.sections || []).find((s) => s.id === option.value);
      (section?.knowledgePoints || []).forEach((kp) => names.add(kp.name));
    });
    return Array.from(names).map((name) => ({ value: name, label: name }));
  }, [sectionOptions, sectionIds, bookChapters]);

  // 级联联动：切换年级/册别/章节/小节时清掉失效的下级选择
  useEffect(() => {
    setChapterIds((prev) => prev.filter((id) => bookChapters.some((c) => c.id === id)));
  }, [bookChapters]);

  useEffect(() => {
    setSectionIds((prev) => prev.filter((id) => sectionOptions.some((s) => s.value === id)));
  }, [sectionOptions]);

  useEffect(() => {
    setPointNames((prev) => prev.filter((name) => pointOptions.some((p) => p.value === name)));
  }, [pointOptions]);

  const loadStudents = useCallback(async (keyword) => {
    setSearching(true);
    try {
      const res = await searchMathPaperStudents(keyword);
      setStudents(dataOf(res) || []);
    } catch {
      setStudents([]);
    } finally {
      setSearching(false);
    }
  }, []);

  const handleStudentSearch = (value) => {
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => loadStudents(value), 300);
  };

  const resetPaper = () => setPaper(null);

  const configured = mode === 'chapter' || Boolean(student);

  const handleGenerate = async () => {
    if (!configured) {
      message.warning('请先选择学员');
      return;
    }
    setGenerating(true);
    try {
      const res = await generateMathPaper({
        mode,
        studentId: student?.id,
        chapterIds: chapterIds.length ? chapterIds : undefined,
        sectionIds: sectionIds.length ? sectionIds : undefined,
        knowledgePoints: pointNames.length ? pointNames : undefined,
        difficulty: difficulty || undefined,
        questionTypes: types.length ? types : undefined,
        questionCount: count,
      });
      setPaper(dataOf(res) || null);
    } catch (e) {
      message.error(e?.message || '组卷失败，请稍后重试');
      setPaper(null);
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div className="math-paper-page">
      <Card className="no-print math-paper-config" size="small">
        <div className="math-paper-row">
          <Text strong>组卷方式</Text>
          <Radio.Group
            value={mode}
            onChange={(e) => {
              setMode(e.target.value);
              setPaper(null);
            }}
            options={[
              { value: 'chapter', label: '通用组卷' },
              { value: 'student', label: '按学员错题' },
            ]}
            optionType="button"
          />
          {mode === 'student' && (
            <Select
              showSearch
              allowClear
              style={{ width: 320 }}
              placeholder="输入学号或姓名搜索学员"
              value={student?.id}
              filterOption={false}
              loading={searching}
              onSearch={handleStudentSearch}
              onFocus={() => {
                if (!students.length) loadStudents('');
              }}
              onChange={(value) => {
                const picked = students.find((s) => s.id === value) || null;
                setStudent(picked);
                setPaper(null);
              }}
              options={(student && !students.some((s) => s.id === student.id)
                ? [student, ...students]
                : students).map((s) => ({
                value: s.id,
                label: `${s.studentNo} ${s.name}${s.campus ? `（${s.campus}）` : ''}`,
              }))}
              notFoundContent={searching ? <Spin size="small" /> : '无匹配学员'}
            />
          )}
          {student && (
            <Tag color="blue" bordered={false} closable onClose={() => { setStudent(null); setPaper(null); }}>
              {student.studentNo} {student.name}
            </Tag>
          )}
        </div>

        <div className="math-paper-row">
          <Text strong>范围</Text>
          <Select
            allowClear
            placeholder="年级"
            style={{ width: 120 }}
            value={grade || undefined}
            onChange={(v) => {
              setGrade(v || '');
              setBook('');
              setChapterIds([]);
              setSectionIds([]);
              setPointNames([]);
            }}
            options={grades.map((g) => ({ value: g.grade, label: g.grade }))}
          />
          <Select
            allowClear
            placeholder="册别"
            style={{ width: 110 }}
            value={book || undefined}
            onChange={(v) => {
              setBook(v || '');
              setChapterIds([]);
              setSectionIds([]);
              setPointNames([]);
            }}
            options={((grades.find((g) => g.grade === grade)?.books) || [])
              .map((b) => ({ value: b.term, label: b.label }))}
            disabled={!grade}
          />
          <Select
            mode="multiple"
            allowClear
            maxTagCount="responsive"
            placeholder="章节（可多选，不选则整册/全库）"
            style={{ minWidth: 320, flex: 1 }}
            value={chapterIds}
            onChange={(v) => {
              setChapterIds(v);
              setSectionIds([]);
              setPointNames([]);
            }}
            options={chapterOptions}
            optionFilterProp="label"
          />
        </div>

        <div className="math-paper-row">
          <Text strong>细化</Text>
          <Select
            mode="multiple"
            allowClear
            maxTagCount="responsive"
            placeholder="小节（可选）"
            style={{ minWidth: 280, flex: 1 }}
            value={sectionIds}
            onChange={(v) => {
              setSectionIds(v);
              setPointNames([]);
            }}
            options={sectionOptions}
            optionFilterProp="label"
            disabled={!sectionOptions.length}
          />
          <Select
            mode="multiple"
            allowClear
            maxTagCount="responsive"
            placeholder="知识点（可选，任一命中）"
            style={{ minWidth: 280, flex: 1 }}
            value={pointNames}
            onChange={setPointNames}
            options={pointOptions}
            optionFilterProp="label"
            disabled={!pointOptions.length}
          />
        </div>

        <div className="math-paper-row">
          <Text strong>题目</Text>
          <Select
            allowClear
            placeholder="难度"
            style={{ width: 120 }}
            value={difficulty || undefined}
            onChange={(v) => setDifficulty(v || '')}
            options={(meta?.difficulties || []).map((d) => ({
              value: d.value,
              label: `${d.label}（${d.count} 题）`,
            }))}
          />
          <Select
            mode="multiple"
            allowClear
            maxTagCount="responsive"
            placeholder="题型"
            style={{ width: 320 }}
            value={types}
            onChange={setTypes}
            options={(meta?.questionTypes || []).filter((t) => t.count > 0).map((t) => ({
              value: t.value,
              label: `${t.label}（${t.count} 题）`,
            }))}
          />
          <InputNumber
            min={1}
            max={60}
            value={count}
            onChange={(v) => setCount(v || 20)}
            addonAfter="题"
            style={{ width: 130 }}
          />
          <Select
            style={{ width: 110 }}
            value={COUNTS.includes(count) ? count : undefined}
            placeholder="快捷题量"
            onChange={setCount}
            options={COUNTS.map((c) => ({ value: c, label: `${c} 题` }))}
          />
          <Button
            type="primary"
            icon={<ThunderboltOutlined />}
            loading={generating}
            disabled={!configured || loadingMeta}
            onClick={handleGenerate}
          >
            生成试卷
          </Button>
          {paper && <Button onClick={resetPaper}>返回配置</Button>}
        </div>

        <div className="math-paper-hint">
          {mode === 'student'
            ? '按学员组卷会优先选该生做错过的数学题（题卡标「错题×N」），题量不足时用同范围练习题补齐。'
            : '不选章节则从全部数学题库抽题；选择章节/小节/知识点后只在这些范围内抽题。'}
          {meta ? ` 当前数学题库共 ${meta.totalQuestions} 题。` : null}
        </div>
      </Card>

      {!paper && (
        <Card className="no-print">
          <Empty
            description={configured ? '点击「生成试卷」按当前配置抽题' : '请先选择学员'}
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        </Card>
      )}

      {paper && (
        <>
          {paper.warnings?.length > 0 && (
            <Alert
              className="no-print math-paper-warnings"
              type="info"
              showIcon
              message="组卷说明"
              description={(
                <ul>
                  {paper.warnings.map((text, i) => <li key={i}>{text}</li>)}
                </ul>
              )}
            />
          )}

          <div className="math-paper-toolbar no-print">
            <Space wrap size={16}>
              <Text type="secondary">
                共 {paper.questionCount} 题 · 满分 {paper.totalScore} 分
                {paper.wrongQuestionCount > 0 ? ` · 其中该生错题 ${paper.wrongQuestionCount} 道` : ''}
                {' '}· 生成于 {paper.generatedAt}
              </Text>
              {(paper.typeStats || []).map((s) => (
                <Tag key={`t-${s.name}`} bordered={false}>{s.name} {s.count}</Tag>
              ))}
            </Space>
            <Space>
              <Checkbox checked={withAnswers} onChange={(e) => setWithAnswers(e.target.checked)}>
                打印含答案解析
              </Checkbox>
              <Button icon={<PrinterOutlined />} onClick={() => window.print()}>打印 / 导出 PDF</Button>
            </Space>
          </div>

          {paper.questionCount === 0 ? (
            <Card className="no-print">
              <Empty description="当前条件下没有可用题目，请放宽范围或先补充题库" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            </Card>
          ) : (
            <div className="paper">
              <h1 className="paper-title">{paper.title}</h1>
              <div className="paper-meta">
                <span>姓名：________________</span>
                <span>班级：________________</span>
                {paper.studentNo ? <span>学号：{paper.studentNo}</span> : null}
                <span>得分：________</span>
                <span>日期：________________</span>
              </div>
              <div className="paper-tip">
                本卷共 {paper.questionCount} 题，满分 {paper.totalScore} 分，请认真作答。
              </div>

              {paper.questions.map((q, idx) => (
                <PaperQuestion key={q.id || idx} q={q} index={idx} withAnswers={withAnswers} />
              ))}

              {withAnswers && (
                <div className="paper-answers">
                  <h2 className="paper-answers-title">参考答案与解析</h2>
                  {paper.questions.map((q, idx) => (
                    <div className="paper-answer-item" key={`a-${q.id || idx}`}>
                      <span className="paper-answer-no">{idx + 1}.</span>
                      <span className="paper-answer-value">{q.answerText || '（无标准答案）'}</span>
                      {q.analysis ? (
                        <div className="paper-answer-analysis">
                          <RichContent text={q.analysis} />
                        </div>
                      ) : null}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
