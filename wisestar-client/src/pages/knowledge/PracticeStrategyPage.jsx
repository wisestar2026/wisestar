/**
 * PracticeStrategyPage.jsx - 全局出题策略（知识管理）
 *
 * 配置学员端各出题场景的默认题量与防重复规则，所有小节默认继承本策略，
 * 小节可在「小节管理 - 练习设置」中按字段覆盖：
 *   - 例题/预习：预习学习后检测题量
 *   - 专项训练：每知识点出题上限
 *   - 小节通关：按知识点数分档的固定题量（小/中/大）
 *   - 章节测试：exam 小节基础题量，以及是否按章知识点数自动扩容
 *   - 防重复：出题时排除最近 N 次同范围已做题
 *   - 缺题储备目标：缺题清单按重要程度的储备目标题量
 *
 * 接口:
 *   GET  /api/system/practiceStrategy
 *   POST /api/system/practiceStrategy/update
 */

import { useEffect, useState } from 'react';
import { Card, Form, InputNumber, Switch, Button, Space, Divider, Alert, message } from 'antd';
import { SaveOutlined, ReloadOutlined } from '@ant-design/icons';
import { getPracticeStrategy, updatePracticeStrategy } from '../../api/system';

export default function PracticeStrategyPage() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const loadSetting = async () => {
    setLoading(true);
    try {
      const res = await getPracticeStrategy();
      const s = res?.data || {};
      form.setFieldsValue({
        previewCount: s.previewCount,
        drillPerKp: s.drillPerKp,
        trialSmallMaxKp: s.trialSmallMaxKp,
        trialMediumMaxKp: s.trialMediumMaxKp,
        trialSmallCount: s.trialSmallCount,
        trialMediumCount: s.trialMediumCount,
        trialLargeCount: s.trialLargeCount,
        examCount: s.examCount,
        examExpandByKp: s.examExpandByKp !== false,
        repeatWindow: s.repeatWindow,
        reserveNormal: s.reserveNormal,
        reserveMinor: s.reserveMinor,
        reserveKey: s.reserveKey,
      });
    } catch (e) {
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
    } catch (e) {
      return;
    }
    setSaving(true);
    try {
      await updatePracticeStrategy(values);
      message.success('出题策略已保存');
    } catch (e) {
      // 拦截器已统一提示
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: 20 }}>
      <Card title="出题策略" style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="以下为全局默认策略，所有小节默认继承；未单独配置的小节按此出题。"
        />
        <Form form={form} layout="vertical">
          <Divider orientation="left">例题 / 预习</Divider>
          <Form.Item
            name="previewCount"
            label="预习检测题量"
            tooltip="小节预习学习后，例题检测给出的题目数量"
          >
            <InputNumber min={1} max={50} style={{ width: 200 }} addonAfter="题" />
          </Form.Item>

          <Divider orientation="left">专项训练</Divider>
          <Form.Item
            name="drillPerKp"
            label="每知识点出题上限"
            tooltip="专项训练按知识点抽题时，单个知识点最多出的题目数"
          >
            <InputNumber min={1} max={50} style={{ width: 200 }} addonAfter="题/知识点" />
          </Form.Item>

          <Divider orientation="left">小节通关</Divider>
          <Form.Item
            name="trialSmallMaxKp"
            label="「小」档知识点数上限"
            tooltip="小节知识点数不超过该值时，按「小」档题量出题"
          >
            <InputNumber min={1} max={50} style={{ width: 200 }} addonAfter="个" />
          </Form.Item>
          <Form.Item
            name="trialMediumMaxKp"
            label="「中」档知识点数上限"
            tooltip="知识点数在「小」档上限与该值之间时按「中」档；超过则按「大」档"
          >
            <InputNumber min={1} max={50} style={{ width: 200 }} addonAfter="个" />
          </Form.Item>
          <Space size="large" wrap>
            <Form.Item name="trialSmallCount" label="「小」档题量">
              <InputNumber min={1} max={50} style={{ width: 160 }} addonAfter="题" />
            </Form.Item>
            <Form.Item name="trialMediumCount" label="「中」档题量">
              <InputNumber min={1} max={50} style={{ width: 160 }} addonAfter="题" />
            </Form.Item>
            <Form.Item name="trialLargeCount" label="「大」档题量">
              <InputNumber min={1} max={50} style={{ width: 160 }} addonAfter="题" />
            </Form.Item>
          </Space>

          <Divider orientation="left">章节测试</Divider>
          <Form.Item
            name="examCount"
            label="章节测试基础题量"
            tooltip="章节测评（exam）小节的基础出题数量"
          >
            <InputNumber min={1} max={50} style={{ width: 200 }} addonAfter="题" />
          </Form.Item>
          <Form.Item
            name="examExpandByKp"
            label="按知识点数自动扩容"
            valuePropName="checked"
            tooltip="开启后，当本章知识点数超过基础题量时，章节测试题量自动扩容到知识点数，保证每知识点至少 1 题"
          >
            <Switch checkedChildren="开启" unCheckedChildren="关闭" />
          </Form.Item>

          <Divider orientation="left">防重复</Divider>
          <Form.Item
            name="repeatWindow"
            label="防重复滑窗"
            tooltip="出题时排除该学员最近 N 次同范围已做过的题；0 表示不去重"
          >
            <InputNumber min={0} max={20} style={{ width: 200 }} addonAfter="次" />
          </Form.Item>

          <Divider orientation="left">缺题储备目标</Divider>
          <Space size="large" wrap>
            <Form.Item name="reserveNormal" label="一般知识点储备">
              <InputNumber min={1} max={200} style={{ width: 160 }} addonAfter="题" />
            </Form.Item>
            <Form.Item name="reserveMinor" label="次重点知识点储备">
              <InputNumber min={1} max={200} style={{ width: 160 }} addonAfter="题" />
            </Form.Item>
            <Form.Item name="reserveKey" label="重点知识点储备">
              <InputNumber min={1} max={200} style={{ width: 160 }} addonAfter="题" />
            </Form.Item>
          </Space>

          <Space>
            <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={handleSave}>
              保存策略
            </Button>
            <Button icon={<ReloadOutlined />} loading={loading} onClick={loadSetting}>
              重新加载
            </Button>
          </Space>
        </Form>
      </Card>
    </div>
  );
}
