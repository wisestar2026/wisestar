# 项目词典 · 后端 api（HTTP 接口层：Controller / 启动类 / 全局配置）

> 本文件由源码清单自动生成后再人工校订，颗粒度到「每个类 + 每个公开方法」。
> 阅读方式：`归属` 段给出模块与包路径；`说明` 段取自类级 Javadoc；`方法` 段逐个列出公开/受保护方法（含 Javadoc 首句）。
>
> 归属规则：模块（Maven module）= 顶层归属；包（package）= 二级归属。
> 相关规则：`注解` 表示框架角色，`注入/字段` 表示直接依赖（协作方）。
> 本文件仅描述代码事实，未收录任何密钥；配置项中的 token 均为占位符。

---
### `api/src/main/java/cn/wisestar/server/SurveyServerApplication.java`
- 包: `cn.wisestar.server`
- 类型: `class SurveyServerApplication`
- 注解: @SpringBootApplication, @EnableConfigurationProperties
- **类说明**：
  AI 自习室系统（wisestar）后端服务启动类（SurveyServerApplication）。
  **所属模块**：api 模块（Web 应用模块，是整个后端唯一可运行的 Spring Boot 启动入口， 它同时依赖 rdbms、shared、ai 等模块，Maven 依赖会将这些模块的类都纳入 classpath）。

  **类职责**：

  - 作为 Spring Boot 应用的启动类，main 方法入口；
  - 声明组件扫描范围：@SpringBootApplication 默认扫描 cn.wisestar.server 包及其子包， 因此 api（cn.wisestar.server.api）、shared、rdbms、ai（cn.wisestar.server.ai）各模块 中的 @RestController / @Service / @Component 等都会被装配进 Spring 容器；
  - 通过 @EnableConfigurationProperties 开启配置属性类的绑定 （配合核心包中的 @ConfigurationProperties 类，如 api.prefix 等自定义配置）。

  **被谁调用**：由运维/部署脚本或 IDE 以 `java -jar survey-server.jar` / `mvn spring-boot:run` 方式启动； 支持在启动命令中传入额外参数以触发数据库初始化（见 main 方法说明）。

  **依赖的服务**：启动阶段依赖 core 工具类 DatabaseInitHelper（cn.wisestar.server.core.uitls 包，用于快速建库/初始化数据）。 运行期依赖 Spring Boot 全家桶（web/undertow、security、mybatis 等，由各模块 pom 引入）。
- 方法:
  - `public static void main(String[] args)`
    应用启动入口。
    **功能**：启动 Spring Boot 应用。特殊逻辑——如果 JVM 启动参数 args 非空， 则先调用 DatabaseInitHelper#init(String[]) 执行一次快速的数据库初始化操作 （例如初始化/升级数据库表结构、写入种子数据等，具体动作由该工具类实现决定）， 然后再通过 SpringApplication#run(Class, String...) 正式启动 Spring 容器。

    **请求/数据流**：本方法不处理 HTTP 请求，仅负责进程启动： JVM 启动参数 → 判断是否执行数据库初始化 → SpringApplication.run 装载所有 Bean → 内嵌 Undertow 容器监听端口 → 接受来自 api 模块各 Controller 的 HTTP 请求。

    **异常**：若数据库不可用、配置错误或端口被占用，启动过程会抛出异常并终止进程。

### `api/src/main/java/cn/wisestar/server/api/AnalysisApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class AnalysisApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  学生答题情况分析接口（AnalysisApi）
  **所属模块**：api 模块（Web 接口层，Spring MVC REST Controller）。

  **类职责**：提供 AI 自习室系统中"学生答题情况分析"相关的统计分析接口， 目前包含两个只读（GET）接口：

  - 按知识点聚合统计答题情况（正确率）——GET ${api.prefix}/analysis/knowledge-point/stats
  - 单学生知识点画像 ——GET ${api.prefix}/analysis/knowledge-point/student-profile

  **请求路径前缀**：由配置项 `api.prefix` 决定（通常为 /api），类级路径为 `${api.prefix`/analysis}，方法级路径见各方法注解。

  **被谁调用**：由前端管理后台"统计分析"页面（学生答题情况分析面板）通过 HTTP 调用， 两个接口均要求登录认证（@PreAuthorize("isAuthenticated()")，需携带有效的 JWT 令牌）。

  **依赖的服务**：注入 AnalysisService（位于 shared 模块 cn.wisestar.server.service 包）， 其实现类位于 rdbms 模块，内部通过 MyBatis Mapper 查询答题明细表并做知识点维度聚合。

  **完整数据流（以 knowledgePointStats 为例）**：

  ```
   前端 HTTP GET /api/analysis/knowledge-point/stats?studentId=xxx&subject=数学 --> Spring Security 认证过滤器校验 JWT（isAuthenticated） --> AnalysisApi#knowledgePointStats(KnowledgePointQuery query) （本类） --> AnalysisService#knowledgePointStats(KnowledgePointQuery) （shared 接口） --> AnalysisServiceImpl#knowledgePointStats(...) （rdbms 实现） --> XxxMapper 查询答题明细表，按 subject/chapter/knowledge_point GROUP BY 聚合 --> 组装 List 返回前端渲染
  ```
- 方法:
  - `public List<KnowledgePointStat> knowledgePointStats(KnowledgePointQuery query)`
    按知识点聚合统计答题情况（正确率）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/analysis/knowledge-point/stats （例如 /api/analysis/knowledge-point/stats）。

    **功能**：将学生的答题记录按"学科(subject) - 章节(chapter) - 知识点(knowledgePoint)" 三个维度聚合，统计每个知识点的答题次数（attempts）、正确次数（correctCount）与 正确率（correctRate，0-1 之间的小数），用于学习分析面板展示各知识点掌握程度。

    **请求参数**：通过 Spring MVC 参数绑定自动封装为 KnowledgePointQuery（GET 查询参数）， 均为可选过滤条件：

    - studentId：学生 id，为空时按当前登录用户统计（后端从 SecurityContext 取 userId）
    - subject：学科（模糊或精确过滤，由实现决定）
    - chapter：章节
    - knowledgePoint：知识点（精确匹配）

    **返回值结构**：`List`，每个元素包含 subject、chapter、knowledgePoint、attempts（long）、correctCount（long）、 correctRate（double，0~1）六个字段；直接以 JSON 数组形式返回。

    **权限**：@PreAuthorize("isAuthenticated()")——要求请求携带有效登录态，否则 401/403。

    **异常**：查询过程本身不主动抛业务异常；若未登录由 Spring Security 拦截返回未认证错误。

    **调用的下层 Service**：AnalysisService#knowledgePointStats(KnowledgePointQuery)。

    **数据流**：前端 GET 请求（携带过滤参数）→ 本方法（参数绑定为 KnowledgePointQuery） → AnalysisService.knowledgePointStats → AnalysisServiceImpl 组装查询条件 → MyBatis Mapper 按知识点 GROUP BY 聚合答题明细 → 结果映射为 List → 逐层返回，Spring 序列化为 JSON 响应。
  - `public List<KnowledgePointStat> studentProfile(String studentId)`
    单学生知识点画像。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/analysis/knowledge-point/student-profile （例如 /api/analysis/knowledge-point/student-profile?studentId=xxx）。

    **功能**：针对指定学生（或当前登录学生）生成"知识点画像"，返回该生在所有 学科/章节/知识点维度上的答题次数与正确率，用于直观展示学生的强项与薄弱知识点， 供教师端或学生端"我的画像"页面使用。

    **请求参数**：studentId（String，GET 查询参数，可选）。为空时后端取当前登录用户 的 userId 作为统计对象。

    **返回值结构**：`List`，与 knowledgePointStats 返回值结构相同 （subject、chapter、knowledgePoint、attempts、correctCount、correctRate）， 但含义上表示"单个学生"的画像数据。

    **权限**：@PreAuthorize("isAuthenticated()")——要求登录。

    **调用的下层 Service**：AnalysisService#studentProfile(String)。

    **数据流**：前端 GET 请求（studentId 参数）→ 本方法 → AnalysisService.studentProfile(studentId) → AnalysisServiceImpl 以学生 id 过滤 答题明细 → MyBatis Mapper 按知识点聚合 → List → JSON 响应。

### `api/src/main/java/cn/wisestar/server/api/AnswerApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class AnswerApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  答卷（答案）管理接口（AnswerApi）。
  **所属模块**：api 模块（Web 接口层，Spring MVC REST Controller）。

  **类职责**：提供答卷数据的管理接口：答案列表/详情、回收站（删除/恢复/彻底删除）、 数据表格保存答案、答案更新、答案导出（Excel）、答案附件上传。

  **请求路径前缀**：类级路径为 `${api.prefix`/answer}（api.prefix 通常为 /api）， 各方法再追加子路径（如 /api/answer/list、/api/answer/download 等）。

  **被谁调用**：前端管理后台：答卷数据管理页（表格展示/导出）、答卷详情页、 附件上传场景。所有接口要求登录并校验对应权限点。

  **依赖的服务**：注入 AnswerService（shared 模块接口，rdbms 模块实现）—— 负责答卷的 CRUD、回收站、导出（fastexcel 写 Excel）与附件上传业务。

  **数据流概览**：前端 HTTP 请求 → 本类方法（权限注解 + 参数校验）→ AnswerService（shared 接口）→ rdbms 实现 → 答卷 Mapper → 数据库 → 视图 DTO / Excel 响应流。
- 方法:
  - `public PaginationResponse<AnswerView> listAnswer(AnswerQuery query)`
    获取答案列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/answer/list（如 /api/answer/list）。

    **功能**：分页查询答卷列表（按项目、时间、答卷人等条件筛选）， 供答卷数据管理页表格展示。

    **请求参数**：AnswerQuery（GET 查询参数）——分页 + 筛选条件。

    **返回值结构**：PaginationResponse（分页包装的答卷列表）。

    **权限**：@PreAuthorize("hasAuthority('answer:list')")。

    **调用的下层 Service**：AnswerService#listAnswer(AnswerQuery)。
  - `public List<AnswerView> listAnswerDeleted(AnswerQuery query)`
    获取删除的答案（回收站列表）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/answer/trash（如 /api/answer/trash）。

    **功能**：查询已删除（回收站中）的答卷列表，供答卷回收站页面展示。

    **请求参数**：AnswerQuery（GET 查询参数，筛选条件）。

    **返回值结构**：`List`（已删除答卷列表）。

    **权限**：@PreAuthorize("hasAuthority('answer:list')")。

    **调用的下层 Service**：AnswerService#listAnswerDeleted(AnswerQuery)。
  - `public AnswerView getAnswer(AnswerQuery query)`
    获取答案详情。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/answer?id=xxx（如 /api/answer?id=xxx）。

    **功能**：按查询条件获取单份答卷的详情（含各题答案明细），供答卷详情页展示。

    **请求参数**：AnswerQuery（GET 查询参数，含答卷 id）。

    **返回值结构**：AnswerView（答卷详情视图）。

    **权限**：@PreAuthorize("hasAuthority('answer:detail')")。

    **调用的下层 Service**：AnswerService#getAnswer(AnswerQuery)。
  - `public void saveAnswer(@RequestBody AnswerRequest request)`
    数据表格保存答案。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/answer/create（如 /api/answer/create）。

    **功能**：在答卷数据管理页中手动新增一条答卷记录（如人工录入/补录答卷）。

    **请求参数**：AnswerRequest（@RequestBody JSON）——答卷数据 （项目 id、答卷人、各题答案等）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('answer:create')")。

    **调用的下层 Service**：AnswerService#saveAnswer(AnswerRequest)。
  - `public void updateAnswer(@RequestBody AnswerRequest request)`
    更新答案。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/answer/update（如 /api/answer/update）。

    **功能**：更新答卷记录（修改答卷人信息、答案明细、状态等）。

    **请求参数**：AnswerRequest（@RequestBody JSON，含答卷 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('answer:update')")。

    **调用的下层 Service**：AnswerService#updateAnswer(AnswerRequest)。
  - `public void deleteAnswer(@RequestBody AnswerRequest request)`
    删除答案（答案放到回收站）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/answer/delete（如 /api/answer/delete）。

    **功能**：将答卷逻辑删除（标记删除状态，进入回收站，可恢复）。

    **请求参数**：AnswerRequest（@RequestBody JSON，含答卷 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('answer:delete')")。

    **调用的下层 Service**：AnswerService#deleteAnswer(AnswerRequest)。
  - `public void batchDestroyAnswer(@RequestBody AnswerRequest request)`
    从回收站里面清空答案（彻底删除）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/answer/destroy（如 /api/answer/destroy）。

    **功能**：将回收站中的答卷物理删除（支持批量，不可恢复）。

    **请求参数**：AnswerRequest（@RequestBody JSON，id 支持逗号分隔批量）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('answer:delete')")。

    **调用的下层 Service**：AnswerService#batchDestroyAnswer(AnswerRequest)。
  - `public void restoreAnswer(@RequestBody AnswerRequest request)`
    从回收站里面恢复答案。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/answer/restore（如 /api/answer/restore）。

    **功能**：将回收站中的答卷恢复为正常状态。

    **请求参数**：AnswerRequest（@RequestBody JSON，含答卷 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('answer:update')")。

    **调用的下层 Service**：AnswerService#restoreAnswer(AnswerRequest)。
  - `public ResponseEntity<Resource> download(DownloadQuery query)`
    答案导出。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/answer/download（如 /api/answer/download）。

    **功能**：将符合条件的答卷数据导出为 Excel 文件。特殊处理——支持按请求参数 locale 切换导出文件的国际化文案（如表头语言），导出前临时设置 LocaleContextHolder， 导出完成后在 finally 中恢复原 Locale，避免影响其他请求线程的国际化上下文。

    **请求参数**：DownloadQuery（GET 查询参数）——导出筛选条件 + locale （语言标识，如下划线分隔的 zh_CN，方法内会规范为连字符语言标签 zh-CN）。

    **返回值结构**：ResponseEntity（Excel 文件字节流，附件下载）。

    **权限**：@PreAuthorize("hasAuthority('answer:export')")。

    **调用的下层 Service**：AnswerService#download(DownloadQuery)。
  - `public AnswerUploadView upload(AnswerUploadRequest request)`
    修改答案上传附件。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/answer/upload（如 /api/answer/upload）。

    **功能**：为答卷上传/更新附件（如答卷中上传的图片、文件）， 返回附件在项目 schema 中的引用信息。

    **请求参数**：AnswerUploadRequest（multipart 表单绑定）——附件文件 + 关联的答卷/题目信息。

    **返回值结构**：AnswerUploadView（附件上传结果视图，含项目 schema 中附件引用）。

    **权限**：@PreAuthorize("hasAuthority('answer:upload')")。

    **调用的下层 Service**：AnswerService#upload(AnswerUploadRequest)。

### `api/src/main/java/cn/wisestar/server/api/CampusApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class CampusApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  校区管理接口（行政管理-校区管理）。
  **所属模块**：api 模块（Web 接口层）。

  **路径前缀**：`${api.prefix`/system/campus}（api.prefix 通常为 /api）。

  **被谁调用**：校区管理页 /admin/campus、学员管理页校区下拉、系统用户管理页校区多选。

  **数据权限**：列表与下拉按当前账号校区数据权限范围返回（校长/教务/学管师仅见自己 绑定校区）；写操作（create/update/delete）当前仅管理员内置角色拥有。
- 方法:
  - `public List<CampusView> listCampuses()`
    校区管理列表（含引用学员数/绑定角色员工数统计，受当前账号校区数据权限约束）。
  - `public List<CampusView> campusOptions(@RequestParam(defaultValue = "false") boolean includeDisabled)`
    校区下拉数据源（供学员表单/用户管理校区字段使用）。
  - `public void createCampus(@RequestBody CampusRequest request)`
    新增校区。
  - `public void updateCampus(@RequestBody CampusRequest request)`
    更新校区（改名同事务同步学员引用）。
  - `public void deleteCampus(@RequestBody CampusRequest request)`
    删除校区（仅零引用可删）。

