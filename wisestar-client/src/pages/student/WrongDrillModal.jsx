/**
 * WrongDrillModal.jsx - 错题本「消灭错题 / 消灭知识点」页内做题弹层
 *
 * 功能:
 *   1. 组题：先放入原错题（含标准答案），逐题作答后按需增补「同类型提高题」，全部在弹层内作答，不跳转学习页
 *      - 消灭错题(single)：原错题 1 道
 *      - 消灭知识点(kp)：该知识点下全部原错题
 *      - 答对一道原错题后，即时增补 1 道同类型更高难度题（easy→medium→hard，取不到更高难度则同难度）
 *   2. 逐题渲染 QuestionCard 判分，展示对错与正确答案
 *   3. 结算：整组（原错题 + 已增补提高题）全部答对才把原错题移出错题本（POST /student/wrong/redo）
 *
 * Props:
 *   open          boolean        是否可见
 *   mode          'single'|'kp'  消灭模式
 *   originals     Array          原错题项（wrong-list 元素）
 *   onClose       () => void     关闭
 *   onEliminated  () => void     成功消灭后的刷新回调
 *   fetchOriginal (item) => 题目  取原题（含标准答案）；默认走数学 study/questions
 *   redoQuestion  ({questionId, answer}) => res  单题消灭；默认走数学 /student/wrong/redo
 *   fetchHarder   (base, seen) => 题目|null      增补同类型提高题；默认走数学数据源
 *
 * 被谁引用: WrongBookPanel（数学）、EnglishReviewPage（英语，注入英语数据源）
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { Button, Modal, Progress, Typography, Space, message } from 'antd';
import { CheckCircleFilled, CloseCircleFilled } from '@ant-design/icons';
import { getStudyQuestions, wrongRedo } from '../../api/student';
import { evaluateAnswer } from '../../utils/practiceHelpers';
import QuestionCard from '../../components/practice/QuestionCard';
import { typeLabel } from '../../utils/wrongBook';

const { Text } = Typography;

/** 难度递进：easy→medium→hard（hard 已最高，维持 hard） */
const NEXT_DIFFICULTY = { easy: 'medium', medium: 'hard', hard: 'hard' };

/** 默认取原题：数学 study/questions 单题重做（含标准答案） */
async function defaultFetchOriginal(item) {
  const res = await getStudyQuestions({ questionId: item.questionId, exposeAnswer: true });
  return (res?.data || [])[0];
}

/** study/questions 返回项 → QuestionCard 需要的题目结构 */
function toCardQuestion(source, fallbackType) {
  return {
    id: source.id,
    name: source.name,
    questionType: source.questionType || fallbackType,
    knowledgePointId: source.knowledgePointId,
    knowledgePointName: source.knowledgePointName,
    knowledgePointPoints: source.knowledgePointPoints,
    template: source.schema || {},
  };
}

/** 取题目难度（题目 schema.attribute.difficulty） */
function difficultyOf(question) {
  return question?.template?.attribute?.difficulty || question?.difficulty || '';
}

/** 组装一次错题组：先放入原错题，逐题答对后再按需增补提高题 */
async function buildDrillSequence(originals, seen, fetchOriginal) {
  const questions = [];
  const originalIds = [];
  for (const item of originals) {
    // 原题详情（含标准答案）
    // eslint-disable-next-line no-await-in-loop
    const source = await fetchOriginal(item);
    if (!source?.id || seen.has(source.id)) {
      continue;
    }
    seen.add(source.id);
    questions.push({
      ...toCardQuestion(source, item.questionType),
      isOriginal: true,
      knowledgePointId: item.knowledgePointId || source.knowledgePointId || '',
      sectionId: item.sectionId || '',
      repoId: item.repoId || '',
    });
    originalIds.push(source.id);
  }
  return { questions, originalIds };
}

/**
 * 答对后增补一道同类型更高难度题（复用原题知识点/小节/题库范围；不限范围兜底）。
 *
 * <p>难度优先取更高一档，其次同难度，最后不限难度；同一难度档再依次尝试
 * 知识点 / 小节 / 题库范围（不同题库的出题数据源绑定方式不同，逐级回退兜底），
 * 排除本组已出现的题目；全部取不到时返回 null。</p>
 */
