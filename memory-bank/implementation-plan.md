# MyHEBNU — 实施计划

> 版本: v1.0 | 创建日期: 2026-06-04 | 状态: 待执行

---

## 1. 开发路线图

```
Phase 0  →  Phase 1  →  Phase 2  →  Phase 3  →  Phase 4  →  Phase 5  →  Phase 6  →  Phase 7  →  Phase 8
侦察       项目骨架    认证模块     课表模块     成绩模块     空教室       考试安排     Widget+      打磨+发布
(当前)                                                                   通知
└── 1-3天 ──┘ └─ 2-3天 ──┘ └─ 2-3天 ──┘ └─ 3-4天 ──┘ └─ 3-4天 ──┘ └─ 3-4天 ──┘ └─ 2-3天 ──┘ └─ 3-5天 ──┘
```

---

## 2. Phase 0: API 侦察 🔍

| 项 | 详情 |
|------|------|
| **目标** | 完整分析教务系统的认证流程和所有核心 API 端点 |
| **输入** | 管理员提供的教务系统网址 + 学号密码 |
| **方法** | 浏览器 F12 → Network 标签 → 操作关键功能 → 导出 HAR 文件 |
| **持续时间** | 1-3 天 |

### 2.1 抓包清单

管理员需要在浏览器中执行以下操作，每次操作前清空 Network 面板：

| # | 操作 | 需要捕获的信息 |
|----|------|---------------|
| 1 | **登录流程** | SSO 重定向链、POST 参数、Set-Cookie 响应头、最终登录成功的 URL |
| 2 | **查看课表** | 课表 API 端点 URL、请求参数、响应格式（JSON/HTML）、数据结构 |
| 3 | **查看成绩** | 成绩 API 端点 URL、请求参数、学期筛选参数、响应数据结构 |
| 4 | **查询空教室** | 空教室 API 端点、筛选项参数名、可选值范围、响应数据结构 |
| 5 | **查询考试安排** | 考试 API 端点、响应数据结构 |

### 2.2 分析输出

- [ ] SSO 认证流程图（Mermaid 时序图）
- [ ] 完整 API 端点清单（URL、Method、Params、Response Schema）
- [ ] 数据结构文档（字段名、类型、示例值）
- [ ] Retrofit 接口代码草案

### 2.3 HAR 导出示意

```
Chrome/Edge:
1. F12 → Network 标签
2. 勾选 "Preserve log"
3. 完成操作（登录 → 课表 → 成绩 → 空教室 → 考试）
4. 右键任意请求 → "Save all as HAR with content"
5. 将 .har 文件发送给我
```

---

## 3. Phase 1: 项目骨架 🏗

| 项 | 详情 |
|------|------|
| **目标** | 可编译运行的空壳 APP，架构和主题就位 |
| **持续时间** | 2-3 天 |

### 3.1 任务清单

- [ ] **1.1** Gradle 项目初始化
  - 创建 Android 项目（Kotlin, Compose, API 31+）
  - 配置 Version Catalog (`libs.versions.toml`)
  - 添加所有依赖（Compose, Hilt, Room, Retrofit, Vico, Glance, etc.）

- [ ] **1.2** Hilt 依赖注入配置
  - `MyHebnuApplication.kt` (@HiltAndroidApp)
  - `NetworkModule.kt` (OkHttp, Retrofit 单例)
  - `DatabaseModule.kt` (Room Database, DAOs)
  - `RepositoryModule.kt` (Repository 绑定)

- [ ] **1.3** Material Design 3 主题
  - `Theme.kt` — Light/Dark 配色
  - `Color.kt` — 学校蓝色系主色 + Dynamic Color
  - `Type.kt` — 字体排版

- [ ] **1.4** 导航框架
  - `AppNavigation.kt` — 路由图（Schedule 为起始）
  - `DrawerContent.kt` — 侧边抽屉（4 个顶级导航项）
  - `MainActivity.kt` — Compose 宿主 + Scaffold

- [ ] **1.5** 多语言基础设施
  - `strings.xml` (zh) + `strings.xml` (en)
  - 语言切换逻辑（DataStore 存储偏好）

### 3.2 交付物
- 可编译安装的 APK
- 显示侧边抽屉导航 + 4 个占位页面 + 深色模式切换

---

## 4. Phase 2: SSO 认证模块 🔐

| 项 | 详情 |
|------|------|
| **目标** | 完整的登录/自动登录/会话管理 |
| **持续时间** | 2-3 天 |

### 4.1 任务清单

- [ ] **2.1** SSO WebView 登录
  - 加载教务系统登录 URL
  - WebView 设置（JavaScript 启用、Cookie 启用）
  - 监听 URL 重定向 → 判断登录成功

