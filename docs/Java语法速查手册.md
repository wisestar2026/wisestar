# Java 语法速查手册 -- 以 SurveyKing 源码为例

> 面向编程新手的 Java 关键字和注解详解。每一个概念都配有 SurveyKing 项目里的真实代码当例子。

---

## 一、类相关的关键字

### 1.1 `class` -- 定义一个类

**含义**：类就是"蓝图"或"模具"。定义了某个东西有什么属性、能做什么。

```java
// 源代码：BaseModel.java
public class BaseModel {
    private String id;       // 属性：ID
    private Date createAt;   // 属性：创建时间
}
```

**通俗理解**：`class` 就像"人类"这个模板 -- 人类都有名字、年龄这些属性，也都能吃饭、走路这些行为。而 `new BaseModel()` 就是根据模板造出一个具体的"人"。

---

### 1.2 `interface` -- 定义一个接口

**含义**：接口就是一份"合同"或"规范"，只规定必须做什么，不规定怎么做。

```java
// 源代码：ProjectService.java（接口）
public interface ProjectService {
    ProjectView addProject(ProjectRequest request);  // 只声明方法名，没有方法体
    void updateProject(ProjectRequest request);
}

// 源代码：ProjectServiceImpl.java（实现类）
public class ProjectServiceImpl implements ProjectService {
    @Override
    public ProjectView addProject(ProjectRequest request) {
        // 这里才写真正的实现代码
    }
}
```

**通俗理解**：接口就像"遥控器说明书" -- 规定了按键应该有什么功能，但具体怎么实现由各个厂商决定。Spring 依赖注入时，只看接口（说明书），不管具体是哪个实现（哪个品牌的遥控器）。

---

### 1.3 `enum` -- 枚举类型

**含义**：限定只能从预设的几个选项里选一个，不能随便写。

```java
// 源代码：SurveySchema.java
public enum QuestionType {
    Radio,     // 单选题
    Checkbox,  // 多选题
    FillBlank, // 填空题
    Score,     // 打分题
    Upload     // 上传题
}
```

**通俗理解**：就像性别的"男/女"、星期的"周一~周日"，值是固定死的，防止有人写错。在代码里用 `QuestionType.Radio` 比直接写字符串 `"Radio"` 更安全（编译期就能发现拼写错误）。

---

### 1.4 `extends` -- 继承

**含义**：子类继承父类的所有属性和方法，不用重复写。

```java
// 源代码：Project.java
public class Project extends BaseModel {
    // 自动拥有了 BaseModel 里的 id, createAt, createBy, updateAt, updateBy, deleted
    // 只需要写自己独有的字段
    private String name;
    private SurveySchema survey;
}
```

**通俗理解**：就像你继承了父母的房子 -- 你不用再买一遍，直接就能住。`Project` 继承了 `BaseModel`，就不用再写一遍 `id`、`createAt` 这些公共字段了。

---

### 1.5 `implements` -- 实现接口

**含义**：类承诺实现接口里声明的所有方法。

```java
// 源代码：ProjectServiceImpl.java
public class ProjectServiceImpl
    extends BaseService<ProjectMapper, Project>   // 继承一个类
    implements ProjectService {                    // 实现一个接口
    // 必须实现 ProjectService 接口里所有方法
}
```

**通俗理解**：签了合同（`implements`），就必须履行合同里的每一条。接口里写了 `addProject()` 方法，实现类就必须真的写出这个方法。

---

### 1.6 `@Data` -- Lombok 自动生成代码

**含义**：自动帮你写 getter、setter、toString、equals、hashCode 方法。

```java
// 源代码：ProjectRequest.java
@Data
public class ProjectRequest {
    private String id;
    private String name;
}
```

**如果没有 @Data，你需要手写**：

```java
public class ProjectRequest {
    private String id;

    public String getId() { return id; }         // 手写 getter
    public void setId(String id) { this.id = id; } // 手写 setter

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    // ... 还要写 toString, equals, hashCode，几十行代码
}
```

**通俗理解**：`@Data` 就像一个代码小助手，你写好字段，它帮你自动生成所有配套的 get/set 方法。

---

## 二、访问控制关键字

### 2.1 `public` -- 完全公开

**含义**：任何地方都能访问。

```java
// 源代码：ProjectApi.java
public class ProjectApi {           // 这个类谁都能用
    public ProjectView addProject() // 这个方法谁都能调用
}
```

