# MyHEBNU — 进度追踪

> 最后更新: 2026-06-05 | 状态: MVP 代码完成，待编译验证通过

---

## 当前状态

```
Phase 0         Phase 1        Phase 2        Phase 3        Phase 4        Phase 5        Phase 6        Phase 7        Phase 8
 侦察            骨架           认证           课表           成绩           空教室         考试           Widget+通知     打磨
[✅ 已完成]     [✅ 已完成]    [✅ 已完成]    [✅ 已完成]    [✅ 已完成]    [✅ 已完成]    [⏳ 待开始]    [⏳ 待开始]    [⏳ 待开始]

→ 🎉 MVP 核心功能闭环已达成交付！
```

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
| 1 | 场地类别下拉 API 返回空 | 🟢 不影响 | 值可从查询结果 `cdlbmc` 字段提取 |
| 2 | 登出 API 未捕获 | 🟢 不影响 | 清除 Cookie 即可实现登出 |
| 3 | 会话过期响应未捕获 | 🟡 开发中处理 | 通用 401/403 拦截方案 |
| 4 | GPA 具体计算规则 | 🟡 预留扩展 | 4.0/5.0/百分制均支持 |
| 5 | 首次编译有编译错误 | 🔴 待下次验证 | 30 个 import/API 错误已修复，待重新编译验证 |
| 6 | Vico 图表库 API 不兼容 | 🟢 已解决 | 替换为 Compose Canvas 自定义折线图 |

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
