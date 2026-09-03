/**
 * SentenceManagePage.jsx - 句式管理页（空白占位页）
 *
 * URL: /english/sentence（待开发）
 */

import { Empty, Button } from 'antd';
import { FieldBinaryOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

export default function SentenceManagePage() {
  const navigate = useNavigate();

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 40 }}>
      <Empty
        image={<FieldBinaryOutlined style={{ fontSize: 64 }} />}
        description={
          <div>
            <div style={{ fontSize: 18, marginBottom: 8 }}>句式管理</div>
            <div style={{ color: '#999', marginBottom: 16 }}>功能开发中...</div>
            <Button type="primary" onClick={() => navigate('/english/word')} style={{ marginTop: 16 }}>
              前往单词管理
            </Button>
          </div>
        }
      />
    </div>
  );
}
