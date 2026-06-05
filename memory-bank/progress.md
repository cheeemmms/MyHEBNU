# MyHEBNU — 进度追踪

> 最后更新: 2026-06-05 | 状态: 细化打磨阶段 — 已完成核心闭环，开始按优先级修 Bug + 优化体验

---

## 当前状态

```
Phase 0         Phase 1        Phase 2        Phase 3        Phase 4        Phase 5        Phase 6        Phase 7        Phase 8
 侦察            骨架           认证           课表           成绩           空教室         考试           Widget+通知     打磨
[✅ 已完成]     [✅ 已完成]    [✅ 已完成]    [✅ 已完成]    [✅ 已完成]    [✅ 已完成]    [⏳ 待开始]    [⏳ 待开始]    [⏳ 待开始]

→ 课表 + 成绩在真机（小米15 / Android 16）上可实际运行
→ 空教室 API + UI 代码已完成，但查询闪退（待修复）
→ 考试安排的数据接口已确认，待开发
→ 进入细化打磨阶段，按 Batch 优先修复 Bug 再做架构变更
```

---

## 打磨阶段计划（按优先级排序）

> 评估日期: 2026-06-05 | 基于代码审查 + 用户反馈的优先级排序

### 排序逻辑

综合考虑三个维度：**用户影响**（崩溃/阻塞/体验）> **代码改动量**（低改动优先做）> **依赖关系**（被依赖的优先）

```
Batch 1: 修 Bug（P0 — 阻塞使用）
  ├── #5 空教室闪退 ──→ 缺三步请求序列中的页面加载步骤，教务系统返回 HTML 导致解析崩溃
  └── #1 登录后需杀应用 ──→ ViewModel 仅在 isCached=true 时订阅 Room Flow，首次登录无缓存时数据静默丢失

Batch 2: 数据正确性（P1 — 核心体验缺陷）
  ├── #4c 按周过滤课程 ──→ WeekViewGrid 未按 displayWeek 过滤 startWeek..endWeek
  └── #2 默认周数为真实当前周 ──→ currentWeek 硬编码为 1，应从学期起始日期计算

Batch 3: UI 打磨（P2 — 课表卡片优化）
  ├── #4a 周一~周五填满屏宽 ──→ 移除 horizontalScroll，改用动态列宽
  ├── #4b 课程名/教师/教室排版 ──→ 调整 CourseCard 字号和顺序
  └── #3 课程详情展开 ──→ 点击 CourseCard → BottomSheet 显示完整信息

Batch 4: 新功能（P3 — 考试安排）
  └── #6 考试安排页面 ──→ 全新三层开发（Repository + ViewModel + Screen），架构模式已成熟可复用

Batch 5: 架构级变更（P4 — 需等 #6 完成后开始）
  ├── #7 单首页设计 ──→ 卡片式入口：下一节课 / 空教室 / 下一场考试 / 成绩
  └── #4d UI/UX Pro Max ──→ 调用 ui-ux-pro-max skill 重新设计课表页（后于 #7，避免风格冲突）
```

### 关键依赖

- **#7 ← #6**: 单首页需展示 "下一场考试" 卡片 → 考试数据必须可用
- **#4d ← #7**: 首页架构定了，课表页设计才有方向
- **Batch 1~2 各项相互独立**，可安全并行

### 各项代码影响评估

| # | 问题 | 影响文件数 | 改动范围 |
|---|------|-----------|----------|
| #5 | 空教室闪退 | ~2 | `RoomRepository` 补页面加载请求；`EASystemApi` 补页面加载接口 |
| #1 | 登录不显示 | ~1 | `ScheduleViewModel.loadInitialData()` — 修复 Flow 订阅时机 |
| #4c | 按周过滤 | ~1 | `WeekViewGrid` / `ScheduleViewModel` — 增加过滤条件 |
| #2 | 默认周数 | ~1 | `UserPreferences` 默认值 或 `ScheduleViewModel` init |
| #4a | 填满屏宽 | ~1 | `WeekViewGrid` — 移除 `horizontalScroll`，计算动态宽度 |
| #4b | 卡片排版 | ~1 | `CourseCard` — 调整 TextStyle |
| #3 | 课程详情 | ~3 | `CourseCard` + `ScheduleViewModel` + 新 `CourseDetailSheet` |
| #6 | 考试页面 | ~5 | `ExamRepository` + `ExamViewModel` + `ExamScreen` + DTO + 导航接线 |
| #7 | 单首页 | ~6+ | `MainActivity` + `AppNavigation` + 新 `HomeScreen` + 各模块入口重构 |
| #4d | UI/UX 重设计 | ~4 | `ScheduleScreen` + `WeekViewGrid` + `CourseCard` + `Theme` |