**通俗理解**：就像广场上的公共厕所，谁都可以进去。

---

### 2.2 `private` -- 完全私有

**含义**：只有自己类内部能用，外部看不到。

```java
// 源代码：ProjectApi.java
public class ProjectApi {
    private final ProjectService projectService;  // 外部代码访问不到
    private final UserService userService;
}
```

**通俗理解**：就像你家里的卧室，只有你自己能进去。

---

### 2.3 `protected` -- 家族内部公开

**含义**：自己类和子类能用，同包的其他类也能用。

```java
// 子类可以访问父类的 protected 成员
public class BaseService {
    protected M baseMapper;  // 子类可以直接用
}
```

**通俗理解**：就像家族遗产，只有自己和子女能继承。

---

### 2.4 默认（不写修饰符）-- 包内公开

**含义**：没有写 public/private/protected 时，只能在同一个包内访问。

```java
class PackagePrivateClass {  // 没有修饰符 = 包级私有
    // 只有同包的其他类能看到
}
```

**通俗理解**：同一个公司的同事可以进你的工位，但外人不行。

---

### 访问权限速查表

| 修饰符 | 自己类 | 同包 | 子类 | 任何地方 |
|--------|--------|------|------|---------|
| `public` | 能 | 能 | 能 | 能 |
| `protected` | 能 | 能 | 能 | 不能 |
| 默认（不写） | 能 | 能 | 不能 | 不能 |
| `private` | 能 | 不能 | 不能 | 不能 |

---

## 三、变量和方法的修饰符

### 3.1 `static` -- 属于类，不属于对象

**含义**：静态成员不依赖于具体的对象实例，而是属于类本身。

```java
// SurveyKing 中常见用法：工具类方法
public class NanoIdUtils {
    public static String randomNanoId(int size) {  // 不用 new，直接 NanoIdUtils.randomNanoId(6)
        // ...
    }
}
```

**对比示例**：

```java
// 普通方法：必须先 new 一个对象才能用
ProjectServiceImpl service = new ProjectServiceImpl();
service.addProject(request);        // 必须通过对象调用

// 静态方法：直接用类名调用，不需要 new
NanoIdUtils.randomNanoId(6);        // 直接用类名调用
```

**通俗理解**：`static` 就像贴在教室墙上的课程表 -- 不管有没有学生进来，它都在那里。不需要先"创建学生"才能看到课程表。

---

### 3.2 `final` -- 不可改变

**含义**有三层：

**1) final 变量 = 常量，赋值后不能改**

```java
// 源代码：ProjectApi.java
private final ProjectService projectService;  // 构造器注入后不能再改

// 其他地方
public static final String TOKEN_NAME = "token";  // 常量，永远不会变
```

**2) final 方法 = 子类不能重写**

**3) final 类 = 不能被继承**

**通俗理解**：`final` 就像刻在石头上的字，写好了就不能改了。

---

### 3.3 `void` -- 没有返回值

**含义**：这个方法执行完就算了，不返回任何东西。

```java
// 源代码：ProjectApi.java -- 修改、删除方法返回 void
@PostMapping("/update")
public void updateProject(@RequestBody ProjectRequest project) {
    projectService.updateProject(project);  // 执行完就结束了，没有 return
}
```

**对比有返回值的情况**：

```java
// 创建方法有返回值，返回新创建的对象给前端
@PostMapping("/create")
public ProjectView addProject(@RequestBody ProjectRequest project) {
    return projectService.addProject(project);  // return 一个对象给前端
}
```

**通俗理解**：`void` 就像"通知型"的微信消息 -- 发出去就完了。有返回值的方法就像"问答型"消息 -- 必须回一个答案。

---

### 3.4 `return` -- 返回结果

**含义**：结束方法并返回一个值给调用者。

```java
// 源代码：ProjectServiceImpl.java
private String generateProjectId() {
    String projectId = NanoIdUtils.randomNanoId(6);
    if (Character.isDigit(projectId.charAt(0))) {
        return generateProjectId();  // 递归重试，返回新的结果
    }
    return projectId;  // 成功，返回生成的ID
}
```

**通俗理解**：就像快递员把包裹送到你手上，`return` 后面的东西就是包裹。

---

### 3.5 `this` -- 当前对象自己的引用

**含义**：指代"我自己"这个对象实例。

```java
// 代码中的用法
this.save(project);          // 调用"我自己"的 save 方法
this.projectService.xxx();   // 访问"我自己"的 projectService 字段
```

