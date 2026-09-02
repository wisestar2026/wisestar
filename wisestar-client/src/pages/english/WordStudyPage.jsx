/**
 * WordStudyPage.jsx - 英语单词学习页
 *
 * 功能:
 *   1. 单词卡片展示（拼写 + 音标 + 释义 + 图片 + 例句）
 *   2. 听音辨词（播放发音，选择正确单词）
 *   3. 听写拼写（播放发音，拼写单词）
 *   4. 学习结果记录（熟练度 + 下次复习时间）
 *
 * URL: /english/word（受 AuthGuard 保护）
 * 被谁引用：App.jsx 路由表
 *
 * 数据流:
 *   GET /api/english/word/study → 待学习/复习单词列表
 *   POST /api/english/word/record → 记录学习结果
 */

import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Button, Space, Input, Radio, message, Progress, Tag } from 'antd';
import { SoundOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';

const API_BASE = '/api/english/word';

export default function WordStudyPage() {
  const navigate = useNavigate();
  const [words, setWords] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [loading, setLoading] = useState(false);
  const [mode, setMode] = useState('card'); // card / listen / spell
  const [selectedAnswer, setSelectedAnswer] = useState(null);
  const [spelledWord, setSpelledWord] = useState('');
  const [showResult, setShowResult] = useState(false);
  const [isCorrect, setIsCorrect] = useState(false);
  const [stats, setStats] = useState({ learned: 0, correct: 0, wrong: 0 });

  // 加载待学习单词
  const loadWords = useCallback(() => {
    setLoading(true);
    fetch(`${API_BASE}/study?limit=10`)
      .then((res) => res.json())
      .then((res) => {
        if (res.code === 200) {
          setWords(res.data || []);
          setCurrentIndex(0);
          setStats({ learned: 0, correct: 0, wrong: 0 });
        } else {
          message.error('加载失败');
        }
      })
      .catch(() => message.error('网络异常'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadWords();
  }, [loadWords]);

  // 当前单词
  const currentWord = words[currentIndex];

  // 播放发音
  const playAudio = () => {
    if (currentWord?.audioUrl) {
      const audio = new Audio(currentWord.audioUrl);
      audio.play();
    } else {
      // 使用浏览器内置发音（备选方案）
      const utterance = new SpeechSynthesisUtterance(currentWord?.spell || '');
      utterance.lang = 'en-US';
      speechSynthesis.speak(utterance);
    }
  };

  // 听音辨词 - 选择答案
  const handleListenSelect = (answer) => {
    if (selectedAnswer) return; // 已选择
    setSelectedAnswer(answer);
    const correct = answer === currentWord.spell;
    setIsCorrect(correct);
    setShowResult(true);
    setStats((prev) => ({
      ...prev,
      learned: prev.learned + 1,
      correct: prev.correct + (correct ? 1 : 0),
      wrong: prev.wrong + (correct ? 0 : 1),
    }));
    // 记录学习结果
    recordLearning(correct);
  };

  // 听写拼写 - 提交答案
  const handleSpellSubmit = () => {
    if (!spelledWord.trim()) return;
    const correct = spelledWord.trim().toLowerCase() === currentWord.spell.toLowerCase();
    setIsCorrect(correct);
    setShowResult(true);
    setStats((prev) => ({
      ...prev,
      learned: prev.learned + 1,
      correct: prev.correct + (correct ? 1 : 0),
      wrong: prev.wrong + (correct ? 0 : 1),
    }));
    recordLearning(correct);
  };

  // 记录学习结果
  const recordLearning = (correct) => {
    fetch(`${API_BASE}/record`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ wordId: currentWord.id, correct }),
    }).catch(() => {});
  };

  // 下一个单词
  const nextWord = () => {
    if (currentIndex < words.length - 1) {
      setCurrentIndex(currentIndex + 1);
      setSelectedAnswer(null);
      setSpelledWord('');
      setShowResult(false);
      setIsCorrect(false);
    } else {
      // 学习完成
      message.success(`学习完成！共学习 ${stats.learned} 个，正确 ${stats.correct} 个`);
      navigate('/english/word-book');
    }
  };

  if (loading) {
    return <div style={{ padding: 40, textAlign: 'center' }}>加载中...</div>;
  }

  if (!currentWord) {
    return (
      <div style={{ padding: 40, textAlign: 'center' }}>
        <div style={{ fontSize: 48, marginBottom: 16 }}>🎉</div>
        <div>暂无待学习单词</div>
        <Button type="primary" onClick={() => navigate('/english/word-book')} style={{ marginTop: 16 }}>
          查看单词本
        </Button>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 20 }}>
      {/* 顶部进度条 */}
      <div style={{ marginBottom: 20 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
          <span>学习进度</span>
          <span>{currentIndex + 1} / {words.length}</span>
        </div>
        <Progress percent={((currentIndex + 1) / words.length) * 100} showInfo={false} />
        <Space style={{ marginTop: 12 }}>
          <Tag color="green">已学 {stats.learned}</Tag>
          <Tag color="green">正确 {stats.correct}</Tag>
          <Tag color="red">错误 {stats.wrong}</Tag>
        </Space>
      </div>

      {/* 模式切换 */}
      <div style={{ marginBottom: 16, textAlign: 'center' }}>
        <Radio.Group value={mode} onChange={(e) => setMode(e.target.value)}>
          <Radio.Button value="card"> 卡片学习</Radio.Button>
          <Radio.Button value="listen">🎧 听音辨词</Radio.Button>
          <Radio.Button value="spell">✍️ 听写拼写</Radio.Button>
        </Radio.Group>
      </div>

      {/* 单词卡片 */}
      <Card
        title={
          <Space>
            <span>{mode === 'card' ? '📖' : mode === 'listen' ? '🎧' : '️'}</span>
            <span>
              {mode === 'spell' ? '听写模式' : mode === 'listen' ? '听音辨词' : '单词学习'}
            </span>
          </Space>
        }
        extra={
          <Button icon={<SoundOutlined />} onClick={playAudio}>
            播放发音
          </Button>
        }
        style={{ marginBottom: 20 }}
      >
        {mode === 'card' && (
          <div>
            <div style={{ fontSize: 32, fontWeight: 'bold', marginBottom: 8, color: '#1890ff' }}>
              {currentWord.spell}
            </div>
            <div style={{ fontSize: 18, color: '#666', marginBottom: 16 }}>
              {currentWord.phonetic && <span style={{ marginRight: 16 }}>/ {currentWord.phonetic} /</span>}
            </div>
            <div style={{ fontSize: 20, marginBottom: 16, color: '#333' }}>
              {currentWord.meaning}
            </div>
            {currentWord.imageUrl && (
              <img
                src={currentWord.imageUrl}
                alt={currentWord.spell}
                style={{ maxWidth: '100%', maxHeight: 200, borderRadius: 8, marginBottom: 16 }}
              />
            )}
            {currentWord.exampleSentence && (
              <div style={{ fontSize: 16, color: '#666', background: '#f5f5f5', padding: 12, borderRadius: 8 }}>
                <b>例句：</b>{currentWord.exampleSentence}
              </div>
            )}
            <div style={{ textAlign: 'center', marginTop: 20 }}>
              <Button type="primary" size="large" onClick={nextWord}>
                下一个
              </Button>
            </div>
          </div>
        )}

        {mode === 'listen' && (
          <div>
            <div style={{ textAlign: 'center', marginBottom: 20 }}>
              <Button type="primary" size="large" icon={<SoundOutlined />} onClick={playAudio}>
                点击播放发音
              </Button>
            </div>
            <div style={{ textAlign: 'center' }}>
              <Radio.Group
                value={selectedAnswer}
                onChange={(e) => handleListenSelect(e.target.value)}
                disabled={!!selectedAnswer}
              >
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                  {[currentWord.spell, 'option1', 'option2', 'option3'].map((opt, i) => (
                    <Radio.Button key={i} value={opt} style={{ fontSize: 16 }}>
                      {opt}
                    </Radio.Button>
                  ))}
                </div>
              </Radio.Group>
              {showResult && (
                <div style={{ marginTop: 20, fontSize: 18, textAlign: 'center' }}>
                  {isCorrect ? (
                    <div style={{ color: '#52c41a' }}>
                      <CheckCircleOutlined /> 回答正确！
                    </div>
                  ) : (
                    <div style={{ color: '#ff4d4f' }}>
                      <CloseCircleOutlined /> 回答错误，正确答案：{currentWord.spell}
                    </div>
                  )}
                  <Button type="primary" onClick={nextWord} style={{ marginTop: 16 }}>
                    下一个
                  </Button>
                </div>
              )}
            </div>
          </div>
        )}

        {mode === 'spell' && (
          <div>
            <div style={{ textAlign: 'center', marginBottom: 20 }}>
              <Button type="primary" size="large" icon={<SoundOutlined />} onClick={playAudio}>
                点击播放发音
              </Button>
            </div>
            <div style={{ textAlign: 'center' }}>
              <Input
                placeholder="请输入单词拼写"
                value={spelledWord}
                onChange={(e) => setSpelledWord(e.target.value)}
                disabled={!!showResult}
                style={{ fontSize: 18, width: 300 }}
                onPressEnter={handleSpellSubmit}
              />
              {!showResult && (
                <Button type="primary" onClick={handleSpellSubmit} style={{ marginTop: 16 }}>
                  提交
                </Button>
              )}
              {showResult && (
                <div style={{ marginTop: 20, fontSize: 18 }}>
                  {isCorrect ? (
                    <div style={{ color: '#52c41a' }}>
                      <CheckCircleOutlined /> 拼写正确！
                    </div>
                  ) : (
                    <div style={{ color: '#ff4d4f' }}>
                      <CloseCircleOutlined /> 拼写错误，正确答案：{currentWord.spell}
                    </div>
                  )}
                  <Button type="primary" onClick={nextWord} style={{ marginTop: 16 }}>
                    下一个
                  </Button>
                </div>
              )}
            </div>
          </div>
        )}
      </Card>

      {/* 底部导航 */}
      <div style={{ textAlign: 'center' }}>
        <Button onClick={() => navigate('/english/word-book')}>返回单词本</Button>
      </div>
    </div>
  );
}
