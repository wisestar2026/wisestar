# 项目词典 · 后端 flow（工作流：Flowable 表单 / 流程接口）

> 本文件由源码清单自动生成后再人工校订，颗粒度到「每个类 + 每个公开方法」。
> 阅读方式：`归属` 段给出模块与包路径；`说明` 段取自类级 Javadoc；`方法` 段逐个列出公开/受保护方法（含 Javadoc 首句）。
>
> 归属规则：模块（Maven module）= 顶层归属；包（package）= 二级归属。
> 相关规则：`注解` 表示框架角色，`注入/字段` 表示直接依赖（协作方）。
> 本文件仅描述代码事实，未收录任何密钥；配置项中的 token 均为占位符。

---
### `flow/src/main/java/cn/wisestar/server/flow/api/FlowApi.java`
- 包: `cn.wisestar.server.flow.api`
- 类型: `class FlowApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  工作流（审批流程）对外 HTTP 接口层（Controller）。
  职责：暴露 AI 自习室系统中"问卷/表单审批流程"相关的 REST 接口，包括流程设计保存、 流程部署发布、任务查询、审批操作、审核记录查询、表单 schema 权限过滤、回退节点查询、 任务统计等。所有请求统一以 `${api.prefix}/workflow` 为前缀。

  所属流程环节：本类是流程模块的入口，前端页面（流程设计器、待办中心、审批详情页）通过 这些接口与后端交互；不直接操作数据库，全部逻辑委托给 FlowService 处理。

  被谁调用：前端（浏览器）通过 HTTP 调用；无其它服务依赖本类。

  依赖什么：FlowService（核心业务门面）、PaginationResponse（统一分页返回体）、 SurveySchema（问卷结构定义）、以及 flow 包下的各类 DTO（请求/响应参数对象）。
- 方法:
  - `public FlowEntryView getFlowEntry(String projectId)`
    获取某个问卷（项目）绑定的流程定义信息。
    流程环节：流程设计环节的"读取"。前端打开流程设计器时调用，用于回显已保存的 BPMN XML 和节点配置；若该问卷尚未配置流程，返回一个仅含 projectId 的空视图。
  - `public void saveFlow(@RequestBody FlowEntryRequest request)`
    保存流程设计（草稿态）。
    流程环节：流程设计环节的"写入"。前端流程设计器点击保存时调用，将 BPMN XML 与节点配置（含字段权限、审批人、节点设置等）落库到 t_flow_entry 表；此时仅保存 草稿，不会真正部署到 Flowable 引擎。
  - `public void deploy(String projectId)`
    部署流程设计（发布为新版本）。
    流程环节：流程设计环节的"发布"。前端点击发布时调用，将草稿态的 BPMN XML 转换为 BpmnModel 并部署到 Flowable 引擎，同时将节点配置刷入 t_flow_entry_node 表、 生成 t_flow_entry_publish 发布版本记录，并把流程定义标记为已发布。
  - `public List<FlowOperationView> getAuditRecord(String processInstanceId)`
    查询某个流程实例的完整审核（操作）记录。
    流程环节：审批详情环节。前端审批详情页展示"审批历史/流转记录"时调用，返回 按时间正序排列的用户任务操作记录，并补充节点名称、审批人信息、待审批人列表等展示字段。
  - `public PaginationResponse<FlowTaskView> getFlowTasks(FlowTaskQuery query)`
    分页获取当前用户的任务列表。
    流程环节：待办中心环节。前端"待办/已办/我发起的"页面调用，根据查询类型 （`todo`、`finished`、`selfCreated`）从 Flowable 任务表或 本模块的 t_flow_operation / t_flow_instance 表分别组装分页数据，并附带表单答案、 字段权限过滤后的可见内容与任务状态。
  - `public SurveySchema loadSchema(SchemaQuery query)`
    加载问卷 schema（表单结构），并按当前任务节点的字段权限过滤。
    流程环节：审批处理环节的前置步骤。审批人打开审批详情页时调用，返回当前任务节点 配置的字段权限（隐藏/只读/可编辑）过滤之后的问卷结构，保证审批人只能看到被授权的字段。
  - `public void getTaskInfo()`
    获取任务信息（占位接口）。
    当前未实现任何逻辑：方法体为空，保留作为扩展点。若后续需要在前端直接拉取 单个任务的完整信息（而非通过 getFlowTasks / getAuditRecord 组合获取），可在 此接口补充实现。
  - `public List<RevokeView> getRevertNodes(String processInstanceId)`
    查询当前流程实例可以回退（驳回）的历史节点列表。
    流程环节：审批处理环节的"驳回到指定节点"前置查询。前端"驳回"对话框需要列出 可选节点时调用，返回基于历史操作记录构建的节点链路中可回退的节点（不含当前节点）。
  - `public void approvalTask(@RequestBody ApprovalTaskRequest request)`
    审批任务：处理用户对任务的一次操作（保存/同意/拒绝/驳回/撤回等）。
    流程环节：审批处理环节的核心入口。前端在任何审批节点点击"同意/拒绝/驳回/ 保存/撤回"等按钮时统一调用本接口，后端根据 `type` 字段分发到对应的 TaskHandler（如 agreeTaskHandler、refuseTaskHandler）执行任务流转，并落库 操作记录（t_flow_operation）与更新表单答案。
  - `public FlowStaticsView statics()`
    获取当前用户的待办/已办/我发起任务的统计数量。
    流程环节：待办中心首页统计。前端工作台/待办页面顶部展示各类任务数量时调用， 返回 todo（我的待办）、finished（已办）、copyTo（抄送）、selfCreated（我发起的） 四个维度的计数。

### `flow/src/main/java/cn/wisestar/server/flow/config/WorkflowConfig.java`
- 包: `cn.wisestar.server.flow.config`
- 类型: `class WorkflowConfig`
- 注解: @Configuration, @MapperScan, @Slf4j
- **类说明**：
  工作流模块 Spring 配置类。
  职责：

  - 通过 `@MapperScan` 扫描 cn.wisestar.server.flow.mapper 包下的 MyBatis-Plus Mapper 接口，将其注册为 Spring Bean；
  - 自定义 Flowable 流程引擎配置：为流程引擎注册全局事件监听器，使流程实例在 开始/完成/取消/挂起以及活动节点开始时，同步维护本模块的 t_flow_instance 表状态。

  所属流程环节：配置引导环节。应用启动时由 Spring 容器自动加载，是整个流程模块 与 Flowable 引擎、MyBatis 集成的基础设施。

  被谁调用：由 Spring Boot 自动装配加载，无直接调用方。

  依赖什么：SpringProcessEngineConfiguration（Flowable Spring 配置类）、 流程监听器（ProcessStartedListener、ProcessCompletedListener、 ActivityStartedListener、ProcessCancelledListener、ProcessSuspendedListener）。
- 方法:
  - `public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> customizeSpringProcessEngineConfiguration()`
    定制 Flowable 流程引擎配置：注册全局流程事件监听器。
    仅在 classpath 中存在 SpringProcessEngineConfiguration（即项目引入了 Flowable Spring Boot Starter）时才生效（`@ConditionalOnClass`），避免 未引入 Flowable 的环境加载失败。

    内部逻辑：构建事件类型 → 监听器列表的映射表并注入引擎配置：

    - PROCESS_STARTED（流程启动）→ ProcessStartedListener：插入 t_flow_instance 流程实例记录；
    - PROCESS_COMPLETED（流程正常结束）→ ProcessCompletedListener：实例状态置为"已结束"；
    - PROCESS_CANCELLED（流程被删除/拒绝）→ ProcessCancelledListener：实例状态置为"已拒绝"；
    - ACTIVITY_STARTED（活动节点开始）→ ActivityStartedListener：更新实例的当前审批阶段与"审批中"状态；
    - ENTITY_SUSPENDED / ENTITY_ACTIVATED（实例挂起/激活）→ ProcessSuspendedListener：状态置为"申请人完善中"。

### `flow/src/main/java/cn/wisestar/server/flow/constant/FieldPermissionType.java`
- 包: `cn.wisestar.server.flow.constant`
- 类型: `class FieldPermissionType`
- **类说明**：
  问卷字段权限类型常量。
  职责：定义流程节点中"字段权限"（`fieldPermission`）的取值，该权限在 问卷提交、审批人打开表单时被用于过滤字段（隐藏 / 只读 / 可编辑）。

  所属流程环节：贯穿"发起申请 → 逐级审批"全过程。发起人配置流程节点时以这些 常量记录字段权限（存储于 t_flow_entry_node.field_permission JSON 列）， `SchemaHelper.updateSchemaByPermission` 与审批处理逻辑读取该值决定 每个问卷题目对当前用户是否可见、可编辑。

  被谁调用：`SchemaHelper`（schema 过滤）、审批/保存 TaskHandler （答案合并权限判断）、流程节点保存逻辑。

  依赖什么：无（纯常量类）。

### `flow/src/main/java/cn/wisestar/server/flow/constant/FlowApprovalType.java`
- 包: `cn.wisestar.server.flow.constant`
- 类型: `class FlowApprovalType`
- **类说明**：
  审批类型常量类。
  职责：定义流程审批/流转中所有操作类型的标识字符串，是 `ApprovalTaskRequest.type` 字段的取值字典。前端审批按钮触发的每次操作 都会携带一个 type，后端 `FlowServiceImpl.approvalTask` 依据该值 通过 Spring 容器按 Bean 名称（type + "TaskHandler"）分发到对应的 TaskHandler 执行流转（如 agreeTaskHandler、refuseTaskHandler 等）。

  所属流程环节：审批处理环节的核心类型字典，同时被审核记录展示 （`DICT_MAP` 用于把类型码翻译成中文名）所使用。

  被谁调用：FlowApi（请求参数 type）、FlowServiceImpl（handler 分发、审核记录 类型名翻译）、AbstractTaskHandler 及其子类（操作记录 approvalType 落库）。

  依赖什么：无（纯常量类，仅依赖 JDK 集合）。

### `flow/src/main/java/cn/wisestar/server/flow/constant/FlowConstant.java`
- 包: `cn.wisestar.server.flow.constant`
- 类型: `class FlowConstant`
- **类说明**：
  工作流通用常量。
  职责：集中定义流程运转过程中使用的关键字符串常量，包括流程变量名 （answerId、发起人 ID）与发起节点的 ID / 名称。这些常量被流程发起 （`SaveTaskHandler`）、流程实例同步（`ProcessStartedListener`）、 审核记录展示（`FlowServiceImpl.getAuditRecord`）等环节共享，避免魔法字符串散落各处。

  所属流程环节：贯穿"发起 → 流转 → 展示"全过程，是流程变量与引擎节点之间的约定。

  被谁调用：SaveTaskHandler、ProcessStartedListener、FlowServiceImpl、 AbstractTaskHandler 等。

  依赖什么：无（纯常量类）。

### `flow/src/main/java/cn/wisestar/server/flow/constant/FlowEntryStatus.java`
- 包: `cn.wisestar.server.flow.constant`
- 类型: `class FlowEntryStatus`
- **类说明**：
  流程定义（t_flow_entry.status）发布状态常量。
  职责：标识一份流程设计当前处于"草稿未发布"还是"已发布"状态。部署成功后才 能被问卷发起流程时使用。

  所属流程环节：流程设计/发布环节。保存草稿时保持未发布；调用 `FlowServiceImpl.deploy` 部署成功后将状态置为已发布。

  被谁调用：`FlowServiceImpl.deploy`（部署成功置为 PUBLISHED）。

  依赖什么：无（纯常量类）。

### `flow/src/main/java/cn/wisestar/server/flow/constant/FlowInstanceStatus.java`
- 包: `cn.wisestar.server.flow.constant`
- 类型: `class FlowInstanceStatus`
- **类说明**：
  流程实例状态（t_flow_instance.status）常量。
  职责：定义流程实例的完整生命周期状态，并维护状态码 → 中文名称的字典 （#getDictStatus(int)）。流程实例状态由全局事件监听器在引擎事件发生时 自动同步维护，也用于"我发起的"任务列表的状态展示与统计。

  状态流转总览（流程从哪开始、如何流转、到哪结束）：

  - 发起（SaveTaskHandler 启动流程实例）→ ProcessStartedListener 置为 APPROVING；
  - 每次进入审批用户任务节点 → ActivityStartedListener 置为 APPROVING 并更新审批阶段；
  - 最后节点同意、流程正常结束 → ProcessCompletedListener 置为 FINISHED；
  - 审批人拒绝（删除流程实例）→ ProcessCancelledListener 置为 REFUSED；
  - 发起人撤回并回退到发起节点 → 实例被挂起（suspend），ProcessSuspendedListener 置为 SUSPENDED（申请人完善中）；
  - 申请人完善后再次提交 → SaveTaskHandler 重新激活实例，状态回到审批中。

  被谁调用：5 个流程监听器、FlowServiceImpl（任务列表/统计/审核记录）、前端状态展示。

  依赖什么：无（纯常量类，仅依赖 JDK 集合）。
- 方法:
  - `public static String getDictStatus(int status)`
    将状态码翻译成中文名称。

### `flow/src/main/java/cn/wisestar/server/flow/constant/FlowTaskQueryType.java`
- 包: `cn.wisestar.server.flow.constant`
- 类型: `class FlowTaskQueryType`
- **类说明**：
  任务查询类型常量。
  职责：定义前端"任务列表"页签的查询类型，是 `FlowTaskQuery.type` 字段 的取值字典。`FlowServiceImpl.getFlowTasks` 根据该值分发到不同的查询分支： 待办走 Flowable 任务表，已办走 t_flow_operation 表，我发起的走 t_flow_instance 表。

  所属流程环节：待办中心/任务列表查询环节。

  被谁调用：FlowApi.getFlowTasks（请求参数）、FlowServiceImpl.getFlowTasks（查询分发）。

  依赖什么：无（纯常量类）。

### `flow/src/main/java/cn/wisestar/server/flow/constant/FlowTaskType.java`
- 包: `cn.wisestar.server.flow.constant`
- 类型: `class FlowTaskType`
- **类说明**：
  流程任务类型常量。
  职责：定义流程中各类任务节点的类型标识，主要记录在 t_flow_operation.task_type （本次操作所属的任务类型）。当前业务中"用户任务"（userTask）是核心，抄送、 邮件、短信、HTTP 等任务类型为流程设计器预留能力。

  所属流程环节：审批处理环节。保存操作记录、查询已办列表、统计已办数量时， 通过该类型区分"人工审批操作"与其它自动任务。

  被谁调用：AbstractTaskHandler（操作记录落库）、FlowServiceImpl（已办列表、 已办统计、审核记录过滤）。

  依赖什么：无（纯常量类）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/ApprovalTaskRequest.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class ApprovalTaskRequest`
