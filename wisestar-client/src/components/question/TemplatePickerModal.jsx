/**
 * TemplatePickerModal.jsx - 从系统题目库选择题目加入问卷
 *
 * 功能:
 *   1. 分页加载系统已录入的题目（/api/template/list）
 *   2. 支持按名称搜索、按题型筛选
 *   3. 勾选多道题目，确认后回调转换好的问卷问题节点
 *
 * Props:
 *   open: boolean         - 弹窗是否可见
 *   onCancel: () => void  - 关闭回调
 *   onAdd: (questions) => void - 确认回调，questions 为转换后的问卷问题节点数组
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
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const pageSize = 10;

  const [keyword, setKeyword] = useState('');
  const [filterType, setFilterType] = useState(undefined);
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [selectedRows, setSelectedRows] = useState([]);

  // 打开时重置搜索状态
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
  const fetchData = useCallback(async (p = 1) => {
    setLoading(true);
    try {
      const params = { current: p, pageSize };
      if (keyword.trim()) params.name = keyword.trim();
      if (filterType) params.questionType = filterType;
      const res = await listTemplate(params);
      setData(res.data?.list || []);
      setTotal(res.data?.total || 0);
    } catch {
      message.error('加载题目失败');
    } finally {
      setLoading(false);
    }
  }, [keyword, filterType]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (open) fetchData(page);
  }, [open, page, keyword, filterType, fetchData]);

  // ---- 确认加入问卷 ----
  const handleAdd = () => {
    if (selectedRows.length === 0) {
      message.warning('请先勾选要添加的题目');
      return;
    }
    const questions = selectedRows.map((row) => templateToQuestion(row));
    onAdd(questions);
    setSelectedRowKeys([]);
    setSelectedRows([]);
    onCancel();
  };

  // ---- 表格列 ----
  const columns = [
    {
      title: '题目', dataIndex: 'name', ellipsis: true,
      render: (text, record) => {
        const attr = record.template?.attribute || {};
        const hasAnswer = !!attr.examCorrectAnswer;
        const hasAnalysis = !!attr.examAnalysis;
        const hasImages = (attr.examImages || []).length > 0;
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
      render: (t) => <Tag>{TYPE_LABELS[t] || t}</Tag>,
    },
    {
      title: '所属题库', dataIndex: 'repoName', width: 120,
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
