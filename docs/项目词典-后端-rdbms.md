# 项目词典 · 后端 rdbms（数据层与业务实现：实体 / Mapper / ServiceImpl / 配置）

> 本文件由源码清单自动生成后再人工校订，颗粒度到「每个类 + 每个公开方法」。
> 阅读方式：`归属` 段给出模块与包路径；`说明` 段取自类级 Javadoc；`方法` 段逐个列出公开/受保护方法（含 Javadoc 首句）。
>
> 归属规则：模块（Maven module）= 顶层归属；包（package）= 二级归属。
> 相关规则：`注解` 表示框架角色，`注入/字段` 表示直接依赖（协作方）。
> 本文件仅描述代码事实，未收录任何密钥；配置项中的 token 均为占位符。

---
### `rdbms/src/main/java/cn/wisestar/server/core/model/BaseModel.java`
- 包: `cn.wisestar.server.core.model`
- 类型: `class BaseModel`
- 注解: @Data
- **类说明**：
  所有业务实体（model）的公共基础父类。 【类职责】 统一提供数据库表公共字段：主键 id、创建/更新时间、创建/更新人、逻辑删除标记。 rdbms 模块下 domain.model 中所有实体均继承本类（Answer 覆写了 id 生成策略）。 【依赖什么】 - MyBatis-Plus 注解体系：@TableId(ASSIGN_ID 雪花ID) / @TableField(fill 自动填充) /
- 注入/字段: String id, Date createAt, String createBy, Date updateAt, String updateBy, Boolean deleted

### `rdbms/src/main/java/cn/wisestar/server/core/mybatis/ListTypeHandler.java`
- 包: `cn.wisestar.server.core.mybatis`
- 类型: `class ListTypeHandler`
- 注解: @Slf4j, @MappedTypes, @MappedJdbcTypes
- 方法:
  - `protected static ObjectMapper objectMapper = new ObjectMapper()`
  - `public ListTypeHandler()`
  - `public ListTypeHandler(Class<?> type)`
  - `protected Object parse(String json)`
  - `protected String toJson(Object obj)`
  - `public static void setObjectMapper(ObjectMapper objectMapper)`
  - `public static Type getSuperGenricTypes(final Class<?> clz)`

### `rdbms/src/main/java/cn/wisestar/server/core/mybatis/MyMetaObjectHandler.java`
- 包: `cn.wisestar.server.core.mybatis`
- 类型: `class MyMetaObjectHandler`
- 注解: @Component
- 方法:
  - `public void insertFill(MetaObject metaObject)`
  - `public void updateFill(MetaObject metaObject)`

### `rdbms/src/main/java/cn/wisestar/server/core/mybatis/MybatisPlugConfig.java`
- 包: `cn.wisestar.server.core.mybatis`
- 类型: `class MybatisPlugConfig`
- 注解: @Configuration, @MapperScan
- 方法:
  - `public MybatisPlugConfig(ObjectMapper objectMapper)`
  - `public MybatisPlusInterceptor mybatisPlusInterceptor()`
    新的分页插件,一缓和二缓遵循mybatis的规则,需要设置 MybatisConfiguration#useDeprecatedExecutor = false 避免缓存出现问题(该属性会在旧插件移除后一同移除)

### `rdbms/src/main/java/cn/wisestar/server/domain/dto/FlatSurveySchemaByType.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class FlatSurveySchemaByType`
- 注解: @Data

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/AnswerViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface AnswerViewMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/CampusDtoMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface CampusDtoMapper`
- 注解: @Mapper
- **类说明**：
  校区 请求DTO ↔ 实体 ↔ 视图DTO 转换（MapStruct 生成实现）。

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/ChapterViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface ChapterViewMapper`
- 注解: @Mapper
- **类说明**：
  Chapter 对象转换（Request ↔ Model ↔ View）。

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/CommDictItemViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface CommDictItemViewMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/CommDictViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface CommDictViewMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/DashboardViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface DashboardViewMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/DeptDtoMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface DeptDtoMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/FileViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface FileViewMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/KnowledgePointViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface KnowledgePointViewMapper`
- 注解: @Mapper
- **类说明**：
  KnowledgePoint 对象转换（Request ↔ Model ↔ View）。

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/MallGoodsViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface MallGoodsViewMapper`
- 注解: @Mapper
- **类说明**：
  MallGoods 对象转换（Request ↔ Model ↔ View）。

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/OrderViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface OrderViewMapper`
- 注解: @Mapper
- **类说明**：
  StudentOrder 对象转换（Request ↔ Model ↔ View）。
  订单表 grades 以逗号分隔字符串存储，视图层转为 List。

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/PositionDtoMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface PositionDtoMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/ProjectPartnerViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface ProjectPartnerViewMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/ProjectViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface ProjectViewMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/RepoViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface RepoViewMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/RoleViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface RoleViewMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/SectionViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface SectionViewMapper`
- 注解: @Mapper
- **类说明**：
  Section 对象转换（Request ↔ Model ↔ View）。

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/StudentViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface StudentViewMapper`
- 注解: @Mapper
- **类说明**：
  Student 对象转换（Request ↔ Model ↔ View）。

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/SubjectViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface SubjectViewMapper`
- 注解: @Mapper
- **类说明**：
  Subject 对象转换（Request ↔ Model ↔ View）。

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/TaskViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface TaskViewMapper`
- 注解: @Mapper
- **类说明**：
  Task 对象转换（Request ↔ Model ↔ View）。

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/TemplateViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface TemplateViewMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/UserBookViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface UserBookViewMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/UserEditMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface UserEditMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/UserPositionDtoMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface UserPositionDtoMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/mapper/UserViewMapper.java`
- 包: `cn.wisestar.server.domain.mapper`
- 类型: `interface UserViewMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Account.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Account`
- 注解: @Data, @TableName, @EqualsAndHashCode

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Answer.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Answer`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  答卷实体（对应数据库表 t_answer，autoResultMap=true 支持 JSON 类型字段自动映射）。 【类职责】 记录一次问卷/考试/练习的完整答卷：暂存答案（未提交）、最终答案、问卷快照、 考试得分与答题元信息（作答起止时间、客户端信息等）。是系统"答卷"概念的核心载体， 也是答题明细（t_answer_detail）生成的数据源。 【被谁调用】 - 数据访问层：AnswerMapper、AnswerDetailMapper（MyBatis-Plus） - 业务层：AnswerServiceImpl（保存/查询/导出/删除答卷）、SurveyServiceImpl（答卷相关 公开操作）、RepoServiceImpl（错题本保存临时答案）、RandomSurveyProcessor（随机抽题 时保存随机 schema 到答案表）、UserServiceImpl（历史任务查询） 【依赖什么】 - 继承 BaseModel：id（主键）、createAt、createBy、updateAt、updateBy、deleted - 题目/问卷结构复用 SurveySchema；答案结构 LinkedHashMap（questionId -> optionId -> value） - AnswerMetaInfo：作答元信息（开始/结束时间、客户端等） - AnswerExamInfo：考试信息（每题得分 questionScore） 【核心数据流】 学生作答（Controller）→ SurveyServiceImpl.saveAnswer → AnswerServiceImpl.saveAnswer/ updateAnswer → 计算考试分值 computeExamScore → 更新关联问卷答案 updateLinkSurveyAnswer → 保存本实体 → generateAnswerDetails 按题生成明细到 t_answer_detail → 返回 AnswerView。
- 注入/字段: String id, LinkedHashMap tempAnswer, LinkedHashMap answer, SurveySchema survey, AnswerMetaInfo metaInfo, AnswerExamInfo examInfo, String createBy, Date updateAt

### `rdbms/src/main/java/cn/wisestar/server/domain/model/AnswerDetail.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class AnswerDetail`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  答题明细表实体（对应数据库表 t_answer_detail）：一次答卷中单道题的作答记录。 【类职责】 把一次答卷（t_answer）按题目拆成一条条明细，每条记录"某学生在某次答卷中对某道题的 作答结果"，是学生答题情况分析（优势/薄弱知识点，AnalysisServiceImpl）的数据基础。 由 AnswerServiceImpl.generateAnswerDetails 在提交答卷（tempSave=1）时按题生成。 【被谁调用】 - 写入方：AnswerServiceImpl.generateAnswerDetails（先按 answerId 删除旧明细再逐条插入， 保证重复提交幂等） - 读取方：AnalysisServiceImpl.aggregate（按学生/学科/章节筛选聚合）、 AnswerDetailMapper（MyBatis-Plus 基础 CRUD） 【依赖什么】 - 继承 BaseModel：id、createAt（答题时间）、createBy（学生ID，MyBatis-Plus 自动填充）、 updateAt、updateBy、deleted 逻辑删除字段 - 数据来源：提交答卷时从 Answer（survey 问卷快照 + answer 答案 Map）中按题提取 【核心数据流】 学生提交答卷（Controller）→ AnswerServiceImpl.saveAnswer/updateAnswer → generateAnswerDetails：遍历问卷快照中的每一题，取题目知识点四维信息（attribute 快照）、 学生答案、对错判定结果、分值 → 逐条 insert 到 t_answer_detail → AnalysisServiceImpl 按 createBy(学生) + subject/chapter 筛选，knowledgePoint 逗号拆分后聚合统计。 【存储格式说明】 - knowledgePoint 列：多值知识点以英文逗号分隔存为字符串（如 "函数单调性,奇偶性"） - isCorrect 列：null=无标准答案不计分，1=正确，0=错误 - createBy 列即学生ID（答题人），聚合分析时以此为归属依据

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Campus.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Campus`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  校区档案（对应表 t_campus，行政管理-校区管理）。
  校区以唯一名称标识；学员主数据 t_student.campus 存校区名称文本， 员工账号通过 t_user_campus 绑定多校区（构成校长/教务/学管师的数据权限范围）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Chapter.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Chapter`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  章节实体（对应数据库表 t_chapter，知识管理板块二级维度）。
  挂载于学科（subjectId）下，其下包含若干小节（t_section）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/ChapterRepo.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class ChapterRepo`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  章节-题库绑定实体（对应数据库表 t_chapter_repo）。
  章节与题库（t_repo）的多对多关联：一个章节可绑定多个题库， 一个题库也可被多个章节引用。绑定题库只能从题库管理选择，不能在本模块新增。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/CommDict.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class CommDict`
- 注解: @TableName, @Data, @EqualsAndHashCode
- 注入/字段: String code, String name, String remark, Integer dictType, Boolean deleted, long serialVersionUID

### `rdbms/src/main/java/cn/wisestar/server/domain/model/CommDictItem.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class CommDictItem`
- 注解: @TableName, @Data, @EqualsAndHashCode
- 注入/字段: String dictCode, String itemName, String itemValue, String parentItemValue, Integer itemLevel, Integer itemOrder, Boolean deleted, long serialVersionUID

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Dashboard.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Dashboard`
- 注解: @TableName, @Data
- **类说明**：
  仪表盘
- 注入/字段: String id, String key, Integer type, String projectId, DashboardSetting setting, Date createAt, String createBy, Date updateAt, String updateBy, long serialVersionUID

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Dept.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Dept`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  组织机构

### `rdbms/src/main/java/cn/wisestar/server/domain/model/EnglishAiPack.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class EnglishAiPack`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  AI 单元内容包实体（对应数据库表 t_english_ai_pack）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/EnglishGrammar.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class EnglishGrammar`
- 注解: @Data, @TableName
- **类说明**：
  英语语法实体（对应数据库表 t_english_grammar）。
- 注入/字段: String id

### `rdbms/src/main/java/cn/wisestar/server/domain/model/EnglishLearningLog.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class EnglishLearningLog`
- 注解: @Data, @TableName
- **类说明**：
  英语学习日志实体（对应数据库表 t_english_learning_log）。
  该表沿用早期字段命名（duration / correct_count / created_at）， 不继承 BaseModel。
- 注入/字段: String id