- 注解: @Data
- **类说明**：
  审批任务请求 DTO。
  职责：前端调用 `POST /workflow/approvalTask` 时提交的请求体， 聚合了审批操作所需的全部参数：审批类型（save/agree/refuse/rollback/revert）、 任务与流程实例定位信息、当前节点的表单答案、审批意见、附件、委托/驳回目标节点等。 后端 `FlowServiceImpl.approvalTask` 依据 `type` 将本对象分发给对应 TaskHandler 处理。

  所属流程环节：审批处理环节（发起、同意、拒绝、驳回、撤回、转交等全部操作的统一入参）。

  被谁调用：FlowApi.approvalTask（HTTP 反序列化）、FlowServiceImpl.approvalTask、 各 TaskHandler（AbstractTaskHandler 及其子类）。

  依赖什么：AnswerRequest（附件/答案更新请求，复用问卷模块的答案结构）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/FlowEntryNodeRequest.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class FlowEntryNodeRequest`
- 注解: @Data
- **类说明**：
  流程节点保存请求 DTO（流程设计器的单个节点配置）。
  职责：前端保存流程设计时，FlowEntryRequest#getNodes() 中每个节点的 配置载体。包含节点 ID（对应 BPMN XML 中的 activityId）、名称、类型、字段权限、 节点设置、授权用户与条件表达式。由 MapStruct 转换器 （`FlowEntryElementModelMapper`）转换为持久化实体 `FlowEntryNode` 后存入 t_flow_entry（nodes JSON 列）或 t_flow_entry_node 表。

  所属流程环节：流程设计环节（保存/部署时由前端提交）。

  被谁调用：FlowApi.saveFlow（HTTP 反序列化）、FlowServiceImpl.saveFlow、 FlowEntryElementModelMapper（MapStruct 转换）。

  依赖什么：FlowNodeSetting（节点行为设置）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/FlowEntryNodeView.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class FlowEntryNodeView`
- 注解: @Data
- **类说明**：
  流程节点展示视图 DTO。
  职责：向前端返回流程设计器中单个节点的完整配置（与保存请求 FlowEntryNodeRequest 结构基本对应，另含 flowId、flowType 展示字段）。 由 MapStruct 转换器（`FlowEntryElementModelMapper`）从持久化实体 `FlowEntryNode` 转换而来，供流程设计器回显、审核记录展示节点权限等使用。

  所属流程环节：流程设计环节（读取回显）与审批展示环节（节点信息查询）。

  被谁调用：FlowServiceImpl.getFlowEntry（组装节点列表返回前端）。

  依赖什么：FlowNodeSetting（节点行为设置）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/FlowEntryRequest.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class FlowEntryRequest`
- 注解: @Data
- **类说明**：
  流程设计保存请求 DTO。
  职责：前端调用 `POST /workflow/saveFlow` 保存流程设计时提交的请求体， 包含流程 BPMN XML、项目 ID 与节点配置列表。后端 `FlowServiceImpl.saveFlow` 将其保存到 t_flow_entry 表（bpmn_xml 与 nodes JSON 列）。

  所属流程环节：流程设计环节（草稿保存，不触发引擎部署）。

  被谁调用：FlowApi.saveFlow（HTTP 反序列化）、FlowServiceImpl.saveFlow。

  依赖什么：FlowEntryNodeRequest（节点配置列表）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/FlowEntryView.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class FlowEntryView`
- 注解: @Data
- **类说明**：
  流程定义展示视图 DTO。
  职责：向前端流程设计器返回某个问卷绑定的完整流程定义（BPMN XML、图标、节点列表）。 由 MapStruct 转换器（`FlowEntryModelMapper`）从实体 `FlowEntry` 转换而来； 若问卷尚未配置流程，返回仅含 projectId 的空视图。

  所属流程环节：流程设计环节（读取回显）。

  被谁调用：FlowServiceImpl.getFlowEntry（组装返回前端）。

  依赖什么：FlowEntryNodeView（节点视图列表）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/FlowNodeSetting.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class FlowNodeSetting`
- 注解: @Data
- **类说明**：
  流程节点行为设置 DTO。
  职责：描述某个审批节点"允许/禁止做什么"的行为配置，是节点配置 （FlowEntryNodeRequest / `FlowEntryNode`）的组成部分， 以 JSON 形式存储于 t_flow_entry_node.setting 列。前端流程设计器针对每个节点 配置这些开关项，审批详情页按配置展示操作按钮与流程日志。

  所属流程环节：贯穿流程设计（配置）与审批处理/详情展示（读取）环节。

  被谁调用：流程设计器（配置）、审批详情页（按配置渲染操作项）。当前引擎流转 逻辑中部分字段为预留配置，尚未参与运行时判断。

  依赖什么：无（纯数据结构）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/FlowOperationView.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class FlowOperationView`
- 注解: @Data
- **类说明**：
  审核（操作）记录展示视图 DTO。
  职责：向前端审批详情页返回一条完整的历史操作记录（谁在哪个节点做了什么操作、 意见是什么、当前等待谁来审批、抄送给了谁等），由 MapStruct 转换器 （`FlowOperationModelMapper`）从实体 `FlowOperation` 转换后， 再经 `FlowServiceImpl.getAuditRecord` 补充节点名称、审批人、待审批人、 抄送人等信息。

  所属流程环节：审批详情/审核记录展示环节。

  被谁调用：FlowServiceImpl.getAuditRecord（组装返回前端）。

  依赖什么：UserInfo（用户信息，含审批人/待审批人/抄送人）。
- 注入/字段: Date createAt

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/FlowStaticsView.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class FlowStaticsView`
- 注解: @Data
- **类说明**：
  任务统计视图 DTO。
  职责：向前端工作台/待办首页返回当前用户四类任务的数量统计，由 `FlowServiceImpl.statics` 组装：待办（Flowable 运行时任务）、已办 （t_flow_operation 用户任务）、抄送（预留）、我发起的（t_flow_instance 审批中实例）。

  所属流程环节：待办中心统计展示环节。

  被谁调用：FlowServiceImpl.statics（组装返回前端）。

  依赖什么：无（纯统计结果容器）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/FlowTaskQuery.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class FlowTaskQuery`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  任务列表查询参数 DTO。
  职责：前端调用 `GET /workflow/getFlowTasks` 时的查询参数，继承通用 分页查询 PageQuery（current / pageSize），额外携带任务查询类型、项目 ID、 状态与创建人过滤条件。`FlowServiceImpl.getFlowTasks` 根据 type 分发到 待办/已办/我发起的三个查询分支。

  所属流程环节：待办中心/任务列表查询环节。

  被谁调用：FlowApi.getFlowTasks（HTTP 参数绑定）、FlowServiceImpl.getFlowTasks 及其内部查询分支方法。

  依赖什么：PageQuery（分页基类）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/FlowTaskView.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class FlowTaskView`
- 注解: @Data
- **类说明**：
  任务列表展示视图 DTO。
  职责：向前端待办中心/任务列表返回单条任务的完整展示数据：任务定位信息 （任务 ID、流程实例 ID、答案 ID）、当前状态与审批阶段、按字段权限过滤后的表单 答案与附件、创建人/审批人信息等。由 `FlowServiceImpl.getFlowTasks` 的 三个查询分支组装，并经 setFlowTaskAnswer / setTaskStatus 补充答案、权限与状态。

  所属流程环节：待办中心/任务列表展示环节。

  被谁调用：FlowServiceImpl.getFlowTasks（组装返回前端）。

  依赖什么：UserInfo（用户信息）、FileView（附件）、 DeptView（部门信息，复用问卷模块的数据结构）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/RevokeView.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class RevokeView`
