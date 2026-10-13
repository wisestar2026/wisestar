/**
 * AiSettingPage.jsx - AI 服务设置（系统管理）
 *
 * 配置英语 AI 单元生成 / AI 问答 / 学习轨迹总结等模块统一使用的 AI 服务：
 *   - 启用开关
 *   - 可用模型列表（生成与问答默认取第一个）
 *   - 接口地址（任意 OpenAI 兼容服务，留空用默认 SiliconFlow）
 *   - 平台 API Token（读写：Token 保存后不可回显）
 *   - 通用系统提示词（兼容旧配置，作用于 AI 问答与学习轨迹每日总结）
 *   - 分场景系统提示词（按场景覆盖，留空回退内置默认）
 *
 * 接口:
 *   GET  /api/system/aiSetting
 *   GET  /api/system/aiPromptDefaults
 *   POST /api/system/update   body { aiSetting: {...} }
 */

import { useEffect, useState } from 'react';
import { Card, Form, Switch, Select, Input, Button, Space, Alert, message, Collapse, Typography } from 'antd';
import { SaveOutlined, ReloadOutlined } from '@ant-design/icons';
import { getAiSetting, saveAiSetting, getAiPromptDefaults } from '../../api/system';

const MODEL_OPTIONS = [
  // 硅基流动（默认；9B 及以下小模型永久免费）
  { value: 'Qwen/Qwen2.5-7B-Instruct', label: '硅基流动·免费：Qwen2.5-7B-Instruct' },
  { value: 'Qwen/Qwen3-8B', label: '硅基流动·免费：Qwen3-8B' },
  { value: 'Qwen/Qwen2.5-Coder-7B-Instruct', label: '硅基流动·免费：Qwen2.5-Coder-7B-Instruct' },
  { value: 'THUDM/GLM-4-9B-0414', label: '硅基流动·免费：GLM-4-9B' },
  { value: 'deepseek-ai/DeepSeek-R1-Distill-Qwen-7B', label: '硅基流动·免费：DeepSeek-R1-Distill-Qwen-7B' },
  // 智谱 AI（GLM Flash 系列官方标注永久免费，国内直连；baseUrl https://open.bigmodel.cn/api/paas/v4）
  { value: 'glm-4.7-flash', label: '智谱·免费：glm-4.7-flash' },
  { value: 'glm-4-flash', label: '智谱·免费：glm-4-flash' },
  { value: 'glm-4.5-flash', label: '智谱·免费：glm-4.5-flash' },
  { value: 'glm-4.6v-flash', label: '智谱·免费：glm-4.6v-flash（视觉）' },
  // 百度千帆（部分模型永久免费；baseUrl https://qianfan.baidubce.com/v2）
  { value: 'ernie-speed-8k', label: '百度千帆·免费：ernie-speed-8k' },
  { value: 'ernie-3.5-8k', label: '百度千帆·免费：ernie-3.5-8k' },
  // 付费但常用（质量更高）
  { value: 'deepseek-chat', label: 'DeepSeek（付费）：deepseek-chat' },
  { value: 'Qwen/Qwen2.5-72B-Instruct', label: '硅基流动（付费）：Qwen2.5-72B-Instruct' },
];

