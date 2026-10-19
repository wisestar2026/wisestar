/**
 * EnglishReadAloudPage.jsx - 学员端英语「课本跟读」
 *
 * 功能:
 *   1. 按当前版本/年级/册别/单元加载课本跟读篇章（篇章 + 逐句中英文）
 *   2. 逐句播放原音（后端音频优先，缺失时浏览器语音合成）
 *   3. 麦克风录音跟读，录完可回放对比
 *   4. 本地标记已跟读句子，纯前端练习，不上传录音
 *
 * URL: /student/english/read-aloud?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心模块卡
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishReadAloud } from '../../api/englishStudent';
import { speakEnglish } from '../../utils/english';
import './EnglishReadAloudPage.css';

export default function EnglishReadAloudPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);

  const [passages, setPassages] = useState([]);
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

  const startRecord = async (key) => {
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
        setPracticed((prev) => ({ ...prev, [key]: true }));
        stream.getTracks().forEach((track) => track.stop());
        setRecordingId(null);
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
                    </div>
                    <div className="eng-ra-actions">
                      <button
                        type="button"
                        className="eng-ra-play"
                        onClick={() => speakEnglish(line.en)}
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
                          onClick={() => startRecord(key)}
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
        小提示：先听原音，再按住节奏朗读；录音只保存在本机，不会上传。
      </div>
    </div>
  );
}
