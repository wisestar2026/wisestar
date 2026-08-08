/**
 * ProjectEditPage.jsx - 问卷编辑器页面
 *
 * 功能:
 *   1. 左侧：问题列表（拖拽排序区域 + 新建/删除问题按钮）
 *   2. 中间：问题编辑区（标题、类型、选项配置、必填开关）
 *   3. 右侧：问卷设置区（标题、描述、提交按钮文案、后缀文案）
 *
 * 被谁引用: App.jsx 路由表（/projects/:id/edit）；从 ProjectListPage 进入
 *
 * 数据流:
 *   加载: loadProject → getProject(id) → GET /api/project → 解析 survey JSON（字符串或对象）
 *   编辑: 所有修改通过 cloneSurvey 深拷贝 → setSurvey，保证不可变更新
 *   保存: handleSave → updateProject({id, name, survey}) → POST /api/project/update
 *   选题: 左栏"添加问题 → 从系统题目选择" → TemplatePickerModal
 *         → templateToQuestion 转换 → addTemplateQuestions 追加到问卷
 *
 * 重点逻辑: 答案编辑区（详见 updateAnswer 与 Checkbox 分支注释）
 *   1. 多选题（Checkbox）: <Select mode="multiple"> 值为数组，
 *      updateAnswer 收到数组后 join('\n') 转成 \n 分隔字符串存 attribute.examCorrectAnswer
 *      （与 QuestionEditModal 的存取约定一致，后端判分按此解析）
 *   2. 回填: 读取 attribute.examCorrectAnswer 后 split('\n') 拆回数组渲染多选
 *   3. 判断题/单选题: 单值选择，直接存取字符串
 *
 * URL: /projects/:id/edit
 */

import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card, Row, Col, Button, Input, Select, Switch, Form,
  Space, Popconfirm, Empty, message, Spin, Typography, Divider, Dropdown,
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, ArrowLeftOutlined,
  SaveOutlined, UnorderedListOutlined, QuestionCircleOutlined, DatabaseOutlined,
} from '@ant-design/icons';
import { getProject, updateProject } from '../../../api/project';
import {
  QUESTION_TYPES, TYPES_WITH_OPTIONS,
  createEmptySurvey, createQuestion, createOption, cloneSurvey,
} from '../../../utils/surveyHelpers';
import TemplatePickerModal from '../../../components/question/TemplatePickerModal';

const { TextArea } = Input;
const { Title, Text } = Typography;

// 编辑器题型列表（在标准题型基础上补充判断题）
const EDITOR_TYPES = [
  ...QUESTION_TYPES,
  { label: '判断题', value: 'Judge' },
];