### `rdbms/src/main/java/cn/wisestar/server/domain/model/EnglishSentence.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class EnglishSentence`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  英语句子实体（对应数据库表 t_english_sentence）。
  独立的句库，与单词解耦，可单独维护与导入。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/EnglishSentenceBook.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class EnglishSentenceBook`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  用户句子本实体（对应数据库表 t_english_sentence_book）。
  记录学员对句子的熟练度与复习调度信息。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/EnglishUnit.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class EnglishUnit`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  英语单元目录实体（对应数据库表 t_english_unit）。
  单词与句子共用同一套单元目录，用于学习中心的单元列表展示与排序。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/EnglishWord.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class EnglishWord`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  英语单词实体（对应数据库表 t_english_word）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/EnglishWordBook.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class EnglishWordBook`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  用户单词本实体（对应数据库表 t_english_word_book）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/File.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class File`
- 注解: @Data, @TableName, @Accessors, @EqualsAndHashCode

### `rdbms/src/main/java/cn/wisestar/server/domain/model/KnowledgePoint.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class KnowledgePoint`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  知识点实体（对应数据库表 t_knowledge_point，知识管理板块最小学习单元）。
  挂载于小节（sectionId）下；题目通过 t_knowledge_point_question 关联表 从题目库（t_template）选择绑定（不能在本模块新增题目）。 imageUrl 为知识点配图地址（复用 /api/file/create 上传返回的 previewUrl）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/KnowledgePointQuestion.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class KnowledgePointQuestion`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  知识点-题目绑定实体（对应数据库表 t_knowledge_point_question）。
  知识点与题目（t_template）的多对多关联：一个知识点可绑定多道题库题目， 一道题目也可被多个知识点引用。绑定题目只能从题目库选择，不能在本模块新增。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/MallGoods.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class MallGoods`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  积分商城商品实体（对应数据库表 t_mall_goods）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/MallOrder.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class MallOrder`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  学币兑换订单实体（对应数据库表 t_mall_order）。
  学员在商城发起兑换时生成订单并「暂时扣除」学币，同时下发随机 6 位核销码； 老师在核销页凭核销码确认后订单完成（学币正式扣除）。订单记录永久保留。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Permission.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Permission`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  这个只用来存用户自定义的权限

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Position.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Position`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  岗位

### `rdbms/src/main/java/cn/wisestar/server/domain/model/PracticeDetail.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class PracticeDetail`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  练习逐题明细（t_practice_detail）。
  一次练习（PracticeRecord）下每道题的作答结果：学生答案、判分结果、得分。 错题本的数据来源：is_correct = 0 的记录即为错题，按 question_id 关联回题目。

  **判分结果约定**：is_correct = 1 正确 / 0 错误（含未作答）/ null 无标准答案不计分。
- 注入/字段: String practiceId, String questionId, String questionType, String userAnswer, Integer isCorrect, Double score, Boolean corrected, java.util.Date correctedAt

### `rdbms/src/main/java/cn/wisestar/server/domain/model/PracticeRecord.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class PracticeRecord`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  练习会话记录（t_practice_record）。
  学员每次交卷生成一条记录，保存本次练习的模式、来源题库、题目数、得分与用时， 作为错题本、学习统计等后续功能的数据底座。

  **判分结果约定**：每题正确与否记录在 PracticeDetail（t_practice_detail）， 本表只存汇总字段；错题查询 = t_practice_detail 中 is_correct=0 的记录。
- 注入/字段: String userId, String mode, String repoId, Integer totalQuestions, Integer correctCount, Double score, Double totalScore, Long durationMs

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Project.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Project`
- 注解: @Data, @TableName, @EqualsAndHashCode
- 注入/字段: SurveySchema survey, ProjectSetting setting, Date createAt, Date updateAt

### `rdbms/src/main/java/cn/wisestar/server/domain/model/ProjectPartner.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class ProjectPartner`
- 注解: @TableName, @Data, @EqualsAndHashCode
- **类说明**：
  项目参与者
- 注入/字段: String projectId, Integer type, Integer status, String userId, String userName, String groupId, String dataPermission, long serialVersionUID, Boolean deleted

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Repo.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Repo`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  模板组
- 注入/字段: String[] tag, boolean deleted

### `rdbms/src/main/java/cn/wisestar/server/domain/model/RepoTemplate.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class RepoTemplate`
- 注解: @Data, @TableName
- **类说明**：
  模板库模板
- 注入/字段: Date createAt, String createBy

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Role.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Role`
- 注解: @Data, @TableName, @EqualsAndHashCode
- 注入/字段: Date createAt

### `rdbms/src/main/java/cn/wisestar/server/domain/model/RolePermission.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class RolePermission`
- 注解: @Data, @TableName, @EqualsAndHashCode

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Section.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Section`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  小节实体（对应数据库表 t_section，知识管理板块三级维度）。
  挂载于章节（chapterId）下，其下包含若干知识点（t_knowledge_point）。 content/practice 为 JSON 字符串，由前端序列化提交、解析展示，后端仅透传存储。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/SectionPass.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class SectionPass`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  小节通关记录（t_section_pass）。
  每学员每小节唯一一条：累加交卷次数，保留历史最佳正确率/星级， 首次通关时记录 firstPassAt 且 passed 保持 true（不回退）。
- 注入/字段: String userId, String sectionId

### `rdbms/src/main/java/cn/wisestar/server/domain/model/SectionRepo.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class SectionRepo`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  小节-题库绑定实体（对应数据库表 t_section_repo）。
  小节与题库（t_repo）的多对多关联：一个小节可绑定多个题库， 一个题库也可被多个小节引用。绑定题库只能从题库管理选择，不能在本模块新增。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Student.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Student`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  学员主数据实体（对应数据库表 t_student，学员管理模块）。
  **账号关联**：新增学员时服务端在同一事务内创建 t_student 与 t_account（user_type=Student、auth_account=学号），学号即学员登录账号。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/StudentActivity.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class StudentActivity`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  学员实时位置实体（对应数据库表 t_student_activity，学员动态监控模块）。
  **用途**：学员端在路由变化/进入习题时上报当前位置（page/questionId）， 后台老师按学员实时查看其在哪个页面、哪道习题，并可查看该习题答案与解析。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/StudentCoin.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class StudentCoin`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  学员学币发放记录实体（对应数据库表 t_student_coin，老师手动加学币）。
- 注入/字段: Boolean deleted

### `rdbms/src/main/java/cn/wisestar/server/domain/model/StudentOrder.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class StudentOrder`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  学员订单实体（对应数据库表 t_student_order，学员管理模块）。
  **权限载体**：创建订单时服务端在同一事务内写入 t_student_order 与 t_student_permission（多选学科×年级笛卡尔积展开），权限有效期 expireAt 由服务端按 duration + durationUnit 计算。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/StudentPermission.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class StudentPermission`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  学员权限实体（对应数据库表 t_student_permission，学员管理模块）。
  **用途**：订单中多选学科×多选年级按笛卡尔积展开为权限行， 学员端鉴权按 student_id + subject_id + grade 且 expire_at > NOW() 查询。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/StudentRecord.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class StudentRecord`
- 注解: @Data, @TableName
- **类说明**：
  学员学习记录（记录学员学习轨迹）
- 注入/字段: String id, Date createAt, Date updateAt, Integer isDeleted

### `rdbms/src/main/java/cn/wisestar/server/domain/model/StudentTask.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class StudentTask`
- 注解: @Data, @TableName
- **类说明**：
  学员任务分配表
- 注入/字段: String id, Date createTime, Date updateTime, Integer deleted

### `rdbms/src/main/java/cn/wisestar/server/domain/model/StudySession.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class StudySession`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  学习会话（t_study_session）。
  学员端周期性心跳续会话：距上次心跳不超过 30 分钟视为同一次会话， 否则开启新会话；`durationMs` 累计会话时长，达到 60 分钟触发当日学习总结。
- 注入/字段: Date startAt

### `rdbms/src/main/java/cn/wisestar/server/domain/model/StudySummary.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class StudySummary`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  当日学习总结（t_study_summary）。
  学员学习会话累计满 60 分钟后由系统生成；以 `studentId + summaryDate` 定位当日唯一总结，重复触发时覆盖更新。`model` 记录生成来源， AI 不可用时由规则模板生成并标记为 `rule`。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Subject.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Subject`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  学科字典实体（对应数据库表 t_subject，知识管理板块一级维度）。
  **层级关系**：学科 → 章节（t_chapter）→ 小节（t_section）→ 知识点（t_knowledge_point）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/SubjectSemester.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class SubjectSemester`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  学科学期学习币实体（对应数据库表 t_subject_semester，分学科、单学期上限 10000，仅作兑换）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/SysInfo.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class SysInfo`
- 注解: @TableName, @Data
- **类说明**：
  系统信息
- 注入/字段: String id, String name, String description, String avatar, String locale, String version, Boolean isDefault, SystemInfo.RegisterInfo registerInfo, SystemInfo.SystemSetting setting, SystemInfo.AiSetting aiSetting, Date createAt, String createBy, Date updateAt, String updateBy, long serialVersionUID

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Tag.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Tag`
- 注解: @Data, @TableName
- **类说明**：
  模板组

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Task.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Task`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  今日任务实体（对应数据库表 t_task，老师后台布置、学员端呈现）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/Template.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class Template`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  问题模板表实体（对应数据库表 t_template） 【类职责】 代表"题库"中的一个具体题目（一道单选/多选/判断/填空/简答题），是 AI 自习室系统中 题库（Repo）与问卷/考试（Project）之间的桥梁：一个题库（t_repo）下挂多道题目， 题目以"模板"形式存储；出卷时（pickQuestionFromRepo / RandomSurveyProcessor）从题库 拉取模板并渲染为问卷题目（SurveySchema）。 【被谁调用】 - 数据访问层：TemplateMapper（MyBatis-Plus 基础 CRUD）、t_tag 标签表通过 entity_id 关联本表 - 业务层：TemplateServiceImpl（四维筛选 listTemplate）、RepoServiceImpl（导出/批量导入/ 随机抽题 pickQuestionFromRepo）、SurveyServiceImpl（题库练习加载题目）、RandomSurveyProcessor 【依赖什么】 - 继承 BaseModel，自动获得 id、createAt、createBy、updateAt、updateBy、deleted 逻辑删除字段 - 题目 JSON 结构复用 domain.dto.SurveySchema（问卷 schema 同一套结构） - ProjectModeEnum：题目所处模式（survey 问卷 / exam 考试） 【核心数据流】 题库管理端（Controller）→ RepoServiceImpl/TemplateServiceImpl → TemplateMapper → t_template 表；出题时 RepoServiceImpl.pickQuestionFromRepo 将本实体转换为 SurveySchema 下发到前端答题。题目难度体系（subject/chapter/knowledgePoint/difficulty 四字段）在答题 提交时被 AnswerServiceImpl.generateAnswerDetails 快照进 t_answer_detail 答题明细表， 供 AnalysisServiceImpl 做知识点聚合分析。
- 注入/字段: SurveySchema template, String[] tag, String createBy, String[] knowledgePoint

### `rdbms/src/main/java/cn/wisestar/server/domain/model/User.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class User`
- 注解: @Data, @TableName, @Accessors, @EqualsAndHashCode

### `rdbms/src/main/java/cn/wisestar/server/domain/model/UserBook.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class UserBook`
- 注解: @TableName, @Data, @EqualsAndHashCode
- **类说明**：
  错题本
- 注入/字段: String templateId, String name, Integer wrongTimes, Integer correctTimes, String note, Integer status, Integer type, long serialVersionUID, Boolean deleted

