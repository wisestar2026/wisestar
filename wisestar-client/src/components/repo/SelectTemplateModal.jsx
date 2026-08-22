/**
 * SelectTemplateModal.jsx - 练习批量选择题目弹窗
 *
 * 功能:
 *   从题目管理（全局题目库）中批量勾选题目，绑定到当前练习。
 *   题目信息统一来源于题目管理板块，本弹窗只做"选择 + 绑定"，不提供创建/编辑题目入口。
 *
 * 数据流:
 *   打开: listTemplate({current:1, pageSize:500}) → GET /api/template/list（全量题目）
 *   过滤: 前端排除已绑定当前练习的题目（record.repoId === repoId → 禁用勾选）
 *   确认: bindTemplate({repoId, ids}) → POST /api/repo/bind → onSuccess() 刷新练习题目列表
 *
 * 被谁引用: RepoDetailPage（练习详情页「批量选择题目」按钮）
 *
 * URL: 无独立路由，Modal 形式挂载在 RepoDetailPage 内
 */

import { useState, useEffect, useMemo } from 'react';
import { Modal, Table, Input, Select, Tag, Button, message, Typography, Space } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { listTemplate } from '../../api/template';
import { bindTemplate } from '../../api/repo';

const { Text } = Typography;

// 题型标签映射（与 QuestionListPage 一致）
const TYPE_LABELS = {
  Radio: '单选题', Checkbox: '多选题', Select: '下拉题',
  FillBlank: '填空题', Text: '多行文本', Score: '评分题',
  Remark: '备注说明', Judge: '判断题',
};

export default function SelectTemplateModal({ open, repoId, onCancel, onSuccess }) {
  // ---- 状态 ----
  const [allTemplates, setAllTemplates] = useState([]);   // 全量题目（不含已绑定本练习）
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState('');             // 名称搜索
  const [qType, setQType] = useState(undefined);          // 题型筛选
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [confirmLoading, setConfirmLoading] = useState(false);

  // ---- 加载全量题目（题目管理全局库） ----
  const fetchAll = async () => {
    setLoading(true);
    try {
      const res = await listTemplate({ current: 1, pageSize: 500 });
      const list = res.data?.list || [];
      // 排除已绑定当前练习的题目（前端过滤；数据量可控时一次性加载更利于勾选跨页）
      const others = list.filter((t) => t.repoId !== repoId);
      setAllTemplates(others);
      // 关闭已有选中（题目归属可能已变化）
      setSelectedRowKeys((prev) => prev.filter((id) => others.some((t) => t.id === id)));
    } catch {
      message.error('加载题目失败');
    } finally {
      setLoading(false);
    }
  };

  // 打开时重新加载
  useEffect(() => {
    if (open) {
      setKeyword('');
      setQType(undefined);
      setSelectedRowKeys([]);
      fetchAll();
    }
  }, [open]); // eslint-disable-line

  // ---- 前端筛选（搜索 + 题型） ----
  const filtered = useMemo(() => {
    let list = allTemplates;
    if (keyword.trim()) {
      const kw = keyword.trim().toLowerCase();
      list = list.filter((t) => (t.name || '').toLowerCase().includes(kw));
    }
    if (qType) {
      list = list.filter((t) => t.questionType === qType);
    }
    return list;
  }, [allTemplates, keyword, qType]);

  // ---- 确认绑定 ----
  const handleConfirm = async () => {
    if (!selectedRowKeys.length) { message.warning('请先勾选要加入练习的题目'); return; }
    setConfirmLoading(true);
    try {
      await bindTemplate({ repoId, ids: selectedRowKeys });
      message.success(`已加入 ${selectedRowKeys.length} 道题目`);
      onSuccess();
    } catch {
      message.error('绑定失败');
    } finally {
      setConfirmLoading(false);
    }
  };

  // ---- 表格列 ----
  const columns = [
    {
      title: '题目', dataIndex: 'name', ellipsis: true,
      render: (text, r) => {
        const attr = r.template?.attribute || {};
        return (
          <Space size={4}>
            <span>{text}</span>
            {attr.examCorrectAnswer && <Tag color="green" style={{ fontSize: 10, lineHeight: '16px' }}>答案</Tag>}
            {attr.examAnalysis && <Tag color="orange" style={{ fontSize: 10, lineHeight: '16px' }}>解析</Tag>}
          </Space>
        );
      },
    },
    {
      title: '题型', dataIndex: 'questionType', width: 100,
      render: (t) => <Tag>{TYPE_LABELS[t] || t}</Tag>,
    },
    {
      title: '分值', width: 70, align: 'center',
      render: (_, r) => r.template?.attribute?.examScore || '-',
    },
    {
      title: '所属练习', dataIndex: 'repoId', width: 110,
      render: (rid) => (rid ? <Tag color="blue">{rid === repoId ? '本练习' : '其他'}</Tag> : <Text type="secondary">未绑定</Text>),
    },
    {
      title: '标签', dataIndex: 'tag', width: 130,
      render: (tags) => (!tags?.length ? '-' : tags.slice(0, 2).map((t) => <Tag key={t} color="blue">{t}</Tag>)),
    },
  ];

  return (
    <Modal
      title="批量选择题目"
      open={open}
      onCancel={onCancel}
      onOk={handleConfirm}
      confirmLoading={confirmLoading}
      okText={`加入练习（${selectedRowKeys.length}）`}
      cancelText="取消"
      width={820}
      destroyOnHidden
    >
      {/* ---- 说明 ---- */}
      <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
        从题目管理中勾选已有题目加入本练习。题目内容统一在「题目管理」中维护，此处仅选择与绑定。
      </Text>

      {/* ---- 筛选栏 ---- */}
      <Space style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
        <Space>
          <Input
            allowClear
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="搜索题目名称"
            prefix={<SearchOutlined />}
            style={{ width: 220 }}
          />
          <Select
            allowClear
            value={qType}
            onChange={setQType}
            placeholder="题型"
            style={{ width: 130 }}
            options={Object.entries(TYPE_LABELS).map(([v, l]) => ({ label: l, value: v }))}
          />
          <Button icon={<ReloadOutlined />} onClick={fetchAll}>刷新</Button>
        </Space>
        <Text type="secondary">可选 {filtered.length} 题（已绑定本练习的题目不在列表中）</Text>
      </Space>

      {/* ---- 题目表格 ---- */}
      <Table
        rowKey="id"
        size="small"
        loading={loading}
        dataSource={filtered}
        columns={columns}
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        pagination={{ pageSize: 10, showTotal: (t) => `共 ${t} 题` }}
        scroll={{ y: 380 }}
      />

      {/* ---- 空状态提示 ---- */}
      {!loading && filtered.length === 0 && (
        <div style={{ textAlign: 'center', padding: '24px 0', color: '#999' }}>
          题目管理中没有可选题目，请先到「题目管理」创建题目后再回来选择。
        </div>
      )}
    </Modal>
  );
}
