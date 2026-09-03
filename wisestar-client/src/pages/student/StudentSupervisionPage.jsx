/**
 * StudentSupervisionPage.jsx - 学员督学页（教师端查看学员学习状态）
 *
 * 功能:
 *   1. 显示在线学员列表（最近 5 分钟活跃）
 *   2. 显示学员当前学习位置（章节/小节/知识点）
 *   3. 显示学员当前做题状态（题目/答案/解析）
 *   4. 离线学员不显示
 *
 * URL: /student/supervision
 */

import { useEffect, useState } from 'react';
import { Table, Tag, Card, Space, Badge, Alert } from 'antd';
import { UserOutlined, BookOutlined, CheckCircleOutlined, ClockCircleOutlined } from '@ant-design/icons';

const API_BASE = '/api/student/supervision';

export default function StudentSupervisionPage() {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(false);

  // 加载在线学员列表
  const loadOnlineStudents = () => {
    setLoading(true);
    fetch(`${API_BASE}/online-students`)
      .then((res) => res.json())
      .then((res) => {
        if (res.code === 200) {
          setStudents(res.data || []);
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadOnlineStudents();
    // 每 30 秒刷新一次
    const timer = setInterval(loadOnlineStudents, 30000);
    return () => clearInterval(timer);
  }, []);

  // 状态标签
  const statusMap = {
    learning: { color: 'blue', icon: <BookOutlined />, text: '学习中' },
    exercising: { color: 'green', icon: <CheckCircleOutlined />, text: '做题中' },
    offline: { color: 'default', icon: <ClockCircleOutlined />, text: '已离线' },
  };

  // 表格列
  const columns = [
    {
      title: '学员',
      dataIndex: 'studentName',
      width: 150,
      render: (name, record) => (
        <Space>
          <UserOutlined />
          <div>
            <div>{name}</div>
            <div style={{ fontSize: 12, color: '#999' }}>{record.studentNo}</div>
          </div>
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status) => {
        const config = statusMap[status] || statusMap.offline;
        return (
          <Badge color={config.color} text={config.text} />
        );
      },
    },
    {
      title: '当前学习位置',
      dataIndex: 'currentLocation',
      width: 200,
      render: (_, record) => {
        if (record.chapterName || record.sectionName || record.knowledgePointName) {
          return (
            <div>
              {record.chapterName && <div>📖 {record.chapterName}</div>}
              {record.sectionName && <div>📖 {record.sectionName}</div>}
              {record.knowledgePointName && <div>💡 {record.knowledgePointName}</div>}
            </div>
          );
        }
        return record.currentLocation || '-';
      },
    },
    {
      title: '做题状态',
      key: 'exercise',
      width: 300,
      render: (_, record) => {
        if (record.status !== 'exercising' || !record.questionContent) {
          return <span style={{ color: '#999' }}>-</span>;
        }
        return (
          <Card size="small" style={{ margin: 0 }}>
            <div style={{ marginBottom: 8 }}>
              <strong>题目：</strong>{record.questionContent}
            </div>
            {record.studentAnswer && (
              <div style={{ marginBottom: 8, color: '#1890ff' }}>
                <strong>学员答案：</strong>{record.studentAnswer}
              </div>
            )}
            {record.correctAnswer && (
              <div style={{ marginBottom: 8, color: '#52c41a' }}>
                <strong>正确答案：</strong>{record.correctAnswer}
              </div>
            )}
            {record.answerAnalysis && (
              <div style={{ color: '#faad14' }}>
                <strong>解析：</strong>{record.answerAnalysis}
              </div>
            )}
          </Card>
        );
      },
    },
    {
      title: '最后活跃时间',
      dataIndex: 'lastActiveTime',
      width: 180,
    },
  ];

  return (
    <div style={{ maxWidth: 1400, margin: '0 auto', padding: 20 }}>
      <div style={{ marginBottom: 16 }}>
        <Alert
          message="学员督学"
          description="显示最近 5 分钟内活跃的学员学习状态，每 30 秒自动刷新"
          type="info"
          showIcon
        />
      </div>

      <Table
        rowKey="studentId"
        loading={loading}
        columns={columns}
        dataSource={students}
        pagination={false}
        locale={{ emptyText: '当前无在线学员' }}
      />
    </div>
  );
}
