/**
 * HandRaisePage.jsx - 举手提醒（教师端 / 学管师 / 教研）
 *
 * 功能:
 *   1. 「求助讲解」标签：学员在题目页举手求讲解，点开提醒查看该学员当时的多选作答
 *      （选中项高亮、标准答案标绿、解析对照），并可标记已处理
 *   2. 「题目报错」标签：题干有误 / 答案有误的待修任务，点开核对后可跳转题目管理修改
 *
 * URL: /student/hand-raise（AuthGuard handraise:list）
 * 被谁引用: App.jsx 路由表；MainLayout 学员管理 → 举手提醒（带待处理角标）
 *
 * 数据流:
 *   listHandRaises({ raiseType, status, keyword, current, pageSize }) → GET /api/hand-raise/list
 *   handleHandRaise({ id, status, handleNote }) → POST /api/hand-raise/handle
 */

import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Tag, Button, Space, Typography, Input, Select, Modal, Tabs, message, Empty,
} from 'antd';
import { ReloadOutlined, EditOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { listHandRaises, handleHandRaise } from '../../api/handRaise';
import useUserStore from '../../stores/useUserStore';

const { Title, Text, Paragraph } = Typography;

const TYPE_LABELS = {
  Radio: '单选',
  Checkbox: '多选',
  Multiple: '多选',
  Judge: '判断',
  FillBlank: '填空',
  MultipleBlank: '多项填空',
  Text: '简答',
};

const RAISE_TYPE_META = {
  explain: { label: '求助讲解', color: 'orange' },
  question_error: { label: '题干有误', color: 'volcano' },
  answer_error: { label: '答案有误', color: 'magenta' },
};

const STATUS_META = {
  0: { label: '待处理', color: 'processing' },
  2: { label: '已处理', color: 'success' },
  3: { label: '已忽略', color: 'default' },
};

const formatTime = (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-');

/** 解析选项 / 选中项 / 正确项 */
function parseOptions(record) {
  let options = [];
  try {
    options = JSON.parse(record?.optionsJson || '[]');
  } catch {
    options = [];
  }
  const studentIds = String(record?.studentOptionIds || '').split(',').filter(Boolean);
  const correctIds = String(record?.correctOptionIds || '').split(',').filter(Boolean);
  return { options, studentIds, correctIds };
}

export default function HandRaisePage() {
  const navigate = useNavigate();
  const authorityList = useUserStore((s) => s.user?.authorityList) || [];
  const canEditQuestion = authorityList.includes('template:list');

  const [activeTab, setActiveTab] = useState('explain');
  const [status, setStatus] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const [detail, setDetail] = useState(null);
  const [handleNote, setHandleNote] = useState('');
  const [handling, setHandling] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    listHandRaises({
      raiseType: activeTab,
      status: status === -1 ? undefined : status,
      keyword: keyword.trim() || undefined,
      current: page,
      pageSize,
    })
      .then((res) => {
        setList(res?.data?.list || []);
        setTotal(res?.data?.total || 0);
      })
      .catch(() => {
        setList([]);
        setTotal(0);
        message.error('举手提醒加载失败');
      })
      .finally(() => setLoading(false));
  }, [activeTab, status, keyword, page, pageSize]);

  useEffect(() => {
    load();
  }, [load]);

  const switchTab = (key) => {
    setActiveTab(key);
    setPage(1);
  };

  const submitHandle = (targetStatus) => {
    if (!detail || handling) return;
    setHandling(true);
    handleHandRaise({ id: detail.id, status: targetStatus, handleNote: handleNote.trim() || undefined })
      .then(() => {
        message.success(targetStatus === 2 ? '已标记处理完成' : '已忽略该反馈');
        setDetail(null);
        setHandleNote('');
        load();
      })
      .catch(() => message.error('操作失败，请重试'))
      .finally(() => setHandling(false));
  };

  const columns = [
    {
      title: '学员',
      key: 'student',
      width: 140,
      render: (_, r) => (
        <div>
          <div style={{ fontWeight: 500 }}>{r.studentName || '-'}</div>
          <Text type="secondary" style={{ fontSize: 12 }}>{r.studentNo || ''}</Text>
        </div>
      ),
    },
    {
      title: '类型',
      dataIndex: 'raiseType',
      width: 110,
      render: (v) => {
        const meta = RAISE_TYPE_META[v] || { label: v, color: 'default' };
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
    {
      title: '题目',
      key: 'question',
      render: (_, r) => (
        <div>
          <Space size={6} wrap>
            <Tag color="geekblue">{TYPE_LABELS[r.questionType] || r.questionType || '题目'}</Tag>
            {r.subject && <Tag>{r.subject}</Tag>}
            {r.knowledgePoint && <Tag color="cyan">{r.knowledgePoint}</Tag>}
          </Space>
          <Paragraph style={{ margin: '6px 0 0' }} ellipsis={{ rows: 2 }}>
            {r.questionContent || '（题目已删除）'}
          </Paragraph>
          {r.remark && (
            <Text type="secondary" style={{ fontSize: 12 }}>学员备注：{r.remark}</Text>
          )}
        </div>
      ),
    },
    {
      title: '学员作答',
      key: 'answer',
      width: 180,
      render: (_, r) => (
        <Text style={{ fontSize: 13 }}>{r.studentAnswer || '未作答'}</Text>
      ),
    },
    {
      title: '提交时间',
      dataIndex: 'createAt',
      width: 165,
      render: formatTime,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (v) => {
        const meta = STATUS_META[v] || { label: v, color: 'default' };
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, r) => (
        <Button size="small" type="link" onClick={() => { setDetail(r); setHandleNote(r.handleNote || ''); }}>
          查看
        </Button>
      ),
    },
  ];

  const { options, studentIds, correctIds } = parseOptions(detail);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <Title level={4} style={{ margin: 0 }}>
          举手提醒
          <Text type="secondary" style={{ fontSize: 13, marginLeft: 12 }}>
            学员做题时举手，这里实时汇总；求助讲解点开即看学员当时的多选作答
          </Text>
        </Title>
        <Button icon={<ReloadOutlined />} onClick={load} loading={loading}>刷新</Button>
      </div>

      <Tabs
        activeKey={activeTab}
        onChange={switchTab}
        items={[
          { key: 'explain', label: '求助讲解' },
          { key: 'error', label: '题目报错' },
        ]}
      />

      <Space style={{ marginBottom: 12 }} wrap>
        <Select
          value={status}
          onChange={(v) => { setStatus(v); setPage(1); }}
          style={{ width: 130 }}
          options={[
            { value: 0, label: '待处理' },
            { value: 2, label: '已处理' },
            { value: 3, label: '已忽略' },
            { value: -1, label: '全部状态' },
          ]}
        />
        <Input.Search
          allowClear
          placeholder="搜索学员姓名 / 学号 / 题干"
          style={{ width: 260 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={() => { setPage(1); load(); }}
        />
      </Space>

      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={list}
        locale={{ emptyText: <Empty description={activeTab === 'explain' ? '暂无学员举手求助' : '暂无题目报错'} /> }}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          onChange: (p, ps) => { setPage(p); setPageSize(ps); },
        }}
        onRow={(record) => ({
          onClick: () => { setDetail(record); setHandleNote(record.handleNote || ''); },
          style: { cursor: 'pointer' },
        })}
      />

      <Modal
        title="反馈详情"
        open={!!detail}
        onCancel={() => setDetail(null)}
        width={720}
        destroyOnHidden
        footer={
          <Space>
            <Button onClick={() => setDetail(null)}>关闭</Button>
            {detail?.status === 0 && (
              <>
                <Button danger loading={handling} onClick={() => submitHandle(3)}>忽略</Button>
                <Button type="primary" loading={handling} onClick={() => submitHandle(2)}>标记已处理</Button>
              </>
            )}
          </Space>
        }
      >
        {detail && (
          <div>
            <Space size={8} wrap style={{ marginBottom: 12 }}>
              <Tag color={(RAISE_TYPE_META[detail.raiseType] || {}).color}>
                {(RAISE_TYPE_META[detail.raiseType] || {}).label || detail.raiseType}
              </Tag>
              <Tag color={(STATUS_META[detail.status] || {}).color}>
                {(STATUS_META[detail.status] || {}).label || detail.status}
              </Tag>
              {detail.studentName && <Text strong>{detail.studentName}</Text>}
              {detail.studentNo && <Text type="secondary">{detail.studentNo}</Text>}
              <Text type="secondary">{formatTime(detail.createAt)}</Text>
            </Space>

            <div style={{ marginBottom: 10 }}>
              <Space size={6} wrap>
                <Tag color="geekblue">{TYPE_LABELS[detail.questionType] || detail.questionType || '题目'}</Tag>
                {detail.subject && <Tag>{detail.subject}</Tag>}
                {detail.chapter && <Tag color="blue">{detail.chapter}</Tag>}
                {detail.knowledgePoint && <Tag color="cyan">{detail.knowledgePoint}</Tag>}
                {detail.difficulty && <Tag>{detail.difficulty}</Tag>}
              </Space>
            </div>

            <Paragraph strong style={{ whiteSpace: 'pre-wrap', marginBottom: 12 }}>
              {detail.questionContent || '（题目已删除）'}
            </Paragraph>

            {options.length > 0 && (
              <div style={{ marginBottom: 12 }}>
                <Text type="secondary" style={{ fontSize: 12 }}>学员作答（多选界面）</Text>
                <div style={{ marginTop: 6 }}>
                  {options.map((opt, i) => {
                    const isStudent = studentIds.includes(opt.id);
                    const isCorrect = correctIds.includes(opt.id);
                    const border = isStudent && !isCorrect ? '#ff7875' : (isCorrect ? '#52c41a' : '#e0e0e0');
                    const background = isStudent && !isCorrect ? '#fff1f0' : (isCorrect ? '#f6ffed' : '#fff');
                    return (
                      <div
                        key={opt.id || i}
                        style={{
                          display: 'flex', gap: 8, alignItems: 'flex-start',
                          padding: '6px 10px', marginBottom: 6,
                          border: `1px solid ${border}`, borderRadius: 6, background,
                        }}
                      >
                        <Text strong>{String.fromCharCode(65 + i)}.</Text>
                        <div style={{ flex: 1 }}>{opt.title}</div>
                        {isStudent && <Tag color={isCorrect ? 'success' : 'error'}>{isCorrect ? '学员选对' : '学员所选'}</Tag>}
                        {!isStudent && isCorrect && <Tag color="success">正确答案</Tag>}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 12 }}>
              <div style={{ padding: '6px 12px', background: '#fff1f0', border: '1px solid #ffa39e', borderRadius: 6 }}>
                <Text strong>学员作答：</Text>
                <Text>{detail.studentAnswer || '未作答'}</Text>
              </div>
              <div style={{ padding: '6px 12px', background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 6 }}>
                <Text strong style={{ color: '#389e0d' }}>标准答案：</Text>
                <Text style={{ color: '#389e0d' }}>{detail.correctAnswer || '（未配置）'}</Text>
              </div>
            </div>

            {detail.answerAnalysis && (
              <div style={{ padding: '8px 12px', background: '#fffbe6', border: '1px solid #ffe58f', borderRadius: 6, marginBottom: 12 }}>
                <Text strong style={{ color: '#d48806' }}>解析：</Text>
                <Text style={{ color: '#874d00', whiteSpace: 'pre-wrap' }}>{detail.answerAnalysis}</Text>
              </div>
            )}

            {detail.remark && (
              <div style={{ marginBottom: 12 }}>
                <Text type="secondary">学员补充说明：</Text>
                <Text>{detail.remark}</Text>
              </div>
            )}

            {detail.status === 0 && (
              <Input.TextArea
                rows={2}
                maxLength={200}
                value={handleNote}
                onChange={(e) => setHandleNote(e.target.value)}
                placeholder="处理备注（选填）"
              />
            )}

            {detail.status !== 0 && detail.handlerName && (
              <Text type="secondary">
                由 {detail.handlerName} 于 {formatTime(detail.handleAt)} 处理
                {detail.handleNote ? `：${detail.handleNote}` : ''}
              </Text>
            )}

            {(detail.raiseType === 'question_error' || detail.raiseType === 'answer_error') && canEditQuestion && detail.questionId && (
              <div style={{ marginTop: 12 }}>
                <Button
                  icon={<EditOutlined />}
                  onClick={() => navigate(`${detail.subject === '英语' ? '/english/questions' : '/questions'}?questionId=${encodeURIComponent(detail.questionId)}`)}
                >
                  去题目管理修改
                </Button>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