### `api/src/main/java/cn/wisestar/server/api/ChapterApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class ChapterApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  章节管理接口（知识管理板块二级维度）。
  **定位**：管理端「知识管理 → 章节」页面数据源——章节 CRUD； 章节挂载于学科下（subjectId），列表按学科过滤，供顶部下拉二级联动使用； 章节题库经 t_chapter_repo 从题库管理（t_repo）绑定。
- 方法:
  - `public List<ChapterView> listChapters(ChapterRequest query)`
    章节列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/chapter/list（如 /api/chapter/list）。

    **请求参数**：ChapterRequest（Query 参数：subjectId 可选， 不传返回全部章节）。

    **功能**：返回章节列表（sort 升序），每项含该章节下的小节数 sectionCount 与已绑定题库数 repoCount。

    **返回值结构**：ChapterView 列表。
  - `public void exportChapters(ChapterRequest query)`
    导出章节列表为 Excel（附件下载）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/chapter/export （如 /api/chapter/export?subjectId=&grade=&term=&version=）。

    **功能**：导出 xlsx 文件，列结构：章节名称/年级/学期/版本/小节数/练习数； 过滤条件与 #listChapters(ChapterRequest) 一致（subjectId/grade/term/version 可选）。

    **权限**：hasAuthority('knowledge:list')。
  - `public String addChapter(@RequestBody ChapterRequest request)`
    新增章节。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/chapter/create（如 /api/chapter/create）。

    **请求参数**：ChapterRequest（@RequestBody JSON： subjectId/name/grade/term/version；icon/sort 由系统默认维护，可不传）。

    **返回值结构**：新章节 id（String）。
  - `public ImportResultView importChapters(ChapterImportRequest request)`
    批量导入章节（multipart 表单：subjectId + file）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/chapter/import （如 /api/chapter/import）。

    **功能**：解析 Excel（列：学科名/章节名称/年级(选填)/学期(选填)/版本(选填)， 首行为表头跳过），按「学科名+章节名」去重后批量写入 t_chapter； 图标与排序由系统默认维护（图标 📖、排序追加至学科末尾）。

    **权限**：hasAuthority('knowledge:create')。
  - `public void updateChapter(@RequestBody ChapterRequest request)`
    更新章节。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/chapter/update（如 /api/chapter/update）。
  - `public void deleteChapter(@RequestBody ChapterRequest request)`
    删除章节（级联逻辑删除其下小节、知识点及题库绑定）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/chapter/delete（如 /api/chapter/delete）。
  - `public void saveRepos(@RequestBody ChapterRepoRequest request)`
    保存章节-题库绑定（已停用）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/chapter/repos （如 /api/chapter/repos）。

    **功能**：练习仅支持绑定到小节，章节不支持直接绑定练习。历史章节绑定数据 已展开迁移到其下小节，本接口不再接受写入，一律返回业务错误。前端章节级 「绑定练习」入口已移除（绑定入口在小节管理 / 习题列表的小节层级）。

    **请求参数**：ChapterRepoRequest（@RequestBody JSON： chapterId + repoIds[]）。
  - `public List<RepoView> listRepos(@RequestParam("chapterId") String chapterId)`
    查询章节已绑定的题库列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/chapter/repos （如 /api/chapter/repos?chapterId=xxx）。

    **功能**：章节不再支持直接绑定题库，表数据已清空，本接口始终返回空列表 （仅保留供兼容读取）。练习绑定关系请查询小节 /api/section/repos。

    **返回值结构**：RepoView 列表。

### `api/src/main/java/cn/wisestar/server/api/DashboardApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class DashboardApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  数据看板接口（DashboardApi）。
  **所属模块**：api 模块（Web 接口层，Spring MVC REST Controller）。

  **类职责**：提供数据看板/统计图表的数据查询接口——当前仅包含看板列表查询 （按条件返回一组统计卡片/图表数据，如答卷量趋势、题目完成度等）。

  **请求路径前缀**：类级路径为 `${api.prefix`/dashboard}（api.prefix 通常为 /api）， 当前方法路径为 ${api.prefix}/dashboard/list。

  **被谁调用**：前端管理后台"数据看板"页面（需登录，是否要求权限点由全局安全规则控制）。

  **依赖的服务**：注入 DashboardService（shared 模块接口，rdbms 模块实现）—— 负责从答卷/项目/题目等表聚合统计看板数据。

  **数据流**：前端 GET /api/dashboard/list?projectId=xxx → 本类 listDashboard(DashboardQuery) → DashboardService#listDashboard → rdbms 实现 → 多个 Mapper 聚合统计 → List → JSON。
- 方法:
  - `public List<DashboardView> listDashboard(DashboardQuery query)`
    获取数据看板列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/dashboard/list（如 /api/dashboard/list）。

    **功能**：按查询条件返回一组看板统计项（每项为一个统计视图：指标名称、 数值、趋势数据等），供前端看板页面渲染图表。

    **请求参数**：DashboardQuery（GET 查询参数）——统计范围条件 （如项目 id、时间范围、统计类型等）。

    **返回值结构**：`List`（看板统计视图列表）。

    **调用的下层 Service**：DashboardService#listDashboard(DashboardQuery)。

### `api/src/main/java/cn/wisestar/server/api/EnglishAiPackApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class EnglishAiPackApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  AI 单元内容包接口（后台管理端：AI 生成整套单元学习内容）。
- 方法:
  - `public PaginationResponse<EnglishAiPackView> list(EnglishAiPackQuery query)`
    内容包分页列表（不含 content 正文）。
  - `public EnglishAiPackView detail(@RequestParam String id)`
    内容包详情（含 content 正文）。
  - `public EnglishAiPackView generate( @RequestParam String version, @RequestParam String grade, @RequestParam String unit, @RequestParam(required = false) String topic)`
    调用大模型生成整套单元内容（预览，不落库）。
  - `public EnglishAiPackView save(@RequestBody EnglishAiPackView pack)`
    保存内容包（同 版本+年级+单元 覆盖更新）。
  - `public PackSyncResult sync(@RequestParam String id)`
    将内容包同步到词库与语法库。
  - `public void delete(@RequestParam String id)`
    删除内容包。

### `api/src/main/java/cn/wisestar/server/api/EnglishSentenceApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class EnglishSentenceApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  英语句库接口（后台管理端）。
- 方法:
  - `public PaginationResponse<EnglishSentenceView> list(EnglishSentenceQuery query)`
    句子分页列表。
  - `public void save(@RequestBody EnglishSentenceView view)`
    新增/更新句子。
  - `public void delete(@RequestParam String id)`
    删除句子。
  - `public ImportResult importSentences(@RequestParam MultipartFile file)`
    批量导入句子（Excel）。

### `api/src/main/java/cn/wisestar/server/api/EnglishStudentApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class EnglishStudentApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  英语学员学习接口（学生端学习中心）。
- 方法:
  - `public List<EnglishUnitProgressView> units(@RequestParam(required = false) String version, @RequestParam(required = false) String grade, @RequestParam(required = false) String term)`
    单元列表 + 学习进度。
  - `public List<EnglishSentenceView> sentences(@RequestParam(required = false) String version, @RequestParam(required = false) String grade, @RequestParam(required = false) String term, @RequestParam(required = false) String unit)`
    单元句子列表（含熟练度）。
  - `public List<EnglishSentenceView> studySentences(@RequestParam(defaultValue = "10") Integer limit)`
    待学习/复习句子。
  - `public void recordSentence(@RequestBody Map<String, Object> request)`
    记录句子作答（{sentenceId, correct}）。
  - `public List<ReviewSessionView> review(@RequestParam(defaultValue = "20") Integer limit)`
    智能复习会话（单词 + 句子混合队列）。
  - `public void session(@RequestBody Map<String, Object> request)`
    记录学习会话（{type, durationSeconds, correctCount}）。

### `api/src/main/java/cn/wisestar/server/api/EnglishUnitApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class EnglishUnitApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  英语单元目录接口（后台管理端）。
- 方法:
  - `public PaginationResponse<EnglishUnitView> list(EnglishUnitQuery query)`
    单元分页列表。
  - `public void save(@RequestBody EnglishUnitView view)`
    新增/更新单元。
  - `public void delete(@RequestParam String id)`
    删除单元。

### `api/src/main/java/cn/wisestar/server/api/EnglishWordAiApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class EnglishWordAiApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  英语单词 AI 生成接口。
- 方法:
  - `public void generateContent(@RequestParam String id)`
    为单个单词生成 AI 内容（图片/音频/例句）。
  - `public ImportResult batchGenerate(@RequestBody Map<String, Object> request)`
    批量为单词生成 AI 内容。
  - `public ImportResult generateByCondition( @RequestParam(required = false) String version, @RequestParam(required = false) String grade, @RequestParam(required = false) String unit )`
    按条件批量生成 AI 内容（版本/年级/单元）。

### `api/src/main/java/cn/wisestar/server/api/EnglishWordApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class EnglishWordApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  英语单词列表接口（学生端学习接口见 EnglishWordStudentApi）。
- 方法:
  - `public cn.wisestar.server.core.common.PaginationResponse<EnglishWordView> listWords(EnglishWordQuery query)`
    单词列表（按版本/年级/单元筛选）。

### `api/src/main/java/cn/wisestar/server/api/EnglishWordManagerApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class EnglishWordManagerApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  英语单词管理接口（后台管理端）。
- 方法:
  - `public cn.wisestar.server.core.common.PaginationResponse<EnglishWordView> listWords(EnglishWordQuery query)`
    单词列表（按版本/年级/单元筛选）。
  - `public EnglishWordView detail(@RequestParam String id)`
    单词详情。
  - `public void createWord(@RequestBody EnglishWordView word)`
    新增单词。
  - `public void updateWord(@RequestBody EnglishWordView word)`
    编辑单词。
  - `public void deleteWord(@RequestParam String id)`
    删除单词。
  - `public cn.wisestar.server.domain.dto.english.ImportResult importWords(@RequestParam MultipartFile file)`
    批量导入单词（Excel）。

### `api/src/main/java/cn/wisestar/server/api/EnglishWordStudentApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class EnglishWordStudentApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  英语单词学习接口（学生端）。
- 方法:
  - `public List<EnglishWordView> studyWords(@RequestParam(defaultValue = "10") Integer limit)`
    学生端：获取待学习/复习单词列表。
  - `public void recordLearning(@RequestBody java.util.Map<String, Object> request)`
    学生端：记录学习结果（熟练度 + 下次复习时间）。
  - `public PaginationResponse<EnglishWordView> getWordBook(EnglishWordQuery query)`
    学生端：按条件筛选单词本。

### `api/src/main/java/cn/wisestar/server/api/ExerciseApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class ExerciseApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  练习接口（ExerciseApi）。
  **所属模块**：api 模块（Web 接口层，Spring MVC REST Controller）。

  **类职责**：提供"练习"（exercise）场景的查询接口——目前仅包含历史练习列表查询。 练习即用户对问卷/题库进行答题练习，历史练习记录从答卷数据（answer）中派生。

  **请求路径前缀**：类级路径为 `${api.prefix`/exercise}（api.prefix 通常为 /api）， 当前方法路径为 ${api.prefix}/exercise/list。

  **被谁调用**：前端"练习中心/历史练习"页面（需要登录且具备 exercise:list 权限）。

  **依赖的服务**：注入 AnswerService（shared 模块接口，rdbms 模块实现）—— 注意本类将练习查询复用答卷服务的能力（历史练习本质上是答卷记录的筛选视图）。

  **数据流**：前端 GET /api/exercise/list?pageNo=&pageSize= → 本类 historyExercise(HistoryExerciseQuery) → AnswerService#historyExercise → rdbms 实现 → 答卷 Mapper 按当前用户 + 练习类型筛选 → PaginationResponse → JSON。
- 方法:
  - `public PaginationResponse<ExerciseView> historyExercise(@Valid HistoryExerciseQuery query)`
    历史练习列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/exercise/list（如 /api/exercise/list）。

    **功能**：分页查询当前登录用户的"历史练习"记录（练习过的问卷/题库、 练习时间、成绩等），供练习中心历史列表页展示。

    **请求参数**：HistoryExerciseQuery（GET 查询参数，@Valid 校验）—— 分页参数（pageNo/pageSize）及练习筛选条件（如练习类型、状态、时间范围）。

    **返回值结构**：PaginationResponse（分页包装： total + 当前页练习记录列表）。

    **权限**：@PreAuthorize("hasAuthority('exercise:list')")——需要该权限点。

    **调用的下层 Service**：AnswerService#historyExercise(HistoryExerciseQuery)。

### `api/src/main/java/cn/wisestar/server/api/FileApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class FileApi`
- 注解: @RestController, @RequestMapping, @AllArgsConstructor
- **类说明**：
  文件管理接口（FileApi）。
  **所属模块**：api 模块（Web 接口层，Spring MVC REST Controller）。

  **类职责**：提供系统内文件（图片、附件、Excel 导入模板等）的上传、下载、 列表查询与删除能力，供管理后台"文件管理"页面以及问卷/答案附件场景使用。

  **请求路径前缀**：类级路径为 `${api.prefix`/file}（api.prefix 通常为 /api）， 各方法在类级路径上追加子路径。

  **被谁调用**：前端管理后台文件管理页、问卷编辑器（上传问卷封面/附件）、 公开答卷页（经 SurveyApi 转发到 fileService.upload）。

  **依赖的服务**：注入 FileService（shared 模块接口，rdbms 模块实现）， 其内部负责文件落盘（本地/OSS 等存储策略）与文件元数据表的读写。

  **完整数据流（以 getFile 为例）**：

  ```
   前端 HTTP GET /api/file?id=xxx（FileQuery 绑定查询参数） --> FileApi#getFile(FileQuery) （本类） --> FileService#loadFile(FileQuery) （shared 接口） --> FileServiceImpl#loadFile(...) （rdbms 实现） --> 文件 Mapper 查询元数据 + 从存储读取二进制流 --> ResponseEntity 以字节流响应给前端
  ```
- 方法:
  - `public ResponseEntity<Resource> getFile(FileQuery query)`
    获取文件（按文件 id / 条件下载单个文件内容）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/file（如 /api/file?id=xxx）。

    **功能**：根据 FileQuery 中的条件定位一个文件，将其内容以 ResponseEntity 字节流形式返回；支持按 dispositionType 控制是内联预览（inline）还是附件下载（attachment），可携带自定义响应头 （如 SurveyApi#preview 中设置 30 天缓存头）。

    **请求参数**：FileQuery（GET 查询参数绑定）——包含文件 id（attachmentId）、 文件名、目标路径、dispositionType 等筛选/控制字段，详情见 shared 模块 FileQuery DTO。

    **返回值结构**：HTTP 200 + 文件二进制流（Resource），响应头含 Content-Disposition、 缓存头等；文件不存在时由服务层返回 404 响应。

    **调用的下层 Service**：FileService#loadFile(FileQuery)。

    **数据流**：GET 请求 → 本方法 → fileService.loadFile(query) → 查询文件元数据 → 读取存储二进制 → ResponseEntity 返回。
  - `public List<FileView> listFiles(FileQuery query)`
    获取文件列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/file/list（如 /api/file/list）。

    **功能**：按 FileQuery 中的条件（如所属项目、关联业务 id、文件类型等） 分页/筛选查询文件元数据列表，供前端文件管理列表展示。

    **请求参数**：FileQuery（GET 查询参数绑定，筛选条件）。

    **返回值结构**：`List`，FileView 为文件元数据视图 （文件名、路径、大小、上传时间、上传人等，详见 shared 模块 DTO）。

    **调用的下层 Service**：FileService#listFiles(FileQuery)。
  - `public FileView upload(UploadFileRequest request)`
    添加文件（上传文件）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/file/create（如 /api/file/create）。

    **功能**：接收 multipart/form-data 上传的文件（UploadFileRequest 为 表单字段绑定，含文件流、所属业务 id、文件类型等），将文件落盘存储并写入文件元数据表。

    **请求参数**：UploadFileRequest（multipart 表单绑定，非 JSON body）—— 含 file（MultipartFile 文件流）、projectId（所属项目）、业务类型等字段。

    **返回值结构**：FileView（新文件的元数据视图，含文件 id 供后续引用）。

    **调用的下层 Service**：FileService#upload(UploadFileRequest)。
  - `public void deleteImage(@RequestBody UploadFileRequest request)`
    删除文件。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/file/delete（如 /api/file/delete）。

    **功能**：按文件 id 删除文件——移除存储中的二进制文件并删除（或标记删除） 文件元数据记录。请求体为 JSON。

    **请求参数**：UploadFileRequest（@RequestBody JSON）——其中 id 字段标识要删除的文件。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **调用的下层 Service**：先取 `request.getId()`，再调用 FileService#deleteFile(String)（注意：本方法在 Controller 层只透传 id）。
  - `public ResponseEntity<Resource> downloadTemplate(String name)`
    下载导入 Excel 模板。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/file/downloadTemplate（如 /api/file/downloadTemplate?name=xxx）。

    **功能**：下载系统预置的 Excel 导入模板（如用户导入、题目导入模板）， 供管理员下载后按模板填写数据再批量导入。

    **请求参数**：name（GET 查询参数，导入模板名称，用于定位 classpath 下的模板文件）。

    **返回值结构**：ResponseEntity（Excel 模板文件字节流，附件下载）。

    **调用的下层 Service**：FileService#downloadTemplate(String)。