**通俗理解**：在自我介绍时说"我叫张三"，`this` 就是"我"。

---

### 3.6 `super` -- 父类的引用

**含义**：调用父类的方法或构造器。

```java
// BaseService 中
public Page<T> pageByQuery(PageQuery pageQuery) {
    return super.page(page);  // 调用父类 ServiceImpl 的 page 方法
}
```

**通俗理解**：在家里对别人说"我爸说了..."，`super` 就是"我爸"。

---

### 3.7 `new` -- 创建新对象

**含义**：根据类的蓝图创建一个具体的对象。

```java
// 源代码：ProjectServiceImpl.java
ProjectRequest request = new ProjectRequest();  // 创建一个新的请求对象
Page<Project> page = new Page<>(1, 10);          // 创建一个分页对象
```

**通俗理解**：`class` 是饼干模具，`new` 就是用模具压出一个实际的饼干。

---

## 四、注解（@xxx）详解

### 4.1 Spring 框架注解

#### `@RestController` -- 声明 HTTP 接口类

```java
// 源代码：ProjectApi.java
@RestController
@RequestMapping("${api.prefix}/project")
public class ProjectApi { ... }
```

**含义**：告诉 Spring "这个类里的方法要处理 HTTP 请求，返回 JSON 数据"。

**通俗理解**：就像给餐厅挂上"营业中"的牌子，顾客才知道这里能点菜。

---

#### `@RequestMapping` / `@GetMapping` / `@PostMapping` -- 映射 URL 路径

```java
// 源代码：ProjectApi.java
@RequestMapping("${api.prefix}/project")   // 类级别：所有方法路径都以 /api/project 开头
public class ProjectApi {

    @GetMapping("/list")                   // GET 请求：/api/project/list
    public PaginationResponse listProject() { ... }

    @PostMapping("/create")                // POST 请求：/api/project/create
    public ProjectView addProject() { ... }
}
```

**含义**：把一个 URL 路径绑定到具体的 Java 方法上。

| 注解 | HTTP 方法 | 典型用途 |
|------|-----------|---------|
| `@GetMapping` | GET | 查询数据（查问卷列表） |
| `@PostMapping` | POST | 提交数据（创建问卷） |
| `@PutMapping` | PUT | 替换数据（整体更新） |

**通俗理解**：就像医院挂号 -- 内科在 3 楼（`/api/project`），3 号窗口看专家号（`/list`），5 号窗口办住院（`/create`）。

---

#### `@Service` -- 声明 Service Bean

```java
// 源代码：ProjectServiceImpl.java
@Service
@Transactional
public class ProjectServiceImpl extends BaseService<ProjectMapper, Project>
    implements ProjectService { ... }
```

**含义**：告诉 Spring "这个类是 Service 层的 Bean，请帮我管理它的生命周期"。

**通俗理解**：`@Service` 就像把厨师登记到员工花名册上，需要时直接喊名字就能找到，不用每次都 new 一个。

---

#### `@RequiredArgsConstructor` -- 自动生成构造器

```java
// 源代码：ProjectApi.java
@RequiredArgsConstructor
public class ProjectApi {
    private final ProjectService projectService;   // final 字段
    private final UserService userService;         // final 字段
    // Lombok 自动生成：
    // public ProjectApi(ProjectService ps, UserService us) {
    //     this.projectService = ps;
    //     this.userService = us;
    // }
}
```

**通俗理解**：你把食材清单写在 `final` 字段里，`@RequiredArgsConstructor` 帮你自动生成"采购清单"（构造器），Spring 自动帮你采购（注入）。

---

#### `@Transactional` -- 数据库事务

```java
// 源代码：ProjectServiceImpl.java
@Service
@Transactional     // 类上声明：所有方法都有事务保护
public class ProjectServiceImpl { ... }
```

**含义**：这个方法里的数据库操作要么全成功，要么全失败（回滚）。不会出现"新增了问卷但没有添加参与者"这种半截情况。

**通俗理解**：就像转账 -- 你扣了钱、对方必须收到钱。如果对方没收到的异常，你的钱也会退回来。`@Transactional` 保证数据库操作也遵循这个"原子性"原则。

---

#### `@PreAuthorize` -- 权限门禁

```java
// 源代码：ProjectApi.java
@PostMapping("/create")
@PreAuthorize("hasAuthority('project:create')")  // 必须有 project:create 权限
public ProjectView addProject(@RequestBody ProjectRequest project) {
    return projectService.addProject(project);
}
```