### `rdbms/src/main/java/cn/wisestar/server/domain/model/UserCampus.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class UserCampus`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  员工-校区绑定关系（对应表 t_user_campus）。
  一个员工账号可绑定多个校区；校长/教务/学管师账号的绑定校区 构成其学员业务数据可见范围（停用校区仍保留绑定与范围）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/UserKnowledgeProgress.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class UserKnowledgeProgress`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  知识点掌握度实体（对应数据库表 t_user_knowledge_progress，按学科+版本独立）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/UserLearningRecord.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class UserLearningRecord`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  学习行为记录实体（对应数据库表 t_user_learning_record，奖励发放与防刷依据）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/UserPoints.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class UserPoints`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  用户学海积分/头衔实体（对应数据库表 t_user_points，终身、全学科，仅作荣誉评价）。

### `rdbms/src/main/java/cn/wisestar/server/domain/model/UserPosition.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class UserPosition`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  用户岗位
- 注入/字段: boolean deleted

### `rdbms/src/main/java/cn/wisestar/server/domain/model/UserRepo.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class UserRepo`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  学员-题库分配（练习题库分配记录）
  **所属模块**：rdbms 模块数据模型包（cn.wisestar.server.domain.model）。

  **表**：t_user_repo。记录老师为学员手动分配的可练习题库， 学员端「我的题库」= 手动分配记录 ∪ 按标签自动匹配的题库。

  分配方式（assignType）：manual=老师手动分配；auto=系统按标签自动分配 （auto 类型为查询时动态计算，不落库，本表仅存 manual）。
- 注入/字段: String userId, String repoId, String assignType

### `rdbms/src/main/java/cn/wisestar/server/domain/model/UserRole.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class UserRole`
- 注解: @Data, @TableName, @EqualsAndHashCode
- 注入/字段: boolean deleted

### `rdbms/src/main/java/cn/wisestar/server/domain/model/UserWeakKnowledge.java`
- 包: `cn.wisestar.server.domain.model`
- 类型: `class UserWeakKnowledge`
- 注解: @Data, @TableName, @EqualsAndHashCode
- **类说明**：
  薄弱知识点研判实体（对应数据库表 t_user_weak_knowledge，active/conquered）。

### `rdbms/src/main/java/cn/wisestar/server/impl/AiServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class AiServiceImpl`
- 注解: @Slf4j, @Service
- **类说明**：
  AI 服务实现（TTS 语音 + DALL-E 图片 + GPT 文本）。
- 注入/字段: boolean ttsEnabled, String ttsApiUrl, String ttsApiKey, boolean imageEnabled, String imageUrl, String imageApiKey, boolean textEnabled, String textApiUrl, String textApiKey
- 方法:
  - `public String generateSpeech(String text, String lang)`
  - `public String generateImage(String prompt)`
  - `public String generateText(String prompt)`

### `rdbms/src/main/java/cn/wisestar/server/impl/AnalysisServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class AnalysisServiceImpl`
- 注解: @Service, @RequiredArgsConstructor, @Slf4j
- **类说明**：
  学生答题情况分析实现：基于答题明细表 t_answer_detail 按知识点聚合统计。 【类职责】 提供学生"知识点掌握情况"分析能力：按学生 +（可选）学科/章节/知识点筛选， 汇总每个知识点的作答次数（attempts）、答对次数（correctCount）与正确率（correctRate）， 用于生成学生画像（优势/薄弱知识点），支撑 AI 自习室的个性化学情反馈。 【被谁调用】 - 上层：AnalysisController（知识点评分统计接口、学生画像接口） - 接口定义：AnalysisService（本类是其实现） 【依赖什么】 - AnswerDetailMapper：读取 t_answer_detail（MyBatis-Plus lambda 条件查询） - SecurityContextUtils：获取当前登录用户 ID（studentId 未传时默认查自己） - KnowledgePointQuery / KnowledgePointStat：查询入参与聚合结果 DTO 【核心数据流】 前端请求 → AnalysisController → knowledgePointStats(query) 或 studentProfile(studentId) → 确定查询目标学生（入参优先，否则当前登录用户）→ aggregate() 从 t_answer_detail 按 createBy(学生) + subject/chapter 过滤 → 内存中把每题的 knowledge_point 逗号拆开， 按"学科|章节|知识点"聚合 → 计算正确率 → 返回 List。 【注意（越权防护，已处理）】 AnalysisApi（Controller 层）已加入越权校验：非管理员传入他人 studentId 查询画像时， 强制改为查询当前登录用户自己；管理员可查任意学生。本实现层保持纯查询职责。
- 方法:
  - `public List<KnowledgePointStat> knowledgePointStats(KnowledgePointQuery query)`
    知识点掌握情况统计（对外接口）。
  - `public List<KnowledgePointStat> studentProfile(String studentId)`
    学生画像（对外接口）：聚合指定学生的全部答题明细，不限制学科/章节/知识点维度。

### `rdbms/src/main/java/cn/wisestar/server/impl/AnswerServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class AnswerServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor, @Slf4j
- **类说明**：
  答卷（Answer）业务实现：答卷的保存/查询/导出/删除、考试计分与答题明细生成。 【类职责】 处理"答卷"全生命周期业务： 1. 答卷 CRUD：listAnswer 分页查询、getAnswer 详情、saveAnswer/updateAnswer 保存与更新、 deleteAnswer 删除、回收站（listAnswerDeleted/restoreAnswer/batchDestroyAnswer） 2. 考试计分：beforeSaveAnswer → computeExamScore 用 AnswerScoreEvaluator 计算总分与每题得分 3. 答题明细生成：generateAnswerDetails（提交答卷时按题落库 t_answer_detail， 供学生知识点分析使用，幂等先删后插） 4. 导出：downloadSurvey 导出 xlsx、downloadAttachment 附件下载/打包 zip、 附件重命名表达式 parseAttachmentNameByExp 5. 其他：Excel 批量导入 upload、练习历史 historyExercise、关联问卷答案同步 updateLinkSurveyAnswer 【被谁调用】 - Controller：AnswerController（管理端答卷管理）、SurveyController（公开答卷提交间接调用） - 业务层：SurveyServiceImpl.saveAnswer / tempSaveAnswer / loadProject、RepoServiceImpl （错题本保存临时答案）、RandomSurveyProcessor（保存随机 schema）、UserServiceImpl （历史任务查询）、FileServiceImpl（附件下载时验证项目） 【依赖什么】 - AnswerMapper（BaseMapper CRUD + 回收站自定义 SQL）、AnswerDetailMapper（答题明细）、 ProjectMapper / ProjectPartnerMapper（项目与参与人） - FileService（附件）、UserService/DeptService/ProjectService（答案附加信息回填） - AnswerViewMapper（MapStruct：Answer↔AnswerView↔AnswerRequest 转换） - AnswerScoreEvaluator（考试计分）、SchemaHelper（schema 扁平化/解析）、ExcelExporter（导出） 【核心数据流】 学生提交答卷 → SurveyServiceImpl.saveAnswer → saveAnswer/updateAnswer → beforeSaveAnswer（计分 + 关联问卷同步）→ save/updateById 写 t_answer → generateAnswerDetails 按题写 t_answer_detail（先删后插） → AnswerViewMapper.toView 返回 AnswerView（含题目附加信息/排名等）。
- 方法:
  - `public PaginationResponse<AnswerView> listAnswer(AnswerQuery query)`
    分页查询答卷列表。
  - `public AnswerView getAnswer(AnswerQuery query)`
    查询单份答卷详情。
  - `public AnswerView saveAnswer(AnswerRequest request)`
    保存答卷（新增或更新分派）。
  - `public long count(AnswerQuery query)`
    统计答卷数量（用于提交限制校验，如最大答题数/时间窗/登录/Cookie/IP/白名单限制）。
  - `public AnswerView updateAnswer(AnswerRequest request)`
    更新答卷：保存前计算考试分值/同步关联问卷答案，更新后重新生成答题明细。
  - `public void deleteAnswer(AnswerRequest request)`
    删除答卷（逻辑删除，is_deleted=1；回收站可见，可恢复）。
  - `public DownloadData downloadSurvey(DownloadQuery query)`
    导出答卷为 Excel（异步管道流，避免大数据量一次性载入内存）。
  - `public DownloadData downloadAttachment(DownloadQuery query)`
    下载答卷附件：指定 answerId 时下载单份答卷附件，否则把选定答卷的附件打包为 zip。
  - `public List<AnswerView> listAnswerDeleted(AnswerQuery query)`
    查询回收站中已逻辑删除的答卷。
  - `public void batchDestroyAnswer(AnswerRequest request)`
    彻底销毁答卷（物理删除，不可恢复）。
  - `public void restoreAnswer(AnswerRequest request)`
    恢复回收站中的答卷（is_deleted 置 0）。
  - `public AnswerUploadView upload(AnswerUploadRequest request)`
    Excel 批量导入答卷（行头匹配已有项目 schema 或自动创建项目 schema）。
  - `public PaginationResponse<ExerciseView> historyExercise(HistoryExerciseQuery query)`
    练习历史分页查询（顺序/随机/错题练习记录）。

### `rdbms/src/main/java/cn/wisestar/server/impl/CampusScopeServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class CampusScopeServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  校区数据权限解析实现。
  依据当前登录用户的角色配置的 `data_scope`（ALL 全校可见 / CAMPUS 仅绑定校区）与 t_user_campus 绑定计算可见范围，供学员/订单/督学 服务统一过滤。多个角色并存时取更宽松者：只要有一个角色为 ALL 即全校可见。
- 方法:
  - `public CampusScope resolveScope()`

### `rdbms/src/main/java/cn/wisestar/server/impl/CampusServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class CampusServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- **类说明**：
  校区服务实现（行政管理-校区管理）。
  校区列表/下拉遵守当前账号数据权限范围；改名在同事务内同步学员引用 （t_student.campus 文本 = 校区名，保证引用跟随）；删除仅允许零引用并物理删除， 使同一名称可重新创建。
- 方法:
  - `public List<CampusView> listCampuses()`
  - `public List<CampusView> campusOptions(boolean includeDisabled)`
  - `public void createCampus(CampusRequest request)`
  - `public void updateCampus(CampusRequest request)`
  - `public void deleteCampus(String id)`
  - `public void checkAssignable(String name, String currentName)`

### `rdbms/src/main/java/cn/wisestar/server/impl/ChapterServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class ChapterServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- **类说明**：
  章节管理业务实现（知识管理板块二级维度）。
  【被谁调用】ChapterApi（管理端章节管理）。

  【依赖什么】ChapterMapper/SectionMapper/KnowledgePointMapper/KnowledgePointQuestionMapper/ ChapterRepoMapper/SectionRepoMapper/RepoMapper（BaseMapper CRUD）、 ChapterViewMapper/RepoViewMapper（MapStruct 转换）。

  【数据流】ChapterApi → ChapterServiceImpl → ChapterMapper（t_chapter）；列表返回时经 SectionMapper 统计各章节小节数、经 SectionRepoMapper 统计章节下小节直绑练习去重数 （业务规则：练习仅支持绑定到小节，章节自身不直接绑定练习）； 删除时级联逻辑删除其下小节/知识点/知识点题目绑定、章节级残留绑定与各小节题库绑定。
- 方法:
  - `public List<ChapterView> listChapters(ChapterRequest query)`
    章节列表（按学科/年级/学期/版本过滤，sort 升序），并统计各章节下小节数 与练习数（章节自身不再直接绑定练习，练习数 = 该章节下各小节直绑练习的去重数）。
  - `public String addChapter(ChapterRequest request)`
    新增章节（图标/排序走系统默认：图标缺省 📖，排序自动追加到该学科末尾）。
  - `public ImportResultView importChapters(ChapterImportRequest request)`
    批量导入章节（Excel：学科名/章节名称/年级(选填)/学期(选填)/版本(选填)； 按学科名匹配 t_subject.name 定位归属，学科+章节名重名跳过）。
  - `public void updateChapter(ChapterRequest request)`
    更新章节（仅更新管理端可维护列：名称/年级/学期/版本/排序； 图标由系统默认维护，不随本次更新改动；排序留空时不改动原值）。
  - `public void deleteChapter(ChapterRequest request)`
    删除章节（级联逻辑删除其下小节、知识点、知识点-题目绑定、章节题库绑定与各小节题库绑定）。
  - `public void saveRepos(ChapterRepoRequest request)`
    保存章节-题库绑定（已停用）。
    业务规则：练习仅支持绑定到小节，章节不再作为练习的绑定对象。 已迁移的历史章节绑定（t_chapter_repo 存量数据）已展开写入其下小节。 本接口调用一律拒绝，前端不再暴露章节级「绑定练习」入口。
  - `public List<RepoView> listRepos(String chapterId)`
    查询章节已绑定的题库列表（已停用，恒为空）。
    章节不再支持直接绑定题库（历史数据已迁移到小节），本方法仅保留兼容读取。
  - `public void exportChapters(ChapterRequest query)`
    导出章节列表为 Excel（列：章节名称/年级/学期/版本/小节数/练习数）， 与列表页过滤口径一致（subjectId/grade/term/version 可选），附件下载。

### `rdbms/src/main/java/cn/wisestar/server/impl/CheckinServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class CheckinServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  学员每日签到实现。
  签到固定发放学习币，按自然日幂等；无学科语义，计入学员首个有效权限学科的单学期上限。
- 方法:
  - `public StudentCheckinView view(String userId)`
  - `public StudentCheckinView checkin(String userId)`

### `rdbms/src/main/java/cn/wisestar/server/impl/DashboardServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class DashboardServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- 方法:
  - `public List<DashboardView> listDashboard(DashboardQuery query)`
  - `public void saveDashboard(List<DashboardRequest> request)`

### `rdbms/src/main/java/cn/wisestar/server/impl/DeptServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class DeptServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- 方法:
  - `public List<DeptView> listDept(SelectDeptRequest request)`
  - `public DeptView getDept(String id)`
  - `public void addDept(DeptRequest request)`
  - `public void updateDept(DeptRequest request)`
  - `public void deleteDept(String id)`
  - `public void sortDept(DeptSortRequest request)`

### `rdbms/src/main/java/cn/wisestar/server/impl/DictItemServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class DictItemServiceImpl`
- 注解: @Service, @Transactional

