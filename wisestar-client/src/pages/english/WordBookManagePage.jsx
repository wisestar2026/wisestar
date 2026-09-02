/**
 * WordBookManagePage.jsx - 英语单词本管理页
 *
 * 功能:
 *   1. 单词本展示（已学单词 + 熟练度 + 下次复习时间）
 *   2. 按熟练度筛选（未学习/生疏/熟悉/熟练/精通）
 *   3. 按复习时间筛选（待复习/已掌握）
 *   4. 点击进入单词学习
 *
 * URL: /english/word-book（受 AuthGuard 保护）
 * 被谁引用：App.jsx 路由表
 *
 * 数据流:
 *   GET /api/english/word/word-book → 单词本列表（按版本/年级/单元筛选）
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Tag, Space, Select, Input, Button, Progress, Card, Statistic } from 'antd';
import { BookOutlined, ClockCircleOutlined, CheckCircleOutlined } from '@ant-design/icons';

const API_BASE = '/api/english/word'; // Student API

const FAMILIARITY_MAP = {
  0: { text: '未学习', color: 'default' },
  1: { text: '生疏', color: 'red' },
  2: { text: '熟悉', color: 'orange' },
  3: { text: '熟练', color: 'blue' },
  4: { text: '精通', color: 'green' },
};

export default function WordBookManagePage() {
  const navigate = useNavigate();
  const [words, setWords] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // 筛选条件
  const [version, setVersion] = useState('');
  const [grade, setGrade] = useState('');
  const [unit, setUnit] = useState('');
  const [familiarity, setFamiliarity] = useState('');

  // 统计数据
  const [stats, setStats] = useState({
    total: 0,
    needReview: 0,
    mastered: 0,
  });

  // 加载单词列表
  const loadList = () => {
    setLoading(true);
    const params = new URLSearchParams({
      current,
      pageSize,
      ...(version && { version }),
      ...(grade && { grade }),
      ...(unit && { unit }),
    });

    fetch(`${API_BASE}/word-book?${params}`)
      .then((res) => res.json())
      .then((res) => {
        if (res.code === 200) {
          setWords(res.data?.list || []);
          setTotal(res.data?.total || 0);
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadList();
  }, [current, pageSize, version, grade, unit]);

  // 加载统计数据
  useEffect(() => {
    fetch(`${API_BASE}/study?limit=1000`)
      .then((res) => res.json())
      .then((res) => {
        if (res.code === 200) {
          const list = res.data || [];
          setStats({
            total: list.length,
            needReview: list.filter((w) => w.familiarity < 4).length,
            mastered: list.filter((w) => w.familiarity === 4).length,
          });
        }
      })
      .catch(() => {});
  }, []);

  // 表格列
  const columns = [
    {
      title: '单词',
      dataIndex: 'spell',
      width: 120,
      render: (spell) => <span style={{ fontWeight: 'bold', color: '#1890ff' }}>{spell}</span>,
    },
    { title: '音标', dataIndex: 'phonetic', width: 120 },
    { title: '释义', dataIndex: 'meaning', ellipsis: true },
    {
      title: '熟练度',
      dataIndex: 'familiarity',
      width: 100,
      render: (fam) => {
        const info = FAMILIARITY_MAP[fam || 0];
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Button
          type="link"
          icon={<BookOutlined />}
          onClick={() => navigate(`/english/word?wordId=${record.id}`)}
        >
          学习
        </Button>
      ),
    },
  ];

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 20 }}>
      {/* 顶部统计卡片 */}
      <Space size={16} style={{ marginBottom: 20 }}>
        <Card style={{ width: 200 }}>
          <Statistic title="总单词数" value={stats.total} prefix={<BookOutlined />} />
        </Card>
        <Card style={{ width: 200 }}>
          <Statistic
            title="待复习"
            value={stats.needReview}
            prefix={<ClockCircleOutlined />}
            valueStyle={{ color: '#faad14' }}
          />
        </Card>
        <Card style={{ width: 200 }}>
          <Statistic
            title="已掌握"
            value={stats.mastered}
            prefix={<CheckCircleOutlined />}
            valueStyle={{ color: '#52c41a' }}
          />
        </Card>
      </Space>

      {/* 筛选栏 */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          placeholder="教材版本"
          allowClear
          style={{ width: 120 }}
          value={version}
          onChange={setVersion}
          options={[
            { value: '人教版', label: '人教版' },
            { value: '苏教版', label: '苏教版' },
            { value: '北师大版', label: '北师大版' },
            { value: '外研版', label: '外研版' },
          ]}
        />
        <Select
          placeholder="年级"
          allowClear
          style={{ width: 100 }}
          value={grade}
          onChange={setGrade}
          options={[
            { value: '一年级', label: '一年级' },
            { value: '二年级', label: '二年级' },
            { value: '三年级', label: '三年级' },
            { value: '四年级', label: '四年级' },
            { value: '五年级', label: '五年级' },
            { value: '六年级', label: '六年级' },
          ]}
        />
        <Input
          placeholder="单元"
          allowClear
          style={{ width: 100 }}
          value={unit}
          onChange={(e) => setUnit(e.target.value)}
        />
        <Select
          placeholder="熟练度"
          allowClear
          style={{ width: 100 }}
          value={familiarity}
          onChange={setFamiliarity}
          options={Object.entries(FAMILIARITY_MAP).map(([value, info]) => ({
            value,
            label: info.text,
          }))}
        />
        <Button onClick={loadList}>查询</Button>
        <Button onClick={() => navigate('/english/word')}>开始学习</Button>
      </Space>

      {/* 单词列表 */}
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={words}
        pagination={{
          current,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (c, s) => {
            setCurrent(c);
            setPageSize(s);
          },
        }}
      />
    </div>
  );
}
