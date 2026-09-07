/**
 * ImportModal.jsx - Excel 批量导入题目弹窗
 *
 * 功能:
 *   1. 选择目标练习（可选，留空则导入为「未分配」题目，归入系统全局题库）
 *   2. 上传 Excel 文件（.xlsx / .xls）
 *   3. 提交到后端 /api/repo/import 接口
 *
 * Excel 格式要求（标准单表模板）:
 *   一个工作表（首 sheet），第一行为表头（22 列），从第二行起每行一道题目。
 *   表头顺序: 学科 / 题型 / 章节 / 小节 / 知识点 / 题目 / 选项A~H / 难易程度 / 正确答案1~5 / 解析 / 标签
 *   题型填中文: 判断题 / 单选题 / 单项填空 / 多选题 / 多项填空
 *   学科/章节/小节/知识点必须与系统中已建体系名称一致（名称不匹配的行会报错并中止导入）
 *
 * 被谁引用: QuestionListPage（题目管理页的"Excel 导入"按钮）
 *
 * Props:
 *   open: boolean           - 弹窗是否可见
 *   onCancel: () => void    - 关闭回调
 *   onSuccess: () => void   - 导入成功后回调（父组件刷新列表）
 *   repos: Array<{id, name}> - 可选练习列表（留空不选时题目以「未分配」入库）
 *
 * 核心数据流:
 *   选择练习（可选） + 文件 → handleImport → importTemplate({file, repoId})
 *   → POST /api/repo/import (multipart/form-data) → 后端解析 Excel 入库
 *   → onSuccess() → QuestionListPage.fetchData(1) 刷新
 */

import { useState } from 'react';
import { Modal, Select, Upload, Button, Space, Typography, message, Alert } from 'antd';
import { InboxOutlined, DownloadOutlined } from '@ant-design/icons';
import { importTemplate, downloadImportTemplate } from '../../api/repo';

const { Text } = Typography;
const { Dragger } = Upload;