### `rdbms/src/main/java/cn/wisestar/server/impl/DictServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class DictServiceImpl`
- 注解: @Transactional, @Service, @RequiredArgsConstructor
- 方法:
  - `public PaginationResponse<CommDictView> listDict(CommDictQuery query)`
  - `public void addDict(CommDictRequest request)`
  - `public void updateDict(CommDictRequest request)`
  - `public void deleteDict(String id)`
  - `public PaginationResponse<CommDictItemView> listDictItem(CommDictItemQuery query)`
  - `public void saveOrUpdateDictItem(CommDictItemRequest request)`
  - `public void deleteDictItem(String id)`
  - `public void importDictItem(CommDictItemRequest request)`
  - `public List<CommDictView> selectDict()`

### `rdbms/src/main/java/cn/wisestar/server/impl/EnglishAiPackServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class EnglishAiPackServiceImpl`
- 注解: @Slf4j, @Service
- **类说明**：
  AI 单元内容包服务实现。
  功能：调用系统 AI 设置（SystemInfo.AiSetting：SiliconFlow 平台，与 AI 问答同一套 enabled/token/models 配置）为指定 版本+年级+单元 生成整套英语学习内容；生成结果先 以内容包形式预览/保存，确认后再同步到正式词库（t_english_word）与语法库 （t_english_grammar），供后续学生端单词记忆、语法学习闭环使用。
- 方法:
  - `public EnglishAiPackServiceImpl(EnglishAiPackMapper packMapper, EnglishWordMapper wordMapper, EnglishGrammarMapper grammarMapper, SystemService systemService)`
  - `public PaginationResponse<EnglishAiPackView> listPacks(EnglishAiPackQuery query)`
  - `public EnglishAiPackView getPack(String id)`
  - `public void deletePack(String id)`
  - `public EnglishAiPackView generateUnit(String version, String grade, String unit, String topic)`
  - `public EnglishAiPackView savePack(EnglishAiPackView pack)`
  - `public PackSyncResult syncToBank(String id)`

### `rdbms/src/main/java/cn/wisestar/server/impl/EnglishReviewScheduler.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class EnglishReviewScheduler`
- **类说明**：
  英语复习调度器（单词与句子共用）。
  熟练度规则：答对 +1（上限 4），答错 -1（下限 0）。 下次复习间隔沿用艾宾浩斯曲线： 0→5 分钟、1→30 分钟、2→12 小时、3→1 天、4→2 天、5+→4 天。
- 方法:
  - `public static int adjustFamiliarity(int current, boolean correct)`
    根据当前熟练度与作答结果计算新的熟练度。
  - `public static Date nextReviewTime(int familiarity)`
    根据熟练度计算下次复习时间。

### `rdbms/src/main/java/cn/wisestar/server/impl/EnglishSentenceServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class EnglishSentenceServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  英语句库服务实现。
- 方法:
  - `public PaginationResponse<EnglishSentenceView> list(EnglishSentenceQuery query)`
  - `public void saveOrUpdate(EnglishSentenceView view)`
  - `public void delete(String id)`
  - `public ImportResult importSentences(MultipartFile file)`

### `rdbms/src/main/java/cn/wisestar/server/impl/EnglishStudentServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class EnglishStudentServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  英语学员学习服务实现。
- 方法:
  - `public List<EnglishUnitProgressView> unitProgress(String userId, String version, String grade, String term)`
  - `public List<EnglishSentenceView> sentences(String userId, String version, String grade, String term, String unit)`
  - `public List<EnglishSentenceView> studySentences(String userId, int limit)`
  - `public void recordSentence(String userId, String sentenceId, boolean correct)`
  - `public List<ReviewSessionView> reviewSession(String userId, int limit)`
  - `public void recordSession(String userId, String type, int durationSeconds, int correctCount)`

### `rdbms/src/main/java/cn/wisestar/server/impl/EnglishUnitServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class EnglishUnitServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  英语单元目录服务实现。
  单元列表由「词库单元 ∪ 句库单元 ∪ 单元目录表」派生： 词库/句库决定实际有哪些单元，单元目录表仅用于补充占位单元与自定义排序， 因此无需依赖脆弱的跨方言种子 SQL。
- 方法:
  - `public PaginationResponse<EnglishUnitView> list(EnglishUnitQuery query)`
  - `public List<EnglishUnitView> listByBook(String version, String grade, String term)`
  - `public void saveOrUpdate(EnglishUnitView view)`
  - `public void delete(String id)`

### `rdbms/src/main/java/cn/wisestar/server/impl/EnglishWordAiService.java`
- 包: `cn.wisestar.server.service`
- 类型: `class EnglishWordAiService`
- 注解: @Slf4j, @Service, @RequiredArgsConstructor
- **类说明**：
  英语单词 AI 生成服务。
- 方法:
  - `public void generateContent(EnglishWord word)`
    为单个单词生成 AI 内容（图片/音频/例句）。
  - `public ImportResult batchGenerateContent(List<String> wordIds)`
    批量为单词生成 AI 内容。
  - `public ImportResult batchGenerateByCondition(String version, String grade, String unit)`
    为指定版本/年级/单元的单词批量生成 AI 内容。

### `rdbms/src/main/java/cn/wisestar/server/impl/EnglishWordManagerServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class EnglishWordManagerServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  英语单词管理服务实现。
- 方法:
  - `public PaginationResponse<EnglishWordView> listWords(EnglishWordQuery query)`
  - `public EnglishWordView getDetail(String id)`
  - `public void createWord(EnglishWordView word)`
  - `public void updateWord(EnglishWordView word)`
  - `public void deleteWord(String id)`
  - `public ImportResult importWords(MultipartFile file)`

### `rdbms/src/main/java/cn/wisestar/server/impl/EnglishWordServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class EnglishWordServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  英语单词学习服务实现。
- 方法:
  - `public PaginationResponse<EnglishWordView> listWords(EnglishWordQuery query)`
  - `public PaginationResponse<EnglishWordView> wordBook(String userId, EnglishWordQuery query)`
  - `public List<EnglishWordView> getStudyWords(String userId, int limit)`
  - `public void recordLearning(String userId, String wordId, boolean correct)`

### `rdbms/src/main/java/cn/wisestar/server/impl/EvaluationServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class EvaluationServiceImpl`
- 注解: @Slf4j, @Service, @Transactional, @RequiredArgsConstructor
- **类说明**：
  学员学习评价实现。
  掌握度 = 最近 5 次练习正确率的加权平均（越近权重越高 5,4,3,2,1）； 薄弱 = 掌握度 <55 或存在未订正错题；攻克 = 正确率 ≥80（显式）或 掌握度 ≥70 且本次正确率 ≥80（自动）。
- 方法:
  - `public void recordPractice(PracticeEvaluationContext context)`
  - `public boolean conquer(String userId, String knowledgePointId, int correctRate)`
  - `public void refreshWeakAfterCorrection(String userId, String knowledgePointId)`

### `rdbms/src/main/java/cn/wisestar/server/impl/FileServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class FileServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor, @Slf4j
- 注入/字段: ProjectService projectService, SurveyService surveyService
- 方法:
  - `public void deleteFile(String id)`
  - `public FileView upload(UploadFileRequest request)`
  - `public List<FileView> listFiles(FileQuery query)`
  - `public ResponseEntity<Resource> loadFile(FileQuery query)`
  - `public ResponseEntity<Resource> downloadTemplate(String name)`

### `rdbms/src/main/java/cn/wisestar/server/impl/KnowledgePointServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class KnowledgePointServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- **类说明**：
  知识点管理业务实现（知识管理板块最小学习单元）。
  【被谁调用】KnowledgePointApi（管理端知识点管理）。

  【依赖什么】KnowledgePointMapper/SectionMapper/ChapterMapper/SubjectMapper/ KnowledgePointQuestionMapper/TemplateMapper（BaseMapper CRUD）、 KnowledgePointViewMapper/TemplateViewMapper（MapStruct 转换）。

  【数据流】KnowledgePointApi → KnowledgePointServiceImpl → KnowledgePointMapper （t_knowledge_point）；分页列表回填三级归属名称（学科/章节/小节）与绑定题目数； 题目绑定经 t_knowledge_point_question 关联题目库（t_template），全量替换式保存。
- 方法:
  - `public PaginationResponse<KnowledgePointView> listKnowledgePoints(KnowledgePointQuery query)`
    知识点分页列表（三级下拉筛选：学科 → 章节 → 小节，条件均可选）。
    【筛选逻辑】sectionId 优先；未传时按 chapterId 找到该章节下所有小节； 再未传时按 subjectId 找到该学科下所有小节；都未传则全量分页。 返回视图回填学科/章节/小节名称与已绑定题目数。
  - `public String addKnowledgePoint(KnowledgePointRequest request)`
    新增知识点。
  - `public ImportResultView importKnowledgePoints(KnowledgePointImportRequest request)`
    批量导入知识点（Excel：学科名/章节名/小节名/知识点名/排序(选填)/年级(选填)/学期(选填)/内容设置(选填，仅文本)； 内容设置不支持图片，整格文本作为一条讲解要点写入 content JSON； 按 sectionId+name 去重；跳过原因分类统计（缺失必填/归属未匹配/重名）。
  - `public void updateKnowledgePoint(KnowledgePointRequest request)`
    更新知识点（含内容设置 JSON 与图片地址）。 grade/term/importance 支持清空：请求显式传空串表达清除，统一落 null（updateById 忽略 null， 故清空需在 updateById 之外显式覆盖；未传的调用方（如仅存内容设置）不受影响）。
  - `public void deleteKnowledgePoint(KnowledgePointRequest request)`
    删除知识点（连带逻辑删除其知识点-题目绑定）。
  - `public void saveQuestions(KnowledgePointQuestionRequest request)`
    保存知识点-题目绑定（全量替换：先清空旧绑定，再批量写入新绑定，事务内完成）。
  - `public List<TemplateView> listQuestions(String knowledgePointId)`
    查询知识点已绑定的题目列表（题目库 t_template 数据，保持绑定顺序）。
  - `public List<TemplateView> listMatchedQuestions(String knowledgePointId)`
    查询题库中「知识点标签」匹配该知识点的题目（无论是否已绑定）。
    先用 LIKE 对 knowledge_point 列与 template JSON 文本做粗筛，再在内存中按知识点名 精确匹配（忽略大小写/首尾空格），避免 JSON 内其它字段（如解析文字）出现同名造成误命中。

### `rdbms/src/main/java/cn/wisestar/server/impl/MallGoodsServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class MallGoodsServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  积分商城商品服务实现。
- 方法:
  - `public List<MallGoodsView> listGoods(Integer status)`
  - `public void createGoods(MallGoodsRequest request)`
  - `public void updateGoods(MallGoodsRequest request)`
  - `public void deleteGoods(MallGoodsRequest request)`

### `rdbms/src/main/java/cn/wisestar/server/impl/MallOrderServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class MallOrderServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  学币兑换订单服务实现。
  **兑换流程**：学员在商城对商品发起兑换 → 校验学币余额 → 生成订单（含随机 6 位核销码） 并写入负向学币流水（暂时扣除）→ 老师凭核销码在核销页完成订单（学币正式扣除），记录保留。
- 方法:
  - `public MallOrderView create(MallOrderRequest request)`
  - `public List<MallOrderView> myOrders()`
  - `public List<MallOrderView> listOrders(MallOrderRequest request)`
  - `public MallOrderView verify(MallOrderRequest request)`

### `rdbms/src/main/java/cn/wisestar/server/impl/OnlineChestServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class OnlineChestServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  在线时长宝箱服务实现。
  在线时长 = 当日 `t_study_session.duration_ms` 求和；领取状态 = 当日 `t_user_learning_record` 中 online_chest_* 行为；领取复用 RewardService#settle 实现幂等与单科上限裁剪。
- 方法:
  - `public StudentOnlineChestView view(String userId)`
  - `public StudentOnlineChestClaimView claim(String userId, Integer tierMinutes, String subjectId)`

### `rdbms/src/main/java/cn/wisestar/server/impl/OrderServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class OrderServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- **类说明**：
  学员订单业务实现（学员管理模块）。
  【被谁调用】OrderApi（管理端订单管理）。

  【依赖什么】StudentOrderMapper/StudentPermissionMapper/StudentMapper/SubjectMapper （BaseMapper CRUD）、OrderViewMapper（MapStruct 转换）。

  【核心逻辑】创建订单：校验 → 计算 expireAt（now + duration × 单位）→ 同一事务内写 t_student_order + 按学科×年级笛卡尔积展开写 t_student_permission。 作废/删除订单时同步逻辑删除该订单的权限行。
