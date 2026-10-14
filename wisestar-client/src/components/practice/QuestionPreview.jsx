/**
 * QuestionPreview.jsx - 题目只读回显（题干 + 配图 + 选项，不含答案）
 *
 * 用途：诊断报告 / 阶段检测「逐题解析」、练习结果「题目回顾」与英语复习错题中原样回显题目信息，
 *       便于学生对照题干核对失误，不提供作答交互。
 *
 * 被谁引用: KnowledgeDetectPage、PracticeSessionPage、EnglishUnitPracticePage、
 *           EnglishGrammarPracticePage、EnglishReviewPage
 *
 * Props:
 *   question: Object - 题目视图（StudentQuestionView: { name, questionType, schema, difficulty }）
 *
 * 说明：schema 已由后端剥离标准答案与解析（attribute.examCorrectAnswer / examAnalysis），
 *       本组件只负责展示，不参与判分。
 */

import { Tag, Image } from 'antd';
import RichContent from '../common/RichContent';
import './QuestionPreview.css';

// 题型中文映射（与 QuestionCard / practiceHelpers 保持一致）
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Select: '下拉题',
  FillBlank: '单项填空', Text: '多行文本', Score: '评分题',
  Remark: '备注说明', Judge: '判断题', MultipleBlank: '多项填空',
};

export default function QuestionPreview({ question }) {
  if (!question) return null;

  const schema = question.schema || question.template || {};
  const attr = schema.attribute || {};
  const children = schema.children || [];
  const qtype = question.questionType;
  const title = schema.title || question.name || '(未命名题目)';
  const optionImage = attr.examOptionImage || '';
  // 需要展示选项的题型（与 QuestionCard 一致）
  const showOptions = ['Radio', 'Judge', 'Select', 'Checkbox'].includes(qtype);

  return (
    <div className="q-preview">
      <div className="q-preview-head">
        <Tag color="blue" bordered={false}>{TYPE_LABELS[qtype] || qtype || '题目'}</Tag>
      </div>

      <div className="q-preview-stem">
        <RichContent text={title} />
      </div>

      {attr.examImages?.length > 0 && (
        <div className="q-preview-images">
          {attr.examImages.map((img, i) => (
            <Image key={i} src={img} alt="题目配图" style={{ maxWidth: 360, marginBottom: 8 }} />
          ))}
        </div>
      )}

      {showOptions && (
        <div className="q-preview-options">
          {optionImage && (
            <div style={{ marginBottom: 8 }}>
              <Image src={optionImage} alt="选项图" style={{ maxWidth: '100%' }} />
            </div>
          )}
          {children.map((opt, i) => (
            <div className="q-preview-option" key={opt.id || i}>
              <span className="q-preview-option-letter">{String.fromCharCode(65 + i)}.</span>
              <span className="q-preview-option-text"><RichContent text={opt.title || ''} /></span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
