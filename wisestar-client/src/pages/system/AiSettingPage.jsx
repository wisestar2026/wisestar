/**
 * AiSettingPage.jsx - AI 服务设置（系统管理）
 *
 * 配置英语 AI 单元生成 / AI 问答 / 学习轨迹总结等模块统一使用的 AI 服务：
 *   - 启用开关
 *   - 可用模型列表（生成与问答默认取第一个）
 *   - 接口地址（任意 OpenAI 兼容服务，留空用默认 SiliconFlow）
 *   - 平台 API Token（读写：Token 保存后不可回显）
 *   - 系统提示词（作用于 AI 问答与学习轨迹总结，可在线调语气）
 *
 * 接口:
 *   GET  /api/system/aiSetting
 *   POST /api/system/update   body { aiSetting: {...} }
 */

import { useEffect, useState } from 'react';
import { Card, Form, Switch, Select, Input, Button, Space, Alert, message } from 'antd';
import { SaveOutlined, ReloadOutlined } from '@ant-design/icons';
import { getAiSetting, saveAiSetting } from '../../api/system';

const MODEL_OPTIONS = [
  { value: 'deepseek-chat', label: 'DeepSeek Chat（deepseek-chat）' },
  { value: 'Qwen/Qwen2.5-72B-Instruct', label: 'Qwen2.5-72B-Instruct' },
  { value: 'Qwen/Qwen2.5-7B-Instruct', label: 'Qwen2.5-7B-Instruct' },
  { value: 'meta-llama/Meta-Llama-3.1-70B-Instruct', label: 'Meta-Llama-3.1-70B-Instruct' },
];

export default function AiSettingPage() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const loadSetting = async () => {
    setLoading(true);
    try {
      const res = await getAiSetting();
      const s = res?.data || {};
      form.setFieldsValue({
        enabled: !!s.enabled,
        models: s.models || [],
        prompt: s.prompt || '',
        baseUrl: s.baseUrl || '',
        token: undefined,
      });
    } catch {
      // 拦截器已统一提示
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSetting();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSave = async () => {
    let values;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSaving(true);
    try {
      const aiSetting = {
        enabled: !!values.enabled,
        models: values.models || [],
        prompt: values.prompt || '',
        // 接口地址：留空表示回退默认 SiliconFlow（后端据空串清空配置）
        baseUrl: (values.baseUrl || '').trim(),
      };
      // Token 仅在填写时才提交（后端对空白 Token 忽略，不覆盖已有配置）
      if (values.token && values.token.trim()) {
        aiSetting.token = values.token.trim();
      }
      await saveAiSetting(aiSetting);
      message.success('AI 服务设置已保存');
      form.setFieldValue('token', undefined);
    } catch {
      // 拦截器已统一提示
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ maxWidth: 860, margin: '0 auto', padding: 20 }}>
      <Card title="AI 服务设置" style={{ marginBottom: 16 }}>
        <Form
          form={form}
          layout="vertical"
          initialValues={{ enabled: false, models: [], prompt: '', baseUrl: '' }}
        >
          <Form.Item name="enabled" label="启用 AI 服务" valuePropName="checked">
            <Switch checkedChildren="已启用" unCheckedChildren="已停用" />
          </Form.Item>

          <Form.Item
            name="models"
            label="可用模型列表"
            tooltip="多个模型按优先级排列，AI 生成与问答默认使用第一个；支持手动输入模型 ID"
          >
            <Select
              mode="tags"
              placeholder="选择或输入模型 ID，如 deepseek-chat"
              options={MODEL_OPTIONS}
              tokenSeparators={[',']}
            />
          </Form.Item>

          <Form.Item
            name="baseUrl"
            label="接口地址（可选）"
            tooltip="任意 OpenAI 兼容服务地址，可填完整地址或基础地址；留空使用默认 SiliconFlow（https://api.siliconflow.cn）"
          >
            <Input
              placeholder="如 https://api.deepseek.com 或 https://api.siliconflow.cn/v1/chat/completions（留空用默认）"
              autoComplete="off"
            />
          </Form.Item>

          <Form.Item
            name="token"
            label="API Token"
            tooltip="Token 保存后仅存储于服务端，出于安全不回显；留空保存表示不修改"
          >
            <Input.Password
              placeholder="sk-...  SiliconFlow API Token（留空表示不修改）"
              autoComplete="new-password"
            />
          </Form.Item>

          <Form.Item name="prompt" label="系统提示词（可选）" tooltip="作用于 AI 问答与学习轨迹每日总结；留空时学习轨迹使用内置默认提示词">
            <Input.TextArea rows={3} placeholder="例如：用简洁温暖的口吻，面向家长总结孩子的每日学习" />
          </Form.Item>

          <Space>
            <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={handleSave}>
              保存设置
            </Button>
            <Button icon={<ReloadOutlined />} loading={loading} onClick={loadSetting}>
              重新加载
            </Button>
          </Space>
        </Form>
      </Card>

      <Alert
        type="info"
        showIcon
        message="支持任意 OpenAI 兼容服务：填写「接口地址 + API Token + 模型」并启用后，英语板块「AI 内容生成」、AI 问答与学习轨迹每日总结即可调用；接口地址留空默认使用 SiliconFlow。未启用时相关操作会给出明确提示并降级为规则模板。"
      />
    </div>
  );
}
