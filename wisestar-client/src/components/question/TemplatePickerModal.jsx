/**
 * TemplatePickerModal.jsx - 从系统题目库选择题目加入问卷
 *
 * 功能:
 *   1. 分页加载系统已录入的题目（GET /api/template/list）
 *   2. 支持按名称搜索、按题型筛选
 *   3. 勾选多道题目，确认后回调转换好的问卷问题节点
 *
 * 被谁引用: ProjectEditPage（问卷编辑器左栏"添加问题 → 从系统题目选择"）
 *
 * Props:
 *   open: boolean         - 弹窗是否可见
 *   onCancel: () => void  - 关闭回调
 *   onAdd: (questions) => void - 确认回调，questions 为转换后的问卷问题节点数组
 *
 * 核心数据流:
 *   打开弹窗 → fetchData → GET /api/template/list（题目分页列表）
 *   → 勾选行 → handleAdd → templateToQuestion(题目) 逐条转换
 *   → onAdd(questions) → ProjectEditPage.addTemplateQuestions 追加到问卷 JSON
 *
 * 转换逻辑（重点）:
 *   templateToQuestion 会把题目的 attribute（答案/解析/分值/知识点快照）和
 *   children（选项）复制到问卷节点中，并重新生成 ID 避免与问卷现有节点冲突。
 *   详见 src/utils/surveyHelpers.js
 */

import { useState, useEffect, useCallback } from 'react';
import {
  Modal, Table, Input, Select, Space, Button, Tag, Typography, message, Image, Tooltip,
} from 'antd';
import { SearchOutlined, PictureOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { listTemplate } from '../../api/template';
import { templateToQuestion } from '../../utils/surveyHelpers';
import { QUESTION_TYPES } from '../../utils/surveyHelpers';

const { Text } = Typography;

// 完整题型映射（含判断题）
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Select: '下拉题',
  FillBlank: '填空题', Text: '多行文本', Score: '评分题',
  Remark: '备注说明', Judge: '判断题',
};

const TYPE_OPTIONS = [
  ...QUESTION_TYPES,
  { label: '判断题', value: 'Judge' },
];