---

## Phase 0: API 侦察 ✅ 已完成

| # | 任务 | 状态 | 备注 |
|----|------|------|------|
| 0.1 | 获取教务系统网址 | ✅ | `jwgl.hebtu.edu.cn` + CAS `cas.hebtu.edu.cn` |
| 0.2 | 登录流程抓包 | ✅ | CAS RSA加密 + 5次302重定向 + Ticket交换 |
| 0.3 | 课表 API 抓包 | ✅ | 4个接口，完整数据模型已确认 |
| 0.4 | 成绩 API 抓包 | ✅ | 3个接口，含成绩明细子项API |
| 0.5 | 空教室 API 抓包 | ✅ | 4个接口，含双校区楼栋列表 |
| 0.6 | 考试安排 API 抓包 | ✅ | 2个接口，完整字段已确认 |
| 0.7 | API 文档输出 | ✅ | 已写入 architecture.md §5 |
| 0.8 | 补充抓包（下拉选项/成绩明细） | ✅ | 校区楼栋列表、成绩构成明细均已捕获 |

### Phase 0 交付物
- 7 个 HAR 文件（位于 `HAR/` 目录）
- 17 个 API 端点的完整文档
- SSO 认证流程图
- 学期/校区编码规则
- Retrofit 接口代码草案

---

## Phase 1: 项目骨架 ✅ 已完成

| # | 任务 | 状态 | 备注 |
|----|------|------|------|
| 1.1 | Gradle 项目初始化 | ✅ | Version Catalog + 13 开源库 + Kotlin DSL |
| 1.2 | Hilt 依赖注入配置 | ✅ | NetworkModule + DatabaseModule + RepositoryModule |
| 1.3 | Material Design 3 主题 | ✅ | Light/Dark/Dynamic Color + Typography |
| 1.4 | 导航框架 | ✅ | Drawer + 4 Routes (Schedule/Grade/Room/Exam) |
| 1.5 | 多语言基础设施 | ✅ | zh + en, 50+ 字符串键值 |

### Phase 1 交付物
- 45 个文件，完整可编译的 Android 项目骨架
- Room 数据库 + DAO（课表缓存表）
- Retrofit API 接口（17 个端点全覆盖）
- UserPreferences (DataStore)
- 5 个 Repository（Auth/Schedule/Grade/Room/Exam）
- 通知渠道（上课提醒 + 考试提醒）
- Widget 占位框架（Glance 待 Phase 7 实现）
- 4 个占位 Screen（待后续 Phase 填充实现）

---

## Phase 2: SSO 认证模块 ✅ 已完成

| # | 任务 | 状态 | 备注 |
|----|------|------|------|
| 2.1 | SSO WebView 登录 | ✅ | WebView 加载 CAS + 监听 URL 检测登录成功 |
| 2.2 | Cookie/Session 管理 | ✅ | PersistentCookieJar + EncryptedSharedPreferences |
| 2.3 | 自动登录 | ✅ | 启动时检查存储的加密 Cookie |
| 2.4 | 会话过期处理 | ✅ | AuthInterceptor 检测 302/401/403 |
| 2.5 | 登录 UI | ✅ | LoginScreen + LoginViewModel |

### Phase 2 交付物 (7 新文件, 4 修改文件)
- CryptoUtil: RSA 公钥解析 + 密码加密
- SessionManager: EncryptedSharedPreferences Cookie 存储
- PersistentCookieJar: OkHttp CookieJar + 持久化
- AuthInterceptor: 会话过期检测 → 信号发射
- CasApi: CAS REST 接口
- LoginScreen: WebView SSO 登录
- LoginViewModel: 登录状态管理

---

## Phase 3: 课表模块 ✅ 已完成

