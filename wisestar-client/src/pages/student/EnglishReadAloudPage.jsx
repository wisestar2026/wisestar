/**
 * EnglishReadAloudPage.jsx - 学员端英语「课本跟读」
 *
 * 功能:
 *   1. 按当前版本/年级/册别/单元加载课本跟读篇章（篇章 + 逐句中英文）
 *   2. 逐句播放原音（服务端合成音频优先，缺失时回退浏览器语音合成）
 *   3. 麦克风录音跟读，录完可回放对比
 *   4. 录音自动打分（流畅度/完整度为主，音量与 MFCC + DTW 发音接近度为辅，60 分以上记为已跟读）
 *   5. 本地标记已跟读句子，纯前端练习，不上传录音
 *
 * URL: /student/english/read-aloud?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心模块卡
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Modal, Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishReadAloud } from '../../api/englishStudent';
import { getEnglishAudioUrl, getSpeechReport, isSpeechSupported, speakEnglish } from '../../utils/english';
import { scoreLevel, scorePronunciation } from '../../utils/pronunciationScore';
import './EnglishReadAloudPage.css';

export default function EnglishReadAloudPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);

  const [passages, setPassages] = useState([]);
  const [scores, setScores] = useState({});
  const [loading, setLoading] = useState(true);
  const [recordingId, setRecordingId] = useState(null);
  const [recordings, setRecordings] = useState({});
  const [practiced, setPracticed] = useState({});
  const recorderRef = useRef(null);
  const chunksRef = useRef([]);
  const streamRef = useRef(null);

  const load = useCallback(() => {
    if (!unit) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setPracticed({});
    setRecordings({});
    setScores({});
    getEnglishReadAloud({ version, grade, term, unit })
      .then((res) => setPassages(res?.data || []))
      .catch(() => message.error('课本跟读内容加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term, unit]);

  useEffect(() => {
    load();
  }, [load]);

  // 离开页面时释放麦克风与录音 URL
  useEffect(() => () => {
    if (recorderRef.current?.state === 'recording') {
      recorderRef.current.stop();
    }
    streamRef.current?.getTracks?.().forEach((track) => track.stop());
  }, []);

  const startRecord = async (key, text) => {
    if (!navigator.mediaDevices?.getUserMedia || typeof window.MediaRecorder === 'undefined') {
      message.warning('当前浏览器不支持录音，请使用 Chrome / Edge 最新版');
      return;
    }
    try {
      if (recorderRef.current?.state === 'recording') {
        recorderRef.current.stop();
      }
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
      chunksRef.current = [];
      const recorder = new MediaRecorder(stream);
      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) chunksRef.current.push(event.data);
      };
      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || 'audio/webm' });
        setRecordings((prev) => {
          if (prev[key]) URL.revokeObjectURL(prev[key]);
          return { ...prev, [key]: URL.createObjectURL(blob) };
        });
        stream.getTracks().forEach((track) => track.stop());
        setRecordingId(null);
        markAndScore(key, text, blob);
      };
      recorder.start();
      recorderRef.current = recorder;
      setRecordingId(key);
    } catch {
      message.error('无法使用麦克风，请检查浏览器录音权限');
    }
  };

  const stopRecord = () => {
    if (recorderRef.current?.state === 'recording') {
      recorderRef.current.stop();
    }
  };

  // 录音打分：与原音做 MFCC + DTW 比对；技术性失败（拿不到原音/解码失败）仍记为已跟读，
  // 只有分数不达标才要求重读，避免因为环境问题卡住进度
  const markAndScore = (key, text, blob) => {
    setScores((prev) => ({ ...prev, [key]: { status: 'scoring' } }));
    getEnglishAudioUrl(text).then(async (referenceUrl) => {
      if (!referenceUrl) {
        setScores((prev) => ({ ...prev, [key]: { status: 'fail', tips: ['暂时拿不到原音，本次不打分'] } }));
        setPracticed((prev) => ({ ...prev, [key]: true }));
        return;
      }
      const result = await scorePronunciation(blob, referenceUrl);
      if (!result.ok) {
        const reason = result.reason === 'no-speech' || result.reason === 'too-short'
          ? '没有听清，请靠近麦克风完整读完一句'
          : '本次录音无法识别，请再读一次';
        setScores((prev) => ({ ...prev, [key]: { status: 'fail', tips: [reason] } }));
        setPracticed((prev) => ({ ...prev, [key]: true }));
        return;
      }
      setScores((prev) => ({ ...prev, [key]: { status: 'done', ...result } }));
      setPracticed((prev) => ({ ...prev, [key]: result.score >= 60 }));
    }).catch(() => {
      setScores((prev) => ({ ...prev, [key]: { status: 'fail', tips: ['打分失败，请再读一次'] } }));
      setPracticed((prev) => ({ ...prev, [key]: true }));
    });
  };

  // 播放原音：浏览器语音合成失败时明确提示，避免出现「点了没反应」的哑按钮
  const playLine = (text) => {
    if (!isSpeechSupported()) {
      message.warning('当前浏览器不支持英文语音合成，请改用 Chrome / Edge 最新版');
      return;
    }
    speakEnglish(text).then((ok) => {
      if (!ok) {
        message.warning('英文语音没有播放出来，点下方「语音自检」查看浏览器语音环境');
      }
    });
  };

  // 语音自检：把浏览器语音环境与试播结果摊开，便于判断是缺音色、被拦截还是引擎卡死
  const runSpeechCheck = async () => {
    const ok = await speakEnglish('Hello, this is a voice test.');
    const report = getSpeechReport();
    const rows = [
      `语音合成：${report.supported ? '支持' : '不支持（请改用 Chrome / Edge 最新版）'}`,
      `系统音色总数：${report.voiceCount}`,
      `英文音色：${report.englishVoices.length ? report.englishVoices.join('；') : '无（系统没有安装英文语音包）'}`,
      `当前选用：${report.chosen || '浏览器默认音色'}`,
      `试播结果：${ok ? '成功' : '失败'}`,
      `引擎状态：speaking=${String(report.speaking)}，paused=${String(report.paused)}`,
    ];
    Modal.info({
      title: '语音自检',
      width: 560,
      content: (
        <div className="eng-ra-check">
          {rows.map((row) => <div key={row}>{row}</div>)}
        </div>
      ),
    });
  };

  if (loading) {
    return <div className="eng-ra-wrap"><div className="eng-ra-loading"><Spin size="large" /></div></div>;
  }

  if (!unit || passages.length === 0) {
    return (
      <div className="eng-ra-wrap">
        <div className="eng-ra-empty">
          <div className="eng-ra-empty-emoji">🎤</div>
          <div className="eng-ra-empty-title">{!unit ? '未选择单元' : '本单元暂无跟读内容'}</div>
          <button type="button" className="eng-ra-btn" onClick={() => navigate('/student/english')}>返回英语学习中心</button>
        </div>
      </div>
    );
  }

  const totalLines = passages.reduce((sum, passage) => sum + (passage.lines?.length || 0), 0);
  const doneCount = Object.keys(practiced).length;

  return (
    <div className="eng-ra-wrap">
      <div className="eng-ra-head">
        <button type="button" className="eng-ra-back" onClick={() => navigate('/student/english')}>← 返回学习中心</button>
        <div>
          <div className="eng-ra-unit">{unit} · 课本跟读</div>
          <div className="eng-ra-title">听一句，读一句</div>
        </div>
        <div className="eng-ra-progress">
          <b>{doneCount}</b><span>/{totalLines} 句已跟读</span>
        </div>
      </div>

      <div className="eng-ra-list">
        {passages.map((passage, pi) => (
          <section className="eng-ra-passage" key={passage.id || `${passage.title}-${pi}`}>
            <div className="eng-ra-passage-head">
              <div className="eng-ra-passage-title">{passage.title || `课文 ${pi + 1}`}</div>
              <div className="eng-ra-passage-meta">{(passage.lines || []).length} 句</div>
            </div>
            <div className="eng-ra-lines">
              {(passage.lines || []).map((line, li) => {
                const key = `${pi}-${li}`;
                const isRecording = recordingId === key;
                return (
                  <div className={`eng-ra-card ${practiced[key] ? 'done' : ''}`} key={key}>
                    <div className="eng-ra-index">{li + 1}</div>
                    <div className="eng-ra-body">
                      <div className="eng-ra-en">{line.en}</div>
                      {line.zh && <div className="eng-ra-zh">{line.zh}</div>}
                      {recordings[key] && (
                        <audio className="eng-ra-audio" controls src={recordings[key]}>
                          <track kind="captions" />
                        </audio>
                      )}
                      {scores[key]?.status === 'scoring' && (
                        <div className="eng-ra-score is-scoring">正在打分…</div>
                      )}
                      {scores[key]?.status === 'fail' && (
                        <div className="eng-ra-score is-retry">
                          {scores[key].tips.map((tip) => <span key={tip}>{tip}</span>)}
                        </div>
                      )}
                      {scores[key]?.status === 'done' && (
                        <div className={`eng-ra-score is-${scoreLevel(scores[key].score).key}`}>
                          <span className="eng-ra-score-main">
                            <b>{scores[key].score}</b> 分
                            <em>{scoreLevel(scores[key].score).label}</em>
                          </span>
                          <span className="eng-ra-score-meta">
                            完整度 {scores[key].completeness} · 流畅度 {scores[key].fluency} · 音量 {scores[key].volume} · 发音接近度 {scores[key].timbre}
                          </span>
                          {scores[key].tips.map((tip) => <span key={tip}>{tip}</span>)}
                        </div>
                      )}
                    </div>
                    <div className="eng-ra-actions">
                      <button
                        type="button"
                        className="eng-ra-play"
                        onClick={() => playLine(line.en)}
                      >
                        🔊 听原音
                      </button>
                      {isRecording ? (
                        <button type="button" className="eng-ra-record is-recording" onClick={stopRecord}>
                          ⏹ 停止录音
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="eng-ra-record"
                          disabled={Boolean(recordingId)}
                          onClick={() => startRecord(key, line.en)}
                        >
                          🎤 跟读
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </section>
        ))}
      </div>

      <div className="eng-ra-tip">
        小提示：先听原音，再跟着完整读一遍，录完自动打分（60 分以上记为已跟读）；打分主要看是否连贯读完（明显卡顿或漏读才扣分），带点口音没关系，不联网、录音不上传。
        {!isSpeechSupported() && ' 当前浏览器不支持英文语音合成，请改用 Chrome / Edge 最新版。'}
        <button type="button" className="eng-ra-check-btn" onClick={runSpeechCheck}>
          语音自检
        </button>
      </div>
    </div>
  );
}