- 注解: @Data
- **类说明**：
  可回退节点展示视图 DTO。
  职责：向前端"驳回"对话框返回当前流程实例可回退的历史审批节点列表 （节点 ID + 节点名称），由 `RevertTaskHandler.getRevertNodes` 基于历史 操作记录构建的节点树（TaskTreeNode 链路）计算得出，并经 `FlowServiceImpl.getRevertNodes` 翻译节点名称后返回。

  所属流程环节：审批处理环节（驳回操作的前置节点选择）。

  被谁调用：FlowServiceImpl.getRevertNodes（组装返回前端）。

  依赖什么：无（纯展示结构）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/SchemaQuery.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class SchemaQuery`
- 注解: @Data
- **类说明**：
  加载问卷 schema（表单结构）的查询参数 DTO。
  职责：前端调用 `GET /workflow/loadSchema` 时的查询参数，用于定位 要加载的问卷结构以及按哪个审批节点的字段权限过滤。`FlowServiceImpl .loadSchemaByPermission` 据此加载项目问卷 schema 并应用节点字段权限。

  所属流程环节：审批处理/表单查看环节（审批人打开表单前的权限过滤）。

  被谁调用：FlowApi.loadSchema（HTTP 参数绑定）、FlowServiceImpl.loadSchemaByPermission。

  依赖什么：无（纯查询参数）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/dto/UpdateFlowOperationUserRequest.java`
- 包: `cn.wisestar.server.flow.domain.dto`
- 类型: `class UpdateFlowOperationUserRequest`
- 注解: @Data
- **类说明**：
  更新操作人（已办记录）最新状态的请求 DTO。
  职责：在保存操作记录时，将指定流程实例中某个用户的历史操作记录的 latest 标记 全部置为 false，用于保证"同一用户在同一个流程实例中只保留最新一条已办记录" （如果一个人参与了多个流程节点，只显示最近参与的流程节点）。由 `FlowOperationMapper.updateOperationUserLatest` 消费。

  所属流程环节：审批处理环节（操作记录落库时的已办去重）。

  被谁调用：AbstractTaskHandler.saveOperation（组装参数并调用 Mapper）。

  依赖什么：无（纯查询/更新参数）。

### `flow/src/main/java/cn/wisestar/server/flow/domain/handler/FlowEntryNodeTypeHandler.java`
- 包: `cn.wisestar.server.flow.domain.handler`
- 类型: `class FlowEntryNodeTypeHandler`
- 注解: @MappedTypes, @MappedJdbcTypes
- **类说明**：
  流程节点 JSON 类型处理器（MyBatis-Plus 自定义 TypeHandler）。
  职责：实现 t_flow_entry.nodes 列（存 JSON 字符串）与 Java 对象 `List` 之间的自动转换：

  - 读库时 #parse(String) 将 JSON 反序列化为节点列表；
  - 写库时 #toJson(List) 将节点列表序列化为 JSON 字符串。

  所属流程环节：流程设计/持久化环节。实体 `FlowEntry.nodes` 字段标注 了本处理器（@TableField(typeHandler = FlowEntryNodeTypeHandler.class)）， 使得"临时节点配置"可以随流程草稿一并 JSON 存储，无需单独建表。

  被谁调用：MyBatis-Plus 在读写 t_flow_entry.nodes 列时自动调用，无需业务代码直接调用。

  依赖什么：Spring 容器中的 Jackson ObjectMapper（通过 ContextHelper 获取，保证与全局序列化配置一致）。
- 方法:
  - `protected List<FlowEntryNode> parse(String json)`
    将数据库读取的 JSON 字符串反序列化为流程节点列表。
  - `protected String toJson(List<FlowEntryNode> obj)`
    将流程节点列表序列化为 JSON 字符串写入数据库。

### `flow/src/main/java/cn/wisestar/server/flow/domain/mapper/FlowEntryElementModelMapper.java`
- 包: `cn.wisestar.server.flow.domain.mapper`
- 类型: `interface FlowEntryElementModelMapper`
- 注解: @Mapper
- **类说明**：
  流程节点对象映射器（MapStruct）。
  职责：完成流程节点三类对象之间的自动转换：

  - 请求 DTO FlowEntryNodeRequest → 实体 FlowEntryNode（保存流程时落库）；
  - 实体 FlowEntryNode → 视图 DTO FlowEntryNodeView（查询流程时回显）。

  所属流程环节：流程设计环节（保存/读取节点配置）。

  被谁调用：FlowServiceImpl.saveFlow（fromRequest 保存节点）、 FlowServiceImpl.getFlowEntry（toView 回显节点）、FlowServiceImpl.deploy（保存节点）。

  依赖什么：BaseModelMapper（核心模块提供的通用转换接口），编译期由 MapStruct 生成转换实现，运行时作为 Spring Bean 注入。

### `flow/src/main/java/cn/wisestar/server/flow/domain/mapper/FlowEntryModelMapper.java`
- 包: `cn.wisestar.server.flow.domain.mapper`
- 类型: `interface FlowEntryModelMapper`
- 注解: @Mapper
- **类说明**：
  流程定义对象映射器（MapStruct）。
  职责：完成流程定义实体 FlowEntry 与展示视图 FlowEntryView 之间的自动转换（无请求 DTO，保存走实体直接落库）。

  所属流程环节：流程设计环节（读取流程定义回显）。

  被谁调用：FlowServiceImpl.getFlowEntry（实体转视图返回前端）。

  依赖什么：BaseModelMapper（核心模块提供的通用转换接口），编译期由 MapStruct 生成实现。

### `flow/src/main/java/cn/wisestar/server/flow/domain/mapper/FlowOperationModelMapper.java`
- 包: `cn.wisestar.server.flow.domain.mapper`
- 类型: `interface FlowOperationModelMapper`
- 注解: @Mapper
- **类说明**：
  流程操作对象映射器（MapStruct）。
  职责：完成操作记录实体 FlowOperation 与展示视图 FlowOperationView 之间的自动转换，供审核记录（审批历史）列表组装使用。

  所属流程环节：审批详情/审核记录展示环节。

  被谁调用：FlowServiceImpl.getAuditRecord（操作记录实体列表转视图列表）。

  依赖什么：BaseModelMapper（核心模块提供的通用转换接口），编译期由 MapStruct 生成实现。

### `flow/src/main/java/cn/wisestar/server/flow/domain/model/FlowEntry.java`
- 包: `cn.wisestar.server.flow.domain.model`
- 类型: `class FlowEntry`
- 注解: @TableName, @Data
- **类说明**：
  流程定义实体（对应表 t_flow_entry）。
  职责：存储某个问卷绑定的流程定义信息，包括流程 BPMN XML（bpmnXml）、 部署后的引擎定义 ID 与部署 ID、临时节点配置（nodes JSON）、发布状态等。 一份流程定义对应一个问卷（projectId 即流程定义 key），可被多次部署生成多个版本。

  所属流程环节：流程设计环节（保存草稿、部署发布、查询回显）。

  被谁调用：FlowEntryService 及其实现（CRUD）、FlowServiceImpl （保存/部署/查询）、AbstractTaskHandler（getFlowEntry 查询流程定义）、 FlowEntryNodeTypeHandler（nodes 字段 JSON 转换）。

  依赖什么：FlowEntryNodeTypeHandler（nodes 列 JSON 类型处理器）。
- 注入/字段: String id, String projectId, String processDefinitionId, String deployId, String bpmnXml, List<FlowEntryNode> nodes, String icon, Integer status, Date createAt, String createBy, Date updateAt, String updateBy, long serialVersionUID

### `flow/src/main/java/cn/wisestar/server/flow/domain/model/FlowEntryNode.java`
- 包: `cn.wisestar.server.flow.domain.model`
- 类型: `class FlowEntryNode`
- 注解: @TableName, @Data
- **类说明**：
  已发布的流程节点实体（对应表 t_flow_entry_node）。
  职责：存储某个流程定义部署后每个审批节点的持久化配置：审批人（identity）、 字段权限（fieldPermission）、节点行为设置（setting）、任务类型、条件表达式等。 节点 ID（id）与 BPMN XML 中的 activityId 一致，引擎运行到某节点时 （TaskHelper.getUsers、ActivityStartedListener、审批处理等）按节点 ID 读取配置。

  所属流程环节：流程部署环节（saveFlowElement 落库）与审批处理环节（读取配置）。

  被谁调用：FlowEntryNodeService 及其实现（CRUD）、FlowServiceImpl （部署保存节点/权限过滤/审核记录节点名翻译）、AbstractTaskHandler（答案合并权限）、 TaskHelper（计算审批人）、ActivityStartedListener（审批阶段名）、 RevertTaskHandler（驳回节点名）等。

  依赖什么：FlowNodeSetting（节点行为设置，Jackson JSON 存储）、 JacksonTypeHandler（field_permission / setting / identity 列 JSON 转换）。
- 注入/字段: String id, String name, String projectId, Integer taskType, Integer> fieldPermission, FlowNodeSetting setting, String[] identity, String expression, Date createAt, String createBy, Date updateAt, String updateBy, long serialVersionUID

### `flow/src/main/java/cn/wisestar/server/flow/domain/model/FlowEntryPublish.java`
- 包: `cn.wisestar.server.flow.domain.model`
- 类型: `class FlowEntryPublish`
- 注解: @TableName, @Data
- **类说明**：
  流程发布版本实体（对应表 t_flow_entry_publish）。
  职责：记录流程定义的每次发布版本：关联的引擎流程定义 ID、版本号、是否主版本、 是否激活、发布时间等。每次部署都会把旧版本标记为历史（mainVersion=false、 activeStatus=false），并新增一条当前主版本记录。

  所属流程环节：流程部署/发布环节（deploy 时生成与更新版本记录）。

  被谁调用：FlowEntryPublishService 及其实现（CRUD）、 FlowServiceImpl.deploy（更新旧版本、保存新版本）。

  依赖什么：无（纯实体，无特殊类型处理器）。
- 注入/字段: String id, String entryId, String processDefinitionId, Integer publishVersion, Boolean activeStatus, Boolean mainVersion, Date publishTime, Date createAt, String createBy, Date updateAt, String updateBy, long serialVersionUID

