# AGENTS.md

本文件面向在此仓库工作的 AI/自动化编码代理，给出必须遵守的构建、运行、数据库与提交约定。人类开发者请先读 `README.md` 与 `docs/开发规范.md`。

## 项目概览

wisestar（智慧星 AI 自习室）由开源项目 SurveyKing 改造而来，后端 Spring Boot 多模块（Maven / JDK 17），前端 React 19 + antd 6 + Vite。仓库统一在 `main` 分支开发，不新建分支。

## 模块与依赖方向

- `server/api`：唯一启动入口（`SurveyServerApplication`），Controller 层，端口 1991（preview）/ 7007（dev）。
- `server/shared`：统一响应/分页/异常/鉴权/常量/DTO/服务接口。
- `server/rdbms`：实体、MyBatis-Plus Mapper、Service 实现、数据库种子脚本。
- `server/ai`：AI 对话能力。
- `wisestar-client`：前端 SPA，开发端口 3000，`/api` 反向代理到后端。
- `research-platform/server`：教研平台独立后端（Spring Boot 2.7.7 / JDK 17 / MyBatis-Plus），包名 `cn.wisestar.research`，端口 7010，独立库 `wisestar_research`。
- `research-platform/client`：教研平台独立前端（React 19 / antd 6 / Vite），端口 3100。

依赖方向固定为 `api → rdbms → shared` 与 `api → ai`，禁止反向或跨层直连。`research-platform` 不依赖任何 `server` Maven 模块，两套系统只通过 `/open/research/v1/**` 只读接口通信，禁止跨库 JOIN 或直连对方数据库。

## 构建与运行

```bash
# 后端构建（必须 clean，防止旧 jar 残留）
cd server && mvn clean package -pl api -am -DskipTests

# 后端启动（preview，内置 H2，库缺失时自动恢复快照）
cd server && ./start-preview.sh

# 前端
cd wisestar-client && npm ci && npm run dev

# 教研平台后端（独立 H2，首次启动自动建表。7010 仅内部使用，浏览器不直接访问）
cd research-platform/server && mvn clean package -DskipTests && ./start-preview.sh

# 教研平台前端（3100 仅内部使用；主站 3000 的 /research/ 会反代到它）
cd research-platform/client && npm install && npm run dev
```

- **日常只开一个入口 `http://localhost:3000`**：`/` 主站、`/api` 主站后端、`/research/` 教研前端、`/research/api` 教研后端。3000 的 `vite.config.js` 已配好 `/research/`→3100、`/research/api`→7010，与生产 nginx 一致；`research-platform/client/.env.development`（已提交）固定 `VITE_BASE_PATH=/research/`、`VITE_API_BASE=/research/api`，本地可用 `.env.local`（gitignore）覆盖。只联调主站时可以不启动 7010/3100。

- 教研平台内置管理员 `admin / 123456`；老师账号走 AI 自习室只读校验，学员账号拒绝登录。
- 只读接口契约唯一真源：`research-platform/contract/open-research-v1.yaml`；服务令牌 `RESEARCH_OPEN_TOKEN`（默认 `dev-research-token`），生产必须覆盖且不得暴露到公网。
- 教研平台生产部署走单域名路径反代（`/research/` + `/research/api/`），示例见 `research-platform/deploy/nginx.example.conf`；会话 Cookie 名 `research-token`，`Path=/research`。

