# change-log.md · 项目变更日志（归档）

- **用途**：按时间顺序记录项目阶段、批次、发布与关键决策，供追溯“为什么有这个改动”。
- **阅读时机**：追溯历史决策、写发布说明、判断某段代码的来历；日常任务不必通读。
- **最后更新**：2026-10-31
- **状态**：归档（2026-06-04 → 2026-10-30；v1.7.0 之后的增量见「2026-10 下旬」章节，发版时再拆）

## 目录

- [2026-06 从 0 到 MVP](#2026-06-从-0-到-mvp)
- [2026-07 打磨与 Batch 10](#2026-07-打磨与-batch-10)
- [2026-08 至 09 稳定与发布](#2026-08-至-09-稳定与发布)
- [2026-10 记忆库重整](#2026-10-记忆库重整)
- [2026-10 下旬 图书馆深化与「想看」闭环](#2026-10-下旬-图书馆深化与想看闭环)
- [发布索引](#发布索引)

> 本文件由旧 `memory-bank/progress.md` 的「变更日志」压缩整理；更细的逐条记录可用 `git log` 与旧文件历史查看。

## 2026-06 从 0 到 MVP

| 日期 | 变更 | 原因/结果 |
| --- | --- | --- |
| 2026-06-04 | 项目启动；8 轮需求沟通；初始化记忆库 | 确定「河北师大非官方教务助手」方向 |
| 2026-06-04 | Phase 0：API 侦察，7 个 HAR + 17 个端点文档 | 确认 ZFSOFT 接口与门控机制 |
| 2026-06-04 | Phase 1：45 文件项目骨架 | Compose + Hilt + Room + Retrofit 分层 |
| 2026-06-04 | Phase 2：SSO 认证（WebView + CookieJar + EncryptedPrefs） | 早期认证方案，后续改为自定义登录 |
| 2026-06-04 | Phase 3：课表（周视图 + Room 缓存 + CourseCard） | 核心功能闭环 |
| 2026-06-04 | Phase 4：成绩（GPA + 明细 Sheet + Canvas 趋势图） | Vico 替换为 Canvas |
| 2026-06-04 | Phase 5：空教室（双校区 + 多条件筛选 + 位掩码） | — |
| 2026-06-04 | Git init + 代理/镜像/Wrapper 配置 | 构建工具链成型 |
| 2026-06-05 | 修复 30 个编译错误（Vico→Canvas 等） | 首次编译通过 |
| 2026-06-05 | **MVP 核心闭环**：83 文件、约 4800 行、13 次提交 | 里程碑 |
| 2026-06-05 | 真机调试：10+ 构建兼容性问题 | 锁定 AGP/Kotlin/KSP/Hilt/Room 版本 |
| 2026-06-05 | mitmproxy 双向抓包 + 教务行为逆向 | 三步门控、浏览器头、Cookie 合并 |
| 2026-06-05 | 修复 Cookie 合并 / WebView URL / 请求头 / 16KB Page | 真机可登录 |
| 2026-06-05 | 课表 + 成绩在小米 15 真机验证 | 功能可用 |
| 2026-06-05 | Batch 1：空教室闪退（8 次提交）+ 登录数据丢失 | 嵌套滚动根因；见 [diagnostics/compose-nested-scroll.md](../diagnostics/compose-nested-scroll.md) |
| 2026-06-05 | 编译警告清零（6 个 deprecation） | 代码质量 |
| 2026-06-07 | Batch 2.5：成绩数据加载修复 | `getAllGrades()` 错误传播 + 内存缓存 + 页面主动刷新 |
| 2026-06-07 | Batch 3：考试安排模块 | 新增领域模型/Repository/VM/Screen/Card |
| 2026-06-07 | Batch 4：单首页 + 课表重设计 + 设置 + 6 轮精修 | 杂志式首页、逐节网格、双层绝对定位、MD3 色板 |
| 2026-06-07 | Batch 5：自定义登录 + 自动重登 + 欢迎页 | 替换 WebView；加密凭证；302 自愈；验证码 |
| 2026-06-08 | Batch 6：导航 + 主题 Bug（#6a–#6g） | 返回栈/BackHandler、自由主题色三层联动、深色闪屏 |
| 2026-06-08 | Batch 7：空教室分页 + 首页上课中状态 + GPA | `queryModel.showCount`；真实 13 节；IN_CLASS 倒计时 |
| 2026-06-08 | Batch 8：联系反馈 + GitHub 更新检测 | GitHub Releases + 应用内检查 |
| 2026-06-08 | Batch 9：设置页胶囊重构 + 关于页 + 系统与更新 | 设置 UI 定稿；OSS Licenses 改内联 |
| 2026-06-09 | Phase 7.1：Widget MIUI 兼容性 | 全量资源化；见 [diagnostics/glance-miui-resources.md](../diagnostics/glance-miui-resources.md) |
| 2026-06-09 | Phase 7.2/7.3：上课提醒 + 考试提醒 | Room 考试缓存；Worker 去重；通知渠道 |
| 2026-06-10 | Widget 预览图/深色/跨日/精修（HS-1~HS-9） | 3 尺寸定稿；19:00 跨日逻辑；HyperOS 配置 |

## 2026-07 打磨与 Batch 10

| 日期 | 变更 | 原因/结果 |
| --- | --- | --- |
| 2026-07-17 | Batch 10 立项（#22–#30） | 真机反馈 + 新功能规划；先修阻塞项 |
| 2026-07-17 | #23 删除浏览器登录（WebView fallback） | 解决部分机型登录后退回桌面 |
| 2026-07-17 | #27 成绩列表补 `showCount=1000` | 修复只显示约 10 门 |
| 2026-07-17 | #22 冷启动白闪修复 | 新增 `values-night/themes.xml` + 窗口背景 |
| 2026-07-17 | #31 显示周末列开关 | 默认 5 列；高级功能开 7 列 |
| 2026-07-17 | #32 课表节次列收窄 | 5 列 36dp / 7 列 28dp；时间字号 8.5sp |
| 2026-07-17 | #33 课表卡片内容自适应 | 取消课名 2 行硬限；`WeekViewGrid` 传 `cardHeight` |
| 2026-07-17 | #30 节次持久化（修 #28） | `periodTimesJson` 写 DataStore，Widget 跨进程读 |
| 2026-07-17 | #30 小学期适配立项 | 学期枚举 `[3,12,16]`、周数/成绩年份/校区数据驱动 |
| 2026-07-17 | HAR 二次勘察：学期下拉是 HTML `<select>` | `16` 已内置；无需解析动态列表 |
| 2026-07-20 | #29 下学期课表三态浮层定稿 | DISCOVERY/REENTRY/BACK；会话内忽略；不切全局学期 |
| 2026-07-21 | #30 小学期适配落地 | `guessCurrentSemester` 加 16；周数上限动态；成绩年份动态；校区读偏好 |
| 2026-07-21 | #25 首页 cache-first | 启动零联网；考试读缓存；GPA 缓存统一公式 |
| 2026-07-21 | **v1.3.0 发布** | #22–#33 累积更新；versionCode 4 |

## 2026-08 至 09 稳定与发布

| 日期 | 变更 | 原因/结果 |
| --- | --- | --- |
| 2026-08-30 | HTTP 901 根因定位 + 修复 | `AuthInterceptor` 识别 901；见 [diagnostics/http901-session-expiry.md](../diagnostics/http901-session-expiry.md) |
| 2026-08-30 | **v1.3.1 发布** | 仅稳定性修复；versionCode 5 |
| 2026-08-30 | 历史回退：`git reset --hard v1.3.0`，改动备份到 `backup/2026-08-30`（`797e0ef`） | 管理员要求回到已发布版本；后续重新前进 |
| 2026-09-04 | 学期设置（M3 原生选择器）+ 考试学期收敛 + 表头月日 | **v1.4.0**；versionCode 6 |
| 2026-09-13 | 成绩/课表缓存优先 + 悬浮刷新 + 节次起止时间 | **v1.5.0**；versionCode 7；新增 Room 成绩表（v3→v4） |
| 2026-09-13 | 发布校验：签名 v2、证书 SHA-256 与 v1.4.0 一致、APK 7,367,150 B | 升级安装兼容 |

## 2026-10 记忆库重整

| 日期 | 变更 | 原因/结果 |
| --- | --- | --- |
| 2026-10-21 | 记忆库目录 `memory-bank/` → `memory_bank/`；新增 readme/tech-stack/handover-notes；architecture 拆子文档；progress 拆为路线图 + 本日志；诊断文档独立；batch4 有效需求并入设计文档 | 对齐团队规范工作流；旧文件内容仍可通过 Git 历史追溯 |
| 2026-10-21 | 代理端口 `7892`/工作区 `10808` → `7897`；许可证统一 AGPL-3.0 并补 `LICENSE`；新增安全项清单（随后搁置）；关闭考试分页项（用户实测正常）；小学期成绩改为待验证；i18n 搁置；修正课表长按发布说明（仅成绩页支持长按） | 用户逐项确认；许可证按应用内既有 AGPL 表述统一 |
| 2026-10-21 | `assembleDebug` 构建通过（增量 27s；`clean assembleDebug` 1m38s；APK 71,289,145 B）；代理 7897 验证可用 | 用户授权代理直接构建；从干净状态验证配置与工具链 |
| 2026-10-21 | 启动优化：移除 MainActivity/LoginViewModel 启动期网络会话校验，改为本地 `isFirstLaunch + isLoggedIn + hasLocalCookieSession` 判定；会话改为首次 API 惰性校验 + autoLogin；LoginViewModel 增加 `consumeLoginSuccess()` 防成功状态残留；构建 18s，APK 71,509,476 B；真机 24129PN74C / Android 17 安装并冷启动 528ms | 启动转圈数秒，根因为 `index_initMenu` 网络校验与重复调用；详见 [diagnostics/startup-latency.md](../diagnostics/startup-latency.md) |
| 2026-10-21 | 定位课表 Session 失效误报：真实课表页 HTTP 200、含“课表”、无登录表单，但 JS 注释含 5 处“登录”，被 `ScheduleRepository` 裸 `contains("登录")` 误判 | 重试/清数据/重登录均无效；详见 [diagnostics/schedule-login-false-positive.md](../diagnostics/schedule-login-false-positive.md) |
| 2026-10-21 | 修复课表 Session 误报：新增 `LoginPageDetector`（`login_slogin` + `yhm` 表单特征），替换 Schedule/Grade/Room 三处裸 `"登录"` 判断；新增 5 个单测；`assembleDebug` 27s，APK 71,509,296 B；真机 deep link 进课表 `isLoginPage=false` / `getSchedule -> 200`，课表正常 | 用户明确授权“按这个方案做”；单测与真机加载均通过 |

| 2026-10-21 | 小组件重做：`WidgetTickScheduler` + `WidgetTickWorker` 事件边界刷新（替代 24h 周期任务），`WidgetScheduleLogic` 纯函数（下一节次/硬滑动/假期周/行数自适应），三种尺寸样式统一；单测 24 个新增（合计 29），真机 tick 调度 `next tick in 154 min` 验证通过 | 用户答复刷新规则取舍：当天上完但未到 19:00 仍显示「已结束」，19:00 后才展示明天；不做色相 24 档实验 |
| 2026-10-21 | MD3 前端整改（分支 `feat/md3-ui-refresh`，提交 `f99d848`）：补全 15 个颜色角色、新增 `AppShapes`、去 alpha 与状态栏涂色、`NavigationBar` 顶层导航、GPA 分段按钮、空教室暴露式下拉、设置页 `ListItem`、课表周切换器上移；29 单测全绿，真机逐页截图核对 | 用户要求严格对齐 MD3 并参考 PixelPlayer；本轮只改前端，不动业务逻辑 |

## 2026-10 下旬 图书馆深化与「想看」闭环

| 日期 | 变更 | 原因/结果 |
| --- | --- | --- |
| 2026-10-28 | **发版 v1.7.0**（`673fed7`，versionCode 9）：图书馆模块 + CAS 单点登录 + MD3 整改 + 小组件重做；GitHub Release 置 latest | 见 README 与 CHANGELOG |
| 2026-10-30（晨） | 课表周次判定重构（`01b9bce`）：`oldzc`/`oldjc` 位掩码为权威来源，`CourseEntity` 增 `weekBits`/`periodBits`，五处统一改调；Room 4→5；修「第N周」被展开为整学期的缺陷 | 文本解析歧义；真机 20 行逐周比对 0 差异 |
| 2026-10-30（晚） | **「想看」功能闭环**（`0a7a119`/`88330c1`/`b3e3dc6`/`00a7d6d`/`143d1ce`）：本地数据层（Room 5→6 wishlist 快照表）→ 详情页收藏胶囊 → 想看列表页 → 首页入口与指标 → JSON 导出导入（SAF、按 bibId 幂等合并）；移动借书码入口；指标条「罚金」格改「想看」 | 管理员提出「书籍收藏」需求并逐项验收 |
| 2026-10-30（晚） | **书籍详情页重设计**（含于上组 commit）：沉浸头图（高度固定、渐变按封面比例贴图收口）+ 45% 视差 + 滚动整幅模糊淡出 + 状态栏图标随深度反色 + 骨架屏实验后移除；封面缓存键分层（`BookCovers.kt`）与预取；修换书封面串号（uiState.bibId 闸门）、DstIn 离屏合成、`wrapContentHeight` 解码失控等缺陷 | 逐轮真机反馈驱动；`dumpsys gfxinfo` 实测确认首帧组合为掉帧固有成本 |
| 2026-10-31（`8085d6b`） | **Compose 1.9 迁移 + 书目详情转场定稿**：依赖升到 Compose 1.9.5 / Material3 1.4.0（AGP 8.13.2、compileSdk 36、Glance 1.2.0，Kotlin 2.2.21 / KSP / Gradle 9.5.1 不动），源码只有 `Modifier.sharedElement` 的 `state` → `sharedContentState` 一处改名；详情页转场回到「整页挂共享容器」（`sharedBounds` 默认 `FillWidth` + `ParentClip`、不传 enter/exit、几何 220ms `M3EmphasizedEasing`），页面级对详情路由改成 `fadeIn(200ms)`（无 delay、去掉 `scaleIn`）+ `fadeOut(60ms)`，内容与封面同时显形、落位即实心；根节点补不透明底色；删掉 `contentFadeThrough` / `settleContentAfterTransform` | 用户反馈「卡片放大时书名也放大、就位后内容才补上 / 详情信息闪现」。先后否决：`Fit`+`OverlayClip`、元素级 hero（路线 I）、回滚 v1.7.0；最终以「录屏对齐版 + 内容提前显形」通过 |
| 2026-10-30（晚，已提交 `1be26c3`） | **容器变换返工**（检索结果 / 想看 → 书籍详情）：卡片根仍走 `sharedBounds`，其封面与详情头图封面改挂真 `sharedElement`（`sharedCoverModifier`，key `search-cover-<bibId>`）；两端取景统一 `Crop + TopCenter`；目标页内容改由 `ContainerSettleEnterTransition` 在容器走完后（300ms + 160ms）落定；往返统一 300ms `M3DecelerateEasing`；`AnimatedContent` 只对 bibDetail 往返用 fade-through；想看行不再整行做容器变换，只让封面成为共享元素；路由第 4 段携带卡片封面消除首帧空封面。`assembleDebug` 21:54 通过并装到 `db072f50` | 用户反馈「放大到近全屏时减淡 + 详情渐显，闪烁且封面不连贯」，选定「档 1 + 档 2」修法；真机验收待做 |
| 2026-10-30（晚） | **动画定案**（`3877e97`/`2e55867`/`31c914e`）：容器变换（sharedBounds 整页展开、默认参数、tween 220ms FastOutSlowIn）保留为最重视动画；检索结果 → 详情页不走共享元素；内容级动画（馆藏卡展开收起）用 M3 emphasized 曲线。否决实验：fade through 错开（转场变长）、RemeasureToBounds（每帧重测整页掉帧）、封面 sharedElement、纯模糊过渡（与共享元素互斥）。**后两条（检索路径不走共享元素、否决封面 sharedElement）当日晚些时候被管理员推翻并返工，见上一行** | 多方案对照实验 + 官方文档查证；管理员拍板（其中「检索路径」结论实为编码模型误改所致） |
| 2026-10-30 | **记忆库整理**：删除内容过期的 `memory_bank/design-document.md`（v1.5.0 需求台账，无图书馆/想看）与仓库根 `widget/HyperOS-Widget-Adaptation-Plan.md`（按 4 尺寸 + Fragment 写，与 3 尺寸 Compose 实现不符，待办已全部落地）；有效内容并入 handover-notes（产品定位与边界）、architecture（决策 12、13）、tech-stack（状态表达）；同时校正导航（无底部栏/无抽屉）、Room v6 四表、首页五卡等与代码不符的描述 | 遵循「不得有多余文件、同一内容只存一处」的维护约定 |

| 2026-10-31 | **Compose 1.9 工具链迁移**（未提交）：compose-bom 2024.12.01 → 2025.11.01（ui 1.9.5 / material3 1.4.0 / material-icons-extended 1.7.8，版本逐条取自 BOM 的 pom）、AGP 8.7.3 → 8.13.2、Glance 1.1.1 → 1.2.0、compileSdk 35 → 36；Kotlin 2.2.21 / KSP / Gradle 9.5.1 / targetSdk 35 / 其余 androidx 库全部不动。`platforms/android-36` 由 AGP 自动补装。源码唯一改动：`Modifier.sharedElement` 的参数 `state` 更名为 `sharedContentState`。`assembleDebug` 1m35s 通过，`adb install -r` 到 `db072f50` 成功 | 依据管理员「组件版本要成套匹配、不要迁一半再手动修」的要求，按 6 条耦合线（AGP↔Gradle、AGP↔compileSdk、Kotlin↔KSP、Kotlin↔Compose 编译插件、Compose↔AAR 的 `minCompileSdk`、Compose↔BOM）一次对齐；基线 tag `pre-compose19` |

## 发布索引

| 版本 | 日期 | 主要变化 |
| --- | --- | --- |
| v1.0.0 | 2026-06-07 | MVP：登录、课表、成绩、空教室、考试、单首页 |
| v1.1.0 | 待补充 | 旧记录未保留明确日期与范围 |
| v1.2.0 | 待补充 | 旧记录未保留明确日期与范围 |
| v1.3.0 | 2026-07-21 | Batch 10 #22–#33 |
| v1.3.1 | 2026-08-30 | HTTP 901 修复 |
| v1.4.0 | 2026-09-04 | 学期设置、考试学期收敛、表头月日 |
| v1.5.0 | 2026-09-13 | 缓存优先、悬浮刷新、节次起止时间 |
| v1.6.0 | 2026-10-22 | 图书馆（汇文 OPAC）只读 7 页；教务/图书馆统一走学校 CAS，旧 xtgl 直登删除 |
| v1.7.0 | 2026-10-28 | 图书馆 UI 四轮迭代（逐级返回、页码翻页、荐购标签、瀑布流、骨架屏、转场提速）+ MD3 整改 + 小组件重做 |

> v1.1.0/v1.2.0 的发布日期在现有记忆与 CHANGELOG 中均未记录；需要时用 `git log --decorate --tags` 反查，不要猜。
