/**
 * englishStudent.js - 学员端英语学习中心 API
 *
 * 接口（后端 EnglishStudentApi / EnglishWordStudentApi，前缀 /api）：
 *   GET  /english/student/units            单元列表 + 学习进度（version/grade/term）
 *   GET  /english/student/sentences        单元句子列表（含熟练度）
 *   GET  /english/student/sentence/study   待学习/复习句子
 *   POST /english/student/sentence/record  记录句子作答 { sentenceId, correct }
 *   GET  /english/student/review           智能复习（单词 + 句子混合队列）
 *   POST /english/student/session          记录学习会话 { type, durationSeconds, correctCount }
 *   GET  /english/word/word-book           单元单词列表（含熟练度，复用单词接口）
 *   POST /english/word/record              记录单词作答 { wordId, correct }
 *
 * 调用方：
 *   - EnglishCenterPage         单元进度与智能复习入口
 *   - EnglishWordLearnPage      单词卡片学习
 *   - EnglishSentenceLearnPage  句子组句学习
 *   - EnglishReviewPage         单词 + 句子混合复习
 */

import request from './request';

/** 单元列表 + 学习进度 */
export function getEnglishUnits(params) {
  return request.get('/english/student/units', { params });
}

/** 单元句子列表（含熟练度） */
export function getEnglishSentences(params) {
  return request.get('/english/student/sentences', { params });
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

/** 记录学习会话 */
export function recordEnglishSession(data) {
  return request.post('/english/student/session', data);
}

/** 单元单词列表（含熟练度，复用单词接口） */
export function getEnglishWordBook(params) {
  return request.get('/english/word/word-book', { params });
}

/** 记录单词作答 */
export function recordEnglishWord(data) {
  return request.post('/english/word/record', data);
}
