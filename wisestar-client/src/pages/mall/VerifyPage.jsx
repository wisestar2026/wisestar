/**
 * VerifyPage.jsx - 商品核销页（老师端）
 *
 * 功能:
 *   1. 核销码输入框：填入学员出示的 6 位核销码 → 订单完成（学币正式扣除）
 *   2. 核销申请列表：展示学员购买信息（学号/姓名/商品/消耗学币/核销码/状态/时间）
 *      支持按状态、学员姓名或核销码筛选；待核销订单可直接点击「核销」
 *
 * URL: /mall/verify（受 AuthGuard 保护，管理端）
 * 数据流:
 *   listMallOrders({ status, keyword }) → 列表；verifyMallOrder({ verifyCode }) → 核销
 */

import { useEffect, useState } from 'react';
import {
  Table, Space, Button, Input, Select, Tag, Typography, message,
} from 'antd';
import { CheckCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import { listMallOrders, verifyMallOrder } from '../../api/mall';
import { usePermission } from '../../utils/usePermission';

const { Title } = Typography;

export default function VerifyPage() {
  const { can } = usePermission();
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [verifyCode, setVerifyCode] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [status, setStatus] = useState(undefined);
  const [keyword, setKeyword] = useState('');

  const loadList = () => {
    setLoading(true);
    listMallOrders({ status, keyword: keyword || undefined })
      .then((res) => setList(res?.data || []))
      .catch(() => setList([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status]);

  // ---- 凭核销码核销 ----
  const handleVerify = () => {
    const code = verifyCode.trim();
    if (!/^\d{6}$/.test(code)) {
      message.warning('请输入 6 位数字核销码');
      return;
    }
    setSubmitting(true);
    verifyMallOrder({ verifyCode: code })
      .then(() => {
        message.success('核销成功，订单已完成');
        setVerifyCode('');
        loadList();
      })
      .catch(() => {})
      .finally(() => setSubmitting(false));
  };

  // ---- 列表内直接核销 ----
  const handleVerifyRow = (record) => {
    verifyMallOrder({ id: record.id })
      .then(() => {
        message.success('核销成功，订单已完成');
        loadList();
      })
      .catch(() => {});
  };

  const columns = [
    {
      title: '学员', width: 170,
      render: (_, r) => (
        <span>{r.studentNo} <b>{r.studentName}</b></span>
      ),
    },
    { title: '兑换商品', dataIndex: 'goodsName', width: 180 },
    {
      title: '消耗学币', dataIndex: 'coins', width: 100,
      render: (v) => <span style={{ color: '#ff8a3d', fontWeight: 700 }}>-{v}</span>,
    },
    {
      title: '核销码', dataIndex: 'verifyCode', width: 110,
      render: (v) => <b style={{ letterSpacing: 2, color: '#b7791f' }}>{v}</b>,
    },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (v) => (v === 1 ? <Tag color="green">已核销</Tag> : <Tag color="orange">待核销</Tag>),
    },
    { title: '申请时间', dataIndex: 'createAt', width: 170 },
    { title: '核销时间', dataIndex: 'verifyAt', width: 170, render: (v) => v || '-' },
    {
      title: '操作', key: 'action', width: 110,
      render: (_, record) => (
        record.status === 0 && can('mall:update') ? (
          <Button type="link" size="small" icon={<CheckCircleOutlined />} onClick={() => handleVerifyRow(record)}>
            核销
          </Button>
        ) : '-'
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>商品核销</Title>
      </div>

      {/* ---- 核销码录入 ---- */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          placeholder="输入学员核销码（6 位数字）"
          style={{ width: 240 }}
          maxLength={6}
          value={verifyCode}
          onChange={(e) => setVerifyCode(e.target.value.replace(/\D/g, ''))}
          onPressEnter={handleVerify}
          prefix={<CheckCircleOutlined />}
        />
        <Button type="primary" loading={submitting} onClick={handleVerify}>确认核销</Button>
      </Space>

      {/* ---- 筛选 ---- */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          placeholder="状态" allowClear style={{ width: 140 }} value={status}
          onChange={setStatus}
          options={[{ value: 0, label: '待核销' }, { value: 1, label: '已核销' }]}
        />
        <Input
          placeholder="学员姓名 / 核销码" allowClear style={{ width: 200 }} value={keyword}
          onChange={(e) => setKeyword(e.target.value)} onPressEnter={loadList}
        />
        <Button onClick={loadList}>搜索</Button>
        <Button icon={<ReloadOutlined />} onClick={loadList}>刷新</Button>
      </Space>

      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 条` }}
        locale={{ emptyText: '暂无核销申请' }}
      />
    </div>
  );
}