### `api/src/main/java/cn/wisestar/server/api/KnowledgePointApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class KnowledgePointApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  知识点管理接口（知识管理板块最小学习单元）。
  **定位**：管理端「知识管理 → 知识点」页面数据源——知识点 CRUD + 题目绑定；知识点挂载于小节下（sectionId），列表支持学科/章节/小节三级下拉筛选； 知识点可配图片（imageUrl 为 /api/file/create 上传返回的 previewUrl）； 知识点关联的题目来自题目库（t_template），仅可绑定选择，不能在此新增。
- 方法:
  - `public PaginationResponse<KnowledgePointView> listKnowledgePoints(KnowledgePointQuery query)`
    知识点分页列表（三级下拉筛选）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/knowledge-point/list （如 /api/knowledge-point/list）。

    **请求参数**：KnowledgePointQuery（Query 参数： subjectId/chapterId/sectionId 均可选 + current/pageSize；都不传 → 全量分页）。

    **功能**：返回知识点分页（sort 升序），每项含三级归属名称 （subjectName/chapterName/sectionName）、内容设置 JSON 与已绑定题目数 questionCount。

    **返回值结构**：PaginationResponse（total + list， 元素为 KnowledgePointView）。
  - `public String addKnowledgePoint(@RequestBody KnowledgePointRequest request)`
    新增知识点。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/knowledge-point/create （如 /api/knowledge-point/create）。

    **请求参数**：KnowledgePointRequest（@RequestBody JSON： sectionId/name/sort/content/imageUrl）。

    **返回值结构**：新知识点 id（String）。
  - `public ImportResultView importKnowledgePoints(KnowledgePointImportRequest request)`
    批量导入知识点（multipart 表单：sectionId + file）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/knowledgePoint/import。

    **功能**：解析 Excel（列：知识点名/排序(选填)，首行为表头跳过）， 按 sectionId+name 去重后批量写入 t_knowledge_point。

    **权限**：hasAuthority('knowledge:create')。
  - `public void updateKnowledgePoint(@RequestBody KnowledgePointRequest request)`
    更新知识点（含内容设置 JSON 与图片地址）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/knowledge-point/update （如 /api/knowledge-point/update）。
  - `public void deleteKnowledgePoint(@RequestBody KnowledgePointRequest request)`
    删除知识点（连带逻辑删除其题目绑定）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/knowledge-point/delete （如 /api/knowledge-point/delete）。
  - `public void saveQuestions(@RequestBody KnowledgePointQuestionRequest request)`
    保存知识点-题目绑定（全量替换）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/knowledge-point/questions （如 /api/knowledge-point/questions）。

    **功能**：将题目库（t_template）中选中的题目整体绑定到知识点—— 先清空旧绑定再写入新绑定（事务内完成）。题目不能在此新增。

    **请求参数**：KnowledgePointQuestionRequest（@RequestBody JSON： knowledgePointId + questionIds[]）。
  - `public List<TemplateView> listQuestions(@RequestParam("knowledgePointId") String knowledgePointId)`
    查询知识点已绑定的题目列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/knowledge-point/questions （如 /api/knowledge-point/questions?knowledgePointId=xxx）。

    **功能**：返回该知识点已绑定的题目库题目（保持绑定顺序）， 供前端编辑绑定弹窗回显已选题目。

    **返回值结构**：TemplateView 列表。
  - `public List<TemplateView> listMatchedQuestions(@RequestParam("knowledgePointId") String knowledgePointId)`
    查询题库中「知识点标签」匹配该知识点的题目（无论是否已绑定）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/knowledge-point/questions/matched （如 /api/knowledge-point/questions/matched?knowledgePointId=xxx）。

    **功能**：题目在入库时带 subject/chapter/section/knowledgePoint 标签 （或旧数据 template JSON 内 attribute.knowledgePoint 快照），本接口按知识点名称 精确匹配这些标签，供教研平台「点击知识点自动列出关联题目并一键绑定」使用。

    **返回值结构**：TemplateView 列表（含未绑定的匹配题目，前端据此去重）。

### `api/src/main/java/cn/wisestar/server/api/MallGoodsApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class MallGoodsApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  积分商城商品接口。
  **定位**：商品列表学员端/后台共用（学员端仅看上架）；商品增删改由后台老师维护。
- 方法:
  - `public List<MallGoodsView> listGoods(@RequestParam(required = false) Integer status)`
    商品列表（学员端传 status=1 仅上架；后台不传返回全部）。
  - `public void createGoods(@RequestBody MallGoodsRequest request)`
    新增商品。
  - `public void updateGoods(@RequestBody MallGoodsRequest request)`
    编辑商品。
  - `public void deleteGoods(@RequestBody MallGoodsRequest request)`
    删除商品（逻辑删除）。

### `api/src/main/java/cn/wisestar/server/api/MallOrderApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class MallOrderApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  学币兑换订单接口。
  **定位**：学员端商城兑换与兑换记录（isAuthenticated）； 老师端核销申请列表与核销操作（mall:list / mall:update）。
- 方法:
  - `public MallOrderView create(@RequestBody MallOrderRequest request)`
    学员发起兑换（创建订单，暂时扣除学币并生成核销码）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/mall/order/create。

    **权限**：isAuthenticated()（服务层校验学员身份）。
  - `public List<MallOrderView> mine()`
    学员兑换记录（我的订单）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/mall/order/mine。

    **权限**：isAuthenticated()（服务层校验学员身份）。
  - `public List<MallOrderView> list(MallOrderRequest request)`
    老师端核销申请列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/mall/order/list?status=&keyword=。

    **权限**：hasAuthority('mall:list')。
  - `public MallOrderView verify(@RequestBody MallOrderRequest request)`
    老师端核销（凭核销码完成订单，学币正式扣除）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/mall/order/verify。

    **权限**：hasAuthority('mall:update')。

### `api/src/main/java/cn/wisestar/server/api/OrderApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class OrderApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  学员订单接口（学员管理模块）。
  **定位**：管理端「学员管理 → 订单管理」页面数据源——为学员创建订单 开通 AI 自习室权限（学科多选 × 年级多选 × 教材版本 × 账号时长）。
- 方法:
  - `public OrderView createOrder(@RequestBody OrderRequest request)`
    创建订单。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/order/create（如 /api/order/create）。

    **功能**：校验学员/学科/年级/时长 → 服务端计算有效期 → 同一事务内写入订单主表与权限表（学科×年级笛卡尔积展开）。

    **请求参数**：OrderRequest（@RequestBody JSON： studentId/subjectIds[]/grades[]/version/duration/durationUnit）。

    **返回值结构**：OrderView（含学员信息、学科名称、有效期）。
  - `public PaginationResponse<OrderView> listOrders(OrderQuery query)`
    订单分页列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/order/list（如 /api/order/list）。

    **请求参数**：OrderQuery（GET 参数：current/pageSize/studentId/studentName/status）。

    **返回值结构**：PaginationResponse（total + list）。
  - `public void cancelOrder(@RequestBody OrderRequest request)`
    作废订单。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/order/cancel（如 /api/order/cancel）。

    **功能**：订单状态置 0 作废，并逻辑删除该订单的全部权限行。
  - `public void deleteOrder(@RequestBody OrderRequest request)`
    删除订单。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/order/delete（如 /api/order/delete）。

    **功能**：逻辑删除订单与该订单的全部权限行。

### `api/src/main/java/cn/wisestar/server/api/PracticeApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class PracticeApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  练习接口（学员端练习交卷落库）。
  **定位**：AI 自习室学员端练习闭环的数据落点——交卷后调用本接口， 后端复核判分并写入 t_practice_record / t_practice_detail（错题标记）。
- 方法:
  - `public PracticeResultView submitPractice(@RequestBody PracticeSubmitRequest request)`
    提交一次练习（交卷落库 + 错题标记）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/practice/submit （如 /api/practice/submit）。

    **功能**：接收前端逐题作答结果，后端按题目 id 回源并复核判分， 写入练习会话记录与逐题明细；is_correct=0 的明细即错题，供错题本查询。

    **请求参数**：PracticeSubmitRequest（@RequestBody JSON： mode/repoId/durationMs/items[{questionId, answer}]）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("isAuthenticated()")（学员端登录即可提交）。

    **调用的下层 Service**：PracticeService#submitPractice(PracticeSubmitRequest)。
  - `public PaginationResponse<WrongQuestionView> listWrongQuestions(WrongQuestionQuery query)`
    分页查询错题库（题目 × 学员聚合）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/practice/wrong-list （如 /api/practice/wrong-list）。

    **功能**：管理端「错题库管理」页面数据源——从 t_practice_detail（is_correct=0） 按题目 + 学员聚合错题，返回题目信息、学员、累计错误次数、最近做错时间与最近答案。

    **请求参数**：WrongQuestionQuery（Query 参数： repoId/questionType/keyword/startTime/endTime/current/pageSize）。

    **返回值结构**：PaginationResponse（total + list， 元素为 WrongQuestionView）。

    **权限**：@PreAuthorize("isAuthenticated()")（登录用户可查看错题库）。

    **调用的下层 Service**：PracticeService#listWrongQuestions(WrongQuestionQuery)。
  - `public void saveWrongReason(@RequestBody WrongReasonRequest request)`
    保存错题错误归因（学员标注）。

### `api/src/main/java/cn/wisestar/server/api/ProjectApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class ProjectApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  问卷项目接口（ProjectApi）。
  **所属模块**：api 模块（Web 接口层，Spring MVC REST Controller）。

  **类职责**：提供问卷项目的管理接口：项目列表/详情/设置、项目增删改、 参与者管理（列表/增删/下载/导入）、回收站（删除列表/彻底删除/恢复）、 以及问卷编辑器用到的公共选择接口（用户/部门/角色/岗位/字典/模板/题库/标签）。

  **请求路径前缀**：类级路径为 `${api.prefix`/project}（api.prefix 通常为 /api）， 各方法在类级路径上追加子路径（如 /api/project/list、/api/project/partner/list 等）。

  **被谁调用**：前端管理后台：项目列表页、问卷编辑器（设计器）、参与者管理页、 回收站页面。除 select* 系列（编辑器内选择器）外，大部分接口要求登录并校验权限点。

  **依赖的服务**：

  - ProjectService——项目 CRUD、回收站、设置；
  - ProjectPartnerService——参与者管理；
  - UserService——编辑器用户选择；
  - PositionService / DeptService / RoleService——编辑器岗位/部门/角色选择；
  - DictService / TemplateService / RepoService / TagService——编辑器字典/模板/题库/标签选择。

  **数据权限说明**：getProject/setting/update/delete 及 partner 系列接口使用了 自定义注解 EnableDataPerm，用于校验当前用户对指定项目的数据权限 （项目所有者/参与者），key 表达式指定从参数中取项目 id。

  **数据流概览**：前端 HTTP 请求 → 本类方法（权限 + 数据权限注解拦截）→ 对应 shared Service 接口 → rdbms 实现 → MyBatis Mapper → 数据库 → 视图 DTO 返回。