- 方法:
  - `public OrderView createOrder(OrderRequest request)`
    创建订单：订单主表 + 权限展开行（同一事务）。
  - `public PaginationResponse<OrderView> pageOrders(OrderQuery query)`
    订单分页查询（studentName 模糊匹配先按学员名查学员ID再过滤）。
  - `public void cancelOrder(OrderRequest request)`
    作废订单：status=0 + 该订单权限逻辑删除。
  - `public void deleteOrder(OrderRequest request)`
    删除订单：逻辑删除 + 该订单权限逻辑删除。

### `rdbms/src/main/java/cn/wisestar/server/impl/PositionServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class PositionServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- 方法:
  - `public PaginationResponse<PositionView> listPosition(PositionQuery query)`
  - `public void addPosition(PositionRequest request)`
  - `public void updatePosition(PositionRequest request)`
  - `public void deletePosition(String id)`
  - `public List<PositionView> selectPositions(SelectPositionRequest request)`

### `rdbms/src/main/java/cn/wisestar/server/impl/PracticeServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class PracticeServiceImpl`
- 注解: @Slf4j, @Service
- **类说明**：
  练习服务实现（交卷落库 + 错题标记）。
  **核心数据流**：
   前端交卷 → PracticeApi.submitPractice → 本类 submitPractice：

  - 按 items 中的 questionId 批量回源题目（t_template）；
  - 逐题用 AnswerJudgeUtil 复核判分（与前端判分语义一致，防篡改）；
  - 汇总得分/答对数后写 t_practice_record，逐题写 t_practice_detail （is_correct=0 即错题，供阶段二错题本查询）；

  **判分与计分约定**（与前端 practiceHelpers 一致）：
   - 每题分值 = attribute.examScore，无则 1 分； - 单项填空/多项填空：有标准答案时逐空判分（attribute.examBlankScores 为空时按 整题分 ÷ 空位数 均摊），按正确空位分值累加计分（部分给分）；其余题型整题判分： 答对得分 = 题分；答错/未作答 = 0 分；无标准答案 = 不计分也不当错题； - is_correct=1 仅表示该题全对（填空中每空均正确）； - total_score = 全部题分值之和；score = 各题得分之和（含填空部分分）； correct_count = 判对（全对）题数。
  **健壮性**：某题回源失败/判分异常时跳过该题不阻断整单落库（练习记录是学习数据底座， 不能因单题异常丢失整次练习）。
- 方法:
  - `public PracticeServiceImpl(PracticeDetailMapper practiceDetailMapper, TemplateServiceImpl templateService, StudentMapper studentMapper, EvaluationService evaluationService, RewardService rewardService, KnowledgePointMapper knowledgePointMapper, SectionMapper sectionMapper, ChapterMapper chapterMapper, SectionPracticeService sectionPracticeService, SectionPassService sectionPassService)`
    构造器注入。
  - `public PracticeResultView submitPractice(PracticeSubmitRequest request)`
    提交一次练习（交卷落库 + 错题标记）。
  - `public PaginationResponse<WrongQuestionView> listWrongQuestions(WrongQuestionQuery query)`
    分页查询错题库（题目 × 学员聚合）。
  - `public void saveWrongReason(WrongReasonRequest request)`
    保存错题错误归因（校验明细属于当前学员）。

### `rdbms/src/main/java/cn/wisestar/server/impl/ProjectPartnerServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class ProjectPartnerServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- 方法:
  - `public PaginationResponse<ProjectPartnerView> listProjectPartner(ProjectPartnerQuery query)`
  - `public void addProjectPartner(ProjectPartnerRequest request)`
  - `public void deleteProjectPartner(ProjectPartnerRequest request)`
  - `public List<String> getProjectPerms()`
  - `public void downloadPartner(ProjectPartnerQuery query)`
  - `public void importPartner(WhiteListRequest request)`

### `rdbms/src/main/java/cn/wisestar/server/impl/ProjectServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class ProjectServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- 方法:
  - `public PaginationResponse<ProjectView> listProject(ProjectQuery query)`
  - `public ProjectView getProject(String id)`
  - `public ProjectView addProject(ProjectRequest request)`
  - `public void updateProject(ProjectRequest request)`
  - `public void deleteProject(ProjectRequest request)`
  - `public ProjectSetting getSetting(ProjectQuery query)`
  - `public List<ProjectView> getDeleted(ProjectQuery query)`
  - `public void batchDestroyProject(ProjectRequest request)`
  - `public void restoreProject(ProjectRequest request)`

### `rdbms/src/main/java/cn/wisestar/server/impl/RandomSurveyProcessor.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class RandomSurveyProcessor`
- 注解: @Component, @RequiredArgsConstructor, @Slf4j
- **类说明**：
  随机问题处理器 负责处理考试模式下的随机问题选择和错题练习功能
- 方法:
  - `public void processRandomSurvey(ProjectView project, PublicProjectView projectView)`
    处理随机问题逻辑，包括随机问题和错题练习

### `rdbms/src/main/java/cn/wisestar/server/impl/RepoServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class RepoServiceImpl`
- 注解: @Transactional, @Service, @RequiredArgsConstructor
- **类说明**：
  题库（Repo）业务实现：题库 CRUD、题目批量管理、随机抽题、错题本与题库导出。 【类职责】 1. 题库 CRUD：listRepo 分页查询、addRepo/updateRepo/deleteRepo、selectRepo 选择器 2. 题库-题目批量管理：batchAddRepoTemplate（Excel 导入/批量保存，按"序号+题型"幂等更新）、 batchUnBindTemplate 解绑题目 3. 随机抽题：pickQuestionFromRepo（按题库/题型/标签条件随机选题，供考试随机抽题与练习使用） 4. 题库导出增强：exportRepoQuestions（标准单表 30 列导出：学科/题型/章节/小节/知识点/题目/ 选项A-H/难易程度/正确答案1-12/解析/标签），配套辅助方法 standardRowOf / answerCellsOf / queryQuestionsForExport / buildGuideSheet；导入模板与导出共用列结构 5. 错题本：listUserBook/createUserBook/updateUserBook/deleteUserBook 【被谁调用】 - Controller：RepoController（题库管理/导出/错题本接口） - 业务层：SurveyServiceImpl（题库练习加载）、RandomSurveyProcessor（随机抽题）、 TemplateServiceImpl.selectTemplate（间接取题库列表）、AnswerServiceImpl（无） 【依赖什么】 - RepoMapper（BaseMapper + selectRepoTemplateTags/selectRepoQuestionTypes 自定义统计）、 TemplateServiceImpl（题目 CRUD）、TagServiceImpl（标签）、UserBookServiceImpl（错题本）、 AnswerServiceImpl（错题练习时保存临时答案） - RepoViewMapper / UserBookViewMapper（MapStruct 转换）、RepoTemplateExcelParseHelper（导入解析）、 ExcelExporter 对应物（导出用 fastexcel 直接写）、AnswerScoreEvaluator（错题判分） 【核心数据流】 管理端维护题库 → RepoController → RepoServiceImpl → RepoMapper（t_repo）+ TemplateServiceImpl （t_template）+ TagServiceImpl（t_tag）→ 列表回填各题统计；导出时按题型分组写入 xlsx 流。
- 方法:
  - `public PaginationResponse<RepoView> listRepo(RepoQuery query)`
    分页查询题库列表。
  - `public RepoView getRpo(String id)`
    查询单个题库详情。
  - `public void addRepo(RepoRequest request)`
    新增题库：先生成雪花 ID，再保存题库与标签。
  - `public void updateRepo(RepoRequest request)`
    更新题库（含标签全量重置：先删旧标签再批量新增）。
  - `public void deleteRepo(RepoRequest request)`
    删除题库：级联删除该题库下所有题目（t_template）与题库标签（t_tag）。
  - `public void batchAddRepoTemplate(RepoTemplateRequest request)`
    批量添加/更新题库下的题目（Excel 导入或页面批量操作）。 【内部逻辑步骤】 1. 加载该题库现有题目列表（按 repoId）； 2. 遍历请求中的每道题：以"序号 serialNo + 题型 questionType"为匹配键在现有题目中查找， 命中则复用其 id（视为更新，进 templatesUpdate），否则生成新 id（进 templatesAdd）； 3. 冗余同步：把 template.tags 同步到模板顶层 tag 数组；收集所有标签为 Tag 实体待批量入库； 4. questionType 统一取 template.type； 5. 新增批量走 templateService.batchAddTemplate（保存 + 关联 repoId）； 更新批量走 batchUpdateTemplate（更新前删除这些题目的旧标签）； 6. 最后批量插入收集好的题目标签。 【为什么这么写】 - 以"序号+题型"而非 id 做匹配，是为了 Excel 反复导入同一题库时能幂等更新， 避免同一道题重复落库； - 标签双写（模板 tag 列 + t_tag 表）是为了标签筛选走 exists 子查询（t_tag 支持 高效 IN 匹配），列表展示直接用 tag 列，各取所长。
  - `public void batchUnBindTemplate(RepoTemplateRequest request)`
    批量解绑题库下的题目（题目从题库移除，保留在题目管理全局库中）。
  - `public void bindTemplates(RepoTemplateRequest request)`
    批量绑定已有题目到题库（题目管理中的题目 → 指定题库）。
    仅更新题目归属字段 repoId，不改动题目内容（名称/题型/答案/解析等）。 已在目标题库的题目（repoId 已等于目标值）自动跳过，保证幂等。

    注意：跳过条件必须写成 `repo_id IS NULL OR repo_id <> 目标`—— 若只写 `repo_id <> 目标`，SQL 三值逻辑会把 repo_id 为 NULL （尚未绑定任何题库）的题目一并排除，导致题目管理中的新题目永远绑定不进来。
  - `public List<SurveySchema> pickQuestionFromRepo(List<ProjectSetting.RandomSurveyCondition> repos)`
    从题库中随机挑选题目（考试随机抽题/练习出卷核心）。 【内部逻辑步骤】 1. 遍历每个抽题条件（RandomSurveyCondition）：按 repoId + 题型（types 可选）+ 标签（tags 可选，t_tag exists 子查询）筛选题目； 2. 若配置了 questionsNum：Collections.shuffle 打乱后截取前 N 题； 3. 给选中题目附加分值：配置了 examScore 时写入题目 attribute.examScore（无 attribute 则先创建），同一题目在多条件中重复命中时只保留第一个（去重）； 4. 把 Template 转为 SurveySchema（id 用模板 id，保证答案回填能对上题目）， 按题型排序返回（相同题型排在一起，便于前端分组展示）。 【为什么这么写】 - 随机性用 shuffle 而非数据库 RAND()：题目总量可控，内存打乱更稳定可控； - schema.id 复用模板 id：提交答案时 questionId 即模板 id，明细/计分能回源到原题。
  - `public void importFromTemplate(RepoTemplateRequest request)`
    从 Excel 文件导入题目（先解析再批量入库）。
  - `public List<RepoView> myRepos()`
    查询当前登录学员「我的题库」。
    **分配来源**（并集去重）：

    - 老师手动分配：t_user_repo 中 user_id=当前用户 的题库
    - 系统按标签自动分配：题库 tag 与学员标签（t_tag, category=user）有交集
  - `public void assignRepo(String userId, List<String> repoIds)`
    老师手动分配题库给学员（批量，幂等）。
  - `public void deleteAssign(List<String> ids)`
    删除分配记录（批量，逻辑删除）。
  - `public List<RepoAssignView> listAssign(String userId)`
    查询某学员的分配记录（管理端展示）。
  - `public Set<String> getUserTags(String userId)`
    查询学员标签（t_tag, category=user）。
  - `public void saveUserTags(String userId, String[] tags)`
    保存学员标签（覆盖式，category=user），用于按标签自动分配题库。
  - `public PaginationResponse<UserBookView> listUserBook(UserBookQuery query)`
    分页查询当前用户的错题本。
  - `public void createUserBook(UserBookRequest request)`
    创建错题本记录（加入错题本）。
  - `public UserBookView updateUserBook(UserBookRequest request)`
    更新错题本（错题练习判分核心）：作答后记录答对/答错次数、连续答对自动移出错题本， 并同步保存练习答卷的临时答案与每题得分。 【内部逻辑步骤】 1. 若 userBook.id 为空：按 templateId + 当前用户查找已有记录，命中则复用 id（幂等）； 2. 若请求带 answer（本次作答）：从模板加载题目，强制设置每题分值 5 分， 用 AnswerScoreEvaluator 判分得到 qScore： - qScore > 0（答对）：correctTimes+1；若用户配置的"连续答对移出错题数" （userInfo.correctTimes）>= 当前正确次数，则 wrongTimes 置 0（移出错题本）； - qScore = 0（答错）：wrongTimes+1，correctTimes 清零（做错一次重置连续正确数）； 3. 若带 answerId（练习答卷）：把本次答案合并进答卷 tempAnswer，并把每题得分写进 examInfo.questionScore（answerService.updateById 保存）； 4. 有 id 更新、无 id 新增错题本记录。 【为什么这么写】 - 连续答对 N 次自动移出错题：避免学生永远困在同一道错题上，答对一定次数视为已掌握； - 一次做错就清零连续正确数：错题本机制的核心是"连续做对才算掌握"。
  - `public void deleteUserBook(UserBookRequest request)`
    删除错题本记录（支持按 id / ids / templateId 三种方式，均限本人数据）。
  - `public List<RepoView> selectRepo(SelectRepoRequest request)`
    题库选择器：按模式查询本人创建或共享的题库（供出卷/抽题界面下拉选择）。
  - `public void exportRepoQuestions(RepoRequest request)`
    导出题库题目为 Excel（标准单表模板，30 列单 sheet）。
    列结构与「题目管理 → 导入模板」完全一致：学科/题型/章节/知识点/题目/选项A~H/ 难易程度/正确答案1~12/解析/标签；题型仅含判断/单选/单项填空/多选/多项填空 （Textarea 简答题随题型收窄不再导出）。

    当筛选结果为空（含未传 repoId 下载模板场景）时：第一个 sheet 只输出表头， 并附加「填写说明」sheet 展示列规则与格式示例，可直接作为导入模板使用。
     【数据流向】 RepoApi.exportRepoQuestions → exportRepoQuestions → queryQuestionsForExport（t_template） → standardRowOf 逐题装配 30 列 → fastexcel 写流 → 浏览器下载 xlsx。
  - `public void downloadImportTemplate()`
    下载题目导入模板（标准单表 30 列空模板 + 「填写说明」sheet）。
    仅输出表头与说明页，不含任何题目数据，供「题目管理 → 导入 → 下载模板」使用； 与 exportRepoQuestions（无 repoId 导出全量题目）语义区分。
  - `public RepoBindLocationView listRepoLocations(String repoId)`
    练习绑定位置反查：返回该练习绑定的章节/小节及其知识上下文。
  - `public List<NodeQuestionView> listNodeQuestions(String nodeType, String nodeId, Boolean withAnswer)`
    节点刷题内容预览（习题列表页）：与学员端 study/questions 同语义聚合题目。

