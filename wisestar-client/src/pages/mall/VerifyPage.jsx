/**
 * VerifyPage.jsx - 商品核销页（空白占位页）
 * URL: /mall/verify（待开发）
 */
import { Empty, Button } from 'antd';
import { QrcodeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
export default function VerifyPage() {
  const navigate = useNavigate();
  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 40 }}>
      <Empty
        image={<QrcodeOutlined style={{ fontSize: 64 }} />}
        description={
          <div>
            <div style={{ fontSize: 18, marginBottom: 8 }}>商品核销</div>
            <div style={{ color: '#999', marginBottom: 16 }}>功能开发中...</div>
            <Button type="primary" onClick={() => navigate('/mall/goods')} style={{ marginTop: 16 }}>
              前往商品管理
            </Button>
          </div>
        }
      />
    </div>
  );
}
