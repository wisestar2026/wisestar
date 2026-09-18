/**
 * importance.jsx - 小节 / 知识点「重点程度」标识工具
 *
 * 三级取值（与后端 t_section.importance / t_knowledge_point.importance 对齐）：
 *   core   = 核心（红色）
 *   key    = 重点（橙色）
 *   normal = 一般（默认灰）
 * 空值表示未标注，不展示标识。
 *
 * 使用方：NodeEditModal（编辑选择）、SectionManagePage / KnowledgePointManagePage（列表）、
 *         TeachingResearchPlatformPage（教研平台知识树）、student/KnowledgePage（学员端知识页）。
 */
import { Tag } from 'antd';

/** 重点程度可选项（供 Select 使用） */
export const IMPORTANCE_OPTIONS = [
  { value: 'core', label: '核心' },
  { value: 'key', label: '重点' },
  { value: 'normal', label: '一般' },
];

/** 各取值的展示元数据：label + antd Tag color */
export const IMPORTANCE_META = {
  core: { label: '核心', color: 'red' },
  key: { label: '重点', color: 'orange' },
  normal: { label: '一般', color: 'default' },
};

/**
 * 重点程度标识组件：未标注（空值/未知值）时渲染 null。
 * @param {Object} props - { value, ...TagProps }
 */
export default function ImportanceTag({ value, ...rest }) {
  const meta = IMPORTANCE_META[value];
  if (!meta) return null;
  return <Tag color={meta.color} {...rest}>{meta.label}</Tag>;
}