- 默认账号：管理员 `admin / 123456`，学员 `a000001 / 123456`。
- 英语朗读音频由服务端合成（Piper 神经语音，回退 espeak-ng）。首次部署执行 `bash server/tts-setup.sh`：装 `espeak-ng`/`lame`、建 `/opt/wisestar-tts/venv`、下载 `en_GB-jenny_dioco-medium` 英式女声（`bash server/tts-setup.sh` 按 `VOICE_NAME` 推导下载路径，默认即该音色）。配置在 `application.yml` 的 `english.tts.*`：`engine: piper`（`espeak` 用机械音；`none` 则前端播放直接提示失败，不再回退浏览器语音合成），`piper-bin`/`piper-voice` 可用 `ENGLISH_TTS_PIPER_BIN`/`ENGLISH_TTS_PIPER_VOICE` 覆盖，缓存目录默认 `${java.io.tmpdir}/wisestar-tts`（`ENGLISH_TTS_CACHE_DIR`）。`speed: 109` 是 espeak 词/分口径（≈常速 0.75 倍），Piper 侧按固定基准 145 换算 `--length-scale`（改小 speed 才会真正放慢，旧实现以自身为基准导致配置无效）。学员端打开跟读/单词页会在后台批量预热音频（**必须用学员端实际播放语速预热**：练习页 125、课文默认 109，缓存键含语速，语速对不上的预热无效；只预热单页 ≤60 词，整册取干扰项池不预热）。`english.tts.daemon: true` 同时启动句子/课文（109）与单词练习（125）两个常驻 Piper 进程：模型只加载一次，首次点击也在 1s 内（每个常驻进程约 170MB，`daemon-max: 2`、`daemon-idle-seconds: 0` 常驻不回收、`preload-speeds: 109,125`；常驻进程只为 `preload-speeds` 里的语速启动，其他语速即使名额空着也只走一次性合成）。常驻合成以 Piper stderr 的 `Wrote <路径>` 行作为完成信号，读完即删该 wav；不要再按 RIFF 头长度轮询——Piper 边写边更新头部，早读会拿到半截音频。`max-chars` 必须 ≥ 最长跟读句（当前 516 字符，配置 600），否则「听原音」只读前半句；改 `read-aloud.json` 后要复核这个上限。`sentence-silence: 0.3` 是句号/问号/感叹号后的静音秒数，一次性合成、常驻进程、批量预热三处 Piper 启动都要带 `--sentence-silence`。缓存键带版本号（当前 `v3`），改合成链路时递增即可让旧音频整体失效；前端 `api/englishStudent.js` 的 `TTS_AUDIO_VERSION` 必须同步递增，否则浏览器 7 天 HTTP 缓存会继续返回旧音频。缓存命中后单次响应约 10ms。课程静态音频可用 `bash server/tts-pregenerate.sh` 全量预生成：扫描句子/单词/课文跟读/语法课本原文与例句，句子/课文/语法按 109、单词按 109 与 125，已缓存自动跳过，产物写入 `server/tts-cache/` 并打成 `server/tts-cache.tar.gz`（均 gitignore），`start-preview.sh` 检测到该目录会自动把 `ENGLISH_TTS_CACHE_DIR` 指过去；预生成与线上必须使用同一 `CACHE_VERSION`、音色路径和语速，云端把 tar 解压到 `ENGLISH_TTS_CACHE_DIR` 即可命中，改音色/语速/缓存版本后要重跑，preview H2 运行前需先停后端。管理端句库/词库列表会回填 `audioCached`（句子 109、单词 109/125）并支持试听，用于查看语音包是否已预生成；缓存检查只放在管理端列表，学员端大列表不要逐行查缓存。不要给 20 句以上的句子列表/句库页做整页批量预取：常驻 Piper 是单队列，点击会排在几十条预取后面反而等十几秒，句子页只在真正点击时合成。单词练习页不自动朗读（题目出现、作答揭晓、切题都静音），只有点「听发音」或点单词本身才播，别再改回自动播；每次朗读先停上一段并用播放令牌丢弃过期请求，避免旧音频串到新题上。mp3 固定 64kbps（32kbps 齿音发闷、带压缩杂音，语言学习不可接受）；缓存键含引擎/音色/语速/文本，改音色或语速自动失效。
- 英语各功能的返回一律回到所属大板块：单词练习 / 单词情景对话回 `/student/english/word?unit=`，句式情景对话回 `/student/english/sentence?unit=`，语法练习回 `/student/english/grammar?unit=`；从智能复习进入的单词练习与语法练习（`from=review`）回复习页；阅读理解、课本跟读、单元练习属于学习中心一级模块，回 `/student/english`。作答页顶部要有可见返回按钮，别只留浏览器后退。
- 检测单元接口 `/student/detect/units` 的 `name` 只有「Unit 1」这类短名，英语内容包单元名是「Unit 1 What's on your plate?」全名；学员端把全名匹配到检测单元（取 `chapterId`）必须用 `utils/english.js` 的 `matchDetectUnit`（按名称前缀 + 分隔符/结尾匹配，避免把 Unit 10 当成 Unit 1），不要再用 `u.name === unit`；检测单元完全匹配不到（如九年级上册内容包是 Unit、检测树却是 Module）时仍按单元全名组卷，`chapterId` 传空即可，不能直接进「暂无可用单元练习」空态。
- 语法点自 2026-11 起按结构化字段维护：`structure`（结构公式）、`textbook`（课本原句 JSON，含 `source`）、`samples`（例句 JSON，含 `note`）、`exercises`（例题 JSON，`type=choice|build`，带 `answer`/`explanation`）；学员端重点语法页依次渲染「结构 → 课本原文 → 例句 → 例题」，例题只展示前 3 条并高亮正确项。`exercises` 列沿用 H2 `json` 类型，按字符串写入会被二次编码，读取时必须解包成原始 JSON 文本（`EnglishGrammarServiceImpl`/`EnglishStudentServiceImpl` 的 `unwrapJsonText`），否则前端会拿到套了一层的字符串。AI 批量补齐走 `POST /english/grammar/ai-enrich`（`EnglishAiPackService.generateGrammarPreview`），先预览再逐条/整体入库，提示词见 `AiPromptKeys.EN_PACK`。
- 单元练习默认 36 题（上限 40，管理端可选 10/15/20/36）：`EnglishUnitPaperServiceImpl` 先按三模块均分名额（默认 36 题 ≈ 12/12/12），每个模块内 `takeModule` 按知识点权重分配（薄弱知识点 priority==0 权重 2，其余 1），再按知识点 D1→D3 阶梯取题，最后用 carry 把空出的名额补到其他模块；不要改回纯平均或不加权。单元题量不足时优先往 `t_english_sentence` 和该单元语法点的 `examples` 补同主题句子（会同时派生单词、句式、语法题），不要复制旧题硬凑。语法融合题同时派生「选正确形态」（Radio/FillBlank）与「连词成句」（build），例句连词成句优先选非课本原句，且与句式模块原句重复时跳过，避免跨模块重题。
- 句式通关进度只按答对统计：圆环取 `EnglishSentenceView.angleCorrect`（`t_memory_review_log` 里 `correct=1` 的角度），顶部进度条按 `correctCount > 0` 的句数，`angleUsed` 含答错角度不得用于进度；从题型返回时重新拉 `sentences` 与 `unit-pass`，题目顺序每次进入都 `shuffle`（含综合练习），别改回固定题序。
- 单词复习队列只取 `english_word_book.next_review_time <= now` 的到期词，按 `next_review_time` 升序、同拼写去重，单次上限 `MAX_WORD_REVIEW = 20`（旧「三/四年级 20、五/六年级 30」的凑量逻辑已废弃）；无到期词返回空列表，复习页显示「本次 0 词」并把开始按钮置灰。复习页卡片的「应复习/陌生/一般/熟练词数 + 涉及单元」由 `/english/student/review/word-summary` 给出（与队列复用同一 `dueWords` 取词，概览不限量、队列截 20），别再在前端按当前 20 词反推总数。到期词可能不足 4 个，四选一的干扰项从整册 `word-book` 取，只有出题目标来自复习队列。
- 到期与薄弱判定自 2026-11 起统一由 `MemoryReviewService`（FSRS-5 + SM-2 冷启动，表 `t_memory_schedule` / `t_memory_review_log`）维护：`reps<2` 走 SM-2，之后走 FSRS-5，目标保持率 0.9；薄弱进池六条（R<0.6、连错 2 次、输出题错、攻克后复发、连续两次假会 `fragile`、累计正确率 <60% `low_accuracy`），攻克需复习前 R<0.85、本次与上一轮评分均 ≥3、换角度（知识点无角度时自动满足）、输出薄弱需输出题达标、复习后 S≥7 天；重做/补答（`firstAttempt=false`）不重复入账，避免「首答错重做对」洗白记忆强度。旧 `EnglishReviewScheduler` 固定档位与「最近 5 次加权正确率」已降级为回写口径，新代码不要再用它们做调度或薄弱排序；调度结果会回写 `*_book`、`t_english_learning_state`、`UserKnowledgeProgress` 与 `UserWeakKnowledge`，别在别处重复覆盖这些字段。
- 阅读理解种子 `server/rdbms/src/main/resources/english/reading-articles.json`（外研 3-6 共 265 篇 / 53 单元，人教 7-9 共 168 篇 / 42 单元，人教每单元固定 4 篇）必须中英逐句对应，结尾只收束故事情节，不写「这告诉我们 / 写作手法」式点评；同一英文句不得跨篇复用（≥5 词重复即视为模板串篇）；每篇固定 4 题，正确答案在文内只有一处依据。改完重启后端由 `EnglishReadingSeeder` 按单元整体覆盖 `reading_content`，不要手工改库。
- 人教版情景对话（单词情景 + 句式情景）种子 `server/rdbms/src/main/resources/english/scenario-packs.json`，由 `EnglishScenarioSeeder`（`@Order(3)`）按「版本+年级+册别+单元」只写 `word_content` / `pattern_content`，保留阅读列；重新生成时更新该 JSON 并重启即可。
- 人教版内容补全由五个幂等 Seeder 在启动时执行：`EnglishWordSectionSeeder`（`@Order(4)`，>40 词的单元按每节约 30 词切「重点词汇（1/2/3）」，中文数字旧名会迁移为阿拉伯数字，否则按 section 排序会得到「一、三、二」）、`EnglishGrammarContentSeeder`（`@Order(5)`，读 `english/grammar-content.json` 只替换占位讲解，拼接标题拆出的新行不存在才插入）、`EnglishSentenceSeeder`（`@Order(6)`，读 `english/unit-sentences.json` 按「版本+年级+册别+单元+英文」插入，已存在只补缺失 `zh`；插入 id 冲突说明旧实现留下过逻辑删除的残留行，seeder 会自动换后缀重试，勿手工改库）、`EnglishProperNounSeeder`（`@Order(7)`，读 `english/proper-nouns.json` 按「版本+年级+册别+单元+拼写」补入附录专有名词）、`EnglishGrammarExampleSeeder`（`@Order(8)`，读 `english/grammar-examples.json` 给语法点补/换例句与练习：`mode=replace` 整组替换 `examples`/`samples`/`textbook`/`exercises`，`mode=append` 只追加缺失例句和 `q` 不重复的选择题；例句全部不足 4 词视为占位自动替换）。管理端手工维护的小节/讲解不会被覆盖。
- 语法练习题量：人教 42 单元各 20-24 题（选择题为主、纯英文题干），依赖单元语法点的 `examples` 与 `exercises`。`EnglishFusionQuestionFactory` 的 `grammarCloze` 对每条例句最多派生 3 道挖空选择题，`grammarExercise` 每道配置练习派生 1 题；补题优先往 `grammar-examples.json` 加例句/练习，别在页面里硬编码题目。
- 中考真题题库 `server/rdbms/src/main/resources/english/exam-questions.json`（classpath，不落库）：每条含 `id/version/grade/term/units[]/module/knowledgePoint/difficulty/title/options/answer/analysis/source`；`EnglishUnitPaperServiceImpl.loadExamQuestions` 启动后首次加载并缓存，按版本+年级+册别+单元命中后以 `exam:<id>` 为 contentKey 追加进 `loadQuestions`，题干前缀按来源区分「【真题】」/「【中考模拟】」（`source` 含「模拟」即后者）；`module` 取「单词/句式/语法」以参与模块过滤。判分走通用 Radio 逻辑（选项 id `exam:<id>#<index>`），交卷时 `loadQuestions` 会重新带回真题，因此真题必须自包含答案。新增真题只需往该 JSON 追加条目并重启，无需改库；来源字段务必写清（如 `2020年山东中考·单项选择`）。**只收人教版有对应语法点的题**：按「考点/考查」短语 + 正确答案形态判定归属单元，无法确信的一律丢弃（宁缺毋滥，避免超纲/错配）；剔除含图片、多空残缺、答案与解析「故选X」不一致的题；每题 `analysis` 必须是中文解析（现库 95 条，其中真人真题 85 条、分布 26 个单元、每单元 ≤4 条，2002 年黑龙江 2024 等均为作答自洽的真实中考卷）。单元练习每卷实际出现 4~7 条真题（含旧模拟题），真题按普通语法组参与 `takeModule` 按权重分配名额。采集管线（夸克网盘真题 → doc/docx 抽文本 → 解析）为一次性脚本，产物不入库。
- 跟读（`/student/english/read-aloud`）打分是离线四项加权，口径是「判通顺、判漏读」，口音尽量不扣分：流畅度 40%（只把 ≥400ms 空档算卡顿、占比 32% 起扣）、完整度 30%（时长比 ±25% 满分）、发音接近度 20%（MFCC+DTW，`1 - dist/8`，换人 3~4 只掉几个点）、音量 10%。**不要把频谱相似度调回主权重，也别把流畅度改回 250ms/8% 的严口径**——那会让「读得很标准只有 50 多分」。完整度 <35% 压到 45、<70% 压 65，发音接近度 <25% 压 60，音量 <25% 压 55；算法不联网、不上传录音，也不校验「读的是不是这句话」（实测读别的句子也可能拿 90 分，只能靠完整度挡漏读）。
- 前端 `wisestar-client/.env.local`（gitignore）可设置 `API_TARGET`。
- 预览环境只有一个对外端口（前端 3000）。

