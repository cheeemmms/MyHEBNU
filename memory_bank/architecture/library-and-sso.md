# library-and-sso.md · 图书馆模块与统一身份认证

- **用途**：说明「我的图书馆」（汇文 OPAC）模块的结构、接口与 UI 约定，以及教务/图书馆共用的学校 CAS 单点登录链路。
- **阅读时机**：改动图书馆任一页面、排查 OPAC 请求与会话问题、调整 CAS 登录或验证码流程、给图书馆加写操作时。
- **最后更新**：2026-10-30
- **状态**：只读功能已交付（v1.7.0 发布）；写操作未做

## 目录

- [一句话概述](#一句话概述) — 这套东西是什么、边界在哪
- [包与文件结构](#包与文件结构) — 代码放在哪
- [页面与路由](#页面与路由) — 7 个页面、路由常量、返回阶梯
- [数据与网络层](#数据与网络层) — 独立 OkHttp、会话状态机、统一错误处理
- [已验证的 OPAC 接口](#已验证的-opac-接口) — 路径与实测结论
- [统一身份认证（CAS）](#统一身份认证cas) — 登录链路、密码加密、硬约束
- [UI 约定](#ui-约定) — 设计系统、共享元素、瀑布流、标签配色
- [已验证事实](#已验证事实) — 真机/接口实测记录
- [已知坑](#已知坑) — 会浪费时间的陷阱
- [未完成](#未完成) — 写操作与 i18n

## 一句话概述

学校把教务入口换成 CAS 标准的 `sso/zfiotlogin`，图书馆（汇文 OPAC，`hwdw.hebtu.edu.cn`）也走同一套 CAS；两者共用 `cas.hebtu.edu.cn` 域下的 TGC，**成功认证一次即两边可用**。图书馆模块只做只读查询：借阅、借书码、馆藏检索、书目详情、预约与荐购、罚金。教务与图书馆是两套独立会话、两个 OkHttpClient，但共用同一个 `PersistentCookieJar`（按 host 分桶）。

## 包与文件结构

| 位置 | 职责 |
| --- | --- |
| `app/src/main/java/com/myhebnu/data/remote/opac/OpacApi.kt` | Retrofit 接口定义（`users/*`、`search/*`、`bibs/*`、`commend/*`、`new/books`、`third_api/*`） |
| `.../opac/OpacModels.kt` | DTO 与 `OpacEnvelope<T>`；**全部字段可空**（Gson 用 Unsafe 分配，缺字段是 null 而不是默认值，取值一律 `orEmpty()` / `?:`） |
| `.../opac/OpacAuthManager.kt` | 图书馆会话状态机：`ensureLoggedIn(force)`、`invalidate()`；`OpacAuthState { Unknown, LoggedIn, LoggedOut }` |
| `app/src/main/java/com/myhebnu/data/repository/OpacRepository.kt` | 业务方法 + 统一错误处理 + 会话失效重登 + 字符串化 JSON 二次解析 + 封面批量取 |
| `app/src/main/java/com/myhebnu/di/OpacModule.kt` | `@Named("opac")` 的独立 OkHttpClient 与 Retrofit（`BASE_URL = https://hwdw.hebtu.edu.cn/meta-local/opac/`） |
| `app/src/main/java/com/myhebnu/ui/library/LibraryRoutes.kt` | 路由常量与书目详情参数编解码 |
| `app/src/main/java/com/myhebnu/ui/library/*Screen.kt` + `*ViewModel.kt` | 7 个页面，每个页面一个 ViewModel |
| `app/src/main/java/com/myhebnu/ui/library/LibraryFormat.kt` | 日期、册数、作者串等格式化；`coverUrl(isbn)` 兜底封面地址 |
| `app/src/main/java/com/myhebnu/ui/library/ReaderCodeImage.kt` | ZXing（`zxing-core 3.5.3`）渲染 `BarcodeImage`（Code128）与 `QrCodeImage` |
| `app/src/main/java/com/myhebnu/ui/library/Wishlist{Screen,ViewModel}.kt` | 「我想看的书」列表页与 VM（2026-10-30 新增）：本地 Room 数据 + 封面预取；行尾爱心移除（弹窗确认） |
| `app/src/main/java/com/myhebnu/data/repository/{WishlistRepository,WishlistBackup}.kt` | 想看仓库（toggle 事务 / mergeAll 幂等合并）与 JSON 备份（SAF 导出导入，带 version） |
| `app/src/main/java/com/myhebnu/ui/library/BookCovers.kt` | 封面请求统一入口：缓存键分层（小图 `book-cover-<bibId>` / 大图 `book-cover-lg-<bibId>`）、显式解码尺寸、列表预取 |
| `app/src/main/java/com/myhebnu/ui/library/BibDetail{Screen,ViewModel,Header}.kt` | 书目详情（2026-10-30 重设计）：沉浸头图 + 视差 + 滚动模糊淡出；**换书闸门**（uiState.bibId 比对路由 bibId）防封面串号 |
| `app/src/main/java/com/myhebnu/ui/library/HoldingCard.kt` | 馆藏卡：索书号为主、可借优先、展开收起用 AnimatedVisibility（M3 曲线） |
| `app/src/main/java/com/myhebnu/data/remote/sso/SsoLoginManager.kt` | CAS 客户端：`prepare` / `attempt` / `refreshCaptcha` |
| `.../sso/ZfsoftRsa.kt` | 正方自定义 RSA（非 PKCS#1），单测 `app/src/test/java/com/myhebnu/data/remote/sso/ZfsoftRsaTest.kt` |
| `app/src/main/java/com/myhebnu/data/repository/AuthRepository.kt` | 登录编排（`startLoginSession` / `performLogin` / `autoLogin` / `logout` / `handleSessionExpired`） |

## 页面与路由

`LibraryRoutes` 常量（全部是普通字符串，参数用 `\u0001` 拼进 route，由 `parseBibDetail` 还原）：

| 常量 | 值 | 页面 |
| --- | --- | --- |
| `HOME` | `library` | `LibraryHomeScreen` |
| `READER_CODE` | `library_reader_code` | `ReaderCodeScreen`（动态借书码） |
| `LOANS` | `library_loans` | `LoansScreen`（当前借阅） |
| `REQUESTS` | `library_requests` | `RequestsScreen`（预约与荐购） |
| `FINES` | `library_fines` | `FinesScreen`（罚金与违约） |
| `SEARCH` | `library_search` | `SearchScreen`（馆藏检索） |
| `BIB_DETAIL_PREFIX` | `library_bib\u0001…` | `BibDetailScreen`（书目详情） |

- 入口是首页第五张卡片（`ui/home/components/HomeCardPanel.kt`，route=`library`，共享元素 key `home-card-library`），分支挂在 `MainActivity.kt` 的 `when (route)`。
- 图书馆内部导航**全部压栈**（`navigateTo`），返回 **一律弹栈**（`goBack`），不使用深链。
- **检索页的返回阶梯**：书目详情 →（结果仍在的）检索结果 → 搜索框（清空检索态）→ 图书馆首页。实现是 `SearchScreen` 里的 `hasSearchState = query.isNotBlank() || results.isNotEmpty() || lastSearched.isNotBlank()` + `BackHandler(enabled = hasSearchState)`，有检索态时返回先 `viewModel.reset()`，再返回才 `onBack()`；顶栏返回箭头走同一个 `handleBack`。**不要**再把 `reset()` 放进 `DisposableEffect(onDispose)`——那会在跳详情时清空检索态。

## 数据与网络层

- **为什么单独一套 client**：教务的 `AuthInterceptor` 会给所有 host 强塞教务 Referer/Origin，并把 CAS 的 `302 → cas/login` 误判成教务会话过期，污染全局会话状态；OPAC 还需要自己的 `X-Hw-Lang: i18n.zh-cn`、`X-Hw-explorer: m` 头。两个 client 共用同一个 `PersistentCookieJar`，Cookie 按 host 分桶，互不影响。
- **会话**：`OpacAuthManager.ensureLoggedIn(force)` 在每次请求前执行；OPAC 的 CAS 登录走 `LIBRARY_SERVICE`，成功后探 `mspace/casBoard` 验证。
- **统一错误处理**（`OpacRepository.request`）：先 `ensureLoggedIn()`；失败且是会话错误（HTTP 401/403/302，或消息含「未登录 / 登录超时 / NOT_LOGIN」）则 `invalidate()` 后强制重登重试一次，仍失败返回 `OpacApiException("图书馆会话已失效，请重新登录")`。
- **信封解包**（`unwrap`）：`code != 0` 抛 `OpacApiException(msg 或「图书馆服务返回异常（code=…）」)`，`data == null` 抛「图书馆未返回数据」；业务层看到的是 `Result<T>`，不向上抛异常。
- **字符串化 JSON**：OPAC 部分字段返回的是「JSON 里的 JSON 字符串」（如 `loan_info`、`users/stats` 的分布数据），需要 `Gson` 二次解析，`OpacRepository` 里用同一个 `Gson` 实例处理。

## 已验证的 OPAC 接口

| 用途 | 请求 | 备注 |
| --- | --- | --- |
| 读者信息 | `GET users/info` | 含姓名/学号（页面不展示，见「UI 约定」） |
| 借阅统计 | `GET users/stats` | 首页指标条：在借/预约/逾期/罚金 |
| 额度 | `GET users/loan_info` | 直接返回形如 `"3 / 20"` 的文本 |
| 借书码 | `GET users/elec` | 返回 4 组 4 位码；条形码与二维码**必须同源同串** |
| 借阅列表 | `GET users/loans` | 服务端按到期日排序，`page/pageSize` |
| 即将到期 | `GET users/overdue_soon_page` | 首页「最近应还」 |
| 罚金 | `GET users/fines` | 结构同 requests 列表 |
| 借阅分布 | `GET users/loan_range_dist`、`users/loan_dcp_dist` | 近 12 个月趋势 / 分类占比 |
| 预约与荐购 | `GET users/requests/list`（`doing`/`all`）、`GET users/requests?type=req.acq.suggest` | 预约列表 / 荐购列表 |
| 检索 | `POST search/` | body `OpacSearchRequest(page, pageSize, indexName="idx.opac", sortField="relevance", collapseField="groupId", queryFieldList=[OpacQueryField(field, [query], operator)])` |
| 联想 | `GET search/_suggest?fieldName=all&query=..&size=8` | 220ms 防抖后请求 |
| 检索历史 | `GET search_hist/user` | 最近搜索 |
| 封面 | `POST search/extend3?coversOnly=true`，body `{"isbns":[],"bibIds":[]}` | 返回 `bibId → [{_id, imageUrl}]`，`imageUrl` 可能是协议相对 `//img3m1.ddimg.cn/..`，需补 `https:` |
| 书目详情 | `GET bibs/{bibId}/infos?detail=true&isMobile=1` | 编目字段以中文标签为键，值可能含 `<a>` HTML |
| 馆藏 | `GET bibs/{bibId}/holdings` | 馆藏地、索书号、条码、是否在架 |
| 豆瓣补充 | `GET third_api/douban/{isbn}/{bibId}/info` | 简介/评分，可能 404 |
| 新书/热门 | `GET new/books`、`GET commend/hot1` | 检索页发现区各 5 条 |

> 检索结果自带的 `thumb` **恒为空串**，封面只能靠 `search/extend3` 批量取；按 ISBN 拼 `https://img.hwlsp.cn/bookcover/xx/xx/ISBN.jpg`（`LibraryFormat.coverUrl`）只对部分书目有效，所以封面解析顺序是：`search/extend3` → `item.thumb` → `LibraryFormat.coverUrl(isbn)`。

## 统一身份认证（CAS）

```
GET  <service-url>                → 302 cas.hebtu.edu.cn/cas/login?service=...
GET  /cas/login?service=...       → 登录页隐藏域 execution / _eventId
GET  /cas/v2/getKaptchaStatus     → "true"/"false"，是否需要图形验证码
GET  /cas/kaptcha?time=<ms>       → 验证码图片（JPEG，4 位数字）
GET  /cas/v2/getPubKey            → {"modulus": .., "exponent": ..}
POST /cas/login?v=<random>        → 表单提交，password 经 RSA 加密
     302 → <service-url>?ticket=ST-... → 目标系统建立会话
```

| service | 值 |
| --- | --- |
| 教务 | `http://jwgl.hebtu.edu.cn/sso/zfiotlogin`（注意不是旧版 `/xtgl/login_slogin.html`） |
| 图书馆 | `https://hwdw.hebtu.edu.cn/meta-local/opac/cas/rosetta` |

硬约束（推翻会直接登录失败）：

- **密码加密不是标准 PKCS#1**：先整串反转，再按 16 位小端字分块、无填充头、`chunkSize = 2 * ((bits+15)/16 - 1)`、多块用空格连接，输出 hex。写成 PKCS#1 会一直提示「密码错误，请重新输入」。见 `ZfsoftRsa`。
- **`execution` 与会话绑定且一次性**：看到的验证码必须与提交的 `execution` 同源，不能提交前重开登录页。
- **登录失败返回 200 + 完整登录页**（带新 `execution` 与 `<span id="msg">` 文案），失败后必须重新解析表单才能重试。
- **没有有效 TGC 时先丢弃 `cas.hebtu.edu.cn` 下的旧 Cookie** 再取表单，否则服务器回「登录页面闲置时间过长」。
- **CAS client 不跟随重定向**（`followRedirects(false)`），每一跳都要让 CookieJar 接管 `Set-Cookie`。
- 会话失效信号：`302` 到 `cas/login`/`login_slogin`、`401`、`403`，以及 ZFSOFT 的非标准 `901`（见 [../diagnostics/http901-session-expiry.md](../diagnostics/http901-session-expiry.md)）。

## UI 约定

| 项 | 约定 |
| --- | --- |
| 设计令牌 | 颜色只取 `MaterialTheme.colorScheme`（成功色用 `LocalSemanticColors`）、形状只取 `MaterialTheme.shapes`、间距只取 `Spacing`、可点元素 ≥ `MinTouchTarget`（48dp） |
| 骨架屏 | 首载用 `SkeletonBox(modifier, shape)`（`ui/design/Components.kt`，1.4s 线性高光、`drawBehind` 只重绘不重组）拼出真实布局，图书馆首页是 `LibraryHomeSkeleton`；**不要再回退到居中的 `CircularProgressIndicator`**——转圈会让共享元素转场抖 |
| 共享元素 | 每个入口用 `sharedCardPageModifier(key, sharedScope, animatedScope)` 挂到目标页 `ScreenScaffold(modifier)` 上；key 约定：首页卡片 `home-card-<route>`、检索卡片与详情页 `search-card-<bibId>`；卡片转场用 `QuickCardBoundsTransform`（220ms，`FastOutSlowInEasing`），首页其它卡片仍用默认弹簧 |
| 检索结果 | 双列 `LazyVerticalStaggeredGrid`（`StaggeredGridCells.Fixed(2)`，间距 `Spacing.md`）；**封面宽高比必须由 `coverRatioFor(seed)` 预先定死**（`COVER_RATIOS = [0.62, 0.68, 0.74, 0.8]`），不能在图片加载完成后回写，否则交错网格重排会明显卡顿 |
| 滚动位置 | `gridState` 放在 `SearchViewModel`（`val gridState = LazyStaggeredGridState()`），滚顶用一次性标志 `pendingScrollToTop` + `onScrolledToTop()` 消费；这样从详情返回停在原位置 |
| 翻页 | 每页 20 条，底部 `ResultPager`（上一页 / `page / totalPage` / 下一页），服务端返回空页只提示「这一页没有更多结果了」，不清空已有结果 |
| 状态标签 | `StatusPill` 的 `Tone.Positive` 用 `LocalSemanticColors.successContainer`（琥珀调色板里 `secondaryContainer` 与 `tertiaryContainer` 都是暖色，分不开）；荐购标签：不订购/退订 = `Tone.Critical`（红），入藏/订购 = `Tone.Positive`（绿），判断时否决分支必须在「订购」之前（`不订购` 含 `订购` 子串） |
| 个人信息 | 图书馆各页面**不再展示姓名/学号/班级**（首页指标条已足够）；借书码页为方便扫码仍显示掩码姓名（`苟*霖`）与学号，条形码/二维码需真实白底黑码，是唯一允许写死颜色的地方 |
| 文案 | 图书馆文案目前中文硬编码（汇文返回的馆藏地/状态本身只有中文），`values-en` 只覆盖首页卡片标题与提示 |

## 已验证事实

- 图书馆首页指标与官网一致：`3 在借 / 6 预约 / 0 逾期 / 0.00 罚金`，额度 `3 / 20`。
- 检索「村上春树」139 种、「Java核心技术」102 种；页码翻页与瀑布流封面均实测可用。
- 输入 `Java` 后弹出 8 条联想；检索页退出再进回到发现区（检索态已清空）。
- 书目详情页「馆藏位置」标题与「内容简介」「出版信息」左边缘一致（`Spacing.screen`）。
- CAS 登录：`getKaptchaStatus` 决定是否出验证码；`attempt` 提交后跟跳到 `index_initMenu.html` 即成功；教务探针 200 且不跳登录页。

## 已知坑

- 深链 `myhebnu://navigate/library` 不可用：清单里的 intent-filter 缺 `CATEGORY_DEFAULT`/`BROWSABLE`，`am start` 解析失败，只能用界面点击。
- `GroupRow` 的 `trailingText` 与文字共享行宽，给馆藏行加「在架」会把 supporting 行挤成两行；当前实现是 `trailingText = if (available) null else "借出"`。
- 验证码图接口是 `GET {casBase}kaptcha?time=<ms>`，返回 **JPEG**（扩展名写 .png 会被图片工具拒读）。
- `input text` 打英文时 MIUI 输入法先放进候选区，需要再发一次 `input keyevent 62`（空格）提交到输入框；中文关键词只能手动输入。
- `adb exec-out screencap > x.png` 会写出损坏文件，用 `adb shell screencap -p /sdcard/x.png` 再 `adb pull`；屏幕超时会截出全黑图，先 `input keyevent KEYCODE_WAKEUP`。

## 未完成

1. **图书馆写操作**：续借 `POST users/renew`、荐购提交 `users/requests?type=req.acq.suggest`、预约/委托（`requests/item_copies` 预检 + 下单接口尚未抓到）。用户已明确留到下一期，接口线索记在 [../implementation-plan.md](../implementation-plan.md)。
2. **英文资源**：图书馆页面文案硬编码中文。
3. **检索结果角标**：卡片右侧的「N 册 / 借完」在改瀑布流时移除，可借册数只在详情页展示；若产品要求，可做成封面角标。
4. **想看数据云端同步**：当前仅本机存储 + 手动 JSON 导出导入（2026-10-30 设计决策）；如需跨设备同步另立项。