**含义**：在执行方法之前，先检查当前登录用户有没有指定的权限。没有就返回 403 禁止访问。

**通俗理解**：就像 VIP 区的门禁 -- 刷卡显示"你有 VIP 权限"才能进去。`project:create` 就是那张 VIP 卡。

---

#### `@RequestBody` -- 自动解析 JSON

```java
@PostMapping("/create")
public ProjectView addProject(@RequestBody ProjectRequest project) {
    // Spring 自动把 HTTP 请求体里的 JSON 转成 ProjectRequest 对象
}
```

**含义**：前端发来的 JSON 字符串 `{"name":"满意度调查", "mode":"survey"}` 自动变成 `ProjectRequest` 对象，`getName()` 返回 `"满意度调查"`。

**通俗理解**：就像收到一个快递包裹（JSON），`@RequestBody` 帮你自动拆箱、分类、整理好（变成 Java 对象）。

---

#### `@EnableDataPerm` -- 数据权限

```java
@GetMapping
@EnableDataPerm(key = "#id")    // 检查当前用户是否有权访问这个 id 对应的问卷
public ProjectView getProject(String id) { ... }
```

**含义**：不仅检查用户有没有调用这个接口的权限（`@PreAuthorize`），还检查这个具体的数据是不是属于你的。

**通俗理解**：`@PreAuthorize` 是"你能进图书馆吗？"，`@EnableDataPerm` 是"这本书是你借的吗？"。

---

### 4.2 MyBatis-Plus 注解

#### `@TableName` -- 指定对应的数据库表名

```java
// 源代码：Project.java
@TableName(value = "t_project", autoResultMap = true)
public class Project extends BaseModel { ... }
```

**含义**：告诉 MyBatis-Plus "这个 Java 类对应数据库里的 `t_project` 表"。`autoResultMap=true` 表示需要用自定义的结果映射（因为 JSON 字段需要特殊处理）。

---

#### `@TableId` -- 指定主键

```java
// 源代码：BaseModel.java
@TableId(type = IdType.ASSIGN_ID)  // 雪花算法自动生成 ID
private String id;
```

**含义**：标记这是主键字段。`IdType.ASSIGN_ID` 表示插入数据时自动用雪花算法生成唯一 ID。

**雪花算法**：生成一个 64 位的长整数作为 ID，全局唯一，不会重复。比自增 ID（1, 2, 3...）更适合分布式系统。

---

#### `@TableField` -- 字段映射配置

```java
// 源代码：Project.java
@TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.LONGVARCHAR)
private SurveySchema survey;  // Java 对象 <--> 数据库 JSON 字符串

@TableField(fill = FieldFill.INSERT)
private Date createAt;         // 插入时自动填充当前时间
```

**含义**：
- `typeHandler`：指定类型转换器（`JacksonTypeHandler` 负责 Java 对象和 JSON 字符串之间的转换）
- `fill`：自动填充策略（`INSERT` 表示插入时自动填值，`UPDATE` 表示更新时自动填值）

---

#### `@TableLogic` -- 逻辑删除

```java
// 源代码：BaseModel.java
@TableLogic
@TableField(value = "is_deleted", select = false)
private Boolean deleted = false;
```

**含义**：调用 `removeById()` 时不是真删，而是把 `is_deleted` 设为 `true`。查询时自动过滤掉 `deleted=true` 的记录。

**通俗理解**：就像 Windows 的回收站 -- 删除只是扔进回收站，数据还在，可以恢复。

---

#### `@EqualsAndHashCode` -- Lombok 自动生成

```java
// 源代码：Project.java
@EqualsAndHashCode(callSuper = false)
public class Project extends BaseModel { ... }
```

**含义**：`callSuper=false` 表示在判断两个 Project 是否相等时，不考虑父类（BaseModel）的字段。

---

### 4.3 其他常见注解

#### `@Override` -- 标记重写方法

```java
// 源代码：ProjectServiceImpl.java
@Override
public ProjectView addProject(ProjectRequest request) {
    // 重写父类/接口的方法
}
```

**含义**：这是一个从父类或接口继承来的方法，我正在用自己的实现覆盖它。如果父类没有这个方法，编译器会报错（防止你拼错方法名）。

---

#### `@SneakyThrows` -- 偷偷抛出异常