| # | 任务 | 状态 | 备注 |
|----|------|------|------|
| 3.1 | 数据层 (Room + Repository) | ✅ | API 解析 + JSON → Entity + Room 缓存 |
| 3.2 | 周视图 UI (Grid + Cards) | ✅ | 7列×6行网格 + CourseCard 颜色卡片 |
| 3.3 | ScheduleViewModel | ✅ | 加载/缓存/错误/空 四态 + 周次导航 |
| 3.4 | 刷新 + 周次切换 | ✅ | 缓存优先策略 + 下拉刷新 |

### Phase 3 交付物 (6 文件)
- ScheduleRepository: API fetch → JSON parse → Room cache (cache-first)
- ScheduleViewModel: UiState 管理（Loading/Cached/Error/Empty）
- WeekViewGrid: 7天×6节次可滚动网格，当天高亮
- CourseCard: 彩色课程卡片（名称+教师+教室）
- WeekSelector: 周次导航栏（前/后/本周按钮）
- ScheduleScreen: 完整页面组合（四态全覆盖）

---

## Phase 4: 成绩模块 ✅ 已完成

| # | 任务 | 状态 | 备注 |
|----|------|------|------|
| 4.1 | 数据层 (Repository) | ✅ | 成绩列表 + 明细 + 多学期批量 |
| 4.2 | 成绩 UI (学期列表 + GPA 卡片) | ✅ | GpaCard + SemesterSection 展开列表 |
| 4.3 | GPA 计算器 | ✅ | 策略模式 (4.0/5.0/百分制加权) |
| 4.4 | GradeViewModel + 图表 | ✅ | 明细懒加载 + Vico 趋势折线图 |

### Phase 4 交付物 (8 文件)
- GradeModels.kt: 领域模型 + GpaCalculator (3 种策略)
- GradeRepository: 3 API 端点 + 多学期全量拉取
- GradeViewModel: 展开/折叠 + 明细 BottomSheet + 策略切换
- GpaCard: 顶部 GPA 总览 + FilterChip 策略选择
- SemesterSection: 按学期展开列表 + 颜色分数标签
- GradeDetailSheet: ModalBottomSheet 成绩构成 + 进度条
- GradeTrendChart: Vico 折线图（多学期 GPA 趋势）
- GradeScreen: 四态覆盖（加载/错误/空/数据）

---

## Phase 5: 空教室模块 ✅ 已完成

| # | 任务 | 状态 | 备注 |
|----|------|------|------|
| 5.1 | 数据层 (Repository) | ✅ | 校区信息 + 空教室查询 + 位掩码工具 |
| 5.2 | 筛选面板 UI | ✅ | 校区/楼号下拉 + 周次/星期/节次多选 |
| 5.3 | 结果列表 UI | ✅ | RoomCard（名称+楼栋+楼层+座位数） |
| 5.4 | RoomViewModel | ✅ | 联动筛选 + 懒加载校区数据 |

### Phase 5 交付物 (6 文件)
- RoomModels.kt: 领域模型 + BitmaskUtil（周次/节次位掩码）
- RoomRepository: 校区楼栋列表 + 空教室查询 + 描述工具
- RoomViewModel: 筛选条件联动 + 多选状态管理
- FilterPanel: 6 组筛选控件（校区/楼号/周次/星期/节次/查询）
- RoomList: LazyColumn 结果列表 + RoomCard 卡片
- RoomScreen: 提示/加载/错误/结果/空 五态覆盖

---

## Phase 6: 考试安排

| # | 任务 | 状态 | 备注 |
|----|------|------|------|
| 6.1 | 数据层 (Repository) | ⏳ 待开始 | — |
| 6.2 | 考试列表 UI | ⏳ 待开始 | — |
| 6.3 | 倒计时计算 | ⏳ 待开始 | — |

---

## Phase 7: Widget + 通知

| # | 任务 | 状态 | 备注 |
|----|------|------|------|
| 7.1 | 课表 Widget (Glance) | ⏳ 待开始 | — |
| 7.2 | 上课提醒通知 | ⏳ 待开始 | — |
| 7.3 | 考试提醒通知 | ⏳ 待开始 | — |

---

## Phase 8: 国际化 + 无障碍 + 打磨