### `rdbms/src/main/java/cn/wisestar/server/impl/RepoTemplateServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class RepoTemplateServiceImpl`
- 注解: @Service, @Transactional

### `rdbms/src/main/java/cn/wisestar/server/impl/ReportServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class ReportServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- 方法:
  - `public ReportData getData(String shortId)`
  - `public int compareTo(Number n1, Number n2)`

### `rdbms/src/main/java/cn/wisestar/server/impl/RewardServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class RewardServiceImpl`
- 注解: @Slf4j, @Service, @Transactional, @RequiredArgsConstructor
- **类说明**：
  学员积分·学币统一结算实现。
  【核心逻辑】校验行为 → 学期幂等判定（ref_id 统一加学期前缀）→ 单科 10000 上限裁剪 → 写行为记录 → 累加学海积分并重算头衔 → 检查每日里程碑。奖励金额只由行为类型决定，与内容配置无关。
- 方法:
  - `public StudentPreviewCompleteView settle(RewardContext context)`
  - `public int currentPoints(String userId)`

### `rdbms/src/main/java/cn/wisestar/server/impl/RoleServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class RoleServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- 方法:
  - `public List<RoleView> selectRoles(SelectRoleRequest request)`

### `rdbms/src/main/java/cn/wisestar/server/impl/SectionPassServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class SectionPassServiceImpl`
- 注解: @Slf4j, @Service, @RequiredArgsConstructor
- **类说明**：
  小节通关服务实现。
  按 (user_id, section_id) 取唯一记录：不存在则插入，存在则累加尝试次数、 刷新历史最佳（best_rate/best_stars/best_score 取 max）；passed 由 false 变 true 时 记录首次通关时间，此后保持 true。
- 方法:
  - `public SectionPassResult record(String userId, String sectionId, int rate, double score, double totalScore, int passRate, boolean unlockedNext)`

### `rdbms/src/main/java/cn/wisestar/server/impl/SectionPracticeServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class SectionPracticeServiceImpl`
- 注解: @Slf4j, @Service, @RequiredArgsConstructor
- **类说明**：
  小节练习配置服务实现。
  将 t_section.practice 的 JSON 原文反序列化为 SectionPracticeConfig； 原文为空或非法时补全缺省值并记录告警，不阻断组卷与判定。
- 方法:
  - `public SectionPracticeConfig getConfig(String sectionId)`
  - `public SectionPracticeConfig parse(String json)`

### `rdbms/src/main/java/cn/wisestar/server/impl/SectionServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class SectionServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- **类说明**：
  小节管理业务实现（知识管理板块三级维度）。
  【被谁调用】SectionApi（管理端小节管理）。

  【依赖什么】SectionMapper/KnowledgePointMapper/KnowledgePointQuestionMapper/ SectionRepoMapper/RepoMapper（BaseMapper CRUD）、 SectionViewMapper/RepoViewMapper（MapStruct 转换）。

  【数据流】SectionApi → SectionServiceImpl → SectionMapper（t_section）；列表返回时经 KnowledgePointMapper 统计各小节知识点数、经 SectionRepoMapper 统计已绑定题库数； 小节题库经 t_section_repo 关联题库管理（t_repo），全量替换式保存； 删除时级联逻辑删除其下知识点、知识点题目绑定与本小节的题库绑定。
- 方法:
  - `public List<SectionView> listSections(SectionRequest query)`
    小节列表（chapterId 可选，年级/学期可选等值过滤，sort 升序）， 并统计各小节下知识点数与已绑定题库数。
  - `public String addSection(SectionRequest request)`
    新增小节（sort 为空时自动追加到所属章节现有最大 sort 之后）。
  - `public ImportResultView importSections(SectionImportRequest request)`
    批量导入小节（Excel：学科名/章节名/小节名/年级(选填)/学期(选填)； 排序不参与导入，按所属章节自动追加；按 chapterId+name 去重）。
  - `public void updateSection(SectionRequest request)`
    更新小节（含内容设置/练习设置 JSON）。 grade/term/importance 支持清空：请求显式传空串表达清除，统一落 null（updateById 忽略 null， 故清空需在 updateById 之外显式覆盖；未传的调用方（如仅存内容/练习设置）不受影响）。
  - `public void deleteSection(SectionRequest request)`
    删除小节（级联逻辑删除其下知识点、知识点-题目绑定与本小节的题库绑定）。
  - `public void saveRepos(SectionRepoRequest request)`
    保存小节-题库绑定（全量替换：先清空旧绑定，再批量写入新绑定，事务内完成）。
  - `public List<SectionRepoView> listRepos(String sectionId)`
    查询小节已绑定的题库列表（题库管理 t_repo 数据，保持绑定顺序，附带用途标记）。

### `rdbms/src/main/java/cn/wisestar/server/impl/StudentServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class StudentServiceImpl`
- 注解: @Service, @Slf4j, @Transactional, @RequiredArgsConstructor
- **类说明**：
  学员管理业务实现（学员管理模块）。
  【被谁调用】StudentApi（管理端学员管理）。

  【依赖什么】StudentMapper（t_student CRUD）、AccountMapper（t_account 登录账号）、 StudentViewMapper（MapStruct 转换）、PasswordEncoder（初始密码 bcrypt 加密）。

  【核心逻辑】新增学员：校验姓名+联系号码组合查重 → 生成 8 位唯一学号 → 同一事务内写 t_student + t_account（user_type=Student、auth_account=学号、 初始密码 123456）。
- 方法:
  - `public StudentView createStudent(StudentRequest request)`
    新增学员：自动生成学号 + 创建登录账号（同一事务）。
  - `public StudentPermissionView permissions()`
    学员有效权限（多条有效订单合并，expire_at > NOW()）。
  - `public List<StudentSubjectView> studySubjects()`
  - `public List<ChapterView> studyChapters(String subjectId, String grade)`
  - `public List<SectionView> studySections(String chapterId)`
  - `public List<KnowledgePointView> studyPoints(String sectionId)`
  - `public StudentStudyProgressView studyProgress(String subjectId, String versionId)`
    学科学习进度：章节 → 知识点掌握度/评级/薄弱（真实评价值，不返回 mock）。
  - `public List<StudentQuestionView> studyQuestions(String sectionId, List<String> knowledgePointIds, String repoId, String questionId, Integer count, Integer perKp, Boolean groupByKp, List<String> types, String difficulty, Boolean random, Boolean exposeAnswer, String usage)`
  - `public SectionPracticeConfig sectionPracticeConfig(String sectionId)`
  - `public StudentStatsView stats()`
    学员学习统计（基于真实练习记录聚合：累计/今日/分科学币）。
  - `public void addCoin(StudentCoinRequest request)`
    老师给学员发放学币。
  - `public int coinBalance(String studentId)`
    查询指定学员学币余额（本学期各科学习币 + 手动发放/扣减合计）。
  - `public void deductCoins(String studentId, int coins, String reason)`
    扣减指定学员学币（写入负向学币流水）。
  - `public StudentPreviewCompleteView completePreview(StudentPreviewCompleteRequest request)`
    学员预习完成：标记该小节/知识点预习完成并结算奖励（同一目标仅首次发放）。
  - `public void uploadActivity(StudentActivityRequest request)`
    学员端实时位置上报（按学员覆盖，记录最后活跃时间）。
  - `public List<StudentActivityView> listActivities()`
    后台学员实时位置列表（含学员姓名/学号与习题标题）。
  - `public PaginationResponse<StudentView> pageStudents(StudentQuery query)`
    学员分页查询（姓名/学号/联系号码模糊匹配）。
  - `public void updateStudent(StudentRequest request)`
    更新学员（学号不可修改；姓名+联系号码组合查重排除自身）。
  - `public void deleteStudent(StudentRequest request)`
    删除学员（逻辑删除；校区数据权限范围内）。
  - `public StudentView me()`
    当前登录学员信息（学员端档案展示，按登录用户ID查 t_student；学员ID即 t_student.id）。
  - `public StudentProfileView profile()`
    个人中心档案。
  - `public StudentPointsView points()`
    个人中心-积分板块。
  - `public StudentCoinsView coins()`
    本学期学习币（分学科 + 手动发币合计）。
  - `public StudentTodayView today()`
    学员端主页今日总览 + 积分获取引导。
  - `public List<StudentWeakView> weakList()`
    薄弱知识点列表。
  - `public StudentKnowledgeDetailView knowledgeDetail(String knowledgePointId)`
    知识点详情（掌握度/评级/薄弱/预习状态）。
  - `public StudentPreviewCompleteView completeLearning(StudentLearningCompleteRequest request)`
    学习完成统一结算。
  - `public StudentWrongRedoView wrongRedo(StudentWrongRedoRequest request)`
    错题重做：答对则订正、移出错题本、刷新薄弱并结算奖励。
  - `public StudentWeakConquerView weakConquer(StudentWeakConquerRequest request)`
    薄弱知识点攻克。

### `rdbms/src/main/java/cn/wisestar/server/impl/StudentSubjectResolver.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class StudentSubjectResolver`
- 注解: @Component, @RequiredArgsConstructor
- **类说明**：
  学员有效学科权限解析。
  签到、任务完成等无学科语义的奖励，回退到学员有效订单权限中的首个学科， 使这些奖励同样计入该学科单学期学习币上限。
- 方法:
  - `public String firstActiveSubject(String userId)`
    取学员首个有效权限学科。

### `rdbms/src/main/java/cn/wisestar/server/impl/StudentSupervisionServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class StudentSupervisionServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  学员督学服务实现（教师端实时监督在线学员学习位置）。
  数据源为学员端活动上报表 t_student_activity（学员路由变化/做题切换时 upsert 当前位置），仅返回最近 #ONLINE_WINDOW_MINUTES 分钟内活跃的在线学员。 章节/小节归属由上报的 sectionId 反查，做题中（questionId 非空）额外回填题干、 标准答案与解析文本，供督学老师直接查看。
- 方法:
  - `public List<StudentSupervisionView> getOnlineStudents()`

### `rdbms/src/main/java/cn/wisestar/server/impl/StudentTaskServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class StudentTaskServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  学员任务服务实现
- 方法:
  - `public boolean assignTasks(String schoolId, StudentTaskDTO request)`
  - `public List<StudentTaskView> getStudentTasks(String studentId)`
  - `public int publishTasks(StudentTaskPublishDTO request)`
  - `public PaginationResponse<StudentTaskView> pageTasks(StudentTaskQuery query)`
  - `public boolean deleteTask(String id)`
  - `public List<StudentTaskView> listMyTasks()`
  - `public StudentTaskCompleteView completeTask(String taskId)`

