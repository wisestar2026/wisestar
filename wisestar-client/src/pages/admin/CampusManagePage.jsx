/**
 * CampusManagePage.jsx - 校区管理页（空白占位页）
 * URL: /admin/campus（待开发）
 */
import { Empty, Button } from 'antd';
import { HomeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
export default function CampusManagePage() {
  const navigate = useNavigate();
  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 40 }}>
      <Empty
        image={<HomeOutlined style={{ fontSize: 64 }} />}
        description={
          <div>
            <div style={{ fontSize: 18, marginBottom: 8 }}>校区管理</div>
            <div style={{ color: '#999', marginBottom: 16 }}>功能开发中...</div>
            <Button type="primary" onClick={() => navigate('/admin/roles')} style={{ marginTop: 16 }}>
              前往角色权限
            </Button>
          </div>
        }
      />
    </div>
  );
}
