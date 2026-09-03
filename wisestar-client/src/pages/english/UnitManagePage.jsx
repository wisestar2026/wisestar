/**
 * UnitManagePage.jsx - 英语单元管理页（空白占位页）
 *
 * 功能:
 *   1. 设置英语年级
 *   2. 设置单元
 *
 * URL: /english/unit（待开发）
 */

import { Empty, Button } from 'antd';
import { BookOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

export default function UnitManagePage() {
  const navigate = useNavigate();

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 40 }}>
      <Empty
        image={<BookOutlined style={{ fontSize: 64 }} />}
        description={
          <div>
            <div style={{ fontSize: 18, marginBottom: 8 }}>单元管理</div>
            <div style={{ color: '#999', marginBottom: 16 }}>
              功能开发中...
            </div>
            <div style={{ color: '#666', fontSize: 14 }}>
              功能说明：
              <ul style={{ textAlign: 'left', marginTop: 8 }}>
                <li>设置英语年级（一年级 ~ 六年级）</li>
                <li>设置单元（Unit 1, Unit 2...）</li>
                <li>管理年级与单元的对应关系</li>
              </ul>
            </div>
            <Button type="primary" onClick={() => navigate('/english/word-manager')} style={{ marginTop: 16 }}>
              前往单词管理
            </Button>
          </div>
        }
      />
    </div>
  );
}
