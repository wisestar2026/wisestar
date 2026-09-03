/**
 * StudentSupervisionPage.jsx - 学员督学页（空白占位页）
 *
 * URL: /student/supervision（待开发）
 */

import { Empty, Button } from 'antd';
import { UsergroupAddOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

export default function StudentSupervisionPage() {
  const navigate = useNavigate();

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 40 }}>
      <Empty
        image={<UsergroupAddOutlined style={{ fontSize: 64 }} />}
        description={
          <div>
            <div style={{ fontSize: 18, marginBottom: 8 }}>学员督学</div>
            <div style={{ color: '#999', marginBottom: 16 }}>功能开发中...</div>
            <Button type="primary" onClick={() => navigate('/students')} style={{ marginTop: 16 }}>
              返回学员列表
            </Button>
          </div>
        }
      />
    </div>
  );
}
