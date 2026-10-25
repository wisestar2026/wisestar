/**
 * englishStudent.js - 学员端英语学习中心 API
 *
 * 接口（后端 EnglishStudentApi / EnglishWordStudentApi，前缀 /api）：
 *   GET  /english/student/units            单元列表 + 学习进度（version/grade/term）
 *   GET  /english/student/sentences        单元句子列表（含熟练度）
 *   GET  /english/student/grammars         单元重点语法列表（含掌握度）
 *   GET  /english/student/sentence/study   待学习/复习句子
 *   POST /english/student/sentence/record  记录句子作答 { sentenceId, correct }
 *   POST /english/student/grammar/record   记录语法作答 { grammarId, correct }
 *   GET  /english/student/review           智能复习（单词 + 句子混合队列）
 *   POST /english/student/session          记录学习会话 { type, durationSeconds, correctCount }
 *   GET  /english/student/scenarios        情景对话（AI 按单元单词实时生成，不落库）
 *   GET  /english/student/pattern-scenarios 重点句式情景（AI 按单元重点句式实时生成，不落库）
 *   GET  /english/word/drill               进入学习前强制巩固单词（最近答错 + 复习到期）
 *   GET  /english/word/drill/completed     当天是否已完成强制巩固（账号维度，跨设备）
 *   GET  /english/word/word-book           单元单词列表（含熟练度，复用单词接口）
 *   POST /english/word/record              记录单词作答 { wordId, correct }
 *
 * 调用方：
 *   - EnglishCenterPage         单元进度与智能复习入口
 *   - EnglishWordLearnPage      单词卡片学习
 *   - EnglishSentenceLearnPage  句子组句学习
 *   - EnglishReviewPage         单词 + 句子混合复习
 */

import { message } from 'antd';
import request from './request';

/** 单元列表 + 学习进度 */
export function getEnglishUnits(params) {
  return request.get('/english/student/units', { params });
}

/** 单元句子列表（含熟练度） */
export function getEnglishSentences(params) {
  return request.get('/english/student/sentences', { params });
}

/** 单元重点语法列表（含掌握度） */
export function getEnglishGrammars(params) {
  return request.get('/english/student/grammars', { params });
}

/** 记录语法作答 */
export function recordEnglishGrammar(data) {
  return request.post('/english/student/grammar/record', data);
}

/** 待学习/复习句子 */
export function getEnglishStudySentences(params) {
  return request.get('/english/student/sentence/study', { params });
}

/** 记录句子作答 */
export function recordEnglishSentence(data) {
  return request.post('/english/student/sentence/record', data);
}

/** 智能复习队列（单词 + 句子） */
export function getEnglishReview(params) {
  return request.get('/english/student/review', { params });
}

/** 英语复习错题（来自检测记录聚合，隐藏答案解析） */
export function getEnglishWrongQuestions(params) {
  return request.get('/english/student/wrong-questions', { params });
}

/** 取单道英语错题的重做用题目（含标准答案，仅本人错题可读） */
export function getEnglishWrongQuestion(params) {
  return request.get('/english/student/wrong-question', { params });
}

/** 英语错题重做：答对即消灭（后端幂等落库） */
export function redoEnglishWrong(data) {
  return request.post('/english/student/wrong/redo', data);
}

/** 情景对话练习（AI 按单元单词实时生成，不落库） */
export function getEnglishScenarios(params) {
  return request.get('/english/student/scenarios', { params });
}

/** 重点句式情景练习（AI 按单元重点句式实时生成，不落库） */
export function getEnglishPatternScenarios(params) {
  return request.get('/english/student/pattern-scenarios', { params });
}

/** 单元阅读理解（多篇文章 + 每篇 4 道选择题；无固化内容时 AI 生成 1 篇兜底） */
export function getEnglishReading(params) {
  return request.get('/english/student/reading', { params, timeout: 120000 });
}

/** 单元课文跟读（篇章 + 逐句中英文） */
export function getEnglishReadAloud(params) {
  return request.get('/english/student/read-aloud', { params });
}

/** 英文短句朗读音频（服务端合成 mp3/wav，返回 Blob；失败静默，由调用方回退浏览器语音） */
/** 单词复习队列：跨单元按熟练度由低到高、复习间隔由长到短，词量按年级（三四年级 20 / 五六年级 30） */
export function getEnglishWordReview(params) {
  return request.get('/english/student/review/words', { params });
}