- 方法:
  - `public PaginationResponse<ProjectView> listProject(ProjectQuery query)`
    获取项目列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/project/list（如 /api/project/list）。

    **功能**：分页查询当前用户有权限的项目列表（支持按名称/状态/创建时间等筛选）， 供项目列表页展示。

    **请求参数**：ProjectQuery（GET 查询参数）——分页参数 + 筛选条件。

    **返回值结构**：PaginationResponse（分页包装的项目列表）。

    **权限**：@PreAuthorize("hasAuthority('project:list')")。

    **调用的下层 Service**：ProjectService#listProject(ProjectQuery)。
  - `public ProjectView getProject(String id)`
    获取项目信息。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/project?id=xxx（如 /api/project?id=xxx）。

    **功能**：按项目 id 获取项目详情（基本信息 + 问卷 schema + 配置）， 供问卷编辑器打开项目时加载。

    **请求参数**：id（GET 查询参数，项目 id）。

    **返回值结构**：ProjectView（项目视图：基本信息 + 问卷 schema）。

    **权限/数据权限**：@PreAuthorize("hasAuthority('project:detail')") + @EnableDataPerm(key = "#id")——校验对 #id 项目的操作权。

    **调用的下层 Service**：ProjectService#getProject(String)。
  - `public ProjectSetting getSetting(ProjectQuery query)`
    获取项目设置。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/project/setting（如 /api/project/setting?id=xxx）。

    **功能**：获取项目设置信息（问卷风格、答题限制、白名单、随机规则等 ProjectSetting），供项目设置页加载。

    **请求参数**：ProjectQuery（GET 查询参数，含项目 id）。

    **返回值结构**：ProjectSetting（项目设置视图）。

    **权限/数据权限**：@PreAuthorize("hasAuthority('project:detail')") + @EnableDataPerm(key = "#id")（key 指向 query 中的 id）。

    **调用的下层 Service**：ProjectService#getSetting(ProjectQuery)。
  - `public ProjectView addProject(@RequestBody ProjectRequest project)`
    添加项目。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/create（如 /api/project/create）。

    **功能**：新建问卷项目（基本信息 + 初始问卷 schema），返回新项目视图。

    **请求参数**：ProjectRequest（@RequestBody JSON）——项目名称、类型、 描述、schema 等。

    **返回值结构**：ProjectView（新建的项目视图，含新项目 id）。

    **权限**：@PreAuthorize("hasAuthority('project:create')")。

    **调用的下层 Service**：ProjectService#addProject(ProjectRequest)。
  - `public void updateProject(@RequestBody ProjectRequest project)`
    更新项目。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/update（如 /api/project/update）。

    **功能**：更新项目基本信息与问卷 schema（编辑器保存时调用）。

    **请求参数**：ProjectRequest（@RequestBody JSON，含项目 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限/数据权限**：@PreAuthorize("hasAuthority('project:update')") + @EnableDataPerm(key = "#project.id")。

    **调用的下层 Service**：ProjectService#updateProject(ProjectRequest)。
  - `public void deleteProject(@RequestBody ProjectRequest project)`
    删除项目（放入回收站）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/delete（如 /api/project/delete）。

    **功能**：将项目标记删除（逻辑删除，进入回收站，可恢复）。

    **请求参数**：ProjectRequest（@RequestBody JSON，含项目 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限/数据权限**：@PreAuthorize("hasAuthority('project:delete')") + @EnableDataPerm(key = "#project.id")。

    **调用的下层 Service**：ProjectService#deleteProject(ProjectRequest)。
  - `public PaginationResponse<ProjectPartnerView> listProjectPartner(ProjectPartnerQuery query)`
    获取项目参与者列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/project/partner/list （如 /api/project/partner/list?projectId=xxx）。

    **功能**：分页查询指定项目的参与者（协作成员）列表，供参与者管理页展示。

    **请求参数**：ProjectPartnerQuery（GET 查询参数，含 projectId 与分页条件）。

    **返回值结构**：PaginationResponse。

    **数据权限**：@EnableDataPerm(key = "#query.projectId")。

    **调用的下层 Service**：ProjectPartnerService#listProjectPartner(ProjectPartnerQuery)。
  - `public void addProjectPartner(@RequestBody ProjectPartnerRequest request)`
    添加项目参与者。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/partner/create （如 /api/project/partner/create）。

    **功能**：向项目添加协作成员（指定用户 + 角色权限）。

    **请求参数**：ProjectPartnerRequest（@RequestBody JSON，含 projectId、userId、角色等）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **数据权限**：@EnableDataPerm(key = "#request.projectId")。

    **调用的下层 Service**：ProjectPartnerService#addProjectPartner(ProjectPartnerRequest)。
  - `public void deleteProjectPartner(@RequestBody ProjectPartnerRequest request)`
    删除项目参与者。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/partner/delete （如 /api/project/partner/delete）。

    **功能**：移除项目中的某个协作成员。

    **请求参数**：ProjectPartnerRequest（@RequestBody JSON，含 projectId 与参与者 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **数据权限**：@EnableDataPerm(key = "#request.projectId")。

    **调用的下层 Service**：ProjectPartnerService#deleteProjectPartner(ProjectPartnerRequest)。
  - `public void downloadPartner(ProjectPartnerQuery query)`
    下载项目参与者列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/project/partner/download （如 /api/project/partner/download?projectId=xxx）。

    **功能**：将指定项目的参与者列表导出为 Excel 文件（响应流由服务层写出）。

    **请求参数**：ProjectPartnerQuery（GET 查询参数，含 projectId）。

    **返回值结构**：无方法返回值；服务层直接把 Excel 写入 HTTP 响应流。

    **数据权限**：@EnableDataPerm(key = "#query.projectId")。

    **调用的下层 Service**：ProjectPartnerService#downloadPartner(ProjectPartnerQuery)。
  - `public void importPartner(WhiteListRequest request)`
    导入项目参与者列表。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/partner/import （如 /api/project/partner/import）。

    **功能**：按模板 Excel 批量导入项目参与者（与 download 下载的模板配套）。

    **请求参数**：WhiteListRequest（multipart 表单绑定，含 projectId 与 Excel 文件）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **数据权限**：@EnableDataPerm(key = "#request.projectId")。

    **调用的下层 Service**：ProjectPartnerService#importPartner(WhiteListRequest)。
  - `public List<ProjectView> getDeleted(ProjectQuery query)`
    获取回收站里的项目列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/project/trash（如 /api/project/trash）。

    **功能**：查询当前用户已删除（回收站中）的项目列表，供回收站页面展示。

    **请求参数**：ProjectQuery（GET 查询参数，筛选条件）。

    **返回值结构**：`List`（已删除项目列表）。

    **权限**：@PreAuthorize("hasAuthority('project:list')")。

    **调用的下层 Service**：ProjectService#getDeleted(ProjectQuery)。
  - `public void batchDestroyProject(@RequestBody ProjectRequest request)`
    从回收站彻底移除项目。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/destroy（如 /api/project/destroy）。

    **功能**：将回收站中的项目（支持多个 id）物理删除（连同答案等关联数据）， 不可恢复。

    **请求参数**：ProjectRequest（@RequestBody JSON，id 字段支持逗号分隔多个项目 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('project:create')")——复用创建权限。

    **调用的下层 Service**：ProjectService#batchDestroyProject(ProjectRequest)。
  - `public void restoreProject(@RequestBody ProjectRequest request)`
    从回收站里面恢复项目。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/restore（如 /api/project/restore）。

    **功能**：将回收站中的项目（支持多个 id）恢复为正常状态。

    **请求参数**：ProjectRequest（@RequestBody JSON，含项目 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('project:create')")——复用创建权限。

    **调用的下层 Service**：ProjectService#restoreProject(ProjectRequest)。
  - `public List<UserInfo> selectUser(@RequestBody SelectUserRequest request)`
    编辑器里面获取用户信息。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/selectUser（如 /api/project/selectUser）。

    **功能**：问卷编辑器中的"用户选择器"数据源——按关键字/条件搜索用户列表 （如权限设置、条件跳转等场景引用用户）。

    **请求参数**：SelectUserRequest（@RequestBody JSON，搜索条件）。

    **返回值结构**：`List`（匹配的用户列表）。

    **调用的下层 Service**：UserService#selectUsers(SelectUserRequest)。
  - `public List<DeptView> selectDept(@RequestBody SelectDeptRequest request)`
    编辑器里面获取部门信息。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/selectDept（如 /api/project/selectDept）。

    **功能**：问卷编辑器中的"部门选择器"数据源——按条件查询部门列表。

    **请求参数**：SelectDeptRequest（@RequestBody JSON，搜索条件）。

    **返回值结构**：`List`（部门视图列表）。

    **调用的下层 Service**：DeptService#listDept(SelectDeptRequest)（复用部门查询，参数类型不同）。
  - `public List<RoleView> selectRole(@RequestBody SelectRoleRequest request)`
    编辑器里面获取角色信息。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/selectRole（如 /api/project/selectRole）。

    **功能**：问卷编辑器中的"角色选择器"数据源——按条件查询角色列表。

    **请求参数**：SelectRoleRequest（@RequestBody JSON，搜索条件）。

    **返回值结构**：`List`（角色视图列表）。

    **调用的下层 Service**：RoleService#selectRoles(SelectRoleRequest)。
  - `public List<PositionView> selectPosition(@RequestBody SelectPositionRequest request)`
    编辑器里面获取岗位信息。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/selectPosition （如 /api/project/selectPosition）。

    **功能**：问卷编辑器中的"岗位选择器"数据源——按条件查询岗位列表。

    **请求参数**：SelectPositionRequest（@RequestBody JSON，搜索条件）。

    **返回值结构**：`List`（岗位视图列表）。

    **调用的下层 Service**：PositionService#selectPositions(SelectPositionRequest)。
  - `public List<CommDictView> selectDict()`
    编辑器里面获取字典。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/selectDict（如 /api/project/selectDict）。

    **功能**：问卷编辑器中的"字典选择器"数据源——返回全部字典列表（无需参数）。

    **请求参数**：无。

    **返回值结构**：`List`（字典视图列表）。

    **调用的下层 Service**：DictService#selectDict()。
  - `public Map<String, List<TemplateView>> selectTemplate(@RequestBody SelectTemplateRequest request)`
    编辑器里面获取题库模板。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/selectTemplate （如 /api/project/selectTemplate）。

    **功能**：问卷编辑器中的"题库模板选择器"数据源——按分类/条件查询模板， 返回按分类分组的模板 Map。

    **请求参数**：SelectTemplateRequest（@RequestBody JSON，模板筛选条件）。

    **返回值结构**：`Map>`——key 为模板分类， value 为该分类下的模板列表。

    **调用的下层 Service**：TemplateService#selectTemplate(SelectTemplateRequest)。
  - `public List<RepoView> selectRepo(@RequestBody SelectRepoRequest request)`
    编辑器里面获取题库。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/selectRepo（如 /api/project/selectRepo）。

    **功能**：问卷编辑器中的"题库选择器"数据源——按条件查询题库列表。

    **请求参数**：SelectRepoRequest（@RequestBody JSON，题库筛选条件）。

    **返回值结构**：`List`（题库视图列表）。

    **调用的下层 Service**：RepoService#selectRepo(SelectRepoRequest)。
  - `public Set<String> selectTag(@RequestBody SelectTagRequest request)`
    编辑器里面获取标签。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/project/selectTag（如 /api/project/selectTag）。

    **功能**：问卷编辑器中的"标签选择器"数据源——按条件查询标签集合。

    **请求参数**：SelectTagRequest（@RequestBody JSON，标签筛选条件）。

    **返回值结构**：`Set`（去重后的标签字符串集合）。

    **调用的下层 Service**：TagService#selectTag(SelectTagRequest)。

### `api/src/main/java/cn/wisestar/server/api/RepoApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class RepoApi`
- 注解: @RequestMapping, @RequiredArgsConstructor, @RestController
- **类说明**：
  题库接口（RepoApi）。
  **所属模块**：api 模块（Web 接口层，Spring MVC REST Controller）。

  **类职责**：提供"题库"（Repo）管理接口：题库列表/详情、题库增删改、 批量创建（从模板批量入题）、模板绑定/解绑、从题库挑题、模板导入题库、 以及"我的笔记"（UserBook）的列表/增改删，和题库题目导出。

  **请求路径前缀**：类级路径为 `${api.prefix`/repo}（api.prefix 通常为 /api）， 各方法再追加子路径（如 /api/repo/list、/api/repo/book/list 等）。

  **被谁调用**：前端管理后台：题库管理页（题目库维护）、问卷编辑器 （从题库挑题）、笔记管理页（我的笔记）。

  **依赖的服务**：注入 RepoService（shared 模块接口，rdbms 模块实现）—— 负责题库 CRUD、模板绑定、挑题、笔记、导出等业务。

  **数据流概览**：前端 HTTP 请求 → 本类方法（权限注解）→ RepoService → rdbms 实现 → 题库/模板关联/笔记 Mapper → 数据库 → 视图 DTO 返回。
- 方法:
  - `public PaginationResponse<RepoView> listRepo(RepoQuery query)`
    获取题库列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/repo/list（如 /api/repo/list）。

    **功能**：分页查询题库列表（按名称/标签/类型筛选），供题库管理页展示。

    **请求参数**：RepoQuery（GET 查询参数）——分页 + 筛选条件。

    **返回值结构**：PaginationResponse（分页包装的题库列表）。

    **权限**：@PreAuthorize("hasAuthority('repo:list')")。

    **调用的下层 Service**：RepoService#listRepo(RepoQuery)。
  - `public RepoView getRpo(String id)`
    获取题库详情。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/repo（如 /api/repo?id=xxx）。 由类级路径 + 方法级 @GetMapping（无路径值）组合映射。

    **功能**：按 id 获取题库详情（含题库下题目列表）。

    **请求参数**：id（GET 查询参数，题库 id）。

    **返回值结构**：RepoView（题库详情视图）。

    **权限**：@PreAuthorize("hasAuthority('repo:detail')")。

    **调用的下层 Service**：RepoService#getRpo(String)。
  - `public void addRepo(@RequestBody RepoRequest request)`
    创建题库。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/create（如 /api/repo/create）。

    **功能**：新建题库（题库名称、类型、题目列表等）。

    **请求参数**：RepoRequest（@RequestBody JSON，题库信息）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('repo:create')")。

    **调用的下层 Service**：RepoService#addRepo(RepoRequest)。
  - `public void updateRepo(@RequestBody RepoRequest request)`
    更新题库。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/update（如 /api/repo/update）。

    **功能**：更新题库信息（含题目列表的增删改）。

    **请求参数**：RepoRequest（@RequestBody JSON，含题库 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('repo:update')")。

    **调用的下层 Service**：RepoService#updateRepo(RepoRequest)。
  - `public void deleteRepo(@RequestBody RepoRequest request)`
    删除题库。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/delete（如 /api/repo/delete）。

    **功能**：按 id 删除题库（逻辑删除，由服务层实现）。

    **请求参数**：RepoRequest（@RequestBody JSON，含题库 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('repo:delete')")。

    **调用的下层 Service**：RepoService#deleteRepo(RepoRequest)。
  - `public void batchAddRepoTemplate(@RequestBody RepoTemplateRequest request)`
    批量创建题库（从模板批量入题）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/batchCreate （如 /api/repo/batchCreate）。

    **功能**：将一批问卷/问题模板（Template）批量导入/绑定到题库，快速初始化题库。

    **请求参数**：RepoTemplateRequest（@RequestBody JSON）——题库与模板关联信息。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('repo:create')")。

    **调用的下层 Service**：RepoService#batchAddRepoTemplate(RepoTemplateRequest)。
  - `public void bindTemplates(@RequestBody RepoTemplateRequest request)`
    批量绑定已有题目到题库。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/bind（如 /api/repo/bind）。

    **功能**：将题目管理中的一批已有题目（ids）绑定到指定题库（repoId）， 仅更新题目归属，不修改题目内容；已在目标题库的题目自动跳过（幂等）。

    **请求参数**：RepoTemplateRequest（@RequestBody JSON，repoId + ids）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('repo:create')")。

    **调用的下层 Service**：RepoService#bindTemplates(RepoTemplateRequest)。
  - `public void batchUnBindTemplate(@RequestBody RepoTemplateRequest request)`
    模板解绑题库。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/unbind（如 /api/repo/unbind）。

    **功能**：将题库与一批模板解除绑定（题目从题库中移除关联，不删除模板本身）； 同时清空练习内分值（attribute.examScore / examBlankScores），避免重新加入其他练习时带出旧分值。

    **请求参数**：RepoTemplateRequest（@RequestBody JSON，题库与模板关联信息）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：无 @PreAuthorize 注解（是否要求登录由全局安全规则控制）。

    **调用的下层 Service**：RepoService#batchUnBindTemplate(RepoTemplateRequest)。
  - `public List<SurveySchema> pickQuestionFromRepo(@RequestBody List<ProjectSetting.RandomSurveyCondition> repos)`
    从题库里面挑选试题。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/pick（如 /api/repo/pick）。

    **功能**：问卷编辑器"随机抽题"场景——按随机条件（题库 + 抽题数量）从题库 挑选题目，返回可直接放入问卷 schema 的题目数据。

    **请求参数**：`List`（@RequestBody JSON 数组）—— 每个元素描述一次随机抽题条件（题库 id、抽题数等）。

    **返回值结构**：`List`（挑选出的题目 schema 列表）。

    **调用的下层 Service**：RepoService#pickQuestionFromRepo(List)。
  - `public void importFromTemplate(RepoTemplateRequest request)`
    从模板导入题库。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/import（如 /api/repo/import）。

    **功能**：将指定模板导入到题库中（与 batchCreate 类似，导入方向为模板 → 题库）。

    **请求参数**：RepoTemplateRequest（@RequestBody JSON，题库与模板关联信息）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：无 @PreAuthorize 注解。

    **调用的下层 Service**：RepoService#importFromTemplate(RepoTemplateRequest)。
  - `public PaginationResponse<UserBookView> listUserBook(UserBookQuery query)`
    我的笔记列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/repo/book/list（如 /api/repo/book/list）。

    **功能**：分页查询当前用户的"笔记"列表（对题目/知识点的个人笔记）， 供笔记管理页展示。

    **请求参数**：UserBookQuery（GET 查询参数）——分页 + 筛选条件。

    **返回值结构**：PaginationResponse。

    **权限**：@PreAuthorize("hasAuthority('repo:book')")。

    **调用的下层 Service**：RepoService#listUserBook(UserBookQuery)。
  - `public void createUserBook(@RequestBody UserBookRequest request)`
    创建笔记。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/book/create （如 /api/repo/book/create）。

    **功能**：为当前用户创建一条笔记。

    **请求参数**：UserBookRequest（@RequestBody JSON）——笔记内容及关联题目/知识点。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('repo:book')")。

    **调用的下层 Service**：RepoService#createUserBook(UserBookRequest)。
  - `public UserBookView updateUserBook(@RequestBody UserBookRequest request)`
    更新笔记。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/book/update （如 /api/repo/book/update）。

    **功能**：更新笔记内容，返回更新后的笔记视图。

    **请求参数**：UserBookRequest（@RequestBody JSON，含笔记 id）。

    **返回值结构**：UserBookView（更新后的笔记视图）。

    **权限**：@PreAuthorize("hasAuthority('repo:book')")。

    **调用的下层 Service**：RepoService#updateUserBook(UserBookRequest)。
  - `public void deleteUserBook(@RequestBody UserBookRequest request)`
    删除笔记。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/book/delete （如 /api/repo/book/delete）。

    **功能**：按 id 删除当前用户的笔记。

    **请求参数**：UserBookRequest（@RequestBody JSON，含笔记 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('repo:book')")。

    **调用的下层 Service**：RepoService#deleteUserBook(UserBookRequest)。
  - `public void exportRepoQuestions(RepoRequest request)`
    导出题库题目。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/repo/export（如 /api/repo/export）。

    **功能**：将题库中的题目导出为 Excel 文件（响应流由服务层写出）。 注意：权限注解被注释掉（见代码中的 @PreAuthorize 注释行），当前任何登录用户均可导出。

    **请求参数**：RepoRequest（GET 查询参数，含题库 id/筛选条件）。

    **返回值结构**：无方法返回值；服务层直接把 Excel 写入 HTTP 响应流。

    **调用的下层 Service**：RepoService#exportRepoQuestions(RepoRequest)。
  - `public void downloadImportTemplate()`
    下载题目导入模板（标准单表 29 列，含「填写说明」sheet）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/repo/import/template （如 /api/repo/import/template）。

    **功能**：下载仅供导入使用的空模板（不包含任何题目数据，避免把存量题目 当作模板行再次导入）。模板列：学科/题型/章节/知识点/题目/选项A~H/难易程度/ 正确答案1~12/解析/标签。

    **返回值结构**：无方法返回值；服务层直接把 Excel 写入 HTTP 响应流。

    **权限**：与导入接口一致，不设 @PreAuthorize（题目管理页导入入口已有页面权限）。
    **调用的下层 Service**：RepoService#downloadImportTemplate()。
  - `public List<RepoView> myRepos()`
    学员端「我的题库」。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/repo/my（如 /api/repo/my）。

    **功能**：查询当前登录学员可练习的题库列表 （老师手动分配 ∪ 系统按标签自动匹配），每项回填题目总数。 学员端选题页以此为数据源，只能选择分配到的题库，不能勾选单题。

    **返回值结构**：List。

    **权限**：@PreAuthorize("isAuthenticated()")——登录即可（学员端入口）。

    **调用的下层 Service**：RepoService#myRepos()。
  - `public void assignRepo(@RequestBody RepoAssignRequest request)`
    老师手动分配题库给学员。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/assign（如 /api/repo/assign）。

    **功能**：为指定学员批量分配题库（幂等，已分配的跳过）。 分配后学员端「我的题库」即可见。

    **请求参数**：RepoAssignRequest（@RequestBody JSON）——userId + repoIds。

    **权限**：@PreAuthorize("hasAuthority('repo:list')")。

    **调用的下层 Service**：RepoService#assignRepo(String, java.util.List)。
  - `public void deleteAssign(@RequestBody RepoAssignRequest request)`
    删除分配记录。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/assign/delete（如 /api/repo/assign/delete）。

    **功能**：按分配记录 id 批量删除（逻辑删除），删除后学员端不再可见该题库。

    **请求参数**：RepoAssignRequest（@RequestBody JSON）——ids。

    **权限**：@PreAuthorize("hasAuthority('repo:list')")。

    **调用的下层 Service**：RepoService#deleteAssign(java.util.List)。
  - `public List<RepoAssignView> listAssign(String userId)`
    查询学员分配记录（管理端）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/repo/assign/list（如 /api/repo/assign/list）。

    **功能**：查询指定学员（或全部）的题库分配记录，含学员姓名/题库名称。

    **请求参数**：userId（GET 查询参数，可选，为空查全部）。

    **返回值结构**：List。

    **权限**：@PreAuthorize("hasAuthority('repo:list')")。

    **调用的下层 Service**：RepoService#listAssign(String)。
  - `public Set<String> getUserTags(String userId)`
    查询学员标签。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/repo/user/tags（如 /api/repo/user/tags）。

    **功能**：查询学员标签（category=user），用于「按标签自动分配题库」的规则设置。

    **请求参数**：userId（GET 查询参数，必填）。

    **返回值结构**：Set。

    **权限**：@PreAuthorize("hasAuthority('repo:list')")。

    **调用的下层 Service**：RepoService#getUserTags(String)。
  - `public void saveUserTags(@RequestBody RepoAssignRequest request)`
    保存学员标签。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/repo/user/tags（如 /api/repo/user/tags）。

    **功能**：覆盖式保存学员标签（category=user）。题库 tag 与学员标签有交集时， 系统自动将该题库分配到该学员的「我的题库」。

    **请求参数**：RepoAssignRequest（@RequestBody JSON）——userId + tags。

    **权限**：@PreAuthorize("hasAuthority('repo:list')")。

    **调用的下层 Service**：RepoService#saveUserTags(String, String[])。
  - `public RepoBindLocationView listRepoLocations(@RequestParam("repoId") String repoId)`
    练习绑定位置反查。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/repo/locations?repoId=（如 /api/repo/locations）。

    **功能**：按 t_chapter_repo / t_section_repo 反查该练习绑定的章节/小节， 供练习详情页回显知识投放范围并跳转习题列表页定位。

    **权限**：@PreAuthorize("hasAuthority('repo:list')")。

    **调用的下层 Service**：RepoService#listRepoLocations(String)。
  - `public List<NodeQuestionView> listNodeQuestions(@RequestParam("nodeType") String nodeType, @RequestParam("nodeId") String nodeId, @RequestParam(value = "withAnswer", required = false) Boolean withAnswer)`
    节点刷题内容预览（习题列表页）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/repo/node/questions?nodeType=&nodeId=&withAnswer=。

    **功能**：按节点聚合该节点刷题将命中的题目（练习题目 + 知识点直绑题目， 与学员端 study/questions 同语义），供管理端核验绑定配置。

    **权限**：@PreAuthorize("hasAuthority('repo:list')")。

    **调用的下层 Service**：RepoService#listNodeQuestions(String, String, Boolean)。

