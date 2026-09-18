# 项目词典 · 后端 shared（共享层：常量 / 工具 / 缓存 / 安全 / DTO / Service 接口）

> 本文件由源码清单自动生成后再人工校订，颗粒度到「每个类 + 每个公开方法」。
> 阅读方式：`归属` 段给出模块与包路径；`说明` 段取自类级 Javadoc；`方法` 段逐个列出公开/受保护方法（含 Javadoc 首句）。
>
> 归属规则：模块（Maven module）= 顶层归属；包（package）= 二级归属。
> 相关规则：`注解` 表示框架角色，`注入/字段` 表示直接依赖（协作方）。
> 本文件仅描述代码事实，未收录任何密钥；配置项中的 token 均为占位符。

---
### `shared/src/main/java/cn/wisestar/server/core/aop/DataPermAspect.java`
- 包: `cn.wisestar.server.core.aop`
- 类型: `class DataPermAspect`
- 注解: @Aspect, @Component, @Slf4j, @RequiredArgsConstructor
- **类说明**：
  数据权限校验切面（DataPermAspect）。
  **所属模块**：shared 模块 AOP 切面包（cn.wisestar.server.core.aop）。

  **类职责**：对标注了 EnableDataPerm 注解的 Controller 方法 做数据级权限的前置校验（区别于 @PreAuthorize 的功能级权限）。 用于"项目/问卷"维度的数据隔离：当前用户必须拥有访问指定问卷的权限， 否则抛出异常阻止请求继续执行。

  **工作机制**：

  - 通过切点表达式 `@annotation(dataPerm)` 匹配所有标注
- 方法:
  - `public void checkPermissionPointCut(EnableDataPerm dataPerm)`
    切点定义：匹配所有标注了 @EnableDataPerm 的方法。
  - `public void around(JoinPoint joinPoint, EnableDataPerm dataPerm) throws Throwable`
    前置通知：方法执行前校验数据权限。
    **执行步骤**：

    - 解析方法参数名与参数值，构建 SpEL 求值上下文（参数名作为变量）；
    - 求值注解 key 表达式得到目标资源标识（如问卷 id），空值直接拒绝 （"未找到对应的问卷"）；
    - 获取当前用户可见的项目权限集合，标识不在集合内则拒绝 （"没有权限访问本问卷"）。

### `shared/src/main/java/cn/wisestar/server/core/base/converter/PublicQueryConverter.java`
- 包: `cn.wisestar.server.core.base.converter`
- 类型: `class PublicQueryConverter`
- **类说明**：
  公共查询参数转换器（PublicQueryConverter）。
  **所属模块**：shared 模块基础框架转换器包（cn.wisestar.server.core.base.converter）。

  **类职责**：把 Spring 参数绑定产生的 LinkedHashMap（JSON 对象/ 查询参数映射）转换为 ProjectSetting.PublicQuery 对象。 在 cn.wisestar.server.core.config.AppConfig#initAfterStartup() 中 注册到全局 DefaultConversionService，供"问卷公共查询设置"等复杂参数的 自动类型转换使用。

  **使用场景**：前端提交 JSON 中包含对象类型的字段（如问卷的公共查询 条件），Spring 无法直接实例化目标类型时，会调用本转换器完成 map → 对象的映射。
- 方法:
  - `public ProjectSetting.PublicQuery convert(LinkedHashMap<String, ?> value)`
    执行转换：把 Map 属性值映射到 PublicQuery 的对应字段。

### `shared/src/main/java/cn/wisestar/server/core/base/converter/RandomSurveyConverter.java`
- 包: `cn.wisestar.server.core.base.converter`
- 类型: `class RandomSurveyConverter`
- **类说明**：
  随机问卷条件转换器（RandomSurveyConverter）。
  **所属模块**：shared 模块基础框架转换器包（cn.wisestar.server.core.base.converter）。

  **类职责**：把 Spring 参数绑定产生的 LinkedHashMap（JSON 对象映射） 转换为 ProjectSetting.RandomSurveyCondition 对象。 在 cn.wisestar.server.core.config.AppConfig#initAfterStartup() 中 注册到全局 DefaultConversionService。

  **特殊处理**：随机问卷条件中的 examScore（分值）字段，前端 JSON 可能 传整数（Integer）或小数（Double），本转换器先把 Integer 统一转为 Double， 再执行 map → bean 映射，避免类型不匹配导致的转换失败。
- 方法:
  - `public ProjectSetting.RandomSurveyCondition convert(LinkedHashMap<String, Object> value)`
    执行转换：修正 examScore 数值类型后映射为 RandomSurveyCondition 对象。

### `shared/src/main/java/cn/wisestar/server/core/base/converter/UniqueLimitSettingConverter.java`
- 包: `cn.wisestar.server.core.base.converter`
- 类型: `class UniqueLimitSettingConverter`
- **类说明**：
  唯一性/提交次数限制设置转换器（UniqueLimitSettingConverter）。
  **所属模块**：shared 模块基础框架转换器包（cn.wisestar.server.core.base.converter）。

  **类职责**：把 Spring 参数绑定产生的 LinkedHashMap 转换为 ProjectSetting.UniqueLimitSetting 对象。在 cn.wisestar.server.core.config.AppConfig#initAfterStartup() 中注册到 全局 DefaultConversionService。

  **转换细节**：从 Map 中取出 limitNum（次数上限，Integer）与 limitFreq（频率单位，String），把 limitFreq 通过 AnswerFreqEnum#valueOf 转为枚举后构造 UniqueLimitSetting 对象。若 limitFreq 传入的字符串不在 AnswerFreqEnum 枚举定义中，会抛出 IllegalArgumentException（由参数校验层兜底）。
- 方法:
  - `public ProjectSetting.UniqueLimitSetting convert(LinkedHashMap<?, ?> value)`
    执行转换：解析限制次数与频率枚举。

### `shared/src/main/java/cn/wisestar/server/core/base/mapper/BaseModelMapper.java`
- 包: `cn.wisestar.server.core.base.mapper`
- 类型: `interface BaseModelMapper`
- **类说明**：
  Model对象到Domain类型对象的相互转换。实现类通常声明在Model实体类中。
  **所属模块**：shared 模块基础框架（cn.wisestar.server.core.base.mapper）。

  **类职责**：定义"请求 DTO（R）→ 数据库实体（M）→ 视图 DTO（V）" 三层对象转换的标准接口，并基于 Jackson 提供 "bean ↔ map" 的默认转换工具方法。各模块在 Model 实体类中通过 MapStruct 生成该接口的实现（配合 mapToMap 优化 Map 字段的拷贝）。

  **数据流**：前端请求（Request DTO）→ fromRequest → Model 实体 → 落库； 数据库查询（Model 实体）→ toView → View DTO → 响应前端。

### `shared/src/main/java/cn/wisestar/server/core/cache/DeptKeyGenerator.java`
- 包: `cn.wisestar.server.core.cache`
- 类型: `class DeptKeyGenerator`
- 注解: @Component
- **类说明**：
  部门缓存 Key 生成器（DeptKeyGenerator）。
  **所属模块**：shared 模块缓存包（cn.wisestar.server.core.cache）。

  **类职责**：实现 Spring 缓存抽象 KeyGenerator，为部门相关的
- 方法:
  - `public Object generate(Object target, Method method, Object... params)`
    根据方法参数生成缓存 Key。

### `shared/src/main/java/cn/wisestar/server/core/common/ApiResponse.java`
- 包: `cn.wisestar.server.core.common`
- 类型: `class ApiResponse`
- 注解: @Data
- **类说明**：
  统一 API 响应包装对象（ApiResponse）。
  **所属模块**：shared 模块核心通用类（cn.wisestar.server.core.common）。

  **类职责**：系统所有 HTTP 接口的统一响应体结构，包含业务状态码 code、 提示信息 message 与业务数据 data 三要素。前端据此统一处理成功/失败分支。

  **响应约定**：

  - code：业务状态码（见 cn.wisestar.server.core.constant.ResponseCode， 0 表示成功，非 0 表示各类业务失败，异常场景见 ErrorCode 体系）；
  - message：可读提示信息（失败时展示给用户）；
  - data：业务数据负载（成功时携带）。

  **谁在生成**：

  - 成功场景：cn.wisestar.server.core.mvc.advice.CustomResponseBodyAdvice （ResponseBodyAdvice）自动把 Controller 返回值包装为 `ApiResponse(SUCCESS.code, data)`；
  - 失败场景：cn.wisestar.server.core.mvc.advice.GlobalExceptionHandler 与 cn.wisestar.server.core.security.RestAuthenticationEntryPoint 统一构造。

  **数据流**：Controller 方法返回值 → CustomResponseBodyAdvice 包装 → Jackson 序列化为 JSON → 前端读取 code/data 分支处理。
- 方法:
  - `public ApiResponse()`
    空构造器（序列化 / 反序列化需要）。
  - `public ApiResponse(int code, T data)`
    构造"仅含状态码 + 数据"的响应（如成功响应，message 为空）。
  - `public ApiResponse(int code, String message)`
    构造"仅含状态码 + 提示信息"的响应（如失败响应，data 为空）。
  - `public String toString()`
    以 JSON 字符串形式输出响应内容（便于日志打印排查）。

### `shared/src/main/java/cn/wisestar/server/core/common/PaginationResponse.java`
- 包: `cn.wisestar.server.core.common`
- 类型: `class PaginationResponse`
- 注解: @Data
- **类说明**：
  分页响应对象（PaginationResponse）。
  **所属模块**：shared 模块核心通用类（cn.wisestar.server.core.common）。

  **类职责**：所有分页查询接口的统一返回结构，包含总数 total、 当前页数据列表 list、当前页码 current 与每页大小 pageSize。

  **数据流**：Service 层分页查询（MyBatis-Plus Page）→ 组装本对象 → Controller 返回 → CustomResponseBodyAdvice 包装为 ApiResponse → JSON 给前端（前端根据 total 渲染分页器）。

  **使用示例**：cn.wisestar.server.service.TemplateService#listTemplate、 用户/角色/部门等所有分页接口均返回本类型。
- 方法:
  - `public PaginationResponse(Long total, List<T> list)`
    构造分页响应（total + list；current / pageSize 需由调用方另行 set，或保持为空）。

### `shared/src/main/java/cn/wisestar/server/core/common/Tuple2.java`
- 包: `cn.wisestar.server.core.common`
- 类型: `class Tuple2`
- **类说明**：
  二元组对象（Tuple2）。
  **所属模块**：shared 模块核心通用类（cn.wisestar.server.core.common）。

  **类职责**：简单的不可变二元组（Pair），用于"一个方法需要同时返回两个值" 的场景，避免为临时结果单独定义 DTO 或使用 Object[] 数组。

  **使用场景**：如 Service/工具方法需要返回"对象 + 状态"、"结果 + 计数"等 成对数据时使用。字段通过 final 保证不可变，构造后不可修改。
- 方法:
  - `public Tuple2(T1 first, T2 second)`
    构造二元组。
  - `public T1 getFirst()`
    获取第一个值。
  - `public T2 getSecond()`
    获取第二个值。

### `shared/src/main/java/cn/wisestar/server/core/config/AppConfig.java`
- 包: `cn.wisestar.server.core.config`
- 类型: `class AppConfig`
- 注解: @Configuration, @EnableAspectJAutoProxy, @EnableAsync
- **类说明**：
  应用级配置（AppConfig）。
  **所属模块**：shared 模块核心框架配置包（cn.wisestar.server.core.config）。

  **类职责**：集中配置应用运行期的三类能力：

  - **AOP 代理**（@EnableAspectJAutoProxy）：开启基于注解的 AOP （如 cn.wisestar.server.core.aop.DataPermAspect 数据权限切面）；
  - **异步支持**（@EnableAsync + AsyncConfigurer）：提供全局线程池 （核心 4、最大 8、前缀 "MyExecutor-"），供 @Async 方法使用 （如消息通知、异步统计等）；
  - **全局类型转换器注册**：应用启动完成（ApplicationReadyEvent）后， 向 Spring 默认转换服务注册三个自定义 Converter： UniqueLimitSettingConverter、PublicQueryConverter、 RandomSurveyConverter，使 URL 参数能自动绑定为对应的复杂类型。

  **为什么在 ApplicationReadyEvent 中注册转换器**：需要保证在 Spring 启动 完成后、请求进入前完成注册；使用共享 DefaultConversionService 实例， 使所有使用该转换服务的场景（如 @RequestParam 绑定）都能生效。
- 方法:
  - `public void setUserService(UserService userService)`
  - `public void initAfterStartup()`
    应用启动完成后的初始化操作。
    向共享 DefaultConversionService 注册三个自定义类型转换器，用于 HTTP 参数到复杂对象的自动转换：

    - UniqueLimitSettingConverter：字符串 → 唯一性/次数限制设置对象；
    - PublicQueryConverter：查询参数 → 公共查询对象；
    - RandomSurveyConverter：字符串 → 随机问卷条件对象。

    注：userService.init() 初始化逻辑已被注释停用。
  - `public Executor getAsyncExecutor()`
    全局异步任务线程池。
    核心线程数 4、最大线程数 8、线程名前缀 "MyExecutor-"， 供所有 @Async 注解方法提交任务使用（队列默认无界）。

### `shared/src/main/java/cn/wisestar/server/core/config/CacheConfig.java`
- 包: `cn.wisestar.server.core.config`
- 类型: `class CacheConfig`
- 注解: @Configuration, @EnableCaching, @ConfigurationProperties
- **类说明**：
  缓存配置（CacheConfig）。
  **所属模块**：shared 模块核心框架配置包（cn.wisestar.server.core.config）。

  **类职责**：启用 Spring 缓存抽象（@EnableCaching），并基于 Caffeine 本地缓存自定义 CacheManager，核心能力是： **允许针对每个 cacheName 单独配置过期时间**（区别于 Spring 默认的 ConcurrentMapCacheManager 全局统一 TTL）。

  **工作机制**：

  - 通过 `@ConfigurationProperties("custom-cache")` 从配置文件读取 `custom-cache.entries` 映射（cacheName → 过期时长 Duration）；
  - 创建 Caffeine 缓存：每个缓存条目的 TTL 由对应 cacheName 配置决定 （expireAfterCreate 读取配置，expireAfterUpdate/expireAfterRead 保持原时长）， 单缓存最大容量 100 条；
  - 预注册所有配置的 cacheName，业务代码通过 @Cacheable / @CacheEvict 等注解 使用缓存时按名字命中对应 Caffeine 缓存。

  **使用示例**（application.yml）：
  ```
   custom-cache: entries: user: 30m # user 缓存 30 分钟 deptTree: 1h # deptTree 缓存 1 小时
  ```

  **业务用途**：缓存用户信息、部门树、字典等读取频繁、变更低频的数据， 降低数据库压力；配合 cn.wisestar.server.core.cache.DeptKeyGenerator 等自定义 key 生成器使用。
- 方法:
  - `public CacheManager cacheManager()`
    创建自定义 CacheManager（覆盖父类默认实现）。
    内部采用匿名子类方式扩展 ConcurrentMapCacheManager：

    - 每个 cacheName 构建一个 ConcurrentMapCache，底层存储使用 Caffeine 的 asMap()（具备 TTL 过期能力）；
    - 过期策略：创建时按 entries 配置设置 TTL；更新/读取时沿用当前剩余时长 （不自动续期）；
    - maximumSize(100)：单缓存最多缓存 100 个键值对，防止内存无限增长。
  - `protected Cache createConcurrentMapCache(final String name)`
  - `public long expireAfterCreate(Object key, Object value, long currentTime)`
  - `public long expireAfterUpdate(Object key, Object value, long currentTime, @NonNegative long currentDuration)`
  - `public long expireAfterRead(Object key, Object value, long currentTime, @NonNegative long currentDuration)`
  - `public Map<String, Duration> getEntries()`
    获取缓存配置映射。
  - `public void setEntries(Map<String, Duration> entries)`
    设置缓存配置映射（由 Spring Boot ConfigurationProperties 绑定调用）。

### `shared/src/main/java/cn/wisestar/server/core/config/WebConfig.java`
- 包: `cn.wisestar.server.core.config`
- 类型: `class WebConfig`
- 注解: @Configuration, @RequiredArgsConstructor, @RestController
- **类说明**：
  Web MVC 配置（WebConfig）。
  **所属模块**：shared 模块核心框架配置包（cn.wisestar.server.core.config）。

  **类职责**：实现 WebMvcConfigurer，对 Spring MVC 做全局增强， 同时本身也是一个 @RestController，负责单 jar 部署时根路径首页的返回：

  - **消息转换器**：#configureMessageConverters 在转换器列表首位 加入基于共享 ObjectMapper 的 MappingJackson2HttpMessageConverter， 使 Controller 可以直接返回 String 类型（避免默认 StringHttpMessageConverter 与 JSON 转换器顺序问题）；
  - **静态资源映射**：#addResourceHandlers 把 css/js/图片/字体等 静态资源映射到 classpath:/static/ 目录，并设置优先级高于根路径 @GetMapping、 缓存周期一天（浏览器缓存 86400 秒）；
  - **根路径首页**：#index() 返回 classpath:/static/index.html， 保证访问根路径时加载前端 SPA 页面。

  **为什么静态资源优先级设为 -1**：registry.setOrder(-1) 使 ResourceHandler 的优先级高于 @GetMapping 根路径映射，这样以 .js/.css 等结尾的请求先被静态资源 处理器接管，不会落入 index() 兜底逻辑。
