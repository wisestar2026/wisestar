/**
 * UnitPaperManagePage.jsx - 英语「单元练习卷」组卷与打印（后台管理端）
 *
 * 功能（URL: /english/unit-paper，权限复用 template:list）:
 *   1. 选择 年级/册别/单元（单元来自检测章节，锁定英语学科 subjectId=1003）
 *   2. 从题库按单元自动组卷（复用学员端检测组卷接口，剥离答案），生成可打印练习卷
 *   3. 浏览器打印（window.print + 打印样式），可在系统打印对话框另存为 PDF
 *
 * 说明：本轮先做「出题 + 打印」，「布置到学生」后续接入 t_task。
 *
 * 数据流:
 *   GET  /api/student/detect/units     可选单元（章节）列表
 *   POST /api/student/detect/generate  按单元组卷（剥离标准答案与解析）
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Card, Empty, Select, Space, Spin, Tag, Typography, message } from 'antd';
import { PrinterOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { getDetectUnits, generateDetectPaper } from '../../api/detect';
import RichContent from '../../components/common/RichContent';
import './UnitPaperManagePage.css';

const { Text } = Typography;

/** 英语学科 ID */
const ENGLISH_SUBJECT_ID = '1003';

const GRADES = ['三年级', '四年级', '五年级', '六年级'];
const TERMS = ['上册', '下册'];
const COUNTS = [10, 15, 20, 30];

const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Select: '下拉题',
  FillBlank: '单项填空', Text: '多行文本', Score: '评分题',
  Remark: '备注说明', Judge: '判断题', MultipleBlank: '多项填空',
};

const OPTION_TYPES = ['Radio', 'Judge', 'Select', 'Checkbox'];

const dataOf = (res) => res?.data;

/** 需要作答横线的题型（非选项类） */
function needAnswerLines(qtype) {
  return !OPTION_TYPES.includes(qtype);
}

export default function UnitPaperManagePage() {
  const [grade, setGrade] = useState('');
  const [term, setTerm] = useState('');
  const [unitId, setUnitId] = useState('');
  const [questionCount, setQuestionCount] = useState(20);

  const [units, setUnits] = useState([]);
  const [loadingUnits, setLoadingUnits] = useState(false);

  const [generating, setGenerating] = useState(false);
  const [questions, setQuestions] = useState(null); // null=未组卷；[]=无题

  const unitName = useMemo(
    () => units.find((u) => u.id === unitId)?.name || '',
    [units, unitId],
  );

  // 载入可选单元
  const loadUnits = useCallback(async () => {
    if (!grade || !term) {
      setUnits([]);
      return;
    }
    setLoadingUnits(true);
    try {
      const res = await getDetectUnits({ subjectId: ENGLISH_SUBJECT_ID, grade, term });
      const list = dataOf(res) || [];
      setUnits(list);
      setUnitId((prev) => (list.some((u) => u.id === prev) ? prev : ''));
    } catch {
      // 拦截器已统一提示
      setUnits([]);
    } finally {
      setLoadingUnits(false);
    }
  }, [grade, term]);

  useEffect(() => {
    loadUnits();
  }, [loadUnits]);

  const configured = grade && term && unitId;

  const handleGenerate = async () => {
    if (!configured) return;
    setGenerating(true);
    try {
      const res = await generateDetectPaper({
        subjectId: ENGLISH_SUBJECT_ID,
        grade,
        term,
        chapterIds: [unitId],
        questionCount,
      });
      setQuestions(dataOf(res) || []);
    } catch (e) {
      message.error(e?.message || '组卷失败，请稍后重试');
      setQuestions(null);
    } finally {
      setGenerating(false);
    }
  };

  const handlePrint = () => window.print();

  const resetPaper = () => setQuestions(null);

  return (
    <div className="paper-page">
      <div className="paper-toolbar no-print">
        <Space wrap>
          <Text strong>英语单元练习卷</Text>
          <Select
            placeholder="年级"
            style={{ width: 120 }}
            value={grade || undefined}
            onChange={(v) => { setGrade(v); setQuestions(null); }}
            options={GRADES.map((g) => ({ value: g, label: g }))}
          />
          <Select
            placeholder="册别"
            style={{ width: 110 }}
            value={term || undefined}
            onChange={(v) => { setTerm(v); setQuestions(null); }}
            options={TERMS.map((t) => ({ value: t, label: t }))}
          />
          <Select
            placeholder="单元"
            style={{ width: 200 }}
            loading={loadingUnits}
            disabled={!grade || !term}
            value={unitId || undefined}
            onChange={(v) => { setUnitId(v); setQuestions(null); }}
            options={units.map((u) => ({ value: u.id, label: `${u.name}（${u.questionCount ?? 0} 题）` }))}
            notFoundContent={loadingUnits ? <Spin size="small" /> : '无可用单元'}
          />
          <Select
            style={{ width: 120 }}
            value={questionCount}
            onChange={setQuestionCount}
            options={COUNTS.map((c) => ({ value: c, label: `${c} 题` }))}
          />
          <Button
            type="primary"
            icon={<ThunderboltOutlined />}
            loading={generating}
            disabled={!configured}
            onClick={handleGenerate}
          >
            生成练习卷
          </Button>
        </Space>

        {questions && questions.length > 0 && (
          <Space>
            <Button icon={<PrinterOutlined />} onClick={handlePrint}>打印 / 导出 PDF</Button>
            <Button onClick={resetPaper}>返回配置</Button>
          </Space>
        )}
      </div>

      {!questions && (
        <Card className="no-print">
          <Empty
            description={configured ? '点击「生成练习卷」按单元组卷' : '请选择年级、册别与单元'}
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        </Card>
      )}

      {questions && questions.length === 0 && (
        <Card className="no-print">
          <Empty description="该单元暂无可用题目，请先在「英语题目」中补充题库" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        </Card>
      )}

      {questions && questions.length > 0 && (
        <div className="paper">
          <h1 className="paper-title">{grade}{term} 英语 · {unitName} 单元练习卷</h1>
          <div className="paper-meta">
            <span>姓名：________________</span>
            <span>班级：________________</span>
            <span>得分：________</span>
            <span>日期：________________</span>
          </div>
          <div className="paper-tip">本卷共 {questions.length} 题，请认真作答。</div>

          {questions.map((q, idx) => {
            const schema = q.schema || {};
            const attr = schema.attribute || {};
            const children = schema.children || [];
            const showOptions = OPTION_TYPES.includes(q.questionType);
            return (
              <div className="paper-question" key={q.id || idx}>
                <div className="paper-q-head">
                  <span className="paper-q-no">{idx + 1}.</span>
                  <Tag color="blue" bordered={false}>{TYPE_LABELS[q.questionType] || q.questionType || '题目'}</Tag>
                </div>
                <div className="paper-q-stem">
                  <RichContent text={q.name || schema.title || '(未命名题目)'} />
                </div>
                {attr.examImages?.length > 0 && (
                  <div className="paper-q-images">
                    {attr.examImages.map((img, i) => (
                      <img key={i} src={img} alt="题目配图" />
                    ))}
                  </div>
                )}
                {showOptions && (
                  <div className="paper-q-options">
                    {children.map((opt, i) => (
                      <div className="paper-option" key={opt.id || i}>
                        <span className="paper-option-letter">{String.fromCharCode(65 + i)}.</span>
                        <span className="paper-option-text"><RichContent text={opt.title || ''} /></span>
                      </div>
                    ))}
                  </div>
                )}
                {needAnswerLines(q.questionType) && <div className="paper-answer-line" />}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