export default function TemplatePickerModal({ open, onCancel, onAdd }) {
  // ---- 列表状态 ----
  const [loading, setLoading] = useState(false);   // 请求题目列表的加载标记
  const [data, setData] = useState([]);            // 当前页题目数据（TemplateView 数组）
  const [total, setTotal] = useState(0);           // 符合条件的题目总数
  const [page, setPage] = useState(1);             // 当前页码
  const pageSize = 10;                             // 每页 10 条（固定，不提供切换）

  // ---- 搜索筛选状态 ----
  const [keyword, setKeyword] = useState('');      // 题目名称搜索词
  const [filterType, setFilterType] = useState(undefined); // 题型过滤

  // ---- 勾选状态 ----
  const [selectedRowKeys, setSelectedRowKeys] = useState([]); // 勾选的题目 ID（控制按钮状态）
  const [selectedRows, setSelectedRows] = useState([]);       // 勾选的完整题目行（供转换）

  // 打开时重置搜索状态（保证每次打开都是干净起点）
  useEffect(() => {
    if (open) {
      setKeyword('');
      setFilterType(undefined);
      setSelectedRowKeys([]);
      setSelectedRows([]);
      setPage(1);
    }
  }, [open]);

  // ---- 加载题目列表 ----
  // useCallback 缓存: 依赖 keyword/filterType，变化时重新创建函数
  // 数据流: 本组件 → listTemplate(params) → GET /api/template/list → 返回题目分页
  const fetchData = useCallback(async (p = 1) => {
    setLoading(true);
    try {
      const params = { current: p, pageSize };
      if (keyword.trim()) params.name = keyword.trim();          // 名称模糊搜索
      if (filterType) params.questionType = filterType;          // 按题型过滤
      const res = await listTemplate(params);
      setData(res.data?.list || []);
      setTotal(res.data?.total || 0);
    } catch {
      message.error('加载题目失败');
    } finally {
      setLoading(false);
    }
  }, [keyword, filterType]); // eslint-disable-line react-hooks/exhaustive-deps

  // open 打开 / 翻页 / 搜索条件变化时重新拉取列表
  useEffect(() => {
    if (open) fetchData(page);
  }, [open, page, keyword, filterType, fetchData]);

  // ---- 确认加入问卷 ----
  // 核心转换点: 将选中的题目行（TemplateView）逐条调用 templateToQuestion
  // 转换为问卷 children 中的问题节点，再通过 onAdd 回传给 ProjectEditPage
  const handleAdd = () => {
    if (selectedRows.length === 0) {
      message.warning('请先勾选要添加的题目');
      return;
    }
    // templateToQuestion: 保留答案/解析/分值/知识点快照，选项与节点 ID 重新生成
    const questions = selectedRows.map((row) => templateToQuestion(row));
    onAdd(questions);
    // 清空勾选并关闭弹窗
    setSelectedRowKeys([]);
    setSelectedRows([]);
    onCancel();
  };

  // ---- 表格列 ----
  const columns = [
    {
      title: '题目', dataIndex: 'name', ellipsis: true,
      // 题目名 + 标记标签：是否有答案/解析/图片（从 template.attribute 读取）
      render: (text, record) => {
        const attr = record.template?.attribute || {};
        const hasAnswer = !!attr.examCorrectAnswer;   // 含正确答案标记
        const hasAnalysis = !!attr.examAnalysis;      // 含答案解析标记
        const hasImages = (attr.examImages || []).length > 0; // 含配图标记（悬停预览首图）
        return (
          <Space size={4} wrap>
            <span>{text}</span>
            {hasAnswer && <Tag color="green" style={{ fontSize: 10, lineHeight: '16px' }}>答案</Tag>}
            {hasAnalysis && <Tag color="orange" style={{ fontSize: 10, lineHeight: '16px' }}>解析</Tag>}
            {hasImages && (
              <Tooltip title={<Image src={attr.examImages[0]} width={160} />} placement="right">
                <Tag color="purple" style={{ fontSize: 10, lineHeight: '16px' }}>
                  <PictureOutlined /> 图
                </Tag>
              </Tooltip>
            )}
          </Space>
        );
      },
    },
    {
      title: '题型', dataIndex: 'questionType', width: 90,
      // TYPE_LABELS 比 QUESTION_TYPES 多一个 Judge（判断题），查不到时原样显示
      render: (t) => <Tag>{TYPE_LABELS[t] || t}</Tag>,
    },
    {
      title: '所属练习', dataIndex: 'repoName', width: 120,
      // 题目未绑定练习时显示"未分配"
      render: (name) => name
        ? <Tag color="blue">{name}</Tag>
        : <Text type="secondary">未分配</Text>,
    },
  ];

  return (
    <Modal
      title="从系统题目选择"
      open={open}
      onCancel={onCancel}
      width={720}
      footer={[
        <Button key="cancel" onClick={onCancel}>取消</Button>,
        <Button
          key="add"
          type="primary"
          disabled={selectedRowKeys.length === 0}
          onClick={handleAdd}
        >
          加入问卷（{selectedRowKeys.length}）
        </Button>,
      ]}
    >
      {/* ---- 搜索栏 ---- */}
      <Space style={{ marginBottom: 12 }} wrap>
        <Input
          placeholder="搜索题目名称"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onPressEnter={() => { setPage(1); fetchData(1); }}
          style={{ width: 240 }}
          prefix={<SearchOutlined />}
          allowClear
        />
        <Select
          placeholder="全部题型"
          value={filterType}
          onChange={(v) => setFilterType(v)}
          options={TYPE_OPTIONS}
          style={{ width: 140 }}
          allowClear
        />
        <Button type="primary" onClick={() => { setPage(1); fetchData(1); }}>查询</Button>
      </Space>

      {/* ---- 题目列表 ---- */}
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        rowSelection={{
          selectedRowKeys,
          onChange: (keys, rows) => {
            setSelectedRowKeys(keys);
            setSelectedRows(rows);
          },
        }}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: false,
          showTotal: (t) => `共 ${t} 道题目`,
          onChange: (p) => setPage(p),
        }}
      />
    </Modal>
  );
}