/** 单词复习概览：应复习词数、熟练度分布（陌生/一般/熟练）、涉及单元 */
export function getEnglishWordReviewSummary(params) {
  return request.get('/english/student/review/word-summary', { params });
}

export function getEnglishTts(text, speed) {
  return request.get('/english/student/tts', {
    params: { text, speed },
    responseType: 'blob',
    meta: { silent: true },
  });
}

/** 单元句子同步练习（多题型：连词成句/句子默写/句子选择，读取管理端已固化内容） */
export function getEnglishSentenceExercises(params) {
  return request.get('/english/student/sentence-exercises', { params });
}

/**
 * 记录学习会话，并展示奖励结算反馈。
 *
 * 后端与主系统共用 RewardService：首次完成对应类型当日任务时返回 firstTime=true，
 * 此处按主系统一致的方式提示「学习币 +X · 积分 +Y」。
 */
export function recordEnglishSession(data) {
  return request.post('/english/student/session', data).then((res) => {
    const reward = res?.data;
    if (reward?.firstTime && (reward.coins > 0 || reward.points > 0)) {
      message.success(`学习币 +${reward.coins} · 积分 +${reward.points}`);
    }
    return res;
  });
}

/**
 * 记录情景练习结果（单词情景 / 句式情景），按整组正确率纳入「单元·模块」掌握度。
 */
export function recordEnglishScenarioPractice(data) {
  return request.post('/english/student/scenario/record', data);
}

/** 单元单词列表（含熟练度，复用单词接口） */export function getEnglishWordBook(params) {
  return request.get('/english/word/word-book', { params });
}

/** 进入学习前强制巩固的单词（最近答错 + 复习到期，排除已熟悉） */
export function getEnglishDrillWords(params) {
  return request.get('/english/word/drill', { params });
}

/** 当天是否已完成强制巩固（按账号 + 自然日，跨设备生效） */
export function getEnglishDrillCompleted() {
  return request.get('/english/word/drill/completed');
}

/** 记录单词作答 */
export function recordEnglishWord(data) {
  return request.post('/english/word/record', data);
}

/**
 * 英语单元练习组卷（内容包阶梯卷：按知识点 D1→D3 取题，缺档用最近难度补位）
 */
export function getEnglishUnitPaper(data) {
  return request.post('/english/student/unit-paper', data);
}

/**
 * 英语单元练习交卷（按内容键回读题库判分，返回诊断报告）
 */
export function submitEnglishUnitPaper(data) {
  return request.post('/english/student/unit-paper/submit', data);
}

/**
 * 英语学习诊断组卷（D1→D3 三档各 8 题，共 24 题，跨单元抽取知识点）
 */
export function getEnglishDiagnosePaper(data) {
  return request.post('/english/student/diagnose-paper', data);
}

/**
 * 英语学习诊断交卷（生成家长向报告，含学习趋势）
 */
export function submitEnglishDiagnose(data) {
  return request.post('/english/student/diagnose-submit', data);
}

/**
 * 英语练习交卷上报（阅读理解等前端判分页面）
 * 入参 { module, version, grade, term, unit, durationSeconds, items:[{questionId,questionType,chapter,
 * knowledgePoint,studentAnswer,correctAnswer,analysis,correct,question}] }
 * 返回 { recordId, score, bestScore, lastScore, previousScore, attempts, wrongCount, lastTime }
 */
export function recordEnglishPractice(data) {
  return request.post('/english/student/practice/record', data);
}

/** 英语练习成绩概览（最高分 / 上次 / 错题数） */
export function getEnglishPracticeSummary(params) {
  return request.get('/english/student/practice/summary', { params });
}

/** 上一次练习作答（excludeId 用于排除刚交的这次） */
export function getEnglishPracticeLast(params) {
  return request.get('/english/student/practice/last', { params });
}

/** 历史练习记录列表（学习中心「查看过往记录」，不含逐题明细） */
export function getEnglishPracticeHistory(params) {
  return request.get('/english/student/practice/history', { params });
}

/** 按记录 ID 读取一次作答的逐题明细（仅限本人记录） */
export function getEnglishPracticeDetail(id) {
  return request.get('/english/student/practice/detail', { params: { id } });
}
