/**
 * AnswerDetailPage.jsx - 答卷详情页
 *
 * 功能:
 *   1. 加载答卷详情（含问卷结构和用户答案）
 *   2. 将问卷题目与用户答案一一对应渲染
 *   3. 高亮显示用户选择的选项 / 填写的内容
 *   4. 展示答卷元信息（IP、时间、耗时、地区）
 *
 * 被谁引用: App.jsx 路由表（/answers/:id）；从 AnswerListPage"详情"按钮进入
 *
 * 答卷答案格式 (answer 字段):
 *   LinkedHashMap<String, Object>，key 为 questionId，
 *   value 为 { optionId: value } 或 { text: "用户输入" } 或 { score: number }
 *   （该结构由 SurveyViewPage.handleSubmit 构造并 POST 到 /api/public/saveAnswer 落库）
 *
 * 数据流:
 *   本页 → getAnswer(answerId) → GET /api/answer?id=xxx
 *   → data: { id, survey(问卷JSON), answer(答案Map), metaInfo, tempSave, createAt }
 *   → 渲染: 题目卡片 + 答案对照（选项高亮 / 填空只读 / 评分星星）
 *
 * URL: /answers/:id
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card, Typography, Spin, Empty, Button, Descriptions, Tag,
  Radio, Checkbox, Input, Rate, Divider, Space,
} from 'antd';
import { ArrowLeftOutlined, ClockCircleOutlined, EnvironmentOutlined } from '@ant-design/icons';
import { getAnswer } from '../../api/answer';

const { Title, Text, Paragraph } = Typography;

export default function AnswerDetailPage() {
  const { id: answerId } = useParams();
  const navigate = useNavigate();

  // ---- 状态 ----
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState(null);  // 答卷详情数据
  const [error, setError] = useState('');

  // ---- 加载答卷详情 ----
  useEffect(() => {
    (async () => {
      setLoading(true);
      setError('');
      try {
        const res = await getAnswer(answerId);
        // 后端可能返回 null（答卷不存在）或正常对象
        if (!res || !res.data) {
          setError('答卷数据不存在或已被删除');
          return;
        }
        setDetail(res.data);
      } catch (err) {
        // 优先展示后端返回的错误消息，其次 HTTP 状态码信息
        const backendMsg = err?.response?.data?.message;
        const httpMsg = err?.message;
        setError(backendMsg || httpMsg || '加载答卷失败，请稍后重试');
      } finally {
        setLoading(false);
      }
    })();
  }, [answerId]);

  // ---- 渲染 ----

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin size="large" tip="加载答卷详情..." />
      </div>
    );
  }

  if (error || !detail) {
    return <Empty description={error || '答卷数据不存在'} />;
  }

  const survey = detail.survey;                       // 问卷 JSON
  const answer = detail.answer || {};                 // 用户答案 Map
  const meta = detail.metaInfo || {};                 // 元信息
  const clientInfo = meta.clientInfo || {};
  const answerInfo = meta.answerInfo || {};

  // 计算答题耗时
  let duration = '-';
  if (answerInfo.startTime && answerInfo.endTime) {
    const diff = (answerInfo.endTime - answerInfo.startTime) / 1000;
    if (diff < 60) duration = `${Math.round(diff)} 秒`;
    else duration = `${Math.floor(diff / 60)} 分 ${Math.round(diff % 60)} 秒`;
  }

  // 解析答案值，返回可读的展示内容
  const getAnswerDisplay = (qid, answerData) => {
    if (!qid) return null;
    const val = answerData[qid];
    if (!val) return <Text type="secondary">(未作答)</Text>;
    if (typeof val !== 'object') return <Text>{String(val)}</Text>;

    const entries = Object.entries(val);
    if (entries.length === 0) return <Text type="secondary">(未作答)</Text>;

    // 填空题/文本题 → { text: "内容" }
    if (entries[0][0] === 'text' || entries[0][0] === '0') {
      return <Text>{entries.map(([, v]) => v != null ? String(v) : '').join('')}</Text>;
    }

    // 评分题 → { score: 4 }
    if (entries.some(([k]) => k === 'score')) {
      const score = parseFloat(val.score || Object.values(val)[0]);
      return <Rate disabled value={score} />;
    }

    // 选择题 → { optionId: "选项文本", ... }
    const labels = entries.map(([, v]) => v != null ? String(v) : '');
    return (
      <Space wrap size={[4, 4]}>
        {labels.map((label, i) => (
          <Tag key={i} color="blue">{label}</Tag>
        ))}
      </Space>
    );
  };

  // 获取问题类型中文名
  const getTypeLabel = (type) => {
    const map = {
      Radio: '单选', Checkbox: '多选', Select: '下拉',
      FillBlank: '填空', Text: '文本', Score: '评分', Remark: '备注',
    };
    return map[type] || type;
  };

  const questions = survey?.children || [];

  return (
    <div>
      {/* ---- 顶部工具栏 ---- */}
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/answers')}>
          返回列表
        </Button>
        <Title level={4} style={{ margin: 0 }}>答卷详情</Title>
      </Space>

      {/* ---- 答卷元信息 ---- */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Descriptions size="small" column={4}>
          <Descriptions.Item label="答卷 ID">{detail.id}</Descriptions.Item>
          <Descriptions.Item label="提交时间">
            {detail.createAt ? new Date(detail.createAt).toLocaleString('zh-CN') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={detail.tempSave === 1 ? 'green' : 'orange'}>
              {detail.tempSave === 1 ? '已完成' : '暂存'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="答题耗时">
            <ClockCircleOutlined style={{ marginRight: 4 }} />
            {duration}
          </Descriptions.Item>
          <Descriptions.Item label="IP 地址">
            <EnvironmentOutlined style={{ marginRight: 4 }} />
            {clientInfo.remoteIp || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="地区">
            {clientInfo.region || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="浏览器">
            {clientInfo.agent
              ? clientInfo.agent.substring(0, 60) + (clientInfo.agent.length > 60 ? '...' : '')
              : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Cookie">
            {clientInfo.cookie ? clientInfo.cookie.substring(0, 20) + '...' : '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* ---- 问卷标题 ---- */}
      <Card style={{ marginBottom: 16 }}>
        <Title level={4} style={{ textAlign: 'center', marginBottom: 4 }}>
          {survey?.title || '未命名问卷'}
        </Title>
        {survey?.description && (
          <Paragraph style={{ textAlign: 'center', color: '#666', marginBottom: 0 }}>
            {survey.description}
          </Paragraph>
        )}
      </Card>

      {/* ---- 题目 + 答案对照 ---- */}
      {questions.length === 0 ? (
        <Empty description="此问卷没有题目" />
      ) : (
        questions.map((q, idx) => (
          <Card key={q.id} style={{ marginBottom: 12 }}>
            {/* 题目标题行 */}
            <div style={{ marginBottom: 12 }}>
              <Space>
                <Tag>{getTypeLabel(q.type)}</Tag>
                <Text strong style={{ fontSize: 15 }}>
                  {idx + 1}. {q.title || '(未命名题目)'}
                </Text>
                {q.attribute?.required && (
                  <Text type="danger">* 必填</Text>
                )}
              </Space>
            </div>

            {/* 选项列表（选择类题目） + 用户答案标记 */}
            {['Radio', 'Checkbox', 'Select'].includes(q.type) && (
              <div>
                {/* 渲染原题的所有选项 */}
                {q.children?.map((opt) => {
                  const userAnswer = answer[q.id];
                  let isSelected = false;
                  if (userAnswer && typeof userAnswer === 'object') {
                    isSelected = Object.prototype.hasOwnProperty.call(userAnswer, opt.id);
                  }

                  return (
                    <div
                      key={opt.id}
                      style={{
                        padding: '6px 12px',
                        marginBottom: 6,
                        borderRadius: 6,
                        background: isSelected ? '#e6f4ff' : '#fafafa',
                        border: isSelected ? '1px solid #1677ff' : '1px solid #f0f0f0',
                        display: 'flex',
                        alignItems: 'center',
                      }}
                    >
                      {q.type === 'Checkbox' ? (
                        <Checkbox checked={isSelected} disabled style={{ marginRight: 8 }} />
                      ) : (
                        <Radio checked={isSelected} disabled style={{ marginRight: 8 }} />
                      )}
                      <Text style={isSelected ? { fontWeight: 'bold', color: '#1677ff' } : {}}>
                        {opt.title || '(未命名选项)'}
                      </Text>
                      {isSelected && (
                        <Tag color="blue" style={{ marginLeft: 'auto' }}>已选</Tag>
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            {/* 填空题 / 文本题 → 显示用户输入 */}
            {['FillBlank', 'Text'].includes(q.type) && (
              <Input.TextArea
                value={(() => {
                  const val = answer[q.id];
                  if (!val) return '';
                  if (typeof val === 'object') return Object.values(val).join('') || '';
                  return String(val);
                })()}
                readOnly
                rows={q.type === 'Text' ? 4 : 2}
                style={{ background: '#fafafa' }}
              />
            )}

            {/* 评分题 → 显示星级 + 分数 */}
            {q.type === 'Score' && (
              <Space>
                <Rate
                  disabled
                  value={(() => {
                    const val = answer[q.id];
                    if (!val) return 0;
                    if (typeof val === 'object') return parseFloat(val.score || Object.values(val)[0]) || 0;
                    return parseFloat(val) || 0;
                  })()}
                  count={q.children?.length || 5}
                />
                <Text type="secondary">
                  {(() => {
                    const val = answer[q.id];
                    if (!val) return '未评分';
                    if (typeof val === 'object') return `${val.score || Object.values(val)[0]} 分`;
                    return `${val} 分`;
                  })()}
                </Text>
              </Space>
            )}

            {/* 备注说明 → 仅展示文本 */}
            {q.type === 'Remark' && (
              <Text type="secondary" style={{ fontSize: 13 }}>
                {q.title || '(备注说明)'}
              </Text>
            )}

            {/* 其他未识别题型 → 原始值 */}
            {!['Radio', 'Checkbox', 'Select', 'FillBlank', 'Text', 'Score', 'Remark'].includes(q.type) && (
              <Text type="secondary">
                {getAnswerDisplay(q.id, answer) || '(无答案数据)'}
              </Text>
            )}
          </Card>
        ))
      )}

      {/* ---- 底部 ---- */}
      <Divider />
      <Text type="secondary" style={{ fontSize: 12 }}>
        {survey?.attribute?.suffix || '感谢您的参与！'}
      </Text>
    </div>
  );
}
