/**
 * RepoAssignPage.jsx - 已下线（练习分配页面）
 *
 * 2026-09 简化「练习」模块时移除管理端「练习分配」功能：
 *   - App.jsx 路由、MainLayout 菜单均已删除，本文件不再被任何模块引用；
 *   - 相关前端 API（assignRepo/deleteAssign/listAssign/getUserTags/saveUserTags）已从 api/repo.js 移除；
 *   - 后端分配接口与数据（t_user_repo、标签自动分配）保留，学员端「我的练习」仍消费同一数据源。
 *
 * 本文件保留仅为 git 历史可读性，占位导出空组件；后续清理仓库时可直接删除。
 */

export default function RepoAssignPage() {
  return null;
}