| # | 任务 | 状态 | 备注 |
|----|------|------|------|
| 8.1 | 全量中英双语 | ⏳ 待开始 | — |
| 8.2 | 无障碍适配 | ⏳ 待开始 | — |
| 8.3 | 全面错误处理 | ⏳ 待开始 | — |
| 8.4 | 离线体验优化 | ⏳ 待开始 | — |
| 8.5 | 性能优化 | ⏳ 待开始 | — |

---

## 已知问题 & 待决策项

| # | 问题 | 状态 | 备注 |
|----|------|------|------|
| 1 | **登录后需杀应用才能看到课表** | 🔴 P0 待修复 | `ScheduleViewModel` 仅在缓存命中时订阅 Room Flow，首次登录无缓存导致数据静默丢失 |
| 2 | **空教室点击查询即闪退** | 🔴 P0 待修复 | 缺三步请求序列中的页面加载步骤，教务系统门控返回 HTML 而非 JSON |
| 3 | 课表未按周过滤（非本周课程也显示） | 🟡 P1 待修复 | `WeekViewGrid` 不按 `displayWeek` 过滤 `startWeek..endWeek` |
| 4 | 默认周数为第1周而非真实当前周 | 🟡 P1 待修复 | `currentWeek` 硬编码为 1，需从学期起始日期计算 |
| 5 | 课表需横向滑动才能看完整 | 🟢 P2 待优化 | 周一~周日 7列 + 固定 `columnWidth=100dp` + `horizontalScroll` |
| 6 | 场地类别下拉 API 返回空 | 🟢 不影响 | 值可从查询结果 `cdlbmc` 字段提取 |
| 7 | 登出 API 未捕获 | 🟢 不影响 | 清除 Cookie 即可实现登出 |
| 8 | 会话过期响应未捕获 | 🟡 开发中处理 | 通用 401/403 拦截方案 |
| 9 | GPA 具体计算规则 | 🟡 预留扩展 | 4.0/5.0/百分制均支持 |
| 10 | Vico 图表库 API 不兼容 | 🟢 已解决 | 替换为 Compose Canvas 自定义折线图 |

---

## 构建修复 & 版本兼容性

经过 10+ 轮迭代，最终锁定以下兼容版本组合：

| 组件 | 版本 | 备注 |
|------|------|------|
| AGP | `8.7.3` | 锚点版本，不动 |
| Kotlin | `2.2.21` | 最低满足 OkHttp 5.3.2 / Coroutines 1.11.0 的 metadata 2.2.0 要求 |
| KSP | `2.2.21-2.0.5` | 精确匹配 Kotlin 2.2.21（旧格式 `{kotlin}-{ksp}`） |
| Hilt | `2.57.2` | 首个内置 kotlin-metadata-jvm 2.2.0 的版本 |
| Room | `2.7.2` | 修复 `? super Continuation` KSP 代码生成 |
| Gradle | `9.5.1` | 阿里云镜像 + 代理双通道 |

关键踩坑：
- Hilt 2.59.2 要求 AGP 9.0.0+，但阿里云镜像无 AGP 9.x 稳定版
- KSP 版本格式已从 `{kotlin}-1.0.{build}` 变为独立 `X.Y.Z`（2.3.0 起）
- Room 2.6.x 处理 Kotlin 2.2.x 的 `? super Continuation` 协变签名时崩溃（`unexpected jvm signature V`）
- 阿里云 `public` 镜像缺 KSP 2.4.x / AGP 9.x 稳定版 → 不能追最新版本
- `compileSdk = 36` 超出 AGP 8.7.3 测试范围 → 降回 35
- 小米 15（骁龙 8 Elite / 16KB Page）→ 需 `android.experimental.enable16KbPageAlignment=true`

详见 [[教务系统逆向分析]] §构建兼容性清单。

---

## 教务系统 API 逆向分析成果

通过 mitmproxy 对手机浏览器和 App 进行双向抓包对比，获得了教务系统（ZFSOFT 新方正）的关键行为特征：

### 请求伪装要求

教务系统会拒绝非浏览器发起的 API 请求（返回 HTML "无功能权限"）。必须伪装成浏览器 AJAX：

| 请求头 | 浏览器值 | 说明 |
|--------|----------|------|
| `User-Agent` | Chrome Mobile UA | 绝不能暴露 `okhttp/5.3.2` |
| `Accept` | `*/*` | 不能只用 `application/json` |
| `Referer` | 对应功能页面 URL | 课表 API 必须 Referer 课表页面，不是菜单页 |
| `X-Requested-With` | `XMLHttpRequest` | 教务系统用此头判断 AJAX |
| `Origin` | `http://jwgl.hebtu.edu.cn` | 同域 POST 也需携带 |