### `flow/src/main/java/cn/wisestar/server/flow/domain/model/FlowInstance.java`
- 包: `cn.wisestar.server.flow.domain.model`
- 类型: `class FlowInstance`
- 注解: @TableName, @Data
- **类说明**：
  流程实例实体（对应表 t_flow_instance）。
  职责：记录一次审批流程的运行实例：申请人、答案、当前状态、当前审批阶段等。 实例 ID 与 Flowable 流程实例 ID 完全一致（ProcessStartedListener 创建时直接使用 引擎的 processInstanceId），用于业务侧（任务列表、统计、审核记录）与引擎侧关联。

  所属流程环节：贯穿整个流程生命周期——发起时创建（ProcessStartedListener）， 流转中由各监听器维护状态（审批中/完善中/已拒绝/已结束），结束态由引擎事件同步。

  被谁调用：FlowInstanceService 及其实现（CRUD）、ProcessStartedListener （创建实例）、ActivityStartedListener / ProcessCompletedListener / ProcessCancelledListener / ProcessSuspendedListener（状态同步）、 FlowServiceImpl（我发起的列表/统计/审核记录状态判断）。

  依赖什么：无（纯实体，无特殊类型处理器）。
- 注入/字段: String id, String projectId, String answerId, Integer status, String approvalStage, Date createAt, String createBy, Date updateAt, String updateBy, long serialVersionUID

### `flow/src/main/java/cn/wisestar/server/flow/domain/model/FlowOperation.java`
- 包: `cn.wisestar.server.flow.domain.model`
- 类型: `class FlowOperation`
- 注解: @TableName, @Data
- **类说明**：
  流程操作记录实体（对应表 t_flow_operation）。
  职责：记录流程实例每次操作（发起保存、同意、拒绝、驳回、撤回等）的详细信息： 操作人、操作类型、所在节点、目标节点、审批意见、提交的表单答案、委托对象等。 是"已办事项"列表与"审核记录（审批历史）"的数据来源，并通过 latest 字段 标记每个实例的最新一条操作（仅最新操作人可执行撤回）。

  所属流程环节：审批处理环节（AbstractTaskHandler.saveOperation 落库）与 审批详情/已办列表展示环节（getAuditRecord / getFinished 查询）。

  被谁调用：FlowOperationService 及其实现（CRUD）、AbstractTaskHandler （保存操作记录、查询操作历史、构建节点树）、FlowServiceImpl（已办列表、审核记录、 已办统计）、RevertTaskHandler（撤回权限校验）。

  依赖什么：JacksonTypeHandler（answer 列 JSON 转换，存储节点提交的答案快照）。
- 注入/字段: String id, String instanceId, String projectId, String activityId, String newActivityId, String taskId, String answerId, String taskName, Integer taskType, String approvalType, String comment, String delegateAssignee, Object> answer, Date createAt, String createBy, Date updateAt, String updateBy, long serialVersionUID

### `flow/src/main/java/cn/wisestar/server/flow/domain/model/FlowOperationUser.java`
- 包: `cn.wisestar.server.flow.domain.model`
- 类型: `class FlowOperationUser`
- 注解: @TableName, @Data
- **类说明**：
  流程操作人关联实体（对应表 t_flow_operation_user）。
  职责：记录"某条操作记录由哪些用户参与处理"。主要用于已办事项的归属判断： 已办列表通过 exists 子查询（latest=1 且 user_id=当前用户）过滤出当前用户处理过 的操作；同时 latest 字段保证同一用户在同一实例中只显示最近参与的节点。

  所属流程环节：审批处理环节（saveOperationUser 落库、updateOperationUserLatest 置历史）与已办列表查询环节。

  被谁调用：FlowOperationUserService 及其实现（CRUD）、AbstractTaskHandler （保存操作人）、FlowOperationMapper.updateOperationUserLatest（置为历史）。

  依赖什么：无（纯实体，无特殊类型处理器）。
- 注入/字段: String id, String operationId, String userId, String groupId, String linkType, Boolean latest, Date createAt, String createBy, Date updateAt, String updateBy, long serialVersionUID

### `flow/src/main/java/cn/wisestar/server/flow/exception/FlowableRuntimeException.java`
- 包: `cn.wisestar.server.flow.exception`
- 类型: `class FlowableRuntimeException`
- **类说明**：
  流程模块自定义运行时异常。
  职责：包装流程处理过程中的业务异常（如"该问卷未设置流程"、"当前节点不能进行 驳回操作"、"流程部署失败"等），由全局异常处理器统一捕获并转为友好的 HTTP 错误响应。 继承 RuntimeException，因此不强制调用方显式声明捕获。

  所属流程环节：贯穿流程设计（部署）、审批处理（撤回校验）等环节的异常出口。

  被谁调用：FlowServiceImpl（部署/未设置流程校验）、RevertTaskHandler （撤回权限校验）、AbstractTaskHandler 及其子类。

  依赖什么：无（纯异常类，仅依赖 JDK）。
- 方法:
  - `public FlowableRuntimeException(String message)`
    构造仅含消息的异常。
  - `public FlowableRuntimeException(String message, Throwable cause)`
    构造含消息与根因的异常。

### `flow/src/main/java/cn/wisestar/server/flow/helper/TaskHelper.java`
- 包: `cn.wisestar.server.flow.helper`
- 类型: `class TaskHelper`
- 注解: @Component, @RequiredArgsConstructor
- **类说明**：
  任务辅助组件（BPMN 表达式 Bean，名称为 "t"）。
  职责：

  - #getUsers(String, String)：作为 BPMN XML 用户任务节点"审批人集合" 表达式（如 `${t.getUsers(execution.activityId, execution.starterUserId)}`） 的求值入口，根据节点配置的 identity（U:用户 / R:角色 / P:岗位）解析出最终审批人 ID 集合；
  - #condition(DelegateExecution)：条件判断占位实现（预留，当前为空）。

  所属流程环节：审批流转环节。引擎每次进入用户任务节点创建待办任务时调用 getUsers 计算该节点的审批人（候选者），是"流程如何流转"中确定下一审批人的关键一步。

  被谁调用：Flowable 引擎（BPMN 表达式求值）。

  依赖什么：FlowEntryNodeService（按节点 ID 读取节点配置）、 UserService（按角色/岗位组展开用户，getUsersByGroup）。
- 方法:
  - `public Set<String> getUsers(String activityId, String starterUserId)`
    根据 xml 节点计算审批人。
    内部逻辑：按 activityId 加载节点配置，遍历 identity 列表：

    - 格式为 "U:用户ID" → 直接取冒号后的用户 ID；
    - 格式为 "R:角色ID" / "P:岗位ID" → 调用 userService.getUsersByGroup 将该组下所有成员（并结合发起人过滤，如部门主管取申请人所在部门主管）展开为具体用户。
     结果使用 LinkedHashSet 保证有序且去重，作为引擎创建任务时的候选者集合。
  - `public void condition(DelegateExecution execution)`
    条件判断占位方法。
    预留扩展点：设计用于 BPMN 排他网关等条件表达式的求值（如根据表单值决定 流程走向），当前实现为空，未参与流程流转判断。

### `flow/src/main/java/cn/wisestar/server/flow/listener/ActivityStartedListener.java`
- 包: `cn.wisestar.server.flow.listener`
- 类型: `class ActivityStartedListener`
- 注解: @Slf4j
- **类说明**：
  活动节点开始事件监听器（ACTIVITY_STARTED）。
  职责：当流程执行进入某个活动节点时（每次流转都会触发），若进入的是用户任务 节点（userTask），则同步更新 t_flow_instance 表：把流程实例状态置为"审批中"、 把当前审批阶段更新为该节点的名称。这是"审批中"状态与"当前阶段"展示的数据来源。

  所属流程环节：审批流转环节。每次任务流转到新的审批节点时由 Flowable 引擎触发 （监听器注册见 cn.wisestar.server.flow.config.WorkflowConfig）。

  被谁调用：Flowable 引擎事件分发器（全局事件监听器，非业务代码直接调用）。

  依赖什么：ContextHelper（从 Spring 容器获取 Bean）、 FlowInstanceService（更新实例）、FlowEntryNodeService（按节点 ID 查询节点名称）。
- 方法:
  - `public void onEvent(FlowableEvent event)`
    事件回调：更新流程实例的当前审批阶段与状态。
    内部逻辑：

    - 仅处理 userTask 类型的活动事件（忽略开始事件、网关、服务任务等其它节点）；
    - 从事件中取出流程实例 ID 与活动节点 ID（taskDefKey）；
    - 按节点 ID 查询节点配置，取节点名称作为审批阶段名，查询不到时回退为 "审批中"；
    - 组装 FlowInstance（仅含 id、approvalStage、status 三个字段）执行局部更新。
  - `public boolean isFailOnException()`
    监听器内部异常是否向外抛出：true 表示事件处理失败时中断引擎事务
  - `public boolean isFireOnTransactionLifecycleEvent()`
    是否在事务生命周期事件（提交/回滚）时触发：false 表示仅在业务事件发生时触发
  - `public String getOnTransaction()`
    指定在事务的哪个阶段触发（commit/rollback/complete）：null 表示不限制

### `flow/src/main/java/cn/wisestar/server/flow/listener/ProcessCancelledListener.java`
- 包: `cn.wisestar.server.flow.listener`
- 类型: `class ProcessCancelledListener`
- 注解: @Slf4j
- **类说明**：
  流程实例取消（删除）事件监听器（PROCESS_CANCELLED）。
  职责：当流程实例被删除（审批人"拒绝"时 RefuseTaskHandler 调用 deleteProcessInstance 会触发该事件）时，同步更新 t_flow_instance 表： 把实例状态置为"已拒绝"、审批阶段置为"已拒绝"。

  所属流程环节：审批处理环节的"拒绝"分支，是"流程到哪结束"的终止态之一。 （监听器注册见 cn.wisestar.server.flow.config.WorkflowConfig。）

  被谁调用：Flowable 引擎事件分发器（全局事件监听器）。

  依赖什么：ContextHelper（获取 Bean）、FlowInstanceService（更新实例）。
- 方法:
  - `public void onEvent(FlowableEvent event)`
    事件回调：将流程实例状态同步为"已拒绝"。
    内部逻辑：从事件中取出流程实例 ID，组装仅含 id、status、approvalStage 三个字段的 FlowInstance 执行局部更新，审批阶段显示为"已拒绝"。
  - `public boolean isFailOnException()`
    监听器内部异常是否向外抛出：false 表示事件处理失败不影响引擎主流程
  - `public boolean isFireOnTransactionLifecycleEvent()`
    是否在事务生命周期事件时触发：false 表示仅在业务事件发生时触发
  - `public String getOnTransaction()`
    指定在事务的哪个阶段触发：null 表示不限制

