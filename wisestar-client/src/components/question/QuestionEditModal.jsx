/**
 * QuestionEditModal.jsx - 题目新建/编辑弹窗（全局通用）
 *
 * 功能:
 *   1. 题目标题（支持文字 + 内嵌图片）
 *   2. 题型选择（单选/多选/下拉/填空/多行文本/评分/备注/判断）
 *   3. 选项增删编辑
 *   4. 正确答案设置（选择题型下拉选择、填空自由输入）
 *   5. 分值 & 计分方式
 *   6. 答案解析（支持 Markdown）
 *   7. 题目图片上传（通过 /api/file/create）
 *   8. 所属题库选择、标签、分类
 *
 * Props:
 *   open: boolean           - 弹窗是否可见
 *   onCancel: () => void    - 关闭回调
 *   onSave: (data) => void  - 保存回调，data 为 TemplateRequest 格式
 *   record: object | null   - 编辑时传入的题目数据，新建时传 null
 *   repos: Array<{id, name}> - 可选题库列表（供题库选择下拉）
 */

import { useState, useEffect, useRef } from 'react';
import {
  Modal, Input, Select, Switch, Space, Typography, Button, InputNumber,
  message, Upload, Image, Divider, Tag,
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, UploadOutlined, BulbOutlined, CloseOutlined,
} from '@ant-design/icons';
import { QUESTION_TYPES, TYPES_WITH_OPTIONS, createQuestion } from '../../utils/surveyHelpers';
import { uploadImage } from '../../api/upload';

const { Text } = Typography;

// 判断题固定选项
const JUDGE_OPTIONS_RAW = [
  { id: 'true', type: 'Option', title: '正确', attribute: {} },
  { id: 'false', type: 'Option', title: '错误', attribute: {} },
];

// 需要展示正确答案选择器的题型
const CHOICE_LIKE_TYPES = ['Radio', 'Checkbox', 'Select', 'Judge'];

// 完整题型列表（含判断题）
const ALL_TYPES = [
  ...QUESTION_TYPES,
  { label: '判断题', value: 'Judge' },
];

