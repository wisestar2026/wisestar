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
 *   7. 题目图片上传（通过 POST /api/file/create）
 *   8. 所属题库选择、标签、分类
 *   9. 知识点属性录入区（学科/章节/知识点多值/难度）
 *
 * 被谁引用: QuestionListPage（题目管理页"新建题目"/"编辑"按钮）
 *
 * Props:
 *   open: boolean           - 弹窗是否可见
 *   onCancel: () => void    - 关闭回调
 *   onSave: (data) => void  - 保存回调，data 为 TemplateRequest 格式
 *   record: object | null   - 编辑时传入的题目数据（TemplateView），新建时传 null
 *   repos: Array<{id, name}> - 可选题库列表（供题库选择下拉）
 *
 * 重点逻辑说明（后期维护必读）:
 *
 * 1. 多选题答案的多选存取（mode="multiple" + \n 分隔字符串）
 *    - UI 层: 多选题正确答案用 <Select mode="multiple">，state 是数组 ['A选项文本','B选项文本']
 *    - 落库层: attribute.examCorrectAnswer 存为 \n 分隔的字符串 "A选项文本\nB选项文本"
 *      （见 handleSave 中 answer.join('\n')，兼容后端单字段存储）
 *    - 回填层: 编辑时按 qType === 'Checkbox' 将字符串 split('\n') 拆回数组
 *      （见初始化 useEffect 中 setAnswer 的三元表达式）
 *    - 为什么这么写: 后端 SurveySchema.Attribute.examCorrectAnswer 是单字符串字段，
 *      多选答案必须用分隔符串起来；\n 作为分隔符是因为选项文本理论上不含换行，安全
 *
 * 2. 知识点属性（学科/章节/知识点/难度）
 *    - 录入: 学科/章节为单行输入，知识点为 tags 多值输入，难度为下拉（easy/medium/hard）
 *    - 存储: 同时写入顶层 payload（subject/chapter/knowledgePoint[]/difficulty，存 t_template 表）
 *      和 template.attribute（subject/chapter/knowledgePoint/difficulty 快照，供入卷时随卷保存）
 *    - 快照意义: 题目转入问卷后，历史答卷的分析依赖快照，题目后续修改不影响已发出的问卷
 *      （快照的读取方见 surveyHelpers.templateToQuestion）
 *
 * 核心数据流:
 *   QuestionListPage → QuestionEditModal → handleSave 组装 TemplateRequest
 *   → onSave(payload) → createTemplate / updateTemplate → POST /api/template/create|update
 *   图片: 上传 → uploadImage(file) → POST /api/file/create → previewUrl 存入 attribute.examImages
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
// 判断题不是真正的选择题，选项固定为"正确/错误"且不可编辑
// 注意: 这里的 id 是 'true'/'false' 字符串，后续答案就存储为"正确"/"错误"文本
const JUDGE_OPTIONS_RAW = [
  { id: 'true', type: 'Option', title: '正确', attribute: {} },
  { id: 'false', type: 'Option', title: '错误', attribute: {} },
];

// 需要展示正确答案选择器的题型（答案从选项里选，而非自由输入）
const CHOICE_LIKE_TYPES = ['Radio', 'Checkbox', 'Select', 'Judge'];

// 完整题型列表（含判断题）
// QUESTION_TYPES 来自 surveyHelpers，只有 7 种；这里补充 Judge 形成 8 种
const ALL_TYPES = [
  ...QUESTION_TYPES,
  { label: '判断题', value: 'Judge' },
];

