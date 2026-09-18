/**
 * NodeEditModal.jsx - 教研平台节点编辑弹窗（章节 / 小节 / 知识点通用）
 *
 * 按 nodeType 渲染不同字段：
 *   - chapter:        名称 / 图标 / 排序（年级、学期只读回显）
 *   - section:        名称 / 排序 / 年级 / 学期
 *   - knowledgePoint: 名称 / 排序 / 年级 / 学期 / 简介（content.intro） / 讲解要点（content.points）
 *
 * 知识点保存合并逻辑：
 *   读取 record.content JSON → 保留既有 points（要点）并写入 intro →
 *   以 { points, intro } 整体回写 content（不触碰 imageUrl 等其它字段，record 无该字段时不发送）。
 *
 * 被谁引用: TeachingResearchPlatformPage（点节点名/编辑图标打开）
 */
import { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, Select, Button, message } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { IMPORTANCE_OPTIONS } from '../../utils/importance';

const GRADES = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级'];
const TERMS = ['上', '下'];

const TYPE_TITLES = { chapter: '章节', section: '小节', knowledgePoint: '知识点' };

// 解析知识点 content JSON → { points: [], intro: '' }（解析失败/空则给默认结构）
function parseKpContent(content) {
  let points = [];
  let intro = '';
  try {
    const obj = content ? JSON.parse(content) : {};
    if (Array.isArray(obj.points)) points = obj.points.filter((p) => typeof p === 'string');
    if (typeof obj.intro === 'string') intro = obj.intro;
  } catch { /* 结构异常视为无内容 */ }
  return { points, intro };
}