### `flow/src/main/java/cn/wisestar/server/flow/listener/ProcessCompletedListener.java`
- 包: `cn.wisestar.server.flow.listener`
- 类型: `class ProcessCompletedListener`
- 注解: @Slf4j
- **类说明**：
  流程实例完成事件监听器（PROCESS_COMPLETED）。
  职责：当流程走完最后一个节点正常结束时（最后节点同意后 Flowable 触发 PROCESS_COMPLETED 事件），同步更新 t_flow_instance 表：把实例状态置为 "已结束"、审批阶段置为"已结束"。这是流程"正常到终点"的终止态。

  所属流程环节：审批流转环节的"同意-流程结束"分支。 （监听器注册见 cn.wisestar.server.flow.config.WorkflowConfig。）

  被谁调用：Flowable 引擎事件分发器（全局事件监听器）。

  依赖什么：ContextHelper（获取 Bean）、FlowInstanceService（更新实例）。
- 方法:
  - `public void onEvent(FlowableEvent event)`
    事件回调：将流程实例状态同步为"已结束"。
    内部逻辑：从事件中取出流程实例 ID，组装仅含 id、status、approvalStage 三个字段的 FlowInstance 执行局部更新，审批阶段显示为"已结束"。
  - `public boolean isFailOnException()`
    监听器内部异常是否向外抛出：false 表示事件处理失败不影响引擎主流程
  - `public boolean isFireOnTransactionLifecycleEvent()`
    是否在事务生命周期事件时触发：false 表示仅在业务事件发生时触发
  - `public String getOnTransaction()`
    指定在事务的哪个阶段触发：null 表示不限制

### `flow/src/main/java/cn/wisestar/server/flow/listener/ProcessStartedListener.java`
- 包: `cn.wisestar.server.flow.listener`
- 类型: `class ProcessStartedListener`
- 注解: @Slf4j
- **类说明**：
  流程实例启动事件监听器（PROCESS_STARTED）。
  职责：当申请人发起流程（SaveTaskHandler 调用 startProcessInstanceByKey）时， 同步创建 t_flow_instance 流程实例记录：实例 ID 取引擎的 processInstanceId、 状态置为"审批中"、记录申请人（当前登录用户）与答案 ID、项目 ID。该记录是 "我发起的"任务列表与统计的数据来源。

  所属流程环节：流程发起环节，是整个流程生命周期的起点。 （监听器注册见 cn.wisestar.server.flow.config.WorkflowConfig。）

  被谁调用：Flowable 引擎事件分发器（全局事件监听器）。

  依赖什么：ContextHelper（获取 Bean）、SecurityContextUtils （获取当前登录用户）、FlowConstant（流程变量名）、FlowInstanceService（保存实例）。
- 方法:
  - `public void onEvent(FlowableEvent event)`
    事件回调：创建流程实例记录。
    内部逻辑：

    - 从启动事件中取流程变量 answerId（发起时 SaveTaskHandler 放入）；
    - 从执行实体中取流程定义 key 作为 projectId、取 processInstanceId 作为实例 ID；
    - 组装 FlowInstance（状态"审批中"、创建人取当前登录用户）并入库。
  - `public boolean isFailOnException()`
    监听器内部异常是否向外抛出：false 表示事件处理失败不影响引擎主流程
  - `public boolean isFireOnTransactionLifecycleEvent()`
    是否在事务生命周期事件时触发：false 表示仅在业务事件发生时触发
  - `public String getOnTransaction()`
    指定在事务的哪个阶段触发：null 表示不限制

### `flow/src/main/java/cn/wisestar/server/flow/listener/ProcessSuspendedListener.java`
- 包: `cn.wisestar.server.flow.listener`
- 类型: `class ProcessSuspendedListener`
- **类说明**：
  流程实例挂起/激活事件监听器（ENTITY_SUSPENDED / ENTITY_ACTIVATED）。
  职责：当流程实例被挂起或激活时，同步更新 t_flow_instance 表状态为"申请人 完善中"。业务场景：发起人"撤回"任务时（RevertTaskHandler.rollbackToStartEvent 调用 suspendProcessInstanceById），实例被挂起等待申请人完善表单；此时"我发起的" 列表应展示"完善中"状态。

  所属流程环节：流程流转环节的"撤回/完善"分支。 （监听器注册见 cn.wisestar.server.flow.config.WorkflowConfig， 挂起与激活事件都挂载本监听器，统一置为"申请人完善中"。）

  被谁调用：Flowable 引擎事件分发器（全局事件监听器）。

  依赖什么：ContextHelper（获取 Bean）、FlowInstanceService（更新实例）。
- 方法:
  - `public void onEvent(FlowableEvent event)`
    事件回调：将流程实例状态同步为"申请人完善中"。
    内部逻辑：从事件中取出流程实例 ID，组装仅含 id、status、approvalStage 三个字段的 FlowInstance 执行局部更新，审批阶段显示为"申请人完善中"。
  - `public boolean isFailOnException()`
    监听器内部异常是否向外抛出：false 表示事件处理失败不影响引擎主流程
  - `public boolean isFireOnTransactionLifecycleEvent()`
    是否在事务生命周期事件时触发：false 表示仅在业务事件发生时触发
  - `public String getOnTransaction()`
    指定在事务的哪个阶段触发：null 表示不限制

### `flow/src/main/java/cn/wisestar/server/flow/mapper/FlowEntryMapper.java`
- 包: `cn.wisestar.server.flow.mapper`
- 类型: `interface FlowEntryMapper`
- **类说明**：
  流程定义 Mapper 接口（对应表 t_flow_entry）。
  职责：为 FlowEntry 实体提供 MyBatis-Plus 通用 CRUD 能力 （继承 BaseMapper），无需手写 SQL；如需扩展复杂查询可在此补充方法。 由 cn.wisestar.server.flow.config.WorkflowConfig 中的

### `flow/src/main/java/cn/wisestar/server/flow/mapper/FlowEntryNodeMapper.java`
- 包: `cn.wisestar.server.flow.mapper`
- 类型: `interface FlowEntryNodeMapper`
- **类说明**：
  流程节点 Mapper 接口（对应表 t_flow_entry_node）。
  职责：为 FlowEntryNode 实体提供 MyBatis-Plus 通用 CRUD 能力 （继承 BaseMapper），无需手写 SQL；如需扩展复杂查询可在此补充方法。 由 cn.wisestar.server.flow.config.WorkflowConfig 中的

### `flow/src/main/java/cn/wisestar/server/flow/mapper/FlowEntryPublishMapper.java`
- 包: `cn.wisestar.server.flow.mapper`
- 类型: `interface FlowEntryPublishMapper`
- **类说明**：
  流程发布版本 Mapper 接口（对应表 t_flow_entry_publish）。
  职责：为 FlowEntryPublish 实体提供 MyBatis-Plus 通用 CRUD 能力 （继承 BaseMapper），无需手写 SQL；如需扩展复杂查询可在此补充方法。 由 cn.wisestar.server.flow.config.WorkflowConfig 中的

### `flow/src/main/java/cn/wisestar/server/flow/mapper/FlowInstanceMapper.java`
- 包: `cn.wisestar.server.flow.mapper`
- 类型: `interface FlowInstanceMapper`
- **类说明**：
  流程实例 Mapper 接口（对应表 t_flow_instance）。
  职责：为 FlowInstance 实体提供 MyBatis-Plus 通用 CRUD 能力 （继承 BaseMapper），无需手写 SQL；如需扩展复杂查询可在此补充方法。 由 cn.wisestar.server.flow.config.WorkflowConfig 中的

### `flow/src/main/java/cn/wisestar/server/flow/mapper/FlowOperationMapper.java`
- 包: `cn.wisestar.server.flow.mapper`
- 类型: `interface FlowOperationMapper`
- **类说明**：
  流程操作记录 Mapper 接口（对应表 t_flow_operation）。
  职责：为 FlowOperation 实体提供 MyBatis-Plus 通用 CRUD 能力 （继承 BaseMapper），并额外提供两个自定义更新方法（对应 XML 中的 SQL）：

  - #updateOperationLatest(String)：将某实例全部操作的 latest 置为 false，
  - #updateOperationUserLatest(UpdateFlowOperationUserRequest)：将某用户在某 实例中的操作人记录置为历史。
   由 cn.wisestar.server.flow.config.WorkflowConfig 中的 @MapperScan 扫描注册。
  所属流程环节：审批处理环节（操作记录落库与 latest 标记维护）。

  被谁调用：AbstractTaskHandler.saveOperation（保存操作记录前调用两个自定义方法）。

  依赖什么：FlowOperation 实体、UpdateFlowOperationUserRequest（更新参数）。

### `flow/src/main/java/cn/wisestar/server/flow/mapper/FlowOperationUserMapper.java`
- 包: `cn.wisestar.server.flow.mapper`
- 类型: `interface FlowOperationUserMapper`
- **类说明**：
  流程操作人 Mapper 接口（对应表 t_flow_operation_user）。
  职责：为 FlowOperationUser 实体提供 MyBatis-Plus 通用 CRUD 能力 （继承 BaseMapper），无需手写 SQL；如需扩展复杂查询可在此补充方法。 由 cn.wisestar.server.flow.config.WorkflowConfig 中的

### `flow/src/main/java/cn/wisestar/server/flow/service/FlowEntryNodeService.java`
- 包: `cn.wisestar.server.flow.service`
- 类型: `interface FlowEntryNodeService`
- **类说明**：
  流程节点服务接口。
  职责：为 FlowEntryNode（已发布流程节点配置）提供 MyBatis-Plus 通用服务能力（继承 IService，包含 CRUD、批量操作、Lambda 查询等）， 当前无额外业务方法，如需扩展节点相关逻辑可在此声明。

  所属流程环节：流程部署环节（节点落库）与审批处理环节（节点配置读取）。

  被谁调用：FlowServiceImpl（部署保存节点、权限过滤、节点名翻译）、 AbstractTaskHandler（答案合并权限）、TaskHelper（计算审批人）、 ActivityStartedListener（审批阶段名）、RevertTaskHandler（驳回节点名）等。

  依赖什么：FlowEntryNode 实体；实现见 `FlowEntryNodeServiceImpl`。

### `flow/src/main/java/cn/wisestar/server/flow/service/FlowEntryPublishService.java`
- 包: `cn.wisestar.server.flow.service`
- 类型: `interface FlowEntryPublishService`
- **类说明**：
  流程发布版本服务接口。
  职责：为 FlowEntryPublish（流程发布版本记录）提供 MyBatis-Plus 通用服务能力（继承 IService），当前无额外业务方法，如需扩展版本管理逻辑 （如查询主版本、历史版本清理等）可在此声明。

  所属流程环节：流程部署/发布环节（新旧版本切换）。

  被谁调用：FlowServiceImpl.deploy（更新旧版本、保存新版本）。

  依赖什么：FlowEntryPublish 实体；实现见 `FlowEntryPublishServiceImpl`。

