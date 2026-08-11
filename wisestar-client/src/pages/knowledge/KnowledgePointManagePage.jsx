/**
 * KnowledgePointManagePage.jsx - 知识点管理页（知识管理板块）
 *
 * 功能:
 *   1. 顶部学科/章节/小节三级下拉联动筛选（均可选，不选则范围放宽，全不选 = 全量分页）
 *   2. 知识点分页列表 + CRUD（真实 API；删除连带清理题目绑定）
 *   3. 「内容设置」: 编辑知识点讲解要点（t_knowledge_point.content JSON）+ 上传配图
 *      （imageUrl 复用 /api/file/create 上传返回的 previewUrl）
 *   4. 「绑定题目」: 从题目库（t_template）选择题目绑定到知识点（全量替换保存），
 *      题目只能选自题目库，不能在此新增
 *
 * URL: /knowledge/points（受 AuthGuard 保护）
 * 被谁引用: App.jsx 路由表；小节管理页「管理知识点」按钮跳转进入
 *
 * 数据流:
 *   listSubjects/listChapters/listSections 三级联动 → listKnowledgePoints 分页
 *   内容设置 JSON.stringify({points}) + imageUrl 独立字段提交
 *   绑定题目: listKnowledgePointQuestions 回显 → 弹窗勾选 → saveKnowledgePointQuestions 全量保存
 */

import { useEffect, useMemo, useState } from 'react';
import {
  Table, Space, Button, Input, InputNumber, Select, Modal, Form, Tag, Typography, Breadcrumb, Popconfirm, message, Upload,
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, FileTextOutlined,
  LinkOutlined, ArrowLeftOutlined, UploadOutlined, PictureOutlined,
} from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  listSubjects, listChapters, listSections, listKnowledgePoints,
  createKnowledgePoint, updateKnowledgePoint, deleteKnowledgePoint,
  saveKnowledgePointQuestions, listKnowledgePointQuestions,
} from '../../api/knowledge';
import { listTemplate } from '../../api/template';
import { uploadImage } from '../../api/upload';

const { Text } = Typography;

const QUESTION_TYPE_LABEL = {
  Radio: '单选',
  Checkbox: '多选',
  Judge: '判断',
  FillBlank: '填空',
};

