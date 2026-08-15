# 人事管理 · 角色权限管理 需求文档

Feature Name: hr-role-permission
Updated: 2026-08-13

## Introduction

人事管理板块聚焦**后台系统的角色与权限控制**：管理员可新增角色、删除角色、编辑角色权限；权限粒度对应后台系统各功能模块的增删改查操作。系统预置 5 个业务角色（管理员、校长、教师、学管师、教务）并分配合理默认权限；前端菜单按登录用户权限动态显示，后端接口按权限点拦截，形成完整的 ERP 风格角色权限管理体系。

本需求文档基于用户 2026-08-13 口述需求整理，采用 EARS 语法规范化。

## Glossary

- **角色（Role）**：后台系统的权限载体，承载一组权限点，通过用户-角色关联授予后台用户。
- **权限点（Authority）**：后台系统功能操作的授权编码，格式 `module:action`（如 `student:create`、`repo:list`）。
- **权限树（PermissionTree）**：按功能模块分组的权限点清单，用于角色编辑页的树形勾选。
- **内置角色（Builtin Role）**：系统预置的 5 个业务角色（管理员/校长/教师/学管师/教务），不可删除、可编辑权限（管理员除外）。
- **菜单级权限（Menu-level）**：前端菜单与路由按当前用户权限点过滤显示。
- **接口级权限（API-level）**：后端接口通过 `@PreAuthorize("hasAuthority('xxx')")` 校验访问权限。

## Requirements

### R1. 角色列表

**User Story:** AS 管理员，I want 查看后台角色列表，SO THAT 掌握系统角色全貌并定位目标角色。

#### Acceptance Criteria

1. WHEN 管理员进入角色管理页，the system SHALL 分页展示角色列表（角色名称、编码、备注、权限点数量、创建时间、状态）。
2. WHEN 管理员按角色名称模糊搜索，the system SHALL 返回匹配角色的分页结果。
3. WHEN 角色列表加载，the system SHALL 标识内置角色（不可删除）。

### R2. 新增角色

**User Story:** AS 管理员，I want 创建新角色并为其分配权限，SO THAT 系统可按业务需要新增角色。

#### Acceptance Criteria

1. WHEN 管理员提交新增角色表单，the system SHALL 校验角色名称非空、编码非空后写入角色表。
2. WHEN 新增角色时勾选权限，the system SHALL 保存所选权限点列表。
3. IF 角色编码与已有角色重复，the system SHALL 拒绝新增并返回重复提示。
4. WHEN 新增角色成功，the system SHALL 返回角色列表并刷新。
5. WHEN 新增角色时未勾选权限，the system SHALL 允许保存为空权限角色（仅可见菜单为空的兜底态，由前端菜单过滤自然收敛）。

### R3. 编辑角色与权限

**User Story:** AS 管理员，I want 编辑角色基础信息与权限树勾选，SO THAT 角色的功能访问范围随业务调整。

#### Acceptance Criteria

1. WHEN 管理员编辑角色基础信息（名称、备注），the system SHALL 保存更新结果。
2. WHEN 管理员修改角色权限勾选并保存，the system SHALL 以新权限点列表整体覆盖旧权限点。
3. WHEN 角色权限更新，the system SHALL 清除该角色关联用户的权限缓存，使新权限即时生效。
4. WHEN 编辑内置角色（除管理员外），the system SHALL 允许编辑权限，但编码不可修改。

### R4. 删除角色

**User Story:** AS 管理员，I want 删除不再使用的角色，SO THAT 角色体系保持精简。

#### Acceptance Criteria

1. WHEN 管理员删除非内置角色，the system SHALL 删除角色并同步解除用户-角色关联。
2. IF 目标角色为内置角色，the system SHALL 拒绝删除并提示内置角色不可删除。
3. IF 系统仅剩一个角色，the system SHALL 拒绝删除并提示至少保留一个角色。
4. WHEN 删除角色成功，the system SHALL 清除该角色关联用户的权限缓存。

### R5. 权限树清单

**User Story:** AS 管理员，I want 以树形结构查看并按模块勾选功能权限，SO THAT 角色权限配置直观高效。

#### Acceptance Criteria

1. WHEN 打开角色编辑权限树，the system SHALL 按功能模块分组展示权限点（模块 → 操作）。
2. WHEN 勾选/取消父节点，the system SHALL 联动勾选/取消该模块下全部操作。
3. WHEN 勾选/取消子节点，the system SHALL 按子节点选中状态自动计算父节点半选/选中/未选状态。
4. WHEN 保存权限，the system SHALL 仅提交叶节点（具体操作点）的权限编码。

### R6. 权限生效范围（菜单级 + 接口级）

**User Story:** AS 后台用户，I want 仅看到并访问被授予权限的功能，SO THAT 权限边界明确。

#### Acceptance Criteria

1. WHEN 后台用户登录，the system SHALL 返回其全部权限点列表。
2. WHILE 后台用户未具备某功能模块的任意权限，the system SHALL 在侧边菜单隐藏该模块入口。
3. WHEN 后台用户直接访问无权限的路由，the system SHALL 拦截并提示无权限。
4. WHEN 后台用户调用无权限的接口，the system SHALL 返回 403 并拒绝执行。
5. WHEN 后台用户具备 `xxx:list`（查看）权限，the system SHALL 允许其访问该模块的查看接口；具备 `xxx:create/update/delete` 权限，the system SHALL 允许对应写操作接口。

### R7. 内置角色预置

**User Story:** AS 系统，I want 预置 5 个业务角色并分配合理默认权限，SO THAT 系统开箱即用。

#### Acceptance Criteria

1. WHEN 数据库初始化，the system SHALL 预置角色：管理员（admin）、校长（principal）、教师（teacher）、学管师（consultant）、教务（academic）。
2. WHEN 数据库初始化，the system SHALL 为各内置角色分配与其职责匹配的默认权限点。
3. WHEN 数据库初始化，the system SHALL 将管理员角色标记为超管，拥有全部权限点。
4. WHEN 系统新增功能权限点，the system SHALL 将新权限点自动授予管理员角色。

## 边界约束（Non-Functional）

1. 内置角色（5 个）不可删除，编码不可修改；管理员角色权限不可编辑（始终全量）。
2. 系统至少保留一个角色。
3. 权限点编码格式 `module:action`，模块分组与后台侧边菜单一一对应。
4. 角色权限更新后必须清除关联用户权限缓存，保证即时生效。
5. 前端菜单过滤与后端接口拦截双重生效，前端隐藏不替代后端校验。

## 已确认决策（2026-08-13）

1. **权限树数据源**：按当前后台功能模块重新梳理权限点清单（仪表盘/题库/题目/知识管理/练习/学员管理/订单/系统管理/历史问卷与答案模块仍在菜单中的一并纳入）；历史遗留且无菜单入口的权限点（如 file）不纳入权限树，仅保留在管理员角色中。
2. **内置角色策略**：数据库种子预置 管理员/校长/教师/学管师/教务 5 角色并分配默认权限；内置角色不可删除，管理员为超管（权限不可编辑、始终全量）。
3. **权限生效范围**：菜单级（菜单/路由按权限过滤）+ 接口级（后端按权限点拦截）；按钮级细粒度控制留待后续迭代。
4. **用户-角色分配**：沿用现有系统管理-用户管理（后端 `system:user:update` 已支持 roles 字段），本次迭代不重复开发分配界面。
