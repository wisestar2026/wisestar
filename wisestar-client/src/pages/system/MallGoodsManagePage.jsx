/**
 * MallGoodsManagePage.jsx - 积分商城商品管理页（后台）
 *
 * 功能:
 *   1. 商品列表（图片/名称/描述/所需积分/状态/排序）
 *   2. 新增/编辑商品（名称/描述/图片上传/积分/上架状态/排序）
 *   3. 删除商品
 *
 * URL: /mall/goods（受 AuthGuard 保护，需要 mall:list 权限）
 * 被谁引用: App.jsx 路由；MainLayout 侧边栏「积分商城 → 商品管理」菜单进入
 *
 * 数据流:
 *   listGoods() → 商品列表；createGoods/updateGoods/deleteGoods → CRUD
 *   图片: uploadImage(file) → { previewUrl } → imageUrl 随商品保存
 */

import { useEffect, useState, useCallback } from 'react';
import {
  Table, Space, Button, Input, InputNumber, Switch, Modal, Form, Typography, Popconfirm, message, Image, Tag, Upload,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, UploadOutlined } from '@ant-design/icons';
import { listGoods, createGoods, updateGoods, deleteGoods } from '../../api/mall';
import { uploadImage } from '../../api/upload';
import { usePermission } from '../../utils/usePermission';

const { Title } = Typography;

export default function MallGoodsManagePage() {
  const { can } = usePermission();
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [form] = Form.useForm();

  const loadList = useCallback(() => {
    setLoading(true);
    listGoods()
      .then((res) => setList(res?.data || []))
      .catch(() => setList([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadList();
  }, [loadList]);

  const openModal = (goods = null) => {
    setEditing(goods);
    setModalOpen(true);
    if (goods) {
      form.setFieldsValue({
        name: goods.name,
        description: goods.description,
        imageUrl: goods.imageUrl,
        points: goods.points ?? 0,
        sort: goods.sort ?? 1,
        status: goods.status === 0 ? false : true,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ points: 0, sort: 1, status: true });
    }
  };

  // 图片上传（上传成功后回填 imageUrl）
  const handleUpload = (file) => {
    setUploading(true);
    uploadImage(file)
      .then((res) => {
        form.setFieldsValue({ imageUrl: res?.previewUrl || res?.url || '' });
        message.success('图片上传成功');
      })
      .catch(() => message.error('图片上传失败'))
      .finally(() => setUploading(false));
    return false; // 阻止 antd 自动上传
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      const payload = {
        name: values.name,
        description: values.description,
        imageUrl: values.imageUrl || undefined,
        points: values.points ?? 0,
        sort: values.sort ?? 1,
        status: values.status ? 1 : 0,
      };
      setSaving(true);
      if (editing) {
        updateGoods({ ...payload, id: editing.id })
          .then(() => {
            message.success('商品已更新');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      } else {
        createGoods(payload)
          .then(() => {
            message.success('商品已新增');
            setModalOpen(false);
            loadList();
          })
          .finally(() => setSaving(false));
      }
    });
  };

  const handleDelete = (goods) => {
    deleteGoods({ id: goods.id }).then(() => {
      message.success('商品已删除');
      loadList();
    });
  };

  const columns = [
    {
      title: '商品图片', dataIndex: 'imageUrl', width: 90,
      render: (v) => (v ? <Image src={v} width={56} height={56} style={{ objectFit: 'cover', borderRadius: 8 }} /> : <Tag>无图</Tag>),
    },
    { title: '商品名称', dataIndex: 'name', width: 160, render: (v) => v || '-' },
    { title: '描述', dataIndex: 'description', ellipsis: true, render: (v) => v || '-' },
    { title: '所需积分', dataIndex: 'points', width: 90, render: (v) => <Tag color="gold">⭐ {v ?? 0}</Tag> },
    { title: '排序', dataIndex: 'sort', width: 70 },
    {
      title: '状态', dataIndex: 'status', width: 80,
      render: (v) => (v === 1 ? <Tag color="green">上架</Tag> : <Tag>下架</Tag>),
    },
    {
      title: '操作', key: 'action', width: 140,
      render: (_, record) => (
        <Space>
          {can('mall:update') && (
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(record)}>编辑</Button>
          )}
          {can('mall:delete') && (
            <Popconfirm title="确定删除该商品？" onConfirm={() => handleDelete(record)}>
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>积分商城 · 商品管理</Title>
        {can('mall:create') && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增商品</Button>
        )}
      </div>

      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={false}
        locale={{ emptyText: '暂无商品，点击「新增商品」上架第一个商品' }}
      />

      {/* ---- 新增/编辑商品弹窗 ---- */}
      <Modal
        title={editing ? '编辑商品' : '新增商品'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 5 }} wrapperCol={{ span: 18 }}>
          <Form.Item name="name" label="商品名称" rules={[{ required: true, message: '请输入商品名称' }]}>
            <Input placeholder="如：小鲸笔记本" maxLength={128} />
          </Form.Item>
          <Form.Item name="description" label="商品描述">
            <Input.TextArea placeholder="选填" rows={2} maxLength={512} />
          </Form.Item>
          <Form.Item name="imageUrl" label="商品图片">
            <Space direction="vertical" style={{ width: '100%' }}>
              <Upload
                beforeUpload={handleUpload}
                showUploadList={false}
                accept="image/*"
                disabled={uploading}
              >
                <Button icon={<UploadOutlined />} loading={uploading}>上传图片</Button>
              </Upload>
              {form.getFieldValue('imageUrl') && (
                <Image src={form.getFieldValue('imageUrl')} width={80} height={80} style={{ objectFit: 'cover', borderRadius: 8 }} />
              )}
            </Space>
          </Form.Item>
          <Form.Item name="points" label="所需积分" rules={[{ required: true, message: '请输入所需积分' }]}>
            <InputNumber min={0} style={{ width: '100%' }} placeholder="兑换所需积分" />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber min={1} style={{ width: '100%' }} placeholder="数字越小越靠前" />
          </Form.Item>
          <Form.Item name="status" label="上架" valuePropName="checked">
            <Switch checkedChildren="上架" unCheckedChildren="下架" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