### `flow/src/main/java/cn/wisestar/server/flow/service/FlowEntryService.java`
- 包: `cn.wisestar.server.flow.service`
- 类型: `interface FlowEntryService`
- **类说明**：
  流程定义服务接口。
  职责：为 FlowEntry（流程定义/草稿）提供 MyBatis-Plus 通用服务能力 （继承 IService），当前无额外业务方法，如需扩展流程定义相关逻辑可在此声明。

  所属流程环节：流程设计环节（保存草稿、部署、查询）。

  被谁调用：FlowServiceImpl（保存/部署/查询流程定义）、 AbstractTaskHandler（getFlowEntry 查询流程定义）。

  依赖什么：FlowEntry 实体；实现见 `FlowEntryServiceImpl`。

### `flow/src/main/java/cn/wisestar/server/flow/service/FlowInstanceService.java`
- 包: `cn.wisestar.server.flow.service`
- 类型: `interface FlowInstanceService`
- **类说明**：
  流程实例服务接口。
  职责：为 FlowInstance（流程实例）提供 MyBatis-Plus 通用服务能力 （继承 IService），当前无额外业务方法，如需扩展实例管理逻辑（如按状态统计、 实例生命周期查询等）可在此声明。

  所属流程环节：贯穿流程全生命周期——发起时创建、流转中由监听器更新状态、 "我发起的"列表与统计查询。

  被谁调用：ProcessStartedListener（创建实例）、ActivityStartedListener / ProcessCompletedListener / ProcessCancelledListener / ProcessSuspendedListener （状态同步）、FlowServiceImpl（列表/统计/审核记录）。

  依赖什么：FlowInstance 实体；实现见 `FlowInstanceServiceImpl`。

### `flow/src/main/java/cn/wisestar/server/flow/service/FlowOperationService.java`
- 包: `cn.wisestar.server.flow.service`
- 类型: `interface FlowOperationService`
- **类说明**：
  流程操作记录服务接口。
  职责：为 FlowOperation（操作记录）提供 MyBatis-Plus 通用服务能力 （继承 IService），当前无额外业务方法，如需扩展操作记录查询逻辑 （如按实例查历史、按用户查已办等）可在此声明。

  所属流程环节：审批处理环节（操作记录落库）与审批详情/已办列表展示环节。

  被谁调用：AbstractTaskHandler（保存操作记录、查询历史、构建节点树）、 FlowServiceImpl（已办列表、审核记录、已办统计）。

  依赖什么：FlowOperation 实体；实现见 `FlowOperationServiceImpl`。

### `flow/src/main/java/cn/wisestar/server/flow/service/FlowOperationUserService.java`
- 包: `cn.wisestar.server.flow.service`
- 类型: `interface FlowOperationUserService`
- **类说明**：
  流程操作人服务接口。
  职责：为 FlowOperationUser（操作人关联记录）提供 MyBatis-Plus 通用服务能力（继承 IService），当前无额外业务方法，如需扩展操作人查询逻辑 可在此声明。

  所属流程环节：审批处理环节（保存操作人）与已办列表查询环节（归属判断）。

  被谁调用：AbstractTaskHandler.saveOperationUser（保存操作人记录）。

  依赖什么：FlowOperationUser 实体；实现见 `FlowOperationUserServiceImpl`。

### `flow/src/main/java/cn/wisestar/server/flow/service/FlowService.java`
- 包: `cn.wisestar.server.flow.service`
- 类型: `interface FlowService`
- **类说明**：
  工作流业务门面接口。
  职责：定义流程模块对外暴露的全部业务能力，是 FlowApi 的唯一业务依赖： 流程设计（保存/查询）、流程部署、任务查询（待办/已办/我发起的）、审批操作 （审批/驳回/撤回分发）、表单 schema 权限过滤、审核记录查询、回退节点查询、任务统计， 以及发起问卷前的权限预检。实现见 `FlowServiceImpl`。

  所属流程环节：流程模块的总入口服务，串联"设计 → 发布 → 发起 → 流转 → 结束"全链路。

  被谁调用：FlowApi（HTTP 层）、问卷提交前的权限校验（beforeLaunchProcess）。

  依赖什么：flow 包下全部 DTO 与核心模块的通用结构（PaginationResponse、 SurveySchema、PublicProjectView）。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/FlowEntryNodeServiceImpl.java`
- 包: `cn.wisestar.server.flow.service.impl`
- 类型: `class FlowEntryNodeServiceImpl`
- 注解: @Service
- **类说明**：
  流程节点服务实现。
  职责：基于 MyBatis-Plus ServiceImpl 为 FlowEntryNode（已发布 流程节点配置）提供通用 CRUD 实现，绑定 FlowEntryNodeMapper；当前无额外 业务逻辑，是 FlowEntryNodeService 的默认实现，由 Spring 注入给 FlowServiceImpl、TaskHelper、各监听器与 TaskHandler 等调用方。

  所属流程环节：流程部署环节（节点落库）与审批处理环节（节点配置读取）。

  被谁调用：FlowServiceImpl、AbstractTaskHandler、TaskHelper、ActivityStartedListener 等。

  依赖什么：FlowEntryNodeMapper（数据访问层）。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/FlowEntryPublishServiceImpl.java`
- 包: `cn.wisestar.server.flow.service.impl`
- 类型: `class FlowEntryPublishServiceImpl`
- 注解: @Service
- **类说明**：
  流程发布版本服务实现。
  职责：基于 MyBatis-Plus ServiceImpl 为 FlowEntryPublish （流程发布版本记录）提供通用 CRUD 实现，绑定 FlowEntryPublishMapper； 当前无额外业务逻辑，是 FlowEntryPublishService 的默认实现，由 Spring 注入给 FlowServiceImpl 使用。

  所属流程环节：流程部署/发布环节（新旧版本切换）。

  被谁调用：FlowServiceImpl.deploy（更新旧版本、保存新版本）。

  依赖什么：FlowEntryPublishMapper（数据访问层）。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/FlowEntryServiceImpl.java`
- 包: `cn.wisestar.server.flow.service.impl`
- 类型: `class FlowEntryServiceImpl`
- 注解: @Service
- **类说明**：
  流程定义服务实现。
  职责：基于 MyBatis-Plus ServiceImpl 为 FlowEntry（流程定义） 提供通用 CRUD 实现，绑定 FlowEntryMapper；当前无额外业务逻辑，是 FlowEntryService 的默认实现，由 Spring 注入给 FlowServiceImpl 等调用方。

  所属流程环节：流程设计环节（保存草稿、部署、查询）。

  被谁调用：FlowServiceImpl、AbstractTaskHandler（通过 FlowEntryService 接口注入）。

  依赖什么：FlowEntryMapper（数据访问层）。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/FlowInstanceServiceImpl.java`
- 包: `cn.wisestar.server.flow.service.impl`
- 类型: `class FlowInstanceServiceImpl`
- 注解: @Service
- **类说明**：
  流程实例服务实现。
  职责：基于 MyBatis-Plus ServiceImpl 为 FlowInstance（流程实例） 提供通用 CRUD 实现，绑定 FlowInstanceMapper；当前无额外业务逻辑，是 FlowInstanceService 的默认实现，由 Spring 注入给各流程监听器与 FlowServiceImpl 等调用方。

  所属流程环节：贯穿流程全生命周期——发起时创建、流转中状态同步、列表与统计查询。

  被谁调用：ProcessStartedListener、ActivityStartedListener、ProcessCompletedListener、 ProcessCancelledListener、ProcessSuspendedListener、FlowServiceImpl。

  依赖什么：FlowInstanceMapper（数据访问层）。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/FlowOperationServiceImpl.java`
- 包: `cn.wisestar.server.flow.service.impl`
- 类型: `class FlowOperationServiceImpl`
- 注解: @Service
- **类说明**：
  流程操作记录服务实现。
  职责：基于 MyBatis-Plus ServiceImpl 为 FlowOperation（操作记录） 提供通用 CRUD 实现，绑定 FlowOperationMapper；当前无额外业务逻辑，是 FlowOperationService 的默认实现，由 Spring 注入给 AbstractTaskHandler 与 FlowServiceImpl 等调用方（自定义 Mapper 方法通过 getBaseMapper() 获取调用）。

  所属流程环节：审批处理环节（操作记录落库）与审批详情/已办列表展示环节。

  被谁调用：AbstractTaskHandler、FlowServiceImpl。

  依赖什么：FlowOperationMapper（数据访问层，含 latest 标记更新 SQL）。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/FlowOperationUserServiceImpl.java`
