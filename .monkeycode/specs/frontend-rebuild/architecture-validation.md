# SurveyKing 前端架构校验

## 一、技术选型校验

| 校验项 | 结论 |
|--------|------|
| Vite 5 + React 18 兼容 Java 17 环境？ | 前端不依赖 JDK 版本，无影响 |
| Ant Design 5 覆盖所有需要的 UI 组件？ | 表格、表单、弹窗、上传、图表全覆盖 |
| Zustand 够用吗？ | 本项目状态简单（用户信息 + 当前问卷），不需要 Redux |
| wangeditor 支持视频嵌入吗？ | 支持自定义 HTML，iframe/视频标签可以直接粘贴 |
| 打包后能嵌入 Spring Boot JAR？ | Vite build 输出到 `server/api/src/main/resources/static/`，Spring Boot 自动提供静态服务 |

## 二、API 覆盖校验

| 后端 Controller | API 数量 | 前端是否覆盖 |
|----------------|---------|------------|
| UserApi | 9 个接口 | 全部覆盖（login/register/currentUser/userOverview/logout/importUser/listUserTask/listHistoryTask/listRegisterRole） |
| ProjectApi | 18 个接口 | 全部覆盖（CRUD/回收站/参与者/编辑器查询） |
| SurveyApi | 11 个接口 | 全部覆盖（loadProject/validateProject/saveAnswer/tempSaveAnswer/upload/loadExamResult/loadQuery/getQueryResult/statistics/loadDict/loadLinkResult） |
| AnswerApi | 8 个接口 | 全部覆盖（CRUD/下载/附件上传） |
| SystemApi | 15+ 个接口 | 全部覆盖（角色/用户/部门/岗位/字典 CRUD） |
| TemplateApi | 4 个接口 | 全部覆盖（CRUD） |
| RepoApi | 6 个接口 | 全部覆盖（CRUD/题本/错题本） |
| FileApi | 3 个接口 | 全部覆盖（上传/下载/删除） |
| DashboardApi | 1 个接口 | 覆盖（仪表盘数据） |
| ReportApi | 1 个接口 | 覆盖（报表数据） |

## 三、题型覆盖校验

| 题型 | 编辑器组件 | 渲染组件 | 状态 |
|------|----------|---------|------|
| Radio 单选 | RadioConfig | SurveyRadio | 规划中 |
| Checkbox 多选 | CheckboxConfig | SurveyCheckbox | 规划中 |
| FillBlank 填空 | FillBlankConfig | SurveyFillBlank | 规划中 |
| Textarea 长文本 | TextareaConfig | SurveyTextarea | 规划中 |
| MultipleBlank 多填空 | FillBlankConfig | SurveyMultipleBlank | 规划中 |
| Select 下拉 | SelectConfig | SurveySelect | 规划中 |
| Cascader 级联 | CascaderConfig | SurveyCascader | 规划中 |
| Score 打分 | ScoreConfig | SurveyScore | 规划中 |
| Nps 推荐值 | NpsConfig | SurveyNps | 规划中 |
| Upload 上传 | UploadConfig | SurveyUpload | 规划中 |
| RichText 富文本 | RichTextConfig | SurveyRichText | 规划中 |
| Signature 签名 | SignatureConfig | SurveySignature | 规划中 |
| MatrixRadio 矩阵单选 | MatrixConfig | SurveyMatrixRadio | 规划中 |
| MatrixCheckbox 矩阵多选 | MatrixConfig | SurveyMatrixCheckbox | 规划中 |
| MatrixFillBlank 矩阵填空 | MatrixConfig | SurveyMatrixFillBlank | 规划中 |
| MatrixScore 矩阵打分 | MatrixConfig | SurveyMatrixScore | 规划中 |
| MatrixNps 矩阵NPS | MatrixConfig | SurveyMatrixNps | 规划中 |
| Address 地址 | AddressConfig | SurveyAddress | 规划中 |
| Barcode 条码 | BarcodeConfig | SurveyBarcode | 规划中 |
| Judge 判断 | JudgeConfig | SurveyJudge | 规划中 |
| HorzBlank 横向填空 | FillBlankConfig | SurveyHorzBlank | 规划中 |
| Pagination 分页 | 自动生成 | PaginationContainer | 规划中 |
| Remark 备注 | RemarkConfig | SurveyRemark | 规划中 |
| SplitLine 分割线 | 自动生成 | SplitLineRenderer | 规划中 |

总计 24 种题型 / 辅助元素，全部有对应组件规划。

## 四、关键风险识别

| 风险 | 影响 | 应对 |
|------|------|------|
| 题型编辑器是最复杂的部分 | 开发周期最长 | 先实现最常用的 5 种题型，其他逐步迭代 |
| RSA 前端加密需要 jsencrypt 库 | 额外依赖 | npm install jsencrypt，登录时前端加密密码 |
| 问卷 JSON 结构复杂，递归渲染容易出 bug | 答题页渲染异常 | 先写单元测试覆盖递归渲染逻辑 |
| Ant Design 5 和原系统 (Ant Design 4) 有 API 差异 | 迁移成本 | 直接按 Ant Design 5 新 API 写，不管旧版 |
| 文件上传大小限制 | 大文件上传失败 | 配置 Vite 代理的 body 大小限制 |

## 五、结论

架构设计通过校验。所有后端 API 均有对应前端页面覆盖，所有题型均有对应组件规划，技术栈选型合理。可以开始实施。