async function fetchHarderQuestion(base, seen) {
  const cur = difficultyOf(base);
  const next = NEXT_DIFFICULTY[cur];
  const diffs = [];
  if (next) diffs.push(next);
  if (cur && cur !== next) diffs.push(cur);
  diffs.push('');
  const scopes = [];
  if (base.knowledgePointId) scopes.push({ knowledgePointId: base.knowledgePointId });
  if (base.sectionId) scopes.push({ sectionId: base.sectionId });
  if (base.repoId) scopes.push({ repoId: base.repoId });
  scopes.push({});
  for (const diff of diffs) {
    for (const scope of scopes) {
      const params = {
        // 后端 @RequestParam List<String> types 以逗号拼接绑定（勿传数组，会被序列化成 types[] 而丢失）
        types: base.questionType,
        count: 12,
        random: true,
        exposeAnswer: true,
        ...scope,
      };
      if (diff) params.difficulty = diff;
      // eslint-disable-next-line no-await-in-loop
      const res = await getStudyQuestions(params);
      const found = (res?.data || []).find((q) => q?.id && !seen.has(q.id));
      if (found) {
        seen.add(found.id);
        return toCardQuestion(found, base.questionType);
      }
    }
  }
  return null;
}

export default function WrongDrillModal({
  open,
  mode,
  originals,
  onClose,
  onEliminated,
  fetchOriginal,
  redoQuestion,
  fetchHarder,
}) {
  const [loading, setLoading] = useState(false);
  const [questions, setQuestions] = useState([]);
  const [originalIds, setOriginalIds] = useState([]);
  const [index, setIndex] = useState(0);
  const [answers, setAnswers] = useState({});
  const [confirmed, setConfirmed] = useState({});
  const [finished, setFinished] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [preparingNext, setPreparingNext] = useState(false);
  const seenRef = useRef(new Set());
  const spawnedRef = useRef(new Set());

  const start = useCallback(async () => {
    setLoading(true);
    setAnswers({});
    setConfirmed({});
    setIndex(0);
    setFinished(false);
    setQuestions([]);
    setOriginalIds([]);
    setPreparingNext(false);
    seenRef.current = new Set();
    spawnedRef.current = new Set();
    try {
      const { questions: qs, originalIds: ids } = await buildDrillSequence(
        originals || [],
        seenRef.current,
        fetchOriginal || defaultFetchOriginal,
      );
      setQuestions(qs);
      setOriginalIds(ids);
    } catch {
      setQuestions([]);
      setOriginalIds([]);
    } finally {
      setLoading(false);
    }
  }, [originals, fetchOriginal]);

  useEffect(() => {
    if (open) {
      start();
    }
  }, [open, start]);

  const current = questions[index];
  const currentResult = current ? confirmed[current.id] : null;
  const allCorrect = questions.length > 0 && questions.every((q) => confirmed[q.id]?.correct === 1);
  const progress = questions.length ? Math.round(((index + (currentResult ? 1 : 0)) / questions.length) * 100) : 0;

  const handleConfirm = async () => {
    if (!current) return;
    const result = evaluateAnswer(current, answers[current.id]);
    setConfirmed((prev) => ({ ...prev, [current.id]: result }));
    // 答对原错题后，即时增补一道同类型更高难度题（提高题）；每题只增补一次
    if (result?.correct !== 1 || !current.isOriginal || spawnedRef.current.has(current.id)) {
      return;
    }
    spawnedRef.current.add(current.id);
    setPreparingNext(true);
    try {
      const harder = await (fetchHarder || fetchHarderQuestion)(current, seenRef.current);
      if (harder) {
        setQuestions((prev) => [...prev, harder]);
      }
    } catch {
      // 增补提高题失败不阻断本轮作答
    } finally {
      setPreparingNext(false);
    }
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
        const res = await (redoQuestion || wrongRedo)({ questionId: qid, answer: answers[qid] });
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
    if (preparingNext) return;
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
        {preparingNext && (
          <Text type="secondary" style={{ display: 'block', marginTop: 12, textAlign: 'center' }}>
            答对啦！正在准备一道同类型提高题…
          </Text>
        )}
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
        disabled={!currentResult || loading || questions.length === 0 || preparingNext}
        loading={submitting || preparingNext}
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
