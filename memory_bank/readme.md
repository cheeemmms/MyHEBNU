# memory_bank · 记忆库总纲

- **用途**：MyHEBNU 项目记忆库的唯一入口，承载懒加载协议、文件清单与当前快照。
- **阅读时机**：任何会话开始处理本项目时，先只读本文件。
- **最后更新**：2026-10-31
- **状态**：稳定

## 目录

- [懒加载协议](#懒加载协议对所有会话生效) — 读取顺序与边界
- [当前快照](#当前快照) — 项目类型、版本、下一步、阻塞
- [文件清单](#文件清单) — 每个文件「何时读」
- [维护约定](#维护约定) — 更新规则
- [历史记录](#历史记录) — 记忆库自身的结构调整

## 懒加载协议（对所有会话生效）

1. 开始任务时**只读本文件**：「当前快照」+「文件清单」通常已足够定位工作。
2. 需要细节时，按「文件清单」的「何时读」一列，按需打开 **1–2 个**具体文件。
3. 不要一次性读取整个 `memory_bank/`，也不要为了「了解全貌」而全量读取。
4. 拆分文件的唯一理由是**主题分化**（一个文件承载多个可独立引用的主题）。不设行数硬上限，不为满足篇幅指标而拆。
5. 内容较长的文件必须在开头提供**小节目录**（锚点 + 一句话说明），让读者直接跳到相关小节。

## 当前快照

| 项目 | 内容 |
| --- | --- |
| 项目 | **MyHEBNU** — 河北师范大学非官方 Android 教务助手（包名 `com.myhebnu`） |
| 项目类型 | 既有仓库 · 接手维护（GitHub `cheeemmms/MyHEBNU`，132 次提交，2026-06-04 起） |
| 当前版本 | **v1.7.1**（2026-10-31 已发 GitHub Release，tag `v1.7.1`；同日因备份导入崩溃撤包后补发）：v1.7.0 之上完成 Compose 1.9 迁移、路由迁移 NavHost、书目详情转场定稿、详情数据预取，并修复 release 包「想看」备份导入 |
| 当前阶段 | Phase 0–7 与 v1.6.0/v1.7.0 已交付；**图书馆写操作（续借/荐购/预约）与 Phase 8「打磨/收尾」未完成** |
| 最近一次整理 | 2026-10-30 记忆库精简：删除 `design-document.md` 与仓库根 `widget/HyperOS-Widget-Adaptation-Plan.md`（内容过期），有效内容并入 handover/architecture/tech-stack；同时校正「无底部导航栏」「Room v6 四张表」「首页五张卡片」等与代码不符的描述。详见 [history/change-log.md](history/change-log.md) |
| 最近一次实质进展 | 2026-10-31（**已提交，HEAD 分支 `refactor/navigation-compose`**）**路由迁移 NavHost + 想看行容器变换 + Compose 1.9 迁移 + 书目详情转场按录屏对齐**：① 依赖升级到 Compose 1.9.5 / Material3 1.4.0（AGP 8.13.2、compileSdk 36、Glance 1.2.0，Kotlin 2.2.21 / KSP / Gradle 9.5.1 不动），源码只有 1 处 API 改名（`Modifier.sharedElement` 的 `state` → `sharedContentState`）；② 详情页转场回到「整页挂共享容器」：`sharedBounds` 默认 `ScaleToBounds(FillWidth, Center)` + `ParentClip`、不传 enter/exit、几何 220ms `M3EmphasizedEasing`；③ 详情路由的页面级转场改成 `fadeIn(200ms)`（无 delay、去掉 `scaleIn`）+ `fadeOut(60ms)`，内容与封面同时显形，落位时已是实心；④ 详情页根节点补不透明底色；⑤ 撤掉 `contentFadeThrough` / `settleContentAfterTransform` 两个开关。时序与已否决做法见 [architecture/modules-and-ui.md](architecture/modules-and-ui.md#共享元素与容器变换)，进度见 [implementation-plan.md](implementation-plan.md)；⑥ 「想看」列表行接入与检索卡片相同的整页容器变换（共用 `bib-card-<bibId>` 键，`2604012`）；⑦ 路由层从手写返回栈迁移到 androidx.navigation 的 `NavHost`（`27f90b9` 先用手写 `SaveableStateProvider` 修返回丢滚动位置，`fc3573a` 整体换成 `NavHost` + `NavGraphBuilder.page()` 统一四类转场，书目详情改 query 参数 + `Uri.encode`）；⑧ 详情数据改为点击即预取（`BibDetailLoader`，60s 内存缓存、按 bibId 单飞、并发上限 1）+ 馆藏定高占位，消除落位后内容突现（`141994d`）；⑨ **修复 release 包「想看」备份导入崩溃**（`d5b9538`）：Gson 反射模型落在 `com.myhebnu.data.repository`、不在 `data.remote` 的 proguard keep 范围内，被 R8 重命名字段 / 退化 `items` 泛型签名 / 裁掉 `version`，导致 debug↔release 互导皆失败；修法 = proguard 字段级 keep + 每字段 `@SerializedName` + `decode` 安全转换，并新增 `WishlistBackupTest` 钉死键名 |
| 最近一次实质进展（上一轮） | 2026-10-30（晚）**「想看」功能闭环 + 书籍详情页重设计 + 动画第一版定案**，8 个 commit（`0a7a119`→`31c914e`，含 `0a7a119` 本身）全部真机验收并推送：① 想看数据层（Room 5→6，wishlist 快照表）→ 详情页收藏胶囊 → 想看列表页（封面/移除确认/空态）→ 图书馆首页入口与指标（「罚金」格改「想看」）→ JSON 导出导入（SAF、按 bibId 幂等合并）；② 详情页重设计：沉浸头图（高度固定、渐变按封面比例贴图收口）+ 45% 视差 + 滚动模糊淡出 + 状态栏图标随深度反色 + 首帧性能优化（0.8 倍解码、模糊层条件渲染）；③ 封面缓存键分层（`BookCovers.kt`）与检索/想看列表预取；④ 修「换书显示上本书封面」（uiState.bibId 闸门）等三个严重缺陷；⑤ 动画第一版定案：容器变换（sharedBounds 整页展开、默认参数）保留为最重视的动画，检索结果 → 详情页不走共享元素，内容级动画用 M3 emphasized 曲线。其间否决的实验：fade through 错开（拉长转场）、RemeasureToBounds（每帧重测整页掉帧）、封面 sharedElement、纯模糊过渡（与共享元素互斥）。**注**：「检索路径不走共享元素」与「否决封面 sharedElement」两条已于同日 21:00 后由管理员推翻，见上一行「最近一次实质进展」。Compose 1.9 迁移列为 P0 待办 |
| 最近一次实质进展（更早） | 2026-10-30（晨）课表周次判定重构（commit `01b9bce`，已验收并推送）：解析改用教务 `oldzc`/`oldjc` 位掩码并由掩码反推起止周与节次，`zcd` 文本解析降为兜底。`CourseEntity` 新增 `weekBits`/`periodBits` 与唯一判定入口 `isActiveInWeek()`，五处统一改调；Room 4→5。同日上午先把 v1.7.0 遗留文档补提交并入主线（`7580673`/`1e66d1b`/`ae4689a`）。 |
| 最近一次实质进展（更早） | 2026-10-27 图书馆模块上线：汇文 OPAC 只读 7 页（首页/当前借阅/动态借书码/馆藏检索/书目详情/预约与荐购/罚金与违约）已装真机跑通数据；同一轮把教务登录从旧 xtgl 表单切到学校 CAS 单点登录，图书馆复用同一会话，旧的 xtgl 登录代码整段删除、无双路径 |
| 最近一次实质进展（更早） | 2026-10-21 MD3 前端整改（15 个颜色角色、`AppShapes`、NavigationBar 五顶层导航，分支 `feat/md3-ui-refresh` 提交 `f99d848`；该底部导航同日在 `54557e5` 被首页卡片共享展开动画取代，现状无 tab）；小组件重做（事件边界刷新引擎 + 硬滑动 + 行数自适应） |
| 下一步 | ① 图书馆写操作（续借/荐购/预约委托）；② 下一版收尾（CHANGELOG/versionCode）；③ 可选跟进：用 `Modifier.onVisibilityChanged` 替掉瀑布流的 `isScrollInProgress` 门控；④ 冷启动首次展开卡片掉帧（已定位为 debug 包 20 dex / 无 baseline.prof，release 包不存在，用户已搁置） |
| 已知阻塞 | 无。adb 已于 2026-10-31 打通（`C:\Users\yongl\AppData\Local\Android\Sdk` 建了指向 `D:\Software_data\Android Studio\SDK` 的目录联接，插件 `resolveAdbBinary()` 走 PATH 命中）；构建/安装/推送需要 `danger-full-access` |
| 交接状态 | **可交接**：先读 [handover-notes.md](handover-notes.md)；图书馆与 CAS 的技术细节在 [architecture/library-and-sso.md](architecture/library-and-sso.md)；未完成项在 [implementation-plan.md](implementation-plan.md) |
| 分支状态 | `refactor/navigation-compose` 已快进合入 `main` 并推送；HEAD = `main` = `d5b9538`，与 `origin/main` 同步；tag `v1.7.1` 已（撤包后重新）发布；迁移基线 tag `pre-compose19` = `dda8017`；工作区干净 |
| 最后更新 | 2026-10-31 |

## 文件清单

### 核心文件

| 文件 | 内容 | 何时读 | 最后更新 |
| --- | --- | --- | --- |
| [architecture.md](architecture.md) | 系统全景、模块边界、关键架构决策与子文档导航 | 改结构、找代码位置、评估影响面 | 2026-10-30 |
| [implementation-plan.md](implementation-plan.md) | 当前里程碑、未完成任务、验证方式与风险 | 排期、接续未完成的活、确认验收标准 | 2026-10-30 |
| [tech-stack.md](tech-stack.md) | 技术栈、版本矩阵、构建/运行命令、本机环境与编码规范 | 配环境、跑构建测试、加依赖、判断该用什么写法 | 2026-10-30 |
| [handover-notes.md](handover-notes.md) | 上手路径、产品定位与边界、关键文件地图、工作流约定、坑与未决问题 | 刚接手、久未参与、准备交接 | 2026-10-30 |

> 项目按「既有仓库 · 接手维护」维护，核心文件是 `readme.md`、`architecture.md`、`tech-stack.md`、`handover-notes.md` 加主题子文档。`implementation-plan.md` 因承载「当前路线图 + 验收标准 + 验证结果」而保留为独立主题；原 `design-document.md` 已于 2026-10-30 删除——它的有效内容（产品定位、目标与非目标、已放弃项、状态表达约定）分别并入 `handover-notes.md`、`architecture.md`、`tech-stack.md`，其余是与代码重复的需求台账。

### 架构子文档

| 文件 | 内容 | 何时读 | 最后更新 |
| --- | --- | --- | --- |
| [architecture/modules-and-ui.md](architecture/modules-and-ui.md) | 包结构、模块职责、导航与页面状态、主题与 DI | 新增页面/模块、改导航或主题 | 2026-10-31 |
| [architecture/data-and-storage.md](architecture/data-and-storage.md) | Room 实体/DAO/迁移、DataStore 键、凭证与 Cookie、缓存策略 | 改数据模型、排查缓存/迁移问题 | 2026-10-30 |
| [architecture/ea-api.md](architecture/ea-api.md) | 教务系统认证、请求门控、课程/成绩/空教室/考试接口与响应结构 | 对接教务、排查请求失败、扩展接口 | 2026-10-30 |
| [architecture/library-and-sso.md](architecture/library-and-sso.md) | 图书馆模块的结构、OPAC 接口、CAS 单点登录链路与 UI 约定 | 改图书馆页面、排查 OPAC/登录问题、加写操作 | 2026-10-30 |
| [architecture/widget-and-reminders.md](architecture/widget-and-reminders.md) | Glance Widget、跨进程数据、WorkManager 提醒、更新检查 | 改 Widget/通知/后台任务 | 2026-10-21 |
| [architecture/security.md](architecture/security.md) | 威胁模型、明文 HTTP、Cookie/日志/备份风险与修复优先级 | 发布前安全检查、处理凭证/隐私问题 | 2026-10-21 |

### 诊断复盘

| 文件 | 内容 | 何时读 | 最后更新 |
| --- | --- | --- | --- |
| [diagnostics/http901-session-expiry.md](diagnostics/http901-session-expiry.md) | HTTP 901 会话失效的根因、日志证据与抓取方法 | 出现 901、会话/自动重登异常 | 2026-10-21 |
| [diagnostics/glance-miui-resources.md](diagnostics/glance-miui-resources.md) | Glance 1.1.1 在 MIUI/HyperOS 上的资源解析坑与修法 | 改 Widget 尺寸/颜色/圆角后异常 | 2026-10-21 |
| [diagnostics/compose-nested-scroll.md](diagnostics/compose-nested-scroll.md) | 空教室闪退的嵌套滚动根因、排查清单与教训 | 出现 Compose measure 崩溃、页面卡死 | 2026-10-21 |
| [diagnostics/startup-latency.md](diagnostics/startup-latency.md) | 启动转圈的内容、根因、本地优先修复与耗时验收方法 | 启动变慢、改会话/首帧逻辑 | 2026-10-21 |
| [diagnostics/schedule-login-false-positive.md](diagnostics/schedule-login-false-positive.md) | 课表页因 JS 注释含“登录”被误判 Session 失效的根因与修复方案 | 课表加载失败/重试无效/清数据无效 | 2026-10-21 |

### 历史归档

| 文件 | 内容 | 何时读 | 最后更新 |
| --- | --- | --- | --- |
| [history/change-log.md](history/change-log.md) | 2026-06-04 → 2026-09-13 的阶段/批次/发布变更日志 | 追溯「为什么有这个改动」、写发布说明 | 2026-10-21 |

> 仓库根目录的 [README.md](../README.md) 面向使用者，[CHANGELOG.md](../CHANGELOG.md) 面向版本发布；两者与记忆库分工不同，不互相复制。

## 维护约定

- 做完有实质影响的改动后，至少刷新本文件「当前快照」和受影响的单个文件。
- 每次更新刷新「最后更新」日期（用本机本地日期）。
- 废弃内容直接删除或移入文件末尾的「历史记录」，不要写成「已从 X 改为 Y」的补丁式叙述。
- 文件之间用相对链接互相引用；同一段内容只保留一处。
- 新增主题文件或子目录时，在本文件「文件清单」登记，并写明「何时读」。
- 文件集按「核心文件 + 主题子文档」维护：一个主题只允许有一个家；一次性文档（需求台账、计划书、交接纪要）用完即删或并入对应文档，不留档。
- 只写有依据的内容。不确定的标 `待验证`，并注明需要什么才能确定，不猜测。
- 项目既有约定：**构建与真机验收由用户在 Android Studio 完成**，除非用户明确要求，代理不自动执行 Gradle 构建。

## 历史记录

- **2026-10-31（路由迁移）**：`NavHost` 取代手写返回栈后同步——[architecture/modules-and-ui.md](architecture/modules-and-ui.md) 的「主界面导航」整节改写（NavHost 注册位置、`page()` 统一四类转场、返回交 `popBackStack`、`SaveableStateHolder` 由 `NavBackStackEntry` 负责、VM owner 变化）并新增历史条目；[architecture/library-and-sso.md](architecture/library-and-sso.md) 的书目详情路由改为 query 参数说明；[architecture.md](architecture.md) 决策 #1 改为「NavHost」；[handover-notes.md](handover-notes.md) 文件地图去掉「手写 backStack」；[history/change-log.md](history/change-log.md) 补 2026-10-31 迁移行。

- **2026-10-30（晚，动画返工）**：快照新增本轮返回（未提交/待验收）并把上一轮降为「上一轮」；「下一步」「分支状态」写明待验收与未提交；[architecture/modules-and-ui.md](architecture/modules-and-ui.md) 新增 [共享元素与容器变换](architecture/modules-and-ui.md#共享元素与容器变换) 一节（key 表、时序契约、硬规则与坑）并撤销「检索路径不走共享元素 / 封面 sharedElement 已否决」；[implementation-plan.md](implementation-plan.md) 新增「进行中 · 动画返工」与验证结果；[handover-notes.md](handover-notes.md) 刷新状态、文件地图、三个新坑与交接清单；[history/change-log.md](history/change-log.md) 补本轮变更行。
- **2026-10-30（状态同步）**：核对仓库实际状态后刷新——`main` HEAD 记为新提交 `ee94009` 并标注「领先 `origin/main` 1 个提交、未推送」；Debug 构建结果更新为当日多轮成功（产物 20:46）；[implementation-plan.md](implementation-plan.md) 补 v1.8.0（待发）里程碑与「想看闭环 / 详情页重设计 / 动画定案」完成项；修正「7 个 commit」为 8 个（含 `0a7a119`）。
- **2026-10-30（记忆库整理）**：按「不得有多余文件」精简——删除 `design-document.md`（v1.5.0 需求台账，与代码重复且缺图书馆/想看）与仓库根 `widget/HyperOS-Widget-Adaptation-Plan.md`（按 4 尺寸 + Fragment 写，待办已全部落地）；[handover-notes.md](handover-notes.md) 新增「产品定位与边界」、任务清单改为指向 [implementation-plan.md](implementation-plan.md)；[architecture.md](architecture.md) 补决策 12/13；[tech-stack.md](tech-stack.md) 补「状态表达」；[architecture/modules-and-ui.md](architecture/modules-and-ui.md) 校正导航（无底部栏/无抽屉）、127 个 Kotlin 文件、Room v6、首页五卡。
- **2026-10-30（晚）**：随「想看」功能闭环与详情页重设计同步——快照换最新进展并压缩旧条目；[data-and-storage.md](architecture/data-and-storage.md) 登记 wishlist 表与 Room 5→6；[library-and-sso.md](architecture/library-and-sso.md) 补想看/详情页/BookCovers 文件与"云端同步未立项"；[modules-and-ui.md](architecture/modules-and-ui.md) 更新共享元素约定（容器变换定案 + 检索路径例外）；[handover-notes.md](handover-notes.md) 刷新状态/文件地图/工作流约定并新增 P0 与四个新坑；[history/change-log.md](history/change-log.md) 新增「2026-10 下旬」章节。
- **2026-10-30**：课表周次判定重构。新增 [architecture/ea-api.md](architecture/ea-api.md) 的位掩码说明（`oldzc`/`oldjc` 为权威周次/节次来源、`zcd` 会出现「第N周」写法、`xslxbj` 是课名尾部符号、换教师为同日同节多条条目）；[architecture/data-and-storage.md](architecture/data-and-storage.md) 更新 `courses` 表（增 `weekBits`/`periodBits`、`oddEven` 降级为兜底）与 Room 4→5 迁移表。上午另有一轮仓库收口：v1.7.0 遗留文档补提交与 `feat/md3-ui-refresh` 快进并入 `main`。
- **2026-10-30（仓库收口）**：`CHANGELOG.md` 的 v1.7.0 段（`7580673`）与记忆库重整（`1e66d1b`）此前只存在于工作区，本次按主题拆成两次提交入库；`feat/md3-ui-refresh` 快进合并进 `main`，两条分支同指并推送。未改动任何 `app/` 源码，与已发布的 v1.7.0 二进制一致。
- **2026-10-28**：随 v1.7.0 发版同步。删除一次性交接文档 `handover-library-sso.md`（长期技术内容并入新建的 [architecture/library-and-sso.md](architecture/library-and-sso.md)，未完成项并入 [implementation-plan.md](implementation-plan.md)，构建/发版命令并入 [tech-stack.md](tech-stack.md)）；[architecture.md](architecture.md) 补图书馆与 CAS 模块、关键决策 9–11；[architecture/modules-and-ui.md](architecture/modules-and-ui.md) 补图书馆页面、逐级返回、`SemanticColors`/`SkeletonBox`/共享元素约定；[architecture/ea-api.md](architecture/ea-api.md) 的登录章节从 xtgl 表单直登改写为 CAS 单点登录；[tech-stack.md](tech-stack.md) 新增「版本与发布」；[handover-notes.md](handover-notes.md) 刷新状态、文件地图与发版流程。
- **2026-10-21**：按团队 memory_bank 规范重整。目录由 `memory-bank/` 改为 `memory_bank/`；`progress.md` 拆分为当前路线图（[implementation-plan.md](implementation-plan.md)）与变更日志（[history/change-log.md](history/change-log.md)）；`architecture.md` 拆为主题子文档；`batch4-requirements.md` 的有效需求并入 `design-document.md`（该文件已于 2026-10-30 删除，有效内容转入 handover-notes / architecture / tech-stack），过程记录归档到变更日志；`HTTP901-诊断与日志采集.md` 与 `glance-api-research.md` 移入 `diagnostics/` 并规范化；新增本入口文件、`tech-stack.md`、`handover-notes.md`、`architecture/security.md`。同日对齐代理端口 7897、统一许可证为 AGPL-3.0、关闭考试分页项、将小学期成绩改为待验证、i18n 搁置、安全项搁置、修正课表长按发布说明。启动改为本地判定并移除启动网络校验，详见 [startup-latency.md](diagnostics/startup-latency.md)。
