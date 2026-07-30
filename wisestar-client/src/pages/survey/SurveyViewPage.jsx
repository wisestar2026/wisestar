/**
 * SurveyViewPage.jsx - 公开问卷填写页面（无需登录）
 *
 * 功能:
 *   1. 加载问卷内容，渲染问题列表
 *   2. 用户填写表单（单选/多选/填空/文本/评分）
 *   3. 提交答案到后端 /api/public/saveAnswer
 *   4. 提交成功后显示感谢页
 *
 * URL: /survey/:id（公开路由，不需要登录）
 *
 * 后端接口:
 *   POST /api/public/loadProject  加载问卷
 *   POST /api/public/saveAnswer   提交答案
 */

import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  Card, Form, Radio, Checkbox, Input, Rate, Button,
  Typography, Spin, message, Result, Divider,
} from 'antd';
import { loadProject, saveAnswer } from '../../api/survey';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

export default function SurveyViewPage() {
  const { id: projectId } = useParams();
  const [form] = Form.useForm();

  // ---- 状态 ----
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);  // 是否已提交
  const [projectData, setProjectData] = useState(null); // 问卷数据
  const [error, setError] = useState('');

  // ---- 加载问卷 ----
  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const res = await loadProject({ id: projectId });
        const data = res.data;

        // 问卷状态校验
        if (data.status === 'closed') {
          setError('该问卷已关闭');
          setLoading(false);
          return;
        }

        setProjectData(data);
      } catch (err) {
        const msg = err?.response?.data?.message || err?.message || '加载问卷失败';
        setError(msg);
      } finally {
        setLoading(false);
      }
    })();
  }, [projectId]);

  // ---- 提交答案 ----
  const handleSubmit = async () => {
    try {
      // 触发表单全部字段验证
      const values = await form.validateFields();

      // 构造 answer 格式: { questionId: { optionId: value } }
      const answer = {};
      for (const [qid, val] of Object.entries(values)) {
        if (val != null && val !== '') {
          // 如果是数组（多选），转为 object
          if (Array.isArray(val)) {
            answer[qid] = {};
            val.forEach((v) => { answer[qid][v] = v; });
          } else if (typeof val === 'number') {
            // 评分题返回数字
            answer[qid] = { score: val };
          } else {
            // 其他类型：填空题、文本题等
            answer[qid] = { text: val };
          }
        }
      }

      setSubmitting(true);
      await saveAnswer({
        projectId,
        answer,
        tempSave: 1, // 1 表示已完成
      });

      setSubmitted(true);
      message.success('提交成功！');
    } catch (err) {
      if (err?.errorFields) {
        // 表单验证错误，Ant Design 会自动提示
        message.warning('请完成所有必填问题');
      } else {
        message.error(err?.response?.data?.message || '提交失败，请重试');
      }
    } finally {
      setSubmitting(false);
    }
  };

  // ---- 渲染 ----

  // 加载中
  if (loading) {
    return (
      <div style={{
        display: 'flex', justifyContent: 'center', alignItems: 'center',
        minHeight: '100vh', background: '#f5f5f5',
      }}>
        <Spin size="large" tip="加载问卷中..." />
      </div>
    );
  }

  // 错误
  if (error) {
    return (
      <div style={{
        display: 'flex', justifyContent: 'center', alignItems: 'center',
        minHeight: '100vh', background: '#f5f5f5',
      }}>
        <Result status="warning" title={error} />
      </div>
    );
  }

  // 未找到问卷
  if (!projectData) {
    return (
      <div style={{
        display: 'flex', justifyContent: 'center', alignItems: 'center',
        minHeight: '100vh', background: '#f5f5f5',
      }}>
        <Result status="404" title="问卷不存在" />
      </div>
    );
  }

  // 已提交 - 显示感谢页
  if (submitted) {
    return (
      <div style={{
        display: 'flex', justifyContent: 'center', alignItems: 'center',
        minHeight: '100vh', background: '#f5f5f5',
      }}>
        <Result
          status="success"
          title="提交成功"
          subTitle={projectData.survey?.attribute?.suffix || '感谢您的参与！'}
        />
      </div>
    );
  }

  // ---- 统计 ----
  const survey = projectData.survey;
  const questions = survey?.children || [];

  return (
    <div style={{ minHeight: '100vh', background: '#f5f5f5', padding: '40px 0' }}>
      <div style={{ maxWidth: 800, margin: '0 auto' }}>
        {/* 问卷标题区 */}
        <Card style={{ marginBottom: 16 }}>
          <Title level={3} style={{ textAlign: 'center', marginBottom: 8 }}>
            {survey?.title || projectData.name || '未命名问卷'}
          </Title>
          {survey?.description && (
            <Paragraph style={{ textAlign: 'center', color: '#666' }}>
              {survey.description}
            </Paragraph>
          )}
        </Card>

        {/* 问题列表 */}
        <Form form={form} layout="vertical">
          {questions.map((q, idx) => (
            <Card key={q.id} style={{ marginBottom: 12 }}>
              {/* 问题标题 */}
              <div style={{ marginBottom: 12 }}>
                <Text strong style={{ fontSize: 15 }}>
                  {idx + 1}. {q.title || '(未命名问题)'}
                </Text>
                {q.attribute?.required && (
                  <Text type="danger" style={{ marginLeft: 4 }}>*</Text>
                )}
              </div>

              {/* 根据问题类型渲染不同表单控件 */}
              {q.type === 'Radio' && (
                <Form.Item
                  name={q.id}
                  rules={q.attribute?.required ? [{ required: true, message: '请选择' }] : []}
                  noStyle
                >
                  <Radio.Group>
                    {q.children.map((opt) => (
                      <Radio key={opt.id} value={opt.id} style={{ display: 'block', marginBottom: 8 }}>
                        {opt.title || '(未命名选项)'}
                      </Radio>
                    ))}
                  </Radio.Group>
                </Form.Item>
              )}

              {q.type === 'Checkbox' && (
                <Form.Item
                  name={q.id}
                  rules={q.attribute?.required ? [{ required: true, message: '请选择' }] : []}
                  noStyle
                >
                  <Checkbox.Group>
                    <div>
                      {q.children.map((opt) => (
                        <Checkbox key={opt.id} value={opt.id} style={{ display: 'block', marginBottom: 8 }}>
                          {opt.title || '(未命名选项)'}
                        </Checkbox>
                      ))}
                    </div>
                  </Checkbox.Group>
                </Form.Item>
              )}

              {q.type === 'Select' && (
                <Form.Item
                  name={q.id}
                  rules={q.attribute?.required ? [{ required: true, message: '请选择' }] : []}
                  noStyle
                >
                  <Radio.Group>
                    {q.children.map((opt) => (
                      <Radio key={opt.id} value={opt.id} style={{ display: 'block', marginBottom: 8 }}>
                        {opt.title || '(未命名选项)'}
                      </Radio>
                    ))}
                  </Radio.Group>
                </Form.Item>
              )}

              {q.type === 'FillBlank' && (
                <Form.Item
                  name={q.id}
                  rules={q.attribute?.required ? [{ required: true, message: '请填写' }] : []}
                  noStyle
                >
                  <Input placeholder="请输入" />
                </Form.Item>
              )}

              {q.type === 'Text' && (
                <Form.Item
                  name={q.id}
                  rules={q.attribute?.required ? [{ required: true, message: '请填写' }] : []}
                  noStyle
                >
                  <TextArea rows={3} placeholder="请输入" />
                </Form.Item>
              )}

              {q.type === 'Score' && (
                <Form.Item
                  name={q.id}
                  rules={q.attribute?.required ? [{ required: true, message: '请评分' }] : []}
                  noStyle
                >
                  <Rate count={q.children.length || 5} />
                </Form.Item>
              )}

              {q.type === 'Remark' && (
                <Text type="secondary" style={{ fontSize: 13 }}>
                  {q.title || '(备注说明)'}
                </Text>
              )}
            </Card>
          ))}
        </Form>

        {/* 提交按钮 */}
        {questions.length > 0 && (
          <div style={{ textAlign: 'center', marginTop: 24 }}>
            <Button
              type="primary"
              size="large"
              loading={submitting}
              onClick={handleSubmit}
              style={{ minWidth: 200 }}
            >
              {survey?.attribute?.submitButton || '提交'}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