### `api/src/main/java/cn/wisestar/server/api/ReportApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class ReportApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  问卷报表接口（ReportApi）。
  **所属模块**：api 模块（Web 接口层，Spring MVC REST Controller）。

  **类职责**：提供问卷统计分析报表的数据查询接口——当前仅包含按短链接 （shortId）获取报表数据。

  **请求路径前缀**：类级路径为 `${api.prefix`/report}（api.prefix 通常为 /api）， 当前方法路径为 ${api.prefix}/report/{shortId}。

  **被谁调用**：前端管理后台"问卷报表"页面（需要 project:report 权限）。

  **依赖的服务**：注入 ReportService（shared 模块接口，rdbms 模块实现）—— 负责从项目/答卷数据聚合生成报表统计结果。

  **数据流**：前端 GET /api/report/{shortId} → 本类 getData(shortId) → ReportService#getData → rdbms 实现 → 答卷/项目 Mapper 聚合统计 → ReportData → JSON。
- 方法:
  - `public ReportData getData(@PathVariable String shortId)`
    获取问卷报表数据。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/report/{shortId} （如 /api/report/abc123）。

    **功能**：按项目的短链接标识（shortId）获取该问卷的统计分析报表数据 （答卷总量、回收率、各题选项统计等），供报表页面渲染。

    **请求参数**：shortId（@PathVariable 路径变量，项目短链接标识）。

    **返回值结构**：ReportData（报表统计视图：基础统计 + 各题明细统计）。

    **权限**：@PreAuthorize("hasAuthority('project:report')")——需要该权限点。

    **调用的下层 Service**：ReportService#getData(String)。

### `api/src/main/java/cn/wisestar/server/api/SectionApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class SectionApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  小节管理接口（知识管理板块三级维度）。
  **定位**：管理端「知识管理 → 小节」页面数据源——小节 CRUD； 小节挂载于章节下（chapterId），列表按章节过滤，供顶部下拉三级联动使用； 小节的"内容设置/练习设置"为 JSON 文本透传存储。
- 方法:
  - `public List<SectionView> listSections(SectionRequest query)`
    小节列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/section/list（如 /api/section/list）。

    **请求参数**：SectionRequest（Query 参数：chapterId 可选， 不传返回全部小节）。

    **功能**：返回小节列表（sort 升序），每项含该小节下的知识点数 knowledgePointCount、已绑定题库数 repoCount 与内容/练习设置 JSON 原文。

    **返回值结构**：SectionView 列表。
  - `public String addSection(@RequestBody SectionRequest request)`
    新增小节。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/section/create（如 /api/section/create）。

    **请求参数**：SectionRequest（@RequestBody JSON： chapterId/name/sort/content/practice）。

    **返回值结构**：新小节 id（String）。
  - `public ImportResultView importSections(SectionImportRequest request)`
    批量导入小节（multipart 表单：chapterId + file）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/section/import。

    **功能**：解析 Excel（列：小节名/排序(选填)，首行为表头跳过）， 按 chapterId+name 去重后批量写入 t_section。

    **权限**：hasAuthority('knowledge:create')。
  - `public void updateSection(@RequestBody SectionRequest request)`
    更新小节（含内容设置/练习设置 JSON）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/section/update（如 /api/section/update）。
  - `public void deleteSection(@RequestBody SectionRequest request)`
    删除小节（级联逻辑删除其下知识点及题库绑定）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/section/delete（如 /api/section/delete）。
  - `public void saveRepos(@RequestBody SectionRepoRequest request)`
    保存小节-题库绑定（全量替换）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/section/repos （如 /api/section/repos）。

    **功能**：将题库管理（t_repo）中选中的题库整体绑定到小节—— 先清空旧绑定再写入新绑定（事务内完成）。题库只能来自题库管理，不能在此新增。

    **请求参数**：SectionRepoRequest（@RequestBody JSON： sectionId + repoIds[]）。
  - `public List<SectionRepoView> listRepos(@RequestParam("sectionId") String sectionId)`
    查询小节已绑定的题库列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/section/repos （如 /api/section/repos?sectionId=xxx）。

    **功能**：返回该小节已绑定的题库（保持绑定顺序）， 供前端编辑绑定弹窗回显已选题库。

    **返回值结构**：SectionRepoView 列表。

### `api/src/main/java/cn/wisestar/server/api/StudentApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class StudentApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  学员管理接口（学员管理模块）。
  **定位**：管理端「学员管理 → 学员列表」页面数据源——学员 CRUD； 新增学员自动生成「字母 + 6 位数字」学号（如 a000001）并创建学员登录账号（初始密码 123456）。
- 方法:
  - `public StudentView addStudent(@RequestBody StudentRequest request)`
    新增学员。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/student/create（如 /api/student/create）。

    **功能**：校验姓名/联系号码必填与组合查重 → 自动生成「字母 + 6 位数字」唯一学号 → 同一事务内写入学员主数据与登录账号（学号即账号，初始密码 123456）。

    **请求参数**：StudentRequest（@RequestBody JSON：name/age/phone/school/campus）。

    **返回值结构**：StudentView（含系统生成的学号）。
  - `public PaginationResponse<StudentView> listStudents(StudentQuery query)`
    学员分页列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/list（如 /api/student/list）。

    **请求参数**：StudentQuery（GET 参数：current/pageSize/name/studentNo/phone）。

    **返回值结构**：PaginationResponse（total + list）。
  - `public void updateStudent(@RequestBody StudentRequest request)`
    更新学员（学号不可修改）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/student/update（如 /api/student/update）。
  - `public void deleteStudent(@RequestBody StudentRequest request)`
    删除学员（逻辑删除）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/student/delete（如 /api/student/delete）。
  - `public StudentView myStudentInfo()`
    当前登录学员信息（学员端档案展示）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/me（如 /api/student/me）。

    **功能**：按当前登录用户ID查询 t_student 返回学员视图（学号/姓名/年龄/ 联系号码/学校/校区）；系统用户调用返回 400 校验异常。

    **返回值结构**：StudentView。
  - `public StudentPermissionView permissions()`
    学员有效权限（多条有效订单合并）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/permissions。

    **功能**：返回可访问的学科（含名称）、年级、教材版本（expire_at > NOW()）， 供学员端按订单授予范围过滤内容。

    **权限**：isAuthenticated()（学员端登录即可）。
  - `public StudentStatsView stats()`
    学员学习统计（首页真实化，基于练习记录聚合）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/stats。

    **权限**：isAuthenticated()（学员端登录即可）。
  - `public List<StudentSubjectView> studySubjects()`
    学员端学科列表（按订单有效权限过滤）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/study/subjects。

    **权限**：isAuthenticated()（服务层校验学员身份与订单权限）。
  - `public List<ChapterView> studyChapters(@RequestParam(required = false) String subjectId, @RequestParam(required = false) String grade)`
    学员端章节列表（按订单权限过滤学科与年级）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/study/chapters?subjectId=&grade=。
  - `public List<SectionView> studySections(@RequestParam(required = false) String chapterId)`
    学员端小节列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/study/sections?chapterId=。
  - `public List<KnowledgePointView> studyPoints(@RequestParam(required = false) String sectionId)`
    学员端知识点列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/study/points?sectionId=。
  - `public StudentStudyProgressView studyProgress(@RequestParam(required = false) String subjectId, @RequestParam(required = false) String versionId)`
    学员端学科学习进度（章节 → 知识点掌握度/评级/薄弱）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/study/progress?subjectId=&versionId=。
  - `public List<StudentQuestionView> studyQuestions(@RequestParam(required = false) String sectionId, @RequestParam(required = false) String knowledgePointId, @RequestParam(required = false) List<String> knowledgePointIds, @RequestParam(required = false) String repoId, @RequestParam(required = false) String questionId, @RequestParam(required = false) Integer count, @RequestParam(required = false) Integer perKp, @RequestParam(required = false) Boolean groupByKp, @RequestParam(required = false) List<String> types, @RequestParam(required = false) String difficulty, @RequestParam(required = false) Boolean random, @RequestParam(required = false) Boolean exposeAnswer, @RequestParam(required = false) String usage)`
    学员端练习/试炼题目（剥离标准答案）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/study/questions?sectionId=&knowledgePointIds=&count=&types=&difficulty=&random=&perKp=。
  - `public SectionPracticeConfig practiceConfig(@RequestParam(required = false) String sectionId)`
    学员端小节练习配置（出题策略来源）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/practice/config?sectionId=。
  - `public void uploadActivity(@RequestBody StudentActivityRequest request)`
    学员端实时位置上报（路由变化/进入习题时调用）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/student/activity。

    **权限**：isAuthenticated()（服务层校验学员身份）。
  - `public List<StudentActivityView> activities()`
    后台学员实时位置列表（老师监控）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/activities。

    **权限**：hasAuthority('student:list')（后台老师/管理员）。
  - `public void addCoin(@RequestBody StudentCoinRequest request)`
    老师给学员发放学币（student:update）。
  - `public StudentPreviewCompleteView completePreview(@RequestBody StudentPreviewCompleteRequest request)`
    学员预习完成（知识点预习讲完后的「预习完成」按钮）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/student/preview/complete。

    **功能**：标记该小节/知识点预习完成（学习完成度 100%）并结算奖励 （学习币 +5 / 学海积分 +3）；同一目标仅首次结算，重复调用不重复发放。

    **权限**：isAuthenticated()（服务层校验学员身份）。
  - `public StudentProfileView profile()`
    个人中心档案（姓名/学号/头衔/累计积分/知识点/薄弱数）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/profile。
  - `public StudentPointsView points()`
    个人中心-积分板块（积分/头衔/下一目标/规则/最近明细）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/points。
  - `public StudentCoinsView coins()`
    本学期学习币（分学科 + 手动发币，单科上限 10000）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/coins。
  - `public StudentTodayView today()`
    学员端主页今日总览 + 积分获取引导。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/today。
  - `public List<StudentWeakView> weakList()`
    薄弱知识点列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/weak/list。
  - `public StudentKnowledgeDetailView knowledgeDetail(@RequestParam String knowledgePointId)`
    知识点详情（掌握度/评级/薄弱/预习状态）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/student/knowledge/detail?knowledgePointId=。
  - `public StudentPreviewCompleteView completeLearning(@RequestBody StudentLearningCompleteRequest request)`
    学习完成统一结算（预习/练习/试炼/错题订正等）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/student/learning/complete。
  - `public StudentWrongRedoView wrongRedo(@RequestBody StudentWrongRedoRequest request)`
    错题重做（答对则订正、移出错题本、刷新薄弱并结算奖励）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/student/wrong/redo。
  - `public StudentWeakConquerView weakConquer(@RequestBody StudentWeakConquerRequest request)`
    薄弱知识点攻克（复测达标后消除薄弱并结算奖励）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/student/weak/conquer。