### `rdbms/src/main/java/cn/wisestar/server/impl/StudySessionServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class StudySessionServiceImpl`
- 注解: @Slf4j, @Service, @RequiredArgsConstructor
- **类说明**：
  学习会话服务实现。
  心跳策略：取当日最近一条会话，若距上次心跳不超过 30 分钟则续用并累加时长， 否则新建会话；会话累计时长达到 60 分钟时生成或刷新当日学习总结。
- 方法:
  - `public StudyHeartbeatView heartbeat(StudyHeartbeatRequest request)`

### `rdbms/src/main/java/cn/wisestar/server/impl/StudySummaryServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class StudySummaryServiceImpl`
- 注解: @Slf4j, @Service, @RequiredArgsConstructor
- **类说明**：
  当日学习总结服务实现。
  聚合学员当日练习记录、答题明细、学习行为、掌握度与薄弱点数据，优先调用系统 AI 生成 面向家长的总结文本；AI 未启用或调用失败时降级为规则模板，`model` 标记为 rule。
- 方法:
  - `public StudySummaryView generate(String studentId, String summaryDate, String sessionId)`
  - `public StudySummaryView getMySummary()`
  - `public StudySummaryView getStudentSummary(String studentId, String date)`

### `rdbms/src/main/java/cn/wisestar/server/impl/SubjectServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class SubjectServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- **类说明**：
  学科管理业务实现（知识管理板块一级维度）。
  【被谁调用】SubjectApi（管理端学科管理）。

  【依赖什么】SubjectMapper/ChapterMapper（BaseMapper CRUD）、SubjectViewMapper（MapStruct 转换）。

  【数据流】SubjectApi → SubjectServiceImpl → SubjectMapper（t_subject）；列表返回时经 ChapterMapper 统计各学科章节数填充 chapterCount。
- 方法:
  - `public List<SubjectView> listSubjects()`
    学科列表（全量，sort 升序），并统计各学科下章节数。
  - `public String addSubject(SubjectRequest request)`
    新增学科。
  - `public void updateSubject(SubjectRequest request)`
    更新学科。
  - `public void deleteSubject(SubjectRequest request)`
    删除学科（逻辑删除；其下章节/小节/知识点不级联删除，仅学科不可见）。

### `rdbms/src/main/java/cn/wisestar/server/impl/SurveyServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class SurveyServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor, @Slf4j
- **类说明**：
  问卷/考试公开访问业务实现：问卷加载校验、公开答卷提交、公开查询、成绩查询、 答题限制（登录/密码/白名单/Cookie/IP）、关联问卷联动等。 【类职责】 面向"答卷人"（学生/外部用户）的公开入口逻辑： 1. 问卷加载：loadProject（含登录表单验证、题库练习、随机问题处理）、validateProject 校验 2. 答卷提交：saveAnswer（区分随机卷/公开查询修改/允许修改开关）、tempSaveAnswer 暂存、 答题后更新白名单状态 updateProjectPartnerByAnswer 3. 公开查询：loadQuery 查询表单、getQueryResult 查询结果（字段权限过滤/可编辑回写）、 loadExamResult 成绩查询（排名/正确答案可见性由考试设置控制）、loadLinkResult 关联问卷回填 4. 答题限制：登录限制/密码/白名单（内部/导入用户）/Cookie 限制/IP 限制/最大答题数/ 时间窗（CronHelper）校验 5. 其他：loadDict 字典加载、答案唯一性/配额校验（validateAnswer） 【被谁调用】 - Controller：SurveyController（公开访问接口）、AnswerController（间接） - 业务层：FileServiceImpl.upload（公开上传时校验项目状态） 【依赖什么】 - ProjectService/ProjectViewMapper（项目与视图）、AnswerServiceImpl（答卷读写）、 ProjectPartnerMapper（参与人/白名单）、RepoServiceImpl/UserBookServiceImpl/ TemplateServiceImpl（题库练习）、RandomSurveyProcessor（随机问题处理）、 DictItemServiceImpl（字典）、JwtTokenUtil/AuthenticationManager（答卷登录）、 ObjectMapper（JSON 序列化）、MessageSource（i18n） 【核心数据流】 答卷人访问链接 → SurveyController → loadProject（校验+加载 schema）→ 提交答案 → saveAnswer（限制校验 → AnswerServiceImpl.saveAnswer 落库）→ 考试模式计算错题入错题本 → 白名单状态更新 → 返回答卷 ID；查询侧按配置的字段权限过滤后返回。
- 方法:
  - `public PublicProjectView loadProject(ProjectQuery query)`
    加载公开问卷/考试页面数据（进入答卷页面的主入口）。 【分支逻辑】 1. repoId 非空（题库练习）：从题库加载题目组装练习卷；已有未完成答卷则回填 已答内容（examInfo 供前端判断对错），否则按练习类型（O 顺序/R 随机/W 错题） 生成题目列表并预创建一份 tempSave=0 的答卷； 2. answerId 非空（随机卷/修改答案）：直接按答卷 id 回显答案与问卷快照； 3. 其他：先做登录表单校验（convertAndValidateLoginFormIfNeeded），再校验问卷状态 （validateProject：停用/数量/时间/各类限制）； - 需要登录/密码/白名单时返回登录表单 schema（loginRequired=true）； - 否则处理随机问题（randomSurveyProcessor.processRandomSurvey）并回填最近答案 （允许修改开关开启时 getLatestAnswer）。
  - `public PublicProjectView validateProject(ProjectQuery query)`
    校验问卷并加载页面数据（登录表单验证 + 随机问题处理 + 问卷状态校验）。
  - `public PublicStatisticsView statProject(ProjectQuery query)`
    问卷答题统计（各题选项计数，供前端实时统计/配额校验）。
  - `public PublicAnswerView saveAnswer(AnswerRequest request)`
    公开提交/更新答卷（答卷人入口）。 【答案归属确定逻辑】（按优先级） 1. 随机卷 Cookie（COOKIE_RANDOM_PROJECT_PREFIX+projectId）非空 → 复用 Cookie 中答卷 id； 2. 公开查询修改（queryId 非空）→ validateAndMergeAnswer 校验可编辑字段后合并旧答案； 3. 显式传 id 且非练习项目 → 需项目开启"允许修改答案"开关（enableUpdate）否则拒绝； 4. 其他 → validateAndGetLatestAnswer：校验通过且（已登录 + 允许修改）时复用最近一次答卷。 【保存后处理】 - AnswerServiceImpl.saveAnswer 落库 + 生成答题明细； - 考试模式（非练习项目）：返回总分与每题得分，并把错题写入错题本 （userBookService.saveWrongQuestion）； - 白名单答卷：updateProjectPartnerByAnswer 更新参与人状态为已答题； - 清理随机卷 Cookie。
  - `public PublicQueryVerifyView loadQuery(PublicQueryRequest request)`
    加载公开查询验证表单（校验链接有效性后返回查询条件表单 schema）。
  - `public PublicQueryView getQueryResult(PublicQueryRequest request)`
    公开查询答卷结果。
  - `public List<PublicDictView> loadDict(PublicDictRequest request)`
    加载公开字典项（答卷页下拉选项数据）。
  - `public PublicExamResult loadExamResult(PublicExamRequest request)`
    考试结束后的成绩查询页数据。
  - `public void tempSaveAnswer(AnswerRequest request)`
    暂存答案（目前仅支持登录用户 + 随机卷，按 Cookie 中的答卷 id 更新 tempAnswer）。
  - `public PublicLinkResult loadLinkResult(PublicLinkRequest request)`
    关联问卷联动数据加载：选择某题的某个选项后，返回关联问卷中匹配该选项值的 最近答卷字段，用于自动回填。
  - `public void fillLinkFieldAndAnswer(LinkedHashMap answer, List<SurveySchema.LinkField> linkFields, LinkedHashMap<String, Map<String, Object>> fillAnswer)`
    填充关联问卷字段值到联动结果 Map（按 linkFields 配置把关联答卷中的字段值 拷贝到填充题的对应选项位置）。
  - `public void validateProject(ProjectView project)`
    按问卷设置校验项目状态与各类答题限制（提交前必查）。 【校验项】（按顺序，任一不满足即抛错） 1. 项目存在性；2. status=0 已暂停（SurveySuspend）； 3. 最大答卷数 maxAnswers（AnswerService.count 统计）； 4. 问卷结束时间 endTime；5. 登录限制 loginLimit（需开启 loginRequired）； 6. Cookie 限制 cookieLimit；7. IP 限制 ipLimit；8. 白名单限制 whitelistLimit； 9. 考试时间窗（validateExamSetting）。

### `rdbms/src/main/java/cn/wisestar/server/impl/SystemServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class SystemServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- 方法:
  - `public SystemInfo getSystemInfo()`
  - `public void updateSystemInfo(SystemInfoRequest request)`
  - `public PaginationResponse<RoleView> getRoles(RoleQuery query)`
  - `public void createRole(RoleRequest request)`
  - `public void updateRole(RoleRequest request)`
  - `public void deleteRole(RoleRequest request)`
  - `public List<PermissionView> getPermissions()`
  - `public void extractCodeDiffDbPermissions()`
  - `public SystemInfo.AiSetting getSystemAiSetting()`

### `rdbms/src/main/java/cn/wisestar/server/impl/TagServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class TagServiceImpl`
- 注解: @Service, @Transactional
- 方法:
  - `public void batchAddTag(String entityId, TagCategoryEnum category, String[] tagArr)`
  - `public void deleteTagByEntryId(String entityId)`
  - `public Set<String> selectTag(SelectTagRequest request)`

### `rdbms/src/main/java/cn/wisestar/server/impl/TaskServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class TaskServiceImpl`
- 注解: @Service, @RequiredArgsConstructor
- **类说明**：
  今日任务服务实现。
- 方法:
  - `public List<TaskView> listTasks(String taskDate, String name)`
  - `public void createTask(TaskRequest request)`
  - `public void batchCreateTasks(List<TaskRequest> requests)`
  - `public void updateTask(TaskRequest request)`
  - `public void deleteTask(TaskRequest request)`
  - `public List<StudentTaskView> studentTasks(String taskDate)`

### `rdbms/src/main/java/cn/wisestar/server/impl/TemplateServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class TemplateServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- **类说明**：
  题目模板（Template）业务实现：题目 CRUD、四维筛选（学科/章节/知识点/难度）、 分类/标签查询与错题本信息回填。 【类职责】 1. 题目 CRUD：listTemplate 分页四维筛选、addTemplate/batchAddTemplate/updateTemplate/ batchUpdateTemplate/deleteTemplate、getTemplate 详情（含错题本信息）、 selectTemplate 按题库分组选择题目、listTemplateCategories 分类列表、getTags 标签集合 2. 与题库（Repo）联动：题目挂在题库下（repoId），题库导出/批量导入委托本服务操作题目 【被谁调用】 - Controller：TemplateController（模板广场/题目管理） - 业务层：RepoServiceImpl（批量添加/删除/导出题目、随机抽题 pickQuestionFromRepo）、 SurveyServiceImpl（题库练习时加载题目列表）、RandomSurveyProcessor（间接）、 UserBookServiceImpl/错题本相关（间接） 【依赖什么】 - TemplateMapper（BaseMapper CRUD）、TemplateViewMapper（MapStruct 转换）、 RepoServiceImpl（selectTemplate 取题库列表，注意循环依赖用 ContextHelper 取 Bean）、 UserBookServiceImpl（getTemplate 回填错题本 note/correctTimes/wrongTimes） 【核心数据流】 管理端/模板广场 → TemplateController → TemplateServiceImpl → TemplateMapper（t_template） → 列表/详情/标签/分类返回；出卷时题目经 RepoServiceImpl.pickQuestionFromRepo 转 SurveySchema。
