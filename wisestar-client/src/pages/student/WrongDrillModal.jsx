/**
 * WrongDrillModal.jsx - 错题本「消灭错题 / 消灭知识点」页内做题弹层
 *
 * 功能:
 *   1. 组题：原题（含标准答案）+ 同题型新题，全部在弹层内作答，不跳转学习页
 *      - 消灭错题(single)：原题 1 道 + 同题型新题至多 2 道（合计 ≤ 3）
 *      - 消灭知识点(kp)：该知识点下全部原题，每题再补 2 道同题型新题
 *   2. 逐题渲染 QuestionCard 判分，展示对错与正确答案
 *   3. 结算：整组全部答对才把原题移出错题本（POST /student/wrong/redo）
 *
 * Props:
 *   open          boolean        是否可见
 *   mode          'single'|'kp'  消灭模式
 *   originals     Array          原错题项（wrong-list 元素）
 *   onClose       () => void     关闭
 *   onEliminated  () => void     成功消灭后的刷新回调
 *
 * 被谁引用: WrongBookPanel
 */

import { useCallback, useEffect, useState } from 'react';
import { Button, Modal, Progress, Typography, Space, message } from 'antd';
import { CheckCircleFilled, CloseCircleFilled } from '@ant-design/icons';
import { getStudyQuestions, wrongRedo } from '../../api/student';
import { evaluateAnswer } from '../../utils/practiceHelpers';
import QuestionCard from '../../components/practice/QuestionCard';
import { typeLabel } from '../../utils/wrongBook';

const { Text } = Typography;

/** study/questions 返回项 → QuestionCard 需要的题目结构 */
function toCardQuestion(source, fallbackType) {
  return {
    id: source.id,
    name: source.name,
    questionType: source.questionType || fallbackType,
    template: source.schema || {},
  };
}

/** 生成原题的同题型新题（排除已出现题目），不足时按实际数量返回 */
async function fetchNewQuestions(origin, need, seen) {
  if (need <= 0) {
    return [];
  }
  const params = {
    // 后端 @RequestParam List<String> types 以逗号拼接绑定（勿传数组，会被序列化成 types[] 而丢失）
    types: origin.questionType,
    count: Math.min(50, need + 3),
    random: true,
    exposeAnswer: true,
  };
  if (origin.knowledgePointId) {
    params.knowledgePointId = origin.knowledgePointId;
  } else if (origin.sectionId) {
    params.sectionId = origin.sectionId;
  } else if (origin.repoId) {
    params.repoId = origin.repoId;
  }
  const res = await getStudyQuestions(params);
  const picked = [];
  (res?.data || []).forEach((q) => {
    if (!q?.id || seen.has(q.id) || picked.length >= need) {
      return;
    }
    seen.add(q.id);
    picked.push(toCardQuestion(q, origin.questionType));
  });
  return picked;
}

/** 组装一次错题组（原题 + 同题型新题） */
async function buildDrillSequence(mode, originals) {
  const questions = [];
  const originalIds = [];
  const seen = new Set();
  const needPerOriginal = 2;
  for (const item of originals) {
    // 原题详情（含标准答案）
    // eslint-disable-next-line no-await-in-loop
    const res = await getStudyQuestions({ questionId: item.questionId, exposeAnswer: true });
    const source = (res?.data || [])[0];
    if (!source?.id || seen.has(source.id)) {
      continue;
    }
    seen.add(source.id);
    questions.push({ ...toCardQuestion(source, item.questionType), isOriginal: true });
    originalIds.push(source.id);
    // 同题型新题
    // eslint-disable-next-line no-await-in-loop
    const news = await fetchNewQuestions(item, needPerOriginal, seen);
    news.forEach((q) => questions.push({ ...q, isOriginal: false }));
  }
  return { questions, originalIds };
}

