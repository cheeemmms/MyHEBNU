# 🎓 MyHEBNU

<div align="center">

[![Android](https://img.shields.io/badge/Android-12%2B-34A853?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Comose-2024.12-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Material Design 3](https://img.shields.io/badge/Material%20Design-3-FF7043?logo=materialdesign&logoColor=white)](https://m3.material.io)
[![Hilt](https://img.shields.io/badge/Hilt-2.57.2-FF5722?logo=dagger&logoColor=white)](https://dagger.dev/hilt)
[![License](https://img.shields.io/badge/License-MIT-green)](./LICENSE)
[![Status](https://img.shields.io/badge/Status-MVP%20Core%20Complete-brightgreen)]()

**河北师范大学非官方教务助手** — Material Design 3 风格的 Android 课表 · 成绩 · 空教室 · 考试 App

</div>

---

## 📖 目录

- [👋 快速上手（小白友好）](#-快速上手小白友好)
  - [这个 App 是干嘛的？](#这个-app-是干嘛的)
  - [长什么样？](#长什么样)
  - [怎么安装？](#怎么安装)
  - [怎么用？](#怎么用)
  - [遇到问题怎么办？](#遇到问题怎么办)
- [⚙️ 功能状态](#️-功能状态)
  - [✅ 已实现](#-已实现)
  - [🚧 即将实现](#-即将实现)
- [🛠 技术细节（面向极客）](#-技术细节面向极客)
  - [架构总览](#架构总览)
  - [技术栈清单](#技术栈清单)
  - [教务系统逆向分析](#教务系统逆向分析)
  - [项目结构](#项目结构)
  - [构建与运行](#构建与运行)
  - [已知技术债](#已知技术债)
- [📋 变更日志](#-变更日志)
- [⚠️ 免责声明](#️-免责声明)

---

# 👋 快速上手（小白友好）

## 这个 App 是干嘛的？

简单来说，**把学校的教务系统装进了手机里**，而且比网页版好看、好用。

河北师范大学的教务系统（新方正）在电脑浏览器上能用，但在手机上操作很痛苦——页面老旧、按钮太小、每次都要重新登录。MyHEBNU 替你解决了这些烦恼：

| 你想做的事 | 用教务网站 | 用 MyHEBNU |
|-----------|-----------|------------|
| 看课表 | 打开电脑 → 登录 → 点菜单 → 看课表 | 打开 App → 自动登录 → 直接看到 |
| 查成绩 | 登录 → 多次点击 → 一个学期一个学期查 | 一页看完所有学期，自动算 GPA |
| 找空教室自习 | 登录 → 找到对应菜单 → 一步步筛选 | 点两下直接出结果 |
| 看考试安排 | 同上 | 同上，还帮你倒数还剩几天 |

## 长什么样？

App 使用了 **Material Design 3**
- ☀️ 浅色 / 🌙 深色模式自动切换（不完善）
- 🎨 跟随手机壁纸的动态取色（开发中）

## 怎么安装？

> 🔧 目前为开发阶段，尚未发布到应用商店。安装方式：

1. 到 [Releases](https://github.com/cheeemmms/MyHEBNU/releases) 页面下载最新的 `.apk` 文件
2. 传到手机上，点击安装
3. 如果提示"未知来源"，选择「允许」即可
4. 打开 App，用你的**学校统一认证账号**登录（就是查课表用的那个账号密码）

> ⚠️ 需要 Android 12 及以上系统（2021 年后的手机基本都满足）


## 遇到问题怎么办？

1. **登录不上？** 确认账号密码正确（和电脑上登录教务系统的一致）
2. **数据加载不出来？** 下拉刷新试试；如果一直不行，可能是教务系统暂时抽风，等会儿再试
3. **App 闪退？** 到 [Issues](https://github.com/cheeemmms/MyHEBNU/issues) 反馈，写明手机型号和操作步骤
4. **给yonglin200608@163.com**发邮件

---

# ⚙️ 功能状态

> 状态图例：✅ 已完成 | 🟡 待修复 | 🔧 开发中 | ⏳ 计划中

## ✅ 已实现

### 🔐 SSO 统一认证
- WebView 加载学校 CAS 登录页，和浏览器登录体验一致
- 登录凭证加密存储，下次打开自动登录
- 会话过期自动检测

### 📅 课表 — 周视图
- 7 天 × 6 节次的网格布局，当前周高亮
- 每门课一张彩色卡片：课程名、教师、教室、上课周次范围
- 不同课程自动分配不同颜色
- 支持切换周次（前后翻周）、下拉刷新
- 数据本地缓存，没网也能看

### 📊 成绩 — GPA & 趋势
- 按学期展开/折叠，一屏看完全部学期
- GPA 自动计算，支持 4.0 制 / 5.0 制 / 百分制三种方式切换
- 点击课程展开成绩详细构成（各项权重 + 得分）
- 学期 GPA 趋势折线图

### 🏠 空教室 — 多条件筛选
- 双校区支持（老校区 + 裕华校区）
- 筛选项：学年学期、校区、楼号、场地类别、周次、星期几、节次
- 结果卡片：教室名称、楼层、座位数

## 🚧 即将实现

按优先级从高到低排列：

| 优先级 | 功能 | 说明 | 状态 |
|--------|------|------|------|
| P1 | **课表按周过滤** | 当前显示全学期课程，需按教学周过滤 | 🟡 |
| P1 | **真实当前周** | 默认周数从学期起始日期自动计算 | 🟡 |
| P2 | **课表填满屏宽** | 周一~周五自适应填满屏幕 | 🟡 |
| P2 | **课程详情展开** | 点击卡片弹出 BottomSheet 显示完整信息 | 🟡 |
| P3 | **考试安排** | 考试列表 + 倒计时天数 | ⏳ |
| P4 | **单首页设计** | 卡片式入口：下一节课 / 空教室 / 下一场考试 / 成绩 | ⏳ |
| P4 | **UI/UX 全面升级** | 调用专业设计 skill 重新打磨 | ⏳ |
| P5 | **桌面 Widget** | 桌面小部件显示当天课表 | ⏳ |
| P5 | **上课提醒** | 课前 15 分钟推送通知 | ⏳ |
| P5 | **考试提醒** | 考前 1 天 + 考前 1 小时推送通知 | ⏳ |
| P6 | **中英双语** | 全量国际化 | ⏳ |
| P6 | **无障碍适配** | TalkBack + 高对比度 + 大字体 | ⏳ |

> 📋 更详细的进度追踪见 [`memory-bank/progress.md`](./memory-bank/progress.md)

---

# 🛠 技术细节（面向极客）

## 架构总览

```
┌──────────────────────────────────────────────────┐
│                    UI Layer                        │
│   ScheduleScreen  GradeScreen  RoomScreen  ...    │
│         ↓               ↓            ↓            │
│   ScheduleVM      GradeVM      RoomVM  (StateFlow)│
├──────────────────────────────────────────────────┤
│               Repository Layer                     │
│   AuthRepo  ScheduleRepo  GradeRepo  RoomRepo     │
│         ↕               ↕            ↕             │
├──────────────────────────────────────────────────┤
│                 Data Layer                         │
│   Room (SQLite)    Retrofit (HTTP)    DataStore    │
└──────────────────────────────────────────────────┘
```

- **架构模式**：MVVM + Repository，单 Activity 架构
- **状态管理**：每个 ViewModel 暴露 `UiState` sealed class（Loading / Cached / Data / Empty / Error）
- **DI**：Hilt 全应用注入
- **缓存策略**：课表 = Cache-First（先 Room 后网络）；成绩/空教室 = Network-Only（实时性要求）

## 技术栈清单

| 层级 | 库 | 版本 | 选型理由 |
|------|-----|------|----------|
| **UI** | Jetpack Compose + Material3 | BOM 2024.12 | 声明式 UI，Material You |
| **导航** | Compose Navigation | 2.8.5 | 类型安全路由 |
| **DI** | Hilt | 2.57.2 | 唯一兼容 Kotlin 2.2.21 的版本 |
| **网络** | Retrofit + OkHttp | 3.0.0 / 5.3.2 | 成熟的 HTTP 栈 |
| **数据库** | Room | 2.7.2 | 修复了 Kotlin 2.2.x KSP 代码生成 |
| **KV 存储** | DataStore | 1.1.1 | 偏好设置 |
| **加密存储** | EncryptedSharedPreferences | - | 登录凭证 |
| **HTML 解析** | Jsoup | 1.22.2 | 教务系统偶返 HTML |
| **图表** | Canvas 自定义 | - | 原 Vico 兼容性问题，手工实现 |
| **Widget** | Glance | 1.1.1 | Compose 风格 Widget（待启用） |
| **协程** | Kotlin Coroutines | 1.11.0 | 异步/Flow |
| **构建** | Gradle KTS + Version Catalog | AGP 8.7.3 | 声明式依赖管理 |

### 版本兼容性矩阵（踩坑总结）

```
AGP 8.7.3  ←→  Kotlin 2.2.21  ←→  KSP 2.2.21-2.0.5
                    ↕
              Hilt 2.57.2  ←→  Room 2.7.2
```

关键约束：
- **阿里云 Maven 镜像** 缺 KSP 2.4.x / AGP 9.x 稳定版 → 不能追最新
- Kotlin ≥ 2.2.0 才能读 metadata 2.2.0（OkHttp / Coroutines 的新格式）
- Hilt ≥ 2.57 才内置 kotlin-metadata-jvm 2.2.0
- Room ≥ 2.7.0 才正确生成 `? super Continuation` 的协变签名
- 小米 15（骁龙 8 Elite / 16KB Page）需 `enable16KbPageAlignment=true`

## 教务系统逆向分析

通过对手机浏览器和 App 的双向 mitmproxy 抓包，揭示了 ZFSOFT 新方正教务系统的关键行为：

### 请求序列（三步门控）

```
① POST 菜单点击注册 → ② GET 功能页面 → ③ POST 数据请求
```

**每一步都是必需的。** 缺第②步，数据 API 返回 HTML 错误页 "无功能权限"（HTTP 200）。

### 请求伪装要求

| 请求头 | 值 | 必要性 |
|--------|-----|--------|
| `X-Requested-With` | `XMLHttpRequest` | 🔴 必须 — 服务器用此头判断 AJAX |
| `User-Agent` | Chrome Mobile UA | 🔴 必须 — 暴露 `okhttp` 会被拒 |
| `Referer` | 功能页面 URL | 🟡 建议 — 增强合法性 |
| `Origin` | `http://jwgl.hebtu.edu.cn` | 🟡 建议 — 同域 POST 需携带 |

### API 端点总结

| 模块 | 代码 | 核心端点数 |
|------|------|-----------|
| 课表 | N2151 | 4 个 |
| 成绩 | N305007 | 3 个（列表 + 明细 + 汇总） |
| 空教室 | N2155 | 4 个（校区信息 + 查询 + 周次/节次描述） |
| 考试 | N358105 | 2 个（安排 + 字典） |
| SSO 认证 | CAS | 7 步重定向链 |

> 📋 完整 API 文档见 [`memory-bank/architecture.md` §5](./memory-bank/architecture.md)

### Compose 嵌套滚动规则

**Compose 不允许在垂直滚动容器内嵌套另一个垂直滚动容器。** 违反会抛 `IllegalStateException: infinity maximum height constraints`。

| ✅ 正确 | ❌ 错误 |
|---------|---------|
| Screen 用 `Column(verticalScroll)`，子组件用 `Column + forEach` | Screen 用 `Column(verticalScroll)` 里嵌套 `LazyColumn` |
| Scaffold 中用 `LazyColumn(Modifier.fillMaxSize())` | `AnimatedContent` 子级用 `LazyColumn` |

> 这是 Batch 1 空教室闪退的主因——8 次提交才定位到三层根因中的 Compose 布局层。

## 项目结构

```
app/src/main/java/com/myhebnu/
├── MyHebnuApplication.kt          # @HiltAndroidApp
├── MainActivity.kt                 # 唯一 Activity
├── di/                             # Hilt Module (Network/Database/Repository)
├── data/
│   ├── local/db/                   # Room Entity + DAO + Database
│   ├── local/preferences/          # DataStore
│   ├── remote/                     # Retrofit API + DTO + Interceptor
│   └── repository/                 # Auth/Schedule/Grade/Room/Exam Repository
├── domain/model/                   # 领域模型 (Course/Grade/EmptyRoom/Exam)
├── ui/
│   ├── theme/                      # MD3 Theme/Color/Typography
│   ├── navigation/                 # NavGraph + Drawer
│   ├── auth/                       # LoginScreen + LoginViewModel
│   ├── schedule/                   # ScheduleScreen + WeekView + CourseCard
│   ├── grade/                      # GradeScreen + GpaCard + TrendChart
│   ├── room/                       # RoomScreen + FilterPanel + RoomList
│   └── exam/                       # ExamScreen (占位)
├── widget/                         # Glance Widget (占位)
└── worker/                         # WorkManager (占位)
```

## 构建与运行

### 前置条件
- Android Studio (任意较新版本)
- JDK 17
- Gradle 9.5.1（Wrapper 自带）

### 构建命令

```bash
# Debug APK
./gradlew assembleDebug

# 安装到连接的设备
./gradlew installDebug

# Release APK (混淆 + 资源压缩)
./gradlew assembleRelease
```

### 代理配置（国内网络环境）

```bash
# gradle.properties 中已配置代理
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7892
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7892
```

Maven 镜像使用阿里云（配置于 `settings.gradle.kts`）。

## 已知技术债

- **Vico → Canvas**：原选型 Vico 图表库 API 不兼容，已用手写 `Canvas` 折线图替代，后续可考虑迁移到成熟库
- **Cookie 桥接**：WebView 登录和 OkHttp API 调用之间的时序问题偶发，重启 App 可解决
- **教务系统 HTML 依赖**：部分接口返回格式不稳定，`RoomRepository` 内有 HTML 守卫逻辑

> 🔍 完整已知问题清单见 [`memory-bank/progress.md` §已知问题](./memory-bank/progress.md)

---

# 📋 变更日志

| 日期 | 变更 |
|------|------|
| 2026-06-04 | 项目启动 — 8 轮需求沟通 + Phase 0 API 侦察（7 HAR + 17 端点） |
| 2026-06-04 | Phase 1~2 — 项目骨架 + SSO 认证 |
| 2026-06-04 | Phase 3~5 — 课表 + 成绩 + 空教室核心功能 |
| 2026-06-05 | **MVP 核心闭环** — 83 文件, 14 次提交, 真机验证通过 |
| 2026-06-05 | Batch 1 — 空教室闪退修复（8 次提交）+ 登录数据丢失修复 |
| 2026-06-05 | 编译器警告清零 + GitHub 仓库发布 |

---

# ⚠️ 免责声明

**MyHEBNU 是一个非官方的第三方应用**，与河北师范大学及其教务系统开发商（北京新方正）**无任何关联**。

- 本应用仅作为**个人学习项目**开发，用于探索 Android 开发、网络逆向分析和用户体验设计。
- 所有数据来源于河北师范大学**公开教务系统**，用户需使用自己的合法账号登录。
- 本应用**不收集、不存储、不上传**用户的个人信息到第三方服务器。登录凭证仅加密存储在用户设备本地。
- 使用者应自行承担使用本应用带来的风险，包括但不限于：教务系统服务中断、数据展示差异、账号安全等。
- 如学校教务系统更新导致 API 兼容性问题，本应用可能暂时无法正常使用，开发者会尽力跟进适配。
- 本软件按"**原样**"提供，不提供任何明示或暗示的保证。

---

<div align="center">

**Built with ❤️ + Kotlin + 逆向分析**\
[memory-bank](./memory-bank/) · [Issues](https://github.com/cheeemmms/MyHEBNU/issues)

</div>
