/**
 * WordAiManagePage.jsx - 英语单词 AI 生成管理页
 *
 * 功能:
 *   1. 批量生成 AI 内容（TTS 音频/DALL-E 图片/GPT 例句）
 *   2. 按版本/年级/单元筛选批量生成
 *   3. 查看生成进度和结果
 *
 * URL: /english/word-ai（受 AuthGuard 保护）
 */

import { useState } from 'react';
import { Card, Button, Select, Space, Progress, message, Alert } from 'antd';
import { ThunderboltOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';

const API_BASE = '/api/english/word-ai';

export default function WordAiManagePage() {
  const [version, setVersion] = useState('');
  const [grade, setGrade] = useState('');
  const [unit, setUnit] = useState('');
  const [generating, setGenerating] = useState(false);
  const [result, setResult] = useState(null);

  // 批量生成 AI 内容
  const handleBatchGenerate = () => {
    if (!version && !grade && !unit) {
      message.warning('请至少选择一个筛选条件');
      return;
    }

    setGenerating(true);
    setResult(null);

    const params = new URLSearchParams({
      ...(version && { version }),
      ...(grade && { grade }),
      ...(unit && { unit }),
    });

    fetch(`${API_BASE}/generate-by-condition?${params}`, {
      method: 'POST',
    })
      .then((res) => res.json())
      .then((res) => {
        if (res.code === 200) {
          setResult(res.data);
          if (res.data.failed === 0) {
            message.success(`生成完成：成功 ${res.data.success} 个`);
          } else {
            message.warning(`生成完成：成功 ${res.data.success} 个，失败 ${res.data.failed} 个`);
          }
        } else {
          message.error('生成失败');
        }
      })
      .catch(() => message.error('生成失败'))
      .finally(() => setGenerating(false));
  };

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 20 }}>
      <Card title="🤖 AI 单词内容生成" extra={<ThunderboltOutlined style={{ fontSize: 24 }} />}>
        {/* 筛选条件 */}
        <Space wrap style={{ marginBottom: 20 }}>
          <Select
            placeholder="教材版本"
            allowClear
            style={{ width: 150 }}
            value={version}
            onChange={setVersion}
            options={[
              { value: '人教版', label: '人教版' },
              { value: '苏教版', label: '苏教版' },
              { value: '北师大版', label: '北师大版' },
              { value: '外研版', label: '外研版' },
            ]}
          />
          <Select
            placeholder="年级"
            allowClear
            style={{ width: 120 }}
            value={grade}
            onChange={setGrade}
            options={[
              { value: '一年级', label: '一年级' },
              { value: '二年级', label: '二年级' },
              { value: '三年级', label: '三年级' },
              { value: '四年级', label: '四年级' },
              { value: '五年级', label: '五年级' },
              { value: '六年级', label: '六年级' },
            ]}
          />
          <Select
            placeholder="单元"
            allowClear
            style={{ width: 120 }}
            value={unit}
            onChange={setUnit}
            options={[
              { value: 'Unit 1', label: 'Unit 1' },
              { value: 'Unit 2', label: 'Unit 2' },
              { value: 'Unit 3', label: 'Unit 3' },
              { value: 'Unit 4', label: 'Unit 4' },
              { value: 'Unit 5', label: 'Unit 5' },
              { value: 'Unit 6', label: 'Unit 6' },
            ]}
          />
          <Button
            type="primary"
            icon={<ThunderboltOutlined />}
            onClick={handleBatchGenerate}
            loading={generating}
          >
            批量生成 AI 内容
          </Button>
        </Space>

        {/* 生成结果 */}
        {result && (
          <div style={{ marginBottom: 20 }}>
            <Alert
              message={`生成总数：${result.total}`}
              description={
                <div>
                  <div style={{ display: 'flex', gap: 20, marginTop: 10 }}>
                    <span style={{ color: '#52c41a' }}>
                      <CheckCircleOutlined /> 成功：{result.success}
                    </span>
                    {result.failed > 0 && (
                      <span style={{ color: '#ff4d4f' }}>
                        <CloseCircleOutlined /> 失败：{result.failed}
                      </span>
                    )}
                  </div>
                  {result.errors && result.errors.length > 0 && (
                    <div style={{ marginTop: 10, maxHeight: 200, overflow: 'auto', fontSize: 12 }}>
                      {result.errors.map((err, i) => (
                        <div key={i} style={{ color: '#ff4d4f', marginBottom: 4 }}>
                          {err}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              }
              type={result.failed === 0 ? 'success' : 'warning'}
              showIcon
            />
          </div>
        )}

        {/* 功能说明 */}
        <div style={{ background: '#f5f5f5', padding: 16, borderRadius: 8 }}>
          <h4 style={{ margin: '0 0 12px 0' }}>AI 生成服务说明</h4>
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            <li><strong>TTS 语音生成</strong>：自动生成单词发音（需要配置 TTS API）</li>
            <li><strong>DALL-E 图片生成</strong>：自动生成单词配图（需要配置 DALL-E API）</li>
            <li><strong>GPT 例句生成</strong>：自动生成英文例句（需要配置 GPT API）</li>
          </ul>
          <div style={{ marginTop: 12, color: '#faad14' }}>
            ⚠️ 注意：需要先在 application.yml 中配置相应的 API Key 才能使用 AI 生成功能
          </div>
        </div>
      </Card>
    </div>
  );
}