- 包: `cn.wisestar.server.flow.service.impl`
- 类型: `class FlowOperationUserServiceImpl`
- 注解: @Service
- **类说明**：
  流程操作人服务实现。
  职责：基于 MyBatis-Plus ServiceImpl 为 FlowOperationUser （操作人关联记录）提供通用 CRUD 实现，绑定 FlowOperationUserMapper； 当前无额外业务逻辑，是 FlowOperationUserService 的默认实现，由 Spring 注入给 AbstractTaskHandler 使用。

  所属流程环节：审批处理环节（保存操作人记录）。

  被谁调用：AbstractTaskHandler.saveOperationUser。

  依赖什么：FlowOperationUserMapper（数据访问层）。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/FlowServiceImpl.java`
- 包: `cn.wisestar.server.flow.service.impl`
- 类型: `class FlowServiceImpl`
- 注解: @Service, @RequiredArgsConstructor, @Transactional
- **类说明**：
  工作流业务门面实现（核心服务类）。
  职责：实现 FlowService 定义的全部业务能力，是流程模块的大脑：

  - **流程设计**：saveFlow 保存草稿、getFlowEntry 查询回显；
  - **流程部署**：deploy 将 BPMN XML 部署到 Flowable 引擎并同步节点配置与版本；
  - **发起预检**：beforeLaunchProcess 校验申请人是否有权发起并按字段权限过滤问卷；
  - **审批处理**：approvalTask 按 type 分发到对应 TaskHandler 执行流转；
  - **任务查询**：getFlowTasks 按待办/已办/我发起的三种口径分页查询；
  - **展示查询**：loadSchemaByPermission 权限过滤 schema、getAuditRecord 审核记录、 getRevertNodes 可回退节点、statics 任务统计。

  流程机制总览（流程从哪开始、如何流转、到哪结束）：

  - **发起**：用户填写问卷提交 → SaveTaskHandler 启动流程实例（同时 ProcessStartedListener 创建 t_flow_instance）；
  - **流转**：引擎进入用户任务节点 → TaskHelper.getUsers 计算审批人创建待办， ActivityStartedListener 更新审批阶段；审批人通过 approvalTask 操作，按 type 分发（同意→下一节点/流程结束、拒绝→删除实例、驳回→跳到指定节点、撤回→挂起回发起节点）；
  - **结束**：最后节点同意 → ProcessCompletedListener 置"已结束"；拒绝 → ProcessCancelledListener 置"已拒绝"；撤回 → ProcessSuspendedListener 置"完善中"， 申请人重新提交后再次激活实例继续流转。

  被谁调用：cn.wisestar.server.flow.api.FlowApi（HTTP 层）以及问卷 提交前的权限校验逻辑。

  依赖什么：Flowable 引擎服务（RepositoryService / RuntimeService / TaskService）、 本模块各实体 Service（FlowEntryService / FlowEntryNodeService / FlowEntryPublishService / FlowInstanceService / FlowOperationService）、核心模块服务（UserService / AnswerService / ProjectService）、MapStruct 转换器（FlowEntryModelMapper / FlowEntryElementModelMapper / FlowOperationModelMapper）、RevertTaskHandler（回退节点计算）。
- 方法:
  - `public void beforeLaunchProcess(PublicProjectView projectView)`
    问卷开始之前，根据权限过滤字段。
    流程环节：发起预检。用户在问卷页开始填表前由问卷模块调用，判断当前问卷 是否绑定流程以及当前用户是否有权发起，并按发起节点的字段权限过滤问卷结构：

    - 问卷未绑定流程（无节点配置）→ 直接放行；
    - 发起节点未配置审批人（identity 为空）→ 仅按字段权限过滤 schema 放行；
    - 开启工作流（节点配置了审批人）但用户未登录 → 标记 loginRequired 并置空 survey，阻止作答；
    - 用户已登录：若其用户组包含发起节点的任一授权身份 → 按字段权限过滤 schema 放行；
    - 否则视为无发起权限 → 置空 survey，前端提示无权发起。
  - `public void saveFlow(FlowEntryRequest request)`
    保存流程设计（草稿态）。
    流程环节：流程设计保存。将前端提交的 BPMN XML 与节点配置保存到 t_flow_entry（bpmn_xml 与 nodes JSON 列）：首次保存则新建，已存在则覆盖更新。 注意此时节点仅以 JSON 形式存于流程定义的 nodes 列，尚未拆解到 t_flow_entry_node 表（该步骤在 deploy 部署时执行）。
  - `public void deploy(String projectId)`
    部署流程设计（发布为新版本）。
    流程环节：流程发布。完整流程：

    - 校验问卷是否已配置流程（无则抛异常）；
    - 将 BPMN XML 解析为 BpmnModel，调用 Flowable RepositoryService 部署 （资源名 = 流程记录 ID + ".bpmn20.xml"），拿到部署 ID 与流程定义；
    - 回填 flowEntry 的 deployId / processDefinitionId，并置 status=已发布；
    - 将旧版本发布记录全部置为历史版本（mainVersion=false、activeStatus=false）；
    - 写入新的发布版本记录（id=部署 ID、版本号=引擎版本号、主版本=true）；
    - 将流程定义的 nodes（草稿节点）拆解保存到 t_flow_entry_node 表（saveFlowElement）。
     任何异常统一包装为 FlowableRuntimeException 抛出（事务回滚）。
  - `public FlowEntryView getFlowEntry(String projectId)`
    获取流程定义信息（设计器回显）。
    流程环节：流程设计读取。按 projectId 查询流程定义，经 MapStruct 转为 FlowEntryView 并补充节点视图列表；问卷尚未配置流程时返回仅含 projectId 的空视图。
  - `public void approvalTask(ApprovalTaskRequest request)`
    审批任务：按类型分发到对应 TaskHandler 执行。
    流程环节：审批处理的总分发入口。根据请求的 type（save/agree/refuse/ rollback/revert）从 Spring 容器按 Bean 名（type + "TaskHandler"）获取处理器 并执行。各处理器都继承 AbstractTaskHandler，统一在 process 模板方法中 完成"引擎流转 + 操作记录落库 + 答案更新"。
  - `public PaginationResponse<FlowTaskView> getFlowTasks(FlowTaskQuery query)`
    分页获取当前用户的任务列表。
    流程环节：待办中心。按查询类型分发：

    - todo（我的待办）→ #getTodo：查 Flowable 运行时任务（候选人或指派人）；
    - finished（已办事项）→ #getFinished：查 t_flow_operation 中 latest=1 且操作人=当前用户 的操作记录；
    - selfCreated（我发起的）→ #getSelfCreated：查 t_flow_instance。
     查询后统一补充表单答案（按字段权限过滤）与任务状态信息。
  - `public SurveySchema loadSchemaByPermission(SchemaQuery query)`
    按当前任务节点的字段权限过滤问卷 schema。
    流程环节：审批表单加载。审批人打开表单时调用：先加载项目问卷结构，再按 任务节点（taskDefKey）配置的字段权限过滤（隐藏/只读/可编辑），使审批人只能 看到并编辑被授权的字段。

    数据链路：Controller → loadSchemaByPermission → ProjectService.getProject （加载问卷）+ FlowEntryNodeService.getById（加载节点权限）→ SchemaHelper 过滤 → 返回前端。
  - `public List<FlowOperationView> getAuditRecord(String processInstanceId)`
    获取流程实例的审核记录（审批历史）。
    流程环节：审批详情。完整逻辑：

    - 查询该实例全部用户任务操作记录（按创建时间正序）；
    - 若实例处于"审批中"：查询当前唯一子执行所在的节点，动态追加一条 "待审批（todo）"记录，并组装等待审批的用户列表（任务指派人 + 候选者）；
    - 若实例处于"申请人完善中"：追加一条发起节点（starter）的待审批记录， 等待人即申请人；
    - 为每条记录补充：操作人信息（auditUser）、节点名称（activityName / newActivityName）、审批类型中文名；save 类型且无节点名时显示"申请人"。
  - `public List<RevokeView> getRevertNodes(String processInstanceId)`
    获取当前实例可回退的节点列表。
    流程环节：驳回操作前置查询。委托 RevertTaskHandler 基于历史操作 记录构建节点树并取当前节点父链路，再逐个翻译节点名称后返回。
  - `public FlowStaticsView statics()`
    获取当前用户的任务统计数量。
    流程环节：待办中心统计。分别统计：

    - todo：当前用户作为候选者或指派人的活跃任务数（Flowable 任务表）；
    - finished：全局用户任务操作记录数（排除保存类型；当前统计口径未按用户过滤）；
    - selfCreated：当前用户发起且处于审批中的流程实例数；
    - copyTo：预留，未计算（保持 0）。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/taskHandler/AbstractTaskHandler.java`
- 包: `cn.wisestar.server.flow.service.impl.taskHandler`
- 类型: `class AbstractTaskHandler`
- **类说明**：
  审批任务处理器抽象基类。
  职责：为所有具体 TaskHandler（同意/拒绝/驳回/撤回/保存）提供公共能力与统一 的处理模板：

  - **处理模板** #process：执行子类 #innerProcess 完成引擎侧 任务流转，成功后统一落库操作记录（saveOperation）并更新表单答案（updateTaskAnswer）；
  - **答案合并** #mergeAnswer：按字段权限合并当前节点提交的答案与原始答案；
  - **操作记录维护** #saveOperation / #saveOperationUser： 维护 t_flow_operation 与 t_flow_operation_user 的 latest 标记与记录写入；
  - **查询工具**：当前任务、历史活动、活跃任务、操作历史、流程定义、开始节点等查询；
  - **撤回/回退支持** #rollbackToStartEvent（撤回至发起节点）、 #getHistoricTree（构建操作节点树）与内部类 TaskTreeNode。

  所属流程环节：审批处理环节的公共服务层，被全部 5 个具体处理器继承。

  被谁调用：AgreeTaskHandler / RefuseTaskHandler / RollbackTaskHandler / RevertTaskHandler / SaveTaskHandler（继承调用模板方法），RevertTaskHandler 的节点树逻辑亦供 FlowServiceImpl 使用。

  依赖什么：Flowable 引擎服务（TaskService / RuntimeService / HistoryService / RepositoryService）、各实体 Service（AnswerService / UserService / FlowInstanceService / FlowEntryNodeService / FlowEntryService / FlowOperationService / FlowOperationUserService）、 FlowOperationMapper（latest 标记更新 SQL）。
- 注入/字段: AnswerService answerService, TaskService taskService, RuntimeService runtimeService, UserService userService, FlowInstanceService flowInstanceService, FlowEntryNodeService entryNodeService, FlowEntryService entryService, FlowOperationService flowOperationService, HistoryService historyService, FlowOperationUserService flowOperationUserService, RepositoryService repositoryService
- 方法:
  - `public abstract boolean innerProcess(ApprovalTaskRequest request)`
    执行引擎侧的任务流转（子类实现）。
    由各具体处理器实现：保存→启动/重新激活流程；同意→完成当前任务；拒绝→删除 流程实例；驳回→移动当前活动到目标节点；撤回→移动/挂起到发起节点。
  - `public void process(ApprovalTaskRequest request)`
    审批处理模板方法。
    内部逻辑（流程从请求到落库的完整链路）：

    - 调用子类 #innerProcess(request) 执行 Flowable 引擎侧流转 （启动实例/完成任务/删实例/移动节点/挂起实例等）；
    - 流转失败（返回 false）则直接返回，不产生操作记录；
    - 流转成功 → #saveOperation 落库操作记录（先置旧记录 latest=false， 再写入新记录与操作人记录）；
    - #updateTaskAnswer 按字段权限合并本次提交的答案并更新表单答案。
  - `protected LinkedHashMap mergeAnswer(LinkedHashMap target, LinkedHashMap source, LinkedHashMap<String, Integer> fieldPermission)`
    合并当前答案与原始答案（按字段权限）。
    内部逻辑：以原始答案为基础副本，将原始答案中"当前节点可编辑"（editable） 的字段移除（因为这些字段会以审批人本次提交的答案为准），再合并本次提交的答案， 最终得到：历史不可编辑字段保留原值 + 可编辑字段以本次提交值为准。
  - `protected void updateTaskAnswer(ApprovalTaskRequest request)`
    更新任务对应的表单答案。
    内部逻辑：若本次请求携带了答案，则加载该节点配置获取字段权限，用 #mergeAnswer 合并本次提交答案与原始答案后，调用 AnswerService 更新 表单答案记录。数据链路：ApprovalTaskRequest.answer → mergeAnswer → AnswerService .updateAnswer → t_answer 表。
  - `protected Task getCurrentRunningTask(String taskId)`
    查询当前运行中的任务。
  - `protected List<HistoricActivityInstance> getHistoricActivityInstanceList(String processInstanceId)`
    查询流程实例的全部历史活动实例。
  - `protected List<Task> getProcessInstanceActiveTaskList(String processInstanceId)`
    查询流程实例当前活跃（待办）任务列表。
  - `protected List<FlowOperation> getOperations(String processInstanceId)`
    查询流程实例的用户任务操作记录（按创建时间倒序）。
  - `protected FlowEntry getFlowEntry(String projectId)`
    按项目 ID 查询流程定义。
  - `protected boolean rollbackToStartEvent(ApprovalTaskRequest request)`
    撤回/回退到发起节点（申请人）。
    内部逻辑：当请求的目标节点 ID（newActivityId）等于项目 ID（流程定义 key， 前端约定"回退到发起人"的标志）时：

    - 取当前活跃任务所在节点；
    - 将 newActivityId 置为固定的发起节点 ID（starter）；
    - 挂起流程实例（原实现注释了 moveActivityIdTo 移动节点，改为直接挂起： 回滚至开始节点之后任务会自动流转到下一节点，因此需暂停任务等待申请人完善）；
    - 返回 true 表示已处理回退到发起节点。
  - `protected TaskTreeNode getHistoricTree(String processInstanceId)`
    将任务流转记录转换成一棵节点树。
    用途：回退（驳回）节点计算的数据基础。遍历实例的用户任务操作记录 （仅保存/同意/驳回/撤回四种类型，按时间正序），按规则构建有向树：

    - 第一条记录作为根节点；
    - 连续相同节点记录去重跳过；
    - save（发起保存）：回到根节点继续；
    - agree（同意）：作为最后一个节点的子节点加入（同节点去重）；
    - rollback（驳回）：将游标回退到目标节点（findParentByKey），本记录不入树；
    - revert（撤回）：节点的 taskDefKey 与已完成节点一致，不改变树结构。
  - `public TaskTreeNode findParentByKey(String key)`
    沿父链向上查找指定活动 ID 的节点。
    用途：驳回操作时定位"要回退到的目标节点"在树中的位置。
  - `public String toString()`
    调试用字符串表示。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/taskHandler/AgreeTaskHandler.java`
- 包: `cn.wisestar.server.flow.service.impl.taskHandler`
- 类型: `class AgreeTaskHandler`
- 注解: @Component
- **类说明**：
  同意任务处理器（Bean 名 "agreeTaskHandler"）。
  职责：处理"同意"审批操作。内部逻辑最简单：直接调用 Flowable TaskService 完成当前待办任务（complete），引擎会自动沿 BPMN 流转到下一节点；若当前是最后 一个审批节点，流程将正常结束（触发 ProcessCompletedListener 将实例状态置为 "已结束"）。

  流程流转（同意分支）：审批人点击同意 → FlowServiceImpl.approvalTask 按 type 分发到本处理器 → innerProcess 完成当前任务（引擎流转到下一节点或结束）→ 父类模板落库操作记录（approvalType=agree）并更新表单答案。

  被谁调用：FlowServiceImpl.approvalTask（按 Bean 名动态获取）。

  依赖什么：父类 AbstractTaskHandler（模板方法、操作记录落库）。
- 方法:
  - `public boolean innerProcess(ApprovalTaskRequest request)`
    执行"同意"流转：完成当前待办任务。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/taskHandler/RefuseTaskHandler.java`