### `api/src/main/java/cn/wisestar/server/api/StudentCheckinApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class StudentCheckinApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  学员每日签到接口。
- 方法:
  - `public StudentCheckinView view()`
    查询当日签到状态。
  - `public StudentCheckinView checkin()`
    领取当日签到奖励。

### `api/src/main/java/cn/wisestar/server/api/StudentOnlineChestApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class StudentOnlineChestApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  学员端在线时长宝箱接口。
  宝箱状态随 `POST /student/study/heartbeat` 返回；本接口用于领取已达档宝箱， 按「学员 + 档位 + 日期」幂等，复用统一奖励账本。
- 方法:
  - `public StudentOnlineChestClaimView claim(@RequestBody StudentOnlineChestClaimRequest request)`
    领取在线时长宝箱。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/student/online/chest/claim（如 /api/student/online/chest/claim）。

    **请求参数**：StudentOnlineChestClaimRequest（tier 档位分钟数 30/60/120；subjectId 可空）。

    **返回值结构**：StudentOnlineChestClaimView（本次到账学习币 + 最新宝箱状态）。

### `api/src/main/java/cn/wisestar/server/api/StudentStudyApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class StudentStudyApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  学员学习会话与总结接口。
  学员端周期性上报心跳以累计学习时长；会话累计满 60 分钟自动生成当日学习总结。 学员可查看本人总结，教师/管理员可查看指定学员总结。
- 方法:
  - `public StudyHeartbeatView heartbeat(@RequestBody(required = false) StudyHeartbeatRequest request)`
    学员学习心跳（每 5 分钟及页面可见性变化时上报）。
  - `public StudySummaryView getMySummary()`
    学员查看本人当日学习总结。
  - `public StudySummaryView getStudentSummary(@RequestParam String studentId, @RequestParam(required = false) String date)`
    教师/管理员查看指定学员某日学习总结。

### `api/src/main/java/cn/wisestar/server/api/StudentSupervisionApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class StudentSupervisionApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  学员督学接口（教师端查看学员学习状态）
- 方法:
  - `public List<StudentSupervisionView> getOnlineStudents()`
    获取在线学员列表（只显示在线学员）

### `api/src/main/java/cn/wisestar/server/api/StudentTaskApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class StudentTaskApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  学员任务接口（管理端任务发布 + 学员端任务展示）
- 方法:
  - `public boolean assignTasks(@RequestBody StudentTaskDTO request)`
    给学生分配任务（同一学生最多 3 个任务）
  - `public List<StudentTaskView> getStudentTasks(@RequestParam String studentId)`
    获取学员今日任务
  - `public int publishTasks(@RequestBody StudentTaskPublishDTO request)`
    任务发布：向多个学员各发布一条纯文本任务（学管师后台）。
  - `public PaginationResponse<StudentTaskView> pageTasks(StudentTaskQuery query)`
    管理端任务分页列表（发布记录）。
  - `public boolean deleteTask(@RequestParam String id)`
    删除（撤回）一条已发布任务。
  - `public List<StudentTaskView> listMyTasks()`
    学员端：当前登录学员收到的全部任务（按发布时间倒序）。
  - `public StudentTaskCompleteView completeTask(@RequestParam String id)`
    学员端：完成一条任务并结算学习币。

### `api/src/main/java/cn/wisestar/server/api/SubjectApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class SubjectApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  学科管理接口（知识管理板块一级维度）。
  **定位**：管理端「知识管理 → 学科」页面数据源——学科 CRUD； 学科是知识管理三级层级（学科 → 章节 → 小节 → 知识点）的最顶层。
- 方法:
  - `public List<SubjectView> listSubjects()`
    学科列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/subject/list（如 /api/subject/list）。

    **功能**：返回全量学科（sort 升序），每项含该学科下的章节数 chapterCount， 供管理端学科列表与顶部下拉联动使用。

    **返回值结构**：SubjectView 列表。

    **权限**：@PreAuthorize("isAuthenticated()")（登录用户可查看）。
  - `public String addSubject(@RequestBody SubjectRequest request)`
    新增学科。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/subject/create（如 /api/subject/create）。

    **请求参数**：SubjectRequest（@RequestBody JSON： name/code/icon/themeColor/sort）。

    **返回值结构**：新学科 id（String）。
  - `public void updateSubject(@RequestBody SubjectRequest request)`
    更新学科。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/subject/update（如 /api/subject/update）。
  - `public void deleteSubject(@RequestBody SubjectRequest request)`
    删除学科（逻辑删除）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/subject/delete（如 /api/subject/delete）。

### `api/src/main/java/cn/wisestar/server/api/SurveyApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class SurveyApi`
- 注解: @RequestMapping, @RequiredArgsConstructor, @RestController
- **类说明**：
  答卷页面接口（SurveyApi）。
  **所属模块**：api 模块（Web 接口层，Spring MVC REST Controller）。

  **类职责**：提供"公开答卷页"所需的全部接口：加载问卷、问卷校验、投票统计、 答案保存/暂存、公开文件上传/预览、公开查询（验证码校验）、问卷字典加载、 问卷结果/关联结果加载。这些接口均为公开访问（不要求登录）。

  **请求路径前缀**：类级路径为 `${api.prefix`/public}（api.prefix 通常为 /api）， 各方法再追加子路径（如 /api/public/loadProject、/api/public/saveAnswer 等）。

  **被谁调用**：前端答题端（答卷页/公开查询页/投票页），面向答卷人开放，不要求登录。

  **依赖的服务**：

  - SurveyService——问卷加载、校验、统计、答案保存、公开查询等核心业务；
  - FileService——公开上传（mark publicUpload=true）与文件预览加载。

  **数据流概览**：答卷人浏览器 → 本类各方法 → SurveyService（shared 接口） → rdbms 实现 → MyBatis Mapper → 问卷/答案/字典表 → 视图 DTO 返回。
- 方法:
  - `public PublicProjectView loadProject(@RequestBody ProjectQuery query)`
    加载问卷。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/loadProject （如 /api/public/loadProject）。

    **功能**：按项目短链接/校验码加载一份问卷的完整渲染数据（题目、选项、 样式、题目随机配置等），答卷页打开问卷时调用。

    **请求参数**：ProjectQuery（@RequestBody JSON）——问卷定位条件 （shortId / checkCode / id 等，详见 DTO）。

    **返回值结构**：PublicProjectView（公开问卷视图：问卷基本信息 + 题目 schema + 样式配置）。

    **调用的下层 Service**：SurveyService#loadProject(ProjectQuery)。
  - `public PublicProjectView validateProject(@RequestBody ProjectQuery query)`
    问卷校验。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/validateProject （如 /api/public/validateProject）。

    **功能**：校验问卷是否可访问/可填写（问卷状态、有效期、填答次数限制、 白名单等），返回校验后的问卷视图；校验不通过时由服务层抛异常。

    **请求参数**：ProjectQuery（@RequestBody JSON，问卷定位条件）。

    **返回值结构**：PublicProjectView（校验通过的问卷视图）。

    **调用的下层 Service**：SurveyService#validateProject(ProjectQuery)。
  - `public PublicStatisticsView statProject(@RequestBody ProjectQuery query)`
    单选、多选投票获取统计信息。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/statistics （如 /api/public/statistics）。

    **功能**：针对问卷中的单选题/多选题（投票题）返回实时统计结果 （每个选项的投票数/占比），供投票类问卷的结果页展示。

    **请求参数**：ProjectQuery（@RequestBody JSON，问卷定位条件）。

    **返回值结构**：PublicStatisticsView（投票统计视图：题目 + 各选项计数）。

    **调用的下层 Service**：SurveyService#statProject(ProjectQuery)。
  - `public PublicAnswerView saveAnswer(@RequestBody AnswerRequest request)`
    答案保存。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/saveAnswer （如 /api/public/saveAnswer）。

    **功能**：答卷人提交问卷答案，服务层落库并返回答卷结果视图 （含提交成功标志、答卷编号、可查询校验码等，便于"公开查询"功能后续使用）。

    **请求参数**：AnswerRequest（@RequestBody JSON）——答卷数据： 问卷定位（projectId/shortId）、答卷人信息、各题目答案明细等。

    **返回值结构**：PublicAnswerView（答卷结果视图：答卷 id、查询码、 提交时间等）。

    **调用的下层 Service**：SurveyService#saveAnswer(AnswerRequest)。
  - `public void tempSaveAnswer(@RequestBody AnswerRequest request)`
    答案暂存（目前仅支持问题随机）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/tempSaveAnswer （如 /api/public/tempSaveAnswer）。

    **功能**：答卷过程中的草稿暂存——当问卷开启"题目随机"时，暂存已答内容， 防止刷新/断线丢失；正式提交仍走 saveAnswer。

    **请求参数**：AnswerRequest（@RequestBody JSON，同 saveAnswer）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **调用的下层 Service**：SurveyService#tempSaveAnswer(AnswerRequest)。
  - `public FileView upload(UploadFileRequest request)`
    上传文件（公开上传）。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/upload（如 /api/public/upload）。

    **功能**：答卷页（或公开场景）上传附件（如问卷附件题上传文件）。 关键点：先把 publicUpload 标记置为 true（表示公开上传、无需登录），再调用 FileService.upload。

    **请求参数**：UploadFileRequest（multipart 表单绑定，含文件流与业务关联信息）。

    **返回值结构**：FileView（上传成功后文件元数据视图）。

    **调用的下层 Service**：FileService#upload(UploadFileRequest)。
  - `public ResponseEntity<Resource> preview(@PathVariable("attachmentId") String attachmentId)`
    预览文件。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/public/preview/{attachmentId} （如 /api/public/preview/xxx）。

    **功能**：按附件 id 内联预览文件（图片直接展示）。设置 30 天浏览器缓存头 （Cache-Control: max-age=2592000）提升重复查看性能；dispositionType 设为 inline 使浏览器内联渲染而非下载。

    **请求参数**：attachmentId（@PathVariable 路径变量，附件/文件 id）。

    **返回值结构**：ResponseEntity（文件字节流 + 缓存头 + 内联 Content-Disposition）。

    **调用的下层 Service**：先组装 FileQuery（id + 内联类型 + 自定义头）， 再调用 FileService#loadFile(FileQuery)。
  - `public PublicQueryVerifyView loadQuery(@RequestBody PublicQueryRequest request)`
    加载公开查询验证页面数据。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/loadQuery （如 /api/public/loadQuery）。

    **功能**：加载"公开查询"功能所需的数据（如查询验证方式、验证码配置、 问卷信息摘要），供查询验证页初始化渲染。

    **请求参数**：PublicQueryRequest（@RequestBody JSON，问卷定位条件）。

    **返回值结构**：PublicQueryVerifyView（公开查询验证页视图数据）。

    **调用的下层 Service**：SurveyService#loadQuery(PublicQueryRequest)。
  - `public PublicQueryView getQueryResult(@RequestBody PublicQueryRequest request)`
    获取公开查询结果。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/getQueryResult （如 /api/public/getQueryResult）。

    **功能**：答卷人通过问卷编码/查询码（+ 可能的验证码）查询自己的答卷结果， 返回结果数据（如分数、各题答案等，取决于问卷配置是否开放查询）。

    **请求参数**：PublicQueryRequest（@RequestBody JSON，含查询码/验证码）。

    **返回值结构**：PublicQueryView（答卷查询结果视图）。

    **调用的下层 Service**：SurveyService#getQueryResult(PublicQueryRequest)。
  - `public List<PublicDictView> loadDict(@RequestBody PublicDictRequest request)`
    问卷加载字典。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/loadDict （如 /api/public/loadDict）。

    **功能**：加载问卷题目中引用的字典数据（题目依赖的字典选项列表）， 供答卷页渲染下拉/级联类题目。

    **请求参数**：PublicDictRequest（@RequestBody JSON，问卷/题目关联条件）。

    **返回值结构**：`List`（公开字典视图列表）。

    **调用的下层 Service**：SurveyService#loadDict(PublicDictRequest)。
  - `public PublicExamResult loadExamResult(@RequestBody PublicExamRequest request)`
    加载问卷结果。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/loadExamResult （如 /api/public/loadExamResult）。

    **功能**：加载考试/练习类问卷的作答结果（分数、正确/错误明细、解析等）， 供答卷人查看考试结果页。

    **请求参数**：PublicExamRequest（@RequestBody JSON，考试/答卷定位条件）。

    **返回值结构**：PublicExamResult（考试结果视图）。

    **调用的下层 Service**：SurveyService#loadExamResult(PublicExamRequest)。
  - `public PublicLinkResult loadLinkResult(@RequestBody PublicLinkRequest request)`
    加载问卷关联结果。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/loadLinkResult （如 /api/public/loadLinkResult）。

    **功能**：加载问卷的"关联结果"——问卷提交后根据答案联动展示的结果内容 （如跳转问卷、动态展示文案），供答卷提交后展示。

    **请求参数**：PublicLinkRequest（@RequestBody JSON，关联结果定位条件）。

    **返回值结构**：PublicLinkResult（关联结果视图）。

    **调用的下层 Service**：SurveyService#loadLinkResult(PublicLinkRequest)。

### `api/src/main/java/cn/wisestar/server/api/SystemApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class SystemApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  系统管理接口（SystemApi）。
  **所属模块**：api 模块（Web 接口层，Spring MVC REST Controller）。

  **类职责**：提供系统级管理功能：系统信息查询/更新、AI 设置、角色管理、 权限管理、系统用户管理、岗位管理、部门管理、字典/字典条目管理。是后台管理端 "系统管理"菜单对应的后端接口集合。

  **请求路径前缀**：类级路径为 `${api.prefix`/system}（api.prefix 通常为 /api）， 各方法再追加子路径（如 /api/system/user/list、/api/system/role/list 等）。

  **被谁调用**：前端管理后台的系统管理模块页面：系统信息设置页、AI 设置页、 角色管理页、权限管理页、系统用户管理页、岗位管理页、部门管理页、字典管理页。

  **依赖的服务**：

  - SystemService——系统信息、AI 设置、角色、权限相关业务；
  - UserService——系统用户管理业务；
  - PositionService——岗位管理业务；
  - DeptService——部门管理业务；
  - DictService——字典与字典条目业务；
  - MessageSource——i18n 国际化消息源，用于删除类接口的校验提示文案。

  **数据流概览**：前端 HTTP 请求 → 本类各方法（权限注解校验 + 参数校验）→ 对应的 shared Service 接口方法 → rdbms 模块实现 → MyBatis Mapper → 数据库 → 结果返回。