- 注入/字段: Resource indexHtml
- 方法:
  - `public void configureMessageConverters(List<HttpMessageConverter<?>> converters)`
    配置 HTTP 消息转换器列表。
    在转换器列表首位插入基于共享 ObjectMapper 的 JSON 转换器， 解决"Controller 直接返回 String 时被默认转换器处理导致乱码/类型错误" 的问题，并保证所有 JSON 序列化使用统一的 ObjectMapper 配置。
  - `public void addResourceHandlers(ResourceHandlerRegistry registry)`
    注册静态资源处理器。
    把 #STATIC_RESOURCES 匹配的静态资源映射到 classpath:/static/ 目录；优先级设为 -1（高于根路径 @GetMapping），缓存周期 86400 秒（1 天）。
  - `public Object index()`
    根路径首页兜底：返回前端 SPA 的 index.html。
    单 jar 部署时访问 `/` 返回静态首页；未匹配任何静态资源与 Controller 的路径由框架按此规则兜底。

### `shared/src/main/java/cn/wisestar/server/core/config/WebSecurityConfig.java`
- 包: `cn.wisestar.server.core.config`
- 类型: `class WebSecurityConfig`
- 注解: @EnableWebSecurity, @EnableGlobalMethodSecurity, @ConfigurationProperties
- **类说明**：
  Spring Security 核心配置（WebSecurityConfig）。
  **所属模块**：shared 模块核心框架配置包（cn.wisestar.server.core.config）。

  **类职责**：基于 Spring Security 的 WebSecurityConfigurerAdapter， 定义整个后端的安全策略：

  - **启用全局方法级安全**（@EnableGlobalMethodSecurity）： Controller 上的 @Secured / @PreAuthorize 注解生效，授权控制粒度到方法；
  - **无状态会话**（STATELESS）：不创建 HttpSession，认证状态完全依赖 JWT；
  - **关闭 CSRF**、开启 CORS（自定义 CorsFilter，允许任意来源携带凭据）；
  - **URL 级访问规则**：公开接口（/api/public/**、/api/system、/captcha/get、 /captcha/check、GET /api/file/**、/）permitAll，其余 /api/** 要求认证， 但实际授权主要靠方法级注解，URL 规则只做兜底；
  - **注册 JWT 过滤器**：把 JwtTokenFilter 插入到 UsernamePasswordAuthenticationFilter 之前，实现无状态 JWT 认证；
  - **认证失败处理**：使用 RestAuthenticationEntryPoint 输出 JSON；
  - **支持 URL 参数携带令牌**（urlTokenAuthentication，默认开启）， 用于文件下载等无法携带 Cookie 的场景。

  **外部化配置**：本类同时是 `@ConfigurationProperties("sk.security")`， 可通过配置文件前缀 `sk.security` 绑定属性（如 `sk.security.url-token-authentication.enabled` 控制 URL 参数令牌认证开关）。

  **认证机制**：#configure(AuthenticationManagerBuilder) 指定使用 UserService#loadUserByUsername 加载用户（配合 PasswordEncoder 做 BCrypt 密码比对）。
- 方法:
  - `public WebSecurityConfig(JwtTokenFilter jwtTokenFilter, UserService userService, RestAuthenticationEntryPoint authenticationEntryPoint)`
  - `protected void configure(AuthenticationManagerBuilder auth) throws Exception`
    配置认证管理器：指定用户加载方式与密码编码器。
    通过 `userDetailsService` 注册 UserService.loadUserByUsername， 配合全局 PasswordEncoder（BCrypt）完成用户名 + 密码的认证。 预留了 `auth.authenticationProvider()` 扩展点，可追加更多认证方式。
  - `public void configure(HttpSecurity http) throws Exception`
    配置 HTTP 级安全规则（过滤器链）。
    **规则明细**：

    - 禁用 X-Frame-Options（允许 iframe 嵌入，如问卷预览页面被嵌入）；
    - 开启 CORS、关闭 CSRF；
    - 无状态会话（STATELESS）；
    - 认证失败走 RestAuthenticationEntryPoint；
    - URL 匹配规则（见类注释），所有请求默认放行 + 注解授权，保证单 jar 部署时 任意路由都能命中前端静态页面；
    - 在 UsernamePasswordAuthenticationFilter 之前插入 JwtTokenFilter。
  - `public CorsFilter corsFilter()`
    全局 CORS 过滤器 Bean。
    允许任意来源（Origin）、任意请求头、任意方法，并支持携带凭据（Cookie）， 满足前后端分离部署与本地开发调试场景。
  - `public AuthenticationManager authenticationManagerBean() throws Exception`
  - `public UrlTokenAuthentication getUrlTokenAuthentication()`
    获取 URL 参数令牌认证配置（供 JwtTokenFilter 读取开关状态）。
  - `public boolean isEnabled()`
  - `public void setEnabled(boolean enabled)`

### `shared/src/main/java/cn/wisestar/server/core/constant/AnswerFreqEnum.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `enum AnswerFreqEnum`
- **类说明**：
  答题频率枚举（AnswerFreqEnum）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义"答卷人同一问卷的提交频率限制"的单位枚举， 每个枚举值同时携带对应的 Quartz Cron 表达式，用于"限制周期结束" 定时任务的调度。与 cn.wisestar.server.domain.dto.ProjectSetting.UniqueLimitSetting 配合使用（该对象由 cn.wisestar.server.core.base.converter.UniqueLimitSettingConverter 将 limitFreq 字符串转换为本枚举）。

  **取值说明**：only（仅一次）、hour（每小时）、day（每天）、week（每周）、 month（每月）、quarter（每季度）、year（每年）。
- 方法:
  - `public String getCron()`
    获取 Cron 表达式。

### `shared/src/main/java/cn/wisestar/server/core/constant/AppConsts.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `class AppConsts`
- **类说明**：
  应用全局常量定义（AppConsts）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：集中管理系统内广泛使用的常量：认证令牌名、Cookie 名、 文件类型、项目状态、字典编码、权限类型、用户状态、数据权限类型、 仪表盘类型等，避免魔法值散落各业务代码。

  **使用说明**：内部按业务语义组织为多个内部接口/枚举：

  - FileType：存储文件类型（与 StorageTypeEnum 数值一致）；
  - DICTCODE_PERMISSION_TYPE：字典编码-权限类型；
  - RESOURCE_PERMISSION_DISPLAY_TYPE：前端权限展示类型；
  - AUTH_TYPE / USER_TYPE / USER_STATUS：认证与用户相关；
  - DataPermissionTypeEnum：岗位数据权限范围；
  - DispositionTypeEnum：文件下载方式（预览/附件）；
  - DashboardType：仪表盘类型；
  - PermType：数据权限类型（默认 project）；
  - ProjectPartnerStatus：项目参与者状态。
- 内部类型: interface FileType, enum DICTCODE_PERMISSION_TYPE, enum RESOURCE_PERMISSION_DISPLAY_TYPE, enum AUTH_TYPE, enum USER_TYPE, interface USER_STATUS, enum DataPermissionTypeEnum, enum DispositionTypeEnum, interface DashboardType, interface PermType, interface ProjectPartnerStatus

### `shared/src/main/java/cn/wisestar/server/core/constant/AttachmentNameVariableEnum.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `enum AttachmentNameVariableEnum`
- **类说明**：
  附件命名变量枚举（AttachmentNameVariableEnum）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义附件（上传文件）自定义命名规则中可用的变量名。 用户在配置上传文件命名模板时，可以引用这些变量占位符，系统在保存文件时 会将占位符替换为实际值（见 rdbms 模块附件命名逻辑）。

  **可用变量**：

  - projectId：项目 id
  - serialNum：全局附件序号（所有附件从 1 开始编号）
  - serialNumInAnswer：同一问卷内附件序号（从 1 开始）
  - uploadDate / uploadDateTime：上传日期 / 日期时间
  - sourceName：原始文件名
  - questionTitle：问题标题

### `shared/src/main/java/cn/wisestar/server/core/constant/CacheConsts.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `class CacheConsts`
- **类说明**：
  缓存名称常量（CacheConsts）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：集中定义 Spring Cache 抽象中使用的所有 cacheName 常量。 业务代码通过 @Cacheable(cacheNames = CacheConsts.xxx) 引用， 避免魔法字符串散落各处。每个 cacheName 对应的过期时长在配置文件 `custom-cache.entries` 中配置（见 cn.wisestar.server.core.config.CacheConfig）。

  **注意**：cacheName 必须与 application.yml 中 custom-cache.entries 的 key 保持一致，否则 CacheConfig 创建缓存时取不到 TTL 配置会抛空指针。

### `shared/src/main/java/cn/wisestar/server/core/constant/ErrorCode.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `enum ErrorCode`
- **类说明**：
  业务错误码枚举（ErrorCode）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义系统全部业务错误码（code）与默认错误消息（message）。 业务代码通过抛出 cn.wisestar.server.core.exception.ErrorCodeException 携带本枚举，由 cn.wisestar.server.core.mvc.advice.GlobalExceptionHandler#handleErrorCodeException 捕获后把 code/message 透传给前端，前端按 code 做国际化或分支提示。

  **编码规则**：前两位区分模块（10xx 账号、14xx 注册、40xx 问卷回收/ 文件/关联、60xx 考试、70xx 公开查询），后两位区分具体错误消息。

### `shared/src/main/java/cn/wisestar/server/core/constant/ExamExerciseTypeEnum.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `enum ExamExerciseTypeEnum`
- **类说明**：
  考试练习类型枚举（ExamExerciseTypeEnum）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义考试模式下"练习"的类型标识，用于区分练习题的出题方式。 与 AI 自习室系统的练习场景（随机抽题练习）配合使用。

  **取值说明**：R = Random（随机练习）、O = Order（顺序练习）、 W = Wrong（错题练习）。

### `shared/src/main/java/cn/wisestar/server/core/constant/FieldPermissionType.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `class FieldPermissionType`
- **类说明**：
  字段权限类型常量（FieldPermissionType）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义"问卷/考试中的字段（问题）"对特定用户角色的可见/可编辑 权限级别。用于项目参与者（答卷人/协作者）的字段级权限控制： 不同角色对同一问卷字段拥有不同的展示与编辑能力。

  **取值说明**：hidden=0（默认隐藏）、visible=1（仅可见，不可编辑）、 editable=2（可编辑）。

### `shared/src/main/java/cn/wisestar/server/core/constant/LocalStorageNameStrategyEnum.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `enum LocalStorageNameStrategyEnum`
- **类说明**：
  本地存储文件名策略枚举（LocalStorageNameStrategyEnum）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义本地存储（LocalStorageService）保存文件时的 文件名生成策略，供 cn.wisestar.server.storage.StorageProperties 中 name-strategy 配置项引用（如 `storage.name-strategy: uuid`）。

  **取值说明**：

  - seqAndOriginalName：序列号 + 原文件名（1653122982531_fileName.jpg）；
  - originalNameAndSeq：原文件名 + 序列号（fileName_1653122982531.jpg）；
  - seq：仅序列号（1653122982531.jpg）；
  - uuid：无短杠 UUID（8328839eae07f93443733bc7b0468f04.jpg）。
- 方法:
  - `public String getStrategy()`
    获取策略标识。

### `shared/src/main/java/cn/wisestar/server/core/constant/LocalStoragePathStrategyEnum.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `enum LocalStoragePathStrategyEnum`
- **类说明**：
  本地存储路径策略枚举（LocalStoragePathStrategyEnum）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义本地存储（LocalStorageService）保存文件时的 目录组织策略，供 cn.wisestar.server.storage.StorageProperties 中 path-strategy 配置项引用（如 `storage.path-strategy: byId`）。

  **取值说明**：

  - byNo：所有文件直接存 rootPath 下；
  - byId：按项目的 short-id 分文件夹（rootPath/RyP2rR）；
  - byDate：按上传日期分文件夹（rootPath/2022/06/01）。
- 方法:
  - `public String getStrategy()`
    获取策略标识。

### `shared/src/main/java/cn/wisestar/server/core/constant/PermissionConsts.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `class PermissionConsts`
- **类说明**：
  后台系统权限点清单（权限树数据源）。
  角色权限管理（人事管理）模块使用：角色编辑页的权限树按功能模块分组展示 权限点，勾选结果以权限编码列表保存到 `t_role.authority`（逗号分隔）， 后端接口通过 `@PreAuthorize("hasAuthority('module:action')")` 拦截。

  本类集中定义后台全部功能模块的权限点，是权限树、内置角色默认权限、 管理员全量权限的唯一权威清单。
- 方法:
  - `public Node()`
  - `public Node(String key, String name)`
  - `public String getKey()`
  - `public void setKey(String key)`
  - `public String getName()`
  - `public void setName(String name)`
  - `public List<Node> getChildren()`
  - `public void setChildren(List<Node> children)`
  - `public static List<Node> tree()`
    返回权限树（模块 → 操作点）。
  - `public static boolean isBuiltin(String code)`
    是否为内置角色编码。

### `shared/src/main/java/cn/wisestar/server/core/constant/ProjectModeEnum.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `enum ProjectModeEnum`
- **类说明**：
  项目场景模式枚举（ProjectModeEnum）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义项目（Project）/模板（Template）的场景模式， 决定项目的使用方式与功能集合。项目与模板视图/请求 DTO 中均通过 本枚举标识模式。

  **取值说明**：

  - survey：问卷模式（收集数据）；
  - exam：考试模式（自动判分）；
  - folder：文件夹模式（用于组织项目层级，非可答题项目）。

### `shared/src/main/java/cn/wisestar/server/core/constant/ProjectPartnerTypeEnum.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `enum ProjectPartnerTypeEnum`
- **类说明**：
  项目参与者类型枚举（ProjectPartnerTypeEnum）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义"项目参与者"的角色类型（对应项目 - 用户关联关系 t_project_partner 表）。不同类型的参与者拥有不同的项目操作权限 （所有者可管理、协作者可协作、答卷人仅可答题）。

  **取值说明**：OWNER=1（所有者）、COLLABORATOR=2（协作者）、 RESPONDENT_SYS_USER=3（系统用户身份的答卷人）、 RESPONDENT_IMP_USER=4（外部导入的答卷人）。
- 方法:
  - `public int getType()`
    获取类型数值。

### `shared/src/main/java/cn/wisestar/server/core/constant/ReportStatKeyEnum.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `enum ReportStatKeyEnum`
- **类说明**：
  报表统计 Key 枚举（ReportStatKeyEnum）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义项目报表（数据看板）中使用的统计指标 Key， 作为统计数据聚合结果的标识（如日报表按天统计的数量）。 目前仅定义"每日数量"指标，后续统计维度在此扩展。

### `shared/src/main/java/cn/wisestar/server/core/constant/ResponseCode.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `enum ResponseCode`
- **类说明**：
  通用响应状态码枚举（ResponseCode）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义系统统一响应体 cn.wisestar.server.core.common.ApiResponse 中 code 字段的标准取值（HTTP 语义对齐），供成功响应包装 （cn.wisestar.server.core.mvc.advice.CustomResponseBodyAdvice）与 异常响应构造（cn.wisestar.server.core.mvc.advice.GlobalExceptionHandler、 cn.wisestar.server.core.security.RestAuthenticationEntryPoint）使用。

  **注意**：虽然 HTTP 状态码固定为 200，但业务 code 采用标准 HTTP 语义编码，前端通过 code 值区分成功（200）与各类失败。

### `shared/src/main/java/cn/wisestar/server/core/constant/SectionRepoUsage.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `class SectionRepoUsage`
- **类说明**：
  小节-题库绑定用途常量（t_section_repo.usage_type）。
  用途决定绑定题库服务的学员端场景： 预习（#PREVIEW）只取预习专用与通用题库； 专项练习与小节通关（#PRACTICE）只取练习专用与通用题库。 历史数据缺省按通用处理。
- 方法:
  - `public static String normalize(String usage)`
    归一化用途：空值或非法值统一回退为通用。
  - `public static boolean availableForPreview(String usage)`
    判断用途是否属于预习链路可用范围（预习专用或通用）。
  - `public static boolean availableForPractice(String usage)`
    判断用途是否属于练习链路可用范围（练习专用或通用）。

### `shared/src/main/java/cn/wisestar/server/core/constant/StorageTypeEnum.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `enum StorageTypeEnum`
- **类说明**：
  存储文件类型枚举（StorageTypeEnum）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义上传文件（附件/图片）的业务类型，决定文件存储目录 归属、命名规则与访问控制策略。与 cn.wisestar.server.storage.StorageService 配合，上传接口通过本枚举标识文件用途。

  **取值说明**：BACKGROUND_IMAGE=1（问卷背景图）、HEADER_IMAGE=2（顶部图）、 QUESTION_IMAGE=3（题目图片）、ANSWER_ATTACHMENT=4（答卷附件）、 TEMPLATE_PREVIEW_IMAGE=5（模板预览图）。 与 cn.wisestar.server.core.constant.AppConsts.FileType 数值一致， 存储层以本枚举为准。
- 方法:
  - `public int getType()`
    获取类型数值。

### `shared/src/main/java/cn/wisestar/server/core/constant/StudentRewardConstants.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `class StudentRewardConstants`
- **类说明**：
  学员积分·学币奖励体系常量。
  【设计原则】奖励金额只由「学习行为类型」决定，与章节/小节/知识点配置无关， 因此教学内容调整不影响积分与学习币体系（体系稳定）。

  【学习币】分学科、单学期上限 #SUBJECT_COIN_LIMIT，只用于商品兑换； 【学海积分】全学科、终身、无上限，只用于荣誉评价（头衔/证书）。
- 方法:
  - `public static int[] reward(String actionType, Integer stage)`
    奖励值：返回 `[学习币, 学海积分]`。
  - `public static String onlineChestAction(int tierMinutes)`
    在线宝箱档位对应的行为类型。
  - `public static int titleLevel(int points)`
    由累计学海积分计算头衔等级（1..5，只升不降由调用方保证）。
  - `public static String titleName(int level)`
    头衔名称。
  - `public static int nextTitlePoints(int level)`
    下一头衔所需积分；已是最高头衔返回 -1。
  - `public static String currentSemester()`
    当前学期键：上学期 9/1-次年 1/31 记 {学年}-1；下学期 2/1-8/31 记 {年}-2。
  - `public static String actionLabel(String actionType)`
    行为名称（积分板块/明细展示用）。

### `shared/src/main/java/cn/wisestar/server/core/constant/TagCategoryEnum.java`
- 包: `cn.wisestar.server.core.constant`
- 类型: `enum TagCategoryEnum`
- **类说明**：
  标签分类枚举（TagCategoryEnum）。
  **所属模块**：shared 模块常量包（cn.wisestar.server.core.constant）。

  **类职责**：定义标签（Tag）的业务归属分类，同一标签体系下通过 分类区分标签的使用场景：模板广场标签、题目模板库标签、问卷标签、考试标签、 学员标签（用于按标签自动分配题库）。

### `shared/src/main/java/cn/wisestar/server/core/exception/ErrorCodeException.java`
- 包: `cn.wisestar.server.core.exception`
- 类型: `class ErrorCodeException`
- **类说明**：
  带错误码的业务异常（ErrorCodeException）。
  **所属模块**：shared 模块核心异常体系（cn.wisestar.server.core.exception）。

  **类职责**：业务代码抛出的"带预定义错误码"的运行时异常。与普通 RuntimeException 的区别是携带了 ErrorCode 枚举（错误码 + 默认消息）， 供前端按错误码做国际化/分支处理。

  **使用场景**：Service 层校验失败、业务规则不满足时抛出，例如 "问卷不存在""无权限访问"等。抛出后由 cn.wisestar.server.core.mvc.advice.GlobalExceptionHandler#handleErrorCodeException 捕获，把 errorCode.code 与 errorCode.message 透传给前端。

  **数据流**：Service 抛 ErrorCodeException → GlobalExceptionHandler 捕获 → ApiResponse{code=errorCode.code, message=errorCode.message} → 前端按 code 处理。
- 方法:
  - `public ErrorCodeException(ErrorCode errorCode)`
    构造异常。
  - `public ErrorCodeException(ErrorCode errorCode, Throwable cause)`
    构造异常（附带根因）。
  - `public ErrorCodeException(ErrorCode errorCode, String message)`
    构造异常（附带面向用户的自定义消息）。
  - `public ErrorCode getErrorCode()`
    获取错误码对象。

### `shared/src/main/java/cn/wisestar/server/core/exception/InternalServerError.java`
- 包: `cn.wisestar.server.core.exception`
- 类型: `class InternalServerError`
- **类说明**：
  服务器内部错误异常（InternalServerError）。
  **所属模块**：shared 模块核心异常体系（cn.wisestar.server.core.exception）。

  **类职责**：表示"服务器内部错误"的运行时异常，通常由业务代码在 遇到不可预期/不可恢复的错误时主动抛出（如依赖服务失败、数据状态非法等）。

  **与普通异常的区别**：cn.wisestar.server.core.mvc.advice.GlobalExceptionHandler 的兜底 ExceptionHandler 会特判本类型：只有本类型的 message 才会被透传给前端， 其他未知异常一律返回通用文案"服务出了点问题"，避免把内部实现细节泄露给客户端。

  **使用场景示例**：cn.wisestar.server.core.aop.DataPermAspect 在 数据权限校验失败时抛出本异常（如"没有权限访问本问卷"）。
- 方法:
  - `public InternalServerError()`
    构造空异常。
  - `public InternalServerError(String message)`
    构造带消息的异常（消息会透传给前端）。
  - `public InternalServerError(String message, Throwable cause)`
    构造带消息与根因的异常。
  - `public InternalServerError(Throwable cause)`
    构造仅带根因的异常。

### `shared/src/main/java/cn/wisestar/server/core/mvc/advice/CustomResponseBodyAdvice.java`
- 包: `cn.wisestar.server.core.mvc.advice`
- 类型: `class CustomResponseBodyAdvice`
- 注解: @RestControllerAdvice
- **类说明**：
  统一响应体包装器（CustomResponseBodyAdvice）。
  **所属模块**：shared 模块 MVC 增强包（cn.wisestar.server.core.mvc.advice）。

  **类职责**：实现 ResponseBodyAdvice，在 Controller 方法返回后、 JSON 序列化之前，把返回值统一包装为 ApiResponse， 实现"Controller 只写业务逻辑、响应格式全局统一"的目标。

  **包装规则**（beforeBodyWrite）：

  - 返回值是 Resource（文件下载、图片等流式资源）→ 原样返回，不包装；
  - 返回值已是 ApiResponse（如全局异常处理器的响应）→ 原样返回，避免二次包装；
  - 其他任意业务返回值 → 包装为 `ApiResponse(ResponseCode.SUCCESS.code, body)`。

  **与异常处理的分工**：本类负责"成功响应"包装；失败响应由 GlobalExceptionHandler 直接构造 ApiResponse（因此不会二次包装）。

  **数据流**：Controller 方法返回业务对象 → 本类的 beforeBodyWrite → ApiResponse{code=0, data=业务对象} → Jackson 序列化 → 前端。
- 方法:
  - `public boolean supports(MethodParameter returnType, Class converterType)`
    是否启用包装。
    当响应由 ResourceRegionHttpMessageConverter 处理时（大文件 分片/范围请求下载场景），返回 false 跳过包装，避免破坏流式响应； 其余转换器一律启用包装。
  - `public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response)`
    响应体写出前的包装逻辑（详见类注释的包装规则）。

### `shared/src/main/java/cn/wisestar/server/core/mvc/advice/GlobalExceptionHandler.java`
- 包: `cn.wisestar.server.core.mvc.advice`
- 类型: `class GlobalExceptionHandler`
- 注解: @ControllerAdvice, @Slf4j
- **类说明**：
  全局异常处理器（GlobalExceptionHandler）。
  **所属模块**：shared 模块 MVC 增强包（cn.wisestar.server.core.mvc.advice）。

  **类职责**：基于 @ControllerAdvice 捕获 Controller 层抛出的各类异常， 统一转换为 ApiResponse JSON 响应，保证任何异常都不会以裸堆栈/HTML 形式返回给前端。HTTP 状态码固定为 200，业务成败通过响应体 code 区分 （业务约定，便于前端统一拦截处理）。

  **异常处理映射表**：

  - NoHandlerFoundException → 404：返回前端 SPA 的 index.html （单 jar 部署兜底：任意未匹配的前端路由都回到首页，由前端路由接管）；
  - ValidationException → 参数校验异常（javax.validation）， 返回 FAIL + 异常 message；
  - MissingServletRequestParameterException → 缺少必填请求参数； 返回 FAIL + "Missing request parameter"；
  - MethodArgumentTypeMismatchException → 参数类型不匹配（如传 abc 给 int）， 返回 FAIL + "Method argument type mismatch"；
  - MethodArgumentNotValidException → @Valid 校验失败， 返回 FAIL + "Method argument validation failed"；
  - AccessDeniedException → 权限不足（已登录但无该操作权限）， 返回 FORBIDDEN（403 码）+ "Authentication failed"；
  - ErrorCodeException → 业务错误码异常，透传 errorCode 的 code 与 message （见 ErrorCode 枚举定义）；
  - Exception（兜底）→ 记录完整堆栈；若为 InternalServerError 则透传其 message，否则返回通用文案"服务出了点问题"。

  **注意**：Spring Security 过滤器链内部抛出的异常（如认证失败）不会进入本类， 由 cn.wisestar.server.core.security.RestAuthenticationEntryPoint 处理。
- 注入/字段: Resource indexHtml
- 方法:
  - `public Object handleError404(HttpServletRequest request, Exception e)`
    处理 404（无对应 Handler）异常：返回 SPA 首页。
    单 jar 部署模式下，未匹配 Controller 且非静态资源的路径（即前端 路由地址，如刷新 /survey/detail 页面）统一返回 index.html，由前端 路由接管渲染。
  - `public ResponseEntity<ApiResponse<String>> handleValidationException(HttpServletRequest request, ValidationException ex)`
    处理 javax 参数校验异常（ValidationException）。
  - `public ResponseEntity<ApiResponse<String>> handleMissingServletRequestParameterException(HttpServletRequest request, MissingServletRequestParameterException ex)`
    处理缺少必填请求参数异常。
  - `public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentTypeMismatchException( HttpServletRequest request, MethodArgumentTypeMismatchException ex)`
    处理请求参数类型不匹配异常（如字符串传给数字参数）。
  - `public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException( HttpServletRequest request, MethodArgumentNotValidException ex)`
    处理 @Valid 注解校验失败异常（含字段级错误明细）。
  - `public ResponseEntity<ApiResponse<String>> handleAccessDeniedException(HttpServletRequest request, AccessDeniedException ex)`
    处理权限不足异常（AccessDeniedException）。
    触发场景：用户已登录但缺少目标接口所需的权限码 （Controller @PreAuthorize 校验失败）。
  - `public ResponseEntity<ApiResponse<String>> handleErrorCodeException(HttpServletRequest request, ErrorCodeException ex)`
    处理带错误码的业务异常（ErrorCodeException）。
    透传 ErrorCode 的 code；消息取异常自定义 message（如 Excel 导入的行级 校验明细），未携带自定义消息时回退 ErrorCode.message。
  - `public ResponseEntity<ApiResponse<String>> handleInternalServerError(HttpServletRequest request, Exception ex)`
    兜底异常处理（所有未被上面规则捕获的异常）。
    记录完整堆栈；响应体策略：

    - 异常是 InternalServerError：透传其 message（该异常的消息设计为 可安全展示给用户）；
    - 其他未知异常：返回通用文案"服务出了点问题"，避免泄露内部错误细节。

### `shared/src/main/java/cn/wisestar/server/core/security/JwtTokenFilter.java`
- 包: `cn.wisestar.server.core.security`
- 类型: `class JwtTokenFilter`
- 注解: @Component, @RequiredArgsConstructor
- **类说明**：
  JWT 认证过滤器（JwtTokenFilter）。
  **所属模块**：shared 模块核心安全框架（cn.wisestar.server.core.security）。

  **类职责**：作为 Spring Security 过滤器链中的一个前置过滤器（注册在 `UsernamePasswordAuthenticationFilter` 之前），负责在每个 HTTP 请求进入 Controller 之前完成 JWT 令牌的解析与用户身份认证：

  - 从请求 Cookie（AppConsts#TOKEN_NAME）或 URL 查询参数中取出 JWT 令牌；
  - 校验令牌的签名、有效期等合法性（委托 JwtTokenUtil#validate）；
  - 令牌合法时根据令牌内携带的 userId 加载用户信息（委托 UserService#loadUserById）， 组装 UsernamePasswordAuthenticationToken 并写入 SecurityContextHolder，完成登录态的建立；
  - 若请求仅以 URL 参数携带令牌（如 H5 下载链接等场景），认证通过后还会将令牌 回写为 HttpOnly Cookie，方便后续请求自动携带。

  **为什么放行而不直接 401**：本过滤器对"无令牌 / 令牌非法"的请求不主动拦截， 直接放行到后续流程——因为整个应用的授权控制是通过 Controller 方法上的 `@PreAuthorize` 注解（配合 `@EnableGlobalMethodSecurity`）完成的， 未登录用户访问受保护接口时会被方法级安全抛出认证异常，再交给 RestAuthenticationEntryPoint 统一输出 JSON 错误。这样单 jar 部署时 任意前端路由都可以直达静态页面。

  **异常处理**：`loadUserById` 抛出 AuthenticationException （如账号已禁用/不存在）时，由于该异常发生在 Spring Security 过滤器链内部， 普通 `@RestControllerAdvice` 的 `GlobalExceptionHandler` 无法捕获， 因此这里显式调用 RestAuthenticationEntryPoint#commence 输出 application/json 格式的未认证响应。

  **调用方**：由 Spring Security 过滤器链自动调用（见 WebSecurityConfig#configure(HttpSecurity) 中的 `http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)`）。
- 方法:
  - `protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException`
    请求认证核心逻辑（每请求只执行一次，由 `OncePerRequestFilter` 保证）。
    **执行流程**：

    - **取令牌**：优先从 Cookie 中读取；若配置了 `sk.security.url-token-authentication.enabled=true`（默认开启）， 则同时允许从 URL 查询参数中读取同名参数作为令牌。
    - **无令牌放行**：Cookie 与参数都取不到令牌时，不建立认证，直接放行到过滤器链后续环节 （未登录访问受保护接口将由方法级安全拦截）。
    - **校验令牌**：调用 JwtTokenUtil#validate 校验签名与有效期， 非法令牌同样直接放行（不在此处拦截）。
    - **建立认证**：按令牌内 userId 加载用户（UserService#loadUserById）， 构造 UsernamePasswordAuthenticationToken（携带该用户的权限集合）， 并绑定请求详情（IP、Session 等，见 WebAuthenticationDetailsSource）， 最后写入 SecurityContextHolder 供后续 @PreAuthorize 校验使用。
    - **令牌回写 Cookie**：若本次令牌来自 URL 参数（而非 Cookie），认证成功后将其 回写为 HttpOnly Cookie，实现"一次携带、后续自动带上"。
    - **放行**：继续执行过滤器链，进入 Controller。

### `shared/src/main/java/cn/wisestar/server/core/security/JwtTokenUtil.java`
- 包: `cn.wisestar.server.core.security`
- 类型: `class JwtTokenUtil`
- 注解: @Slf4j, @Component, @RequiredArgsConstructor
- **类说明**：
  JWT 令牌工具类（JwtTokenUtil）。
  **所属模块**：shared 模块核心安全框架（cn.wisestar.server.core.security）。

  **类职责**：封装基于 jjwt 库的 JWT（JSON Web Token）生成、校验与解析逻辑， 为无状态（STATELESS）的登录认证提供支撑：

  - **生成令牌**：#generateAccessToken —— 登录成功后由 UserService（rdbms 实现）调用，将当前用户信息 （UserTokenView）作为自定义 claim 写入令牌；
  - **校验令牌**：#validate —— 由 JwtTokenFilter 在每次请求时调用， 校验签名与有效期，捕获并记录各类非法令牌日志；
  - **解析令牌**：#getUser —— 从合法令牌中反序列化出用户信息 UserTokenView，供过滤器定位 userId。

  **签名机制**：使用 HS512（HMAC-SHA512）算法对称签名，密钥由 旧版每次启动随机生成密钥导致重启后令牌全部失效；现改为固定配置（wisestar.jwt.secret），应用每次重启后所有 已签发的令牌都会失效（用户需要重新登录）。

  **数据流**：登录成功 → 组装 UserTokenView → generateAccessToken 生成令牌返回前端 → 前端存入 Cookie/请求头 → 后续请求 → JwtTokenFilter → validate 校验 → getUser 解析用户。
- 注入/字段: String jwtSecret
- 方法:
  - `public String generateAccessToken(UserTokenView user)`
    生成访问令牌（Access Token）。
  - `public boolean validate(String token)`
    校验令牌的签名与结构是否合法（不含业务校验）。
    捕获 jjwt 抛出的全部令牌异常类型并记录 error 日志，任意一种异常都会返回 false：

    - SignatureException：签名不匹配（令牌被篡改或使用错误密钥签发）；
    - MalformedJwtException：令牌结构畸形（非三段式、非法 Base64 等）；
    - ExpiredJwtException：令牌已过期；
    - UnsupportedJwtException：不支持的令牌类型；
    - IllegalArgumentException：令牌为空字符串。
  - `public UserTokenView getUser(String token)`
    从合法令牌中解析出登录用户信息。
    使用 Jackson 反序列化器把 payload 中的 "user" claim 映射回 UserTokenView 对象。注意：本方法不校验签名，调用方应保证 传入的令牌已通过 #validate 校验（JwtTokenFilter 中已保证）。

### `shared/src/main/java/cn/wisestar/server/core/security/PasswordEncoder.java`
- 包: `cn.wisestar.server.core.security`
- 类型: `class PasswordEncoder`
- 注解: @Component
- **类说明**：
  密码编码器（PasswordEncoder）。
  **所属模块**：shared 模块核心安全框架（cn.wisestar.server.core.security）。

  **类职责**：继承 Spring Security 的 BCryptPasswordEncoder， 并将其注册为 Spring 容器中的 @Component，用于系统内所有密码的 加密存储 与 校验比对。

  **使用的算法**：BCrypt（自带随机盐、单向不可逆、同密码多次加密结果不同）， 是业界推荐的密码散列方案，可有效抵御彩虹表攻击与暴力破解。

  **使用场景与调用方**：

  - 用户注册/创建用户时调用 `encode(明文)` 加密密码后落库（见 UserService 实现）；
  - 登录认证时由 WebSecurityConfig#configure(AuthenticationManagerBuilder) 配置到 Spring Security 的 DaoAuthenticationProvider 中，自动调用 `matches(明文, 密文)` 校验用户提交的密码；
  - 修改密码等场景校验旧密码。

  **为什么自定义子类**：直接以 @Component 注入容器，使业务代码可以 通过构造器注入本类型，避免与 Spring Security 内部使用的编码器实例混淆。

### `shared/src/main/java/cn/wisestar/server/core/security/PreAuthorizeAnnotationExtractor.java`
- 包: `cn.wisestar.server.core.security`
- 类型: `class PreAuthorizeAnnotationExtractor`
- 注解: @Slf4j
- 方法:
  - `public static Set<String> extractAllApiPermissions()`
    提取系统中全部 API 权限码（带缓存）。
    遍历 Spring 容器中所有 @RestController Bean，分别提取类级与方法级的
  - `public static List<Method> extractAnnotationMethods(Class<?> clazz, Class<? extends Annotation> annotationClass)`
    获取类及其所有父类中标注了指定注解的方法列表。
    自底向上遍历继承链（含 Object），收集每个类上声明且带有目标注解的方法。 子类重写（override）父类方法时，以子类声明为准（父类的同名方法注解 会被子类方法取代，因为遍历时子类方法已加入列表）。

### `shared/src/main/java/cn/wisestar/server/core/security/RestAuthenticationEntryPoint.java`
- 包: `cn.wisestar.server.core.security`
- 类型: `class RestAuthenticationEntryPoint`
- 注解: @Component, @RequiredArgsConstructor
- **类说明**：
  REST 认证失败入口点（RestAuthenticationEntryPoint）。
  **所属模块**：shared 模块核心安全框架（cn.wisestar.server.core.security）。

  **类职责**：实现 Spring Security 的 AuthenticationEntryPoint， 在"未认证/认证失败"的请求访问受保护资源时，向客户端输出 application/json 格式的统一错误响应，而不是 Spring Security 默认的重定向到登录页或返回 HTML 错误页。

  **触发场景**：

  - 匿名用户访问受保护的 `/api/**` 接口（被 WebSecurityConfig 中 `.antMatchers("/api/**").authenticated()` 拦截）；
  - JwtTokenFilter 在过滤器链内加载用户失败抛出 AuthenticationException 时被显式调用（见该过滤器 catch 分支）。

  **响应格式**：HTTP 状态码保持 200（业务约定），响应体为 ApiResponse 的 JSON 结构：`{code: ResponseCode.UNAUTHORIZED.code, message: 异常信息`}。前端根据 code 判断未登录并跳转登录页。

  **调用方**：Spring Security 异常处理机制自动调用（见 WebSecurityConfig#configure(HttpSecurity) 中 `http.exceptionHandling().authenticationEntryPoint(...)`）；以及 JwtTokenFilter 手动调用。Bean 名称 "restAuthenticationEntryPoint" 供按名称注入使用。
- 方法:
  - `public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException) throws IOException, ServletException`
    输出未认证 JSON 响应（认证失败的统一出口）。

### `shared/src/main/java/cn/wisestar/server/core/uitls/Ansi.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class Ansi`
- **类说明**：
  Usage:
  - String msg = Ansi.Red.and(Ansi.BgYellow).format("Hello %s", name)
  - String msg = Ansi.Blink.colorize("BOOM!") Or, if you are adverse to that, you can use the constants directly:
  - String msg = new Ansi(Ansi.ITALIC, Ansi.GREEN).format("Green money") Or, even:
  - String msg = Ansi.BLUE + "scientific" NOTE: Nothing stops you from combining multiple FG colors or BG colors, but only the last one will display.
- 方法:
  - `public static final Ansi HighIntensity = new Ansi(HIGH_INTENSITY)`
  - `public static final Ansi LowIntensity = new Ansi(LOW_INTENSITY)`
  - `public static final Ansi Italic = new Ansi(ITALIC)`
  - `public static final Ansi Underline = new Ansi(UNDERLINE)`
  - `public static final Ansi Blink = new Ansi(BLINK)`
  - `public static final Ansi RapidBlink = new Ansi(RAPID_BLINK)`
  - `public static final Ansi Black = new Ansi(BLACK)`
  - `public static final Ansi Red = new Ansi(RED)`
  - `public static final Ansi Green = new Ansi(GREEN)`
  - `public static final Ansi Yellow = new Ansi(YELLOW)`
  - `public static final Ansi Blue = new Ansi(BLUE)`
  - `public static final Ansi Magenta = new Ansi(MAGENTA)`
  - `public static final Ansi Cyan = new Ansi(CYAN)`
  - `public static final Ansi White = new Ansi(WHITE)`
  - `public static final Ansi BgBlack = new Ansi(BACKGROUND_BLACK)`
  - `public static final Ansi BgRed = new Ansi(BACKGROUND_RED)`
  - `public static final Ansi BgGreen = new Ansi(BACKGROUND_GREEN)`
  - `public static final Ansi BgYellow = new Ansi(BACKGROUND_YELLOW)`
  - `public static final Ansi BgBlue = new Ansi(BACKGROUND_BLUE)`
  - `public static final Ansi BgMagenta = new Ansi(BACKGROUND_MAGENTA)`
  - `public static final Ansi BgCyan = new Ansi(BACKGROUND_CYAN)`
  - `public static final Ansi BgWhite = new Ansi(BACKGROUND_WHITE)`
  - `public Ansi(String... codes)`
  - `public Ansi and(Ansi other)`
  - `public String colorize(String original)`
  - `public String format(String template, Object... args)`

### `shared/src/main/java/cn/wisestar/server/core/uitls/AnswerJudgeUtil.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class AnswerJudgeUtil`
- **类说明**：
  练习答题判分工具（纯静态，无状态）。
  **定位**：供练习落库（PracticeServiceImpl）对"前端提交的学生答案"复核判分， 语义与前端 utils/practiceHelpers.js 一致：单选/判断按选项标题文本等值、 多选按选项标题集合相等（与顺序无关）、填空/文本按输入内容等值。 与 AnswerServiceImpl 的判分规则对齐，避免前后端判分不一致。

  **答案格式**：学生答案由前端提交，结构为 `{type: 'option', optionId} / {type: 'options', optionIds: []} / {type: 'text', text}`； 判分前先按题目选项映射为"选项标题文本"再比较。

  **返回值约定**：1 正确 / 0 错误（含未作答）/ null 无标准答案（不计分）。
- 方法:
  - `public static List<String> extractCorrectAnswers(SurveySchema question)`
    提取题目标准答案列表；无任何标准答案时返回 null。
    **提取优先级**：
     1. 整题级答案：题目 attribute.examCorrectAnswer（多选多个答案以 \n 分隔）； 2. 选项级答案：遍历子选项，收集 attribute.examCorrectAnswer 非空的选项标题。
  - `public static Integer evaluate(SurveySchema question, Map<String, Object> studentAnswer)`
    判定题目对错。
  - `public static int[] evaluateBlanks(SurveySchema question, Map<String, Object> studentAnswer)`
    填空类题目（单项填空/多项填空）逐空判分。
    **语义**：多项填空允许多个空位部分正确，返回每个空位的命中标记（1=对 / 0=错）， 供练习计分按"空位分值"累加（全对时与 evaluate=1 等价）。

    **空位定义**：题目标准答案 examCorrectAnswer 以 `|` 分隔各空答案； 单项填空无分隔符视为单空。学生答案同样按 `|` 拆空。
  - `public static String formatAnswer(SurveySchema question, Map<String, Object> answer)`
    把前端答案 Map 格式化为可比较/可展示的文本。
    **映射规则**：
     - type=option：optionId 对应子选项的 title（单选/判断）； - type=options：多个 optionIds 对应 titles 逗号拼接（多选）； - type=text：直接取 text（填空/文本）； - 其他/无效：null。

### `shared/src/main/java/cn/wisestar/server/core/uitls/AnswerScoreEvaluator.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class AnswerScoreEvaluator`
- 注解: @Slf4j
- **类说明**：
  用于计算问卷的分值
- 内部类型: interface Evaluator, class OnlyOneEvaluator, class SelectAllEvaluator, class SelectCorrectEvaluator, class SelectEvaluator, class DefaultEvaluator
- 方法:
  - `public AnswerScoreEvaluator(SurveySchema schema, LinkedHashMap answer)`
  - `public Double eval()`
    计算总分
  - `public LinkedHashMap<String, Double> getQuestionScore()`
    获取问题的分值
  - `public Double eval(SurveySchema qSchema)`
    只有一个正确答案
  - `public boolean support(SurveySchema qSchema)`
  - `public Double eval(SurveySchema qSchema)`
    全部答对才得分
  - `public boolean support(SurveySchema qSchema)`
  - `public Double eval(SurveySchema qSchema)`
    按正确答案计分，选择题答错不得分、填空题按正确答案算分
  - `public boolean support(SurveySchema qSchema)`
  - `public Double eval(SurveySchema qSchema)`
    按选中的答案算分
  - `public boolean support(SurveySchema qSchema)`
  - `public Double eval(SurveySchema qSchema)`
  - `public boolean support(SurveySchema qSchema)`

### `shared/src/main/java/cn/wisestar/server/core/uitls/BarcodeReader.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class BarcodeReader`
- 方法:
  - `public static String readBarcode(InputStream inputStream)`
  - `public static String readBarcode(InputStream inputStream, Map hintMap, Map hintMap_pure) throws FileNotFoundException, IOException, NotFoundException`

### `shared/src/main/java/cn/wisestar/server/core/uitls/ClassUtils.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class ClassUtils`
- 方法:
  - `public static List<String> flatClassFields(Class cls, List<String> parentAttr, int maxDepth)`
    将 class的多个层级的field 打平成 . 分割的

### `shared/src/main/java/cn/wisestar/server/core/uitls/ContextHelper.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class ContextHelper`
- 注解: @Component, @Lazy, @Slf4j
- **类说明**：
  Spring上下文帮助类
- 方法:
  - `public void setApplicationContext(ApplicationContext applicationContext) throws BeansException`
  - `public static ApplicationContext getApplicationContext()`
    获取ApplicationContext上下文
  - `public static Object getBean(String beanId)`
    根据beanId获取Bean实例
  - `public static <T> T getBean(Class<T> clazz)`
    获取指定类型的单个Bean实例
  - `public static <T> List<T> getBeans(Class<T> type)`
    获取指定类型的全部实现类
  - `public static List<Object> getBeansByAnnotation(Class<? extends Annotation> annotationType)`
    根据注解获取beans
  - `public static HttpServletRequest getCurrentHttpRequest()`
  - `public static HttpServletResponse getCurrentHttpResponse()`
  - `public static String getCookie(String cookieName)`

### `shared/src/main/java/cn/wisestar/server/core/uitls/CronHelper.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class CronHelper`
- 方法:
  - `public CronHelper(String cron)`
  - `public Tuple2<LocalDateTime, LocalDateTime> currentWindow()`
  - `public Tuple2<LocalDateTime, LocalDateTime> nextWindow()`
    下个时间窗

### `shared/src/main/java/cn/wisestar/server/core/uitls/DatabaseInitHelper.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class DatabaseInitHelper`
- 方法:
  - `public static void init(String[] args)`
  - `public static void initMySql()`
  - `public static void initH2()`
  - `public static String getJarFilePath()`
    获取当前 jar 运行目录

### `shared/src/main/java/cn/wisestar/server/core/uitls/DateUtils.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class DateUtils`
- 方法:
  - `public static Date localDateTime2date(LocalDateTime in)`
  - `public static LocalDateTime date2localDateTime(Date in)`
  - `public static boolean isBetween(Date current, Date start, Date end)`

### `shared/src/main/java/cn/wisestar/server/core/uitls/ExcelExporter.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class ExcelExporter`
- **类说明**：
  轻量级 excel 导出工具，换成 poi 打的包会多出十几 M，不能接受，-_-||
- 方法:
  - `public ExcelExporter()`
  - `public ExcelExporter(OutputStream outputStream)`
  - `public Worksheet createSheet(String sheetName)`
  - `public void createHeader(List<String> columns)`
  - `public void createRow(List<List<Object>> rows)`
  - `public ByteArrayInputStream export()`
  - `public void exportToStream()`
  - `public ExcelExporter build()`

### `shared/src/main/java/cn/wisestar/server/core/uitls/FileUtils.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class FileUtils`
- 方法:
  - `public static Double bytesToMb(long bytes)`
    将文件大小从字节转换为兆字节（MB）。

### `shared/src/main/java/cn/wisestar/server/core/uitls/HTTPUtils.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class HTTPUtils`
- **类说明**：
  参考 https://segmentfault.com/a/1190000023601065
- 方法:
  - `public static String getContentDispositionValue(String fileName)`
    获取下载文件名，避免中文文件名乱码

### `shared/src/main/java/cn/wisestar/server/core/uitls/IPUtils.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class IPUtils`
- 方法:
  - `public static String getClientIpAddress(HttpServletRequest request)`

### `shared/src/main/java/cn/wisestar/server/core/uitls/JSONUtil.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class JSONUtil`
- 注解: @Slf4j
- 方法:
  - `public static String toJSONString(Object object)`
    Transfer object to JSON string

### `shared/src/main/java/cn/wisestar/server/core/uitls/KnowledgeValueNormalizer.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class KnowledgeValueNormalizer`
- **类说明**：
  知识结构（章节/小节）字段归一化工具。
  Excel 导入时「学期/版本」常见多种写法（如「上册」「人教版RJ」），而管理端筛选下拉 使用统一口径（学期「上/下」、版本「人教版/苏教版/北师大版/外研版」）。入库前归一化， 避免存值与筛选值不一致导致按条件查不到数据。
- 方法:
  - `public static String term(String text)`
    学期归一化：含「上」→「上」，含「下」→「下」；其余原样返回。
  - `public static String version(String text)`
    教材版本归一化：命中已知版本（兼容去掉「版」的简称，如 人教/苏教）时返回基础版本名，否则原样返回。

### `shared/src/main/java/cn/wisestar/server/core/uitls/MapBeanUtils.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class MapBeanUtils`
- 方法:
  - `public static <T> Map<String, ?> beanToMap(T bean)`
  - `public static <T> T mapToBean(Map<String, ?> map, Class<T> clazz) throws IllegalAccessException, InstantiationException`
  - `public static <T> List<Map<String, ?>> objectsToMaps(List<T> objList)`
  - `public static <T> List<T> mapsToObjects(List<Map<String, ?>> maps, Class<T> clazz) throws InstantiationException, IllegalAccessException`

### `shared/src/main/java/cn/wisestar/server/core/uitls/NanoIdUtils.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class NanoIdUtils`
- **类说明**：
  A class for generating unique String IDs. The implementations of the core logic in this class are based on NanoId, a JavaScript library by Andrey Sitnik released under the MIT license. (https://github.com/ai/nanoid)
- 方法:
  - `public static final SecureRandom DEFAULT_NUMBER_GENERATOR = new SecureRandom()`
    The default random number generator used by this class. Creates cryptographically strong NanoId Strings.
  - `public static String randomNanoId()`
    Static factory to retrieve a url-friendly, pseudo randomly generated, NanoId String. The generated NanoId String will have 21 symbols. The NanoId String is generated using a cryptographically strong pseudo random number generator.
  - `public static String randomNanoId(int size)`
    Static factory to retrieve a url-friendly, pseudo randomly generated, NanoId String. The generated NanoId String will have 21 symbols. The NanoId String is generated using a cryptographically strong pseudo random number generator.
  - `public static String randomNanoId(int size, Set<String> ids)`
  - `public static String randomNanoId(final Random random, final char[] alphabet, final int size)`
    Static factory to retrieve a NanoId String. The string is generated using the given random number generator.

### `shared/src/main/java/cn/wisestar/server/core/uitls/PinyinUtils.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class PinyinUtils`
- 方法:
  - `public static String chineseToPinyin(String str)`
    汉字转拼音

### `shared/src/main/java/cn/wisestar/server/core/uitls/ProjectStatHelper.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class ProjectStatHelper`
- 方法:
  - `public ProjectStatHelper(SurveySchema schema, List<AnswerView> answers)`
  - `public PublicStatisticsView stat()`

### `shared/src/main/java/cn/wisestar/server/core/uitls/RSAUtils.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class RSAUtils`
- 注解: @Component
- 方法:
  - `public void init()`
  - `public static HashMap<String, String> genKeyPair() throws NoSuchAlgorithmException`
    生成随机 RSA 密钥对
  - `public static String encrypt(String str, String publicKey) throws Exception`
    使用 RSA 公钥加密字符串
  - `public static String decrypt(String str, String privateKey) throws Exception`
    使用 RSA 私钥解密字符串
  - `public static String decrypt(String str) throws Exception`
    使用 RSA 私钥解密字符串

### `shared/src/main/java/cn/wisestar/server/core/uitls/RepoTemplateExcelParseHelper.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class RepoTemplateExcelParseHelper`
- 方法:
  - `public RepoTemplateExcelParseHelper(MultipartFile templateFile)`
  - `public List<TemplateRequest> parse()`

### `shared/src/main/java/cn/wisestar/server/core/uitls/RepoTemplateI18n.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class RepoTemplateI18n`
- **类说明**：
  国际化辅助，统一管理题库 Excel 模板的 sheet 名称和表头
- 内部类型: enum SheetType, enum HeaderLabel
- 方法:
  - `public static String message(String key, String defaultMessage)`
  - `public static String message(String key, String defaultPattern, Object... args)`
  - `public static boolean isOptionColumn(String cellText)`
  - `public static boolean isBlankColumn(String cellText)`
  - `public static String optionLabel(String suffix)`
  - `public static String blankLabel(String index)`
  - `public static String workbookName()`
  - `public String displayName()`
  - `public boolean matches(String name)`
  - `public static SheetType fromName(String name)`
  - `public boolean matches(String name)`
  - `public void apply(Map<String, Integer> target, int columnIndex)`
  - `public String displayLabel()`
  - `public static HeaderLabel fromText(String text)`

### `shared/src/main/java/cn/wisestar/server/core/uitls/SchemaHelper.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class SchemaHelper`
- 注解: @Slf4j
- **类说明**：
  schema的一些帮助方法
- 内部类型: enum LoginFormFieldEnum
- 注入/字段: String openidColumnName
- 方法:
  - `public static String getOpenIdColumnName()`
  - `public static List<String> parseColumns(List<SurveySchema> schemaDataTypes, ProjectModeEnum mode)`
    将 schema 解析为导出的 excel的header
  - `public static List<SurveySchema> flatSurveySchema(SurveySchema schema)`
  - `public static List<SurveySchema> flatSurveySchemaWitchOption(SurveySchema schema, boolean withOption)`
  - `public static List<Object> parseRowData(AnswerView answerInfo, List<SurveySchema> dataTypes, int index, ProjectModeEnum mode)`
    转换答案为导出 excel 的行格式
  - `public static String trimHtmlTag(String string)`
    移除HTML标签和实体字符 更好的方式是使用 Jsoup.parse(html).text(); 但是我不想引入过多的第三方 jar
  - `public static void updateSchemaByPermission(LinkedHashMap<String, Integer> fieldPermission, SurveySchema schema)`
    根据字段权限更新Schema
  - `public static void ignoreAttributes(SurveySchema schema, String... attributes)`
    移除 schema 里面的指定属性值
  - `public static List<SurveySchema> findSchemaListByAttribute(SurveySchema schema, String attributeName, Object attributeValue)`
    根据属性名和属性值找到所有满足条件的子 schema 列表
  - `public static List<SurveySchema> findSchemaHasAttribute(SurveySchema schema, String attributeName)`
    根据属性名查找子 schema 列表
  - `public static SurveySchema buildFillBlankQuerySchema(LoginFormFieldEnum field)`
    主要是用于构建 FillBlank 类型的查询表单
  - `public static Object getLoginFormAnswer(LinkedHashMap<String, Object> answer, LoginFormFieldEnum field)`
  - `public static void appendChildIfNotExist(SurveySchema parent, SurveySchema child)`
    将问题 schema 添加到问卷里面
  - `public static void setQuestionValue(LinkedHashMap<String, Object> answer, String qId, String oId, Object newValue)`
  - `public static Object getQuestionValue(LinkedHashMap<String, Object> answer, String qId, String oId)`
  - `public static TreeNode SurveySchema2TreeNode(SurveySchema surveySchema)`
  - `public static String buildLinkLikeCondition(SurveySchema.LinkSurvey linkSurvey, Object value)`
    构建 linkSurvey 的查询条件
  - `public TreeNode(SurveySchema data, TreeNode parent)`
  - `public Map<String, TreeNode> getTreeNodeMap()`
  - `public TreeNode getParent()`
  - `public SurveySchema getData()`
  - `public String getTitle()`
  - `public SurveySchema.DataType getDataType()`

### `shared/src/main/java/cn/wisestar/server/core/uitls/ScriptRunner.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class ScriptRunner`
- **类说明**：
  Tool to run database scripts
- 注入/字段: PrintWriter logWriter, PrintWriter errorLogWriter
- 方法:
  - `public static final Pattern delimP = Pattern.compile("^\\s*(--)?\\s*delimiter\\s*=?\\s*([^\\s]+)+\\s*.*$", Pattern.CASE_INSENSITIVE)`
    regex to detect delimiter. ignores spaces, allows delimiter in comment, allows an equals-sign
  - `public ScriptRunner(Connection connection, boolean autoCommit, boolean stopOnError)`
    Default constructor
  - `public void setDelimiter(String delimiter, boolean fullLineDelimiter)`
  - `public void setLogWriter(PrintWriter logWriter)`
    Setter for logWriter property
  - `public void setErrorLogWriter(PrintWriter errorLogWriter)`
    Setter for errorLogWriter property
  - `public void setUserDirectory(String userDirectory)`
    Set the current working directory. Source commands will be relative to this.
  - `public void runScript(String filepath) throws IOException, SQLException`
    Runs an SQL script (read in using the Reader parameter)
  - `public void runScript(Reader reader) throws IOException, SQLException`
    Runs an SQL script (read in using the Reader parameter)

### `shared/src/main/java/cn/wisestar/server/core/uitls/SecurityContextUtils.java`
- 包: `cn.wisestar.server.core.uitls`
- 类型: `class SecurityContextUtils`
- 方法:
  - `public static UserInfo getUser()`
  - `public static String getUsername()`
  - `public static String getUserId()`
  - `public static boolean isAuthenticated()`
    用户是否已登录
  - `public static boolean isAnonymous()`
    当前是否是匿名访问
  - `public static boolean isAdmin()`

### `shared/src/main/java/cn/wisestar/server/domain/dto/AnswerExamInfo.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class AnswerExamInfo`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/AnswerMetaInfo.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class AnswerMetaInfo`
- 注解: @Data, @Builder, @AllArgsConstructor, @NoArgsConstructor

### `shared/src/main/java/cn/wisestar/server/domain/dto/AnswerQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class AnswerQuery`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  答卷分页查询条件 DTO。 【类职责】 承载"答卷列表查询"（AnswerApi.listAnswer）的过滤参数：按项目、主键、时间区间、 项目名称等维度筛选答卷。 【被谁调用】 - AnswerApi.listAnswer → AnswerServiceImpl.listAnswer（构造 MyBatis-Plus 查询条件） - AnswerServiceImpl.downloadSurvey（导出答卷时复用 listAnswer 查询） 【数据流】 前端答卷列表页（AnswerListPage）GET /api/answer/list → 本 DTO（GET query 参数绑定） → AnswerServiceImpl.listAnswer → AnswerMapper 分页查询 t_answer → AnswerView 列表返回

### `shared/src/main/java/cn/wisestar/server/domain/dto/AnswerRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class AnswerRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/AnswerUploadRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class AnswerUploadRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/AnswerUploadView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class AnswerUploadView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/AnswerView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class AnswerView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/AuthRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class AuthRequest`
- 注解: @Data
- 注入/字段: String username, String password

### `shared/src/main/java/cn/wisestar/server/domain/dto/CampusRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class CampusRequest`
- 注解: @Data
- **类说明**：
  校区请求（行政管理-校区管理，create/update 共用）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/CampusScope.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class CampusScope`
- 注解: @Data
- **类说明**：
  校区数据权限范围（当前登录用户）。
  规则：角色含 admin 或不属于校长/教务/学管师的账号 → ALL； 属于校长/教务/学管师且绑定校区 → SCOPED(绑定校区名称集合，含停用校区)； 属于上述角色但未绑定任何校区 → EMPTY。

  调用方在学员/订单/督学查询与单对象访问处消费该范围；范围过滤在服务层强制执行。
- 方法:
  - `public static CampusScope all()`
  - `public static CampusScope empty()`
  - `public static CampusScope scoped(Set<String> campusNames)`
  - `public boolean isAll()`
  - `public boolean isEmpty()`
  - `public boolean isScoped()`
  - `public Set<String> getCampusNames()`

### `shared/src/main/java/cn/wisestar/server/domain/dto/CampusView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class CampusView`
- 注解: @Data
- **类说明**：
  校区视图（行政管理-校区管理列表/下拉）。
  列表场景附带统计：引用学员数 studentCount 与绑定 校长/教务/学管师三类角色员工数（roleCount，按 t_user_campus 关联角色统计）； 用户管理绑定场景仅使用 id/name/status 字段。

### `shared/src/main/java/cn/wisestar/server/domain/dto/CategoryQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class CategoryQuery`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/CommDictItemQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class CommDictItemQuery`
- 注解: @Data, @EqualsAndHashCode

### `shared/src/main/java/cn/wisestar/server/domain/dto/CommDictItemRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class CommDictItemRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/CommDictItemView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class CommDictItemView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/CommDictQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class CommDictQuery`
- 注解: @Data, @EqualsAndHashCode

### `shared/src/main/java/cn/wisestar/server/domain/dto/CommDictRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class CommDictRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/CommDictView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class CommDictView`
- 注解: @Data
- 注入/字段: Date createAt

### `shared/src/main/java/cn/wisestar/server/domain/dto/DashboardQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class DashboardQuery`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/DashboardRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class DashboardRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/DashboardSetting.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class DashboardSetting`
- 注解: @Data
- 注入/字段: Boolean _static

### `shared/src/main/java/cn/wisestar/server/domain/dto/DashboardView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class DashboardView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/DatabaseInitProperties.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class DatabaseInitProperties`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/DeptRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class DeptRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/DeptSortRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class DeptSortRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/DeptView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class DeptView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/DownloadData.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class DownloadData`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/DownloadQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class DownloadQuery`
- 注解: @Data, @EqualsAndHashCode
- 内部类型: enum DownloadType
- 注入/字段: String projectId, Date startTime, Date endTime, DownloadType type

### `shared/src/main/java/cn/wisestar/server/domain/dto/ExerciseProjectTemplate.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class ExerciseProjectTemplate`
- **类说明**：
  练习模式项目模板
- 方法:
  - `public static ProjectView getExerciseTemplate()`

### `shared/src/main/java/cn/wisestar/server/domain/dto/ExerciseView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class ExerciseView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/FileQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class FileQuery`
- 注解: @Data
- 方法:
  - `public FileQuery()`
  - `public FileQuery(String id)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/FileView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class FileView`
- 注解: @Data
- 注入/字段: String filePath

### `shared/src/main/java/cn/wisestar/server/domain/dto/HistoryExerciseQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class HistoryExerciseQuery`
- 注解: @Data, @EqualsAndHashCode
- 注入/字段: Date[] createTime

### `shared/src/main/java/cn/wisestar/server/domain/dto/KnowledgePointQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class KnowledgePointQuery`
- 注解: @Data
- **类说明**：
  知识点统计查询条件

### `shared/src/main/java/cn/wisestar/server/domain/dto/KnowledgePointStat.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class KnowledgePointStat`
- 注解: @Data
- **类说明**：
  知识点统计结果：答题次数、正确次数、正确率

### `shared/src/main/java/cn/wisestar/server/domain/dto/MyTaskQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class MyTaskQuery`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  首页任务查询

### `shared/src/main/java/cn/wisestar/server/domain/dto/MyTaskView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class MyTaskView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/NodeQuestionView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class NodeQuestionView`
- 注解: @Data
- **类说明**：
  习题列表页「节点刷题内容预览」题目视图（与学员端刷题同一题目语义）。
  schema 保留 SurveySchema 原结构：withAnswer=true 时含标准答案与解析； withAnswer=false 时递归剥离答案字段（含子题），防误展示。

### `shared/src/main/java/cn/wisestar/server/domain/dto/PageQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PageQuery`
- 注解: @Data
- 方法:
  - `public int getPageSize()`

### `shared/src/main/java/cn/wisestar/server/domain/dto/PermissionView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PermissionView`
- 注解: @Data
- 方法:
  - `public PermissionView(String code)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/PickRepoQuestionRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PickRepoQuestionRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/PositionQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PositionQuery`
- 注解: @Data, @EqualsAndHashCode

### `shared/src/main/java/cn/wisestar/server/domain/dto/PositionRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PositionRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/PositionView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PositionView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/PracticeResultView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PracticeResultView`
- 注解: @Data
- **类说明**：
  练习提交判分结果。
  返回总分/答对数与逐题对错（含标准答案，供学员端即时反馈）。
- 方法:
  - `public PracticeResultItem()`
  - `public PracticeResultItem(String questionId, Integer correct, String correctAnswer, String detailId)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/PracticeSubmitRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PracticeSubmitRequest`
- 注解: @Data
- **类说明**：
  练习交卷提交请求（学员端练习完成后落库）。
  **数据流**：前端交卷 → PracticeApi.submitPractice → PracticeServiceImpl： 后端按 questionId 回源题目并复核判分，写入 t_practice_record + t_practice_detail， 错题（is_correct=0）后续供错题本查询。

  **学生答案格式**：items[i].answer 为前端答案结构 `{type: 'option', optionId} / {type: 'options', optionIds: []} / {type: 'text', text}`。

### `shared/src/main/java/cn/wisestar/server/domain/dto/ProjectPartnerQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class ProjectPartnerQuery`
- 注解: @Data, @EqualsAndHashCode

### `shared/src/main/java/cn/wisestar/server/domain/dto/ProjectPartnerRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class ProjectPartnerRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/ProjectPartnerView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class ProjectPartnerView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/ProjectQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class ProjectQuery`
- 注解: @Data, @EqualsAndHashCode
- 方法:
  - `public void setAnswer(LinkedHashMap<String, Object> answer)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/ProjectRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class ProjectRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/ProjectSetting.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class ProjectSetting`
- 注解: @Data, @NoArgsConstructor, @AllArgsConstructor, @Builder
- 内部类型: enum TriggerType
- 注入/字段: Integer status
- 方法:
  - `public UniqueLimitSetting()`
  - `public UniqueLimitSetting(Integer limitNum, AnswerFreqEnum limitFreq)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/ProjectView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class ProjectView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/PublicAnswerView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PublicAnswerView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/PublicDictRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PublicDictRequest`
- 注解: @Data
- 注入/字段: String dictCode

### `shared/src/main/java/cn/wisestar/server/domain/dto/PublicDictView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PublicDictView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/PublicExamRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PublicExamRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/PublicExamResult.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PublicExamResult`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/PublicLinkRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PublicLinkRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/PublicLinkResult.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PublicLinkResult`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/PublicProjectView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PublicProjectView`
- 注解: @Data
- **类说明**：
  答卷页面

### `shared/src/main/java/cn/wisestar/server/domain/dto/PublicQueryRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PublicQueryRequest`
- 注解: @Data
- **类说明**：
  公开查询支持两种答案查询方式，1、通过查询表单提交参数 2、通过 url 参数

### `shared/src/main/java/cn/wisestar/server/domain/dto/PublicQueryVerifyView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PublicQueryVerifyView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/PublicQueryView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PublicQueryView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/PublicStatisticsView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class PublicStatisticsView`
- 注解: @Data
- 方法:
  - `public QuestionStatistics()`
  - `public QuestionStatistics(int count, List<OptionStatistics> optionStatistics)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/RegisterRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RegisterRequest`
- 注解: @Data
- 注入/字段: String username, String password

### `shared/src/main/java/cn/wisestar/server/domain/dto/RegisterRoleView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RegisterRoleView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/RepoAssignRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RepoAssignRequest`
- 注解: @Data
- **类说明**：
  学员-题库分配请求（分配/删除/学员标签）
  **所属模块**：shared 模块 DTO 包（cn.wisestar.server.domain.dto）。

  **功能**：管理端「题库分配」页面请求体—— assign：userId + repoIds 批量手动分配；delete：ids 删除分配记录； userTags：userId + tags 保存学员标签（按标签自动分配用）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/RepoAssignView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RepoAssignView`
- 注解: @Data
- **类说明**：
  学员-题库分配记录视图（管理端查询分配列表用）
  **所属模块**：shared 模块 DTO 包（cn.wisestar.server.domain.dto）。

  **功能**：管理端「题库分配」页面展示某学员已分配的题库列表， 含学员信息与题库信息，便于增删分配。

### `shared/src/main/java/cn/wisestar/server/domain/dto/RepoBindLocationView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RepoBindLocationView`
- 注解: @Data
- **类说明**：
  练习详情页「知识绑定回显」数据：某练习被挂载到的章节/小节位置。

### `shared/src/main/java/cn/wisestar/server/domain/dto/RepoQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RepoQuery`
- 注解: @Data, @EqualsAndHashCode

### `shared/src/main/java/cn/wisestar/server/domain/dto/RepoQuestionTypeTotalView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RepoQuestionTypeTotalView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/RepoRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RepoRequest`
- 注解: @Data
- **类说明**：
  题库请求 DTO（同时复用为"题库题目导出"的查询参数）。 【类职责】 承载题库的创建/更新请求参数，以及题库题目导出（RepoApi.exportRepoQuestions → RepoServiceImpl.exportRepoQuestions）的筛选条件。 【被谁调用】 - RepoApi.createRepo / updateRepo：题库 CRUD（@RequestBody JSON） - RepoApi.exportRepoQuestions：导出参数（GET query 参数绑定，除 id 外其余为题目筛选） 【数据流】 前端题库管理/题目管理页 → RepoApi → RepoService/RepoServiceImpl → t_repo / t_template 表 【导出筛选说明】 导出时除 id（题库 id，可空=导出全部）外，name/questionType/subject/chapter/ knowledgePoint/difficulty 作为题目维度筛选条件，与题目管理页筛选栏一致（AND 关系）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/RepoTemplateRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RepoTemplateRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/RepoView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RepoView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/ReportData.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class ReportData`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/RoleQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RoleQuery`
- 注解: @Data, @EqualsAndHashCode

### `shared/src/main/java/cn/wisestar/server/domain/dto/RoleRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RoleRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/RoleView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class RoleView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/SelectCategoryRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class SelectCategoryRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/SelectDeptRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class SelectDeptRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/SelectPositionRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class SelectPositionRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/SelectRepoRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class SelectRepoRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/SelectRoleRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class SelectRoleRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/SelectTagRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class SelectTagRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/SelectTemplateRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class SelectTemplateRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/SelectUserRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class SelectUserRequest`
- 注解: @Data, @EqualsAndHashCode
- 方法:
  - `public String getName()`

### `shared/src/main/java/cn/wisestar/server/domain/dto/SurveySchema.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class SurveySchema`
- 注解: @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
- 内部类型: enum QuestionType, enum DataType, enum ExamMatchRule, enum RejectOtherOption, enum ExamScoreMode, enum LinkType, enum SchemaMode
- 方法:
  - `public static EnumSet<QuestionType> dataType()`
  - `public static EnumSet<QuestionType> voidType()`
  - `public static EnumSet<QuestionType> examType()`
  - `public DataSource()`
  - `public DataSource(String label, String value, List<DataSource> children)`
  - `public SurveySchema deepCopy()`

### `shared/src/main/java/cn/wisestar/server/domain/dto/SystemInfo.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class SystemInfo`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/SystemInfoRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class SystemInfoRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/TagQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class TagQuery`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/TemplateQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class TemplateQuery`
- 注解: @Data, @EqualsAndHashCode

### `shared/src/main/java/cn/wisestar/server/domain/dto/TemplateRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class TemplateRequest`
- 注解: @Data, @Builder, @AllArgsConstructor, @NoArgsConstructor

### `shared/src/main/java/cn/wisestar/server/domain/dto/TemplateTagTotalView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class TemplateTagTotalView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/TemplateView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class TemplateView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/UpdateUserRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UpdateUserRequest`
- 注解: @Data
- 注入/字段: String fullName

### `shared/src/main/java/cn/wisestar/server/domain/dto/UploadFileRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UploadFileRequest`
- 注解: @Data
- 注入/字段: Boolean publicUpload

### `shared/src/main/java/cn/wisestar/server/domain/dto/UserBookQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UserBookQuery`
- 注解: @Data, @EqualsAndHashCode
- 注入/字段: Date startDate, Date endDate

### `shared/src/main/java/cn/wisestar/server/domain/dto/UserBookRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UserBookRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/UserBookView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UserBookView`
- 注解: @Data
- 注入/字段: Date createAt

### `shared/src/main/java/cn/wisestar/server/domain/dto/UserInfo.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UserInfo`
- 注解: @Data
- 注入/字段: Integer status, String username, String password
- 方法:
  - `public UserInfo()`
  - `public UserInfo(String username)`
  - `public String getUsername()`
  - `public boolean isAccountNonExpired()`
  - `public boolean isAccountNonLocked()`
  - `public boolean isCredentialsNonExpired()`
  - `public boolean isEnabled()`
  - `public Collection<? extends GrantedAuthority> getAuthorities()`
  - `public String getPassword()`
  - `public List<String> getAuthorityList()`
  - `public UserInfo simpleMode()`

### `shared/src/main/java/cn/wisestar/server/domain/dto/UserOverview.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UserOverview`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/UserPositionRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UserPositionRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/UserPositionView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UserPositionView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/UserQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UserQuery`
- 注解: @Data, @EqualsAndHashCode

### `shared/src/main/java/cn/wisestar/server/domain/dto/UserRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UserRequest`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/UserTokenView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UserTokenView`
- 注解: @Data
- 方法:
  - `public UserTokenView()`
  - `public UserTokenView(String userId)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/UserView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class UserView`
- 注解: @Data

### `shared/src/main/java/cn/wisestar/server/domain/dto/WhiteListRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class WhiteListRequest`
- 注解: @Data
- **类说明**：
  问卷/考试参与白名单设置请求 DTO。 【类职责】 承载"设置问卷参与白名单"接口（ProjectApi 的 setWhiteList 类接口）的请求参数， 支持两种方式指定白名单人员：手工勾选用户列表（selected）或上传用户文件（file）。 【被谁调用】 - 写入方：ProjectApi 白名单设置接口，将本 DTO 转为 ProjectPartner / 白名单记录保存 - 来源：前端项目管理页的"白名单设置"弹窗表单 【依赖什么】 - MultipartFile：Spring MVC 文件上传对象，仅使用 selected 方式时可为空 【数据流】 前端弹窗（勾选用户或上传文件）→ ProjectApi（HTTP multipart/form-data 或 JSON） → 解析为 WhiteListRequest → Service 层落库（t_project_partner / 白名单相关表）

### `shared/src/main/java/cn/wisestar/server/domain/dto/WrongQuestionQuery.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class WrongQuestionQuery`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  错题列表查询条件。
  **用途**：管理端「错题库管理」页面的筛选条件 + 分页参数 （GET /api/practice/wrong-list）。

  **筛选维度**：题库、题型、关键词（题目标题/学员姓名模糊）、 做错时间范围；keyword 同时匹配题目名与学员姓名，便于按人查错题。

### `shared/src/main/java/cn/wisestar/server/domain/dto/WrongQuestionView.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class WrongQuestionView`
- 注解: @Data
- **类说明**：
  错题聚合视图（错题库管理列表项）。
  **数据来源**：t_practice_detail（is_correct = 0）按 「题目 + 学员」聚合：同一学员反复做错同一题，合并为一条并累计错误次数。

  **用途**：管理端「错题库管理」页面列表数据；学员端错题本（阶段二） 可复用同一聚合语义，仅增加按当前用户过滤。

### `shared/src/main/java/cn/wisestar/server/domain/dto/WrongReasonRequest.java`
- 包: `cn.wisestar.server.domain.dto`
- 类型: `class WrongReasonRequest`
- 注解: @Data
- **类说明**：
  错题错误归因请求（学员在查看错题时标注）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/english/EnglishAiPackQuery.java`
- 包: `cn.wisestar.server.domain.dto.english`
- 类型: `class EnglishAiPackQuery`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  AI 单元内容包查询 DTO。

### `shared/src/main/java/cn/wisestar/server/domain/dto/english/EnglishAiPackView.java`
- 包: `cn.wisestar.server.domain.dto.english`
- 类型: `class EnglishAiPackView`
- 注解: @Data
- **类说明**：
  AI 单元内容包视图 DTO。
  content 为 AI 生成的整套内容 JSON（含单词/语法讲解/例句/练习）， 列表接口为控制传输体积会置空，详情接口返回完整内容。

### `shared/src/main/java/cn/wisestar/server/domain/dto/english/EnglishSentenceQuery.java`
- 包: `cn.wisestar.server.domain.dto.english`
- 类型: `class EnglishSentenceQuery`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  英语句子查询 DTO。

### `shared/src/main/java/cn/wisestar/server/domain/dto/english/EnglishSentenceView.java`
- 包: `cn.wisestar.server.domain.dto.english`
- 类型: `class EnglishSentenceView`
- 注解: @Data
- **类说明**：
  英语句子视图 DTO。

### `shared/src/main/java/cn/wisestar/server/domain/dto/english/EnglishUnitProgressView.java`
- 包: `cn.wisestar.server.domain.dto.english`
- 类型: `class EnglishUnitProgressView`
- 注解: @Data
- **类说明**：
  英语单元进度视图 DTO（学员端学习中心）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/english/EnglishUnitQuery.java`
- 包: `cn.wisestar.server.domain.dto.english`
- 类型: `class EnglishUnitQuery`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  英语单元查询 DTO。

### `shared/src/main/java/cn/wisestar/server/domain/dto/english/EnglishUnitView.java`
- 包: `cn.wisestar.server.domain.dto.english`
- 类型: `class EnglishUnitView`
- 注解: @Data
- **类说明**：
  英语单元视图 DTO。

### `shared/src/main/java/cn/wisestar/server/domain/dto/english/EnglishWordQuery.java`
- 包: `cn.wisestar.server.domain.dto.english`
- 类型: `class EnglishWordQuery`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  英语单词查询 DTO。

### `shared/src/main/java/cn/wisestar/server/domain/dto/english/EnglishWordView.java`
- 包: `cn.wisestar.server.domain.dto.english`
- 类型: `class EnglishWordView`
- 注解: @Data
- **类说明**：
  英语单词视图 DTO。

### `shared/src/main/java/cn/wisestar/server/domain/dto/english/ImportResult.java`
- 包: `cn.wisestar.server.domain.dto.english`
- 类型: `class ImportResult`
- 注解: @Data
- **类说明**：
  单词导入结果 DTO。
- 方法:
  - `public ImportResult()`
  - `public ImportResult(Integer total, Integer success, Integer failed, java.util.List<String> errors)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/english/PackSyncResult.java`
- 包: `cn.wisestar.server.domain.dto.english`
- 类型: `class PackSyncResult`
- 注解: @Data
- **类说明**：
  内容包同步到词库/语法库的结果统计。
- 方法:
  - `public PackSyncResult(int wordsAdded, int wordsUpdated, int grammarSynced)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/english/ReviewSessionView.java`
- 包: `cn.wisestar.server.domain.dto.english`
- 类型: `class ReviewSessionView`
- 注解: @Data
- **类说明**：
  英语智能复习条目视图 DTO（单词与句子混合队列）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/ChapterImportRequest.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class ChapterImportRequest`
- 注解: @Data
- **类说明**：
  章节批量导入请求（multipart 表单绑定）。
  Excel 列格式（首行为表头自动跳过）：学科名 / 章节名 / 图标(选填) / 排序(选填，默认 1)。 归属学科由「学科名」列按 t_subject.name 匹配定位。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/ChapterRepoRequest.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class ChapterRepoRequest`
- 注解: @Data
- **类说明**：
  章节-题库绑定请求。
  将题库管理（t_repo）中的题库绑定到章节（全量替换式保存）： 传入完整的 repoIds 列表，后端先清空旧绑定再写入新绑定。 章节的题库仅能从题库管理选择，不能在此新增。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/ChapterRequest.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class ChapterRequest`
- 注解: @Data
- **类说明**：
  章节请求/查询（对应 t_chapter）。
  GET /list 时以 GET 参数绑定（subjectId 筛选）；POST create/update 时以 body 提交。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/ChapterView.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class ChapterView`
- 注解: @Data
- **类说明**：
  章节视图（返回前端展示用，含小节数统计）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/ImportResultView.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class ImportResultView`
- 注解: @Data
- **类说明**：
  批量导入结果视图。
  imported：实际新增条数；skipped：跳过总条数（= missingRequired + sectionNotFound + duplicate）。 分类字段由章节/小节/知识点导入共同填充，便于前端提示具体跳过原因。
- 方法:
  - `public ImportResultView()`
  - `public ImportResultView(int imported, int skipped)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/KnowledgePointImportRequest.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class KnowledgePointImportRequest`
- 注解: @Data
- **类说明**：
  知识点批量导入请求（multipart 表单绑定）。
  Excel 列格式（首行为表头自动跳过）： 学科名 / 章节名 / 小节名 / 知识点名 / 排序(选填，默认 1) / 年级(选填) / 学期(选填，上/下) / 内容设置(选填，仅文本)。 归属由「学科名 + 章节名 + 小节名」按 t_subject.name + t_chapter.name + t_section.name 匹配定位； 年级/学期留空则不写入（列表无需年级学期也能正常显示）； 内容设置不支持图片，整格文本作为一条讲解要点（`{"points":[文本]`}）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/KnowledgePointQuery.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class KnowledgePointQuery`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  知识点分页查询参数（对应 t_knowledge_point）。
  GET /list 以 GET 参数绑定。三级下拉条件均可选： 都不传 → 全量分页；只传 subjectId → 该学科全部知识点； 再传 chapterId → 缩小到该章节；传全 sectionId → 该小节知识点。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/KnowledgePointQuestionRequest.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class KnowledgePointQuestionRequest`
- 注解: @Data
- **类说明**：
  知识点-题目绑定请求。
  将题目库（t_template）中的题目绑定到知识点（全量替换式保存）： 传入完整的 questionIds 列表，后端先清空旧绑定再写入新绑定。 题目不能在此新增，仅能从未绑定的题目库中选择。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/KnowledgePointRequest.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class KnowledgePointRequest`
- 注解: @Data
- **类说明**：
  知识点请求（对应 t_knowledge_point）。
  content 为 JSON 字符串（讲解要点数组：{"points":["要点1"]}）， imageUrl 为知识点配图地址（复用 /api/file/create 上传返回的 previewUrl，可为空）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/KnowledgePointView.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class KnowledgePointView`
- 注解: @Data
- **类说明**：
  知识点视图（返回前端展示用）。
  含三级归属名称（学科/章节/小节，列表直接展示）与绑定题目数统计； 题目从题目库（t_template）选择绑定，不能在此新增。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/SectionImportRequest.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class SectionImportRequest`
- 注解: @Data
- **类说明**：
  小节批量导入请求（multipart 表单绑定）。
  Excel 列格式（首行为表头自动跳过）：学科名 / 章节名 / 小节名 / 年级(选填) / 学期(选填)。 归属由「学科名 + 章节名」按 t_subject.name + t_chapter.name 匹配定位； 排序不参与导入，由系统按所属章节自动追加。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/SectionPracticeConfig.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class SectionPracticeConfig`
- 注解: @Data
- **类说明**：
  小节练习配置（t_section.practice JSON 的强类型视图）。
  字段缺省策略：mode 缺省 normal；passRate 缺省 80 并收敛到 0-100； unlockNext 缺省 false；preview 缺省题量 3、题型不限。 用于专项练习、小节通关与知识点预习统一出题引擎的策略来源。
- 方法:
  - `public boolean isRandom()`
    是否为随机出题模式
  - `public void applyDefaults()`
    补全缺省值（解析失败或字段缺失时调用）

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/SectionRepoRequest.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class SectionRepoRequest`
- 注解: @Data
- **类说明**：
  小节-题库绑定请求。
  将题库管理（t_repo）中的题库绑定到小节（全量替换式保存）： 传入完整的 repoIds 列表，后端先清空旧绑定再写入新绑定。 小节的题库仅能从题库管理选择，不能在此新增。

  usageByRepo 为可选的用途标记（repoId → preview/practice/both）； 未出现的 repoId 在保存时复用其既有用途，仍缺失时按通用处理。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/SectionRepoView.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class SectionRepoView`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  小节已绑定题库视图。
  在题库信息（RepoView）基础上追加用途标记，供管理端 「练习设置」回显每个绑定题库的用途（preview/practice/both）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/SectionRequest.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class SectionRequest`
- 注解: @Data
- **类说明**：
  小节请求/查询（对应 t_section）。
  content/practice 为 JSON 字符串（内容设置与练习设置）， 由前端序列化提交、前端解析展示，后端仅做透传存储。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/SectionView.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class SectionView`
- 注解: @Data
- **类说明**：
  小节视图（返回前端展示用，含内容/练习设置状态与知识点数统计）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/SubjectRequest.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class SubjectRequest`
- 注解: @Data
- **类说明**：
  学科视图（对应 t_subject，知识管理板块一级维度）。
  查询/创建/更新共用本类：GET 参数绑定（list 时可不传）、 POST body（create/update 含 id）。字段与数据库列一一对应。

### `shared/src/main/java/cn/wisestar/server/domain/dto/knowledge/SubjectView.java`
- 包: `cn.wisestar.server.domain.dto.knowledge`
- 类型: `class SubjectView`
- 注解: @Data
- **类说明**：
  学科视图（返回前端展示用，含章节数统计）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/mall/MallGoodsRequest.java`
- 包: `cn.wisestar.server.domain.dto.mall`
- 类型: `class MallGoodsRequest`
- 注解: @Data
- **类说明**：
  积分商城商品请求（后台商品管理）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/mall/MallGoodsView.java`
- 包: `cn.wisestar.server.domain.dto.mall`
- 类型: `class MallGoodsView`
- 注解: @Data
- **类说明**：
  积分商城商品视图。

### `shared/src/main/java/cn/wisestar/server/domain/dto/mall/MallOrderRequest.java`
- 包: `cn.wisestar.server.domain.dto.mall`
- 类型: `class MallOrderRequest`
- 注解: @Data
- **类说明**：
  学币兑换订单请求（学员发起兑换 / 老师核销 / 列表筛选）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/mall/MallOrderView.java`
- 包: `cn.wisestar.server.domain.dto.mall`
- 类型: `class MallOrderView`
- 注解: @Data
- **类说明**：
  学币兑换订单视图（学员端兑换记录 / 老师端核销申请）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/OrderQuery.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class OrderQuery`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  订单分页查询（学员管理模块）。
  GET 参数绑定：studentId 精确筛选；studentName 模糊匹配学员姓名； status 状态筛选（1 生效 0 作废，不传查全部）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/OrderRequest.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class OrderRequest`
- 注解: @Data
- **类说明**：
  订单请求（学员管理模块）。
  create 用：studentId 必传；subjectIds 多选学科ID、grades 多选年级， 服务端按学科×年级笛卡尔积展开写入权限表。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/OrderView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class OrderView`
- 注解: @Data
- **类说明**：
  订单视图（学员管理模块，返回前端展示用）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/PracticeEvaluationContext.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class PracticeEvaluationContext`
- 注解: @Data
- **类说明**：
  一次练习的学情评价上下文（跨模块传递，避免 shared 依赖 rdbms 实体）。
  由练习交卷方（rdbms）从逐题明细与题目 schema 组装后交给评价服务。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/RewardContext.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class RewardContext`
- 注解: @Data
- **类说明**：
  奖励结算上下文（统一结算入参）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/SectionPassResult.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class SectionPassResult`
- 注解: @Data
- **类说明**：
  小节通关结果（交卷响应与小节列表展示共用）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentActivityRequest.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentActivityRequest`
- 注解: @Data
- **类说明**：
  学员实时位置上报请求（学员端路由变化/进入习题时调用）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentActivityView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentActivityView`
- 注解: @Data
- **类说明**：
  学员实时位置视图（后台老师监控用）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentCheckinView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentCheckinView`
- 注解: @Data
- **类说明**：
  学员每日签到视图。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentCoinRequest.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentCoinRequest`
- 注解: @Data
- **类说明**：
  学员学币发放请求（老师手动加学币）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentCoinsView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentCoinsView`
- 注解: @Data
- **类说明**：
  学员本学期学习币视图（分学科，单科上限 10000）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentKnowledgeDetailView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentKnowledgeDetailView`
- 注解: @Data
- **类说明**：
  知识点详情视图（掌握度/评级/薄弱/预习状态）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentLearningCompleteRequest.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentLearningCompleteRequest`
- 注解: @Data
- **类说明**：
  学习完成统一结算请求（预习/练习/试炼/错题订正等）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentOnlineChestClaimRequest.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentOnlineChestClaimRequest`
- 注解: @Data
- **类说明**：
  在线宝箱领取请求。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentOnlineChestClaimView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentOnlineChestClaimView`
- 注解: @Data
- **类说明**：
  在线宝箱领取结果（含最新宝箱状态）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentOnlineChestView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentOnlineChestView`
- 注解: @Data
- **类说明**：
  学员端在线时长宝箱视图（首页悬浮窗用）。
  在线时长取自当日学习心跳会话累计时长，宝箱分 30/60/120 分钟三档， 达档可领取学习币，按「学员 + 档位 + 日期」幂等。
- 方法:
  - `public Chest()`
  - `public Chest(int tierMinutes, int coins, String state)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentPermissionView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentPermissionView`
- 注解: @Data
- **类说明**：
  学员有效权限视图（多条有效订单合并，expire_at > NOW() 才生效）。
  供学员端按订单授予范围过滤可访问内容：学科 + 年级 + 教材版本。
- 方法:
  - `public SubjectItem()`
  - `public SubjectItem(String id, String name)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentPointsView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentPointsView`
- 注解: @Data
- **类说明**：
  个人中心-积分板块视图（简单明了呈现积分评价）。
- 方法:
  - `public RuleItem()`
  - `public RuleItem(String actionType, String label, int points, int coins)`
  - `public RecordItem()`
  - `public RecordItem(String label, int points, int coins, java.util.Date at)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentPreviewCompleteRequest.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentPreviewCompleteRequest`
- 注解: @Data
- **类说明**：
  学员预习完成请求（知识点预习讲完后的「预习完成」按钮）。
  小节预习传 sectionId，知识点预习传 knowledgePointId；同一目标仅首次结算奖励。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentPreviewCompleteView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentPreviewCompleteView`
- 注解: @Data
- **类说明**：
  学员预习完成视图（奖励结算结果）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentProfileView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentProfileView`
- 注解: @Data
- **类说明**：
  学员档案视图（个人中心用）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentQuery.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentQuery`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  学员分页查询（学员管理模块）。
  GET 参数绑定：name/studentNo/phone 为可选筛选条件，均为模糊匹配。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentQuestionView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentQuestionView`
- 注解: @Data
- **类说明**：
  学员端题目视图（剥离标准答案，防作弊）。
  作答时前端提交 `questionId + answer` 到 /api/practice/submit， 由后端基于题库标准答案判分。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentRequest.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentRequest`
- 注解: @Data
- **类说明**：
  学员请求（学员管理模块）。
  create/update 共用：新增时 id 为空；学号 studentNo 由系统自动生成， update 接口不接收 studentNo（学号不可人工修改）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentStatsView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentStatsView`
- 注解: @Data
- **类说明**：
  学员学习统计视图（首页学习统计真实化，基于真实练习记录聚合）。
- 方法:
  - `public SubjectCoins()`
  - `public SubjectCoins(String subjectName, int coins)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentStudyProgressView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentStudyProgressView`
- 注解: @Data
- **类说明**：
  学员学科学习进度视图（章节 → 知识点掌握度/评级/薄弱）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentSubjectView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentSubjectView`
- 注解: @Data
- **类说明**：
  学员端学科视图（按订单有效权限过滤）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentSupervisionView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentSupervisionView`
- 注解: @Data
- **类说明**：
  学员督学视图（教师端查看学员学习状态）

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentTaskCompleteView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentTaskCompleteView`
- 注解: @Data
- **类说明**：
  学员任务完成结果。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentTaskDTO.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentTaskDTO`
- 注解: @Data
- **类说明**：
  学员任务分配 DTO

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentTaskPublishDTO.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentTaskPublishDTO`
- 注解: @Data
- **类说明**：
  学员任务发布 DTO（学管师向学员下发纯文本任务）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentTaskQuery.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentTaskQuery`
- 注解: @Data, @EqualsAndHashCode
- **类说明**：
  学员任务分页查询 DTO（管理端任务发布列表）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentTaskView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentTaskView`
- 注解: @Data
- **类说明**：
  学员任务视图

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentTodayView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentTodayView`
- 注解: @Data
- **类说明**：
  学员端今日总览 + 积分获取引导视图（主页用）。
- 方法:
  - `public Guide()`
  - `public Guide(String actionType, String label, int points, int coins, boolean done, String progress, String target)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentView`
- 注解: @Data
- **类说明**：
  学员视图（学员管理模块，返回前端展示用）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentWeakConquerRequest.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentWeakConquerRequest`
- 注解: @Data
- **类说明**：
  薄弱点攻克请求。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentWeakConquerView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentWeakConquerView`
- 注解: @Data
- **类说明**：
  薄弱点攻克结果视图。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentWeakView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentWeakView`
- 注解: @Data
- **类说明**：
  薄弱知识点视图。
- 方法:
  - `public StudentWeakView()`
  - `public StudentWeakView(String kpId, String name, String subjectId, int mastery)`

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentWrongRedoRequest.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentWrongRedoRequest`
- 注解: @Data
- **类说明**：
  错题重做请求。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudentWrongRedoView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudentWrongRedoView`
- 注解: @Data
- **类说明**：
  错题重做结果视图。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudyHeartbeatRequest.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudyHeartbeatRequest`
- 注解: @Data
- **类说明**：
  学员学习心跳请求。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudyHeartbeatView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudyHeartbeatView`
- 注解: @Data
- **类说明**：
  学员学习心跳响应。

### `shared/src/main/java/cn/wisestar/server/domain/dto/student/StudySummaryView.java`
- 包: `cn.wisestar.server.domain.dto.student`
- 类型: `class StudySummaryView`
- 注解: @Data
- **类说明**：
  学习总结视图（学员本人 / 教师查看）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/task/StudentTaskView.java`
- 包: `cn.wisestar.server.domain.dto.task`
- 类型: `class StudentTaskView`
- 注解: @Data
- **类说明**：
  学员端当日任务视图（含完成状态与正确率）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/task/TaskRequest.java`
- 包: `cn.wisestar.server.domain.dto.task`
- 类型: `class TaskRequest`
- 注解: @Data
- **类说明**：
  今日任务请求（后台老师布置）。

### `shared/src/main/java/cn/wisestar/server/domain/dto/task/TaskView.java`
- 包: `cn.wisestar.server.domain.dto.task`
- 类型: `class TaskView`
- 注解: @Data
- **类说明**：
  今日任务视图（后台管理）。

### `shared/src/main/java/cn/wisestar/server/service/AiService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface AiService`
- **类说明**：
  AI 服务接口（TTS 语音 + DALL-E 图片 + GPT 文本）。

### `shared/src/main/java/cn/wisestar/server/service/AnalysisService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface AnalysisService`
- **类说明**：
  学生答题情况分析服务接口（AnalysisService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。 为 AI 自习室系统新增的统计分析能力。

  **类职责**：定义"学生答题情况"的统计分析接口，按知识点维度 聚合学生的答题记录，输出答题次数、正确次数与正确率，用于 学习情况诊断与知识点画像。

  **实现类**：`cn.wisestar.server.impl.AnalysisServiceImpl` （rdbms 模块，@Service 注册），内部通过 AnswerDetailMapper 查询 答题明细表（t_answer_detail），在内存中按 "学科 subject - 章节 chapter - 知识点 knowledgePoint" 聚合； 一道题可挂多个知识点（逗号分隔），会拆开分别计入各知识点。

  **调用方**：`cn.wisestar.server.api.AnalysisApi` （api 模块 Controller）：

  - GET /api/analysis/knowledge-point/stats → knowledgePointStats(KnowledgePointQuery)
  - GET /api/analysis/knowledge-point/student-profile → studentProfile(String)

  **数据流**：前端 GET 请求（携带筛选条件）→ AnalysisApi → 本接口 → AnalysisServiceImpl（解析 studentId 为空时取当前登录用户） → AnswerDetailMapper 查询 → 聚合为 List → 返回前端。

### `shared/src/main/java/cn/wisestar/server/service/AnswerService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface AnswerService`
- **类说明**：
  答卷服务接口（AnswerService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供答卷（Answer）的管理与回收能力：答卷分页、详情、 计数、保存（提交/编辑）、删除、回收站（已删除列表/批量销毁/恢复）、 附件/答卷 Excel 下载、答卷上传、历史练习查询。实现类位于 rdbms 模块 （AnswerServiceImpl）。

  **内置 default 方法**：

  - #parseClientInfo：从当前请求解析答卷人客户端信息（User-Agent、 IP、限制 Cookie），用于答卷元数据记录与作答频率限制；
  - #download：按下载类型分发到 downloadSurvey（答卷 Excel）或 downloadAttachment（附件），统一拼装 Content-Disposition 响应头。

### `shared/src/main/java/cn/wisestar/server/service/CampusScopeService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface CampusScopeService`
- **类说明**：
  校区数据权限解析服务。
  **所属模块**：shared 模块服务接口包。实现类位于 rdbms 模块（CampusScopeServiceImpl）。

  **类职责**：解析当前登录用户可见校区范围（ALL / EMPTY / SCOPED）， 供学员管理、订单、督学等服务在列表查询与单对象访问处统一施加过滤， 使校长/教务/学管师只能看到自己绑定校区的学员业务数据。

### `shared/src/main/java/cn/wisestar/server/service/CampusService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface CampusService`
- **类说明**：
  校区服务接口（行政管理-校区管理）。
  **所属模块**：shared 模块服务接口包。实现类位于 rdbms 模块（CampusServiceImpl）。

  **类职责**：校区档案 CRUD 与统计；校区列表/下拉均遵守当前账号的 校区数据权限范围（校长/教务/学管师仅见自己绑定校区）。

### `shared/src/main/java/cn/wisestar/server/service/ChapterService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface ChapterService`
- **类说明**：
  章节管理服务（知识管理板块二级维度）。

### `shared/src/main/java/cn/wisestar/server/service/CheckinService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface CheckinService`
- **类说明**：
  学员每日签到服务。
  签到固定发放学习币，每个自然日仅一次，无连续签到天数概念。

### `shared/src/main/java/cn/wisestar/server/service/CommDictService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface CommDictService`
- **类说明**：
  通用字典服务接口（CommDictService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：通用字典（CommDict）相关服务的接口占位声明。 当前接口体为空，字典的查询/维护能力集中在 DictService （listDict/addDict/updateDict/deleteDict/selectDict）与 DictItemService 中提供。

### `shared/src/main/java/cn/wisestar/server/service/DashboardService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface DashboardService`
- **类说明**：
  仪表盘服务接口（DashboardService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供用户自定义仪表盘（首页/项目概要页布局配置）的查询与 保存能力。仪表盘内容为前端可配置的卡片/模块布局。实现类位于 rdbms 模块 （DashboardServiceImpl）。

### `shared/src/main/java/cn/wisestar/server/service/DeptService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface DeptService`
- **类说明**：
  组织机构（部门）服务接口（DeptService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供组织机构的树形管理能力：部门列表（树）、部门详情、 新增/修改/删除部门、部门排序。部门用于组织用户归属与数据权限范围。 实现类位于 rdbms 模块（DeptServiceImpl）。

  **缓存设计**：部门树使用 CacheConsts#deptCacheName 缓存： 仅查询全部机构（参数为 null）时启用缓存（key 由 cn.wisestar.server.core.cache.DeptKeyGenerator 生成）， 任何增删改/排序操作均全量清空该缓存（allEntries=true）保证一致性。

### `shared/src/main/java/cn/wisestar/server/service/DictItemService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface DictItemService`
- **类说明**：
  字典项服务接口（DictItemService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：字典明细项（CommDictItem）相关服务的接口占位声明。 当前接口体为空，字典项的查询/维护能力集中在 DictService （listDictItem/saveOrUpdateDictItem/deleteDictItem/importDictItem）中提供。

### `shared/src/main/java/cn/wisestar/server/service/DictService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface DictService`
- **类说明**：
  字典服务接口（DictService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供通用字典（CommDict）与字典项（CommDictItem）的完整 维护能力：字典分页、字典 CRUD、字典项分页/新增/更新/删除/导入，以及 字典选择器数据源。字典用于问卷下拉选项、系统参数等可配置数据。 实现类位于 rdbms 模块（DictServiceImpl）。

  **调用方**：api 模块系统管理相关接口（SystemApi 中的字典管理端点）。

### `shared/src/main/java/cn/wisestar/server/service/EnglishAiPackService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface EnglishAiPackService`
- **类说明**：
  英语 AI 单元内容包服务（生成/保存/同步词库与语法库）。

### `shared/src/main/java/cn/wisestar/server/service/EnglishSentenceService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface EnglishSentenceService`
- **类说明**：
  英语句库服务（管理与学员共用）。

### `shared/src/main/java/cn/wisestar/server/service/EnglishStudentService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface EnglishStudentService`
- **类说明**：
  英语学员学习服务（学员端学习中心）。

### `shared/src/main/java/cn/wisestar/server/service/EnglishUnitService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface EnglishUnitService`
- **类说明**：
  英语单元目录服务（管理端与学员端共用）。

### `shared/src/main/java/cn/wisestar/server/service/EnglishWordManagerService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface EnglishWordManagerService`
- **类说明**：
  英语单词管理服务接口。

### `shared/src/main/java/cn/wisestar/server/service/EnglishWordService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface EnglishWordService`
- **类说明**：
  英语单词学习服务。

### `shared/src/main/java/cn/wisestar/server/service/EvaluationService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface EvaluationService`
- **类说明**：
  学员薄弱点·学习结果评价服务。
  以 `t_practice_detail` 为唯一数据源，按知识点刷新掌握度、研判薄弱、自动攻克； 掌握度采用「最近 5 次练习正确率加权（越近权重越高）」计算。

### `shared/src/main/java/cn/wisestar/server/service/FileService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface FileService`
- **类说明**：
  文件服务接口（FileService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供文件（附件/图片）的上传、列表查询、读取（预览/下载）、 删除能力，以及模板文件下载。底层文件存储依赖 cn.wisestar.server.storage.StorageService， 文件元数据记录在数据库（t_file）。实现类位于 rdbms 模块（FileServiceImpl）。

  **调用方**：api 模块 FileApi（/api/file/**，GET 读取路径在 WebSecurityConfig 中配置为 permitAll，支持公开访问预览）。

### `shared/src/main/java/cn/wisestar/server/service/KnowledgePointService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface KnowledgePointService`
- **类说明**：
  知识点管理服务（知识管理板块最小学习单元）。

### `shared/src/main/java/cn/wisestar/server/service/MallGoodsService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface MallGoodsService`
- **类说明**：
  积分商城商品服务。

### `shared/src/main/java/cn/wisestar/server/service/MallOrderService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface MallOrderService`
- **类说明**：
  学币兑换订单服务。
  学员在商城兑换商品后生成订单并「暂时扣除」学币、下发随机 6 位核销码； 老师在核销页凭核销码完成订单（学币正式扣除），订单记录永久保留。

### `shared/src/main/java/cn/wisestar/server/service/OnlineChestService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface OnlineChestService`
- **类说明**：
  在线时长宝箱服务。
  以自然日心跳在线会话累计时长为进度，30/60/120 分钟三档；领取复用统一奖励账本， 按「学员 + 档位 + 日期」幂等，不新增表、不新增心跳链路。

### `shared/src/main/java/cn/wisestar/server/service/OrderService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface OrderService`
- **类说明**：
  学员订单服务（学员管理模块）。
  **定位**：为已注册学员创建订单并配置学科/年级/教材版本/账号时长， 订单写入同时按学科×年级笛卡尔积展开写入学员权限表（t_student_permission）， 供学员端按有效期鉴权。

### `shared/src/main/java/cn/wisestar/server/service/PositionService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface PositionService`
- **类说明**：
  岗位服务接口（PositionService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供岗位（Position）的管理能力：分页列表、新增、修改、 删除，以及岗位选择器数据源。岗位用于定义用户的数据权限范围 （本人/本部门/全部等，见 cn.wisestar.server.core.constant.AppConsts.DataPermissionTypeEnum）。 实现类位于 rdbms 模块（PositionServiceImpl）。

### `shared/src/main/java/cn/wisestar/server/service/PracticeService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface PracticeService`
- **类说明**：
  练习服务（练习会话落库 / 错题标记 / 错题库查询）。
  **功能**：学员端交卷后，按提交的逐题作答结果回源题目并复核判分， 汇总写入 t_practice_record（练习会话）与 t_practice_detail（逐题明细，is_correct=0 即错题）； 管理端「错题库管理」按题目 × 学员聚合查询错题记录。

### `shared/src/main/java/cn/wisestar/server/service/ProjectPartnerService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface ProjectPartnerService`
- **类说明**：
  项目参与者服务接口（ProjectPartnerService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供项目参与者（答卷人/协作者）的管理能力：参与者分页、 添加/删除参与者、获取当前用户的可见项目权限集合（数据权限核心）、 参与者导出与批量导入。实现类位于 rdbms 模块（ProjectPartnerServiceImpl）。

  **缓存设计**：当前用户的"可见项目权限集合"（getProjectPerms）使用 CacheConsts#projectPermissionCacheName 缓存（key 为用户 id）， 添加/删除参与者时按当前用户 id 精确失效该缓存，保证权限变更实时生效。 该缓存被 cn.wisestar.server.core.aop.DataPermAspect 数据权限切面调用。

### `shared/src/main/java/cn/wisestar/server/service/ProjectService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface ProjectService`
- **类说明**：
  项目（问卷/考试）服务接口（ProjectService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供问卷/考试项目（Project）的完整管理能力：项目分页、 项目详情、新增/更新/删除项目、项目设置（ProjectSetting）获取、 回收站（已删除列表/批量销毁/恢复）。实现类位于 rdbms 模块（ProjectServiceImpl）。

  **缓存设计**：项目详情（getProject）使用 CacheConsts#projectCache 缓存（key 为项目 id，参数非空且结果非空时才缓存），更新/删除时按 id 精确失效缓存。

### `shared/src/main/java/cn/wisestar/server/service/RepoService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface RepoService`
- **类说明**：
  题目模板库（题库）服务接口（RepoService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供题库（Repo）与题库题目（模板）的管理能力：题库分页、 题库 CRUD、题库绑定/解绑模板、从题库挑题（随机问卷数据源）、从模板导入、 用户错题本（UserBook）管理、题库选择器、题库题目导出。 题库是 AI 自习室系统中知识点（subject/chapter/knowledgePoint/difficulty） 组织与复用试题的核心载体。实现类位于 rdbms 模块（RepoServiceImpl）。

### `shared/src/main/java/cn/wisestar/server/service/ReportService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface ReportService`
- **类说明**：
  报表数据服务接口（ReportService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供问卷项目的统计报表数据，供项目概要/数据看板页面 展示回收统计、每日回收数量趋势等。实现类位于 rdbms 模块 （ReportServiceImpl）。

  **调用方**：api 模块 ReportApi（GET /api/report/xxx 系列接口）。

### `shared/src/main/java/cn/wisestar/server/service/RewardService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface RewardService`
- **类说明**：
  学员积分·学币统一结算服务。
  所有奖励的合法性校验、学期幂等、单科 10000 上限、头衔晋升均在本服务内完成； 调用方只负责传入行为上下文并展示结果。

### `shared/src/main/java/cn/wisestar/server/service/RoleService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface RoleService`
- **类说明**：
  角色服务接口（RoleService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：角色（Role）相关业务。当前提供"角色选择器"数据源： 供新建/编辑用户、分配角色等场景下拉选择使用。 完整角色 CRUD 在 SystemService（getRoles/createRole/updateRole/deleteRole）中。

  **实现类**：rdbms 模块 RoleServiceImpl；**调用方**：api 模块用户/角色相关接口。

### `shared/src/main/java/cn/wisestar/server/service/SectionPassService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface SectionPassService`
- **类说明**：
  小节通关服务。
  交卷后按正确率与通关阈值判定是否通关，写入或更新 t_section_pass， 返回本次结果与历史最佳（最佳值单调、通关状态不可逆）。

### `shared/src/main/java/cn/wisestar/server/service/SectionPracticeService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface SectionPracticeService`
- **类说明**：
  小节练习配置服务。
  读取 t_section.practice JSON 并解析为 SectionPracticeConfig， 供出题引擎（专项练习/小节通关）与通关判定使用。解析失败时返回缺省配置。

### `shared/src/main/java/cn/wisestar/server/service/SectionService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface SectionService`
- **类说明**：
  小节管理服务（知识管理板块三级维度）。

### `shared/src/main/java/cn/wisestar/server/service/StudentService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface StudentService`
- **类说明**：
  学员管理服务（学员管理模块）。
  **定位**：学员主数据 CRUD。新增学员时自动生成「字母 + 6 位数字」学号并创建 学员登录账号（t_account，user_type=Student，初始密码 123456）。

### `shared/src/main/java/cn/wisestar/server/service/StudentSupervisionService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface StudentSupervisionService`
- **类说明**：
  学员督学服务

### `shared/src/main/java/cn/wisestar/server/service/StudentTaskService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface StudentTaskService`
- **类说明**：
  学员任务服务

### `shared/src/main/java/cn/wisestar/server/service/StudySessionService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface StudySessionService`
- **类说明**：
  学习会话服务。
  接收学员端心跳，按 30 分钟间隔续会话或开启新会话，累计时长达到 60 分钟时触发当日学习总结。

### `shared/src/main/java/cn/wisestar/server/service/StudySummaryService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface StudySummaryService`
- **类说明**：
  当日学习总结服务。
  聚合学员当日练习记录、答题明细、学习行为、掌握度与薄弱点数据，调用系统 AI 生成总结； AI 未启用或失败时降级为规则模板（model=rule）。

### `shared/src/main/java/cn/wisestar/server/service/SubjectService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface SubjectService`
- **类说明**：
  学科管理服务（知识管理板块一级维度）。

### `shared/src/main/java/cn/wisestar/server/service/SurveyService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface SurveyService`
- **类说明**：
  问卷（公开访问）服务接口（SurveyService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：定义问卷对外（答卷人/公开链接）访问所需的核心能力： 加载问卷、查看统计、提交/暂存答卷、公开查询（验证身份后查结果）、 加载字典、加载考试结果、加载关联链接等。这些能力服务于 无需登录 或 以答卷人身份 访问的场景。

  **实现类**：rdbms 模块 SurveyServiceImpl；**调用方**：api 模块 SurveyApi（/api/public/** 等公开端点，无需登录）。

### `shared/src/main/java/cn/wisestar/server/service/SystemService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface SystemService`
- **类说明**：
  系统管理服务接口（SystemService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供系统级管理能力：系统信息（名称/Logo/AI 设置）查询与更新、 角色 CRUD、权限码查询、角色与权限初始化。实现类位于 rdbms 模块 （SystemServiceImpl）。

  **缓存设计**：系统信息与 AI 设置使用 CacheConsts#commonCacheName 缓存（key 分别为 'systemInfo' 与 'aiInfo'），更新系统信息时通过 @Caching 同时失效这两个缓存 key。

### `shared/src/main/java/cn/wisestar/server/service/TagService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface TagService`
- **类说明**：
  标签服务接口（TagService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供标签的通用维护能力：为业务实体（模板/题库/问卷/考试） 批量添加标签、按实体删除标签、按条件查询标签。标签按 TagCategoryEnum 区分业务归属。实现类位于 rdbms 模块（TagServiceImpl）。

  **调用方**：模板管理、题库管理等模块在创建/更新实体时调用 batchAddTag / deleteTagByEntryId 维护标签关系；标签筛选项查询调用 selectTag。

### `shared/src/main/java/cn/wisestar/server/service/TaskService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface TaskService`
- **类说明**：
  今日任务服务。

### `shared/src/main/java/cn/wisestar/server/service/TemplateService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface TemplateService`
- **类说明**：
  问卷/问题模板服务接口（TemplateService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供问卷模板（Template）与题目模板的完整管理能力： 模板分页列表、详情、创建（含批量）、更新（含批量）、删除、模板选择器、 模板广场的分类与标签查询。模板可携带知识点四字段 （subject 学科 / chapter 章节 / knowledgePoint 知识点 / difficulty 难度）， 供 AI 自习室系统按知识点检索与统计。实现类位于 rdbms 模块 （TemplateServiceImpl）。

  **调用方**：api 模块 TemplateApi（路径前缀 ${api.prefix}/template）。

### `shared/src/main/java/cn/wisestar/server/service/UserService.java`
- 包: `cn.wisestar.server.service`
- 类型: `interface UserService`
- **类说明**：
  用户服务接口（UserService）。
  **所属模块**：shared 模块服务接口包（cn.wisestar.server.service）。

  **类职责**：提供用户（系统用户）的完整管理能力，并实现 Spring Security 的 UserDetailsService 作为认证用户来源：用户信息加载、分页查询、 新增/修改/删除、用户名查重、岗位分配、用户组、选择器、注册、用户概览、 导入、任务查询、验证码校验、历史任务查询等。实现类位于 rdbms 模块 （UserServiceImpl）。

  **安全相关**：loadUserByUsername / loadUserById 被 Spring Security 认证链路（cn.wisestar.server.core.config.WebSecurityConfig 与 cn.wisestar.server.core.security.JwtTokenFilter）调用，返回的 UserDetails 包含用户权限（角色）列表。

### `shared/src/main/java/cn/wisestar/server/storage/AbstractStorageService.java`
- 包: `cn.wisestar.server.storage`
- 类型: `class AbstractStorageService`
- 注解: @Data
- **类说明**：
  存储服务抽象基类（AbstractStorageService）。
  **所属模块**：shared 模块存储抽象包（cn.wisestar.server.storage）。

  **类职责**：实现 StorageService 接口中与具体存储介质无关的 公共逻辑——缩略图能力：

  - 从配置读取缩略图尺寸（宽/高，见 StorageProperties.ThumbImage， 默认 640x480）；
  - 生成缩略图文件路径：在原始文件名后缀前插入 `_宽x高`（如 xxx.jpg → xxx_640x480.jpg，见 #getThumbImageFilePath）；
  - 基于 Thumbnailator 在内存中生成缩略图字节流（见 #generateThumbImage）。

  **子类**：LocalStorageService（本地磁盘存储）继承本类， 只需实现文件的上传/下载与初始化即可复用缩略图能力。构造时传入 StorageProperties 并调用子类 #init() 完成存储目录初始化。
- 方法:
  - `public AbstractStorageService(StorageProperties storageConfig)`
    构造器：读取缩略图尺寸配置并触发子类存储初始化。
  - `protected String getThumbPrefixName()`
    生成缩略图文件名后缀（如 "_640x480"）。
  - `public String getThumbImageFilePath(String filePath)`
    根据原文件路径推导缩略图文件路径。
    在最后一个点号（扩展名前）插入缩略图后缀： 如 "dir/xxx.jpg" → "dir/xxx_640x480.jpg"。
  - `public ByteArrayInputStream generateThumbImage(InputStream inputStream) throws IOException`
    在内存中生成缩略图字节流（不落盘）。
    使用 Thumbnailator 把输入图片缩放为配置的宽高尺寸， 结果写入内存中的 ByteArrayInputStream，供上传流程直接保存。

### `shared/src/main/java/cn/wisestar/server/storage/LocalStorageService.java`
- 包: `cn.wisestar.server.storage`
- 类型: `class LocalStorageService`
- **类说明**：
  本地磁盘存储服务（LocalStorageService）。
  **所属模块**：shared 模块存储抽象包（cn.wisestar.server.storage）。

  **类职责**：StorageService 的本地文件系统实现： 把上传文件保存到配置的根目录（`file-storage.local.root-path`）， 支持文件上传、字节读取与流式读取。同时具备缩略图能力（继承 AbstractStorageService）。

  **安全设计**：#resolvePath 对目标路径做 normalize 归一化并校验 必须位于根目录内（startsWith 检查），防止通过 ../ 等路径穿越写出根目录 （路径穿越防护）。

  **实例化**：由 StorageAutoConfiguration 在配置了 `file-storage.local.root-path` 时创建（@ConditionalOnProperty）。

  **异常约定**：上传失败抛 ErrorCode#FileUploadError（4041）、 读取失败抛 ErrorCode#FileNotExists（4040），由全局异常处理器转成 JSON 响应。
- 方法:
  - `public LocalStorageService(StorageProperties configuration)`
    构造本地存储服务（触发父类初始化流程）。
  - `public void init()`
    初始化：创建存储根目录（目录已存在时无副作用）。
  - `public void uploadFile(InputStream file, String path)`
    上传文件到指定相对路径（父目录自动创建，已存在文件覆盖）。
  - `public byte[] download(String filePath)`
    按相对路径读取文件全部字节（下载）。
  - `public InputStream downloadAsStream(String filePath)`
    按相对路径以输入流方式读取文件（适用于流式响应/大文件）。
    注意：本方法直接 resolve 根目录，未做路径穿越校验（与 download 不同）， 调用方需确保传入的是服务端生成的受控路径。

### `shared/src/main/java/cn/wisestar/server/storage/StorageAutoConfiguration.java`
- 包: `cn.wisestar.server.storage`
- 类型: `class StorageAutoConfiguration`
- 注解: @Configuration, @ConfigurationPropertiesScan
- **类说明**：
  存储服务自动配置（StorageAutoConfiguration）。
  **所属模块**：shared 模块存储抽象包（cn.wisestar.server.storage）。

  **类职责**：Spring Boot 自动配置类，负责在满足条件时创建 StorageService 实例注入容器，供业务代码（如 FileService）注入使用。

  **装配条件**：

  - @ConfigurationPropertiesScan：扫描本包下所有 @ConfigurationProperties 类（即 StorageProperties），绑定 file-storage.* 配置；
  - @ConditionalOnProperty：配置了 `file-storage.local.root-path` 才创建 （未配置本地存储时不装配，便于后续扩展其他存储介质）；
  - @ConditionalOnMissingBean：容器中没有其他 StorageService 实现时创建， 允许外部覆盖默认本地存储实现。

  **数据流**：文件上传请求 → FileService → StorageService（本类创建的 LocalStorageService）→ 本地磁盘。
- 方法:
  - `public StorageService storageService(StorageProperties properties) throws IOException`
    创建本地存储服务 Bean（默认存储实现）。

### `shared/src/main/java/cn/wisestar/server/storage/StorageProperties.java`
- 包: `cn.wisestar.server.storage`
- 类型: `class StorageProperties`
- 注解: @ConfigurationProperties, @Data
- **类说明**：
  文件存储配置属性（StorageProperties）。
  **所属模块**：shared 模块存储抽象包（cn.wisestar.server.storage）。

  **类职责**：通过 `@ConfigurationProperties("file-storage")` 绑定 配置文件前缀 `file-storage` 下的所有存储相关属性，供 LocalStorageService / AbstractStorageService 读取使用。

  **配置示例**（application.yml）：
  ```
   file-storage: local: root-path: ./data/file path-strategy: byId # byNo / byId / byDate date-format: yyyyMM/dd name-strategy: seqAndOriginalName # seqAndOriginalName / originalNameAndSeq / seq / uuid thumb-image: width: 640 height: 480
  ```
- 内部类型: class ThumbImage, class LocalStorage
- 方法:
  - `public final ThumbImage thumbImage = new ThumbImage()`
    缩略图配置（上传图片时生成的缩略图尺寸）。

### `shared/src/main/java/cn/wisestar/server/storage/StorageService.java`
- 包: `cn.wisestar.server.storage`
- 类型: `interface StorageService`
- **类说明**：
  存储服务接口（StorageService）。
  **所属模块**：shared 模块存储抽象包（cn.wisestar.server.storage）。

  **类职责**：定义文件存储抽象能力，屏蔽底层存储介质差异（当前唯一 实现为本地磁盘 LocalStorageService，未来可扩展 OSS、S3 等）。 业务代码（如 FileService 上传/下载接口）只依赖本接口编程。

  **能力清单**：

  - init：初始化存储（如创建根目录）；
  - uploadFile：上传文件（输入流 + 相对路径）；
  - download / downloadAsStream：按相对路径读取文件（字节 / 流）；
  - getThumbImageFilePath：由原路径推导缩略图路径；
  - generateThumbImage：内存生成缩略图。

  **路径约定**：所有 path 均为相对存储根目录的相对路径。

### `shared/src/main/java/cn/wisestar/server/storage/StorePath.java`
- 包: `cn.wisestar.server.storage`
- 类型: `class StorePath`
- 注解: @Data
- **类说明**：
  存储文件的路径信息（StorePath）。
  **所属模块**：shared 模块存储抽象包（cn.wisestar.server.storage）。

  **类职责**：文件上传完成后的结果载体，携带文件名、相对路径以及 （可选）缩略图路径，供业务层落库（t_file 表）与返回前端预览地址使用。

  **数据流**：上传文件 → 存储服务生成路径 → 组装 StorePath → FileService 记录到数据库（文件名/路径）→ 前端通过路径拼接访问地址。
- 方法:
  - `public StorePath(String fileName)`
    仅文件名构造器。
  - `public StorePath(String fileName, String filePath)`
    文件名 + 相对路径构造器。
  - `public StorePath(String fileName, String filePath, String thumbFilePath)`
    完整构造器（含缩略图路径）。
