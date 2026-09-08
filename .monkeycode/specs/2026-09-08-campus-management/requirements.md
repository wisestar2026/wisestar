# 校区管理（Campus Management）

## Introduction

本迭代把「行政管理-校区管理」从占位页建设为正式功能：维护校区档案（名称/状态/备注），校区作为标签用于学员主数据与员工账号，并作为校长/教务/学管师三类角色的**数据权限范围**：这类账号只能看到自己绑定校区的学员业务数据。

## Glossary

- **校区（Campus）**：线下教学点。以唯一名称标识，含启用/停用状态与备注。
- **后台员工账号（User）**：`t_user` + `t_account`（后台登录）对应的运营/管理人员，由「系统管理-用户管理」维护。
- **内置角色**：管理员（admin）、校长（principal）、教务（academic）、学管师（consultant）、教师（teacher）等，编码见 `t_role.code`。
- **校区数据权限（Campus Data Scope）**：账号可见数据被限制为指定校区集合的机制。
- **学员业务域**：学员管理、学员订单、督学（学习记录/任务/学币/活动等学员运营数据）。本定义不包含首页仪表盘统计。
- **引用学员（Referencing Student）**：`t_student.campus` 文本等于某校区名称的学员记录。

## Requirements

### R1. 校区档案维护

**User Story:** 作为管理员，我要维护校区档案，使校区成为学员与员工可用的组织标签。

#### Acceptance Criteria

1. WHEN 具备 `campus:list` 权限的用户打开「行政管理-校区管理」，the system SHALL 展示校区列表，包含名称、状态、备注、创建时间，以及该校区下引用学员数、绑定（校长/教务/学管师）员工数。
2. WHEN 管理员创建校区，the system SHALL 保存名称、状态（默认启用）、备注；IF 名称与现存校区重复，the system SHALL 拒绝保存并提示重名。
3. WHEN 管理员修改校区状态为停用，the system SHALL 保留该校区既有学员与员工绑定的历史数据，并仅在停用后的学员新增/编辑表单下拉中隐藏该校区。
4. WHEN 管理员修改校区名称，the system SHALL 在同一事务内同步更新所有引用该校区名称的学员记录，并保持全局名称唯一。
5. WHEN 管理员请求删除校区，IF 校区存在引用学员或绑定员工，the system SHALL 拒绝删除并提示先停用；IF 无任何引用，the system SHALL 删除校区及其员工绑定关系。

### R2. 员工-校区绑定

**User Story:** 作为管理员，我要为员工账号绑定校区，以界定其数据可见范围。

#### Acceptance Criteria

1. WHEN 管理员在「系统管理-用户管理」新增或编辑用户，the system SHALL 提供校区多选字段，选项来自启用校区；IF 用户角色包含校长/教务/学管师任一编码，the system SHALL 展示该字段。
2. WHEN 用户选择多个校区并保存，the system SHALL 将全部校区写入该用户的多校区关联。
3. WHEN 编辑用户时清空已选校区，the system SHALL 删除该用户全部校区关联。
4. WHILE 校区处于停用状态，the system SHALL 仍保留该校区与用户的既有绑定并计入数据权限范围。
5. 校区绑定写入 the system SHALL 仅在具备 `system:user:create`/`system:user:update` 权限的接口执行（当前仅管理员可维护）。

### R3. 校区数据权限（核心）

**User Story:** 作为绑定校区的校长/教务/学管师，我只能查看本校区学员的业务数据。

#### Acceptance Criteria

1. WHEN 登录账号角色包含校长/教务/学管师任一编码且不含 admin 编码，the system SHALL 将其可见的学员业务数据限定为绑定校区的学员。
2. IF 此类账号未绑定任何校区，the system SHALL 对其返回空数据集（列表为空，不可见任何校区数据）。
3. WHEN 此类账号访问学员管理列表/详情、订单列表/详情、督学数据，the system SHALL 依据校区范围过滤列表，并在按 ID 访问单条数据时校验目标学员属于其校区范围，拒绝越权访问。
4. WHILE 学员未归属任何校区（校区为空），the system SHALL 不向上述有校区范围的账号展示该学员；该学员对管理员及未受校区限制的账号保持原可见性。
5. WHEN 账号角色包含 admin，或账号角色不包含校长/教务/学管师任一编码，the system SHALL 不施加校区过滤，维持现状全量可见。
6. WHEN 校区被停用或改名，the system SHALL 使数据权限范围随之联动：改名同步生效、停用仍保留在范围内。
7. WHEN 上述账号打开「校区管理」列表，the system SHALL 仅返回其绑定校区（含停用校区）的行，避免暴露其它校区信息；管理员与未受校区限制的账号 SHALL 看到全部校区。

### R4. 菜单、权限点与角色授权

**User Story:** 作为平台配置方，我要让「校区管理」出现在行政管理下并对相关角色授权。

#### Acceptance Criteria

1. the system SHALL 在「行政管理」分组提供菜单项「校区管理」（URL `/admin/campus`），菜单可见性依赖权限点 `campus:list`。
2. the system SHALL 授予管理员 `campus:list`、`campus:create`、`campus:update`、`campus:delete` 四个权限点；授予校长、教务、学管师内置角色 `campus:list`；教师及其它内置角色不授予校区权限点。
3. WHEN 管理员打开角色权限树编辑界面，the system SHALL 展示「校区管理」分组及其 4 个权限点，供自定义角色按需勾选。
4. IF 角色被移除 `campus:list`，the system SHALL 隐藏该账号的校区管理菜单，并拒绝其访问 `/admin/campus` 接口。

### R5. 学员校区标签使用

**User Story:** 作为学员运营/管理人员，我要让学员的校区下拉来自校区管理真实数据，并能按校区筛选。

#### Acceptance Criteria

1. WHEN 打开学员新增/编辑表单，the system SHALL 从校区管理接口加载启用校区作为下拉选项，替代现有前端写死的四个校区常量。
2. WHEN 保存学员校区，the system SHALL 将所选校区名称写入 `t_student.campus`（延续现有存储字段语义）；停用校区的既有学员 the system SHALL 原样展示历史校区名。
3. WHEN 学员列表提供按校区筛选条件并提交，the system SHALL 在查询结果中过滤出所选校区的学员，且结果不超出当前账号的数据权限范围。
4. WHEN 校区不存在或已被停用，the system SHALL 不在下拉中提供该校区，但不影响已引用它的历史学员展示。

### R6. 非目标（本迭代明确不做）

1. 学生端（C 端学员账号）界面与数据访问不因校区变化而调整。
2. 内容域（题库/知识点/教材/教案/作答数据）不做校区隔离。
3. 首页仪表盘统计不按校区过滤。
4. 教师角色不参与校区绑定与校区数据隔离。
