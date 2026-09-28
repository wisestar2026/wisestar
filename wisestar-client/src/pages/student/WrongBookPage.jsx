/**
 * WrongBookPage.jsx - 学员端错题本（独立路由页）
 *
 * 功能:
 *   1. 展示学员练习中的错题（/api/practice/wrong-list，含我的答案/正确答案/错误归因）
 *   2. 顶部统计区（知识点错题环形图 + 按知识点/按题型错题数据）与「知识点错题 / 错误类型错题」双页签
 *   3. 消灭错题 / 消灭知识点（页内弹层做题，整组答对才移除，不跳转学习页）
 *
 * URL: /student/wrong（受 AuthGuard 保护，学员端）
 * 被谁引用: App.jsx 路由；学员端底部导航「错题本」
 *
 * 说明: 具体列表与交互封装在可复用的 WrongBookPanel 中，
 * KnowledgePage（?tab=wrong）复用同一面板，避免两处实现不一致。
 */

import WrongBookPanel from '../../components/student/WrongBookPanel';
import IconTile from '../../components/common/IconTile';
import './WrongBookPage.css';

export default function WrongBookPage() {
  return (
    <div className="sll-page-enter wrong-book">
      <div className="sll-card wrong-card">
        <div className="wrong-title"><IconTile emoji="📕" tone="pink" size="sm" /> 我的错题本</div>
        <WrongBookPanel />
      </div>
    </div>
  );
}