- 方法:
  - `public SystemInfo getSystemInfo()`
    获取当前系统信息。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/system（如 /api/system）。

    **功能**：返回系统公开配置信息（系统名称、描述、图标、默认语言、版本、 注册开关、验证码开关、版权、备案号、RSA 公钥、AI 是否启用等）。 登录页/注册页/全局展示均依赖此接口（前端用它取 publicKey 做密码 RSA 加密）。

    **请求参数**：无。

    **返回值结构**：SystemInfo（见 shared 模块 DTO，含嵌套的 RegisterInfo、SystemSetting、AiSetting 子结构）。

    **调用的下层 Service**：SystemService#getSystemInfo()。
  - `public SystemInfo.AiSetting getSystemAiSetting()`
    获取系统 AI 设置。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/system/aiSetting（如 /api/system/aiSetting）。

    **功能**：返回 AI 功能设置（是否启用、可用模型列表、提示词、Token）。 安全处理：返回前将 token（AI 平台 API Key）置空，避免敏感凭证泄露给前端。 ai 模块的 SiliconflowChatServiceImpl 会通过 SystemService#getSystemAiSetting 读取同样的配置（包含 token）用于调用大模型 API。

    **请求参数**：无。

    **返回值结构**：`SystemInfo.AiSetting`（enabled、models、prompt；token 恒为 null）。

    **权限**：@PreAuthorize("hasRole('admin')")——仅管理员可查。

    **调用的下层 Service**：SystemService#getSystemAiSetting()。
  - `public void updateSystemInfo(@RequestBody SystemInfoRequest request)`
    更新系统信息。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/update（如 /api/system/update）。

    **功能**：更新系统配置信息（系统名称、描述、图标、语言、注册开关、 验证码开关、版权、备案号、AI 设置含 token/模型/prompt 等）。

    **请求参数**：SystemInfoRequest（@RequestBody JSON）——系统信息更新请求体， 包含 SystemInfo 各字段及嵌套设置对象。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasRole('admin')")——仅管理员可操作。

    **调用的下层 Service**：SystemService#updateSystemInfo(SystemInfoRequest)。
  - `public PaginationResponse<RoleView> roles(RoleQuery query)`
    获取系统角色列表。
    **HTTP 方法 + 完整路径**：任意方法 + ${api.prefix}/system/role/list （@RequestMapping 不限定方法，如 /api/system/role/list）。

    **功能**：分页查询系统角色列表（角色名、编码、描述、成员数等）， 供角色管理页表格展示。

    **请求参数**：RoleQuery（分页及筛选条件）。

    **返回值结构**：PaginationResponse（分页包装的角色列表）。

    **权限**：@PreAuthorize("hasAuthority('system:role:list')")——需要该权限点。

    **调用的下层 Service**：SystemService#getRoles(RoleQuery)。
  - `public List<PermissionConsts.Node> permissionTree()`
    获取角色权限树。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/system/permissionTree （如 /api/system/permissionTree）。

    **功能**：返回后台系统按功能模块分组的权限点清单（模块 → 操作点）， 供角色管理页权限树勾选。数据源为静态清单 PermissionConsts， 与后端接口的 @PreAuthorize 权限点一一对应。

    **返回值结构**：List（模块分组树）。

    **权限**：@PreAuthorize("hasRole('admin')")——仅管理员可查。
  - `public void createRole(@RequestBody RoleRequest request)`
    添加系统角色。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/role/create （如 /api/system/role/create）。

    **功能**：新建一个角色，并绑定角色对应的权限集合。

    **请求参数**：RoleRequest（@RequestBody JSON）——角色名称、编码、 描述、勾选的权限 id 列表等。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:role:create')")。

    **调用的下层 Service**：SystemService#createRole(RoleRequest)。
  - `public void updateRole(@RequestBody RoleRequest request)`
    更新系统角色。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/role/update （如 /api/system/role/update）。

    **功能**：更新角色基本信息及其绑定的权限集合。

    **请求参数**：RoleRequest（@RequestBody JSON，含角色 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:role:update')")。

    **调用的下层 Service**：SystemService#updateRole(RoleRequest)。
  - `public void deleteRole(@RequestBody RoleRequest request)`
    删除系统角色。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/role/delete （如 /api/system/role/delete）。

    **功能**：删除角色。前置保护逻辑（Controller 层完成）：

    - 请求体无 id 直接静默返回；
    - 若当前系统中角色总数 <= 1，则抛出 ValidationException（i18n 消息 system.role.delete.retainOne），提示必须至少保留一个角色。

    **请求参数**：RoleRequest（@RequestBody JSON，含角色 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **异常**：唯一角色时抛 ValidationException，消息源读取国际化文案。

    **权限**：@PreAuthorize("hasAuthority('system:role:delete')")。

    **调用的下层 Service**：SystemService#getRoles(RoleQuery) → SystemService#deleteRole(RoleRequest)。
  - `public List<PermissionView> permissions()`
    获取系统权限列表。
    **HTTP 方法 + 完整路径**：任意方法 + ${api.prefix}/system/permission/list （如 /api/system/permission/list）。

    **功能**：返回系统中全部权限点（菜单/按钮权限树），供角色编辑页勾选权限使用。

    **请求参数**：无。

    **返回值结构**：`List`（权限视图列表，含层级父子关系）。

    **权限**：无 @PreAuthorize 注解（由全局安全规则控制）。

    **调用的下层 Service**：SystemService#getPermissions()。
  - `public void extractCodeDiffDbPermissions()`
    比对数据库和代码里面配置的权限。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/system/permission/diff （如 /api/system/permission/diff）。

    **功能**：开发/运维辅助接口——扫描代码中 @PreAuthorize 等注解声明的权限点， 与数据库权限表比对，输出差异（缺失/多余权限），用于排查权限配置不一致问题。

    **请求参数**：无。

    **返回值结构**：无返回值（差异结果由服务层内部日志输出）。

    **权限**：@PreAuthorize("hasRole('admin')")——仅管理员。

    **调用的下层 Service**：SystemService#extractCodeDiffDbPermissions()。
  - `public PaginationResponse<UserView> roles(UserQuery query)`
    系统用户列表。
    **HTTP 方法 + 完整路径**：任意方法 + ${api.prefix}/system/user/list （如 /api/system/user/list）。

    **功能**：分页查询系统全部用户（管理员视角，含用户名、角色、部门、岗位、 状态等），供系统用户管理页表格展示。

    **请求参数**：UserQuery（分页及筛选条件：关键字、角色、部门等）。

    **返回值结构**：PaginationResponse（分页包装的用户列表）。

    **权限**：@PreAuthorize("hasAuthority('system:user:list')")。

    **调用的下层 Service**：UserService#getUsers(UserQuery)。
  - `public void createUser(@RequestBody @Valid UserRequest request)`
    创建系统用户。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/user/create （如 /api/system/user/create）。

    **功能**：管理员新建系统用户（设置用户名、初始密码、角色、部门、岗位等）。

    **请求参数**：UserRequest（@RequestBody JSON，@Valid 校验）——用户信息 + 角色分配。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:user:create')")。

    **调用的下层 Service**：UserService#createUser(UserRequest)。
  - `public void updateUser(@RequestBody @Valid UserRequest request)`
    更新系统用户。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/user/update （如 /api/system/user/update）。

    **功能**：管理员更新用户基本信息（昵称、邮箱、角色、部门、启用状态等）。

    **请求参数**：UserRequest（@RequestBody JSON，@Valid 校验，含用户 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:user:update')")。

    **调用的下层 Service**：UserService#updateUser(UserRequest)。
  - `public void updateUserPosition(@RequestBody @Valid UserRequest request)`
    更新用户岗位信息。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/user/updatePosition （如 /api/system/user/updatePosition）。

    **功能**：单独更新指定用户的岗位绑定（与 updateUser 拆分，便于岗位调整场景）。

    **请求参数**：UserRequest（@RequestBody JSON，@Valid 校验，含用户 id 与岗位 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:user:updatePosition')")。

    **调用的下层 Service**：UserService#updateUserPosition(UserRequest)。
  - `public void deleteUser(@RequestBody UserRequest request)`
    删除用户。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/user/delete （如 /api/system/user/delete）。

    **功能**：按 id 批量删除用户（id 支持逗号分隔多个）。前置保护逻辑：

    - 无 id 直接返回；
    - id 按逗号拆分、trim、去重、过滤空串；
    - 不允许删除自己（id 列表含当前登录用户时抛 i18n 异常 system.user.delete.self）；
    - 删除后系统剩余用户数必须 >= 1（否则抛 system.user.delete.retainOne）。

    **请求参数**：UserRequest（@RequestBody JSON，id 字段可含多个逗号分隔 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **异常**：删除自己/仅剩一个用户时抛 ValidationException（i18n 文案）。

    **权限**：@PreAuthorize("hasAuthority('system:user:delete')")。

    **调用的下层 Service**：UserService#getUsers(UserQuery) → 循环 UserService#deleteUser(String)。
  - `public boolean checkUsernameExist(String username)`
    检查登录名是否存在。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/system/checkUsernameExist （如 /api/system/checkUsernameExist?username=xxx）。

    **功能**：校验用户名是否已被占用，供用户创建/编辑表单做实时重名校验。

    **请求参数**：username（GET 查询参数，登录用户名）。

    **返回值结构**：boolean——true 表示用户名已存在。

    **调用的下层 Service**：UserService#checkUsernameExist(String)。
  - `public PaginationResponse<PositionView> listPosition(PositionQuery query)`
    查询岗位列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/system/position/list （如 /api/system/position/list）。

    **功能**：分页查询系统岗位列表（岗位名称、编码、状态等），供岗位管理页展示。

    **请求参数**：PositionQuery（分页及筛选条件）。

    **返回值结构**：PaginationResponse。

    **权限**：@PreAuthorize("hasAuthority('system:position:list')")。

    **调用的下层 Service**：PositionService#listPosition(PositionQuery)。
  - `public void addPosition(@RequestBody PositionRequest request)`
    添加岗位。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/position/create （如 /api/system/position/create）。

    **功能**：新建岗位。

    **请求参数**：PositionRequest（@RequestBody JSON，岗位名称/编码等）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:position:create')")。

    **调用的下层 Service**：PositionService#addPosition(PositionRequest)。
  - `public void updatePosition(@RequestBody PositionRequest request)`
    更新岗位信息。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/position/update （如 /api/system/position/update）。

    **功能**：更新岗位基本信息。

    **请求参数**：PositionRequest（@RequestBody JSON，含岗位 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:position:update')")。

    **调用的下层 Service**：PositionService#updatePosition(PositionRequest)。
  - `public void deletePosition(@RequestBody PositionRequest request)`
    删除岗位信息。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/position/delete （如 /api/system/position/delete）。

    **功能**：按 id 删除岗位。

    **请求参数**：PositionRequest（@RequestBody JSON，含岗位 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:position:delete')")。

    **调用的下层 Service**：PositionService#deletePosition(String)（注意传入的是 id）。
  - `public List<DeptView> listDept()`
    获取部门列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/system/dept/list （如 /api/system/dept/list）。

    **功能**：获取部门树（全部部门，含父子层级），供部门管理页及用户编辑页部门选择器使用。

    **请求参数**：无（null 表示查询全部）。

    **返回值结构**：`List`（部门视图树）。

    **权限**：@PreAuthorize("hasAuthority('system:dept:list')")。

    **调用的下层 Service**：DeptService#listDept(null)。
  - `public void addOrg(@RequestBody DeptRequest request)`
    添加部门。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/dept/create （如 /api/system/dept/create）。

    **功能**：新建部门节点（含父部门、排序号等）。

    **请求参数**：DeptRequest（@RequestBody JSON，部门信息）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:dept:create')")。

    **调用的下层 Service**：DeptService#addDept(DeptRequest)。
  - `public void updateOrg(@RequestBody DeptRequest request)`
    更新部门。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/dept/update （如 /api/system/dept/update）。

    **功能**：更新部门名称、上级部门、排序等信息。

    **请求参数**：DeptRequest（@RequestBody JSON，含部门 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:dept:update')")。

    **调用的下层 Service**：DeptService#updateDept(DeptRequest)。
  - `public void deleteOrg(@RequestBody DeptRequest request)`
    删除部门。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/dept/delete （如 /api/system/dept/delete）。

    **功能**：删除部门。前置保护逻辑：无 id 直接返回；若删除前部门总数 <= 1 则抛 i18n 异常 system.dept.delete.retainOne（至少保留一个部门）。

    **请求参数**：DeptRequest（@RequestBody JSON，含部门 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **异常**：仅剩一个部门时抛 ValidationException（i18n 文案）。

    **权限**：@PreAuthorize("hasAuthority('system:dept:delete')")。

    **调用的下层 Service**：DeptService#listDept(null) → DeptService#deleteDept(String)。
  - `public void sortOrg(@RequestBody DeptSortRequest request)`
    部门排序。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/dept/sort （如 /api/system/dept/sort）。

    **功能**：调整部门节点间的排序（同级排序调整，前端拖拽后提交新排序）。

    **请求参数**：DeptSortRequest（@RequestBody JSON，含部门 id 与排序值）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:dept:create')")——复用创建权限。

    **调用的下层 Service**：DeptService#sortDept(DeptSortRequest)。
  - `public PaginationResponse<CommDictView> listDict(CommDictQuery query)`
    获取字典项列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/system/dict/list （如 /api/system/dict/list）。

    **功能**：分页查询系统字典（字典名、编码、描述等），供字典管理页展示。

    **请求参数**：CommDictQuery（分页及筛选条件）。

    **返回值结构**：PaginationResponse。

    **权限**：@PreAuthorize("hasAuthority('system:dict:list')")。

    **调用的下层 Service**：DictService#listDict(CommDictQuery)。
  - `public void addDict(@RequestBody CommDictRequest request)`
    创建字典项。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/dict/create （如 /api/system/dict/create）。

    **功能**：新建一个字典（如"性别""状态"等字典定义）。

    **请求参数**：CommDictRequest（@RequestBody JSON，字典编码/名称等）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:dict:create')")。

    **调用的下层 Service**：DictService#addDict(CommDictRequest)。
  - `public void updateDict(@RequestBody CommDictRequest request)`
    更新字典项。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/dict/update （如 /api/system/dict/update）。

    **功能**：更新字典基本信息。

    **请求参数**：CommDictRequest（@RequestBody JSON，含字典 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:dict:update')")。

    **调用的下层 Service**：DictService#updateDict(CommDictRequest)。
  - `public void deleteDict(@RequestBody CommDictRequest request)`
    删除字典项。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/dict/delete （如 /api/system/dict/delete）。

    **功能**：按 id 删除字典（同时处理其下字典条目的级联，具体由服务层决定）。

    **请求参数**：CommDictRequest（@RequestBody JSON，含字典 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:dict:delete')")。

    **调用的下层 Service**：DictService#deleteDict(String)（传入 id）。
  - `public PaginationResponse<CommDictItemView> listDictItem(CommDictItemQuery query)`
    获取字典条目列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/system/dictItem/list （如 /api/system/dictItem/list）。

    **功能**：分页查询某字典下的具体字典条目（键值对：label/value）， 供字典条目管理页展示。

    **请求参数**：CommDictItemQuery（分页及所属字典过滤条件）。

    **返回值结构**：PaginationResponse。

    **权限**：@PreAuthorize("hasAuthority('system:dictItem:list')")。

    **调用的下层 Service**：DictService#listDictItem(CommDictItemQuery)。
  - `public void createDictItem(@RequestBody CommDictItemRequest request)`
    添加字典条目。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/dictItem/create （如 /api/system/dictItem/create）。

    **功能**：为字典新增一个字典条目（label/value 键值对）。

    **请求参数**：CommDictItemRequest（@RequestBody JSON，所属字典 id + 条目值）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:dictItem:create')")。

    **调用的下层 Service**：DictService#saveOrUpdateDictItem(CommDictItemRequest) （新增与更新复用同一方法，由 id 是否为空区分）。
  - `public void updateItem(@RequestBody CommDictItemRequest request)`
    修改字典条目。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/dictItem/update （如 /api/system/dictItem/update）。

    **功能**：更新字典条目（与 create 共用 saveOrUpdateDictItem，按 id 存在性区分新增/修改）。

    **请求参数**：CommDictItemRequest（@RequestBody JSON，含条目 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:dictItem:update')")。

    **调用的下层 Service**：DictService#saveOrUpdateDictItem(CommDictItemRequest)。
  - `public void importDictItem(CommDictItemRequest request)`
    导入字典条目。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/dictItem/import （如 /api/system/dictItem/import）。

    **功能**：批量导入字典条目（Excel 或 JSON 列表，由服务层解析落库）。

    **请求参数**：CommDictItemRequest（multipart 表单绑定，含导入文件或条目数据）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:dictItem:import')")。

    **调用的下层 Service**：DictService#importDictItem(CommDictItemRequest)。
  - `public void deleteDictItem(@RequestBody CommDictItemRequest request)`
    删除字典条目。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/system/dictItem/delete （如 /api/system/dictItem/delete）。

    **功能**：按 id 删除字典条目。

    **请求参数**：CommDictItemRequest（@RequestBody JSON，含条目 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('system:dictItem:delete')")。

    **调用的下层 Service**：DictService#deleteDictItem(String)（传入 id）。