- 包: `cn.wisestar.server.flow.service.impl.taskHandler`
- 类型: `class RefuseTaskHandler`
- 注解: @Component
- **类说明**：
  拒绝任务处理器（Bean 名 "refuseTaskHandler"）。
  职责：处理"拒绝"审批操作。拒绝之后直接结束流程：调用 Flowable RuntimeService 删除整个流程实例（deleteProcessInstance，删除原因记录审批意见），引擎触发 PROCESS_CANCELLED 事件，由 ProcessCancelledListener 将 t_flow_instance 状态同步为"已拒绝"。

  流程流转（拒绝分支）：审批人点击拒绝 → FlowServiceImpl.approvalTask 分发到本 处理器 → innerProcess 删除流程实例（流程到此终止）→ 父类模板落库操作记录 （approvalType=refuse）。

  被谁调用：FlowServiceImpl.approvalTask（按 Bean 名动态获取）。

  依赖什么：父类 AbstractTaskHandler（模板方法、操作记录落库）、 Flowable RuntimeService（删除实例）。
- 方法:
  - `public boolean innerProcess(ApprovalTaskRequest request)`
    执行"拒绝"流转：删除整个流程实例。
    删除原因传入审批意见 comment，供引擎历史与后续审计查看。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/taskHandler/RevertTaskHandler.java`
- 包: `cn.wisestar.server.flow.service.impl.taskHandler`
- 类型: `class RevertTaskHandler`
- 注解: @Component, @Slf4j
- **类说明**：
  撤回任务处理器（Bean 名 "revertTaskHandler"）。
  职责：处理"撤回"审批操作：撤回当前用户提交的、但尚未被审批的待办任务。 仅"已办任务的最新操作人"才能执行该操作（#canRevert 校验）。撤回后：

  - 若目标是发起节点（newActivityId = 项目 ID）→ #rollbackToStartEvent 挂起流程实例，等待申请人完善表单后重新提交（实例状态"申请人完善中"）；
  - 否则 → 使用 Flowable 状态变更 API 将当前活跃任务移动到指定历史节点。

  流程流转（撤回分支）：申请人点击撤回 → FlowServiceImpl.approvalTask 分发到本 处理器 → canRevert 校验 → 移动/挂起到目标节点 → 父类模板落库操作记录 （approvalType=revert）。

  被谁调用：FlowServiceImpl.approvalTask（按 Bean 名动态获取）； #getRevertNodes 同时被 FlowServiceImpl.getRevertNodes 调用（可回退节点计算）。

  依赖什么：父类 AbstractTaskHandler（模板方法、操作记录落库、节点树构建）。
- 方法:
  - `public boolean innerProcess(ApprovalTaskRequest request)`
    执行"撤回"流转。
    内部逻辑：

    - 校验当前用户是否为最近操作人（#canRevert），否则抛异常拒绝操作；
    - 若请求目标为发起节点（newActivityId = 项目 ID）→ 走 #rollbackToStartEvent 挂起实例，结束；
    - 否则（TODO：会签撤回未实现），取当前活跃任务，通过引擎状态变更 API 将当前 节点移动到请求指定的活动节点（request.getActivityId），完成撤回。
  - `public boolean canRevert(String taskId, String processInstanceId)`
    校验当前用户是否可撤回：只有最近一条的操作记录是自己才能进行驳回操作。
    内部逻辑：查询实例的用户任务操作记录（倒序），若最新一条记录的 ID 与请求的 taskId 一致（说明最新操作人就是当前用户），则允许撤回。注意此处 taskId 语义 复用为"最近操作记录 ID"。
  - `public List<TaskTreeNode> getRevertNodes(String processInstanceId)`
    获取当前可以回退的节点列表。
    内部逻辑：基于历史操作记录构建节点树（AbstractTaskHandler#getHistoricTree）， 取当前节点的 parent 开始沿父链向上收集全部祖先节点（不能包含当前节点），即为 可回退的节点链路。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/taskHandler/RollbackTaskHandler.java`
- 包: `cn.wisestar.server.flow.service.impl.taskHandler`
- 类型: `class RollbackTaskHandler`
- 注解: @Component, @Slf4j
- **类说明**：
  驳回任务处理器（Bean 名 "rollbackTaskHandler"）。
  职责：处理"驳回"审批操作：当前审批人主动将待办任务驳回到指定（或上一个） 审批节点。只有当前待办任务的指派人或者候选者才能完成该操作（由 Flowable 任务 权限天然保证）。

  流程流转（驳回分支）：审批人点击驳回到某节点 → FlowServiceImpl.approvalTask 分发到本处理器 → 定位当前任务节点与目标节点 → 引擎状态变更 API 将当前活动移动到 目标节点（重新生成目标节点的待办任务）→ 父类模板落库操作记录（approvalType=rollback）。

  内部逻辑细节：

  - 若目标为发起节点（newActivityId = 项目 ID）→ 走 #rollbackToStartEvent 挂起实例；
  - 未指定目标节点时默认驳回到上一节点（取节点树当前节点的 parent）；
  - 指定了目标节点则直接移动（moveActivityIdTo）。

  被谁调用：FlowServiceImpl.approvalTask（按 Bean 名动态获取）。

  依赖什么：父类 AbstractTaskHandler（模板方法、操作记录落库、节点树构建）。
- 方法:
  - `public boolean innerProcess(ApprovalTaskRequest request)`
    执行"驳回"流转。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/taskHandler/SaveTaskHandler.java`
- 包: `cn.wisestar.server.flow.service.impl.taskHandler`
- 类型: `class SaveTaskHandler`
- 注解: @Component
- **类说明**：
  保存（发起）任务处理器（Bean 名 "saveTaskHandler"）。
  职责：处理"保存/提交"操作，即流程发起。申请人在问卷页填写完成并提交时触发：

  - **首次提交**（processInstanceId 为空）：按项目 ID（流程定义 key）启动新的 Flowable 流程实例，业务键为答案 ID，并把 answerId、发起人 ID 放入流程变量； 引擎触发 PROCESS_STARTED 事件创建 t_flow_instance；
  - **再次提交**（processInstanceId 已存在，即"撤回后申请人完善"场景）： 重新激活被挂起的实例，并把当前活跃任务移动回发起节点（starter），让流程重新走 第一轮审批（引擎再次触发 ACTIVITY_STARTED 更新审批阶段）。

  流程流转（发起分支）：申请人提交表单 → FlowServiceImpl.approvalTask 分发到本 处理器 → 启动/重新激活实例 → 父类模板落库操作记录（approvalType=save）并更新答案。

  被谁调用：FlowServiceImpl.approvalTask（按 Bean 名动态获取）。

  依赖什么：父类 AbstractTaskHandler（模板方法、操作记录落库）、 Flowable RuntimeService（启动/激活实例）、FlowConstant（流程变量名）。
- 方法:
  - `public boolean innerProcess(ApprovalTaskRequest request)`
    执行"保存/发起"流转。
    内部逻辑：

    - 组装流程变量：answerId（答案主键）、starterUserId（当前登录用户即发起人）；
    - 查询项目绑定的流程定义，未绑定（entry 为空）→ 返回 false（父类模板将跳过 操作记录落库与答案更新）；
    - 首次提交：startProcessInstanceByKey 以项目 ID 为 key、答案 ID 为业务键启动 实例，并把生成的实例 ID 回写请求；
    - 再次提交：activateProcessInstanceById 重新激活实例，然后把当前活跃任务移动 到发起节点 starter（流程重新从第一轮审批开始），并回写 newActivityId。

### `flow/src/main/java/cn/wisestar/server/flow/service/impl/taskHandler/TaskHandler.java`
- 包: `cn.wisestar.server.flow.service.impl.taskHandler`
- 类型: `interface TaskHandler`
- **类说明**：
  审批任务处理器接口。
  职责：定义审批操作的统一处理入口。每种审批类型对应一个 Spring Bean （Bean 名 = 类型 + "TaskHandler"，如 agreeTaskHandler、refuseTaskHandler）， 由 `FlowServiceImpl.approvalTask` 根据请求 type 动态获取并调用。

  所属流程环节：审批处理环节的总分派点。

  被谁调用：FlowServiceImpl.approvalTask（按类型分发）。

  依赖什么：ApprovalTaskRequest（审批请求参数）； 实现类继承 AbstractTaskHandler。
