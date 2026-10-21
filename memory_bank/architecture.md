# architecture.md · 系统架构

- **用途**：给出 MyHEBNU 的整体结构、模块边界与关键架构决策，作为架构子文档的索引。
- **阅读时机**：改结构、加模块、找代码位置、评估改动影响面。
- **最后更新**：2026-10-21
- **状态**：稳定（与 v1.5.0 代码一致）

## 目录

- [一句话概述](#一句话概述)
- [系统全景](#系统全景)
- [分层与数据流](#分层与数据流)
- [缓存策略](#缓存策略)
- [关键架构决策](#关键架构决策)
- [子文档导航](#子文档导航)
- [已知约束与技术债](#已知约束与技术债)
- [历史记录](#历史记录)

## 一句话概述

MyHEBNU 是一个单 Activity 的 Android 12+ 应用：Kotlin + Jetpack Compose（Material 3）构建界面，Hilt 注入，Retrofit/OkHttp 伪装浏览器访问河北师大 ZFSOFT 教务系统（`jwgl.hebtu.edu.cn`，明文 HTTP），Room + DataStore 负责本地缓存与偏好，Glance 提供桌面课表，WorkManager 负责上课/考试提醒。

## 系统全景

| 层/模块 | 职责 | 关键位置 |
| --- | --- | --- |
| 应用入口 | 单 Activity；欢迎页 → 登录页 → 主界面；会话过期自愈；Widget 深链 | `MainActivity.kt`、`MyHebnuApplication.kt` |
| 首页 | 问候语、下一节课/空教室/下一场考试/成绩四张卡片；**纯本地读取，启动零联网** | `ui/home/` |
| 课表 | 周视图网格、5/7 列切换、当前周/假期识别、学期末下学期课表面板、悬浮刷新 | `ui/schedule/`、`data/repository/ScheduleRepository.kt` |
| 成绩 | 按学期展开、GPA 4.0/5.0/百分制、趋势图、成绩明细、缓存优先 | `ui/grade/`、`data/repository/GradeRepository.kt` |
| 空教室 | 双校区、楼栋/周次/星期/节次筛选、分页；**每次实时查询** | `ui/room/`、`data/repository/RoomRepository.kt` |
| 考试 | 当前学期考试列表、倒计时；进入页面联网拉取并写穿 Room | `ui/exam/`、`data/repository/ExamRepository.kt` |
| 设置 | 主题模式、学期设置、高级功能（自由主题色、显示周末列）、关于/更新 | `ui/settings/`、`ui/about/` |
| 数据本地层 | Room v4（课程/考试/成绩）、DataStore 偏好、EncryptedSharedPreferences 凭证 | `data/local/` |
| 数据远端层 | Retrofit 接口、持久 CookieJar、浏览器伪装与 901 识别拦截器、RSA 加密 | `data/remote/` |
| 依赖注入 | Hilt：Network / Database / Repository 模块 | `di/` |
| 桌面 Widget | 3 种 Glance 尺寸；跨进程读 Room + DataStore；深链回 App | `widget/` |
| 后台任务 | 上课提醒（15 min）、考试提醒（60 min）、Widget 午夜刷新 | `worker/`、`MyHebnuApplication.kt` |
| 更新检查 | GitHub Releases `releases/latest`；应用内横幅/手动检查 | `data/repository/UpdateRepository.kt`、`ui/about/` |

## 分层与数据流

```text
Compose Screen
    ↓ 用户事件
ViewModel (StateFlow<UiState>)
    ↓
Repository
    ├── Room            课程 / 考试 / 成绩缓存（Flow 驱动 UI）
    ├── DataStore       学期、偏好、节次时间表、首页均分缓存
    └── Retrofit/OkHttp 教务 API（三步门控 + 浏览器头）
             ↑
    AuthInterceptor（302/401/403/901 → sessionExpired → autoLogin）
```

- **单向数据流**：UI 只读 `UiState`、只发事件；Repository 返回 `Result<T>`，不向上抛异常。
- **跨进程**：Glance Widget 运行在独立进程，通过 `ScheduleWidgetEntryPoint` 读取 Room 与 DataStore（重点是 `periodTimesJson`）。
- **后台**：Worker 直接读 Room/DataStore，不依赖 ViewModel。

## 缓存策略

| 数据 | 读取策略 | 写入/刷新 | 说明 |
| --- | --- | --- | --- |
| 首页 | 只读本地（Room + DataStore） | 页面进入时刷新，回首页由 `observeCache` 更新 | 启动零联网（#25） |
| 课表 | 缓存优先；无缓存才联网 | 进入页面；本地判定过期（末周周日 + 14 天）或手动刷新 | 悬浮刷新仅短按（当前无长按） |
| 成绩 | 缓存优先；无缓存才拉全部学期 | 短按刷最新学期 + 当前学期，长按拉近 3 学年 | v1.5.0 新增 Room 成绩表 |
| 考试 | **联网优先**（每次进页拉取） | 成功后写穿 Room；首页“下一场考试”读缓存 | 分页项用户实测正常，已关闭 |
| 空教室 | 实时查询，不缓存 | 每次筛选/翻页请求 | 分页参数 `queryModel.showCount` |
| Widget | 本地 Room + DataStore | XML 1 小时 + 午夜 Worker + App 内更新 | 无分钟级更新 |

## 关键架构决策

| # | 决策 | 背景与取舍 | 状态 |
| --- | --- | --- | --- |
| 1 | **单 Activity + 手写返回栈**，用 `AnimatedContent` 切页，不用 `NavHost` | 页面数量少、需要统一转场与会话门控；代价是导航能力自建，`navigation-compose` 依赖实际未使用 | 生效 |
| 2 | **首页/课表/成绩缓存优先**，把网络拉取移到各页面进入时 | 降低启动等待与教务服务端压力；代价是可能短暂显示旧数据，用 Toast 与过期判定兜底 | 生效（v1.5.0） |
| 3 | **教务三步门控 + 浏览器请求头伪装** | ZFSOFT 会拒绝非浏览器 AJAX；缺页面加载步骤返回“无功能权限” HTML | 生效 |
| 4 | **学期枚举 `[3,12,16]`**，`16` 为小学期；区分“当前学期”与“展示学期” | 支持小学期与下学期课表预览，同时不把全局学期切走 | 生效 |
| 5 | **节次时间表持久化到 DataStore** | Glance 独立进程拿不到 Repository 内存缓存，曾导致 Widget 时间错误（#28） | 生效 |
| 6 | **Widget 所有尺寸/颜色/圆角使用 Android 资源** | MIUI 把裸 Int 当资源 ID 解析，AOSP 有 fallback、MIUI 没有 | 生效 |
| 7 | **自定义登录页 + RSA 加密 + 加密凭证 + 静默 autoLogin** | 教务登录页为 `login_slogin.html`；302/401/403/901 均触发自愈，失败才回登录页 | 生效 |
| 8 | **选择类交互只用 Material 3 原生控件**（`ExposedDropdownMenuBox` / `SegmentedButton`） | 自研滚轮多次出现惯性落点/中心项回算缺陷，最终回退原生控件 | 生效 |

## 子文档导航

| 文档 | 回答什么问题 |
| --- | --- |
| [modules-and-ui.md](architecture/modules-and-ui.md) | 包怎么分？页面/导航/状态怎么组织？主题与 DI 在哪？ |
| [data-and-storage.md](architecture/data-and-storage.md) | 存了哪些数据？表结构、DataStore 键、迁移与凭证如何管理？ |
| [ea-api.md](architecture/ea-api.md) | 教务怎么登录？各接口参数与响应是什么？门控与 Cookie 怎么处理？ |
| [widget-and-reminders.md](architecture/widget-and-reminders.md) | Widget 与提醒如何工作？有哪些平台限制？ |
| [security.md](architecture/security.md) | 凭证、Cookie、日志、备份、明文 HTTP 的风险与修复优先级 |
| [diagnostics/](diagnostics/) | 出现 901、Widget 异常、Compose 崩溃时先看哪里？ |

## 已知约束与技术债

| 项 | 影响 | 依据/线索 |
| --- | --- | --- |
| 教务仅提供明文 HTTP | 无法启用 HTTPS/证书固定；`usesCleartextTraffic=true` + `network_security_config` 全局放开 | `NetworkModule.BASE_URL`、`AndroidManifest.xml`；详见 [security.md](architecture/security.md)（修复已搁置） |
| 小学期（`16`）成绩尚未接入 | 小学期未开始，接口/展示方式未知；待暴露后再验证，当前不算缺陷 | `GradeRepository.SEMESTER_TERMS` |
| Cookie 明文持久化 + BODY 日志 | 会话可被备份/root/日志读取 | 详见 [security.md](architecture/security.md)（修复已搁置） |
| `navigation-compose` 已引入但未使用 | 依赖冗余，易误以为使用官方导航 | `app/build.gradle.kts`、无 `NavHost` |
| 无自动化测试与 CI；Room `exportSchema=false` | 回归依赖手工真机验收，schema 无版本化产物 | `AppDatabase.kt`、`app/src` 仅 `main` |
| 国际化未完成（搁置） | `values-en` 126 条 vs 默认 133 条，且部分界面硬编码中文；用户决定暂缓 | `res/values-en/`、`AdvancedSettingsScreen.kt` |
| README 仍有旧信息 | 架构图/技术栈表有旧值；版本、变更日志、许可证、缓存策略、Widget 状态已同步 | `README.md` |

> 考试接口分页：用户实测未出现问题（2026-10-21），不再作为待修项。
> 开源许可已于 2026-10-21 统一为 AGPL-3.0 并补齐 `LICENSE`。

> 以上为当前代码可直接验证的事实，不代表最终决定；处理计划见 [implementation-plan.md](implementation-plan.md)。

## 历史记录

- **2026-10-21**：原 `architecture.md`（621 行，含技术栈、包结构、数据模型、API、导航、Widget、通知、错误处理、逆向工程、Compose 规则）按主题拆分。技术栈与构建命令移至 [tech-stack.md](tech-stack.md)；数据模型移至 [data-and-storage.md](architecture/data-and-storage.md)；API 与请求门控移至 [ea-api.md](architecture/ea-api.md)；Widget/提醒移至 [widget-and-reminders.md](architecture/widget-and-reminders.md)；安全项移至 [security.md](architecture/security.md)；诊断复盘移至 [diagnostics/](diagnostics/)。同时修正了“Drawer 导航”“4 种 Widget”“HTTPS 强制”等与当前代码不符的描述；关闭考试分页项，小学期成绩改为待验证。