### `api/src/main/java/cn/wisestar/server/api/TaskApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class TaskApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  今日任务接口（后台布置 + 学员端呈现）。
- 方法:
  - `public List<TaskView> listTasks(@RequestParam(required = false) String taskDate, @RequestParam(required = false) String name)`
    后台任务列表（task:list）。
  - `public void createTask(@RequestBody TaskRequest request)`
    新增任务（task:create）。
  - `public void batchCreateTasks(@RequestBody List<TaskRequest> requests)`
    批量布置任务（同一弹窗一次布置多条，task:create）。
  - `public void updateTask(@RequestBody TaskRequest request)`
    编辑任务（task:update）。
  - `public void deleteTask(@RequestBody TaskRequest request)`
    删除任务（task:delete）。
  - `public List<StudentTaskView> studentTasks(@RequestParam(required = false) String taskDate)`
    学员端当日任务（含完成状态：当日交卷且正确率≥60%）。

### `api/src/main/java/cn/wisestar/server/api/TemplateApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class TemplateApi`
- 注解: @RestController, @RequestMapping, @RequiredArgsConstructor
- **类说明**：
  问卷/问题模板接口（TemplateApi）。
  **所属模块**：api 模块（Web 接口层，Spring MVC REST Controller）。

  **类职责**：提供问卷模板的管理接口：模板分页列表、模板详情、创建/批量创建、 更新、删除，以及"模板广场"的分类与标签查询。

  **请求路径前缀**：类级路径为 `${api.prefix`/template}（api.prefix 通常为 /api）， 各方法再追加子路径（如 /api/template/list、/api/template/listCategory 等）。

  **被谁调用**：前端管理后台：问卷编辑器（从模板新建问卷）、模板管理页、 模板广场页（分类/标签筛选、模板列表）。

  **依赖的服务**：注入 TemplateService（shared 模块接口，rdbms 模块实现）—— 负责模板 CRUD、分类/标签查询、模板选择器等业务。

  **数据流概览**：前端 HTTP 请求 → 本类方法（权限注解）→ TemplateService → rdbms 实现 → 模板 Mapper → 数据库 → 视图 DTO 返回。
- 方法:
  - `public PaginationResponse<TemplateView> listQuestionTemplate(TemplateQuery query)`
    获取模板分页列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/template/list（如 /api/template/list）。

    **功能**：分页查询问卷/问题模板列表（支持按分类、标签、名称筛选）， 供模板管理页与模板广场展示。

    **请求参数**：TemplateQuery（GET 查询参数）——分页 + 筛选条件。

    **返回值结构**：PaginationResponse（分页包装的模板列表）。

    **权限**：@PreAuthorize("hasAuthority('template:list')")。

    **调用的下层 Service**：TemplateService#listTemplate(TemplateQuery)。
  - `public TemplateView getTemplate(TemplateQuery query)`
    获取模板详情。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/template/get（如 /api/template/get?id=xxx）。

    **功能**：按 id 获取单个模板的完整内容（含题目 schema），供模板编辑或 从模板新建问卷时加载。无权限注解（是否要求登录由全局安全规则控制）。

    **请求参数**：TemplateQuery（GET 查询参数，含模板 id）。

    **返回值结构**：TemplateView（模板详情视图，含题目 schema）。

    **调用的下层 Service**：TemplateService#getTemplate(TemplateQuery)。
  - `public String addTemplate(@RequestBody TemplateRequest template)`
    创建模板。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/template/create（如 /api/template/create）。

    **功能**：新建一个问卷/问题模板，返回新模板 id。

    **请求参数**：TemplateRequest（@RequestBody JSON）——模板名称、分类、 标签、题目 schema 等。

    **返回值结构**：String（新模板 id）。

    **权限**：@PreAuthorize("hasAuthority('template:create')")。

    **调用的下层 Service**：TemplateService#addTemplate(TemplateRequest)。
  - `public void batchAddTemplate(@RequestBody List<TemplateRequest> templateRequests)`
    批量创建题目模板。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/template/batchCreate （如 /api/template/batchCreate）。

    **功能**：一次提交多个模板（批量导入题目模板），逐条创建。

    **请求参数**：`List`（@RequestBody JSON 数组）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('template:create')")。

    **调用的下层 Service**：TemplateService#batchAddTemplate(List)。
  - `public void updateTemplate(@RequestBody TemplateRequest template)`
    更新模板。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/template/update（如 /api/template/update）。

    **功能**：更新模板信息与题目 schema。

    **请求参数**：TemplateRequest（@RequestBody JSON，含模板 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('template:update')")。

    **调用的下层 Service**：TemplateService#updateTemplate(TemplateRequest)。
  - `public void deleteTemplate(@RequestBody TemplateRequest request)`
    删除模板。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/template/delete（如 /api/template/delete）。

    **功能**：按 id 删除模板（逻辑删除，由服务层实现）。

    **请求参数**：TemplateRequest（@RequestBody JSON，含模板 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('template:delete')")。

    **调用的下层 Service**：TemplateService#deleteTemplate(TemplateRequest)。
  - `public Set<String> listTemplateCategories(CategoryQuery query)`
    模板广场获取分类。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/template/listCategory （如 /api/template/listCategory）。

    **功能**：返回模板广场页展示的分类集合（按条件过滤后的去重分类列表）， 供分类筛选项使用。

    **请求参数**：CategoryQuery（GET 查询参数，分类筛选条件）。

    **返回值结构**：`Set`（去重分类名集合）。

    **调用的下层 Service**：TemplateService#listTemplateCategories(CategoryQuery)。
  - `public Set<String> getTags(TagQuery query)`
    模板广场获取标签。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/template/listTag（如 /api/template/listTag）。

    **功能**：返回模板广场页展示的标签集合（去重），供标签筛选项使用。

    **请求参数**：TagQuery（GET 查询参数，标签筛选条件）。

    **返回值结构**：`Set`（去重标签集合）。

    **调用的下层 Service**：TemplateService#getTags(TagQuery)。
  - `public Set<String> listTemplateTags()`
    题目管理页获取全部题目标签（「按标签筛选」下拉选项）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/template/listTemplateTag （如 /api/template/listTemplateTag）。

    **功能**：返回普通题型（非 Survey 问卷题）模板使用过的全部标签（去重）， 供题目管理页「按标签筛选」下拉使用；与 /template/list 的 tag 过滤同源，保证能筛到题目。

    **请求参数**：无。

    **返回值结构**：`Set`（去重标签集合，按字典序）。

    **权限**：无 @PreAuthorize 注解（与同处 /listCategory、/listTag 一致）。

    **调用的下层 Service**：TemplateService#listTemplateTags()。

### `api/src/main/java/cn/wisestar/server/api/UserApi.java`
- 包: `cn.wisestar.server.api`
- 类型: `class UserApi`
- 注解: @RestController, @RequiredArgsConstructor, @RequestMapping
- **类说明**：
  用户相关接口（UserApi）。
  **所属模块**：api 模块（Web 接口层，Spring MVC REST Controller）。

  **类职责**：提供用户侧核心接口：登录、登出、注册、当前用户信息、 用户信息更新、注册角色查询、用户导入、我的任务/历史任务查询等。

  **请求路径前缀**：类级路径为 `${api.prefix`}（api.prefix 通常为 /api）， 即该类的所有接口直接挂在 /api 下，无额外类级子路径。

  **被谁调用**：前端登录页（/api/public/login、/api/public/register）、 管理后台用户中心（/api/currentUser、/api/userOverview、/api/user 更新）、 首页任务看板（/api/listUserTask、/api/listHistoryTask）、 用户管理页（/api/importUser）等。

  **依赖的服务/组件**：

  - UserService（shared 模块接口）——用户注册、查询、更新、任务查询等业务逻辑；
  - AuthenticationManager——Spring Security 认证管理器，校验用户名密码；
  - JwtTokenUtil——JWT 令牌生成工具；
  - RSAUtils——RSA 解密工具（登录密码前端 RSA 加密传输，后端解密）；
  - SecurityContextUtils——从 SecurityContext 读取当前登录用户 id；
  - CaptchaService（anji-plus 滑动验证码）——注册等场景的验证码校验（部分流程使用）。

  **认证方式说明**：登录成功后服务端生成 JWT，通过两条通道返回给前端： 1）写入 HttpOnly Cookie（TOKEN_NAME）；2）放在 Authorization 响应头。后续请求携带二者之一即可。
- 方法:
  - `public ResponseEntity login(@RequestBody @Valid AuthRequest request)`
    用户登录。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/login（如 /api/public/login）。

    **功能**：校验用户名密码，成功后生成 JWT 令牌并返回。密码流程：前端使用系统 公钥（/api/system 返回的 publicKey）RSA 加密密码明文，后端用 RSAUtils#decrypt 解密得到明文后交给 AuthenticationManager 认证。

    **请求参数**：AuthRequest（@RequestBody JSON，@Valid 校验）—— username（用户名）、password（RSA 密文密码），可能含验证码相关字段。

    **返回值结构**：HTTP 200，无 body；响应头包含：

    - Set-Cookie：TOKEN_NAME=JWT（HttpOnly、path=/）——浏览器自动携带；
    - Authorization：JWT 字符串——非浏览器客户端（App/Postman）取此头使用。

    **异常**：用户名或密码错误（RSA 解密失败、认证失败）统一捕获后抛出 ErrorCodeException(ErrorCode.UsernameOrPasswordError)，由全局异常处理器 转成 401 及错误信息响应。

    **调用的下层组件**：RSAUtils.decrypt → authenticationManager.authenticate → JwtTokenUtil.generateAccessToken(UserTokenView)。

    **数据流**：前端 POST JSON → 本方法 → RSA 解密密码 → AuthenticationManager 认证 （UserDetailsService 查询用户、BCrypt 比对密码）→ 成功取得 UserInfo → 生成 JWT（载荷含 userId）→ 写入 Cookie + Authorization 头返回。
  - `public ResponseEntity logout()`
    用户登出。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/logout（如 /api/public/logout）。

    **功能**：清除浏览器端登录 Cookie——将 TOKEN_NAME 的值置空并把 maxAge 设为 0 （立即过期），使浏览器后续不再携带该 Cookie。服务端 JWT 本身无状态，不做失效处理。

    **请求参数**：无。

    **返回值结构**：HTTP 200，仅返回 Set-Cookie（清空令牌）的响应头。
  - `public void register(@RequestBody RegisterRequest request)`
    用户注册。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/public/register（如 /api/public/register）。

    **功能**：创建新用户账号。系统是否开放注册由 SystemInfo.RegisterInfo.registerEnabled 控制，注册时按配置校验强密码规则、可选注册角色等（具体在 UserService.register 内实现）。

    **请求参数**：RegisterRequest（@RequestBody JSON）——用户名、密码、 昵称、邮箱、注册角色等字段。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **异常**：用户名已存在、未开放注册等业务异常由 UserService 抛出 ErrorCodeException，全局异常处理器统一处理。

    **调用的下层 Service**：UserService#register(RegisterRequest)。
  - `public UserInfo currentUser()`
    获取当前登录用户信息。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/currentUser（如 /api/currentUser）。

    **功能**：返回当前登录用户的完整信息（个人信息 + 角色 + 权限等）， 前端登录后拉取用户信息与权限菜单即调用此接口。

    **请求参数**：无（当前用户 id 从 SecurityContext 获取）。

    **返回值结构**：UserInfo（用户信息视图，含 userId、用户名、昵称、 角色列表、权限列表、所属部门/岗位等）。

    **权限**：@PreAuthorize("isAuthenticated()")——需登录。

    **调用的下层 Service**：SecurityContextUtils.getUserId() → UserService#loadUserById(String)。
  - `public UserOverview userOverview()`
    获取当前用户的总览数据。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/userOverview（如 /api/userOverview）。

    **功能**：返回当前登录用户的工作台/首页总览数据（如我创建的问卷数、 我参与的问卷数、待办任务数等统计），供前端首页卡片展示。

    **请求参数**：无。

    **返回值结构**：UserOverview（用户总览统计视图）。

    **权限**：@PreAuthorize("isAuthenticated()")——需登录。

    **调用的下层 Service**：UserService#getUserOverviewData()。
  - `public UserInfo updateUser(@RequestBody UserRequest request)`
    更新当前登录用户的个人信息。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/user（如 /api/user）。

    **功能**：修改当前登录用户的个人资料（昵称、邮箱、头像等）。 安全控制：本接口只允许修改"本人"的信息——Controller 强制把请求体中的 id 覆盖为当前登录用户 id，防止越权修改他人资料。

    **请求参数**：UserRequest（@RequestBody JSON）——需更新的用户资料字段。

    **返回值结构**：更新后的 UserInfo（重新加载用户信息返回）。

    **权限**：@PreAuthorize("hasAuthority('user:update')")——需要该权限点。

    **调用的下层 Service**：SecurityContextUtils.getUserId() → UserService#updateUser(UserRequest) → UserService#loadUserById(String)。
  - `public List<RegisterRoleView> getRegisterRoles()`
    获取注册时可选的角色列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/public/listRegisterRole （如 /api/public/listRegisterRole）。

    **功能**：返回开放注册时可供新用户选择的角色列表（公开接口，无需登录）， 供注册页角色下拉框使用。

    **请求参数**：无。

    **返回值结构**：`List`（角色视图列表：角色 id、名称等）。

    **调用的下层 Service**：UserService#getRegisterRoles()。
  - `public void importUser(UserRequest request)`
    导入用户。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/importUser（如 /api/importUser）。

    **功能**：按 Excel 模板批量导入用户（管理员在用户管理页下载模板、填写后上传导入）， 请求为 multipart 表单（文件 + 参数绑定到 UserRequest）。

    **请求参数**：UserRequest（multipart 表单绑定）——含上传的 Excel 文件流等。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **权限**：@PreAuthorize("hasAuthority('home')")——需登录且有 home 权限。

    **调用的下层 Service**：UserService#importUser(UserRequest)。
  - `public PaginationResponse<MyTaskView> myTask(MyTaskQuery query)`
    查询用户任务。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/listUserTask（如 /api/listUserTask）。

    **功能**：分页查询当前登录用户收到的"待办/进行中"任务列表 （例如问卷分发、审批等协同任务），供首页任务看板展示。

    **请求参数**：MyTaskQuery（GET 查询参数）——分页参数 pageNo/pageSize 及任务筛选条件。

    **返回值结构**：PaginationResponse（分页包装： total + 当前页任务列表）。

    **权限**：@PreAuthorize("hasAuthority('home')")——需登录且有 home 权限。

    **调用的下层 Service**：UserService#queryTask(MyTaskQuery)。
  - `public PaginationResponse<MyTaskView> myHistoryTask(MyTaskQuery query)`
    查询历史任务。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/listHistoryTask （如 /api/listHistoryTask）。

    **功能**：分页查询当前登录用户的"已完成/历史"任务列表（与 listUserTask 相对）， 供首页任务看板的历史 Tab 展示。

    **请求参数**：MyTaskQuery（GET 查询参数，同 queryTask）。

    **返回值结构**：PaginationResponse（分页包装）。

    **权限**：无 @PreAuthorize 注解——但 Spring Security 全局规则仍要求登录 （具体由 SecurityConfig 的放行/拦截规则决定）。

    **调用的下层 Service**：UserService#queryHistoryTask(MyTaskQuery)。
  - `public PaginationResponse<UserView> listUser(UserQuery query)`
    分页查询用户列表（管理端：题库分配选学员等场景）。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/user/list（如 /api/user/list）。

    **功能**：分页查询系统用户（按姓名模糊筛选）， 供管理端「题库分配」页选择学员使用。

    **请求参数**：UserQuery（GET 查询参数）——分页 + name 模糊筛选。

    **返回值结构**：PaginationResponse。

    **权限**：@PreAuthorize("hasAuthority('system:user:list')")。

    **调用的下层 Service**：UserService#getUsers(UserQuery)。
