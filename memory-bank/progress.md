# MyHEBNU — 进度追踪

> 最后更新: 2026-06-08 | 状态: 设置页重构 ✅。Phase 7.1 Widget 实施中 ⏳（API 适配受阻，详细记录见 [[glance-api-research]]）

---

## 当前状态

```
Phase 0         Phase 1        Phase 2        Phase 3        Phase 4        Phase 5        Phase 6        Phase 7        Phase 8
 侦察            骨架           认证           课表           成绩           空教室         考试           Widget+通知     打磨
[✅ 已完成]     [✅ 已完成]    [✅ 已完成]    [✅ 已完成]    [✅ 已完成]    [✅ 已验收]    [✅ 已完成]    [⏳ 待开始]    [🔄 进行中]

→ 课表 + 成绩 + 空教室 在真机（小米15 / Android 16）上验证通过
→ Batch 1 (P0) 完成：空教室查询闪退修复 + 登录数据丢失修复
→ Batch 2 (P1) 完成：课表按周过滤 + 自动学期探测 + 自动当前周定位
→ Batch 2.5 (P0) 完成：成绩数据消失修复 — getAllGrades() 错误传播 + ViewModel 内存缓存 + 页面进入主动刷新
→ Batch 3 (P1) 完成+验证：考试安排模块
→ Batch 4 (P2) 完成+验证：单首页 + 课表重设计 + 设置 + 6轮精修
  ├─ 首页: 杂志式布局 + 4独立ElevatedCard + 齿轮设置入口
  ├─ 课表: 逐节网格 + 双层架构(绝对定位跨行) + MD3 Tonal Palette + 紧凑排版
  ├─ 色彩: 6套预设模板 + HSL色轮 + WCAG对比度
  └─ 导航: 删除抽屉, 首页⚙→设置, 子页←返回首页
→ Batch 5 (P0+P3) 完成+验证：自定义登录 + 自动重登 + 欢迎页
  ├─ 登录: 自定义Compose UI + 无框线胶囊设计 + 呼吸红错误动效 + 验证码展开动画
  ├─ 凭证: EncryptedSharedPreferences 加密存储学号+密码
  ├─ 自动重登: Session过期(302) → autoLogin(加密凭证) → 无感恢复 / 失败→提示重新登录
  ├─ 欢迎页: 仅首次启动展示, 隐私声明+GitHub+胶囊按钮
  ├─ 验证码修复: loginClient共享CookieJar加载/kaptcha; logoutAccount仅首次调用
  └─ 导航: 登录页移除"用浏览器登录"→设置页"账号"区块
```

---

## 打磨阶段计划（按优先级排序）

> 评估日期: 2026-06-05 | 基于代码审查 + 用户反馈的优先级排序

