# 图书馆模块与统一身份认证改造 · 交接状态

- **用途**：把「我的图书馆」模块 + 教务改用学校统一身份认证（CAS）这一轮工作的现状、证据与未完成项，交给下一个会话。
- **阅读时机**：接续图书馆功能、排查统一身份认证登录、或需要判断「这轮到底做到哪一步」时；先读 [readme.md](readme.md)，再读本文件。
- **最后更新**：2026-10-28（第 4 轮）
- **状态**：功能可用，等待下一轮 UI 迭代与写操作补齐

## 目录

- [一句话现状](#一句话现状) — 已交付什么、卡在哪
- [本轮交付物](#本轮交付物) — 新增/重写的文件清单
- [已验证的事实](#已验证的事实) — 带证据的技术结论，避免重复试错
- [未完成项](#未完成项) — 按优先级排列
- [如何构建与验收](#如何构建与验收) — 命令、设备、截图流程
- [已知坑](#已知坑) — 会浪费时间的陷阱

## 一句话现状

教务与图书馆已统一到学校 CAS 单点登录（一次登录两边可用），图书馆模块 7 个只读页面全部落地并已装真机跑通数据；**版本号已提到 v1.6.0**。剩余工作是图书馆写操作（续借/预约/荐购）、界面细节迭代、以及本机 `dsh-android` 插件的 adb 发现未打通。

## 本轮交付物

### 图书馆模块（全部新建）

| 文件 | 作用 |
| --- | --- |
| `app/src/main/java/com/myhebnu/data/remote/opac/OpacApi.kt` | 汇文 OPAC 接口定义（`users/*`、`search/*`、`bibs/*`、`commend/*`） |
| `.../opac/OpacModels.kt` | DTO；**所有字段可空**（Gson 用 Unsafe 分配，缺字段会填 null） |
| `.../opac/OpacAuthManager.kt` | 图书馆会话状态机，复用统一 SSO |
| `app/src/main/java/com/myhebnu/data/repository/OpacRepository.kt` | 统一错误处理、会话失效重登、字符串化 JSON 二次解析、批量封面 |
| `app/src/main/java/com/myhebnu/di/OpacModule.kt` | 独立 OkHttpClient（同一 CookieJar，不挂教务的 AuthInterceptor） |
| `app/src/main/java/com/myhebnu/ui/library/*` | 7 个页面 + ViewModel + 路由 + 格式化工具 + 条码/二维码渲染 |

页面：`LibraryHomeScreen`（模块首页）、`LoansScreen`（当前借阅）、`ReaderCodeScreen`（动态借书码）、`SearchScreen`（馆藏检索）、`BibDetailScreen`（书目详情）、`RequestsScreen`（预约与荐购）、`FinesScreen`（罚金与违约）。入口是首页第五张卡片（`HomeCardPanel.kt`），路由常量在 `ui/library/LibraryRoutes.kt`，分支挂在 `MainActivity.kt` 的 `when (route)`。

### 统一身份认证（重写既有认证链路）

| 文件 | 作用 |
| --- | --- |
| `app/src/main/java/com/myhebnu/data/remote/sso/SsoLoginManager.kt` | CAS 登录状态机：`prepare` / `attempt` / `refreshCaptcha` |
| `.../sso/ZfsoftRsa.kt` | **正方自定义 RSA**（非 PKCS#1），带单测 `app/src/test/java/.../ZfsoftRsaTest.kt` |
| `app/src/main/java/com/myhebnu/data/repository/AuthRepository.kt` | 整体重写：旧的 xtgl 表单登录已**全部删除**，无双路径 |
| `app/src/main/java/com/myhebnu/ui/auth/LoginViewModel.kt` | 每次进入登录页重开会话；验证码只由服务端 `getKaptchaStatus` 决定 |
| `app/src/main/java/com/myhebnu/ui/design/DesignTokens.kt`、`Components.kt` | 全模块复用的间距/动效令牌与基础组件（`ScreenScaffold`、`GroupRow`、`GroupedSurface`…） |

### 关键设计约束（本轮已定的，别推翻）

- 密码处理：**先整串反转**，再按正方自定义容器打包（按 16 位小端字分块、无填充头、`chunkSize = 2 * ((bits+15)/16 - 1)`、多块用空格连接）。写成 PKCS#1 会被判「密码错误，请重新输入」。
- 表单令牌 `execution` 与会话绑定且一次性：**看到的验证码必须与提交的 execution 同源**，不能提交前重开登录页。
- 没有有效 TGC 时，先丢弃 `cas.hebtu.edu.cn` 下的旧 Cookie 再取表单，否则服务器回「登录页面闲置时间过长」。
- 图书馆请求不能走教务的 OkHttpClient（会被塞教务 Referer，且 302→`cas/login` 会误判教务会话过期）。
- 借书码：条形码（Code128）与二维码**必须同源同串**，且真机扫码需要真实白底黑码，这是唯一允许写死颜色的地方。

## 已验证的事实

- SSO 登录：真机日志确认 `getKaptchaStatus` 决定是否出验证码；`attempt` 提交后跟跳到 `index_initMenu.html` 即成功；教务探针 200 且不跳登录页。
- 图书馆：首页 `3 在借 / 6 预约 / 0 逾期 / 0.00 罚金`、额度 `3 / 20`，与官网一致；检索「村上春树」139 种、翻页到 40/102。
- 封面：检索结果自带的 `thumb` **恒为空串**，封面必须另调 `POST search/extend3?coversOnly=true`（body `{"isbns":[],"bibIds":[]}`，返回 `bibId → [{_id, imageUrl}]`，`imageUrl` 可能是协议相对的 `//img3m1.ddimg.cn/..`）。按 ISBN 拼 `https://img.hwlsp.cn/bookcover/xx/xx/ISBN.jpg` 只对部分书目有效。
- 验证码图接口是 `GET {casBase}kaptcha?time=<ms>`，返回 **JPEG**（扩展名写 .png 会被图片工具拒读）。

## 未完成项

1. **图书馆写操作**：续借（`POST users/renew`）、预约/委托（`requests/item_copies` 预检 + 下单接口未抓到）、荐购（`users/requests?type=req.acq.suggest` 提交）。用户已明确本期不做，属下一期。
2. **界面迭代**：用户提出的十条问题逐条真机核验后，八条在 v1.6.0 首个 debug 包里已成立（用 `uiautomator dump` 实测：检索退出清空、输入 `Java` 出 8 条联想、139 种能续页、结果带封面），仅「当前借阅顶部重复的共 N 册/额度」「书目详情『馆藏位置』标题未跟页边距对齐」两条是真缺口，已在 `LoansScreen.kt`（删掉 `LoansHeader`）与 `BibDetailScreen.kt`（给 `SectionHeader` 补 `horizontal = Spacing.screen`）修掉。用户随后又提四条（2026-10-28，见下），代码已改完、`assembleDebug` 通过并装机（debug 包 `com.myhebnu.debug`），真机验证由用户完成。
   - ①首页→图书馆的转场动画与其他卡片不一致：`LibraryHomeScreen` 缺 `sharedScope`/`animatedScope`，卡片 `home-card-library` 在目标页没有配对共享元素，回落到默认淡入淡出。已给 `LibraryHomeScreen` 加这两个参数并用 `sharedCardPageModifier("home-card-library", ...)` 包 `ScreenScaffold`，`MainActivity` 的 `LibraryRoutes.HOME` 分支补传 scope。
   - ②检索页返回越级：根因是 `SearchScreen` 的 `DisposableEffect { onDispose { viewModel.reset() } }`，进书目详情时把检索态清空了。改为「有检索态时返回先 `reset()` 回搜索框，再返回才 `onBack()`」+ `BackHandler(enabled = hasSearchState)`，顶栏返回箭头同走 `handleBack`。阶梯：书目详情 →（结果仍在的）检索结果 → 搜索框 → 我的图书馆。
   - ③结果列表改页码翻页：`SearchViewModel` 删掉累加式 `loadMore()`，改为 `goToPage(target)`（每页 20 条只保留该页条目）+ `reloadCurrentPage()`（失败重试当前页）+ `pagesOf(total)`，状态新增 `totalPage`；`SearchScreen` 删掉 `LoadMoreFooter`（原「上滑加载更多」只转圈不出结果的元凶），改为底部 `ResultPager`（上一页 / `page / totalPage` / 下一页），并用 `LaunchedEffect(page, lastSearched)` 把列表滚回顶部。服务端返回空页时只提示「这一页没有更多结果了」，不清空当前结果。
   - ④荐购状态标签分级：`RequestsScreen.RequestRow` 的 tone 改为「不订购/退订 → `Tone.Critical`（红），入藏/订购 → `Tone.Positive`（绿）」，且否决分支必须排在「订购」之前判断（`不订购` 含 `订购` 子串）。因为琥珀色调色板里 `secondaryContainer` 与 `tertiaryContainer` 都是暖色、Positive 与 Warning 分不开，新增 `app/src/main/java/com/myhebnu/ui/theme/SemanticColors.kt`（`SemanticColors` + `LocalSemanticColors`，由 `MyHEBNUTheme` 按深浅主题注入），`Color.kt` 加 `Success*` 一组固定绿色，`Components.kt` 的 `StatusPill` 把 `Tone.Positive` 换成这套成功色。
   - ⑤（2026-10-28 追加）检索结果改双列交错瀑布流（小红书式）：`SearchScreen` 的 `ResultList` 由 `LazyColumn` + 单个 `GroupedSurface` 换成 `LazyVerticalStaggeredGrid(columns = Fixed(2), verticalItemSpacing = md, horizontalArrangement = spacedBy(md))`，统计行与页码条用 `StaggeredGridItemSpan.FullLine` 跨列；新卡片 `BookCard` = 封面 + 题名（`titleMedium` 16sp SemiBold）+ 作者（`bodyMedium` 14sp）+ 出版者·年（`bodyMedium` 14sp，`onSurfaceVariant`），底色 `surfaceContainerLow`、圆角 `shapes.large`；封面 `BookCover` 用 Coil `AsyncImage(onSuccess = …)` 读 `drawable.intrinsicWidth/Height` 动态 `aspectRatio`（clamp 0.55–1.0，默认 0.7），缺封面或加载失败退回「书本图标 + 暂无封面」占位。卡片挂共享元素 `sharedCardPageModifier("search-card-<bibId>")`，`BibDetailScreen` 新增 `sharedScope`/`animatedScope` 参数并用同 key 挂整页，`MainActivity` 两处调用补传 scope，从而复用首页卡片那套容器变换动画。原列表行右侧的「N 册 / 借完」角标随行式布局一并移除，可借册数仍在详情页展示。
   - ⑥（2026-10-28 追加）瀑布流三条体验问题：①滑动卡顿——`BookCover` 原来在 `onSuccess` 里读图片真实尺寸回写 `aspectRatio`，图片到达后条目变高、交错网格重排整列；改为按书目标识哈希取固定档位 `COVER_RATIOS = [0.62f, 0.68f, 0.74f, 0.8f]`（`coverRatioFor(seed)`），高度在图片到达前即恒定，图片继续 `ContentScale.Crop`，去掉 `onSuccess` 回写。②从详情返回跳回顶部——根因是 `ResultList` 的 `LaunchedEffect(uiState.page, uiState.lastSearched) { gridState.scrollToItem(0) }` 在页面重建后再次执行，且 `rememberLazyStaggeredGridState()` 随页面销毁丢失；改为 `gridState` 由 `SearchViewModel` 持有（`val gridState = LazyStaggeredGridState()`），滚顶改成一次性请求：`SearchUiState.pendingScrollToTop` 由 `search()`/`requestPage()` 成功时置 true，`LaunchedEffect(uiState.pendingScrollToTop)` 滚顶后调 `viewModel.onScrolledToTop()` 消费。③出版者/出版年降一级字号并压成一行：`BookCard` 拆出 `pubYear` 参数，出版者 `bodySmall` 12sp + `maxLines = 1` + `Modifier.weight(1f, fill = false)`（带 weight 的子项最后测量，空间不足先省略出版社），出版年单独一个不换行 `Text`（`" · $pubYear"`）始终可见。已 `assembleDebug` 通过并装机（`lastUpdateTime=2026-10-28 20:59:29`），未做点击调试。
   - ⑦（2026-10-28 追加）图书馆首页骨架屏 + 卡片动画提速：①首页首载原来是居中 `CircularProgressIndicator`（`LoadingBlock`），改为 `LibraryHomeSkeleton`——形状照抄真实布局（四个指标块、额度线、最近应还两行、`常用` 五行入口、`借阅偏好` 三行），静态标题直接用真文案，数据到达只换掉灰块、位置不动；转场因此有稳定结构，不再「一个圈 → 一整页」跳变，动画也不再和进度圈重绘抢帧。骨架组件是设计系统的 `SkeletonBox(modifier, shape)`（`Components.kt`），1.4s 线性高光扫过一次（`rememberInfiniteTransition` + `drawBehind` 里的 `Brush.linearGradient`，只重绘不重组）。②书目卡片动画提速：`ui/components/SharedCardPage.kt` 增加 `QuickCardBoundsTransform`（`BoundsTransform` 220ms `tween` + `FastOutSlowInEasing`），`sharedCardPageModifier` 新增可选参数 `boundsTransform`（默认 null = Compose 默认弹簧），检索卡片 `BookCard` 与 `BibDetailScreen` 两处传它，展开/收回都更利落；首页其它卡片维持原节奏。注意 `BoundsTransform` 是 `androidx.compose.animation` 的顶层接口，不是 `SharedTransitionScope` 的嵌套类型，用 `@OptIn(ExperimentalSharedTransitionApi::class)` 标注顶层 val。
3. **英文资源**：图书馆各页面文案目前中文硬编码（汇文返回的馆藏地/状态本身只有中文），`values-en` 只覆盖首页卡片标题与提示。
4. **`dsh-android` 插件**：`adb` 未在 PATH，`ADB`/`ANDROID_HOME` 未设置，SDK 实际在 `D:\Software_data\Android Studio\SDK`，插件因此报 `adb is unavailable`。会话中尝试建 `%LOCALAPPDATA%\Android\Sdk` junction 被用户拒绝。**需要用户在启动 DSH 前设置 `ADB` 环境变量或把 platform-tools 加入 PATH**，代理不要擅自改系统目录。
5. **记忆库其余文件**：`architecture/`、`tech-stack.md`、`implementation-plan.md` 尚未同步本轮内容（只更新了 [readme.md](readme.md) 与本文件）。

## 如何构建与验收

```powershell
./gradlew.bat :app:assembleDebug
# 产物：app/build/outputs/apk/debug/MyHEBNU-v1.6.0-debug.apk

$adb = "D:\Software_data\Android Studio\SDK\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\MyHEBNU-v1.6.0-debug.apk
```

真机（Xiaomi 24129PN74C，序列号 `db072f50`）验收由用户完成。代理自查时：`am force-stop` + `am start -n com.myhebnu.debug/com.myhebnu.MainActivity`，用 `input tap` 点卡片，`screencap -p` + `adb pull` 取图。

Gradle 与 adb 都必须用 `sandbox_permissions: danger-full-access`（缓存 `D:\Software_data\DevCache` 在工作区外，adb 要命名管道）。中文输出在 bash 里是乱码，验证中文请写文件后用 `ops_read_files` 读。

## 已知坑

- 深链 `myhebnu://navigate/library` 不可用：清单里的 intent-filter 缺 `CATEGORY_DEFAULT`/`BROWSABLE`，`am start` 解析失败——用坐标点击代替。
- `adb exec-out screencap > x.png` 会写出损坏文件；用 `adb shell screencap -p /sdcard/x.png` 再 `adb pull`。
- 屏幕超时会截出全黑图；先 `input keyevent KEYCODE_WAKEUP`。
- `input text` 打英文时 MIUI 输入法会先放进候选区，需要再发一次 `input keyevent 62`（空格）把字符提交到输入框，联想词才会触发。
- 用真机做搜索验证时，别拿中文关键词：`input text` 不支持非 ASCII，会卡在拼音候选态。
- 探测脚本在 `build/`（未纳入版本管理）：`probe_covers.py` 能一键登录 + 检索 + 验证封面接口；`cas_oracle.py` 可跑完整 CAS 登录。
