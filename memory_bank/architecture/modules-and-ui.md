# modules-and-ui.md · 包结构、导航与界面状态

- **用途**：说明代码如何按包分层、页面如何导航、各屏状态由谁持有，以及主题与依赖注入的位置。
- **阅读时机**：新增页面/模块、修改导航或主题、定位某个界面逻辑。
- **最后更新**：2026-10-30
- **状态**：稳定（2026-10-21 MD3 前端整改后持续维护；`feat/md3-ui-refresh` 已并入 `main`）

## 目录

- [包结构](#包结构)
- [应用入口与导航](#应用入口与导航)
- [页面与状态](#页面与状态)
- [主题与视觉](#主题与视觉)
- [共享元素与容器变换](#共享元素与容器变换) — key 约定、时序契约、硬规则与坑
- [依赖注入](#依赖注入)
- [约定与注意点](#约定与注意点)

## 包结构

`app/src/main/java/com/myhebnu/`（127 个 Kotlin 文件，2026-10-30 统计）：

```text
com.myhebnu
├── MyHebnuApplication.kt       # @HiltAndroidApp；通知渠道 + Worker 调度
├── MainActivity.kt             # 单 Activity；欢迎/登录/主界面；手写返回栈；Widget 深链
├── di/                         # NetworkModule / DatabaseModule / RepositoryModule
├── data/
│   ├── local/
│   │   ├── db/                 # AppDatabase(v6)、dao/、entity/、migrations/
│   │   └── preferences/        # UserPreferences(DataStore)、CredentialManager(加密凭证)
│   ├── model/                  # GitHubRelease
│   ├── remote/                 # EASystemApi、GitHubApi、CasApi、SessionManager、
│   │                           # PersistentCookieJar、CryptoUtil、interceptor/AuthInterceptor
│   └── repository/             # Auth / Schedule / Grade / Room / Exam / Update
├── domain/                     # GradeModels、ExamModels、RoomModels（领域模型）
├── ui/
│   ├── about/                  # AboutScreen、SystemUpdateScreen
│   ├── auth/                   # LoginScreen、LoginViewModel
│   ├── components/             # RefreshFab 等跨页面组件
│   ├── exam/                   # ExamScreen、ExamViewModel、components/ExamCard
│   ├── grade/                  # GradeScreen、GradeViewModel、components/*
│   ├── home/                   # HomeScreen、HomeViewModel、components/HomeCardPanel
│   ├── navigation/             # AppNavigation.kt（TopLevelRoute 路由常量）
│   ├── room/                   # RoomScreen、RoomViewModel、components/FilterPanel、RoomList
│   ├── schedule/               # ScheduleScreen、ScheduleViewModel、components/*
│   ├── settings/               # Settings、Advanced、ColorTheme、SemesterSetup
│   ├── theme/                  # Theme.kt、Color.kt、Type.kt
│   └── welcome/                # WelcomeScreen
├── util/                       # SemesterDateUtils（开学日/周次/日期标签）
├── widget/                     # 3 个 Glance Widget + 数据/Worker/入口/深链
└── worker/                     # ReminderManager + 上课/考试 Worker
```

- 只有一个 Gradle 模块 `:app`；未做多模块拆分。
- `domain/` 只放与 UI 无关的数据类，不放业务用例（用例逻辑在 Repository/ViewModel）。
- 资源：`res/values`（默认中文）、`values-en`、`values-zh`、`values-night`、`xml/`（Widget 配置与网络安全配置）。

## 应用入口与导航

### 启动分层

`MyHEBNUApp` 用一个 `AppScreen` 枚举控制三大阶段：

```text
Welcome（仅首次启动） → Login（无有效会话） → Main（已登录）
```

- 首次启动标记：`UserPreferences.isFirstLaunch`；点击“开始使用”后写为 false。
- **启动判定（2026-10-21 起）**：一次 `combine(isFirstLaunch, isLoggedIn).first()` + `AuthRepository.hasLocalCookieSession()`，**纯本地、不联网**；随后进入 Welcome / Main / Login。
- 会话过期：启动不主动校验；`AuthInterceptor` 在首次 API 调用命中 302/401/403/901 → `sessionExpired` → `autoLogin()` → 成功留主界面，失败回登录页并提示。
- 详细根因、修复与真机计时方法见 [../diagnostics/startup-latency.md](../diagnostics/startup-latency.md)。

### 主界面导航

- `MainAppContent` 持有 `currentRoute: String` + `backStack: SnapshotStateList<String>`。
- `navigateTo` 压栈前进，`goBack` 弹栈；栈空回 `home`。
- **没有底部导航栏，也没有抽屉**：唯一入口是首页卡片面板（`HomeCardPanel`，按 `card.route` 逐张挂共享元素 `home-card-<route>`）；各页面自带 `TopAppBar`。
- 返回拦截：`BackHandler(enabled = currentRoute != TopLevelRoute.Home.route) { goBack() }` —— 除首页外任何路由都被系统返回手势拦下先弹栈，栈空回 `home`。
- 页面切换用 `SharedTransitionLayout { AnimatedContent(targetState = currentRoute) }`，每个页面都接收 `sharedScope`/`animatedScope`；外层 `Box` 用 `Modifier.background(MaterialTheme.colorScheme.background)` 兜底，避免暗色下交叉淡入透出白底，并只给横向 `displayCutout` 加 inset（纵向留给页面自己处理，书籍详情页封面要铺到状态栏下面）。
- 路由集合（`TopLevelRoute` + 字符串）：`home`、`schedule`、`grade`、`empty_room`、`exam`、`settings`、`advanced_settings`、`color_theme`、`about`、`system_update`、`semester_setup`，以及图书馆的 `library`、`library_reader_code`、`library_loans`、`library_requests`、`library_fines`、`library_search`、`library_bib\u0001…`（见 [library-and-sso.md](library-and-sso.md#页面与路由)）。
- 二级页一律压栈前进、弹栈返回；图书馆检索页额外约定「详情 → 检索结果 → 搜索框 → 图书馆首页」的逐级返回，用 `BackHandler(enabled = hasSearchState)` 拦一次返回先 `reset()` 检索态。
- 每个页面自带 `Scaffold` + `TopAppBar`；首页不接 `onBack`（无返回箭头），课表/成绩/空教室/考试/设置等其余页面都接 `onBack = { goBack() }`，因此在这个「无 tab」结构里都能看到返回箭头。
- Widget 深链：`myhebnu://navigate/{route}`，由 `MainActivity.handleDeepLink()` 写入 `pendingNavigation`，主界面消费后清空返回栈并跳转。

> 注意：`ui/navigation/AppNavigation.kt` 里的 `TopLevelRoute` 只是路由常量集合（其 KDoc 还写着「for the Navigation Drawer」，是历史注释，项目里并没有抽屉）；项目**没有使用** Navigation Compose 的 `NavHost`，尽管 `libs.navigation.compose` 仍在依赖中。

## 页面与状态

| 页面 | ViewModel / 状态 | 关键行为 |
| --- | --- | --- |
| 首页 `HomeScreen` | `HomeViewModel` + `HomeUiState` | 问候语按 6–11/12–13/14–17/其余分四档；`NextClassState` = IN_CLASS / HAS_CLASS / ALL_DONE / WEEKEND / HOLIDAY；五张卡片入口（下一节课/空教室/下一场考试/成绩/我的图书馆）；显示更新横幅/学期设置横幅 |
| 课表 `ScheduleScreen` | `ScheduleViewModel` + `ScheduleUiState`（881 行，项目最复杂） | Room Flow + 展示周 + 颜色偏好 `combine`；`dayLabels` 随“显示周末列”在 5/7 列切换；`currentWeek` 与 `displayWeek` 分离；假期周用 `isVacation` 提示；学期末 `SemesterPanelMode` 三态浮层（DISCOVERY/REENTRY/BACK） |
| 成绩 `GradeScreen` | `GradeViewModel` | 观察 Room 全量成绩 → 按学期分组 → 计算 4.0/5.0/加权百分制；首帧保持 loading 防闪“无成绩”；短按/长按悬浮刷新；明细走 `getGradeDetail()` |
| 空教室 `RoomScreen` | `RoomViewModel` | 校区/楼栋/周次/星期/节次筛选；分页 `currentPage/totalPage`；每次查询实时请求 |
| 考试 `ExamScreen` | `ExamViewModel` | 进入页面按当前学期联网拉取；按日期排序、倒计时徽标 |
| 设置 `SettingsScreen` | `SettingsViewModel` | 主题模式、当前周/学期、高级功能、关于；学期设置用 BottomSheet |
| 学期设置 `SemesterSetupScreen` | `SemesterSetupViewModel` | 选学年 + 学期（M3 原生下拉/分段按钮）；保存时用 N2154 周次映射推导开学日/放假日 |
| 自由主题色 `ColorThemeScreen` | `SettingsViewModel` | 6 套内置预设 + 自定义 HSL 色轮；预设 JSON 存 DataStore |
| 关于/系统更新 | `UpdateRepository` | 版本号、源码/交流群/邮箱、许可证；手动检查更新、自动检查开关 |
| 我的图书馆（7 页） | `LibraryHomeViewModel`、`LoansViewModel`、`ReaderCodeViewModel`、`SearchViewModel`、`BibDetailViewModel`、`RequestsViewModel`、`FinesViewModel` | 首页指标条（在借/预约/逾期/罚金 + 额度）与 5 个入口；借书码页 ZXing 渲染条形码 + 二维码（同源同串、26 秒自动换码）；检索页双列瀑布流 + 页码翻页（`gridState` 与 `pendingScrollToTop` 在 ViewModel 里，从详情返回停原位）；详情页含馆藏位置/内容简介/出版信息。细节见 [library-and-sso.md](library-and-sso.md) |

### 关键状态契约

- `ScheduleUiState` 区分 `semesterYear/semesterTerm`（展示学期）与 ViewModel 内部的 `_currentYear/_currentTerm`（真实本学期）；查看下学期时 `currentWeek=0` 防止误高亮。
- `HomeUiState` 不持有网络加载门控：首页数据全部来自本地，`loadHomeData()` 结束即 `isLoading=false`。
- 所有 ViewModel 暴露 `StateFlow<UiState>`；一次性提示（Toast/Snackbar）通过 `consumeToast()` 等显式消费，避免重复弹出。

## 主题与视觉

| 项 | 实现 |
| --- | --- |
| 主题入口 | `MyHEBNUTheme(themeMode, useCustomColors, seedHue)` |
| 明暗模式 | `themeMode` = `light` / `dark` / `system`；`system` 跟随 `isSystemInDarkTheme()` |
| 动态取色 | Android 12+ 默认 `dynamicLight/DarkColorScheme` |
| 自定义主题 | 用户选择 seed hue → `seedColorScheme()` 生成整套色板，优先级高于动态取色 |
| 课程配色 | 课程名 hash 得到稳定 hue，再叠加主题 seedOffset → `CourseTonalPalette`，浅/深色各一套 |
| 颜色角色 | `Color.kt` 的 `lightSurfaceRoles()` / `darkSurfaceRoles()` 补全 MD3 的 `surfaceContainerLowest/Low/…/Highest`、`surfaceDim/Bright`、`surfaceTint`、`inverseSurface/OnInverseSurface` 等 15 个角色（M3 1.3.1 的 `lightColorScheme()` 默认是灰色，必须显式给） |
| 形状 | `Shapes.kt` 的 `AppShapes`：extraSmall 4 / small 8 / medium 12 / large 16 / extraLarge 28 dp；业务代码统一用 `MaterialTheme.shapes.*`，不再写字面量圆角 |
| 状态栏 | `DisposableEffect` 只设置 `isAppearanceLightStatusBars`，不再把状态栏涂成 primary（避免与顶部栏脱节、并入主流体验） |
| 字体 | `Type.kt`；课程卡片按卡片实际高度动态决定课名行数（`WeekViewGrid` 传 `cardHeight`），课名 14/13/12sp 退让、次信息 11sp |
| 文本对比 | 不再用 `copy(alpha = …)` 模拟次级文本，统一走 `onSurfaceVariant`；只有装饰性发光/网格线保留 alpha |
| 弹层/选择器 | 一律 Material 3 原生控件；禁止自研 iOS 风格滚轮 |
| 语义成功色 | `ui/theme/SemanticColors.kt` 的 `SemanticColors` + `LocalSemanticColors`，由 `MyHEBNUTheme` 按深浅主题注入；`Color.kt` 的 `Success*` 是固定绿。用于 `StatusPill` 的 `Tone.Positive`（琥珀色板里没有绿色角色，`secondaryContainer`/`tertiaryContainer` 都是暖色分不开） |
| 骨架屏 | `ui/design/Components.kt` 的 `SkeletonBox(modifier, shape)`：`rememberInfiniteTransition` + `drawBehind` 画 1.4s 线性高光，只重绘不重组；首载页面用它拼真实布局，不要用居中 `CircularProgressIndicator` |
| 共享元素转场 | 容器变换（sharedBounds 整页展开）是**最受重视的动画**；检索结果 / 想看列表 → 书籍详情页在其之上再挂一个**封面共享元素**，见下节 [共享元素与容器变换](#共享元素与容器变换)。key：首页卡片 `home-card-<route>`、页面级 `search-card-<bibId>`、封面级 `search-cover-<bibId>` |

## 共享元素与容器变换

实现全部集中在 `ui/components/SharedCardPage.kt`（2026-10-30 晚返工后的最终形态）。

**三处 key 与挂载位置**

| key | 挂在哪 | 谁与谁配对 |
| --- | --- | --- |
| `home-card-<route>` | 首页卡片（`HomeCardPanel`）与目标页根容器 | 首页 → 各顶层页 |
| `search-card-<bibId>` | 检索卡片根 `Column`、详情页根 `BoxWithConstraints` | 检索结果 → 详情 |
| `search-cover-<bibId>` | 检索卡片封面 `Box`、想看行封面缩略图 `Box`、详情页 `BookBackdrop` 的封面层 `Box` | 封面 → 头图 |

**时序契约（`SharedCardPage.kt` 常量，改动画只动这里）**

| 项 | 值 | 说明 |
| --- | --- | --- |
| 容器尺寸补间 | `ContainerTransformBoundsTransform` = 300ms `M3DecelerateEasing`，**去程回程同一条曲线** | 曾经按 M3 原始 token 分「扩张 decelerate / 收缩 accelerate」，往返看起来像两套动画 |
| 内容退场 | `ContainerExitTransition` = 90ms `M3AccelerateEasing` | 旧内容先退干净 |
| 内容进场 | `ContainerEnterTransition` = 210ms、延迟 90ms、`M3DecelerateEasing` | 与退场合计正好 300ms，无重叠 |
| 目标页内容落定 | `ContainerSettleEnterTransition` = 160ms、延迟 300ms | 目标页挂共享封面时用 `settleContentAfterTransform = true`：共享封面在覆盖层绘制、压在页面内容之上，内容提前进场会被它盖住 |

**硬规则与坑**

- **封面必须是共享元素，不能只靠两端淡变换手**：卡片是小图、详情是整屏大图，两端各画一张再交叉淡化，换手那几帧会发虚、错位——观感就是「闪一下」。`sharedCoverModifier(key, sharedScope, animatedScope)` 用的是 `sharedElement`，转场期间它是一张持续存在、不透明、被容器裁切的图层。
- **两端取景必须一致**：卡片与头图都 `ContentScale.Crop + Alignment.TopCenter`。曾出现卡片 Crop、头图 `FillWidth` 混用，同一本书在转场中换了裁切。
- **目标页的头图节点要嵌在页面节点内部**：共享元素默认 `ParentClip`，靠外层容器（页面级 sharedBounds）裁切边界一起长大。
- **列表行不要整行做容器变换**：想看行是「满宽 × 88dp」的扁条，`ScaleToBounds` 要用 `max(放大比)` 盖住容器（横向近 10 倍），屏幕上是行中间那段文字被拽到版心再放大。只让封面缩略图挂 `sharedCoverModifier`，行内文字随页面淡出。
- **首帧必须有封面**：详情页数据是点击后才请求的，`MainActivity` 通过路由第 4 段把卡片封面 `cover` 带进 `BibDetailScreen(fallbackCover=)`，闸门状态直接用 `immediateCover`（`fallbackCover` 否则按 ISBN 拼图床），否则转场一开始封面是空的、回来时又突然出现。
- **页面级 enter/exit 也要对齐**：`AnimatedContent` 默认 enter 是 `fadeIn + scaleIn(0.92)`。`MainActivity` 用 `transitionSpec` 判定 `isBibDetailRoute(initialState) || isBibDetailRoute(targetState)`，这类往返统一用 `ContainerEnterTransition togetherWith ContainerExitTransition`，其余路由保持 Compose 默认。
- **检索瀑布流仍要按滚动门控**：`sharedBounds` 会给每个已组合的条目建动画状态，快速滑动时会抽动；`ResultList` 用 `gridState.isScrollInProgress` 决定 `BookCard(enableSharedElement)`，Compose 1.7 没有 `Modifier.onVisibilityChanged`（1.9 才有，见 [../implementation-plan.md](../implementation-plan.md) P0）。

> 2026-10-30 晚之前的记忆库写着「检索结果 → 详情页刻意不走共享元素、封面 sharedElement 已否决」。那两条是**模型误改留下的结论**（当时编码模型额度中断、接手的模型误解了指令，把调好的动画拆了），不是管理员决策；2026-10-30 21:00 后已按管理员要求恢复并加强（整卡张成整页 + 封面连续 + 内容落定）。

## 依赖注入

| 模块 | 提供 |
| --- | --- |
| `NetworkModule` | 主 OkHttp（CookieJar + AuthInterceptor + BODY 日志 + 15s 超时 + 禁止自动重定向）、Retrofit（教务 BASE_URL）、CAS Retrofit、GitHub Retrofit（独立 UA/超时） |
| `DatabaseModule` | Room `AppDatabase` v6（courses/exams/grades/wishlist），注册 `MIGRATION_1_2`、`MIGRATION_2_3`、`MIGRATION_3_4`、`MIGRATION_4_5`、`MIGRATION_5_6` |
| `RepositoryModule` | 绑定各 Repository 到 Singleton |
| `OpacModule` | 图书馆专用 `@Named("opac")` OkHttpClient（同一 `PersistentCookieJar`、不挂 `AuthInterceptor`、带 `X-Hw-*` 头）+ Retrofit（`https://hwdw.hebtu.edu.cn/meta-local/opac/`） |

> `HttpLoggingInterceptor.Level.BODY` 目前在所有构建类型中开启。发布前如需避免 Cookie 泄露，应按 `BuildConfig.DEBUG` 降级（见 [../tech-stack.md](../tech-stack.md) 的风险项）。

## 约定与注意点

- 新增页面：在 `MainAppContent` 的 `when(route)` 加分支 + `TopLevelRoute` 常量（如需要），并处理返回。
- 新增带共享元素转场的页面：页面签名加 `sharedScope: SharedTransitionScope? = null, animatedScope: AnimatedVisibilityScope? = null`，`MainActivity` 调用处补传，否则动画会静默退化。需要「封面连续」的路径再挂 `sharedCoverModifier`，规则见 [共享元素与容器变换](#共享元素与容器变换)。
- 新增列表滚动状态：需要跨页面留存时把 `LazyStaggeredGridState`/`LazyListState` 放进 ViewModel，不要用 `remember*`（页面销毁即丢）。
- 新增全局状态：优先放 DataStore（需要跨进程/重启保留）或 ViewModel（会话内）；不要在 Composable 里直接读写 SharedPreferences。
- 新增弹窗/选择器：使用 M3 `AlertDialog` / `ExposedDropdownMenuBox` / `SegmentedButton` / `Slider`。
- 新增滚动容器：先读 [../diagnostics/compose-nested-scroll.md](../diagnostics/compose-nested-scroll.md)。
- 字符串：新增文案优先加到 `values/strings.xml` 与 `values-en/strings.xml`；当前仍有硬编码中文，改动时顺手补齐。

## 历史记录

- **2026-10-30（晚，动画返工）**：新增 [共享元素与容器变换](#共享元素与容器变换) 一节——记录 `sharedCoverModifier`（封面共享元素）、`ContainerSettleEnterTransition`（内容延后落定）、往返统一 300ms decelerate、`AnimatedContent` 的 bibDetail 专属 `transitionSpec`、路由携带 `cover` 避免首帧空封面等约定；并撤销此前「检索路径不走共享元素 / 封面 sharedElement 已否决」的结论（那是模型误改，非管理员决策）。
- **2026-10-30（记忆库整理）**：按当前代码校正——删掉「底层 5 目的地 NavigationBar」这一已不存在的描述，改为首页卡片面板 + 手写返回栈 + 共享元素转场；`TopLevelRoute` 注明位于 `AppNavigation.kt`；Kotlin 文件数 84→127、Room v4→v6、迁移补到 `MIGRATION_5_6`、首页四卡→五卡。