export default function QuestionEditModal({ open, onCancel, onSave, record, repos = [] }) {
  // ---- 基础字段 ----
  const [title, setTitle] = useState('');       // 题目标题
  const [qType, setQType] = useState('Radio');  // 题型
  const [options, setOptions] = useState(['', '']); // 选项文本数组（UI 层状态）
  const [required, setRequired] = useState(false);  // 是否必填
  const [tags, setTags] = useState('');         // 标签（逗号分隔的原始字符串，保存时拆分）
  const [category, setCategory] = useState(''); // 分类
  const [repoId, setRepoId] = useState(undefined); // 所属题库 ID（可选）
  const [saving, setSaving] = useState(false);  // 保存请求进行中

  // ---- 答案 & 解析 ----
  // answer 的类型随题型变化:
  //   - Checkbox（多选题）: 数组 ['选项文本A', '选项文本B']（Select mode="multiple"）
  //   - 其他选择题型: 单个选项文本字符串
  //   - 填空/文本: 自由输入的字符串
  const [answer, setAnswer] = useState('');
  const [analysis, setAnalysis] = useState(''); // 答案解析
  const [score, setScore] = useState(5);        // 分值，默认 5 分
  const [scoreMode, setScoreMode] = useState('onlyOne'); // 计分方式: onlyOne/selectCorrect/selectAll/manual

  // ---- 知识点属性（学科/章节/知识点/难度） ----
  const [subject, setSubject] = useState('');         // 学科（如: 数学）
  const [chapter, setChapter] = useState('');         // 章节（如: 第三章 函数）
  const [knowledgePoints, setKnowledgePoints] = useState([]); // 知识点（多值数组，tags 模式录入）
  const [difficulty, setDifficulty] = useState(undefined);    // 难度: easy/medium/hard

  // ---- 题目图片 ----
  const [images, setImages] = useState([]); // [{ uid, name, url, status }] antd Upload 文件列表结构
  const [uploading, setUploading] = useState(false); // 图片上传中标记

  // 初始值备份（用于 cancel 时还原）
  const initialRef = useRef({});

  // ---- 初始化（编辑回填 / 新建清空） ----
  // 依赖 [open, record]: 弹窗每次打开时执行一次，用 record 填充或清空表单
  useEffect(() => {
    if (!open) return;

    if (record) {
      // 编辑模式：回填已有数据（record 为 TemplateView，见 api/template.js）
      setTitle(record.name || '');
      setQType(record.questionType || 'Radio');
      setTags((record.tag || []).join(','));      // 标签数组 → 逗号分隔字符串
      setCategory(record.category || '');
      setRepoId(record.repoId || undefined);

      const tmpl = record.template;
      // 选项回填: 从问卷 JSON 的 children 取选项文本
      if (tmpl?.children?.length > 0) {
        setOptions(tmpl.children.map((c) => c.title || ''));
      } else {
        setOptions(qType === 'Judge' ? ['正确', '错误'] : ['', '']);
      }

      const attr = tmpl?.attribute || {};
      setRequired(attr.required || false);
      // 多选题正确答案为多选（存 \n 分隔字符串，回填时拆分为数组）
      // 为什么这么写: 后端 examCorrectAnswer 是单字符串字段，多选答案以 \n 连接；
      // 回填时必须 split 还原为数组，才能正确渲染 Select mode="multiple"
      setAnswer(qType === 'Checkbox'
        ? (attr.examCorrectAnswer ? String(attr.examCorrectAnswer).split('\n').filter(Boolean) : [])
        : (attr.examCorrectAnswer || ''));
      setAnalysis(attr.examAnalysis || '');
      setScore(attr.examScore || 5);
      setScoreMode(attr.examScoreMode || 'onlyOne');
      // 知识点属性回填: 兼容数组 / 单值两种情况，统一转为数组
      setSubject(attr.subject || '');
      setChapter(attr.chapter || '');
      setKnowledgePoints(Array.isArray(attr.knowledgePoint) ? attr.knowledgePoint : (attr.knowledgePoint ? [attr.knowledgePoint] : []));
      setDifficulty(attr.difficulty || undefined);

      // 回填图片: examImages 为 URL 数组 → antd Upload 文件列表格式
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
      setSubject('');
      setChapter('');
      setKnowledgePoints([]);
      setDifficulty(undefined);
      setImages([]);
    }

    // 备份初始值（title/题型/图片），供取消时对比是否需要还原
    initialRef.current = { title: record?.name, qType: record?.questionType, images: images.map((i) => i.url) };
  }, [open, record]); // eslint-disable-line react-hooks/exhaustive-deps

  // ---- 题型切换时重置选项和答案 ----
  // 为什么这么写: 不同题型的选项结构/答案形态不同，切换时必须同步清理，
  // 避免残留的选项或数组型答案导致保存校验/渲染出错
  const handleTypeChange = (val) => {
    setQType(val);
    if (val === 'Judge') { setOptions(['正确', '错误']); setAnswer(''); }
    else if (val === 'Checkbox') { setOptions(['', '']); setAnswer([]); }       // 多选题答案初始化为空数组
    else if (!TYPES_WITH_OPTIONS.includes(val)) { setOptions([]); setAnswer(''); } // 无选项题型
    else if (!TYPES_WITH_OPTIONS.includes(qType) && TYPES_WITH_OPTIONS.includes(val)) { setOptions(['', '']); setAnswer(''); } // 填空→单选
  };

  // ---- 图片上传 ----
  // 数据流: 选择图片 → uploadImage(file) → POST /api/file/create (FormData)
  // → 返回 { previewUrl } → 追加到 images 状态（最终保存时写入 attribute.examImages）
  // 返回 false 阻止 antd Upload 的默认自动上传行为（由本函数手动控制）
  const handleImageUpload = async (file) => {
    setUploading(true);
    try {
      const result = await uploadImage(file);
      if (result?.previewUrl) {
        // 追加到文件列表，uid 用时间戳保证唯一
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

  // 按 uid 移除某张图片（对应右上角红色 × 按钮）
  const handleRemoveImage = (uid) => {
    setImages((prev) => prev.filter((i) => i.uid !== uid));
  };

  // ---- 保存 ----
  // 组装 TemplateRequest 并回调 onSave（由父组件决定 create 还是 update）
  const handleSave = async () => {
    // 前置校验: 标题必填
    if (!title.trim()) { message.warning('请输入题目内容'); return; }

    // 需要选项的题型（选择题型 + 判断题）必须填满所有选项
    const needsOptions = TYPES_WITH_OPTIONS.includes(qType) || qType === 'Judge';
    if (needsOptions && options.some((o) => !o.trim())) {
      message.warning('请填写所有选项'); return;
    }

    setSaving(true);
    try {
      // 构建题目 JSON（createQuestion 生成问卷节点骨架，含 id/type/attribute/children）
      const templateJson = createQuestion(qType);
      templateJson.title = title;

      // 属性（含答案、解析、图片、知识点属性）
      // 重点: 多选题答案多选 → \n 分隔字符串（与回填时 split('\n') 对称）
      // 空值用 undefined，后端序列化时会丢弃，避免存空串脏数据
      templateJson.attribute = {
        required,
        // 多选题答案存 \n 分隔字符串，其余题型为字符串
        examCorrectAnswer: qType === 'Checkbox'
          ? (answer && answer.length ? answer.join('\n') : undefined)
          : (answer || undefined),
        examAnalysis: analysis || undefined,
        examScore: score,
        examScoreMode: scoreMode,
        examImages: images.length > 0 ? images.map((i) => i.url) : undefined,
        // 知识点属性快照（写入 template.attribute，供题目转入问卷时随卷保存）
        subject: subject || undefined,
        chapter: chapter || undefined,
        knowledgePoint: knowledgePoints.length > 0 ? knowledgePoints : undefined,
        difficulty: difficulty || undefined,
      };

      // 选项
      // 判断题: 固定用"正确/错误"两个选项（不可编辑）
      // 选择题型: 过滤空选项后生成 Option 节点，ID 用随机字符串
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

      // 标签: 逗号分隔字符串 → 去空格过滤空值 → 数组
      const tagArr = tags ? tags.split(',').map((t) => t.trim()).filter(Boolean) : [];
      const payload = {
        name: title,
        questionType: qType,
        template: templateJson,
        tag: tagArr,
        category: category || undefined,
        // 知识点属性（顶层字段存 t_template 表，attribute 内快照供入卷使用）
        // 双写原因: 顶层字段供"题目管理"列表筛选（学科/章节/难度/知识点四维筛选），
        // attribute 快照供"从系统题目选择"入卷时随问卷保存
        subject: subject || undefined,
        chapter: chapter || undefined,
        knowledgePoint: knowledgePoints.length > 0 ? knowledgePoints : undefined,
        difficulty: difficulty || undefined,
        // 如果选择了所属题库，传 repoId + mode='exam'（表示考试题库类型的题目）
        ...(repoId ? { repoId, mode: 'exam' } : {}),
      };

      // 编辑时带上 ID（父组件据此判断调用 updateTemplate 而非 createTemplate）
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
  // 生成 { label: "A. 选项文本", value: "选项文本" } 格式
  // value 直接存选项文本（而非 ID），保证提交答案与题目 JSON 中选项文本一致，
  // 也便于后端判分（ExamScore 按文本匹配）
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

        {/* ========== 知识点属性（学科/章节/知识点/难度） ========== */}
        {/* 重点: 三级归类（学科→章节→知识点）+ 难度，用于学生答题情况分析；
             题目入卷时由 templateToQuestion 快照到问卷节点 attribute，
             历史答卷的分析不受后续题目修改影响 */}
        <Text strong style={{ fontSize: 12 }}>知识点属性</Text>
        <Text type="secondary" style={{ fontSize: 10, display: 'block' }}>
          学科 → 章节 → 知识点三级归类，用于学生答题情况分析；入卷时自动快照，历史答卷不受题目修改影响
        </Text>
        <div style={{ display: 'flex', gap: 12 }}>
          {/* 学科: 单行文本录入（保存时写入顶层 subject + attribute.subject） */}
          <Input
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            placeholder="学科（如：数学）"
            style={{ flex: 1 }}
          />
          {/* 章节: 单行文本录入（保存时写入顶层 chapter + attribute.chapter） */}
          <Input
            value={chapter}
            onChange={(e) => setChapter(e.target.value)}
            placeholder="章节（如：第三章 函数）"
            style={{ flex: 1 }}
          />
          {/* 难度: 下拉选择 easy/medium/hard（保存时写入顶层 difficulty + attribute.difficulty） */}
          <Select
            value={difficulty}
            onChange={setDifficulty}
            placeholder="难度"
            allowClear
            style={{ width: 120 }}
            options={[
              { label: '简单', value: 'easy' },
              { label: '中等', value: 'medium' },
              { label: '困难', value: 'hard' },
            ]}
          />
        </div>
        {/* 知识点: mode="tags" 多值输入，回车/逗号分隔确认
             状态为数组（knowledgePoints），保存时写入顶层 knowledgePoint[] + attribute.knowledgePoint */}
        <Select
          mode="tags"
          value={knowledgePoints}
          onChange={setKnowledgePoints}
          placeholder="知识点（可输入多个，回车确认，如：函数单调性）"
          style={{ width: '100%' }}
          tokenSeparators={[',', '，']}
        />

        <Divider style={{ margin: '4px 0' }} />

        {/* ========== 答案与解析 ========== */}
        <Text strong style={{ fontSize: 12 }}><BulbOutlined /> 答案与解析</Text>

        <div style={{ display: 'flex', gap: 12 }}>
          {/* 正确答案 */}
          <div style={{ flex: 1 }}>
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 2 }}>正确答案</Text>
            {CHOICE_LIKE_TYPES.includes(qType) ? (
              qType === 'Checkbox' ? (
                /* 多选题答案: mode="multiple" 多选，state 为数组
                   保存时 handleSave 里 join('\n') 转字符串落库 */
                <Select
                  mode="multiple"
                  value={answer || []}
                  onChange={setAnswer}
                  placeholder="请选择（可多选）"
                  style={{ width: '100%' }}
                  options={answerOptions}
                />
              ) : (
                /* 单选/下拉/判断题: 单值选择，value 为选项文本 */
                <Select
                  value={answer || undefined}
                  onChange={setAnswer}
                  placeholder="请选择"
                  style={{ width: '100%' }}
                  allowClear
                  options={answerOptions}
                />
              )
            ) : (
              /* 填空/文本题: 自由输入正确答案 */
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