## 前端性能约定（学员机常为十年前的笔记本）

- 页面一律 `React.lazy` 路由级懒加载；`vite.config.js` 的 `manualChunks` 只固定 `react`/`react-dom`/`react-router`/`katex`/`jsencrypt`，**不要把 antd/rc-* 聚成单块**（会把管理页组件全拉进首屏，实测 472KB → 176KB gzip）；`MainLayout` 也懒加载。
- `npm run build` = `vite build` + `node scripts/compress-dist.mjs`（预压 `.gz`）；`vite preview` 中间件按 `Accept-Encoding` 回 gz 并给带 hash 资源加 `immutable` 缓存，改动构建配置后需重启预览。
- 重型库按需动态加载（如 `jsencrypt` 在点登录时 `import()`）；首屏 gzip 预算约 300KB，新增全局依赖前先估算。
- 低配降级：`utils/perfLight.js` 自动判定（`hardwareConcurrency<=4`/`deviceMemory<=4`）并加 `html.low-perf`，`styles/low-perf.css` 关闭毛玻璃与无限装饰动画；`?perf=light|high` 可人工强制。
- 学员端新增样式避免 `backdrop-filter` 与大面积常驻 `animation`，需要时用 `low-perf` 类提供降级写法。
- 学员端页面按视口比例缩放：`styles/page-scale.css` 在 `:root` 提供 `--page-u`（及其原始值 `--page-u-raw`；1366→1px，宽屏按视口宽/1366 放大，封顶 1.75，≤760px 固定 1px）。学员端页面 CSS（首页/研习/知识点/检测/错题/档案/商城/英语/登录）与共用 `quiz-stage.css`、`QuestionCard.css`、`VirtualKeyboard.css`、`IconTile.css` 的尺寸统一写 `calc(原px * var(--page-u))`；SVG 的 `size` 属性用 `utils/pageScale.js` 的 `usePageUnit()` 同步。顶部通栏在 `student.css` 里把 `--page-u` 局部封顶 1.2，避免宽屏徽标把它挤成两行；管理端 CSS 不要引用该变量。`@media` 断点仍用视口 px，不随变量换算。三列/多列流程按容器宽度收敛：带单元栏的页面整页宽度 100%（上限 1600×变量），1281~1600 收窄箭头与主环，≤1280 堆叠并隐藏箭头；新增学员页沿用该口径，不要在页面里另写媒体查询跳档。

