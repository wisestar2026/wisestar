/**
 * WordImageReviewPage.jsx - 英语单词配图审核页（后台管理端）
 *
 * 功能:
 *   1. 按版本/年级/册别/单元/小节筛选单词，默认仅展示「无配图」的单词
 *   2. 勾选单词后批量抓取候选配图（来自免费词典接口，只读不落库）
 *   3. 在审核弹窗中为每个单词选择候选图或上传本地图片，确认后入库
 *
 * URL: /english/word-image（受 AuthGuard 保护，权限 english:word:update）
 * 被谁引用：App.jsx 路由表；菜单入口在 MainLayout.jsx「英语」板块
 *
 * 数据流:
 *   GET  /api/english/word-manager/list?hasImage=false  → 无图单词分页列表
 *   POST /api/english/word-image/candidates  { wordIds }        → 候选图列表
 *   POST /api/english/word-image/confirm     { wordId, imageUrl } → 确认入库
 *   POST /api/english/word-image/upload?wordId=  (multipart)      → 上传本地图片入库
 */

import { useEffect, useState } from 'react';
import { Table, Space, Button, Input, Select, Modal, message, Checkbox, Upload, Image, Tag, Typography } from 'antd';
import {
  PictureOutlined,
  CloudDownloadOutlined,
  CheckCircleOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import request from '../../api/request';
import { getWordImageCandidates, confirmWordImage, uploadWordImage } from '../../api/englishAdmin';

const { Title } = Typography;

const VERSION_OPTIONS = ['人教版', '苏教版', '北师大版', '外研版'].map((v) => ({ value: v, label: v }));
const GRADE_OPTIONS = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级'].map((g) => ({ value: g, label: g }));
const TERM_OPTIONS = ['上册', '下册'].map((t) => ({ value: t, label: t }));

export default function WordImageReviewPage() {
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // 筛选条件
  const [version, setVersion] = useState('');
  const [grade, setGrade] = useState('');
  const [term, setTerm] = useState('');
  const [unit, setUnit] = useState('');
  const [section, setSection] = useState('');
  const [onlyMissing, setOnlyMissing] = useState(true);

  // 行选择
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [fetching, setFetching] = useState(false);

  // 审核弹窗
  const [reviewOpen, setReviewOpen] = useState(false);
  const [reviewItems, setReviewItems] = useState([]);
  const [savingAll, setSavingAll] = useState(false);

  const loadList = () => {
    setLoading(true);
    request
      .get('/english/word-manager/list', {
        params: {
          current,
          pageSize,
          ...(version && { version }),
          ...(grade && { grade }),
          ...(term && { term }),
          ...(unit && { unit }),
          ...(section && { section }),
          ...(onlyMissing && { hasImage: 'false' }),
        },
      })
      .then((res) => {
        setList(res.data?.list || []);
        setTotal(res.data?.total || 0);
      })
      .catch(() => message.error('加载失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadList();
  }, [current, pageSize, onlyMissing]);

  const handleReset = () => {
    setVersion('');
    setGrade('');
    setTerm('');
    setUnit('');
    setSection('');
    setOnlyMissing(true);
    setCurrent(1);
  };

  // 抓取候选图并打开审核弹窗
  const handleFetchCandidates = () => {
    if (!selectedRowKeys.length) {
      message.warning('请先勾选需要配图的单词');
      return;
    }
    setFetching(true);
    getWordImageCandidates(selectedRowKeys)
      .then((res) => {
        const data = res.data || [];
        if (!data.length) {
          message.warning('未获取到候选配图，可尝试手动上传');
          return;
        }
        setReviewItems(
          data.map((item) => ({
            wordId: item.wordId,
            spell: item.spell,
            meaning: item.meaning,
            candidates: item.candidates || [],
            selectedUrl: (item.candidates && item.candidates[0]) || '',
            imageUrl: '',
            saved: false,
          }))
        );
        setReviewOpen(true);
      })
      .catch(() => {})
      .finally(() => setFetching(false));
  };

  const updateItem = (wordId, patch) => {
    setReviewItems((prev) => prev.map((it) => (it.wordId === wordId ? { ...it, ...patch } : it)));
  };

  // 确认候选图入库
  const handleConfirm = (item) => {
    if (!item.selectedUrl) {
      message.warning('请先选择一张候选图或上传本地图片');
      return;
    }
    confirmWordImage(item.wordId, item.selectedUrl)
      .then((res) => {
        const view = res.data || {};
        updateItem(item.wordId, { saved: true, imageUrl: view.imageUrl || '' });
        message.success(`「${item.spell}」配图已入库`);
      })
      .catch(() => {});
  };

  // 上传本地图片入库
  const handleUpload = (item, file) => {
    uploadWordImage(item.wordId, file)
      .then((view) => {
        updateItem(item.wordId, { saved: true, imageUrl: view?.imageUrl || '' });
        message.success(`「${item.spell}」本地图片已入库`);
      })
      .catch((e) => message.error(e.message || '上传失败'));
    return false; // 阻止 antd 自动上传
  };

  // 一键入库所有「已选图且未保存」项
  const handleConfirmAll = () => {
    const pending = reviewItems.filter((it) => it.selectedUrl && !it.saved);
    if (!pending.length) {
      message.info('没有待入库的配图');
      return;
    }
    setSavingAll(true);
    Promise.all(
      pending.map((it) =>
        confirmWordImage(it.wordId, it.selectedUrl)
          .then((res) => ({ ok: true, wordId: it.wordId, imageUrl: res.data?.imageUrl || '' }))
          .catch(() => ({ ok: false, wordId: it.wordId }))
      )
    )
      .then((results) => {
        const okMap = {};
        results.forEach((r) => {
          if (r.ok) okMap[r.wordId] = r.imageUrl;
        });
        setReviewItems((prev) =>
          prev.map((it) => (okMap[it.wordId] ? { ...it, saved: true, imageUrl: okMap[it.wordId] } : it))
        );
        const okCount = results.filter((r) => r.ok).length;
        message.success(`批量入库完成：成功 ${okCount} 个，失败 ${results.length - okCount} 个`);
      })
      .finally(() => setSavingAll(false));
  };

  const handleCloseReview = () => {
    setReviewOpen(false);
    setSelectedRowKeys([]);
    loadList();
  };

  const columns = [
    { title: '单词', dataIndex: 'spell', width: 130 },
    { title: '音标', dataIndex: 'phonetic', width: 110 },
    { title: '释义', dataIndex: 'meaning', ellipsis: true },
    {
      title: '配图',
      dataIndex: 'imageUrl',
      width: 80,
      render: (url) =>
        url ? (
          <Image src={url} width={44} height={44} style={{ objectFit: 'cover', borderRadius: 4 }} />
        ) : (
          <Tag color="warning">无图</Tag>
        ),
    },
    { title: '版本', dataIndex: 'version', width: 80 },
    { title: '年级', dataIndex: 'grade', width: 80 },
    { title: '册别', dataIndex: 'term', width: 70 },
    { title: '单元', dataIndex: 'unit', width: 90, ellipsis: true },
    { title: '小节', dataIndex: 'section', width: 120, ellipsis: true },
  ];

  return (
    <div style={{ maxWidth: 1400, margin: '0 auto', padding: 20 }}>
      <Title level={4}>单词配图</Title>
      <div style={{ color: '#888', marginBottom: 16 }}>
        勾选无配图的单词，一键抓取候选图后逐词审核确认；也可直接上传本地图片。
      </div>

      {/* 顶部工具栏 */}
      <Space style={{ marginBottom: 16 }}>
        <Button
          type="primary"
          icon={<CloudDownloadOutlined />}
          onClick={handleFetchCandidates}
          loading={fetching}
          disabled={!selectedRowKeys.length}
        >
          抓取候选图{selectedRowKeys.length ? `（${selectedRowKeys.length}）` : ''}
        </Button>
      </Space>

      {/* 筛选栏 */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Select placeholder="教材版本" allowClear style={{ width: 120 }} value={version} onChange={setVersion} options={VERSION_OPTIONS} />
        <Select placeholder="年级" allowClear style={{ width: 100 }} value={grade} onChange={setGrade} options={GRADE_OPTIONS} />
        <Select placeholder="册别" allowClear style={{ width: 90 }} value={term} onChange={setTerm} options={TERM_OPTIONS} />
        <Input placeholder="单元" allowClear style={{ width: 100 }} value={unit} onChange={(e) => setUnit(e.target.value)} />
        <Input placeholder="小节" allowClear style={{ width: 120 }} value={section} onChange={(e) => setSection(e.target.value)} onPressEnter={loadList} />
        <Checkbox checked={onlyMissing} onChange={(e) => { setOnlyMissing(e.target.checked); setCurrent(1); }}>仅看无图</Checkbox>
        <Button type="primary" onClick={loadList}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
      </Space>

      {/* 单词列表 */}
      <Table
        rowKey="id"
        rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={{
          current,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (c, s) => {
            setCurrent(c);
            setPageSize(s);
          },
        }}
      />

      {/* 配图审核弹窗 */}
      <Modal
        title={<Space><PictureOutlined />单词配图审核</Space>}
        open={reviewOpen}
        onCancel={handleCloseReview}
        width={920}
        footer={
          <Space>
            <Button onClick={handleCloseReview}>关闭</Button>
            <Button type="primary" icon={<CheckCircleOutlined />} loading={savingAll} onClick={handleConfirmAll}>
              一键入库已选图
            </Button>
          </Space>
        }
      >
        <div style={{ maxHeight: '60vh', overflowY: 'auto', paddingRight: 8 }}>
          {reviewItems.map((item) => (
            <div
              key={item.wordId}
              style={{ borderBottom: '1px solid #f0f0f0', padding: '12px 0' }}
            >
              <div style={{ marginBottom: 8 }}>
                <strong style={{ fontSize: 16 }}>{item.spell}</strong>
                {item.meaning && <span style={{ color: '#888', marginLeft: 8 }}>{item.meaning}</span>}
                {item.saved && <Tag color="success" style={{ marginLeft: 8 }}>已入库</Tag>}
              </div>

              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 8 }}>
                {item.candidates.length ? (
                  item.candidates.map((url) => (
                    <img
                      key={url}
                      src={url}
                      alt={item.spell}
                      referrerPolicy="no-referrer"
                      onClick={() => updateItem(item.wordId, { selectedUrl: url })}
                      style={{
                        width: 96,
                        height: 96,
                        objectFit: 'cover',
                        cursor: 'pointer',
                        borderRadius: 4,
                        border: item.selectedUrl === url ? '2px solid #1677ff' : '1px solid #e5e5e5',
                      }}
                    />
                  ))
                ) : (
                  <span style={{ color: '#bbb' }}>无候选图，请上传本地图片</span>
                )}
              </div>

              <Space>
                <Upload
                  showUploadList={false}
                  accept="image/*"
                  beforeUpload={(file) => handleUpload(item, file)}
                >
                  <Button size="small" icon={<UploadOutlined />}>上传本地图片</Button>
                </Upload>
                <Button
                  size="small"
                  type="primary"
                  icon={<CheckCircleOutlined />}
                  disabled={!item.selectedUrl || item.saved}
                  onClick={() => handleConfirm(item)}
                >
                  确认入库
                </Button>
                {item.imageUrl && <Image src={item.imageUrl} width={40} height={40} style={{ objectFit: 'cover', borderRadius: 4 }} />}
              </Space>
            </div>
          ))}
        </div>
      </Modal>
    </div>
  );
}
