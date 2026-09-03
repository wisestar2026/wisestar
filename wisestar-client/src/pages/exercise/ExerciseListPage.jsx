/**
 * ExerciseListPage.jsx - 习题列表页（空白占位页）
 *
 * URL: /exercise/list（待开发）
 */

import { Empty, Button } from 'antd';
import { BookOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

export default function ExerciseListPage() {
  const navigate = useNavigate();

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 40 }}>
      <Empty
        image={<BookOutlined style={{ fontSize: 64 }} />}
        description={
          <div>
            <div style={{ fontSize: 18, marginBottom: 8 }}>习题列表</div>
            <div style={{ color: '#999', marginBottom: 16 }}>功能开发中...</div>
            <Button type="primary" onClick={() => navigate('/questions')} style={{ marginTop: 16 }}>
              前往题目管理
            </Button>
          </div>
        }
      />
    </div>
  );
}