## 做题界面版式约定（学员端）

- 逐题作答统一走 `KnowledgePage` 的 `quizMode` 版式：容器 1340px、`.qz-shell` 网格「主卡 + 240px 答题卡」，右栏 sticky 题号卡 + 三态图例，底部 `.qz-nav` 胶囊导航；≤1180px 单列、答题卡置顶横排。
- 尺寸基准：题卡 `padding 28/32`、圆角 18；题干 17px/1.85；选项 `min-height 58`、`padding 15/18`、文本 16px、字母标 30px；提交按钮 46px 高。
- 共用 `QuestionCard`（检测/单元练习/语法练习/错题重做/练习答题）容器 1000、题干 20px/1.8/字重 500、选项 `padding 14/18` + `min-height 54`、选项文本 18px，改它即全站同步，勿在单页覆盖同名字号。
- 做题/学习页字号自第 252 节起对齐「小珈享学」参考站，回归紧凑排版（题干 18px/字重 500/行高 1.8、选项文本 17px、选项 `min-height 54`、题卡 `padding 26/32`、`min-height 440`），取消第 198 节的整体 +50% 放大；新增页面沿用当前紧凑尺度，保持海洋配色与既有元素，长应用题优先保证一屏可读、不频繁滚动。
- 即时判题（`eng-feedback` 那类结论）一律放在白卡外，用 `components/student/MascotBubble`（海豚头像 + 对话气泡）播报，卡内不再写判题文案：气泡容器固定高度、只换文案与配色，作答不会把选项往下挤。素材 `public/student-assets/dolphin-captain.webp` 由首页 `ship-start.webp` 裁出。
- 第 199 节起所有答题/练习页的白色主卡统一加 `quiz-stage` 类（`styles/quiz-stage.css`，`main.jsx` 全局引入）：宽度上限 1240px，`min-height: clamp(520px, 100vh-260px, 720px)` 保证常规题目下各页尺寸一致；内容变多时卡片跟着长高，由浏览器整体滚动，卡内禁止出现滚动条（第 201 节起改为 `min-height` 口径，取消内部滚动）。新增答题页只需给主卡加该类，宽度上限统一 1240，`KnowledgePage` 因右侧答题卡网格保持 1076。
- 英语页自第 253 节起整体再收敛 12%：`.eng-stage / .eng-learn-wrap / .eng-page / .eng-read-wrap / .eng-diag-wrap / .eng-ra-wrap` 局部声明 `--page-u: calc(var(--page-u-raw) * 0.88)`，只用这一处缩放 + 两侧留白，不要在各页零散改字号；`EnglishReadAloudPage.css` 读不到 `EnglishCenterPage.css`，跟读的 `--page-u` 必须在自己的 `.eng-ra-wrap` 上再声明一次。英语练习主卡把 `--quiz-stage-min` 覆盖为 `clamp(260, 100vh-440, 460)`，单词练习选项收紧到 `min-height 56`，语法/单元练习给共用 `QuestionCard` 传 `compact`（选项 46px/文本 16px/间距 8px）以贴近一屏。
- 学习/练习类页面（`/student/study`、`knowledge`、`wrong`、`detect`、`diagnose`、`english/*`）不渲染 `sll-tabbar`，`StudentLayout` 会给 `.sll-main` 追加 `sll-main--no-tab`，把为悬浮导航预留的底部留白（`clamp(96u,9vw,150u)`）回收为 `24u`；新增隐藏导航的页面时复用该 class，别再让页面底部空出一大截。
- 学员端学习/练习类页面（`/student/study`、`knowledge`、`wrong`、`detect`、`diagnose`、`english/*`）隐藏悬浮底部导航 `sll-tabbar`（`StudentLayout` 按路径前缀判断），避免压住题卡与选项；首页、档案、商城、个人中心保留。