export default function ImportModal({ open, onCancel, onSuccess, repos = [] }) {
  // repoId: 选中的目标练习 ID（未选时 undefined）
  // file: 用户选择的 Excel 文件对象（originFileObj 是浏览器原生 File）
  // importing: 导入请求进行中标记（控制按钮 loading 状态，防止重复提交）
  const [repoId, setRepoId] = useState(undefined);
  const [file, setFile] = useState(null);
  const [importing, setImporting] = useState(false);

  // ---- 重置状态 ----
  // 关闭弹窗前清空已选练习和文件，保证下次打开是干净状态
  const handleCancel = () => {
    setRepoId(undefined);
    setFile(null);
    onCancel();
  };

  // ---- 文件选择 ----
  // antd Upload 的 onChange：通过 beforeUpload={() => false} 阻止自动上传，
  // 这里手动从 fileList 中取出第一个文件的 originFileObj（原生 File 对象）
  const handleFileSelect = (info) => {
    if (info.fileList?.length > 0) {
      setFile(info.fileList[0].originFileObj);
    }
  };

  // ---- 执行导入 ----
  // 前置校验: 必须选择文件；目标练习可选（留空则导入为「未分配」题目，归入系统全局题库）
  const handleImport = async () => {
    if (!file) { message.warning('请选择 Excel 文件'); return; }

    setImporting(true);
    try {
      // 调用 api/repo.js 的 importTemplate，内部使用原生 axios 走 FormData
      // 数据流: ImportModal → importTemplate → POST /api/repo/import → 后端解析 Excel
      await importTemplate({ file, repoId });
      message.success('导入成功！请刷新页面查看导入的题目');
      handleCancel();        // 清空本地状态并关闭弹窗
      onSuccess?.();         // 通知父组件（QuestionListPage）刷新列表
    } catch (err) {
      // 优先展示后端返回的业务错误信息（如 Excel 格式不符合要求）
      const msg = err?.response?.data?.message || err?.message || '导入失败，请检查文件格式是否正确';
      message.error(msg);
    } finally {
      setImporting(false);
    }
  };

  // ---- 下载模板 ----
  // 后端 /api/repo/import/template 返回空白标准模板（22 列表头 + 填写说明 sheet）
  // 同样用隐藏 a 标签触发下载，避免走 JS 二进制处理
  const handleDownloadTemplate = () => {
    downloadImportTemplate();
    message.info('正在下载模板...');
  };

  return (
    <Modal
      title="Excel 批量导入题目"
      open={open}
      onCancel={handleCancel}
      footer={null}
      width={600}
      destroyOnHidden
      mask={{ closable: false }}
    >
      <Space orientation="vertical" style={{ width: '100%' }} size="middle">

        {/* ---- Excel 格式说明 ---- */}
        <Alert
          type="info"
          showIcon
          message="Excel 格式要求"
          description={
            <div style={{ fontSize: 12 }}>
              <p style={{ margin: '4px 0' }}>1. 使用<strong>标准单表模板</strong>：一个工作表，第一行为表头，第二行起每行一道题目</p>
              <p style={{ margin: '4px 0' }}>2. 题型填中文：<strong>判断题 / 单选题 / 单项填空 / 多选题 / 多项填空</strong></p>
              <p style={{ margin: '4px 0' }}>3. 选择题填「选项A~H」列，答案列填选项字母；多选题答案可填多个（如 AB）</p>
              <p style={{ margin: '4px 0' }}>4. 多项填空的多个空位答案用 <strong>|</strong> 分隔（如 m|a）</p>
              <p style={{ margin: '4px 0' }}>5. 学科 / 章节 / 小节 / 知识点需与系统中已建名称一致；小节可留空（填写时须匹配该学科/章节）</p>
              <p style={{ margin: '4px 0' }}>6. 难易程度填：简单 / 中等 / 困难</p>
              <p style={{ margin: '4px 0' }}>
                7. 任一行的内容/归属校验失败会提示具体行号，并<strong>中止整次导入</strong>
              </p>
              <p style={{ margin: '4px 0' }}>8. 目标练习可留空：留空导入的题目为「未分配」，可在题目管理列表直接看到</p>
              <Button type="link" size="small" icon={<DownloadOutlined />} onClick={handleDownloadTemplate} style={{ padding: 0 }}>
                下载 Excel 导入模板
              </Button>
            </div>
          }
        />

        {/* ---- 选择目标练习（可选） ---- */}
        <div>
          <Text strong style={{ display: 'block', marginBottom: 4 }}>目标练习（可选）</Text>
          <Select
            value={repoId}
            onChange={setRepoId}
            placeholder="留空则导入为未分配题目"
            allowClear
            style={{ width: '100%' }}
            options={repos.map((r) => ({ label: r.name, value: r.id }))}
          />
          <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 4 }}>
            不选择时导入的题目「所属练习」为空，直接进入题目管理列表
          </Text>
        </div>

        {/* ---- 上传文件 ---- */}
        <div>
          <Text strong style={{ display: 'block', marginBottom: 4 }}>上传 Excel 文件</Text>
          <Dragger
            accept=".xlsx,.xls"
            maxCount={1}
            beforeUpload={() => false}  // 阻止自动上传，手动控制
            onChange={handleFileSelect}
            onRemove={() => setFile(null)}
          >
            <p className="ant-upload-drag-icon"><InboxOutlined /></p>
            <p className="ant-upload-text">点击或拖拽 Excel 文件到此区域</p>
            <p className="ant-upload-hint">支持 .xlsx 和 .xls 格式</p>
          </Dragger>
        </div>

        {/* ---- 操作按钮 ---- */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <Button onClick={handleCancel}>取消</Button>
          <Button type="primary" onClick={handleImport} loading={importing} disabled={!file}>
            开始导入
          </Button>
        </div>

      </Space>
    </Modal>
  );
}
