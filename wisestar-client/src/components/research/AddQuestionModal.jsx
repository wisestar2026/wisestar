/**
 * AddQuestionModal.jsx - 教研平台「从题库添加题目」弹窗
 *
 * 先选「练习题库」→ 加载该题库题目列表（分页）→ 勾选要绑定的题目（多选）→ 确认保存绑定。
 * 题目本体录入/新建仍走「题目管理」页，本弹窗只做绑定。
 */
import { useEffect, useRef, useState } from 'react';
import { Modal, Select, Table, Input, message } from 'antd';
import { listRepo } from '../../api/repo';
import { listTemplate } from '../../api/template';

const DIFF_LABELS = { 1: '容易', 2: '中等', 3: '困难' };
const TYPE_LABELS = {
  Radio: '单选', Checkbox: '多选', Judge: '判断', Fill: '填空',
  ShortAnswer: '简答', MultipleBlank: '多空填空', Select: '下拉', Essay: '作文', '': '',
};

/** 拦截器返回 { code, data } 整包 → 取业务 data */
function unwrap(res) {
  return res && res.code !== undefined ? res.data : res;
}

export default function AddQuestionModal({ open, onCancel, onAdd }) {
  const [repos, setRepos] = useState([]);
  const [repoId, setRepoId] = useState(undefined);
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [saving, setSaving] = useState(false);
  const pageRef = useRef(1);

  // 打开时加载题库下拉
  useEffect(() => {
    if (!open) return;
    listRepo({ current: 1, pageSize: 200 })
      .then((res) => {
        const d = unwrap(res);
        setRepos((Array.isArray(d) ? d : d?.list) || []);
      })
      .catch((e) => message.error('加载题库失败：' + (e?.message || e)));
  }, [open]);

  // 题库/关键字/页码变化 → 刷新题目列表
  useEffect(() => {
    if (!open || !repoId) return;
    fetchRows(1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, repoId]);

  const fetchRows = (page) => {
    pageRef.current = page;
    setLoading(true);
    listTemplate({ repoId, current: page, pageSize: 8, name: keyword || undefined })
      .then((res) => {
        const d = unwrap(res) || {};
        const list = (Array.isArray(d) ? d : d.list) || [];
        setRows(list);
        setTotal(Array.isArray(d) ? list.length : d.total || 0);
      })
      .catch((e) => message.error('加载题目失败：' + (e?.message || e)))
      .finally(() => setLoading(false));
  };

  const handleSearch = () => fetchRows(1);

  const handleOk = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先勾选要绑定的题目');
      return;
    }
    setSaving(true);
    onAdd(selectedRowKeys);
    setSaving(false);
  };

  const columns = [
    { title: '题名', dataIndex: 'name', ellipsis: true, render: (t) => t || '（未命名题目）' },
    { title: '题型', dataIndex: 'questionType', width: 90, render: (t) => TYPE_LABELS[t] || t || '-' },
    {
      title: '难度', dataIndex: 'difficulty', width: 90,
      render: (d) => DIFF_LABELS[d] || '-',
    },
  ];

  return (
    <Modal
      title="从题库添加题目"
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      confirmLoading={saving}
      width={760}
      destroyOnClose
    >
      <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
        <Select
          style={{ width: 280 }}
          placeholder="选择练习题库"
          value={repoId}
          onChange={(v) => {
            setRepoId(v);
            setSelectedRowKeys([]);
            setRows([]);
          }}
          options={repos.map((r) => ({ value: r.id, label: r.name }))}
          showSearch
          optionFilterProp="label"
        />
        <Input.Search
          style={{ flex: 1 }}
          placeholder="按题名搜索"
          allowClear
          onSearch={handleSearch}
          onChange={(e) => setKeyword(e.target.value)}
        />
      </div>
      {!repoId ? (
        <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
          请先选择练习题库
        </div>
      ) : (
        <Table
          rowKey="id"
          size="small"
          loading={loading}
          columns={columns}
          dataSource={rows}
          pagination={{
            current: pageRef.current,
            pageSize: 8,
            total,
            showSizeChanger: false,
            onChange: (p) => fetchRows(p),
          }}
          rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys),
          }}
        />
      )}
    </Modal>
  );
}
