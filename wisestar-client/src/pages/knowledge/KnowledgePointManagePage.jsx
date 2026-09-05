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
  ImportOutlined,
  DownloadOutlined,
} from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  listSubjects, listChapters, listSections, listKnowledgePoints,
  createKnowledgePoint, updateKnowledgePoint, deleteKnowledgePoint,
  saveKnowledgePointQuestions, listKnowledgePointQuestions,
  importKnowledgePoints,
} from '../../api/knowledge';
import { listTemplate } from '../../api/template';
import { uploadImage } from '../../api/upload';
import { usePermission } from '../../utils/usePermission';

const { Text } = Typography;

const QUESTION_TYPE_LABEL = {
  Radio: '单选',
  Checkbox: '多选',
  Judge: '判断',
  FillBlank: '填空',
};

// 年级/学期可选项（与章节/小节管理页一致）
const GRADE_OPTIONS = [{ value: '一年级', label: '一年级' }, { value: '二年级', label: '二年级' }, { value: '三年级', label: '三年级' }, { value: '四年级', label: '四年级' }, { value: '五年级', label: '五年级' }, { value: '六年级', label: '六年级' }];
const TERM_OPTIONS = [{ value: '上', label: '上册' }, { value: '下', label: '下册' }];

export default function KnowledgePointManagePage() {
  const { can } = usePermission();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const urlSubjectId = searchParams.get('subjectId');
  const urlChapterId = searchParams.get('chapterId');
  const urlSectionId = searchParams.get('sectionId');

  // ---- 三级下拉联动 state ----
  const [subjects, setSubjects] = useState([]);
  const [chapters, setChapters] = useState([]);
  const [sections, setSections] = useState([]);
  // 弹窗内「学科→章节→小节」联动状态（与顶部筛选独立）
  const [dialogSubjectId, setDialogSubjectId] = useState(undefined);
  const [dialogChapterId, setDialogChapterId] = useState(undefined);
  const [dialogChapters, setDialogChapters] = useState([]);
  const [dialogSections, setDialogSections] = useState([]);
  const [subjectId, setSubjectId] = useState(urlSubjectId || undefined);
  const [chapterId, setChapterId] = useState(urlChapterId || undefined);
  const [sectionId, setSectionId] = useState(urlSectionId || undefined);
  // 筛选栏过滤条件（年级/学期，任一变化即触发列表重查）
  const [searchGrade, setSearchGrade] = useState(undefined);
  const [searchTerm, setSearchTerm] = useState(undefined);

  // ---- 知识点分页 ----
  const [kps, setKps] = useState([]);
  const [total, setTotal] = useState(0);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);

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
  }, [subjectId, chapterId, sectionId, searchGrade, searchTerm]);

  // 组装列表查询参数（三级任一 + 年级/学期）
  const buildListParams = (page) => {
    const params = { current: page, pageSize };
    if (sectionId) params.sectionId = sectionId;
    else if (chapterId) params.chapterId = chapterId;
    else if (subjectId) params.subjectId = subjectId;
    if (searchGrade) params.grade = searchGrade;
    if (searchTerm) params.term = searchTerm;
    return params;
  };

  // 拉取列表（供筛选 effect / 导入 / 新增后刷新复用）
  const fetchKps = (page) => {
    if (!subjectId) return;
    setLoading(true);
    listKnowledgePoints(buildListParams(page)).then((res) => {
      setKps(res?.data?.list || []);
      setTotal(res?.data?.total || 0);
    }).catch(() => { setKps([]); setTotal(0); }).finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchKps(current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [subjectId, chapterId, sectionId, searchGrade, searchTerm, current, pageSize]);

  const subject = useMemo(() => subjects.find((s) => s.id === subjectId), [subjects, subjectId]);
  const chapter = useMemo(() => chapters.find((c) => c.id === chapterId), [chapters, chapterId]);
  const section = useMemo(() => sections.find((s) => s.id === sectionId), [sections, sectionId]);



  // ---- Excel 批量导入 ----
  const handleImport = (file) => {
    setImporting(true);
    importKnowledgePoints(file)
      .then((res) => {
        const d = res?.data || {};
        message.success(`导入完成：新增 ${d.imported ?? 0} 个知识点，跳过 ${d.skipped ?? 0} 个（归属未匹配或重名）`);
        setCurrent(1);
        fetchKps(1);
      })
      .catch((err) => message.error(err?.message || '导入失败'))
      .finally(() => setImporting(false));
    return false; // 阻止 antd 自动上传
  };

  // ---- 新增/编辑 ----
  const openModal = (kp = null) => {
    setEditing(kp);
    setModalOpen(true);
    if (kp) {
      form.resetFields();
      form.setFieldsValue({ sectionId: kp.sectionId, name: kp.name, sort: kp.sort, grade: kp.grade || undefined, term: kp.term || undefined });
      // 编辑：反查归属（小节→章节→学科）
      listSections().then((res) => {
        const allSec = res?.data || [];
        const sec = allSec.find((x) => x.id === kp.sectionId);
        if (sec) {
          setDialogChapterId(sec.chapterId);
          setDialogSections(allSec.filter((x) => x.chapterId === sec.chapterId));
          listChapters().then((res2) => {
            const allCh = res2?.data || [];
            const ch = allCh.find((c) => c.id === sec.chapterId);
            if (ch) {
              setDialogSubjectId(ch.subjectId);
              setDialogChapters(allCh.filter((c) => c.subjectId === ch.subjectId));
            } else {
              setDialogSubjectId(undefined);
              setDialogChapters(allCh);
            }
          });
        } else {
          setDialogSubjectId(undefined);
          setDialogChapterId(undefined);
          setDialogChapters([]);
          setDialogSections(allSec);
        }
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ sectionId: sectionId || undefined, sort: kps.length + 1, grade: searchGrade || undefined, term: searchTerm || undefined });
      setDialogSubjectId(subjectId || undefined);
      setDialogChapterId(chapterId || undefined);
      if (subjectId) {
        listChapters({ subjectId }).then((res) => setDialogChapters(res?.data || []));
      } else {
        setDialogChapters([]);
      }
      if (chapterId) {
        listSections({ chapterId }).then((res) => setDialogSections(res?.data || []));
      } else {
        setDialogSections([]);
      }
    }
  };

  // 弹窗内学科切换 → 联动章节
  const handleDialogSubjectChange = (val) => {
    setDialogSubjectId(val);
    setDialogChapterId(undefined);
    form.setFieldsValue({ chapterId: undefined, sectionId: undefined });
    setDialogSections([]);
    if (val) {
      listChapters({ subjectId: val }).then((res) => setDialogChapters(res?.data || []));
    } else {
      setDialogChapters([]);
    }
  };

  // 弹窗内章节切换 → 联动小节
  const handleDialogChapterChange = (val) => {
    setDialogChapterId(val);
    form.setFieldsValue({ sectionId: undefined });
    if (val) {
      listSections({ chapterId: val }).then((res) => setDialogSections(res?.data || []));
    } else {
      setDialogSections([]);
    }
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      if (!values.subjectId || !values.chapterId || !values.sectionId) {
        message.warning('请选择所属学科、章节与小节');
        return;
      }
      if (editing) {
        const payload = { ...values, id: editing.id };
        if (values.grade === undefined) payload.grade = '';
        if (values.term === undefined) payload.term = '';
        updateKnowledgePoint(payload).then(() => {
          message.success('知识点已更新');
          setModalOpen(false);
          setKps((prev) => prev.map((k) => (k.id === editing.id ? { ...k, ...values, grade: values.grade || null, term: values.term || null } : k)));
        });
      } else {
        const payload = { ...values, grade: values.grade || undefined, term: values.term || undefined };
        createKnowledgePoint(payload).then(() => {
          message.success('知识点已新增');
          setModalOpen(false);
          setCurrent(1);
          fetchKps(1);
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
      title: '年级', dataIndex: 'grade', width: 80, align: 'center',
      render: (v) => (v ? <Tag>{v}</Tag> : <Text type="secondary">-</Text>),
    },
    {
      title: '学期', dataIndex: 'term', width: 70, align: 'center',
      render: (v) => (v ? <Tag>{v === '上' ? '上册' : '下册'}</Tag> : <Text type="secondary">-</Text>),
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
{can('knowledge:update') && (
            <Button size="small" icon={<EditOutlined />} onClick={() => openModal(k)}>编辑</Button>
          )}
          {can('knowledge:delete') && (
          <Popconfirm
            title={`删除知识点「${k.name}」？`}
            description="其绑定的题目将一并解除，删除后不可恢复。"
            onConfirm={() => handleDelete(k)}
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
          )}
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
        <Space>
          {can('knowledge:create') && (
            <Button icon={<DownloadOutlined />} href="/templates/knowledge-point-import-template.xlsx" download>
              模板下载
            </Button>
          )}
          {can('knowledge:create') && (
            <Upload beforeUpload={handleImport} showUploadList={false} accept=".xlsx,.xls">
              <Button icon={<ImportOutlined />} loading={importing}>Excel 导入</Button>
            </Upload>
          )}
          {can('knowledge:create') && (<Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>新增知识点</Button>)}
        </Space>
      </div>

      {/* ---- 三级联动下拉 ---- */}
      <Space style={{ marginBottom: 16 }}>
        <Select
          style={{ width: 160 }}
          value={subjectId}
          onChange={setSubjectId}
          placeholder="全部学科"
          options={subjects.map((s) => ({ value: s.id, label: `${s.icon || ''} ${s.name}` }))}
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
        <Select
          style={{ width: 120 }}
          value={searchGrade}
          onChange={setSearchGrade}
          placeholder="全部年级"
          allowClear
          options={GRADE_OPTIONS}
        />
        <Select
          style={{ width: 110 }}
          value={searchTerm}
          onChange={setSearchTerm}
          placeholder="全部学期"
          allowClear
          options={TERM_OPTIONS}
        />
        <Text type="secondary">下拉可逐级筛选并叠加年级/学期</Text>
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
          <Form.Item name="subjectId" label="所属学科" rules={[{ required: true, message: '请选择所属学科' }]}>
            <Select
              placeholder="选择所属学科（与导入 Excel 的「学科名」对应）"
              value={dialogSubjectId}
              onChange={handleDialogSubjectChange}
              options={subjects.map((sub) => ({ value: sub.id, label: `${sub.icon || ''} ${sub.name}` }))}
              showSearch optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="chapterId" label="所属章节" rules={[{ required: true, message: '请选择所属章节' }]}>
            <Select
              placeholder="先选学科，再选所属章节（与导入 Excel 的「章节名」对应）"
              value={dialogChapterId}
              onChange={handleDialogChapterChange}
              options={dialogChapters.map((c) => ({ value: c.id, label: `${c.icon || ''} ${c.name}` }))}
              showSearch optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="sectionId" label="所属小节" rules={[{ required: true, message: '请选择所属小节' }]}>
            <Select
              placeholder="先选章节，再选所属小节（与导入 Excel 的「小节名」对应）"
              options={dialogSections.map((sec) => ({ value: sec.id, label: sec.name }))}
              showSearch optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="name" label="知识点名称" rules={[{ required: true, message: '请输入知识点名称' }]}>
            <Input placeholder="如：进位加法" maxLength={30} />
          </Form.Item>
          <Form.Item name="sort" label="排序（数字越小越靠前）">
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="grade" label="年级">
            <Select
              placeholder="不限年级"
              allowClear
              options={GRADE_OPTIONS}
            />
          </Form.Item>
          <Form.Item name="term" label="学期">
            <Select
              placeholder="不限学期"
              allowClear
              options={TERM_OPTIONS}
            />
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
              title: '所属练习', dataIndex: 'repoName', width: 140, ellipsis: true,
              render: (n) => n || '-',
            },
          ]}
        />
      </Modal>
    </div>
  );
}