## 数据库约定

- 两套幂等种子脚本必须同步修改：`server/rdbms/src/main/resources/scripts/init-h2.sql` 与 `init-mysql.sql`。
- 新增表用 `CREATE TABLE IF NOT EXISTS`；新增列用 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`；内置权限用 `INSERT ... WHERE NOT EXISTS` + 角色 `UPDATE` 收敛。
- `spring.sql.init.mode=always` 会在每次启动重跑 `init-h2.sql` / `init-mysql.sql`，**禁止**在其中写会覆盖业务数据的 `UPDATE`（如人教版句子中文/语法样例）；这类数据统一放 `english/*.json` 由 Seeder 幂等同步，一次性迁移 `UPDATE` 用完即删。
- 业务表统一审计列：`id / create_at / create_by / update_at / update_by / is_deleted`，实体继承 `BaseModel`。
- 新增权限点必须同时在 `shared/.../constant/PermissionConsts.java` 与两份脚本登记。
- H2 快照流程：停止后端 → `server/db-export.sh` → 提交 `server/db-snapshot/wisestar.sql`。H2 文件库为单进程独占，禁止启用 `AUTO_SERVER`。
- 教研平台使用独立库，两份幂等脚本在 `research-platform/server/src/main/resources/db/init-h2.sql` 与 `init-mysql.sql`，同样必须同步修改；生产 `mysql < init-mysql.sql` 后用 `--spring.profiles.active=mysql` 启动。

## 代码约定（详见 docs/开发规范.md）

- 统一响应 `{code, data, message}`（成功 code=200）；分页 `{total, list, current, pageSize}`。
- Controller 只做校验与编排，业务写在 `ServiceImpl`，`Request → Model → View` 转换走 MapStruct。
- 鉴权：功能级 `@PreAuthorize("hasAuthority('模块:动作')")`；数据级 `@EnableDataPerm` / `CampusScopeService`。
- 日志统一用 `@Slf4j` 的 `log`，禁止 `System.out` 与 `printStackTrace`。
- 前端所有 HTTP 调用经 `src/api/request.js`，响应已由拦截器解包。

## 提交约定

- 只在 `main`；提交前逐文件 `git add <file>`（禁止 `git add -A` / `git add .`）。
- 提交信息格式：`<type>(<scope>): <中文描述>`，type ∈ `feat|fix|chore|refactor|style|docs|test`。
- `commit` 与 `push` 前需与用户确认；push 不带 `-o merge_request.*`。

## 会话收尾与交接

- 每次会话结束前（或交接给下一次对话前），按 `docs/会话收尾与交接清单.md` 执行：核对文档最新 → 构建/lint/提交/push 使主线最新 → 停后端导出 H2 快照并提交 → 重启预览自检。
- 数据库跨会话迁移依赖该清单第 3 步：`server/db-export.sh` 导出 `server/db-snapshot/wisestar.sql` 并提交，新环境由 `server/start-preview.sh` 自动恢复。

## 文档

- 规范：`docs/开发规范.md`
- 现状：`docs/项目结构梳理.md`、`docs/项目词典*.md`
- 过程记录：`docs/开发维护日志.md`；文档索引：`docs/README.md`
- 教研平台：`research-platform/README.md`、`research-platform/AGENTS.md`、`.monkeycode/specs/2026-11-23-teaching-research-platform-v2/`
- 收尾与交接：`docs/会话收尾与交接清单.md`
- 变更结构/接口/权限后，同步更新对应词典与日志。
