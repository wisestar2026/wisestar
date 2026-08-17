/**
 * usePermission.js - 按钮级权限 hook
 *
 * 功能:
 *   按当前登录用户的权限点列表（user.authorityList，来自 /api/currentUser）
 *   判断页面内操作按钮是否可执行，实现按钮级权限控制（新增/修改/删除等）。
 *
 * 使用:
 *   const { can } = usePermission();
 *   {can('project:create') && <Button>新增</Button>}
 *   <Button disabled={!can('project:update')}>编辑</Button>
 *
 * 被谁引用: 各管理端页面（项目/答案/题库/题目/知识/学员/订单/人事/系统管理）
 */

import useUserStore from '../stores/useUserStore';

export function usePermission() {
  const user = useUserStore((s) => s.user);
  const authorities = user?.authorityList || [];

  /** 是否拥有指定权限点 */
  const can = (permission) => authorities.includes(permission);

  /** 是否拥有任一权限点（数组任意命中即 true） */
  const canAny = (permissions) => permissions.some((p) => authorities.includes(p));

  return { authorities, can, canAny };
}