```
Batch 1: 修 Bug（P0 — 阻塞使用）✅ 已完成
  ├── #5 空教室闪退 ──→ Compose 嵌套滚动容器 + 三步序列 + gnmkdm 参数
  └── #1 登录后需杀应用 ──→ ViewModel Flow 仅在缓存命中时订阅

Batch 2: 数据正确性（P1）✅ 已完成
  ├── #4c 按周过滤课程 ──→ combine(Room Flow, displayWeek) + filterCoursesByWeek()
  ├── #2 默认周数为真实当前周 ──→ N2154 + 手机日期比对, 自动计算 currentWeek
  ├── 学期自动切换 ──→ guessCurrentSemester() + API 验证, 假期保护回退
  └── 奇偶周过滤 ──→ CourseEntity + oddEven 字段, filter 时检查

Batch 2.5: 成绩数据加载修复（P0 — 数据消失）✅ 已完成
  └── #8 成绩打开过一会就显示无成绩 ──→ getAllGrades() fold 收集失败 + ViewModel in-memory cache + GradeScreen LaunchedEffect 主动刷新

Batch 3: 考试安排（P1 — Batch 4 前置依赖）✅ 已完成
  ├── #6 考试安排页面 ──→ ExamRepository(registerMenuClick+API+JSON解析) + ViewModel(StateFlow) + Screen(AnimatedContent+TopAppBar+LazyColumn) + ExamCard(MD3 ElevatedCard+倒计时Badge)
  └── 新建 4 文件: domain/ExamModels.kt + ui/exam/ExamViewModel.kt + ui/exam/ExamScreen.kt + ui/exam/components/ExamCard.kt

Batch 4: 架构级变更（P2）✅ 已完成 + 6轮精修
  ├── 导航架构重构: 全局TopAppBar移除 → 各页面独立Scaffold + 子页←返回箭头
  ├── #7 单首页: 杂志式布局 + 4独立ElevatedCard + ⚙设置入口
  ├── #4e 课表重设计: 逐节网格 + 双层架构(绝对定位跨行) + MD3 Tonal Palette + CJK断行
  ├── 设置: 深色/浅色/跟随系统下拉 + 高级功能 + 色彩模板+HSL色轮+WCAG
  ├── 卡片排版: 课程名11sp/教室10sp/教师9.5sp + 3dp间距 + 2dp呼吸
  └── 导航简化: 抽屉删除 → 仅首页⚙设置入口

Batch 5: HTTP 302 + 自定义登录 + 欢迎页（P0+P3）✅ 已完成

Batch 6: 导航 + 主题 Bug 修复（P0+P1）✅ 已完成（2026-06-08）
  ├── #6a 捐赠展示: 开关关闭时展示收款二维码 + 图注文案；开关打开时收起 ✅
  ├── #6b 自由主题色: 三层联动修复 ✅
  │     ├─ ① 实时生效 → MainActivity: LaunchedEffect+.first() → collectAsState() 持续观察
  │     ├─ ② 开关误关 → SettingsViewModel: 移除 useCustomColors 条件绑定 (不再 && activePreset)
  │     ├─ ③ 课表联动 → ScheduleViewModel: colorPrefsFlow + combine + seedOffset 传入 assignCourseHues
  │     ├─ ④ 预设改名 → Color.kt: "彩虹色系"→"彩虹" 等六个去"系"
  │     └─ ⑤ 色相条 → ColorThemeScreen: HueSliderBar 彩虹渐变替换单色 Slider
  ├── #6c 多级页面导航返回失效 ✅
  │     └─ 修复: previousRoute String → backStack mutableStateListOf + navigateTo/goBack 函数
  ├── #6d 系统返回手势直接退出应用 ✅
  │     └─ 修复: BackHandler(enabled = currentRoute != "home") 拦截系统返回 → goBack()
  ├── #6e 深色模式页面切换闪屏 ✅ (2026-06-08)
  │     └─ 修复: MainActivity AnimatedContent 外包 Box(background=theme) 防白底穿透
  │           + Theme SideEffect → DisposableEffect 优化状态栏同步
  ├── #6f 捐赠图注更改 ✅
  │     └─ 图注1 → "诚信捐赠，本项目所有收入将用于支持项目开发"
  └── #6g 首页成绩修正 + GpaCard排版 ✅
        ├─ HomeViewModel: 全学期展平 → 真·本学期(year/term过滤)
        ├─ GpaCard: CenterHorizontally → Start 左对齐
        └─ GradeScreen: GpaCard下方免责声明

Batch 7: 数据完整性 + 首页上课状态 + GPA（P1）✅ 已完成
  ├── #7a 空教室只显示 10 个 — 分页查询 ✅ (2026-06-08)
  ├── #7b 首页上课中状态识别 + 距下课倒计时 ✅ (2026-06-08)
  │     ├─ 根因: computeNextClass() 只找"下一节"不识别"当前节",
  │     │   estimateCurrentPeriod() 硬编码12区间与实际13节次表不匹配,
  │     │   HOLIDAY 误判(数据未加载时 courses.any 返回 false)
  │     ├─ 修复: 新增 ScheduleRepository.fetchPeriods() 调 API (/kbcx/xskbcx_cxRjc.html) 获取真实13节时间表,
  │     │   computeNextClass() 重写 → 用真实时间逐课程比对: 优先检测 IN_CLASS(距下课倒计时),
  │     │   其次 HAS_CLASS(查找未开始的最近课程), HOLIDAY 仅数据已加载且无附近课程时触发
  │     ├─ 节次表: buildPerPeriodLabels() 硬编码11节删除 → API 数据(fallback=真实13节)
  │     ├─ 课表布局: WeekViewGrid 固定55dp行高 + verticalScroll, 表头固定不滚动
  │     ├─ 主页卡片: 新增 NextClassState.IN_CLASS, 标题动态切换"正在上课"/"下一节课",
  │     │   卡片显示"距下课 XX分钟"倒计时
  │     └─ 共 6 files: EASystemApi + ScheduleRepository + ScheduleViewModel + WeekViewGrid + HomeViewModel + HomeCardPanel
  └── #7c GPA 算法适配本校 ✅ 已确认
        └─ 当前: 标准北大4.0 + 标准5.0 + 百分制加权，公式无逻辑错误
            用户已确认，无需调整

Batch 8: 应用生态 — 反馈 + 更新通道（P2 新功能）✅ 已完成 (2026-06-08)
  ├── #8a 设置页展示邮箱 + QQ交流群 ✅
  │     └─ 新增"联系与反馈"section: 联系邮箱(mailto:) + QQ交流群(复制到剪贴板)
  │         + SettingsTappableItem 可交互组件 + 占位符待用户填充真实信息
  ├── #8b 启动时 GitHub Releases API 检测更新 + 通知 ✅
  │     └─ GitHubApi 调 /repos/cheeemmms/MyHEBNU/releases/latest
  │         + UpdateRepository: semver 比对 + 自动/手动双模式 + 通知 + dismiss 去重
  │         + 设置页"检查更新"多态按钮(空闲/检查中/已最新/发现新版本/失败)
  │         + HomeViewModel init 自动检查 fire-and-forget
  │         + 新增 app_update 通知渠道
  └─ 共 14 files: 3 new + 11 modified. 编译通过.

Batch 9: 设置页胶囊重构 + 关于页面 + 系统与更新（P2 UI 重设计）✅ 已完成 (2026-06-08)
  ├── 设置页胶囊卡片重构 ✅
  │     └─ 全部项目用 ElevatedCard(shape=16dp, elevation=0) 包裹
  │         + 外观/教务/高级/账号/关于 五个胶囊 section
  ├── 关于页面 ✅
  │     ├─ App 图标(80dp, 自定义 app_icon.png) + MyHEBNU 标题 + 版本号
  │     ├─ 社区与源码 Card: 查看源码(GitHub) + 加入交流群(QQ复制) + 联系邮箱(mailto)
  │     ├─ 许可证 Card: 开源协议(AGPL-3.0 URL) + 第三方开源证书(AlertDialog内联)
  │     └─ 系统与更新 Card → 导航到二级页面
  ├── 系统与更新二级页面 ✅
  │     ├─ 自动检查更新 Switch (UserPreferences.autoCheckUpdate, 默认 true)
  │     ├─ 检查更新按钮(多态: 检查/检查中/已最新/发现新版本)
  │     └─ 更新日志卡片 (v1.0.0 changelog)
  ├── HomeViewModel 自动检查尊重 autoCheckUpdate 偏好
  └── 共 6 files: AboutScreen + SystemUpdateScreen + SettingsScreen 重写 + MainActivity + SettingsViewModel + UserPreferences
```