- [ ] **2.2** Cookie/Session 管理
  - OkHttp `CookieJar` 实现（内存 + 持久化）
  - `EncryptedSharedPreferences` 存储 Cookie
  - 后续 API 请求自动携带 Cookie

- [ ] **2.3** 自动登录
  - APP 启动 → 检查已保存的 Cookie
  - Cookie 有效 → 直接进入首页
  - Cookie 过期 → 展示登录页

- [ ] **2.4** 会话过期处理
  - `AuthInterceptor` 拦截 401/403 响应
  - 静默重试登录 → 成功继续 / 失败提示用户

- [ ] **2.5** UI
  - `LoginScreen.kt` — 简洁登录页（可能仅需 WebView）
  - 加载状态 + 错误状态
  - `LoginViewModel.kt`

### 4.2 交付物
- SSO 登录流程可用
- 关闭 APP 再打开 → 自动登录

---

## 5. Phase 3: 课表模块 📅

| 项 | 详情 |
|------|------|
| **目标** | 完整的课表功能（周视图 + 缓存 + Widget 占位） |
| **持续时间** | 3-4 天 |

### 5.1 任务清单

- [ ] **3.1** 数据层
  - `CourseEntity.kt` — Room 实体
  - `ScheduleDao.kt`, `CourseDao.kt`
  - `ScheduleRepository.kt` — Remote 拉取 + Local 缓存
  - 首次使用设置当前周次（`UserPreferences`）

- [ ] **3.2** 周视图 UI
  - `WeekViewGrid.kt` — 7 天 x 6 节次网格布局
  - `CourseCard.kt` — 课程卡片（名称+教师+教室+时间+颜色条）
  - `WeekSelector.kt` — 顶部周次左右切换
  - 当前周高亮 + 当前课程高亮
  - 不同课程自动颜色分配（基于课程名称 hash）

- [ ] **3.3** ViewModel
  - `ScheduleViewModel.kt`
  - UiState: `Loading → CachedData → FreshData | Error`
  - 支持切换周次（前后滑动）

- [ ] **3.4** 刷新
  - 进入页面自动拉取最新课表
  - 支持下拉手动刷新

### 5.2 交付物
- 周视图课表完整可用
- 离线打开 → 显示缓存课表

---

## 6. Phase 4: 成绩模块 📊

| 项 | 详情 |
|------|------|
| **目标** | 成绩查看 + GPA + 趋势图表 |
| **持续时间** | 3-4 天 |

### 4.1 任务清单

- [ ] **4.1** 数据层
  - `GradeRepository.kt` — 仅远程拉取，不缓存
  - 支持按学期筛选 / 全部学期

- [ ] **4.2** UI
  - `GradeScreen.kt` — 学期折叠面板列表
  - `SemesterSection.kt` — 可展开/折叠的学期区块
  - `GpaCard.kt` — 顶部 GPA 总览卡片
  - `GradeTrendChart.kt` — Vico 折线图（X=学期，Y=GPA/均分）
  - 加载骨架屏 + 错误状态

- [ ] **4.3** GPA 计算
  - 可配置计算方式：4.0制 / 5.0制 / 百分制加权平均
  - `GpaCalculator` 工具类（策略模式，预留扩展）

- [ ] **4.4** ViewModel
  - `GradeViewModel.kt`
  - UiState: `Loading → Data(GPA, semesters, trend) | Error`

### 4.2 交付物
- 成绩按学期展示，GPA 卡片 + 趋势图可用

---

## 7. Phase 5: 空教室模块 🏠

| 项 | 详情 |
|------|------|
| **目标** | 多条件筛选的空教室查询 |
| **持续时间** | 3-4 天 |

### 7.1 任务清单

- [ ] **5.1** 数据层
  - `RoomRepository.kt` — 实时请求，不缓存
  - `RoomFilterRequest` — 筛选参数模型

- [ ] **5.2** UI
  - `FilterPanel.kt` — 筛选面板
    - 必选：学年学期下拉框、校区下拉框（2个校区）
    - 可选：楼号、场地类别、周次、星期几、节次
    - "查询" 按钮
  - `RoomList.kt` — 教室卡片列表
    - 教室名称、容量、场地类别、所在楼栋
  - 空状态 + 加载状态 + 错误状态

- [ ] **5.3** ViewModel
  - `RoomViewModel.kt`
  - 筛选条件变更 → 按钮点击触发查询
  - UiState: `Idle → Loading → Results | Empty | Error`

### 7.2 交付物
- 空教室多条件筛选查询完整可用

---