```java
// 源代码：ProjectServiceImpl.java
@Override
@SneakyThrows   // 自动把受检异常包装成非受检异常
public void updateProject(ProjectRequest request) {
    synchronized (request.getId().intern()) {
        // 如果这里有受检异常，@SneakyThrows 会自动处理
    }
}
```

**含义**：Java 要求某些异常必须用 `try-catch` 或者 `throws` 声明，`@SneakyThrows` 帮你绕过这个限制。属于 Lombok 提供的便利功能。

---

## 五、参数传递注解

### `@RequestParam` vs `@RequestBody` vs `@PathVariable`

| 注解 | 数据来源 | URL 示例 | 适用场景 |
|------|---------|---------|---------|
| `@RequestParam` | URL 查询参数 | `/list?page=1&size=10` | 简单查询参数 |
| `@PathVariable` | URL 路径变量 | `/project/{id}` | RESTful 风格 |
| `@RequestBody` | HTTP 请求体 | POST 的 JSON body | 复杂对象提交 |

```java
// SurveyKing 中的实际用法

// GET 请求 + 查询参数（Spring 自动绑定到对象）
@GetMapping("/list")
public PaginationResponse listProject(ProjectQuery query) { ... }
// 前端调用：GET /api/project/list?name=满意&mode=survey

// POST 请求 + JSON 请求体
@PostMapping("/create")
public ProjectView addProject(@RequestBody ProjectRequest project) { ... }
// 前端调用：POST /api/project/create   body: {"name":"满意度","mode":"survey"}
```

---

## 六、完整的代码解读示例

把所有这些关键字放在一起，重新读 `ProjectApi.java` 的第一段：

```java
// 第1行
@RestController
// ↑ 声明：这个类处理 HTTP 请求，返回 JSON

// 第2行
@RequestMapping("${api.prefix}/project")
// ↑ 声明：所有方法的 URL 都以 /api/project 开头
//   ${api.prefix} 是配置文件里的变量，值为 /api

// 第3行
@RequiredArgsConstructor
// ↑ Lombok：自动生成构造器，注入下面所有 final 字段

// 第4行
public class ProjectApi {
// ↑ public: 谁都能访问这个类
//   class:  这是一个类（蓝图）

    // 第5行
    private final ProjectService projectService;
    // ↑ private: 只有这个类内部能访问
    //   final:   一旦赋值不能修改（构造器注入后永不变）

    // 第6行
    @GetMapping("/list")
    // ↑ GET 请求，完整 URL 是 /api/project/list

    // 第7行
    @PreAuthorize("hasAuthority('project:list')")
    // ↑ 检查当前用户有没有 project:list 权限

    // 第8行
    public PaginationResponse<ProjectView> listProject(ProjectQuery query) {
    // ↑ public:           谁都能调用
    //   PaginationResponse: 返回类型（分页结果）
    //   <ProjectView>:     泛型，表示分页里装的是 ProjectView 对象
    //   listProject:       方法名
    //   ProjectQuery query: 参数（查询条件）

        // 第9行
        return projectService.listProject(query);
        // ↑ return: 把 Service 层的结果返回给前端
        //   projectService: 上面第5行注入的 Service 对象
        //   .listProject(query): 调用它的 listProject 方法
    }
}
```

---

## 七、速查清单

| 关键字/注解 | 一句话解释 |
|-------------|-----------|
| `class` | 定义蓝图/模板 |
| `interface` | 定义合同/规范（只声明不实现） |
| `enum` | 限定选项的列表 |
| `extends` | 继承父类的东西 |
| `implements` | 履行接口的合同 |
| `public` | 谁都能访问 |
| `private` | 只能自己访问 |
| `protected` | 自己和子类能访问 |
| `static` | 属于类，不属于对象 |
| `final` | 不能改变 |
| `void` | 不返回任何东西 |
| `return` | 返回结果给调用者 |
| `new` | 造一个新对象 |
| `this` | 我自己 |
| `super` | 我的父类 |
| `@Data` | 自动生成 get/set/toString |
| `@RestController` | HTTP 接口类 |
| `@Service` | Service Bean |
| `@Transactional` | 数据库事务保护 |
| `@PreAuthorize` | 权限门禁 |
| `@RequestBody` | JSON 自动转对象 |
| `@TableName` | 对应的数据库表 |
| `@TableId` | 主键字段 |
| `@TableLogic` | 逻辑删除 |
| `@Override` | 重写父类方法 |