export default function ProjectEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form] = Form.useForm(); // 问卷设置的 Ant Design 表单

  // ---- 状态 ----
  const [loading, setLoading] = useState(true);  // 页面加载中
  const [saving, setSaving] = useState(false);   // 保存中
  const [projectName, setProjectName] = useState(''); // 项目名称（列表显示用）
  const [survey, setSurvey] = useState(null);    // 问卷 JSON（完整结构）
  const [selectedQid, setSelectedQid] = useState(null); // 当前选中的问题 ID
  const [pickerOpen, setPickerOpen] = useState(false); // 系统题目选择弹窗

  // ---- 加载问卷数据 ----
  // 数据流: 本页 → getProject(id) → GET /api/project?id=xxx → 返回项目对象
  const loadProject = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getProject(id);
      const data = res.data; // 响应拦截器已返回 response.data，data 即为项目对象
      setProjectName(data.name || '未命名问卷');

      // 如果问卷已有 survey 数据就用它，否则创建空白骨架
      if (data.survey) {
        // 后端可能返回 JSON 字符串或对象，统一解析
        // 为什么这么写: 后端对 survey 字段的序列化方式可能因创建途径不同而异，
        // 这里做了兼容（字符串 → JSON.parse，对象直接使用）
        const parsed = typeof data.survey === 'string'
          ? JSON.parse(data.survey)
          : data.survey;
        setSurvey(parsed);
        // 默认选中第一个问题（编辑器默认聚焦，方便直接编辑）
        if (parsed.children && parsed.children.length > 0) {
          setSelectedQid(parsed.children[0].id);
        }
      } else {
        // 新问卷无 survey 数据 → 生成空白骨架（children 为空数组）
        const empty = createEmptySurvey(data.name);
        setSurvey(empty);
      }
    } catch {
      message.error('加载问卷失败，请检查网络或权限');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadProject();
  }, [loadProject]);

  // ---- 保存问卷 ----
  // 数据流: 本页 → updateProject({id, name, survey}) → POST /api/project/update
  // 保存内容 = 现有 survey + 右侧表单里的问卷级设置（标题/描述/按钮文案/后缀）
  const handleSave = async () => {
    if (!survey) return;

    // 从表单读取问卷级别设置（右侧"问卷设置"卡片）
    const formValues = form.getFieldsValue();
    // 用表单值覆盖 survey 的顶层字段和 attribute（未填写的字段保留原值）
    const toSave = {
      ...survey,
      title: formValues.title || survey.title,
      description: formValues.description || survey.description,
      attribute: {
        ...survey.attribute,
        suffix: formValues.suffix || survey.attribute?.suffix,
        submitButton: formValues.submitButton || survey.attribute?.submitButton,
      },
    };

    setSaving(true);
    try {
      await updateProject({
        id,
        name: projectName,
        survey: toSave,
      });
      message.success('问卷已保存');
    } catch {
      message.error('保存失败');
    } finally {
      setSaving(false);
    }
  };

  // ---- 问题操作 ----
  // 更新某个问题的某个字段
  // 为什么 clone: 直接改 prev.children 里的对象会破坏 React 状态不可变性，
  // 先深拷贝再修改，保证 setSurvey 收到全新引用触发重渲染
  const updateQuestion = (qid, field, value) => {
    setSurvey((prev) => {
      const next = cloneSurvey(prev);
      const q = next.children.find((c) => c.id === qid);
      if (q) q[field] = value;
      return next;
    });
  };

  // 添加新问题
  // 提前调用 createQuestion 拿到带 ID 的新节点（ID 在同一次渲染内稳定），
  // 追加后再选中它，用户可直接开始编辑
  const addQuestion = (type) => {
    const newQ = createQuestion(type); // 提前创建以获取稳定的 ID
    setSurvey((prev) => {
      const next = cloneSurvey(prev);
      next.children.push(newQ);
      return next;
    });
    // 选中新问题
    setSelectedQid(newQ.id);
  };

  // 从系统题目库添加问题（TemplatePickerModal 已转换为问卷问题节点）
  // 数据流: TemplatePickerModal → templateToQuestion 转换 → onAdd(questions)
  // → 本函数 → 追加到 survey.children（含知识点/答案/解析快照）
  const addTemplateQuestions = (questions) => {
    if (!questions || questions.length === 0) return;
    setSurvey((prev) => {
      const next = cloneSurvey(prev);
      next.children.push(...questions);
      return next;
    });
    // 选中最后添加的一道题
    setSelectedQid(questions[questions.length - 1].id);
  };

  // 删除问题
  const deleteQuestion = (qid) => {
    // 记录要删除问题在原数组中的位置，用于确定下一个选中
    const oldIdx = survey?.children?.findIndex((c) => c.id === qid) ?? -1;

    setSurvey((prev) => {
      const next = cloneSurvey(prev);
      next.children = next.children.filter((c) => c.id !== qid);
      return next;
    });

    // 如果删除的是当前选中的问题，自动选中相邻的
    // （用旧 survey 计算前后节点，因为此时 state 里的还是旧数据）
    if (selectedQid === qid && oldIdx >= 0) {
      setSelectedQid(() => {
        // survey 是旧值，children 还包含被删的问题
        const children = survey?.children || [];
        const before = children[oldIdx - 1];
        const after = children[oldIdx + 1];
        return before?.id || after?.id || null;
      });
    }
  };

  // ---- 选项操作 ----
  // 添加选项（clone → 找到问题 → children.push(createOption())）
  const addOption = (qid) => {
    setSurvey((prev) => {
      const next = cloneSurvey(prev);
      const q = next.children.find((c) => c.id === qid);
      if (q) q.children.push(createOption());
      return next;
    });
  };

  // 删除选项
  const deleteOption = (qid, optId) => {
    setSurvey((prev) => {
      const next = cloneSurvey(prev);
      const q = next.children.find((c) => c.id === qid);
      if (q) q.children = q.children.filter((c) => c.id !== optId);
      return next;
    });
  };

  // 更新选项文本
  const updateOption = (qid, optId, value) => {
    setSurvey((prev) => {
      const next = cloneSurvey(prev);
      const q = next.children.find((c) => c.id === qid);
      if (q) {
        const opt = q.children.find((c) => c.id === optId);
        if (opt) opt.title = value;
      }
      return next;
    });
  };

  // 更新当前选中问题的正确答案（存于 attribute.examCorrectAnswer）
  // 重点: 多选题传入数组，落库为 \n 分隔字符串（与 QuestionEditModal 存取约定一致）
  //   数组 → join('\n') → "选项A\n选项B" 存入 attribute.examCorrectAnswer
  //   回填时由 JSX 中的 split('\n').filter(Boolean) 还原为数组
  // 为什么这么写: 后端 Attribute.examCorrectAnswer 是单字符串字段，
  //   多选答案只能约定分隔符拼接；\n 不会被选项文本包含，是最安全的分隔符
  const updateAnswer = (value) => {
    setSurvey((prev) => {
      const next = cloneSurvey(prev);
      const q = next.children.find((c) => c.id === selectedQ.id);
      if (q) {
        const stored = Array.isArray(value)
          ? (value.length ? value.join('\n') : undefined)
          : (value || undefined);
        q.attribute = { ...q.attribute, examCorrectAnswer: stored };
      }
      return next;
    });
  };

  // ---- 计算 ----
  const questions = survey?.children || [];
  const selectedQ = questions.find((q) => q.id === selectedQid);
  const selectedTypeLabel = EDITOR_TYPES.find((t) => t.value === selectedQ?.type)?.label;

  // ---- 渲染 ----
  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin size="large" tip="加载问卷中..." />
      </div>
    );
  }

  if (!survey) {
    return <Empty description="问卷数据加载失败" />;
  }

  return (
    <div style={{ padding: '0 0 24px' }}>
      {/* ---- 顶部工具栏 ---- */}
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Space>
            {/* 返回按钮：回到问卷列表 */}
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/projects')}>
              返回列表
            </Button>
            <Title level={4} style={{ margin: 0 }}>
              编辑：{projectName}
            </Title>
          </Space>
        </Col>
        <Col>
          <Space>
            {/* 保存按钮 */}
            <Button
              type="primary"
              icon={<SaveOutlined />}
              loading={saving}
              onClick={handleSave}
            >
              保存问卷
            </Button>
          </Space>
        </Col>
      </Row>

      {/* ---- 三栏布局 ---- */}
      <Row gutter={16}>
        {/* ---- 左栏：问题列表 ---- */}
        <Col span={5}>
          <Card
            title="问题列表"
            size="small"
            extra={
              <Dropdown
                menu={{
                  items: [
                    {
                      type: 'group',
                      label: '新建题目',
                      children: EDITOR_TYPES.map((t) => ({ key: t.value, label: t.label })),
                    },
                    { type: 'divider' },
                    {
                      key: '__pick__',
                      label: '从系统题目选择',
                      icon: <DatabaseOutlined />,
                    },
                  ],
                  onClick: ({ key }) => {
                    if (key === '__pick__') {
                      setPickerOpen(true);
                    } else {
                      addQuestion(key);
                    }
                  },
                }}
              >
                <Button size="small" type="primary" icon={<PlusOutlined />}>
                  添加问题
                </Button>
              </Dropdown>
            }
          >
            {questions.length === 0 ? (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无问题，请点击 + 添加"
              />
            ) : (
              <div style={{ maxHeight: 'calc(100vh - 220px)', overflow: 'auto' }}>
                {questions.map((q, idx) => (
                  <div
                    key={q.id}
                    onClick={() => setSelectedQid(q.id)}
                    style={{
                      padding: '8px 12px',
                      marginBottom: 4,
                      borderRadius: 6,
                      cursor: 'pointer',
                      background: selectedQid === q.id ? '#e6f4ff' : '#fafafa',
                      border: selectedQid === q.id ? '1px solid #1677ff' : '1px solid #f0f0f0',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <Space size={4}>
                      <UnorderedListOutlined style={{ color: '#999', fontSize: 12 }} />
                      <Text ellipsis={{ tooltip: q.title || '(未命名)' }} style={{ maxWidth: 120 }}>
                        {q.title || '(未命名)'}
                      </Text>
                    </Space>
                    <Text type="secondary" style={{ fontSize: 11 }}>
                      {EDITOR_TYPES.find((t) => t.value === q.type)?.label}
                    </Text>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </Col>

        {/* ---- 中栏：问题编辑区 ---- */}
        <Col span={12}>
          {selectedQ ? (
            <Card
              title={
                <Space>
                  <QuestionCircleOutlined />
                  <span>问题编辑</span>
                </Space>
              }
              extra={
                <Popconfirm
                  title="确定删除此问题？"
                  onConfirm={() => deleteQuestion(selectedQ.id)}
                  okText="删除"
                  cancelText="取消"
                >
                  <Button size="small" danger icon={<DeleteOutlined />}>
                    删除
                  </Button>
                </Popconfirm>
              }
              size="small"
            >
              <Space orientation="vertical" style={{ width: '100%' }} size="middle">
                {/* 问题类型选择 */}
                <div>
                  <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
                    问题类型
                  </Text>
                  <Select
                    value={selectedQ.type}
                    onChange={(val) => updateQuestion(selectedQ.id, 'type', val)}
                    options={EDITOR_TYPES}
                    style={{ width: 160 }}
                  />
                  <Text type="secondary" style={{ marginLeft: 12, fontSize: 12 }}>
                    当前：{selectedTypeLabel}
                  </Text>
                </div>

                {/* 问题标题 */}
                <div>
                  <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
                    问题标题
                  </Text>
                  <Input
                    value={selectedQ.title}
                    onChange={(e) => updateQuestion(selectedQ.id, 'title', e.target.value)}
                    placeholder={selectedQ.type === 'Remark' ? '备注内容...' : '请输入问题标题'}
                  />
                </div>

                {/* 必填开关 */}
                <div>
                  <Space>
                    <Switch
                      checked={selectedQ.attribute?.required || false}
                      onChange={(checked) => {
                        setSurvey((prev) => {
                          const next = cloneSurvey(prev);
                          const q = next.children.find((c) => c.id === selectedQ.id);
                          if (q) q.attribute = { ...q.attribute, required: checked };
                          return next;
                        });
                      }}
                      size="small"
                    />
                    <Text type="secondary" style={{ fontSize: 12 }}>此题必填</Text>
                  </Space>
                </div>

                {/* 选项编辑区（仅选择题型显示） */}
                {TYPES_WITH_OPTIONS.includes(selectedQ.type) && (
                  <>
                    <Divider style={{ margin: '8px 0' }} />
                    <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 8 }}>
                      选项列表
                    </Text>
                    {selectedQ.children.map((opt, idx) => (
                      <Row key={opt.id} gutter={8} align="middle" style={{ marginBottom: 8 }}>
                        <Col flex="auto">
                          <Input
                            value={opt.title}
                            onChange={(e) => updateOption(selectedQ.id, opt.id, e.target.value)}
                            placeholder={`选项 ${idx + 1}`}
                            addonBefore={<Text type="secondary" style={{ fontSize: 12 }}>{String.fromCharCode(65 + idx)}</Text>}
                          />
                        </Col>
                        <Col>
                          {/* 至少保留 2 个选项才能删除 */}
                          {selectedQ.children.length > 2 && (
                            <Button
                              size="small"
                              danger
                              type="text"
                              icon={<DeleteOutlined />}
                              onClick={() => deleteOption(selectedQ.id, opt.id)}
                            />
                          )}
                        </Col>
                      </Row>
                    ))}
                    {/* 添加选项按钮 */}
                    <Button
                      type="dashed"
                      block
                      size="small"
                      icon={<PlusOutlined />}
                      onClick={() => addOption(selectedQ.id)}
                    >
                      添加选项
                    </Button>
                  </>
                )}

                {/* 答案编辑区（填空题自由输入，判断题/选择题从选项中选择） */}
                {/* 重点: 多选题答案多选，存 \n 分隔字符串
                      - 渲染: 读 attribute.examCorrectAnswer，split('\n') 还原为数组
                      - 保存: updateAnswer 收到数组 → join('\n') 转字符串
                      - 为什么: 后端 Attribute.examCorrectAnswer 是单字符串字段 */}
                {(selectedQ.type === 'FillBlank' || selectedQ.type === 'Judge' || TYPES_WITH_OPTIONS.includes(selectedQ.type)) && (
                  <>
                    <Divider style={{ margin: '8px 0' }} />
                    <div>
                      <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
                        正确答案
                      </Text>
                      {selectedQ.type === 'FillBlank' ? (
                        /* 填空题: 自由输入正确答案（保存为字符串） */
                        <Input
                          value={selectedQ.attribute?.examCorrectAnswer || ''}
                          onChange={(e) => updateAnswer(e.target.value)}
                          placeholder="请输入正确答案"
                          allowClear
                        />
                      ) : selectedQ.type === 'Judge' ? (
                        /* 判断题: 从"正确/错误"二选一 */
                        <Select
                          value={selectedQ.attribute?.examCorrectAnswer || undefined}
                          onChange={updateAnswer}
                          placeholder="请选择正确答案"
                          style={{ width: 200 }}
                          allowClear
                          options={[
                            { label: '正确', value: '正确' },
                            { label: '错误', value: '错误' },
                          ]}
                        />
                      ) : (
                        selectedQ.type === 'Checkbox' ? (
                          /* 多选题: mode="multiple" 多选
                             选项 value 为选项文本（非 ID），保存时 join('\n') 落库 */
                          <Select
                            mode="multiple"
                            value={selectedQ.attribute?.examCorrectAnswer
                              ? String(selectedQ.attribute.examCorrectAnswer).split('\n').filter(Boolean)
                              : []}
                            onChange={updateAnswer}
                            placeholder="请选择正确答案（可多选）"
                            style={{ width: 240 }}
                            options={selectedQ.children
                              .filter((o) => o.title && o.title.trim())
                              .map((o, idx) => ({
                                label: `${String.fromCharCode(65 + idx)}. ${o.title}`,
                                value: o.title,
                              }))}
                          />
                        ) : (
                          /* 单选/下拉: 单值选择 */
                          <Select
                            value={selectedQ.attribute?.examCorrectAnswer || undefined}
                            onChange={updateAnswer}
                            placeholder="请选择正确答案"
                            style={{ width: 240 }}
                            allowClear
                            options={selectedQ.children
                              .filter((o) => o.title && o.title.trim())
                              .map((o, idx) => ({
                                label: `${String.fromCharCode(65 + idx)}. ${o.title}`,
                                value: o.title,
                              }))}
                          />
                        )
                      )}
                    </div>
                  </>
                )}
              </Space>
            </Card>
          ) : (
            <Card size="small">
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={questions.length === 0 ? '请点击左上角 + 添加问题' : '请从左侧列表选择一个问题'}
              />
            </Card>
          )}
        </Col>

        {/* ---- 右栏：问卷设置 ---- */}
        <Col span={7}>
          <Card title="问卷设置" size="small">
            <Form
              form={form}
              layout="vertical"
              size="small"
              initialValues={{
                title: survey.title,
                description: survey.description,
                suffix: survey.attribute?.suffix || '感谢您的参与！',
                submitButton: survey.attribute?.submitButton || '提交',
              }}
            >
              {/* 问卷标题 */}
              <Form.Item name="title" label="问卷标题">
                <Input placeholder="请输入问卷标题" />
              </Form.Item>

              {/* 问卷描述 */}
              <Form.Item name="description" label="问卷说明">
                <TextArea rows={3} placeholder="请输入问卷说明/引导语" />
              </Form.Item>

              {/* 提交按钮文案 */}
              <Form.Item name="submitButton" label="提交按钮文案">
                <Input placeholder="提交" />
              </Form.Item>

              {/* 提交后提示 */}
              <Form.Item name="suffix" label="提交后提示">
                <Input placeholder="感谢您的参与！" />
              </Form.Item>
            </Form>
          </Card>
        </Col>
      </Row>

      {/* ---- 从系统题目选择弹窗 ---- */}
      <TemplatePickerModal
        open={pickerOpen}
        onCancel={() => setPickerOpen(false)}
        onAdd={addTemplateQuestions}
      />
    </div>
  );
}