export default function NodeEditModal({ open, nodeType, record, onCancel, onSaved }) {
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  // 知识点 content 解析结果缓存（编辑期间暂存，保存时与表单值合并）
  const [kpContent, setKpContent] = useState({ points: [], intro: '' });

  // 每次打开按 record 回填
  useEffect(() => {
    if (!open) return;
    if (nodeType === 'knowledgePoint') {
      setKpContent(parseKpContent(record?.content));
      form.setFieldsValue({
        name: record?.name || '',
        sort: record?.sort,
        grade: record?.grade,
        term: record?.term,
        importance: record?.importance || undefined,
        intro: parseKpContent(record?.content).intro,
      });
    } else if (nodeType === 'section') {
      form.setFieldsValue({
        name: record?.name || '',
        sort: record?.sort,
        grade: record?.grade,
        term: record?.term,
        importance: record?.importance || undefined,
      });
    } else {
      form.setFieldsValue({
        name: record?.name || '',
        sort: record?.sort,
        grade: record?.grade || undefined,
        term: record?.term || undefined,
        version: record?.version || '',
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, nodeType, record]);

  const handleOk = () => {
    form.validateFields().then((values) => {
      setSaving(true);
      try {
        if (nodeType === 'chapter') {
          onSaved({
            nodeType,
            id: record.id,
            name: values.name?.trim(),
            grade: values.grade || '',
            term: values.term || '',
            version: values.version?.trim() || '',
            sort: values.sort,
          });
        } else if (nodeType === 'section') {
          onSaved({
            nodeType,
            id: record.id,
            name: values.name?.trim(),
            sort: values.sort,
            grade: values.grade ?? '',
            term: values.term ?? '',
            importance: values.importance ?? '',
          });
        } else {
          // 知识点：合并 intro + points（保留解析所得结构，避免清空既有要点）
          const merged = {
            ...(record?.content ? safeParse(record.content) : {}),
            intro: (values.intro || '').trim(),
            points: kpContent.points,
          };
          // 移除冗余 undefined/空简介
          if (!merged.intro) delete merged.intro;
          const payload = {
            nodeType,
            id: record.id,
            name: values.name?.trim(),
            sort: values.sort,
            grade: values.grade ?? '',
            term: values.term ?? '',
            importance: values.importance ?? '',
            content: JSON.stringify(merged),
          };
          if (record?.imageUrl) payload.imageUrl = record.imageUrl;
          onSaved(payload);
        }
      } catch (e) {
        message.error('保存失败：' + (e?.message || e));
        setSaving(false);
      }
    }).catch(() => {});
  };

  const safeParse = (s) => {
    try { return JSON.parse(s); } catch { return {}; }
  };

  const isKp = nodeType === 'knowledgePoint';

  return (
    <Modal
      title={`编辑${TYPE_TITLES[nodeType] || ''} - ${record?.name || ''}`}
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      confirmLoading={saving}
      destroyOnClose
      width={nodeType === 'section' ? 460 : 680}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="名称"
          rules={[{ required: true, whitespace: true, message: '请输入名称' }]}
        >
          <Input placeholder="请输入名称" maxLength={64} />
        </Form.Item>

        {nodeType === 'chapter' && (
          <div style={{ display: 'flex', gap: 12 }}>
            <Form.Item name="grade" label="年级" style={{ flex: 1 }}>
              <Select allowClear placeholder="请选择年级" options={GRADES.map((g) => ({ value: g, label: g }))} />
            </Form.Item>
            <Form.Item name="term" label="学期" style={{ flex: 1 }}>
              <Select allowClear placeholder="请选择学期" options={TERMS.map((t) => ({ value: t, label: t === '上' ? '上册' : '下册' }))} />
            </Form.Item>
            <Form.Item name="version" label="教材版本" style={{ flex: 1.4 }}>
              <Input placeholder="如：人教版" maxLength={16} />
            </Form.Item>
          </div>
        )}

        {nodeType === 'section' && (
          <div style={{ display: 'flex', gap: 12 }}>
            <Form.Item name="grade" label="年级" style={{ flex: 1 }}>
              <Select allowClear placeholder="请选择年级" options={GRADES.map((g) => ({ value: g, label: g }))} />
            </Form.Item>
            <Form.Item name="term" label="学期" style={{ flex: 1 }}>
              <Select allowClear placeholder="请选择学期" options={TERMS.map((t) => ({ value: t, label: t === '上' ? '上册' : '下册' }))} />
            </Form.Item>
            <Form.Item name="importance" label="重点程度" style={{ flex: 1 }}>
              <Select allowClear placeholder="未标注" options={IMPORTANCE_OPTIONS} />
            </Form.Item>
          </div>
        )}

        {isKp && (
          <>
            <div style={{ display: 'flex', gap: 12 }}>
              <Form.Item name="grade" label="年级" style={{ flex: 1 }}>
                <Select allowClear placeholder="请选择年级" options={GRADES.map((g) => ({ value: g, label: g }))} />
              </Form.Item>
              <Form.Item name="term" label="学期" style={{ flex: 1 }}>
                <Select allowClear placeholder="请选择学期" options={TERMS.map((t) => ({ value: t, label: t === '上' ? '上册' : '下册' }))} />
              </Form.Item>
              <Form.Item name="importance" label="重点程度" style={{ flex: 1 }}>
                <Select allowClear placeholder="未标注" options={IMPORTANCE_OPTIONS} />
              </Form.Item>
            </div>
            <Form.Item name="intro" label="知识点简介（展示在知识点节点旁）">
              <Input.TextArea rows={2} placeholder="一句话介绍该知识点" maxLength={200} showCount />
            </Form.Item>
            <div style={{ fontWeight: 600, marginBottom: 8 }}>讲解要点（学员预习讲解内容）</div>
            {kpContent.points.map((p, idx) => (
              <div key={idx} style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                <Input
                  value={p}
                  onChange={(e) => {
                    const next = [...kpContent.points];
                    next[idx] = e.target.value;
                    setKpContent({ ...kpContent, points: next });
                  }}
                  placeholder="输入讲解要点"
                />
                <Button
                  icon={<MinusCircleOutlined />}
                  onClick={() => setKpContent({ ...kpContent, points: kpContent.points.filter((_, i) => i !== idx) })}
                />
              </div>
            ))}
            <Button
              type="dashed"
              block
              icon={<PlusOutlined />}
              onClick={() => setKpContent({ ...kpContent, points: [...kpContent.points, ''] })}
            >
              添加要点
            </Button>
          </>
        )}

        <Form.Item name="sort" label="排序（数字越小越靠前）">
          <InputNumber min={1} style={{ width: '100%' }} placeholder="默认 1" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
