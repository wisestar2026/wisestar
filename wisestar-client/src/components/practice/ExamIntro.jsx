/**
 * ExamIntro.jsx - 套卷模拟「卷首信息确认页」
 *
 * 功能:
 *   1. 展示试卷信息: 题量 / 总分 / 限时 / 题型分布
 *   2. 展示考试规则说明（倒计时到点自动交卷、超时未答计错、可跳题浏览）
 *   3. 点「开始作答」才启动倒计时进入答题（避免加载完就开考）
 *
 * 被谁引用: PracticeSessionPage（仅套卷模拟模式）
 *
 * Props:
 *   questions: Array - 已加载题目
 *   totalSeconds: number - 总限时（秒）
 *   totalScore: number - 卷面总分
 *   onStart: () => void - 开始作答回调（触发倒计时启动）
 *   onBack: () => void - 返回选题页
 */

import { Card, Button, Space, Typography, Tag, Row, Col } from 'antd';
import {
  ClockCircleOutlined, FileTextOutlined, SafetyCertificateOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';

const { Title, Text } = Typography;

// 题型中文映射（与 QuestionCard 保持一致）
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Select: '下拉题',
  FillBlank: '填空题', Text: '多行文本', Score: '评分题',
  Remark: '备注说明', Judge: '判断题',
};

export default function ExamIntro({ questions, totalSeconds, totalScore, onStart, onBack }) {
  // 题型分布统计
  const typeCount = {};
  questions.forEach((q) => {
    const t = TYPE_LABELS[q?.questionType] || q?.questionType || '其他';
    typeCount[t] = (typeCount[t] || 0) + 1;
  });

  // 格式化限时 mm:ss / hh:mm:ss
  const formatDuration = (sec) => {
    if (sec == null) return '--:--';
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = sec % 60;
    const mm = String(m).padStart(2, '0');
    const ss = String(s).padStart(2, '0');
    return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
  };

  const rules = [
    '全卷共一次作答机会，提交后不可修改答案',
    '右上角显示全局倒计时，时间到系统自动交卷',
    '超时未作答的题目自动计为错误',
    '可通过「答题卡」跳转浏览任意题目，随时交卷',
  ];

  return (
    <div style={{ maxWidth: 720, margin: '0 auto' }}>
      <Card>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <FileTextOutlined style={{ fontSize: 40, color: '#1677ff' }} />
          <Title level={3} style={{ margin: '12px 0 4px' }}>套卷模拟</Title>
          <Text type="secondary">整卷顺序作答，模拟真实考试节奏</Text>
        </div>

        {/* ---- 试卷信息统计 ---- */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col span={8}>
            <div style={{ textAlign: 'center', background: '#f5f7fa', borderRadius: 8, padding: '16px 8px' }}>
              <div style={{ fontSize: 28, fontWeight: 'bold', color: '#1677ff' }}>{questions.length}</div>
              <Text type="secondary">题目数量</Text>
            </div>
          </Col>
          <Col span={8}>
            <div style={{ textAlign: 'center', background: '#f5f7fa', borderRadius: 8, padding: '16px 8px' }}>
              <div style={{ fontSize: 28, fontWeight: 'bold', color: '#1677ff' }}>{totalScore}</div>
              <Text type="secondary">卷面总分</Text>
            </div>
          </Col>
          <Col span={8}>
            <div style={{ textAlign: 'center', background: '#fff7e6', borderRadius: 8, padding: '16px 8px' }}>
              <div style={{ fontSize: 28, fontWeight: 'bold', color: '#fa8c16' }}>
                <ClockCircleOutlined /> {formatDuration(totalSeconds)}
              </div>
              <Text type="secondary">限时作答</Text>
            </div>
          </Col>
        </Row>

        {/* ---- 题型分布 ---- */}
        <div style={{ marginBottom: 24 }}>
          <Text strong>题型分布：</Text>
          <div style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {Object.entries(typeCount).map(([t, c]) => (
              <Tag key={t} color="blue">{t} × {c}</Tag>
            ))}
          </div>
        </div>

        {/* ---- 考试规则 ---- */}
        <div style={{ background: '#fafafa', borderRadius: 8, padding: '16px 20px', marginBottom: 24 }}>
          <Text strong style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 12 }}>
            <SafetyCertificateOutlined style={{ color: '#1677ff' }} /> 考试须知
          </Text>
          {rules.map((r, i) => (
            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0' }}>
              <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 13 }} />
              <Text style={{ fontSize: 13, color: '#555' }}>{r}</Text>
            </div>
          ))}
        </div>

        {/* ---- 操作按钮 ---- */}
        <Space style={{ width: '100%', justifyContent: 'center' }}>
          <Button size="large" onClick={onBack}>返回选题</Button>
          <Button size="large" type="primary" onClick={onStart}>
            开始作答
          </Button>
        </Space>
      </Card>
    </div>
  );
}
