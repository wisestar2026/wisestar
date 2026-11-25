/**
 * EnglishQuizLayout.jsx - 英语套卷类练习统一版式（左题卡 + 右答题卡）
 *
 * 版式与数学 KnowledgePage 的 qz-shell 一致：
 *   左侧白色题卡（内含当前题目与「上一题 / 下一题」导航），右侧固定题号答题卡（含交卷按钮）。
 *
 * 用法：
 *   <EnglishQuizLayout
 *     total={questions.length}
 *     current={current}
 *     onJump={setCurrent}
 *     onPrev={() => setCurrent((c) => c - 1)}
 *     onNext={() => setCurrent((c) => c + 1)}
 *     isAnswered={(idx) => isAnsweredAnswer(answers[questions[idx].id])}
 *     answeredCount={answeredCount}
 *     onSubmit={handleSubmit}
 *     submitting={submitting}
 *     submitLabel="交卷查看结果"
 *   >
 *     <QuestionCard ... />
 *   </EnglishQuizLayout>
 */
import { Button } from 'antd';
import './EnglishQuizLayout.css';

export default function EnglishQuizLayout({
  total = 0,
  current = 0,
  onJump,
  onPrev,
  onNext,
  isAnswered,
  answeredCount = 0,
  onSubmit,
  submitting = false,
  submitLabel = '交卷',
  dotStyle,
  children,
}) {
  const last = total - 1;
  return (
    <div className="engq-shell">
      <div className="engq-main">
        <div className="engq-card quiz-stage">
          <div className="engq-card-body">{children}</div>
          <div className="engq-nav">
            <Button size="large" disabled={current === 0} onClick={onPrev}>上一题</Button>
            <span className="engq-nav-meta">
              第 {current + 1} / {total} 题 · 已答 {answeredCount}
            </span>
            <Button size="large" type="primary" disabled={current >= last} onClick={onNext}>下一题</Button>
          </div>
        </div>
      </div>

      <aside className="engq-rail">
        <div className="engq-rail-title">答题卡</div>
        <div className="engq-rail-grid">
          {Array.from({ length: total }).map((_, idx) => (
            <button
              key={idx}
              type="button"
              className={`engq-num${idx === current ? ' is-current' : ''}${isAnswered?.(idx) ? ' is-done' : ''}`}
              style={dotStyle ? dotStyle(idx) : undefined}
              onClick={() => onJump?.(idx)}
            >
              {idx + 1}
            </button>
          ))}
        </div>
        <div className="engq-legend">
          <span><i className="engq-dot" />未答</span>
          <span><i className="engq-dot is-done" />已答</span>
          <span><i className="engq-dot is-current" />当前</span>
        </div>
        <Button
          className="engq-submit"
          type="primary"
          size="large"
          block
          loading={submitting}
          onClick={onSubmit}
        >
          {submitLabel}
        </Button>
      </aside>
    </div>
  );
}