export default function WrongDrillModal({ open, mode, originals, onClose, onEliminated }) {
  const [loading, setLoading] = useState(false);
  const [questions, setQuestions] = useState([]);
  const [originalIds, setOriginalIds] = useState([]);
  const [index, setIndex] = useState(0);
  const [answers, setAnswers] = useState({});
  const [confirmed, setConfirmed] = useState({});
  const [finished, setFinished] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const start = useCallback(async () => {
    setLoading(true);
    setAnswers({});
    setConfirmed({});
    setIndex(0);
    setFinished(false);
    setQuestions([]);
    setOriginalIds([]);
    try {
      const { questions: qs, originalIds: ids } = await buildDrillSequence(mode, originals || []);
      setQuestions(qs);
      setOriginalIds(ids);
    } catch {
      setQuestions([]);
      setOriginalIds([]);
    } finally {
      setLoading(false);
    }
  }, [mode, originals]);

  useEffect(() => {
    if (open) {
      start();
    }
  }, [open, start]);

  const current = questions[index];
  const currentResult = current ? confirmed[current.id] : null;
  const allCorrect = questions.length > 0 && questions.every((q) => confirmed[q.id]?.correct === 1);
  const progress = questions.length ? Math.round(((index + (currentResult ? 1 : 0)) / questions.length) * 100) : 0;

  const handleConfirm = () => {
    if (!current) return;
    const result = evaluateAnswer(current, answers[current.id]);
    setConfirmed((prev) => ({ ...prev, [current.id]: result }));
  };

  const settle = async () => {
    if (!allCorrect || originalIds.length === 0) {
      return;
    }
    setSubmitting(true);
    let removedAll = true;
    for (const qid of originalIds) {
      try {
        // eslint-disable-next-line no-await-in-loop
        const res = await wrongRedo({ questionId: qid, answer: answers[qid] });
        if (!res?.data?.removed) {
          removedAll = false;
        }
      } catch {
        removedAll = false;
      }
    }
    setSubmitting(false);
    if (removedAll) {
      onEliminated?.();
    } else {
      message.warning('消灭失败，请重试');
    }
  };

  const handleNext = () => {
    if (index < questions.length - 1) {
      setIndex((i) => i + 1);
      return;
    }
    setFinished(true);
    settle();
  };

  const renderBody = () => {
    if (loading) {
      return <div className="wrong-drill-loading">正在准备题目…</div>;
    }
    if (questions.length === 0) {
      return <div className="wrong-drill-loading">暂无可用题目，请稍后重试</div>;
    }
    if (finished) {
      const wrongList = questions.filter((q) => confirmed[q.id]?.correct !== 1);
      return (
        <div className="wrong-drill-result">
          {allCorrect ? (
            <div className="wrong-drill-result-ok">
              <CheckCircleFilled /> 全部答对，已消灭 {originalIds.length} 道错题！
            </div>
          ) : (
            <>
              <div className="wrong-drill-result-no">
                <CloseCircleFilled /> 本组有 {wrongList.length} 道答错，原错题保留
              </div>
              <div className="wrong-drill-wrong-list">
                {wrongList.map((q) => (
                  <div key={q.id} className="wrong-drill-wrong-item">
                    <span className="wrong-drill-wrong-name">{q.name || '（无题干）'}</span>
                    <span className="wrong-drill-wrong-type">{typeLabel(q.questionType)}</span>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      );
    }
    return (
      <>
        <div className="wrong-drill-progress">
          <Progress percent={progress} showInfo={false} strokeColor="#4bb7e8" />
          <Text type="secondary">{index + 1} / {questions.length}</Text>
        </div>
        <QuestionCard
          question={current}
          index={index + 1}
          total={questions.length}
          value={answers[current.id]}
          onChange={(value) => setAnswers((prev) => ({ ...prev, [current.id]: value }))}
          confirmed={confirmed}
          onConfirm={handleConfirm}
          judgeMode
        />
      </>
    );
  };

  const renderFooter = () => {
    if (finished) {
      return (
        <Space>
          {!allCorrect && <Button onClick={start}>再练一遍</Button>}
          <Button type="primary" onClick={onClose}>完成</Button>
        </Space>
      );
    }
    return (
      <Button
        type="primary"
        disabled={!currentResult || loading || questions.length === 0}
        loading={submitting}
        onClick={handleNext}
      >
        {index + 1 >= questions.length ? '提交' : '下一题'}
      </Button>
    );
  };

  const title = mode === 'kp' ? '消灭知识点错题' : '消灭错题';

  return (
    <Modal
      open={open}
      title={title}
      width={720}
      onCancel={onClose}
      maskClosable={false}
      footer={renderFooter()}
      destroyOnClose
    >
      <div className="wrong-drill">
        {renderBody()}
      </div>
    </Modal>
  );
}
