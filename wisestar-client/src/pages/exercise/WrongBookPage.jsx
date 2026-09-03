/**
 * WrongBookPage.jsx - 错题管理页（空白占位页）
 *
 * URL: /exercise/wrong-book（待开发）
 */

import { Empty, Button } from 'antd';
import { BookOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

export default function WrongBookPage() {
  const navigate = useNavigate();

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 40 }}>
      <Empty
        image={<BookOutlined style={{ fontSize: 64 }} />}
        description={
          <div>
            <div style={{ fontSize: 18, marginBottom: 8 }}>错题管理</div>
            <div style={{ color: '#999', marginBottom: 16 }}>功能开发中...</div>
            <Button type="primary" onClick={() => navigate('/exercise/list')} style={{ marginTop: 16 }}>
              前往习题列表
            </Button>
          </div>
        }
      />
    </div>
  );
}
