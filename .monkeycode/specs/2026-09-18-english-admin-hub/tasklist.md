# 需求实施计划

Feature: 2026-09-18-english-admin-hub
Created: 2026-09-18
基于: `requirements.md`、`design.md`

- [ ] 1. 扩展数据库初始化脚本（R1/R4/R7，design §3）
   - 在 `server/rdbms/src/main/resources/scripts/init-h2.sql` 新增 `t_english_section` 表（唯一键 `version+grade+term+unit+section`）
   - 为 `t_english_word`、`t_english_sentence` 增加 `section` 字段，并把 `t_english_word.term` 写入建表语句
   - 为 `t_english_grammar` 增加 `version/term/unit/section/sort` 与 `create_at/create_by/update_at/update_by/is_deleted` 审计列（幂等 ALTER）
   - 在 `server/rdbms/src/main/resources/scripts/init-mysql.sql` 同步以上全部变更
   - 在管理员角色 authority 串追加 `english:section:*`、`english:grammar:*` 权限点

- [ ] 2. 登记权限常量（R7，design §4.5）
   - 在 `server/shared/src/main/java/cn/wisestar/server/core/constant/PermissionConsts.java` 的 `ADMIN_AUTHORITY` 增加小节与语法权限点

- [ ] 3. 后端实体与 Mapper（R1/R4，design §4.1/§4.2）
   - 新增 `EnglishSection extends BaseModel`（version/grade/term/unit/section/sort）
   - 改造 `EnglishGrammar` 继承 `BaseModel` 并新增 version/term/unit/section/sort
   - `EnglishWord`、`EnglishSentence` 增加 `section`
   - 新增 `EnglishSectionMapper`

- [ ] 4. 后端 DTO / Query / View（R1/R2/R3/R4，design §4.3）
   - 新增 `EnglishSectionQuery`、`EnglishSectionView`
   - 新增 `EnglishGrammarQuery`、`EnglishGrammarView`
   - `EnglishWordQuery`/`EnglishWordView`、`EnglishSentenceQuery`/`EnglishSentenceView` 增加 `section`

- [ ] 5. 后端服务实现（R1/R2/R3/R4/R6，design §4.4/§4.6）
   - 新增 `EnglishSectionService` 接口与实现（list / listByUnit / saveOrUpdate 重名校验 / delete）
   - 新增 `EnglishGrammarService` 接口与实现（list / saveOrUpdate / delete / upsertFromAi）
   - 增强 `EnglishWordManagerServiceImpl`：查询、创建、更新、导入支持 `section`
   - 增强 `EnglishSentenceServiceImpl`：查询、保存、导入支持 `section`，导入去重键追加 `section`
   - 增强 `EnglishStudentServiceImpl` 与 `EnglishWordServiceImpl`：视图返回 `section`
   - 改造 `EnglishAiPackServiceImpl.syncToBank`：调用 `upsertFromAi`，去重键改为版本+年级+学期+单元+小节+标题

- [ ] 6. 后端 API（R1/R4，design §4.5）
   - 新增 `EnglishSectionApi`（/english/section list/save/delete + 权限注解）
   - 新增 `EnglishGrammarApi`（/english/grammar list/save/delete + 权限注解）

- [ ] 7. 检查点 - 后端编译通过
   - 执行 `mvn clean package -pl api -am -DskipTests` 确保编译通过，如有疑问请询问用户

- [ ] 8. 前端管理端接口层与页面（R1/R2/R3/R4/R5，design §5.1）
   - 新增 `wisestar-client/src/api/englishAdmin.js`（小节、语法接口）
   - 新增 `pages/english/SectionManagePage.jsx`（筛选/列表/增删改）
   - 新增 `pages/english/GrammarManagePage.jsx`（筛选/列表/增删改，例句与练习题多行编辑）
   - `WordManagePage.jsx` 增加小节筛选、表单字段、列表列与联动
   - `SentenceManagePage.jsx` 增加小节筛选、表单字段、列表列与导入模板列

- [ ] 9. 前端入口收敛与路由修正（R5，design §5.1）
   - `App.jsx` 新增 `/english/section`、`/english/grammar` 路由并挂权限
   - `MainLayout.jsx`「英语板块」增加「小节管理」「语法管理」，修正 `/english/*` 菜单选中
   - 修正 `WordBookManagePage.jsx`「学习」跳转到 `/english/study?wordId=`

- [ ] 10. 学员端按小节分组（R6，design §5.2）
   - `EnglishWordLearnPage.jsx` 按 `section` 分组渲染单词，「未分节」置末
   - `EnglishSentenceLearnPage.jsx` 按 `section` 分组渲染句子，「未分节」置末

- [ ] 11. 检查点 - 前端构建通过
   - 执行 `npm run build` 确保前端构建通过，如有疑问请询问用户

- [ ] 12. 端到端联调与数据快照（R1-R7，design §9）
   - 启动预览后端与前端，用 admin cookie 验证小节与语法 CRUD
   - 验证单词/句子挂小节后学员端分组展示
   - 导出 H2 快照 `server/db-snapshot/wisestar.sql`

- [ ]* 13. 测试补充（可选）
   - [ ]* 13.1 小节唯一性校验与语法 upsert 去重键单元测试
   - [ ]* 13.2 导入包含小节列时的去重更新测试

## 说明

- 带 `*` 的测试任务默认可选，首期以核心开发任务为主。
