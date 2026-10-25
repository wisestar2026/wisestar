/**
 * MascotBubble.jsx - 白卡外的海豚反馈气泡
 *
 * 答题页的判题结论原本写在白色主卡内部，作答后会把选项、按钮往下挤。
 * 现在统一移到卡片外（进度条与卡片之间）：右手边是首页同款海豚船长头像，
 * 左手边是它的对话气泡，位置固定、高度固定，换文案不会引起页面跳动。
 *
 * Props:
 *   tone: 'idle' | 'ok' | 'no'  —— 决定气泡配色（中性 / 答对 / 答错）
 *   children: 气泡文案（可为节点，用于「再想想～ + 正确答案」多行）
 *
 * 被谁引用: EnglishWordPracticePage / EnglishSentenceExercise / EnglishSentenceLearnPage
 */

import './MascotBubble.css';

export default function MascotBubble({ tone = 'idle', children }) {
  return (
    <div className={`mb-wrap is-${tone}`} role="status" aria-live="polite">
      <div className="mb-bubble">
        <span className="mb-text">{children}</span>
      </div>
      <img className="mb-avatar" src="/student-assets/dolphin-captain.webp" alt="海豚船长" />
    </div>
  );
}