### 关键依赖 (修订)

```
Batch 2.5 ──→ Batch 3 ──→ Batch 4 ──→ Batch 5
 (独立)      (考试数据    (单首页需要   (课表UI细节
              是首页卡片    考试卡片)    在重设计后
              的前置)                   再打磨)

Batch 6 (导航/主题修复) ──→ Batch 7 (数据/首页状态)
  独立, 无前置依赖              独立, 可并行
                              
Batch 6 + 7 ──→ Batch 8 (应用生态) → Phase 7 (Widget+通知) → Phase 8 (国际化+打磨)
                 可选, 低优先级

- **Batch 2.5 独立**: 成绩加载策略修复不依赖任何后续批次
- **Batch 3 ← 2.5**: 可选并行, 但成绩修复更快更紧急
- **Batch 4 ← Batch 3**: 单首页需展示 "下一场考试" 卡片
- **Batch 4 ← 4**: `ui-ux-pro-max` 重设计需等首页架构 (导航/抽屉/入口) 确定
- **Batch 5 ← Batch 4**: 课表 UI 细节等重设计完成后再打磨, 避免返工

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

## Phase 6: 考试安排 ✅ 已完成

| # | 任务 | 状态 | 备注 |
|----|------|------|------|
| 6.1 | 数据层 (Repository) | ✅ | registerMenuClick("N358105") + getExams + JSON items[] 解析 |
| 6.2 | 考试列表 UI | ✅ | MD3 CenterAlignedTopAppBar + AnimatedContent 四态 + LazyColumn |
| 6.3 | 倒计时计算 | ✅ | LocalDate 比对 + 色彩规则(≤3d error / ≤7d tertiary / ≤14d onSurfaceVariant) + ExamCard MiniBadge |

---

## Phase 7: Widget + 通知

| # | 任务 | 状态 | 备注 |
|----|------|------|------|
| 7.1 | 课表 Widget (Glance) | 🔴 阻塞 | Glance 1.1.1 API 不兼容 — `provideContent`/`Dimension.Dp` 不可用，详见 [[glance-api-research]] |
| 7.2 | 上课提醒通知 | ⏳ 待开始 | — |
| 7.3 | 考试提醒通知 | ⏳ 待开始 | — |

### 7.1 进度摘要

- **已完成**：4 种 Widget 配置文件(XML) + 共享数据加载层 + 4 个 GlanceAppWidget 子类框架 + Hilt EntryPoint + 颜色/排版工具 + 4 个 Receiver + Worker/UpdateManager 基础设施 + Deep Link 导航
- **阻塞于**：`provideContent` 在 Glance 1.1.1 中并非 `GlanceAppWidget` 成员函数；`Dimension.Dp` 构造函数私有，无法创建 dp 值
- **推荐方案**：升级 Glance 到 1.2.0+（需确认阿里云 Maven 镜像可用性）
- **已创建 14 个文件，修改 7 个文件，删除 3 个文件** — 代码在 `main` 分支待编译修复
- **详细记录**：[[glance-api-research]]

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

| # | 问题 | 状态 | 批次 | 根因摘要 |
|---|------|------|------|----------|
| **6b** | 自由主题色：①选色后需杀应用 ②开关误关 ③课表颜色不跟随 ④需实时生效 | 🟢 已修复 | Batch 6 | 三层修复：(1) MainActivity collectAsState 持续观察；(2) SettingsViewModel 移除条件绑定；(3) ScheduleViewModel colorPrefsFlow+seedOffset；附加：预设去"系"+彩虹色相条 |
| **6c** | 设置→高级→自由主题色，进入后卡死无法返回 | 🟢 已修复 | Batch 6 | `previousRoute` String → `backStack` mutableStateListOf + `navigateTo`/`goBack` |
| **6d** | 系统返回手势（侧滑）直接退出到桌面 | 🟢 已修复 | Batch 6 | `BackHandler(enabled = currentRoute != "home")` 拦截系统返回 → `goBack()` |
| **6e** | 深色模式下切换页面闪屏 | 🟢 已修复 | Batch 6 | `AnimatedContent` crossfade 暴露 Scaffold 白底 + `Box(background)` wrapper + `SideEffect→DisposableEffect` |
| **7a** | 空教室查询只显示 10 个 | 🟢 已修复 | Batch 7 | API 参数名错误(`rows`→`queryModel.showCount`) + 未解析 totalPage + 新增 PageBar 翻页条 |
| **7b** | 上课中主页显示"放假中"或未识别当前课程 | 🟢 已修复 | Batch 7 | 新增 fetchPeriods() 调 API 获取13节时间表; computeNextClass() 重写用真实时间逐课程比对; 优先 IN_CLASS(倒计时) → HAS_CLASS → ALL_DONE; 删除硬编码 estimateCurrentPeriod(); 修复 HOLIDAY 仅数据已加载时触发 |
| **8a** | 缺少内置反馈通道 | 🟢 已修复 | Batch 8 | 关于页"联系与反馈": 邮箱(mailto) + QQ群(复制) |
| **8b** | 缺少应用更新检测 | 🟢 已修复 | Batch 8 | GitHubApi + UpdateRepository + 关于页检查更新 + 自动检查开关 |
| **9** | 关于页闪退 | 🟢 已修复 | Batch 9 | `R.mipmap.ic_launcher` adaptive-icon XML → `R.drawable.app_icon` PNG |
| **10** | 第三方开源证书不可用 | 🟢 已修复 | Batch 9 | Google OSS Licenses 插件不在阿里云镜像 → AlertDialog 内联展示 |
| **1** | ~~登录后需杀应用才能看到课表~~ | 🟢 已修复 | Batch 1 | `ScheduleViewModel` Flow 订阅移至独立协程 |
| **2** | ~~空教室点击查询即闪退~~ | 🟢 已修复 | Batch 1 | Compose 嵌套滚动容器 + 数据层三步序列 + gnmkdm 参数 |
| **3** | 课表未按周过滤（非本周课程也显示） | 🟢 已修复 | Batch 2 | combine(Room Flow, displayWeek) + filterCoursesByWeek |
| **4** | 默认周数为第1周而非真实当前周 | 🟢 已修复 | Batch 2 | N2154 getWeeksBySemester + 手机日期比对自动计算 |
| **5** | 课表需横向滑动才能看完整 | 🟡 已知 | — | 设计取舍：周视图需滚动 5 天列 |
| **8** | ~~成绩页数据显示后过一段时间消失~~ | 🟢 已修复 | Batch 2.5 | getAllGrades() fold + ViewModel 内存缓存 + LaunchedEffect 主动刷新 |
| **14** | ~~登录后一段时间访问考试/成绩报 HTTP 302~~ | 🟢 已修复 | Batch 5 | autoLogin() 用加密凭证自动重登 |

---

## 空教室闪退诊断：教训与沉淀

> 这是整个项目迄今为止最顽固的 Bug。8 次提交、4 轮 logcat 分析、从数据层猜到 Compose 布局才最终定位。以下教训值得记录。

### 为什么花了这么长时间？

| 失误 | 表现 | 正确做法 |
|------|------|----------|
| **隧道视野** | 看到"点击查询→闪退"，始终认定崩溃在数据层（网络/JSON/门控），反复修改 `RoomRepository`。实际崩溃在 Compose 渲染层。 | 先看 crash 堆栈，再猜测原因。堆栈中 `LazyColumn` 相关的错误不可能是 JSON 解析导致的。 |
| **logcat 过滤过严** | 第一轮 logcat 只过滤 `tag:MyHEBNU`，FATAL EXCEPTION（tag:AndroidRuntime）被排除。零 crash 堆栈导致盲猜 3 轮。 | crash 诊断 logcat 应**不加 tag 过滤**。或至少同时包含 `AndroidRuntime` + `MyHEBNU`。 |
| **修复不彻底** | 发现 `RoomScreen` 的 `LazyColumn` 问题并修了，但未检查子组件 `RoomList` 中的另一个 `LazyColumn`——同一个 bug 藏了两处。 | 修复嵌套滚动 bug 时，`grep LazyColumn` 全局搜索所有同名模式。 |
| **"闪退"的歧义** | "点击按钮后崩溃" 让人以为崩溃在按钮的 onClick 逻辑中。实际是在点击触发的状态变更后的 **Compose 重组/渲染** 阶段崩溃。 | Compose 中，"操作后闪退" 往往 ≠ "操作逻辑崩溃"，= 重组崩溃。先检查 stack trace 的函数栈（是 `measure` 还是 `onClick`）。 |
| **空数据误导** | 26-27 学年返回空数据不崩溃（`rooms.isEmpty → LazyColumn 不渲染`），制造了"数据层面没问题"的假象。 | 有数据才触发的崩溃 ≠ 数据格式问题。区分"有数据崩/空数据不崩" — 这是经典的渲染层条件触达 bug。 |

### 实际根因（三层）

1. **Compose 布局层**（主因）：`RoomScreen: Column(verticalScroll)` 内部放 `RoomList: LazyColumn`——嵌套垂直滚动容器，Compose 给 LazyColumn 传了 `infinity max-height`，测量阶段直接 `IllegalStateException`。
2. **数据层**（次因）：`getCampusInfo()` 缺少页面加载步骤，教务系统 302 拒绝，楼栋列表为空。
3. **API 层**（次因）：`registerMenuClick` 的 `gnmkdm` 参数默认空串，应为 `"index"`；返回类型 `Response<String>` 让 Gson 解析文本"操作成功！"失败。

### 修复文件汇总

| 层 | 文件 | 改动 |
|----|------|------|
| Compose | `RoomScreen.kt` | `LazyColumn` → `Column(verticalScroll)` |
| Compose | `RoomList.kt` | `LazyColumn` → `Column + forEach` |
| API | `EASystemApi.kt` | 新增 `loadRoomPage()`；`registerMenuClick` gnCode 默认值 `""→"index"`，返回类型 `String→ResponseBody`；`getCampusBuildingInfo`/`getEmptyRooms` 返回类型 `JsonObject→ResponseBody` |
| 数据 | `RoomRepository.kt` | `readJsonBody()` Helper 绕过 Gson；`getCampusInfo()` 加三步序列；`queryEmptyRooms()` 加三步序列 + HTML 守卫 + 全链路日志 |
| UI | `RoomViewModel.kt` | 校区标签"校区2"→"红旗校区"；必填字段客户端校验（星期/节次空时拒绝请求）；新增学年/学期 setter |
| UI | `FilterPanel.kt` | 新增学年/学期下拉；所有下拉选中后自动关闭(`closeMenu` 回调) |

### 可复用的检查清单

以后遇到 Compose 应用"操作后闪退"问题：
1. **先看 stack trace**，不是先猜原因
2. logcat **不加 tag 过滤**
3. 在 stack trace 中搜索 `LazyColumn`、`LazyRow`、`scroll`、`measure`——如果在 measure 阶段崩溃，是布局问题
4. `grep LazyColumn` 全局排查嵌套滚动
5. 区分"空数据不崩/有数据崩"——这是条件渲染触发的布局崩溃的特征信号

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
| 当前分支 | main (17+ 次提交) |

---

## 变更日志

| 日期 | 变更 | 原因 |
|------|------|------|
| **2026-06-08** | **Batch 6 扩展 + 新增 Batch 7/8 — 6 项真机反馈评估** | **计划更新** |
| → | Batch 6 扩展: #6d 系统返回手势退出 + #6e 深色模式闪屏 | 新增 P0/P1 Bug |
| → | Batch 7 新建: #7a 空教室分页 + #7b 首页上课中状态+倒计时 | 新增 P1 数据+体验 |
| → | Batch 8 新建: #8a 反馈通道 + #8b GitHub 更新检测 | 新增 P2 功能 |
| → | #6c 精确定位: 设置→高级→自由主题色 3级导航返回失效 | 用户反馈复现路径 |
| **2026-06-08** | **#6c + #6d 修复: 导航返回栈 + BackHandler** | **P0 Bug** |
| → | `MainActivity.kt`: `previousRoute` String → `backStack` mutableStateListOf + `navigateTo`/`goBack` | #6c 导航返回 |
| → | `MainActivity.kt`: 新增 `BackHandler(enabled = currentRoute != "home")` 拦截系统返回手势 | #6d 系统返回 |
| **2026-06-08** | **#6b 深度诊断: 用户反馈 4 项具体症状** | **Bug 精确定位** |
| → ① | 选色后需杀应用才生效 → `remember` State 不观察 DataStore Flow | MainActivity 层 |
| → ② | 开关可能误显示关闭 → `useCustomColors && activePreset != null` 条件过于严苛 | SettingsViewModel 层 |
| → ③ | 课表卡片颜色不变 → `assignCourseHues(课程名)` 独立分配, 零感知 seedHue | ScheduleViewModel 层 |
| → ④ | 要求实时生效 → 需三层联动修复 | 架构级 |
| **2026-06-08** | **#6b 修复: 自由主题色彩三层联动 + UX 改进** | **P0 Bug** |
| → | `MainActivity.kt`: LaunchedEffect+.first() → collectAsState() 持续观察 4 个 DataStore Flow | 实时生效 |
| → | `SettingsViewModel.kt`: 移除三处 useCustomColors 条件绑定 (不再 && activePreset) | 开关不误关 |
| → | `ScheduleViewModel.kt`: colorPrefsFlow + combine + seedOffset → assignCourseHues | 课表联动 |
| → | `Color.kt`: 预设改名去"系" + assignCourseHues 加 seedOffset 参数 | UX |
| → | `ColorThemeScreen.kt`: 自制 HueSliderBar 彩虹渐变替换单色 Slider | UX |
| → | `WeekViewGrid.kt`: fallback isDark 从硬编码 false → isSystemInDarkTheme() | 暗色修正 |
| → | 共 6 文件 | |
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
| 2026-06-05 | Batch 1 完成：空教室闪退（8次提交）+ 登录数据丢失（1次提交） | Bug 修复 |
| 2026-06-05 | 编译警告清零（6 个 deprecation 警告） | 代码质量 |
| **2026-06-07** | **Batch 2.5: 成绩数据加载修复** | **P0 Bug** |
| → | `GradeRepository.getAllGrades()`: `onSuccess{}` → `fold()` + errors 列表, 全失败时返回 failure | 错误传播 |
| → | `GradeViewModel`: 新增 in-memory `cachedSemesters`, 失败时查缓存决定 error/warning | 数据持久化 |
| → | `GradeScreen`: 新增 `LaunchedEffect(Unit)` 自动刷新 + Snackbar 警告, 删除 `init{}` 避免双次触发 | UI 行为 |
| **2026-06-07** | **Batch 3: 考试安排模块** | **P1 新功能** |
| → | `ExamModels.kt`: Exam 数据类 + `kssj` 正则解析 + `daysRemaining` + 日期格式化工具 | 领域模型 |
| → | `ExamRepository`: registerMenuClick("N358105") + getExams + JSON items[] 解析 + HTML 守卫 | 数据层 |
| → | `ExamViewModel`: ExamUiState + loadExams() + fold 错误处理 | 状态管理 |
| → | `ExamScreen`: CenterAlignedTopAppBar + AnimatedContent 四态 + LazyColumn | 屏幕 UI |
| → | `ExamCard`: MD3 ElevatedCard + Row(左信息列/右倒计时Badge) + AssistChip + 无障碍 | 卡片组件 |
| **2026-06-07** | **Batch 3 UI 精修 (3 轮)** | **真机反馈** |
| → ① | 移除 ExamScreen 内重复 CenterAlignedTopAppBar（MainActivity 已有全局 TopAppBar） | 双层顶栏 |
| → ② | ExamCard 移除学院/教师行 + AssistChip；考试类型改为列表顶部统一 labelLarge 标记 | 信息精简 |
| → ③ | 日期时间行 bodyLarge(16sp) → bodyMedium(14sp)，与教室行保持 MD3 同层级 Token 一致 | 字号对齐 |
| **2026-06-07** | **Batch 4: 单首页 + 课表重设计 + 设置** | **P2 架构变更** |
| → Part A | HomeScreen(displaySmall问候语+留白+HomeCardPanel) + HomeViewModel(5态聚合) | 单首页 |
| → Part B | WeekViewGrid(5列动态宽+去横滚+等高行) + 紧凑节次栏 + CourseDetailSheet | 课表重设计 |
| → Part C | SettingsScreen(暗色模式/教学周) + AdvancedSettingsScreen(捐赠开关) | 设置页面 |
| → 17 files, +1140/-160 lines | |
| **2026-06-07** | **Batch 4 精修 R1: 导航架构** | **真机反馈** |
| → | 全局TopAppBar移除 → 各页面独立Scaffold; 子页←返回箭头; Drawer仅设设置 | |
| **2026-06-07** | **Batch 4 精修 R2: 首页+课表+色彩** | **真机反馈** |
| → ① | 首页: greeting传入完整字符串修复姓名; 4独立ElevatedCard | |
| → ② | 课表: 逐节网格+WeekSelector底置+细线+CJK断行 | |
| → ③ | 色彩: MD3 Tonal Palette(hue→5调色板)+6模板+HSL+WCAG | |
| → ④ | 排版: Title Medium(16sp)+Place图标+Body Medium(14sp)+Label Large(12sp) | |
| **2026-06-07** | **Batch 4 精修 R3: 卡片形态** | **真机反馈** |
| → | rowHeight=fillMaxWidth→fillMaxSize; padding(2dp)移除→12dp圆角矩形 | |
| **2026-06-07** | **Batch 4 精修 R4: 跨行合并** | **真机反馈** |
| → | 根因: Compose父Row测量约束maxHeight=55dp→子Card height=166dp被压缩 | |
| → | 方案: 双层架构 Layer1=空网格(线+标签) Layer2=课程卡片(offset绝对定位) | |
| **2026-06-07** | **Batch 4 精修 R5: 排版精简** | **真机反馈** |
| → | 删除Place图标; 字号缩小: 课程名11sp/教室10sp/教师9.5sp; 间距2dp→3dp | |
| **2026-06-07** | **Batch 4 精修 R6: 卡片收敛+抽屉删除** | **真机反馈** |
| → | 卡片宽高各减4dp+offset各偏2dp=四周2dp呼吸; 删除ModalNavigationDrawer | |
| → | 首页☰→⚙设置齿轮; DrawerContent.kt删除 | |
| **2026-06-07** | **Batch 5: 自定义登录 + 自动重登 + 欢迎页** | **P0 + P3** |
| → Part A | 自定义登录: Compose UI + 无框线胶囊设计 + 呼吸红错误动效 + 验证码展开动画 | 替换WebView |
| → Part B | 凭证管理: `CredentialManager` (EncryptedSharedPreferences) 加密存储学号+密码 | 自动填充 |
| → Part C | 自动重登: `MainActivity` 观察 `sessionExpired` → `autoLogin()` 无感恢复 | HTTP 302 |
| → Part D | 欢迎页: `WelcomeScreen` 仅首次展示, 隐私声明+GitHub+胶囊按钮 | 首次引导 |
| → Part E | 验证码修复: loginClient 共享 CookieJar 加载 `/kaptcha`; logoutAccount 仅首次调用 | 验证码 |
| → Part F | UI: 登录页移除"用浏览器登录"→设置页"账号"区块; +webview_login 路由 | 导航 |
| → 5 new, 8 modified files | |
| → HAR 逆向: 登录 API 端点 + RSA 加密流程 + 验证码机制 (/kaptcha) 从 login.js 逆向 | |
| **2026-06-08** | **#6e 修复: 深色模式页面切换闪屏** | **P0 Bug** |
| → | `MainActivity.kt`: AnimatedContent 外包 Box(fillMaxSize.background=theme) 防白底穿透 | 2 files, +5 lines |
| → | `Theme.kt`: SideEffect → DisposableEffect(colorScheme.primary, darkTheme) 优化状态栏同步 | |
| **2026-06-08** | **#7a 修复(修订): 空教室分页 — 正确参数 + 翻页 UI** | **P1** |
| → 诊断 | 上次 `rows=200` 无效 — HAR 确认参数名应为 `queryModel.showCount` + `queryModel.currentPage` | dot notation |
| → | `EASystemApi.kt`: 删除 rows，新增 queryModel.showCount=20 + queryModel.currentPage=1 | 数据层 |
| → | `RoomModels.kt`: RoomQueryResult 新增 totalPage 字段 | 数据层 |
| → | `RoomRepository.kt`: queryEmptyRooms(filter, page) 新增 page 参数 + 解析 totalPage | 数据层 |
| → | `RoomViewModel.kt`: UiState 新增 currentPage/totalPage，query()→第1页，goToPage() | 业务层 |
| → | `RoomScreen.kt`: Column(verticalScroll) → LazyColumn 防嵌套滚动崩溃 | UI层 |
| → | `RoomList.kt`: 新增 PaginationBar (←上一页/第X/Y页/下一页→)，仅多页时显示 | UI层 |
| → | 共 6 files, +160/-80 lines | |
| **2026-06-08** | **Batch 6 完成 + #7a 完成 — 提交 a6be1c6** | **里程碑** |
| **2026-06-08** | **#7b 修复: 首页上课状态 + 节次表13节** | **P1** |
| → | ScheduleRepository.fetchPeriods() 调 API 获取真实13节时间表 | 数据层 |
| → | HomeViewModel.computeNextClass() 重写: IN_CLASS(倒计时) → HAS_CLASS → ALL_DONE | 业务层 |
| → | WeekViewGrid 固定55dp行高 + verticalScroll, 表头固定 | UI层 |
| → | 删除 estimateCurrentPeriod() 硬编码 + buildPerPeriodLabels() 硬编码11节 | 清理 |
| **2026-06-08** | **Batch 8: 联系反馈 + GitHub 更新检测** | **P2 新功能** |
| → | GitHubApi + GitHubRelease + UpdateRepository: semver比对 + 通知 + dismiss | 更新检测 |
| → | SettingsScreen 检查更新多态按钮 + HomeViewModel 启动自动检查 | UI |
| → | 联系与反馈 section + app_update 通知渠道 | 反馈通道 |
| **2026-06-08** | **#7b 修订: 主页逻辑修复 + 课表滚动 + 设置页滚动** | **Bug** |
| → | SettingsScreen Column 加 verticalScroll 防止内容溢出 | |
| → | 联系反馈图标 14dp→20dp, Email/Group 语义化图标 | |
| **2026-06-08** | **设置页胶囊卡片重构 + 关于页面 + 系统与更新** | **UI 重设计** |
| → | SettingsScreen: 全部项目用 ElevatedCard(16dp, elevation=0) 包裹 | 胶囊卡片 |
| → | AboutScreen: App图标 + 版本号 + 三张卡片(社区/许可证/系统与更新) | 关于页 |
| → | SystemUpdateScreen: 自动检查开关 + 检查更新按钮 + 更新日志 | 二级页面 |
| → | 流动渐变背景动画 → 放弃, 改用纯色背景 | |
| → | 第三方开源证书: AlertDialog 内联展示 (OSS Licenses 插件不可用) | |
| → | APP 图标: 使用自定义 app_icon.png | |

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
