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
import EnglishWrongBookPanel from '../../components/student/EnglishWrongBookPanel';
import IconTile from '../../components/common/IconTile';
import useStudentStore, { SUBJECTS } from '../../stores/useStudentStore';
import './WrongBookPage.css';
import './EnglishCenterPage.css';

export default function WrongBookPage() {
  // 英语学科下的错题本只展示英语错题（数据源 /english/student/wrong-questions）
  const activeSubject = useStudentStore((s) => s.activeSubject);
  const subjects = useStudentStore((s) => s.studyContent?.subjects);
  // 真实学科的 key 是后端学科 ID（英语为 1003），mock 模式是 english，按名称兜底
  const activeSubjectInfo = (subjects || []).find((s) => s.id === activeSubject)
    || SUBJECTS.find((s) => s.key === activeSubject);
  const isEnglish = activeSubjectInfo?.name === '英语'
    || activeSubjectInfo?.key === 'english'
    || activeSubjectInfo?.key === '1003';

  return (
    <div className="sll-page-enter wrong-book">
      <div className="sll-card wrong-card">
        <div className="wrong-title">
          <IconTile emoji="📕" tone="pink" size="sm" />
          {isEnglish ? '我的英语错题本' : '我的错题本'}
        </div>
        {isEnglish ? <EnglishWrongBookPanel /> : <WrongBookPanel />}
      </div>
    </div>
  );
}
