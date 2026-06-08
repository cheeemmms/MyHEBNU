# 🎓 MyHEBNU

<div align="center">

[![Android](https://img.shields.io/badge/Android-12%2B-34A853?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Material Design 3](https://img.shields.io/badge/Material%20Design-3-FF7043?logo=materialdesign&logoColor=white)](https://m3.material.io)
[![Hilt](https://img.shields.io/badge/Hilt-2.57.2-FF5722?logo=dagger&logoColor=white)](https://dagger.dev/hilt)
[![License](https://img.shields.io/badge/License-MIT-green)](./LICENSE)
[![Release](https://img.shields.io/badge/Release-v1.0.0-blue)](https://github.com/cheeemmms/MyHEBNU/releases)

**河北师范大学非官方教务助手** — Material Design 3 风格的 Android 课表 · 成绩 · 空教室 · 考试 App

</div>

---

## 📖 目录

- [👋 快速上手](#-快速上手)
- [⚙️ 功能状态](#️-功能状态)
- [🛠 技术细节](#-技术细节面向极客)
- [📋 变更日志](#-变更日志)
- [⚠️ 免责声明](#️-免责声明)

---

# 👋 快速上手

## 这个 App 是干嘛的？

把**学校的教务系统装进了手机里**，而且比网页版好看、好用。

| 你想做的事 | 用教务网站 | 用 MyHEBNU |
|-----------|-----------|------------|
| 看课表 | 打开电脑 → 登录 → 点菜单 → 看课表 | 打开 App → 自动登录 → 直接看到 |
| 查成绩 | 登录 → 多次点击 → 一个学期一个学期查 | 一页看完所有学期，自动算 GPA |
| 找空教室自习 | 登录 → 找到对应菜单 → 一步步筛选 | 点两下直接出结果 |
| 看考试安排 | 登录 → 多次点击 | 一页看完，还帮你倒数还剩几天 |

## 怎么安装？

1. 到 [Releases](https://github.com/cheeemmms/MyHEBNU/releases) 页面下载最新的 `.apk` 文件
2. 传到手机上，点击安装
3. 如果提示"未知来源"，选择「允许」即可
4. 打开 App，用你的**学校统一认证账号**登录

> ⚠️ 需要 Android 12 及以上系统

## 怎么用？

### 1️⃣ 登录
首次打开 → 欢迎页 → 输入学号密码 → 登录。之后每次打开自动登录，无需重复输入。如果长时间未使用导致登录过期，App 会自动重新登录，你完全无感。登录页输入错误 3 次会出现验证码。

### 2️⃣ 首页
登录后进入杂志式首页，四张功能卡片一览：

- **下一节课**：显示今天接下来的课程（课程名、教室、教师）。今天没课或已上完会提示"今日课程已结束"或"今天周末好好休息"。点击进入课表。
- **空教室查询**：快速入口，点击进入空教室筛选。
- **下一场考试**：显示最近一场考试的时间、地点、距今天数。无考试时显示"暂无考试安排"。点击进入考试列表。
- **成绩查询**：显示当前学期的加权平均分。点击进入成绩页面。

左上角齿轮图标进入设置。

### 3️⃣ 看课表
首页 → 课程卡片 或 齿轮 → 课表。默认显示当前教学周，周一至周日 + 节次网格。每门课一张彩色卡片：课程名、教师、教室。不同课程自动分配不同颜色。上下滑动查看所有节次，下拉刷新获取最新课表。课表数据缓存在本地，没网也能看。

### 4️⃣ 查成绩
首页 → 成绩卡片 → 按学期展开/折叠 → 一屏看完全部学期 → 顶部显示 GPA。支持 4.0 制 / 5.0 制 / 百分制三种计算方式切换。点击某门课程可展开成绩详细构成（课堂表现、平时作业、期末考试等各子项的百分比权重和得分）。学期 GPA 趋势折线图直观展示成绩变化。

### 5️⃣ 找空教室
首页 → 空教室卡片 → 选校区（老校区 / 裕华校区）和教学楼 → 选周次、星期几、第几节 → 空闲教室列表（名称、楼层、座位数）。

### 6️⃣ 看考试安排
首页 → 考试卡片 → 按时间排列的考试列表 → 每场考试显示科目、日期时间、地点、座位号、考试类型。倒计时标签一目了然：≤3 天标红，≤7 天标黄。

---

# ⚙️ 功能状态

> 图例：✅ 已完成 | 🔴 待修复 | 🔧 开发中 | ⏳ 计划中

## ✅ 已实现

### 🔐 自定义登录 + 自动重登（Batch 5）
- Compose 自定义登录页，无框线胶囊设计 + 呼吸红错误动效
- 学号密码加密存储（EncryptedSharedPreferences）
- Session 过期自动无感重登，失败则提示重新登录
- 首次启动欢迎页（隐私声明 + GitHub 链接）

### 📅 课表 — 周视图（Batch 4 重设计）
- 逐节网格 + 双层架构（绝对定位跨行合并）
- MD3 Tonal Palette 课程配色
- 紧凑排版：课程名 11sp / 教室 10sp / 教师 9.5sp
- 支持切换周次（前后翻周）+ 自动当前周定位
- 数据本地 Room 缓存，离线可查看

### 📊 成绩 — GPA & 趋势
- 按学期展开/折叠，一屏看完全部学期
- GPA 自动计算（4.0 / 5.0 / 百分制）
- 点击课程展开成绩详细构成（各项权重 + 得分）
- Canvas 自定义折线图（学期 GPA 趋势）

### 🏠 空教室 — 多条件筛选
- 双校区支持（老校区 + 裕华校区）
- 筛选项：学年学期、校区、楼号、场地类别、周次、星期几、节次

### 📝 考试安排 — 倒计时
- 按时间排列的考试列表
- MD3 ExamCard + 倒计时 Badge（≤3 天标红）

### 🎨 主题与设置
- 浅色 / 深色 / 跟随系统
- 自由主题色：种子色驱动 → 全局 MD3 色板（高级功能）
- 6 套内置预设 + 自定义 HSL 色轮创建
- 捐赠支持（高级功能诚信激活）

### 🏠 单首页（Batch 4）
- 杂志式布局 + 4 独立 ElevatedCard
- 齿轮设置入口，子页返回箭头

## 🚧 计划中

| 优先级 | 功能 | 说明 |
|--------|------|------|
| P1 | **桌面 Widget** | 桌面小部件显示当天课表 |
| P1 | **上课提醒** | 课前 15 分钟推送通知 |
| P1 | **考试提醒** | 考前 1 天 + 考前 1 小时推送通知 |
| P2 | **中英双语** | 全量国际化 |
| P2 | **无障碍适配** | TalkBack + 高对比度 + 大字体 |

> 📋 详细进度追踪见 [`memory-bank/progress.md`](./memory-bank/progress.md)

---

# 🛠 技术细节（面向极客）

## 架构总览

```
┌──────────────────────────────────────────────────┐
│                    UI Layer                        │
│   HomeScreen  ScheduleScreen  GradeScreen  ...    │
│         ↓            ↓              ↓             │
│   HomeVM      ScheduleVM     GradeVM  (StateFlow) │
├──────────────────────────────────────────────────┤
│               Repository Layer                     │
│   AuthRepo  ScheduleRepo  GradeRepo  RoomRepo     │
│         ↕               ↕            ↕             │
├──────────────────────────────────────────────────┤
│                 Data Layer                         │
│   Room (SQLite)    Retrofit (HTTP)    DataStore    │
│   EncryptedSP      CookieJar          Preferences  │
└──────────────────────────────────────────────────┘
```

- **架构模式**：MVVM + Repository，单 Activity，自定义导航（AnimatedContent）
- **状态管理**：ViewModel + StateFlow，四态覆盖（Loading / Data / Empty / Error）
- **DI**：Hilt 全应用注入
- **缓存策略**：课表 = Cache-First；成绩/空教室 = Network-Only

## 技术栈清单

| 层级 | 库 | 版本 |
|------|-----|------|
| **UI** | Jetpack Compose + Material3 | BOM 2024.12 |
| **导航** | Compose Navigation | 2.8.5 |
| **DI** | Hilt | 2.57.2 |
| **网络** | Retrofit + OkHttp | 3.0.0 / 5.3.2 |
| **数据库** | Room | 2.7.2 |
| **KV 存储** | DataStore | 1.1.1 |
| **加密存储** | EncryptedSharedPreferences + security-crypto | 1.1.0-alpha06 |
| **HTML 解析** | Jsoup | 1.22.2 |
| **图片加载** | Coil | 2.7.0 |
| **Widget** | Glance | 1.1.1 |
| **协程** | Kotlin Coroutines | 1.11.0 |
| **构建** | Gradle KTS + Version Catalog | AGP 8.7.3 |

### 版本兼容性矩阵

```
AGP 8.7.3  ←→  Kotlin 2.2.21  ←→  KSP 2.2.21-2.0.5
                    ↕
              Hilt 2.57.2  ←→  Room 2.7.2
```

## 教务系统逆向分析

通过对手机浏览器和 App 的双向 mitmproxy 抓包，揭示了 ZFSOFT 新方正教务系统的关键行为：

### 请求序列（三步门控）

```
① POST 菜单点击注册 → ② GET 功能页面 → ③ POST 数据请求
```

缺第②步，数据 API 返回 HTML 错误页 "无功能权限"（HTTP 200）。

### 登录流程（HAR 逆向）

```
① GET  login_slogin.html           → 解析 csrftoken
② GET  login_getPublicKey.html     → RSA 公钥 {modulus(base64), exponent(base64)}
③ POST login_slogin.html           → yhm + RSA加密密码 + csrftoken
④ 302 → 提取 JSESSIONID cookie     → 登录成功
```

密码加密：`base64(modulus) → hex → BigInteger → RSAPublicKey → encrypt → hex → base64 → POST`

验证码：3 次失败后触发，图片端点 `/kaptcha`，需与登录请求共享 CookieJar。

### 请求伪装要求

| 请求头 | 值 | 必要性 |
|--------|-----|--------|
| `X-Requested-With` | `XMLHttpRequest` | 🔴 必须 |
| `User-Agent` | Chrome Mobile UA | 🔴 必须 |
| `Referer` | 功能页面 URL | 🟡 建议 |
| `Origin` | `http://jwgl.hebtu.edu.cn` | 🟡 建议 |

> 📋 完整 API 文档见 [`memory-bank/architecture.md` §5](./memory-bank/architecture.md)

## 项目结构

```
app/src/main/java/com/myhebnu/
├── MyHebnuApplication.kt
├── MainActivity.kt
├── di/                             # Hilt Module
├── data/
│   ├── local/db/                   # Room
│   ├── local/preferences/          # DataStore + CredentialManager
│   ├── remote/                     # Retrofit + CookieJar + Interceptor + Crypto
│   └── repository/                 # Auth/Schedule/Grade/Room/Exam
├── domain/                         # GradeModels / ExamModels / RoomModels
├── ui/
│   ├── theme/                      # MD3 Theme/Color (种子色生成)
│   ├── welcome/                    # WelcomeScreen (首次启动)
│   ├── auth/                       # LoginScreen + LoginVM
│   ├── home/                       # HomeScreen + HomeCardPanel
│   ├── schedule/                   # WeekViewGrid + CourseCard + CourseDetailSheet
│   ├── grade/                      # GpaCard + SemesterSection + GradeTrendChart
│   ├── room/                       # FilterPanel + RoomList
│   ├── exam/                       # ExamCard + ExamScreen
│   ├── settings/                   # Settings + Advanced + ColorTheme
│   └── navigation/                 # Route 定义
└── widget/                         # Glance (待实现)
```

## 构建与运行

```bash
# Debug APK（applicationId: com.myhebnu.debug）
./gradlew assembleDebug

# Release APK（applicationId: com.myhebnu，已签名）
./gradlew assembleRelease

# 产物命名：MyHEBNU-v1.0.0-release.apk
```

---

# 📋 变更日志

| 日期 | 变更 |
|------|------|
| 2026-06-04 | 项目启动 + Phase 0 API 侦察（7 HAR + 17 端点） |
| 2026-06-04 | Phase 1~2 — 项目骨架 + SSO 认证 |
| 2026-06-04 | Phase 3~5 — 课表 + 成绩 + 空教室核心功能 |
| 2026-06-05 | **MVP 核心闭环** — 83 文件, 14 次提交, 真机验证通过 |
| 2026-06-05 | Batch 1 — 空教室闪退修复 + 登录数据丢失修复 |
| 2026-06-07 | Batch 2 — 课表按周过滤 + 自动学期探测 + 自动当前周定位 |
| 2026-06-07 | Batch 2.5 — 成绩数据消失修复 |
| 2026-06-07 | Batch 3 — 考试安排模块 |
| 2026-06-07 | Batch 4 — 单首页 + 课表重设计 + 设置 + 6 轮精修 |
| 2026-06-07 | Batch 5 — 自定义登录 + 自动重登 + 欢迎页 + 验证码修复 + UI 重设计 |
| 2026-06-07 | **v1.0.0 发布** — GitHub Release + APK 签名 |

---

# ⚠️ 免责声明

**MyHEBNU 是一个非官方的第三方应用**，与河北师范大学及其教务系统开发商（北京新方正）**无任何关联**。

- 本应用仅作为**个人学习项目**开发，用于探索 Android 开发、网络逆向分析和用户体验设计。
- 所有数据来源于河北师范大学**公开教务系统**，用户需使用自己的合法账号登录。
- 本应用**不收集、不存储、不上传**用户的个人信息到第三方服务器。登录凭证仅加密存储在用户设备本地。
- 使用者应自行承担使用本应用带来的风险。
- 本软件按"**原样**"提供，不提供任何明示或暗示的保证。

---

<div align="center">

**Built with ❤️ + Kotlin + 逆向分析**\
[memory-bank](./memory-bank/) · [Issues](https://github.com/cheeemmms/MyHEBNU/issues) · [Releases](https://github.com/cheeemmms/MyHEBNU/releases)

</div>
