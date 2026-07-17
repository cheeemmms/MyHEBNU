# MyHEBNU — 进度追踪

> 最后更新: 2026-07-17 | 状态: Phase 7 完成 ✅；Phase 8 打磨中 — Batch 10（8 项 Bug/新需求）已记录待修复。

---

## 当前状态

```
Phase 0         Phase 1        Phase 2        Phase 3        Phase 4        Phase 5        Phase 6        Phase 7        Phase 8
 侦察            骨架           认证           课表           成绩           空教室         考试           Widget+通知     打磨
[✅ 已完成]     [✅ 已完成]    [✅ 已完成]    [✅ 已完成]    [✅ 已完成]    [✅ 已验收]    [✅ 已完成]    [✅ 已验收]    [⏳ 待开始]

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
| 7.1 | 课表 Widget (Glance) | ✅ 已完成 | 4 种尺寸全通过 — 根因是 MIUI RemoteViews 膨胀器把 Int 当资源 ID（非 API 问题），全量改 `R.dimen.*`/`R.color.*` 解决，详见 [[glance-api-research]] |
| 7.2 | 上课提醒通知 | ✅ 已完成 | ClassReminderWorker (15min周期) + ReminderManager — 读Room课程+内存节次表→15分钟窗口检测→通知+DataStore去重 |
| 7.3 | 考试提醒通知 | ✅ 已完成 | ExamReminderWorker (60min周期) + ExamEntity Room缓存 — 读Room考试缓存→前一天/前1小时检测→通知+去重 |

### 7.1 交付物

- **最终修改**：4 Widget 文件 + ScheduleWidgetCommon.kt + ScheduleWidgetData.kt + colors.xml(新建) + dimens.xml(新建) + 3 预览 drawable(新建) + 4 widget XML 配置
- **核心修复**：所有 Glance 维度值 (size/width/height/padding/cornerRadius) 改用 `@DimenRes` 资源引用；所有颜色改用 `ResourceColorProvider(R.color.xxx)`；加 `previewImage` 防止 MIUI 桌面超时
- **真机验证**：小米15/Android 16/MIUI 桌面 — Widget 显示课表数据、点击跳转 App 均正常
- **错诊纠正**：此前 `provideContent` 不存在等判断均为误判，代码在 Glance 1.1.1 上完全可编译

### 7.2 + 7.3 交付物

- **新文件 (6)**：ExamEntity.kt + ExamDao.kt + ReminderEntryPoint.kt + ReminderManager.kt + ClassReminderWorker.kt + ExamReminderWorker.kt
- **修改文件 (9)**：ExamModels.kt (id字段) + AppDatabase.kt (v3+ExamEntity) + Migrations.kt (2→3) + DatabaseModule.kt (ExamDao) + ExamRepository.kt (write-through+转换) + RepositoryModule.kt + UserPreferences.kt (sentReminders去重) + MyHebnuApplication.kt (2个schedule方法) + strings.xml (zh+en)
- **核心架构**：考试数据 Room 持久化（主键=`$year-$term-$sjbh`，索引 semesterYear+semesterTerm）；通知模块纯只读（Worker→Room→判断→通知，零API、零同步）；DataStore 去重+自动过期清理；Worker 所有异常内部消化→始终返回 Result.success() 保持周期存活
- **编译状态**：BUILD SUCCESSFUL，零错误

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
| **15** | ~~Widget 深色模式未实现~~ | 🟢 已修复 | Widget-FE | 新建 `res/values-night/colors.xml` (MD3 Dark Scheme) + `ResourceColorProvider` 自动切换 |
| **15.1** | ~~Widget Micro 内容视觉偏上~~ | 🟢 已修复 | Widget-FE-S1 | MicroHasCourses Column 内加双 `defaultWeight()` Spacer 垂直居中 |
| **15.2** | ~~Widget Medium 时间列换行 (09:45→09:\n45)~~ | 🟢 已修复 | Widget-FE-S1 | 时间列 `width(32→42)` + 标题→课程间距 `height(6→10)` |
| **16** | ~~Widget 今日课程结束后不显示明日课程~~ | 🟢 已修复 | Widget-FE | 跨日逻辑: 全部结束>19:00→明日; 周五>19:00→Weekend; 周日>19:00→周一(跨周); 新增 AllDoneToday 状态 |
| **17** | Widget 色彩不跟随 App 主题 | ⚪ 暂不处理 | Widget-FE | 管理员决定不扩颜色桶, 保持现有 6 色 |
| **18** | Widget 课程色桶仅 6 个, App 支持 HSL 连续色相 | ⚪ 暂不处理 | Widget-FE | 与 #17 同, 保持现状 |
| **19** | Widget 1 小时系统刷新周期 | ⚪ 已接受 | Widget-FE | 管理员决定放弃分钟更新, 维持 XML 1 小时间隔 |
| **20** | ~~Widget 预览图是 XML Shape 纯色块~~ | 🟢 已修复 | Widget-FE | 真机截图经 ffmpeg 缩放为 220×220/440×220/440×440 PNG |
| **21** | App 内调起 Widget 不可用 | ⚪ 已放弃 | Widget-FE | `requestPinAppWidget` 需小米审核，放弃此功能 |
| **22** | 启动应用可能闪现欢迎页面 | 🟢 已修复·编译通过·已合入main | 启动/主题 | **根因确认**：冷启动 `windowBackground` 白闪——`themes.xml` 用 `Theme.Material.Light.NoActionBar` 且未设 `windowBackground`，缺 `values-night/themes.xml`；`MyHEBNUApp` 的 null/Loading 分支无背景透白。`isFirstLaunch` 逻辑正常（非根因）。修法=新增 night 主题深色 `windowBackground` + 浅色显式背景 + `MainActivity` 外层 Compose 背景兜底。**2026-07-17：lint 修复（launch_bg 合并为单色名），AS 编译通过；视觉验收（深色冷启动白闪）暂缓，待真机复核** |
| **23** | 设置中选"浏览器登录"退回桌面 | 🟢 已修复 | 认证/移除功能 | 管理员决策：**直接删除浏览器登录（WebView fallback）功能**（初衷为兜底首页登录，实测首页登录无问题）。已移除 `webview_login` 路由、`WebViewFallbackScreen`、`LoginViewModel` 全部 WebView 回调、`AuthRepository.onWebViewLoginSuccess/isLoginSuccessUrl/getLoginUrl/transferCookies/parseCookieString`、`SettingsScreen` 浏览器登录入口；`LOGIN_URL`/`CookieManager` 保留供自定义登录与 `logout` 使用。编译验证通过 |
| **24** | 欢迎页 GitHub 链接错误 | 🟢 已修复 | 欢迎页 | 管理员核实 `cheeemmms/MyHEBNU` 链接正确（与 About 页一致），无需改动 |
| **25** | 首页预览需等待 1-2 秒才出现 | 🔴 已定位 | 首页/性能 | **机制确认：每次开 App `HomeViewModel.loadHomeData()` 均重拉服务器**——`fetchPeriods`(API) + `getExams`(API) + `getAllGrades`(连查 4 学期 API)；`isLoading` 门控显示，须等全部网络往返完成。本地缓存未被首页利用：课表读 Room✅、考试有 Room 缓存但首页用网络 `getExams`❌、成绩**无任何 Room 缓存**❌（GradeRepository 无 DAO）。详见下方「#25 加载机制与消除等待方案」 |
| **26** | 欢迎页/登录页未适配深色模式 | 🟢 已修复 | 启动/主题 | 管理员真机核实两页已正确适配深色模式（均用 `MaterialTheme.colorScheme`），原现象与 #22 同源于冷启动白闪 |
| **27** | 成绩页只显示约 10 门课程 | 🟡 已实施待验收 | 成绩 | `EASystemApi.getGradeList` 缺 `queryModel.showCount`（空教室 #7a 同因）；已补 `showCount="1000"` 全取 + `GradeRepository` 显式传入；UI 不改。**待用户在 Android Studio 编译 + 真机验收** |
| **28** | 小组件课程时间有误 | 🔴 已定位 | 小组件 | 小组件 `loadPeriodTimes` 用硬编码节次表；App 主程序 `fetchPeriods()` 拉真实节次仅存内存，Glance 独立进程取不到 → 永远用错表 |
| **29** | （新功能）查看已公布的下学期课表 | 🟢 已定案 | 课表/新功能 | 管理员否决"选择器"与"自动切全局"；**已定案临时预览(Peek)方案**（见「#29 修订」）。下学期推算：`[3,12,16]` 顺序取下一项（见 #30 学期枚举）|
| **30** | 课表数据驱动化 / 小学期适配 | 🟢 已立项 | 课表/结构性 | HAR 查明：学期下拉=HTML `<select>`、**无独立 JSON 列表接口**；且 **term 码 `16`=第三学期/小学期已内置**。据此改为：学期枚举 `[3,12,16]`(含小学期) + 天数数据驱动(max xqj) + 节次持久化共用 + 周数动态。详见「硬编码清单 & 小学期适配评估(修订)」|

| **31** | 设置→高级功能：显示周末列开关 | 🟢 已验收通过 | 设置/UI/课表 | **实施完成（2026-07-17）**：默认隐藏周末（5 列，保持现状）；高级功能加「显示周末列」开关，勾选后网格展开 7 列（周一~周日），周末课（dayOfWeek 6/7）自动跟随教务显示。改动 4 文件：UserPreferences（showWeekendColumns 键/Flow/setter）、ScheduleViewModel（dayLabels 默认 7 + combine 据偏好折叠 5/7）、SettingsViewModel（暴露状态+setter）、AdvancedSettingsScreen（高级菜单加 Switch）。周末列整体一起隐藏/显示以保高亮映射正确。Widget 周末不在范围。验证：AS 编译 + 课表页开关 OFF 5 列 / ON 7 列 |
| **32** | 课表页面布局优化（收窄节次列） | 🟢 已验收通过·已合入main | 课表/UI | 用户提：7 列会拥挤，需**收窄节次(时间)列**获得更大课程区；顺带优化现有 5 列布局（节次列过宽）。与 #30 7 列方案协同，缓解拥挤。**2026-07-17(7) 实施**：`timeColumnWidth` 由固定 40dp 改为动态——`columns>=7 → 28dp`、否则 `36dp`；节次时间字号 `9sp→8.5sp` 防换行。仅改 `WeekViewGrid.kt` 1 文件，课程卡片区随之变宽。**2026-07-17(8) 验收通过 + push(9b83f44)；5列36dp/7列28dp均正常** |
| **33** | 课表卡片内容自适应（取消课名2行硬限） | 🟢 已实施·agent自编译通过·待用户视觉验收 | 课表/UI/卡片 | 用户反馈：课名 `maxLines=2` 硬限致周末跨节课"拉成长条"、空间浪费。**实施（2026-07-17(9)）**：`CourseCard` 改用 `BoxWithConstraints` 读卡片真实内高 `maxHeight`；减教室(`innerH>=70dp`→2行否则1行)/老师固定占用+gap，得课名可用高→`nameMaxLines=max(1,(available/14dp).toInt())` 动态行数，长卡片自然填满、矮卡片仍紧凑；仅当总和快超才限行，单行仍溢出则 `overflow=Ellipsis`；极矮(`innerH<40dp`)课名10sp略缩。取消原硬限 `maxLines=2`。仅改 `CourseCard.kt`。**修订（2026-07-17(10)）**：初版用 `BoxWithConstraints` 致 `TextStyle` 在该作用域解析异常（报需 `spanStyle`/`paragraphStyle`）；改由父层 `WeekViewGrid` 传 `cardHeight: Dp`（= `rowHeight*spanCount-4.dp`）入参；并弃用 `TextStyle` 构造器、改用 `Text` 原生命名参数（`fontSize`/`fontWeight`/`lineHeight`/`color`，`lineHeight` 用 `sp`）。根因=误用 `LineBreak` 参数 + `lineHeight` 误传 `Dp`（应为 `TextUnit`），致 `TextStyle` 重载解析失败回退错误候选，产生一连串"无 fontWeight/fontSize/lineHeight"误导报错。agent 自行 `compileDebugKotlin` 通过（EXIT=0）。**待用户 AS 课表页视觉验收（重点：周末跨节长卡片课名填满不再空白拉条、单节矮卡片不溢出）** |

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
| **2026-07-17** | **Batch 10 进度：#24/#26 标记已修复；#25 机制确认（每次开 App 实时拉取，isLoading 门控）；#29 给出触发/交互设计建议（解耦发现与全局切换）** | **用户核实 + 代码勘察** |
| **2026-07-17 (2)** | **#29 定案临时预览(Peek)；#30 立项。HAR 二次勘察：学期下拉=HTML `<select>` 无 JSON 接口；term `16`=小学期已内置 → #30 落地降级为"学期枚举[3,12,16]+数据驱动"，比原评估更轻。管理员新提"周末课表跟随教务"(=H1 既定方案)** | **用户决策 + HAR 解析** |
| **2026-07-17 (4)** | **#23 修复：删除浏览器登录（WebView fallback）功能** — 移除 `webview_login` 路由、`WebViewFallbackScreen`、`LoginViewModel` 全部 WebView 回调、`AuthRepository` 的 `onWebViewLoginSuccess/isLoginSuccessUrl/getLoginUrl/transferCookies/parseCookieString`、`SettingsScreen` 浏览器登录入口；保留 `LOGIN_URL`/`CookieManager`（自定义登录与 `logout` 仍用）。消除退回桌面 bug，并消解 Batch 10 中唯一硬阻塞项 | **用户决策 + 代码删除** |
| **2026-07-17 (3)** | **新增 UI 决策：① 设置→高级功能 加"隐藏周末列"开关(#31)；② 课表布局优化/收窄节次列缓解7列拥挤(#32)。用户要求给出整体把握与待讨论清单** | **用户决策** |
| **2026-07-17 (5)** | **#27 推送(main `7775b35`)：成绩列表补 `queryModel.showCount='1000'` 全取；#22 实施(待验收)：冷启动 windowBackground 白闪根治——新增 `values-night/themes.xml` 深色窗口背景、`values/themes.xml` 显式浅色背景、颜色资源、`MainActivity` 外层 Compose 背景兜底** | **用户验收 + 代码实施** |
| **2026-07-17 (6)** | **#22 + #31 推送 main：#22 冷启动白闪根治（`launch_bg`+night 主题，`MainActivity` 背景兜底），编译通过已合入；#31「显示周末列」开关（高级功能，默认隐藏周末/5列，开启展开7列）管理员 AS 编译 + 真机验收通过** | **用户验收 + push** |
| **2026-07-17 (7)** | **#32 实施（待验收）：`WeekViewGrid` 节次列 `timeColumnWidth` 固定40dp→动态（≥7列28dp / 否则36dp），时间字号9sp→8.5sp；课程区随之变宽，与 #31 周末列协同** | **代码实施** |
| **2026-07-17 (8)** | **#33 立项（待 #32 验收后实施）：课表卡片内容自适应——取消课名2行硬限，测量驱动动态压缩；回应"周末课拉成长条"** | **用户决策** |
| **2026-07-17 (9)** | **#32 验收通过 + push(`9b83f44`)；#33 实施（待验收）：`CourseCard` 改 `BoxWithConstraints` 测真实内高，`nameMaxLines` 由可用高动态计算，长卡片填满/矮卡片紧凑，取消硬限 `maxLines=2`** | **用户验收 + push + 代码实施** |
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
| **2026-06-10** | **HS-3: Widget 预览图替换** | **资源替换** |
| → | 3 张真机截图 → ffmpeg 缩放至 220×220/440×220/440×440 PNG | |
| → | 删除 3 个 XML Shape 占位 drawable | |
| → | 编译零错误. 验收通过. | |
| **2026-06-10** | **HS-9: Widget 深色模式** | **1 新文件** |
| → | 新建 `res/values-night/colors.xml` — 15 色 MD3 Dark Scheme | |
| → | 零代码改动: `ResourceColorProvider` 自动按系统深/浅模式切换资源 | |
| → | 编译零错误. | |
| **2026-06-10** | **#16 + Micro 课程轮换: 跨日逻辑 + AllDoneToday** | **核心逻辑** |
| → 根因 | `loadDayCourses()` 中 `nextCourseIndex = if (nextIdx >= 0) nextIdx else 0` 全天结束时误判为有课 | |
| → 修复 | `nextCourseIndex = nextIdx` 直接透传; `loadDaySchedule()` 重构两阶段决策 | |
| → 新增 | `AllDoneToday` 状态 + `HasCourses.isTomorrow`/`tomorrowDayOfWeek` 字段 | |
| → 逻辑 | 工作日<19:00 全部结束→AllDoneToday; >19:00→明日; 周五>19:00→Weekend; 周日>19:00→周一(跨周) | |
| → UI | Micro "明天" 标签 + Medium/Large "明天 周一" 日期前缀 | |
| → | 共 4 files. 编译零错误. 真机验证通过. | |
| → | `LocalTime` 是 locale-independent, 不受系统 12H/24H 显示格式影响 — 已确认 | |
| **2026-06-10** | **Widget 前端精修 Session 3: HyperOS 对标检查** | **配置补全** |
| → HS-4 | 新建 `res/layout/widget_loading.xml` + 3 Widget XML 加 `initialLayout` | |
| → HS-5 | `AndroidManifest.xml` 加 `miuiWidgetVersion=1` meta-data | |
| → HS-6 | `strings.xml` 3 Widget 标签改为 `MyHEBNU·课表` (HyperOS 命名规范) | |
| → HS-7 | App 内调起 Widget `requestPinAppWidget` — 放弃 (需小米审核) | |
| → HS-1+2 | 设计审计: 圆角28dp/字号层级/对比度/触摸区 全合规 | |
| → HS-8 | 无障碍评估: Glance 1.1.1 无 contentDescription API，已穷尽 | |
| → | 共 1 new + 4 modified files. 编译零错误. | |
| **2026-06-10** | **Widget 前端精修 Session 2: Medium 信息层级 + 间距统一** | **样式修复** |
| → S2-1 | `ScheduleMediumWidget.kt`: 课程行间距 `height(6→8)` 增强卡片呼吸感 | |
| → S2-2 | `ScheduleMediumWidget.kt`: 水平间距审计 — 非对称 6dp/8dp 保持 (色条靠近时间是有意设计) | |
| → S2-3 | `ScheduleMediumWidget.kt`: "其他N节"间距 `height(4→8)` 与课程间距统一节奏 | |
| → | 共 1 file, 2 lines. 编译零错误通过. | |
| **2026-06-10** | **Widget 前端精修 Session 1: Micro 居中 + Medium 时间列 + 间距** | **样式修复** |
| → S1-1 | `ScheduleMicroWidget.kt`: MicroHasCourses Column 内加双 `defaultWeight()` Spacer, 垂直居中 | |
| → S1-2 | `ScheduleMediumWidget.kt`: MediumCourseRow 时间列 `width(32→42)`, 修复 `09:45` 冒号断行 | |
| → S1-3 | `ScheduleMediumWidget.kt`: MediumHasCourses 标题→课程间距 `height(6→10)` | |
| → S1-4 | `ScheduleLargeListWidget.kt`: 目视确认无需改动 (36dp 时间列 + 12sp 字号比例合适) | |
| → | 共 2 files, 4 lines. 编译零错误通过. | |
| **2026-06-09** | **| **2026-06-10** | **Phase 7.2 + 7.3: 上课提醒 + 考试提醒通知** | **里程碑** |
| → | 考试 Room 持久化: ExamEntity + ExamDao + MIGRATION_2_3 + ExamRepository write-through | 数据层 |
| → | 上课提醒: ClassReminderWorker (15min) → Room课程+内存节次表 → 15min窗口 → 通知+DataStore去重 | 功能 |
| → | 考试提醒: ExamReminderWorker (60min) → Room考试缓存 → 前一天/前1h → 通知+去重 | 功能 |
| → | 通知去重: UserPreferences sentReminders + 过期清理 + 上限500 | 基础设施 |
| → | Worker: 所有异常内部消化 → Result.success() 保持 PeriodicWork 存活 | 可靠性 |
| → | 共 6 new + 9 modified files. 编译 BUILD SUCCESSFUL. | |
| **2026-06-09** | **Phase 7.1 Widget 修复 — MIUI 兼容性** | **里程碑** |
| → 诊断 | 此前 `provideContent` 等误判全部推翻：javap 反编译确认 Glance 1.1.1 API 完整可用，代码可编译 | |
| → 根因 | MIUI RemoteViews 膨胀器把所有 Int 参数当 `@DimenRes`/`@ColorRes` 资源 ID 查表（非 Glance API 问题） | |
| → 修复1 | padding: `padding(all = 12)` → `padding(all = R.dimen.widget_dp_12)` | |
| → 修复2 | 颜色: `ColorProvider(widgetXxx(isDark))` → `ResourceColorProvider(R.color.widget_xxx)` 直接返回 | |
| → 修复3 | cornerRadius: `cornerRadius(28)` → `cornerRadius(R.dimen.widget_dp_28)` | |
| → 修复4 | 全量: 所有 `size/width/height(Int)` → `(R.dimen.widget_dp_N)`，values/colors.xml + dimens.xml 覆盖15色+15维度 | |
| → 修复5 | `previewImage` + XML Shape drawable 占位 — 防止 MIUI 桌面 Glance WorkManager 延迟触发超时 | |
| → | 共 14 files: res/values/colors.xml (新), res/values/dimens.xml (新), 3 drawable (新), 4 widget XML, ScheduleWidgetCommon.kt, ScheduleWidgetData.kt, 4 widget KT files | |
| → | 真机验证: 小米15/Android 16 — 4 种 Widget 显示+跳转正常 | |

---

## Phase 8 待修复批次（Batch 10 — 记录于 2026-07-17）

> 来源：管理员真机反馈（#22~#28）+ 新功能规划（#29）。以下 8 项均已完成代码勘察，按"修改复杂度 × 项目影响"排序。

### 代码勘察结论（根因定位）

| # | 问题 | 根因置信度 | 根因定位 | 关键文件 |
|---|------|-----------|---------|---------|
| 27 | 成绩只显示约 10 门 | 🟡 已实施 | `getGradeList` 缺 `queryModel.showCount`（与空教室 #7a 同因）；已补 `showCount="1000"` 全取 + `GradeRepository` 显式传入；UI 不改。**待用户 AS 编译 + 真机验收** | `EASystemApi.kt:62` |
| 28 | 小组件课程时间有误 | 🔴 高 | 小组件 `loadPeriodTimes` 硬编码节次表；App `fetchPeriods()` 真实节次仅存内存，Glance 独立进程取不到 | `ScheduleWidgetData.kt:236` |
| 24 | 欢迎页 GitHub 链接错误 | 🟢 已修复 | 管理员核实链接正确（与 About 一致），无需改动 | `WelcomeScreen.kt:20` |
| 25 | 首页预览需等 1-2s 才出现 | 🔴 已定位 | 每次开 App `loadHomeData` 重拉服务器（fetchPeriods+getExams+getAllGrades[×4 学期]）；`isLoading` 门控；本地缓存未被首页利用（考试 Room 缓存未用、成绩无 Room 缓存） | `HomeViewModel.kt:76` |
| 22 | 启动闪现欢迎页面 | 🟢 高 | **根因=冷启动 windowBackground 白闪**（非 `isFirstLaunch`）：`themes.xml` 用 Light 主题+无 windowBackground，缺 `values-night/themes.xml`；Loading/null 分支无背景透白。修法=night 主题深色 windowBackground + 浅色显式背景 + `MainActivity` 外层 Compose 背景兜底 | `themes.xml` / `MainActivity.kt:75` |
| 26 | 欢迎/登录未适配深色模式 | 🟢 已修复 | 管理员真机核实已正确适配深色模式 | `Theme.kt` / `WelcomeScreen.kt` |
| 23 | 浏览器登录退回桌面 | 🟡 低 | 疑似 CAS 重定向外跳系统浏览器 / WebView·cookie 迁移崩溃；`webview_login` 复用同一 LoginViewModel + 双 `isLoggedIn` LaunchedEffect 竞态 | `MainActivity.kt:295` / `AuthRepository.kt` |
| 29 | 下学期课表（新功能） | 🟡 待决策 | 触发/提示规格已定；自动切学期逻辑待定（建议解耦"发现"与"全局切换"） | `ScheduleRepository.kt` |

### 修复优先级排序（复杂度 ★ / 影响 ★★★）

| 序 | 问题 | 复杂度 | 影响 | 确定性 | 说明 |
|----|------|--------|------|--------|------|
| 1 | **#27 成绩 10 门** | ★ | ★★★ | ★★★ | 先修：2 行接口改动，价值高且确定，复刻已修的 #7a |
| 2 | **#24 GitHub 链接** | ★ | ★ | ★★ | 一行修正，需管理员提供正确 URL |
| 3 | **#28 小组件时间** | ★★ | ★★ | ★★★ | 持久化真实节次（DataStore/Room）供小组件读取，每日可见 |
| 4 | **#25 首页预览增强** | ★★ | ★★ | ★★ | UI 增强，首页模块内，不破坏现有链路 |
| 5 | **#22 + #26 启动闪现/深色** | ★★ | ★★ | ★★ | 高度疑似同源（窗口背景），合并修复 |
| 6 | **#23 浏览器登录退回桌面** | ★★★ | ★★ | ★ | 调查成本最高，需真机 logcat 定位根因 |
| 7 | **#29 下学期课表（新）** | ★★★ | ★★ | ★★ | 新功能，单独规划，数据层已就绪 |

### 依赖与备注
- **#22 与 #26 同源**：冷启动 `windowBackground` 未随主题着色，深色模式下出现白闪，易被误认为"欢迎/登录页未适配深色"。建议合并修复（设置主题背景色 / 加主题色 Splash）。
- **#23 与认证域耦合**：改动需谨慎，避免破坏现有自定义登录与自动重登链路（原 Batch 5）。
- **#27 通用隐患**："分页默认条数"是该类教务接口的通病（已踩空教室 #7a）。建议顺手审计所有列表接口是否都显式传了 `queryModel.showCount`，防止复发。
- **#28 数据一致性**：若修复，应让小组件与 App 共用同一份"真实节次"数据源（持久化），而非各自维护硬编码表。

---

## #25 加载机制与消除等待方案（2026-07-17 勘察确认）

### 当前机制（确认为"每次开 App 实时拉取服务器"）

`HomeViewModel.loadHomeData()` 在 `init{}` 调用，**每次 App 启动执行一次**，内部为串行 suspend 序列：

| 步骤 | 调用 | 数据源 | 是否网络 |
|------|------|--------|----------|
| 0 | 读 `studentName`/`currentSemester*`/`currentWeek` | DataStore | 本地（快） |
| 1 | `fetchPeriods(year, term)` | 3 步 API 序列 | ⏳ 网络 |
| 2 | `computeNextClass` → `observeSchedule().first()` | **Room** | 本地（快）但依赖①的 periods |
| 3 | `computeNextExam` → `getExams(year, term)` | 3 步 API 序列 | ⏳ 网络（Room 已有缓存 `getCachedExams`/`observeExams`，**首页未用**） |
| 4 | `computeGradeInfo` → `getAllGrades()` | **连查 4 学期，每学期 3 步 API** | ⏳ 网络（**GradeRepository 无 DAO，零本地缓存**） |

`isLoading` 在步骤0置 `true`、步骤4完成才置 `false` → 首页卡片**必须等全部网络往返完成才显示** → 1–2 秒（网络差时更久）等待。

**结论**：首页预览 = 服务器实时拉取，非本地读取。本地缓存未被首页利用（考试 Room 缓存存在却走网络；成绩无任何持久化）。

### 消除等待的方案（建议，待实现）

核心原则：**先显本地缓存 → 再后台刷新**；取消 `isLoading` 对"网络完成"的硬门控。

1. **Phase A（即时展示）**：从 Room 直接读课表 + 考试缓存（`getCachedExams`/`observeExams`）+ **持久化的节次表** → 立即 `isLoading=false` 出卡片；成绩若无本地缓存则先显占位/"—"或上次 GPA（DataStore）。
2. **持久化节次表**（DataStore 或 Room）→ 重启后无需 API 即可精确判断"正在上课/距下课"。
3. **Phase B（后台并行刷新）**：`launch` 并行刷新 periods/exams/grades，成功后带 diff 更新字段（避免闪烁）。
4. **给成绩加 Room 缓存**（或至少持久化"最新 GPA"到 DataStore）→ 这是最大改动，也是主瓶颈（目前 4 学期串行网络）。

> 该方案归属 Phase 8.5（性能优化）。修复前建议先确认管理员是否接受"首页先显示旧数据、后台静默刷新"的体验（与课表页现有 cache-first 策略一致）。

---

## #29 下学期课表：触发 / 交互设计（2026-07-17）

### 管理员规格

- **触发**：课表到达 20 周后（`currentWeek ≥ 20`）。
- **交互**：底部弹出**可关闭**提示框："本学期课程已结束，是否尝试查询下学期课表？"
- **原设想**：查询到下学期课表则**自动进入**下一学期。

### 我的建议（待管理员拍板）

1. **触发 refined**：硬编码"20 周"未必命中（HEBNU 学期常 18 周）。建议用**实际学期末周**（N2154 周映射的最大周数）或 `max(18, 实际末周)` 触发，更稳健。
2. **解耦"发现"与"全局切换"（核心：反对自动切全局学期）**：
   - 自动把全局 `currentSemester` 切到下学期，会连动课表/成绩/考试/Widget 全部翻转，用户可能仍想看本学期已修课程 → **反模式、易迷失**。
   - 正确做法：提示框"查询" = **fetch 下学期并写入 Room**（多学期已支持），**但不改全局学期**；随后在**学期切换器**中作为可选项出现，由用户主动进入。
   - 即：先补一个**学期选择器 UI**（#29 原计划即有），提示框只负责"试探性拉取 + 提醒"，是否进入由用户决定。
3. **学期参数推算（已确认编码）**：
   - `term "3"` = 第一学期（秋，如 `2025-2026-1`）；`term "12"` = 第二学期（春，如 `2025-2026-2`）。
   - 下一学期：`(2025,"3") → (2025,"12")`；`(2025,"12") → (2026,"3")`（即 `3→12` 同年，`12→3` 年+1）。
4. **未公布处理**：`refreshSchedule` 若返回空 / HTML 错误 → 提示"暂未公布下学期课表"，并**冷却**（本学期内不再重复打扰）。
5. **关闭记忆**：提示框可关闭，按**学期**持久化"已忽略"标记（明年同学期或新学年可再问；或提供"永久不再问"选项）。
6. 首页"正在上课"卡片在学期结束后本就显示"已全部结束/放假"，提示框是额外发现入口，二者不冲突。

---

## 硬编码清单 & 小学期适配评估（2026-07-17 勘察）

> 管理员提问：学校将推行小学期，课表哪些是硬编码？能否随教务返回内容自动变更而无需发版更新？

### 结论一句话
**节次时间在主课表页是数据驱动（会自动跟随）；但"星期/天数、学期识别（秋/春两学期硬编码，无小学期槽位）、成绩学期列表、小组件节次表"均为硬编码，小学期到来时不会自动适配，需改造。**

### 数据驱动 ✅（教务改了会自动跟随）
| 项 | 来源 |
|----|------|
| 节次时间表（主课表页） | `ScheduleRepository.fetchPeriods()` → API `getPeriodList`，`periodLabels` 由其生成 |
| 课程名/教室/教师/周次范围/单双周 | `parseScheduleResponse` 全部从 API JSON 解析 |
| 周次↔日期映射、当前周 | N2154 API `fetchWeekDateMapping` |
| 课表网格行列数 | `WeekViewGrid` 用 `periodLabels.size` / `dayLabels.size`（**行数据驱动，列受下方 dayLabels 限制**） |

### 硬编码 ❌（小学期到来不会自动适配）
| # | 位置 | 硬编码内容 | 小学期影响 |
|---|------|-----------|-----------|
| H1 | `ScheduleViewModel.dayLabels` (:37) | 固定 `一~五`（5天） | 周末/加排日的课解析得到但网格只画5列 → **漏显** |
| H2 | `guessCurrentSemester()` (:292) | 仅 term `"3"`(秋)/`"12"`(春) + 月份映射 | **无小学期(第三学期)槽位** → 无法自动探测/切换（最根本阻碍） |
| H3 | `GradeRepository.getAllGrades()` (:85) | 硬编码 4 个学期(term 3/12) | 小学期成绩**不会被查询** |
| H4 | `ScheduleWidgetData.loadPeriodTimes()` (:236) | 完全硬编码13节，忽略API | 小组件节次永远错（即 #28） |
| H5 | `goToNextWeek()<20` + `WeekSelector<20` | 周数上限20 | 若小学期编号超20或独立编号 → 翻不过去 |
| H6 | `fetchPeriods → getPeriodList(campusId="4")` | 校区码"4" | 跨校区节次可能不符 |
| H7 | `fallbackPeriods()` / `parseWeekRange` 默认1-18 | 仅解析/网络失败时兜底 | 影响小（兜底路径） |
| H8 | `ExamViewModel/RoomViewModel/SettingsViewModel` 默认 term`"12"` | 初值 | 影响小（会被探测覆盖） |

### HAR 实证：学期下拉的真实来源（2026-07-17 二次勘察）

用脚本解析 9 个 HAR 后确认：
- **学期下拉是服务器端渲染的 HTML `<select>`，不是独立 JSON 接口**。出现在课表页 `xskbcx_cxXskbcxIndex.html` (N2151)、成绩页 `cjcx_cxDgXsxmcj.html` (N305007)、周次页 `xskbcxZccx_cxXskbcxIndex.html` (N2154)。
- **xnm（学年）选项**：`2031`…`2002`（显示如 `2031-2032`，跨度很大）。
- **xqm（学期）选项**：`('3','1')`、`('12','2')`、**`('16','3')`**。
  - 👉 **`16` = 第三学期 = 小学期，教务系统已经内置该 term 码！** 项目只需处理 `16` 即可支持小学期，无需等教务"改格式"。
- 课表数据 JSON（`xskbcx_cxXsgrkb.html` / `xskbcxMobile_cxXsKb.html`）每条记录带 `xnm`/`xqm`（如 `2025`/`12`），`xnmc`/`xqmc`（如 `2025-2026`/`裕华校区`）——这是课程自身字段，**不是学期列表接口**。

**结论对 #30 的影响（落地比原评估更轻）**：
1. 不存在"干净的学期列表 JSON"，学期列表嵌在 HTML `<select>` 里；**解析 HTML 脆弱，不推荐**。
2. 但 term 码 `16` 已存在 → 学期维度**无需"动态查询"，改为"枚举"即可**：学期候选 = `[3,12,16]`（三学期制，含小学期），与教务下拉一致且稳健。学年无需全量枚举，仅需"当前±前后若干年"用于 #29 临时预览与成绩查询。
3. 因此 #30 从"结构性大改（解析动态列表）"降级为"枚举扩展 + 计算逻辑修正 + 数据驱动化"，工作量中等偏下。

### 让项目"随教务自动适配"的改造方向（治本，据 HAR 修订）
核心：**学期维度从"硬编码秋/春二分"改为"三学期枚举 `[3,12,16]`"**（教务下拉已含 `16`=小学期）；天数/节次/周数全数据驱动。据此：
1. **学期枚举 `[3,12,16]`** → 替代 `guessCurrentSemester` 的秋/春二分；探测/成绩/课表全部改读该枚举，小学期(`16`)自动可见。（解决 H2/H3）
   - "下学期/下一学期"推算：在 `[3,12,16]` 顺序取下一项，到 `16` 后回绕次年 `3`：`(y,3)→(y,12)→(y,16)→(y+1,3)`。（#29 直接复用）
2. **天数数据驱动 + 隐藏开关** → 原始设计文档即"周一至周日(7列)"，实现误缩为5列；恢复为**固定 7 列（周一-周日）**，有课的周末列自然显示。新增 **设置→高级功能 "隐藏周末列" 开关**（#31）：默认"隐藏无课的周末列"（数据安全，避免丢课），用户亦可强制隐藏。此开关 + 7 列布局共同解决 H1，回应管理员"天数跟随教务"诉求。
3. **节次统一持久化** → 把 API 节次表存 DataStore/Room，主课表页与**小组件共用同一份**，删除小组件硬编码。（解决 H4，即 #28）
4. **周数上限动态** → 用 N2154 周映射的最大周数替代 `20`。（解决 H5）
5. 校区码走 DataStore `campusId`（已有键，`fetchPeriods` 未用，改为读取）。（解决 H6）

> 归属：H4=#28（已在 Batch 10）；H1/H2/H3/H5/H6 建议合并为新条目 **#30「课表数据驱动化/小学期适配」**，属 Phase 8 结构性改造。这套改造同时让 #29 的"下学期"计算不再依赖硬编码 3↔12 推算。

---

## #29 修订：去掉学期选择器，改"临时预览"（2026-07-17）

管理员否决"学期选择器"（臃肿）＋"自动切全局学期"，理由：想看旧学期去教务后台即可。改为更轻方案：

**方案：临时预览（Peek），用完即弃**
- 学期末提示框「是否查询下学期课表」→ 确认后：在**同一课表页**加载下学期数据，顶部显示一条**可关闭横幅**："正在查看下学期 · 点此返回本学期"。
- 仅新增 `ScheduleViewModel` 中 1 个布尔标志 `previewingNextSemester` + `nextSemesterYear/term`；**不改全局 `currentSemester`**、**无设置入口**、**无历史浏览**。离开页面或点返回即恢复本学期。
- 若下学期未公布（refresh 返回空/HTML）→ 提示"暂未公布"并本学期内冷却，不再打扰。
- "下学期/下一学期"推算：`[3,12,16]` 顺序取下一项，到 `16` 回绕次年 `3`，即 `(y,3)→(y,12)→(y,16)→(y+1,3)`。（小学期 `16` 已含，无需动态列表）

比选择器轻得多，符合"临时看一眼、不常驻"的诉求。

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