- 注入/字段: UserBookServiceImpl userBookService
- 方法:
  - `public PaginationResponse<TemplateView> listTemplate(TemplateQuery query)`
    分页查询题目模板列表（四维筛选：学科 subject / 章节 chapter / 知识点 knowledgePoint / 难度 difficulty）。 【筛选条件说明】 - name 模糊、questionType（为空时排除 Survey 普通问卷题，默认只查考试题型）、 category 分类、repoId 题库、mode 模式； - 四维筛选：subject/chapter/difficulty 精确等值；knowledgePoint 用 like （因存储为 JSON 数组字符串，只能做包含匹配）； - hasImage 图片筛选：配图存于 template JSON 的 attribute.examImages， true 时 template 文本包含 examImages，false 时取反（含 template 为空）； - tag 标签：t_tag 表 exists 子查询（IN 匹配）； - shared 权限：shared=0 只查本人；shared=null 且未指定 repoId 时也只查本人； 排序按 priority 升序（值越小越靠前）。
  - `public String addTemplate(TemplateRequest request)`
    新增单个题目模板。
  - `public void batchAddTemplate(List<TemplateRequest> templateRequests)`
    批量新增题目模板。
  - `public void batchUpdateTemplate(List<TemplateRequest> templateRequests)`
    批量更新题目模板。
  - `public void updateTemplate(TemplateRequest request)`
    更新单个题目模板。
  - `public void deleteTemplate(TemplateRequest request)`
    删除题目模板（批量逻辑删除）。
  - `public Map<String, List<TemplateView>> selectTemplate(SelectTemplateRequest request)`
    按题库分组选择题目（出卷界面按题库浏览题目）。
  - `public Set<String> listTemplateCategories(CategoryQuery query)`
    查询模板分类集合（模板广场分类导航）。
  - `public Set<String> getTags(TagQuery query)`
    查询模板标签集合（问卷题模板广场标签筛选）。
  - `public Set<String> listTemplateTags()`
    查询题目模板的全部标签（题目管理页「按标签筛选」下拉选项）。
  - `public TemplateView getTemplate(TemplateQuery query)`
    查询单个题目模板详情（含当前用户的错题本信息回填）。

### `rdbms/src/main/java/cn/wisestar/server/impl/UserBookServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class UserBookServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor
- 方法:
  - `public void saveWrongQuestion(LinkedHashMap<String, Double> questionScore)`
    保存错题

### `rdbms/src/main/java/cn/wisestar/server/impl/UserRepoServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class UserRepoServiceImpl`
- 注解: @Slf4j, @Service
- **类说明**：
  学员-题库分配服务实现
  **所属模块**：rdbms 模块 impl 包（cn.wisestar.server.impl）。

  **功能**：t_user_repo 表 CRUD（继承 BaseService/ServiceImpl）， 供 RepoServiceImpl 组装「我的题库」与分配管理使用。

### `rdbms/src/main/java/cn/wisestar/server/impl/UserServiceImpl.java`
- 包: `cn.wisestar.server.impl`
- 类型: `class UserServiceImpl`
- 注解: @Service, @Transactional, @RequiredArgsConstructor, @Slf4j
- 方法:
  - `public UserInfo loadUserByUsername(String username) throws UsernameNotFoundException`
  - `public UserInfo loadUserById(String userId)`
  - `public PaginationResponse<UserView> getUsers(UserQuery query)`
  - `public void createUser(UserRequest request)`
  - `public void updateUser(UserRequest request)`
  - `public void deleteUser(String id)`
  - `public boolean checkUsernameExist(String username)`
  - `public void updateUserPosition(UserRequest request)`
  - `public Set<String> getUserGroups(String userId)`
  - `public Set<String> getUsersByGroup(String groupId, String currentUser)`
  - `public List<UserInfo> selectUsers(SelectUserRequest request)`
  - `public void register(RegisterRequest request)`
  - `public List<RegisterRoleView> getRegisterRoles()`
  - `public UserOverview getUserOverviewData()`
  - `public void importUser(UserRequest request)`
  - `public PaginationResponse<MyTaskView> queryTask(MyTaskQuery query)`
  - `public PaginationResponse<MyTaskView> queryHistoryTask(MyTaskQuery query)`
  - `public void validateCaptcha(AuthRequest request)`

### `rdbms/src/main/java/cn/wisestar/server/mapper/AccountMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface AccountMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/AnswerDetailMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface AnswerDetailMapper`
- **类说明**：
  答题明细表（t_answer_detail）数据访问 Mapper（MyBatis-Plus）。 【类职责】 提供 AnswerDetail 实体的基础 CRUD（继承 BaseMapper 自动获得 selectList/insert/delete 等）， 供学生答题情况分析相关业务读写"一次答卷中单道题的作答记录"。 本接口没有自定义 SQL，全部走 MyBatis-Plus 通用方法，逻辑删除由 BaseModel.deleted 自动接管。 【被谁调用】 - 写入方：AnswerServiceImpl.generateAnswerDetails —— 提交答卷时先按 answerId delete 旧明细， 再逐条 insert 新明细（先删后插保证幂等） - 读取方：AnalysisServiceImpl.knowledgePointStats / studentProfile —— 按学生/学科/章节筛选 明细做知识点聚合统计 【依赖什么】 - AnswerDetail 实体（继承 BaseModel，含 createBy=学生ID、isCorrect、knowledgePoint 等分析字段） - MyBatis-Plus BaseMapper 通用能力（需要 Mapper 扫描与 MybatisPlugConfig 分页插件配合） 【核心数据流】 学生提交答卷 → AnswerServiceImpl.generateAnswerDetails 写入本表 → 学生画像/知识点统计（AnalysisServiceImpl）→ selectList(Wrappers.lambdaQuery) → SQL: SELECT * FROM t_answer_detail WHERE is_deleted=0 AND create_by=? AND subject=?... → 内存聚合统计 → KnowledgePointStat 列表返回前端。

### `rdbms/src/main/java/cn/wisestar/server/mapper/AnswerMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface AnswerMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/CampusMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface CampusMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/ChapterMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface ChapterMapper`
- **类说明**：
  Chapter Mapper（MyBatis-Plus 基础 CRUD，无需手写 SQL）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/ChapterRepoMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface ChapterRepoMapper`
- **类说明**：
  ChapterRepo Mapper（MyBatis-Plus 基础 CRUD，无需手写 SQL）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/CommDictItemMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface CommDictItemMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/mapper/CommDictMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface CommDictMapper`
- 注解: @Mapper

### `rdbms/src/main/java/cn/wisestar/server/mapper/DashboardMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface DashboardMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/DeptMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface DeptMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/EnglishAiPackMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface EnglishAiPackMapper`
- **类说明**：
  EnglishAiPack Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/EnglishGrammarMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface EnglishGrammarMapper`
- **类说明**：
  EnglishGrammar Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/EnglishLearningLogMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface EnglishLearningLogMapper`
- **类说明**：
  EnglishLearningLog Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/EnglishSentenceBookMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface EnglishSentenceBookMapper`
- **类说明**：
  EnglishSentenceBook Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/EnglishSentenceMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface EnglishSentenceMapper`
- **类说明**：
  EnglishSentence Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/EnglishUnitMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface EnglishUnitMapper`
- **类说明**：
  EnglishUnit Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/EnglishWordBookMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface EnglishWordBookMapper`
- **类说明**：
  EnglishWordBook Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/EnglishWordMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface EnglishWordMapper`
- **类说明**：
  EnglishWord Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/FileMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface FileMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/KnowledgePointMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface KnowledgePointMapper`
- **类说明**：
  KnowledgePoint Mapper（MyBatis-Plus 基础 CRUD，无需手写 SQL）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/KnowledgePointQuestionMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface KnowledgePointQuestionMapper`
- **类说明**：
  KnowledgePointQuestion Mapper（MyBatis-Plus 基础 CRUD，无需手写 SQL）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/MallGoodsMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface MallGoodsMapper`
- **类说明**：
  MallGoods Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/MallOrderMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface MallOrderMapper`
- **类说明**：
  MallOrder Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/PositionMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface PositionMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/PracticeDetailMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface PracticeDetailMapper`
- 注解: @Mapper
- **类说明**：
  练习逐题明细 Mapper。
  **所属模块**：rdbms 模块 Mapper 包（cn.wisestar.server.mapper）。

  **功能**：t_practice_detail 表 CRUD，继承 MyBatis-Plus BaseMapper； 自定义 #selectWrongQuestions 按「题目 + 学员」聚合错题，供错题库管理页查询。

  **错题语义**：is_correct = 0（含未作答）即错题；聚合口径为 t_practice_detail JOIN t_practice_record（取学员）JOIN t_user（取姓名） JOIN t_template（取题目信息）JOIN t_repo（取题库名）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/PracticeRecordMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface PracticeRecordMapper`
- 注解: @Mapper
- **类说明**：
  练习会话记录 Mapper。
  **所属模块**：rdbms 模块 Mapper 包（cn.wisestar.server.mapper）。

  **功能**：t_practice_record 表 CRUD，继承 MyBatis-Plus BaseMapper， 支持 LambdaQueryWrapper 按 userId/mode 等条件查询。

### `rdbms/src/main/java/cn/wisestar/server/mapper/ProjectMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface ProjectMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/ProjectPartnerMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface ProjectPartnerMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/RepoMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface RepoMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/RepoTemplateMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface RepoTemplateMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/RoleMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface RoleMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/SectionMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface SectionMapper`
- **类说明**：
  Section Mapper（MyBatis-Plus 基础 CRUD，无需手写 SQL）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/SectionPassMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface SectionPassMapper`
- 注解: @Mapper
- **类说明**：
  小节通关记录 Mapper。

### `rdbms/src/main/java/cn/wisestar/server/mapper/SectionRepoMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface SectionRepoMapper`
- **类说明**：
  SectionRepo Mapper（MyBatis-Plus 基础 CRUD，无需手写 SQL）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/StudentActivityMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface StudentActivityMapper`
- **类说明**：
  StudentActivity Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/StudentCoinMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface StudentCoinMapper`
- **类说明**：
  StudentCoin Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/StudentMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface StudentMapper`
- **类说明**：
  Student Mapper（MyBatis-Plus 基础 CRUD，无需手写 SQL）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/StudentOrderMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface StudentOrderMapper`
- **类说明**：
  StudentOrder Mapper（MyBatis-Plus 基础 CRUD，无需手写 SQL）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/StudentPermissionMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface StudentPermissionMapper`
- **类说明**：
  StudentPermission Mapper（MyBatis-Plus 基础 CRUD，无需手写 SQL）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/StudentRecordMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface StudentRecordMapper`
- **类说明**：
  学员学习记录 Mapper

### `rdbms/src/main/java/cn/wisestar/server/mapper/StudentTaskMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface StudentTaskMapper`
- **类说明**：
  学员任务 Mapper

### `rdbms/src/main/java/cn/wisestar/server/mapper/StudySessionMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface StudySessionMapper`
- 注解: @Mapper
- **类说明**：
  学习会话 Mapper。

### `rdbms/src/main/java/cn/wisestar/server/mapper/StudySummaryMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface StudySummaryMapper`
- 注解: @Mapper
- **类说明**：
  当日学习总结 Mapper。

### `rdbms/src/main/java/cn/wisestar/server/mapper/SubjectMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface SubjectMapper`
- **类说明**：
  Subject Mapper（MyBatis-Plus 基础 CRUD，无需手写 SQL）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/SubjectSemesterMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface SubjectSemesterMapper`
- **类说明**：
  SubjectSemester Mapper（学科学期学习币）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/SysInfoMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface SysInfoMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/TagMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface TagMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/TaskMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface TaskMapper`
- **类说明**：
  Task Mapper（MyBatis-Plus 基础 CRUD）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/TemplateMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface TemplateMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/UserBookMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface UserBookMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/UserCampusMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface UserCampusMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/UserKnowledgeProgressMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface UserKnowledgeProgressMapper`
- **类说明**：
  UserKnowledgeProgress Mapper（知识点掌握度）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/UserLearningRecordMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface UserLearningRecordMapper`
- **类说明**：
  UserLearningRecord Mapper（学习行为记录/防刷）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/UserMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface UserMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/UserPointsMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface UserPointsMapper`
- **类说明**：
  UserPoints Mapper（学海积分/头衔）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/UserPositionMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface UserPositionMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/UserRepoMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface UserRepoMapper`
- 注解: @Mapper
- **类说明**：
  学员-题库分配 Mapper
  **所属模块**：rdbms 模块 Mapper 包（cn.wisestar.server.mapper）。

  **功能**：t_user_repo 表 CRUD，继承 MyBatis-Plus BaseMapper， 支持 LambdaQueryWrapper 条件查询（按 userId/repoId/assignType）。

### `rdbms/src/main/java/cn/wisestar/server/mapper/UserRoleMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface UserRoleMapper`

### `rdbms/src/main/java/cn/wisestar/server/mapper/UserWeakKnowledgeMapper.java`
- 包: `cn.wisestar.server.mapper`
- 类型: `interface UserWeakKnowledgeMapper`
- **类说明**：
  UserWeakKnowledge Mapper（薄弱知识点研判）。

### `rdbms/src/main/java/cn/wisestar/server/service/BaseService.java`
- 包: `cn.wisestar.server.service`
- 类型: `class BaseService`
- 方法:
  - `public <E extends IPage<T>> Page<T> pageByQuery(PageQuery pageQuery)`
  - `public <E extends IPage<T>> Page pageByQuery(PageQuery pageQuery, Wrapper<T> queryWrapper)`