## 8. Phase 6: 考试安排 📝

| 项 | 详情 |
|------|------|
| **目标** | 考试列表 + 倒计时 |
| **持续时间** | 2-3 天 |

### 8.1 任务清单

- [ ] **6.1** 数据层
  - `ExamRepository.kt` — 远程拉取，可选缓存
  - 距离天数计算（基于 `LocalDate`）

- [ ] **6.2** UI
  - `ExamScreen.kt` — 按时间排序的考试列表
  - `ExamCard.kt` — 考试卡片
    - 科目名称、日期时间、地点、座位号
    - 考试类型标签（期末/补考/重修）
    - 倒计时标签（距今天数，紧急标红）
  - 空状态 + 加载状态

### 8.2 交付物
- 考试安排完整可用

---

## 9. Phase 7: Widget + 通知 🔔

| 项 | 详情 |
|------|------|
| **目标** | 桌面课表 Widget + 提醒通知 |
| **持续时间** | 3-5 天 |

### 9.1 任务清单

- [ ] **7.1** 课表桌面 Widget (Glance)
  - `ScheduleWidget.kt` — 4x2 基础尺寸
  - 显示当天课程列表
  - 从 Room 数据库读取
  - 点击跳转 APP 课表页
  - 每天凌晨自动刷新

- [ ] **7.2** 上课提醒通知
  - `ClassReminderWorker.kt` — WorkManager 调度
  - 课前 15 分钟推送
  - 通知内容：课程名称 + 教室 + 时间
  - 通知渠道：`class_reminder`

- [ ] **7.3** 考试提醒通知
  - `ExamReminderWorker.kt`
  - 考前 1 天 推送 + 考前 1 小时推送
  - 通知渠道：`exam_reminder`

### 9.2 交付物
- Widget 可用 + 两种通知正常工作

---

## 10. Phase 8: 国际化 + 无障碍 + 打磨 ✨

| 项 | 详情 |
|------|------|
| **目标** | 全面打磨，达到发布标准 |
| **持续时间** | 3-5 天 |

### 10.1 任务清单

- [ ] **8.1** 全量中英双语翻译
  - 所有 UI 文案中英文对应
  - 语言切换即时生效（无需重启）

- [ ] **8.2** 无障碍适配
  - 所有交互元素设置 `contentDescription`
  - 支持 TalkBack 屏幕阅读器
  - 高对比度主题变体
  - 支持系统字体缩放

- [ ] **8.3** 全面错误处理
  - 所有网络请求 Error 状态覆盖
  - 超时、断网、服务器错误、解析失败

- [ ] **8.4** 离线体验优化
  - 离线状态指示器（顶部 Banner）
  - 缓存过期提示

- [ ] **8.5** 性能优化
  - Compose 重组优化
  - 图片懒加载
  - 避免主线程阻塞

### 10.2 交付物
- 生产可用的完整 APP

---

## 11. 开源库复用总清单

| 库 | 版本（最新稳定） | 用途 | 许可证 |
|-----|-----------------|------|--------|
| Jetpack Compose BOM | 2024.x+ | UI 框架 | Apache 2.0 |
| Material3 | - | MD3 组件 | Apache 2.0 |
| Compose Navigation | 2.8.x | 导航 | Apache 2.0 |
| Hilt | 2.51+ | DI | Apache 2.0 |
| Retrofit 2 | 2.11.x | HTTP 客户端 | Apache 2.0 |
| OkHttp 4 | 4.12.x | HTTP 引擎 | Apache 2.0 |
| Room | 2.6.x | 数据库 ORM | Apache 2.0 |
| DataStore | 1.1.x | KV 存储 | Apache 2.0 |
| Vico | 2.0.x | 图表 (Compose) | Apache 2.0 |
| Glance | 1.1.x | Widget | Apache 2.0 |
| Coil | 2.7.x | 图片加载 | Apache 2.0 |
| Jsoup | 1.18.x | HTML 解析 | MIT |
| WorkManager | 2.9.x | 后台任务调度 | Apache 2.0 |

---

## 12. 风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| 教务系统只有 HTML 无 JSON API | 需 Jsoup 解析，增加工作量 | Phase 0 提前确认，优先找 API 端点 |
| SSO 流程复杂（验证码/动态Token） | 登录模块开发困难 | WebView 方案可应对大多数 SSO 场景 |
| 教务系统改版/更换域名 | API 失效 | 配置化 Base URL，易于更新 |
| Android 权限适配（通知/后台） | 部分设备通知失效 | Phase 7 设置引导用户打开通知权限 |

---

> **关联文档**: [[design-document]] | [[architecture]] | [[progress]]