export default function QuestionEditModal({ open, onCancel, onSave, record, repos = [] }) {
  // ---- 基础字段 ----
  const [title, setTitle] = useState('');
  const [qType, setQType] = useState('Radio');
  const [options, setOptions] = useState(['', '']);
  const [required, setRequired] = useState(false);
  const [tags, setTags] = useState('');
  const [category, setCategory] = useState('');
  const [repoId, setRepoId] = useState(undefined);
  const [saving, setSaving] = useState(false);

  // ---- 答案 & 解析 ----
  const [answer, setAnswer] = useState('');
  const [analysis, setAnalysis] = useState('');
  const [score, setScore] = useState(5);
  const [scoreMode, setScoreMode] = useState('onlyOne');

  // ---- 题目图片 ----
  const [images, setImages] = useState([]); // [{ uid, name, url, status }]
  const [uploading, setUploading] = useState(false);

  // 初始值备份（用于 cancel 时还原）
  const initialRef = useRef({});

  // ---- 初始化（编辑回填 / 新建清空） ----
  useEffect(() => {
    if (!open) return;

    if (record) {
      // 编辑模式：回填已有数据
      setTitle(record.name || '');
      setQType(record.questionType || 'Radio');
      setTags((record.tag || []).join(','));
      setCategory(record.category || '');
      setRepoId(record.repoId || undefined);

      const tmpl = record.template;
      if (tmpl?.children?.length > 0) {
        setOptions(tmpl.children.map((c) => c.title || ''));
      } else {
        setOptions(qType === 'Judge' ? ['正确', '错误'] : ['', '']);
      }

      const attr = tmpl?.attribute || {};
      setRequired(attr.required || false);
      setAnswer(attr.examCorrectAnswer || '');
      setAnalysis(attr.examAnalysis || '');
      setScore(attr.examScore || 5);
      setScoreMode(attr.examScoreMode || 'onlyOne');

      // 回填图片
      const imgs = (attr.examImages || []).map((url, i) => ({
        uid: `img_${i}`,
        name: `image_${i}`,
        url,
        status: 'done',
      }));
      setImages(imgs);
    } else {
      // 新建模式：清空所有字段
      setTitle('');
      setQType('Radio');
      setOptions(['', '']);
      setRequired(false);
      setTags('');
      setCategory('');
      setRepoId(undefined);
      setAnswer('');
      setAnalysis('');
      setScore(5);
      setScoreMode('onlyOne');
      setImages([]);
    }

    initialRef.current = { title: record?.name, qType: record?.questionType, images: images.map((i) => i.url) };
  }, [open, record]); // eslint-disable-line react-hooks/exhaustive-deps

  // ---- 题型切换时重置选项和答案 ----
  const handleTypeChange = (val) => {
    setQType(val);
    if (val === 'Judge') { setOptions(['正确', '错误']); setAnswer(''); }
    else if (!TYPES_WITH_OPTIONS.includes(val)) { setOptions([]); setAnswer(''); }
    else if (!TYPES_WITH_OPTIONS.includes(qType) && TYPES_WITH_OPTIONS.includes(val)) { setOptions(['', '']); }
  };

  // ---- 图片上传 ----
  const handleImageUpload = async (file) => {
    setUploading(true);
    try {
      const result = await uploadImage(file);
      if (result?.previewUrl) {
        setImages((prev) => [...prev, {
          uid: `img_${Date.now()}`,
          name: result.originalName || file.name,
          url: result.previewUrl,
          status: 'done',
        }]);
        message.success('图片上传成功');
      } else {
        message.error('图片上传失败：未获取到预览地址');
      }
    } catch {
      message.error('图片上传失败');
    } finally {
      setUploading(false);
    }
    return false; // 阻止默认上传行为
  };

  const handleRemoveImage = (uid) => {
    setImages((prev) => prev.filter((i) => i.uid !== uid));
  };

  // ---- 保存 ----
  const handleSave = async () => {
    if (!title.trim()) { message.warning('请输入题目内容'); return; }

    const needsOptions = TYPES_WITH_OPTIONS.includes(qType) || qType === 'Judge';
    if (needsOptions && options.some((o) => !o.trim())) {
      message.warning('请填写所有选项'); return;
    }

    setSaving(true);
    try {
      // 构建题目 JSON
      const templateJson = createQuestion(qType);
      templateJson.title = title;

      // 属性（含答案、解析、图片）
      templateJson.attribute = {
        required,
        examCorrectAnswer: answer || undefined,
        examAnalysis: analysis || undefined,
        examScore: score,
        examScoreMode: scoreMode,
        examImages: images.length > 0 ? images.map((i) => i.url) : undefined,
      };

      // 选项
      if (qType === 'Judge') {
        templateJson.children = JUDGE_OPTIONS_RAW;
      } else if (TYPES_WITH_OPTIONS.includes(qType)) {
        templateJson.children = options.filter((o) => o.trim()).map((title) => ({
          id: 'opt_' + Math.random().toString(36).substring(2, 10),
          type: 'Option',
          title,
          attribute: {},
        }));
      }

      const tagArr = tags ? tags.split(',').map((t) => t.trim()).filter(Boolean) : [];
      const payload = {
        name: title,
        questionType: qType,
        template: templateJson,
        tag: tagArr,
        category: category || undefined,
        // 如果选择了所属题库，传 repoId
        ...(repoId ? { repoId, mode: 'exam' } : {}),
      };

      // 编辑时带上 ID
      if (record?.id) {
        payload.id = record.id;
        payload.repoId = record.repoId || repoId;
        payload.mode = record.mode || 'exam';
      }

      await onSave(payload);
    } finally {
      setSaving(false);
    }
  };

  // ---- 当前题型的选项列表（用于正确答案选择器） ----
  const answerOptions = (qType === 'Judge' ? ['正确', '错误'] : options.filter((o) => o.trim()))
    .map((title, i) => ({ label: `${String.fromCharCode(65 + i)}. ${title}`, value: title }));

  return (
    <Modal
      title={record?.id ? '编辑题目' : '新建题目'}
      open={open}
      onCancel={onCancel}
      onOk={handleSave}
      confirmLoading={saving}
      okText="保存"
      cancelText="取消"
      width={700}
      destroyOnHidden
      mask={{ closable: false }}
    >
      <Space orientation="vertical" style={{ width: '100%' }} size="small">

        {/* ========== 基础信息 ========== */}
        <Text type="secondary" style={{ fontSize: 11 }}>基础信息</Text>

        <Input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="请输入题目内容"
          maxLength={500}
          showCount
        />

        <div style={{ display: 'flex', gap: 12 }}>
          <Select
            value={qType}
            onChange={handleTypeChange}
            options={ALL_TYPES}
            style={{ width: 160 }}
          />
          {repos.length > 0 && (
            <Select
              value={repoId}
              onChange={setRepoId}
              placeholder="选择所属题库（可选）"
              allowClear
              style={{ flex: 1 }}
              options={repos.map((r) => ({ label: r.name, value: r.id }))}
            />
          )}
        </div>

        {/* ========== 选项编辑区 ========== */}
        {(TYPES_WITH_OPTIONS.includes(qType) || qType === 'Judge') && (
          <div style={{ background: '#fafafa', borderRadius: 6, padding: 10 }}>
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 6 }}>选项</Text>
            {options.map((opt, idx) => (
              <div key={idx} style={{ display: 'flex', gap: 8, marginBottom: 4 }}>
                <Tag color="blue" style={{ margin: 0, minWidth: 24, textAlign: 'center' }}>
                  {String.fromCharCode(65 + idx)}
                </Tag>
                <Input
                  value={opt}
                  onChange={(e) => { const n = [...options]; n[idx] = e.target.value; setOptions(n); }}
                  placeholder={`选项 ${String.fromCharCode(65 + idx)}`}
                  disabled={qType === 'Judge'}
                />
                {qType !== 'Judge' && options.length > 2 && (
                  <Button danger size="small" icon={<DeleteOutlined />}
                    onClick={() => setOptions(options.filter((_, i) => i !== idx))}
                  />
                )}
              </div>
            ))}
            {qType !== 'Judge' && (
              <Button type="dashed" size="small" block icon={<PlusOutlined />}
                onClick={() => setOptions([...options, ''])}
              >
                添加选项
              </Button>
            )}
          </div>
        )}

        {/* ========== 必填开关 ========== */}
        <Space>
          <Switch checked={required} onChange={setRequired} size="small" />
          <Text type="secondary" style={{ fontSize: 11 }}>此题必填</Text>
        </Space>

        <Divider style={{ margin: '4px 0' }} />

        {/* ========== 答案与解析 ========== */}
        <Text strong style={{ fontSize: 12 }}><BulbOutlined /> 答案与解析</Text>

        <div style={{ display: 'flex', gap: 12 }}>
          {/* 正确答案 */}
          <div style={{ flex: 1 }}>
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 2 }}>正确答案</Text>
            {CHOICE_LIKE_TYPES.includes(qType) ? (
              <Select
                value={answer || undefined}
                onChange={setAnswer}
                placeholder="请选择"
                style={{ width: '100%' }}
                allowClear
                options={answerOptions}
              />
            ) : (
              <Input
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
                placeholder="输入正确答案"
              />
            )}
          </div>

          {/* 分值 */}
          <div style={{ width: 100 }}>
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 2 }}>分值</Text>
            <InputNumber min={0} max={100} value={score} onChange={setScore} style={{ width: '100%' }} />
          </div>
        </div>

        {/* 计分方式 */}
        <div>
          <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 2 }}>计分方式</Text>
          <Select value={scoreMode} onChange={setScoreMode} style={{ width: '100%' }}
            options={[
              { label: '完全匹配得分', value: 'onlyOne' },
              { label: '答对任一得分', value: 'selectCorrect' },
              { label: '全选才得分', value: 'selectAll' },
              { label: '人工评分', value: 'manual' },
            ]}
          />
        </div>

        {/* 答案解析 */}
        <div>
          <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 2 }}>答案解析</Text>
          <Input.TextArea
            value={analysis}
            onChange={(e) => setAnalysis(e.target.value)}
            placeholder="输入答案解析（如：通过勾股定理可得 a² + b² = c²）"
            rows={3}
          />
        </div>

        <Divider style={{ margin: '4px 0' }} />

        {/* ========== 题目图片 ========== */}
        <Text strong style={{ fontSize: 12 }}>题目图片</Text>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {images.map((img) => (
            <div key={img.uid} style={{ position: 'relative', display: 'inline-block' }}>
              <Image src={img.url} width={80} height={80} style={{ objectFit: 'cover', borderRadius: 4 }} />
              <Button
                size="small" danger shape="circle" icon={<CloseOutlined />}
                style={{ position: 'absolute', top: -8, right: -8 }}
                onClick={() => handleRemoveImage(img.uid)}
              />
            </div>
          ))}
          <Upload
            beforeUpload={handleImageUpload}
            showUploadList={false}
            accept="image/*"
            disabled={uploading}
          >
            <Button icon={<UploadOutlined />} loading={uploading}>上传图片</Button>
          </Upload>
        </div>
        <Text type="secondary" style={{ fontSize: 10 }}>
          支持上传题目配图，上传后自动关联到当前题目
        </Text>

        <Divider style={{ margin: '4px 0' }} />

        {/* ========== 标签 & 分类 ========== */}
        <div style={{ display: 'flex', gap: 12 }}>
          <Input
            value={tags}
            onChange={(e) => setTags(e.target.value)}
            placeholder="标签（逗号分隔）如: 通用,单选"
            style={{ flex: 1 }}
          />
          <Input
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            placeholder="分类（如: 数学、语文）"
            style={{ flex: 1 }}
          />
        </div>

      </Space>
    </Modal>
  );
}