export default function AiSettingPage() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [promptMetas, setPromptMetas] = useState([]);

  const loadPromptMetas = async () => {
    try {
      const res = await getAiPromptDefaults();
      setPromptMetas(res?.data || []);
    } catch {
      // 拦截器已统一提示
    }
  };

  const loadSetting = async () => {
    setLoading(true);
    try {
      const res = await getAiSetting();
      const s = res?.data || {};
      form.setFieldsValue({
        enabled: !!s.enabled,
        models: s.models || [],
        prompt: s.prompt || '',
        prompts: s.prompts || {},
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
    loadPromptMetas();
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
      // 分场景提示词：仅提交用户填写的项；空串表示清除该场景覆盖（回退内置默认）
      const prompts = {};
      promptMetas.forEach((meta) => {
        const v = values.prompts?.[meta.key];
        if (v !== undefined && v !== null) {
          prompts[meta.key] = v;
        }
      });
      const aiSetting = {
        enabled: !!values.enabled,
        models: values.models || [],
        prompt: values.prompt || '',
        prompts,
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
      await loadSetting();
    } catch {
      // 拦截器已统一提示
    } finally {
      setSaving(false);
    }
  };

  const fillDefault = (meta) => {
    form.setFieldValue(['prompts', meta.key], meta.defaultPrompt || '');
  };

  return (
    <div style={{ maxWidth: 860, margin: '0 auto', padding: 20 }}>
      <Card title="AI 服务设置" style={{ marginBottom: 16 }}>
        <Form
          form={form}
          layout="vertical"
          initialValues={{ enabled: false, models: [], prompt: '', prompts: {}, baseUrl: '' }}
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
            tooltip="任意 OpenAI 兼容服务地址，可填完整地址、基础地址或仅到域名；留空使用默认 SiliconFlow（https://api.siliconflow.cn）"
          >
            <Input
              placeholder="留空=硅基流动；智谱 https://open.bigmodel.cn/api/paas/v4 ；百度千帆 https://qianfan.baidubce.com/v2"
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

          <Form.Item
            name="prompt"
            label="通用系统提示词（兼容旧配置，可选）"
            tooltip="历史兼容字段，作为「AI 问答」「学习轨迹·每日叙述」在未单独配置时的兜底；留空使用内置默认提示词。新场景建议使用下方「分场景提示词」"
          >
            <Input.TextArea rows={3} placeholder="例如：用简洁温暖的口吻，面向家长总结孩子的每日学习" />
          </Form.Item>

          <Form.Item label="分场景系统提示词" tooltip="每个 AI 场景可使用不同提示词；留空表示使用内置默认">
            <Collapse
              size="small"
              items={promptMetas.map((meta) => ({
                key: meta.key,
                label: meta.label,
                children: (
                  <>
                    <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
                      {meta.description}
                    </Typography.Paragraph>
                    <Form.Item name={['prompts', meta.key]} noStyle>
                      <Input.TextArea
                        rows={6}
                        placeholder={
                          meta.defaultPrompt
                            ? '留空使用内置默认提示词；可点「填入默认模板」后在此基础上修改'
                            : '留空使用内置默认提示词'
                        }
                        style={{ fontFamily: 'monospace' }}
                      />
                    </Form.Item>
                    {meta.defaultPrompt ? (
                      <Button size="small" type="link" style={{ paddingLeft: 0 }} onClick={() => fillDefault(meta)}>
                        填入默认模板
                      </Button>
                    ) : null}
                  </>
                ),
              }))}
            />
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
        style={{ marginBottom: 16 }}
        title="完全免费方案（国内直连）"
        description={
          <div style={{ lineHeight: 1.9 }}>
            <div>方案一 · 智谱 AI：接口地址填 <b>https://open.bigmodel.cn/api/paas/v4</b>，模型选 <b>glm-4.7-flash</b>（或 glm-4-flash），Token 在 bigmodel.cn 注册后获取（GLM Flash 系列官方标注永久免费）。</div>
            <div>方案二 · 硅基流动：接口地址留空即可，模型选 <b>Qwen/Qwen2.5-7B-Instruct</b> 等 9B 及以下小模型（永久免费），Token 在 siliconflow.cn 注册后获取。</div>
            <div>方案三 · 百度千帆：接口地址填 <b>https://qianfan.baidubce.com/v2</b>，模型选 <b>ernie-speed-8k</b>，Token 在百度智能云千帆控制台获取。</div>
            <div>填写「接口地址 + API Token + 模型」并启用后即可使用。免费额度/模型随平台政策调整，请以各平台官网为准。</div>
          </div>
        }
      />

      <Alert
        type="info"
        showIcon
        title="支持任意 OpenAI 兼容服务：填写「接口地址 + API Token + 模型」并启用后，英语板块「AI 内容生成」、AI 问答与学习轨迹每日总结即可调用；接口地址留空默认使用 SiliconFlow。未启用时相关操作会给出明确提示并降级为规则模板。各 AI 场景可使用「分场景提示词」分别调优，留空则使用内置默认。"
      />
    </div>
  );
}