### 请求序列（关键发现）

浏览器访问课表的完整序列：

```
① GET  index_initMenu.html              → 主菜单页
② POST index_cxBczjsygnmk?gnmkdm=index  → 菜单点击注册 (body: gndm=N2151)
③ GET  cxXskbcxIndex.html               → 课表页面（建立功能上下文）
④ POST cxXsgrkb.html                    → 获取课表数据（JSON）✅
```

每个 API 调用前必须完成三步前置：菜单注册 → 页面加载 → 数据请求。缺一步即返回 HTML 错误页。

### Cookie 与会话

- 教务系统使用 `JSESSIONID` + `jw` 双 cookie
- `PersistentCookieJar.saveFromResponse()` 必须**合并**而非替换 cookie（修复：302 响应只带 `jw` 会覆盖掉 `JSESSIONID`）
- `CookieManager.getCookie()` 必须传完整 URL（`http://host/`）而非裸域名，否则取不到所有 cookie
- 登录后需重启 App 才能正常加载课表——原因待查，疑似 WebView 登录和 OkHttp API 调用之间的 cookie 桥接时序问题

### MITM 抓包数据

| 抓包文件 | 描述 |
|----------|------|
| `mitm/浏览器访问` | 手机 Edge 浏览器成功访问课表（JSON 4053B） |
| `mitm/MyHEBNU` | App 第一次访问（课表 API 返回 HTML 错误） |
| `mitm/MyHEBNU2` | App 第二次访问（同上，证明非偶然） |

详见 [[教务系统逆向分析]]。

---

## 工作环境

| 项 | 值 |
|----|-----|
| Android Studio | `D:\Software\Android\bin\studio64.exe` |
| 代理端口 | `127.0.0.1:7892` (已写入 gradle.properties) |
| Gradle 镜像 | 阿里云 (settings.gradle.kts) |
| Git 仓库 | `D:\Personal_file\VibeCoding\Program\My-University` |
| 当前分支 | main (13 次提交) |

---

## 变更日志

| 日期 | 变更 | 原因 |
|------|------|------|
| 2026-06-04 | 初始化 memory-bank + 8 轮需求沟通 | 项目启动 |
| 2026-06-04 | Phase 0: 7 HAR + 17 API 端点文档 | API 侦察 |
| 2026-06-04 | Phase 1: 45 文件项目骨架 | 项目初始化 |
| 2026-06-04 | Phase 2: SSO 认证 (WebView+CookieJar+EncryptedPrefs) | 认证 |
| 2026-06-04 | Phase 3: 课表 (周视图+Room缓存+CourseCard) | 课表 |
| 2026-06-04 | Phase 4: 成绩 (GPA+明细Sheet+Canvas趋势图) | 成绩 |
| 2026-06-04 | Phase 5: 空教室 (双校区+多条件筛选+位掩码) | 空教室 |
| 2026-06-04 | Git init + 配置代理/镜像/Wrapper | 构建工具链 |
| 2026-06-05 | 修复 30 个编译错误 (Vico→Canvas 替换等) | 首次编译 |
| **2026-06-05** | **🎉 MVP 核心闭环: 83 文件, ~4800 行, 13 次提交** | **里程碑** |
| 2026-06-05 | 真机调试：修复 10+ 构建兼容性问题（版本链） | 首次编译 |
| 2026-06-05 | 真机调试：mitmproxy 双向抓包 + 教务系统行为逆向 | API 调试 |
| 2026-06-05 | 修复：Cookie 合并 / WebView URL / 请求头伪装 / 16KB Page | Bug 修复 |
| 2026-06-05 | 课表 + 成绩在小米15真机验证通过 | 功能验证 |

---

## 图例

| 符号 | 含义 |
|------|------|
| ⏳ | 待开始 |
| 🔄 | 进行中 |
| ✅ | 已完成 |
| ❌ | 已取消 |
| 🔴 | 阻塞项 |
| 🟡 | 需关注 |

---

> **关联文档**: [[design-document]] | [[architecture]] | [[implementation-plan]]