export default function KnowledgePointManagePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const urlSubjectId = searchParams.get('subjectId');
  const urlChapterId = searchParams.get('chapterId');
  const urlSectionId = searchParams.get('sectionId');

  // ---- 三级下拉联动 state ----
  const [subjects, setSubjects] = useState([]);
  const [chapters, setChapters] = useState([]);
  const [sections, setSections] = useState([]);
  const [subjectId, setSubjectId] = useState(urlSubjectId || undefined);
  const [chapterId, setChapterId] = useState(urlChapterId || undefined);
  const [sectionId, setSectionId] = useState(urlSectionId || undefined);

  // ---- 知识点分页 ----
  const [kps, setKps] = useState([]);
  const [total, setTotal] = useState(0);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [loading, setLoading] = useState(false);

  // ---- 新增/编辑弹窗 ----
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();

  // ---- 内容设置弹窗 ----
  const [contentOpen, setContentOpen] = useState(false);
  const [contentKp, setContentKp] = useState(null);
  const [contentForm] = Form.useForm();
  const [imageUrl, setImageUrl] = useState('');
  const [uploading, setUploading] = useState(false);

  // ---- 绑定题目弹窗 ----
  const [bindOpen, setBindOpen] = useState(false);
  const [bindKp, setBindKp] = useState(null);
  const [bindKeyword, setBindKeyword] = useState('');
  const [tplList, setTplList] = useState([]);
  const [tplTotal, setTplTotal] = useState(0);
  const [tplCurrent, setTplCurrent] = useState(1);
  const [tplLoading, setTplLoading] = useState(false);
  const [selectedIds, setSelectedIds] = useState([]);
  const [savingBind, setSavingBind] = useState(false);

  // ---- 加载学科（默认选中第一个） ----
  useEffect(() => {
    listSubjects().then((res) => {
      const list = res?.data || [];
      setSubjects(list);
      setSubjectId((prev) => prev || list[0]?.id);
    }).catch(() => { /* request 拦截器已提示 */ });
  }, []);

  // ---- 学科 → 章节 ----
  useEffect(() => {
    if (!subjectId) { setChapters([]); return; }
    listChapters({ subjectId }).then((res) => {
      const list = res?.data || [];
      setChapters(list);
      setChapterId((prev) => (prev && list.some((c) => c.id === prev) ? prev : undefined));
    }).catch(() => setChapters([]));
  }, [subjectId]);

  // ---- 章节 → 小节 ----
  useEffect(() => {
    if (!chapterId) { setSections([]); return; }
    listSections({ chapterId }).then((res) => {
      const list = res?.data || [];
      setSections(list);
      setSectionId((prev) => (prev && list.some((s) => s.id === prev) ? prev : undefined));
    }).catch(() => setSections([]));
  }, [chapterId]);

  // ---- 三级筛选变化 → 重置页码并刷新列表 ----
  useEffect(() => {
    setCurrent(1);
  }, [subjectId, chapterId, sectionId]);

  useEffect(() => {
    if (!subjectId) return;
    setLoading(true);
    const params = { current, pageSize };
    if (sectionId) params.sectionId = sectionId;
    else if (chapterId) params.chapterId = chapterId;
    else if (subjectId) params.subjectId = subjectId;
    listKnowledgePoints(params).then((res) => {
      setKps(res?.data?.list || []);
      setTotal(res?.data?.total || 0);
    }).catch(() => { setKps([]); setTotal(0); }).finally(() => setLoading(false));
  }, [subjectId, chapterId, sectionId, current, pageSize]);

  const subject = useMemo(() => subjects.find((s) => s.id === subjectId), [subjects, subjectId]);
  const chapter = useMemo(() => chapters.find((c) => c.id === chapterId), [chapters, chapterId]);
  const section = useMemo(() => sections.find((s) => s.id === sectionId), [sections, sectionId]);

  // ---- 新增/编辑 ----
  const openModal = (kp = null) => {
    setEditing(kp);
    setModalOpen(true);
    if (kp) {
      form.setFieldsValue({ name: kp.name, sort: kp.sort });
    } else {
      form.resetFields();
      form.setFieldsValue({ sort: kps.length + 1 });
    }
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      if (editing) {
        updateKnowledgePoint({ ...values, id: editing.id, sectionId: editing.sectionId }).then(() => {
          message.success('知识点已更新');
          setModalOpen(false);
          setKps((prev) => prev.map((k) => (k.id === editing.id ? { ...k, ...values } : k)));
        });
      } else {
        if (!sectionId) { message.warning('请先选择小节，知识点必须归属到小节下'); return; }
        createKnowledgePoint({ ...values, sectionId }).then(() => {
          message.success('知识点已新增');
          setModalOpen(false);
          listKnowledgePoints({ current, pageSize, sectionId }).then((res) => {
            setKps(res?.data?.list || []);
            setTotal(res?.data?.total || 0);
          });
        });
      }
    });
  };

  // ---- 删除 ----
  const handleDelete = (kp) => {
    deleteKnowledgePoint({ id: kp.id }).then(() => {
      message.success('知识点已删除');
      setKps((prev) => prev.filter((k) => k.id !== kp.id));
      setTotal((t) => Math.max(0, t - 1));
    });
  };

  // ---- 内容设置（要点 + 配图上传） ----
  const openContent = (kp) => {
    setContentKp(kp);
    setContentOpen(true);
    let content = {};
    try { content = kp.content ? JSON.parse(kp.content) : {}; } catch { content = {}; }
    contentForm.setFieldsValue({ points: content.points?.length ? content.points : [''] });
    setImageUrl(kp.imageUrl || '');
  };

  const handleImageUpload = (file) => {
    setUploading(true);
    uploadImage(file).then((res) => {
      if (res?.previewUrl) {
        setImageUrl(res.previewUrl);
        message.success('图片上传成功');
      } else {
        message.error('图片上传失败');
      }
    }).catch(() => message.error('图片上传失败')).finally(() => setUploading(false));
    return false; // 阻止 antd 默认上传
  };

  const saveContent = () => {
    contentForm.validateFields().then((values) => {
      const payload = JSON.stringify({
        points: (values.points || []).filter((p) => p && p.trim()),
      });
      updateKnowledgePoint({ id: contentKp.id, sectionId: contentKp.sectionId, content: payload, imageUrl }).then(() => {
        message.success('内容设置已保存');
        setContentOpen(false);
        setKps((prev) => prev.map((k) => (k.id === contentKp.id ? { ...k, content: payload, imageUrl } : k)));
      });
    });
  };

  // ---- 绑定题目（题目库选择） ----
  const openBind = (kp) => {
    setBindKp(kp);
    setBindOpen(true);
    setBindKeyword('');
    setTplCurrent(1);
    setSelectedIds([]);
    listKnowledgePointQuestions(kp.id).then((res) => {
      setSelectedIds((res?.data || []).map((q) => q.id));
    }).catch(() => { /* 已提示 */ });
    fetchTemplates(1, '');
  };

  const fetchTemplates = (page, keyword) => {
    setTplLoading(true);
    listTemplate({ current: page, pageSize: 8, name: keyword || undefined })
      .then((res) => {
        setTplList(res?.data?.list || []);
        setTplTotal(res?.data?.total || 0);
      }).catch(() => { setTplList([]); setTplTotal(0); }).finally(() => setTplLoading(false));
  };

  const onBindKeywordSearch = () => {
    setTplCurrent(1);
    fetchTemplates(1, bindKeyword);
  };

  const saveBind = () => {
    setSavingBind(true);
    saveKnowledgePointQuestions({ knowledgePointId: bindKp.id, questionIds: selectedIds }).then(() => {
      message.success('题目绑定已保存');
      setBindOpen(false);
      setKps((prev) => prev.map((k) => (k.id === bindKp.id ? { ...k, questionCount: selectedIds.length } : k)));
    }).finally(() => setSavingBind(false));
  };

  // ---- 表格列 ----
  const columns = [
    {
      title: '知识点名称', dataIndex: 'name', width: 180,
      render: (n) => <Text strong>{n}</Text>,
    },
    {
      title: '所属小节', width: 200,
      render: (_, k) => (
        <Text type="secondary">{k.subjectName} / {k.chapterName} / {k.sectionName}</Text>
      ),
    },
    {
      title: '排序', dataIndex: 'sort', width: 60, align: 'center',
    },
    {
      title: '内容设置', width: 130, align: 'center',
      render: (_, k) => {
        let content = {};
        try { content = k.content ? JSON.parse(k.content) : {}; } catch { content = {}; }
        const pointCount = content.points?.length || 0;
        return (
          <Space size={4}>
            {pointCount > 0 ? <Tag color="green">{pointCount} 条要点</Tag> : <Tag>未设置</Tag>}
            {k.imageUrl ? <Tag color="purple" icon={<PictureOutlined />}>有图</Tag> : null}
          </Space>
        );
      },
    },
    {
      title: '绑定题目', dataIndex: 'questionCount', width: 90, align: 'center',
      render: (count) => (count > 0 ? <Tag color="blue">{count} 题</Tag> : <Tag>未绑定</Tag>),
    },
    {
      title: '操作', key: 'action', width: 320,
      render: (_, k) => (
        <Space wrap>
          <Button type="primary" size="small" icon={<FileTextOutlined />} onClick={() => openContent(k)}>内容设置</Button>
          <Button size="small" icon={<LinkOutlined />} onClick={() => openBind(k)}>绑定题目</Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => openModal(k)}>编辑</Button>
          <Popconfirm
            title={`删除知识点「${k.name}」？`}
            description="其绑定的题目将一并解除，删除后不可恢复。"
            onConfirm={() => handleDelete(k)}
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* ---- 页面标题 ---- */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Space align="center">
          <Button size="small" icon={<ArrowLeftOutlined />} onClick={() => navigate(`/knowledge/sections?subjectId=${subjectId}&chapterId=${chapterId || ''}`)} />
          <Breadcrumb
            items={[
              { title: <Text strong>知识管理</Text> },
              { title: <Text strong>{subject ? `${subject.icon} ${subject.name}` : '学科'}</Text> },
              { title: <Text strong>{chapter?.name || '章节'}</Text> },
              { title: <Text strong>{section?.name || '小节'}</Text> },
            ]}
          />
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增知识点</Button>
      </div>

      {/* ---- 三级联动下拉 ---- */}
      <Space style={{ marginBottom: 16 }}>
        <Select
          style={{ width: 160 }}
          value={subjectId}
          onChange={setSubjectId}
          placeholder="全部学科"
          options={subjects.map((s) => ({ value: s.id, label: `${s.icon} ${s.name}` }))}
        />
        <Select
          style={{ width: 200 }}
          value={chapterId}
          onChange={setChapterId}
          placeholder="全部章节"
          allowClear
          options={chapters.map((c) => ({ value: c.id, label: `${c.icon} ${c.name}` }))}
        />
        <Select
          style={{ width: 200 }}
          value={sectionId}
          onChange={setSectionId}
          placeholder="全部小节"
          allowClear
          options={sections.map((s) => ({ value: s.id, label: s.name }))}
        />
        <Text type="secondary">下拉可逐级筛选，全选「全部」可查看所有知识点</Text>
      </Space>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={kps}
        loading={loading}
        pagination={{
          current,
          pageSize,
          total,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (c, s) => { setCurrent(c); setPageSize(s); },
        }}
        locale={{ emptyText: '当前筛选条件下暂无知识点，点击右上角「新增知识点」创建' }}
      />

      {/* 新增/编辑知识点弹窗 */}
      <Modal title={editing ? '编辑知识点' : '新增知识点'} open={modalOpen} onOk={handleSave} onCancel={() => setModalOpen(false)} okText="保存" cancelText="取消" destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="知识点名称" rules={[{ required: true, message: '请输入知识点名称' }]}>
            <Input placeholder="如：进位加法" maxLength={30} />
          </Form.Item>
          <Form.Item name="sort" label="排序（数字越小越靠前）">
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 知识点内容设置弹窗（要点 + 配图） */}
      <Modal
        title={`内容设置 - ${contentKp?.name || ''}`}
        open={contentOpen}
        onOk={saveContent}
        onCancel={() => setContentOpen(false)}
        okText="保存"
        cancelText="取消"
        width={680}
        destroyOnClose
      >
        <Form form={contentForm} layout="vertical">
          <Form.Item label="讲解要点（学生端知识点详情逐条展示）">
            <Form.List name="points">
              {(fields, { add, remove }) => (
                <>
                  {fields.map(({ key, name }) => (
                    <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                      <Form.Item name={name} rules={[{ required: true, message: '请输入要点内容' }]} style={{ marginBottom: 0, flex: 1 }}>
                        <Input placeholder="输入一条讲解要点" maxLength={100} />
                      </Form.Item>
                      <Button danger size="small" onClick={() => remove(name)}>删除</Button>
                    </Space>
                  ))}
                  <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add('')}>添加要点</Button>
                </>
              )}
            </Form.List>
          </Form.Item>
          <Form.Item label="知识点配图（可选，学生端知识点详情展示）">
            <Upload
              listType="picture-card"
              showUploadList={false}
              accept="image/*"
              beforeUpload={handleImageUpload}
            >
              {imageUrl
                ? <img src={imageUrl} alt="知识点配图" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                : (
                  <div style={{ textAlign: 'center' }}>
                    {uploading ? <UploadOutlined /> : <PlusOutlined />}
                    <div style={{ marginTop: 8 }}>{uploading ? '上传中' : '上传图片'}</div>
                  </div>
                )}
            </Upload>
            {imageUrl && (
              <Button size="small" style={{ marginTop: 8 }} onClick={() => setImageUrl('')}>移除图片</Button>
            )}
          </Form.Item>
        </Form>
      </Modal>

      {/* 绑定题目弹窗（题目库选择，全量替换保存） */}
      <Modal
        title={`绑定题目 - ${bindKp?.name || ''}`}
        open={bindOpen}
        onOk={saveBind}
        onCancel={() => setBindOpen(false)}
        okText="保存绑定"
        cancelText="取消"
        width={820}
        confirmLoading={savingBind}
        destroyOnClose
      >
        <Space style={{ marginBottom: 12 }} align="center">
          <Input.Search
            style={{ width: 320 }}
            placeholder="按题目名称搜索题目库"
            value={bindKeyword}
            onChange={(e) => setBindKeyword(e.target.value)}
            onSearch={onBindKeywordSearch}
            allowClear
          />
          <Text type="secondary">已选 {selectedIds.length} 题（题目来自题目库，不能在此新增）</Text>
        </Space>
        <Table
          rowKey="id"
          size="small"
          loading={tplLoading}
          dataSource={tplList}
          rowSelection={{
            selectedRowKeys: selectedIds,
            onChange: (keys) => setSelectedIds(keys),
          }}
          pagination={{
            current: tplCurrent,
            pageSize: 8,
            total: tplTotal,
            size: 'small',
            onChange: (c) => { setTplCurrent(c); fetchTemplates(c, bindKeyword); },
          }}
          columns={[
            { title: '题目名称', dataIndex: 'name', ellipsis: true, render: (n) => <Text strong>{n}</Text> },
            {
              title: '题型', dataIndex: 'questionType', width: 80, align: 'center',
              render: (t) => <Tag>{QUESTION_TYPE_LABEL[t] || t}</Tag>,
            },
            {
              title: '所属题库', dataIndex: 'repoName', width: 140, ellipsis: true,
              render: (n) => n || '-',
            },
          ]}
        />
      </Modal>
    </div>
  );
}
